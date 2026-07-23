import type {
  CampaignChannel,
  OpportunityStage,
  ProspectStatus,
} from "./types";

const prospectStatusLabels: Record<ProspectStatus, string> = {
  NEW: "Nuevo",
  QUALIFYING: "En calificación",
  READY_TO_CONTACT: "Listo para contactar",
  FOLLOW_UP: "En seguimiento",
  DEMO_PROPOSED: "Demostración propuesta",
  DEMO_SCHEDULED: "Demostración programada",
  PROPOSAL: "Propuesta enviada",
  CUSTOMER: "Cliente",
  NEEDS_ENRICHMENT: "Requiere completar datos",
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

const opportunityStageLabels: Record<OpportunityStage, string> = {
  QUALIFICATION: "Calificación",
  DISCOVERY: "Diagnóstico",
  DEMO: "Demostración",
  PROPOSAL: "Propuesta",
  NEGOTIATION: "Negociación",
  WON: "Ganada",
  LOST: "Perdida",
};

const channelLabels: Record<CampaignChannel | "PHONE" | "WEBSITE" | "SOCIAL" | "OTHER", string> = {
  EMAIL: "Correo electrónico",
  WHATSAPP: "WhatsApp",
  PHONE: "Teléfono",
  WEBSITE: "Sitio web",
  SOCIAL: "Red social",
  OTHER: "Otro canal",
};

const labels: Record<string, string> = {
  ...prospectStatusLabels,
  ...opportunityStageLabels,
  ...channelLabels,
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
  SUCCEEDED: "Completado",
  RETRY: "Pendiente de reintento",
  DEAD: "Agotó los reintentos",
  BLOCKED: "Bloqueado",
  RECEIVED: "Recibido",
  QUARANTINED: "En revisión",
  ASSOCIATED: "Asociado",
  DISCARDED: "Descartado",
  DRAFT: "Borrador",
  SIMULATED: "Simulada",
  SCHEDULED: "Programada",
  PAUSED: "Pausada",
  VALID: "Válido",
  MISSING_CHANNEL: "Sin canal disponible",
  INELIGIBLE: "No elegible",
  ADMIN: "Administrador",
  MANAGER: "Responsable comercial",
  SALES: "Operador comercial",
  VIEWER: "Solo lectura",
  UNKNOWN: "Sin definir",
  GRANTED: "Consentimiento registrado",
  DENIED: "Sin consentimiento",
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
  MANUAL: "Carga manual",
  DUPLICATE_REVIEW: "Revisión de duplicados",
  EXISTING_CONVERSATION: "Ya existe una conversación",
  EXISTING_CUSTOMER: "Ya es cliente",
  UNSUBSCRIBE_REQUEST: "Solicitó no recibir mensajes",
  NEGATIVE_REPLY: "Indicó que no está interesado",
  PERMANENT_BOUNCE: "El correo fue rechazado de forma permanente",
  INVALID_CONTACT: "El dato de contacto no es válido",
  IRRELEVANT_INSTITUTION: "La institución no corresponde al público objetivo",
  NOOP: "Sin conexión externa",
  DEEPLINK_ONLY: "Enlace manual únicamente",
  IMPLEMENTED_NOT_CONNECTED: "Disponible, sin conexión externa",
  FAKE: "Simulación segura",
  FAKE_INBOUND: "Recepción de prueba",
  STOP: "Finalizar secuencia",
  WAIT: "Esperar",
  CONDITION: "Evaluar condición",
  MANUAL_TASK: "Crear tarea manual",
  PROSPECT_CREATED: "Prospecto creado",
  PROSPECT_UPDATED: "Prospecto actualizado",
  PROSPECT_STATUS_CHANGED: "Estado del prospecto actualizado",
  PROSPECT_ARCHIVED: "Prospecto archivado",
  PROSPECT_RESTORED: "Prospecto restaurado",
  CONTACT_CREATED: "Contacto creado",
  CONTACT_UPDATED: "Contacto actualizado",
  CONTACT_CHANNEL_CREATED: "Canal de contacto agregado",
  CONTACT_CHANNEL_UPDATED: "Canal de contacto actualizado",
  CONTACT_CHANNEL_REMOVED: "Canal de contacto eliminado",
  CONTACT_REMOVED: "Contacto eliminado",
  CONTACT_PRIMARY_CHANGED: "Contacto principal actualizado",
  DUPLICATE_REVIEW_RESOLVED: "Revisión de duplicado resuelta",
  DUPLICATE_REVIEW_DEFERRED: "Revisión de duplicado postergada",
  Prospect: "Prospecto",
  Contact: "Contacto",
  ContactChannel: "Canal de contacto",
  DuplicateReview: "Revisión de duplicado",
  Campaign: "Campaña",
  Opportunity: "Oportunidad",
  ImportJob: "Importación",
  ImportRow: "Fila importada",
};

const sensitiveKey = /(password|secret|token|cookie|authorization|credential|api[-_]?key)/i;

export function labelFor(value: string | null | undefined): string {
  if (!value) return "Sin definir";
  const known = labels[value];
  if (known) return known;
  return value
    .replaceAll("_", " ")
    .replaceAll("-", " ")
    .toLocaleLowerCase("es-AR")
    .replace(/^./, (character) => character.toLocaleUpperCase("es-AR"));
}

export function prospectStatusLabel(value: ProspectStatus): string {
  return prospectStatusLabels[value];
}

export function opportunityStageLabel(value: OpportunityStage): string {
  return opportunityStageLabels[value];
}

export function channelLabel(value: string): string {
  return labels[value] ?? labelFor(value);
}

export function humanizeError(raw: string): string {
  const message = raw.trim();
  const mappings: Array<[RegExp, string]> = [
    [/Duplicate review was already resolved/i, "Esta revisión ya fue resuelta. Actualizá la pantalla para ver su estado actual."],
    [/Contact channel already exists/i, "Ese canal de contacto ya está registrado en otro contacto."],
    [/Prospect not found/i, "No se encontró el prospecto solicitado o ya no está disponible."],
    [/Owner must be an active member/i, "El responsable seleccionado no es un usuario activo de esta organización."],
    [/modified by another user|Optimistic/i, "El registro fue modificado por otra persona. Actualizá la pantalla antes de volver a intentarlo."],
    [/Access Denied|Forbidden|HTTP 403/i, "No tenés permisos para realizar esta acción."],
    [/Unauthorized|HTTP 401/i, "La sesión venció o no es válida. Volvé a ingresar."],
    [/HTTP 404/i, "No se encontró la información solicitada."],
    [/HTTP 409/i, "La información cambió mientras trabajabas. Actualizá la pantalla y revisá el estado actual."],
    [/HTTP 422/i, "No se pudo completar la acción porque faltan datos válidos o existe una restricción operativa."],
    [/Failed to fetch|NetworkError/i, "No se pudo conectar con el servidor. Revisá la conexión e intentá nuevamente."],
  ];
  for (const [pattern, replacement] of mappings) {
    if (pattern.test(message)) return replacement;
  }
  if (/[áéíóúñ¿¡]|\b(no|esta|este|seleccion|revis|actualiz|datos|acción|archivo|sesión)\b/i.test(message)) {
    return message;
  }
  return "No se pudo completar la acción. Revisá los datos e intentá nuevamente.";
}

function sanitize(value: unknown, key = ""): unknown {
  if (sensitiveKey.test(key)) return "[dato protegido]";
  if (Array.isArray(value)) return value.slice(0, 50).map((item) => sanitize(item));
  if (value && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value as Record<string, unknown>)
        .slice(0, 80)
        .map(([entryKey, entryValue]) => [entryKey, sanitize(entryValue, entryKey)]),
    );
  }
  if (typeof value === "string" && value.length > 2_000) return `${value.slice(0, 2_000)}…`;
  return value;
}

