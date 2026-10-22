import { describe, expect, it } from "vitest";
import { agendaTaskFilters, localDate, pendingTasks, taskTiming } from "./marketingWork";
import type { Task } from "./types";

const task = (id: string, dueAt: string, status: Task["status"] = "OPEN", ownerUserId = "marketing") =>
  ({ id, dueAt, status, ownerUserId } as Task);

describe("agenda de Marketing", () => {
  it("filtra pendientes y responsable en el servidor antes de solicitar cada página", () => {
    expect(agendaTaskFilters("marketing", "", 8, 25)).toEqual({
      statuses: ["OPEN", "IN_PROGRESS"], ownerUserId: "marketing", page: 8, size: 25,
      dueFrom: undefined, dueUntil: undefined,
    });
    expect(agendaTaskFilters(undefined, "", 0, 5).ownerUserId).toBeUndefined();
  });

  it("usa los límites de día de Buenos Aires para filtrar vencidas, hoy y próximas sin huecos", () => {
    const now = new Date("2026-10-23T01:30:00Z");
    const overdue = agendaTaskFilters(undefined, "overdue", 0, 25, now);
    const today = agendaTaskFilters(undefined, "today", 0, 25, now);
    const upcoming = agendaTaskFilters(undefined, "upcoming", 0, 25, now);
    expect(overdue.dueFrom).toBeUndefined();
    expect(overdue.dueUntil).toBe("2026-10-22T03:00:00.000Z");
    expect(today.dueFrom).toBe(overdue.dueUntil);
    expect(today.dueUntil).toBe("2026-10-23T03:00:00.000Z");
    expect(upcoming.dueFrom).toBe(today.dueUntil);
    expect(upcoming.dueUntil).toBeUndefined();
  });
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
