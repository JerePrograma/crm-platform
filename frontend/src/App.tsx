import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type FormEvent,
} from "react";
import {
  approveCampaign,
  changePassword,
  changeTaskStatus,
  createActivity,
  createContact,
  createCampaign,
  createExclusion,
  createManualMessageLink,
  createMessageDraft,
  createNote,
  createOpportunity,
  createProspect,
  createTemplate,
  createTask,
  createUser,
  getImportRows,
  getMessagingSafety,
  getCampaignAudience,
  getCampaignSequence,
  getPendingDuplicateReviews,
  getPipelineMetrics,
  getProspect,
  getTimeline,
  getSession,
  importProspects,
  isConflict,
  listAuditEvents,
  listCampaigns,
  listExclusions,
  listOpportunities,
  listTemplates,
  listProspects,
  listContacts,
  listTasks,
  listUsers,
  login,
  logout,
  resolveDuplicateReview,
  replaceCampaignSequence,
  previewTemplate,
  setUserActive,
  simulateCampaign,
  simulateMessage,
  transitionProspect,
  transitionOpportunity,
  freezeCampaignAudience,
  updateProspect,
  associateInbound,
  cancelOutboxEvent,
  discardInbound,
  getInbound,
  getOutboxEvent,
  getOutboxMetrics,
  getWebhookHealth,
  getWorkerState,
  listInbound,
  listOutbox,
  requeueOutboxEvent,
  retryInboundAssociation,
  runOutboxWorker,
  setOutboxWorkerPaused,
  assignTag,
  createTag,
  dashboardCsvUrl,
  deactivateTag,
  getDashboardReport,
  getOrganizationSettings,
  listProspectTags,
  listTags,
  unassignTag,
  updateOrganizationSettings,
} from "./api";
import type {
  AuditEvent,
  AudienceRecipient,
  Campaign,
  CampaignChannel,
  CampaignSimulation,
  CampaignSequenceStep,
  Contact,
  DuplicateReview,
  DuplicateResolutionAction,
  Exclusion,
  ImportRow,
  ImportSummary,
  MessageTemplate,
  ManualMessageLink,
  MessageRecord,
  MessagingSafety,
  Opportunity,
  OpportunityStage,
  PipelineMetrics,
  Prospect,
  ProspectStatus,
  RenderedTemplate,
  SessionUser,
  Task,
  TimelineItem,
  User,
  InboundMessage,
  OutboxEvent,
  OutboxMetric,
  OutboxStatus,
  WebhookHealth,
  WorkerState,
  CrmTag,
  DashboardReport,
  OrganizationSettings,
} from "./types";

type Tab =
  | "dashboard"
  | "prospects"
  | "pipeline"
  | "campaigns"
  | "messages"
  | "outbox"
  | "inbound"
  | "imports"
  | "exclusions"
  | "audit"
  | "reports"
  | "settings"
  | "users"
  | "account";

const opportunityStages: OpportunityStage[] = [
  "QUALIFICATION",
  "DISCOVERY",
  "DEMO",
  "PROPOSAL",
  "NEGOTIATION",
  "WON",
  "LOST",
];

const prospectStatuses: ProspectStatus[] = [
  "NEW",
  "QUALIFYING",
  "READY_TO_CONTACT",
  "FOLLOW_UP",
  "DEMO_PROPOSED",
  "DEMO_SCHEDULED",
  "PROPOSAL",
  "CUSTOMER",
  "NEEDS_ENRICHMENT",
  "READY_FOR_REVIEW",
  "APPROVED",
  "CONTACTED",
  "REPLIED",
  "INTERESTED",
  "QUALIFIED",
  "TRIAL_ACTIVE",
  "QUOTED",
  "NEGOTIATION",
  "WON",
  "LOST",
  "NO_RESPONSE",
  "BOUNCED",
  "UNSUBSCRIBED",
  "DO_NOT_CONTACT",
  "INVALID",
  "DUPLICATE",
  "ARCHIVED",
];

