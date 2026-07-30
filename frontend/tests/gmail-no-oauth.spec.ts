import { expect, test, type Page } from "@playwright/test";
import path from "node:path";

const adminUser = process.env.CRM_E2E_USERNAME ?? "gmail-e2e-admin";
const adminPassword = process.env.CRM_E2E_PASSWORD ?? "gmail-e2e-admin-password";
const screenshotDirectory = path.resolve(
  process.env.CRM_GMAIL_SCREENSHOTS_DIR ?? "../validation-output/gmail-campaign-e2e/screenshots",
);

test.use({ trace: "off", video: "off" });

async function login(page: Page) {
  await page.goto("/");
  await page.getByLabel("Usuario").fill(adminUser);
  await page.getByLabel("Contraseña").fill(adminPassword);
  await page.getByRole("button", { name: "Ingresar" }).click();
  await expect(page.getByRole("heading", { name: "Resumen comercial" })).toBeVisible();
}

async function capture(page: Page, file: string) {
  await page.screenshot({ path: path.join(screenshotDirectory, file), fullPage: true });
}

test("NOOP starts without Google credentials and remains fail-closed", async ({ page }) => {
  await login(page);
  await page.getByRole("button", { name: "Configuración" }).click();
  await expect(page.getByRole("heading", { name: "Cuentas remitentes" })).toBeVisible();
  await expect(page.getByText("OAuth de Google no está configurado en el servidor.")).toBeVisible();
  await expect(page.getByRole("button", { name: "Conectar cuenta de Google" })).toBeDisabled();
  await capture(page, "01-configuracion-sin-oauth.png");

  await expect(page.getByText("Protección de emergencia").first()).toBeVisible();
  await expect(page.getByText("Activa", { exact: true }).first()).toBeVisible();
  await expect(page.getByText("Envíos reales").first()).toBeVisible();
  await expect(page.getByText("Bloqueados", { exact: true }).first()).toBeVisible();
  await capture(page, "31-kill-switch.png");
});
