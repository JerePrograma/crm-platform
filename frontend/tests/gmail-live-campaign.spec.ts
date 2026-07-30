import { execFileSync } from "node:child_process";
import { createHash } from "node:crypto";
import path from "node:path";

import { expect, test, type Page } from "@playwright/test";

const adminUser = process.env.CRM_E2E_USERNAME ?? "gmail-e2e-admin";
const adminPassword = process.env.CRM_E2E_PASSWORD ?? "gmail-e2e-admin-password";
const fakeGoogleBaseUrl = process.env.CRM_E2E_FAKE_GOOGLE_URL ?? "http://127.0.0.1:19090";
const fakeGoogleControlKey = process.env.CRM_E2E_FAKE_GOOGLE_CONTROL ?? "synthetic-local-control";
const screenshotDirectory = path.resolve(
  process.env.CRM_GMAIL_SCREENSHOTS_DIR ?? "../validation-output/gmail-campaign-e2e/screenshots",
);
const postgresContainer = process.env.CRM_E2E_POSTGRES_CONTAINER ?? "";
const composeProject = process.env.CRM_E2E_COMPOSE_PROJECT ?? "";
const databaseName = process.env.CRM_E2E_DATABASE ?? "gestudio_gmail_e2e";
const databaseUser = process.env.CRM_E2E_DATABASE_USER ?? "gestudio_gmail_e2e";
const organizationId = "00000000-0000-0000-0000-000000000010";

test.use({ trace: "off", video: "off" });

async function capture(page: Page, file: string) {
  await page.screenshot({ path: path.join(screenshotDirectory, file), fullPage: true });
}

async function login(page: Page, username = adminUser, password = adminPassword) {
  await page.goto("/");
  const loginButton = page.getByRole("button", { name: "Ingresar" });
  const dashboard = page.getByRole("heading", { name: "Resumen comercial" });
  await expect(loginButton.or(dashboard)).toBeVisible();
  if (await loginButton.isVisible()) {
    await page.getByLabel("Usuario").fill(username);
    await page.getByLabel("Contraseña").fill(password);
    await loginButton.click();
  }
  await expect(page.getByRole("heading", { name: "Resumen comercial" })).toBeVisible();
}

function requireIsolatedDatabase() {
  if (!/^[a-z0-9][a-z0-9_-]{5,80}$/.test(composeProject) || !composeProject.startsWith("crm-gmail-fake-")) {
    throw new Error("CRM_E2E_COMPOSE_PROJECT must identify the isolated gmail fake stack");
  }
  if (!/^[a-f0-9]{12,64}$/.test(postgresContainer)) {
    throw new Error("CRM_E2E_POSTGRES_CONTAINER must be an exact Docker container ID");
  }
  const actualProject = execFileSync(
    "docker",
    ["inspect", "--format", '{{ index .Config.Labels "com.docker.compose.project" }}', postgresContainer],
    { encoding: "utf8" },
  ).trim();
  if (actualProject !== composeProject) {
    throw new Error(`PostgreSQL container belongs to ${actualProject}, not the isolated E2E project`);
  }
}

function sql(statement: string) {
  requireIsolatedDatabase();
  execFileSync(
    "docker",
    ["exec", postgresContainer, "psql", "-v", "ON_ERROR_STOP=1", "-U", databaseUser, "-d", databaseName, "-c", statement],
    { encoding: "utf8", stdio: ["ignore", "pipe", "pipe"] },
  );
}

function sqlScalar(statement: string) {
  requireIsolatedDatabase();
  return execFileSync(
    "docker",
    ["exec", postgresContainer, "psql", "-tA", "-v", "ON_ERROR_STOP=1", "-U", databaseUser, "-d", databaseName, "-c", statement],
    { encoding: "utf8", stdio: ["ignore", "pipe", "pipe"] },
  ).trim();
}

