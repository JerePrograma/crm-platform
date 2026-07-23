import { expect, test, type Page } from "@playwright/test";
import { Buffer } from "node:buffer";
import { createHmac, randomUUID } from "node:crypto";

const adminUser = process.env.CRM_E2E_USERNAME ?? "complete-admin";
const adminPassword = process.env.CRM_E2E_PASSWORD ?? "complete-admin-password";
const inboundSecret =
  process.env.CRM_E2E_INBOUND_SECRET ?? "synthetic-complete-crm-inbound-secret";

async function login(page: Page, username: string, password: string) {
  await page.goto("/");
  await page.getByLabel("Usuario").fill(username);
  await page.getByLabel("Contraseña").fill(password);
  await page.getByRole("button", { name: "Ingresar" }).click();
  await expect(page.getByRole("heading", { name: "Resumen comercial" })).toBeVisible();
}

test("complete synthetic CRM journey stays tenant-scoped and fail-closed", async ({ page }) => {
  test.setTimeout(180_000);
  const suffix = `${Date.now()}`;
  const prospectName = `E2E Synthetic ${suffix}`;
  const opportunityProspectName = `E2E Opportunity ${suffix}`;
  const contactEmail = `synthetic-${suffix}@example.test`;
  const viewerUser = `viewer-${suffix}`;
  const viewerPassword = `viewer-${suffix}-password`;

  await login(page, adminUser, adminPassword);
  await expect(page.getByText("Los envíos reales están bloqueados", { exact: true })).toBeVisible();

  await page.getByRole("button", { name: "Usuarios" }).click();
  await page.getByRole("main").getByLabel("Usuario").fill(viewerUser);
  await page.getByLabel("Nombre visible").fill(`Viewer ${suffix}`);
  await page.getByLabel("Contraseña inicial").fill(viewerPassword);
  await page.getByLabel("Rol").selectOption("VIEWER");
  await page.getByRole("button", { name: "Crear usuario" }).click();
  await expect(page.getByText("Usuario creado.")).toBeVisible();

  await page.getByRole("button", { name: "Prospectos" }).click();
  await page.getByLabel("Nueva institución").fill(prospectName);
  await page.getByLabel("Localidad").first().fill("Rosario");
  await page.getByRole("button", { name: "Crear prospecto" }).click();
  await expect(page.getByRole("cell", { name: prospectName })).toBeVisible();
  await page.getByRole("cell", { name: prospectName }).click();

  await page.getByLabel("Nombre", { exact: true }).fill("Contacto sintético");
  await page.getByLabel("Correo electrónico", { exact: true }).fill(`synthetic-${suffix}@example.test`);
  await page.getByRole("button", { name: "Agregar contacto" }).click();
  await expect(page.getByText("Contacto agregado y elegibilidad actualizada.")).toBeVisible();

  const due = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString().slice(0, 16);
  const tasksPanel = page.locator("details.disclosure-panel").filter({ hasText: "Tareas de seguimiento" });
  await tasksPanel.locator("summary").click();
  await tasksPanel.getByLabel("Nueva tarea").fill(`Tarea synthetic ${suffix}`);
  await tasksPanel.getByLabel("Vencimiento").fill(due);
  await tasksPanel.getByRole("button", { name: "Crear tarea" }).click();
  await expect(page.getByText("Tarea creada.")).toBeVisible();
  const activityPanel = page.locator("details.disclosure-panel").filter({ hasText: "Actividad y notas" });
  await activityPanel.locator("summary").click();
  await activityPanel.getByLabel("Resumen", { exact: true }).fill(`Actividad synthetic ${suffix}`);
  await activityPanel.getByRole("button", { name: "Registrar actividad" }).click();
  await expect(page.getByText("Actividad registrada.")).toBeVisible();
  for (const status of ["En calificación", "Listo para contactar", "Contactado"] as const) {
    await page.getByRole("button", { name: `Pasar a ${status}` }).click();
  }
  await expect(page.getByRole("button", { name: "Pasar a Respondió" })).toBeVisible();
  await activityPanel.getByLabel("Nota").fill(`Nota synthetic ${suffix}`);
  await activityPanel.getByRole("button", { name: "Agregar nota" }).click();
  await expect(page.getByText("Nota registrada.")).toBeVisible();

  await page.getByRole("button", { name: "Prospectos" }).click();
  await page.getByLabel("Nueva institución").fill(opportunityProspectName);
  await page.getByLabel("Localidad").first().fill("Córdoba");
  await page.getByRole("button", { name: "Crear prospecto" }).click();
  await expect(page.getByRole("cell", { name: opportunityProspectName })).toBeVisible();

  await page.getByRole("button", { name: "Importaciones" }).click();
  const csv = Buffer.from(
    `Institución,Localidad\nE2E Syntetic ${suffix},Rosario\nE2E Imported ${suffix},Mendoza\n`,
    "utf8",
  );
  await page.locator('input[type="file"]').setInputFiles({
    name: `synthetic-${suffix}.csv`,
    mimeType: "text/csv",
    buffer: csv,
  });
  await page.getByRole("button", { name: "Generar vista previa" }).click();
  await expect(page.getByText("Vista previa", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Ejecutar importación" }).click();
  const importDialog = page.getByRole("dialog", { name: "Ejecutar importación" });
  await expect(importDialog).toBeVisible();
  await importDialog.getByRole("button", { name: "Ejecutar importación" }).click();
  await expect(page.getByText("Importación ejecutada", { exact: true })).toBeVisible();
  const duplicateReviews = page
    .locator("article.duplicate-review-card")
    .filter({ hasText: `E2E Syntetic ${suffix}` });
  await expect(duplicateReviews).toHaveCount(2);
  const duplicateReview = duplicateReviews.first();
  await duplicateReview.getByRole("button", { name: "Vincular con el existente" }).click();
  const linkDialog = page.getByRole("dialog", { name: "Vincular con el existente" });
  await expect(linkDialog).toBeVisible();
  await linkDialog.getByRole("button", { name: "Vincular con el existente" }).click();
  await expect(duplicateReviews).toHaveCount(1);

  await page.getByRole("button", { name: "Exclusiones" }).click();
  await page.getByLabel("Valor").fill(`excluded-${suffix}@example.test`);
  await page.getByRole("button", { name: "Excluir" }).click();
  await expect(page.getByRole("cell", { name: `excluded-${suffix}@example.test` })).toBeVisible();

  await page.getByRole("button", { name: "Oportunidades" }).click();
  const opportunityPanel = page.getByRole("heading", { name: "Nueva oportunidad" }).locator("..");
  await opportunityPanel.getByLabel("Prospecto").selectOption({ label: opportunityProspectName });
  await opportunityPanel.getByLabel("Nombre de la oportunidad").fill(`Venta synthetic ${suffix}`);
  await opportunityPanel.getByLabel("Valor estimado en ARS").fill("250000");
  await opportunityPanel.getByRole("button", { name: "Crear oportunidad" }).click();
  await expect(page.getByText(`Venta synthetic ${suffix}`).first()).toBeVisible();
  for (const action of ["Mover a Diagnóstico", "Mover a Demostración", "Mover a Propuesta"] as const) {
    const card = page.locator("article.opportunity-card").filter({ hasText: `Venta synthetic ${suffix}` });
    await card.getByRole("button", { name: action, exact: true }).click();
    await expect(card).toBeVisible();
  }
  const wonCard = page.locator("article.opportunity-card").filter({ hasText: `Venta synthetic ${suffix}` });
  await wonCard.getByRole("button", { name: "Marcar como ganada" }).click();
  const closeDialog = page.getByRole("dialog", { name: "Registrar oportunidad ganada" });
  await closeDialog.getByLabel("Motivo del cierre").fill("Cierre sintético E2E");
  await closeDialog.getByRole("button", { name: "Registrar cierre" }).click();
  await expect(wonCard).toContainText("100%");

  await page.getByRole("button", { name: "Campañas" }).click();
  const templatePanel = page.getByRole("heading", { name: "Nueva plantilla versionada" }).locator("..");
  await templatePanel.getByLabel("Nombre").fill(`Template synthetic ${suffix}`);
  await templatePanel.getByRole("button", { name: "Crear versión 1" }).click();
  await expect(page.getByText(/Plantilla .* v1 creada\./)).toBeVisible();
  const campaignPanel = page.getByRole("heading", { name: "Nueva campaña" }).locator("..");
  await campaignPanel.getByLabel("Nombre").fill(`Campaign synthetic ${suffix}`);
  const templateOption = campaignPanel
    .getByLabel("Plantilla")
    .locator("option")
    .filter({ hasText: `Template synthetic ${suffix}` });
  const templateValue = await templateOption.getAttribute("value");
  expect(templateValue).not.toBeNull();
  await campaignPanel.getByLabel("Plantilla").selectOption(templateValue!);
  await campaignPanel.getByRole("button", { name: "Crear borrador" }).click();
  const campaignCard = () => page.locator("article.entity-card").filter({ hasText: `Campaign synthetic ${suffix}` });
  await expect(campaignCard()).toContainText("Borrador");
  await campaignCard().getByRole("button", { name: "Configurar secuencia segura" }).click();
  await expect(page.getByText("Secuencia declarativa de contacto, espera y parada configurada.")).toBeVisible();
  await campaignCard().getByRole("button", { name: "Congelar audiencia" }).click();
  const audienceDialog = page.getByRole("dialog", { name: "Confirmar audiencia" });
  await audienceDialog.getByRole("button", { name: "Confirmar audiencia" }).click();
  await expect(campaignCard()).toContainText("Listo para revisión");
  await campaignCard().getByRole("button", { name: "Aprobar" }).click();
  const approvalDialog = page.getByRole("dialog", { name: "Aprobar para simulación" });
  await approvalDialog.getByRole("button", { name: "Aprobar simulación" }).click();
  await expect(campaignCard()).toContainText("Aprobado");
  await campaignCard().getByRole("button", { name: "Simular" }).click();
  await expect(page.getByText(/Simulación completa:/)).toBeVisible();

  await page.getByRole("button", { name: "Bandeja de salida" }).click();
  await expect(page.getByText("No existe una acción para forzar proveedores externos")).toBeVisible();
  await page.getByRole("button", { name: "Ejecutar una vez" }).click();
  await expect(page.getByText(/Ejecución manual finalizada\./)).toBeVisible();

  const meResponse = await page.request.get("/api/v1/auth/me");
  expect(meResponse.ok()).toBeTruthy();
  const me = (await meResponse.json()) as { organizationId: string };
  const eventId = `e2e-${randomUUID()}`;
  const timestamp = Math.floor(Date.now() / 1000);
  const nonce = `nonce-${randomUUID()}`;
  const inboundBody = JSON.stringify({
    externalEventId: eventId,
    externalMessageId: `message-${randomUUID()}`,
    channel: "EMAIL",
    sender: contactEmail,
    receivedAt: new Date(timestamp * 1000).toISOString(),
    body: "Synthetic inbound response",
  });
  const signature = createHmac("sha256", inboundSecret)
    .update(`${timestamp}.${nonce}.${me.organizationId}.`)
    .update(inboundBody)
    .digest("hex");
  const webhookHeaders = {
    "Content-Type": "application/json",
    "X-Organization-Id": me.organizationId,
    "X-Fake-Timestamp": `${timestamp}`,
    "X-Fake-Nonce": nonce,
    "X-Fake-Signature": signature,
  };
  const accepted = await page.request.post("/api/v1/webhooks/fake-inbound", {
    headers: webhookHeaders,
    data: inboundBody,
  });
  expect(accepted.status()).toBe(202);
  const replay = await page.request.post("/api/v1/webhooks/fake-inbound", {
    headers: webhookHeaders,
    data: inboundBody,
  });
  expect(replay.status()).toBe(200);
  await page.getByRole("button", { name: "Ejecutar una vez" }).click();

  await page.getByRole("button", { name: "Mensajes recibidos" }).click();
  await expect(page.getByRole("heading", { name: "Recepción de prueba", exact: true })).toBeVisible();
  await expect(page.getByText("Deshabilitada", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Reintentar carga" }).click();
  const newestInbound = page.locator("tbody tr").first();
  await expect(newestInbound).toContainText("Correo electrónico");
  await expect(newestInbound).toContainText("Procesado");

  await page.getByRole("button", { name: "Prospectos" }).click();
  await page.getByLabel("Buscar prospectos").fill(prospectName);
  await page.getByRole("button", { name: "Buscar" }).click();
  await expect(page.getByRole("cell", { name: prospectName })).toBeVisible();
  await page.getByRole("cell", { name: prospectName }).click();
  await expect(page.getByRole("definition").filter({ hasText: "Respondió" })).toBeVisible();
  const inboundActivityPanel = page.locator("details.disclosure-panel").filter({ hasText: "Actividad y notas" });
  if (!(await inboundActivityPanel.evaluate((element) => (element as HTMLDetailsElement).open))) {
    await inboundActivityPanel.locator("summary").click();
  }
  await expect(inboundActivityPanel.getByText("Respuesta inbound recibida")).toBeVisible();

  await page.getByRole("button", { name: "Reportes" }).click();
  await expect(page.getByRole("heading", { name: "Período y exportación" })).toBeVisible();
  await expect(page.getByText("Valor por moneda")).toBeVisible();

  await page.getByRole("button", { name: "Configuración" }).click();
  await expect(page.getByText("Ningún usuario puede habilitar envíos reales")).toBeVisible();
  await expect(page.getByText("Disponible, sin conexión externa").first()).toBeVisible();
  await expect(page.getByText("Bloqueados", { exact: true }).first()).toBeVisible();

  const csrfResponse = await page.request.get("/api/v1/auth/csrf");
  expect(csrfResponse.ok()).toBeTruthy();
  const csrf = (await csrfResponse.json()) as { headerName: string; token: string };
  const forbiddenSend = await page.request.post("/api/v1/messages/send", {
    headers: { [csrf.headerName]: csrf.token },
    data: {},
  });
  expect(forbiddenSend.status()).toBe(404);
  const readiness = await page.request.get("/actuator/health/readiness");
  expect(readiness.ok()).toBeTruthy();
  const metrics = await page.request.get("/actuator/metrics");
  expect(metrics.ok()).toBeTruthy();

  await page.getByRole("button", { name: "Auditoría" }).click();
  await expect(page.getByRole("heading", { name: "Eventos recientes" })).toBeVisible();

  await page.getByRole("button", { name: "Cerrar sesión" }).click();
  await expect(page.getByRole("heading", { name: "Ingresar" })).toBeVisible();
  await login(page, viewerUser, viewerPassword);
  await page.getByRole("button", { name: "Prospectos" }).click();
  await expect(page.getByLabel("Nueva institución")).toHaveCount(0);
  await page.getByLabel("Buscar prospectos").fill(prospectName);
  await page.getByRole("button", { name: "Buscar" }).click();
  await expect(page.getByRole("cell", { name: prospectName })).toBeVisible();
  await expect(page.getByRole("button", { name: "Usuarios" })).toHaveCount(0);
  await page.getByRole("button", { name: "Configuración" }).click();
  await expect(page.getByRole("button", { name: "Guardar configuración" })).toHaveCount(0);
  await page.getByRole("button", { name: "Cerrar sesión" }).click();
  await expect(page.getByRole("heading", { name: "Ingresar" })).toBeVisible();
});

test("responsive shell and primary navigation remain keyboard operable", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/");
  await page.getByLabel("Usuario").focus();
  await expect(page.getByLabel("Usuario")).toBeFocused();
  await page.keyboard.press("Tab");
  await expect(page.getByLabel("Contraseña")).toBeFocused();
  await page.keyboard.press("Tab");
  await expect(page.getByRole("button", { name: "Ingresar" })).toBeFocused();
  await page.getByLabel("Usuario").fill(adminUser);
  await page.getByLabel("Contraseña").fill(adminPassword);
  await page.getByRole("button", { name: "Ingresar" }).click();
  await expect(page.getByRole("navigation", { name: "Navegación principal" })).toBeVisible();
  await page.getByRole("button", { name: "Reportes" }).focus();
  await page.keyboard.press("Enter");
  await expect(page.getByRole("heading", { name: "Reportes" })).toBeVisible();
  await expect(page.locator("body")).not.toHaveCSS("overflow-x", "scroll");
});
