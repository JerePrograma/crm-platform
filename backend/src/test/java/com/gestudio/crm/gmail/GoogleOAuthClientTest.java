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
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class GoogleOAuthClientTest {

  private static final Instant NOW = Instant.parse("2026-07-28T12:00:00Z");

  private HttpServer server;

  @AfterEach
  void stop() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void buildsOfflineAuthorizationAndCompletesCodeRefreshUserinfoAndRevocation() throws Exception {
    AtomicReference<String> tokenRequest = new AtomicReference<>();
    AtomicReference<String> revokeRequest = new AtomicReference<>();
    start();
    server.createContext(
        "/token",
        exchange -> {
          String body = read(exchange);
          tokenRequest.set(body);
          if (body.contains("grant_type=authorization_code")) {
            respond(
                exchange,
                200,
                "{\"access_token\":\"access-one\",\"refresh_token\":\"refresh-one\","
                    + "\"expires_in\":3600,\"scope\":\"openid email https://www.googleapis.com/auth/gmail.send\"}");
          } else {
            respond(exchange, 200, "{\"access_token\":\"access-two\",\"expires_in\":1800} ");
          }
        });
    server.createContext(
        "/userinfo",
        exchange ->
            respond(
                exchange,
                200,
                "{\"email\":\"sender@example.test\",\"name\":\"Synthetic Sender\",\"email_verified\":true}"));
    server.createContext(
        "/revoke",
        exchange -> {
          revokeRequest.set(read(exchange));
          respond(exchange, 200, "{}");
        });
    server.start();
    GoogleOAuthClient client = client();
    String state = "A".repeat(43);

    URI authorization = client.authorizationUri(state);
    assertThat(authorization.getRawQuery())
        .contains("response_type=code")
        .contains("access_type=offline")
        .contains("prompt=consent")
        .contains("state=" + state)
        .contains("gmail.send");

    var exchanged = client.exchangeAuthorizationCode("synthetic-code");
    assertThat(exchanged.accessToken()).isEqualTo("access-one");
    assertThat(exchanged.refreshToken()).isEqualTo("refresh-one");
    assertThat(exchanged.scopes()).contains(GmailDeliveryProperties.SEND_SCOPE, "email");
    assertThat(exchanged.toString())
        .doesNotContain("access-one", "refresh-one")
        .contains("REDACTED");
    assertThat(new ObjectMapper().writeValueAsString(exchanged))
        .doesNotContain("access-one", "refresh-one");

    var refreshed = client.refreshAccessToken("refresh-one");
    assertThat(refreshed.accessToken()).isEqualTo("access-two");
    assertThat(refreshed.refreshToken()).isNull();
    assertThat(refreshed.expiresAt()).isEqualTo(NOW.plusSeconds(1800));
    assertThat(tokenRequest.get())
        .contains("refresh_token=refresh-one")
        .doesNotContain("access-one");

    var info = client.userInfo("access-two");
    assertThat(info.email()).isEqualTo("sender@example.test");
    assertThat(info.emailVerified()).isTrue();
    client.revoke("refresh-one");
    assertThat(revokeRequest.get()).isEqualTo("token=refresh-one");
  }

  @Test
  void mapsInvalidGrantAndRejectsUnverifiedIdentity() throws Exception {
    start();
    server.createContext(
        "/token", exchange -> respond(exchange, 400, "{\"error\":\"invalid_grant\"}"));
    server.createContext(
        "/userinfo",
        exchange ->
            respond(exchange, 200, "{\"email\":\"sender@example.test\",\"email_verified\":false}"));
    server.start();
    GoogleOAuthClient client = client();

    assertThatThrownBy(() -> client.refreshAccessToken("revoked-refresh"))
        .isInstanceOfSatisfying(
            GoogleOAuthException.class,
            exception ->
                assertThat(exception.code()).isEqualTo(GoogleOAuthException.Code.INVALID_GRANT));
    assertThatThrownBy(() -> client.userInfo("synthetic-access"))
        .isInstanceOfSatisfying(
            GoogleOAuthException.class,
            exception ->
                assertThat(exception.code()).isEqualTo(GoogleOAuthException.Code.INVALID_RESPONSE));
  }

  private void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
  }

  private GoogleOAuthClient client() {
    return new GoogleOAuthClient(
        HttpClient.newBuilder().build(),
        new ObjectMapper(),
        GmailTestProperties.properties(baseUrl()),
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  private String read(HttpExchange exchange) throws IOException {
    return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
  }

  private void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }
}
