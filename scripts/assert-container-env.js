'use strict';

const fs = require('node:fs');

function parseContainerEnvironmentJson(input) {
  const text = String(input ?? '').trim();
  if (text.length === 0) {
    throw new Error('Container environment JSON is empty.');
  }

  let parsed;
  try {
    parsed = JSON.parse(text);
  } catch {
    throw new Error('Container environment JSON is invalid.');
  }

  if (!Array.isArray(parsed)) {
    throw new Error('Container environment JSON root must be an array.');
  }

  return parsed.map((entry) => String(entry));
}

function assertContainerEnvironmentEntries(input, requiredEntries) {
  if (!Array.isArray(requiredEntries) || requiredEntries.length === 0) {
    throw new Error('At least one required environment entry is required.');
  }

  const entries = parseContainerEnvironmentJson(input);
  const missing = requiredEntries
    .map((entry) => String(entry))
    .filter((entry) => !entries.includes(entry));

  if (missing.length > 0) {
    throw new Error(`Missing required container environment entries: ${missing.join(', ')}`);
  }
}

if (require.main === module) {
  try {
    const input = fs.readFileSync(0, 'utf8');
    assertContainerEnvironmentEntries(input, process.argv.slice(2));
  } catch (error) {
    console.error(error instanceof Error ? error.message : String(error));
    process.exitCode = 1;
  }
}

module.exports = {
  assertContainerEnvironmentEntries,
  parseContainerEnvironmentJson,
};
