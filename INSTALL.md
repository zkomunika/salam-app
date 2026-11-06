# Panduan Pemasangan — Pantau Izin Santri

Dokumen ini menjelaskan langkah-langkah memasang dan menyiapkan aplikasi **Pantau Izin Santri** pada perangkat Android (khususnya Vivo 1819 / Android 9 ke atas).

---

## 1. Aktifkan Opsi Pengembang dan Debugging USB di HP

1. Buka aplikasi **Pengaturan (Settings)** di HP.
2. Masuk ke **Pengaturan Lainnya (More settings)** → **Tentang Ponsel (About phone)**.
3. Ketuk **Versi Perangkat Lunak (Software version)** sebanyak **7 kali** berturut-turut hingga muncul pesan bahwa Opsi Pengembang telah aktif.
4. Kembali ke **Pengaturan Lainnya** → buka **Opsi Pengembang (Developer options)**.
5. Aktifkan sakelar **Opsi Pengembang**, lalu aktifkan **Debugging USB (USB debugging)** dan **Instal via USB** (jika ada).

---

## 2. Pasang APK melalui ADB

1. Hubungkan HP ke komputer menggunakan kabel data USB, lalu pilih mode **Transfer File** dan setujui dialog *Izinkan Debugging USB* di layar HP.
2. Pastikan berkas APK hasil build tersedia di:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```
3. Jalankan perintah berikut dari terminal di direktori proyek:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
   > Atau salin langsung berkas `app-debug.apk` ke penyimpanan HP, buka dari aplikasi *Pengelola File*, dan pilih **Pasang (Install)**.

---

## 3. Izinkan Notifikasi

1. Buka aplikasi **Pantau Izin Santri** untuk pertama kali.
2. Saat diminta izin notifikasi (pada Android 13 ke atas), ketuk **Izinkan (Allow)**.
3. Pada Android 9–12, pastikan notifikasi untuk **Pantau Izin Santri** dan saluran **"Santri terlambat"** dalam keadaan aktif di **Pengaturan** → **Notifikasi dan Bilah Status**.

---

## 4. Aktifkan "Mulai Otomatis" dan Nonaktifkan Penghematan Baterai (Penting untuk HP Vivo)

HP Vivo secara bawaan membatasi aplikasi di latar belakang sehingga alarm pengingat harian berisiko tertunda bila pengaturan ini belum diaktifkan:

1. **Nonaktifkan Pembatasan Baterai:**
   - Buka tab **Pengaturan** di dalam aplikasi **Pantau Izin Santri**, lalu ketuk tombol **Buka pengaturan baterai**; **atau**
   - Buka **Pengaturan HP** → **Baterai** → **Konsumsi Daya Latar Belakang Tinggi (High background power consumption)** → aktifkan sakelar untuk **Pantau Izin Santri**.
2. **Aktifkan Izin Mulai Otomatis (Autostart):**
   - Buka **Pengaturan HP** → **Pengaturan Lainnya** → **Pengelola Aplikasi (Applications)** → **Mulai Otomatis (Autostart)** (atau melalui aplikasi bawaan **i Manager** → **Manajer Aplikasi** → **Manajer Izin** → **Mulai Otomatis**).
   - Aktifkan sakelar untuk **Pantau Izin Santri** agar jadwal sapuan harian otomatis dijadwalkan ulang saat HP dinyalakan kembali (*restart*).
