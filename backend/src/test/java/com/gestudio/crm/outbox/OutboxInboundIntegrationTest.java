package com.gestudio.crm.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestudio.crm.common.OptimisticConflictException;
import com.gestudio.crm.identity.CrmPrincipal;
import com.gestudio.crm.identity.IdentityService;
import com.gestudio.crm.inbound.FakeInboundWebhookService;
import com.gestudio.crm.inbound.FakeInboundWebhookService.ReceiptAccepted;
import com.gestudio.crm.inbound.InboundAssociationService;
import com.gestudio.crm.outbox.OutboxPublisher.PublishCommand;
import com.gestudio.crm.prospect.ProspectApplicationService;
import com.gestudio.crm.prospect.ProspectApplicationService.CreateProspectCommand;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
      "security.bootstrap.username=outbox-admin",
      "security.bootstrap.password=outbox-password-1",
      "crm.inbound.enabled=true",
      "crm.inbound.fake-webhook-secret=synthetic-inbound-secret",
      "crm.outbox.enabled=false",
      "crm.outbox.base-backoff=0ms",
      "crm.outbox.max-backoff=0ms"
    })
@Testcontainers
class OutboxInboundIntegrationTest {

  private static final String SECRET = "synthetic-inbound-secret";

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private IdentityService identityService;
  @Autowired private ProspectApplicationService prospectApplicationService;
  @Autowired private OutboxPublisher publisher;
  @Autowired private OutboxWorkerService worker;
  @Autowired private OutboxEventProcessor processor;
  @Autowired private OutboxProperties outboxProperties;
  @Autowired private FakeInboundWebhookService webhookService;
  @Autowired private InboundAssociationService inboundAssociationService;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private Clock clock;
  @Autowired private MeterRegistry meterRegistry;

  private CrmPrincipal principal;

