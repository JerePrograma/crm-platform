package com.gestudio.crm.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.identity.CrmPrincipal;
import com.gestudio.crm.identity.IdentityService;
import com.gestudio.crm.prospect.ProspectApplicationService;
import com.gestudio.crm.prospect.ProspectApplicationService.CreateProspectCommand;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
      "security.bootstrap.username=task-pages-admin",
      "security.bootstrap.password=synthetic-task-pages-1"
    })
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class TimelineTaskPaginationIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private TimelineService timeline;
  @Autowired private IdentityService identity;
  @Autowired private ProspectApplicationService prospects;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private MockMvc mockMvc;
  @Autowired private EntityManager entityManager;

  private CrmPrincipal principal;

  @BeforeEach
  void authenticate() {
    principal = (CrmPrincipal) identity.loadUserByUsername("task-pages-admin");
    authenticate(principal);
  }

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void filtersPendingOwnersAndDatesBeforePaginationBeyondTwoHundredClosedTasks() {
    UUID prospect = prospect();
    seedTasks(
        principal.organizationId(),
        prospect,
        principal.userId(),
        "COMPLETED",
        "2026-09-01T00:00:00Z",
        240);
    seedTasks(
        principal.organizationId(),
        prospect,
        principal.userId(),
        "OPEN",
        "2026-10-22T03:00:00Z",
        205);
    seedTasks(
        principal.organizationId(),
        prospect,
        principal.userId(),
        "IN_PROGRESS",
        "2026-10-22T02:59:59Z",
        1);
    seedTasks(
        principal.organizationId(),
        prospect,
        principal.userId(),
        "IN_PROGRESS",
        "2026-10-23T03:00:00Z",
        1);
    UUID otherOwner =
        identity
            .createUser(
                principal.organizationId(),
                "task-pages-other",
                "Other owner",
                "synthetic-other-owner-1",
                "SALES",
                principal.userId())
            .id();
    seedTasks(principal.organizationId(), prospect, otherOwner, "OPEN", "2026-10-22T03:00:00Z", 1);
    UUID foreignOrganization = foreignOrganization();
    UUID foreignProspect = foreignProspect(foreignOrganization);
    seedTasks(
        foreignOrganization,
        foreignProspect,
        principal.userId(),
        "OPEN",
        "2026-01-01T00:00:00Z",
        1);

    // The legacy array remains compatible and demonstrates the original truncation condition.
    assertThat(timeline.listTasks(null, null, null, 200))
        .hasSize(200)
        .allMatch(task -> task.status() == TaskStatus.COMPLETED);
    List<TimelineService.TaskView> allPages = new ArrayList<>();
    for (int number = 0; number < 3; number++) {
      var page =
          timeline.searchTasks(
              List.of(TaskStatus.OPEN, TaskStatus.IN_PROGRESS),
              principal.userId(),
              null,
              null,
              null,
              number,
              100);
      assertThat(page.totalElements()).isEqualTo(207);
      assertThat(page.totalPages()).isEqualTo(3);
      assertThat(page.number()).isEqualTo(number);
      assertThat(page.first()).isEqualTo(number == 0);
      assertThat(page.last()).isEqualTo(number == 2);
      allPages.addAll(page.content());
    }
    assertThat(allPages).hasSize(207).allMatch(task -> task.prospectId().equals(prospect));
    assertThat(allPages).extracting(TimelineService.TaskView::id).doesNotHaveDuplicates();
    assertThat(allPages).extracting(TimelineService.TaskView::dueAt).isSorted();
    var today =
        timeline.searchTasks(
            List.of(TaskStatus.OPEN, TaskStatus.IN_PROGRESS),
            principal.userId(),
            null,
            Instant.parse("2026-10-22T03:00:00Z"),
            Instant.parse("2026-10-23T03:00:00Z"),
            0,
            25);
    assertThat(today.totalElements()).isEqualTo(205);
    assertThat(today.content()).hasSize(25);
    assertThat(
            timeline
                .searchTasks(
                    List.of(TaskStatus.OPEN, TaskStatus.IN_PROGRESS),
                    principal.userId(),
                    null,
                    null,
                    Instant.parse("2026-10-22T03:00:00Z"),
                    0,
                    25)
                .totalElements())
        .isEqualTo(1);
    assertThat(
            timeline
                .searchTasks(
                    List.of(TaskStatus.OPEN, TaskStatus.IN_PROGRESS),
                    principal.userId(),
                    null,
                    Instant.parse("2026-10-23T03:00:00Z"),
                    null,
                    0,
                    25)
                .totalElements())
        .isEqualTo(1);
    assertThat(
            timeline
                .searchTasks(
                    List.of(TaskStatus.OPEN, TaskStatus.IN_PROGRESS), null, null, null, null, 0, 25)
                .totalElements())
        .isEqualTo(208);
  }

  @Test
  void pagesTheSelectedProspectBeforeTheGlobalLimitAndPreservesAuthorization() throws Exception {
    UUID earlierProspect = prospect();
    seedTasks(
        principal.organizationId(),
        earlierProspect,
        principal.userId(),
        "COMPLETED",
        "2026-09-01T00:00:00Z",
        240);
    UUID selected = prospect();
    seedTasks(
        principal.organizationId(),
        selected,
        principal.userId(),
        "OPEN",
        "2026-10-22T03:00:00Z",
        230);
    var selectedPage = timeline.searchTasks(null, null, selected, null, null, 9, 25);
    assertThat(selectedPage.totalElements()).isEqualTo(230);
    assertThat(selectedPage.totalPages()).isEqualTo(10);
    assertThat(selectedPage.content())
        .hasSize(5)
        .allMatch(task -> task.prospectId().equals(selected));
    assertThat(selectedPage.last()).isTrue();
    mockMvc
        .perform(
            get("/api/v1/tasks/page")
                .with(user(principal))
                .param("prospectId", selected.toString())
                .param("statuses", "OPEN", "IN_PROGRESS")
                .param("page", "9")
                .param("size", "25"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(230))
        .andExpect(jsonPath("$.content.length()").value(5))
        .andExpect(jsonPath("$.content[0].prospectId").value(selected.toString()))
        .andExpect(jsonPath("$.last").value(true));
    mockMvc
        .perform(get("/api/v1/tasks").with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(200));
    mockMvc
        .perform(get("/api/v1/tasks/page").with(anonymous()))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/v1/tasks/page").with(user("without-read").authorities(List.of())))
        .andExpect(status().isForbidden());

    authenticate(principal);
    UUID unavailable = foreignProspect(foreignOrganization());
    assertThatThrownBy(() -> timeline.searchTasks(null, null, unavailable, null, null, 0, 25))
        .isInstanceOf(ResourceNotFoundException.class);
    mockMvc
        .perform(
            get("/api/v1/tasks/page")
                .with(user(principal))
                .param("prospectId", unavailable.toString()))
        .andExpect(status().isNotFound());
    var capped = timeline.searchTasks(null, null, selected, null, null, -1, 500);
    assertThat(capped.size()).isEqualTo(100);
    assertThat(capped.number()).isZero();
    assertThat(capped.content()).hasSize(100);
    assertThat(
            timeline.searchTasks(null, null, selected, null, null, Integer.MAX_VALUE, 25).number())
        .isEqualTo(9);
    mockMvc
        .perform(
            get("/api/v1/tasks/page")
                .with(user(principal))
                .param("dueFrom", "2026-10-23T03:00:00Z")
                .param("dueUntil", "2026-10-22T03:00:00Z"))
        .andExpect(status().isBadRequest());
  }

  private void authenticate(CrmPrincipal actor) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                actor, actor.password(), actor.getAuthorities()));
  }

  private UUID prospect() {
    String suffix = UUID.randomUUID().toString();
    UUID id =
        prospects
            .create(
                new CreateProspectCommand(
                    "Synthetic task prospect " + suffix,
                    "TEST",
                    "Synthetic",
                    "QA",
                    "Argentina",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "TASK-" + suffix,
                    "TEST",
                    "Synthetic task pagination",
                    null,
                    2,
                    50,
                    null,
                    null,
                    null,
                    null))
            .id();
    entityManager.flush();
    return id;
  }

  private UUID foreignOrganization() {
    UUID organization = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO organization (id, created_at, updated_at, slug, name) VALUES (?, now(), now(), ?, 'Synthetic other organization')",
        organization,
        "task-pages-" + organization);
    return organization;
  }

  private UUID foreignProspect(UUID organization) {
    authenticate(
        new CrmPrincipal(
            principal.userId(),
            organization,
            principal.username(),
            principal.displayName(),
            principal.password(),
            principal.role(),
            principal.permissions(),
            true,
            null));
    try {
      return prospect();
    } finally {
      authenticate(principal);
    }
  }

  private void seedTasks(
      UUID organization, UUID prospect, UUID owner, String status, String dueAt, int count) {
    jdbc.update(
        """
        INSERT INTO crm_task (id, organization_id, prospect_id, owner_user_id, creator_user_id,
          title, due_at, priority, status, task_type, completed_at, created_at, updated_at)
        SELECT gen_random_uuid(), ?, ?, ?, ?, 'Synthetic task ' || i,
          ?::timestamptz + i * INTERVAL '1 minute', 'MEDIUM', ?, 'FOLLOW_UP',
          CASE WHEN ? = 'COMPLETED' THEN now() ELSE NULL END, now(), now()
        FROM generate_series(0, ? - 1) AS series(i)
        """,
        organization,
        prospect,
        owner,
        owner,
        dueAt,
        status,
        status,
        count);
  }
}
