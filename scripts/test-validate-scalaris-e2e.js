const assert = require('node:assert/strict');
const test = require('node:test');
const { parseOptions, syntheticEnvironment, assertBackendEnvironment, assertPlaywrightReport } = require('./validate-scalaris-e2e');

test('the isolated drill overrides inherited operational credentials, Compose selection and sending flags', () => {
  const source = {
    CRM_BOOTSTRAP_USERNAME: 'operational', CRM_BOOTSTRAP_PASSWORD: 'private',
    CRM_MARKETING_PASSWORD: 'private', SCALARIS_DATABASE_PASSWORD: 'private',
    COMPOSE_FILE: 'operational.yml', COMPOSE_ENV_FILES: '/private/operational.env',
    COMPOSE_PROJECT_NAME: 'existing-installation',
    CRM_BACKEND_IMAGE: 'operational/backend:live', CRM_FRONTEND_IMAGE: 'operational/frontend:live',
    TRUSTED_EDGE_CONFIG_FILE: '/private/operational/trusted-edge.conf',
    SENDING_ENABLED: 'true', SENDING_KILL_SWITCH: 'false', MESSAGING_REAL_NETWORK_ALLOWED: 'true',
  };
  const environment = syntheticEnvironment(source, { frontendPort: 18787 }, 'crm-isolated-test', '/tmp/report.json');
  assert.equal(source.CRM_BOOTSTRAP_PASSWORD, 'private', 'The parent environment must remain unchanged');
  assert.equal(environment.COMPOSE_FILE, undefined);
  assert.equal(environment.COMPOSE_ENV_FILES, undefined);
  assert.equal(environment.COMPOSE_PROJECT_NAME, 'crm-isolated-test');
  assert.equal(environment.CRM_BACKEND_IMAGE, 'crm-isolated-test-backend');
  assert.equal(environment.CRM_FRONTEND_IMAGE, 'crm-isolated-test-frontend');
  assert.equal(environment.TRUSTED_EDGE_CONFIG_FILE, require('node:path').resolve(__dirname, '../frontend/nginx-trusted-edge.conf'));
  assert.match(environment.CRM_BOOTSTRAP_PASSWORD, /^synthetic-/);
  assert.match(environment.CRM_MARKETING_PASSWORD, /^synthetic-/);
  assert.match(environment.SCALARIS_DATABASE_PASSWORD, /^synthetic-/);
  assertBackendEnvironment(Object.entries(environment).map(([key, value]) => `${key}=${value}`));
});

test('runtime checks fail closed for a LEGACY profile, missing or unsafe sending guard', () => {
  const environment = syntheticEnvironment({}, { frontendPort: 18787 }, 'isolated', '/tmp/report.json');
  const entries = Object.entries(environment).map(([key, value]) => `${key}=${value}`);
  assert.throws(() => assertBackendEnvironment(entries.filter((entry) => entry !== 'SENDING_ENABLED=false')));
  assert.throws(() => assertBackendEnvironment(entries.map((entry) => entry === 'CRM_BUSINESS_PROFILE=SCALARIS' ? 'CRM_BUSINESS_PROFILE=LEGACY' : entry)));
  assert.throws(() => assertBackendEnvironment(['SENDING_ENABLED=true']));
  assert.throws(() => assertBackendEnvironment(null));
});

test('a skipped, flaky, incomplete or failed Scalaris suite cannot pass the release gate', () => {
  const report = { stats: { expected: 5, unexpected: 0, skipped: 0, flaky: 0 }, errors: [] };
  assertPlaywrightReport(report);
  for (const field of ['unexpected', 'skipped', 'flaky']) {
    assert.throws(() => assertPlaywrightReport({ ...report, stats: { ...report.stats, [field]: 1 } }));
  }
  assert.throws(() => assertPlaywrightReport({ ...report, stats: { ...report.stats, expected: 4 } }));
  assert.throws(() => assertPlaywrightReport({ ...report, errors: [{}] }));
});

test('only supported stacks and valid loopback port numbers are accepted', () => {
  assert.deepEqual(parseOptions(['--stack', 'production', '--frontend-port', '18080']), { stack: 'production', frontendPort: 18080 });
  for (const args of [['--stack', 'existing-installation'], ['--frontend-port', '0'], ['--frontend-port', '65536'], ['--frontend-port', 'abc']]) {
    assert.throws(() => parseOptions(args));
  }
});
