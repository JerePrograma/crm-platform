from pathlib import Path


def replace_exact(path: str, old: str, new: str, expected: int = 1) -> None:
    file_path = Path(path)
    source = file_path.read_text(encoding="utf-8")
    count = source.count(old)
    if count != expected:
        raise RuntimeError(
            f"Expected {expected} match(es) in {path}, found {count}: {old[:140]!r}"
        )
    file_path.write_text(source.replace(old, new), encoding="utf-8")


app = "frontend/src/App.tsx"
replace_exact(app, '<Metric label="Interés o pipeline" value={dashboard.interested} />', '<Metric label="Prospectos con interés" value={dashboard.interested} />')
replace_exact(app, '<Control label="Importación" value="Preview + confirmación" />', '<Control label="Importación" value="Vista previa y confirmación" />')
replace_exact(app, '<Control label="Sesión" value="Cookie HttpOnly + CSRF" />', '<Control label="Sesión" value="Protegida y aislada por organización" />')
replace_exact(app, '<ReportMap title="Outbox por estado" values={report.outbox} />', '<ReportMap title="Bandeja de salida por estado" values={report.outbox} />')
replace_exact(
    app,
    'Los bloqueos de entorno dominan esta pantalla. Ningún usuario puede habilitar envíos reales desde la API o la UI.',
    'Los bloqueos de seguridad tienen prioridad. Ningún usuario puede habilitar envíos reales desde esta pantalla ni desde otras funciones del sistema.',
)
replace_exact(app, '<option value="en-US">English US</option><option value="en">English</option>', '<option value="en-US">Inglés (Estados Unidos)</option><option value="en">Inglés</option>')
replace_exact(
    app,
    '<Panel title="Integraciones"><div className="control-grid"><Control label="Gmail" value="Adaptador implementado, no conectado" /><Control label="WhatsApp Cloud" value="Adaptador implementado, no conectado" /><Control label="Webhook fake" value="Solo entorno sintético firmado" /></div></Panel>',
    '<Panel title="Integraciones"><div className="control-grid"><Control label="Gmail" value="Disponible, sin conexión externa" /><Control label="WhatsApp" value="Disponible, sin conexión externa" /><Control label="Recepción de prueba" value="Solo entorno sintético firmado" /></div></Panel>',
)
replace_exact(app, '<EmptyState text="No se pudo cargar la salud del worker." />', '<EmptyState text="No se pudo cargar el estado del proceso automático." />')
replace_exact(app, '<Control label="Email" value={safety?.selectedEmailProvider ?? "Consultando…"} />', '<Control label="Correo electrónico" value={labelFor(safety?.selectedEmailProvider ?? "NOOP")} />')
replace_exact(app, 'setNotice("Preview renderizado con datos sintéticos.");', 'setNotice("Vista previa generada con datos sintéticos.");')
replace_exact(app, '''<label>
                Score mínimo''', '''<label>
                Puntuación mínima''')
replace_exact(app, '`Simulación completa: ${result.includedCount} borradores fake y ${result.excludedCount} bloqueados.`', '`Simulación completa: ${result.includedCount} borradores de simulación y ${result.excludedCount} bloqueados.`')
replace_exact(app, 'Simular con fake', 'Simular resultado')
replace_exact(app, '<option value="EMAIL">Email</option>', '<option value="EMAIL">Correo electrónico</option>')
replace_exact(app, '<Control label="Proveedor" value={health?.provider ?? "FAKE_INBOUND"} />', '<Control label="Proveedor" value={labelFor(health?.provider ?? "FAKE_INBOUND")} />')
replace_exact(app, '<Panel title="Inbound y quarantine">', '<Panel title="Mensajes recibidos pendientes o en revisión">')
replace_exact(app, '<EmptyState text="No hay mensajes inbound." />', '<EmptyState text="No hay mensajes recibidos." />')
replace_exact(app, '<EmptyState text="Seleccioná un mensaje recibido para ver metadata sanitizada." />', '<EmptyState text="Seleccioná un mensaje recibido para ver sus datos operativos." />')

labels = "frontend/src/uiLabels.ts"
replace_exact(
    labels,
    '  PROCESSING: "Procesando",\n  SUCCEEDED: "Completado",',
    '  PROCESSING: "Procesando",\n  PROCESSED: "Procesado",\n  SUCCEEDED: "Completado",',
)
replace_exact(
    labels,
    '  ASSOCIATED: "Asociado",\n  DISCARDED: "Descartado",',
    '  ASSOCIATED: "Asociado",\n  AMBIGUOUS: "Coincidencia ambigua",\n  NOT_FOUND: "Sin coincidencia",\n  DISCARDED: "Descartado",',
)
replace_exact(
    labels,
    '  FAKE_INBOUND: "Recepción de prueba",\n  STOP: "Finalizar secuencia",',
    '  FAKE_INBOUND: "Recepción de prueba",\n  DRAFT_CREATED: "Borrador creado",\n  PROVIDER_DRAFT_CREATED: "Borrador creado en el proveedor",\n  BLOCKED_BY_KILL_SWITCH: "Bloqueado por la protección de emergencia",\n  BLOCKED_BY_CONFIGURATION: "Bloqueado por la configuración",\n  BLOCKED_BY_EXCLUSION: "Bloqueado por una exclusión",\n  BLOCKED_BY_POLICY: "Bloqueado por la política de seguridad",\n  STOP: "Finalizar secuencia",',
)

