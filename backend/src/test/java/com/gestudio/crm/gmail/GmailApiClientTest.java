package com.gestudio.crm.gmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class GmailApiClientTest {

  private static final Instant NOW = Instant.parse("2026-07-28T12:00:00Z");

  private HttpServer server;

  @AfterEach
  void stop() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void sendsOneMimeMessageAndPersistsProviderAcceptanceMetadata() throws Exception {
    AtomicReference<String> authorization = new AtomicReference<>();
    AtomicReference<String> requestBody = new AtomicReference<>();
    start(
        exchange -> {
          authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          respond(exchange, 200, "{\"id\":\"gmail-message-1\",\"threadId\":\"thread-1\"}");
        });

    GmailApiClient.SendResult result =
        client(Duration.ofSeconds(2)).send("synthetic-access", message());

    assertThat(result.providerMessageId()).isEqualTo("gmail-message-1");
    assertThat(result.threadId()).isEqualTo("thread-1");
    assertThat(result.httpStatus()).isEqualTo(200);
    assertThat(result.acceptedAt()).isEqualTo(NOW);
    assertThat(authorization.get()).isEqualTo("Bearer synthetic-access");
    assertThat(requestBody.get()).contains("\"raw\"").doesNotContain("Synthetic body");
  }

  @Test
  void classifiesAuthenticationPermissionQuotaRateLimitAndServerFailures() throws Exception {
    assertFailure(401, "{}", GmailProviderException.Category.REAUTH_REQUIRED, null);
    assertFailure(
        403,
        "{\"error\":\"insufficient permission\"}",
        GmailProviderException.Category.INSUFFICIENT_SCOPE,
        null);
    assertFailure(
        403, "{\"error\":\"daily quota exceeded\"}", GmailProviderException.Category.QUOTA, null);
    assertFailure(500, "{}", GmailProviderException.Category.RETRYABLE, null);

    start(
        exchange -> {
          exchange.getResponseHeaders().add("Retry-After", "120");
          respond(exchange, 429, "{}");
        });
    assertThatThrownBy(() -> client(Duration.ofSeconds(2)).send("synthetic", message()))
        .isInstanceOfSatisfying(
            GmailProviderException.class,
            exception -> {
              assertThat(exception.category())
                  .isEqualTo(GmailProviderException.Category.RATE_LIMIT);
              assertThat(exception.retryAt()).isEqualTo(NOW.plusSeconds(120));
            });
  }

  @Test
  void treatsMalformedSuccessTimeoutAndClosedConnectionAsAmbiguous() throws Exception {
    start(exchange -> respond(exchange, 200, "{\"threadId\":\"thread-only\"}"));
    assertAmbiguous(Duration.ofSeconds(2));

    stop();
    start(
        exchange -> {
          try {
            Thread.sleep(300);
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
          }
          respond(exchange, 200, "{\"id\":\"too-late\"}");
        });
    assertAmbiguous(Duration.ofMillis(100));

    stop();
    start(HttpExchange::close);
    assertAmbiguous(Duration.ofSeconds(2));
  }

  @Test
  void rejectsInvalidRecipientBeforeNetwork() throws Exception {
    start(exchange -> respond(exchange, 200, "{\"id\":\"unexpected\"}"));
    var original = message();
    var invalid =
        new GmailMimeBuilder.Message(
            original.fromEmail(),
            original.fromDisplayName(),
            "invalid-recipient",
            original.replyTo(),
            original.subject(),
            original.textBody(),
            original.htmlBody(),
            original.unsubscribeUri(),
            original.messageId(),
            original.date());
    assertThatThrownBy(() -> client(Duration.ofSeconds(2)).send("synthetic", invalid))
        .isInstanceOfSatisfying(
            GmailProviderException.class,
            exception ->
                assertThat(exception.category())
                    .isEqualTo(GmailProviderException.Category.VALIDATION));
  }

  private void assertFailure(
      int status, String body, GmailProviderException.Category category, Instant retryAt)
      throws Exception {
    stop();
    start(exchange -> respond(exchange, status, body));
    assertThatThrownBy(() -> client(Duration.ofSeconds(2)).send("synthetic", message()))
        .isInstanceOfSatisfying(
            GmailProviderException.class,
            exception -> {
              assertThat(exception.category()).isEqualTo(category);
              assertThat(exception.httpStatus()).isEqualTo(status);
              assertThat(exception.retryAt()).isEqualTo(retryAt);
              assertThat(exception.getMessage()).doesNotContain(body);
            });
  }

  private void assertAmbiguous(Duration timeout) {
    assertThatThrownBy(() -> client(timeout).send("synthetic", message()))
        .isInstanceOfSatisfying(
            GmailProviderException.class,
            exception -> {
              assertThat(exception.category()).isEqualTo(GmailProviderException.Category.AMBIGUOUS);
              assertThat(exception.retryable()).isFalse();
            });
  }

  private GmailApiClient client(Duration timeout) {
    GmailDeliveryProperties properties = GmailTestProperties.properties(baseUrl(), timeout);
    GmailMimeBuilder builder = new GmailMimeBuilder(properties);
    return new GmailApiClient(
        HttpClient.newBuilder().connectTimeout(timeout).build(),
        new ObjectMapper(),
        properties,
        builder,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private GmailMimeBuilder.Message message() {
    return new GmailMimeBuilder.Message(
        "sender@example.test",
        "Synthetic Sender",
        "recipient@example.test",
        "reply@example.test",
        "Synthetic subject",
        "Synthetic body",
        "<p>Synthetic body</p>",
        URI.create(baseUrl() + "/api/v1/unsubscribe/synthetic-token"),
        UUID.randomUUID(),
        NOW);
  }

  private void start(Handler handler) throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/gmail/v1/users/me/messages/send",
        exchange -> {
          try {
            handler.handle(exchange);
          } finally {
            exchange.close();
          }
        });
    server.start();
  }

  private String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  private void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
  }

  @FunctionalInterface
  private interface Handler {
    void handle(HttpExchange exchange) throws IOException;
  }
}
