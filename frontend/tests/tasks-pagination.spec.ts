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

test("La ficha mantiene bloqueados los envíos mientras actualiza una operación", async ({ page }) => {
  await signIn(page);
  const suffix = Date.now();
  const name = `Ficha ocupada sintética ${suffix}`;
  const note = `Nota conservada durante la actualización ${suffix}`;
  await page.getByRole("button", { name: "Prospectos", exact: true }).click();
  await page.getByLabel("Nuevo prospecto", { exact: true }).fill(name);
  await page.getByLabel("Localidad", { exact: true }).first().fill("Rosario");
  const createdResponse = page.waitForResponse((response) =>
    response.request().method() === "POST" && new URL(response.url()).pathname === "/api/v1/prospects",
  );
  await page.getByRole("button", { name: "Crear prospecto", exact: true }).click();
  const prospect = await (await createdResponse).json() as { id: string };
  await expect(page.getByRole("heading", { name, exact: true })).toBeVisible();
  await page.getByLabel("Nombre", { exact: true }).fill("Contacto sintético");
  await page.getByLabel("Correo electrónico", { exact: true }).fill(`busy-${suffix}@example.test`);
  await page.getByRole("button", { name: "Agregar contacto", exact: true }).click();
  await expect(page.getByText("Contacto agregado y elegibilidad actualizada.", { exact: true })).toBeVisible();
  const qualify = page.getByRole("button", { name: "Pasar a En calificación", exact: true });
  await expect(qualify).toBeEnabled();
  const tasksPanel = page.locator("details.disclosure-panel").filter({ has: page.locator("summary", { hasText: "Tareas de seguimiento" }) });
  const activityPanel = page.locator("details.disclosure-panel").filter({ has: page.locator("summary", { hasText: "Actividad y notas" }) });
  await tasksPanel.locator("summary").click();
  await activityPanel.locator("summary").click();
  const noteInput = activityPanel.getByLabel("Nota", { exact: true });
  await noteInput.fill(note);

  const noteRequests: unknown[] = [];
  page.on("request", (request) => {
    if (request.method() === "POST" && new URL(request.url()).pathname === `/api/v1/prospects/${prospect.id}/notes`) {
      noteRequests.push(request.postDataJSON());
    }
  });
  let notifyRefresh!: () => void;
  let releaseRefresh!: () => void;
  const refreshHeld = new Promise<void>((resolve) => { notifyRefresh = resolve; });
  const refreshReleased = new Promise<void>((resolve) => { releaseRefresh = resolve; });
  let holdNextRefresh = true;
  await page.route("**/api/v1/prospects?*", async (route) => {
    if (holdNextRefresh) {
      holdNextRefresh = false;
      notifyRefresh();
      await refreshReleased;
    }
    await route.continue();
  });
  const submits = [
    page.getByRole("button", { name: "Agregar contacto", exact: true }),
    tasksPanel.getByRole("button", { name: "Crear tarea", exact: true }),
    activityPanel.getByRole("button", { name: "Registrar actividad", exact: true }),
    activityPanel.getByRole("button", { name: "Agregar nota", exact: true }),
  ];
  await qualify.click();
  try {
    await refreshHeld;
    // El estado nuevo ya llegó; la actualización general sigue pendiente.
    await expect(page.getByRole("button", { name: "Pasar a Listo para contactar", exact: true })).toBeVisible();
    for (const submit of submits) await expect(submit).toBeDisabled();
    await expect(noteInput).toHaveValue(note);
    expect(noteRequests).toHaveLength(0);
  } finally {
    releaseRefresh();
  }
  for (const submit of submits) await expect(submit).toBeEnabled();
  await activityPanel.getByRole("button", { name: "Agregar nota", exact: true }).click();
  await expect(page.getByText("Nota registrada.", { exact: true })).toBeVisible();
  await expect(noteInput).toHaveValue("");
  await expect(activityPanel.getByText(note, { exact: true })).toBeVisible();
  expect(noteRequests).toEqual([{ body: note }]);
});

test("La agenda conserva el bloqueo de cada tarea mientras termina otra", async ({ page }) => {
  const tasks = [task(0, syntheticProspect, "synthetic-owner", "Seguimiento concurrente"), task(1, syntheticProspect, "synthetic-owner", "Seguimiento concurrente")];
  const completed = new Set<string>();
  const statusRequests: Array<{ id: string; body: unknown }> = [];
  const holds = new Map(tasks.map((item) => {
    let release!: () => void;
    let notify!: () => void;
    const released = new Promise<void>((resolve) => { release = resolve; });
    const started = new Promise<void>((resolve) => { notify = resolve; });
    return [item.id, { released, started, release, notify }] as const;
  }));
  await page.route(`**/api/v1/prospects/${syntheticProspect}`, (route) => route.fulfill({
    json: { id: syntheticProspect, displayName: "Prospecto sintético de agenda" },
  }));
  await page.route("**/api/v1/tasks/page?*", async (route) => {
    const content = tasks.filter((item) => !completed.has(item.id));
    const size = Number(new URL(route.request().url()).searchParams.get("size"));
    await route.fulfill({ json: { content, number: 0, size, totalElements: content.length, totalPages: content.length ? 1 : 0, first: true, last: true } });
  });
  await page.route("**/api/v1/tasks/*/status", async (route) => {
    const id = new URL(route.request().url()).pathname.split("/").at(-2)!;
    const item = tasks.find((candidate) => candidate.id === id)!;
    statusRequests.push({ id, body: route.request().postDataJSON() });
    const hold = holds.get(id)!;
    hold.notify();
    await hold.released;
    completed.add(id);
    await route.fulfill({ json: { ...item, status: "COMPLETED", version: 1 } });
  });
  await signIn(page);
  await page.getByRole("button", { name: "Agenda", exact: true }).click();
  const agenda = page.getByRole("region", { name: "Agenda de seguimiento", exact: true });
  const first = agenda.locator("article.agenda-item").filter({ has: page.getByRole("heading", { name: tasks[0].title, exact: true }) });
  const second = agenda.locator("article.agenda-item").filter({ has: page.getByRole("heading", { name: tasks[1].title, exact: true }) });
  const firstSubmit = first.getByRole("button", { name: /^(Completar tarea|Guardando…)$/ });
  const secondSubmit = second.getByRole("button", { name: /^(Completar tarea|Guardando…)$/ });
  try {
    await firstSubmit.click();
    await holds.get(tasks[0].id)!.started;
    await secondSubmit.click();
    await holds.get(tasks[1].id)!.started;
    await expect(firstSubmit).toBeDisabled();
    await expect(secondSubmit).toBeDisabled();

    holds.get(tasks[0].id)!.release();
    await expect(first).toHaveCount(0);
    await expect(agenda.getByRole("navigation", { name: "Páginas de agenda", exact: true })).toContainText("de 1 tareas");
    await expect(secondSubmit).toBeDisabled();
    await expect(secondSubmit).toHaveText("Guardando…");
    await secondSubmit.dispatchEvent("click");
    expect(statusRequests).toEqual(tasks.map((item) => ({ id: item.id, body: { version: 0, status: "COMPLETED" } })));

    holds.get(tasks[1].id)!.release();
    await expect(second).toHaveCount(0);
    await expect(agenda.getByText("No tenés tareas pendientes.", { exact: true })).toBeVisible();
    expect(statusRequests).toHaveLength(2);
  } finally {
    for (const hold of holds.values()) hold.release();
  }
});
