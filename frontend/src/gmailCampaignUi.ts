import type { CampaignProgress } from "./types";

const gmailCallbackMessages: Record<string, string> = {
  CONNECTED: "Cuenta de Google conectada correctamente.",
  GMAIL_OAUTH_STATE_INVALID: "La conexión no pudo validarse. Iniciá el proceso nuevamente.",
  GMAIL_OAUTH_STATE_EXPIRED: "La conexión venció. Iniciá el proceso nuevamente.",
  GMAIL_OAUTH_STATE_REPLAYED: "La conexión ya fue utilizada. Iniciá un proceso nuevo.",
  AUTHORIZATION_DENIED: "La autorización de Google fue cancelada o denegada.",
  CALLBACK_CODE_MISSING: "Google no devolvió una autorización utilizable.",
  GMAIL_REFRESH_TOKEN_MISSING: "Google no devolvió acceso offline. Reconectá la cuenta.",
  GOOGLE_INVALID_GRANT: "La cuenta requiere una nueva autorización de Google.",
  GOOGLE_INSUFFICIENT_SCOPE: "Google no otorgó el permiso requerido para enviar correo.",
  GOOGLE_INVALID_RESPONSE: "Google devolvió una respuesta inválida durante la conexión.",
  GOOGLE_REMOTE_FAILURE: "Google no pudo completar la conexión en este momento.",
  GOOGLE_AMBIGUOUS: "La conexión terminó con un resultado incierto. Iniciá el proceso nuevamente.",
  INVALID_CALLBACK_RESPONSE: "Google devolvió una respuesta de conexión inválida.",
  OAUTH_CALLBACK_FAILED: "No se pudo completar la conexión con Google.",
};

export type GmailCallbackNotice = {
  kind: "success" | "error";
  message: string;
};

export function gmailCallbackNotice(search: string): GmailCallbackNotice | null {
  const parameters = new URLSearchParams(search);
  const result = parameters.get("gmail");
  if (result === "connected") {
    return { kind: "success", message: gmailCallbackMessages.CONNECTED! };
  }
  if (result !== "error") return null;
  const code = (parameters.get("code") ?? "").toUpperCase();
  return {
    kind: "error",
    message: gmailCallbackMessages[code] ?? "No se pudo conectar la cuenta de Google.",
  };
}

export function urlWithoutGmailCallback(href: string): string {
  const url = new URL(href);
  for (const name of ["gmail", "code", "state", "scope"]) url.searchParams.delete(name);
  return `${url.pathname}${url.search}${url.hash}`;
}

export function safeAuthorizationUrl(value: string): string {
  const url = new URL(value);
  const loopback = ["127.0.0.1", "localhost", "::1", "[::1]"].includes(url.hostname);
  if (url.protocol !== "https:" && !(url.protocol === "http:" && loopback)) {
    throw new Error("El servidor devolvió una URL OAuth insegura.");
  }
  return url.toString();
}

export function zonedLocalDateTimeToIso(value: string, timeZone: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})$/.exec(value);
  if (!match) throw new Error("La fecha programada no es válida.");
  const target = Date.UTC(
    Number(match[1]),
    Number(match[2]) - 1,
    Number(match[3]),
    Number(match[4]),
    Number(match[5]),
  );
  let instant = target;
  for (let attempt = 0; attempt < 2; attempt += 1) {
    const offset = zonedOffset(instant, timeZone);
    instant = target - offset;
  }
  return new Date(instant).toISOString();
}

function zonedOffset(instant: number, timeZone: string): number {
  const parts = Object.fromEntries(
    new Intl.DateTimeFormat("en-CA", {
      timeZone,
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hourCycle: "h23",
    }).formatToParts(new Date(instant)).map((part) => [part.type, part.value]),
  );
  return Date.UTC(
    Number(parts.year),
    Number(parts.month) - 1,
    Number(parts.day),
    Number(parts.hour),
    Number(parts.minute),
    Number(parts.second),
  ) - instant;
}

export function campaignProgress(summary: CampaignProgress): number {
  if (summary.recipients <= 0) return 0;
  const finished =
    summary.acceptedByGmail +
    summary.skipped +
    summary.ambiguous +
    summary.failed +
    summary.cancelled;
  return Math.min(100, Math.max(0, Math.round((finished * 100) / summary.recipients)));
}