function setPersistentSendingPolicy(enabled: boolean) {
  const values = enabled
    ? { enabled: "true", dryRun: "false", dailyLimit: "10", killSwitch: "false", organizationLimit: 10 }
    : { enabled: "false", dryRun: "true", dailyLimit: "0", killSwitch: "true", organizationLimit: 0 };
  sql(`
    UPDATE organization SET campaign_daily_limit = ${values.organizationLimit}, updated_at = now()
      WHERE id = '${organizationId}';
    UPDATE system_setting SET setting_value = CASE setting_key
      WHEN 'sending.enabled' THEN '${values.enabled}'
      WHEN 'sending.dry-run' THEN '${values.dryRun}'
      WHEN 'sending.daily-limit' THEN '${values.dailyLimit}'
      WHEN 'sending.kill-switch' THEN '${values.killSwitch}'
      ELSE setting_value END,
      updated_at = now(), version = version + 1
      WHERE organization_id = '${organizationId}'
        AND setting_key IN ('sending.enabled','sending.dry-run','sending.daily-limit','sending.kill-switch');
  `);
}

function reduceSyntheticSenderInterval() {
  sql(`
    UPDATE integration_connection SET min_interval_seconds = 1, daily_limit = 10,
      next_send_at = NULL, updated_at = now(), version = version + 1
      WHERE organization_id = '${organizationId}' AND provider = 'GMAIL'
        AND normalized_email LIKE '%@gestudio.test';
  `);
}

async function fakeControl(page: Page, modes: Record<string, string>, retryAfterSeconds = 2) {
  const response = await page.request.post(`${fakeGoogleBaseUrl}/__fake-google__/control`, {
    headers: { "X-Fake-Google-Control": fakeGoogleControlKey },
    data: { modes, retryAfterSeconds },
  });
  expect(response.status()).toBe(200);
}

async function fakeState(page: Page) {
  const response = await page.request.get(`${fakeGoogleBaseUrl}/__fake-google__/state`, {
    headers: { "X-Fake-Google-Control": fakeGoogleControlKey },
  });
  expect(response.status()).toBe(200);
  return response.json() as Promise<{
    acceptedMessageCount: number;
    gmailRequestCount: number;
    gmailRequests: Array<{ recipientHash: string | null; hasPlainText: boolean; hasHtml: boolean; hasOneClickUnsubscribe: boolean }>;
  }>;
}

async function createUser(page: Page, username: string, password: string) {
  await page.getByRole("button", { name: "Usuarios" }).click();
  await page.getByRole("main").getByLabel("Usuario").fill(username);
  await page.getByLabel("Nombre visible").fill("Ventas sintético sin ejecución");
  await page.getByLabel("Contraseña inicial").fill(password);
  await page.getByLabel("Rol").selectOption("SALES");
  await page.getByRole("button", { name: "Crear usuario" }).click();
  await expect(page.getByText("Usuario creado.")).toBeVisible();
}

async function createProspectWithEmail(page: Page, name: string, email: string, consent: "GRANTED" | "DENIED" = "GRANTED") {
  await page.getByRole("button", { name: "Prospectos" }).click();
  await page.getByLabel("Nueva institución").fill(name);
  await page.getByLabel("Localidad").first().fill("Ciudad Sintética");
  await page.getByRole("button", { name: "Crear prospecto" }).click();
  await expect(page.getByRole("cell", { name })).toBeVisible();
  await page.getByRole("cell", { name }).click();
  await page.getByLabel("Nombre", { exact: true }).fill("Contacto");
  await page.getByLabel("Apellido").fill("Sintético");
  await page.getByLabel("Correo electrónico", { exact: true }).fill(email);
  await page.getByLabel("Consentimiento").selectOption(consent);
  await page.getByRole("button", { name: "Agregar contacto" }).click();
  await expect(page.getByText("Contacto agregado y elegibilidad actualizada.")).toBeVisible();
}

