package com.gestudio.crm.inbound;

import com.gestudio.crm.audit.AuditEventWriter;
import com.gestudio.crm.common.CorrelationIds;
import com.gestudio.crm.common.OptimisticConflictException;
import com.gestudio.crm.outbox.OutboxPublisher;
import com.gestudio.crm.outbox.OutboxPublisher.PublishCommand;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

@Service
public class FakeInboundWebhookService {

  private static final String PROVIDER = "FAKE_INBOUND";

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final InboundProperties properties;
  private final WebhookSignatureVerifier signatureVerifier;
  private final ReplayProtectionService replayProtectionService;
  private final InboundMessageNormalizer normalizer;
  private final InboundRateLimiter rateLimiter;
  private final OutboxPublisher outboxPublisher;
  private final AuditEventWriter auditEventWriter;

  public FakeInboundWebhookService(
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper,
      Clock clock,
      InboundProperties properties,
      WebhookSignatureVerifier signatureVerifier,
      ReplayProtectionService replayProtectionService,
      InboundMessageNormalizer normalizer,
      InboundRateLimiter rateLimiter,
      OutboxPublisher outboxPublisher,
      AuditEventWriter auditEventWriter) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.properties = properties;
    this.signatureVerifier = signatureVerifier;
    this.replayProtectionService = replayProtectionService;
    this.normalizer = normalizer;
    this.rateLimiter = rateLimiter;
    this.outboxPublisher = outboxPublisher;
    this.auditEventWriter = auditEventWriter;
  }

  @Transactional
  public ReceiptAccepted receive(
      UUID organizationId,
      long timestamp,
      String nonce,
      String signature,
      byte[] payload,
      String remoteAddress) {
    if (!properties.configured()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Fake inbound is disabled");
    }
    if (payload.length > properties.maxPayloadBytes()) {
      throw new ResponseStatusException(
          HttpStatus.PAYLOAD_TOO_LARGE, "Webhook payload is too large");
    }
    rateLimiter.requireAllowed(organizationId + ":" + remoteAddress);
    Instant signedAt;
    try {
      signedAt = Instant.ofEpochSecond(timestamp);
    } catch (RuntimeException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Webhook timestamp is invalid");
    }
    if (signedAt.isBefore(clock.instant().minus(properties.signatureWindow()))
        || signedAt.isAfter(clock.instant().plus(properties.signatureWindow()))) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Webhook timestamp is outside the allowed window");
    }
    if (!signatureVerifier.verify(organizationId, timestamp, nonce, payload, signature)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Webhook signature is invalid");
    }
    if (!organizationExists(organizationId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Webhook organization was not found");
    }
    FakeInboundRequest request = read(payload);
    validate(request);
    String payloadHash = OutboxPublisher.sha256(new String(payload, StandardCharsets.UTF_8));
    String nonceHash = OutboxPublisher.sha256(nonce);
    ReplayProtectionService.ExistingReceipt existing =
        replayProtectionService.check(
            organizationId, PROVIDER, request.externalEventId(), nonceHash);
    if (existing != null) {
      if (!existing.payloadHash().equals(payloadHash)) {
        throw new OptimisticConflictException(
            "External event ID was already used with a different payload");
      }
      return new ReceiptAccepted(
          existing.id(), existing.status(), true, CorrelationIds.currentOrCreate());
    }

    UUID id = UUID.randomUUID();
    Instant now = clock.instant();
    Instant receivedAt = parsedReceivedAt(request.receivedAt(), signedAt);
    String sender = normalizer.contact(request.channel(), request.sender());
    String recipient =
        request.recipient() == null || request.recipient().isBlank()
            ? null
            : normalizer.contact(request.channel(), request.recipient());
    String correlationId = CorrelationIds.currentOrCreate();
    jdbcTemplate.update(
        """
        INSERT INTO inbound_message (
          id, organization_id, provider, external_event_id, external_message_id,
          external_thread_id, channel, sender_normalized, recipient_normalized,
          received_at, payload_hash, body_excerpt, metadata, status, association_status,
          correlation_id, nonce_hash, created_at
        ) VALUES (?, ?, 'FAKE_INBOUND', ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb),
          'PENDING', 'PENDING', ?, ?, ?)
        """,
        id,
        organizationId,
        request.externalEventId(),
        request.externalMessageId(),
        request.externalThreadId(),
        request.channel(),
        sender,
        recipient,
        Timestamp.from(receivedAt),
        payloadHash,
        normalizer.excerpt(request.body()),
        json(Map.of("inReplyToMessageId", nullable(request.inReplyToMessageId()))),
        correlationId,
        nonceHash,
        Timestamp.from(now));
    outboxPublisher.publish(
        new PublishCommand(
            organizationId,
            "INBOUND_RECEIVED_V1",
            1,
            "INBOUND_MESSAGE",
            id,
            Map.of("inboundMessageId", id.toString()),
            "inbound:fake:" + request.externalEventId(),
            correlationId,
            null,
            5));
    auditEventWriter.recordFor(
        organizationId,
        null,
        "INBOUND_RECEIVED",
        "INBOUND_MESSAGE",
        id,
        "SUCCESS",
        Map.of("provider", PROVIDER, "correlationId", correlationId));
    return new ReceiptAccepted(id, "PENDING", false, correlationId);
  }

  private boolean organizationExists(UUID organizationId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM organization WHERE id = ? AND active = TRUE",
            Integer.class,
            organizationId);
    return count != null && count == 1;
  }

  private FakeInboundRequest read(byte[] payload) {
    try {
      return objectMapper
          .readerFor(FakeInboundRequest.class)
          .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .readValue(payload);
    } catch (JacksonException exception) {
      throw new IllegalArgumentException("Webhook JSON is invalid or contains unexpected fields");
    }
  }

  private void validate(FakeInboundRequest request) {
    if (request == null
        || blank(request.externalEventId())
        || blank(request.externalMessageId())
        || blank(request.channel())
        || blank(request.sender())) {
      throw new IllegalArgumentException("Webhook event, message, channel and sender are required");
    }
    if (request.externalEventId().length() > 200
        || request.externalMessageId().length() > 200
        || request.sender().length() > 320
        || request.body() != null && request.body().length() > 4000) {
      throw new IllegalArgumentException("Webhook field exceeds its allowed limit");
    }
    if (!request.channel().equals("EMAIL") && !request.channel().equals("WHATSAPP")) {
      throw new IllegalArgumentException("Webhook channel is invalid");
    }
  }

  private Instant parsedReceivedAt(String value, Instant fallback) {
    if (blank(value)) {
      return fallback;
    }
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException exception) {
      throw new IllegalArgumentException("Webhook receivedAt must be an ISO-8601 instant");
    }
  }

  private String json(Map<String, ?> value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JacksonException exception) {
      throw new IllegalArgumentException("Webhook metadata could not be serialized", exception);
    }
  }

  private Object nullable(String value) {
    return value == null ? "" : value;
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }

  public record FakeInboundRequest(
      String externalEventId,
      String externalMessageId,
      String externalThreadId,
      String inReplyToMessageId,
      String channel,
      String sender,
      String recipient,
      String receivedAt,
      String body) {}

  public record ReceiptAccepted(
      UUID receiptId, String status, boolean duplicate, String correlationId) {}
}
