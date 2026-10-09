import { mkdir, readFile, writeFile, copyFile, cp } from 'node:fs/promises';
import { resolve, dirname } from 'node:path';

const root = resolve('.');
const mainPath = resolve(root, 'android/app/src/main/java/id/sinkarm1s/gamingspace/MainActivity.java');
const manifestPath = resolve(root, 'android/app/src/main/AndroidManifest.xml');
const nativeDir = resolve(root, 'native/android');

let manifest;
try {
  manifest = await readFile(manifestPath, 'utf8');
} catch {
  console.error('Android project not found. Run npm run android:add first.');
  process.exit(1);
}

// Android 11+ hides most installed packages unless launcher activities are declared visible.
const launcherQueries = `    <queries>\n        <intent>\n            <action android:name=\"android.intent.action.MAIN\" />\n            <category android:name=\"android.intent.category.LAUNCHER\" />\n        </intent>\n    </queries>\n`;
if (!manifest.includes('<queries>')) {
  manifest = manifest.replace(/<application\b/, `${launcherQueries}    <application`);
}

const permissions = [
  'android.permission.SYSTEM_ALERT_WINDOW',
  'android.permission.FOREGROUND_SERVICE',
  'android.permission.FOREGROUND_SERVICE_SPECIAL_USE',
  'android.permission.POST_NOTIFICATIONS'
];
for (const permission of permissions) {
  const line = `    <uses-permission android:name="${permission}" />`;
  if (!manifest.includes(`android:name="${permission}"`)) {
    manifest = manifest.replace(/<application\b/, `${line}\n    <application`);
  }
}

const serviceTag = `        <service\n            android:name=".GameOverlayService"\n            android:enabled="true"\n            android:exported="false"\n            android:stopWithTask="false"\n            android:foregroundServiceType="specialUse">\n            <property\n                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"\n                android:value="User-started movable gaming quick-control overlay" />\n        </service>`;
if (!manifest.includes('android:name=".GameOverlayService"')) {
  manifest = manifest.replace(/<application\b([^>]*)>/, match => `${match}\n${serviceTag}`);
}
await writeFile(manifestPath, manifest);

// Set the real app label and install the user's SIN KARM1S launcher icon.
const stringsPath = resolve(root, 'android/app/src/main/res/values/strings.xml');
try {
  let strings = await readFile(stringsPath, 'utf8');
  if (/<string name="app_name">[\s\S]*?<\/string>/.test(strings)) {
    strings = strings.replace(/<string name="app_name">[\s\S]*?<\/string>/, '<string name="app_name">SIN KARM1S</string>');
  } else {
    strings = strings.replace('</resources>', '    <string name="app_name">SIN KARM1S</string>\n</resources>');
  }
  await writeFile(stringsPath, strings);
} catch {
  console.warn('Could not update app_name resource; Capacitor config still sets SIN KARM1S.');
}
manifest = await readFile(manifestPath, 'utf8');
manifest = manifest.replace(/<application\b([^>]*)>/, (full, attrs) => {
  let next = attrs.replace(/\s+android:label="[^"]*"/, '');
  next = next.replace(/\s+android:icon="[^"]*"/, '');
  next = next.replace(/\s+android:roundIcon="[^"]*"/, '');
  return `<application${next} android:label="@string/app_name" android:icon="@mipmap/ic_launcher" android:roundIcon="@mipmap/ic_launcher_round">`;
});
await writeFile(manifestPath, manifest);
const iconSource = resolve(root, 'native/android/launcher-icons');
const resRoot = resolve(root, 'android/app/src/main/res');
for (const density of ['mdpi','hdpi','xhdpi','xxhdpi','xxxhdpi']) {
  await mkdir(resolve(resRoot, `mipmap-${density}`), { recursive: true });
  await cp(resolve(iconSource, `mipmap-${density}`), resolve(resRoot, `mipmap-${density}`), { recursive: true, force: true });
}
await mkdir(resolve(resRoot, 'mipmap-anydpi-v26'), { recursive: true });
await cp(resolve(iconSource, 'mipmap-anydpi-v26'), resolve(resRoot, 'mipmap-anydpi-v26'), { recursive: true, force: true });
await mkdir(resolve(resRoot, 'drawable-nodpi'), { recursive: true });
await cp(resolve(iconSource, 'drawable-nodpi/ic_launcher_foreground.png'), resolve(resRoot, 'drawable-nodpi/ic_launcher_foreground.png'));

await mkdir(dirname(mainPath), { recursive: true });
const mainActivity = `package id.sinkarm1s.gamingspace;\n\nimport android.os.Bundle;\nimport com.getcapacitor.BridgeActivity;\n\npublic class MainActivity extends BridgeActivity {\n    @Override\n    public void onCreate(Bundle savedInstanceState) {\n        registerPlugin(GameOverlayPlugin.class);\n        super.onCreate(savedInstanceState);\n    }\n}\n`;
await writeFile(mainPath, mainActivity);
await copyFile(resolve(nativeDir, 'GameOverlayPlugin.java'), resolve(dirname(mainPath), 'GameOverlayPlugin.java'));
await copyFile(resolve(nativeDir, 'GameOverlayService.java'), resolve(dirname(mainPath), 'GameOverlayService.java'));
console.log('Installed Android GameOverlay plugin/service and overlay permissions.');
