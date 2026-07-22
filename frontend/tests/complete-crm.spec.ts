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
  await expect(page.getByRole("heading", { name: "Dashboard" })).toBeVisible();
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
  await expect(page.getByText("Envíos bloqueados", { exact: true })).toBeVisible();

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
  await page.getByLabel("Email", { exact: true }).fill(`synthetic-${suffix}@example.test`);
  await page.getByRole("button", { name: "Agregar contacto" }).click();
  await expect(page.getByText("Contacto agregado.")).toBeVisible();

  const due = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString().slice(0, 16);
  await page.getByLabel("Nueva tarea").fill(`Tarea synthetic ${suffix}`);
  await page.getByLabel("Vencimiento").fill(due);
  await page.getByRole("button", { name: "Crear tarea" }).click();
  await expect(page.getByText("Tarea creada.")).toBeVisible();
  await page.getByLabel("Resumen de actividad").fill(`Actividad synthetic ${suffix}`);
  await page.getByRole("button", { name: "Registrar actividad" }).click();
  await expect(page.getByText("Actividad registrada.")).toBeVisible();
  for (const status of ["QUALIFYING", "READY_TO_CONTACT", "CONTACTED"] as const) {
    await page.getByRole("button", { name: `Pasar a ${status}` }).click();
  }
  await expect(page.getByRole("button", { name: "Pasar a REPLIED" })).toBeVisible();
  await page.getByLabel("Nota").fill(`Nota synthetic ${suffix}`);
  await page.getByRole("button", { name: "Agregar nota" }).click();
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
  await page.getByRole("button", { name: "Ejecutar preview" }).click();
  await expect(page.getByText("Preview", { exact: true })).toBeVisible();
  page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "Importar con confirmación" }).click();
  await expect(page.getByText("Ejecución", { exact: true })).toBeVisible();
  const duplicateRow = page
    .getByRole("row")
    .filter({ hasText: `E2E Syntetic ${suffix}` });
  const linkDuplicates = duplicateRow.getByRole("button", { name: "Vincular" });
  await expect(linkDuplicates).toHaveCount(2);
  await linkDuplicates.first().click();
  await expect(linkDuplicates).toHaveCount(1);

  await page.getByRole("button", { name: "Exclusiones" }).click();
  await page.getByLabel("Valor").fill(`excluded-${suffix}@example.test`);
  await page.getByRole("button", { name: "Excluir" }).click();
  await expect(page.getByRole("cell", { name: `excluded-${suffix}@example.test` })).toBeVisible();

  await page.getByRole("button", { name: "Pipeline" }).click();
  const opportunityPanel = page.getByRole("heading", { name: "Nueva oportunidad" }).locator("..");
  await opportunityPanel.getByLabel("Prospecto").selectOption({ label: opportunityProspectName });
  await opportunityPanel.getByLabel("Nombre").fill(`Venta synthetic ${suffix}`);
  await opportunityPanel.getByLabel("Valor estimado ARS").fill("250000");
  await opportunityPanel.getByRole("button", { name: "Crear oportunidad" }).click();
  await expect(page.getByText(`Venta synthetic ${suffix}`).first()).toBeVisible();
  for (const stage of ["DISCOVERY", "DEMO", "PROPOSAL", "WON"] as const) {
    const card = page.locator("article.opportunity-card").filter({ hasText: `Venta synthetic ${suffix}` });
    if (stage === "WON") page.once("dialog", (dialog) => dialog.accept("Cierre sintético E2E"));
    await card.getByRole("button", { name: stage, exact: true }).click();
    await expect(card).toBeVisible();
    if (stage === "WON") await expect(card).toContainText("100%");
  }

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
  await expect(campaignCard()).toContainText("DRAFT");
  await campaignCard().getByRole("button", { name: "Configurar secuencia segura" }).click();
  await expect(page.getByText("Secuencia declarativa de contacto, espera y parada configurada.")).toBeVisible();
  page.once("dialog", (dialog) => dialog.accept());
  await campaignCard().getByRole("button", { name: "Congelar audiencia" }).click();
  await expect(campaignCard()).toContainText("READY_FOR_REVIEW");
  page.once("dialog", (dialog) => dialog.accept());
  await campaignCard().getByRole("button", { name: "Aprobar" }).click();
  await expect(campaignCard()).toContainText("APPROVED");
  await campaignCard().getByRole("button", { name: "Simular" }).click();
  await expect(page.getByText(/Simulación completa:/)).toBeVisible();

  await page.getByRole("button", { name: "Outbox y workers" }).click();
  await expect(page.getByText("No existe una acción para forzar providers reales")).toBeVisible();
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

  await page.getByRole("button", { name: "Inbound y quarantine" }).click();
  await expect(page.getByText("FAKE_INBOUND", { exact: true })).toBeVisible();
  await expect(page.getByText("Deshabilitada", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Reintentar carga" }).click();
  const newestInbound = page.locator("tbody tr").first();
  await expect(newestInbound).toContainText("EMAIL");
  await expect(newestInbound).toContainText("PROCESSED");

  await page.getByRole("button", { name: "Prospectos" }).click();
  await page.getByLabel("Buscar prospectos").fill(prospectName);
  await page.getByRole("button", { name: "Buscar" }).click();
  await expect(page.getByRole("cell", { name: prospectName })).toBeVisible();
  await page.getByRole("cell", { name: prospectName }).click();
  await expect(page.getByRole("definition").filter({ hasText: "REPLIED" })).toBeVisible();
  await expect(page.getByText("Respuesta inbound recibida")).toBeVisible();

  await page.getByRole("button", { name: "Reportes" }).click();
  await expect(page.getByRole("heading", { name: "Período y exportación" })).toBeVisible();
  await expect(page.getByText("Valor por moneda")).toBeVisible();

  await page.getByRole("button", { name: "Configuración" }).click();
  await expect(page.getByText("Ningún usuario puede habilitar envíos reales")).toBeVisible();
  await expect(page.getByText("Adaptador implementado, no conectado").first()).toBeVisible();
  await expect(page.getByText("false", { exact: true }).first()).toBeVisible();

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
