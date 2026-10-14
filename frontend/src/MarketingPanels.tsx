import { useEffect, useState } from "react";
import { changeTaskStatus, getProspect } from "./api";
import { humanizeError, labelFor } from "./uiLabels";
import { pendingTasks, taskTiming } from "./marketingWork";
import type { BusinessProfile, DashboardReport, SessionUser, Task } from "./types";

export type MarketingDestination = "prospects" | "pipeline" | "campaigns" | "messages" | "imports" | "reports" | "help" | "agenda";

export function ScalarisBusinessCard({ profile }: { profile: BusinessProfile | null }) {
  if (!profile || profile.code !== "SCALARIS") return null;
  return (
    <section className="business-card" aria-label="Referencia comercial de Scalaris">
      <div><span className="eyebrow">Nuestra propuesta</span><h2>{profile.tagline}</h2><p>{profile.descriptor}</p></div>
      <div className="business-contact"><strong>{profile.coverage}</strong><span>WhatsApp oficial: {profile.whatsappDisplay}</span></div>
    </section>
  );
}

export function MarketingOverview({
  report, onNavigate, session,
}: {
  report: DashboardReport | null;
  onNavigate: (destination: MarketingDestination) => void;
  session: SessionUser;
}) {
  return (
    <section className="panel">
      <div className="panel-heading"><div><h2>Tu próximo paso</h2><p className="muted">Concentrá el trabajo en consultas, seguimiento y oportunidades para Scalaris.</p></div><button className="link-button" onClick={() => onNavigate("help")}>Ver guía de Marketing</button></div>
      <div className="work-actions">
        <button onClick={() => onNavigate("agenda")}><strong>Revisar seguimientos</strong><span>{report ? `${report.tasks.overdue ?? 0} vencidas · ${report.tasks.today ?? 0} para hoy` : "Abrí la agenda y organizá la próxima acción."}</span></button>
        <button onClick={() => onNavigate("prospects")}><strong>Gestionar consultas</strong><span>{report ? `${report.prospectSummary.replied ?? 0} respondieron · ${report.prospectSummary.unowned ?? 0} sin responsable` : "Cargá particulares, comercios y empresas."}</span></button>
        {session.permissions.includes("CAMPAIGN_READ") && <button onClick={() => onNavigate("campaigns")}><strong>Preparar una campaña</strong><span>Elegí una plantilla y revisá la audiencia antes de simular.</span></button>}
        <button onClick={() => onNavigate("pipeline")}><strong>Revisar oportunidades</strong><span>{report ? `${report.prospectSummary.proposals ?? 0} propuestas · ${report.opportunitySummary.stalled ?? 0} oportunidades estancadas` : "Registrá el servicio, presupuesto y próximo paso."}</span></button>
      </div>
    </section>
  );
}

