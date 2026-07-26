'use strict';

const { execFileSync } = require('node:child_process');
const net = require('node:net');

function parseRequestedPorts(argumentsList) {
  if (!Array.isArray(argumentsList) || argumentsList.length === 0) {
    throw new Error('At least one host port definition is required.');
  }

  const requested = argumentsList.map((argument) => {
    const text = String(argument ?? '');
    const separator = text.lastIndexOf('=');
    if (separator <= 0 || separator === text.length - 1) {
      throw new Error(`Invalid host port definition: ${text}`);
    }

    const name = text.slice(0, separator).trim();
    const rawPort = text.slice(separator + 1).trim();
    const port = Number(rawPort);
    if (name.length === 0 || !Number.isInteger(port) || port < 1 || port > 65535) {
      throw new Error(`Invalid host port definition: ${text}`);
    }

    return { name, port };
  });

  const uniquePorts = new Set(requested.map(({ port }) => port));
  if (uniquePorts.size !== requested.length) {
    throw new Error('All requested host ports must be different.');
  }

  return requested;
}

function readDockerPublications() {
  try {
    return execFileSync(
      'docker',
      ['ps', '--format', '{{.ID}}|{{.Names}}|{{.Ports}}'],
      { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] },
    )
      .split(/\r?\n/u)
      .filter((line) => line.trim().length > 0);
  } catch {
    throw new Error('Could not inspect Docker containers and their published ports.');
  }
}

function findDockerPortOwners(lines, port) {
  const marker = `:${port}->`;
  return lines.filter((line) => line.includes(marker));
}

function assertLoopbackPortAvailable(name, port) {
  return new Promise((resolve, reject) => {
    const server = net.createServer();
    server.unref();

    server.once('error', (error) => {
      reject(
        new Error(
          `${name} host port 127.0.0.1:${port} cannot be bound. ` +
            `The port may be occupied or reserved. Underlying error: ${error.message}`,
        ),
      );
    });

    server.listen({ host: '127.0.0.1', port, exclusive: true }, () => {
      server.close((error) => {
        if (error) {
          reject(error);
          return;
        }
        resolve();
      });
    });
  });
}

async function checkHostPorts(requested, dockerLines = readDockerPublications()) {
  for (const { name, port } of requested) {
    const owners = findDockerPortOwners(dockerLines, port);
    if (owners.length > 0) {
      throw new Error(
        `${name} host port ${port} is already published by another Docker container:\n` +
          `${owners.join('\n')}\n` +
          'Stop or reconfigure the owning container, or choose another host port.',
      );
    }
    console.log(`${name} Docker publication available: ${port}`);
  }

  for (const { name, port } of requested) {
    await assertLoopbackPortAvailable(name, port);
    console.log(`${name} host port available: 127.0.0.1:${port}`);
  }
}

async function main() {
  const requested = parseRequestedPorts(process.argv.slice(2));
  await checkHostPorts(requested);
  console.log('All requested host ports are available and not published by Docker.');
}

if (require.main === module) {
  main().catch((error) => {
    console.error(error instanceof Error ? error.message : String(error));
    process.exitCode = 1;
  });
}

module.exports = {
  assertLoopbackPortAvailable,
  checkHostPorts,
  findDockerPortOwners,
  parseRequestedPorts,
};
