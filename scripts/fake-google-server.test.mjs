import assert from "node:assert/strict";
import test from "node:test";

import { createFakeGoogleServer } from "./fake-google-server.mjs";

const CONTROL_KEY = "synthetic-test-control";
const CLIENT_ID = "synthetic-test-client.apps.test";
const CLIENT_SECRET = "synthetic-test-client-secret";
const REDIRECT_URI = "http://127.0.0.1:18080/api/v1/sender-accounts/gmail/oauth/callback";
const SCOPE = "openid email https://www.googleapis.com/auth/gmail.send";

async function control(baseUrl, body) {
  const response = await fetch(`${baseUrl}/__fake-google__/control`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Fake-Google-Control": CONTROL_KEY,
    },
    body: JSON.stringify(body),
  });
  assert.equal(response.status, 200);
  return response.json();
}

async function authorize(baseUrl) {
  const state = "synthetic-oauth-state-".padEnd(64, "x");
  const authorizationUrl = new URL(`${baseUrl}/o/oauth2/v2/auth`);
  authorizationUrl.search = new URLSearchParams({
    access_type: "offline",
    client_id: CLIENT_ID,
    redirect_uri: REDIRECT_URI,
    response_type: "code",
    scope: SCOPE,
    state,
  });
  const page = await fetch(authorizationUrl);
  assert.equal(page.status, 200);
  assert.match(
    page.headers.get("content-security-policy"),
    new RegExp(`form-action 'self' ${new URL(REDIRECT_URI).origin.replaceAll(".", "\\.")}`),
  );
  assert.match(await page.text(), /Autorizar cuenta sintética/);

  const consent = await fetch(`${baseUrl}/authorize/consent`, {
    method: "POST",
    redirect: "manual",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ redirect_uri: REDIRECT_URI, scope: SCOPE, state }),
  });
  assert.equal(consent.status, 303);
  const callback = new URL(consent.headers.get("location"));
  assert.equal(callback.origin + callback.pathname, REDIRECT_URI);
  assert.equal(callback.searchParams.get("state"), state);
  assert.ok(callback.searchParams.get("code"));
  return callback.searchParams.get("code");
}

async function exchangeCode(baseUrl, code) {
  return fetch(`${baseUrl}/token`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      client_id: CLIENT_ID,
      client_secret: CLIENT_SECRET,
      code,
      grant_type: "authorization_code",
      redirect_uri: REDIRECT_URI,
    }),
  });
}

function syntheticMime() {
  return [
    "From: Gestudio Sintético <campaigns@gestudio.test>",
    "To: Prospecto Uno <recipient-001@example.test>",
    "Reply-To: replies@gestudio.test",
    "Subject: Campaña sintética UTF-8 áéíóú",
    "Message-ID: <synthetic-001@gestudio.test>",
    "Date: Tue, 28 Jul 2026 15:00:00 +0000",
    "MIME-Version: 1.0",
    "List-Unsubscribe: <http://127.0.0.1:18080/api/v1/unsubscribe/synthetic-token>",
    "List-Unsubscribe-Post: List-Unsubscribe=One-Click",
    'Content-Type: multipart/alternative; boundary="synthetic-boundary"',
    "",
    "--synthetic-boundary",
    'Content-Type: text/plain; charset="UTF-8"',
    "",
    "Contenido sintético. Para darse de baja use el enlace.",
    "--synthetic-boundary",
    'Content-Type: text/html; charset="UTF-8"',
    "",
    "<p>Contenido sintético.</p><p><a href=\"http://127.0.0.1/unsubscribe\">Darse de baja</a></p>",
    "--synthetic-boundary--",
    "",
  ].join("\r\n");
}

async function sendMessage(baseUrl, accessToken) {
  return fetch(`${baseUrl}/gmail/v1/users/me/messages/send`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ raw: Buffer.from(syntheticMime()).toString("base64url") }),
  });
}