async function connectSyntheticGoogle(page: Page) {
  await page.getByRole("button", { name: "Configuración" }).click();
  await expect(page.getByRole("heading", { name: "Cuentas remitentes" })).toBeVisible();
  await page.getByRole("button", { name: "Conectar cuenta de Google" }).click();
  await page.waitForURL(new RegExp(`${fakeGoogleBaseUrl.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}/o/oauth2/v2/auth`));
  await expect(page.getByRole("heading", { name: "Autorizar Gmail sintético" })).toBeVisible();
  await capture(page, "02-conectar-google.png");
  await page.getByRole("button", { name: "Autorizar cuenta sintética" }).click();
  await expect(page.getByText("Cuenta de Google conectada correctamente.")).toBeVisible();
  await expect.poll(() => new URL(page.url()).search).toBe("");
  await capture(page, "03-callback-satisfactorio.png");
  await expect(page.getByText("campaigns@gestudio.test").first()).toBeVisible();
  await capture(page, "04-cuenta-conectada.png");
  await expect(page.getByText("Predeterminada").locator("..")).toContainText("Sí");
  await capture(page, "05-cuenta-predeterminada.png");

  await page.getByRole("button", { name: "Verificar" }).click();
  await expect(page.getByText("Cuenta verificada.")).toBeVisible();
  await capture(page, "06-verificacion.png");

  await page.getByRole("button", { name: "Reconectar" }).click();
  await page.waitForURL(/\/o\/oauth2\/v2\/auth/);
  await capture(page, "07-reconexion.png");
  await page.getByRole("button", { name: "Autorizar cuenta sintética" }).click();
  await expect(page.getByText("Cuenta de Google conectada correctamente.")).toBeVisible();
  await expect.poll(() => new URL(page.url()).search).toBe("");
}

function campaignCard(page: Page, name: string) {
  return page.locator("article.entity-card").filter({ hasText: name });
}

async function selectTemplate(page: Page, campaignPanel: ReturnType<Page["locator"]>, templateName: string) {
  const option = campaignPanel.getByLabel("Plantilla").locator("option").filter({ hasText: templateName });
  await expect(option).toHaveCount(1);
  const value = await option.getAttribute("value");
  expect(value).not.toBeNull();
  await campaignPanel.getByLabel("Plantilla").selectOption(value ?? "");
}

async function selectSyntheticSender(campaignPanel: ReturnType<Page["locator"]>) {
  const sender = campaignPanel.getByLabel("Cuenta remitente");
  const option = sender.locator("option").filter({ hasText: "campaigns@gestudio.test" });
  await expect(option).toHaveCount(1);
  const value = await option.getAttribute("value");
  expect(value).not.toBeNull();
  await sender.selectOption(value ?? "");
}

async function createLiveDraft(page: Page, name: string, templateName: string) {
  const panel = page.getByRole("heading", { name: "Nueva campaña" }).locator("..");
  await panel.getByLabel("Nombre").fill(name);
  await panel.getByRole("radio", { name: /Envío real/ }).check();
  await selectTemplate(page, panel, templateName);
  await selectSyntheticSender(panel);
  await panel.getByLabel("Reply-To").fill("replies@gestudio.test");
  await panel.getByLabel("Inicio de ventana").fill("00:01");
  await panel.getByLabel("Fin de ventana").fill("23:59");
  for (const day of ["Sábado", "Domingo"]) {
    const checkbox = panel.getByLabel(day);
    if (!(await checkbox.isChecked())) await checkbox.check();
  }
  await panel.getByLabel("Límite diario").fill("10");
  await panel.getByLabel("Intervalo mínimo (segundos)").fill("1");
  await panel.getByLabel("Máximo de intentos").fill("3");
  await panel.getByRole("button", { name: "Crear borrador real" }).click();
  await expect(campaignCard(page, name)).toContainText("Borrador");
}

async function freezeAndApprove(page: Page, name: string) {
  const card = campaignCard(page, name);
  await card.getByRole("button", { name: "Congelar audiencia" }).click();
  const audienceDialog = page.getByRole("dialog", { name: "Confirmar audiencia" });
  await audienceDialog.getByRole("button", { name: "Confirmar audiencia" }).click();
  await expect(card).toContainText("Listo para revisión");
  await card.getByRole("button", { name: "Aprobar" }).click();
  const approvalDialog = page.getByRole("dialog", { name: "Aprobar para envío real" });
  await approvalDialog.getByRole("button", { name: "Aprobar campaña real" }).click();
  await expect(card).toContainText("Aprobado");
}

