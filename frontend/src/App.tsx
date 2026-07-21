import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type FormEvent,
} from "react";
import {
  changePassword,
  createExclusion,
  createUser,
  getImportRows,
  getPendingDuplicateReviews,
  getProspect,
  getSession,
  importProspects,
  listAuditEvents,
  listExclusions,
  listProspects,
  listUsers,
  login,
  logout,
  setUserActive,
} from "./api";
import type {
  AuditEvent,
  DuplicateReview,
  Exclusion,
  ImportRow,
  ImportSummary,
  Prospect,
  ProspectStatus,
  SessionUser,
  User,
} from "./types";

type Tab =
  | "dashboard"
  | "prospects"
  | "imports"
  | "exclusions"
  | "audit"
  | "users"
  | "account";

const prospectStatuses: ProspectStatus[] = [
  "NEW",
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
  const [selectedProspect, setSelectedProspect] = useState<Prospect | null>(null);
  const [statusFilter, setStatusFilter] = useState<ProspectStatus | "">("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(
    async (filter: ProspectStatus | "" = statusFilter) => {
      setLoading(true);
      setError(null);
      try {
        const [prospectPage, exclusionPage, audits, reviews] = await Promise.all([
          listProspects(filter || undefined),
          listExclusions(),
          listAuditEvents(),
          getPendingDuplicateReviews(),
        ]);
        setProspects(prospectPage.content);
        setExclusions(exclusionPage.content);
        setAuditEvents(audits);
        setDuplicateReviews(reviews);
      } catch (caught) {
        setError(message(caught));
      } finally {
        setLoading(false);
      }
    },
    [statusFilter],
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
    await refresh(value);
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
          <NavButton active={tab === "imports"} onClick={() => setTab("imports")}>
            Importaciones
          </NavButton>
          <NavButton active={tab === "exclusions"} onClick={() => setTab("exclusions")}>
            Exclusiones
          </NavButton>
          <NavButton active={tab === "audit"} onClick={() => setTab("audit")}>
            Auditoría
          </NavButton>
          {session.permissions.includes("USER_MANAGE") && (
            <NavButton active={tab === "users"} onClick={() => setTab("users")}>
              Usuarios
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
            onClick={() => void refresh()}
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
            </div>
            <Panel title="Controles activos">
              <div className="control-grid">
                <Control label="Aprobación de campañas" value="Obligatoria" />
                <Control label="Importación" value="Preview + confirmación" />
                <Control label="Duplicados ambiguos" value="Revisión humana" />
                <Control label="Sesión" value="Cookie HttpOnly + CSRF" />
              </div>
            </Panel>
            <Panel title="Actividad reciente">
              <AuditTable events={auditEvents.slice(0, 8)} />
            </Panel>
          </section>
        )}

        {tab === "prospects" && (
          <section className="two-column">
            <Panel title="Prospectos">
              <div className="toolbar">
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
                        <td>{prospect.institutionName}</td>
                        <td>{prospect.locality ?? "—"}</td>
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
                <ProspectDetail prospect={selectedProspect} />
              ) : (
                <EmptyState text="Seleccioná un prospecto para ver su ficha." />
              )}
            </Panel>
          </section>
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
            onChanged={() => refresh()}
          />
        )}

        {tab === "audit" && (
          <Panel title="Eventos recientes">
            <AuditTable events={auditEvents} />
          </Panel>
        )}

        {tab === "users" && <UsersPanel currentUser={session} />}
        {tab === "account" && (
          <AccountPanel session={session} onPasswordChanged={() => setSession(null)} />
        )}
      </main>
    </div>
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
          <input value={username} onChange={(event) => setUsername(event.target.value)} required />
        </label>
        <label>
          Contraseña
          <input
            type="password"
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
                  <th>Tipo</th>
                  <th>Confianza</th>
                  <th>Nota</th>
                </tr>
              </thead>
              <tbody>
                {duplicateReviews.map((review) => (
                  <tr key={review.id}>
                    <td>
                      {review.sourceSheet}/{review.rowNumber}
                    </td>
                    <td>{review.matchType}</td>
                    <td>{Math.round(review.confidence * 100)}%</td>
                    <td>{review.notes ?? "—"}</td>
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
  onChanged,
}: {
  exclusions: Exclusion[];
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
        <form className="inline-form" onSubmit={(event) => void submit(event)}>
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
        </form>
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

function ProspectDetail({ prospect }: { prospect: Prospect }) {
  return (
    <dl className="detail-grid">
      <Detail label="Institución" value={prospect.institutionName} />
      <Detail label="Categoría" value={prospect.category} />
      <Detail label="Ubicación" value={[prospect.locality, prospect.province].filter(Boolean).join(", ")} />
      <Detail label="País" value={prospect.country} />
      <Detail label="Sitio" value={prospect.website} />
      <Detail label="Estado" value={prospect.status} />
      <Detail label="Elegible" value={prospect.contactEligible ? "Sí" : "No"} />
      <Detail label="Prioridad" value={prospect.priority?.toString()} />
      <Detail label="Puntuación" value={prospect.score?.toString()} />
      <Detail label="Alumnos estimados" value={prospect.estimatedStudents?.toString()} />
      <Detail label="Fuente" value={prospect.source} />
      <Detail label="Propietario" value={prospect.owner} />
      <Detail label="Actualizado" value={dateTime(prospect.updatedAt)} />
    </dl>
  );
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

function Metric({ label, value }: { label: string; value: number }) {
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
    imports: "Importaciones",
    exclusions: "Exclusiones",
    audit: "Auditoría",
    users: "Usuarios",
    account: "Mi cuenta",
  }[tab];
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
