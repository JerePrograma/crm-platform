import { createHash, randomBytes } from "node:crypto";
import http from "node:http";
import { pathToFileURL } from "node:url";

const GMAIL_SEND_SCOPE = "https://www.googleapis.com/auth/gmail.send";
const BODY_LIMIT_BYTES = 768 * 1024;
const VALID_MODES = new Set([
  "success",
  "400",
  "401",
  "403",
  "429",
  "500",
  "malformed",
  "timeout",
  "ambiguous-cut",
]);
const CONTROL_OPERATIONS = new Set([
  "authorization",
  "tokenCode",
  "tokenRefresh",
  "userinfo",
  "revoke",
  "gmailSend",
]);

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function json(res, status, body, headers = {}) {
  const encoded = Buffer.from(JSON.stringify(body));
  res.writeHead(status, {
    "Cache-Control": "no-store",
    "Content-Type": "application/json; charset=utf-8",
    "Content-Length": encoded.length,
    ...headers,
  });
  res.end(encoded);
}

function html(res, status, body, formAction = "'self'") {
  const encoded = Buffer.from(body);
  res.writeHead(status, {
    "Cache-Control": "no-store",
    "Content-Type": "text/html; charset=utf-8",
    "Content-Length": encoded.length,
    "Content-Security-Policy": `default-src 'none'; style-src 'unsafe-inline'; form-action ${formAction}; base-uri 'none'; frame-ancestors 'none'`,
    "Referrer-Policy": "no-referrer",
    "X-Content-Type-Options": "nosniff",
  });
  res.end(encoded);
}

async function readBody(req, limit = BODY_LIMIT_BYTES) {
  const chunks = [];
  let total = 0;
  for await (const chunk of req) {
    total += chunk.length;
    if (total > limit) {
      const error = new Error("request_too_large");
      error.statusCode = 413;
      throw error;
    }
    chunks.push(chunk);
  }
  return Buffer.concat(chunks).toString("utf8");
}

function parseForm(body) {
  return Object.fromEntries(new URLSearchParams(body));
}

function sha256(value) {
  return createHash("sha256").update(value).digest("hex");
}

function randomOpaque(prefix) {
  return `${prefix}_${randomBytes(32).toString("base64url")}`;
}

function parseBearer(req) {
  const header = req.headers.authorization ?? "";
  return header.startsWith("Bearer ") ? header.slice(7) : null;
}

function decodeBase64Url(value) {
  if (typeof value !== "string" || value.length === 0 || value.length > BODY_LIMIT_BYTES) {
    throw new Error("invalid_raw_message");
  }
  if (!/^[A-Za-z0-9_-]+={0,2}$/.test(value)) {
    throw new Error("invalid_raw_message");
  }
  const decoded = Buffer.from(value, "base64url");
  if (decoded.length === 0 || decoded.length > BODY_LIMIT_BYTES) {
    throw new Error("invalid_raw_message");
  }
  return decoded.toString("utf8");
}

function headerValue(mime, name) {
  const headerSection = mime.split(/\r?\n\r?\n/, 1)[0] ?? "";
  const unfolded = headerSection.replace(/\r?\n[\t ]+/g, " ");
  const match = unfolded.match(new RegExp(`^${name}:\\s*(.+)$`, "im"));
  return match?.[1]?.trim() ?? null;
}

