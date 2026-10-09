import { cp, mkdir, copyFile } from 'node:fs/promises';
import { resolve } from 'node:path';
const root = resolve('.');
const web = resolve(root, 'www');
await mkdir(resolve(web, 'assets'), { recursive: true });
for (const file of ['index.html', 'manifest.webmanifest', 'service-worker.js']) {
  await copyFile(resolve(root, file), resolve(web, file));
}
await cp(resolve(root, 'assets'), resolve(web, 'assets'), { recursive: true, force: true });
console.log('Web source copied to www/.');
