import { spawnSync } from 'node:child_process';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(fileURLToPath(new URL('..', import.meta.url)));
const viteBin = resolve(root, 'node_modules', 'vite', 'bin', 'vite.js');
const build = spawnSync(process.execPath, [viteBin, 'build', '--configLoader', 'runner'], { cwd: root, stdio: 'inherit' });
if (build.error) {
  console.error(`Could not start the production build: ${build.error.message}`);
  process.exit(1);
}
if (build.status !== 0) process.exit(build.status ?? 1);

const distDir = join(root, 'dist');
const builtHtml = await readFile(join(distDir, 'index.html'), 'utf8');
const scriptTag = builtHtml.match(/<script\b[^>]*type="module"[^>]*src="([^"]+)"[^>]*><\/script>/i);
const styleTag = builtHtml.match(/<link\b[^>]*rel="stylesheet"[^>]*href="([^"]+)"[^>]*>/i);
if (!scriptTag) throw new Error('The compiled application script was not found in dist/index.html.');
if (!styleTag) throw new Error('The compiled application stylesheet was not found in dist/index.html.');
const relativeAsset = (source) => source.replace(/^\.\//, '');
const jsPath = resolve(distDir, relativeAsset(scriptTag[1]));
const cssPath = resolve(distDir, relativeAsset(styleTag[1]));
const js = (await readFile(jsPath, 'utf8')).replace(/<\/script/gi, '<\\/script');
const css = await readFile(cssPath, 'utf8');
const standalone = builtHtml
  .replace(scriptTag[0], () => `<script type="module">${js}</script>`)
  .replace(styleTag[0], () => `<style>${css}</style>`);
const outputPath = join(root, 'outputs', 'karnataka-loan-intelligence.html');
await mkdir(dirname(outputPath), { recursive: true });
await writeFile(outputPath, standalone, 'utf8');
console.log(`Standalone HTML written to ${outputPath}`);