test("fake Google covers OAuth offline access, refresh, Gmail send, errors and revocation locally", async (t) => {
  const fake = createFakeGoogleServer({
    host: "127.0.0.1",
    port: 0,
    clientId: CLIENT_ID,
    clientSecret: CLIENT_SECRET,
    allowedRedirectUri: REDIRECT_URI,
    controlKey: CONTROL_KEY,
    timeoutDelayMs: 2_000,
  });
  const address = await fake.start();
  const baseUrl = `http://127.0.0.1:${address.port}`;
  t.after(() => fake.close());

  const health = await fetch(`${baseUrl}/__fake-google__/health`);
  assert.deepEqual(await health.json(), { status: "UP", provider: "synthetic-google" });

  const code = await authorize(baseUrl);
  const tokenResponse = await exchangeCode(baseUrl, code);
  assert.equal(tokenResponse.status, 200);
  const tokens = await tokenResponse.json();
  assert.match(tokens.access_token, /^access_/);
  assert.match(tokens.refresh_token, /^refresh_/);
  assert.equal(tokens.token_type, "Bearer");
  assert.match(tokens.scope, /gmail\.send/);

  const replay = await exchangeCode(baseUrl, code);
  assert.equal(replay.status, 400);
  assert.equal((await replay.json()).error, "invalid_grant");

  const userinfo = await fetch(`${baseUrl}/oauth2/v3/userinfo`, {
    headers: { Authorization: `Bearer ${tokens.access_token}` },
  });
  assert.equal(userinfo.status, 200);
  assert.deepEqual(await userinfo.json(), {
    sub: "synthetic-google-user-001",
    email: "campaigns@gestudio.test",
    email_verified: true,
    name: "Gestudio Sintético",
  });

  const send = await sendMessage(baseUrl, tokens.access_token);
  assert.equal(send.status, 200);
  const providerResult = await send.json();
  assert.match(providerResult.id, /^gmail_/);
  assert.match(providerResult.threadId, /^thread_/);

  const stateResponse = await fetch(`${baseUrl}/__fake-google__/state`, {
    headers: { "X-Fake-Google-Control": CONTROL_KEY },
  });
  assert.equal(stateResponse.status, 200);
  const state = await stateResponse.json();
  assert.equal(state.acceptedMessageCount, 1);
  assert.equal(state.gmailRequestCount, 1);
  assert.equal(state.gmailRequests[0].hasOneClickUnsubscribe, true);
  assert.equal(state.acceptedMessages[0].hasPlainText, true);
  assert.equal(state.acceptedMessages[0].hasHtml, true);
  assert.equal(state.acceptedMessages[0].hasOneClickUnsubscribe, true);
  assert.equal(state.acceptedMessages[0].hasVisibleUnsubscribe, true);
  assert.equal("unsubscribeUrl" in state.acceptedMessages[0], false);

  const unsubscribeResponse = await fetch(`${baseUrl}/__fake-google__/last-unsubscribe`, {
    headers: { "X-Fake-Google-Control": CONTROL_KEY },
  });
  assert.equal(unsubscribeResponse.status, 200);
  assert.match((await unsubscribeResponse.json()).url, /\/unsubscribe\//);
  const consumedUnsubscribe = await fetch(`${baseUrl}/__fake-google__/last-unsubscribe`, {
    headers: { "X-Fake-Google-Control": CONTROL_KEY },
  });
  assert.equal(consumedUnsubscribe.status, 404);

  const refresh = await fetch(`${baseUrl}/token`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      client_id: CLIENT_ID,
      client_secret: CLIENT_SECRET,
      grant_type: "refresh_token",
      refresh_token: tokens.refresh_token,
    }),
  });
  assert.equal(refresh.status, 200);
  const refreshed = await refresh.json();
  assert.match(refreshed.access_token, /^access_/);
  assert.equal("refresh_token" in refreshed, false);

  for (const [mode, status] of [
    ["400", 400],
    ["401", 401],
    ["403", 403],
    ["429", 429],
    ["500", 500],
  ]) {
    await control(baseUrl, { operation: "gmailSend", mode, retryAfterSeconds: 7 });
    const response = await sendMessage(baseUrl, refreshed.access_token);
    assert.equal(response.status, status, mode);
    if (mode === "429") assert.equal(response.headers.get("retry-after"), "7");
  }

  await control(baseUrl, { operation: "gmailSend", mode: "malformed" });
  const malformed = await sendMessage(baseUrl, refreshed.access_token);
  assert.equal(malformed.status, 200);
  await assert.rejects(() => malformed.json(), SyntaxError);

  await control(baseUrl, { operation: "gmailSend", mode: "timeout" });
  await assert.rejects(
    () =>
      fetch(`${baseUrl}/gmail/v1/users/me/messages/send`, {
        method: "POST",
        signal: AbortSignal.timeout(100),
        headers: {
          Authorization: `Bearer ${refreshed.access_token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ raw: Buffer.from(syntheticMime()).toString("base64url") }),
      }),
    /aborted|timeout/i,
  );

  await control(baseUrl, { operation: "gmailSend", mode: "ambiguous-cut" });
  await assert.rejects(() => sendMessage(baseUrl, refreshed.access_token));

  await control(baseUrl, { operation: "gmailSend", mode: "success" });
  const revoke = await fetch(`${baseUrl}/revoke`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ token: tokens.refresh_token }),
  });
  assert.equal(revoke.status, 200);
  const refreshAfterRevoke = await fetch(`${baseUrl}/token`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      client_id: CLIENT_ID,
      client_secret: CLIENT_SECRET,
      grant_type: "refresh_token",
      refresh_token: tokens.refresh_token,
    }),
  });
  assert.equal(refreshAfterRevoke.status, 400);
  assert.equal((await refreshAfterRevoke.json()).error, "invalid_grant");
});

test("fake Google rejects redirect, client and control manipulation", async (t) => {
  const fake = createFakeGoogleServer({
    host: "127.0.0.1",
    port: 0,
    clientId: CLIENT_ID,
    clientSecret: CLIENT_SECRET,
    allowedRedirectUri: REDIRECT_URI,
    controlKey: CONTROL_KEY,
  });
  const address = await fake.start();
  const baseUrl = `http://127.0.0.1:${address.port}`;
  t.after(() => fake.close());

  const openRedirect = new URL(`${baseUrl}/o/oauth2/v2/auth`);
  openRedirect.search = new URLSearchParams({
    access_type: "offline",
    client_id: CLIENT_ID,
    redirect_uri: "https://attacker.invalid/callback",
    response_type: "code",
    scope: SCOPE,
    state: "synthetic-oauth-state-".padEnd(64, "x"),
  });
  assert.equal((await fetch(openRedirect)).status, 400);

  const invalidControl = await fetch(`${baseUrl}/__fake-google__/control`, {
    method: "POST",
    headers: { "Content-Type": "application/json", "X-Fake-Google-Control": "wrong" },
    body: JSON.stringify({ operation: "gmailSend", mode: "success" }),
  });
  assert.equal(invalidControl.status, 403);

  const badClient = await fetch(`${baseUrl}/token`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      client_id: CLIENT_ID,
      client_secret: "wrong",
      grant_type: "authorization_code",
      code: "missing",
      redirect_uri: REDIRECT_URI,
    }),
  });
  assert.equal(badClient.status, 401);
  assert.equal((await badClient.json()).error, "invalid_client");
});
