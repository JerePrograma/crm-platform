import { useCallback, useEffect, useState } from "react";
import {
  getSenderAccountConfiguration,
  listSenderAccounts,
  reconnectSenderAccount,
  revokeSenderAccount,
  setDefaultSenderAccount,
  startGmailOAuth,
  verifySenderAccount,
} from "./api";
import { openDecisionDialog } from "./decisionDialog";
import { safeAuthorizationUrl, type GmailCallbackNotice } from "./gmailCampaignUi";
import type { MessagingSafety, SenderAccount, SenderAccountConfiguration, SessionUser } from "./types";
import { humanizeError, labelFor } from "./uiLabels";

type Props = {
  session: SessionUser;
  safety: MessagingSafety | null;
  callbackNotice: GmailCallbackNotice | null;
  onChanged: () => Promise<void>;
};

export function GmailSenderAccountsPanel({ session, safety, callbackNotice, onChanged }: Props) {
  const [accounts, setAccounts] = useState<SenderAccount[]>([]);
  const [configuration, setConfiguration] = useState<SenderAccountConfiguration | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const canManage = session.permissions.includes("SETTINGS_MANAGE");

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [items, currentConfiguration] = await Promise.all([
        listSenderAccounts(),
        getSenderAccountConfiguration(),
      ]);
      setAccounts(items);
      setConfiguration(currentConfiguration);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  async function run(action: () => Promise<unknown>, success: string) {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await action();
      setNotice(success);
      await Promise.all([refresh(), onChanged()]);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setBusy(false);
    }
  }

  async function navigateToGoogle(action: () => Promise<{ authorizationUrl: string }>) {
    setBusy(true);
    setError(null);
    try {
      const started = await action();
      window.location.assign(safeAuthorizationUrl(started.authorizationUrl));
    } catch (caught) {
      setError(errorMessage(caught));
      setBusy(false);
    }
  }

  async function revoke(account: SenderAccount) {
    const answer = await openDecisionDialog({
      title: "Revocar cuenta remitente",
      description: `Se bloquearán nuevos envíos desde ${account.emailAddress}. La revocación local se aplicará aunque Google no esté disponible.`,
      confirmLabel: "Revocar",
      danger: true,
    });
    if (!answer) return;
    await run(() => revokeSenderAccount(account), "Cuenta remitente revocada.");
  }

  return (
    <section className="panel" aria-labelledby="sender-accounts-title">
      <div className="panel-heading">
        <div>
          <h2 id="sender-accounts-title">Cuentas remitentes</h2>
          <p className="muted">Google OAuth se completa en el backend. El navegador nunca recibe tokens.</p>
        </div>
        {canManage && (
          <button
            className="primary-button"
            type="button"
            disabled={busy || !configuration?.oauthConfigured}
            onClick={() => void navigateToGoogle(startGmailOAuth)}
          >
            Conectar cuenta de Google
          </button>
        )}
      </div>
      {callbackNotice && (
        <div className={`alert ${callbackNotice.kind}`} role={callbackNotice.kind === "error" ? "alert" : "status"}>
          {callbackNotice.message}
        </div>
      )}
      {error && <div className="alert error" role="alert">{error}</div>}
      {notice && <div className="alert success" role="status">{notice}</div>}
      {loading && <div className="loading-bar" aria-label="Cargando cuentas remitentes" />}
      {configuration && !configuration.oauthConfigured && (
        <div className="alert safety" role="status">
          OAuth de Google no está configurado en el servidor. Los borradores y simulaciones siguen disponibles.
        </div>
      )}
      {configuration && (
        <div className="control-grid sender-safety-grid">
          <Control label="Proveedor" value={labelFor(configuration.providerMode)} />
          <Control label="Red real" value={configuration.realNetworkAllowed ? "Habilitada" : "Bloqueada"} />
          <Control label="Scope requerido" value={configuration.requiredScope} />
          <Control label="Envíos reales" value={safety?.sendingEnabled ? "Habilitados" : "Bloqueados"} />
          <Control label="Modo de simulación" value={safety?.dryRun === false ? "Inactivo" : "Activo"} />
          <Control label="Protección de emergencia" value={safety?.killSwitch === false ? "Inactiva" : "Activa"} />
          <Control label="Límite máximo diario" value={String(safety?.hardDailyLimit ?? 0)} />
        </div>
      )}
      {!loading && accounts.length === 0 ? (
        <p className="empty-state">No hay cuentas remitentes conectadas.</p>
      ) : (
        <div className="card-grid sender-account-grid">
          {accounts.map((account) => (
            <article className="entity-card" key={account.id}>
              <div>
                <strong>{account.displayName || account.emailAddress}</strong>
                <span className={`badge badge-${account.status.toLowerCase().replaceAll("_", "-")}`}>
                  {labelFor(account.status)}
                </span>
              </div>
              <dl className="detail-grid compact-details">
                <Detail label="Correo" value={account.emailAddress} />
                <Detail label="Proveedor" value={labelFor(account.provider)} />
                <Detail label="Scopes" value={account.grantedScopes.join(", ") || "Sin scopes informados"} />
                <Detail label="Conectada" value={formatDate(account.connectedAt)} />
                <Detail label="Última verificación" value={formatDate(account.verifiedAt)} />
                <Detail label="Predeterminada" value={account.defaultAccount ? "Sí" : "No"} />
                <Detail label="Límite diario" value={String(account.dailyLimit)} />
                <Detail label="Intervalo mínimo" value={`${account.minIntervalSeconds} segundos`} />
                <Detail label="Próximo envío" value={formatDate(account.nextSendAt)} />
              </dl>
              {account.lastErrorSummary && <div className="inline-error" role="status">{account.lastErrorSummary}</div>}
              {canManage && (
                <div className="action-row">
                  <button
                    className="secondary-button"
                    type="button"
                    disabled={busy || account.status === "REVOKED"}
                    onClick={() => void run(() => verifySenderAccount(account), "Cuenta verificada.")}
                  >
                    Verificar
                  </button>
                  {!account.defaultAccount && account.status === "CONNECTED" && (
                    <button
                      className="secondary-button"
                      type="button"
                      disabled={busy}
                      onClick={() => void run(() => setDefaultSenderAccount(account), "Cuenta establecida como predeterminada.")}
                    >
                      Establecer como predeterminada
                    </button>
                  )}
                  <button
                    className="secondary-button"
                    type="button"
                    disabled={busy || !configuration?.oauthConfigured}
                    onClick={() => void navigateToGoogle(() => reconnectSenderAccount(account))}
                  >
                    Reconectar
                  </button>
                  {account.status !== "REVOKED" && (
                    <button className="danger-button" type="button" disabled={busy} onClick={() => void revoke(account)}>
                      Revocar
                    </button>
                  )}
                </div>
              )}
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function Control({ label, value }: { label: string; value: string }) {
  return <div className="control-card"><span>{label}</span><strong>{value}</strong></div>;
}

function Detail({ label, value }: { label: string; value: string }) {
  return <div><dt>{label}</dt><dd>{value}</dd></div>;
}

function formatDate(value: string | null): string {
  return value ? new Intl.DateTimeFormat("es-AR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)) : "Sin información";
}

function errorMessage(caught: unknown): string {
  return humanizeError(caught instanceof Error ? caught.message : "No se pudo completar la acción.");
}