function summarizeMime(mime) {
  const to = headerValue(mime, "To");
  const from = headerValue(mime, "From");
  const subject = headerValue(mime, "Subject");
  const unsubscribe = headerValue(mime, "List-Unsubscribe");
  const unsubscribePost = headerValue(mime, "List-Unsubscribe-Post");
  const urlMatch = unsubscribe?.match(/<((?:https?|http):\/\/[^>]+)>/i);
  return {
    recipientHash: to ? sha256(to.toLowerCase()) : null,
    senderHash: from ? sha256(from.toLowerCase()) : null,
    subjectHash: subject ? sha256(subject) : null,
    hasMessageId: Boolean(headerValue(mime, "Message-ID")),
    hasDate: Boolean(headerValue(mime, "Date")),
    hasMimeVersion: headerValue(mime, "MIME-Version") === "1.0",
    hasPlainText: /Content-Type:\s*text\/plain\b/i.test(mime),
    hasHtml: /Content-Type:\s*text\/html\b/i.test(mime),
    hasVisibleUnsubscribe: /darse de baja|cancelar suscripci[oó]n|unsubscribe/i.test(mime),
    hasOneClickUnsubscribe:
      Boolean(unsubscribe) && unsubscribePost === "List-Unsubscribe=One-Click",
    unsubscribeUrl: urlMatch?.[1] ?? null,
  };
}

function validateMode(value) {
  if (!VALID_MODES.has(value)) {
    throw new Error(`unsupported_mode:${value}`);
  }
  return value;
}

function renderAuthorizationForm({ accountEmail, accountName, redirectUri, scope, state }) {
  return `<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Google falso — autorización sintética</title>
  <style>
    :root { color-scheme: light; font-family: Inter, system-ui, sans-serif; background: #f5f7fb; color: #14213d; }
    body { margin: 0; min-height: 100vh; display: grid; place-items: center; }
    main { width: min(520px, calc(100% - 2rem)); background: white; border: 1px solid #d9dfeb; border-radius: 18px; padding: 2rem; box-shadow: 0 24px 60px #14213d1f; }
    .badge { display: inline-flex; background: #fff1c2; color: #6d5100; border-radius: 999px; padding: .35rem .7rem; font-weight: 700; font-size: .8rem; }
    h1 { margin: 1rem 0 .5rem; font-size: 1.55rem; }
    p { line-height: 1.5; color: #4a5875; }
    dl { background: #f7f9fd; border-radius: 12px; padding: 1rem; }
    dt { font-size: .75rem; color: #687691; text-transform: uppercase; letter-spacing: .06em; }
    dd { margin: .25rem 0 1rem; font-weight: 700; }
    button { width: 100%; border: 0; border-radius: 10px; padding: .9rem 1rem; color: white; background: #2457d6; font-weight: 750; cursor: pointer; }
    small { display: block; margin-top: 1rem; color: #687691; }
  </style>
</head>
<body>
  <main>
    <span class="badge">PROVEEDOR FALSO LOCAL</span>
    <h1>Autorizar Gmail sintético</h1>
    <p>Esta pantalla pertenece al servidor de pruebas. No se conecta con Google ni utiliza credenciales reales.</p>
    <dl>
      <dt>Cuenta</dt><dd>${escapeHtml(accountName)} — ${escapeHtml(accountEmail)}</dd>
      <dt>Permiso</dt><dd>Enviar correo desde Gmail</dd>
    </dl>
    <form method="post" action="/authorize/consent">
      <input type="hidden" name="redirect_uri" value="${escapeHtml(redirectUri)}">
      <input type="hidden" name="scope" value="${escapeHtml(scope)}">
      <input type="hidden" name="state" value="${escapeHtml(state)}">
      <button type="submit">Autorizar cuenta sintética</button>
    </form>
    <small>Datos exclusivamente sintéticos · sin acceso a Internet · sin envío real</small>
  </main>
</body>
</html>`;
}

