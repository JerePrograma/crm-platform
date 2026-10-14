import { useCallback, useEffect, useState } from "react";
import { assignTag, listProspectTags, listTags, unassignTag } from "./api";
import { humanizeError } from "./uiLabels";
import type { CrmTag } from "./types";

export function ProspectTagsPanel({ prospectId, canWrite }: { prospectId: string; canWrite: boolean }) {
  const [tags, setTags] = useState<CrmTag[]>([]);
  const [assigned, setAssigned] = useState<CrmTag[]>([]);
  const [tagId, setTagId] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const load = useCallback(async () => {
    const [catalog, current] = await Promise.all([listTags(), listProspectTags(prospectId)]);
    setTags(catalog); setAssigned(current);
  }, [prospectId]);
  useEffect(() => {
    setError(null); setNotice(null); setTagId("");
    void load().catch((caught) => setError(humanizeError(caught instanceof Error ? caught.message : String(caught))));
  }, [load]);

  async function change(id: string, remove = false) {
    setBusy(true); setError(null); setNotice(null);
    try {
      if (remove) await unassignTag(id, prospectId); else await assignTag(id, prospectId);
      await load(); setTagId(""); setNotice(remove ? "Etiqueta quitada." : "Etiqueta asignada.");
    } catch (caught) { setError(humanizeError(caught instanceof Error ? caught.message : String(caught))); }
    finally { setBusy(false); }
  }

  const available = tags.filter((tag) => tag.active && !assigned.some((current) => current.id === tag.id));
  return <details className="disclosure-panel" open><summary>Servicio, público y zona</summary><div className="disclosure-content">
    <p className="muted">Clasificá el interés en Servicio técnico, Desarrollo o Venta a pedido. Las etiquetas también permiten distinguir particulares, comercios, empresas y zonas de cobertura.</p>
    {error && <p className="alert error" role="alert">{error}</p>}{notice && <p className="alert success" role="status">{notice}</p>}
    <div className="assigned-tags">{assigned.map((tag) => <span className="assigned-tag" key={tag.id}><span className="tag-swatch" style={{ background: tag.color }} aria-hidden="true" />{tag.name}{canWrite && <button disabled={busy} type="button" className="tag-remove" aria-label={`Quitar etiqueta ${tag.name}`} onClick={() => void change(tag.id, true)}>×</button>}</span>)}</div>
    {!assigned.length && <p className="muted">Todavía no se clasificó este prospecto.</p>}
    {canWrite && <form className="inline-form" onSubmit={(event) => { event.preventDefault(); if (tagId) void change(tagId); }}><label className="grow">Agregar etiqueta<select value={tagId} onChange={(event) => setTagId(event.target.value)}><option value="">Elegí servicio, público o zona…</option>{available.map((tag) => <option value={tag.id} key={tag.id}>{tag.name}</option>)}</select></label><button className="secondary-button" disabled={busy || !tagId}>{busy ? "Guardando…" : "Asignar etiqueta"}</button></form>}
    {!tags.length && <p className="muted">El administrador puede completar el catálogo desde Configuración.</p>}
  </div></details>;
}
