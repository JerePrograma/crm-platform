import { createHash } from "node:crypto";
import { cp, mkdir, readFile, readdir, rm, stat, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

import { chromium } from "@playwright/test";

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(scriptDirectory, "..", "..");
const validationRoot = path.resolve(repositoryRoot, "validation-output");
const screenshotSource = path.resolve(
  process.env.CRM_GMAIL_SCREENSHOTS_DIR ?? path.join(validationRoot, "gmail-campaign-e2e", "screenshots"),
);
const outputRoot = path.resolve(
  process.env.CRM_GMAIL_MANUAL_OUTPUT ?? path.join(validationRoot, "gmail-campaign-manual"),
);
const pngOutput = path.join(outputRoot, "png");

const screens = [
  ["01-configuracion-sin-oauth.png", "Configuración sin OAuth", "Estado inicial: el servidor todavía no expone una conexión Gmail utilizable."],
  ["02-conectar-google.png", "Conectar cuenta de Google", "Inicio explícito del flujo OAuth desde Configuración."],
  ["03-callback-satisfactorio.png", "Callback satisfactorio", "Retorno seguro al CRM sin tokens, códigos ni state en la URL visible."],
  ["04-cuenta-conectada.png", "Cuenta conectada", "Cuenta sintética vinculada a la organización y lista para ser administrada."],
  ["05-cuenta-predeterminada.png", "Cuenta predeterminada", "Selección explícita de la cuenta remitente predeterminada."],
  ["06-verificacion.png", "Verificación", "Comprobación controlada de identidad y autorización de la cuenta."],
  ["07-reconexion.png", "Reconexión", "Nueva autorización conservando el refresh token anterior cuando el proveedor no emite otro."],
  ["08-revocacion.png", "Revocación", "Revocación local obligatoria y remota best-effort."],
  ["09-mensajes-integraciones.png", "Mensajes e integraciones", "Estado efectivo del proveedor, red, kill switch y cuota interna."],
  ["10-nueva-plantilla.png", "Nueva plantilla", "Creación de una plantilla versionada con datos exclusivamente sintéticos."],
  ["11-previsualizacion.png", "Previsualización", "Vista previa del contenido antes de congelar la audiencia."],
  ["12-nueva-campana.png", "Nueva campaña", "Borrador de campaña que aún no puede ejecutar envíos reales."],
  ["13-selector-remitente.png", "Selector de remitente", "La campaña elige una cuenta Gmail conectada de la misma organización."],
  ["14-modo-real-simulacion.png", "Modo real y simulación", "Diferenciación inequívoca entre una simulación y una ejecución real."],
  ["15-filtros.png", "Filtros", "Criterios que determinan la audiencia antes de congelarla."],
  ["16-audiencia-congelada.png", "Audiencia congelada", "Snapshot revisable que no reemplaza la validación inmediatamente anterior al envío."],
  ["17-excluidos.png", "Destinatarios excluidos", "Razones visibles para canales no contactables o excluidos."],
  ["18-aprobacion.png", "Aprobación", "Aprobación separada y versionada para ejecución real."],
  ["19-confirmacion-envio-real.png", "Confirmación reforzada", "Frase SEND_LIVE_CAMPAIGN requerida por frontend y backend."],
  ["20-campana-programada.png", "Campaña programada", "Fecha, zona, ventana e intervalo quedan visibles antes de ejecutar."],
  ["21-ejecutandose.png", "Campaña ejecutándose", "Progreso individualizado mediante el outbox."],
  ["22-pausada.png", "Campaña pausada", "La pausa impide nuevos claims de destinatarios."],
  ["23-reanudada.png", "Campaña reanudada", "Reanudación auditada sin duplicar mensajes aceptados."],
  ["24-completada.png", "Campaña completada", "Finalización sin equiparar aceptación de Gmail con entrega al buzón."],
  ["25-resultados.png", "Resultados por destinatario", "Estados individuales, correlación, intentos y errores sanitizados."],
  ["26-retry.png", "Reintento", "Respuesta transitoria y próximo intento respetando Retry-After."],
  ["27-error-permanente.png", "Error permanente", "Fallo no reintentable visible y accionable."],
  ["28-resultado-ambiguo.png", "Resultado ambiguo", "El sistema no reintenta automáticamente cuando Gmail pudo haber aceptado el mensaje."],
  ["29-baja.png", "Baja", "Página pública idempotente que crea una exclusión inmediata."],
  ["30-auditoria.png", "Auditoría", "Eventos sanitizados sin tokens, cuerpos ni correos completos."],
  ["31-kill-switch.png", "Kill switch", "Bloqueo efectivo de campañas reales aun cuando otros controles estén habilitados."],
  ["32-falta-permisos.png", "Falta de permisos", "Un rol sin ejecución real no puede iniciar ni revocar cuentas."],
].map(([file, title, description], order) => ({ file, title, description, order: order + 1 }));

function assertUnderValidationRoot(candidate, label) {
  const relative = path.relative(validationRoot, candidate);
  if (relative.startsWith("..") || path.isAbsolute(relative) || relative === "") {
    throw new Error(`${label} must be a child of ${validationRoot}`);
  }
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function sha256(buffer) {
  return createHash("sha256").update(buffer).digest("hex");
}

function pngDimensions(buffer) {
  const signature = "89504e470d0a1a0a";
  if (buffer.length < 24 || buffer.subarray(0, 8).toString("hex") !== signature) {
    throw new Error("Expected a valid PNG screenshot");
  }
  return { width: buffer.readUInt32BE(16), height: buffer.readUInt32BE(20) };
}

function renderHtml(entries, generatedAt) {
  const cards = entries
    .map(
      (entry) => `
      <section class="screen" id="pantalla-${entry.order}">
        <div class="screen-copy">
          <span class="eyebrow">Pantalla ${String(entry.order).padStart(2, "0")}</span>
          <h2>${escapeHtml(entry.title)}</h2>
          <p>${escapeHtml(entry.description)}</p>
        </div>
        <figure>
          <img src="png/${encodeURIComponent(entry.file)}" alt="${escapeHtml(entry.title)}">
          <figcaption>Datos sintéticos · proveedor Google/Gmail falso local</figcaption>
        </figure>
      </section>`,
    )
    .join("\n");
  return `<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Gestudio CRM — Gmail y campañas reales controladas</title>
  <style>
    :root { --ink:#11213d; --muted:#53617c; --line:#d9e0eb; --accent:#1f57d6; --safe:#087f5b; --warn:#8a5b00; font-family: Inter, "Segoe UI", sans-serif; color:var(--ink); background:#eef2f8; }
    * { box-sizing:border-box; }
    body { margin:0; }
    header { min-height:92vh; display:grid; align-content:center; padding:8vw; background:radial-gradient(circle at 90% 15%,#5f83ea55,transparent 38%),linear-gradient(140deg,#0f2147,#173e8d 62%,#2a61d5); color:white; }
    header .kicker { letter-spacing:.13em; text-transform:uppercase; font-weight:800; color:#bcd0ff; }
    h1 { font-size:clamp(2.5rem,6vw,5.5rem); max-width:13ch; line-height:.96; margin:.5rem 0 1.5rem; }
    header p { max-width:65ch; font-size:1.18rem; line-height:1.65; color:#e1e9ff; }
    .notice { display:inline-flex; width:fit-content; margin-top:1rem; padding:.65rem .9rem; border:1px solid #ffffff55; border-radius:999px; background:#ffffff17; font-weight:700; }
    main { width:min(1180px,calc(100% - 2rem)); margin:2rem auto 5rem; }
    .guide { background:white; border:1px solid var(--line); border-radius:20px; padding:clamp(1.5rem,4vw,3rem); margin-bottom:2rem; }
    .guide h2 { font-size:2rem; }
    .guide-grid { display:grid; grid-template-columns:repeat(3,1fr); gap:1.25rem; }
    .guide-grid article { border-left:4px solid var(--accent); padding-left:1rem; }
    .guide-grid h3 { margin-top:0; }
    .guide-grid p, .guide li { color:var(--muted); line-height:1.55; }
    .warning { border-left-color:#f0a500!important; }
    .safe { border-left-color:var(--safe)!important; }
    .screen { background:white; border:1px solid var(--line); border-radius:20px; overflow:hidden; margin:1.5rem 0; break-inside:avoid; }
    .screen-copy { padding:1.5rem 1.75rem 1rem; }
    .screen h2 { margin:.35rem 0 .5rem; font-size:1.6rem; }
    .screen p { margin:0; color:var(--muted); line-height:1.5; }
    .eyebrow { color:var(--accent); text-transform:uppercase; letter-spacing:.1em; font-size:.75rem; font-weight:850; }
    figure { margin:0; border-top:1px solid var(--line); background:#f6f8fc; padding:1rem; }
    img { display:block; width:100%; border:1px solid #cdd6e5; border-radius:10px; background:white; }
    figcaption { margin:.7rem .25rem 0; color:#687691; font-size:.82rem; }
    footer { padding:3rem 8vw; background:#0f2147; color:#dfe8ff; }
    code { background:#eaf0fd; color:#173e8d; padding:.15em .35em; border-radius:4px; }
    @media(max-width:780px){ .guide-grid{grid-template-columns:1fr;} header{min-height:auto;padding:5rem 1.5rem;} }
    @media print {
      @page { size:A4; margin:0; }
      :root { background:white; }
      header { min-height:0; height:297mm; page-break-after:always; -webkit-print-color-adjust:exact; print-color-adjust:exact; }
      main { width:100%; margin:0; }
      .guide { height:297mm; overflow:hidden; border:0; border-radius:0; page-break-after:always; }
      .screen { height:297mm; display:flex; flex-direction:column; overflow:hidden; border:0; border-radius:0; margin:0; page-break-after:always; }
      .screen-copy { padding:12mm 12mm 7mm; }
      figure { flex:1; min-height:0; display:grid; grid-template-rows:minmax(0,1fr) auto; padding:7mm 12mm 12mm; }
      img { width:100%; height:100%; object-fit:contain; }
      footer { display:none; }
    }
  </style>
</head>
<body>
  <header>
    <span class="kicker">Manual de usuario · SEG-001</span>
    <h1>Gmail y campañas reales controladas</h1>
    <p>Guía operativa de Gestudio CRM para conectar una cuenta remitente mediante OAuth 2.0, revisar una audiencia, aprobar una campaña y ejecutar mensajes individualizados por outbox.</p>
    <span class="notice">Evidencia sintética — no se contactó Google ni se enviaron correos reales</span>
  </header>
  <main>
    <section class="guide">
      <span class="eyebrow">Antes de empezar</span>
      <h2>Controles que nunca deben omitirse</h2>
      <div class="guide-grid">
        <article class="safe"><h3>Conexión</h3><p>Solo Administración conecta, verifica, predetermina, reconecta o revoca cuentas. Los tokens permanecen cifrados y nunca aparecen en pantalla.</p></article>
        <article><h3>Revisión</h3><p>Congelá la audiencia, revisá incluidos y excluidos y aprobá nuevamente si cambia plantilla, remitente, Reply-To o configuración material.</p></article>
        <article class="warning"><h3>Ejecución real</h3><p>Requiere permiso específico, flags habilitados, límite positivo, kill switch desactivado y la frase <code>SEND_LIVE_CAMPAIGN</code>.</p></article>
      </div>
      <h3>Interpretación de resultados</h3>
      <ul>
        <li><strong>Aceptado por Gmail</strong> significa que la API aceptó el mensaje; no demuestra entrega al buzón, apertura, lectura ni respuesta.</li>
        <li><strong>Resultado ambiguo</strong> se detiene para revisión manual y no se reintenta automáticamente.</li>
        <li>Una baja crea una exclusión inmediata y cancela mensajes pendientes del mismo canal.</li>
        <li>Para rollback operativo, activá el kill switch, deshabilitá red real y worker, revocá cuentas y volvés a <code>EMAIL_PROVIDER_MODE=NOOP</code>.</li>
      </ul>
    </section>
    ${cards}
  </main>
  <footer>Gestudio CRM · generado ${escapeHtml(generatedAt)} · material sintético de validación local</footer>
</body>
</html>`;
}

function crc32(buffer) {
  let crc = 0xffffffff;
  for (const byte of buffer) {
    crc ^= byte;
    for (let bit = 0; bit < 8; bit += 1) {
      crc = (crc >>> 1) ^ (0xedb88320 & -(crc & 1));
    }
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function dosDateTime() {
  return { date: (1 << 5) | 1, time: 0 };
}

async function createStoredZip(zipPath, rootDirectory, relativeFiles) {
  const locals = [];
  const centrals = [];
  let offset = 0;
  const { date, time } = dosDateTime();
  for (const relativeFile of relativeFiles) {
    const normalized = relativeFile.replaceAll(path.sep, "/");
    const name = Buffer.from(normalized, "utf8");
    const data = await readFile(path.join(rootDirectory, relativeFile));
    const checksum = crc32(data);
    const local = Buffer.alloc(30);
    local.writeUInt32LE(0x04034b50, 0);
    local.writeUInt16LE(20, 4);
    local.writeUInt16LE(0x0800, 6);
    local.writeUInt16LE(0, 8);
    local.writeUInt16LE(time, 10);
    local.writeUInt16LE(date, 12);
    local.writeUInt32LE(checksum, 14);
    local.writeUInt32LE(data.length, 18);
    local.writeUInt32LE(data.length, 22);
    local.writeUInt16LE(name.length, 26);
    local.writeUInt16LE(0, 28);
    locals.push(local, name, data);

    const central = Buffer.alloc(46);
    central.writeUInt32LE(0x02014b50, 0);
    central.writeUInt16LE(20, 4);
    central.writeUInt16LE(20, 6);
    central.writeUInt16LE(0x0800, 8);
    central.writeUInt16LE(0, 10);
    central.writeUInt16LE(time, 12);
    central.writeUInt16LE(date, 14);
    central.writeUInt32LE(checksum, 16);
    central.writeUInt32LE(data.length, 20);
    central.writeUInt32LE(data.length, 24);
    central.writeUInt16LE(name.length, 28);
    central.writeUInt16LE(0, 30);
    central.writeUInt16LE(0, 32);
    central.writeUInt16LE(0, 34);
    central.writeUInt16LE(0, 36);
    central.writeUInt32LE(0, 38);
    central.writeUInt32LE(offset, 42);
    centrals.push(central, name);
    offset += local.length + name.length + data.length;
  }
  const centralDirectory = Buffer.concat(centrals);
  const end = Buffer.alloc(22);
  end.writeUInt32LE(0x06054b50, 0);
  end.writeUInt16LE(0, 4);
  end.writeUInt16LE(0, 6);
  end.writeUInt16LE(relativeFiles.length, 8);
  end.writeUInt16LE(relativeFiles.length, 10);
  end.writeUInt32LE(centralDirectory.length, 12);
  end.writeUInt32LE(offset, 16);
  end.writeUInt16LE(0, 20);
  await writeFile(zipPath, Buffer.concat([...locals, centralDirectory, end]));
}

async function main() {
  assertUnderValidationRoot(screenshotSource, "CRM_GMAIL_SCREENSHOTS_DIR");
  assertUnderValidationRoot(outputRoot, "CRM_GMAIL_MANUAL_OUTPUT");
  const available = new Set(await readdir(screenshotSource));
  const missing = screens.filter(({ file }) => !available.has(file)).map(({ file }) => file);
  if (missing.length > 0) {
    throw new Error(`Missing required synthetic screenshots:\n${missing.join("\n")}`);
  }

  await rm(outputRoot, { force: true, recursive: true });
  await mkdir(pngOutput, { recursive: true });
  const entries = [];
  for (const screen of screens) {
    const source = path.join(screenshotSource, screen.file);
    const destination = path.join(pngOutput, screen.file);
    await cp(source, destination, { force: false });
    const data = await readFile(destination);
    entries.push({
      ...screen,
      ...pngDimensions(data),
      bytes: data.length,
      sha256: sha256(data),
    });
  }

  const generatedAt = new Date().toISOString();
  const htmlPath = path.join(outputRoot, "SEG-001-gmail-campaign-user-manual.html");
  const pdfPath = path.join(outputRoot, "SEG-001-gmail-campaign-user-manual.pdf");
  const indexPath = path.join(outputRoot, "index.json");
  const zipPath = path.join(outputRoot, "SEG-001-gmail-campaign-user-manual.zip");
  await writeFile(htmlPath, renderHtml(entries, generatedAt), "utf8");

  const browserChannel = process.env.CRM_E2E_BROWSER_CHANNEL ?? "chrome";
  const browser = await chromium.launch(browserChannel && browserChannel !== "bundled" ? { channel: browserChannel } : {});
  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
    await page.goto(pathToFileURL(htmlPath).href, { waitUntil: "networkidle" });
    await page.emulateMedia({ media: "print" });
    await page.pdf({
      path: pdfPath,
      format: "A4",
      printBackground: true,
      margin: { top: "0", right: "0", bottom: "0", left: "0" },
      preferCSSPageSize: true,
    });
  } finally {
    await browser.close();
  }

  const htmlData = await readFile(htmlPath);
  const pdfData = await readFile(pdfPath);
  const index = {
    schemaVersion: 1,
    generatedAt,
    syntheticDataOnly: true,
    provider: "local-fake-google",
    warning: "No se conectó Google y no se enviaron correos reales.",
    screens: entries,
    artifacts: [
      { file: path.basename(htmlPath), bytes: htmlData.length, sha256: sha256(htmlData) },
      { file: path.basename(pdfPath), bytes: pdfData.length, sha256: sha256(pdfData) },
    ],
  };
  await writeFile(indexPath, `${JSON.stringify(index, null, 2)}\n`, "utf8");
  const zipEntries = [path.basename(htmlPath), path.basename(pdfPath), path.basename(indexPath), ...entries.map(({ file }) => path.join("png", file))];
  await createStoredZip(zipPath, outputRoot, zipEntries);
  const zipData = await readFile(zipPath);
  const outputStat = await stat(outputRoot);
  process.stdout.write(
    `${JSON.stringify(
      {
        status: "FUNCTIONAL_PASS",
        output: outputRoot,
        outputCreatedAt: outputStat.birthtime.toISOString(),
        screenshots: entries.length,
        html: index.artifacts[0],
        pdf: index.artifacts[1],
        zip: { file: path.basename(zipPath), bytes: zipData.length, sha256: sha256(zipData) },
      },
      null,
      2,
    )}\n`,
  );
}

main().catch((error) => {
  process.stderr.write(`${error.message}\n`);
  process.exitCode = 1;
});
