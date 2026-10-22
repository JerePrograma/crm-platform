import { useEffect, useState } from "react";
import { getTaskPage } from "./api";
import { humanizeError, labelFor } from "./uiLabels";
import type { Page, Task, TaskFilters } from "./types";

export function useTaskPage(filters: TaskFilters, refreshKey: number) {
  const query = JSON.stringify(filters);
  const [page, setPage] = useState<Page<Task> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setPage(null);
    setError(null);
    void getTaskPage(JSON.parse(query) as TaskFilters)
      .then((result) => { if (active) setPage(result); })
      .catch((caught) => { if (active) setError(humanizeError(caught instanceof Error ? caught.message : String(caught))); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [query, refreshKey, retryKey]);

  return { page, loading, error, retry: () => setRetryKey((key) => key + 1) };
}

export function TaskPagination({ page, onPage, label }: {
  page: Page<Task>;
  onPage: (number: number) => void;
  label: string;
}) {
  if (!page.totalElements) return null;
  return <nav className="pagination" aria-label={label}>
    <span>Mostrando {page.number * page.size + 1}–{page.number * page.size + page.content.length} de {page.totalElements} tareas</span>
    {page.totalPages > 1 && <div className="action-row">
      <button type="button" className="secondary-button" disabled={page.first} onClick={() => onPage(page.number - 1)}>Anterior</button>
      <span>Página {page.number + 1} de {page.totalPages}</span>
      <button type="button" className="secondary-button" disabled={page.last} onClick={() => onPage(page.number + 1)}>Siguiente</button>
    </div>}
  </nav>;
}

export function ProspectTaskList({ prospectId, refreshKey, busy, onComplete }: {
  prospectId: string;
  refreshKey: number;
  busy: boolean;
  onComplete?: (task: Task) => Promise<unknown>;
}) {
  const [number, setNumber] = useState(0);
  const { page, loading, error, retry } = useTaskPage({ prospectId, page: number, size: 25 }, refreshKey);
  return <>
    {loading && <p className="muted" role="status">Cargando tareas…</p>}
    {error && <div className="alert error" role="alert">{error} <button type="button" className="link-button" onClick={retry}>Reintentar</button></div>}
    {page && (page.content.length ? page.content.map((task) => <article className="timeline-item" key={task.id}>
      <strong>{task.title}</strong>
      <span>{labelFor(task.status)} · vence {new Date(task.dueAt).toLocaleString("es-AR", { timeZone: "America/Argentina/Buenos_Aires", dateStyle: "short", timeStyle: "short" })}</span>
      {onComplete && !["COMPLETED", "CANCELLED"].includes(task.status) && <button className="secondary-button" type="button" disabled={busy} onClick={() => void onComplete(task)}>Marcar como completada</button>}
    </article>) : <div className="empty-state">No hay tareas. Creá una para dejar claro el próximo paso.</div>)}
    {page && <TaskPagination page={page} onPage={setNumber} label="Páginas de tareas del prospecto" />}
  </>;
}
