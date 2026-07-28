package com.gestudio.crm.prospect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestudio.crm.activity.ActivityDirection;
import com.gestudio.crm.activity.ActivityType;
import com.gestudio.crm.activity.TaskPriority;
import com.gestudio.crm.activity.TaskStatus;
import com.gestudio.crm.activity.TimelineService;
import com.gestudio.crm.activity.TimelineService.CreateActivityCommand;
import com.gestudio.crm.activity.TimelineService.CreateTaskCommand;
import com.gestudio.crm.common.OptimisticConflictException;
import com.gestudio.crm.contact.ContactChannelType;
import com.gestudio.crm.contact.ContactOperationsService;
import com.gestudio.crm.contact.ContactOperationsService.ChannelCommand;
import com.gestudio.crm.contact.ContactOperationsService.CreateContactCommand;
import com.gestudio.crm.contact.ContactOperationsService.UpdateContactCommand;
import com.gestudio.crm.exclusion.ExclusionApplicationService;
import com.gestudio.crm.exclusion.ExclusionReason;
import com.gestudio.crm.identity.CrmPrincipal;
import com.gestudio.crm.identity.IdentityService;
import com.gestudio.crm.prospect.ProspectApplicationService.CreateProspectCommand;
import com.gestudio.crm.prospect.ProspectOperationsService.SearchFilter;
import com.gestudio.crm.prospect.ProspectOperationsService.TransitionCommand;
import com.gestudio.crm.prospect.ProspectOperationsService.UpdateProspectCommand;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
      "security.bootstrap.username=operations-admin",
      "security.bootstrap.password=operations-password-1"
    })
@Testcontainers
class OperationalProspectIntegrationTest {

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
  @Autowired private ProspectOperationsService prospectOperationsService;
  @Autowired private ContactOperationsService contactOperationsService;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private ExclusionApplicationService exclusionApplicationService;
  @Autowired private TimelineService timelineService;

  private CrmPrincipal principal;

