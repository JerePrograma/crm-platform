// Real nginx against an isolated synthetic backend; never uses CRM data or ports.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { execFileSync } = require('node:child_process');

const root = path.resolve(__dirname, '..');
const nginxImage = process.env.CRM_PROXY_TEST_NGINX_IMAGE || fs.readFileSync(path.join(root, 'frontend', 'Dockerfile'), 'utf8').match(/^FROM (nginx:\S+)/m)[1];
const temporary = fs.mkdtempSync(path.join(os.tmpdir(), 'crm-proxy-test-'));
const network = `crm-proxy-test-${process.pid}-${Date.now()}`;
const backend = `${network}-backend`;
const frontend = `${network}-frontend`;
const edge = `${network}-edge`;
const oldBackendAddress = `${network}-old-backend-address`;
const caddyImage = 'caddy@sha256:685ae3d7c34d9b5f8caeddc5d14051db7bbd88ff1f5a2fa0c16bd8ca328fb7af';
const docker = (...args) => execFileSync('docker', args, { encoding: 'utf8', timeout: 180000, stdio: ['ignore', 'pipe', 'pipe'] }).trim();
const pause = () => Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 150);
const mount = (source, target) => `type=bind,source=${source},target=${target},readonly`;
let checks = 0;
function check(condition, message) { assert.ok(condition, message); checks++; }

function createNetwork() {
  // Explicit IPAM lets the replacement test reserve the previous backend IP.
  // Retry only pool collisions so concurrent synthetic runs remain independent.
  for (let attempt = 0; attempt < 16; attempt++) {
    const subnet = `10.240.${(Date.now() + process.pid + attempt) % 256}.0/24`;
    try { docker('network', 'create', '--subnet', subnet, network); return; }
    catch (error) { if (!String(error.stderr).includes('Pool overlaps')) throw error; }
  }
  throw new Error('No isolated subnet was available for the proxy regression.');
}

function startBackend() {
  return docker('run', '-d', '--name', backend, '--network', network, '--network-alias', 'backend', '--read-only', '--user', '1000:1000', '--mount', mount(temporary, '/test'), 'node:22-alpine', 'node', '/test/backend.cjs');
}

function request(bytes = 0, trusted = false, separateClient = false, uri = '/api/probe', timeoutMs = 0) {
  const script = `let responded=false;const req=require('node:http').request(${JSON.stringify('http://frontend:8080' + uri)},{method:'POST',timeout:${timeoutMs},headers:{Host:'crm.test:9443','Content-Length':'${bytes}','X-Forwarded-Proto':'https',Forwarded:'for=attacker;proto=https;host=attacker.invalid','X-Forwarded-Host':'attacker.invalid','X-Forwarded-Port':'9999','X-Forwarded-Prefix':'/spoof','X-Forwarded-Ssl':'on','X-Forwarded-For':'198.51.100.77'}},res=>{responded=true;let body='';res.on('data',chunk=>body+=chunk);res.on('end',()=>console.log(JSON.stringify({status:res.statusCode,headers:res.headers,body})));});req.on('timeout',()=>req.destroy(new Error('Synthetic proxy request timed out.')));req.on('error',error=>{if(!responded){console.error(error);process.exitCode=1;}});req.end(Buffer.alloc(${bytes},120));`;
  const result = JSON.parse(separateClient
    ? docker('run', '--rm', '--network', network, 'node:22-alpine', 'node', '-e', script)
    : docker('exec', backend, 'node', '-e', script));
  if (result.status === 200) {
    const echo = JSON.parse(result.body);
    check(echo.headers['x-forwarded-proto'] === (trusted ? 'https' : 'http'), 'Only an allowed edge may supply the external HTTPS scheme.');
    check(!echo.headers.forwarded, 'Untrusted RFC Forwarded headers must be removed.');
    check(echo.headers['x-forwarded-host'] === 'crm.test:9443', 'The original Host and port must survive without accepting a spoofed forwarded host.');
    check(!echo.headers['x-forwarded-port'] && !echo.headers['x-forwarded-prefix'] && !echo.headers['x-forwarded-ssl'], 'Alternate forwarding headers must not bypass the trust boundary.');
    check(trusted || !echo.headers['x-forwarded-for'].includes('198.51.100.77'), 'An untrusted peer cannot inject forwarded client addresses.');
    check(echo.bytes === bytes, 'nginx must forward the complete allowed request body.');
  }
  return result;
}