export function MarketingAgenda({
  tasks, session, onChanged, onOpenProspect, compact = false,
}: {
  tasks: Task[];
  session: SessionUser;
  onChanged: () => Promise<void>;
  onOpenProspect: (id: string) => Promise<void>;
  compact?: boolean;
}) {
  const [onlyMine, setOnlyMine] = useState(true);
  const [timing, setTiming] = useState("");
  const [visibleCount, setVisibleCount] = useState(compact ? 5 : 25);
  const [names, setNames] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const pending = pendingTasks(tasks, onlyMine ? session.userId : undefined);
  const filtered = pending.filter((task) => !timing || taskTiming(task) === timing);
  const shown = filtered.slice(0, visibleCount);
  const shownIds = [...new Set(shown.map((task) => task.prospectId))].join(",");

  useEffect(() => {
    let active = true;
    const ids = shownIds.split(",").filter(Boolean);
    void Promise.allSettled(ids.map(async (id) => [id, (await getProspect(id)).displayName] as const))
      .then((results) => {
        if (!active) return;
        const resolved = results.flatMap((result) => result.status === "fulfilled" ? [result.value] : []);
        setNames((current) => ({ ...current, ...Object.fromEntries(resolved) }));
      });
    return () => { active = false; };
  }, [shownIds]);

  async function complete(task: Task) {
    setBusy(task.id); setError(null); setNotice(null);
    try {
      await changeTaskStatus(task, "COMPLETED");
      setNotice("Tarea completada. Registrá la respuesta o el resultado en la ficha del prospecto.");
      await onChanged();
    } catch (caught) { setError(humanizeError(caught instanceof Error ? caught.message : String(caught))); }
    finally { setBusy(null); }
  }

  return (
    <section className="panel" aria-label="Agenda de seguimiento">
      <div className="panel-heading"><div><h2>{compact ? "Seguimientos prioritarios" : "Agenda de seguimiento"}</h2><p className="muted">Abrí la ficha para ver el contexto antes de realizar el contacto.</p></div></div>
      <div className="toolbar agenda-filters">
        <label>Responsable<select value={onlyMine ? "mine" : "all"} onChange={(event) => setOnlyMine(event.target.value === "mine")}><option value="mine">Mis tareas</option><option value="all">Todas las tareas</option></select></label>
        <label>Vencimiento<select value={timing} onChange={(event) => setTiming(event.target.value)}><option value="">Todos los pendientes</option><option value="overdue">Vencidas</option><option value="today">Para hoy</option><option value="upcoming">Próximas</option></select></label>
      </div>
      {error && <p className="alert error" role="alert">{error}</p>}
      {notice && <p className="alert success" role="status">{notice}</p>}
      {shown.length ? <div className="agenda-list">{shown.map((task) => <article className="agenda-item" key={task.id}>
        <div><span className={`badge badge-${taskTiming(task)}`}>{({ overdue: "Vencida", today: "Hoy", upcoming: "Próxima" })[taskTiming(task)]}</span><h3>{task.title}</h3><p>{names[task.prospectId] ?? "Prospecto vinculado"} · {new Date(task.dueAt).toLocaleString("es-AR", { timeZone: "America/Argentina/Buenos_Aires", dateStyle: "medium", timeStyle: "short" })}</p><small>{labelFor(task.priority)} prioridad · {labelFor(task.status)}</small>{task.description && <p>{task.description}</p>}</div>
        <div className="action-row"><button className="secondary-button" onClick={() => void onOpenProspect(task.prospectId)}>Abrir ficha</button>{session.permissions.includes("ACTIVITY_WRITE") && <button className="link-button" disabled={busy === task.id} onClick={() => void complete(task)}>{busy === task.id ? "Guardando…" : "Completar tarea"}</button>}</div>
      </article>)}</div> : <div className="empty-state"><strong>{timing ? "No hay tareas con ese vencimiento." : onlyMine ? "No tenés tareas pendientes." : "No hay seguimientos pendientes."}</strong><p>Creá una tarea desde la ficha de un prospecto, con responsable y vencimiento, para dejar registrado el próximo paso.</p></div>}
      {filtered.length > shown.length && <div className="pagination"><span>Mostrando {shown.length} de {filtered.length} pendientes</span>{!compact && <button className="secondary-button" onClick={() => setVisibleCount((count) => count + 25)}>Ver más tareas</button>}</div>}
    </section>
  );
}

