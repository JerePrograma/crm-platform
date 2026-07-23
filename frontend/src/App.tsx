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
import { openDecisionDialog } from "./decisionDialog";
import {
  auditSummary,
  channelLabel,
  duplicateSourceSummary,
  formatConfiguration,
  humanizeError,
  labelFor,
  opportunityStageLabel,
  prospectStatusLabel,
  safeTechnicalJson,
  suggestedDuplicateName,
} from "./uiLabels";

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
  const [prospectTotal, setProspectTotal] = useState(0);
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
        setProspectTotal(prospectPage.totalElements);
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

  async function clearSearch() {
    setSearchInput("");
    setSearchQuery("");
    updateProspectUrl("", statusFilter);
    await refresh(statusFilter, "");
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
            Resumen
          </NavButton>
          <NavButton active={tab === "prospects"} onClick={() => setTab("prospects")}>
            Prospectos
          </NavButton>
          <NavButton active={tab === "pipeline"} onClick={() => setTab("pipeline")}>
            Oportunidades
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
              Bandeja de salida
            </NavButton>
          )}
          {session.permissions.includes("REPORT_READ") && (
            <NavButton active={tab === "inbound"} onClick={() => setTab("inbound")}>
              Mensajes recibidos
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
        <div className="safety-panel" aria-label="Protecciones de envío activas">
          <strong>Los envíos reales están bloqueados</strong>
          <span>Modo de simulación activo</span>
          <span>Límite diario: 0</span>
          <span>Protección de emergencia activa</span>
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
            <p>Consultá el estado operativo y continuá con la siguiente acción segura. Ningún envío real está disponible.</p>
          </div>
          <button
            className="secondary-button"
            onClick={() => void refreshView()}
          >
            Actualizar
          </button>
        </header>

        {error && <div className="alert error" role="alert">{error}</div>}
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
                      placeholder="Institución, contacto, correo, teléfono, localidad o etiqueta"
                    />
                  </label>
                  <button className="secondary-button" type="submit">Buscar</button>
                  {(searchInput || searchQuery) && (
                    <button className="link-button" type="button" onClick={() => void clearSearch()}>
                      Limpiar búsqueda
                    </button>
                  )}
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
                        {prospectStatusLabel(status)}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
              <p className="result-context" role="status">
                {prospectTotal === 0
                  ? searchQuery || statusFilter
                    ? "No se encontraron prospectos. Revisá la escritura o quitá los filtros."
                    : "Todavía no hay prospectos cargados."
                  : `${prospectTotal.toLocaleString("es-AR")} resultado${prospectTotal === 1 ? "" : "s"}${searchQuery ? ` para “${searchQuery}”` : ""}.`}
              </p>
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
                        tabIndex={0}
                        aria-selected={selectedProspect?.id === prospect.id}
                        onClick={() => void selectProspect(prospect.id)}
                        onKeyDown={(event) => {
                          if (event.key === "Enter" || event.key === " ") {
                            event.preventDefault();
                            void selectProspect(prospect.id);
                          }
                        }}
                      >
                        <td>{prospect.displayName}</td>
                        <td>{prospect.city ?? "Sin localidad cargada"}</td>
                        <td>
                          <Badge value={prospect.status} />
                        </td>
                        <td><Badge value={prospect.contactEligible ? "ELIGIBLE" : "EXCLUDED"} /></td>
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
            prospects={prospects}
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
      {error && <div className="alert error" role="alert">{error}</div>}
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
            <Metric label="Mensajes que agotaron los reintentos" value={report.operations.deadLetter ?? 0} />
            <Metric label="Mensajes que requieren revisión" value={report.operations.quarantine ?? 0} />
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
      {Object.keys(values).length ? <dl className="detail-grid">{Object.entries(values).map(([key, value]) => <Detail key={key} label={labelFor(key)} value={Number(value).toLocaleString("es-AR")} />)}</dl> : <EmptyState text="Sin datos." />}
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
      {error && <div className="alert error" role="alert">{error}</div>}
      {notice && <div className="alert success" role="status">{notice}</div>}
      {settings ? <>
        <div className="metric-grid">
          <Metric label="Envíos reales" value={settings.sending.environmentEnabled ? "Habilitados" : "Bloqueados"} />
          <Metric label="Modo de simulación" value={settings.sending.environmentDryRun ? "Activo" : "Inactivo"} />
          <Metric label="Límite diario" value={settings.sending.environmentDailyLimit.toLocaleString("es-AR")} />
          <Metric label="Protección de emergencia" value={settings.sending.environmentKillSwitch ? "Activa" : "Inactiva"} />
        </div>
        <Panel title="Organización">
          <form className="form-grid" onSubmit={(event) => { event.preventDefault(); if (settings) void run(() => updateOrganizationSettings(settings), "Configuración actualizada; bloqueos de envío preservados."); }}>
            <label>Nombre<input disabled={!canManage} value={settings.name} onChange={(event) => setSettings({ ...settings, name: event.target.value })} /></label>
            <label>Zona horaria<input disabled={!canManage} value={settings.timezone} onChange={(event) => setSettings({ ...settings, timezone: event.target.value })} /></label>
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
          {tags.length ? <ul className="plain-list">{tags.map((tag) => <li key={tag.id}><span className="tag-swatch" style={{ background: tag.color }} aria-hidden="true" /><strong>{tag.name}</strong> · {tag.usageCount} usos {!tag.active && "· inactiva"}{canManage && tag.active && <button className="link-button" onClick={() => void openDecisionDialog({
                  title: "Desactivar etiqueta",
                  description: `La etiqueta “${tag.name}” dejará de estar disponible para nuevas asignaciones. El historial se conservará.`,
                  confirmLabel: "Desactivar etiqueta",
                }).then((answer) => { if (answer) return run(() => deactivateTag(tag), "Etiqueta desactivada sin borrar historial."); })}>Desactivar</button>}</li>)}</ul> : <EmptyState text="No hay etiquetas." />}
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
        El proceso vuelve a comprobar todas las protecciones antes de trabajar. No existe una acción
        para forzar proveedores externos ni marcar un mensaje como enviado.
      </div>
      {error && <div className="alert error" role="alert">{error}</div>}
      {notice && <div className="alert success" role="status">{notice}</div>}
      {loading && <div className="loading-bar" aria-label="Cargando bandeja de salida" />}
      <div className="metric-grid">
        {metrics.map((metric) => (
          <Metric key={metric.status} label={labelFor(metric.status)} value={metric.count} />
        ))}
        {metrics.length === 0 && <Metric label="Eventos" value={0} />}
      </div>
      <Panel title="Estado del proceso automático">
        {worker ? (
          <div className="control-grid">
            <Control label="Ejecución programada" value={worker.worker.schedulerEnabled ? "Habilitado" : "Manual"} />
            <Control label="Organización" value={worker.tenantPaused ? "Pausado" : "Activo"} />
            <Control label="Procesando" value={worker.worker.running ? "Sí" : "No"} />
            <Control label="Cantidad por ejecución" value={String(worker.worker.batchSize)} />
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
                setNotice(`Proceso: ${result.claimed} tomados, ${result.completed} completados.`);
              }, "Ejecución manual finalizada.")}
            >
              Ejecutar una vez
            </button>
            <button
              className="secondary-button"
              onClick={() => void operation(
                () => setOutboxWorkerPaused(!worker.tenantPaused),
                worker.tenantPaused ? "Proceso reanudado." : "Proceso pausado.",
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
                  <option key={value} value={value}>{labelFor(value)}</option>
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
                      <td>{labelFor(event.eventType)}</td>
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
                <Detail label="Identificador de seguimiento" value={selected.correlationId} />
                <Detail label="Registro relacionado" value={`${labelFor(selected.aggregateType)} / ${selected.aggregateId}`} />
                <Detail label="Resultado" value={selected.resultSummary ?? selected.lastErrorCode} />
                <Detail label="Error" value={selected.lastErrorSummary} />
                <Detail label="Procesado" value={selected.processedAt ? dateTime(selected.processedAt) : null} />
              </dl>
              <details className="technical-details">
                <summary>Ver datos técnicos</summary>
                <pre className="preview-box" aria-label="Datos técnicos sanitizados">{safeTechnicalJson(selected.payload)}</pre>
              </details>
              {canOperate && (
                <div className="button-row">
                  {selected.status === "DEAD" && (
                    <button className="primary-button" onClick={() => void openDecisionDialog({
                      title: "Reintentar procesamiento",
                      description: "El mensaje volverá a la cola sin modificar sus datos. Las protecciones de envío seguirán activas.",
                      confirmLabel: "Reintentar",
                    }).then((answer) => { if (answer) return operation(() => requeueOutboxEvent(selected.id), "Evento reencolado."); })}>Reintentar</button>
                  )}
                  {["PENDING", "RETRY"].includes(selected.status) && (
                    <button className="secondary-button" onClick={() => void openDecisionDialog({
                      title: "Cancelar procesamiento",
                      description: "El mensaje dejará de procesarse. La evidencia y el historial se conservarán.",
                      confirmLabel: "Cancelar procesamiento",
                      danger: true,
                    }).then((answer) => { if (answer) return operation(() => cancelOutboxEvent(selected.id), "Evento cancelado."); })}>Cancelar</button>
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
      {error && <div className="alert error" role="alert">{error}</div>}
      {notice && <div className="alert success" role="status">{notice}</div>}
      {loading && <div className="loading-bar" aria-label="Cargando mensajes recibidos" />}
      <Panel title="Recepción de prueba">
        <div className="control-grid">
          <Control label="Proveedor" value={health?.provider ?? "FAKE_INBOUND"} />
          <Control label="Recepción habilitada" value={health?.enabled ? "Habilitado" : "Deshabilitado"} />
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
                      <td>{channelLabel(item.channel)}</td>
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
        <Panel title="Detalle y asociación">
          {selected ? (
            <div className="stack compact">
              <dl className="detail-grid">
                <Detail label="Identificador del mensaje" value={selected.id} />
                <Detail label="Identificador de seguimiento" value={selected.correlationId} />
                <Detail label="Asociación" value={labelFor(selected.associationStatus)} />
                <Detail label="Prospecto" value={selected.prospectId} />
                <Detail label="Motivo de revisión" value={selected.quarantineReason} />
                <Detail label="Huella de integridad" value={selected.payloadHash} />
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
                    onClick={() => void openDecisionDialog({
                      title: "Descartar mensaje recibido",
                      description: "El mensaje dejará de estar pendiente de asociación. La evidencia se conservará para auditoría.",
                      confirmLabel: "Descartar con motivo",
                      danger: true,
                    }).then((answer) => { if (answer) return operation(() => discardInbound(selected.id, discardReason), "Mensaje descartado lógicamente."); })}
                  >Descartar con motivo</button>
                </div>
              )}
            </div>
          ) : (
            <EmptyState text="Seleccioná un mensaje recibido para ver metadata sanitizada." />
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
    const answer = await openDecisionDialog({
      title: user.active ? "Desactivar usuario" : "Activar usuario",
      description: user.active
        ? `“${user.displayName}” perderá el acceso hasta que vuelva a activarse.`
        : `“${user.displayName}” recuperará el acceso con su rol actual.`,
      confirmLabel: user.active ? "Desactivar usuario" : "Activar usuario",
      danger: user.active,
    });
    if (!answer) return;
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
      {error && <div className="alert error" role="alert">{error}</div>}
      {notice && <div className="alert success" role="status">{notice}</div>}
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
              <option value="ADMIN">Administrador</option>
              <option value="MANAGER">Responsable comercial</option>
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
                  <td>{labelFor(user.role)}</td>
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
          <Control label="Rol" value={labelFor(session.role)} />
          <Control label="Organización" value="Organización actual" />
        </div>
      </Panel>
      <Panel title="Cambiar mi contraseña">
        {error && <div className="alert error" role="alert">{error}</div>}
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
        {error && <div className="alert error" role="alert">{error}</div>}
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
        Los envíos reales están bloqueados. Esta pantalla solo permite preparar borradores,
        simular resultados o abrir una aplicación externa de forma manual.
      </div>
      {error && <div className="alert error" role="alert">{error}</div>}
      <div className="control-grid">
        <Control label="Email" value={safety?.selectedEmailProvider ?? "Consultando…"} />
        <Control label="WhatsApp" value={safety?.selectedWhatsAppProvider ?? "Consultando…"} />
        <Control
          label="Red real"
          value={safety?.realNetworkAllowed ? "Habilitada" : "Bloqueada"}
        />
        <Control
          label="Endpoint de envío"
          value={safety?.sendEndpointAvailable ? "Disponible" : "No existe"}
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
                <option value="EMAIL">Correo electrónico</option>
                <option value="WHATSAPP">WhatsApp</option>
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
          {labelFor(result.status)} mediante {labelFor(result.provider)}. Motivo de bloqueo: {labelFor(result.sendingBlockReason)}.
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
          <Control label="Gmail" value="Disponible, sin conexión externa" />
          <Control label="WhatsApp" value="Disponible, sin conexión externa" />
          <Control label="Modo de correo" value={labelFor(safety?.emailMode ?? "NOOP")} />
          <Control label="Modo de WhatsApp" value={labelFor(safety?.whatsAppMode ?? "DEEPLINK_ONLY")} />
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
    const answer = await openDecisionDialog({
      title: "Confirmar audiencia",
      description: "Se guardará una copia de los destinatarios que cumplen los filtros actuales. Las exclusiones seguirán teniendo prioridad.",
      confirmLabel: "Confirmar audiencia",
    });
    if (!answer) return;
    await run(async () => {
      const frozen = await freezeCampaignAudience(campaign, {
        province: province || undefined,
        scoreAtLeast: scoreAtLeast ? Number(scoreAtLeast) : undefined,
      });
      setAudience(await getCampaignAudience(frozen.id));
      setNotice(
        `Audiencia confirmada: ${frozen.recipientCount} incluidos, ${frozen.excludedCount} excluidos.`,
      );
      await onChanged();
    });
  }

  async function approve(campaign: Campaign) {
    const answer = await openDecisionDialog({
      title: "Aprobar para simulación",
      description: "La campaña quedará habilitada únicamente para generar una simulación. Esto no habilita envíos reales.",
      confirmLabel: "Aprobar simulación",
    });
    if (!answer) return;
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
      {error && <div className="alert error" role="alert">{error}</div>}
      {notice && <div className="alert success" role="status">{notice}</div>}
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
                  <option value="EMAIL">Correo electrónico</option>
                  <option value="WHATSAPP">WhatsApp</option>
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
                      {template.name} · v{template.versionNumber} · {channelLabel(template.channel)}
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
              <div><strong>{template.name}</strong><span className="badge">{channelLabel(template.channel)} · v{template.versionNumber}</span></div>
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
              <p>{channelLabel(campaign.channel)} · {campaign.templateName}</p>
              <small>{campaign.recipientCount} incluidos · {campaign.excludedCount} excluidos · modo de simulación</small>
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
      {simulation && <div className="alert success" role="status">Simulación {simulation.id.slice(0, 8)}: ningún envío real; {simulation.includedCount} borradores preparados.</div>}
      {sequence.length > 0 && (
        <Panel title="Secuencia declarativa">
          <ol className="sequence-list">
            {sequence.map((step) => (
              <li key={step.id}><strong>{labelFor(step.type)}</strong><span>{formatConfiguration(step.configuration)}</span></li>
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
      const response = await openDecisionDialog({
        title: stage === "LOST" ? "Registrar oportunidad perdida" : "Registrar oportunidad ganada",
        description:
          stage === "LOST"
            ? "Indicá el motivo para conservar contexto comercial y mejorar los reportes."
            : "Indicá qué permitió cerrar la oportunidad para conservar el aprendizaje comercial.",
        confirmLabel: stage === "LOST" ? "Registrar pérdida" : "Registrar cierre",
        danger: stage === "LOST",
        input: {
          label: stage === "LOST" ? "Motivo de pérdida" : "Motivo del cierre",
          placeholder: "Escribí un motivo breve y verificable",
          required: true,
        },
      });
      if (!response) return;
      reason = response;
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
        <Metric label="Sin avances durante 30 días" value={metrics?.stalledCount ?? 0} />
        <article className="metric-card"><span>Valor total</span><strong>{money(metrics?.totalValue ?? 0)}</strong></article>
        <article className="metric-card"><span>Proyección ponderada</span><strong>{money(metrics?.weightedValue ?? 0)}</strong></article>
      </div>
      <Panel title="Nueva oportunidad">
        {error && <div className="alert error" role="alert">{error}</div>}
        {canWrite ? (
          <form className="inline-form" onSubmit={(event) => void submit(event)}>
            <label className="grow">Prospecto<select value={prospectId} onChange={(event) => setProspectId(event.target.value)} required>
              <option value="">Seleccionar…</option>
              {prospects.map((prospect) => <option key={prospect.id} value={prospect.id}>{prospect.displayName}</option>)}
            </select></label>
            <label className="grow">Nombre de la oportunidad<input value={name} onChange={(event) => setName(event.target.value)} required /></label>
            <label>Valor estimado en ARS<input type="number" min="0" step="0.01" value={estimatedValue} onChange={(event) => setEstimatedValue(event.target.value)} required /></label>
            <label>Fecha estimada de cierre<input type="date" value={expectedCloseDate} onChange={(event) => setExpectedCloseDate(event.target.value)} /></label>
            <button className="primary-button" disabled={busy || prospects.length === 0}>Crear oportunidad</button>
          </form>
        ) : <p className="muted">Tu rol permite consultar oportunidades, pero no modificarlas.</p>}
      </Panel>
      <Panel title="Oportunidades por etapa">
        <div className="kanban" aria-label="Oportunidades por etapa">
          {opportunityStages.map((stage) => {
            const stageItems = opportunities.filter((item) => item.stage === stage);
            return (
              <section className="kanban-column" key={stage} aria-labelledby={`stage-${stage}`}>
                <header><strong id={`stage-${stage}`}>{opportunityStageLabel(stage)}</strong><span>{metrics?.byStage[stage] ?? 0}</span></header>
                {stageItems.length === 0 && <p className="kanban-empty">Sin oportunidades en esta etapa.</p>}
                {stageItems.map((opportunity) => (
                  <article className="opportunity-card" key={opportunity.id}>
                    <strong>{opportunity.name}</strong>
                    <span>{opportunity.prospectName}</span>
                    <span>{money(opportunity.estimatedValue)} · {opportunity.probability}% de probabilidad</span>
                    <small>Responsable: {opportunity.ownerName}{opportunity.primaryActive ? " · Oportunidad principal" : ""}</small>
                    <small>Último cambio: {relativeDate(opportunity.stageChangedAt)}</small>
                    {opportunity.expectedCloseDate && <small>Cierre estimado: {dateOnly(opportunity.expectedCloseDate)}</small>}
                    {canWrite && !["WON", "LOST"].includes(opportunity.stage) && (
                      <div className="review-actions" aria-label={`Acciones para ${opportunity.name}`}>
                        {nextOpportunityStages(opportunity.stage).map((next) => (
                          <button key={next} type="button" className={next === "LOST" ? "danger-button" : "secondary-button"} disabled={busy} onClick={() => void move(opportunity, next)}>
                            {next === "LOST" ? "Marcar como perdida" : next === "WON" ? "Marcar como ganada" : `Mover a ${opportunityStageLabel(next)}`}
                          </button>
                        ))}
                      </div>
                    )}
                  </article>
                ))}
              </section>
            );
          })}
        </div>
      </Panel>
      <Panel title="Listado completo">
        {opportunities.length === 0 ? <EmptyState text="Todavía no hay oportunidades. Creá la primera desde el formulario superior." /> : (
          <div className="table-scroll"><table><thead><tr><th>Oportunidad</th><th>Prospecto</th><th>Etapa</th><th>Responsable</th><th>Valor</th><th>Probabilidad</th></tr></thead><tbody>
            {opportunities.map((opportunity) => <tr key={opportunity.id}><td>{opportunity.name}</td><td>{opportunity.prospectName}</td><td><Badge value={opportunity.stage} /></td><td>{opportunity.ownerName}</td><td>{money(opportunity.estimatedValue)}</td><td>{opportunity.probability}%</td></tr>)}
          </tbody></table></div>
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
  prospects,
  onChanged,
}: {
  duplicateReviews: DuplicateReview[];
  prospects: Prospect[];
  onChanged: () => Promise<void>;
}) {
  const [file, setFile] = useState<File | null>(null);
  const [summary, setSummary] = useState<ImportSummary | null>(null);
  const [rows, setRows] = useState<ImportRow[]>([]);
  const [busy, setBusy] = useState(false);
  const [resolvingId, setResolvingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [rowStatus, setRowStatus] = useState("");
  const [rowQuery, setRowQuery] = useState("");
  const [rowPage, setRowPage] = useState(0);
  const pageSize = 25;
  const previewReady = Boolean(summary?.dryRun && file && summary.fileName === file.name);
  const filteredRows = rows.filter((row) => {
    const matchesStatus = !rowStatus || row.status === rowStatus;
    const query = rowQuery.trim().toLocaleLowerCase("es-AR");
    const matchesQuery = !query || `${row.sourceSheet} ${row.rowNumber} ${row.normalizedEmail ?? ""} ${row.normalizedPhone ?? ""} ${row.errorMessage ?? ""}`.toLocaleLowerCase("es-AR").includes(query);
    return matchesStatus && matchesQuery;
  });
  const pageCount = Math.max(1, Math.ceil(filteredRows.length / pageSize));
  const visibleRows = filteredRows.slice(rowPage * pageSize, (rowPage + 1) * pageSize);

  async function run(execute: boolean) {
    if (!file) {
      setError("Seleccioná un archivo CSV o XLSX.");
      return;
    }
    if (execute && !previewReady) {
      setError("Primero ejecutá la vista previa del archivo seleccionado y revisá su resumen.");
      return;
    }
    if (execute) {
      const answer = await openDecisionDialog({
        title: "Ejecutar importación",
        description: `Se escribirán los resultados validados de “${file.name}”. Las exclusiones tendrán prioridad y los casos ambiguos seguirán requiriendo revisión humana.`,
        confirmLabel: "Ejecutar importación",
        danger: true,
      });
      if (!answer) return;
    }
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const result = await importProspects(file, execute);
      setSummary(result);
      setRows(await getImportRows(result.id));
      setRowPage(0);
      setNotice(execute ? "Importación ejecutada. Revisá el resumen y los casos pendientes." : "Vista previa completada. Revisá los resultados antes de importar.");
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
    const actionLabel = labelFor(action);
    if (action === "CREATE_SEPARATE" || action === "MARK_NOT_DUPLICATE") {
      const response = await openDecisionDialog({
        title: actionLabel,
        description: "Se creará un prospecto independiente conservando únicamente los campos importados conocidos, sus canales válidos y la evidencia segura.",
        confirmLabel: actionLabel,
        input: {
          label: "Nombre del prospecto independiente",
          initialValue: suggestedDuplicateName(review.sourceData, "Nuevo prospecto"),
          required: true,
        },
      });
      if (!response) return;
      separateName = response;
    } else if (action === "MERGE") {
      const candidates = prospects
        .filter((prospect) => prospect.id !== review.existingProspectId)
        .map((prospect) => ({ value: prospect.id, label: prospect.displayName, description: prospect.city ?? "Sin localidad cargada" }));
      if (!review.existingProspectId || candidates.length === 0) {
        setError("No hay dos registros existentes disponibles para una fusión segura.");
        return;
      }
      const response = await openDecisionDialog({
        title: "Fusionar registros",
        description: `Se conservará “${review.existingProspect?.displayName ?? "el candidato existente"}”. Seleccioná el registro que se archivará y transferirá. Esta acción no puede deshacerse desde la interfaz.`,
        confirmLabel: "Fusionar y archivar",
        danger: true,
        choices: candidates,
      });
      if (!response) return;
      absorbedProspectId = response;
    } else {
      const descriptions: Record<DuplicateResolutionAction, string> = {
        LINK_TO_EXISTING: "La fila importada quedará vinculada al registro existente. No se creará un prospecto nuevo.",
        DEFER: "La revisión permanecerá pendiente para resolverla más adelante.",
        REJECT_ROW: "La fila se descartará de la importación. La evidencia se conservará para auditoría.",
        MARK_NOT_DUPLICATE: "",
        CREATE_SEPARATE: "",
        MERGE: "",
      };
      const response = await openDecisionDialog({
        title: actionLabel,
        description: descriptions[action],
        confirmLabel: actionLabel,
        danger: action === "REJECT_ROW",
      });
      if (!response) return;
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
        comment: `Resolución manual: ${actionLabel}`,
        idempotencyKey: `${review.id}:${action}:${crypto.randomUUID()}`,
      });
      setNotice(`Revisión resuelta: ${actionLabel}.`);
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
        <p className="muted">La vista previa es obligatoria. Valida el archivo y guarda evidencia, pero no crea prospectos ni exclusiones.</p>
        {error && <div className="alert error" role="alert">{error}</div>}
        {notice && <div className="alert success" role="status">{notice}</div>}
        <div className="import-actions">
          <label className="file-control">Archivo CSV o XLSX<input type="file" accept=".csv,.xlsx" onChange={(event) => {
            setFile(event.target.files?.[0] ?? null);
            setSummary(null);
            setRows([]);
            setRowPage(0);
            setError(null);
            setNotice(null);
          }} /></label>
          <button className="secondary-button" disabled={busy || !file} onClick={() => void run(false)}>{busy ? "Procesando…" : "Generar vista previa"}</button>
          <button className="danger-button" disabled={busy || !previewReady} title={!previewReady ? "Primero completá la vista previa del archivo seleccionado" : undefined} onClick={() => void run(true)}>Ejecutar importación</button>
        </div>
        {file && <p className="result-context">Archivo seleccionado: <strong>{file.name}</strong> · {(file.size / 1024).toLocaleString("es-AR", { maximumFractionDigits: 1 })} KB</p>}
        {summary && <ImportSummaryView summary={summary} />}
      </Panel>
      {rows.length > 0 && (
        <Panel title="Resultado por fila">
          <div className="toolbar">
            <label>Resultado<select value={rowStatus} onChange={(event) => { setRowStatus(event.target.value); setRowPage(0); }}><option value="">Todos</option>{["ACCEPTED", "EXCLUDED", "REJECTED", "DUPLICATE", "REVIEW_REQUIRED", "PENDING"].map((status) => <option key={status} value={status}>{labelFor(status)}</option>)}</select></label>
            <label className="grow">Buscar dentro de los resultados<input value={rowQuery} onChange={(event) => { setRowQuery(event.target.value); setRowPage(0); }} placeholder="Hoja, fila, correo, teléfono o error" /></label>
          </div>
          <p className="result-context" role="status">{filteredRows.length.toLocaleString("es-AR")} de {rows.length.toLocaleString("es-AR")} filas.</p>
          {visibleRows.length === 0 ? <EmptyState text="No hay filas que coincidan con los filtros actuales." /> : (
            <div className="table-scroll"><table><thead><tr><th>Hoja</th><th>Fila</th><th>Resultado</th><th>Canal detectado</th><th>Detalle</th></tr></thead><tbody>
              {visibleRows.map((row) => <tr key={row.id}><td>{row.sourceSheet}</td><td>{row.rowNumber}</td><td><Badge value={row.status} /></td><td>{row.normalizedEmail ?? row.normalizedPhone ?? "Sin canal utilizable"}</td><td>{row.errorMessage ? humanizeError(row.errorMessage) : "Sin observaciones"}</td></tr>)}
            </tbody></table></div>
          )}
          <div className="pagination" aria-label="Paginación de resultados"><button className="secondary-button" disabled={rowPage === 0} onClick={() => setRowPage((page) => page - 1)}>Anterior</button><span>Página {rowPage + 1} de {pageCount}</span><button className="secondary-button" disabled={rowPage + 1 >= pageCount} onClick={() => setRowPage((page) => page + 1)}>Siguiente</button></div>
        </Panel>
      )}
      <Panel title="Coincidencias que requieren revisión">
        {duplicateReviews.length === 0 ? <EmptyState text="No hay coincidencias ambiguas pendientes." /> : (
          <div className="duplicate-review-list">
            {duplicateReviews.map((review) => (
              <article className="duplicate-review-card" key={review.id}>
                <header><div><strong>Hoja {review.sourceSheet}, fila {review.rowNumber}</strong><span><Badge value={review.matchType} /> · {Math.round(review.confidence * 100)}% de coincidencia</span></div><small>{review.matchReasons ?? "La similitud requiere una decisión humana."}</small></header>
                <div className="duplicate-comparison">
                  <section><h3>Registro importado</h3><dl className="detail-grid">{duplicateSourceSummary(review.sourceData).map(([label, value]) => <Detail key={label} label={label} value={value} />)}<Detail label="Canal normalizado" value={review.normalizedEmail ?? review.normalizedPhone ?? "Sin canal utilizable"} /></dl><details className="technical-details"><summary>Ver evidencia técnica</summary><pre className="preview-box">{safeTechnicalJson(review.sourceData)}</pre></details></section>
                  <section><h3>Candidato existente</h3>{review.existingProspect ? <dl className="detail-grid"><Detail label="Institución" value={review.existingProspect.displayName} /><Detail label="Localidad" value={review.existingProspect.locality ?? "Sin localidad cargada"} /><Detail label="Sitio web" value={review.existingProspect.website ?? "Sin sitio cargado"} /><Detail label="Estado" value={labelFor(review.existingProspect.status)} /></dl> : <EmptyState text="No hay un candidato existente vinculado." />}</section>
                </div>
                <div className="review-actions">
                  <button className="secondary-button" disabled={resolvingId === review.id || !review.existingProspectId} onClick={() => void resolve(review, "LINK_TO_EXISTING")}>Vincular con el existente</button>
                  <button className="secondary-button" disabled={resolvingId === review.id} onClick={() => void resolve(review, "MARK_NOT_DUPLICATE")}>Confirmar que no es duplicado</button>
                  <button className="secondary-button" disabled={resolvingId === review.id} onClick={() => void resolve(review, "CREATE_SEPARATE")}>Crear registro independiente</button>
                  <button className="secondary-button" disabled={resolvingId === review.id || !review.existingProspectId} onClick={() => void resolve(review, "MERGE")}>Fusionar registros</button>
                  <button className="link-button" disabled={resolvingId === review.id} onClick={() => void resolve(review, "DEFER")}>Resolver más tarde</button>
                  <button className="danger-button" disabled={resolvingId === review.id} onClick={() => void resolve(review, "REJECT_ROW")}>Descartar esta fila</button>
                </div>
              </article>
            ))}
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
      <Panel title="Impedir el uso comercial de un canal">
        <p className="muted">La exclusión impide usar este canal en contactos y campañas. No elimina el prospecto ni su historial.</p>
        {error && <div className="alert error" role="alert">{error}</div>}
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
                  <td>{channelLabel(exclusion.channelType)}</td>
                  <td>{exclusion.normalizedValue}</td>
                  <td>{labelFor(exclusion.reason)}</td>
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
  const [lastName, setLastName] = useState("");
  const [contactRole, setContactRole] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [whatsapp, setWhatsapp] = useState("");
  const [preferredChannel, setPreferredChannel] = useState<"EMAIL" | "PHONE" | "WHATSAPP">("EMAIL");
  const [consent, setConsent] = useState<"UNKNOWN" | "GRANTED" | "DENIED">("UNKNOWN");
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
      const [contactList, taskList, timelinePage] = await Promise.all([listContacts(prospect.id), listTasks(), getTimeline(prospect.id)]);
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

  async function addContact(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!email.trim() && !phone.trim() && !whatsapp.trim()) {
      setError("Cargá al menos un correo, teléfono o WhatsApp para que el contacto sea utilizable.");
      return;
    }
    await run(() => createContact(prospect.id, {
      firstName,
      lastName: lastName || undefined,
      role: contactRole || undefined,
      email: email || undefined,
      phone: phone || undefined,
      whatsapp: whatsapp || undefined,
      preferredChannel,
      consent,
    }), "Contacto agregado y elegibilidad actualizada.");
    setFirstName(""); setLastName(""); setContactRole(""); setEmail(""); setPhone(""); setWhatsapp(""); setConsent("UNKNOWN");
  }

  const transitions = allowedTransitions(prospect.status);
  const primaryChannels = contacts.flatMap((contact) => contact.channels).filter((channel) => channel.primary || channel.preferred);

  return (
    <div className="stack detail-workspace">
      {error && <div className="alert error" role="alert">{error}</div>}
      {notice && <div className="alert success" role="status">{notice}</div>}
      <section className="prospect-hero">
        <div><span className="eyebrow">Prospecto</span><h2>{prospect.displayName}</h2><p>{[prospect.city, prospect.province].filter(Boolean).join(", ") || "Sin ubicación cargada"}</p></div>
        <div className="hero-badges"><Badge value={prospect.status} /><Badge value={prospect.eligibility} /></div>
      </section>
      <dl className="detail-grid priority-details">
        <Detail label="Responsable" value={prospect.ownerName ?? "Sin responsable asignado"} />
        <Detail label="Próxima acción" value={prospect.nextActionAt ? dateTime(prospect.nextActionAt) : "Sin próxima acción programada"} />
        <Detail label="Última actividad de contacto" value={prospect.lastContactAt ? dateTime(prospect.lastContactAt) : "Sin contactos registrados"} />
        <Detail label="Canal principal" value={primaryChannels[0] ? `${channelLabel(primaryChannels[0].type)}: ${primaryChannels[0].value}` : "Sin correo, teléfono o WhatsApp utilizable"} />
      </dl>

      <details className="disclosure-panel" open><summary>Resumen y datos administrativos</summary><div className="disclosure-content">
        <dl className="detail-grid">
          <Detail label="Institución" value={prospect.displayName} /><Detail label="Razón social" value={prospect.legalName ?? "Sin razón social cargada"} />
          <Detail label="Ubicación" value={[prospect.city, prospect.province].filter(Boolean).join(", ") || "Sin ubicación cargada"} /><Detail label="Estado" value={prospectStatusLabel(prospect.status)} />
          <Detail label="Elegibilidad" value={labelFor(prospect.eligibility)} /><Detail label="Prioridad" value={prospect.priority === null ? "Sin prioridad definida" : String(prospect.priority)} />
          <Detail label="Puntuación" value={prospect.score === null ? "Sin puntuación" : `${prospect.score}/100`} /><Detail label="Sitio web" value={prospect.website ?? "Sin sitio cargado"} />
        </dl>
        {canWrite && <form className="inline-form compact-form" onSubmit={(event) => void save(event)}><label className="grow">Nombre visible<input value={name} onChange={(event) => setName(event.target.value)} required /></label><label>Localidad<input value={city} onChange={(event) => setCity(event.target.value)} /></label><button className="secondary-button">Guardar cambios</button></form>}
        {canWrite && transitions.length > 0 && <div className="action-row" aria-label="Cambios de estado disponibles">{transitions.map((status) => <button className="secondary-button" type="button" key={status} onClick={() => void run(() => transitionProspect(prospect.id, prospect.version, status), `Estado cambiado a ${prospectStatusLabel(status)}.`)}>Pasar a {prospectStatusLabel(status)}</button>)}</div>}
      </div></details>

      <details className="disclosure-panel" open><summary>Contactos y canales</summary><div className="disclosure-content">
        <p className="muted">La posibilidad de contactar se calcula con los canales válidos y las exclusiones vigentes.</p>
        {contacts.length === 0 ? <EmptyState text="No hay contactos. Agregá una persona y al menos un canal utilizable." /> : contacts.map((contact) => <article className="contact-card" key={contact.id}><div><strong>{contact.displayName}</strong><span>{contact.role ?? "Sin cargo cargado"}</span><small>{contact.primary ? "Contacto principal" : "Contacto adicional"} · {labelFor(contact.consent)}</small></div><div className="channel-list">{contact.channels.length === 0 ? <span>Sin canales cargados</span> : contact.channels.map((channel) => <div className="channel-row" key={channel.id}><span><strong>{channelLabel(channel.type)}</strong> {channel.value}</span><span>{channel.valid ? "Válido" : "No válido"}{channel.preferred ? " · Preferido" : ""}</span><button type="button" className="link-button" onClick={() => void copyText(channel.value).then(() => setNotice(`${channelLabel(channel.type)} copiado.`)).catch(() => setError("No se pudo copiar el dato. Seleccionalo manualmente."))}>Copiar</button></div>)}</div></article>)}
        {canWrite && <form className="form-grid compact-form" onSubmit={(event) => void addContact(event)}><label>Nombre<input value={firstName} onChange={(event) => setFirstName(event.target.value)} required /></label><label>Apellido<input value={lastName} onChange={(event) => setLastName(event.target.value)} /></label><label className="full-width">Cargo o función<input value={contactRole} onChange={(event) => setContactRole(event.target.value)} placeholder="Ej.: Administración, dirección" /></label><label>Correo electrónico<input type="email" value={email} onChange={(event) => setEmail(event.target.value)} /></label><label>Teléfono<input type="tel" value={phone} onChange={(event) => setPhone(event.target.value)} /></label><label>WhatsApp<input type="tel" value={whatsapp} onChange={(event) => setWhatsapp(event.target.value)} /></label><label>Canal preferido<select value={preferredChannel} onChange={(event) => setPreferredChannel(event.target.value as typeof preferredChannel)}><option value="EMAIL">Correo electrónico</option><option value="PHONE">Teléfono</option><option value="WHATSAPP">WhatsApp</option></select></label><label>Consentimiento<select value={consent} onChange={(event) => setConsent(event.target.value as typeof consent)}><option value="UNKNOWN">Sin confirmar</option><option value="GRANTED">Registrado</option><option value="DENIED">No autorizado</option></select></label><button className="primary-button full-width">Agregar contacto</button></form>}
      </div></details>

      <details className="disclosure-panel"><summary>Tareas de seguimiento</summary><div className="disclosure-content">
        {tasks.length === 0 ? <EmptyState text="No hay tareas. Creá una para dejar claro el próximo paso." /> : tasks.map((task) => <article className="timeline-item" key={task.id}><strong>{task.title}</strong><span>{labelFor(task.status)} · vence {dateTime(task.dueAt)}</span>{canWriteActivity && !["COMPLETED", "CANCELLED"].includes(task.status) && <button className="secondary-button" type="button" onClick={() => void run(() => changeTaskStatus(task, "COMPLETED"), "Tarea completada.")}>Marcar como completada</button>}</article>)}
        {canWriteActivity && <form className="inline-form compact-form" onSubmit={(event) => { event.preventDefault(); void run(() => createTask(prospect.id, { ownerUserId: session.userId, title: taskTitle, dueAt: new Date(taskDueAt).toISOString() }), "Tarea creada.").then(() => { setTaskTitle(""); setTaskDueAt(""); }); }}><label className="grow">Nueva tarea<input value={taskTitle} onChange={(event) => setTaskTitle(event.target.value)} required /></label><label>Vencimiento<input type="datetime-local" value={taskDueAt} onChange={(event) => setTaskDueAt(event.target.value)} required /></label><button className="secondary-button">Crear tarea</button></form>}
      </div></details>

      <details className="disclosure-panel"><summary>Actividad y notas</summary><div className="disclosure-content">
        {canWriteActivity && <form className="inline-form compact-form" onSubmit={(event) => { event.preventDefault(); void run(() => createActivity(prospect.id, { type: activityType, summary: activitySummary }), "Actividad registrada.").then(() => setActivitySummary("")); }}><label>Tipo de actividad<select value={activityType} onChange={(event) => setActivityType(event.target.value as typeof activityType)}><option value="PHONE_CALL">Llamada</option><option value="MEETING">Reunión</option><option value="DEMO">Demostración</option><option value="EMAIL_SENT_MANUALLY">Correo enviado manualmente</option><option value="WHATSAPP_SENT_MANUALLY">WhatsApp enviado manualmente</option></select></label><label className="grow">Resumen<input value={activitySummary} onChange={(event) => setActivitySummary(event.target.value)} required /></label><button className="secondary-button">Registrar actividad</button></form>}
        {canWriteActivity && <form className="inline-form compact-form" onSubmit={(event) => { event.preventDefault(); void run(() => createNote(prospect.id, note), "Nota registrada.").then(() => setNote("")); }}><label className="grow">Nota<input value={note} onChange={(event) => setNote(event.target.value)} required /></label><button className="secondary-button">Agregar nota</button></form>}
        {timeline.length === 0 ? <EmptyState text="Todavía no hay actividad registrada." /> : timeline.map((item) => <article className="timeline-item" key={`${item.eventType}-${item.id}`}><small>{dateTime(item.eventAt)} · {labelFor(item.eventType)}</small><strong>{item.title}</strong>{item.detail && <span>{item.detail}</span>}</article>)}
      </div></details>
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
      <Control label="Estado" value={labelFor(summary.status)} />
      <Control label="Modo" value={summary.dryRun ? "Vista previa" : "Importación ejecutada"} />
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
  if (events.length === 0) return <EmptyState text="Todavía no hay eventos de auditoría." />;
  return (
    <div className="table-scroll">
      <table>
        <thead><tr><th>Fecha</th><th>Acción</th><th>Registro</th><th>Descripción</th><th>Detalle</th></tr></thead>
        <tbody>{events.map((event) => <tr key={event.id}><td>{dateTime(event.createdAt)}</td><td>{labelFor(event.action)}</td><td>{labelFor(event.entityType)}{event.entityId ? ` #${event.entityId.slice(0, 8)}` : ""}</td><td>{auditSummary(event.payload)}</td><td><details className="technical-details"><summary>Ver datos técnicos</summary><pre className="preview-box">{safeTechnicalJson(event.payload)}</pre></details></td></tr>)}</tbody>
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
      <dd>{value || "Sin información cargada"}</dd>
    </div>
  );
}

function Badge({ value }: { value: string }) {
  return <span className={`badge badge-${value.toLowerCase().replaceAll("_", "-")}`}>{labelFor(value)}</span>;
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
    dashboard: "Resumen comercial",
    prospects: "Prospectos",
    pipeline: "Oportunidades",
    campaigns: "Campañas y plantillas",
    messages: "Mensajes e integraciones",
    outbox: "Bandeja de salida",
    inbound: "Mensajes recibidos",
    imports: "Importaciones",
    exclusions: "Exclusiones de contacto",
    audit: "Auditoría",
    reports: "Reportes",
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
  return humanizeError(caught instanceof Error ? caught.message : "Ocurrió un error inesperado");
}

function dateOnly(value: string): string {
  return new Intl.DateTimeFormat("es-AR", { dateStyle: "medium" }).format(new Date(value));
}

function relativeDate(value: string): string {
  const difference = new Date(value).getTime() - Date.now();
  const days = Math.round(difference / 86_400_000);
  if (Math.abs(days) < 1) return "hoy";
  return new Intl.RelativeTimeFormat("es-AR", { numeric: "auto" }).format(days, "day");
}

async function copyText(value: string): Promise<void> {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(value);
    return;
  }
  const textarea = document.createElement("textarea");
  textarea.value = value;
  textarea.style.position = "fixed";
  textarea.style.opacity = "0";
  document.body.append(textarea);
  textarea.select();
  const copied = document.execCommand("copy");
  textarea.remove();
  if (!copied) throw new Error("Copy failed");
}

function escapeText(value: string): string {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}
