import { expect, test, type Page } from "@playwright/test";
import type { CrmTag, OrganizationSettings } from "../src/types";

function holdRequest() {
  let release!: () => void;
  let started!: () => void;
  return {
    wait: new Promise<void>((resolve) => { release = resolve; }),
    started: new Promise<void>((resolve) => { started = resolve; }),
    release: () => release(), notifyStarted: () => started(),
  };
}

async function syntheticSettings(page: Page) {
  let settings: OrganizationSettings = {
    version: 0, name: "Organización sintética", timezone: "America/Argentina/Buenos_Aires",
    currency: "ARS", locale: "es-AR", brandingPrimaryColor: "#2563EB", followUpDays: 7,
    operatingWindowStart: "09:30", operatingWindowEnd: "17:30", businessDays: [1, 2, 3, 4, 5],
    campaignDailyLimit: 0, sendingOverrideRejected: false,
    sending: { environmentEnabled: false, environmentDryRun: true, environmentDailyLimit: 0,
      environmentKillSwitch: true, databaseEnabled: "false", databaseDryRun: "true",
      databaseDailyLimit: 0, databaseKillSwitch: "true" },
  };
  let tags: CrmTag[] = [{ id: "synthetic-tag", version: 0, name: "Etiqueta sintética", color: "#2563EB",
    active: true, usageCount: 0, createdAt: "2026-10-22T12:00:00Z", updatedAt: "2026-10-22T12:00:00Z" }];
  const emptyPage = { content: [], totalElements: 0, totalPages: 0, number: 0, size: 25, first: true, last: true };
  const putVersions: number[] = [], deleteVersions: number[] = [], unexpected: string[] = [];
  let failTagPost = false, failSettingsGet = false;
  let heldPut: ReturnType<typeof holdRequest> | null = null;
  let heldGet: ReturnType<typeof holdRequest> | null = null;
  let heldDelete: ReturnType<typeof holdRequest> | null = null;
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request(), url = new URL(request.url()), method = request.method();
    if (url.pathname === "/api/v1/settings") {
      if (method === "PUT") {
        const body = request.postDataJSON(); putVersions.push(body.version);
        const held = heldPut; heldPut = null;
        if (held) { held.notifyStarted(); await held.wait; }
        if (body.version !== settings.version) return route.fulfill({ status: 409, json: { detail: "Versión sintética anterior" } });
        settings = { ...settings, ...body, version: settings.version + 1 };
      } else {
        const held = heldGet; heldGet = null;
        if (held) { held.notifyStarted(); await held.wait; }
        if (failSettingsGet) { failSettingsGet = false; return route.fulfill({ status: 503, json: { detail: "Fallo sintético de actualización" } }); }
      }
      return route.fulfill({ json: settings });
    }
    if (url.pathname === "/api/v1/tags") {
      if (method === "POST") {
        if (failTagPost) { failTagPost = false; return route.fulfill({ status: 503, json: { detail: "Fallo sintético al crear etiqueta" } }); }
        const body = request.postDataJSON();
        const created = { ...tags[0]!, id: "synthetic-created-tag", name: body.name, color: body.color };
        tags = [...tags, created]; return route.fulfill({ status: 201, json: created });
      }
      return route.fulfill({ json: tags });
    }
    if (url.pathname === "/api/v1/tags/synthetic-tag" && method === "DELETE") {
      const version = Number(url.searchParams.get("version")); deleteVersions.push(version);
      const held = heldDelete; heldDelete = null;
      if (held) { held.notifyStarted(); await held.wait; }
      if (version !== tags[0]!.version) return route.fulfill({ status: 409, json: { detail: "Versión sintética anterior" } });
      tags = [{ ...tags[0]!, version: version + 1, active: false }];
      return route.fulfill({ json: tags[0] });
    }
    const responses: Record<string, unknown> = {
      "/api/v1/auth/me": { userId: "synthetic-admin", organizationId: "synthetic-organization", username: "synthetic-admin", displayName: "Administrador sintético", role: "ADMIN", permissions: ["SETTINGS_MANAGE", "REPORT_READ"] },
      "/api/v1/auth/csrf": { token: "synthetic-csrf", headerName: "X-CSRF-TOKEN" },
      "/api/v1/prospects": emptyPage, "/api/v1/exclusions": emptyPage,
      "/api/v1/prospects/metrics": { total: 0, interested: 0, blocked: 0 },
      "/api/v1/opportunities": [], "/api/v1/tasks/page": emptyPage,
      "/api/v1/opportunities/metrics": { activeCount: 0, totalValue: 0, weightedValue: 0, stalledCount: 0, byStage: {} },
      "/api/v1/reports/dashboard": { from: "2026-10-01", to: "2026-10-31", timezone: "America/Argentina/Buenos_Aires", prospectsByStatus: {}, prospectsBySource: {}, prospectsByOwner: {}, prospectSummary: {}, tasks: {}, activitiesByChannel: {}, activityOutcomes: {}, opportunitiesByStage: {}, opportunityValues: [], opportunitySummary: {}, lossReasons: {}, campaigns: {}, outbox: {}, inbound: {}, operations: {} },
      "/api/v1/business-profile": { code: "SCALARIS", organizationName: "Organización sintética", tagline: "Prueba sintética", descriptor: "Prueba", website: "https://example.invalid", whatsappDisplay: "Sintético", whatsappUrl: "", coverage: "Sintética", serviceLines: [], operatingGuidance: [], manualCommunicationsOnly: true },
      "/api/v1/sender-accounts": [],
      "/api/v1/sender-accounts/configuration": { oauthConfigured: false, providerMode: "NOOP", realNetworkAllowed: false, requiredScope: "synthetic" },
    };
    if (url.pathname in responses) return route.fulfill({ json: responses[url.pathname] });
    unexpected.push(`${method} ${url.pathname}`);
    return route.fulfill({ status: 404, json: { detail: "Ruta no prevista por la prueba sintética" } });
  });
  await page.goto("/");
  await page.getByRole("button", { name: "Configuración", exact: true }).click();
  await expect(page.getByRole("button", { name: "Guardar configuración", exact: true })).toBeEnabled();
  return {
    putVersions, deleteVersions, unexpected,
    failTagPost: () => { failTagPost = true; }, failSettingsGet: () => { failSettingsGet = true; },
    holdPut: () => (heldPut = holdRequest()), holdGet: () => (heldGet = holdRequest()),
    holdDelete: () => (heldDelete = holdRequest()),
  };
}

