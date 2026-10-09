const CACHE_NAME = 'sin-karm1s-shell-v4';
const APP_SHELL = ['./', './index.html', './manifest.webmanifest', './assets/icon-192.png', './assets/icon-512.png', './assets/games/free-fire.png', './assets/games/free-fire-max.png', './assets/games/mobile-legends.png', './assets/games/pubg-mobile.png', './assets/games/cod-mobile.png', './assets/icons/home.png', './assets/icons/grid.png', './assets/icons/tool.png', './assets/icons/history.png', './assets/icons/settings.png', './assets/icons/search.png', './assets/icons/gamepad.png', './assets/icons/zap.png', './assets/icons/wifi.png', './assets/icons/target.png', './assets/icons/timer.png', './assets/icons/battery.png', './assets/icons/monitor.png', './assets/icons/mic.png', './assets/icons/volume.png', './assets/icons/cpu.png', './assets/icons/shield.png', './assets/icons/book.png', './assets/icons/plus.png', './assets/icons/x.png', './assets/icons/chevron.png', './assets/icons/star.png', './assets/icons/clock.png', './assets/icons/download.png', './assets/icons/upload.png', './assets/icons/trash.png', './assets/icons/chart.png', './assets/icons/sliders.png', './assets/icons/moon.png', './assets/icons/sun.png', './assets/icons/scan.png', './assets/icons/refresh.png', './assets/icons/activity.png', './assets/icons/file.png', './assets/icons/calculator.png', './assets/icons/keyboard.png', './assets/icons/sparkles.png', './assets/icons/info.png', './assets/icons/waves.png', './assets/icons/swipe.png', './assets/icons/save.png'];
self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE_NAME).then(cache => cache.addAll(APP_SHELL)));
  self.skipWaiting();
});
self.addEventListener('activate', event => {
  event.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(key => key !== CACHE_NAME).map(key => caches.delete(key)))));
  self.clients.claim();
});
self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET' || new URL(event.request.url).origin !== self.location.origin) return;
  event.respondWith(caches.match(event.request).then(cached => cached || fetch(event.request).then(response => {
    const copy = response.clone(); caches.open(CACHE_NAME).then(cache => cache.put(event.request, copy)); return response;
  }).catch(() => caches.match('./index.html'))));
});