export function safeTechnicalJson(value: string): string {
  try {
    return JSON.stringify(sanitize(JSON.parse(value)), null, 2);
  } catch {
    return value.length > 4_000 ? `${value.slice(0, 4_000)}…` : value;
  }
}

export function auditSummary(value: string): string {
  try {
    const parsed = sanitize(JSON.parse(value));
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) return "Se registró el evento.";
    const entries = Object.entries(parsed as Record<string, unknown>).slice(0, 3);
    if (!entries.length) return "Se registró el evento sin cambios adicionales.";
    return entries
      .map(([key, entryValue]) => `${labelFor(key)}: ${formatValue(entryValue)}`)
      .join(" · ");
  } catch {
    return "Se registró el evento. Los datos técnicos están disponibles en el detalle.";
  }
}

export function duplicateSourceData(value: string): Record<string, string> {
  try {
    const parsed = JSON.parse(value) as Record<string, unknown>;
    return Object.fromEntries(
      Object.entries(parsed)
        .filter(([, entryValue]) => typeof entryValue === "string")
        .map(([key, entryValue]) => [key, String(entryValue).trim()] as [string, string])
        .filter(([, entryValue]) => entryValue.length > 0),
    );
  } catch {
    return {};
  }
}

export function duplicateSourceSummary(value: string): Array<[string, string]> {
  const source = duplicateSourceData(value);
  const fields: Array<[string, string[]]> = [
    ["Institución", ["institucion"]],
    ["Correo", ["correo publicado", "correo", "email"]],
    ["Teléfono o WhatsApp", ["telefono whatsapp", "telefono o whatsapp", "telefono", "whatsapp"]],
    ["Localidad", ["localidad"]],
    ["Provincia", ["provincia"]],
    ["Sitio web", ["sitio web"]],
    ["Categoría", ["categoria"]],
    ["Fuente", ["fuente"]],
  ];
  return fields.flatMap(([label, aliases]) => {
    const found = aliases.map((alias) => source[alias]).find(Boolean);
    return found ? [[label, found] as [string, string]] : [];
  });
}

export function suggestedDuplicateName(value: string, fallback = "Nuevo prospecto"): string {
  return duplicateSourceData(value).institucion || fallback;
}

export function formatConfiguration(configuration: Record<string, unknown>): string {
  const entries = Object.entries(configuration);
  if (!entries.length) return "Sin parámetros adicionales";
  return entries.map(([key, value]) => `${labelFor(key)}: ${formatValue(value)}`).join(" · ");
}

function formatValue(value: unknown): string {
  if (value === null || value === undefined || value === "") return "Sin definir";
  if (typeof value === "boolean") return value ? "Sí" : "No";
  if (typeof value === "object") return JSON.stringify(sanitize(value));
  const stringValue = String(value);
  return labels[stringValue] ?? stringValue;
}