test("Configuración conserva el bloqueo hasta recibir la nueva versión y terminar la recarga", async ({ page }) => {
  const api = await syntheticSettings(page);
  const save = page.getByRole("button", { name: "Guardar configuración", exact: true });
  const name = page.getByLabel("Nombre", { exact: true }).first();
  await name.fill("Primera actualización sintética");
  const put = api.holdPut(), refresh = api.holdGet();
  try {
    await save.click(); await put.started;
    await expect(save).toBeDisabled(); await expect(name).toBeDisabled();
    await save.evaluate((button) => button.closest("form")!.dispatchEvent(new Event("submit", { bubbles: true, cancelable: true })));
    expect(api.putVersions).toEqual([0]);
    put.release(); await refresh.started;
    await expect(save).toBeDisabled();
    await expect(page.getByText("Configuración actualizada; bloqueos de envío preservados.", { exact: true })).toHaveCount(0);
  } finally { put.release(); refresh.release(); }
  await expect(save).toBeEnabled();
  await expect(page.getByText("Configuración actualizada; bloqueos de envío preservados.", { exact: true })).toBeVisible();
  await name.fill("Segunda actualización sintética"); await save.click();
  await expect(save).toBeEnabled(); expect(api.putVersions).toEqual([0, 1]); expect(api.unexpected).toEqual([]);
});

test("Un POST fallido conserva el nombre y color de la etiqueta para reintentar", async ({ page }) => {
  const api = await syntheticSettings(page);
  const name = page.getByLabel("Nombre", { exact: true }).nth(1);
  const color = page.getByLabel("Color", { exact: true });
  await name.fill("Etiqueta a conservar"); await color.fill("#123456");
  api.failTagPost(); await page.getByRole("button", { name: "Crear", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText("No se pudo completar la acción. Revisá los datos e intentá nuevamente.");
  await expect(name).toHaveValue("Etiqueta a conservar"); await expect(color).toHaveValue("#123456");
  await page.getByRole("button", { name: "Crear", exact: true }).click();
  await expect(page.getByText("Etiqueta creada.", { exact: true })).toBeVisible();
  await expect(name).toHaveValue(""); expect(api.unexpected).toEqual([]);
});

test("Desactivar etiqueta bloquea desde la confirmación hasta actualizar el catálogo", async ({ page }) => {
  const api = await syntheticSettings(page);
  const save = page.getByRole("button", { name: "Guardar configuración", exact: true });
  const deactivate = page.getByRole("button", { name: "Desactivar", exact: true });
  await deactivate.click();
  await expect(save).toBeDisabled();
  await page.getByRole("dialog").getByRole("button", { name: "Cancelar", exact: true }).click();
  await expect(save).toBeEnabled(); await expect(deactivate).toBeFocused(); expect(api.deleteVersions).toEqual([]);
  const mutation = api.holdDelete(), refresh = api.holdGet();
  try {
    await deactivate.click();
    await page.getByRole("dialog").getByRole("button", { name: "Desactivar etiqueta", exact: true }).click();
    await mutation.started; await expect(deactivate).toBeDisabled(); await expect(save).toBeDisabled();
    mutation.release(); await refresh.started;
    await expect(save).toBeDisabled();
    await expect(page.getByText("Etiqueta desactivada sin borrar historial.", { exact: true })).toHaveCount(0);
  } finally { mutation.release(); refresh.release(); }
  await expect(save).toBeEnabled(); await expect(deactivate).toHaveCount(0);
  await expect(page.getByText("Etiqueta desactivada sin borrar historial.", { exact: true })).toBeVisible();
  expect(api.deleteVersions).toEqual([0]); expect(api.unexpected).toEqual([]);
});

test("La versión devuelta por guardar se conserva aunque falle la recarga posterior", async ({ page }) => {
  const api = await syntheticSettings(page);
  const save = page.getByRole("button", { name: "Guardar configuración", exact: true });
  const name = page.getByLabel("Nombre", { exact: true }).first();
  await name.fill("Guardado antes del fallo sintético"); api.failSettingsGet(); await save.click();
  await expect(page.getByRole("alert")).toContainText("No se pudo completar la acción. Revisá los datos e intentá nuevamente.");
  await expect(save).toBeEnabled(); await expect(name).toHaveValue("Guardado antes del fallo sintético");
  await name.fill("Guardado con la versión recibida"); await save.click();
  await expect(page.getByText("Configuración actualizada; bloqueos de envío preservados.", { exact: true })).toBeVisible();
  expect(api.putVersions).toEqual([0, 1]); expect(api.unexpected).toEqual([]);
});
