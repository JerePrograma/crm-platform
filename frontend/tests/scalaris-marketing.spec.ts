import { expect, test, type Page } from "@playwright/test";
import path from "node:path";
import { Buffer } from "node:buffer";

const adminUser = process.env.CRM_E2E_USERNAME ?? "complete-admin";
const adminPassword = process.env.CRM_E2E_PASSWORD ?? "complete-admin-password";
const screenshotDirectory = process.env.CRM_SCALARIS_SCREENSHOTS_DIR ?? path.resolve("../validation-output/scalaris-marketing/screenshots");
test.use({ actionTimeout: 15_000 });

async function capture(page: Page, file: string) {
  await page.screenshot({ path: path.join(screenshotDirectory, file), fullPage: true });
}

async function signIn(page: Page, username: string, password: string) {
  await page.goto("/");
  await page.getByLabel("Usuario").fill(username);
  await page.getByLabel("Contraseña").fill(password);
  await page.getByRole("button", { name: "Ingresar", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Resumen comercial", exact: true })).toBeVisible();
}

async function addProspect(page: Page, name: string, service: string, email: string) {
  await page.getByRole("button", { name: "Prospectos", exact: true }).click();
  await page.getByLabel("Nuevo prospecto", { exact: true }).fill(name);
  await page.getByLabel("Localidad", { exact: true }).first().fill("Mar del Plata");
  await page.getByRole("combobox", { name: "Origen", exact: true }).first().selectOption("WHATSAPP");
  await page.getByRole("combobox", { name: "Servicio de interés", exact: true }).selectOption({ label: service });
  await page.getByRole("button", { name: "Crear prospecto", exact: true }).click();
  await expect(page.getByRole("heading", { name, exact: true })).toBeVisible();
  await page.getByLabel("Nombre", { exact: true }).fill("Contacto sintético");
  await page.getByLabel("Correo electrónico", { exact: true }).fill(email);
  await page.getByRole("combobox", { name: "Consentimiento", exact: true }).selectOption("GRANTED");
  await page.getByRole("button", { name: "Agregar contacto", exact: true }).click();
  await expect(page.getByText("Contacto agregado y elegibilidad actualizada.", { exact: true })).toBeVisible();
  await expect(page.getByText("Guardando cambios…", { exact: true })).toHaveCount(0);
}

test("El perfil LEGACY conserva Ventas como rol predeterminado y oculta Marketing", async ({ page }) => {
  await page.route("**/api/v1/business-profile", async (route) => {
    const response = await route.fetch();
    const profile = await response.json();
    await route.fulfill({ response, json: { ...profile, code: "LEGACY", serviceLines: [], operatingGuidance: [] } });
  });
  await signIn(page, adminUser, adminPassword);
  await page.getByRole("button", { name: "Usuarios", exact: true }).click();
  const role = page.getByRole("combobox", { name: "Rol", exact: true });
  await expect(role).toHaveValue("SALES");
  await expect(role.locator('option[value="MARKETING"]')).toHaveCount(0);
  await expect(role.locator('option[value="SALES"]')).toHaveCount(1);
  await page.unrouteAll({ behavior: "wait" });
});

test("Marketing gestiona Scalaris con datos persistentes, segmentación y contacto manual", async ({ page }) => {
  test.setTimeout(180_000);
  await signIn(page, adminUser, adminPassword);
  const profileResponse = await page.request.get("/api/v1/business-profile");
  expect(profileResponse.ok()).toBeTruthy();
  const profile = await profileResponse.json();
  expect(profile.code, "El gate Marketing debe ejecutarse contra un backend SCALARIS real").toBe("SCALARIS");
  expect(profile.serviceLines).toEqual(["Servicio técnico", "Desarrollo", "Venta a pedido"]);
  await expect(page.getByText("WhatsApp oficial: 11 7278-9289", { exact: true })).toBeVisible();
  await expect(page.getByText("Zona Oeste, CABA y Mar del Plata", { exact: true })).toBeVisible();

  const suffix = String(Date.now());
  const username = `marketing-${suffix}`;
  const password = `Marketing-${suffix}-synthetic`;
  await page.getByRole("button", { name: "Usuarios", exact: true }).click();
  await page.getByRole("main").getByLabel("Usuario", { exact: true }).fill(username);
  await page.getByLabel("Nombre visible", { exact: true }).fill(`Marketing sintético ${suffix}`);
  await page.getByLabel("Contraseña inicial", { exact: true }).fill(password);
  await page.getByRole("combobox", { name: "Rol", exact: true }).selectOption("MARKETING");
  await page.getByRole("button", { name: "Crear usuario", exact: true }).click();
  await expect(page.getByText("Usuario creado.", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Cerrar sesión", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Ingresar", exact: true })).toBeVisible();
  await signIn(page, username, password);
  await capture(page, "01-resumen-marketing.png");
  await expect(page.getByRole("button", { name: "Usuarios", exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Configuración", exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Bandeja de salida", exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Mensajes recibidos", exact: true })).toHaveCount(0);

  await page.getByRole("button", { name: "Guía de Marketing", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Cómo trabajar con el CRM de Scalaris", exact: true })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Criterios comerciales", exact: true })).toBeVisible();
  await expect(page.getByText("Gestudio", { exact: false })).toHaveCount(0);
  await capture(page, "02-guia-marketing.png");

  const technicalName = `Persona sintética Scalaris ${suffix}`;
  await addProspect(page, technicalName, "Servicio técnico", `scalaris-tech-${suffix}@example.test`);
  await page.getByRole("textbox", { name: "Necesidad y alcance", exact: true }).fill("Notebook de prueba. Revisar el equipo y confirmar diagnóstico y presupuesto antes de reparar.");
  await page.getByLabel("Asignarme el seguimiento de este prospecto").check();
  await page.getByRole("button", { name: "Guardar cambios", exact: true }).click();
  await expect(page.getByText("Prospecto actualizado.", { exact: true })).toBeVisible();
  await expect(page.getByText("Guardando cambios…", { exact: true })).toHaveCount(0);
  const notesPanel = page.locator("details.disclosure-panel").filter({ hasText: "Actividad y notas" });
  await notesPanel.locator("summary").click();
  const noteText = `Conservar nota ante rechazo sintético ${suffix}`;
  await notesPanel.getByRole("textbox", { name: "Nota", exact: true }).fill(noteText);
  await page.route("**/api/v1/prospects/*/notes", (route) => route.fulfill({
    status: 422, contentType: "application/problem+json", body: JSON.stringify({ detail: "Rechazo sintético de prueba" }),
  }));
  await notesPanel.getByRole("button", { name: "Agregar nota", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText("Rechazo sintético de prueba");
  await expect(notesPanel.getByRole("textbox", { name: "Nota", exact: true })).toHaveValue(noteText);
  await page.unroute("**/api/v1/prospects/*/notes");
  await notesPanel.getByRole("button", { name: "Agregar nota", exact: true }).click();
  await expect(page.getByText("Nota registrada.", { exact: true })).toBeVisible();
  await expect(notesPanel.getByRole("textbox", { name: "Nota", exact: true })).toHaveValue("");
  await expect(notesPanel.getByText("Prospecto creado", { exact: true })).toBeVisible();
  await expect(notesPanel.getByText("PROSPECT_CREATED", { exact: true })).toHaveCount(0);
  const tasksPanel = page.locator("details.disclosure-panel").filter({ hasText: "Tareas de seguimiento" });
  await tasksPanel.locator("summary").click();
  const due = new Date(Date.now() + 24 * 60 * 60 * 1000);
  const localDue = new Date(due.getTime() - 3 * 60 * 60 * 1000).toISOString().slice(0, 16);
  const taskTitle = `Coordinar diagnóstico sintético ${suffix}`;
  await tasksPanel.getByLabel("Nueva tarea", { exact: true }).fill(taskTitle);
  await tasksPanel.getByLabel("Vencimiento", { exact: true }).fill(localDue);
  await tasksPanel.getByRole("button", { name: "Crear tarea", exact: true }).click();
  await expect(page.getByText("Tarea creada.", { exact: true })).toBeVisible();
  const desktopLayout = await page.evaluate(() => ({ width: document.documentElement.scrollWidth, viewport: window.innerWidth }));
  expect(desktopLayout.width).toBeLessThanOrEqual(desktopLayout.viewport);
  await capture(page, "03-ficha-prospecto.png");

  await page.getByRole("button", { name: "Agenda", exact: true }).click();
  const agendaTask = page.locator("article.agenda-item").filter({ hasText: taskTitle });
  await expect(agendaTask).toContainText(technicalName);
  await capture(page, "04-agenda.png");
  await agendaTask.getByRole("button", { name: "Abrir ficha", exact: true }).click();
  await expect(page.getByRole("textbox", { name: "Necesidad y alcance", exact: true })).toHaveValue(/Notebook de prueba/);
  await expect(page.getByLabel("Quitar etiqueta Servicio técnico", { exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByRole("heading", { name: "Resumen comercial", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Agenda", exact: true }).click();
  await expect(page.locator("article.agenda-item").filter({ hasText: taskTitle })).toContainText(technicalName);

  const developmentName = `Negocio sintético Desarrollo ${suffix}`;
  await addProspect(page, developmentName, "Desarrollo", `scalaris-dev-${suffix}@example.test`);
  await page.getByRole("button", { name: "Campañas", exact: true }).click();
  const campaignForm = page.getByRole("heading", { name: "Nueva campaña", exact: true }).locator("..");
  await campaignForm.getByLabel("Nombre", { exact: true }).fill(`Servicio técnico sintético ${suffix}`);
  const templates = campaignForm.getByRole("combobox", { name: "Plantilla", exact: true });
  const templateId = await templates.locator("option").filter({ hasText: "Scalaris · Servicio técnico · Presentación" }).first().getAttribute("value");
  expect(templateId).toBeTruthy();
  await templates.selectOption(templateId!);
  await campaignForm.getByRole("button", { name: "Crear borrador", exact: true }).click();
  const campaign = page.locator("article.entity-card").filter({ hasText: `Servicio técnico sintético ${suffix}` });
  await expect(campaign).toBeVisible();
  await page.getByRole("combobox", { name: "Servicio, público o zona", exact: true }).selectOption({ label: "Servicio técnico" });
  await campaign.getByRole("button", { name: "Congelar audiencia", exact: true }).click();
  await page.getByRole("dialog", { name: "Confirmar audiencia", exact: true }).getByRole("button", { name: "Confirmar audiencia", exact: true }).click();
  await expect(page.getByRole("cell", { name: technicalName, exact: true })).toBeVisible();
  await expect(page.getByRole("row").filter({ has: page.getByRole("cell", { name: technicalName, exact: true }) })).toContainText("Incluido");
  await expect(page.getByRole("cell", { name: developmentName, exact: true })).toHaveCount(0);

  await page.getByRole("button", { name: "Agenda", exact: true }).click();
  await page.locator("article.agenda-item").filter({ hasText: taskTitle }).getByRole("button", { name: "Completar tarea", exact: true }).click();
  await expect(page.getByText("Tarea completada. Registrá la respuesta o el resultado en la ficha del prospecto.", { exact: true })).toBeVisible();
  await expect(page.locator("article.agenda-item").filter({ hasText: taskTitle })).toHaveCount(0);
  await page.getByRole("button", { name: "Borradores y contacto manual", exact: true }).click();
  await expect(page.getByRole("button", { name: "Generar enlace manual", exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Enviar", exact: true })).toHaveCount(0);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.getByRole("button", { name: "Abrir menú", exact: true }).click();
  await expect(page.getByRole("navigation", { name: "Navegación principal", exact: true })).toBeVisible();
  await capture(page, "05-navegacion-movil.png");
  await page.getByRole("button", { name: "Guía de Marketing", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Guía de Marketing", exact: true })).toBeFocused();
  await expect(page.getByRole("navigation", { name: "Navegación principal", exact: true })).not.toBeVisible();
  const offering = await page.getByRole("heading", { name: "Qué ofrecemos", exact: true }).locator("..").boundingBox();
  const criteria = await page.getByRole("heading", { name: "Criterios comerciales", exact: true }).locator("..").boundingBox();
  expect(offering).not.toBeNull();
  expect(criteria).not.toBeNull();
  expect(criteria!.y).toBeGreaterThanOrEqual(offering!.y + offering!.height);
  await capture(page, "06-guia-movil.png");
});

test("Marketing puede previsualizar un CSV sintético mayor a 1 MiB a través del proxy", async ({ page }) => {
  test.setTimeout(120_000);
  const marketingUser = process.env.CRM_E2E_MARKETING_USERNAME;
  const marketingPassword = process.env.CRM_E2E_MARKETING_PASSWORD;
  expect(marketingUser, "El gate requiere la cuenta Marketing bootstrap sintética").toBeTruthy();
  expect(marketingPassword).toBeTruthy();
  await signIn(page, marketingUser!, marketingPassword!);
  const profileResponse = await page.request.get("/api/v1/business-profile");
  expect(profileResponse.ok()).toBeTruthy();
  expect((await profileResponse.json()).code).toBe("SCALARIS");
  const me = await page.request.get("/api/v1/auth/me");
  expect(me.ok()).toBeTruthy();
  expect((await me.json()).role).toBe("MARKETING");
  const suffix = Date.now();
  const extraHeaders = Array.from({ length: 80 }, (_, index) => `Dato sintético ${index}`);
  const rows = Array.from({ length: 2 }, (_, index) =>
    [`Carga sintética ${suffix} ${index}`, "Mar del Plata", `large-upload-${suffix}-${index}@example.test`, ...extraHeaders.map(() => "x".repeat(8_000))].join(","),
  );
  const csv = Buffer.from(`Empresa o persona,Localidad,Correo,${extraHeaders.join(",")}\n${rows.join("\n")}\n`, "utf8");
  expect(csv.byteLength).toBeGreaterThan(1024 * 1024);
  expect(csv.byteLength).toBeLessThan(10 * 1024 * 1024);
  await page.getByRole("button", { name: "Importaciones", exact: true }).click();
  await page.locator('input[type="file"]').setInputFiles({
    name: `large-synthetic-${suffix}.csv`, mimeType: "text/csv", buffer: csv,
  });
  const previewResponse = page.waitForResponse((response) =>
    response.url().endsWith("/api/v1/imports/prospects/preview") && response.request().method() === "POST",
  );
  await page.getByRole("button", { name: "Generar vista previa", exact: true }).click();
  const response = await previewResponse;
  expect(response.status(), "El proxy debe permitir el CSV dentro del límite del backend").toBe(200);
  const summary = await response.json();
  expect(summary.status).toBe("COMPLETED");
  expect(summary.dryRun).toBe(true);
  expect(summary.totalRows).toBe(2);
  expect(summary.acceptedRows).toBe(2);
  expect(summary.rejectedRows).toBe(0);
  await expect(page.getByText("Vista previa", { exact: true })).toBeVisible();
});
