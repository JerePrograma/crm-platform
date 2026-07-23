#!/usr/bin/env python3
from pathlib import Path

path = Path(__file__).with_name("remote-ux-overhaul.py")
text = path.read_text(encoding="utf-8")


def replace_once(old: str, new: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected one preflight match, found {count}: {old[:100]!r}")
    text = text.replace(old, new, 1)


replace_once(
'''for old, new in {
    "Dashboard": "Resumen",
    "Outbox y workers": "Bandeja de salida",
    "Inbound y quarantine": "Mensajes recibidos",
}.items():
    app = read(app_path)
    write(app_path, app.replace(old, new))
''',
'''replace_once(app_path, "              Dashboard\\n", "              Resumen\\n")
replace_once(app_path, "              Outbox y workers\\n", "              Bandeja de salida\\n")
replace_once(app_path, "              Inbound y quarantine\\n", "              Mensajes recibidos\\n")
''',
)
replace_once(
'''replace_once(app_path, '{error && <div className="alert error">{error}</div>}', '{error && <div className="alert error" role="alert">{error}</div>}')''',
'''app = read(app_path)
write(app_path, app.replace('{error && <div className="alert error">{error}</div>}', '{error && <div className="alert error" role="alert">{error}</div>}'))''',
)
replace_once(
'''replace_once(app_path, '<option value="EMAIL">EMAIL</option>', '<option value="EMAIL">Correo electrónico</option>')
replace_once(app_path, '<option value="WHATSAPP">WHATSAPP</option>', '<option value="WHATSAPP">WhatsApp</option>')''',
'''app = read(app_path)
app = app.replace('<option value="EMAIL">EMAIL</option>', '<option value="EMAIL">Correo electrónico</option>')
app = app.replace('<option value="WHATSAPP">WHATSAPP</option>', '<option value="WHATSAPP">WhatsApp</option>')
write(app_path, app)''',
)
correlation = '''replace_once(app_path, '<Detail label="Correlation ID" value={selected.correlationId} />', '<Detail label="Identificador de seguimiento" value={selected.correlationId} />')'''
if text.count(correlation) != 2:
    raise RuntimeError(f"Expected two correlation replacements, found {text.count(correlation)}")
text = text.replace(
    correlation,
    '''app = read(app_path)
write(app_path, app.replace('<Detail label="Correlation ID" value={selected.correlationId} />', '<Detail label="Identificador de seguimiento" value={selected.correlationId} />'))''',
    1,
)
text = text.replace(correlation, "", 1)
replace_once(
'''replace_once(app_path, '    dashboard: "Resumen",', '    dashboard: "Resumen comercial",')
replace_once(app_path, '    outbox: "Bandeja de salida",', '    outbox: "Bandeja de salida",')
replace_once(app_path, '    inbound: "Mensajes recibidos",', '    inbound: "Mensajes recibidos",')''',
'''replace_once(app_path, '    dashboard: "Dashboard",', '    dashboard: "Resumen comercial",')
replace_once(app_path, '    outbox: "Outbox y workers",', '    outbox: "Bandeja de salida",')
replace_once(app_path, '    inbound: "Inbound y quarantine",', '    inbound: "Mensajes recibidos",')''',
)
replace_once(
'''              String textValue = value == null ? null : trim(String.valueOf(value));
              result.put(normalizeKey(textKey), textValue);''',
'''              String textValue = value == null ? null : trim(String.valueOf(value));
              if (textValue != null) {
                result.put(normalizeKey(textKey), textValue);
              }''',
)

path.write_text(text, encoding="utf-8")
print("Remote UX transformation script preflight fixes applied.")
