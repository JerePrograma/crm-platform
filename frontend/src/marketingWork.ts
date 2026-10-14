import type { Task } from "./types";

export function localDate(value: string | Date, timezone = "America/Argentina/Buenos_Aires"): string {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: timezone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date(value));
}

export function taskTiming(task: Task, now = new Date(), timezone?: string): "overdue" | "today" | "upcoming" {
  const due = localDate(task.dueAt, timezone);
  const today = localDate(now, timezone);
  return due < today ? "overdue" : due === today ? "today" : "upcoming";
}

export function pendingTasks(tasks: Task[], ownerId?: string): Task[] {
  return tasks
    .filter((task) => ["OPEN", "IN_PROGRESS"].includes(task.status) && (!ownerId || task.ownerUserId === ownerId))
    .sort((left, right) => Date.parse(left.dueAt) - Date.parse(right.dueAt));
}

export const prospectSources = [
  ["MANUAL", "Carga manual"],
  ["WHATSAPP", "Consulta por WhatsApp"],
  ["WEBSITE", "Sitio web / consulta guiada"],
  ["INSTAGRAM", "Instagram"],
  ["FACEBOOK", "Facebook"],
  ["REFERRAL", "Recomendación"],
  ["B2B_PROSPECTING", "Prospección de empresas"],
  ["OTHER", "Otro origen"],
] as const;
