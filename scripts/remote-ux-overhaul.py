#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, content: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")


def write_new(path: str, content: str) -> None:
    target = ROOT / path
    if target.exists():
        raise RuntimeError(f"Refusing to overwrite existing file: {path}")
    write(path, content)


def replace_once(path: str, old: str, new: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected one match in {path}, found {count}: {old[:100]!r}")
    write(path, text.replace(old, new, 1))


def replace_function(path: str, start: str, next_start: str, replacement: str) -> None:
    text = read(path)
    start_index = text.find(start)
    next_index = text.find(next_start, start_index + len(start))
    if start_index < 0 or next_index < 0:
        raise RuntimeError(f"Function markers not found in {path}: {start!r}, {next_start!r}")
    write(path, text[:start_index] + replacement.rstrip() + "\n\n" + text[next_index:])


write_new(
    "frontend/src/labels.ts",
    r'''import type {
  OpportunityStage,
  ProspectStatus,
} from "./types";

export const prospectStatusLabels: Record<ProspectStatus, string> = {
  NEW: "Nuevo",
  QUALIFYING: "En calificación",
  READY_TO_CONTACT: "Listo para contactar",
  FOLLOW_UP: "En seguimiento",
  DEMO_PROPOSED: "Demostración propuesta",
  DEMO_SCHEDULED: "Demostración programada",
  PROPOSAL: "Propuesta enviada",
  CUSTOMER: "Cliente",
  NEEDS_ENRICHMENT: "Necesita completar datos",
  READY_FOR_REVIEW: "Listo para revisión",
  APPROVED: "Aprobado",
  QUEUED: "En cola",
  CONTACTED: "Contactado",
  REPLIED: "Respondió",
  INTERESTED: "Interesado",
  QUALIFIED: "Calificado",
  TRIAL_PROPOSED: "Prueba propuesta",
  TRIAL_ACTIVE: "Prueba activa",
  QUOTED: "Cotizado",
  NEGOTIATION: "En negociación",
  WON: "Ganado",
  LOST: "Perdido",
  NO_RESPONSE: "Sin respuesta",
  BOUNCED: "Correo rechazado",
  UNSUBSCRIBED: "Solicitó la baja",
  DO_NOT_CONTACT: "No contactar",
  INVALID: "Datos inválidos",
  DUPLICATE: "Duplicado",
  ARCHIVED: "Archivado",
};

export const opportunityStageLabels: Record<OpportunityStage, string> = {
  QUALIFICATION: "Calificación",
  DISCOVERY: "Relevamiento",
  DEMO: "Demostración",
  PROPOSAL: "Propuesta",
  NEGOTIATION: "Negociación",
  WON: "Ganada",
  LOST: "Perdida",
};

const labels: Record<string, string> = {
  ...prospectStatusLabels,
  ...opportunityStageLabels,
  ELIGIBLE: "Contactable",
  EXCLUDED: "Excluido",
  CUSTOMER: "Cliente",
  LOW: "Baja",
  MEDIUM: "Media",
  HIGH: "Alta",
  URGENT: "Urgente",
  OPEN: "Abierta",
  IN_PROGRESS: "En curso",
  COMPLETED: "Completada",
  CANCELLED: "Cancelada",
  PENDING: "Pendiente",
  RUNNING: "En proceso",
  FAILED: "Fallida",
  ACCEPTED: "Aceptada",
  REJECTED: "Rechazada",
  REVIEW_REQUIRED: "Requiere revisión",
  DEFERRED: "Postergada",
  PROCESSING: "Procesando",
  SUCCEEDED: "Procesado correctamente",
  RETRY: "Pendiente de reintento",
  DEAD: "No procesado",
  BLOCKED: "Bloqueado",
  DRAFT: "Borrador",
  SIMULATED: "Simulada",
  SCHEDULED: "Programada",
  PAUSED: "Pausada",
  VALID: "Válido",
  MISSING_CHANNEL: "Sin canal disponible",
  INELIGIBLE: "No habilitado para contacto",
  EMAIL: "Correo electrónico",
  WHATSAPP: "WhatsApp",
  PHONE: "Teléfono",
  WEBSITE: "Sitio web",
  SOCIAL: "Red social",
  OTHER: "Otro",
  ADMIN: "Administrador",
  MANAGER: "Responsable comercial",
  SALES: "Operador comercial",
  VIEWER: "Solo lectura",
  UNKNOWN: "Sin definir",
  GRANTED: "Autorizado",
  DENIED: "No autorizado",
  PHONE_CALL: "Llamada",
  MEETING: "Reunión",
  DEMO: "Demostración",
  EMAIL_SENT_MANUALLY: "Correo registrado manualmente",
  WHATSAPP_SENT_MANUALLY: "WhatsApp registrado manualmente",
  STATUS: "Cambio de estado",
  NOTE: "Nota",
  ACTIVITY: "Actividad",
  TASK: "Tarea",
  AUDIT: "Auditoría",
  LINK_TO_EXISTING: "Vincular con el existente",
  MARK_NOT_DUPLICATE: "Confirmar que no es duplicado",
  CREATE_SEPARATE: "Crear como registro independiente",
  MERGE: "Fusionar registros",
  DEFER: "Resolver más tarde",
  REJECT_ROW: "Descartar esta fila",
  EXACT_EMAIL: "Mismo correo electrónico",
  EXACT_PHONE: "Mismo teléfono",
  NOMINAL: "Coincidencia por nombre",
  FAKE_INBOUND: "Recepción de prueba",
  QUARANTINED: "En revisión",
  ASSOCIATED: "Asociado",
  DISCARDED: "Descartado",
  NOOP: "Sin conexión real",
  DEEPLINK_ONLY: "Solo enlace manual",
  IMPLEMENTED_NOT_CONNECTED: "Disponible, sin conexión",
  MANUAL: "Carga manual",
  EXISTING_CONVERSATION: "Conversación existente",
  EXISTING_CUSTOMER: "Cliente existente",
  UNSUBSCRIBE_REQUEST: "Solicitó no recibir mensajes",
  NEGATIVE_REPLY: "Respuesta negativa",
  PERMANENT_BOUNCE: "Rebote permanente",
  INVALID_CONTACT: "Contacto inválido",
  IRRELEVANT_INSTITUTION: "Institución no pertinente",
};

export function labelFor(value: string | null | undefined): string {
  if (!value) return "Sin dato";
  const known = labels[value];
  if (known) return known;
  const words = value.replaceAll("_", " ").replaceAll("-", " ").trim().toLocaleLowerCase("es-AR");
  return words ? words.charAt(0).toLocaleUpperCase("es-AR") + words.slice(1) : "Sin dato";
}

export function friendlyErrorMessage(raw: string): string {
  const message = raw.toLocaleLowerCase("es-AR");
  if (message.includes("duplicate review was already resolved")) {
    return "Esta revisión ya fue resuelta. Actualiza la pantalla para ver su estado actual.";
  }
  if (message.includes("modified by another user") || message.includes("optimistic")) {
    return "El registro cambió mientras lo estabas revisando. Actualiza la pantalla antes de volver a intentarlo.";
  }
  if (message.includes("contact channel already exists")) {
    return "Ese canal de contacto ya está asociado a otro registro de la organización.";
  }
  if (message.includes("prospect not found") || message.includes("contact not found")) {
    return "El registro solicitado ya no está disponible. Actualiza la pantalla.";
  }
  if (message.includes("access denied") || message.includes("forbidden") || message.includes("http 403")) {
    return "No tienes permisos para realizar esta acción.";
  }
  if (message.includes("unauthorized") || message.includes("http 401")) {
    return "La sesión venció. Vuelve a ingresar para continuar.";
  }
  if (message.includes("network") || message.includes("failed to fetch")) {
    return "No se pudo comunicar con el servidor. Revisa la conexión y vuelve a intentarlo.";
  }
  if (/^[\p{L}\p{N} .,:;()/_-]+$/u.test(raw) && /\b(is|required|must|cannot|could not|already|invalid|failed)\b/i.test(raw)) {
    return "No se pudo completar la operación. Revisa los datos ingresados y vuelve a intentarlo.";
  }
  return raw || "Ocurrió un error inesperado. Vuelve a intentarlo.";
}

export function duplicateActionDescription(action: string): string {
  return {
    LINK_TO_EXISTING: "Usa el registro existente y conserva esta fila como evidencia de duplicado.",
    MARK_NOT_DUPLICATE: "Crea un prospecto nuevo con los datos importados y deja constancia de la revisión.",
    CREATE_SEPARATE: "Crea un registro independiente conservando los canales y datos importados válidos.",
    MERGE: "Conserva el candidato mostrado y archiva el registro que selecciones. La trazabilidad permanece disponible.",
    DEFER: "Mantiene la revisión pendiente para resolverla más adelante.",
    REJECT_ROW: "Descarta la fila de la importación sin borrar la evidencia almacenada.",
  }[action] ?? "Revisa las consecuencias antes de continuar.";
}

export function auditDescription(action: string, entityType: string): string {
  const actions: Record<string, string> = {
    PROSPECT_CREATED: "Se creó un prospecto",
    PROSPECT_UPDATED: "Se actualizaron los datos del prospecto",
    PROSPECT_STATUS_CHANGED: "Cambió el estado del prospecto",
    CONTACT_CREATED: "Se agregó un contacto",
    CONTACT_UPDATED: "Se actualizó un contacto",
    CONTACT_CHANNEL_CREATED: "Se agregó un canal de contacto",
    CONTACT_CHANNEL_UPDATED: "Se actualizó un canal de contacto",
    CONTACT_CHANNEL_REMOVED: "Se eliminó un canal de contacto",
    DUPLICATE_REVIEW_RESOLVED: "Se resolvió una revisión de duplicado",
    DUPLICATE_REVIEW_DEFERRED: "Se postergó una revisión de duplicado",
    USER_CREATED: "Se creó un usuario",
    USER_STATUS_CHANGED: "Cambió el estado de un usuario",
    EXCLUSION_CREATED: "Se creó una exclusión comercial",
  };
  return actions[action] ?? `${labelFor(action)} sobre ${labelFor(entityType)}`;
}

const sensitiveKey = /password|secret|token|cookie|authorization|credential|api.?key|session/i;

function redact(value: unknown): unknown {
  if (Array.isArray(value)) return value.map(redact);
  if (value && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value as Record<string, unknown>).map(([key, item]) => [
        key,
        sensitiveKey.test(key) ? "[dato oculto]" : redact(item),
      ]),
    );
  }
  return value;
}

export function safeTechnicalData(payload: string): string {
  try {
    return JSON.stringify(redact(JSON.parse(payload) as unknown), null, 2);
  } catch {
    return "El detalle técnico no tiene un formato estructurado disponible.";
  }
}

export type SourceDataFields = {
  institution: string | null;
  locality: string | null;
  province: string | null;
  website: string | null;
  email: string | null;
  phone: string | null;
  source: string | null;
};

function normalizeKey(value: string): string {
  return value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLocaleLowerCase("es-AR").trim();
}

export function sourceDataFields(raw: string): SourceDataFields {
  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>;
    const values = Object.fromEntries(
      Object.entries(parsed).map(([key, value]) => [normalizeKey(key), typeof value === "string" ? value.trim() || null : null]),
    ) as Record<string, string | null>;
    const first = (...keys: string[]) => keys.map((key) => values[normalizeKey(key)]).find(Boolean) ?? null;
    return {
      institution: first("institucion", "razon social"),
      locality: first("localidad", "ciudad"),
      province: first("provincia"),
      website: first("sitio web", "website"),
      email: first("correo publicado", "correo", "email"),
      phone: first("telefono whatsapp", "telefono o whatsapp", "telefono", "whatsapp"),
      source: first("fuente"),
    };
  } catch {
    return { institution: null, locality: null, province: null, website: null, email: null, phone: null, source: null };
  }
}
''',
)

write_new(
    "frontend/src/dialog.tsx",
    r'''import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type FormEvent,
  type ReactNode,
} from "react";

type DialogOption = { value: string; label: string };
type DialogRequest = {
  title: string;
  description: string;
  confirmLabel?: string;
  cancelLabel?: string;
  danger?: boolean;
  input?: {
    label: string;
    initialValue?: string;
    placeholder?: string;
    options?: DialogOption[];
  };
};
type DialogResult = { confirmed: boolean; value?: string };
type PendingDialog = DialogRequest & { resolve: (result: DialogResult) => void };

type DialogApi = {
  confirm: (request: DialogRequest) => Promise<boolean>;
  prompt: (request: DialogRequest & { input: NonNullable<DialogRequest["input"]> }) => Promise<string | null>;
  select: (request: DialogRequest & { input: NonNullable<DialogRequest["input"]> & { options: DialogOption[] } }) => Promise<string | null>;
};

const DialogContext = createContext<DialogApi | null>(null);

export function DialogProvider({ children }: { children: ReactNode }) {
  const [pending, setPending] = useState<PendingDialog | null>(null);
  const [value, setValue] = useState("");
  const dialogRef = useRef<HTMLDivElement>(null);
  const primaryRef = useRef<HTMLButtonElement>(null);
  const previousFocus = useRef<HTMLElement | null>(null);

  const request = useCallback(
    (options: DialogRequest) =>
      new Promise<DialogResult>((resolve) => {
        previousFocus.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
        setValue(options.input?.initialValue ?? options.input?.options?.[0]?.value ?? "");
        setPending({ ...options, resolve });
      }),
    [],
  );

  useEffect(() => {
    if (!pending) return;
    primaryRef.current?.focus();
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault();
        finish({ confirmed: false });
        return;
      }
      if (event.key !== "Tab" || !dialogRef.current) return;
      const controls = Array.from(
        dialogRef.current.querySelectorAll<HTMLElement>(
          'button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
        ),
      );
      if (controls.length === 0) return;
      const first = controls[0];
      const last = controls[controls.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [pending]);

  function finish(result: DialogResult) {
    const current = pending;
    if (!current) return;
    setPending(null);
    current.resolve(result);
    queueMicrotask(() => previousFocus.current?.focus());
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    if (pending?.input && !value.trim()) return;
    finish({ confirmed: true, value: pending?.input ? value.trim() : undefined });
  }

  const api: DialogApi = {
    confirm: async (options) => (await request(options)).confirmed,
    prompt: async (options) => {
      const result = await request(options);
      return result.confirmed ? result.value ?? "" : null;
    },
    select: async (options) => {
      const result = await request(options);
      return result.confirmed ? result.value ?? "" : null;
    },
  };

  return (
    <DialogContext.Provider value={api}>
      {children}
      {pending && (
        <div className="dialog-backdrop" onMouseDown={(event) => event.target === event.currentTarget && finish({ confirmed: false })}>
          <div
            ref={dialogRef}
            className="dialog-card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="dialog-title"
            aria-describedby="dialog-description"
          >
            <form onSubmit={submit}>
              <header>
                <h2 id="dialog-title">{pending.title}</h2>
                <p id="dialog-description">{pending.description}</p>
              </header>
              {pending.input && (
                <label>
                  {pending.input.label}
                  {pending.input.options ? (
                    <select value={value} onChange={(event) => setValue(event.target.value)} autoFocus>
                      {pending.input.options.map((option) => (
                        <option key={option.value} value={option.value}>{option.label}</option>
                      ))}
                    </select>
                  ) : (
                    <input
                      value={value}
                      onChange={(event) => setValue(event.target.value)}
                      placeholder={pending.input.placeholder}
                      autoFocus
                      required
                    />
                  )}
                </label>
              )}
              <div className="dialog-actions">
                <button type="button" className="secondary-button" onClick={() => finish({ confirmed: false })}>
                  {pending.cancelLabel ?? "Cancelar"}
                </button>
                <button
                  ref={primaryRef}
                  className={pending.danger ? "danger-button" : "primary-button"}
                  disabled={Boolean(pending.input && !value.trim())}
                  type="submit"
                >
                  {pending.confirmLabel ?? "Confirmar"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </DialogContext.Provider>
  );
}

export function useDialog(): DialogApi {
  const dialog = useContext(DialogContext);
  if (!dialog) throw new Error("useDialog debe utilizarse dentro de DialogProvider");
  return dialog;
}
''',
)

write_new(
    "frontend/src/labels.test.ts",
    r'''import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";
import {
  friendlyErrorMessage,
  labelFor,
  safeTechnicalData,
  sourceDataFields,
} from "./labels";

describe("etiquetas visibles", () => {
  it("traduce estados técnicos importantes", () => {
    expect(labelFor("REVIEW_REQUIRED")).toBe("Requiere revisión");
    expect(labelFor("DO_NOT_CONTACT")).toBe("No contactar");
    expect(labelFor("QUALIFICATION")).toBe("Calificación");
  });

  it("presenta errores operativos comprensibles", () => {
    expect(friendlyErrorMessage("Duplicate review was already resolved")).toContain("ya fue resuelta");
  });

  it("oculta claves sensibles en datos técnicos", () => {
    const safe = safeTechnicalData('{"token":"abc","status":"PENDING"}');
    expect(safe).not.toContain("abc");
    expect(safe).toContain("dato oculto");
  });

  it("extrae un resumen controlado de una fila importada", () => {
    expect(sourceDataFields('{"institucion":"Flores","correo publicado":"hola@example.com"}')).toMatchObject({
      institution: "Flores",
      email: "hola@example.com",
    });
  });

  it("no utiliza diálogos nativos del navegador", () => {
    const source = readFileSync(new URL("./App.tsx", import.meta.url), "utf8");
    expect(source).not.toContain("window.prompt");
    expect(source).not.toContain("window.confirm");
  });
});
''',
)

write_new(
    "backend/src/main/java/com/gestudio/crm/deduplication/ImportedProspectData.java",
    r'''package com.gestudio.crm.deduplication;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

record ImportedProspectData(
    String institutionName,
    String category,
    String locality,
    String province,
    String country,
    String website,
    String contactName,
    String contactRole,
    String email,
    String phone,
    String whatsapp,
    String externalId,
    String source,
    String evidence,
    Integer priority,
    String administrativePain,
    Instant verifiedAt) {

  static ImportedProspectData extract(
      String rawData, String normalizedEmail, String normalizedPhone, ObjectMapper objectMapper) {
    Objects.requireNonNull(objectMapper, "Object mapper is required");
    Map<String, String> values = parse(rawData, objectMapper);
    String explicitWhatsapp =
        first(values, "whatsapp", "telefono whatsapp", "telefono o whatsapp");
    String explicitPhone = first(values, "telefono");
    String whatsapp = explicitWhatsapp;
    String phone = explicitWhatsapp == null ? explicitPhone : null;
    if (whatsapp == null && phone == null) {
      whatsapp = trim(normalizedPhone);
    }
    String evidence =
        join(
            first(values, "validacion publicada"),
            first(values, "observaciones"),
            first(values, "observacion de control"));
    return new ImportedProspectData(
        first(values, "institucion", "razon social"),
        first(values, "categoria"),
        first(values, "localidad", "ciudad"),
        first(values, "provincia"),
        valueOr(first(values, "pais"), "Argentina"),
        first(values, "sitio web", "website"),
        first(values, "contacto", "nombre de contacto"),
        valueOr(first(values, "cargo", "rol de contacto"), "Contacto publicado"),
        valueOr(first(values, "correo publicado", "correo", "email"), trim(normalizedEmail)),
        phone,
        whatsapp,
        first(values, "id", "identificador externo"),
        valueOr(first(values, "fuente"), "DUPLICATE_REVIEW"),
        valueOr(evidence, "Creado desde evidencia validada de una revisión de duplicados"),
        priority(first(values, "prioridad")),
        first(values, "motivo de encaje"),
        instant(first(values, "fecha de verificacion")));
  }

  private static Map<String, String> parse(String rawData, ObjectMapper objectMapper) {
    if (rawData == null || rawData.isBlank()) {
      return Map.of();
    }
    try {
      Object decoded = objectMapper.readValue(rawData, Object.class);
      if (!(decoded instanceof Map<?, ?> source)) {
        throw new IllegalArgumentException("Import row evidence must be a JSON object");
      }
      Map<String, String> result = new LinkedHashMap<>();
      source.forEach(
          (key, value) -> {
            if (key instanceof String textKey
                && (value == null || value instanceof String || value instanceof Number)) {
              String textValue = value == null ? null : trim(String.valueOf(value));
              result.put(normalizeKey(textKey), textValue);
            }
          });
      return Map.copyOf(result);
    } catch (JacksonException exception) {
      throw new IllegalArgumentException("Import row evidence is not valid JSON", exception);
    }
  }

  private static String normalizeKey(String value) {
    String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
    return normalized.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
  }

  private static String first(Map<String, String> values, String... keys) {
    for (String key : keys) {
      String value = trim(values.get(normalizeKey(key)));
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private static String valueOr(String value, String fallback) {
    return trim(value) == null ? fallback : value.trim();
  }

  private static String trim(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static String join(String... values) {
    String result =
        java.util.Arrays.stream(values)
            .map(ImportedProspectData::trim)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.joining(" | "));
    return result.isBlank() ? null : result;
  }

  private static Integer priority(String value) {
    String normalized = trim(value);
    if (normalized == null) return null;
    return switch (normalizeKey(normalized)) {
      case "alta" -> 1;
      case "media" -> 2;
      case "baja" -> 3;
      default -> parsePriority(normalized);
    };
  }

  private static Integer parsePriority(String value) {
    try {
      int priority = Integer.parseInt(value);
      return priority >= 0 && priority <= 5 ? priority : null;
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private static Instant instant(String value) {
    String normalized = trim(value);
    if (normalized == null) return null;
    try {
      return Instant.parse(normalized);
    } catch (DateTimeParseException ignored) {
      // Try the accepted date-only formats below.
    }
    for (DateTimeFormatter formatter :
        List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/uuuu"),
            DateTimeFormatter.ofPattern("d-M-uuuu"))) {
      try {
        return LocalDate.parse(normalized, formatter).atStartOfDay().toInstant(ZoneOffset.UTC);
      } catch (DateTimeParseException ignored) {
        // Try the next controlled format.
      }
    }
    return null;
  }
}
''',
)

write_new(
    "backend/src/test/java/com/gestudio/crm/deduplication/ImportedProspectDataTest.java",
    r'''package com.gestudio.crm.deduplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ImportedProspectDataTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void extractsOnlySupportedImportedProspectFields() {
    ImportedProspectData data =
        ImportedProspectData.extract(
            """
            {
              "institucion": "Academia Flores",
              "localidad": "Palermo",
              "provincia": "Buenos Aires",
              "categoria": "Danza",
              "sitio web": "https://example.test",
              "correo publicado": "contacto@example.test",
              "whatsapp": "+54 9 11 5555 1111",
              "fuente": "Directorio público",
              "id": "EXT-100",
              "prioridad": "Alta",
              "motivo de encaje": "Administración manual",
              "fecha de verificacion": "2026-07-20",
              "observaciones": "Dato publicado",
              "password": "must-not-be-used"
            }
            """,
            null,
            null,
            objectMapper);

    assertThat(data.institutionName()).isEqualTo("Academia Flores");
    assertThat(data.locality()).isEqualTo("Palermo");
    assertThat(data.email()).isEqualTo("contacto@example.test");
    assertThat(data.whatsapp()).isEqualTo("+54 9 11 5555 1111");
    assertThat(data.externalId()).isEqualTo("EXT-100");
    assertThat(data.priority()).isEqualTo(1);
    assertThat(data.verifiedAt()).isEqualTo(Instant.parse("2026-07-20T00:00:00Z"));
    assertThat(data.evidence()).contains("Dato publicado");
  }

  @Test
  void usesNormalizedChannelsAsControlledFallbacks() {
    ImportedProspectData data =
        ImportedProspectData.extract(
            "{\"institucion\":\"Academia\"}",
            "correo@example.test",
            "+5491155550000",
            objectMapper);

    assertThat(data.email()).isEqualTo("correo@example.test");
    assertThat(data.whatsapp()).isEqualTo("+5491155550000");
    assertThat(data.phone()).isNull();
  }

  @Test
  void rejectsMalformedEvidenceInsteadOfPersistingUnknownData() {
    assertThatThrownBy(() -> ImportedProspectData.extract("not-json", null, null, objectMapper))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("valid JSON");
  }
}
''',
)

# Backend: preserve controlled imported data during separate duplicate resolution.
replace_once(
    "backend/src/main/java/com/gestudio/crm/deduplication/DuplicateResolutionService.java",
    "import org.springframework.transaction.annotation.Transactional;\n",
    "import org.springframework.transaction.annotation.Transactional;\nimport tools.jackson.databind.ObjectMapper;\n",
)
replace_once(
    "backend/src/main/java/com/gestudio/crm/deduplication/DuplicateResolutionService.java",
    "  private final AuditEventWriter auditEventWriter;\n",
    "  private final AuditEventWriter auditEventWriter;\n  private final ObjectMapper objectMapper;\n",
)
replace_once(
    "backend/src/main/java/com/gestudio/crm/deduplication/DuplicateResolutionService.java",
    "      CurrentActor currentActor,\n      AuditEventWriter auditEventWriter) {\n",
    "      CurrentActor currentActor,\n      AuditEventWriter auditEventWriter,\n      ObjectMapper objectMapper) {\n",
)
replace_once(
    "backend/src/main/java/com/gestudio/crm/deduplication/DuplicateResolutionService.java",
    "    this.auditEventWriter = auditEventWriter;\n",
    "    this.auditEventWriter = auditEventWriter;\n    this.objectMapper = objectMapper;\n",
)
service_path = "backend/src/main/java/com/gestudio/crm/deduplication/DuplicateResolutionService.java"
service = read(service_path)
start = service.index("  private UUID createSeparate(ReviewRow review, String name) {")
end = service.index("  private void merge(", start)
new_create_separate = r'''  private UUID createSeparate(ReviewRow review, String name) {
    ImportedProspectData imported =
        ImportedProspectData.extract(
            review.view().sourceData(),
            review.view().normalizedEmail(),
            review.view().normalizedPhone(),
            objectMapper);
    String sourceId =
        imported.externalId() == null ? "duplicate-review:" + review.id() : imported.externalId();
    UUID prospectId =
        prospectApplicationService
            .create(
                new CreateProspectCommand(
                    name,
                    imported.category(),
                    imported.locality(),
                    imported.province(),
                    imported.country(),
                    imported.website(),
                    imported.contactName(),
                    imported.contactRole(),
                    imported.email(),
                    imported.phone(),
                    imported.whatsapp(),
                    sourceId,
                    imported.source(),
                    imported.evidence(),
                    null,
                    imported.priority(),
                    null,
                    null,
                    imported.administrativePain(),
                    imported.verifiedAt(),
                    null))
            .id();
    entityManager.flush();
    jdbcTemplate.update(
        "UPDATE import_row SET prospect_id = ?, status = 'ACCEPTED', error_message = NULL, updated_at = now(), version = version + 1 WHERE id = ? AND organization_id = ?",
        prospectId,
        review.importRowId(),
        currentActor.organizationId());
    return prospectId;
  }

'''
write(service_path, service[:start] + new_create_separate + service[end:])

replace_once(
    "backend/src/main/java/com/gestudio/crm/prospect/ProspectApplicationService.java",
    "    boolean eligible = contactEligibilityService.evaluate(eligibilityCandidates).eligible();\n",
    "    boolean eligible =\n        !eligibilityCandidates.isEmpty()\n            && contactEligibilityService.evaluate(eligibilityCandidates).eligible();\n",
)
replace_once(
    "backend/src/main/java/com/gestudio/crm/imports/ProspectImportRowProcessor.java",
    "    return contactEligibilityService.evaluate(channels).eligible();\n",
    "    return !channels.isEmpty() && contactEligibilityService.evaluate(channels).eligible();\n",
)

# Keep prospect contactability synchronized with valid channels without overriding explicit exclusions.
contact_path = "backend/src/main/java/com/gestudio/crm/contact/ContactOperationsService.java"
replace_once(
    contact_path,
    "    auditEventWriter.record(\n        \"CONTACT_CREATED\", \"Contact\", contactId, Map.of(\"prospectId\", prospectId));\n",
    "    refreshProspectEligibility(prospectId);\n    auditEventWriter.record(\n        \"CONTACT_CREATED\", \"Contact\", contactId, Map.of(\"prospectId\", prospectId));\n",
)
replace_once(
    contact_path,
    "  public void delete(UUID contactId, long version) {\n    find(contactId);\n",
    "  public void delete(UUID contactId, long version) {\n    find(contactId);\n    UUID prospectId = prospectForContact(contactId);\n",
)
replace_once(
    contact_path,
    "    auditEventWriter.record(\"CONTACT_REMOVED\", \"Contact\", contactId, Map.of());\n",
    "    refreshProspectEligibility(prospectId);\n    auditEventWriter.record(\"CONTACT_REMOVED\", \"Contact\", contactId, Map.of());\n",
)
replace_once(
    contact_path,
    "    createChannel(prospectId, contactId, command, false);\n    auditEventWriter.record(\n",
    "    createChannel(prospectId, contactId, command, false);\n    refreshProspectEligibility(prospectId);\n    auditEventWriter.record(\n",
)
replace_once(
    contact_path,
    "    applyExclusion(reference.prospectId(), command.type(), normalized);\n    auditEventWriter.record(\n",
    "    applyExclusion(reference.prospectId(), command.type(), normalized);\n    refreshProspectEligibility(reference.prospectId());\n    auditEventWriter.record(\n",
)
replace_once(
    contact_path,
    "    auditEventWriter.record(\n        \"CONTACT_CHANNEL_REMOVED\",\n",
    "    refreshProspectEligibility(reference.prospectId());\n    auditEventWriter.record(\n        \"CONTACT_CHANNEL_REMOVED\",\n",
)
replace_once(
    contact_path,
    "  private ContactView find(UUID contactId) {\n",
    r'''  private void refreshProspectEligibility(UUID prospectId) {
    List<ChannelCandidate> candidates =
        jdbcTemplate.query(
            """
            SELECT cc.type, cc.normalized_value
            FROM prospect p
            JOIN contact c ON c.institution_id = p.institution_id
              AND c.organization_id = p.organization_id AND c.deleted_at IS NULL
            JOIN contact_channel cc ON cc.contact_id = c.id
              AND cc.organization_id = c.organization_id
            WHERE p.id = ? AND p.organization_id = ? AND cc.valid = TRUE
              AND cc.consent <> 'DENIED'
            UNION ALL
            SELECT 'WEBSITE', i.website_domain
            FROM prospect p JOIN institution i ON i.id = p.institution_id
              AND i.organization_id = p.organization_id
            WHERE p.id = ? AND p.organization_id = ? AND i.website_domain IS NOT NULL
            """,
            (resultSet, rowNumber) ->
                new ChannelCandidate(
                    ContactChannelType.valueOf(resultSet.getString(1)), resultSet.getString(2)),
            prospectId,
            currentActor.organizationId(),
            prospectId,
            currentActor.organizationId());
    boolean contactable =
        !candidates.isEmpty() && eligibilityService.evaluate(candidates).eligible();
    jdbcTemplate.update(
        """
        UPDATE prospect
        SET contact_eligible = CASE
              WHEN eligibility = 'ELIGIBLE'
                AND status NOT IN ('DO_NOT_CONTACT', 'ARCHIVED', 'DUPLICATE') THEN ?
              ELSE FALSE
            END,
            updated_at = now(), updated_by = ?, version = version + 1
        WHERE id = ? AND organization_id = ?
          AND contact_eligible IS DISTINCT FROM CASE
              WHEN eligibility = 'ELIGIBLE'
                AND status NOT IN ('DO_NOT_CONTACT', 'ARCHIVED', 'DUPLICATE') THEN ?
              ELSE FALSE
            END
        """,
        contactable,
        currentActor.userIdOrNull(),
        prospectId,
        currentActor.organizationId(),
        contactable);
  }

  private ContactView find(UUID contactId) {
''',
)

# Frontend API: real pagination and complete contact channels.
api_path = "frontend/src/api.ts"
api = read(api_path)
start = api.index("export function listProspects(")
end = api.index("export function createProspect(", start)
new_list = r'''export function listProspects(
  status?: ProspectStatus,
  query?: string,
  page = 0,
  size = 25,
): Promise<Page<Prospect>> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort: query?.trim() ? "relevance,desc" : "createdAt,desc",
  });
  if (status) params.set("status", status);
  if (query?.trim()) params.set("query", query.trim());
  return request(`/api/v1/prospects?${params.toString()}`);
}

'''
api = api[:start] + new_list + api[end:]
start = api.index("export function createContact(")
end = api.index("export function createNote(", start)
new_contact = r'''export function createContact(
  prospectId: string,
  input: {
    firstName?: string;
    lastName?: string;
    role?: string;
    email?: string;
    phone?: string;
    whatsapp?: string;
    preferredChannel?: Contact["preferredChannel"];
    consent?: Contact["consent"];
    source?: string;
  },
): Promise<Contact> {
  const channels: Array<{
    type: Contact["channels"][number]["type"];
    value: string;
    primary: boolean;
    valid: boolean;
    verified: boolean;
    consent: Contact["consent"];
    preferred: boolean;
  }> = [];
  const add = (type: Contact["channels"][number]["type"], value?: string) => {
    if (!value?.trim()) return;
    channels.push({
      type,
      value: value.trim(),
      primary: channels.length === 0,
      valid: true,
      verified: false,
      consent: input.consent ?? "UNKNOWN",
      preferred: input.preferredChannel === type,
    });
  };
  add("EMAIL", input.email);
  add("PHONE", input.phone);
  add("WHATSAPP", input.whatsapp);
  return request(`/api/v1/prospects/${prospectId}/contacts`, {
    method: "POST",
    body: JSON.stringify({
      firstName: input.firstName,
      lastName: input.lastName,
      role: input.role,
      primary: true,
      verified: false,
      preferredChannel: input.preferredChannel ?? channels[0]?.type ?? null,
      consent: input.consent ?? "UNKNOWN",
      source: input.source ?? "MANUAL",
      channels,
    }),
  });
}

'''
write(api_path, api[:start] + new_contact + api[end:])

# Dialog provider.
replace_once(
    "frontend/src/main.tsx",
    'import { App } from "./App";\n',
    'import { App } from "./App";\nimport { DialogProvider } from "./dialog";\n',
)
replace_once(
    "frontend/src/main.tsx",
    "    <App />\n",
    "    <DialogProvider><App /></DialogProvider>\n",
)

app_path = "frontend/src/App.tsx"
replace_once(
    app_path,
    "  useMemo,\n  useState,\n",
    "  useMemo,\n  useState,\n",
)
replace_once(
    app_path,
    '} from "./types";\n',
    '} from "./types";\nimport { useDialog } from "./dialog";\nimport {\n  auditDescription,\n  duplicateActionDescription,\n  friendlyErrorMessage,\n  labelFor,\n  prospectStatusLabels,\n  safeTechnicalData,\n  sourceDataFields,\n} from "./labels";\n',
)
replace_once(
    app_path,
    '  const [tab, setTab] = useState<Tab>("dashboard");\n',
    '  const [tab, setTab] = useState<Tab>("dashboard");\n  const [menuOpen, setMenuOpen] = useState(false);\n',
)
replace_once(
    app_path,
    '  const [statusFilter, setStatusFilter] = useState<ProspectStatus | "">("");\n  const initialQuery = new URLSearchParams(window.location.search).get("q") ?? "";\n  const [searchInput, setSearchInput] = useState(initialQuery);\n  const [searchQuery, setSearchQuery] = useState(initialQuery);\n',
    '  const initialParameters = new URLSearchParams(window.location.search);\n  const initialQuery = initialParameters.get("q") ?? "";\n  const requestedStatus = initialParameters.get("status") as ProspectStatus | null;\n  const initialStatus = requestedStatus && prospectStatuses.includes(requestedStatus) ? requestedStatus : "";\n  const [statusFilter, setStatusFilter] = useState<ProspectStatus | "">(initialStatus);\n  const [searchInput, setSearchInput] = useState(initialQuery);\n  const [searchQuery, setSearchQuery] = useState(initialQuery);\n  const [prospectPageNumber, setProspectPageNumber] = useState(0);\n  const [prospectPageInfo, setProspectPageInfo] = useState({ totalElements: 0, totalPages: 0 });\n',
)
replace_once(
    app_path,
    '    async (filter: ProspectStatus | "" = statusFilter, query = searchQuery) => {\n',
    '    async (filter: ProspectStatus | "" = statusFilter, query = searchQuery, page = prospectPageNumber) => {\n',
)
replace_once(
    app_path,
    "          listProspects(filter || undefined, query || undefined),\n",
    "          listProspects(filter || undefined, query || undefined, page),\n",
)
replace_once(
    app_path,
    "        setProspects(prospectPage.content);\n",
    "        setProspects(prospectPage.content);\n        setProspectPageNumber(prospectPage.number);\n        setProspectPageInfo({ totalElements: prospectPage.totalElements, totalPages: prospectPage.totalPages });\n",
)
replace_once(
    app_path,
    "    [session, statusFilter, searchQuery],\n",
    "    [session, statusFilter, searchQuery, prospectPageNumber],\n",
)
replace_once(
    app_path,
    "  async function applyStatusFilter(value: ProspectStatus | \"\") {\n    setStatusFilter(value);\n    updateProspectUrl(searchQuery, value);\n    await refresh(value, searchQuery);\n  }\n",
    "  async function applyStatusFilter(value: ProspectStatus | \"\") {\n    setStatusFilter(value);\n    setProspectPageNumber(0);\n    updateProspectUrl(searchQuery, value);\n    await refresh(value, searchQuery, 0);\n  }\n",
)
replace_once(
    app_path,
    "    setSearchQuery(query);\n    updateProspectUrl(query, statusFilter);\n    await refresh(statusFilter, query);\n",
    "    setSearchQuery(query);\n    setProspectPageNumber(0);\n    updateProspectUrl(query, statusFilter);\n    await refresh(statusFilter, query, 0);\n",
)
replace_once(
    app_path,
    "  async function refreshView() {\n",
    '''  async function clearSearch() {
    setSearchInput("");
    setSearchQuery("");
    setProspectPageNumber(0);
    updateProspectUrl("", statusFilter);
    await refresh(statusFilter, "", 0);
  }

  async function changeProspectPage(page: number) {
    if (page < 0 || page >= prospectPageInfo.totalPages) return;
    setProspectPageNumber(page);
    await refresh(statusFilter, searchQuery, page);
  }

  function navigate(next: Tab) {
    setTab(next);
    setMenuOpen(false);
  }

  async function refreshView() {
''',
)

app = read(app_path)
app = app.replace('onClick={() => setTab("', 'onClick={() => navigate("')
app = app.replace('")}>\n', '")}>\n')
# The previous replacement already preserves the closing call; no global structural change is needed.
write(app_path, app)
replace_once(app_path, '<aside className="sidebar">', '<aside className={menuOpen ? "sidebar menu-open" : "sidebar"}>')
replace_once(
    app_path,
    "            <small>Operación comercial segura</small>\n          </div>\n        </div>\n",
    "            <small>Operación comercial segura</small>\n          </div>\n          <button className=\"menu-toggle\" aria-expanded={menuOpen} onClick={() => setMenuOpen((open) => !open)}>\n            {menuOpen ? \"Cerrar menú\" : \"Abrir menú\"}\n          </button>\n        </div>\n",
)
for old, new in {
    "Dashboard": "Resumen",
    "Outbox y workers": "Bandeja de salida",
    "Inbound y quarantine": "Mensajes recibidos",
}.items():
    app = read(app_path)
    write(app_path, app.replace(old, new))
replace_once(
    app_path,
    "          <strong>Envíos bloqueados</strong>\n          <span>enabled=false</span>\n          <span>dry-run=true</span>\n          <span>daily-limit=0</span>\n          <span>kill switch activo</span>\n",
    "          <strong>Los envíos reales están bloqueados</strong>\n          <span>Modo de simulación activo</span>\n          <span>Límite diario configurado en cero</span>\n          <span>Protección de emergencia activa</span>\n",
)
replace_once(
    app_path,
    "            <p>Fuente de verdad: PostgreSQL. Ningún envío real está disponible.</p>\n",
    "            <p>Consulta, organiza y continúa el trabajo comercial. Los envíos reales permanecen bloqueados.</p>\n",
)
replace_once(app_path, '{error && <div className="alert error">{error}</div>}', '{error && <div className="alert error" role="alert">{error}</div>}')
replace_once(app_path, '<Metric label="Prospectos visibles" value={prospects.length} />', '<Metric label="Prospectos" value={prospectPageInfo.totalElements || prospects.length} />')

# Replace the main prospect workspace with search guidance, keyboard access and pagination.
app = read(app_path)
start = app.index('        {tab === "prospects" && (')
end = app.index('        {tab === "pipeline" && (', start)
prospect_block = r'''        {tab === "prospects" && (
          <section className="two-column prospect-layout">
            <Panel title="Prospectos">
              {session.permissions.includes("PROSPECT_WRITE") && (
                <CreateProspectForm
                  onCreated={async (created) => {
                    await refresh(statusFilter, searchQuery, 0);
                    setSelectedProspect(created);
                  }}
                />
              )}
              <div className="toolbar prospect-toolbar">
                <form onSubmit={(event) => void applySearch(event)} className="inline-form search-form">
                  <label className="grow">
                    Buscar prospectos
                    <input
                      aria-label="Buscar prospectos"
                      value={searchInput}
                      onChange={(event) => setSearchInput(event.target.value)}
                      placeholder="Institución, contacto, correo, teléfono, localidad o etiqueta"
                    />
                  </label>
                  <button className="secondary-button" type="submit">Buscar</button>
                  {(searchInput || searchQuery) && (
                    <button className="link-button" type="button" onClick={() => void clearSearch()}>Limpiar búsqueda</button>
                  )}
                </form>
                <label>
                  Estado
                  <select
                    value={statusFilter}
                    onChange={(event) => void applyStatusFilter(event.target.value as ProspectStatus | "")}
                  >
                    <option value="">Todos los estados</option>
                    {prospectStatuses.map((status) => (
                      <option key={status} value={status}>{prospectStatusLabels[status]}</option>
                    ))}
                  </select>
                </label>
              </div>
              <div className="result-summary" aria-live="polite">
                <strong>{prospectPageInfo.totalElements}</strong> resultados
                {searchQuery && <span> para “{searchQuery}”</span>}
                {statusFilter && <span> · {prospectStatusLabels[statusFilter]}</span>}
              </div>
              {prospects.length === 0 && !loading ? (
                <EmptyState text={searchQuery || statusFilter ? "No encontramos prospectos con estos criterios. Revisa la escritura o quita los filtros." : "Todavía no hay prospectos cargados."} />
              ) : (
                <div className="table-scroll">
                  <table className="responsive-table selectable-table">
                    <thead><tr><th>Institución</th><th>Localidad</th><th>Estado</th><th>Contacto</th></tr></thead>
                    <tbody>
                      {prospects.map((prospect) => (
                        <tr
                          key={prospect.id}
                          tabIndex={0}
                          aria-label={`Abrir ficha de ${prospect.displayName}`}
                          className={selectedProspect?.id === prospect.id ? "selected" : undefined}
                          onClick={() => void selectProspect(prospect.id)}
                          onKeyDown={(event) => {
                            if (event.key === "Enter" || event.key === " ") {
                              event.preventDefault();
                              void selectProspect(prospect.id);
                            }
                          }}
                        >
                          <td data-label="Institución"><strong>{prospect.displayName}</strong></td>
                          <td data-label="Localidad">{prospect.city ?? "Sin localidad cargada"}</td>
                          <td data-label="Estado"><Badge value={prospect.status} /></td>
                          <td data-label="Contacto">{prospect.contactEligible ? "Canal disponible" : "Sin canal utilizable"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              {prospectPageInfo.totalPages > 1 && (
                <nav className="pagination" aria-label="Paginación de prospectos">
                  <button className="secondary-button" disabled={prospectPageNumber === 0} onClick={() => void changeProspectPage(prospectPageNumber - 1)}>Anterior</button>
                  <span>Página {prospectPageNumber + 1} de {prospectPageInfo.totalPages}</span>
                  <button className="secondary-button" disabled={prospectPageNumber + 1 >= prospectPageInfo.totalPages} onClick={() => void changeProspectPage(prospectPageNumber + 1)}>Siguiente</button>
                </nav>
              )}
            </Panel>
            <Panel title="Ficha integral">
              {selectedProspect ? (
                <ProspectDetail
                  prospect={selectedProspect}
                  session={session}
                  onChanged={async () => {
                    setSelectedProspect(await getProspect(selectedProspect.id));
                    await refresh(statusFilter, searchQuery, prospectPageNumber);
                  }}
                />
              ) : (
                <EmptyState text="Selecciona un prospecto para consultar sus datos, contactos y próximos pasos." />
              )}
            </Panel>
          </section>
        )}

'''
write(app_path, app[:start] + prospect_block + app[end:])
replace_once(
    app_path,
    "          <ImportsPanel\n            duplicateReviews={duplicateReviews}\n            onChanged={() => refresh()}\n          />\n",
    "          <ImportsPanel\n            duplicateReviews={duplicateReviews}\n            prospects={prospects}\n            onChanged={() => refresh(statusFilter, searchQuery, prospectPageNumber)}\n          />\n",
)
replace_once(app_path, '<Detail key={key} label={key} value={String(value)} />', '<Detail key={key} label={labelFor(key)} value={String(value)} />')

# Settings confirmations and human labels.
replace_once(app_path, '  const canWriteProspects = session.permissions.includes("PROSPECT_WRITE");\n', '  const canWriteProspects = session.permissions.includes("PROSPECT_WRITE");\n  const dialog = useDialog();\n')
for old, new in {
    '<Metric label="Sending enabled" value={settings.sending.environmentEnabled ? "true" : "false"} />': '<Metric label="Envíos reales" value={settings.sending.environmentEnabled ? "Habilitados" : "Bloqueados"} />',
    '<Metric label="Dry-run" value={settings.sending.environmentDryRun ? "true" : "false"} />': '<Metric label="Modo de operación" value={settings.sending.environmentDryRun ? "Simulación" : "Real"} />',
    '<Metric label="Kill switch" value={settings.sending.environmentKillSwitch ? "activo" : "inactivo"} />': '<Metric label="Protección de emergencia" value={settings.sending.environmentKillSwitch ? "Activa" : "Inactiva"} />',
    '<label>Timezone<input': '<label>Zona horaria<input',
}.items():
    replace_once(app_path, old, new)
old_tag = '{canManage && tag.active && <button className="link-button" onClick={() => window.confirm(`Desactivar ${tag.name}?`) && void run(() => deactivateTag(tag), "Etiqueta desactivada sin borrar historial.")}>Desactivar</button>}'
new_tag = '{canManage && tag.active && <button className="link-button" onClick={() => void dialog.confirm({ title: "Desactivar etiqueta", description: `La etiqueta ${tag.name} dejará de estar disponible para nuevas asignaciones. El historial se conservará.`, confirmLabel: "Desactivar", danger: true }).then((confirmed) => confirmed && run(() => deactivateTag(tag), "Etiqueta desactivada sin borrar historial."))}>Desactivar</button>}'
replace_once(app_path, old_tag, new_tag)

# Outbox dialog and terminology.
replace_once(app_path, '  const canOperate = session.permissions.includes("SETTINGS_MANAGE");\n\n  const refreshOutbox', '  const canOperate = session.permissions.includes("SETTINGS_MANAGE");\n  const dialog = useDialog();\n\n  const refreshOutbox')
replace_once(app_path, 'Los workers reevalúan los kill switches al procesar. No existe una acción para forzar\n        providers reales ni transformar un evento en SENT.', 'El proceso de salida vuelve a verificar todas las protecciones antes de actuar. No existe una acción para forzar proveedores reales ni marcar mensajes como enviados.')
replace_once(app_path, '<Metric key={metric.status} label={metric.status} value={metric.count} />', '<Metric key={metric.status} label={labelFor(metric.status)} value={metric.count} />')
replace_once(app_path, '<Panel title="Estado del worker">', '<Panel title="Estado del proceso de salida">')
replace_once(app_path, '<Control label="Scheduler" value={worker.worker.schedulerEnabled ? "Habilitado" : "Manual"} />', '<Control label="Ejecución automática" value={worker.worker.schedulerEnabled ? "Habilitada" : "Manual"} />')
replace_once(app_path, '<Control label="Tenant" value={worker.tenantPaused ? "Pausado" : "Activo"} />', '<Control label="Estado de la organización" value={worker.tenantPaused ? "Pausado" : "Activo"} />')
replace_once(app_path, '<Control label="Batch" value={String(worker.worker.batchSize)} />', '<Control label="Máximo por ejecución" value={String(worker.worker.batchSize)} />')
replace_once(app_path, '<td>{event.eventType}</td>', '<td>{labelFor(event.eventType)}</td>')
replace_once(app_path, '<Detail label="Correlation ID" value={selected.correlationId} />', '<Detail label="Identificador de seguimiento" value={selected.correlationId} />')
replace_once(app_path, '<Detail label="Aggregate" value={`${selected.aggregateType} / ${selected.aggregateId}`} />', '<Detail label="Registro relacionado" value={`${labelFor(selected.aggregateType)} / ${selected.aggregateId}`} />')
replace_once(app_path, '<pre className="preview-box" aria-label="Payload sanitizado">{selected.payload}</pre>', '<details className="technical-details"><summary>Ver datos técnicos</summary><pre className="preview-box">{safeTechnicalData(selected.payload)}</pre></details>')
old_requeue = '''                    <button className="primary-button" onClick={() => {
                      if (window.confirm("¿Reencolar este evento DEAD sin modificar su payload?")) {
                        void operation(() => requeueOutboxEvent(selected.id), "Evento reencolado.");
                      }
                    }}>Reencolar</button>'''
new_requeue = '''                    <button className="primary-button" onClick={() => void dialog.confirm({ title: "Reintentar procesamiento", description: "El mensaje volverá a la cola sin modificar sus datos. Las protecciones de envío se aplicarán nuevamente.", confirmLabel: "Reintentar" }).then((confirmed) => confirmed && operation(() => requeueOutboxEvent(selected.id), "Mensaje reencolado."))}>Reintentar</button>'''
replace_once(app_path, old_requeue, new_requeue)
old_cancel = '''                    <button className="secondary-button" onClick={() => {
                      if (window.confirm("¿Cancelar este evento pendiente?")) {
                        void operation(() => cancelOutboxEvent(selected.id), "Evento cancelado.");
                      }
                    }}>Cancelar</button>'''
new_cancel = '''                    <button className="secondary-button" onClick={() => void dialog.confirm({ title: "Cancelar mensaje pendiente", description: "El mensaje dejará de procesarse. La evidencia técnica permanecerá disponible.", confirmLabel: "Cancelar mensaje", danger: true }).then((confirmed) => confirmed && operation(() => cancelOutboxEvent(selected.id), "Mensaje cancelado."))}>Cancelar</button>'''
replace_once(app_path, old_cancel, new_cancel)
replace_once(app_path, '{["PENDING", "PROCESSING", "SUCCEEDED", "RETRY", "DEAD", "CANCELLED", "BLOCKED"].map((value) => (\n                  <option key={value}>{value}</option>\n                ))}', '{["PENDING", "PROCESSING", "SUCCEEDED", "RETRY", "DEAD", "CANCELLED", "BLOCKED"].map((value) => (\n                  <option key={value} value={value}>{labelFor(value)}</option>\n                ))}')

# Inbound dialog and language.
replace_once(app_path, '  const canOperate = session.permissions.includes("SETTINGS_MANAGE");\n\n  const refreshInbound', '  const canOperate = session.permissions.includes("SETTINGS_MANAGE");\n  const dialog = useDialog();\n\n  const refreshInbound')
replace_once(app_path, 'aria-label="Cargando inbound"', 'aria-label="Cargando mensajes recibidos"')
replace_once(app_path, '<Panel title="Webhook fake">', '<Panel title="Recepción de prueba">')
replace_once(app_path, '<Control label="Provider" value={health?.provider ?? "FAKE_INBOUND"} />', '<Control label="Origen" value={labelFor(health?.provider ?? "FAKE_INBOUND")} />')
replace_once(app_path, '<Panel title="Inbound y quarantine">', '<Panel title="Mensajes recibidos y pendientes de revisión">')
replace_once(app_path, '<EmptyState text="No hay mensajes inbound." />', '<EmptyState text="No hay mensajes recibidos." />')
replace_once(app_path, '<Detail label="Receipt" value={selected.id} />', '<Detail label="Identificador del mensaje" value={selected.id} />')
replace_once(app_path, '<Detail label="Correlation ID" value={selected.correlationId} />', '<Detail label="Identificador de seguimiento" value={selected.correlationId} />')
replace_once(app_path, '<Detail label="Asociación" value={selected.associationStatus} />', '<Detail label="Asociación" value={labelFor(selected.associationStatus)} />')
replace_once(app_path, '<Detail label="Motivo quarantine" value={selected.quarantineReason} />', '<Detail label="Motivo de revisión" value={selected.quarantineReason} />')
old_discard = '''                    onClick={() => {
                      if (window.confirm("¿Descartar lógicamente este receipt? La evidencia se conserva.")) {
                        void operation(() => discardInbound(selected.id, discardReason), "Receipt descartado lógicamente.");
                      }
                    }}
                  >Descartar con motivo</button>'''
new_discard = '''                    onClick={() => void dialog.confirm({ title: "Descartar mensaje recibido", description: "El mensaje dejará de estar pendiente, pero su evidencia se conservará para auditoría.", confirmLabel: "Descartar", danger: true }).then((confirmed) => confirmed && operation(() => discardInbound(selected.id, discardReason), "Mensaje descartado con su evidencia conservada."))}
                  >Descartar con motivo</button>'''
replace_once(app_path, old_discard, new_discard)
replace_once(app_path, '<EmptyState text="Seleccioná un receipt para ver metadata sanitizada." />', '<EmptyState text="Selecciona un mensaje para consultar su información sanitizada." />')

# Users dialog and visible roles.
replace_once(app_path, '  const [notice, setNotice] = useState<string | null>(null);\n\n  const refreshUsers', '  const [notice, setNotice] = useState<string | null>(null);\n  const dialog = useDialog();\n\n  const refreshUsers')
users_text = read(app_path)
start = users_text.index("  async function toggleUser(user: User) {")
end = users_text.index("\n\n  return (", start)
new_toggle = r'''  async function toggleUser(user: User) {
    const action = user.active ? "desactivar" : "activar";
    const confirmed = await dialog.confirm({
      title: `${user.active ? "Desactivar" : "Activar"} usuario`,
      description: user.active
        ? `${user.displayName} no podrá iniciar sesión hasta que vuelvas a activarlo.`
        : `${user.displayName} recuperará el acceso según los permisos de su rol.`,
      confirmLabel: user.active ? "Desactivar" : "Activar",
      danger: user.active,
    });
    if (!confirmed) return;
    setError(null);
    try {
      await setUserActive(user.id, !user.active);
      setNotice(`Usuario ${action === "activar" ? "activado" : "desactivado"}.`);
      await refreshUsers();
    } catch (caught) {
      setError(message(caught));
    }
  }'''
write(app_path, users_text[:start] + new_toggle + users_text[end:])
replace_once(app_path, '<td>{user.role}</td>', '<td>{labelFor(user.role)}</td>')
replace_once(app_path, '<Control label="Rol" value={session.role} />', '<Control label="Rol" value={labelFor(session.role)} />')
replace_once(app_path, '<Control label="Organización" value={session.organizationId} />', '<Control label="Organización" value="Organización actual" />')

# Messages and campaign terminology.
replace_once(app_path, '<option value="EMAIL">EMAIL</option>', '<option value="EMAIL">Correo electrónico</option>')
replace_once(app_path, '<option value="WHATSAPP">WHATSAPP</option>', '<option value="WHATSAPP">WhatsApp</option>')
replace_once(app_path, '{result.status} mediante {result.provider}. Bloqueo de envío: {result.sendingBlockReason}.', '{labelFor(result.status)} mediante {labelFor(result.provider)}. Motivo de bloqueo: {labelFor(result.sendingBlockReason)}.')
replace_once(app_path, '<Control label="Gmail OAuth" value="IMPLEMENTED_NOT_CONNECTED" />', '<Control label="Gmail" value={labelFor("IMPLEMENTED_NOT_CONNECTED")} />')
replace_once(app_path, '<Control label="WhatsApp Cloud" value="IMPLEMENTED_NOT_CONNECTED" />', '<Control label="WhatsApp Cloud" value={labelFor("IMPLEMENTED_NOT_CONNECTED")} />')
replace_once(app_path, '<Control label="Modo email" value={safety?.emailMode ?? "NOOP"} />', '<Control label="Modo de correo" value={labelFor(safety?.emailMode ?? "NOOP")} />')
replace_once(app_path, '<Control label="Modo WhatsApp" value={safety?.whatsAppMode ?? "DEEPLINK_ONLY"} />', '<Control label="Modo de WhatsApp" value={labelFor(safety?.whatsAppMode ?? "DEEPLINK_ONLY")} />')
replace_once(app_path, '  const writable = session.permissions.includes("CAMPAIGN_WRITE");\n', '  const writable = session.permissions.includes("CAMPAIGN_WRITE");\n  const dialog = useDialog();\n')
campaign_text = read(app_path)
start = campaign_text.index("  async function freeze(campaign: Campaign) {")
end = campaign_text.index("\n\n  async function approve", start)
new_freeze = r'''  async function freeze(campaign: Campaign) {
    const confirmed = await dialog.confirm({
      title: "Congelar audiencia",
      description: "Se guardará una copia de los destinatarios que cumplen los filtros actuales. Luego podrás revisarla antes de aprobar la simulación.",
      confirmLabel: "Congelar audiencia",
    });
    if (!confirmed) return;
    await run(async () => {
      const frozen = await freezeCampaignAudience(campaign, {
        province: province || undefined,
        scoreAtLeast: scoreAtLeast ? Number(scoreAtLeast) : undefined,
      });
      setAudience(await getCampaignAudience(frozen.id));
      setNotice(`Audiencia congelada: ${frozen.recipientCount} incluidos, ${frozen.excludedCount} excluidos.`);
      await onChanged();
    });
  }'''
campaign_text = campaign_text[:start] + new_freeze + campaign_text[end:]
start = campaign_text.index("  async function approve(campaign: Campaign) {")
end = campaign_text.index("\n\n  async function simulate", start)
new_approve = r'''  async function approve(campaign: Campaign) {
    const confirmed = await dialog.confirm({
      title: "Aprobar campaña para simulación",
      description: "La aprobación permite generar una simulación local. No habilita envíos reales ni conexión con proveedores externos.",
      confirmLabel: "Aprobar simulación",
    });
    if (!confirmed) return;
    await run(async () => {
      const approved = await approveCampaign(campaign);
      setNotice(`Campaña ${approved.name} aprobada solo para simulación.`);
      await onChanged();
    });
  }'''
write(app_path, campaign_text[:start] + new_approve + campaign_text[end:])
replace_once(app_path, '{template.name} · v{template.versionNumber} · {template.channel}', '{template.name} · v{template.versionNumber} · {labelFor(template.channel)}')
replace_once(app_path, '<p>{campaign.channel} · {campaign.templateName}</p>', '<p>{labelFor(campaign.channel)} · {campaign.templateName}</p>')
replace_once(app_path, '<small>{campaign.recipientCount} incluidos · {campaign.excludedCount} excluidos · dry-run</small>', '<small>{campaign.recipientCount} incluidos · {campaign.excludedCount} excluidos · modo de simulación</small>')
replace_once(app_path, '{simulation && <div className="alert success">Run fake {simulation.id}: ningún envío de red; {simulation.includedCount} actividades de borrador.</div>}', '{simulation && <div className="alert success">Simulación {simulation.id}: no se realizó ningún envío; se generaron {simulation.includedCount} borradores.</div>}')
replace_once(app_path, '<li key={step.id}><strong>{step.type}</strong><code>{JSON.stringify(step.configuration)}</code></li>', '<li key={step.id}><strong>{labelFor(step.type)}</strong><details className="technical-details"><summary>Ver configuración</summary><code>{JSON.stringify(step.configuration)}</code></details></li>')

# Pipeline uses readable stages and accessible reason dialog.
replace_once(app_path, '  const canWrite = session.permissions.includes("OPPORTUNITY_WRITE");\n', '  const canWrite = session.permissions.includes("OPPORTUNITY_WRITE");\n  const dialog = useDialog();\n',)
pipeline_text = read(app_path)
start = pipeline_text.index("  async function move(opportunity: Opportunity, stage: OpportunityStage) {")
end = pipeline_text.index("\n\n  return (", start)
new_move = r'''  async function move(opportunity: Opportunity, stage: OpportunityStage) {
    let reason: string | undefined;
    if (stage === "LOST" || stage === "WON") {
      const result = await dialog.prompt({
        title: stage === "LOST" ? "Registrar motivo de pérdida" : "Registrar motivo de cierre",
        description: "Este dato quedará en el historial de la oportunidad y ayudará a interpretar los reportes.",
        confirmLabel: "Guardar cambio",
        input: {
          label: stage === "LOST" ? "Motivo de pérdida" : "Motivo del cierre ganado",
          placeholder: "Describe el motivo en una frase breve",
        },
      });
      if (!result) return;
      reason = result;
    }
    setBusy(true);
    setError(null);
    try {
      await transitionOpportunity(opportunity.id, opportunity.version, stage, reason);
      await onChanged();
    } catch (caught) {
      setError(message(caught));
    } finally {
      setBusy(false);
    }
  }'''
write(app_path, pipeline_text[:start] + new_move + pipeline_text[end:])
replace_once(app_path, '<header><strong>{stage}</strong><span>{metrics?.byStage[stage] ?? 0}</span></header>', '<header><strong>{labelFor(stage)}</strong><span>{metrics?.byStage[stage] ?? 0}</span></header>')
replace_once(app_path, '                          {next}\n', '                          {labelFor(next)}\n')

# Replace imports and duplicate-review flow completely.
imports_function = r'''function ImportsPanel({
  duplicateReviews,
  prospects,
  onChanged,
}: {
  duplicateReviews: DuplicateReview[];
  prospects: Prospect[];
  onChanged: () => Promise<void>;
}) {
  const dialog = useDialog();
  const [file, setFile] = useState<File | null>(null);
  const [summary, setSummary] = useState<ImportSummary | null>(null);
  const [rows, setRows] = useState<ImportRow[]>([]);
  const [busy, setBusy] = useState(false);
  const [resolvingId, setResolvingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [previewKey, setPreviewKey] = useState<string | null>(null);
  const [rowStatus, setRowStatus] = useState<ImportRow["status"] | "">("");
  const [rowSheet, setRowSheet] = useState("");
  const [rowSearch, setRowSearch] = useState("");
  const [rowPage, setRowPage] = useState(0);
  const pageSize = 25;

  const currentFileKey = file ? `${file.name}:${file.size}:${file.lastModified}` : null;
  const previewReady = Boolean(summary?.dryRun && currentFileKey && previewKey === currentFileKey);
  const sheets = useMemo(() => [...new Set(rows.map((row) => row.sourceSheet))], [rows]);
  const filteredRows = useMemo(() => {
    const query = rowSearch.trim().toLocaleLowerCase("es-AR");
    return rows.filter((row) =>
      (!rowStatus || row.status === rowStatus)
      && (!rowSheet || row.sourceSheet === rowSheet)
      && (!query || [row.errorMessage, row.normalizedEmail, row.normalizedPhone, String(row.rowNumber)]
        .filter(Boolean)
        .some((value) => String(value).toLocaleLowerCase("es-AR").includes(query))),
    );
  }, [rowSearch, rowSheet, rowStatus, rows]);
  const totalRowPages = Math.ceil(filteredRows.length / pageSize);
  const visibleRows = filteredRows.slice(rowPage * pageSize, (rowPage + 1) * pageSize);

  async function run(execute: boolean) {
    if (!file) {
      setError("Selecciona un archivo CSV o XLSX.");
      return;
    }
    if (execute && !previewReady) {
      setError("Primero ejecuta la vista previa del archivo seleccionado.");
      return;
    }
    if (execute) {
      const confirmed = await dialog.confirm({
        title: "Ejecutar importación",
        description: `Se procesará ${file.name} con las decisiones mostradas en la vista previa. Las exclusiones seguirán teniendo prioridad y no se realizará ningún envío.`,
        confirmLabel: "Importar registros",
        danger: true,
      });
      if (!confirmed) return;
    }
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const result = await importProspects(file, execute);
      setSummary(result);
      setRows(await getImportRows(result.id));
      setRowPage(0);
      if (result.dryRun) {
        setPreviewKey(currentFileKey);
        setNotice("Vista previa completada. Revisa los resultados antes de importar.");
      } else {
        setPreviewKey(null);
        setNotice("Importación completada. Revisa el resumen y las filas que requieren atención.");
      }
      await onChanged();
    } catch (caught) {
      setError(message(caught));
    } finally {
      setBusy(false);
    }
  }

  async function resolve(review: DuplicateReview, action: DuplicateResolutionAction) {
    const source = sourceDataFields(review.sourceData);
    let separateName: string | undefined;
    let absorbedProspectId: string | undefined;
    if (action === "CREATE_SEPARATE" || action === "MARK_NOT_DUPLICATE") {
      const name = await dialog.prompt({
        title: labelFor(action),
        description: duplicateActionDescription(action),
        confirmLabel: "Crear registro",
        input: {
          label: "Nombre de la institución",
          initialValue: source.institution ?? "",
          placeholder: "Nombre visible del nuevo prospecto",
        },
      });
      if (!name) return;
      separateName = name;
    } else if (action === "MERGE") {
      if (!review.existingProspectId) {
        setError("No hay un candidato existente que pueda conservarse en la fusión.");
        return;
      }
      const options = prospects
        .filter((prospect) => prospect.id !== review.existingProspectId)
        .map((prospect) => ({ value: prospect.id, label: `${prospect.displayName} · ${prospect.city ?? "sin localidad"}` }));
      if (options.length === 0) {
        setError("No hay otro prospecto disponible para fusionar.");
        return;
      }
      const selected = await dialog.select({
        title: "Fusionar registros",
        description: `Se conservará ${review.existingProspect?.displayName ?? "el candidato existente"}. El registro seleccionado se archivará y sus referencias se trasladarán.`,
        confirmLabel: "Fusionar y archivar",
        danger: true,
        input: { label: "Registro que se archivará", options },
      });
      if (!selected) return;
      absorbedProspectId = selected;
    } else if (action === "REJECT_ROW" || action === "LINK_TO_EXISTING") {
      const confirmed = await dialog.confirm({
        title: labelFor(action),
        description: duplicateActionDescription(action),
        confirmLabel: action === "REJECT_ROW" ? "Descartar fila" : "Vincular registro",
        danger: action === "REJECT_ROW",
      });
      if (!confirmed) return;
    }

    setResolvingId(review.id);
    setError(null);
    setNotice(null);
    try {
      await resolveDuplicateReview(review.id, {
        action,
        survivorProspectId: review.existingProspectId ?? undefined,
        absorbedProspectId,
        separateName,
        comment: `Resolución manual: ${labelFor(action)}`,
        idempotencyKey: `${review.id}:${action}:${crypto.randomUUID()}`,
      });
      setNotice(`Revisión resuelta: ${labelFor(action)}.`);
      await onChanged();
    } catch (caught) {
      setError(message(caught));
    } finally {
      setResolvingId(null);
    }
  }

  return (
    <section className="stack">
      <Panel title="Importar prospectos y exclusiones">
        <p className="muted">La vista previa es obligatoria: valida y guarda evidencia, pero no crea registros. La ejecución requiere una confirmación separada.</p>
        {error && <div className="alert error" role="alert">{error}</div>}
        {notice && <div className="alert success" role="status">{notice}</div>}
        <div className="import-actions">
          <label className="file-control">
            Archivo CSV o XLSX
            <input
              type="file"
              accept=".csv,.xlsx"
              onChange={(event) => {
                setFile(event.target.files?.[0] ?? null);
                setSummary(null);
                setRows([]);
                setPreviewKey(null);
              }}
            />
          </label>
          <button className="secondary-button" disabled={busy || !file} onClick={() => void run(false)}>
            {busy ? "Procesando…" : "Generar vista previa"}
          </button>
          <button className="danger-button" disabled={busy || !previewReady} onClick={() => void run(true)}>
            Importar resultados revisados
          </button>
        </div>
        {!previewReady && file && <p className="context-help">El botón de importación se habilitará cuando la vista previa de este archivo termine correctamente.</p>}
        {summary && <ImportSummaryView summary={summary} />}
      </Panel>
      {rows.length > 0 && (
        <Panel title="Resultados por fila">
          <div className="toolbar">
            <label>Hoja<select value={rowSheet} onChange={(event) => { setRowSheet(event.target.value); setRowPage(0); }}><option value="">Todas</option>{sheets.map((sheet) => <option key={sheet}>{sheet}</option>)}</select></label>
            <label>Resultado<select value={rowStatus} onChange={(event) => { setRowStatus(event.target.value as ImportRow["status"] | ""); setRowPage(0); }}><option value="">Todos</option>{["ACCEPTED", "EXCLUDED", "REJECTED", "DUPLICATE", "REVIEW_REQUIRED", "PENDING"].map((status) => <option key={status} value={status}>{labelFor(status)}</option>)}</select></label>
            <label className="grow">Buscar dentro de resultados<input value={rowSearch} onChange={(event) => { setRowSearch(event.target.value); setRowPage(0); }} placeholder="Correo, teléfono, fila o error" /></label>
          </div>
          <div className="result-summary"><strong>{filteredRows.length}</strong> filas visibles de {rows.length}</div>
          {visibleRows.length ? (
            <div className="table-scroll"><table className="responsive-table"><thead><tr><th>Hoja</th><th>Fila</th><th>Resultado</th><th>Detalle</th></tr></thead><tbody>
              {visibleRows.map((row) => <tr key={row.id}><td data-label="Hoja">{row.sourceSheet}</td><td data-label="Fila">{row.rowNumber}</td><td data-label="Resultado"><Badge value={row.status} /></td><td data-label="Detalle">{row.errorMessage ?? row.normalizedEmail ?? row.normalizedPhone ?? "Sin observaciones"}</td></tr>)}
            </tbody></table></div>
          ) : <EmptyState text="No hay filas que coincidan con estos filtros." />}
          {totalRowPages > 1 && <nav className="pagination" aria-label="Paginación de filas importadas"><button className="secondary-button" disabled={rowPage === 0} onClick={() => setRowPage((page) => page - 1)}>Anterior</button><span>Página {rowPage + 1} de {totalRowPages}</span><button className="secondary-button" disabled={rowPage + 1 >= totalRowPages} onClick={() => setRowPage((page) => page + 1)}>Siguiente</button></nav>}
        </Panel>
      )}
      <Panel title="Duplicados pendientes de revisión">
        {duplicateReviews.length === 0 ? <EmptyState text="No hay coincidencias pendientes." /> : (
          <div className="duplicate-list">
            {duplicateReviews.map((review) => {
              const source = sourceDataFields(review.sourceData);
              return (
                <article className="duplicate-card" key={review.id}>
                  <header><div><strong>{source.institution ?? "Registro importado sin nombre"}</strong><small>{review.sourceSheet}, fila {review.rowNumber}</small></div><Badge value={review.matchType} /></header>
                  <div className="comparison-grid">
                    <section><h3>Registro importado</h3><Detail label="Correo" value={source.email ?? review.normalizedEmail ?? "Sin correo cargado"} /><Detail label="Teléfono o WhatsApp" value={source.phone ?? review.normalizedPhone ?? "Sin teléfono cargado"} /><Detail label="Ubicación" value={[source.locality, source.province].filter(Boolean).join(", ") || "Sin ubicación cargada"} /><Detail label="Sitio web" value={source.website ?? "Sin sitio cargado"} /></section>
                    <section><h3>Candidato existente</h3>{review.existingProspect ? <><Detail label="Institución" value={review.existingProspect.displayName} /><Detail label="Ubicación" value={review.existingProspect.locality ?? "Sin localidad cargada"} /><Detail label="Sitio web" value={review.existingProspect.website ?? "Sin sitio cargado"} /><Detail label="Estado" value={labelFor(review.existingProspect.status)} /></> : <EmptyState text="No hay un candidato existente asociado." />}</section>
                  </div>
                  <p className="match-explanation"><strong>Confianza: {Math.round(review.confidence * 100)}%</strong> · {review.matchReasons ?? "La coincidencia requiere evaluación humana."}</p>
                  <details className="technical-details"><summary>Ver evidencia técnica sanitizada</summary><pre>{safeTechnicalData(review.sourceData)}</pre></details>
                  <div className="duplicate-actions">
                    {(["LINK_TO_EXISTING", "MARK_NOT_DUPLICATE", "CREATE_SEPARATE", "MERGE", "DEFER", "REJECT_ROW"] as DuplicateResolutionAction[]).map((action) => (
                      <div className="action-choice" key={action}><button className={action === "REJECT_ROW" ? "danger-button" : action === "LINK_TO_EXISTING" ? "primary-button" : "secondary-button"} disabled={resolvingId === review.id || (action === "LINK_TO_EXISTING" && !review.existingProspectId)} onClick={() => void resolve(review, action)}>{labelFor(action)}</button><small>{duplicateActionDescription(action)}</small></div>
                    ))}
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </Panel>
    </section>
  );
}'''
replace_function(app_path, "function ImportsPanel({", "function ExclusionsPanel({", imports_function)

# Prospect detail is reorganized into navigable sections and complete contact capture.
prospect_detail = r'''function ProspectDetail({
  prospect,
  session,
  onChanged,
}: {
  prospect: Prospect;
  session: SessionUser;
  onChanged: () => Promise<void>;
}) {
  type DetailTab = "summary" | "contacts" | "followup" | "tasks" | "activity" | "administrative";
  const [activeTab, setActiveTab] = useState<DetailTab>("summary");
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [timeline, setTimeline] = useState<TimelineItem[]>([]);
  const [name, setName] = useState(prospect.displayName);
  const [city, setCity] = useState(prospect.city ?? "");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [contactRole, setContactRole] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [whatsapp, setWhatsapp] = useState("");
  const [preferredChannel, setPreferredChannel] = useState<Contact["preferredChannel"] | "">("");
  const [consent, setConsent] = useState<Contact["consent"]>("UNKNOWN");
  const [note, setNote] = useState("");
  const [activityType, setActivityType] = useState<"EMAIL_SENT_MANUALLY" | "WHATSAPP_SENT_MANUALLY" | "PHONE_CALL" | "MEETING" | "DEMO">("PHONE_CALL");
  const [activitySummary, setActivitySummary] = useState("");
  const [taskTitle, setTaskTitle] = useState("");
  const [taskDueAt, setTaskDueAt] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const canWrite = session.permissions.includes("PROSPECT_WRITE");
  const canWriteActivity = session.permissions.includes("ACTIVITY_WRITE");

  const loadRelated = useCallback(async () => {
    try {
      const [contactList, taskList, timelinePage] = await Promise.all([
        listContacts(prospect.id),
        listTasks(),
        getTimeline(prospect.id),
      ]);
      setContacts(contactList);
      setTasks(taskList.filter((task) => task.prospectId === prospect.id));
      setTimeline(timelinePage.content);
    } catch (caught) {
      setError(message(caught));
    }
  }, [prospect.id]);

  useEffect(() => {
    setName(prospect.displayName);
    setCity(prospect.city ?? "");
    void loadRelated();
  }, [prospect.displayName, prospect.city, loadRelated]);

  async function run(action: () => Promise<unknown>, success: string) {
    setError(null);
    setNotice(null);
    try {
      await action();
      setNotice(success);
      await onChanged();
      await loadRelated();
    } catch (caught) {
      setError(message(caught));
      if (isConflict(caught)) {
        await onChanged();
        await loadRelated();
      }
    }
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await run(() => updateProspect(prospect.id, {
      version: prospect.version,
      displayName: name,
      legalName: prospect.legalName,
      priority: prospect.priority,
      score: prospect.score,
      estimatedStudents: prospect.estimatedStudents,
      source: prospect.source,
      sourceDetail: prospect.sourceDetail,
      ownerUserId: prospect.ownerUserId,
      website: prospect.website,
      address: prospect.address,
      city,
      province: prospect.province,
      country: prospect.country,
      timezone: prospect.timezone,
      notesSummary: prospect.notesSummary,
      nextActionAt: prospect.nextActionAt,
    }), "Prospecto actualizado.");
  }

  async function copy(value: string) {
    try {
      await navigator.clipboard.writeText(value);
      setNotice("Dato copiado al portapapeles.");
    } catch {
      setError("No se pudo copiar automáticamente. Selecciona el dato y cópialo manualmente.");
    }
  }

  const transitions = allowedTransitions(prospect.status);
  const tabs: Array<{ id: DetailTab; label: string }> = [
    { id: "summary", label: "Resumen" },
    { id: "contacts", label: "Contactos" },
    { id: "followup", label: "Seguimiento" },
    { id: "tasks", label: "Tareas" },
    { id: "activity", label: "Actividad" },
    { id: "administrative", label: "Datos administrativos" },
  ];

  return (
    <div className="stack detail-workspace">
      <header className="prospect-heading"><div><h2>{prospect.displayName}</h2><p>{[prospect.city, prospect.province].filter(Boolean).join(", ") || "Sin ubicación cargada"}</p></div><div className="badge-row"><Badge value={prospect.status} /><Badge value={prospect.eligibility} /></div></header>
      {error && <div className="alert error" role="alert">{error}</div>}
      {notice && <div className="alert success" role="status" aria-live="polite">{notice}</div>}
      <div className="tabs" role="tablist" aria-label="Secciones de la ficha">
        {tabs.map((tab) => <button key={tab.id} type="button" role="tab" aria-selected={activeTab === tab.id} className={activeTab === tab.id ? "tab-button active" : "tab-button"} onClick={() => setActiveTab(tab.id)}>{tab.label}</button>)}
      </div>

      {activeTab === "summary" && <section role="tabpanel" className="stack compact"><dl className="detail-grid"><Detail label="Estado" value={labelFor(prospect.status)} /><Detail label="Disponibilidad de contacto" value={prospect.contactEligible ? "Tiene al menos un canal utilizable" : "Sin canal utilizable"} /><Detail label="Prioridad" value={prospect.priority == null ? "Sin prioridad definida" : String(prospect.priority)} /><Detail label="Puntuación" value={prospect.score == null ? "Sin puntuación" : String(prospect.score)} /><Detail label="Responsable" value={prospect.ownerName ?? "Sin responsable asignado"} /><Detail label="Próxima acción" value={prospect.nextActionAt ? dateTime(prospect.nextActionAt) : "Sin próxima acción programada"} /><Detail label="Último contacto" value={prospect.lastContactAt ? dateTime(prospect.lastContactAt) : "Sin contactos registrados"} /><Detail label="Sitio web" value={prospect.website ?? "Sin sitio cargado"} /></dl>{canWrite && <form className="inline-form compact-form" onSubmit={(event) => void save(event)}><label className="grow">Nombre visible<input value={name} onChange={(event) => setName(event.target.value)} required /></label><label>Localidad<input value={city} onChange={(event) => setCity(event.target.value)} /></label><button className="secondary-button">Guardar cambios</button></form>}</section>}

      {activeTab === "contacts" && <section role="tabpanel" className="stack compact"><div className={prospect.contactEligible ? "alert success" : "alert warning"}>{prospect.contactEligible ? "El prospecto cuenta con un canal válido para contacto." : "El prospecto no tiene un canal utilizable. Agrega un correo, teléfono o WhatsApp válido antes de considerarlo contactable."}</div>{contacts.length === 0 ? <EmptyState text="Sin contactos cargados. Agrega una persona o un canal publicado." /> : <div className="contact-list">{contacts.map((contact) => <article className="contact-card" key={contact.id}><header><div><strong>{contact.displayName || "Contacto sin nombre"}</strong><span>{contact.role ?? "Sin cargo cargado"}</span></div>{contact.primary && <Badge value="PRIMARY" />}</header>{contact.channels.length ? <ul>{contact.channels.map((channel) => <li key={channel.id}><div><strong>{labelFor(channel.type)}</strong><span>{channel.value}</span><small>{channel.valid ? "Canal válido" : "Canal inválido"} · {labelFor(channel.consent)}</small></div><button className="link-button" type="button" onClick={() => void copy(channel.value)}>Copiar</button></li>)}</ul> : <p className="muted">Sin canales cargados.</p>}</article>)}</div>}{canWrite && <form className="form-grid compact-form" onSubmit={(event) => { event.preventDefault(); const selectedPreferred = preferredChannel || (email ? "EMAIL" : whatsapp ? "WHATSAPP" : phone ? "PHONE" : undefined); void run(() => createContact(prospect.id, { firstName, lastName, role: contactRole, email, phone, whatsapp, preferredChannel: selectedPreferred, consent, source: "MANUAL" }), "Contacto agregado.").then(() => { setFirstName(""); setLastName(""); setContactRole(""); setEmail(""); setPhone(""); setWhatsapp(""); setPreferredChannel(""); setConsent("UNKNOWN"); }); }}><label>Nombre<input value={firstName} onChange={(event) => setFirstName(event.target.value)} /></label><label>Apellido<input value={lastName} onChange={(event) => setLastName(event.target.value)} /></label><label className="full-width">Cargo o función<input value={contactRole} onChange={(event) => setContactRole(event.target.value)} placeholder="Administración, dirección, secretaría…" /></label><label>Correo electrónico<input type="email" value={email} onChange={(event) => setEmail(event.target.value)} /></label><label>Teléfono<input type="tel" value={phone} onChange={(event) => setPhone(event.target.value)} /></label><label>WhatsApp<input type="tel" value={whatsapp} onChange={(event) => setWhatsapp(event.target.value)} /></label><label>Canal preferido<select value={preferredChannel ?? ""} onChange={(event) => setPreferredChannel(event.target.value as Contact["preferredChannel"] | "")}><option value="">Elegir automáticamente</option><option value="EMAIL">Correo electrónico</option><option value="PHONE">Teléfono</option><option value="WHATSAPP">WhatsApp</option></select></label><label>Consentimiento<select value={consent} onChange={(event) => setConsent(event.target.value as Contact["consent"])}><option value="UNKNOWN">Sin definir</option><option value="GRANTED">Autorizado</option><option value="DENIED">No autorizado</option></select></label><button className="primary-button full-width" disabled={!firstName.trim() && !lastName.trim() && !email.trim() && !phone.trim() && !whatsapp.trim()}>Agregar contacto</button></form>}</section>}

      {activeTab === "followup" && <section role="tabpanel" className="stack compact"><dl className="detail-grid"><Detail label="Estado actual" value={labelFor(prospect.status)} /><Detail label="Próxima acción" value={prospect.nextActionAt ? dateTime(prospect.nextActionAt) : "Sin próxima acción programada"} /></dl>{canWrite && transitions.length > 0 ? <div className="action-row" aria-label="Cambios de estado disponibles">{transitions.map((status) => <button className="secondary-button" key={status} onClick={() => void run(() => transitionProspect(prospect.id, prospect.version, status), `Estado cambiado a ${labelFor(status)}.`)}>Pasar a {labelFor(status)}</button>)}</div> : <EmptyState text="No hay cambios de estado disponibles para este registro." />}</section>}

      {activeTab === "tasks" && <section role="tabpanel" className="stack compact">{tasks.length === 0 ? <EmptyState text="No hay tareas pendientes para este prospecto." /> : tasks.map((task) => <article className="timeline-item" key={task.id}><strong>{task.title}</strong><span>{labelFor(task.status)} · vence {dateTime(task.dueAt)}</span>{canWriteActivity && !["COMPLETED", "CANCELLED"].includes(task.status) && <button className="secondary-button" onClick={() => void run(() => changeTaskStatus(task, "COMPLETED"), "Tarea completada.")}>Marcar como completada</button>}</article>)}{canWriteActivity && <form className="inline-form compact-form" onSubmit={(event) => { event.preventDefault(); void run(() => createTask(prospect.id, { ownerUserId: session.userId, title: taskTitle, dueAt: new Date(taskDueAt).toISOString() }), "Tarea creada.").then(() => { setTaskTitle(""); setTaskDueAt(""); }); }}><label className="grow">Nueva tarea<input value={taskTitle} onChange={(event) => setTaskTitle(event.target.value)} required /></label><label>Vencimiento<input type="datetime-local" value={taskDueAt} onChange={(event) => setTaskDueAt(event.target.value)} required /></label><button className="secondary-button">Crear tarea</button></form>}</section>}

      {activeTab === "activity" && <section role="tabpanel" className="stack compact">{canWriteActivity && <><form className="inline-form compact-form" onSubmit={(event) => { event.preventDefault(); void run(() => createActivity(prospect.id, { type: activityType, summary: activitySummary }), "Actividad registrada.").then(() => setActivitySummary("")); }}><label>Tipo de actividad<select value={activityType} onChange={(event) => setActivityType(event.target.value as typeof activityType)}><option value="PHONE_CALL">Llamada</option><option value="MEETING">Reunión</option><option value="DEMO">Demostración</option><option value="EMAIL_SENT_MANUALLY">Correo registrado manualmente</option><option value="WHATSAPP_SENT_MANUALLY">WhatsApp registrado manualmente</option></select></label><label className="grow">Resumen<input value={activitySummary} onChange={(event) => setActivitySummary(event.target.value)} required /></label><button className="secondary-button">Registrar actividad</button></form><form className="inline-form compact-form" onSubmit={(event) => { event.preventDefault(); void run(() => createNote(prospect.id, note), "Nota registrada.").then(() => setNote("")); }}><label className="grow">Nota<input value={note} onChange={(event) => setNote(event.target.value)} required /></label><button className="secondary-button">Agregar nota</button></form></>}{timeline.length === 0 ? <EmptyState text="Todavía no hay actividad registrada." /> : timeline.map((item) => <article className="timeline-item" key={`${item.eventType}-${item.id}`}><small>{dateTime(item.eventAt)} · {labelFor(item.eventType)}</small><strong>{item.title}</strong>{item.detail && <span>{item.detail}</span>}</article>)}</section>}

      {activeTab === "administrative" && <section role="tabpanel" className="stack compact"><dl className="detail-grid"><Detail label="Razón social" value={prospect.legalName ?? "Sin razón social cargada"} /><Detail label="Dirección" value={prospect.address ?? "Sin dirección cargada"} /><Detail label="Localidad" value={prospect.city ?? "Sin localidad cargada"} /><Detail label="Provincia" value={prospect.province ?? "Sin provincia cargada"} /><Detail label="País" value={prospect.country ?? "Sin país cargado"} /><Detail label="Zona horaria" value={prospect.timezone ?? "Sin zona horaria cargada"} /><Detail label="Fuente" value={prospect.source ? labelFor(prospect.source) : "Sin fuente cargada"} /><Detail label="Detalle de fuente" value={prospect.sourceDetail ?? "Sin detalle cargado"} /></dl><details className="technical-details"><summary>Ver identificadores técnicos</summary><dl className="detail-grid"><Detail label="ID del prospecto" value={prospect.id} /><Detail label="ID de la institución" value={prospect.institutionId} /><Detail label="Versión" value={String(prospect.version)} /></dl></details></section>}
    </div>
  );
}'''
replace_function(app_path, "function ProspectDetail({", "function allowedTransitions(", prospect_detail)

# Audit is readable first, technical detail second.
audit_function = r'''function AuditTable({ events }: { events: AuditEvent[] }) {
  if (events.length === 0) return <EmptyState text="Todavía no hay eventos de auditoría." />;
  return (
    <div className="table-scroll">
      <table className="responsive-table">
        <thead><tr><th>Fecha</th><th>Descripción</th><th>Entidad</th><th>Detalle</th></tr></thead>
        <tbody>
          {events.map((event) => (
            <tr key={event.id}>
              <td data-label="Fecha">{dateTime(event.createdAt)}</td>
              <td data-label="Descripción"><strong>{auditDescription(event.action, event.entityType)}</strong></td>
              <td data-label="Entidad">{labelFor(event.entityType)}{event.entityId ? ` #${event.entityId.slice(0, 8)}` : ""}</td>
              <td data-label="Detalle"><details className="technical-details"><summary>Ver datos técnicos</summary><pre>{safeTechnicalData(event.payload)}</pre></details></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}'''
replace_function(app_path, "function AuditTable(", "function Panel(", audit_function)

replace_once(
    app_path,
    'function Badge({ value }: { value: string }) {\n  return <span className={`badge badge-${value.toLowerCase().replaceAll("_", "-")}`}>{value}</span>;\n}\n',
    'function Badge({ value }: { value: string }) {\n  const label = labelFor(value);\n  return <span className={`badge badge-${value.toLowerCase().replaceAll("_", "-")}`} title={label !== value ? `Código interno: ${value}` : undefined}>{label}</span>;\n}\n',
)
replace_once(app_path, '    dashboard: "Resumen",', '    dashboard: "Resumen comercial",')
replace_once(app_path, '    outbox: "Bandeja de salida",', '    outbox: "Bandeja de salida",')
replace_once(app_path, '    inbound: "Mensajes recibidos",', '    inbound: "Mensajes recibidos",')
replace_once(
    app_path,
    'function message(caught: unknown): string {\n  return caught instanceof Error ? caught.message : "Ocurrió un error inesperado";\n}\n',
    'function message(caught: unknown): string {\n  return friendlyErrorMessage(caught instanceof Error ? caught.message : "Ocurrió un error inesperado");\n}\n',
)

# Remaining direct values that should be humanized.
app = read(app_path)
app = app.replace('{summary.status}', '{labelFor(summary.status)}')
app = app.replace('<Control label="Estado" value={summary.status} />', '<Control label="Estado" value={labelFor(summary.status)} />')
app = app.replace('<td>{exclusion.channelType}</td>', '<td>{labelFor(exclusion.channelType)}</td>')
app = app.replace('<td>{exclusion.reason}</td>', '<td>{labelFor(exclusion.reason)}</td>')
write(app_path, app)

# Fail immediately if a native prompt or confirmation survived.
app = read(app_path)
if "window.prompt" in app or "window.confirm" in app:
    matches = [line for line in app.splitlines() if "window.prompt" in line or "window.confirm" in line]
    raise RuntimeError("Native dialogs remain: " + " | ".join(matches))

# Visual tokens, accessible dialog, responsive cards and clearer hierarchy.
styles_path = "frontend/src/styles.css"
styles = read(styles_path)
replace_once(
    styles_path,
    "  text-rendering: optimizeLegibility;\n",
    "  text-rendering: optimizeLegibility;\n  --color-ink: #172033;\n  --color-muted: #5f6b7d;\n  --color-surface: #ffffff;\n  --color-canvas: #eef1f5;\n  --color-border: #d7dee8;\n  --color-accent: #8be0c4;\n  --color-focus: #2563eb;\n  --radius-sm: 10px;\n  --radius-md: 16px;\n  --shadow-card: 0 8px 24px rgb(22 32 51 / 8%);\n  --content-max: 1680px;\n",
)
replace_once(styles_path, "  min-height: 100vh;\n}\n\nbutton,", "  min-height: 100vh;\n  line-height: 1.5;\n}\n\nbutton,")
replace_once(styles_path, "  padding: 28px;\n}\n", "  width: 100%;\n  max-width: var(--content-max);\n  margin: 0 auto;\n  padding: 28px;\n}\n")
replace_once(styles_path, "  padding: 10px 15px;\n", "  min-height: 44px;\n  padding: 10px 15px;\n")
replace_once(styles_path, "  outline: 3px solid rgb(71 151 219 / 24%);\n  outline-offset: 1px;\n", "  outline: 3px solid color-mix(in srgb, var(--color-focus) 35%, transparent);\n  outline-offset: 2px;\n")
styles = read(styles_path)
styles += r'''

.menu-toggle {
  display: none;
  min-height: 40px;
  margin-left: auto;
  border: 1px solid #3a4863;
  border-radius: var(--radius-sm);
  color: white;
  background: transparent;
}

.result-summary,
.context-help {
  margin: 0 0 14px;
  color: var(--color-muted);
  font-size: 0.9rem;
}

.selectable-table tbody tr {
  cursor: pointer;
}

.selectable-table tbody tr:focus-visible {
  outline: 3px solid color-mix(in srgb, var(--color-focus) 35%, transparent);
  outline-offset: -3px;
}

.table-scroll thead th {
  position: sticky;
  z-index: 1;
  top: 0;
  background: var(--color-surface);
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
  color: var(--color-muted);
}

.prospect-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.prospect-heading h2,
.prospect-heading p {
  margin-bottom: 4px;
}

.badge-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tabs {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  padding: 4px 0 8px;
  border-bottom: 1px solid var(--color-border);
}

.tab-button {
  min-height: 42px;
  padding: 8px 12px;
  border: 0;
  border-radius: var(--radius-sm) var(--radius-sm) 0 0;
  color: var(--color-muted);
  background: transparent;
  white-space: nowrap;
}

.tab-button.active {
  color: var(--color-ink);
  background: #e8f8f2;
  font-weight: 800;
}

.contact-list,
.duplicate-list {
  display: grid;
  gap: 14px;
}

.contact-card,
.duplicate-card {
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.contact-card header,
.duplicate-card > header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.contact-card header div,
.duplicate-card header div {
  display: grid;
  gap: 2px;
}

.contact-card ul {
  display: grid;
  gap: 8px;
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
}

.contact-card li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px;
  border-radius: var(--radius-sm);
  background: #f8fafc;
}

.contact-card li div {
  display: grid;
  gap: 2px;
}

.comparison-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin: 16px 0;
}

.comparison-grid section {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #f8fafc;
}

.comparison-grid h3 {
  margin: 0 0 4px;
}

.match-explanation {
  padding: 12px;
  border-radius: var(--radius-sm);
  background: #fff8df;
}

.duplicate-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.action-choice {
  display: grid;
  align-content: start;
  gap: 6px;
}

.action-choice small {
  color: var(--color-muted);
}

.technical-details summary {
  color: #334155;
  cursor: pointer;
  font-weight: 700;
}

.technical-details pre {
  max-width: 100%;
  overflow: auto;
  padding: 12px;
  border-radius: var(--radius-sm);
  background: #0f172a;
  color: #e2e8f0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.alert.warning {
  color: #6f5200;
  border: 1px solid #d6b04a;
  background: #fff8df;
}

.dialog-backdrop {
  position: fixed;
  z-index: 1000;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgb(15 23 42 / 62%);
}

.dialog-card {
  width: min(520px, 100%);
  max-height: min(720px, calc(100vh - 40px));
  overflow: auto;
  padding: 22px;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: 0 28px 80px rgb(0 0 0 / 35%);
}

.dialog-card form,
.dialog-card header {
  display: grid;
  gap: 14px;
}

.dialog-card h2,
.dialog-card p {
  margin: 0;
}

.dialog-card p {
  color: var(--color-muted);
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    scroll-behavior: auto !important;
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

@media (max-width: 900px) {
  .duplicate-actions {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .sidebar {
    position: sticky;
    z-index: 20;
    top: 0;
    padding: 14px 16px;
  }

  .sidebar .brand {
    width: 100%;
  }

  .menu-toggle {
    display: inline-flex;
    align-items: center;
  }

  .sidebar nav,
  .sidebar > .secondary-button {
    display: none;
  }

  .sidebar.menu-open nav {
    display: grid;
  }

  .sidebar.menu-open > .secondary-button {
    display: block;
  }

  .comparison-grid,
  .duplicate-actions {
    grid-template-columns: 1fr;
  }

  .responsive-table thead {
    position: absolute;
    width: 1px;
    height: 1px;
    overflow: hidden;
    clip: rect(0 0 0 0);
  }

  .responsive-table,
  .responsive-table tbody,
  .responsive-table tr,
  .responsive-table td {
    display: block;
    width: 100%;
  }

  .responsive-table tr {
    margin-bottom: 12px;
    padding: 10px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background: var(--color-surface);
  }

  .responsive-table td {
    display: grid;
    grid-template-columns: minmax(110px, 0.8fr) minmax(0, 1.2fr);
    gap: 12px;
    border: 0;
    padding: 7px;
  }

  .responsive-table td::before {
    content: attr(data-label);
    color: var(--color-muted);
    font-size: 0.78rem;
    font-weight: 800;
    text-transform: uppercase;
  }

  .detail-workspace {
    max-height: none;
    overflow: visible;
  }
}

@media (max-width: 480px) {
  .main-content {
    padding: 14px;
  }

  .dialog-backdrop {
    align-items: end;
    padding: 0;
  }

  .dialog-card {
    width: 100%;
    max-height: 92vh;
    border-radius: 18px 18px 0 0;
  }

  .dialog-actions,
  .pagination,
  .prospect-heading {
    align-items: stretch;
    flex-direction: column;
  }
}
'''
write(styles_path, styles)

write_new(
    "docs/ux-conventions.md",
    r'''# Convenciones de experiencia de usuario

## Objetivo

La interfaz del CRM debe poder ser utilizada por personal administrativo o comercial sin exponer como lenguaje principal los códigos internos de la API o la base de datos.

## Etiquetas visibles

`frontend/src/labels.ts` es la fuente central para estados, etapas, canales, roles, acciones de duplicados y errores operativos. Los valores persistidos y los contratos REST no se traducen ni se renombran; solo cambia su representación visible.

Los componentes deben usar `labelFor` o los mapas tipados. No deben incorporar ternarios dispersos para traducir estados.

## Confirmaciones y datos técnicos

Las acciones con consecuencias usan el proveedor de diálogos accesibles de `frontend/src/dialog.tsx`. No se permiten `window.prompt` ni `window.confirm` en flujos operativos.

Los diálogos:

- describen la consecuencia antes de confirmar;
- admiten teclado, Escape, foco inicial, trampa de foco y retorno al control de origen;
- no solicitan UUID ni identificadores técnicos al operador.

El JSON de auditoría, importaciones y salida se presenta solo dentro de “Ver datos técnicos”, con redacción de claves sensibles. Nunca se interpreta como HTML.

## Prospectos y contactos

La búsqueda incluye instituciones, contactos, ubicación, sitio, etiquetas y valores normalizados de `contact_channel`, incluidos correo y teléfono. La URL conserva `q` y `status`.

Un prospecto solo se presenta como contactable cuando tiene al menos un canal válido y no excluido. La creación, modificación o eliminación de contactos y canales recalcula `contact_eligible` sin anular exclusiones explícitas.

## Importaciones y duplicados

La vista previa es obligatoria para el archivo actualmente seleccionado. Los resultados se resumen, filtran y paginan antes de ejecutar.

`CREATE_SEPARATE` y `MARK_NOT_DUPLICATE` extraen exclusivamente un conjunto permitido de campos desde la evidencia JSON de `import_row`. Se conservan los canales normalizados, ubicación, categoría, sitio, fuente, prioridad, evidencia e identificador externo cuando están disponibles. Los campos desconocidos no se persisten.

Las exclusiones, la unicidad por organización, la idempotencia y la auditoría continúan siendo dominantes.

## Responsive y accesibilidad

- Los controles interactivos tienen una altura táctil mínima de 44 px.
- Las tablas principales se convierten en tarjetas en móvil.
- El menú se colapsa en pantallas pequeñas.
- El foco es visible y las filas seleccionables responden a Enter y Espacio.
- El color no es el único indicador de estado.
- Se respeta `prefers-reduced-motion`.

## Validaciones

Los comandos canónicos permanecen documentados en `README.md`. Para cambios de interfaz se deben ejecutar, como mínimo:

```text
npm ci
npm run typecheck
npm run test:unit
npm run build
./mvnw -B -f backend/pom.xml verify
git diff --check
bash scripts/check-repository-safety.sh
```
''',
)

readme = read("README.md")
if "docs/ux-conventions.md" not in readme:
    readme += "\n\n## Convenciones de experiencia de usuario\n\nLa traducción visible de estados, los diálogos accesibles, la presentación responsive y la conservación segura de datos importados se documentan en `docs/ux-conventions.md`.\n"
    write("README.md", readme)

print("Remote UX transformations applied successfully.")
