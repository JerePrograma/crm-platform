import { expect, test, type Page } from "@playwright/test";
import type { Task } from "../src/types";

const syntheticProspect = "00000000-0000-0000-0000-000000009999";

async function signIn(page: Page) {
  await page.goto("/");
  await page.getByLabel("Usuario").fill(process.env.CRM_E2E_USERNAME ?? "complete-admin");
  await page.getByLabel("Contraseña").fill(process.env.CRM_E2E_PASSWORD ?? "complete-admin-password");
  await page.getByRole("button", { name: "Ingresar", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Resumen comercial", exact: true })).toBeVisible();
}

function task(index: number, prospectId: string, ownerUserId: string, prefix: string): Task {
  return {
    id: `00000000-0000-0000-0001-${String(index + 1).padStart(12, "0")}`,
    version: 0, prospectId, ownerUserId, creatorUserId: null,
    title: `${prefix} ${index + 1}`, description: null,
    dueAt: new Date(Date.UTC(2030, 0, 1, 12, index)).toISOString(),
    priority: "MEDIUM", status: "OPEN", taskType: "FOLLOW_UP",
    reminderAt: null, completedAt: null, cancelledAt: null, outcome: null,
    createdAt: "2026-10-22T12:00:00Z", updatedAt: "2026-10-22T12:00:00Z",
  };
}

async function taskResponses(page: Page) {
  const requests: URL[] = [];
  let legacyRequests = 0;
  let failNext = false;
  let agendaTotal = 205;
  let pauseNext: { wait: Promise<void>; started: () => void } | null = null;
  await page.route("**/api/v1/tasks", async (route) => {
    legacyRequests++;
    await route.continue();
  });
  await page.route(`**/api/v1/prospects/${syntheticProspect}`, (route) => route.fulfill({
    json: { id: syntheticProspect, displayName: "Prospecto sintético de agenda" },
  }));
  await page.route("**/api/v1/tasks/*/status", async (route) => {
    agendaTotal--;
    await route.fulfill({ json: { ...task(agendaTotal, syntheticProspect, "synthetic-owner", "Pendiente"), status: "COMPLETED", version: 1 } });
  });
  await page.route("**/api/v1/tasks/page?*", async (route) => {
    const url = new URL(route.request().url());
    requests.push(url);
    const pause = pauseNext;
    pauseNext = null;
    if (pause) { pause.started(); await pause.wait; }
    if (failNext) {
      failNext = false;
      await route.fulfill({ status: 503, contentType: "application/problem+json", json: { detail: "Fallo sintético al cargar las tareas" } });
      return;
    }
    const prospectId = url.searchParams.get("prospectId");
    const size = Number(url.searchParams.get("size"));
    const requestedPage = Number(url.searchParams.get("page"));
    const totalElements = prospectId ? 230 : agendaTotal;
    const totalPages = Math.ceil(totalElements / size);
    const number = Math.min(requestedPage, Math.max(totalPages - 1, 0));
    const start = number * size;
    const content = Array.from({ length: Math.min(size, totalElements - start) }, (_, offset) => task(
      start + offset, prospectId ?? syntheticProspect, url.searchParams.get("ownerUserId") ?? "synthetic-owner", prospectId ? "Ficha tarea" : "Pendiente",
    ));
    await route.fulfill({ json: { content, number, size, totalElements, totalPages, first: number === 0, last: number + 1 === totalPages } });
  });
  return {
    requests,
    legacyRequests: () => legacyRequests,
    failNext: () => { failNext = true; },
    setAgendaTotal: (total: number) => { agendaTotal = total; },
    holdNext: () => {
      let release!: () => void;
      let notifyStarted!: () => void;
      const wait = new Promise<void>((resolve) => { release = resolve; });
      const started = new Promise<void>((resolve) => { notifyStarted = resolve; });
      pauseNext = { wait, started: notifyStarted };
      return { started, release };
    },
  };
}

test("La agenda permite recorrer más de 200 pendientes y envía filtros antes de paginar", async ({ page }) => {
  const api = await taskResponses(page);
  await signIn(page);
  await page.getByRole("button", { name: "Agenda", exact: true }).click();
  const pagination = page.getByRole("navigation", { name: "Páginas de agenda", exact: true });
  await expect(pagination).toContainText("Mostrando 1–25 de 205 tareas");
  for (let number = 1; number <= 8; number++) {
    await pagination.getByRole("button", { name: "Siguiente", exact: true }).click();
    await expect(pagination).toContainText(`Página ${number + 1} de 9`);
  }
  await expect(page.getByRole("heading", { name: "Pendiente 205", exact: true })).toBeVisible();
  await expect(pagination.getByRole("button", { name: "Siguiente", exact: true })).toBeDisabled();
  await pagination.getByRole("button", { name: "Anterior", exact: true }).click();
  await expect(pagination).toContainText("Página 8 de 9");
  const pendingRequests = api.requests.filter((url) => !url.searchParams.has("prospectId"));
  expect(pendingRequests.every((url) => url.searchParams.getAll("statuses").join(",") === "OPEN,IN_PROGRESS")).toBeTruthy();
  expect(pendingRequests.every((url) => Boolean(url.searchParams.get("ownerUserId")))).toBeTruthy();

  await page.getByRole("combobox", { name: "Responsable", exact: true }).selectOption("all");
  await expect(pagination).toContainText("Página 1 de 9");
  expect(api.requests.at(-1)!.searchParams.has("ownerUserId")).toBe(false);
  await page.getByRole("combobox", { name: "Vencimiento", exact: true }).selectOption("today");
  await expect.poll(() => api.requests.at(-1)!.searchParams.has("dueFrom") && api.requests.at(-1)!.searchParams.has("dueUntil")).toBe(true);
  await expect(pagination).toContainText("Mostrando 1–25 de 205 tareas");

  api.failNext();
  await page.getByRole("combobox", { name: "Vencimiento", exact: true }).selectOption("overdue");
  await expect(page.getByRole("alert")).toContainText("Fallo sintético al cargar las tareas");
  await expect(page.getByText("No hay tareas con ese vencimiento.", { exact: true })).toHaveCount(0);
  await page.getByRole("button", { name: "Reintentar", exact: true }).click();
  await expect(pagination).toContainText("Mostrando 1–25 de 205 tareas");

  const late = api.holdNext();
  await pagination.getByRole("button", { name: "Siguiente", exact: true }).click();
  await late.started;
  await page.getByRole("combobox", { name: "Responsable", exact: true }).selectOption("mine");
  await expect(pagination).toContainText("Página 1 de 9");
  const oldResponse = page.waitForResponse((response) => new URL(response.url()).pathname === "/api/v1/tasks/page" && new URL(response.url()).searchParams.get("page") === "1");
  late.release();
  await oldResponse;
  await expect(pagination).toContainText("Página 1 de 9");
  await expect(page.getByRole("heading", { name: "Pendiente 1", exact: true })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Pendiente 26", exact: true })).toHaveCount(0);

  api.setAgendaTotal(201);
  await page.getByRole("button", { name: "Actualizar", exact: true }).click();
  await expect(pagination).toContainText("de 201 tareas");
  for (let number = 1; number <= 8; number++) {
    await pagination.getByRole("button", { name: "Siguiente", exact: true }).click();
    await expect(pagination).toContainText(`Página ${number + 1} de 9`);
  }
  await expect(pagination).toContainText("Mostrando 201–201 de 201 tareas");
  await page.getByRole("button", { name: "Completar tarea", exact: true }).click();
  await expect(pagination).toContainText("Mostrando 176–200 de 200 tareas");
  await expect(pagination).toContainText("Página 8 de 8");
  await expect(page.getByRole("heading", { name: "Pendiente 201", exact: true })).toHaveCount(0);
  expect(api.legacyRequests()).toBe(0);
});

test("La ficha pagina sus propias 230 tareas sin depender del lote global", async ({ page }) => {
  const api = await taskResponses(page);
  await signIn(page);
  const name = `Ficha paginada sintética ${Date.now()}`;
  await page.getByRole("button", { name: "Prospectos", exact: true }).click();
  await page.getByLabel("Nuevo prospecto", { exact: true }).fill(name);
  await page.getByLabel("Localidad", { exact: true }).first().fill("Localidad sintética");
  await page.getByRole("button", { name: "Crear prospecto", exact: true }).click();
  await expect(page.getByRole("heading", { name, exact: true })).toBeVisible();
  const panel = page.locator("details.disclosure-panel").filter({ has: page.locator("summary", { hasText: "Tareas de seguimiento" }) });
  await panel.locator("summary").click();
  const pagination = panel.getByRole("navigation", { name: "Páginas de tareas del prospecto", exact: true });
  await expect(pagination).toContainText("Mostrando 1–25 de 230 tareas");
  for (let number = 1; number <= 9; number++) {
    await pagination.getByRole("button", { name: "Siguiente", exact: true }).click();
    await expect(pagination).toContainText(`Página ${number + 1} de 10`);
  }
  await expect(panel.getByText("Ficha tarea 230", { exact: true })).toBeVisible();
  await expect(pagination).toContainText("Mostrando 226–230 de 230 tareas");
  const selectedRequests = api.requests.filter((url) => url.searchParams.has("prospectId"));
  expect(new Set(selectedRequests.map((url) => url.searchParams.get("prospectId"))).size).toBe(1);
  expect(selectedRequests.at(-1)!.searchParams.get("page")).toBe("9");
  expect(selectedRequests.every((url) => !url.searchParams.has("statuses"))).toBeTruthy();
  expect(api.legacyRequests()).toBe(0);
});
