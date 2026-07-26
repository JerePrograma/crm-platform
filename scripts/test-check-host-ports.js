'use strict';

const assert = require('node:assert/strict');
const net = require('node:net');
const {
  assertLoopbackPortAvailable,
  checkHostPorts,
  findDockerPortOwners,
  parseRequestedPorts,
} = require('./check-host-ports');

function listenOnLoopback(port = 0) {
  return new Promise((resolve, reject) => {
    const server = net.createServer();
    server.once('error', reject);
    server.listen({ host: '127.0.0.1', port, exclusive: true }, () => resolve(server));
  });
}

(async () => {
  const requested = parseRequestedPorts([
    'PostgreSQL=25432',
    'Backend=38080',
    'Frontend=35173',
    'Production frontend=48080',
  ]);
  assert.deepEqual(requested.map(({ port }) => port), [25432, 38080, 35173, 48080]);

  assert.throws(
    () => parseRequestedPorts(['Frontend=5173', 'Production frontend=5173']),
    /must be different/u,
  );
  assert.throws(() => parseRequestedPorts(['Frontend=invalid']), /Invalid host port definition/u);

  const dockerLines = ['abc123|demo|127.0.0.1:18080->8080/tcp'];
  assert.deepEqual(findDockerPortOwners(dockerLines, 18080), dockerLines);
  assert.deepEqual(findDockerPortOwners(dockerLines, 48080), []);

  const freeServer = await listenOnLoopback();
  const freePort = freeServer.address().port;
  await new Promise((resolve, reject) =>
    freeServer.close((error) => (error ? reject(error) : resolve())),
  );
  await assert.doesNotReject(() =>
    checkHostPorts([{ name: 'Production frontend', port: freePort }], []),
  );

  const occupiedServer = await listenOnLoopback();
  const occupiedPort = occupiedServer.address().port;
  try {
    await assert.rejects(
      () => assertLoopbackPortAvailable('Production frontend', occupiedPort),
      /Production frontend host port/u,
    );
  } finally {
    await new Promise((resolve, reject) =>
      occupiedServer.close((error) => (error ? reject(error) : resolve())),
    );
  }

  await assert.rejects(
    () => checkHostPorts([{ name: 'Production frontend', port: 18080 }], dockerLines),
    /already published/u,
  );

  console.log('Node host port preflight self-test passed.');
})().catch((error) => {
  console.error(error instanceof Error ? error.stack : String(error));
  process.exitCode = 1;
});
