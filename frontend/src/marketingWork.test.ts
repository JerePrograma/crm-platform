import { describe, expect, it } from "vitest";
import { localDate, pendingTasks, taskTiming } from "./marketingWork";
import type { Task } from "./types";

const task = (id: string, dueAt: string, status: Task["status"] = "OPEN", ownerUserId = "marketing") =>
  ({ id, dueAt, status, ownerUserId } as Task);

describe("agenda de Marketing", () => {
  it("usa el día de Buenos Aires cuando UTC ya pasó a mañana", () => {
    const now = new Date("2026-10-15T01:30:00Z");
    expect(localDate(now)).toBe("2026-10-14");
    expect(taskTiming(task("today", "2026-10-15T02:00:00Z"), now)).toBe("today");
    expect(taskTiming(task("overdue", "2026-10-14T02:00:00Z"), now)).toBe("overdue");
    expect(taskTiming(task("upcoming", "2026-10-15T12:00:00Z"), now)).toBe("upcoming");
  });

  it("ordena pendientes y aplica responsable sin volver a mostrar tareas cerradas", () => {
    const tasks = [
      task("later", "2026-10-16T12:00:00Z"),
      task("completed", "2026-10-12T12:00:00Z", "COMPLETED"),
      task("other", "2026-10-13T12:00:00Z", "IN_PROGRESS", "other-user"),
      task("cancelled", "2026-10-11T12:00:00Z", "CANCELLED"),
      task("first", "2026-10-14T12:00:00Z"),
    ];
    expect(pendingTasks(tasks).map((item) => item.id)).toEqual(["other", "first", "later"]);
    expect(pendingTasks(tasks, "marketing").map((item) => item.id)).toEqual(["first", "later"]);
  });
});
