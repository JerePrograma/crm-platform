package com.gestudio.crm.activity;

import com.gestudio.crm.activity.TimelineService.ActivityView;
import com.gestudio.crm.activity.TimelineService.CreateActivityCommand;
import com.gestudio.crm.activity.TimelineService.CreateTaskCommand;
import com.gestudio.crm.activity.TimelineService.NoteView;
import com.gestudio.crm.activity.TimelineService.TaskView;
import com.gestudio.crm.activity.TimelineService.TimelinePage;
import com.gestudio.crm.activity.TimelineService.UpdateTaskCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TimelineController {

  private final TimelineService timelineService;

  public TimelineController(TimelineService timelineService) {
    this.timelineService = timelineService;
  }

  @GetMapping("/prospects/{prospectId}/timeline")
  @PreAuthorize("hasAuthority('PROSPECT_READ')")
  public TimelinePage timeline(
      @PathVariable UUID prospectId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return timelineService.timeline(prospectId, page, size);
  }

  @PostMapping("/prospects/{prospectId}/notes")
  @PreAuthorize("hasAuthority('ACTIVITY_WRITE')")
  public NoteView createNote(
      @PathVariable UUID prospectId, @Valid @RequestBody NoteRequest request) {
    return timelineService.createNote(prospectId, request.body());
  }

  @PutMapping("/notes/{noteId}")
  @PreAuthorize("hasAuthority('ACTIVITY_WRITE')")
  public NoteView updateNote(@PathVariable UUID noteId, @Valid @RequestBody NoteRequest request) {
    return timelineService.updateNote(noteId, request.body());
  }

  @DeleteMapping("/notes/{noteId}")
  @PreAuthorize("hasAuthority('ACTIVITY_WRITE')")
  public void deleteNote(@PathVariable UUID noteId) {
    timelineService.deleteNote(noteId);
  }

  @PostMapping("/prospects/{prospectId}/activities")
  @PreAuthorize("hasAuthority('ACTIVITY_WRITE')")
  public ActivityView createActivity(
      @PathVariable UUID prospectId, @Valid @RequestBody ActivityRequest request) {
    return timelineService.createActivity(prospectId, request.command());
  }

  @PostMapping("/prospects/{prospectId}/tasks")
  @PreAuthorize("hasAuthority('ACTIVITY_WRITE')")
  public TaskView createTask(
      @PathVariable UUID prospectId, @Valid @RequestBody CreateTaskRequest request) {
    return timelineService.createTask(prospectId, request.command());
  }

  @PutMapping("/tasks/{taskId}")
  @PreAuthorize("hasAuthority('ACTIVITY_WRITE')")
  public TaskView updateTask(
      @PathVariable UUID taskId, @Valid @RequestBody UpdateTaskRequest request) {
    return timelineService.updateTask(taskId, request.command());
  }

  @PostMapping("/tasks/{taskId}/status")
  @PreAuthorize("hasAuthority('ACTIVITY_WRITE')")
  public TaskView changeTaskStatus(
      @PathVariable UUID taskId, @Valid @RequestBody TaskStatusRequest request) {
    return timelineService.changeTaskStatus(
        taskId, request.version(), request.status(), request.outcome());
  }

  @GetMapping("/tasks")
  @PreAuthorize("hasAuthority('PROSPECT_READ')")
  public List<TaskView> listTasks(
      @RequestParam(required = false) TaskStatus status,
      @RequestParam(required = false) UUID ownerUserId,
      @RequestParam(required = false) Instant dueBefore,
      @RequestParam(defaultValue = "200") int limit) {
    return timelineService.listTasks(status, ownerUserId, dueBefore, limit);
  }

  public record NoteRequest(@NotBlank String body) {}

  public record ActivityRequest(
      UUID contactId,
      @NotNull ActivityType type,
      Instant occurredAt,
      String channel,
      @NotNull ActivityDirection direction,
      String outcome,
      @NotBlank String summary,
      String detail,
      String externalReference,
      Map<String, Object> metadata) {
    CreateActivityCommand command() {
      return new CreateActivityCommand(
          contactId,
          type,
          occurredAt,
          channel,
          direction,
          outcome,
          summary,
          detail,
          externalReference,
          metadata);
    }
  }

  public record CreateTaskRequest(
      UUID ownerUserId,
      @NotBlank String title,
      String description,
      @NotNull Instant dueAt,
      @NotNull TaskPriority priority,
      @NotBlank String taskType,
      Instant reminderAt) {
    CreateTaskCommand command() {
      return new CreateTaskCommand(
          ownerUserId, title, description, dueAt, priority, taskType, reminderAt);
    }
  }

  public record UpdateTaskRequest(
      @PositiveOrZero long version,
      @NotNull UUID ownerUserId,
      @NotBlank String title,
      String description,
      @NotNull Instant dueAt,
      @NotNull TaskPriority priority,
      @NotBlank String taskType,
      Instant reminderAt) {
    UpdateTaskCommand command() {
      return new UpdateTaskCommand(
          version, ownerUserId, title, description, dueAt, priority, taskType, reminderAt);
    }
  }

  public record TaskStatusRequest(
      @PositiveOrZero long version, @NotNull TaskStatus status, String outcome) {}
}
