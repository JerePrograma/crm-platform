#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def patch(path: str, old: str, new: str, expected: int = 1) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    count = text.count(old)
    if count != expected:
        raise RuntimeError(f"Expected {expected} matches in {path}, found {count}: {old[:100]!r}")
    target.write_text(text.replace(old, new), encoding="utf-8")


patch(
    "frontend/src/labels.ts",
    '  OTHER: "Otro",\n',
    '  OTHER: "Otro",\n  PRIMARY: "Principal",\n  Prospect: "Prospecto",\n  Contact: "Contacto",\n  ContactChannel: "Canal de contacto",\n  DuplicateReview: "Revisión de duplicado",\n  User: "Usuario",\n  Exclusion: "Exclusión",\n  Campaign: "Campaña",\n  OutboxEvent: "Mensaje de salida",\n',
)

app = "frontend/src/App.tsx"
for old, new in {
    '<option value="ADMIN">Admin</option>': '<option value="ADMIN">Administrador</option>',
    '<option value="MANAGER">Manager</option>': '<option value="MANAGER">Responsable comercial</option>',
    '<option value="en-US">English US</option>': '<option value="en-US">Inglés (Estados Unidos)</option>',
    '<option value="en">English</option>': '<option value="en">Inglés</option>',
    '<Metric label="Dead-letter" value={report.operations.deadLetter ?? 0} />': '<Metric label="Mensajes no procesados" value={report.operations.deadLetter ?? 0} />',
    '<Metric label="Quarantine" value={report.operations.quarantine ?? 0} />': '<Metric label="Mensajes en revisión" value={report.operations.quarantine ?? 0} />',
    '<ReportMap title="Outbox por estado" values={report.outbox} />': '<ReportMap title="Bandeja de salida por estado" values={report.outbox} />',
    'Los bloqueos de entorno dominan esta pantalla. Ningún usuario puede habilitar envíos reales desde la API o la UI.': 'La configuración de seguridad tiene prioridad. Ningún usuario puede habilitar envíos reales desde esta pantalla.',
    '<Control label="Webhook fake" value="Solo entorno sintético firmado" />': '<Control label="Recepción de prueba" value="Solo datos sintéticos con firma de seguridad" />',
    'Worker: ${result.claimed} reclamados, ${result.completed} completados.': 'Proceso de salida: ${result.claimed} tomados, ${result.completed} completados.',
    '"Worker reanudado."': '"Proceso de salida reanudado."',
    '"Worker pausado."': '"Proceso de salida pausado."',
    '<EmptyState text="No se pudo cargar la salud del worker." />': '<EmptyState text="No se pudo consultar el estado del proceso de salida." />',
    '<Control label="Endpoint" value={health?.enabled ? "Habilitado" : "Deshabilitado"} />': '<Control label="Recepción" value={health?.enabled ? "Habilitada" : "Deshabilitada"} />',
    '<Control label="Secreto" value={health?.configured ? "Configurado por entorno" : "No configurado"} />': '<Control label="Firma de seguridad" value={health?.configured ? "Configurada por entorno" : "No configurada"} />',
    '<Detail label="Hash payload" value={selected.payloadHash} />': '<Detail label="Huella del contenido" value={selected.payloadHash} />',
    'setNotice("Preview renderizado con datos sintéticos.");': 'setNotice("Vista previa generada con datos sintéticos.");',
    'Score mínimo': 'Puntuación mínima',
    '<Control label="Modo" value={summary.dryRun ? "Preview" : "Ejecución"} />': '<Control label="Modo" value={summary.dryRun ? "Vista previa" : "Ejecución"} />',
    '<Control label="Importación" value="Preview + confirmación" />': '<Control label="Importación" value="Vista previa y confirmación" />',
    'reports: "Dashboard y reportes"': 'reports: "Reportes"',
    '<Control label="Email" value={safety?.selectedEmailProvider ?? "Consultando…"} />': '<Control label="Correo electrónico" value={labelFor(safety?.selectedEmailProvider ?? "NOOP")} />',
    '<Control label="WhatsApp" value={safety?.selectedWhatsAppProvider ?? "Consultando…"} />': '<Control label="WhatsApp" value={labelFor(safety?.selectedWhatsAppProvider ?? "DEEPLINK_ONLY")} />',
    '<Control\n          label="Red real"\n          value={safety?.realNetworkAllowed ? "HABILITADA" : "BLOQUEADA"}\n        />': '<Control\n          label="Conexión a proveedores reales"\n          value={safety?.realNetworkAllowed ? "Habilitada" : "Bloqueada"}\n        />',
    '<Control\n          label="Endpoint de envío"\n          value={safety?.sendEndpointAvailable ? "DISPONIBLE" : "INEXISTENTE"}\n        />': '<Control\n          label="Acción de envío"\n          value={safety?.sendEndpointAvailable ? "Disponible" : "No disponible"}\n        />',
}.items():
    patch(app, old, new)

patch(
    app,
    '<Panel title="Crear exclusión dominante">\n        {error &&',
    '<Panel title="Crear exclusión dominante">\n        <p className="muted">Una exclusión impide el uso comercial del canal en campañas y contactos. No elimina el prospecto ni su historial.</p>\n        {error &&',
)

# Ensure all visible navigation and critical safety phrases are human-readable.
text = (ROOT / app).read_text(encoding="utf-8")
for forbidden in ["window.prompt", "window.confirm", ">NEW<", ">REVIEW_REQUIRED<", ">DUPLICATE<"]:
    if forbidden in text:
        raise RuntimeError(f"Forbidden visible or native UX token remains: {forbidden}")

print("Remote UX postfix checks applied.")
