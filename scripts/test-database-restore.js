// Functional backup/restore against a private, disposable PostgreSQL database.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const root = path.resolve(__dirname, '..');
const temporary = fs.mkdtempSync(path.join(os.tmpdir(), 'crm-restore-test-'));
const project = `crm-restore-test-${process.pid}-${Date.now()}`;
const composeFile = path.join(temporary, 'compose.yml');
const configuration = path.join(temporary, 'synthetic.env');
const environment = { ...process.env, POSTGRES_DB: 'synthetic_crm', DATABASE_USER: 'synthetic_user' };
const run = (command, args) => {
  const childEnvironment = { ...environment };
  // A Node process launched by PowerShell 7 can inherit incompatible module
  // paths. Let Windows PowerShell rebuild its own built-in module path.
  if (command === 'powershell.exe') {
    for (const key of Object.keys(childEnvironment)) if (key.toLowerCase() === 'psmodulepath') delete childEnvironment[key];
  }
  return execFileSync(command, args, { cwd: root, env: childEnvironment, encoding: 'utf8', timeout: 180000, stdio: ['ignore', 'pipe', 'pipe'] }).trim();
};
const composeArguments = ['compose', '--file', composeFile, '--project-name', project, '--env-file', configuration];
const docker = (...args) => run('docker', args);
function databaseScript(name, extra) {
  if (process.platform === 'win32') {
    return run('powershell.exe', ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', path.join(root, 'scripts', `${name}.ps1`), ...extra.windows, '-ComposeFile', composeFile, '-ComposeProjectName', project, '-EnvFile', configuration]);
  }
  return run('sh', [path.join(root, 'scripts', `${name}.sh`), ...extra.unix, '--compose-file', composeFile, '--project-name', project, '--env-file', configuration]);
}
try {
  fs.writeFileSync(composeFile, 'services:\n  postgres:\n    image: postgres:17-alpine\n    environment:\n      POSTGRES_DB: synthetic_crm\n      POSTGRES_USER: synthetic_user\n      POSTGRES_PASSWORD: synthetic-local-drill-only\n    healthcheck:\n      test: ["CMD-SHELL", "pg_isready -h 127.0.0.1 -U synthetic_user -d synthetic_crm"]\n      interval: 1s\n      timeout: 3s\n      retries: 60\n');
  fs.writeFileSync(configuration, '# Synthetic data only; no operational configuration.\n');
  docker(...composeArguments, 'up', '-d', '--wait');
  const container = docker(...composeArguments, 'ps', '-q', 'postgres');
  const sql = (database, query) => docker('exec', container, 'psql', '-U', 'synthetic_user', '-d', database, '-At', '-v', 'ON_ERROR_STOP=1', '-c', query);
  sql('synthetic_crm', "CREATE TABLE flyway_schema_history(installed_rank integer, version text, success boolean); INSERT INTO flyway_schema_history VALUES(14, '14', true); CREATE TABLE release_probe(id integer PRIMARY KEY, value text); INSERT INTO release_probe VALUES(1, 'SYNTHETIC_RESTORE_CONTENT');");
  const backups = path.join(temporary, 'private backups');
  databaseScript('backup-database', { windows: ['-BackupDirectory', backups], unix: [backups] });
  const files = fs.readdirSync(backups).filter(name => name.endsWith('.dump'));
  assert.equal(files.length, 1, 'Exactly one completed archive must be produced.');
  const backup = path.join(backups, files[0]);
  assert.ok(fs.existsSync(`${backup}.sha256`), 'A checksum must accompany the archive.');
  databaseScript('restore-database', { windows: ['-BackupFile', backup, '-TargetDatabase', 'crm_restore_acceptance', '-ConfirmDestructiveRestore'], unix: [backup, 'crm_restore_acceptance', '--confirm-destructive-restore'] });
  assert.equal(sql('crm_restore_acceptance', 'SELECT value FROM release_probe WHERE id=1'), 'SYNTHETIC_RESTORE_CONTENT');
  assert.equal(sql('crm_restore_acceptance', 'SELECT version FROM flyway_schema_history WHERE success'), '14');
  assert.equal(sql('synthetic_crm', 'SELECT value FROM release_probe WHERE id=1'), 'SYNTHETIC_RESTORE_CONTENT', 'The source database must remain unchanged.');
  databaseScript('verify-backup-restore', { windows: [], unix: [] });
  assert.equal(sql('synthetic_crm', "SELECT count(*) FROM pg_database WHERE datname LIKE 'crm_restore_%'"), '1', 'The drill must clean only its own databases and preserve the independently restored target.');
  console.log('Functional database backup/restore passed: archive/checksum, restored synthetic rows and V14 history, source preserved, drill cleanup; isolated PostgreSQL only.');
} finally {
  try { docker(...composeArguments, 'down', '--volumes', '--remove-orphans'); } finally {
    if (path.resolve(temporary).startsWith(path.resolve(os.tmpdir()) + path.sep) && path.basename(temporary).startsWith('crm-restore-test-')) fs.rmSync(temporary, { recursive: true, force: true });
  }
}
