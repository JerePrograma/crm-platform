const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const { randomUUID } = require('node:crypto');

const repoRoot = path.resolve(__dirname, '..');
const requiredGuards = [
  'SPRING_PROFILES_ACTIVE=scalaris', 'CRM_BUSINESS_PROFILE=SCALARIS',
  'SENDING_ENABLED=false', 'SENDING_DRY_RUN=true', 'SENDING_DAILY_LIMIT=0',
  'SENDING_KILL_SWITCH=true', 'MESSAGING_REAL_NETWORK_ALLOWED=false',
  'EMAIL_PROVIDER_MODE=NOOP', 'WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY',
  'OUTBOX_WORKER_ENABLED=false', 'FAKE_INBOUND_ENABLED=false',
];

function parseOptions(args) {
  const options = { stack: 'scalaris', frontendPort: 18787 };
  for (let index = 0; index < args.length; index += 2) {
    const value = args[index + 1];
    if (args[index] === '--stack' && ['scalaris', 'production'].includes(value)) options.stack = value;
    else if (args[index] === '--frontend-port' && /^\d+$/.test(value ?? '')) options.frontendPort = Number(value);
    else throw new Error('Use --stack scalaris|production and --frontend-port 1..65535');
  }
  assert.ok(options.frontendPort >= 1 && options.frontendPort <= 65535, 'Invalid frontend port');
  return options;
}

function syntheticEnvironment(source, options, project, reportPath) {
  const environment = {
    ...source,
    COMPOSE_PROJECT_NAME: project, COMPOSE_PROFILES: '', COMPOSE_DISABLE_ENV_FILE: '1',
    CRM_BACKEND_IMAGE: `${project}-backend`,
    CRM_FRONTEND_IMAGE: `${project}-frontend`,
    TRUSTED_EDGE_CONFIG_FILE: path.join(repoRoot, 'frontend/nginx-trusted-edge.conf'),
    DATABASE_PASSWORD: 'synthetic-scalaris-drill-database-password',
    CRM_BOOTSTRAP_USERNAME: 'scalaris-complete-admin',
    CRM_BOOTSTRAP_PASSWORD: 'synthetic-scalaris-complete-admin-password',
    CRM_MARKETING_USERNAME: 'scalaris-complete-marketing',
    CRM_MARKETING_PASSWORD: 'synthetic-scalaris-complete-marketing-password',
    CRM_MARKETING_DISPLAY_NAME: 'Marketing sintético Scalaris',
    SESSION_COOKIE_SECURE: 'false',
    PRODUCTION_FRONTEND_PORT: String(options.frontendPort), SCALARIS_FRONTEND_PORT: String(options.frontendPort),
    SCALARIS_DATABASE_PASSWORD: 'synthetic-scalaris-drill-database-password',
    SCALARIS_BOOTSTRAP_USERNAME: 'scalaris-complete-admin',
    SCALARIS_BOOTSTRAP_PASSWORD: 'synthetic-scalaris-complete-admin-password',
    SCALARIS_MARKETING_USERNAME: 'scalaris-complete-marketing',
    SCALARIS_MARKETING_PASSWORD: 'synthetic-scalaris-complete-marketing-password',
    SCALARIS_MARKETING_DISPLAY_NAME: 'Marketing sintético Scalaris',
    CRM_E2E_BASE_URL: `http://127.0.0.1:${options.frontendPort}`,
    CRM_E2E_USERNAME: 'scalaris-complete-admin',
    CRM_E2E_PASSWORD: 'synthetic-scalaris-complete-admin-password',
    CRM_E2E_MARKETING_USERNAME: 'scalaris-complete-marketing',
    CRM_E2E_MARKETING_PASSWORD: 'synthetic-scalaris-complete-marketing-password',
    CRM_SCALARIS_SCREENSHOTS_DIR: reportPath.replace(/-playwright\.json$/, '-screenshots'),
    CRM_E2E_BROWSER_CHANNEL: 'bundled', PLAYWRIGHT_JSON_OUTPUT_FILE: reportPath,
  };
  delete environment.COMPOSE_FILE;
  delete environment.COMPOSE_ENV_FILES;
  for (const entry of requiredGuards) {
    const separator = entry.indexOf('=');
    environment[entry.slice(0, separator)] = entry.slice(separator + 1);
  }
  return environment;
}

