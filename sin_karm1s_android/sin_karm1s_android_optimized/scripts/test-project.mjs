import { readFile, access } from 'node:fs/promises';
import { resolve } from 'node:path';
import vm from 'node:vm';

const root = resolve('.');
const html = await readFile(resolve(root, 'index.html'), 'utf8');
const scriptMatch = html.match(/<script>([\s\S]*?)<\/script>/);
if (!scriptMatch) throw new Error('No inline application script found.');
new vm.Script(scriptMatch[1], { filename: 'index.html:inline-script' });
console.log('PASS: inline JavaScript syntax');

const mainStart = html.indexOf('const features=[');
const mainEnd = html.indexOf('\n];', mainStart);
const catalogStart = html.indexOf('const featureCatalog=[');
const catalogEnd = html.indexOf('\n];', catalogStart);
if (mainStart < 0 || mainEnd < 0 || catalogStart < 0 || catalogEnd < 0) throw new Error('Could not find curated features/catalog.');
const mainSource = html.slice(mainStart, mainEnd);
const catalogSource = html.slice(catalogStart, catalogEnd);
const mainIds = [...mainSource.matchAll(/\{id:'([^']+)'/g)].map(m => m[1]);
const catalogIds = [...catalogSource.matchAll(/\{id:'([^']+)'/g)].map(m => m[1]);
if (mainIds.length !== 20) throw new Error(`Expected exactly 20 main features; found ${mainIds.length}.`);
if (new Set(mainIds).size !== mainIds.length) throw new Error('Duplicate main feature IDs found.');
if (new Set(catalogIds).size !== catalogIds.length) throw new Error('Duplicate detailed feature IDs found.');
console.log(`PASS: exactly ${mainIds.length} main feature groups`);
console.log(`PASS: ${catalogIds.length} detailed tools retained inside groups`);

const allTypes = [...mainSource.matchAll(/type:'([^']+)'/g), ...catalogSource.matchAll(/type:'([^']+)'/g)].map(m => m[1]);
const renderTool = html.slice(html.indexOf('function renderTool('), html.indexOf('function renderProfiles('));
const handled = new Set([...renderTool.matchAll(/f\.type==='([^']+)'/g)].map(m => m[1]));
const unsupported = [...new Set(allTypes.filter(t => !handled.has(t)))];
if (unsupported.length) throw new Error(`Feature type without a render handler: ${unsupported.join(', ')}`);
if (!renderTool.includes("f.type==='hub'") || !renderTool.includes("f.type==='corner'")) throw new Error('Game Corner / grouped-feature handlers missing.');
console.log('PASS: main and child feature types have render handlers');
if (!html.includes('AX-MODE') || !html.includes('ax-control-grid') || !html.includes('cornerGameStrip')) throw new Error('Game Corner dashboard/control panel missing.');
console.log('PASS: Game Corner dashboard and AX-MODE panel present');
if (!html.includes('native-overlay-show') || !html.includes('consumeNativeOverlayAction')) throw new Error('Native floating-overlay button/bridge hook missing.');
console.log('PASS: native floating-overlay button/bridge hook present');
if (!html.includes('getInstalledApps') || !html.includes('data-add-installed-app') || !html.includes('refresh-installed-games')) throw new Error('Installed-app launcher scan and add flow missing.');
if (!html.includes('data-action=\"crosshair-toggle\"') || !html.includes('setCrosshair')) throw new Error('Crosshair overlay control missing.');
console.log('PASS: installed-app scan/add UI and crosshair overlay UI present');
for (const file of ['native/android/GameOverlayPlugin.java','native/android/GameOverlayService.java','scripts/install-native-overlay.mjs']) await access(resolve(root,file));
const nativePlugin = await readFile(resolve(root,'native/android/GameOverlayPlugin.java'),'utf8');
const nativeService = await readFile(resolve(root,'native/android/GameOverlayService.java'),'utf8');
const installer = await readFile(resolve(root,'scripts/install-native-overlay.mjs'),'utf8');
for (const method of ['getInstalledApps','launchGame','setCrosshair','getCrosshairState']) if (!nativePlugin.includes('void '+method+'(')) throw new Error(`Native plugin method missing: ${method}`);
if (!nativeService.includes('class CrosshairView') || !nativeService.includes('showCollapsedHandle') || !nativeService.includes('ACTION_SET_CROSSHAIR')) throw new Error('Native crosshair or collapsible AX-MODE implementation missing.');
if (!installer.includes('android.intent.action.MAIN') || !installer.includes('android.intent.category.LAUNCHER')) throw new Error('Android launcher package visibility queries missing.');
console.log('PASS: native installed-app API, crosshair window, collapsible panel, and Android package visibility source checks');
console.log('PASS: native overlay source and install script exist');
const pkg = JSON.parse(await readFile(resolve(root, 'package.json'), 'utf8'));
if (!pkg.scripts['android:overlay'] || !pkg.scripts['android:add'].includes('android:overlay')) throw new Error('Android build does not install the native overlay.');
console.log('PASS: Android build hooks include native overlay installer');

if (/[\u{1F300}-\u{1FAFF}]/u.test(html)) throw new Error('Emoji code points found in UI source.');
console.log('PASS: no emoji code points in UI source');

const manifest = JSON.parse(await readFile(resolve(root, 'manifest.webmanifest'), 'utf8'));
if (manifest.display !== 'standalone') throw new Error('PWA display mode must be standalone.');
console.log('PASS: PWA manifest JSON');
new vm.Script(await readFile(resolve(root, 'service-worker.js'), 'utf8'), { filename: 'service-worker.js' });
console.log('PASS: service worker JavaScript syntax');

const required = ['assets/icon-192.png','assets/icon-512.png','assets/sin-karm1s-logo-original.png','assets/icons/target.png','assets/games/free-fire.png'];
for (const file of required) await access(resolve(root,file));
console.log('PASS: required PNG assets exist');
console.log('NOTE: these are static/source checks; they do not replace an APK build or real-device testing.');
