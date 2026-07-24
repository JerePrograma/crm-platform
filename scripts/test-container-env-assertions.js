'use strict';

const assert = require('node:assert/strict');
const {
  assertContainerEnvironmentEntries,
  parseContainerEnvironmentJson,
} = require('./assert-container-env');

const requiredEntries = [
  'SENDING_ENABLED=false',
  'SENDING_DRY_RUN=true',
  'SENDING_DAILY_LIMIT=0',
  'SENDING_KILL_SWITCH=true',
  'MESSAGING_REAL_NETWORK_ALLOWED=false',
  'EMAIL_PROVIDER_MODE=NOOP',
  'WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY',
];

const validJson = JSON.stringify(requiredEntries);
assert.deepEqual(parseContainerEnvironmentJson(validJson), requiredEntries);
assert.doesNotThrow(() => assertContainerEnvironmentEntries(validJson, requiredEntries));

const missingEnabled = requiredEntries.filter((entry) => entry !== 'SENDING_ENABLED=false');
assert.throws(
  () => assertContainerEnvironmentEntries(JSON.stringify(missingEnabled), requiredEntries),
  /SENDING_ENABLED=false/,
);

const enabledTrue = [...missingEnabled, 'SENDING_ENABLED=true'];
assert.throws(
  () => assertContainerEnvironmentEntries(JSON.stringify(enabledTrue), requiredEntries),
  /SENDING_ENABLED=false/,
);

assert.throws(() => assertContainerEnvironmentEntries('not-json', requiredEntries), /invalid/);
assert.doesNotThrow(() => assertContainerEnvironmentEntries(`\n${validJson}\n`, requiredEntries));

console.log('Container environment assertion self-test passed.');
