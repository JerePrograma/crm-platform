import { describe, expect, it } from "vitest";
import {
  campaignProgress,
  gmailCallbackNotice,
  safeAuthorizationUrl,
  urlWithoutGmailCallback,
  zonedLocalDateTimeToIso,
} from "./gmailCampaignUi";

describe("flujo visible de Gmail", () => {
  it("solo muestra resultados de callback permitidos", () => {
    expect(gmailCallbackNotice("?gmail=connected")).toEqual({
      kind: "success",
      message: "Cuenta de Google conectada correctamente.",
    });
    expect(gmailCallbackNotice("?gmail=error&code=GMAIL_OAUTH_STATE_EXPIRED")?.message).toContain("venció");
    expect(gmailCallbackNotice("?gmail=error&code=GOOGLE_INSUFFICIENT_SCOPE")?.message).toContain("permiso requerido");
    expect(gmailCallbackNotice("?gmail=error&code=<script>alert(1)</script>")?.message)
      .toBe("No se pudo conectar la cuenta de Google.");
    expect(gmailCallbackNotice("?gmail=unexpected")).toBeNull();
  });

  it("limpia parámetros OAuth sin perder filtros propios", () => {
    expect(
      urlWithoutGmailCallback(
        "https://crm.example.test/?q=Rosario&gmail=connected&code=safe&state=secret#top",
      ),
    ).toBe("/?q=Rosario#top");
  });

  it("acepta HTTPS y loopback de pruebas, pero rechaza redirecciones inseguras", () => {
    expect(safeAuthorizationUrl("https://accounts.google.com/o/oauth2/v2/auth?x=1"))
      .toContain("https://accounts.google.com/");
    expect(safeAuthorizationUrl("http://127.0.0.1:19090/oauth/authorize"))
      .toContain("http://127.0.0.1:19090/");
    expect(() => safeAuthorizationUrl("http://attacker.example/oauth")).toThrow("insegura");
    expect(() => safeAuthorizationUrl("javascript:alert(1)")).toThrow("insegura");
  });

  it("convierte la programación desde la zona de la campaña", () => {
    expect(zonedLocalDateTimeToIso("2026-07-29T09:30", "America/Argentina/Buenos_Aires"))
      .toBe("2026-07-29T12:30:00.000Z");
    expect(zonedLocalDateTimeToIso("2026-07-29T09:30", "UTC"))
      .toBe("2026-07-29T09:30:00.000Z");
    expect(() => zonedLocalDateTimeToIso("fecha-inválida", "UTC")).toThrow("no es válida");
  });
});

describe("progreso de campaña", () => {
  it("cuenta solo resultados terminales y acota el porcentaje", () => {
    expect(campaignProgress({
      campaignId: "campaign-1",
      status: "RUNNING",
      version: 3,
      recipients: 10,
      excluded: 2,
      pending: 2,
      acceptedByGmail: 3,
      skipped: 1,
      ambiguous: 1,
      failed: 1,
      cancelled: 0,
      acceptanceDisclaimer: "Aceptado no significa entregado",
    })).toBe(60);
    expect(campaignProgress({ campaignId: "campaign-2", status: "APPROVED", version: 1, recipients: 0, excluded: 0, pending: 0, acceptedByGmail: 0, skipped: 0, ambiguous: 0, failed: 0, cancelled: 0, acceptanceDisclaimer: "Aceptado no significa entregado" })).toBe(0);
  });
});