export function MarketingHelp({ profile, session, onNavigate }: {
  profile: BusinessProfile | null;
  session: SessionUser;
  onNavigate: (destination: MarketingDestination) => void;
}) {
  const steps: Array<{ title: string; text: string; action: string; destination: MarketingDestination; permission?: string }> = [
    { title: "1. Registrá la consulta o el prospecto", text: "Cargá el nombre de la persona o del negocio, la localidad y el origen: WhatsApp, web, redes, recomendación o prospección. La ficha sirve tanto para particulares como para empresas.", action: "Abrir prospectos", destination: "prospects" },
    { title: "2. Clasificá la necesidad", text: "Asigná Servicio técnico, Desarrollo o Venta a pedido desde las etiquetas de la ficha. Podés combinar servicios y sumar el tipo de cliente o la zona. Escribí la necesidad concreta, su alcance y lo que falta confirmar.", action: "Clasificar en la ficha", destination: "prospects" },
    { title: "3. Confirmá el canal de contacto", text: "Agregá el contacto y su correo, teléfono o WhatsApp. Registrá el consentimiento que realmente exista. Un dato público no equivale a autorización. Si pidió la baja o tiene una exclusión, detené el contacto.", action: "Revisar contactos", destination: "prospects" },
    { title: "4. Dejá un seguimiento concreto", text: "Creá una tarea con responsable y vencimiento: revisar una consulta, acordar diagnóstico, relevar un desarrollo o confirmar una cotización. Registrá el resultado como actividad o nota y revisá la agenda todos los días.", action: "Abrir agenda", destination: "agenda" },
    { title: "5. Registrá la oportunidad", text: "Cuando hay una necesidad concreta, creá una oportunidad con el servicio, el valor estimado y la fecha prevista. Usá Diagnóstico, Demostración o Propuesta según el caso y registrá el motivo del cierre.", action: "Abrir oportunidades", destination: "pipeline" },
    { title: "6. Prepará el mensaje o la campaña", text: "Usá las plantillas Scalaris como punto de partida y adaptá el mensaje al caso. Revisá la audiencia, las exclusiones y la vista previa. Una simulación comprueba el recorrido sin contactar a nadie.", action: "Abrir campañas", destination: "campaigns", permission: "CAMPAIGN_READ" },
    { title: "7. Medí y dejá trazabilidad", text: "Revisá consultas, tareas vencidas, respuestas y oportunidades. El CRM conserva la actividad registrada; un borrador no es un envío y la aceptación de un correo no prueba entrega, lectura o respuesta.", action: "Abrir reportes", destination: "reports", permission: "REPORT_READ" },
  ];
  return <section className="stack">
    <ScalarisBusinessCard profile={profile} />
    <section className="panel"><h2>Cómo trabajar con el CRM de Scalaris</h2><p>El objetivo es que cada consulta tenga contexto, un responsable y un próximo paso. Los datos se guardan en la base local y se comparten entre los usuarios de tu organización.</p><div className="guide-grid">{steps.filter((step) => !step.permission || session.permissions.includes(step.permission)).map((step) => <article className="guide-step" key={step.title}><h3>{step.title}</h3><p>{step.text}</p><button className="link-button" onClick={() => onNavigate(step.destination)}>{step.action}</button></article>)}</div></section>
    <section className="two-column equal">
      <section className="panel"><h2>Qué ofrecemos</h2><dl className="service-guide"><div><dt>Servicio técnico</dt><dd>Diagnóstico, mantenimiento, reparación y mejoras de PC y notebooks. Armado de PC a medida. La reparación física requiere recibir el equipo.</dd></div><div><dt>Desarrollo</dt><dd>Páginas web, tiendas online, sistemas, aplicaciones, automatizaciones, integraciones y mantenimiento. Definí el proceso o problema a resolver antes de hablar de tecnología.</dd></div><div><dt>Venta a pedido</dt><dd>Equipos, componentes, periféricos, almacenamiento, accesorios y productos de telefonía por encargo. Disponibilidad y condiciones se consultan antes de cotizar.</dd></div></dl></section>
      <section className="panel"><h2>Criterios comerciales</h2><ul className="guide-rules"><li>Retiro y entrega de equipos a coordinar según servicio, localidad y caso.</li><li>Atención remota cuando el trabajo lo permite.</li><li>Confirmá presupuesto, traslado, repuestos y condiciones para cada caso.</li><li>Usá un mensaje concreto y una sola acción: consultar, coordinar o pedir presupuesto.</li><li>No prometas stock, gratuidad, horarios, plazos, garantías ni resultados sin confirmación.</li></ul>{profile?.operatingGuidance?.length ? <details className="technical-details"><summary>Referencia operativa de la organización</summary><ul>{profile.operatingGuidance.map((guidance) => <li key={guidance}>{guidance}</li>)}</ul></details> : null}</section>
    </section>
    <section className="panel"><h2>Contacto manual y comunicaciones</h2><p>Los envíos automáticos permanecen bloqueados en el despliegue local. Podés guardar borradores, simular campañas y generar un enlace para abrir tu aplicación de correo o WhatsApp. Revisá el destinatario y el contenido; la decisión de enviar se toma en esa aplicación. Después registrá la actividad en la ficha.</p>{session.permissions.includes("MESSAGE_DRAFT") && <button className="secondary-button" onClick={() => onNavigate("messages")}>Preparar un borrador</button>}<p className="muted">Si no aparece una acción, tu rol puede tener acceso de consulta. Pedile al administrador que revise tu cuenta; no compartan contraseñas.</p></section>
  </section>;
}