export function App() {
  const [session, setSession] = useState<SessionUser | null | undefined>(undefined);
  const [tab, setTab] = useState<Tab>("dashboard");
  const [prospects, setProspects] = useState<Prospect[]>([]);
  const [exclusions, setExclusions] = useState<Exclusion[]>([]);
  const [auditEvents, setAuditEvents] = useState<AuditEvent[]>([]);
  const [duplicateReviews, setDuplicateReviews] = useState<DuplicateReview[]>([]);
  const [opportunities, setOpportunities] = useState<Opportunity[]>([]);
  const [pipelineMetrics, setPipelineMetrics] = useState<PipelineMetrics | null>(null);
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [templates, setTemplates] = useState<MessageTemplate[]>([]);
  const [selectedProspect, setSelectedProspect] = useState<Prospect | null>(null);
  const [statusFilter, setStatusFilter] = useState<ProspectStatus | "">("");
  const initialQuery = new URLSearchParams(window.location.search).get("q") ?? "";
  const [searchInput, setSearchInput] = useState(initialQuery);
  const [searchQuery, setSearchQuery] = useState(initialQuery);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(
    async (filter: ProspectStatus | "" = statusFilter, query = searchQuery) => {
      setLoading(true);
      setError(null);
      try {
        const [
          prospectPage,
          exclusionPage,
          audits,
          reviews,
          opportunityList,
          metrics,
          campaignList,
          templateList,
        ] =
          await Promise.all([
          listProspects(filter || undefined, query || undefined),
          listExclusions(),
          session?.permissions.includes("AUDIT_READ") ? listAuditEvents() : Promise.resolve([]),
          session?.permissions.includes("DUPLICATE_RESOLVE")
            ? getPendingDuplicateReviews()
            : Promise.resolve([]),
          listOpportunities(),
          session?.permissions.includes("REPORT_READ")
            ? getPipelineMetrics()
            : Promise.resolve(null),
          session?.permissions.includes("CAMPAIGN_READ")
            ? listCampaigns()
            : Promise.resolve([]),
          session?.permissions.includes("CAMPAIGN_READ")
            ? listTemplates()
            : Promise.resolve([]),
        ]);
        setProspects(prospectPage.content);
        setExclusions(exclusionPage.content);
        setAuditEvents(audits);
        setDuplicateReviews(reviews);
        setOpportunities(opportunityList);
        setPipelineMetrics(metrics);
        setCampaigns(campaignList);
        setTemplates(templateList);
      } catch (caught) {
        setError(message(caught));
      } finally {
        setLoading(false);
      }
    },
    [session, statusFilter, searchQuery],
  );

  useEffect(() => {
    void getSession()
      .then(setSession)
      .catch(() => setSession(null));
  }, []);

  useEffect(() => {
    if (session) {
      void refresh();
    }
  }, [session, refresh]);

  const dashboard = useMemo(() => {
    const interested = prospects.filter((prospect) =>
      ["INTERESTED", "QUALIFIED", "TRIAL_ACTIVE", "QUOTED", "NEGOTIATION"].includes(
        prospect.status,
      ),
    ).length;
    const blocked = prospects.filter((prospect) => !prospect.contactEligible).length;
    return { interested, blocked };
  }, [prospects]);

  if (session === undefined) {
    return <main className="login-page" aria-label="Restaurando sesión" />;
  }

  if (!session) {
    return <Login onAuthenticated={setSession} />;
  }

  async function selectProspect(id: string) {
    setError(null);
    try {
      setSelectedProspect(await getProspect(id));
    } catch (caught) {
      setError(message(caught));
    }
  }

  async function applyStatusFilter(value: ProspectStatus | "") {
    setStatusFilter(value);
    updateProspectUrl(searchQuery, value);
    await refresh(value, searchQuery);
  }

  async function applySearch(event: FormEvent) {
    event.preventDefault();
    const query = searchInput.trim();
    setSearchQuery(query);
    updateProspectUrl(query, statusFilter);
    await refresh(statusFilter, query);
  }

  async function refreshView() {
    await refresh();
    if (selectedProspect) {
      setSelectedProspect(await getProspect(selectedProspect.id));
    }
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">G</span>
          <div>
            <strong>Gestudio CRM</strong>
            <small>Operación comercial segura</small>
          </div>
        </div>
        <nav aria-label="Navegación principal">
          <NavButton active={tab === "dashboard"} onClick={() => setTab("dashboard")}>
            Dashboard
          </NavButton>
          <NavButton active={tab === "prospects"} onClick={() => setTab("prospects")}>
            Prospectos
          </NavButton>
          <NavButton active={tab === "pipeline"} onClick={() => setTab("pipeline")}>
            Pipeline
          </NavButton>
          {session.permissions.includes("CAMPAIGN_READ") && (
            <NavButton active={tab === "campaigns"} onClick={() => setTab("campaigns")}>
              Campañas
            </NavButton>
          )}
          {session.permissions.includes("REPORT_READ") && (
            <NavButton active={tab === "reports"} onClick={() => setTab("reports")}>
              Reportes
            </NavButton>
          )}
          {session.permissions.includes("REPORT_READ") && (
            <NavButton active={tab === "outbox"} onClick={() => setTab("outbox")}>
              Outbox y workers
            </NavButton>
          )}
          {session.permissions.includes("REPORT_READ") && (
            <NavButton active={tab === "inbound"} onClick={() => setTab("inbound")}>
              Inbound y quarantine
            </NavButton>
          )}
          {session.permissions.includes("CAMPAIGN_READ") && (
            <NavButton active={tab === "messages"} onClick={() => setTab("messages")}>
              Mensajes e integraciones
            </NavButton>
          )}
          {session.permissions.includes("IMPORT_PREVIEW") && (
            <NavButton active={tab === "imports"} onClick={() => setTab("imports")}>
              Importaciones
            </NavButton>
          )}
          <NavButton active={tab === "exclusions"} onClick={() => setTab("exclusions")}>
            Exclusiones
          </NavButton>
          {session.permissions.includes("AUDIT_READ") && (
            <NavButton active={tab === "audit"} onClick={() => setTab("audit")}>
              Auditoría
            </NavButton>
          )}
          {session.permissions.includes("USER_MANAGE") && (
            <NavButton active={tab === "users"} onClick={() => setTab("users")}>
              Usuarios
            </NavButton>
          )}
          {session.permissions.includes("REPORT_READ") && (
            <NavButton active={tab === "settings"} onClick={() => setTab("settings")}>
              Configuración
            </NavButton>
          )}
          <NavButton active={tab === "account"} onClick={() => setTab("account")}>
            Mi cuenta
          </NavButton>
        </nav>
        <div className="safety-panel">
          <strong>Envíos bloqueados</strong>
          <span>enabled=false</span>
          <span>dry-run=true</span>
          <span>daily-limit=0</span>
          <span>kill switch activo</span>
        </div>
        <button
          className="secondary-button"
          onClick={() => void logout().finally(() => setSession(null))}
        >
          Cerrar sesión
        </button>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <div>
            <h1>{title(tab)}</h1>
            <p>Fuente de verdad: PostgreSQL. Ningún envío real está disponible.</p>
          </div>
          <button
            className="secondary-button"
            onClick={() => void refreshView()}
          >
            Actualizar
          </button>
        </header>

        {error && <div className="alert error">{error}</div>}
        {loading && <div className="loading-bar" aria-label="Cargando" />}

        {tab === "dashboard" && (
          <section className="stack">
            <div className="metric-grid">
              <Metric label="Prospectos visibles" value={prospects.length} />
              <Metric label="Interés o pipeline" value={dashboard.interested} />
              <Metric label="Contacto bloqueado" value={dashboard.blocked} />
              <Metric label="Exclusiones" value={exclusions.length} />
              <Metric label="Revisiones pendientes" value={duplicateReviews.length} />
              <Metric label="Oportunidades activas" value={pipelineMetrics?.activeCount ?? 0} />
              <Metric label="Estancadas" value={pipelineMetrics?.stalledCount ?? 0} />
              <Metric
                label="Campañas en borrador"
                value={campaigns.filter((campaign) => campaign.status === "DRAFT").length}
              />
              <Metric
                label="Mensajes bloqueados"
                value={campaigns.reduce((total, campaign) => total + campaign.recipientCount, 0)}
              />
            </div>
            <Panel title="Controles activos">
              <div className="control-grid">
                <Control label="Aprobación de campañas" value="Obligatoria" />
                <Control label="Importación" value="Preview + confirmación" />
                <Control label="Duplicados ambiguos" value="Revisión humana" />
                <Control label="Sesión" value="Cookie HttpOnly + CSRF" />
              </div>
            </Panel>
            {session.permissions.includes("AUDIT_READ") && (
              <Panel title="Actividad reciente">
                <AuditTable events={auditEvents.slice(0, 8)} />
              </Panel>
            )}
          </section>
        )}

        {tab === "prospects" && (
          <section className="two-column">
            <Panel title="Prospectos">
              {session.permissions.includes("PROSPECT_WRITE") && (
                <CreateProspectForm
                  onCreated={async (created) => {
                    await refresh();
                    setSelectedProspect(created);
                  }}
                />
              )}
              <div className="toolbar">
                <form onSubmit={(event) => void applySearch(event)} className="inline-form">
                  <label>
                    Buscar
                    <input
                      aria-label="Buscar prospectos"
                      value={searchInput}
                      onChange={(event) => setSearchInput(event.target.value)}
                      placeholder="Institución, contacto, localidad o etiqueta"
                    />
                  </label>
                  <button className="secondary-button" type="submit">Buscar</button>
                </form>
                <label>
                  Estado
                  <select
                    value={statusFilter}
                    onChange={(event) =>
                      void applyStatusFilter(event.target.value as ProspectStatus | "")
                    }
                  >
                    <option value="">Todos</option>
                    {prospectStatuses.map((status) => (
                      <option key={status} value={status}>
                        {status}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>Institución</th>
                      <th>Localidad</th>
                      <th>Estado</th>
                      <th>Elegible</th>
                    </tr>
                  </thead>
                  <tbody>
                    {prospects.map((prospect) => (
                      <tr
                        key={prospect.id}
                        className={selectedProspect?.id === prospect.id ? "selected" : undefined}
                        onClick={() => void selectProspect(prospect.id)}
                      >
                        <td>{prospect.displayName}</td>
                        <td>{prospect.city ?? "—"}</td>
                        <td>
                          <Badge value={prospect.status} />
                        </td>
                        <td>{prospect.contactEligible ? "Sí" : "No"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Panel>
            <Panel title="Ficha integral">
              {selectedProspect ? (
                <ProspectDetail
                  prospect={selectedProspect}
                  session={session}
                  onChanged={async () => {
                    setSelectedProspect(await getProspect(selectedProspect.id));
                    await refresh();
                  }}
                />
              ) : (
                <EmptyState text="Seleccioná un prospecto para ver su ficha." />
              )}
            </Panel>
          </section>
        )}

        {tab === "pipeline" && (
          <PipelinePanel
            prospects={prospects}
            opportunities={opportunities}
            metrics={pipelineMetrics}
            session={session}
            onChanged={() => refresh()}
          />
        )}

        {tab === "campaigns" && (
          <CampaignsPanel
            campaigns={campaigns}
            templates={templates}
            session={session}
            onChanged={() => refresh()}
          />
        )}

        {tab === "messages" && (
          <MessagesPanel prospects={prospects} session={session} />
        )}

        {tab === "outbox" && <OutboxPanel session={session} />}

        {tab === "inbound" && (
          <InboundPanel session={session} prospects={prospects} />
        )}

        {tab === "imports" && (
          <ImportsPanel
            duplicateReviews={duplicateReviews}
            onChanged={() => refresh()}
          />
        )}

        {tab === "exclusions" && (
          <ExclusionsPanel
            exclusions={exclusions}
            canWrite={session.permissions.includes("PROSPECT_WRITE")}
            onChanged={() => refresh()}
          />
        )}

        {tab === "audit" && (
          <Panel title="Eventos recientes">
            <AuditTable events={auditEvents} />
          </Panel>
        )}

        {tab === "reports" && <ReportsPanel />}

        {tab === "settings" && (
          <SettingsPanel session={session} selectedProspect={selectedProspect} />
        )}

        {tab === "users" && <UsersPanel currentUser={session} />}
        {tab === "account" && (
          <AccountPanel session={session} onPasswordChanged={() => setSession(null)} />
        )}
      </main>
    </div>
  );
}

function ReportsPanel() {
  const [report, setReport] = useState<DashboardReport | null>(null);
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refreshReport = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setReport(await getDashboardReport(from || undefined, to || undefined));
    } catch (caught) {
      setError(message(caught));
    } finally {
      setLoading(false);
    }
  }, [from, to]);

  useEffect(() => {
    void refreshReport();
  }, []);

  return (
    <section className="stack">
      {error && <div className="alert error">{error}</div>}
      {loading && <div className="loading-bar" aria-label="Cargando reportes" />}
      <Panel title="Período y exportación">
        <form
          className="toolbar"
          onSubmit={(event) => {
            event.preventDefault();
            void refreshReport();
          }}
        >
          <label>Desde<input type="date" value={from} onChange={(event) => setFrom(event.target.value)} /></label>
          <label>Hasta<input type="date" value={to} onChange={(event) => setTo(event.target.value)} /></label>
          <button className="primary-button" type="submit">Aplicar</button>
          <a className="secondary-button link-button" href={dashboardCsvUrl(from || undefined, to || undefined)}>
            Exportar CSV seguro
          </a>
        </form>
        {report && <p className="muted">{report.from} a {report.to} · {report.timezone}</p>}
      </Panel>
      {report ? (
        <>
          <div className="metric-grid">
            <Metric label="Contactables" value={report.prospectSummary.contactable ?? 0} />
            <Metric label="Excluidos" value={report.prospectSummary.excluded ?? 0} />
            <Metric label="Sin propietario" value={report.prospectSummary.unowned ?? 0} />
            <Metric label="Tareas abiertas" value={report.tasks.open ?? 0} />
            <Metric label="Tareas vencidas" value={report.tasks.overdue ?? 0} />
            <Metric label="Respuestas" value={report.prospectSummary.replied ?? 0} />
            <Metric label="Dead-letter" value={report.operations.deadLetter ?? 0} />
            <Metric label="Quarantine" value={report.operations.quarantine ?? 0} />
          </div>
          <section className="two-column equal">
            <ReportMap title="Prospectos por estado" values={report.prospectsByStatus} />
            <ReportMap title="Prospectos por fuente" values={report.prospectsBySource} />
            <ReportMap title="Oportunidades por etapa" values={report.opportunitiesByStage} />
            <ReportMap title="Outbox por estado" values={report.outbox} />
          </section>
          <Panel title="Valor por moneda">
            {report.opportunityValues.length ? (
              <div className="table-scroll"><table><thead><tr><th>Moneda</th><th>Oportunidades</th><th>Total</th><th>Ponderado</th></tr></thead><tbody>
                {report.opportunityValues.map((value) => <tr key={value.currency}><td>{value.currency}</td><td>{value.opportunityCount}</td><td>{value.totalValue}</td><td>{value.weightedValue}</td></tr>)}
              </tbody></table></div>
            ) : <EmptyState text="No hay oportunidades activas en el período." />}
          </Panel>
        </>
      ) : !loading && <EmptyState text="No se pudieron cargar los reportes." />}
    </section>
  );
}

function ReportMap({ title: heading, values }: { title: string; values: Record<string, number> }) {
  return (
    <Panel title={heading}>
      {Object.keys(values).length ? <dl className="detail-grid">{Object.entries(values).map(([key, value]) => <Detail key={key} label={key} value={String(value)} />)}</dl> : <EmptyState text="Sin datos." />}
    </Panel>
  );
}

function SettingsPanel({ session, selectedProspect }: { session: SessionUser; selectedProspect: Prospect | null }) {
  const [settings, setSettings] = useState<OrganizationSettings | null>(null);
  const [tags, setTags] = useState<CrmTag[]>([]);
  const [prospectTags, setProspectTags] = useState<CrmTag[]>([]);
  const [newTagName, setNewTagName] = useState("");
  const [newTagColor, setNewTagColor] = useState("#2563EB");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const canManage = session.permissions.includes("SETTINGS_MANAGE");
  const canWriteProspects = session.permissions.includes("PROSPECT_WRITE");

  const refreshSettings = useCallback(async () => {
    setError(null);
    try {
      const [organization, tagList, assigned] = await Promise.all([
        getOrganizationSettings(),
        listTags(canManage),
        selectedProspect ? listProspectTags(selectedProspect.id) : Promise.resolve([]),
      ]);
      setSettings(organization);
      setTags(tagList);
      setProspectTags(assigned);
    } catch (caught) {
      setError(message(caught));
    }
  }, [canManage, selectedProspect?.id]);

  useEffect(() => { void refreshSettings(); }, [refreshSettings]);

  async function run(action: () => Promise<unknown>, success: string) {
    setError(null); setNotice(null);
    try { await action(); setNotice(success); await refreshSettings(); }
    catch (caught) { setError(message(caught)); }
  }

  return (
    <section className="stack">
      <div className="alert safety">Los bloqueos de entorno dominan esta pantalla. Ningún usuario puede habilitar envíos reales desde la API o la UI.</div>
      {error && <div className="alert error">{error}</div>}
      {notice && <div className="alert success">{notice}</div>}
      {settings ? <>
        <div className="metric-grid">
          <Metric label="Sending enabled" value={settings.sending.environmentEnabled ? "true" : "false"} />
          <Metric label="Dry-run" value={settings.sending.environmentDryRun ? "true" : "false"} />
          <Metric label="Límite diario" value={settings.sending.environmentDailyLimit} />
          <Metric label="Kill switch" value={settings.sending.environmentKillSwitch ? "activo" : "inactivo"} />
        </div>
        <Panel title="Organización">
          <form className="form-grid" onSubmit={(event) => { event.preventDefault(); if (settings) void run(() => updateOrganizationSettings(settings), "Configuración actualizada; bloqueos de envío preservados."); }}>
            <label>Nombre<input disabled={!canManage} value={settings.name} onChange={(event) => setSettings({ ...settings, name: event.target.value })} /></label>
            <label>Timezone<input disabled={!canManage} value={settings.timezone} onChange={(event) => setSettings({ ...settings, timezone: event.target.value })} /></label>
            <label>Moneda<input disabled={!canManage} maxLength={3} value={settings.currency} onChange={(event) => setSettings({ ...settings, currency: event.target.value.toUpperCase() })} /></label>
            <label>Idioma<select disabled={!canManage} value={settings.locale} onChange={(event) => setSettings({ ...settings, locale: event.target.value })}><option value="es-AR">Español Argentina</option><option value="es">Español</option><option value="en-US">English US</option><option value="en">English</option></select></label>
            <label>Color principal<input disabled={!canManage} type="color" value={settings.brandingPrimaryColor} onChange={(event) => setSettings({ ...settings, brandingPrimaryColor: event.target.value })} /></label>
            <label>Seguimiento (días)<input disabled={!canManage} type="number" min="1" max="365" value={settings.followUpDays} onChange={(event) => setSettings({ ...settings, followUpDays: Number(event.target.value) })} /></label>
            <label>Inicio operativo<input disabled={!canManage} type="time" value={settings.operatingWindowStart} onChange={(event) => setSettings({ ...settings, operatingWindowStart: event.target.value })} /></label>
            <label>Fin operativo<input disabled={!canManage} type="time" value={settings.operatingWindowEnd} onChange={(event) => setSettings({ ...settings, operatingWindowEnd: event.target.value })} /></label>
            {canManage && <button className="primary-button" type="submit">Guardar configuración</button>}
          </form>
        </Panel>
      </> : <EmptyState text="Cargando configuración." />}
      <section className="two-column equal">
        <Panel title="Etiquetas">
          {canManage && <form className="inline-form" onSubmit={(event) => { event.preventDefault(); void run(() => createTag(newTagName, newTagColor), "Etiqueta creada.").then(() => setNewTagName("")); }}>
            <label>Nombre<input required maxLength={80} value={newTagName} onChange={(event) => setNewTagName(event.target.value)} /></label>
            <label>Color<input type="color" value={newTagColor} onChange={(event) => setNewTagColor(event.target.value)} /></label>
            <button className="secondary-button" type="submit">Crear</button>
          </form>}
          {tags.length ? <ul className="plain-list">{tags.map((tag) => <li key={tag.id}><span className="tag-swatch" style={{ background: tag.color }} aria-hidden="true" /><strong>{tag.name}</strong> · {tag.usageCount} usos {!tag.active && "· inactiva"}{canManage && tag.active && <button className="link-button" onClick={() => window.confirm(`Desactivar ${tag.name}?`) && void run(() => deactivateTag(tag), "Etiqueta desactivada sin borrar historial.")}>Desactivar</button>}</li>)}</ul> : <EmptyState text="No hay etiquetas." />}
        </Panel>
        <Panel title="Asignación al prospecto seleccionado">
          {selectedProspect ? <>
            <p><strong>{selectedProspect.displayName}</strong></p>
            <div className="button-row">{tags.filter((tag) => tag.active).map((tag) => {
              const assigned = prospectTags.some((item) => item.id === tag.id);
              return <button key={tag.id} disabled={!canWriteProspects} className={assigned ? "secondary-button" : "link-button"} onClick={() => void run(() => assigned ? unassignTag(tag.id, selectedProspect.id) : assignTag(tag.id, selectedProspect.id), assigned ? "Etiqueta removida." : "Etiqueta asignada.")}>{assigned ? `Quitar ${tag.name}` : `Asignar ${tag.name}`}</button>;
            })}</div>
          </> : <EmptyState text="Seleccioná un prospecto en la ficha y volvé a Configuración para asignar etiquetas." />}
        </Panel>
      </section>
      <Panel title="Integraciones"><div className="control-grid"><Control label="Gmail" value="Adaptador implementado, no conectado" /><Control label="WhatsApp Cloud" value="Adaptador implementado, no conectado" /><Control label="Webhook fake" value="Solo entorno sintético firmado" /></div></Panel>
    </section>
  );
}

function OutboxPanel({ session }: { session: SessionUser }) {
  const [events, setEvents] = useState<OutboxEvent[]>([]);
  const [metrics, setMetrics] = useState<OutboxMetric[]>([]);
  const [worker, setWorker] = useState<WorkerState | null>(null);
  const [selected, setSelected] = useState<OutboxEvent | null>(null);
  const [status, setStatus] = useState<OutboxStatus | "">("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const canOperate = session.permissions.includes("SETTINGS_MANAGE");

  const refreshOutbox = useCallback(async (filter: OutboxStatus | "" = status) => {
    setLoading(true);
    setError(null);
    try {
      const [page, counts, state] = await Promise.all([
        listOutbox(filter || undefined),
        getOutboxMetrics(),
        getWorkerState(),
      ]);
      setEvents(page.content);
      setMetrics(counts);
      setWorker(state);
      if (selected) {
        setSelected(await getOutboxEvent(selected.id));
      }
    } catch (caught) {
      setError(message(caught));
    } finally {
      setLoading(false);
    }
  }, [selected, status]);

  useEffect(() => {
    void refreshOutbox();
  }, []);

  async function operation(action: () => Promise<unknown>, success: string) {
    setError(null);
    setNotice(null);
    try {
      await action();
      setNotice(success);
      await refreshOutbox();
    } catch (caught) {
      setError(message(caught));
    }
  }

  return (
    <section className="stack">
      <div className="alert safety">
        Los workers reevalúan los kill switches al procesar. No existe una acción para forzar
        providers reales ni transformar un evento en SENT.
      </div>
      {error && <div className="alert error">{error}</div>}
      {notice && <div className="alert success">{notice}</div>}
      {loading && <div className="loading-bar" aria-label="Cargando outbox" />}
      <div className="metric-grid">
        {metrics.map((metric) => (
          <Metric key={metric.status} label={metric.status} value={metric.count} />
        ))}
        {metrics.length === 0 && <Metric label="Eventos" value={0} />}
      </div>
      <Panel title="Estado del worker">
        {worker ? (
          <div className="control-grid">
            <Control label="Scheduler" value={worker.worker.schedulerEnabled ? "Habilitado" : "Manual"} />
            <Control label="Tenant" value={worker.tenantPaused ? "Pausado" : "Activo"} />
            <Control label="Procesando" value={worker.worker.running ? "Sí" : "No"} />
            <Control label="Batch" value={String(worker.worker.batchSize)} />
          </div>
        ) : (
          <EmptyState text="No se pudo cargar la salud del worker." />
        )}
        {canOperate && worker && (
          <div className="button-row">
            <button
              className="primary-button"
              onClick={() => void operation(async () => {
                const result = await runOutboxWorker();
                setNotice(`Worker: ${result.claimed} reclamados, ${result.completed} completados.`);
              }, "Ejecución manual finalizada.")}
            >
              Ejecutar una vez
            </button>
            <button
              className="secondary-button"
              onClick={() => void operation(
                () => setOutboxWorkerPaused(!worker.tenantPaused),
                worker.tenantPaused ? "Worker reanudado." : "Worker pausado.",
              )}
            >
              {worker.tenantPaused ? "Reanudar" : "Pausar"}
            </button>
          </div>
        )}
      </Panel>
      <section className="two-column equal">
        <Panel title="Eventos">
          <div className="toolbar">
            <label>
              Estado
              <select
                value={status}
                onChange={(event) => {
                  const next = event.target.value as OutboxStatus | "";
                  setStatus(next);
                  void refreshOutbox(next);
                }}
              >
                <option value="">Todos</option>
                {["PENDING", "PROCESSING", "SUCCEEDED", "RETRY", "DEAD", "CANCELLED", "BLOCKED"].map((value) => (
                  <option key={value}>{value}</option>
                ))}
              </select>
            </label>
            <button className="secondary-button" onClick={() => void refreshOutbox()}>
              Reintentar carga
            </button>
          </div>
          {events.length === 0 && !loading ? (
            <EmptyState text="No hay eventos para este filtro." />
          ) : (
            <div className="table-scroll">
              <table>
                <thead><tr><th>Evento</th><th>Estado</th><th>Intentos</th><th>Creado</th></tr></thead>
                <tbody>
                  {events.map((event) => (
                    <tr
                      key={event.id}
                      tabIndex={0}
                      onClick={() => setSelected(event)}
                      onKeyDown={(keyEvent) => {
                        if (keyEvent.key === "Enter" || keyEvent.key === " ") {
                          keyEvent.preventDefault();
                          setSelected(event);
                        }
                      }}
                      className={selected?.id === event.id ? "selected" : undefined}
                    >
                      <td>{event.eventType}</td>
                      <td><Badge value={event.status} /></td>
                      <td>{event.attemptCount}/{event.maxAttempts}</td>
                      <td>{dateTime(event.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Panel>
        <Panel title="Detalle sanitizado">
          {selected ? (
            <div className="stack compact">
              <dl className="detail-grid">
                <Detail label="ID" value={selected.id} />
                <Detail label="Correlation ID" value={selected.correlationId} />
                <Detail label="Aggregate" value={`${selected.aggregateType} / ${selected.aggregateId}`} />
                <Detail label="Resultado" value={selected.resultSummary ?? selected.lastErrorCode} />
                <Detail label="Error" value={selected.lastErrorSummary} />
                <Detail label="Procesado" value={selected.processedAt ? dateTime(selected.processedAt) : null} />
              </dl>
              <pre className="preview-box" aria-label="Payload sanitizado">{selected.payload}</pre>
              {canOperate && (
                <div className="button-row">
                  {selected.status === "DEAD" && (
                    <button className="primary-button" onClick={() => {
                      if (window.confirm("¿Reencolar este evento DEAD sin modificar su payload?")) {
                        void operation(() => requeueOutboxEvent(selected.id), "Evento reencolado.");
                      }
                    }}>Reencolar</button>
                  )}
                  {["PENDING", "RETRY"].includes(selected.status) && (
                    <button className="secondary-button" onClick={() => {
                      if (window.confirm("¿Cancelar este evento pendiente?")) {
                        void operation(() => cancelOutboxEvent(selected.id), "Evento cancelado.");
                      }
                    }}>Cancelar</button>
                  )}
                </div>
              )}
            </div>
          ) : (
            <EmptyState text="Seleccioná un evento." />
          )}
        </Panel>
      </section>
    </section>
  );
}

function InboundPanel({ session, prospects }: { session: SessionUser; prospects: Prospect[] }) {
  const [items, setItems] = useState<InboundMessage[]>([]);
  const [selected, setSelected] = useState<InboundMessage | null>(null);
  const [health, setHealth] = useState<WebhookHealth | null>(null);
  const [prospectId, setProspectId] = useState("");
  const [discardReason, setDiscardReason] = useState("No asociable tras revisión manual");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const canOperate = session.permissions.includes("SETTINGS_MANAGE");

  const refreshInbound = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [page, webhook] = await Promise.all([listInbound(), getWebhookHealth()]);
      setItems(page.content);
      setHealth(webhook);
      if (selected) {
        setSelected(await getInbound(selected.id));
      }
    } catch (caught) {
      setError(message(caught));
    } finally {
      setLoading(false);
    }
  }, [selected]);

  useEffect(() => {
    void refreshInbound();
  }, []);

  async function operation(action: () => Promise<unknown>, success: string) {
    setError(null);
    setNotice(null);
    try {
      await action();
      setNotice(success);
      await refreshInbound();
    } catch (caught) {
      setError(message(caught));
    }
  }

  return (
    <section className="stack">
      {error && <div className="alert error">{error}</div>}
      {notice && <div className="alert success">{notice}</div>}
      {loading && <div className="loading-bar" aria-label="Cargando inbound" />}
      <Panel title="Webhook fake">
        <div className="control-grid">
          <Control label="Provider" value={health?.provider ?? "FAKE_INBOUND"} />
          <Control label="Endpoint" value={health?.enabled ? "Habilitado" : "Deshabilitado"} />
          <Control label="Secreto" value={health?.configured ? "Configurado por entorno" : "No configurado"} />
          <Control label="Límite" value={health ? `${health.maxPayloadBytes} bytes` : "—"} />
          <Control label="Respuesta automática" value="Deshabilitada" />
        </div>
      </Panel>
      <section className="two-column equal">
        <Panel title="Inbound y quarantine">
          <div className="toolbar">
            <button className="secondary-button" onClick={() => void refreshInbound()}>
              Reintentar carga
            </button>
          </div>
          {items.length === 0 && !loading ? (
            <EmptyState text="No hay mensajes inbound." />
          ) : (
            <div className="table-scroll">
              <table>
                <thead><tr><th>Canal</th><th>Remitente</th><th>Estado</th><th>Recibido</th></tr></thead>
                <tbody>
                  {items.map((item) => (
                    <tr
                      key={item.id}
                      tabIndex={0}
                      onClick={() => setSelected(item)}
                      onKeyDown={(keyEvent) => {
                        if (keyEvent.key === "Enter" || keyEvent.key === " ") {
                          keyEvent.preventDefault();
                          setSelected(item);
                        }
                      }}
                      className={selected?.id === item.id ? "selected" : undefined}
                    >
                      <td>{item.channel}</td>
                      <td>{item.senderMasked}</td>
                      <td><Badge value={item.status} /></td>
                      <td>{dateTime(item.receivedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Panel>
        <Panel title="Evidencia y asociación">
          {selected ? (
            <div className="stack compact">
              <dl className="detail-grid">
                <Detail label="Receipt" value={selected.id} />
                <Detail label="Correlation ID" value={selected.correlationId} />
                <Detail label="Asociación" value={selected.associationStatus} />
                <Detail label="Prospecto" value={selected.prospectId} />
                <Detail label="Motivo quarantine" value={selected.quarantineReason} />
                <Detail label="Hash payload" value={selected.payloadHash} />
              </dl>
              {canOperate && selected.status === "QUARANTINED" && (
                <div className="form-grid">
                  <label className="full-width">
                    Prospecto sintético o verificado
                    <select value={prospectId} onChange={(event) => setProspectId(event.target.value)}>
                      <option value="">Seleccionar…</option>
                      {prospects.map((prospect) => <option key={prospect.id} value={prospect.id}>{prospect.displayName}</option>)}
                    </select>
                  </label>
                  <button
                    className="primary-button"
                    disabled={!prospectId}
                    onClick={() => void operation(() => associateInbound(selected.id, prospectId), "Asociación encolada para procesamiento.")}
                  >Asociar</button>
                  <button
                    className="secondary-button"
                    onClick={() => void operation(() => retryInboundAssociation(selected.id), "Reintento de asociación encolado.")}
                  >Reintentar asociación</button>
                  <label className="full-width">
                    Motivo de descarte
                    <input value={discardReason} maxLength={500} onChange={(event) => setDiscardReason(event.target.value)} />
                  </label>
                  <button
                    className="secondary-button full-width"
                    disabled={!discardReason.trim()}
                    onClick={() => {
                      if (window.confirm("¿Descartar lógicamente este receipt? La evidencia se conserva.")) {
                        void operation(() => discardInbound(selected.id, discardReason), "Receipt descartado lógicamente.");
                      }
                    }}
                  >Descartar con motivo</button>
                </div>
              )}
            </div>
          ) : (
            <EmptyState text="Seleccioná un receipt para ver metadata sanitizada." />
          )}
        </Panel>
      </section>
    </section>
  );
}

function UsersPanel({ currentUser }: { currentUser: SessionUser }) {
  const [users, setUsers] = useState<User[]>([]);
  const [username, setUsername] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<User["role"]>("SALES");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const refreshUsers = useCallback(async () => {
    try {
      setUsers(await listUsers());
    } catch (caught) {
      setError(message(caught));
    }
  }, []);

  useEffect(() => {
    void refreshUsers();
  }, [refreshUsers]);

  async function submitUser(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setNotice(null);
    try {
      await createUser({ username, displayName, password, role });
      setUsername("");
      setDisplayName("");
      setPassword("");
      setNotice("Usuario creado.");
      await refreshUsers();
    } catch (caught) {
      setError(message(caught));
    }
  }

  async function toggleUser(user: User) {
    if (!window.confirm(`${user.active ? "Desactivar" : "Activar"} a ${user.displayName}?`)) {
      return;
    }
    setError(null);
    try {
      await setUserActive(user.id, !user.active);
      await refreshUsers();
    } catch (caught) {
      setError(message(caught));
    }
  }

  return (
    <section className="stack">
      {error && <div className="alert error">{error}</div>}
      {notice && <div className="alert success">{notice}</div>}
      <Panel title="Usuarios de la organización">
        <form className="inline-form" onSubmit={(event) => void submitUser(event)}>
          <label>
            Usuario
            <input value={username} onChange={(event) => setUsername(event.target.value)} required />
          </label>
          <label className="grow">
            Nombre visible
            <input
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
              required
            />
          </label>
          <label>
            Contraseña inicial
            <input
              type="password"
              minLength={12}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
          </label>
          <label>
            Rol
            <select value={role} onChange={(event) => setRole(event.target.value as User["role"])}>
              <option value="ADMIN">Admin</option>
              <option value="MANAGER">Manager</option>
              <option value="SALES">Ventas</option>
              <option value="VIEWER">Solo lectura</option>
            </select>
          </label>
          <button className="primary-button">Crear usuario</button>
        </form>
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Usuario</th>
                <th>Rol</th>
                <th>Estado</th>
                <th>Último acceso</th>
                <th>Acción</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td>{user.displayName}</td>
                  <td>{user.username}</td>
                  <td>{user.role}</td>
                  <td>{user.active ? "Activo" : "Inactivo"}</td>
                  <td>{user.lastLoginAt ? dateTime(user.lastLoginAt) : "Nunca"}</td>
                  <td>
                    <button
                      className="secondary-button"
                      disabled={user.id === currentUser.userId}
                      onClick={() => void toggleUser(user)}
                    >
                      {user.active ? "Desactivar" : "Activar"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Panel>
    </section>
  );
}

function AccountPanel({
  session,
  onPasswordChanged,
}: {
  session: SessionUser;
  onPasswordChanged: () => void;
}) {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  async function submitPassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    try {
      await changePassword(currentPassword, newPassword);
      onPasswordChanged();
    } catch (caught) {
      setError(message(caught));
    }
  }

  return (
    <section className="stack">
      <Panel title="Sesión actual">
        <div className="control-grid">
          <Control label="Usuario" value={session.username} />
          <Control label="Nombre" value={session.displayName} />
          <Control label="Rol" value={session.role} />
          <Control label="Organización" value={session.organizationId} />
        </div>
      </Panel>
      <Panel title="Cambiar mi contraseña">
        {error && <div className="alert error">{error}</div>}
        <form className="inline-form" onSubmit={(event) => void submitPassword(event)}>
          <label>
            Contraseña actual
            <input
              type="password"
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
              required
            />
          </label>
          <label>
            Nueva contraseña
            <input
              type="password"
              minLength={12}
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              required
            />
          </label>
          <button className="primary-button">Actualizar contraseña</button>
        </form>
      </Panel>
    </section>
  );
}

function Login({ onAuthenticated }: { onAuthenticated: (session: SessionUser) => void }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      onAuthenticated(await login(username.trim(), password));
    } catch (caught) {
      setError(message(caught));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="login-page">
      <form className="login-card" onSubmit={(event) => void submit(event)}>
        <div className="brand login-brand">
          <span className="brand-mark">G</span>
          <div>
            <strong>Gestudio CRM</strong>
            <small>Sesión segura de la organización</small>
          </div>
        </div>
        <h1>Ingresar</h1>
        <p>La contraseña se usa solo para autenticar y no se conserva en el navegador.</p>
        {error && <div className="alert error">{error}</div>}
        <label>
          Usuario
          <input autoComplete="username" value={username} onChange={(event) => setUsername(event.target.value)} required />
        </label>
        <label>
          Contraseña
          <input
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </label>
        <button className="primary-button" disabled={submitting}>
          {submitting ? "Validando…" : "Ingresar"}
        </button>
      </form>
    </main>
  );
}

function MessagesPanel({
  prospects,
  session,
}: {
  prospects: Prospect[];
  session: SessionUser;
}) {
  const [safety, setSafety] = useState<MessagingSafety | null>(null);
  const [prospectId, setProspectId] = useState(prospects[0]?.id ?? "");
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [contactId, setContactId] = useState("");
  const [channel, setChannel] = useState<CampaignChannel>("EMAIL");
  const [subject, setSubject] = useState("Borrador comercial");
  const [body, setBody] = useState("");
  const [result, setResult] = useState<MessageRecord | null>(null);
  const [manualLink, setManualLink] = useState<ManualMessageLink | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const canDraft = session.permissions.includes("MESSAGE_DRAFT");
  const canSimulate = session.permissions.includes("MESSAGE_SIMULATE");
  const compatibleContacts = useMemo(
    () =>
      contacts.filter((contact) =>
        contact.channels.some((item) => item.type === channel && item.valid),
      ),
    [channel, contacts],
  );

  useEffect(() => {
    void getMessagingSafety().then(setSafety).catch((caught) => setError(message(caught)));
  }, []);

  useEffect(() => {
    if (!prospectId && prospects[0]) {
      setProspectId(prospects[0].id);
      return;
    }
    if (!prospectId) {
      setContacts([]);
      setContactId("");
      return;
    }
    void listContacts(prospectId)
      .then((items) => {
        setContacts(items);
        setContactId((current) =>
          items.some((item) => item.id === current) ? current : (items[0]?.id ?? ""),
        );
      })
      .catch((caught) => setError(message(caught)));
  }, [prospectId, prospects]);

  useEffect(() => {
    if (!compatibleContacts.some((contact) => contact.id === contactId)) {
      setContactId(compatibleContacts[0]?.id ?? "");
    }
  }, [compatibleContacts, contactId]);

  function input() {
    return {
      prospectId,
      contactId,
      channel,
      subject: channel === "EMAIL" ? subject : undefined,
      textBody: body,
      htmlBody: channel === "EMAIL" ? `<p>${escapeText(body)}</p>` : undefined,
    };
  }

  async function run(operation: "draft" | "simulate" | "manual") {
    setBusy(true);
    setError(null);
    setResult(null);
    setManualLink(null);
    try {
      if (operation === "draft") {
        setResult(await createMessageDraft(input()));
      } else if (operation === "simulate") {
        setResult(await simulateMessage(input()));
      } else {
        setManualLink(await createManualMessageLink(input()));
      }
    } catch (caught) {
      setError(message(caught));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="stack">
      <div className="alert safety" role="status">
        No existe endpoint de envío. Los proveedores reales no pueden inicializarse mientras la red
        real esté bloqueada, y PostgreSQL conserva un segundo kill switch.
      </div>
      {error && <div className="alert error">{error}</div>}
      <div className="control-grid">
        <Control label="Email" value={safety?.selectedEmailProvider ?? "Consultando…"} />
        <Control label="WhatsApp" value={safety?.selectedWhatsAppProvider ?? "Consultando…"} />
        <Control
          label="Red real"
          value={safety?.realNetworkAllowed ? "HABILITADA" : "BLOQUEADA"}
        />
        <Control
          label="Endpoint de envío"
          value={safety?.sendEndpointAvailable ? "DISPONIBLE" : "INEXISTENTE"}
        />
      </div>
      {(canDraft || canSimulate) && (
        <Panel title="Borrador seguro o simulación">
          <form className="form-grid" onSubmit={(event) => event.preventDefault()}>
            <label>
              Prospecto
              <select
                value={prospectId}
                onChange={(event) => setProspectId(event.target.value)}
                required
              >
                <option value="">Seleccionar…</option>
                {prospects.map((prospect) => (
                  <option key={prospect.id} value={prospect.id}>
                    {prospect.displayName}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Canal
              <select
                value={channel}
                onChange={(event) => setChannel(event.target.value as CampaignChannel)}
              >
                <option value="EMAIL">EMAIL</option>
                <option value="WHATSAPP">WHATSAPP</option>
              </select>
            </label>
            <label className="full-width">
              Contacto con canal válido
              <select
                value={contactId}
                onChange={(event) => setContactId(event.target.value)}
                required
              >
                <option value="">Seleccionar…</option>
                {compatibleContacts.map((contact) => (
                  <option key={contact.id} value={contact.id}>
                    {contact.displayName}
                  </option>
                ))}
              </select>
            </label>
            {channel === "EMAIL" && (
              <label className="full-width">
                Asunto
                <input
                  value={subject}
                  onChange={(event) => setSubject(event.target.value)}
                  required
                />
              </label>
            )}
            <label className="full-width">
              Mensaje
              <textarea value={body} onChange={(event) => setBody(event.target.value)} required />
            </label>
            <div className="action-row full-width">
              {canDraft && (
                <button
                  className="secondary-button"
                  disabled={busy || !contactId}
                  onClick={() => void run("draft")}
                >
                  Crear borrador local
                </button>
              )}
              {canSimulate && (
                <button
                  className="primary-button"
                  disabled={busy || !contactId}
                  onClick={() => void run("simulate")}
                >
                  Simular con fake
                </button>
              )}
              {canDraft && (
                <button
                  className="secondary-button"
                  disabled={busy || !contactId}
                  onClick={() => void run("manual")}
                >
                  Generar enlace manual
                </button>
              )}
            </div>
          </form>
        </Panel>
      )}
      {result && (
        <div className="alert success" aria-live="polite">
          {result.status} mediante {result.provider}. Bloqueo de envío: {result.sendingBlockReason}.
        </div>
      )}
      {manualLink && (
        <div className="alert safety" aria-live="polite">
          Enlace manual generado; el CRM no lo abrió ni registró un envío. {" "}
          <a href={manualLink.url} target="_blank" rel="noreferrer">
            Abrir aplicación local
          </a>
        </div>
      )}
      <Panel title="Integraciones externas">
        <div className="control-grid">
          <Control label="Gmail OAuth" value="IMPLEMENTED_NOT_CONNECTED" />
          <Control label="WhatsApp Cloud" value="IMPLEMENTED_NOT_CONNECTED" />
          <Control label="Modo email" value={safety?.emailMode ?? "NOOP"} />
          <Control label="Modo WhatsApp" value={safety?.whatsAppMode ?? "DEEPLINK_ONLY"} />
        </div>
      </Panel>
    </section>
  );
}

function CampaignsPanel({
  campaigns,
  templates,
  session,
  onChanged,
}: {
  campaigns: Campaign[];
  templates: MessageTemplate[];
  session: SessionUser;
  onChanged: () => Promise<void>;
}) {
  const [templateName, setTemplateName] = useState("");
  const [templateChannel, setTemplateChannel] = useState<CampaignChannel>("EMAIL");
  const [subject, setSubject] = useState("Hola {{prospect.displayName}}");
  const [textBody, setTextBody] = useState(
    "Hola {{contact.firstName}}, te contactamos por {{campaign.name}}.",
  );
  const [htmlBody, setHtmlBody] = useState(
    "<p>Hola <strong>{{contact.firstName}}</strong>, te contactamos por {{campaign.name}}.</p>",
  );
  const [campaignName, setCampaignName] = useState("");
  const [templateVersionId, setTemplateVersionId] = useState("");
  const [province, setProvince] = useState("");
  const [scoreAtLeast, setScoreAtLeast] = useState("");
  const [audience, setAudience] = useState<AudienceRecipient[]>([]);
  const [preview, setPreview] = useState<RenderedTemplate | null>(null);
  const [simulation, setSimulation] = useState<CampaignSimulation | null>(null);
  const [sequence, setSequence] = useState<CampaignSequenceStep[]>([]);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const writable = session.permissions.includes("CAMPAIGN_WRITE");

  async function run(action: () => Promise<void>) {
    setError(null);
    setNotice(null);
    try {
      await action();
    } catch (caught) {
      setError(message(caught));
    }
  }

  async function submitTemplate(event: FormEvent) {
    event.preventDefault();
    await run(async () => {
      const created = await createTemplate({
        name: templateName,
        channel: templateChannel,
        subject,
        textBody,
        htmlBody,
      });
      setTemplateName("");
      setTemplateVersionId(created.versionId);
      setNotice(`Plantilla ${created.name} v${created.versionNumber} creada.`);
      await onChanged();
    });
  }

  async function submitCampaign(event: FormEvent) {
    event.preventDefault();
    const selected = templates.find((template) => template.versionId === templateVersionId);
    if (!selected) {
      setError("Seleccioná una plantilla.");
      return;
    }
    await run(async () => {
      const created = await createCampaign({
        name: campaignName,
        objective: "Prospección comercial simulada",
        channel: selected.channel,
        templateVersionId: selected.versionId,
      });
      setCampaignName("");
      setNotice(`Campaña ${created.name} creada en borrador.`);
      await onChanged();
    });
  }

  async function showPreview(template: MessageTemplate) {
    await run(async () => {
      setPreview(
        await previewTemplate(template.versionId, {
          "prospect.displayName": "Institución de ejemplo",
          "prospect.city": "Rosario",
          "contact.firstName": "Ana",
          "contact.lastName": "Pérez",
          "owner.name": session.displayName,
          "campaign.name": campaignName || "Campaña de ejemplo",
        }),
      );
      setNotice("Preview renderizado con datos sintéticos.");
    });
  }

  async function freeze(campaign: Campaign) {
    if (!window.confirm("¿Congelar esta audiencia? Los filtros quedarán materializados.")) {
      return;
    }
    await run(async () => {
      const frozen = await freezeCampaignAudience(campaign, {
        province: province || undefined,
        scoreAtLeast: scoreAtLeast ? Number(scoreAtLeast) : undefined,
      });
      setAudience(await getCampaignAudience(frozen.id));
      setNotice(
        `Audiencia congelada: ${frozen.recipientCount} incluidos, ${frozen.excludedCount} excluidos.`,
      );
      await onChanged();
    });
  }

  async function approve(campaign: Campaign) {
    if (!window.confirm("¿Aprobar esta campaña para simulación? Esto no habilita envíos reales.")) {
      return;
    }
    await run(async () => {
      const approved = await approveCampaign(campaign);
      setNotice(`Campaña ${approved.name} aprobada solo para simulación.`);
      await onChanged();
    });
  }

  async function simulate(campaign: Campaign) {
    await run(async () => {
      const result = await simulateCampaign(campaign);
      setSimulation(result);
      setNotice(
        `Simulación completa: ${result.includedCount} borradores fake y ${result.excludedCount} bloqueados.`,
      );
      await onChanged();
    });
  }

  async function configureSequence(campaign: Campaign) {
    await run(async () => {
      setSequence(await replaceCampaignSequence(campaign));
      setNotice("Secuencia declarativa de contacto, espera y parada configurada.");
      await onChanged();
    });
  }

  return (
    <section className="stack">
      <div className="alert safety">
        Esta sección solo crea borradores y simulaciones. Los cuatro controles de envío
        permanecen bloqueados y no existe una acción “Enviar”.
      </div>
      {error && <div className="alert error">{error}</div>}
      {notice && <div className="alert success">{notice}</div>}
      {writable && (
        <div className="two-column equal">
          <Panel title="Nueva plantilla versionada">
            <form className="form-grid" onSubmit={(event) => void submitTemplate(event)}>
              <label>
                Nombre
                <input required value={templateName} onChange={(event) => setTemplateName(event.target.value)} />
              </label>
              <label>
                Canal
                <select value={templateChannel} onChange={(event) => setTemplateChannel(event.target.value as CampaignChannel)}>
                  <option value="EMAIL">EMAIL</option>
                  <option value="WHATSAPP">WHATSAPP</option>
                </select>
              </label>
              <label className="full-width">
                Asunto
                <input required value={subject} onChange={(event) => setSubject(event.target.value)} />
              </label>
              <label className="full-width">
                Texto
                <textarea required value={textBody} onChange={(event) => setTextBody(event.target.value)} />
              </label>
              <label className="full-width">
                HTML seguro
                <textarea required value={htmlBody} onChange={(event) => setHtmlBody(event.target.value)} />
              </label>
              <button className="primary-button" type="submit">Crear versión 1</button>
            </form>
          </Panel>
          <Panel title="Nueva campaña">
            <form className="form-grid" onSubmit={(event) => void submitCampaign(event)}>
              <label className="full-width">
                Nombre
                <input required value={campaignName} onChange={(event) => setCampaignName(event.target.value)} />
              </label>
              <label className="full-width">
                Plantilla
                <select required value={templateVersionId} onChange={(event) => setTemplateVersionId(event.target.value)}>
                  <option value="">Seleccionar…</option>
                  {templates.map((template) => (
                    <option key={template.versionId} value={template.versionId}>
                      {template.name} · v{template.versionNumber} · {template.channel}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Provincia (opcional)
                <input value={province} onChange={(event) => setProvince(event.target.value)} />
              </label>
              <label>
                Score mínimo
                <input type="number" min="0" max="100" value={scoreAtLeast} onChange={(event) => setScoreAtLeast(event.target.value)} />
              </label>
              <button className="primary-button" type="submit">Crear borrador</button>
            </form>
          </Panel>
        </div>
      )}
      <Panel title="Plantillas disponibles">
        <div className="card-grid">
          {templates.map((template) => (
            <article className="entity-card" key={template.versionId}>
              <div><strong>{template.name}</strong><Badge value={`${template.channel} · v${template.versionNumber}`} /></div>
              <p>{template.subject}</p>
              <small>Variables: {template.variables.join(", ") || "ninguna"}</small>
              <button className="secondary-button" type="button" onClick={() => void showPreview(template)}>Previsualizar</button>
            </article>
          ))}
          {templates.length === 0 && <EmptyState text="Todavía no hay plantillas." />}
        </div>
        {preview && (
          <div className="preview-box" aria-live="polite">
            <strong>{preview.subject}</strong>
            <p>{preview.textBody}</p>
          </div>
        )}
      </Panel>
      <Panel title="Campañas y audiencias">
        <div className="card-grid">
          {campaigns.map((campaign) => (
            <article className="entity-card" key={campaign.id}>
              <div><strong>{campaign.name}</strong><Badge value={campaign.status} /></div>
              <p>{campaign.channel} · {campaign.templateName}</p>
              <small>{campaign.recipientCount} incluidos · {campaign.excludedCount} excluidos · dry-run</small>
              <div className="action-row">
                {writable && ["DRAFT", "READY_FOR_REVIEW"].includes(campaign.status) && (
                  <button className="secondary-button" type="button" onClick={() => void freeze(campaign)}>Congelar audiencia</button>
                )}
                {writable && campaign.status === "DRAFT" && (
                  <button className="secondary-button" type="button" onClick={() => void configureSequence(campaign)}>Configurar secuencia segura</button>
                )}
                <button className="secondary-button" type="button" onClick={() => void run(async () => setAudience(await getCampaignAudience(campaign.id)))}>Ver audiencia</button>
                <button className="secondary-button" type="button" onClick={() => void run(async () => setSequence(await getCampaignSequence(campaign.id)))}>Ver secuencia</button>
                {campaign.status === "READY_FOR_REVIEW" && session.permissions.includes("CAMPAIGN_APPROVE") && (
                  <button className="primary-button" type="button" onClick={() => void approve(campaign)}>Aprobar</button>
                )}
                {["APPROVED", "SIMULATED"].includes(campaign.status) && session.permissions.includes("MESSAGE_SIMULATE") && (
                  <button className="primary-button" type="button" onClick={() => void simulate(campaign)}>Simular</button>
                )}
              </div>
            </article>
          ))}
          {campaigns.length === 0 && <EmptyState text="No hay campañas." />}
        </div>
      </Panel>
      {audience.length > 0 && (
        <Panel title="Audiencia congelada">
          <div className="table-scroll"><table><thead><tr><th>Prospecto</th><th>Contacto</th><th>Decisión</th><th>Motivo</th></tr></thead><tbody>
            {audience.map((recipient) => <tr key={recipient.prospectId}><td>{recipient.prospectName}</td><td>{recipient.contactName || "—"}</td><td><Badge value={recipient.validationStatus} /></td><td>{recipient.exclusionReason || "Incluido"}</td></tr>)}
          </tbody></table></div>
        </Panel>
      )}
      {simulation && <div className="alert success">Run fake {simulation.id}: ningún envío de red; {simulation.includedCount} actividades de borrador.</div>}
      {sequence.length > 0 && (
        <Panel title="Secuencia declarativa">
          <ol className="sequence-list">
            {sequence.map((step) => (
              <li key={step.id}><strong>{step.type}</strong><code>{JSON.stringify(step.configuration)}</code></li>
            ))}
          </ol>
        </Panel>
      )}
    </section>
  );
}

function PipelinePanel({
  prospects,
  opportunities,
  metrics,
  session,
  onChanged,
}: {
  prospects: Prospect[];
  opportunities: Opportunity[];
  metrics: PipelineMetrics | null;
  session: SessionUser;
  onChanged: () => Promise<void>;
}) {
  const [prospectId, setProspectId] = useState(prospects[0]?.id ?? "");
  const [name, setName] = useState("");
  const [estimatedValue, setEstimatedValue] = useState("0");
  const [expectedCloseDate, setExpectedCloseDate] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const canWrite = session.permissions.includes("OPPORTUNITY_WRITE");

  useEffect(() => {
    if (!prospectId && prospects[0]) setProspectId(prospects[0].id);
  }, [prospectId, prospects]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await createOpportunity({
        prospectId,
        name,
        ownerId: session.userId,
        estimatedValue: Number(estimatedValue),
        currency: "ARS",
        probability: 10,
        expectedCloseDate: expectedCloseDate || undefined,
        source: "MANUAL",
        primaryActive: true,
      });
      setName("");
      setEstimatedValue("0");
      setExpectedCloseDate("");
      await onChanged();
    } catch (caught) {
      setError(message(caught));
    } finally {
      setBusy(false);
    }
  }

  async function move(opportunity: Opportunity, stage: OpportunityStage) {
    let reason: string | undefined;
    if (stage === "LOST" || stage === "WON") {
      reason = window.prompt(stage === "LOST" ? "Motivo de pérdida:" : "Motivo de cierre ganado:")?.trim();
      if (!reason) return;
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
  }

  return (
    <section className="stack">
      <div className="metric-grid">
        <Metric label="Oportunidades activas" value={metrics?.activeCount ?? 0} />
        <Metric label="Estancadas +30 días" value={metrics?.stalledCount ?? 0} />
        <article className="metric-card">
          <span>Pipeline</span>
          <strong>{money(metrics?.totalValue ?? 0)}</strong>
        </article>
        <article className="metric-card">
          <span>Forecast ponderado</span>
          <strong>{money(metrics?.weightedValue ?? 0)}</strong>
        </article>
      </div>
      <Panel title="Nueva oportunidad">
        {error && <div className="alert error">{error}</div>}
        {canWrite ? (
          <form className="inline-form" onSubmit={(event) => void submit(event)}>
            <label className="grow">
              Prospecto
              <select value={prospectId} onChange={(event) => setProspectId(event.target.value)} required>
                {prospects.map((prospect) => (
                  <option key={prospect.id} value={prospect.id}>{prospect.displayName}</option>
                ))}
              </select>
            </label>
            <label className="grow">
              Nombre
              <input value={name} onChange={(event) => setName(event.target.value)} required />
            </label>
            <label>
              Valor estimado ARS
              <input type="number" min="0" step="0.01" value={estimatedValue} onChange={(event) => setEstimatedValue(event.target.value)} required />
            </label>
            <label>
              Cierre esperado
              <input type="date" value={expectedCloseDate} onChange={(event) => setExpectedCloseDate(event.target.value)} />
            </label>
            <button className="primary-button" disabled={busy || prospects.length === 0}>Crear oportunidad</button>
          </form>
        ) : (
          <p className="muted">Acceso de solo lectura al pipeline.</p>
        )}
      </Panel>
      <Panel title="Pipeline por etapa">
        <div className="kanban" aria-label="Pipeline de oportunidades">
          {opportunityStages.map((stage) => (
            <section className="kanban-column" key={stage}>
              <header><strong>{stage}</strong><span>{metrics?.byStage[stage] ?? 0}</span></header>
              {opportunities.filter((item) => item.stage === stage).map((opportunity) => (
                <article className="opportunity-card" key={opportunity.id}>
                  <strong>{opportunity.name}</strong>
                  <span>{opportunity.prospectName}</span>
                  <span>{money(opportunity.estimatedValue)} · {opportunity.probability}%</span>
                  <small>{opportunity.ownerName}{opportunity.primaryActive ? " · Principal" : ""}</small>
                  {canWrite && !["WON", "LOST"].includes(opportunity.stage) && (
                    <div className="review-actions">
                      {nextOpportunityStages(opportunity.stage).map((next) => (
                        <button key={next} className={next === "LOST" ? "danger-button" : "secondary-button"} disabled={busy} onClick={() => void move(opportunity, next)}>
                          {next}
                        </button>
                      ))}
                    </div>
                  )}
                </article>
              ))}
            </section>
          ))}
        </div>
      </Panel>
      <Panel title="Tabla de oportunidades">
        {opportunities.length === 0 ? <EmptyState text="Todavía no hay oportunidades." /> : (
          <div className="table-scroll"><table><thead><tr><th>Oportunidad</th><th>Prospecto</th><th>Etapa</th><th>Valor</th><th>Probabilidad</th><th>Cierre</th><th>Responsable</th></tr></thead>
          <tbody>{opportunities.map((opportunity) => <tr key={opportunity.id}><td>{opportunity.name}</td><td>{opportunity.prospectName}</td><td><Badge value={opportunity.stage} /></td><td>{money(opportunity.estimatedValue)}</td><td>{opportunity.probability}%</td><td>{opportunity.actualCloseDate ?? opportunity.expectedCloseDate ?? "—"}</td><td>{opportunity.ownerName}</td></tr>)}</tbody></table></div>
        )}
      </Panel>
    </section>
  );
}

function nextOpportunityStages(stage: OpportunityStage): OpportunityStage[] {
  return {
    QUALIFICATION: ["DISCOVERY", "LOST"],
    DISCOVERY: ["QUALIFICATION", "DEMO", "PROPOSAL", "LOST"],
    DEMO: ["DISCOVERY", "PROPOSAL", "LOST"],
    PROPOSAL: ["DEMO", "NEGOTIATION", "WON", "LOST"],
    NEGOTIATION: ["PROPOSAL", "WON", "LOST"],
    WON: [],
    LOST: [],
  }[stage] as OpportunityStage[];
}

function ImportsPanel({
  duplicateReviews,
  onChanged,
}: {
  duplicateReviews: DuplicateReview[];
  onChanged: () => Promise<void>;
}) {
  const [file, setFile] = useState<File | null>(null);
  const [summary, setSummary] = useState<ImportSummary | null>(null);
  const [rows, setRows] = useState<ImportRow[]>([]);
  const [busy, setBusy] = useState(false);
  const [resolvingId, setResolvingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function run(execute: boolean) {
    if (!file) {
      setError("Seleccioná un archivo CSV o XLSX.");
      return;
    }
    if (execute && !window.confirm("¿Ejecutar la importación? Esta acción escribirá en PostgreSQL.")) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const result = await importProspects(file, execute);
      setSummary(result);
      setRows(await getImportRows(result.id));
      await onChanged();
    } catch (caught) {
      setError(message(caught));
    } finally {
      setBusy(false);
    }
  }

  async function resolve(review: DuplicateReview, action: DuplicateResolutionAction) {
    let separateName: string | undefined;
    let absorbedProspectId: string | undefined;
    if (action === "CREATE_SEPARATE" || action === "MARK_NOT_DUPLICATE") {
      separateName = window.prompt("Nombre del prospecto independiente:")?.trim();
      if (!separateName) return;
    }
    if (action === "MERGE") {
      absorbedProspectId = window.prompt("ID del prospecto que será absorbido:")?.trim();
      if (!absorbedProspectId) return;
      if (!window.confirm("El merge es transaccional y conservará la trazabilidad. ¿Continuar?")) {
        return;
      }
    }
    if (action === "REJECT_ROW" && !window.confirm("¿Rechazar esta fila de importación?")) return;

    setResolvingId(review.id);
    setError(null);
    try {
      await resolveDuplicateReview(review.id, {
        action,
        survivorProspectId: review.existingProspectId ?? undefined,
        absorbedProspectId,
        separateName,
        comment: `Resolución manual ${action}`,
        idempotencyKey: `${review.id}:${action}:${crypto.randomUUID()}`,
      });
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
        <p className="muted">
          El preview persiste evidencia de validación, pero no crea prospectos ni exclusiones.
        </p>
        {error && <div className="alert error">{error}</div>}
        <div className="import-actions">
          <input
            type="file"
            accept=".csv,.xlsx"
            onChange={(event) => setFile(event.target.files?.[0] ?? null)}
          />
          <button className="secondary-button" disabled={busy} onClick={() => void run(false)}>
            Ejecutar preview
          </button>
          <button className="danger-button" disabled={busy} onClick={() => void run(true)}>
            Importar con confirmación
          </button>
        </div>
        {summary && <ImportSummaryView summary={summary} />}
      </Panel>
      {rows.length > 0 && (
        <Panel title="Resultado por fila">
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Hoja</th>
                  <th>Fila</th>
                  <th>Estado</th>
                  <th>Error</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.id}>
                    <td>{row.sourceSheet}</td>
                    <td>{row.rowNumber}</td>
                    <td>
                      <Badge value={row.status} />
                    </td>
                    <td>{row.errorMessage ?? "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Panel>
      )}
      <Panel title="Duplicados ambiguos pendientes">
        {duplicateReviews.length === 0 ? (
          <EmptyState text="No hay coincidencias ambiguas pendientes." />
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Hoja/Fila</th>
                  <th>Registro importado</th>
                  <th>Coincidencia existente</th>
                  <th>Señales</th>
                  <th>Resolución</th>
                </tr>
              </thead>
              <tbody>
                {duplicateReviews.map((review) => (
                  <tr key={review.id}>
                    <td>
                      {review.sourceSheet}/{review.rowNumber}
                    </td>
                    <td>
                      <code className="source-evidence">{review.sourceData}</code>
                      <small>{review.normalizedEmail ?? review.normalizedPhone ?? "Sin canal"}</small>
                    </td>
                    <td>
                      {review.existingProspect ? (
                        <>
                          <strong>{review.existingProspect.displayName}</strong>
                          <small>
                            {review.existingProspect.locality ?? "Sin localidad"} · {review.existingProspect.status}
                          </small>
                        </>
                      ) : (
                        "Sin candidato enlazado"
                      )}
                    </td>
                    <td>
                      <Badge value={review.matchType} />
                      <small>{Math.round(review.confidence * 100)}% · {review.matchReasons ?? "Sin detalle"}</small>
                    </td>
                    <td>
                      <div className="review-actions">
                        <button className="secondary-button" disabled={resolvingId === review.id} onClick={() => void resolve(review, "LINK_TO_EXISTING")}>
                          Vincular
                        </button>
                        <button className="secondary-button" disabled={resolvingId === review.id} onClick={() => void resolve(review, "MARK_NOT_DUPLICATE")}>
                          No duplicado
                        </button>
                        <button className="secondary-button" disabled={resolvingId === review.id} onClick={() => void resolve(review, "CREATE_SEPARATE")}>
                          Crear separado
                        </button>
                        <button className="secondary-button" disabled={resolvingId === review.id} onClick={() => void resolve(review, "MERGE")}>
                          Fusionar
                        </button>
                        <button className="secondary-button" disabled={resolvingId === review.id} onClick={() => void resolve(review, "DEFER")}>
                          Diferir
                        </button>
                        <button className="danger-button" disabled={resolvingId === review.id} onClick={() => void resolve(review, "REJECT_ROW")}>
                          Rechazar
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Panel>
    </section>
  );
}

function ExclusionsPanel({
  exclusions,
  canWrite,
  onChanged,
}: {
  exclusions: Exclusion[];
  canWrite: boolean;
  onChanged: () => Promise<void>;
}) {
  const [channelType, setChannelType] = useState<Exclusion["channelType"]>("EMAIL");
  const [value, setValue] = useState("");
  const [reason, setReason] = useState("MANUAL");
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    try {
      await createExclusion({ channelType, value, reason });
      setValue("");
      await onChanged();
    } catch (caught) {
      setError(message(caught));
    }
  }

  return (
    <section className="stack">
      <Panel title="Crear exclusión dominante">
        {error && <div className="alert error">{error}</div>}
      {canWrite && <form className="inline-form" onSubmit={(event) => void submit(event)}>
          <label>
            Canal
            <select
              value={channelType}
              onChange={(event) => setChannelType(event.target.value as Exclusion["channelType"])}
            >
              <option value="EMAIL">Email</option>
              <option value="PHONE">Teléfono</option>
              <option value="WHATSAPP">WhatsApp</option>
              <option value="WEBSITE">Sitio web</option>
              <option value="SOCIAL">Red social</option>
            </select>
          </label>
          <label className="grow">
            Valor
            <input value={value} onChange={(event) => setValue(event.target.value)} required />
          </label>
          <label>
            Motivo
            <select value={reason} onChange={(event) => setReason(event.target.value)}>
              <option value="MANUAL">Exclusión manual</option>
              <option value="UNSUBSCRIBE_REQUEST">Pidió baja</option>
              <option value="NEGATIVE_REPLY">Respondió no</option>
              <option value="PERMANENT_BOUNCE">Rebote permanente</option>
              <option value="INVALID_CONTACT">Contacto inválido</option>
              <option value="EXISTING_CUSTOMER">Cliente existente</option>
              <option value="EXISTING_CONVERSATION">Conversación existente</option>
              <option value="IRRELEVANT_INSTITUTION">Institución no pertinente</option>
            </select>
          </label>
          <button className="danger-button">Excluir</button>
      </form>}
      </Panel>
      <Panel title="Canales excluidos">
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Canal</th>
                <th>Valor normalizado</th>
                <th>Motivo</th>
                <th>Creada</th>
              </tr>
            </thead>
            <tbody>
              {exclusions.map((exclusion) => (
                <tr key={exclusion.id}>
                  <td>{exclusion.channelType}</td>
                  <td>{exclusion.normalizedValue}</td>
                  <td>{exclusion.reason}</td>
                  <td>{dateTime(exclusion.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Panel>
    </section>
  );
}

function CreateProspectForm({ onCreated }: { onCreated: (prospect: Prospect) => Promise<void> }) {
  const [name, setName] = useState("");
  const [city, setCity] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const created = await createProspect({
        institutionName: name,
        locality: city || undefined,
        country: "Argentina",
        source: "MANUAL",
      });
      setName("");
      setCity("");
      await onCreated(created);
    } catch (caught) {
      setError(message(caught));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="inline-form compact-form" onSubmit={(event) => void submit(event)}>
      <label className="grow">
        Nueva institución
        <input value={name} onChange={(event) => setName(event.target.value)} required />
      </label>
      <label>
        Localidad
        <input value={city} onChange={(event) => setCity(event.target.value)} />
      </label>
      <button className="primary-button" disabled={busy}>
        {busy ? "Creando…" : "Crear prospecto"}
      </button>
      {error && <span className="inline-error" role="alert">{error}</span>}
    </form>
  );
}

function ProspectDetail({
  prospect,
  session,
  onChanged,
}: {
  prospect: Prospect;
  session: SessionUser;
  onChanged: () => Promise<void>;
}) {
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [timeline, setTimeline] = useState<TimelineItem[]>([]);
  const [name, setName] = useState(prospect.displayName);
  const [city, setCity] = useState(prospect.city ?? "");
  const [firstName, setFirstName] = useState("");
  const [email, setEmail] = useState("");
  const [note, setNote] = useState("");
  const [activityType, setActivityType] = useState<
    "EMAIL_SENT_MANUALLY" | "WHATSAPP_SENT_MANUALLY" | "PHONE_CALL" | "MEETING" | "DEMO"
  >("PHONE_CALL");
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
    await run(
      () =>
        updateProspect(prospect.id, {
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
        }),
      "Prospecto actualizado.",
    );
  }

  const transitions = allowedTransitions(prospect.status);

  return (
    <div className="stack detail-workspace">
      {error && <div className="alert error" role="alert">{error}</div>}
      {notice && <div className="alert success" role="status">{notice}</div>}
      <dl className="detail-grid">
        <Detail label="Institución" value={prospect.displayName} />
        <Detail label="Razón social" value={prospect.legalName} />
        <Detail label="Ubicación" value={[prospect.city, prospect.province].filter(Boolean).join(", ")} />
        <Detail label="Estado" value={prospect.status} />
        <Detail label="Elegibilidad" value={prospect.eligibility} />
        <Detail label="Prioridad" value={prospect.priority?.toString()} />
        <Detail label="Puntuación" value={prospect.score?.toString()} />
        <Detail label="Responsable" value={prospect.ownerName} />
        <Detail label="Próxima acción" value={prospect.nextActionAt ? dateTime(prospect.nextActionAt) : null} />
        <Detail label="Último contacto" value={prospect.lastContactAt ? dateTime(prospect.lastContactAt) : null} />
      </dl>

      {canWrite && (
        <form className="inline-form compact-form" onSubmit={(event) => void save(event)}>
          <label className="grow">
            Nombre visible
            <input value={name} onChange={(event) => setName(event.target.value)} required />
          </label>
          <label>
            Localidad
            <input value={city} onChange={(event) => setCity(event.target.value)} />
          </label>
          <button className="secondary-button">Guardar</button>
        </form>
      )}

      {canWrite && transitions.length > 0 && (
        <div className="action-row" aria-label="Transiciones permitidas">
          {transitions.map((status) => (
            <button
              className="secondary-button"
              key={status}
              onClick={() => void run(() => transitionProspect(prospect.id, prospect.version, status), `Estado cambiado a ${status}.`)}
            >
              Pasar a {status}
            </button>
          ))}
        </div>
      )}

      <section className="subsection">
        <h3>Contactos</h3>
        {contacts.length === 0 ? <EmptyState text="No hay contactos." /> : contacts.map((contact) => (
          <article className="timeline-item" key={contact.id}>
            <strong>{contact.displayName}</strong>
            <span>{contact.role ?? "Sin cargo"}</span>
            {contact.channels.map((channel) => <small key={channel.id}>{channel.type}: {channel.value}</small>)}
          </article>
        ))}
        {canWrite && (
          <form className="inline-form compact-form" onSubmit={(event) => {
            event.preventDefault();
            void run(() => createContact(prospect.id, { firstName, email }), "Contacto agregado.").then(() => {
              setFirstName("");
              setEmail("");
            });
          }}>
            <label>
              Nombre
              <input value={firstName} onChange={(event) => setFirstName(event.target.value)} required />
            </label>
            <label className="grow">
              Email
              <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} />
            </label>
            <button className="secondary-button">Agregar contacto</button>
          </form>
        )}
      </section>

      <section className="subsection">
        <h3>Tareas</h3>
        {tasks.length === 0 ? <EmptyState text="No hay tareas." /> : tasks.map((task) => (
          <article className="timeline-item" key={task.id}>
            <strong>{task.title}</strong>
            <span>{task.status} · vence {dateTime(task.dueAt)}</span>
            {canWriteActivity && task.status !== "COMPLETED" && task.status !== "CANCELLED" && (
              <button className="secondary-button" onClick={() => void run(() => changeTaskStatus(task, "COMPLETED"), "Tarea completada.")}>Completar</button>
            )}
          </article>
        ))}
        {canWriteActivity && (
          <form className="inline-form compact-form" onSubmit={(event) => {
            event.preventDefault();
            void run(() => createTask(prospect.id, { ownerUserId: session.userId, title: taskTitle, dueAt: new Date(taskDueAt).toISOString() }), "Tarea creada.").then(() => {
              setTaskTitle("");
              setTaskDueAt("");
            });
          }}>
            <label className="grow">
              Nueva tarea
              <input value={taskTitle} onChange={(event) => setTaskTitle(event.target.value)} required />
            </label>
            <label>
              Vencimiento
              <input type="datetime-local" value={taskDueAt} onChange={(event) => setTaskDueAt(event.target.value)} required />
            </label>
            <button className="secondary-button">Crear tarea</button>
          </form>
        )}
      </section>

      <section className="subsection">
        <h3>Timeline</h3>
        {canWriteActivity && (
          <form className="inline-form compact-form" onSubmit={(event) => {
            event.preventDefault();
            void run(
              () => createActivity(prospect.id, { type: activityType, summary: activitySummary }),
              "Actividad registrada.",
            ).then(() => setActivitySummary(""));
          }}>
            <label>
              Tipo de actividad
              <select
                value={activityType}
                onChange={(event) => setActivityType(event.target.value as typeof activityType)}
              >
                <option value="PHONE_CALL">Llamada</option>
                <option value="MEETING">Reunión</option>
                <option value="DEMO">Demo</option>
                <option value="EMAIL_SENT_MANUALLY">Email enviado manualmente</option>
                <option value="WHATSAPP_SENT_MANUALLY">WhatsApp enviado manualmente</option>
              </select>
            </label>
            <label className="grow">
              Resumen de actividad
              <input
                value={activitySummary}
                onChange={(event) => setActivitySummary(event.target.value)}
                required
              />
            </label>
            <button className="secondary-button">Registrar actividad</button>
          </form>
        )}
        {canWriteActivity && (
          <form className="inline-form compact-form" onSubmit={(event) => {
            event.preventDefault();
            void run(() => createNote(prospect.id, note), "Nota registrada.").then(() => setNote(""));
          }}>
            <label className="grow">
              Nota
              <input value={note} onChange={(event) => setNote(event.target.value)} required />
            </label>
            <button className="secondary-button">Agregar nota</button>
          </form>
        )}
        {timeline.length === 0 ? <EmptyState text="Todavía no hay eventos." /> : timeline.map((item) => (
          <article className="timeline-item" key={`${item.eventType}-${item.id}`}>
            <small>{dateTime(item.eventAt)} · {item.eventType}</small>
            <strong>{item.title}</strong>
            {item.detail && <span>{item.detail}</span>}
          </article>
        ))}
      </section>
    </div>
  );
}

function allowedTransitions(status: ProspectStatus): ProspectStatus[] {
  const transitions: Partial<Record<ProspectStatus, ProspectStatus[]>> = {
    NEW: ["QUALIFYING", "DO_NOT_CONTACT"],
    QUALIFYING: ["READY_TO_CONTACT", "LOST", "DO_NOT_CONTACT"],
    READY_TO_CONTACT: ["CONTACTED", "DO_NOT_CONTACT"],
    CONTACTED: ["REPLIED", "FOLLOW_UP", "LOST", "DO_NOT_CONTACT"],
    REPLIED: ["INTERESTED", "FOLLOW_UP", "LOST", "DO_NOT_CONTACT"],
    INTERESTED: ["DEMO_PROPOSED", "PROPOSAL", "FOLLOW_UP", "LOST", "DO_NOT_CONTACT"],
    DEMO_PROPOSED: ["DEMO_SCHEDULED", "FOLLOW_UP", "LOST", "DO_NOT_CONTACT"],
    DEMO_SCHEDULED: ["PROPOSAL", "FOLLOW_UP", "LOST", "DO_NOT_CONTACT"],
    PROPOSAL: ["NEGOTIATION", "LOST", "DO_NOT_CONTACT"],
    NEGOTIATION: ["CUSTOMER", "LOST", "DO_NOT_CONTACT"],
  };
  return transitions[status] ?? [];
}

function ImportSummaryView({ summary }: { summary: ImportSummary }) {
  return (
    <div className="summary-grid">
      <Control label="Estado" value={summary.status} />
      <Control label="Modo" value={summary.dryRun ? "Preview" : "Ejecución"} />
      <Control label="Filas" value={summary.totalRows.toString()} />
      <Control label="Aceptadas" value={summary.acceptedRows.toString()} />
      <Control label="Bloqueadas" value={summary.excludedRows.toString()} />
      <Control label="Rechazadas" value={summary.rejectedRows.toString()} />
      <Control label="Duplicadas" value={summary.duplicateRows.toString()} />
      <Control label="A revisión" value={summary.reviewRows.toString()} />
    </div>
  );
}

function AuditTable({ events }: { events: AuditEvent[] }) {
  if (events.length === 0) {
    return <EmptyState text="Todavía no hay eventos de auditoría." />;
  }
  return (
    <div className="table-scroll">
      <table>
        <thead>
          <tr>
            <th>Fecha</th>
            <th>Acción</th>
            <th>Entidad</th>
            <th>Payload</th>
          </tr>
        </thead>
        <tbody>
          {events.map((event) => (
            <tr key={event.id}>
              <td>{dateTime(event.createdAt)}</td>
              <td>{event.action}</td>
              <td>
                {event.entityType} {event.entityId ? `#${event.entityId.slice(0, 8)}` : ""}
              </td>
              <td className="payload-cell">{event.payload}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function Panel({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="panel">
      <h2>{title}</h2>
      {children}
    </section>
  );
}

function Metric({ label, value }: { label: string; value: number | string }) {
  return (
    <article className="metric-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

function Control({ label, value }: { label: string; value: string }) {
  return (
    <div className="control-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function Detail({ label, value }: { label: string; value: string | null | undefined }) {
  return (
    <div>
      <dt>{label}</dt>
      <dd>{value || "—"}</dd>
    </div>
  );
}

function Badge({ value }: { value: string }) {
  return <span className={`badge badge-${value.toLowerCase().replaceAll("_", "-")}`}>{value}</span>;
}

function EmptyState({ text }: { text: string }) {
  return <p className="empty-state">{text}</p>;
}

function NavButton({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button className={active ? "nav-button active" : "nav-button"} onClick={onClick}>
      {children}
    </button>
  );
}

function title(tab: Tab): string {
  return {
    dashboard: "Dashboard",
    prospects: "Prospectos",
    pipeline: "Pipeline",
    campaigns: "Campañas y plantillas",
    messages: "Mensajes e integraciones",
    outbox: "Outbox y workers",
    inbound: "Inbound y quarantine",
    imports: "Importaciones",
    exclusions: "Exclusiones",
    audit: "Auditoría",
    reports: "Dashboard y reportes",
    settings: "Configuración, etiquetas e integraciones",
    users: "Usuarios",
    account: "Mi cuenta",
  }[tab];
}

function updateProspectUrl(query: string, status: ProspectStatus | "") {
  const parameters = new URLSearchParams(window.location.search);
  if (query) parameters.set("q", query);
  else parameters.delete("q");
  if (status) parameters.set("status", status);
  else parameters.delete("status");
  const suffix = parameters.toString();
  window.history.replaceState(null, "", `${window.location.pathname}${suffix ? `?${suffix}` : ""}`);
}

function money(value: number): string {
  return new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" }).format(value);
}

function dateTime(value: string): string {
  return new Intl.DateTimeFormat("es-AR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

function message(caught: unknown): string {
  return caught instanceof Error ? caught.message : "Ocurrió un error inesperado";
}

function escapeText(value: string): string {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}
