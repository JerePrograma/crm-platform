package com.gestudio.crm.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestudio.crm.settings.SendingProperties;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ProviderContractTest {

  private HttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void gmailCreatesDraftAgainstLoopbackContractButSendStaysDisabled() throws Exception {
    AtomicReference<String> request =
        server(
            "/gmail/v1/users/me/drafts",
            200,
            "{\"id\":\"draft-1\",\"message\":{\"threadId\":\"thread-1\"}}");
    var provider =
        new GmailEmailProvider(
            client(),
            new ObjectMapper(),
            baseUri(),
            "synthetic-token",
            Set.of(GmailEmailProvider.COMPOSE_SCOPE),
            Duration.ofSeconds(2),
            false);

    var result = provider.createDraft(message("mail@example.test", "gmail-draft"));
    assertThat(result.result()).isEqualTo("PROVIDER_DRAFT_CREATED");
    assertThat(result.externalMessageId()).isEqualTo("draft-1");
    assertThat(result.externalThreadId()).isEqualTo("thread-1");
    assertThat(request.get()).contains("\"message\"").contains("\"raw\"");
    assertThatThrownBy(() -> provider.send(message("mail@example.test", "gmail-send")))
        .isInstanceOf(ProviderException.class)
        .hasMessageContaining("draft-only");
  }

  @Test
  void whatsappUsesVersionedContractAndClassifiesRateLimitAsRetryable() throws Exception {
    AtomicReference<String> request =
        server("/v99.0/123456/messages", 200, "{\"messages\":[{\"id\":\"wamid.synthetic\"}]}");
    var provider =
        new WhatsAppCloudProvider(
            client(),
            new ObjectMapper(),
            baseUri(),
            "v99.0",
            "123456",
            "synthetic-token",
            Duration.ofSeconds(2),
            4096,
            true);
    var result = provider.send(message("+5493415550101", "wa-contract"));
    assertThat(result.externalMessageId()).isEqualTo("wamid.synthetic");
    assertThat(request.get())
        .contains("\"messaging_product\":\"whatsapp\"")
        .contains("5493415550101");

    server.stop(0);
    server = null;
    server("/v99.0/123456/messages", 429, "{\"error\":{\"message\":\"rate limited\"}}");
    var limited =
        new WhatsAppCloudProvider(
            client(),
            new ObjectMapper(),
            baseUri(),
            "v99.0",
            "123456",
            "synthetic-token",
            Duration.ofSeconds(2),
            4096,
            true);
    assertThatThrownBy(() -> limited.send(message("+5493415550101", "wa-rate-limit")))
        .isInstanceOfSatisfying(
            ProviderException.class,
            exception -> {
              assertThat(exception.code()).isEqualTo("WHATSAPP_HTTP_429");
              assertThat(exception.retryable()).isTrue();
            });
  }

  @Test
  void providerConfigurationFailsClosedBeforeRealAdaptersCanInitialize() {
    var properties =
        new MessagingProperties(
            "GMAIL_DRAFT_ONLY",
            "WHATSAPP_CLOUD",
            false,
            Duration.ofSeconds(2),
            new MessagingProperties.Gmail(
                "https://gmail.googleapis.com",
                "synthetic-token",
                GmailEmailProvider.COMPOSE_SCOPE),
            new MessagingProperties.WhatsApp(
                "https://graph.facebook.com",
                "v99.0",
                "123456",
                "654321",
                "synthetic-token",
                4096));
    var configuration = new MessagingProviderConfiguration();
    assertThatThrownBy(() -> configuration.emailProvider(properties, client(), new ObjectMapper()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("real network is disabled");
    assertThatThrownBy(
            () ->
                configuration.whatsAppProvider(
                    properties,
                    new SendingProperties(false, true, 0, true),
                    client(),
                    new ObjectMapper()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("real network is disabled");
  }

  private AtomicReference<String> server(String path, int status, String response)
      throws IOException {
    AtomicReference<String> body = new AtomicReference<>();
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        path,
        exchange -> {
          body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(status, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
    return body;
  }

  private URI baseUri() {
    return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
  }

  private HttpClient client() {
    return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
  }

  private OutboundMessage message(String recipient, String key) {
    return new OutboundMessage(
        UUID.randomUUID(),
        UUID.randomUUID(),
        recipient,
        "Synthetic subject",
        "Synthetic body",
        "<p>Synthetic body</p>",
        key);
  }
}
