import { test } from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import { mkdtemp, writeFile, rm } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { createScalarisServer } from './scalaris-web-server.mjs';

test('servidor local: sesión, CSRF, importación, contenido y fronteras', async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), 'scalaris-server-'));
  await writeFile(path.join(root, 'index.html'), '<main>Scalaris</main>');
  await writeFile(path.join(root, 'app.js'), 'console.log("Scalaris")');
  const backend = http.createServer((request, response) => {
    let body = '';
    request.on('data', value => { body += value; });
    request.on('end', () => {
      response.setHeader('Set-Cookie', ['JSESSIONID=synthetic; HttpOnly; Path=/', 'XSRF-TOKEN=synthetic; Path=/']);
      response.setHeader('Content-Type', 'application/json');
      response.end(JSON.stringify({ url:request.url, method:request.method, cookie:request.headers.cookie, csrf:request.headers['x-xsrf-token'], type:request.headers['content-type'], forwarded:request.headers['x-forwarded-for'], body }));
    });
  });
  await new Promise(resolve => backend.listen(0, '127.0.0.1', resolve));
  const server = createScalarisServer({ root, backendPort:backend.address().port });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  try {
    assert.match(await (await fetch(`${base}/`)).text(), /Scalaris/);
    assert.match(await (await fetch(`${base}/prospeccion`)).text(), /Scalaris/);
    assert.equal((await fetch(`${base}/missing.js`)).status, 404);
    assert.equal((await fetch(`${base}/app.js`)).headers.get('content-type'), 'text/javascript; charset=utf-8');
    assert.equal((await fetch(`${base}/`, { method:'POST' })).status, 405);
    const foreignHostStatus = await new Promise((resolve,reject) => {
      const request = http.get(`${base}/`, { headers:{ Host:'outside.example' } }, response => { response.resume(); resolve(response.statusCode); });
      request.on('error', reject);
    });
    assert.equal(foreignHostStatus, 403);
    assert.equal((await fetch(`${base}/%5c..%5cconfiguracion.json`)).status, 400);
    const response = await fetch(`${base}/api/v1/imports/prospects/preview?scope=synthetic`, { method:'POST', headers:{ Cookie:'JSESSIONID=synthetic', 'X-XSRF-TOKEN':'synthetic', 'Content-Type':'multipart/form-data; boundary=synthetic', 'X-Forwarded-For':'outside' }, body:'--synthetic\r\nfixture\r\n--synthetic--' });
    const data = await response.json();
    assert.equal(data.cookie, 'JSESSIONID=synthetic');
    assert.equal(data.csrf, 'synthetic');
    assert.equal(data.forwarded, '127.0.0.1');
    assert.match(data.type, /multipart\/form-data/);
    assert.match(data.body, /fixture/);
    assert.match(data.url, /scope=synthetic/);
    assert.equal(response.headers.getSetCookie().length, 2);
    assert.equal((await fetch(`${base}/actuator/health`)).status, 200);
  } finally {
    await new Promise(resolve => server.close(resolve));
    await new Promise(resolve => backend.close(resolve));
    await rm(root, { recursive:true, force:true });
  }
});

test('servidor local: respuesta interrumpida cierra el cliente y permite solicitudes siguientes', async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), 'scalaris-server-interrupted-'));
  await writeFile(path.join(root, 'index.html'), '<main>Scalaris synthetic interruption test</main>');
  const backend = http.createServer((request, response) => {
    response.writeHead(200, { 'Content-Type': 'application/json', 'Content-Length': '100' });
    response.write('{"synthetic":');
    setTimeout(() => response.destroy(), 30);
  });
  await new Promise(resolve => backend.listen(0, '127.0.0.1', resolve));
  const server = createScalarisServer({ root, backendPort: backend.address().port });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  try {
    const response = await fetch(`${base}/api/synthetic-interruption`, { signal: AbortSignal.timeout(1000) });
    assert.equal(response.status, 200);
    await assert.rejects(response.text(), error => error.name !== 'TimeoutError', 'El proxy debe cerrar la respuesta parcial antes del timeout del cliente.');
    const next = await fetch(`${base}/`, { signal: AbortSignal.timeout(1000) });
    assert.equal(next.status, 200);
    assert.match(await next.text(), /Scalaris synthetic interruption test/);
  } finally {
    server.closeAllConnections();
    backend.closeAllConnections();
    await new Promise(resolve => server.close(resolve));
    await new Promise(resolve => backend.close(resolve));
    await rm(root, { recursive: true, force: true });
  }
});