async function startLiveCampaign(page: Page, name: string, captureConfirmation = false) {
  const card = campaignCard(page, name);
  await card.getByRole("button", { name: "Iniciar campaña real" }).click();
  const dialog = page.getByRole("dialog", { name: "Confirmar ejecución real" });
  if (captureConfirmation) await capture(page, "19-confirmacion-envio-real.png");
  await dialog.getByLabel("Escribí SEND_LIVE_CAMPAIGN para confirmar").fill("SEND_LIVE_CAMPAIGN");
  await dialog.getByRole("button", { name: "Iniciar campaña real" }).click();
  await expect(card).toContainText("En proceso");
}

async function runWorkerOnce(page: Page) {
  await page.getByRole("button", { name: "Bandeja de salida" }).click();
  await page.getByRole("button", { name: "Ejecutar una vez" }).click();
  await expect(page.getByText("Ejecución manual finalizada.")).toBeVisible();
  await page.getByRole("button", { name: "Campañas" }).click();
}

async function openResults(page: Page, campaignName: string) {
  const card = campaignCard(page, campaignName);
  await card.getByRole("button", { name: "Ver resultados" }).click();
  await expect(page.getByRole("heading", { name: "Resultados de la campaña" })).toBeVisible();
  await page.getByRole("button", { name: "Actualizar resultados" }).click();
}

