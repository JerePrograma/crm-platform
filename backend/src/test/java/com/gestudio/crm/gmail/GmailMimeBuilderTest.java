package com.gestudio.crm.gmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GmailMimeBuilderTest {

  private final GmailMimeBuilder builder =
      new GmailMimeBuilder(GmailTestProperties.properties("https://example.test"));

  @Test
  void buildsUtf8AlternativeMessageWithOneClickAndVisibleUnsubscribe() {
    GmailMimeBuilder.Message message =
        message("Asunto sintético ñ", "Cuerpo sintético á", "<p>Cuerpo sintético á</p>");

    String raw = builder.raw(message);
    String mime = new String(Base64.getUrlDecoder().decode(raw), StandardCharsets.UTF_8);

    assertThat(raw).doesNotContain("=", "+", "/");
    assertThat(mime)
        .contains("From: =?UTF-8?B?")
        .contains(" <sender@example.test>")
        .contains("To: recipient@example.test")
        .contains("Reply-To: reply@example.test")
        .contains("Message-ID: <" + message.messageId() + "@gestudio.invalid>")
        .contains("MIME-Version: 1.0")
        .contains("multipart/alternative")
        .contains("Content-Type: text/plain; charset=UTF-8")
        .contains("Content-Type: text/html; charset=UTF-8")
        .contains("List-Unsubscribe: <" + message.unsubscribeUri() + ">")
        .contains("List-Unsubscribe-Post: List-Unsubscribe=One-Click");
    assertThat(decodedParts(mime))
        .contains("Cuerpo sintético á", "Darse de baja", message.unsubscribeUri().toString());
  }

  @Test
  void rejectsHeaderInjectionUnsafeHtmlAndForeignUnsubscribeOrigin() {
    assertThatThrownBy(
            () ->
                builder.raw(
                    new GmailMimeBuilder.Message(
                        "sender@example.test",
                        "Synthetic\r\nBcc: attacker@example.test",
                        "recipient@example.test",
                        null,
                        "Subject",
                        "Text",
                        "<p>Text</p>",
                        URI.create("https://example.test/api/v1/unsubscribe/token"),
                        UUID.randomUUID(),
                        Instant.parse("2026-07-28T12:00:00Z"))))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> builder.raw(message("Subject", "Text", "<script>alert(1)</script>")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unsafe");
    var original = message("Subject", "Text", "<p>Text</p>");
    assertThatThrownBy(
            () ->
                builder.raw(
                    new GmailMimeBuilder.Message(
                        original.fromEmail(),
                        original.fromDisplayName(),
                        original.to(),
                        original.replyTo(),
                        original.subject(),
                        original.textBody(),
                        original.htmlBody(),
                        URI.create("https://evil.example/api/v1/unsubscribe/token"),
                        original.messageId(),
                        original.date())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("safe HTTPS");
  }

  @Test
  void permitsLoopbackHttpOnlyWhenExplicitlyConfigured() {
    GmailMimeBuilder loopback =
        new GmailMimeBuilder(GmailTestProperties.properties("http://127.0.0.1:18080"));
    var original = message("Subject", "Text", "<p>Text</p>");
    var local =
        new GmailMimeBuilder.Message(
            original.fromEmail(),
            original.fromDisplayName(),
            original.to(),
            original.replyTo(),
            original.subject(),
            original.textBody(),
            original.htmlBody(),
            URI.create("http://127.0.0.1:18080/api/v1/unsubscribe/token"),
            original.messageId(),
            original.date());

    assertThat(loopback.raw(local)).isNotBlank();
  }

  private GmailMimeBuilder.Message message(String subject, String text, String html) {
    return new GmailMimeBuilder.Message(
        "sender@example.test",
        "Synthetic Sender",
        "recipient@example.test",
        "reply@example.test",
        subject,
        text,
        html,
        URI.create("https://example.test/api/v1/unsubscribe/synthetic-token"),
        UUID.randomUUID(),
        Instant.parse("2026-07-28T12:00:00Z"));
  }

  private String decodedParts(String mime) {
    StringBuilder decoded = new StringBuilder();
    for (String section : mime.split("--gestudio_[0-9a-f]+")) {
      int body = section.indexOf("\r\n\r\n");
      if (body < 0 || !section.contains("Content-Transfer-Encoding: base64")) {
        continue;
      }
      String encoded = section.substring(body + 4).replace("\r", "").replace("\n", "");
      if (!encoded.isBlank()) {
        decoded.append(new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8));
      }
    }
    return decoded.toString();
  }
}