  @BeforeEach
  void authenticate() {
    principal = (CrmPrincipal) identityService.loadUserByUsername("outbox-admin");
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                principal, principal.password(), principal.getAuthorities()));
  }

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void publishesWithTransactionRollbackAndTenantScopedIdempotency() {
    TransactionTemplate transactions = new TransactionTemplate(transactionManager);
    UUID aggregateId = UUID.randomUUID();
    String key = "integration-publish-" + UUID.randomUUID();
    PublishCommand command =
        command("CAMPAIGN_SIMULATED_V1", aggregateId, key, Map.of("value", 1), 3);

    var first = transactions.execute(status -> publisher.publish(command));
    var repeat = transactions.execute(status -> publisher.publish(command));
    assertThat(repeat.id()).isEqualTo(first.id());
    assertThat(repeat.created()).isFalse();

    assertThatThrownBy(
            () ->
                transactions.execute(
                    status ->
                        publisher.publish(
                            command(
                                "CAMPAIGN_SIMULATED_V1", aggregateId, key, Map.of("value", 2), 3))))
        .isInstanceOf(OptimisticConflictException.class);

    String rollbackKey = "rollback-" + UUID.randomUUID();
    transactions.executeWithoutResult(
        status -> {
          publisher.publish(
              command(
                  "CAMPAIGN_SIMULATED_V1",
                  UUID.randomUUID(),
                  rollbackKey,
                  Map.of("rollback", true),
                  3));
          status.setRollbackOnly();
        });
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_event WHERE organization_id = ? AND idempotency_key = ?",
                Integer.class,
                principal.organizationId(),
                rollbackKey))
        .isZero();
  }

  @Test
  void validatesManualAssociationWithoutAnOptionalContact() {
    var prospect =
        prospectApplicationService.create(
            new CreateProspectCommand(
                "Manual association " + UUID.randomUUID(),
                null,
                null,
                null,
                "Argentina",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "TEST",
                "Synthetic",
                null,
                null,
                null,
                null,
                null,
                null,
                principal.username()));

    var association =
        inboundAssociationService.validatedManual(
            principal.organizationId(), prospect.id(), null, "MANUAL_ASSOCIATION");

    assertThat(association.prospectId()).isEqualTo(prospect.id());
    assertThat(association.contactId()).isNull();
    assertThat(association.method()).isEqualTo("MANUAL_ASSOCIATION");
  }

  @Test
  void twoWorkersClaimEachEventOnlyOnceAndPolicyBlocksMessaging() throws Exception {
    TransactionTemplate transactions = new TransactionTemplate(transactionManager);
    List<UUID> ids = new ArrayList<>();
    transactions.executeWithoutResult(
        status -> {
          for (int index = 0; index < 40; index++) {
            var published =
                publisher.publish(
                    command(
                        "CAMPAIGN_SIMULATED_V1",
                        UUID.randomUUID(),
                        "parallel-" + UUID.randomUUID(),
                        Map.of("prospectId", UUID.randomUUID().toString()),
                        3));
            ids.add(published.id());
          }
        });
    OutboxWorkerService second =
        new OutboxWorkerService(
            jdbcTemplate,
            new TransactionTemplate(transactionManager),
            processor,
            outboxProperties,
            clock,
            meterRegistry);
    CountDownLatch start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var firstRun = executor.submit(() -> runAfter(start, worker));
      var secondRun = executor.submit(() -> runAfter(start, second));
      start.countDown();
      firstRun.get(10, TimeUnit.SECONDS);
      secondRun.get(10, TimeUnit.SECONDS);
    }

    assertThat(ids)
        .hasSize(40)
        .allSatisfy(
            id -> {
              assertThat(status(id)).isEqualTo("BLOCKED");
              assertThat(attempts(id)).isEqualTo(1);
            });
  }

  @Test
  void recoversExpiredLeaseAndDeadLettersRetryableFailureAtMaximum() {
    TransactionTemplate transactions = new TransactionTemplate(transactionManager);
    UUID recoverId =
        transactions.execute(
            status ->
                publisher
                    .publish(
                        command(
                            "CAMPAIGN_SIMULATED_V1",
                            UUID.randomUUID(),
                            "recover-" + UUID.randomUUID(),
                            Map.of("prospectId", UUID.randomUUID().toString()),
                            3))
                    .id());
    jdbcTemplate.update(
        """
        UPDATE outbox_event SET status = 'PROCESSING', attempt_count = 1, locked_at = ?,
          lock_expires_at = ?, locked_by = 'crashed-worker'
        WHERE id = ?
        """,
        Timestamp.from(clock.instant().minusSeconds(120)),
        Timestamp.from(clock.instant().minusSeconds(60)),
        recoverId);
    worker.runOnce();
    assertThat(status(recoverId)).isEqualTo("BLOCKED");
    assertThat(attempts(recoverId)).isEqualTo(2);

    UUID deadId =
        transactions.execute(
            status ->
                publisher
                    .publish(
                        command(
                            "INBOUND_RECEIVED_V1",
                            UUID.randomUUID(),
                            "dead-" + UUID.randomUUID(),
                            Map.of("inboundMessageId", UUID.randomUUID().toString()),
                            2))
                    .id());
    worker.runOnce();
    assertThat(status(deadId)).isEqualTo("RETRY");
    worker.runOnce();
    assertThat(status(deadId)).isEqualTo("DEAD");
    assertThat(attempts(deadId)).isEqualTo(2);
  }

  @Test
  void verifiesReplayAndQuarantinesAnUnassociatedInbound() throws Exception {
    long timestamp = clock.instant().getEpochSecond();
    String nonce = "quarantine-" + UUID.randomUUID();
    String eventId = "event-" + UUID.randomUUID();
    byte[] payload =
        payload(
            eventId,
            "message-" + UUID.randomUUID(),
            "unknown-" + UUID.randomUUID() + "@example.test");

    assertThatThrownBy(
            () ->
                webhookService.receive(
                    principal.organizationId(), timestamp, nonce, "00", payload, "127.0.0.1"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("signature is invalid");

    String signature = sign(principal.organizationId(), timestamp, nonce, payload);
    ReceiptAccepted accepted =
        webhookService.receive(
            principal.organizationId(), timestamp, nonce, signature, payload, "127.0.0.1");
    ReceiptAccepted duplicate =
        webhookService.receive(
            principal.organizationId(), timestamp, nonce, signature, payload, "127.0.0.1");
    assertThat(duplicate.receiptId()).isEqualTo(accepted.receiptId());
    assertThat(duplicate.duplicate()).isTrue();
    worker.runOnce();
    assertThat(inboundStatus(accepted.receiptId())).isEqualTo("QUARANTINED");

    byte[] changed = payload(eventId, "message-" + UUID.randomUUID(), "changed@example.test");
    String changedNonce = "changed-" + UUID.randomUUID();
    assertThatThrownBy(
            () ->
                webhookService.receive(
                    principal.organizationId(),
                    timestamp,
                    changedNonce,
                    sign(principal.organizationId(), timestamp, changedNonce, changed),
                    changed,
                    "127.0.0.1"))
        .isInstanceOf(OptimisticConflictException.class);
  }

  @Test
  void associatesInboundCreatesDomainEffectsAndNeverSends() throws Exception {
    ContactFixture fixture = contact();
    jdbcTemplate.update(
        "UPDATE prospect SET status = 'CONTACTED' WHERE organization_id = ? AND id = ?",
        principal.organizationId(),
        fixture.prospectId());
    long timestamp = clock.instant().getEpochSecond();
    String nonce = "associated-" + UUID.randomUUID();
    byte[] payload =
        payload("event-" + UUID.randomUUID(), "message-" + UUID.randomUUID(), fixture.email());
    ReceiptAccepted accepted =
        webhookService.receive(
            principal.organizationId(),
            timestamp,
            nonce,
            sign(principal.organizationId(), timestamp, nonce, payload),
            payload,
            "127.0.0.1");

    worker.runOnce();

    assertThat(inboundStatus(accepted.receiptId())).isEqualTo("PROCESSED");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT status FROM prospect WHERE organization_id = ? AND id = ?",
                String.class,
                principal.organizationId(),
                fixture.prospectId()))
        .isEqualTo("REPLIED");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM activity WHERE organization_id = ? AND prospect_id = ? AND direction = 'INBOUND'",
                Integer.class,
                principal.organizationId(),
                fixture.prospectId()))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM crm_task WHERE organization_id = ? AND prospect_id = ? AND task_type = 'INBOUND_REPLY'",
                Integer.class,
                principal.organizationId(),
                fixture.prospectId()))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM message_record WHERE organization_id = ? AND status = 'SENT'",
                Integer.class,
                principal.organizationId()))
        .isZero();
  }

  private OutboxWorkerService.RunResult runAfter(CountDownLatch start, OutboxWorkerService target)
      throws InterruptedException {
    start.await();
    return target.runOnce();
  }

  private PublishCommand command(
      String eventType, UUID aggregateId, String key, Map<String, ?> payload, int attempts) {
    return new PublishCommand(
        principal.organizationId(),
        eventType,
        1,
        "TEST_AGGREGATE",
        aggregateId,
        payload,
        key,
        "test-" + UUID.randomUUID(),
        principal.userId(),
        attempts);
  }

  private String status(UUID id) {
    return jdbcTemplate.queryForObject(
        "SELECT status FROM outbox_event WHERE id = ?", String.class, id);
  }

  private int attempts(UUID id) {
    return jdbcTemplate.queryForObject(
        "SELECT attempt_count FROM outbox_event WHERE id = ?", Integer.class, id);
  }

  private String inboundStatus(UUID id) {
    return jdbcTemplate.queryForObject(
        "SELECT status FROM inbound_message WHERE id = ?", String.class, id);
  }

  private byte[] payload(String eventId, String messageId, String sender) {
    return ("{\"externalEventId\":\""
            + eventId
            + "\",\"externalMessageId\":\""
            + messageId
            + "\",\"channel\":\"EMAIL\",\"sender\":\""
            + sender
            + "\",\"receivedAt\":\""
            + clock.instant()
            + "\",\"body\":\"Synthetic inbound response\"}")
        .getBytes(StandardCharsets.UTF_8);
  }

  private String sign(UUID organizationId, long timestamp, String nonce, byte[] body)
      throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    mac.update(
        (timestamp + "." + nonce + "." + organizationId + ".").getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(mac.doFinal(body));
  }

  private ContactFixture contact() {
    String suffix = UUID.randomUUID().toString();
    String email = "inbound-" + suffix + "@example.test";
    UUID prospectId =
        prospectApplicationService
            .create(
                new CreateProspectCommand(
                    "Inbound " + suffix,
                    "Education",
                    "Rosario",
                    "Santa Fe",
                    "Argentina",
                    "https://" + suffix + ".example.test",
                    "Contact",
                    "Director",
                    email,
                    null,
                    null,
                    "INBOUND-" + suffix,
                    "TEST",
                    "Synthetic",
                    100,
                    3,
                    70,
                    null,
                    null,
                    Instant.parse("2026-07-21T12:00:00Z"),
                    principal.username()))
            .id();
    UUID contactId =
        jdbcTemplate.queryForObject(
            """
            SELECT c.id FROM prospect p JOIN contact c
              ON c.organization_id = p.organization_id AND c.institution_id = p.institution_id
            WHERE p.organization_id = ? AND p.id = ?
            """,
            UUID.class,
            principal.organizationId(),
            prospectId);
    return new ContactFixture(prospectId, contactId, email);
  }

  private record ContactFixture(UUID prospectId, UUID contactId, String email) {}
}
