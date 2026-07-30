package com.gestudio.crm.gmail;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class GmailMimeBuilder {

  private static final Pattern EMAIL =
      Pattern.compile(
          "^[A-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?(?:\\.[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?)+$",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern UNSAFE_HTML =
      Pattern.compile(
          "(?is)<\\s*(script|iframe|object|embed|link|meta|style|form|base)\\b|\\son[a-z]+\\s*=|(?:javascript|data\\s*:\\s*text/html)\\s*:");

  private final GmailDeliveryProperties properties;

  public GmailMimeBuilder(GmailDeliveryProperties properties) {
    this.properties = properties;
  }

  public String raw(Message message) {
    validate(message);
    String boundary = "gestudio_" + message.messageId().toString().replace("-", "");
    String text =
        message.textBody().stripTrailing() + "\r\n\r\nDarse de baja: " + message.unsubscribeUri();
    String html =
        message.htmlBody().stripTrailing()
            + "\n<p><a rel=\"nofollow\" href=\""
            + escapeHtmlAttribute(message.unsubscribeUri().toASCIIString())
            + "\">Darse de baja</a></p>";

    StringBuilder mime = new StringBuilder(2048 + text.length() + html.length());
    header(mime, "From", mailbox(message.fromDisplayName(), message.fromEmail()));
    header(mime, "To", message.to());
    if (message.replyTo() != null && !message.replyTo().isBlank()) {
      header(mime, "Reply-To", message.replyTo().trim());
    }
    header(mime, "Subject", encodedWord(message.subject()));
    header(
        mime,
        "Date",
        DateTimeFormatter.RFC_1123_DATE_TIME.format(message.date().atOffset(ZoneOffset.UTC)));
    header(mime, "Message-ID", "<" + message.messageId() + "@gestudio.invalid>");
    header(mime, "MIME-Version", "1.0");
    header(mime, "List-Unsubscribe", "<" + message.unsubscribeUri().toASCIIString() + ">");
    header(mime, "List-Unsubscribe-Post", "List-Unsubscribe=One-Click");
    header(mime, "Content-Type", "multipart/alternative; boundary=\"" + boundary + "\"");
    mime.append("\r\n");
    part(mime, boundary, "text/plain", text);
    part(mime, boundary, "text/html", html);
    mime.append("--").append(boundary).append("--\r\n");
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(mime.toString().getBytes(StandardCharsets.UTF_8));
  }

  private void part(StringBuilder mime, String boundary, String mediaType, String content) {
    mime.append("--").append(boundary).append("\r\n");
    header(mime, "Content-Type", mediaType + "; charset=UTF-8");
    header(mime, "Content-Transfer-Encoding", "base64");
    mime.append("\r\n");
    mime.append(
        Base64.getMimeEncoder(76, "\r\n".getBytes(StandardCharsets.US_ASCII))
            .encodeToString(content.getBytes(StandardCharsets.UTF_8)));
    mime.append("\r\n");
  }

  private void validate(Message message) {
    if (message == null
        || message.messageId() == null
        || message.date() == null
        || message.unsubscribeUri() == null) {
      throw new IllegalArgumentException("Complete Gmail message metadata is required");
    }
    email(message.fromEmail(), "From");
    email(message.to(), "To");
    if (message.replyTo() != null && !message.replyTo().isBlank()) {
      email(message.replyTo(), "Reply-To");
    }
    text(message.fromDisplayName(), "From display name", 200, false);
    text(message.subject(), "Subject", 255, false);
    text(message.textBody(), "Text body", 500_000, true);
    text(message.htmlBody(), "HTML body", 1_000_000, true);
    URI unsubscribe = message.unsubscribeUri();
    if (!properties.isApprovedUnsubscribeUri(unsubscribe)
        || containsCrlf(unsubscribe.toASCIIString())) {
      throw new IllegalArgumentException("Unsubscribe URI must be a safe HTTPS URL");
    }
    if (UNSAFE_HTML.matcher(message.htmlBody()).find()) {
      throw new IllegalArgumentException("HTML body contains unsafe content");
    }
  }

  private void email(String value, String label) {
    String candidate = text(value, label, 320, false);
    if (!EMAIL.matcher(candidate).matches()) {
      throw new IllegalArgumentException(label + " email address is invalid");
    }
  }

  private String text(String value, String label, int maximumLength, boolean multiline) {
    if (value == null || value.isBlank() || value.length() > maximumLength) {
      throw new IllegalArgumentException(label + " is missing or too long");
    }
    if ((!multiline && containsCrlf(value)) || value.indexOf('\0') >= 0) {
      throw new IllegalArgumentException(label + " contains invalid characters");
    }
    return value.trim();
  }

  private String mailbox(String displayName, String email) {
    return displayName == null || displayName.isBlank()
        ? email.trim().toLowerCase(Locale.ROOT)
        : encodedWord(displayName.trim()) + " <" + email.trim().toLowerCase(Locale.ROOT) + ">";
  }

  private String encodedWord(String value) {
    return "=?UTF-8?B?"
        + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8))
        + "?=";
  }

  private void header(StringBuilder mime, String name, String value) {
    if (containsCrlf(name) || containsCrlf(value)) {
      throw new IllegalArgumentException("MIME header contains a line break");
    }
    mime.append(name).append(": ").append(value).append("\r\n");
  }

  private boolean containsCrlf(String value) {
    return value != null && (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0);
  }

  private String escapeHtmlAttribute(String value) {
    return value
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
  }

  public record Message(
      String fromEmail,
      String fromDisplayName,
      String to,
      String replyTo,
      String subject,
      String textBody,
      String htmlBody,
      URI unsubscribeUri,
      UUID messageId,
      Instant date) {}
}