  @BeforeEach
  void authenticateBootstrapAdmin() {
    principal = (CrmPrincipal) identityService.loadUserByUsername("operations-admin");
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
  void completesProspectContactTaskActivityLifecycleAndTimeline() {
    String suffix = UUID.randomUUID().toString();
    UUID prospectId = prospectApplicationService.create(createCommand(suffix)).id();
    var initial = prospectOperationsService.get(prospectId);

    var updated =
        prospectOperationsService.update(
            prospectId,
            new UpdateProspectCommand(
                initial.version(),
                "=Academia Operativa " + suffix,
                "Academia Operativa SA",
                4,
                82,
                300,
                "MANUAL",
                "Synthetic integration test",
                principal.userId(),
                "https://operations-" + suffix + ".test",
                "Calle de prueba 123",
                "Rosario",
                "Santa Fe",
                "Argentina",
                "America/Argentina/Buenos_Aires",
                "Seguimiento prioritario",
                Instant.parse("2026-08-01T14:00:00Z")));
    assertThat(updated.displayName()).startsWith("=Academia Operativa");
    assertThat(updated.ownerUserId()).isEqualTo(principal.userId());

    var contact =
        contactOperationsService.create(
            prospectId,
            new CreateContactCommand(
                "Ana",
                "Pérez",
                "Directora",
                true,
                true,
                ContactChannelType.EMAIL,
                "GRANTED",
                "MANUAL",
                Instant.parse("2026-07-21T12:00:00Z"),
                List.of(
                    new ChannelCommand(
                        ContactChannelType.EMAIL,
                        "Ana." + suffix + "@Example.TEST",
                        true,
                        true,
                        true,
                        "GRANTED",
                        true,
                        Instant.parse("2026-07-21T12:00:00Z")))));
    assertThat(contact.displayName()).isEqualTo("Ana Pérez");
    assertThat(contact.channels())
        .singleElement()
        .satisfies(channel -> assertThat(channel.normalizedValue()).endsWith("@example.test"));

    var note = timelineService.createNote(prospectId, "Próximo paso <script>alert(1)</script>");
    assertThat(note.body()).doesNotContain("<script>").contains("&lt;script&gt;");

    var task =
        timelineService.createTask(
            prospectId,
            new CreateTaskCommand(
                principal.userId(),
                "Llamar a la institución",
                "Confirmar disponibilidad",
                Instant.parse("2026-08-01T14:00:00Z"),
                TaskPriority.HIGH,
                "FOLLOW_UP",
                Instant.parse("2026-08-01T13:00:00Z")));
    assertThat(task.status()).isEqualTo(TaskStatus.OPEN);

    var qualifying = transition(prospectId, ProspectStatus.QUALIFYING);
    var ready = transition(prospectId, ProspectStatus.READY_TO_CONTACT);
    assertThat(qualifying.status()).isEqualTo(ProspectStatus.QUALIFYING);
    assertThat(ready.status()).isEqualTo(ProspectStatus.READY_TO_CONTACT);

    timelineService.createActivity(
        prospectId,
        new CreateActivityCommand(
            contact.id(),
            ActivityType.PHONE_CALL,
            Instant.parse("2026-07-21T15:00:00Z"),
            "PHONE",
            ActivityDirection.OUTBOUND,
            "CONNECTED",
            "Llamada manual registrada",
            "Conversación sintética",
            "test-call-" + suffix,
            Map.of("synthetic", true)));
    var contacted = transition(prospectId, ProspectStatus.CONTACTED);
    assertThat(contacted.status()).isEqualTo(ProspectStatus.CONTACTED);
    assertThat(contacted.lastContactAt()).isEqualTo(Instant.parse("2026-07-21T15:00:00Z"));

    var completed =
        timelineService.changeTaskStatus(task.id(), task.version(), TaskStatus.COMPLETED, "DONE");
    assertThat(completed.completedAt()).isNotNull();
    assertThat(prospectOperationsService.get(prospectId).nextActionAt()).isNull();

    var timeline = timelineService.timeline(prospectId, 0, 100);
    assertThat(timeline.content())
        .extracting(TimelineService.TimelineItem::eventType)
        .contains("NOTE", "ACTIVITY", "TASK", "STATUS");
    assertThat(timeline.content())
        .extracting(TimelineService.TimelineItem::title)
        .contains("PHONE_CALL", "READY_TO_CONTACT → CONTACTED");

    String csv =
        prospectOperationsService.exportCsv(
            new SearchFilter("Academia Operativa", null, null, false, 0, 50, "displayName", "asc"));
    assertThat(csv).contains("\"'=Academia Operativa");

    assertThatThrownBy(
            () ->
                prospectOperationsService.update(
                    prospectId,
                    new UpdateProspectCommand(
                        initial.version(),
                        "Stale update",
                        null,
                        1,
                        1,
                        1,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)))
        .isInstanceOf(OptimisticConflictException.class);

    var renamedContact =
        contactOperationsService.update(
            contact.id(),
            new UpdateContactCommand(
                contact.version(),
                "Ana",
                "Pérez",
                "Responsable comercial",
                true,
                true,
                ContactChannelType.EMAIL,
                "GRANTED",
                "MANUAL",
                Instant.parse("2026-07-21T16:00:00Z")));
    assertThat(renamedContact.role()).isEqualTo("Responsable comercial");

    var removableContact =
        contactOperationsService.create(
            prospectId,
            new CreateContactCommand(
                "Contacto",
                "Temporal",
                null,
                false,
                false,
                null,
                "UNKNOWN",
                "MANUAL",
                null,
                List.of()));
    contactOperationsService.delete(removableContact.id(), removableContact.version());
    assertThat(contactOperationsService.listForProspect(prospectId))
        .extracting(ContactOperationsService.ContactView::id)
        .doesNotContain(removableContact.id());

    var archived =
        prospectOperationsService.archive(
            prospectId, prospectOperationsService.get(prospectId).version());
    assertThat(archived.archivedAt()).isNotNull();
    assertThat(
            prospectOperationsService
                .search(new SearchFilter(null, null, null, true, 0, 50, "createdAt", "desc"))
                .content())
        .extracting(ProspectOperationsService.OperationalProspectView::id)
        .contains(prospectId);
    var restored = prospectOperationsService.restore(prospectId, archived.version());
    assertThat(restored.archivedAt()).isNull();
    assertThat(restored.status()).isEqualTo(ProspectStatus.CONTACTED);
  }

  @Test
  void calculatesTenantWideDashboardMetricsBeyondTheFirstPage() {
    String suffix = "dashboard-metrics-" + UUID.randomUUID();
    var before = prospectOperationsService.dashboardMetrics();
    List<UUID> prospectIds = new ArrayList<>();

    for (int index = 0; index < 105; index++) {
      prospectIds.add(
          prospectApplicationService
              .create(createCommandWithoutChannels(suffix + "-" + index))
              .id());
    }

    List<ProspectStatus> interestedStatuses =
        List.of(
            ProspectStatus.INTERESTED,
            ProspectStatus.QUALIFIED,
            ProspectStatus.TRIAL_ACTIVE,
            ProspectStatus.QUOTED,
            ProspectStatus.NEGOTIATION);
    for (int index = 0; index < interestedStatuses.size(); index++) {
      jdbcTemplate.update(
          """
          UPDATE prospect
          SET status = ?, updated_at = now()
          WHERE id = ? AND organization_id = ?
          """,
          interestedStatuses.get(index).name(),
          prospectIds.get(index),
          principal.organizationId());
    }
    for (int index = 0; index < 7; index++) {
      jdbcTemplate.update(
          """
          UPDATE prospect
          SET contact_eligible = TRUE, eligibility = 'ELIGIBLE', updated_at = now()
          WHERE id = ? AND organization_id = ?
          """,
          prospectIds.get(index),
          principal.organizationId());
    }

    var firstPage =
        prospectOperationsService.search(
            new SearchFilter(suffix, null, null, false, 0, 100, "createdAt", "desc"));
    assertThat(firstPage.totalElements()).isEqualTo(105);
    assertThat(firstPage.content()).hasSize(100);

    var after = prospectOperationsService.dashboardMetrics();
    assertThat(after.interested() - before.interested()).isEqualTo(5);
    assertThat(after.blocked() - before.blocked()).isEqualTo(98);
  }

  @Test
  void synchronizesContactabilityWhenUsableChannelsAreAddedAndRemoved() {
    String suffix = UUID.randomUUID().toString();
    UUID prospectId = prospectApplicationService.create(createCommandWithoutChannels(suffix)).id();

    var missing = prospectOperationsService.get(prospectId);
    assertThat(missing.contactEligible()).isFalse();
    assertThat(missing.eligibility()).isEqualTo(ProspectEligibility.INVALID);
    assertThat(missing.status()).isEqualTo(ProspectStatus.NEEDS_ENRICHMENT);

    var contact =
        contactOperationsService.create(
            prospectId,
            new CreateContactCommand(
                "Canal",
                "Disponible",
                "Administración",
                true,
                false,
                ContactChannelType.EMAIL,
                "UNKNOWN",
                "MANUAL",
                null,
                List.of(
                    new ChannelCommand(
                        ContactChannelType.EMAIL,
                        "contactable-" + suffix + "@example.test",
                        true,
                        true,
                        false,
                        "UNKNOWN",
                        true,
                        null))));

    var contactable = prospectOperationsService.get(prospectId);
    assertThat(contactable.contactEligible()).isTrue();
    assertThat(contactable.eligibility()).isEqualTo(ProspectEligibility.ELIGIBLE);
    assertThat(contactable.status()).isEqualTo(ProspectStatus.NEW);

    var channel = contact.channels().getFirst();
    contactOperationsService.deleteChannel(channel.id(), channel.version());

    var missingAgain = prospectOperationsService.get(prospectId);
    assertThat(missingAgain.contactEligible()).isFalse();
    assertThat(missingAgain.eligibility()).isEqualTo(ProspectEligibility.INVALID);
    assertThat(missingAgain.status()).isEqualTo(ProspectStatus.NEEDS_ENRICHMENT);
  }

  @Test
  void keepsExclusionsDominantWhenAChannelIsAdded() {
    String suffix = UUID.randomUUID().toString();
    String email = "blocked-" + suffix + "@example.test";
    UUID prospectId = prospectApplicationService.create(createCommandWithoutChannels(suffix)).id();
    exclusionApplicationService.create(ContactChannelType.EMAIL, email, ExclusionReason.MANUAL);

    contactOperationsService.create(
        prospectId,
        new CreateContactCommand(
            "Canal",
            "Excluido",
            "Administración",
            true,
            false,
            ContactChannelType.EMAIL,
            "UNKNOWN",
            "MANUAL",
            null,
            List.of(
                new ChannelCommand(
                    ContactChannelType.EMAIL, email, true, true, false, "UNKNOWN", true, null))));

    var blocked = prospectOperationsService.get(prospectId);
    assertThat(blocked.contactEligible()).isFalse();
    assertThat(blocked.eligibility()).isEqualTo(ProspectEligibility.EXCLUDED);
    assertThat(blocked.status()).isEqualTo(ProspectStatus.DO_NOT_CONTACT);
  }

  private ProspectOperationsService.OperationalProspectView transition(
      UUID prospectId, ProspectStatus status) {
    long version = prospectOperationsService.get(prospectId).version();
    return prospectOperationsService.transition(
        prospectId, new TransitionCommand(version, status, null, "Integration test", null, false));
  }

  private CreateProspectCommand createCommandWithoutChannels(String suffix) {
    return new CreateProspectCommand(
        "Academia sin canal " + suffix,
        "Danza",
        "Rosario",
        "Santa Fe",
        "Argentina",
        null,
        null,
        null,
        null,
        null,
        null,
        "NO-CHANNEL-" + suffix,
        "fixture",
        "Synthetic no-channel evidence",
        50,
        2,
        40,
        "Planillas",
        "Sin canal publicado",
        Instant.parse("2026-07-21T12:00:00Z"),
        "operations-admin");
  }

  private CreateProspectCommand createCommand(String suffix) {
    return new CreateProspectCommand(
        "Academia Inicial " + suffix,
        "Danza",
        "Rosario",
        "Santa Fe",
        "Argentina",
        "https://initial-" + suffix + ".test",
        null,
        null,
        null,
        null,
        null,
        "OPS-" + suffix,
        "fixture",
        "Synthetic evidence",
        100,
        2,
        60,
        "Planillas",
        "Seguimiento manual",
        Instant.parse("2026-07-21T12:00:00Z"),
        "operations-admin");
  }
}
