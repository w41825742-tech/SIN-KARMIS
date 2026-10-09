import { readFile, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';

const gradlePath = resolve('android/variables.gradle');
let gradle;
try {
  gradle = await readFile(gradlePath, 'utf8');
} catch {
  console.error('Android project not found. Run npm run android:add first.');
  process.exit(1);
}
const pattern = /(minSdkVersion\s*=\s*)\d+/;
if (!pattern.test(gradle)) {
  console.error('Could not locate minSdkVersion in android/variables.gradle; please inspect the Capacitor Android template.');
  process.exit(1);
}
const updated = gradle.replace(pattern, (_match, prefix) => `${prefix}29`);
await writeFile(gradlePath, updated);
console.log('Android minimum SDK set to 29 (Android 10).');