function assertBackendEnvironment(entries) {
  assert.ok(Array.isArray(entries) && entries.length > 0, 'Backend environment must be a nonempty array');
  for (const entry of requiredGuards) assert.ok(entries.includes(entry), `Missing runtime guard: ${entry}`);
}

function assertPlaywrightReport(report) {
  assert.ok(report.stats.expected >= 5, 'All five Scalaris and task pagination E2E tests must execute');
  assert.equal(report.stats.unexpected, 0, 'Unexpected E2E failures');
  assert.equal(report.stats.skipped, 0, 'Scalaris E2E must not skip a test');
  assert.equal(report.stats.flaky, 0, 'Scalaris E2E must not be flaky');
  assert.equal((report.errors ?? []).length, 0, 'Playwright reported global errors');
}

function main(args = process.argv.slice(2)) {
  const options = parseOptions(args);
  const project = `crm-scalaris-${options.stack}-e2e-${Date.now()}-${randomUUID().slice(0, 8)}`;
  const outputDirectory = path.join(repoRoot, 'validation-output');
  fs.mkdirSync(outputDirectory, { recursive: true });
  const reportPath = path.join(outputDirectory, `${project}-playwright.json`);
  const summaryPath = path.join(outputDirectory, `${project}.json`);
  const environment = syntheticEnvironment(process.env, options, project, reportPath);
  const composeFile = path.join(repoRoot, options.stack === 'production'
    ? 'deploy/docker-compose.production.yml' : 'docker-compose.scalaris.yml');
  const composeArgs = ['compose', '--project-name', project, '-f', composeFile];
  const summary = {
    schemaVersion: 1, validation: 'Isolated Scalaris Marketing E2E', status: 'RUNNING',
    stack: options.stack, composeProject: project, syntheticOnly: true,
    startedAtUtc: new Date().toISOString(), frontendPort: options.frontendPort,
    commit: null, phases: {}, playwrightReport: reportPath, cleanup: 'NOT_STARTED',
  };
  let stackAttempted = false;
  function run(command, arguments_, capture = false, workingDirectory = repoRoot) {
    const result = spawnSync(command, arguments_, {
      cwd: workingDirectory, env: environment, encoding: 'utf8',
      stdio: capture ? ['ignore', 'pipe', 'pipe'] : 'inherit', maxBuffer: 20 * 1024 * 1024,
    });
    if (result.error) throw result.error;
    if (result.status !== 0) throw new Error(`${path.basename(command)} failed with exit code ${result.status}`);
    return capture ? result.stdout.trim() : undefined;
  }
  function phase(name, action) {
    summary.phases[name] = 'RUNNING';
    try { action(); summary.phases[name] = 'EXECUTED_PASS'; }
    catch (error) { summary.phases[name] = 'EXECUTED_FAIL'; throw error; }
  }
  try {
    summary.commit = run('git', ['rev-parse', 'HEAD'], true);
    summary.workingTreeClean = run('git', ['status', '--porcelain'], true).length === 0;
    phase('configuration', () => {
      run('docker', ['info'], true);
      run(process.execPath, [path.join(__dirname, 'check-host-ports.js'), `Scalaris frontend=${options.frontendPort}`]);
      run('docker', [...composeArgs, 'config', '--quiet']);
    });
    phase('isolatedStack', () => {
      stackAttempted = true;
      run('docker', [...composeArgs, 'up', '-d', '--build', '--wait', '--wait-timeout', '240']);
    });
    phase('readinessThroughProxy', () => {
      const health = run(process.execPath, ['-e',
        "fetch(process.env.CRM_E2E_BASE_URL + '/actuator/health/readiness', { signal: AbortSignal.timeout(15000) }).then(async response => { if (!response.ok) throw new Error('Readiness HTTP ' + response.status); console.log(JSON.stringify(await response.json())); }).catch(error => { console.error(error.message); process.exitCode = 1; });"], true);
      assert.equal(JSON.parse(health).status, 'UP', 'Readiness through the frontend proxy must be UP');
    });
    let postgres;
    phase('runtimeGuards', () => {
      summary.images = {};
      const backend = run('docker', [...composeArgs, 'ps', '-q', 'backend'], true);
      postgres = run('docker', [...composeArgs, 'ps', '-q', 'postgres'], true);
      assert.ok(backend && postgres, 'Required isolated containers missing');
      assertBackendEnvironment(JSON.parse(run('docker', ['inspect', backend, '--format', '{{json .Config.Env}}'], true)));
      for (const service of ['backend', 'frontend']) {
        const container = run('docker', [...composeArgs, 'ps', '-q', service], true);
        summary.images[service] = {
          reference: run('docker', ['inspect', container, '--format', '{{.Config.Image}}'], true),
          imageId: run('docker', ['inspect', container, '--format', '{{.Image}}'], true),
        };
        const user = run('docker', ['inspect', container, '--format', '{{.Config.User}}'], true);
        const userName = user.split(':')[0];
        assert.ok(userName && userName !== 'root' && !/^0+$/.test(userName), `${service} must run as non-root`);
        assert.equal(run('docker', ['inspect', container, '--format', '{{.HostConfig.ReadonlyRootfs}}'], true), 'true');
      }
      for (const container of [backend, postgres]) {
        const ports = JSON.parse(run('docker', ['inspect', container, '--format', '{{json .HostConfig.PortBindings}}'], true));
        assert.ok(!ports || Object.keys(ports).length === 0, 'Backend and PostgreSQL must not publish host ports');
      }
      const frontend = run('docker', [...composeArgs, 'ps', '-q', 'frontend'], true);
      const frontendPorts = JSON.parse(run('docker', ['inspect', frontend, '--format', '{{json .HostConfig.PortBindings}}'], true));
      const bindings = Object.values(frontendPorts ?? {}).flat();
      assert.equal(bindings.length, 1, 'Only one frontend listener may be published');
      assert.equal(bindings[0].HostIp, '127.0.0.1', 'Frontend must listen on loopback only');
      assert.equal(bindings[0].HostPort, String(options.frontendPort));
    });
    phase('marketingAndLargeImportE2E', () => {
      run(process.execPath, [path.join(repoRoot, 'frontend/node_modules/@playwright/test/cli.js'),
        'test', 'scalaris-marketing.spec.ts', 'tasks-pagination.spec.ts', '--reporter=line,json',
        '--output', path.join(outputDirectory, `${project}-artifacts`)], false, path.join(repoRoot, 'frontend'));
      assertPlaywrightReport(JSON.parse(fs.readFileSync(reportPath, 'utf8')));
    });
    phase('zeroSent', () => {
      const database = options.stack === 'production' ? 'gestudio_crm' : 'scalaris_crm';
      const databaseUser = options.stack === 'production' ? 'gestudio' : 'scalaris';
      const count = run('docker', ['exec', postgres, 'psql', '-U', databaseUser, '-d', database, '-At', '-c',
        "SELECT count(*) FROM message_record WHERE status IN ('SENT','DELIVERED','READ');"], true);
      assert.equal(count, '0', 'Forbidden delivery state found');
    });
    summary.status = 'EXECUTED_PASS';
  } catch (error) {
    summary.status = 'EXECUTED_FAIL'; summary.error = error.message;
    process.exitCode = 1; console.error(error.message);
  } finally {
    if (stackAttempted) {
      try {
        run('docker', [...composeArgs, 'down', '-v', '--remove-orphans']);
        summary.cleanup = 'EXECUTED_PASS';
      } catch (error) {
        summary.cleanup = 'EXECUTED_FAIL'; summary.status = 'EXECUTED_FAIL';
        summary.cleanupError = error.message; process.exitCode = 1;
      }
    } else summary.cleanup = 'NOT_REQUIRED';
    summary.finishedAtUtc = new Date().toISOString();
    fs.writeFileSync(summaryPath, `${JSON.stringify(summary, null, 2)}\n`);
    console.log(`Scalaris ${options.stack} gate: ${summary.status}; evidence ${summaryPath}`);
  }
}

module.exports = { parseOptions, syntheticEnvironment, assertBackendEnvironment, assertPlaywrightReport };
if (require.main === module) main();