try {
  fs.writeFileSync(path.join(temporary, 'backend.cjs'), `require('node:http').createServer((req,res)=>{let bytes=0;req.on('data',chunk=>bytes+=chunk.length);req.on('end',()=>{res.setHeader('Content-Type','application/json');if(req.url==='/api/v1/auth/login'){if(req.headers['x-forwarded-proto']!=='https'){res.statusCode=400;res.end('{}');return;}res.setHeader('Set-Cookie','SYNTHETIC_SESSION=synthetic-test-only; Secure; HttpOnly; SameSite=Lax; Path=/');}if(req.url==='/api/v1/auth/me'&&!String(req.headers.cookie).includes('SYNTHETIC_SESSION=')){res.statusCode=401;}res.end(JSON.stringify({headers:req.headers,bytes}));});}).listen(8080,'0.0.0.0');`);
  fs.writeFileSync(path.join(temporary, 'nginx.conf'), 'pid /tmp/nginx.pid;\nerror_log /dev/stderr warn;\nevents {}\nhttp { access_log off; include /etc/nginx/mime.types; include /etc/nginx/conf.d/default.conf; }\n');
  const allowlist = path.join(temporary, 'trusted-edge.conf');
  fs.writeFileSync(allowlist, '# No trusted peers.\n');
  createNetwork();
  startBackend();
  // A reviewed build must prove its packaged config, without a source override.
  const sourceConfigMount = process.env.CRM_PROXY_TEST_NGINX_IMAGE ? [] : ['--mount', mount(path.join(root, 'frontend', 'nginx.conf'), '/etc/nginx/conf.d/default.conf')];
  docker('run', '-d', '--name', frontend, '--network', network, '--network-alias', 'frontend', '--network-alias', 'scalaris-crm-frontend', '--read-only', '--user', '101:101', '--tmpfs', '/tmp:rw,noexec,nosuid,size=32m,mode=1777', '--mount', mount(temporary, '/test'), ...sourceConfigMount, '--mount', mount(allowlist, '/etc/nginx/crm-trusted-edge.conf'), '--entrypoint', 'nginx', nginxImage, '-c', '/test/nginx.conf', '-g', 'daemon off;');
  docker('exec', frontend, 'nginx', '-t', '-c', '/test/nginx.conf');
  let ready = false;
  let readinessError;
  for (let attempt = 0; attempt < 30; attempt++) {
    try { if (request().status === 200) { ready = true; break; } } catch (error) { readinessError = error; pause(); }
  }
  check(ready, `The isolated nginx/backend must become ready. ${readinessError || ''}`);
  const allowed = request(2 * 1024 * 1024);
  check(allowed.status === 200, 'An import body larger than 1 MiB must reach the backend.');
  check(!allowed.headers['strict-transport-security'], 'A spoofed HTTPS header from an untrusted peer must not enable HSTS.');
  check(allowed.headers['x-content-type-options'] === 'nosniff' && allowed.headers['x-frame-options'] === 'DENY', 'Security headers must also cover proxy responses.');
  check(allowed.headers['content-security-policy'].includes("frame-ancestors 'none'"), 'The proxy must send its frame restriction.');
  check(request(12 * 1024 * 1024).status === 413, 'Requests above the 11 MiB ceiling must fail before reaching the backend.');
  const peer = JSON.parse(docker('inspect', backend))[0].NetworkSettings.Networks[network].IPAddress;
  fs.writeFileSync(allowlist, `${peer}/32 1;\n`);
  docker('exec', frontend, 'nginx', '-t', '-c', '/test/nginx.conf');
  docker('exec', frontend, 'nginx', '-c', '/test/nginx.conf', '-s', 'reload');
  pause();
  const secure = request(0, true);
  check(secure.headers['strict-transport-security'] === 'max-age=31536000', 'HTTPS from the allowed edge must enable HSTS.');
  check(request(0, false, true).status === 200, 'A different peer remains untrusted after an edge is configured.');

  // Run the actual Hostinger site fragment with an ephemeral internal CA. There
  // are no published ports, public DNS changes, real accounts or outbound sends.
  fs.writeFileSync(path.join(temporary, 'Caddyfile'), '{\n admin off\n local_certs\n skip_install_trust\n http_port 8080\n https_port 8443\n}\n' + fs.readFileSync(path.join(root, 'deploy', 'hostinger', 'crm.caddy'), 'utf8'));
  docker('run', '-d', '--name', edge, '--network', network, '--network-alias', 'crm.scalaris.com.ar', '--read-only', '--user', '1000:1000', '--tmpfs', '/data:rw,nosuid,size=16m,mode=1777', '--tmpfs', '/config:rw,nosuid,size=16m,mode=1777', '--mount', mount(temporary, '/test'), caddyImage, 'caddy', 'run', '--config', '/test/Caddyfile', '--adapter', 'caddyfile');
  const edgePeer = JSON.parse(docker('inspect', edge))[0].NetworkSettings.Networks[network].IPAddress;
  fs.writeFileSync(allowlist, `${edgePeer}/32 1;\n`);
  docker('exec', frontend, 'nginx', '-c', '/test/nginx.conf', '-s', 'reload');
  function edgeRequest(uri, cookie = '') {
    // Trust is relaxed only for this disposable CA inside the test network.
    const script = `const url=new URL(${JSON.stringify(uri)});require(url.protocol==='https:'?'node:https':'node:http').get(url,{rejectUnauthorized:false,headers:{Cookie:${JSON.stringify(cookie)},Forwarded:'for=attacker;proto=http','X-Forwarded-Proto':'http'}},res=>{let body='';res.on('data',chunk=>body+=chunk);res.on('end',()=>console.log(JSON.stringify({status:res.statusCode,headers:res.headers,body})));}).on('error',e=>{console.error(e);process.exit(1)});`;
    return JSON.parse(docker('exec', backend, 'node', '-e', script));
  }
  let redirect;
  for (let attempt = 0; attempt < 30; attempt++) {
    try { redirect = edgeRequest('http://crm.scalaris.com.ar:8080/api/v1/auth/login'); break; } catch { pause(); }
  }
  check(redirect?.status === 308 && redirect.headers.location.startsWith('https://crm.scalaris.com.ar'), 'The real Caddy fragment must redirect HTTP to HTTPS.');
  const login = edgeRequest('https://crm.scalaris.com.ar:8443/api/v1/auth/login');
  check(login.status === 200, 'The synthetic login must receive the trusted HTTPS scheme through Caddy and nginx.');
  const cookie = login.headers['set-cookie'][0];
  check(cookie.includes('Secure') && cookie.includes('HttpOnly') && cookie.includes('SameSite=Lax'), 'Secure session cookie attributes must survive the complete proxy chain.');
  check(edgeRequest('https://crm.scalaris.com.ar:8443/api/v1/auth/me', cookie.split(';')[0]).status === 200, 'A session must survive a second HTTPS request through both proxies.');
  check(edgeRequest('https://crm.scalaris.com.ar:8443/actuator/prometheus').status === 404, 'Operational metrics must stay unavailable at the public edge.');
  check(request().status === 200, 'Direct spoofing remains ineffective after the real TLS edge is configured.');
  request(0, false, false, '/api/probe?search=synthetic-private-query');
  request(0, false, false, '/api/v1/unsubscribe/synthetic-private-token');
  const accessLogs = docker('logs', frontend);
  check(!accessLogs.includes('synthetic-private-query') && !accessLogs.includes('synthetic-private-token'), 'Access logs must omit query values and redact unsubscribe tokens.');
  if (process.env.CRM_PROXY_TEST_NGINX_IMAGE) {
    docker('exec', frontend, 'sh', '-ec', '! command -v curl && ! apk info --exists libcurl');
    check(true, 'The built frontend runtime must not retain unused curl/libcurl.');
  }

  // Replace only the backend and occupy its old address with a distinct HTTP
  // sentinel. A proxy that still caches its startup IP cannot pass either route.
  const frontendBeforeReplacement = JSON.parse(docker('inspect', frontend))[0];
  const backendBeforeReplacement = JSON.parse(docker('inspect', backend))[0];
  const previousIp = backendBeforeReplacement.NetworkSettings.Networks[network].IPAddress;
  docker('rm', '-f', backend);
  docker('run', '-d', '--name', oldBackendAddress, '--network', network, '--ip', previousIp, '--read-only', '--user', '1000:1000', 'node:22-alpine', 'node', '-e', "require('node:http').createServer((req,res)=>{res.statusCode=503;res.end('STALE_BACKEND_ADDRESS');}).listen(8080,'0.0.0.0');");
  const replacementId = startBackend();
  const replacementIp = JSON.parse(docker('inspect', backend))[0].NetworkSettings.Networks[network].IPAddress;
  check(replacementId !== backendBeforeReplacement.Id && replacementIp !== previousIp, 'The replacement backend must have a new container ID and a different IP.');
  const recoveryStarted = Date.now();
  let recovered = false;
  let recoveryError;
  while (Date.now() - recoveryStarted < 15000) {
    try {
      const api = request(0, false, false, '/api/probe', 1000);
      const actuator = request(0, false, false, '/actuator/health', 1000);
      if (api.status === 200 && actuator.status === 200) { recovered = true; break; }
    } catch (error) { recoveryError = error; }
    pause();
  }
  check(recovered && Date.now() - recoveryStarted < 15000, `Both /api and /actuator must recover within 15 seconds after backend replacement. ${recoveryError || ''}`);
  const frontendAfterReplacement = JSON.parse(docker('inspect', frontend))[0];
  check(frontendAfterReplacement.Id === frontendBeforeReplacement.Id && frontendAfterReplacement.State.StartedAt === frontendBeforeReplacement.State.StartedAt && frontendAfterReplacement.RestartCount === frontendBeforeReplacement.RestartCount, 'The frontend must recover without a restart, reload or container replacement.');
  console.log(`Production proxy regression passed: ${checks} assertions; synthetic resources only.`);
} catch (error) {
  for (const name of [edge, frontend, backend, oldBackendAddress]) {
    try { console.error(`${name}:\n${docker('logs', name)}`); } catch {}
  }
  throw error;
} finally {
  for (const name of [edge, frontend, backend, oldBackendAddress]) { try { docker('rm', '-f', name); } catch {} }
  try { docker('network', 'rm', network); } catch {}
  if (path.resolve(temporary).startsWith(path.resolve(os.tmpdir()) + path.sep) && path.basename(temporary).startsWith('crm-proxy-test-')) fs.rmSync(temporary, { recursive: true, force: true });
}
