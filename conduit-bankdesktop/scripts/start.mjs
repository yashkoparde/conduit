import { spawn } from 'node:child_process';
import { existsSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(fileURLToPath(new URL('..', import.meta.url)));
const buildScript = resolve(root, 'scripts', 'build.mjs');

if (!existsSync(buildScript)) {
  console.error('Dependencies are missing. Run npm install, then npm start.');
  process.exit(1);
}

console.log('Building the production interface…');
const build = spawn(process.execPath, [buildScript], { cwd: root, stdio: 'inherit' });
build.on('error', (error) => {
  console.error(`Could not start the build: ${error.message}`);
  process.exit(1);
});
build.on('exit', (code) => {
  if (code !== 0) process.exit(code ?? 1);
  const server = spawn(process.execPath, [resolve(root, 'server', 'index.mjs')], { cwd: root, stdio: 'inherit' });
  server.on('error', (error) => {
    console.error(`Could not start the local server: ${error.message}`);
    process.exit(1);
  });
  server.on('exit', (serverCode) => process.exit(serverCode ?? 0));
  for (const signal of ['SIGINT', 'SIGTERM']) {
    process.on(signal, () => server.kill(signal));
  }
});
