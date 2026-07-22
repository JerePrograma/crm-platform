package com.gestudio.crm.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestudio.crm.common.OptimisticConflictException;
import com.gestudio.crm.identity.CrmPrincipal;
import com.gestudio.crm.identity.IdentityService;
import com.gestudio.crm.messaging.MessageDispatcher.CreateMessageCommand;
import com.gestudio.crm.outbox.OutboxWorkerService;
import com.gestudio.crm.prospect.ProspectApplicationService;
import com.gestudio.crm.prospect.ProspectApplicationService.CreateProspectCommand;
import java.time.Instant;
import java.util.UUID;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
      "security.bootstrap.username=message-admin",
      "security.bootstrap.password=message-password-1"
    })
@Testcontainers
class MessagingIntegrationTest {

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
  @Autowired private MessageDispatcherService dispatcher;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private OutboxWorkerService outboxWorkerService;

  private CrmPrincipal principal;

  @BeforeEach
  void authenticate() {
    principal = (CrmPrincipal) identityService.loadUserByUsername("message-admin");
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
  void createsIdempotentLocalDraftAndFakeSimulationWithoutSending() {
    TestContact fixture = contact("safe");
    CreateMessageCommand draftCommand =
        command(fixture, "Safe subject", "Local draft", "draft-" + UUID.randomUUID());

    var draft = dispatcher.createDraft(draftCommand);
    var retry = dispatcher.createDraft(draftCommand);
    assertThat(retry.id()).isEqualTo(draft.id());
    assertThat(draft.status()).isEqualTo("DRAFT_CREATED");
    assertThat(draft.provider()).isEqualTo("NOOP");
    assertThat(draft.sendingBlockReason()).isEqualTo("BLOCKED_BY_KILL_SWITCH");

    var simulation =
        dispatcher.simulate(
            command(
                fixture,
                "Simulation subject",
                "Fake provider only",
                "simulation-" + UUID.randomUUID()));
    assertThat(simulation.status()).isEqualTo("SIMULATED");
    assertThat(simulation.provider()).isEqualTo("FAKE");
    assertThat(simulation.externalMessageId()).startsWith("fake-email-");
    assertThat(simulation.sendingBlockReason()).isEqualTo("BLOCKED_BY_KILL_SWITCH");

    var manual =
        dispatcher.manualLink(
            command(fixture, "Manual subject", "Manual body", "manual-" + UUID.randomUUID()));
    assertThat(manual.url()).startsWith("mailto:").contains("subject=Manual+subject");
    assertThat(manual.sendingBlockReason()).isEqualTo("BLOCKED_BY_KILL_SWITCH");

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM message_record WHERE organization_id = ? AND status = 'SENT'",
                Integer.class,
                principal.organizationId()))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM activity WHERE prospect_id = ? AND outcome = 'SIMULATED'",
                Integer.class,
                fixture.prospectId()))
        .isEqualTo(1);
  }

  @Test
  void blocksExcludedRecipientsBeforePersistingMessageBody() {
    TestContact fixture = contact("excluded");
    String normalized =
        jdbcTemplate.queryForObject(
            "SELECT normalized_value FROM contact_channel WHERE id = ?",
            String.class,
            fixture.channelId());
    jdbcTemplate.update(
        "INSERT INTO exclusion (id, version, organization_id, channel_type, normalized_value, reason, created_at, updated_at) VALUES (?, 0, ?, 'EMAIL', ?, 'MANUAL', now(), now())",
        UUID.randomUUID(),
        principal.organizationId(),
        normalized);

    assertThatThrownBy(
            () ->
                dispatcher.createDraft(
                    command(
                        fixture,
                        "Blocked subject",
                        "Must not persist",
                        "blocked-" + UUID.randomUUID())))
        .isInstanceOf(OptimisticConflictException.class)
        .hasMessage("BLOCKED_BY_EXCLUSION");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM message_record WHERE prospect_id = ?",
                Integer.class,
                fixture.prospectId()))
        .isZero();
  }

  @Test
  void reportsNoSendEndpointAndSafeDefaultProviders() {
    var safety = dispatcher.safety();
    assertThat(safety.emailMode()).isEqualTo("NOOP");
    assertThat(safety.whatsAppMode()).isEqualTo("DEEPLINK_ONLY");
    assertThat(safety.realNetworkAllowed()).isFalse();
    assertThat(safety.selectedEmailProvider()).isEqualTo("NOOP");
    assertThat(safety.selectedWhatsAppProvider()).isEqualTo("NOOP");
    assertThat(safety.sendEndpointAvailable()).isFalse();
  }

  @Test
  void rejectsIdempotencyCollisionAndRechecksExclusionAfterEnqueue() {
    TestContact fixture = contact("policy-recheck");
    String key = "policy-recheck-" + UUID.randomUUID();
    CreateMessageCommand original = command(fixture, "Original", "Original body", key);
    var message = dispatcher.simulate(original);

    assertThatThrownBy(() -> dispatcher.simulate(command(fixture, "Changed", "Changed body", key)))
        .isInstanceOf(OptimisticConflictException.class)
        .hasMessageContaining("different message request");

    String normalized =
        jdbcTemplate.queryForObject(
            "SELECT normalized_value FROM contact_channel WHERE id = ?",
            String.class,
            fixture.channelId());
    jdbcTemplate.update(
        """
        INSERT INTO exclusion (
          id, version, organization_id, channel_type, normalized_value, reason,
          created_at, updated_at
        ) VALUES (?, 0, ?, 'EMAIL', ?, 'MANUAL', now(), now())
        """,
        UUID.randomUUID(),
        principal.organizationId(),
        normalized);

    outboxWorkerService.runOnce();

    assertThat(
            jdbcTemplate.queryForMap(
                """
                SELECT status, last_error_code FROM outbox_event
                WHERE organization_id = ? AND aggregate_type = 'MESSAGE' AND aggregate_id = ?
                """,
                principal.organizationId(),
                message.id()))
        .containsEntry("status", "BLOCKED")
        .containsEntry("last_error_code", "BLOCKED_BY_EXCLUSION");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM message_record WHERE organization_id = ? AND status = 'SENT'",
                Integer.class,
                principal.organizationId()))
        .isZero();
  }

  private TestContact contact(String prefix) {
    String suffix = UUID.randomUUID().toString();
    UUID prospectId =
        prospectApplicationService
            .create(
                new CreateProspectCommand(
                    "Message " + prefix + " " + suffix,
                    "Education",
                    "Rosario",
                    "Santa Fe",
                    "Argentina",
                    "https://" + suffix + ".example.test",
                    "Contact",
                    "Director",
                    prefix + "-" + suffix + "@example.test",
                    null,
                    null,
                    "MESSAGE-" + suffix,
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
    return jdbcTemplate.queryForObject(
        """
        SELECT c.id AS contact_id, cc.id AS channel_id
        FROM prospect p
        JOIN contact c ON c.organization_id = p.organization_id AND c.institution_id = p.institution_id
        JOIN contact_channel cc ON cc.organization_id = c.organization_id AND cc.contact_id = c.id
        WHERE p.organization_id = ? AND p.id = ? AND cc.type = 'EMAIL'
        """,
        (rs, rowNum) ->
            new TestContact(
                prospectId,
                rs.getObject("contact_id", UUID.class),
                rs.getObject("channel_id", UUID.class)),
        principal.organizationId(),
        prospectId);
  }

  private CreateMessageCommand command(
      TestContact fixture, String subject, String body, String idempotencyKey) {
    return new CreateMessageCommand(
        fixture.prospectId(),
        fixture.contactId(),
        "EMAIL",
        subject,
        body,
        "<p>" + body + "</p>",
        idempotencyKey);
  }

  private record TestContact(UUID prospectId, UUID contactId, UUID channelId) {}
}
