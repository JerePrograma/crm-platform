import http from 'node:http';
import { createReadStream } from 'node:fs';
import { stat } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

export function createScalarisServer({ root, backendPort }) {
  const directory = path.resolve(root);
  const types = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8', '.json': 'application/json', '.svg': 'image/svg+xml', '.png': 'image/png', '.ico': 'image/x-icon', '.woff2': 'font/woff2', '.csv': 'text/csv; charset=utf-8' };
  return http.createServer(async (request, response) => {
    if (!/^(127\.0\.0\.1|localhost):\d+$/.test(request.headers.host ?? '')) { response.writeHead(403).end(); return; }
    response.setHeader('X-Content-Type-Options', 'nosniff');
    response.setHeader('X-Frame-Options', 'DENY');
    response.setHeader('Referrer-Policy', 'same-origin');
    response.setHeader('Content-Security-Policy', "default-src 'self'; img-src 'self' data:; style-src 'self'; frame-ancestors 'none'; object-src 'none'");
    let pathname;
    try { pathname = decodeURIComponent(new URL(request.url, 'http://localhost').pathname); }
    catch { response.writeHead(400).end(); return; }
    if (pathname.startsWith('/api/') || pathname.startsWith('/actuator/')) {
      const headers = { ...request.headers, host: `127.0.0.1:${backendPort}` };
      delete headers['x-forwarded-for'];
      delete headers['x-forwarded-host'];
      delete headers['x-forwarded-proto'];
      headers['x-forwarded-for'] = '127.0.0.1';
      headers['x-forwarded-host'] = request.headers.host;
      headers['x-forwarded-proto'] = 'http';
      const upstream = http.request({ hostname: '127.0.0.1', port: backendPort, path: request.url, method: request.method, headers, timeout: 120_000 }, incoming => {
        response.writeHead(incoming.statusCode ?? 502, incoming.headers);
        const closeInterruptedResponse = () => response.destroy();
        incoming.on('aborted', closeInterruptedResponse);
        incoming.on('error', closeInterruptedResponse);
        incoming.on('close', () => { if (!incoming.complete) closeInterruptedResponse(); });
        incoming.pipe(response);
      });
      upstream.on('timeout', () => upstream.destroy());
      upstream.on('error', () => {
        if (response.headersSent) { response.destroy(); return; }
        response.writeHead(502, { 'Content-Type': 'application/json' });
        response.end('{"detail":"El CRM está iniciando. Volvé a intentarlo en unos segundos."}');
      });
      request.on('aborted', () => upstream.destroy());
      response.on('close', () => { if (!response.writableEnded) upstream.destroy(); });
      request.pipe(upstream);
      return;
    }
    if (!['GET', 'HEAD'].includes(request.method)) { response.writeHead(405).end(); return; }
    if (pathname.includes('\\') || pathname.includes('\0')) { response.writeHead(400).end(); return; }
    const filename = path.resolve(directory, `.${pathname}`);
    const relative = path.relative(directory, filename);
    if (relative.startsWith('..') || path.isAbsolute(relative)) { response.writeHead(403).end(); return; }
    try {
      let target = filename;
      let info = await stat(target).catch(() => null);
      if (!info?.isFile()) {
        if (path.extname(pathname)) { response.writeHead(404).end(); return; }
        target = path.join(directory, 'index.html');
        info = await stat(target);
      }
      response.writeHead(200, { 'Content-Type': types[path.extname(target)] ?? 'application/octet-stream', 'Content-Length': info.size, 'Cache-Control': 'no-store' });
      if (request.method === 'HEAD') response.end();
      else createReadStream(target).on('error', () => response.destroy()).pipe(response);
    } catch { response.writeHead(404).end(); }
  });
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const port = Number(process.env.SCALARIS_WEB_PORT);
  const backendPort = Number(process.env.SCALARIS_BACKEND_PORT);
  if (![port, backendPort].every(value => Number.isInteger(value) && value > 1024 && value < 65536)) throw new Error('Puertos locales inválidos.');
  const server = createScalarisServer({ root: process.env.SCALARIS_WEB_ROOT, backendPort });
  server.listen(port, '127.0.0.1', () => process.stdout.write(`Scalaris CRM disponible en http://127.0.0.1:${port}\n`));
  process.on('SIGTERM', () => server.close());
}