export function createFakeGoogleServer(options = {}) {
  const config = {
    host: options.host ?? process.env.FAKE_GOOGLE_HOST ?? "127.0.0.1",
    port: Number(options.port ?? process.env.FAKE_GOOGLE_PORT ?? 19090),
    clientId: options.clientId ?? process.env.FAKE_GOOGLE_CLIENT_ID ?? "synthetic-client.apps.test",
    clientSecret:
      options.clientSecret ?? process.env.FAKE_GOOGLE_CLIENT_SECRET ?? "synthetic-client-secret-not-for-production",
    allowedRedirectUri:
      options.allowedRedirectUri ??
      process.env.FAKE_GOOGLE_ALLOWED_REDIRECT_URI ??
      "http://127.0.0.1:8080/api/v1/sender-accounts/gmail/oauth/callback",
    accountEmail: options.accountEmail ?? process.env.FAKE_GOOGLE_ACCOUNT_EMAIL ?? "campaigns@gestudio.test",
    accountName: options.accountName ?? process.env.FAKE_GOOGLE_ACCOUNT_NAME ?? "Gestudio Sintético",
    controlKey: options.controlKey ?? process.env.FAKE_GOOGLE_CONTROL_KEY ?? "synthetic-local-control",
    timeoutDelayMs: Number(options.timeoutDelayMs ?? process.env.FAKE_GOOGLE_TIMEOUT_DELAY_MS ?? 15_000),
    omitRefreshOnReconnect:
      options.omitRefreshOnReconnect ??
      String(process.env.FAKE_GOOGLE_OMIT_REFRESH_ON_RECONNECT ?? "true").toLowerCase() === "true",
  };
  if (!Number.isInteger(config.port) || config.port < 0 || config.port > 65535) {
    throw new Error("FAKE_GOOGLE_PORT must be between 0 and 65535");
  }
  const allowedRedirect = new URL(config.allowedRedirectUri);
  if (!new Set(["http:", "https:"]).has(allowedRedirect.protocol)) {
    throw new Error("FAKE_GOOGLE_ALLOWED_REDIRECT_URI must use HTTP or HTTPS");
  }

  const modes = Object.fromEntries([...CONTROL_OPERATIONS].map((name) => [name, "success"]));
  const authorizationCodes = new Map();
  const accessTokens = new Map();
  const refreshTokens = new Map();
  const acceptedMessages = [];
  const observedRequests = [];
  const pendingTimers = new Set();
  let authorizationCount = 0;
  let lastUnsubscribeUrl = null;
  let retryAfterSeconds = 2;

  function requireControl(req, res) {
    if (req.headers["x-fake-google-control"] !== config.controlKey) {
      json(res, 403, { error: "forbidden" });
      return false;
    }
    return true;
  }

  function applyMode(operation, req, res) {
    const mode = modes[operation];
    if (mode === "success") return false;
    if (mode === "timeout") {
      const timer = setTimeout(() => {
        pendingTimers.delete(timer);
        if (!res.destroyed) json(res, 504, { error: "synthetic_timeout" });
      }, Math.max(100, Math.min(config.timeoutDelayMs, 30_000)));
      pendingTimers.add(timer);
      req.once("close", () => {
        clearTimeout(timer);
        pendingTimers.delete(timer);
      });
      return true;
    }
    if (mode === "malformed") {
      res.writeHead(200, {
        "Cache-Control": "no-store",
        "Content-Type": "application/json; charset=utf-8",
      });
      res.end('{"synthetic":');
      return true;
    }
    if (mode === "ambiguous-cut") {
      res.writeHead(200, {
        "Cache-Control": "no-store",
        "Content-Type": "application/json; charset=utf-8",
        "Content-Length": "1024",
      });
      res.write('{"id":"synthetic-possibly-accepted"');
      res.socket?.destroy();
      return true;
    }
    const status = Number(mode);
    const headers = status === 429 ? { "Retry-After": String(retryAfterSeconds) } : {};
    const error =
      status === 401
        ? "invalid_token"
        : status === 403
          ? "insufficient_permissions"
          : status === 429
            ? "rate_limit_exceeded"
            : status >= 500
              ? "provider_unavailable"
              : "invalid_request";
    json(res, status, { error, operation }, headers);
    return true;
  }

  function reset() {
    for (const operation of CONTROL_OPERATIONS) modes[operation] = "success";
    authorizationCodes.clear();
    accessTokens.clear();
    refreshTokens.clear();
    acceptedMessages.splice(0);
    observedRequests.splice(0);
    authorizationCount = 0;
    lastUnsubscribeUrl = null;
    retryAfterSeconds = 2;
  }

  const server = http.createServer(async (req, res) => {
    const authority = req.headers.host ?? `127.0.0.1:${config.port}`;
    const url = new URL(req.url ?? "/", `http://${authority}`);
    try {
      if (req.method === "GET" && url.pathname === "/__fake-google__/health") {
        json(res, 200, { status: "UP", provider: "synthetic-google" });
        return;
      }

      if (req.method === "GET" && url.pathname === "/__fake-google__/state") {
        if (!requireControl(req, res)) return;
        json(res, 200, {
          authorizationCount,
          activeAccessTokens: accessTokens.size,
          activeRefreshTokens: [...refreshTokens.values()].filter((entry) => !entry.revoked).length,
          acceptedMessageCount: acceptedMessages.length,
          acceptedMessages: acceptedMessages.map(({ unsubscribeUrl: _unused, ...entry }) => entry),
          gmailRequestCount: observedRequests.length,
          gmailRequests: observedRequests.map((entry) => ({ ...entry })),
          modes: { ...modes },
        });
        return;
      }

      if (req.method === "GET" && url.pathname === "/__fake-google__/last-unsubscribe") {
        if (!requireControl(req, res)) return;
        const value = lastUnsubscribeUrl;
        lastUnsubscribeUrl = null;
        json(res, value ? 200 : 404, value ? { url: value } : { error: "not_available" });
        return;
      }

      if (req.method === "POST" && url.pathname === "/__fake-google__/reset") {
        if (!requireControl(req, res)) return;
        reset();
        json(res, 200, { reset: true });
        return;
      }

      if (req.method === "POST" && url.pathname === "/__fake-google__/control") {
        if (!requireControl(req, res)) return;
        const body = JSON.parse(await readBody(req, 16 * 1024));
        if (body.modes && typeof body.modes === "object") {
          for (const [operation, mode] of Object.entries(body.modes)) {
            if (!CONTROL_OPERATIONS.has(operation)) throw new Error(`unsupported_operation:${operation}`);
            modes[operation] = validateMode(mode);
          }
        } else {
          if (!CONTROL_OPERATIONS.has(body.operation)) throw new Error(`unsupported_operation:${body.operation}`);
          modes[body.operation] = validateMode(body.mode);
        }
        if (body.retryAfterSeconds !== undefined) {
          const parsed = Number(body.retryAfterSeconds);
          if (!Number.isInteger(parsed) || parsed < 1 || parsed > 3600) throw new Error("invalid_retry_after");
          retryAfterSeconds = parsed;
        }
        json(res, 200, { modes: { ...modes }, retryAfterSeconds });
        return;
      }

      if (req.method === "GET" && url.pathname === "/o/oauth2/v2/auth") {
        if (applyMode("authorization", req, res)) return;
        const redirectUri = url.searchParams.get("redirect_uri") ?? "";
        const state = url.searchParams.get("state") ?? "";
        const scope = url.searchParams.get("scope") ?? "";
        const scopes = new Set(scope.split(/\s+/).filter(Boolean));
        const valid =
          url.searchParams.get("client_id") === config.clientId &&
          url.searchParams.get("response_type") === "code" &&
          url.searchParams.get("access_type") === "offline" &&
          redirectUri === config.allowedRedirectUri &&
          state.length >= 32 &&
          state.length <= 2048 &&
          scopes.has("openid") &&
          scopes.has("email") &&
          scopes.has(GMAIL_SEND_SCOPE);
        if (!valid) {
          html(res, 400, "<!doctype html><html lang=\"es\"><title>Solicitud OAuth inválida</title><p>La solicitud OAuth sintética no es válida.</p></html>");
          return;
        }
        html(
          res,
          200,
          renderAuthorizationForm({
            accountEmail: config.accountEmail,
            accountName: config.accountName,
            redirectUri,
            scope,
            state,
          }),
          `'self' ${allowedRedirect.origin}`,
        );
        return;
      }

      if (req.method === "POST" && url.pathname === "/authorize/consent") {
        if (applyMode("authorization", req, res)) return;
        const form = parseForm(await readBody(req, 32 * 1024));
        const scopes = new Set((form.scope ?? "").split(/\s+/).filter(Boolean));
        if (
          form.redirect_uri !== config.allowedRedirectUri ||
          typeof form.state !== "string" ||
          form.state.length < 32 ||
          form.state.length > 2048 ||
          !scopes.has(GMAIL_SEND_SCOPE)
        ) {
          json(res, 400, { error: "invalid_authorization_request" });
          return;
        }
        const code = randomOpaque("code");
        authorizationCodes.set(code, {
          redirectUri: form.redirect_uri,
          scope: form.scope,
          used: false,
        });
        authorizationCount += 1;
        const redirect = new URL(form.redirect_uri);
        redirect.searchParams.set("code", code);
        redirect.searchParams.set("state", form.state);
        res.writeHead(303, {
          "Cache-Control": "no-store",
          Location: redirect.toString(),
          "Referrer-Policy": "no-referrer",
        });
        res.end();
        return;
      }

      if (req.method === "POST" && url.pathname === "/token") {
        const form = parseForm(await readBody(req, 64 * 1024));
        if (form.client_id !== config.clientId || form.client_secret !== config.clientSecret) {
          json(res, 401, { error: "invalid_client" });
          return;
        }
        if (form.grant_type === "authorization_code") {
          if (applyMode("tokenCode", req, res)) return;
          const record = authorizationCodes.get(form.code);
          if (!record || record.used || form.redirect_uri !== record.redirectUri) {
            json(res, 400, { error: "invalid_grant" });
            return;
          }
          record.used = true;
          const accessToken = randomOpaque("access");
          accessTokens.set(accessToken, { accountEmail: config.accountEmail, revoked: false });
          const tokenResponse = {
            access_token: accessToken,
            expires_in: 3600,
            scope: record.scope,
            token_type: "Bearer",
          };
          if (!config.omitRefreshOnReconnect || refreshTokens.size === 0) {
            const refreshToken = randomOpaque("refresh");
            refreshTokens.set(refreshToken, { accountEmail: config.accountEmail, revoked: false });
            tokenResponse.refresh_token = refreshToken;
          }
          json(res, 200, tokenResponse);
          return;
        }
        if (form.grant_type === "refresh_token") {
          if (applyMode("tokenRefresh", req, res)) return;
          const record = refreshTokens.get(form.refresh_token);
          if (!record || record.revoked) {
            json(res, 400, { error: "invalid_grant" });
            return;
          }
          const accessToken = randomOpaque("access");
          accessTokens.set(accessToken, { accountEmail: record.accountEmail, revoked: false });
          json(res, 200, {
            access_token: accessToken,
            expires_in: 3600,
            scope: `openid email ${GMAIL_SEND_SCOPE}`,
            token_type: "Bearer",
          });
          return;
        }
        json(res, 400, { error: "unsupported_grant_type" });
        return;
      }

      if (req.method === "GET" && url.pathname === "/oauth2/v3/userinfo") {
        if (applyMode("userinfo", req, res)) return;
        const token = parseBearer(req);
        const record = token ? accessTokens.get(token) : null;
        if (!record || record.revoked) {
          json(res, 401, { error: "invalid_token" });
          return;
        }
        json(res, 200, {
          sub: "synthetic-google-user-001",
          email: record.accountEmail,
          email_verified: true,
          name: config.accountName,
        });
        return;
      }

      if (req.method === "POST" && url.pathname === "/revoke") {
        if (applyMode("revoke", req, res)) return;
        const form = parseForm(await readBody(req, 32 * 1024));
        if (refreshTokens.has(form.token)) refreshTokens.get(form.token).revoked = true;
        if (accessTokens.has(form.token)) accessTokens.get(form.token).revoked = true;
        res.writeHead(200, { "Cache-Control": "no-store", "Content-Length": "0" });
        res.end();
        return;
      }

      if (req.method === "POST" && url.pathname === "/gmail/v1/users/me/messages/send") {
        const token = parseBearer(req);
        const tokenRecord = token ? accessTokens.get(token) : null;
        if (!tokenRecord || tokenRecord.revoked) {
          json(res, 401, { error: "invalid_token" });
          return;
        }
        const body = JSON.parse(await readBody(req));
        let mime;
        try {
          mime = decodeBase64Url(body.raw);
        } catch {
          json(res, 400, { error: "invalid_raw_message" });
          return;
        }
        const summary = summarizeMime(mime);
        observedRequests.push({
          observedAt: new Date().toISOString(),
          recipientHash: summary.recipientHash,
          senderHash: summary.senderHash,
          hasPlainText: summary.hasPlainText,
          hasHtml: summary.hasHtml,
          hasOneClickUnsubscribe: summary.hasOneClickUnsubscribe,
        });
        if (modes.gmailSend === "ambiguous-cut") {
          acceptedMessages.push({
            providerMessageId: randomOpaque("gmail"),
            acceptedAt: new Date().toISOString(),
            ...summary,
            unsubscribeUrl: undefined,
          });
          lastUnsubscribeUrl = summary.unsubscribeUrl;
          applyMode("gmailSend", req, res);
          return;
        }
        if (applyMode("gmailSend", req, res)) return;
        const providerMessageId = randomOpaque("gmail");
        const threadId = randomOpaque("thread");
        acceptedMessages.push({
          providerMessageId,
          threadId,
          acceptedAt: new Date().toISOString(),
          ...summary,
          unsubscribeUrl: undefined,
        });
        lastUnsubscribeUrl = summary.unsubscribeUrl;
        json(res, 200, { id: providerMessageId, threadId, labelIds: ["SENT"] });
        return;
      }

      json(res, 404, { error: "not_found" });
    } catch (error) {
      const status = Number(error?.statusCode) || 400;
      json(res, status, { error: status === 413 ? "request_too_large" : "invalid_test_request" });
    }
  });

  return {
    config: {
      host: config.host,
      port: config.port,
      allowedRedirectUri: config.allowedRedirectUri,
      accountEmail: config.accountEmail,
    },
    server,
    async start() {
      if (server.listening) return server.address();
      await new Promise((resolve, reject) => {
        server.once("error", reject);
        server.listen(config.port, config.host, () => {
          server.off("error", reject);
          resolve();
        });
      });
      return server.address();
    },
    async close() {
      for (const timer of pendingTimers) clearTimeout(timer);
      pendingTimers.clear();
      if (!server.listening) return;
      await new Promise((resolve, reject) => server.close((error) => (error ? reject(error) : resolve())));
    },
  };
}

async function runCli() {
  const fake = createFakeGoogleServer();
  const address = await fake.start();
  const displayedHost = typeof address === "object" && address ? address.address : fake.config.host;
  const displayedPort = typeof address === "object" && address ? address.port : fake.config.port;
  process.stdout.write(`Synthetic Google server listening on http://${displayedHost}:${displayedPort}\n`);
  const shutdown = async () => {
    await fake.close();
    process.exit(0);
  };
  process.once("SIGINT", shutdown);
  process.once("SIGTERM", shutdown);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  runCli().catch((error) => {
    process.stderr.write(`Synthetic Google server failed: ${error.message}\n`);
    process.exitCode = 1;
  });
}