test("Gmail OAuth and controlled live campaigns run end to end against the local fake", async ({ page }) => {
  test.setTimeout(360_000);
  requireIsolatedDatabase();
  const suffix = Date.now().toString();
  const templateName = `Plantilla Gmail sintética ${suffix}`;
  const primaryCampaign = `Campaña Gmail controlada ${suffix}`;
  const errorCampaign = `Campaña errores Gmail ${suffix}`;
  const scheduledCampaign = `Campaña programada Gmail ${suffix}`;
  const salesUsername = `sales-gmail-${suffix}`;
  const salesPassword = `sales-gmail-${suffix}-password`;
  const includedEmails = Array.from({ length: 4 }, (_, index) => `gmail-live-${suffix}-${index + 1}@example.test`);
  const excludedEmail = `gmail-excluded-${suffix}@example.test`;
  const deniedInstitution = `Institución sin consentimiento Gmail ${suffix}`;
  const deniedEmail = `gmail-denied-${suffix}@example.test`;

  setPersistentSendingPolicy(true);
  await login(page);
  await connectSyntheticGoogle(page);
  reduceSyntheticSenderInterval();

  await page.getByRole("button", { name: "Mensajes e integraciones" }).click();
  await expect(page.getByText("Gmail real controlado", { exact: true }).first()).toBeVisible();
  await expect(page.getByText("campaigns@gestudio.test").first()).toBeVisible();
  await capture(page, "09-mensajes-integraciones.png");

  for (let index = 0; index < includedEmails.length; index += 1) {
    await createProspectWithEmail(page, `Institución Gmail ${suffix} ${index + 1}`, includedEmails[index]);
  }
  await createProspectWithEmail(page, `Institución excluida Gmail ${suffix}`, excludedEmail);
  await createProspectWithEmail(page, deniedInstitution, deniedEmail, "DENIED");
  await page.getByRole("button", { name: "Exclusiones" }).click();
  await page.getByLabel("Valor").fill(excludedEmail);
  await page.getByRole("button", { name: "Excluir" }).click();
  await expect(page.getByRole("cell", { name: excludedEmail })).toBeVisible();

  await page.getByRole("button", { name: "Campañas" }).click();
  const templatePanel = page.getByRole("heading", { name: "Nueva plantilla versionada" }).locator("..");
  await templatePanel.getByLabel("Nombre").fill(templateName);
  await templatePanel.getByLabel("Asunto").fill("Una propuesta sintética para {{prospect.displayName}}");
  await templatePanel.getByLabel("Texto").fill("Hola {{contact.firstName}}, este mensaje sintético corresponde a {{campaign.name}}.");
  await templatePanel.getByLabel("HTML seguro").fill("<p>Hola <strong>{{contact.firstName}}</strong>, este mensaje es sintético.</p>");
  await templatePanel.getByRole("button", { name: "Crear versión 1" }).click();
  await expect(page.getByText(/Plantilla .* v1 creada\./)).toBeVisible();
  await capture(page, "10-nueva-plantilla.png");
  const templateCard = page.locator("article.entity-card").filter({ hasText: templateName });
  await templateCard.getByRole("button", { name: "Previsualizar" }).click();
  await expect(page.getByText("Vista previa generada con datos sintéticos.")).toBeVisible();
  await capture(page, "11-previsualizacion.png");

  const campaignPanel = page.getByRole("heading", { name: "Nueva campaña" }).locator("..");
  await campaignPanel.getByLabel("Nombre").fill(primaryCampaign);
  await capture(page, "12-nueva-campana.png");
  await campaignPanel.getByRole("radio", { name: /Envío real/ }).check();
  await selectTemplate(page, campaignPanel, templateName);
  await selectSyntheticSender(campaignPanel);
  await capture(page, "13-selector-remitente.png");
  await expect(campaignPanel.getByRole("radio", { name: /Simulación/ })).not.toBeChecked();
  await capture(page, "14-modo-real-simulacion.png");
  await campaignPanel.getByLabel("Reply-To").fill("replies@gestudio.test");
  await campaignPanel.getByLabel("Inicio de ventana").fill("00:01");
  await campaignPanel.getByLabel("Fin de ventana").fill("23:59");
  for (const day of ["Sábado", "Domingo"]) await campaignPanel.getByLabel(day).check();
  await campaignPanel.getByLabel("Límite diario").fill("10");
  await campaignPanel.getByLabel("Intervalo mínimo (segundos)").fill("1");
  await campaignPanel.getByLabel("Máximo de intentos").fill("3");
  await capture(page, "15-filtros.png");
  await campaignPanel.getByRole("button", { name: "Crear borrador real" }).click();
  const primaryCard = campaignCard(page, primaryCampaign);
  await expect(primaryCard).toContainText("Borrador");

  await primaryCard.getByRole("button", { name: "Congelar audiencia" }).click();
  await page.getByRole("dialog", { name: "Confirmar audiencia" }).getByRole("button", { name: "Confirmar audiencia" }).click();
  await expect(primaryCard).toContainText("4 incluidos");
  await expect(primaryCard).toContainText("2 excluidos");
  await primaryCard.getByRole("button", { name: "Ver audiencia" }).click();
  await expect(page.getByRole("heading", { name: "Audiencia congelada" })).toBeVisible();
  const audiencePanel = page.getByRole("heading", { name: "Audiencia congelada" }).locator("..");
  await capture(page, "16-audiencia-congelada.png");
  await expect(audiencePanel).toContainText(/Excluido|EXCLUDED/);
  await expect(audiencePanel).toContainText(deniedInstitution);
  await expect(audiencePanel).toContainText("Prospect is not contact eligible");
  await capture(page, "17-excluidos.png");

  await primaryCard.getByRole("button", { name: "Aprobar" }).click();
  const approvalDialog = page.getByRole("dialog", { name: "Aprobar para envío real" });
  await capture(page, "18-aprobacion.png");
  await approvalDialog.getByRole("button", { name: "Aprobar campaña real" }).click();
  await expect(primaryCard).toContainText("Aprobado");

  await startLiveCampaign(page, primaryCampaign, true);
  await capture(page, "21-ejecutandose.png");
  await primaryCard.getByRole("button", { name: "Pausar" }).click();
  await expect(primaryCard).toContainText("Pausada");
  await capture(page, "22-pausada.png");
  await primaryCard.getByRole("button", { name: "Reanudar" }).click();
  await expect(primaryCard).toContainText("En proceso");
  await capture(page, "23-reanudada.png");

  await fakeControl(page, { gmailSend: "success" });
  for (let index = 0; index < includedEmails.length + 2; index += 1) {
    if (index > 0) await page.waitForTimeout(1_100);
    await runWorkerOnce(page);
    if (await primaryCard.getByText("Completada", { exact: true }).count()) break;
  }
  await expect(primaryCard).toContainText("Completada");
  await openResults(page, primaryCampaign);
  await expect(page.getByText("Aceptados por Gmail").locator("..")).toContainText("4");
  await capture(page, "24-completada.png");
  await capture(page, "25-resultados.png");
  const stateAfterPrimary = await fakeState(page);
  expect(stateAfterPrimary.acceptedMessageCount).toBe(4);
  expect(stateAfterPrimary.gmailRequestCount).toBe(4);
  expect(stateAfterPrimary.gmailRequests.every((request) => request.hasPlainText && request.hasHtml && request.hasOneClickUnsubscribe)).toBe(true);
  const unsubscribedRecipientHash = stateAfterPrimary.gmailRequests.at(-1)?.recipientHash;
  expect(unsubscribedRecipientHash).toBeTruthy();
  const unsubscribedEmail = includedEmails.find(
    (email) => createHash("sha256").update(email).digest("hex") === unsubscribedRecipientHash,
  );
  if (!unsubscribedEmail) throw new Error("The fake Gmail recipient does not match the frozen audience");
  sql(`
    INSERT INTO crm_task (
      id, version, organization_id, prospect_id, owner_user_id, creator_user_id,
      title, description, due_at, priority, status, task_type, reminder_at,
      created_at, updated_at
    )
    SELECT gen_random_uuid(), 0, p.organization_id, p.id, owner.id, owner.id,
      'Seguimiento sintético de campaña', 'Debe cancelarse al registrar la baja',
      now() + interval '1 day', 'MEDIUM', 'OPEN', 'FOLLOW_UP',
      now() + interval '12 hours', now(), now()
    FROM prospect p
    JOIN institution i ON i.id = p.institution_id AND i.organization_id = p.organization_id
    JOIN contact c ON c.institution_id = i.id AND c.organization_id = p.organization_id
        JOIN contact_channel cc ON cc.contact_id = c.id AND cc.organization_id = p.organization_id
        JOIN LATERAL (
          SELECT u.id
          FROM app_user u
          JOIN organization_membership om ON om.user_id = u.id
          WHERE om.organization_id = p.organization_id AND om.active AND u.active
          ORDER BY u.created_at, u.id LIMIT 1
        ) owner ON TRUE
    WHERE cc.type = 'EMAIL' AND cc.normalized_value = '${unsubscribedEmail}';
  `);

  const unsubscribeEnvelope = await page.request.get(`${fakeGoogleBaseUrl}/__fake-google__/last-unsubscribe`, {
    headers: { "X-Fake-Google-Control": fakeGoogleControlKey },
  });
  expect(unsubscribeEnvelope.status()).toBe(200);
  const { url: unsubscribeUrl } = (await unsubscribeEnvelope.json()) as { url: string };
  await page.goto(unsubscribeUrl);
  await expect(page.getByRole("heading", { name: "Confirmar baja" })).toBeVisible();
  await page.getByRole("button", { name: "Confirmar baja" }).click();
  await expect(page.getByRole("heading", { name: "Baja registrada" })).toBeVisible();
  sql(`
    DO $$
    BEGIN
      IF EXISTS (
        SELECT 1
        FROM crm_task t
        JOIN prospect p ON p.id = t.prospect_id AND p.organization_id = t.organization_id
        JOIN institution i ON i.id = p.institution_id AND i.organization_id = p.organization_id
        JOIN contact c ON c.institution_id = i.id AND c.organization_id = p.organization_id
        JOIN contact_channel cc ON cc.contact_id = c.id AND cc.organization_id = p.organization_id
        WHERE cc.type = 'EMAIL' AND cc.normalized_value = '${unsubscribedEmail}'
          AND t.task_type = 'FOLLOW_UP' AND t.status <> 'CANCELLED'
      ) THEN
        RAISE EXCEPTION 'Unsubscribe did not cancel the pending follow-up';
      END IF;
    END $$;
  `);
  await capture(page, "29-baja.png");
  await login(page);
  await page.getByRole("button", { name: "Exclusiones" }).click();
  await expect(page.getByRole("cell", { name: unsubscribedEmail })).toBeVisible();

  await page.getByRole("button", { name: "Campañas" }).click();
  await createLiveDraft(page, errorCampaign, templateName);
  await freezeAndApprove(page, errorCampaign);
  await expect(campaignCard(page, errorCampaign)).toContainText("3 incluidos");
  await startLiveCampaign(page, errorCampaign);

  await page.waitForTimeout(1_100);
  await fakeControl(page, { gmailSend: "429" }, 2);
  await runWorkerOnce(page);
  await openResults(page, errorCampaign);
  await expect(page.getByText("Pendiente de reintento")).toBeVisible();
  await capture(page, "26-retry.png");

  await fakeControl(page, { gmailSend: "400" });
  await page.waitForTimeout(1_100);
  await runWorkerOnce(page);
  await openResults(page, errorCampaign);
  await expect(page.getByText("Fallido permanentemente")).toBeVisible();
  await capture(page, "27-error-permanente.png");

  await fakeControl(page, { gmailSend: "ambiguous-cut" });
  await page.waitForTimeout(1_100);
  await runWorkerOnce(page);
  await openResults(page, errorCampaign);
  await expect(page.getByRole("cell", { name: "Resultado ambiguo" })).toBeVisible();
  await capture(page, "28-resultado-ambiguo.png");

  const stateAfterUnsubscribe = await fakeState(page);
  expect(stateAfterUnsubscribe.gmailRequests.filter((request) => request.recipientHash === unsubscribedRecipientHash)).toHaveLength(1);

  await fakeControl(page, { gmailSend: "success" });
  await page.getByRole("button", { name: "Campañas" }).click();
  await createLiveDraft(page, scheduledCampaign, templateName);
  await freezeAndApprove(page, scheduledCampaign);
  const scheduledCard = campaignCard(page, scheduledCampaign);
  const scheduledAt = new Date(Date.now() + 5 * 60_000).toISOString().slice(0, 16);
  await scheduledCard.getByLabel(`Fecha y hora para ${scheduledCampaign}`).fill(scheduledAt);
  await scheduledCard.getByRole("button", { name: "Programar campaña real" }).click();
  const scheduleDialog = page.getByRole("dialog", { name: "Confirmar programación real" });
  await scheduleDialog.getByLabel("Escribí SEND_LIVE_CAMPAIGN para confirmar").fill("SEND_LIVE_CAMPAIGN");
  await scheduleDialog.getByRole("button", { name: "Programar campaña real" }).click();
  await expect(scheduledCard).toContainText("Programada");
  await capture(page, "20-campana-programada.png");

  await page.getByRole("button", { name: "Auditoría" }).click();
  await expect(page.getByRole("heading", { name: "Eventos recientes" })).toBeVisible();
  await capture(page, "30-auditoria.png");

  await createUser(page, salesUsername, salesPassword);
  await page.getByRole("button", { name: "Cerrar sesión" }).click();
  await login(page, salesUsername, salesPassword);
  await page.getByRole("button", { name: "Campañas" }).click();
  await expect(page.getByRole("button", { name: "Iniciar campaña real" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Programar campaña real" })).toHaveCount(0);
  await capture(page, "32-falta-permisos.png");

  await page.getByRole("button", { name: "Cerrar sesión" }).click();
  await login(page);
  await page.getByRole("button", { name: "Configuración" }).click();
  expect(sqlScalar(`
    SELECT count(*) FROM integration_connection
    WHERE organization_id = '${organizationId}' AND provider = 'GMAIL'
      AND encrypted_credential IS NOT NULL AND credential_nonce IS NOT NULL
      AND octet_length(credential_nonce) = 12 AND credential_key_id = 'v1'
      AND position(convert_to('refresh_', 'UTF8') in encrypted_credential) = 0;
  `)).toBe("1");
  await page.getByRole("button", { name: "Revocar" }).click();
  const revokeDialog = page.getByRole("dialog", { name: "Revocar cuenta remitente" });
  await revokeDialog.getByRole("button", { name: "Revocar" }).click();
  await expect(page.getByText("Cuenta remitente revocada.")).toBeVisible();
  await expect(page.getByText("Revocada", { exact: true })).toBeVisible();
  expect(sqlScalar(`
    SELECT count(*) FROM integration_connection
    WHERE organization_id = '${organizationId}' AND provider = 'GMAIL' AND status = 'REVOKED'
      AND encrypted_credential IS NULL AND credential_nonce IS NULL AND credential_key_id IS NULL;
  `)).toBe("1");
  await capture(page, "08-revocacion.png");
  await page.getByRole("button", { name: "Campañas" }).click();
  await expect(
    page.getByLabel("Cuenta remitente").locator("option").filter({ hasText: "campaigns@gestudio.test" }),
  ).toHaveCount(0);

  setPersistentSendingPolicy(false);
});