test = "frontend/tests/complete-crm.spec.ts"
replace_exact(test, 'name: "Dashboard"', 'name: "Resumen comercial"')
replace_exact(test, 'page.getByText("Envíos bloqueados", { exact: true })', 'page.getByText("Los envíos reales están bloqueados", { exact: true })')
replace_exact(test, 'page.getByLabel("Email", { exact: true })', 'page.getByLabel("Correo electrónico", { exact: true })')
replace_exact(test, 'page.getByText("Contacto agregado.")', 'page.getByText("Contacto agregado y elegibilidad actualizada.")')
replace_exact(
    test,
    '''  const due = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString().slice(0, 16);
  await page.getByLabel("Nueva tarea").fill(`Tarea synthetic ${suffix}`);
  await page.getByLabel("Vencimiento").fill(due);
  await page.getByRole("button", { name: "Crear tarea" }).click();''',
    '''  const due = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString().slice(0, 16);
  const tasksPanel = page.locator("details.disclosure-panel").filter({ hasText: "Tareas de seguimiento" });
  await tasksPanel.locator("summary").click();
  await tasksPanel.getByLabel("Nueva tarea").fill(`Tarea synthetic ${suffix}`);
  await tasksPanel.getByLabel("Vencimiento").fill(due);
  await tasksPanel.getByRole("button", { name: "Crear tarea" }).click();''',
)
replace_exact(
    test,
    '''  await page.getByLabel("Resumen de actividad").fill(`Actividad synthetic ${suffix}`);
  await page.getByRole("button", { name: "Registrar actividad" }).click();''',
    '''  const activityPanel = page.locator("details.disclosure-panel").filter({ hasText: "Actividad y notas" });
  await activityPanel.locator("summary").click();
  await activityPanel.getByLabel("Resumen", { exact: true }).fill(`Actividad synthetic ${suffix}`);
  await activityPanel.getByRole("button", { name: "Registrar actividad" }).click();''',
)
replace_exact(test, 'await page.getByLabel("Nota").fill(`Nota synthetic ${suffix}`);', 'await activityPanel.getByLabel("Nota").fill(`Nota synthetic ${suffix}`);')
replace_exact(test, 'await page.getByRole("button", { name: "Agregar nota" }).click();', 'await activityPanel.getByRole("button", { name: "Agregar nota" }).click();')
replace_exact(
    test,
    '''  for (const status of ["QUALIFYING", "READY_TO_CONTACT", "CONTACTED"] as const) {
    await page.getByRole("button", { name: `Pasar a ${status}` }).click();
  }
  await expect(page.getByRole("button", { name: "Pasar a REPLIED" })).toBeVisible();''',
    '''  for (const status of ["En calificación", "Listo para contactar", "Contactado"] as const) {
    await page.getByRole("button", { name: `Pasar a ${status}` }).click();
  }
  await expect(page.getByRole("button", { name: "Pasar a Respondió" })).toBeVisible();''',
)
replace_exact(
    test,
    '''  await page.getByRole("button", { name: "Ejecutar preview" }).click();
  await expect(page.getByText("Preview", { exact: true })).toBeVisible();
  page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "Importar con confirmación" }).click();
  await expect(page.getByText("Ejecución", { exact: true })).toBeVisible();
  const duplicateRow = page
    .getByRole("row")
    .filter({ hasText: `E2E Syntetic ${suffix}` });
  const linkDuplicates = duplicateRow.getByRole("button", { name: "Vincular" });
  await expect(linkDuplicates).toHaveCount(2);
  await linkDuplicates.first().click();
  await expect(linkDuplicates).toHaveCount(1);''',
    '''  await page.getByRole("button", { name: "Generar vista previa" }).click();
  await expect(page.getByText("Vista previa", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Ejecutar importación" }).click();
  const importDialog = page.getByRole("dialog", { name: "Ejecutar importación" });
  await expect(importDialog).toBeVisible();
  await importDialog.getByRole("button", { name: "Ejecutar importación" }).click();
  await expect(page.getByText("Importación ejecutada", { exact: true })).toBeVisible();
  const duplicateReview = page
    .locator("article.duplicate-review-card")
    .filter({ hasText: `E2E Syntetic ${suffix}` });
  await expect(duplicateReview).toBeVisible();
  await duplicateReview.getByRole("button", { name: "Vincular con el existente" }).click();
  const linkDialog = page.getByRole("dialog", { name: "Vincular con el existente" });
  await expect(linkDialog).toBeVisible();
  await linkDialog.getByRole("button", { name: "Vincular con el existente" }).click();
  await expect(duplicateReview).toHaveCount(0);''',
)
replace_exact(test, 'page.getByRole("button", { name: "Pipeline" })', 'page.getByRole("button", { name: "Oportunidades" })')
replace_exact(test, 'opportunityPanel.getByLabel("Nombre")', 'opportunityPanel.getByLabel("Nombre de la oportunidad")')
replace_exact(test, 'opportunityPanel.getByLabel("Valor estimado ARS")', 'opportunityPanel.getByLabel("Valor estimado en ARS")')
replace_exact(
    test,
    '''  for (const stage of ["DISCOVERY", "DEMO", "PROPOSAL", "WON"] as const) {
    const card = page.locator("article.opportunity-card").filter({ hasText: `Venta synthetic ${suffix}` });
    if (stage === "WON") page.once("dialog", (dialog) => dialog.accept("Cierre sintético E2E"));
    await card.getByRole("button", { name: stage, exact: true }).click();
    await expect(card).toBeVisible();
    if (stage === "WON") await expect(card).toContainText("100%");
  }''',
    '''  for (const action of ["Mover a Diagnóstico", "Mover a Demostración", "Mover a Propuesta"] as const) {
    const card = page.locator("article.opportunity-card").filter({ hasText: `Venta synthetic ${suffix}` });
    await card.getByRole("button", { name: action, exact: true }).click();
    await expect(card).toBeVisible();
  }
  const wonCard = page.locator("article.opportunity-card").filter({ hasText: `Venta synthetic ${suffix}` });
  await wonCard.getByRole("button", { name: "Marcar como ganada" }).click();
  const closeDialog = page.getByRole("dialog", { name: "Registrar oportunidad ganada" });
  await closeDialog.getByLabel("Motivo del cierre").fill("Cierre sintético E2E");
  await closeDialog.getByRole("button", { name: "Registrar cierre" }).click();
  await expect(wonCard).toContainText("100%");''',
)
replace_exact(test, 'toContainText("DRAFT")', 'toContainText("Borrador")')
replace_exact(
    test,
    '''  page.once("dialog", (dialog) => dialog.accept());
  await campaignCard().getByRole("button", { name: "Congelar audiencia" }).click();
  await expect(campaignCard()).toContainText("READY_FOR_REVIEW");
  page.once("dialog", (dialog) => dialog.accept());
  await campaignCard().getByRole("button", { name: "Aprobar" }).click();
  await expect(campaignCard()).toContainText("APPROVED");''',
    '''  await campaignCard().getByRole("button", { name: "Congelar audiencia" }).click();
  const audienceDialog = page.getByRole("dialog", { name: "Confirmar audiencia" });
  await audienceDialog.getByRole("button", { name: "Confirmar audiencia" }).click();
  await expect(campaignCard()).toContainText("Listo para revisión");
  await campaignCard().getByRole("button", { name: "Aprobar" }).click();
  const approvalDialog = page.getByRole("dialog", { name: "Aprobar para simulación" });
  await approvalDialog.getByRole("button", { name: "Aprobar simulación" }).click();
  await expect(campaignCard()).toContainText("Aprobado");''',
)
replace_exact(test, 'page.getByRole("button", { name: "Outbox y workers" })', 'page.getByRole("button", { name: "Bandeja de salida" })')
replace_exact(test, 'page.getByText("No existe una acción para forzar providers reales")', 'page.getByText("No existe una acción para forzar proveedores externos")')
replace_exact(test, 'page.getByRole("button", { name: "Inbound y quarantine" })', 'page.getByRole("button", { name: "Mensajes recibidos" })')
replace_exact(test, 'page.getByText("FAKE_INBOUND", { exact: true })', 'page.getByText("Recepción de prueba", { exact: true })')
replace_exact(test, 'toContainText("EMAIL")', 'toContainText("Correo electrónico")')
replace_exact(test, 'toContainText("PROCESSED")', 'toContainText("Procesado")')
replace_exact(test, 'filter({ hasText: "REPLIED" })', 'filter({ hasText: "Respondió" })')
replace_exact(
    test,
    '''  await expect(page.getByText("Ningún usuario puede habilitar envíos reales")).toBeVisible();
  await expect(page.getByText("Adaptador implementado, no conectado").first()).toBeVisible();
  await expect(page.getByText("false", { exact: true }).first()).toBeVisible();''',
    '''  await expect(page.getByText("Ningún usuario puede habilitar envíos reales")).toBeVisible();
  await expect(page.getByText("Disponible, sin conexión externa").first()).toBeVisible();
  await expect(page.getByText("Bloqueados", { exact: true }).first()).toBeVisible();''',
)

print("Final operator UX and E2E patch applied.")
