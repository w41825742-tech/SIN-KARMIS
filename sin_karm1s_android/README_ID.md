# SIN KARM1S Gaming Space — proyek PWA + sumber Android

## Perubahan versi ini
- **GAME CORNER / game launcher** menyatukan daftar game terpasang, favorit, sesi, latihan, dan AX-MODE.
- Pada **APK native**, launcher membaca aplikasi yang memiliki activity launcher Android, mendeteksi game berdasarkan kategori Android dan nama/package umum, lalu mengisi library dari game yang terpasang. Fitur scan ini tidak tersedia pada PWA/preview browser karena browser tidak boleh membaca daftar APK perangkat.
- **Tambah game/APK** menyediakan daftar aplikasi terpasang yang bisa dicari dan ditambahkan ke library, plus form manual untuk nama serta package ID.
- Menekan game dari launcher menggunakan native Android untuk membuka aplikasi itu langsung. Jika izin overlay tersedia, AX-MODE dinyalakan sebelum game tampil.
- **AX-MODE** memakai panel merah-putih di kanan atas yang dapat digeser lewat header. Tombol HIDE mengecilkannya menjadi handle AX; ketuk handle untuk membuka panel lagi. Panel berisi Crosshair, Network, Touch Lab, Aim Lab, Session, dan Open App.
- **Crosshair** punya preview, ukuran, ketebalan, dan pilihan warna. Di APK native tombol aktifkan menggambar crosshair di tengah layar aplikasi lain; ukuran dan warna disimpan.
- Ikon native bernama **SIN KARM1S**. Fitur native membutuhkan izin Android yang sesuai.

## Cara memakai preview / PWA di Android
1. Untuk preview lokal, buka `index.html` setelah mengekstrak ZIP.
2. Untuk memasang sebagai aplikasi web, upload isi folder proyek ke hosting HTTPS.
3. Buka URL dari Chrome Android, lalu pilih **Instal aplikasi** atau **Tambahkan ke layar utama** jika tersedia.
4. PWA dapat menambah game manual, tetapi tidak dapat memindai APK, membuka semua package Android dengan jaminan, atau menampilkan overlay di atas game lain.

## Membangun APK native untuk Android 10 ke atas
1. Pada komputer, instal Node.js LTS, Java 17, Android Studio, dan Android SDK/Build Tools.
2. Ekstrak ZIP, jalankan `npm install`, lalu `npm test`.
3. Jalankan `npm run android:add` satu kali untuk membuat project Android Capacitor, menetapkan minimum Android API 29, menambahkan package-visibility query, serta memasang plugin/service AX-MODE native.
4. Untuk sinkronisasi source, jalankan `npm run android:sync`; untuk APK debug, jalankan `npm run android:build`.
5. APK debug untuk pengujian. APK release membutuhkan signing key milikmu.

### Build melalui GitHub Actions
1. Upload seluruh isi folder proyek ke repository GitHub.
2. Buka tab **Actions**, pilih **Build SIN KARM1S Android APK**, lalu pilih **Run workflow**.
3. Jika workflow sukses, unduh artifact `SIN-KARM1S-Android10-debug-apk` dan ekstrak `app-debug.apk`.

## Izin dan batasan yang perlu diketahui
- Pertama kali memakai AX-MODE atau crosshair global, aktifkan izin **Tampil di atas aplikasi lain** untuk SIN KARM1S. Kalau izin belum diberikan, game tetap dibuka tetapi overlay tidak dapat ditampilkan sampai izin diaktifkan.
- AX-MODE hanya otomatis muncul saat game dibuka melalui launcher SIN KARM1S. Game yang dibuka sendiri dari ikon Android tidak dipantau otomatis oleh aplikasi ini.
- Tes ping mengukur request jaringan dari browser, bukan ping server game. FPS browser bukan FPS Free Fire; aplikasi ini tidak mengklaim meningkatkan FPS atau mengubah RAM/ping secara ajaib.
- Daftar otomatis bergantung pada label/kategori aplikasi Android. Game yang tidak terdeteksi tetap bisa ditambahkan dari **Tambah Game → Aplikasi Terpasang di HP**.
- Source ini belum termasuk APK final yang sudah diuji pada perangkat fisik; izin overlay, posisi panel, serta perilaku OEM perlu diuji pada HP target.

## Data
Library game, catatan, checklist, profil, setting crosshair, statistik, dan riwayat sesi disimpan lokal pada perangkat/browser. Tidak ada akun atau sinkronisasi server. Gunakan **Backup & Restore** secara berkala.
