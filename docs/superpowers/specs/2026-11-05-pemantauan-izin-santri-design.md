# Pantau Izin Santri — Spec Desain

Tanggal: 2026-11-05
Status: revisi 2 (2026-11-05), disetujui pemilik

## 1. Tujuan dan konteks

Aplikasi Android untuk **satu pengguna**: pengurus asrama yang bertanggung jawab atas izin pulang santri. Santri yang ingin pulang meminta izin kepada pengurus dan diberi batas waktu. Aplikasi ini membantu pengurus:

1. Mencatat izin pulang beserta batas waktunya.
2. Memantau siapa yang sudah kembali, tepat waktu atau terlambat.
3. Diingatkan lewat notifikasi saat santri melewati batas waktu dan belum kembali.
4. Mencatat konfirmasi wali (alasan keterlambatan, bukti, tanggal kembali baru).
5. Mengirim pesan pemberitahuan ke wali dari template yang terisi otomatis.

Konsekuensi keterlambatan (hukuman atau keringanan) **tidak** dicatat di sistem. Aplikasi hanya menampilkan siapa yang terlambat dan siapa yang sudah/belum dikonfirmasi wali, agar pengurus bisa menindaklanjuti sendiri.

Kriteria keberhasilan:
- Pengurus tidak melewatkan santri yang terlambat, walau aplikasi sedang ditutup.
- Dari notifikasi ke pesan siap kirim untuk wali butuh paling banyak 3 ketukan.
- Perpanjangan berulang dari wali tetap tercatat utuh sebagai riwayat.

## 2. Ruang lingkup

Di dalam cakupan: semua yang ada di bagian 3 sampai 9.

Di luar cakupan: server atau backend, login, banyak pengguna, impor ulang data santri dari file Excel di dalam aplikasi, pengiriman otomatis ke WhatsApp/Telegram, pencatatan hukuman, cadangan data (backup) ke file, dan dukungan iPhone. Cadangan data adalah kandidat fitur berikutnya karena data hanya ada di HP.

## 3. Platform dan teknologi

- Android native: Kotlin, Jetpack Compose (Material 3), Room (SQLite lokal).
- `minSdk` 28 (Android 9, sesuai HP pengguna: Vivo 1819, Android 9, RAM 5,6 GB). `targetSdk` dan `compileSdk` memakai versi stabil terbaru di SDK terpasang.
- Waktu dan tanggal memakai `java.time` (tersedia penuh di API 28).
- Seluruh data lokal. Tidak ada akses internet; aplikasi tidak meminta izin `INTERNET`.
- Bahasa antarmuka: Indonesia. Format tanggal: lokal `id-ID`, contoh "4 November 2026".

## 4. Model data

### 4.0 `Santri` (data master)

| Kolom | Tipe | Aturan |
|---|---|---|
| `id` | Long (auto) | |
| `nama` | String | wajib, unik (tanpa membedakan huruf besar-kecil), disimpan seperti di data master pondok (huruf kapital) |
| `kamar` | String | teks bebas; wajib, tidak boleh kosong |
| `aktif` | Boolean | `true` untuk santri yang bisa dipilih di form; `false` bila dinonaktifkan |

Sumber awal: berkas `app/src/main/assets/santri_master.csv` (kolom `nama,kamar`, UTF-8, 275 baris, diturunkan dari `Data_Santri_aktif_2026-11-03.xlsx`). **Hanya `nama` dan `kamar` yang diambil**; kolom lain di Excel (NIS, JK, asrama, sekolah, kelas, No. WA Ortu, alamat) tidak diimpor. Berkas dimuat ke tabel sekali, saat tabel `Santri` masih kosong (pembukaan aplikasi pertama). Baris dengan nama atau kamar kosong dilewati. Santri yang bukan penghuni asrama (pengurus wisma) tidak dimasukkan; spasi ganda dan spasi di tepi nama dirapikan.

### 4.1 `Izin`

| Kolom | Tipe | Aturan |
|---|---|---|
| `id` | Long (auto) | |
| `santriId` | Long | wajib; foreign key ke `Santri`. Nama dan kamar selalu dibaca dari data master, tidak disalin ke `Izin` |
| `tanggalPulang` | LocalDate | wajib; boleh tanggal lampau, karena ada santri yang sudah pulang sebelum aplikasi dipakai |
| `alasanJenis` | enum `SAKIT`, `LAIN_LAIN` | wajib |
| `alasanDetail` | String | wajib; nama penyakit jika `SAKIT`, keperluan jika `LAIN_LAIN` |
| `batasJenis` | enum `HARI_1`, `HARI_2`, `HARI_3`, `LEBIH_3_HARI` | wajib |
| `batasAwal` | LocalDate | dihitung, lihat 4.4 |
| `noWaWali` | String? | opsional; disimpan dalam format ternormalisasi (lihat 6.2) |
| `tanggalKembali` | LocalDate? | null selama belum kembali |
| `dibuatPada` | Instant | |

### 4.2 `Konfirmasi` (nol atau lebih per `Izin`)

| Kolom | Tipe | Aturan |
|---|---|---|
| `id` | Long (auto) | |
| `izinId` | Long | foreign key, hapus berantai saat `Izin` dihapus |
| `tanggalKembaliBaru` | LocalDate? | wajib jika santri belum kembali; null jika sudah kembali; saat disimpan harus hari ini atau setelahnya |
| `catatan` | String? | opsional |
| `buktiPath` | String? | opsional; path relatif file salinan di penyimpanan privat aplikasi |
| `dibuatPada` | Instant | |

### 4.3 `Notifikasi` (riwayat)

| Kolom | Tipe | Aturan |
|---|---|---|
| `id` | Long (auto) | |
| `izinId` | Long | foreign key, hapus berantai |
| `jenis` | enum `LEWAT_BATAS`, `LEWAT_TANGGAL_BARU`, `PENGINGAT_HARIAN` | |
| `pesan` | String | teks yang ditampilkan saat itu |
| `tanggalKey` | LocalDate | tanggal sapuan yang membuatnya, untuk mencegah duplikat |
| `dibuatPada` | Instant | |
| `dibaca` | Boolean | awal `false` |

Batasan unik: (`izinId`, `tanggalKey`). Artinya satu santri paling banyak mendapat satu notifikasi per hari.

### 4.4 Aturan turunan (semua perbandingan tingkat tanggal, bukan jam)

- `batasAwal` untuk `HARI_1`/`HARI_2`/`HARI_3` = `tanggalPulang` + 1/2/3 hari. Contoh: pulang 1 November, izin 3 hari, `batasAwal` = 4 November.
- Untuk `LEBIH_3_HARI`, pengurus memilih `batasAwal` langsung dari kalender; nilainya harus **setelah** `tanggalPulang` + 3 hari.
- `batasAktif` = nilai terbesar antara `batasAwal` dan `tanggalKembaliBaru` dari seluruh konfirmasinya.
- Santri **terlambat** jika `tanggalKembali` null dan hari ini **setelah** `batasAwal` (jadi pada tanggal `batasAwal` sendiri santri belum terlambat).
- Santri **kembali terlambat** jika `tanggalKembali` setelah `batasAwal`. Perpanjangan dari konfirmasi **tidak** mengubah ini: statusnya tetap terlambat.
- **Perlu tindak lanjut** jika belum kembali dan hari ini setelah `batasAktif`.
- Semua aturan di atas dihitung terhadap **tanggal sistem hari ini**, bukan tanggal saat data diinput. Izin dengan `tanggalPulang` lampau (santri yang pulang sebelum aplikasi dipakai) langsung berstatus terlambat jika `batasAwal` sudah lewat.
- Satu santri hanya boleh punya **satu izin aktif** (belum kembali) pada satu waktu.

### 4.5 Status tampilan (hasil `StatusResolver`)

| Status | Kondisi |
|---|---|
| `IZIN_BERJALAN` | belum kembali, hari ini ≤ `batasAwal` |
| `TERLAMBAT_BELUM_KONFIRMASI` | belum kembali, hari ini > `batasAwal`, tidak ada konfirmasi |
| `TERLAMBAT_TERKONFIRMASI` | belum kembali, hari ini > `batasAwal`, ada konfirmasi, hari ini ≤ `batasAktif` |
| `TERLAMBAT_KONFIRMASI_HABIS` | belum kembali, ada konfirmasi, hari ini > `batasAktif` (perlu konfirmasi ulang) |
| `KEMBALI_TEPAT` | sudah kembali, `tanggalKembali` ≤ `batasAwal` |
| `KEMBALI_TERLAMBAT` | sudah kembali, `tanggalKembali` > `batasAwal` |

Satu santri hanya punya satu status. Jumlah hari terlambat = `tanggalKembali` (atau hari ini jika belum kembali) dikurangi `batasAwal`.

## 5. Layar

Navigasi bawah dengan 4 tab: **Izin**, **Terlambat**, **Notifikasi**, **Pengaturan**. Tombol tambah (+) di tab Izin membuka form.

### 5.1 Tab Izin (daftar)
- Daftar kartu, terbaru di atas. Filter: Semua, Izin berjalan, Terlambat, Sudah kembali.
- Kartu menampilkan: nama, kamar, alasan, batas aktif, lencana status.
- **Checklist kembali**: kotak centang "Sudah kembali".
  - Ketuk: dialog meminta `tanggalKembali` (default hari ini, bisa diubah) lalu simpan.
  - Hijau saja untuk `KEMBALI_TEPAT`. Hijau dengan lencana "Terlambat N hari" untuk `KEMBALI_TERLAMBAT`.
  - Ketuk lagi pada santri yang sudah dicentang: dialog konfirmasi untuk membatalkan (membuat `tanggalKembali` null kembali).
- **Checkbox konfirmasi**: hanya tampil untuk santri berstatus terlambat (belum kembali maupun kembali terlambat). Tercentang jika santri punya minimal satu konfirmasi. Ketuk membuka form konfirmasi (5.4).
- Tombol **Edit** di kartu membuka form edit (5.2).

### 5.2 Form izin (tambah dan edit)
- **Kamar** (dropdown) dan **Nama santri** (kolom teks dengan daftar saran). Dropdown Kamar berisi "Semua kamar" lalu kamar yang ada di data master, **terurut numerik dari 1** (1, 2, 3, … 10, 11, bukan urutan abjad). Memilih kamar mempersempit saran nama ke kamar itu. Mengetik di kolom Nama memfilter santri aktif (cocok sebagian, tanpa membedakan huruf besar-kecil) dan menampilkan saran berisi nama dan kamar. Memilih nama mengisi dropdown Kamar otomatis ke kamar santri itu. Hanya santri dari data master yang bisa dipilih; kamar pada izin selalu mengikuti data master (untuk mengubahnya, edit di layar Data Santri).
- Jika santri yang dipilih masih punya izin aktif, form menampilkan peringatan "Santri ini masih tercatat izin dan belum kembali" dengan tombol membuka izin tersebut, dan penyimpanan ditahan.
- Kolom lain: tanggal pulang (kalender, default hari ini, boleh tanggal lampau), alasan (dropdown `Sakit` / `Lain-lain`), detail alasan (kolom teks; label "Nama penyakit" atau "Keperluan pulang" mengikuti pilihan), batas waktu (dropdown `1 hari`, `2 hari`, `3 hari`, `>3 hari`), nomor WA wali (opsional).
- **Pratinjau batas kembali**: begitu tanggal pulang dan batas waktu terisi, form menampilkan "Batas kembali: {tanggal}". Jika tanggal itu sudah lewat dibanding hari ini, teks berubah menjadi peringatan "Sudah lewat N hari, santri akan langsung tercatat terlambat".
- Jika `>3 hari`: kalender "Tanggal kembali" muncul dan wajib diisi, harus setelah tanggal pulang + 3 hari.
- Di mode edit semua kolom bisa diubah, termasuk nomor WA wali yang baru didapat belakangan. Mengubah tanggal pulang atau batas menghitung ulang `batasAwal`. Ada tombol **Hapus** dengan dialog konfirmasi; hapus juga menghapus konfirmasi, riwayat notifikasi, dan file bukti santri itu.
- Validasi sesuai tabel 4.1 dan 6.2. Pesan error ditampilkan di bawah kolom yang salah.

### 5.3 Tab Terlambat
- Berisi santri berstatus `TERLAMBAT_BELUM_KONFIRMASI`, `TERLAMBAT_TERKONFIRMASI`, dan `TERLAMBAT_KONFIRMASI_HABIS` (semua yang belum kembali dan sudah lewat `batasAwal`).
- Urutan: yang belum konfirmasi atau konfirmasinya habis di atas, lalu yang terkonfirmasi.
- Kartu menampilkan nama, kamar, jumlah hari terlambat, status konfirmasi, dan riwayat singkat konfirmasi (tanggal kembali baru tiap konfirmasi).
- Tiga aksi di tiap kartu: **Kirim template** (6), **Konfirmasi** (5.4), **Tandai kembali** (dialog tanggal seperti 5.1).
- Ini juga layar tujuan saat notifikasi diketuk.

### 5.4 Form konfirmasi
- Kolom: tanggal kembali baru (kalender; wajib jika santri belum kembali, tidak ada jika sudah kembali), bukti file (opsional; gambar atau PDF dari pemilih file), catatan (opsional).
- File bukti disalin ke penyimpanan privat aplikasi (`filesDir/bukti/`); aplikasi tidak bergantung pada file asli.
- Menyimpan membuat satu baris `Konfirmasi` baru. Konfirmasi lama tidak diubah (menjadi riwayat).

### 5.5 Tab Notifikasi
- Daftar riwayat, terbaru di atas, dengan penanda belum dibaca.
- Filter waktu: Hari ini, 7 hari, 30 hari, dan rentang tanggal pilihan sendiri.
- Ketuk satu item: menandai dibaca dan membuka tab Terlambat.

### 5.6 Tab Pengaturan
- **Jam pengingat harian**: pemilih jam format 24 jam, default **08:00**, dapat diubah kapan saja. Perubahan berlaku mulai sapuan berikutnya sesuai 7.3.
- **Data santri**: membuka layar 5.7.
- **Template pesan** varian A dan B (6.1), bisa diedit, dengan tombol "Kembalikan bawaan".
- **Petunjuk baterai**: penjelasan singkat dan tombol "Buka pengaturan baterai" (`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`). Teks petunjuk menyebut bahwa HP Vivo biasa menghentikan aplikasi di latar belakang, sehingga izin "mulai otomatis" dan "tanpa pembatasan baterai" perlu diaktifkan manual.

### 5.7 Data Santri (di Pengaturan)
- Daftar santri dengan pencarian. Setiap baris menampilkan nama, kamar, dan lencana "Nonaktif" jika perlu.
- **Tambah santri**: nama dan kamar, keduanya wajib; nama tidak boleh kembar dengan yang ada.
- **Ubah**: nama dan kamar (misalnya santri pindah kamar).
- **Nonaktifkan / aktifkan kembali**: santri nonaktif tidak muncul di pilihan form izin. Riwayat izinnya tetap ada. Santri tidak dihapus dari aplikasi.

## 6. Template pesan dan pengiriman

### 6.1 Isi template (bawaan; dapat diedit)

Penampung nilai: `{nama}`, `{kamar}`, `{batas_awal}`, `{batas_aktif}` (tanggal berformat "4 November 2026").

**Varian A** (dipakai untuk `TERLAMBAT_BELUM_KONFIRMASI`):

```
Assalamualaikum Warahmatullahi Wabarakatuh.

Kami dari Pengurus Asrama hendak memberitahukan bahwa putra Bapak/Ibu yang bernama {nama} (kamar {kamar}) telah melewati batas waktu izin pulang yang ditentukan, yaitu {batas_awal}, dan sampai saat ini belum kembali ke pondok.

Mohon Bapak/Ibu berkenan memberikan keterangan mengenai keterlambatan tersebut serta kapan putra Bapak/Ibu akan kembali.

Atas perhatian dan kerja samanya, kami ucapkan terima kasih.
Wassalamualaikum Warahmatullahi Wabarakatuh.
```

**Varian B** (dipakai untuk `TERLAMBAT_TERKONFIRMASI` dan `TERLAMBAT_KONFIRMASI_HABIS`):

```
Assalamualaikum Warahmatullahi Wabarakatuh.

Kami dari Pengurus Asrama hendak memberitahukan bahwa putra Bapak/Ibu yang bernama {nama} (kamar {kamar}) sebelumnya telah dikonfirmasi akan kembali paling lambat {batas_aktif}, dan sampai saat ini belum kembali ke pondok.

Mohon Bapak/Ibu berkenan memberikan keterangan terbaru mengenai kondisi putra Bapak/Ibu serta kapan akan kembali.

Atas perhatian dan kerja samanya, kami ucapkan terima kasih.
Wassalamualaikum Warahmatullahi Wabarakatuh.
```

### 6.2 Nomor WA wali dan tombol "Kirim template"
- Normalisasi: buang semua karakter non-angka; awalan `0` diganti `62`; awalan `62` dipertahankan; awalan `8` diberi `62` di depan. Hasil valid jika berawalan `62` dan panjang 10 sampai 15 digit. Kolom kosong dianggap tidak ada nomor (valid).
- Jika ada nomor: buka `https://wa.me/{nomor}?text={teks_url_encoded}` lewat `ACTION_VIEW`. Pengurus tinggal menekan kirim di WhatsApp.
- Jika tidak ada nomor: teks disalin ke papan klip (disertai pesan singkat "Teks disalin") dan menu bagikan sistem (`ACTION_SEND`, `text/plain`) dibuka.
- Tombol hanya ada pada santri yang belum kembali.

## 7. Notifikasi dan penjadwalan

### 7.1 Sapuan harian (satu mekanisme untuk semua)
Satu alarm harian pada jam pengingat (default 08:00) menjalankan **sapuan**:

1. Ambil semua izin yang belum kembali dan "perlu tindak lanjut" (4.4).
2. Untuk tiap santri yang belum punya notifikasi berkunci (`izinId`, hari ini): buat satu notifikasi.
   - `LEWAT_BATAS`: santri belum punya konfirmasi **dan** belum pernah ada notifikasi apa pun untuk santri ini.
   - `LEWAT_TANGGAL_BARU`: santri punya konfirmasi **dan** tidak ada notifikasi dengan `tanggalKey` setelah `batasAktif` saat ini.
   - `PENGINGAT_HARIAN`: semua kasus lain (sudah pernah dinotifikasi untuk periode ini dan masih perlu tindak lanjut).
3. Simpan ke riwayat dan tampilkan notifikasi sistem.

Konsekuensi: santri yang batas waktunya 4 November dinotifikasi pada 5 November pukul 08:00. Setelah pengurus menyimpan konfirmasi dengan tanggal baru, pengingat harian berhenti sampai tanggal baru itu terlewat, lalu `LEWAT_TANGGAL_BARU` muncul sekali dan pengingat harian berlanjut hingga ada konfirmasi baru atau santri ditandai kembali.

### 7.2 Teks notifikasi
- `LEWAT_BATAS`: `{nama} kamar {kamar} sudah melewati batas waktu izin`
- `LEWAT_TANGGAL_BARU`: `{nama} kamar {kamar} sudah melewati tanggal kembali yang dikonfirmasi`
- `PENGINGAT_HARIAN`: `{nama} kamar {kamar} belum kembali dan belum ada konfirmasi terbaru`

Satu notifikasi sistem per santri (id notifikasi turunan dari `izinId`). Mengetuknya membuka tab Terlambat. Saluran notifikasi: "Santri terlambat", prioritas tinggi.

### 7.3 Menjalankan sapuan
- Alarm harian dengan `AlarmManager.setExactAndAllowWhileIdle`, dijadwalkan ulang setiap kali sapuan selesai. Jika izin alarm tepat (Android 12+) tidak diberikan, pakai `setAndAllowWhileIdle`.
- Dijadwalkan ulang setelah HP restart (`BOOT_COMPLETED`) dan setelah jam pengingat diubah.
- Waktu sapuan berikutnya (`NextSweepCalculator`): jika jam pengingat masih di depan hari ini dan `lastSweepDate` bukan hari ini, maka hari ini pada jam itu; selain itu besok pada jam itu.
- Sapuan satu hari dijalankan sekali; tanggalnya dicatat di `lastSweepDate` (`SettingsStore`). Jika aplikasi dibuka setelah jam pengingat dan sapuan hari itu belum berjalan (misalnya alarm dimatikan HP), sapuan dijalankan saat itu juga. Batasan unik (`izinId`, `tanggalKey`) mencegah notifikasi ganda.
- Menyimpan atau mengubah izin **tidak** memicu sapuan. Izin yang sudah terlambat saat diinput (misalnya santri yang pulang sebelum aplikasi dipakai) baru dinotifikasi pada sapuan berikutnya, sehingga menginput banyak data sekaligus tidak membanjiri notifikasi. Santri itu tetap langsung terlihat di tab Terlambat.
- Izin `POST_NOTIFICATIONS` diminta saat pertama kali aplikasi dibuka (Android 13+).

## 8. Struktur kode

Paket `app.pantauizin`:

- `domain/` — logika murni tanpa dependensi Android, semua menerima `today: LocalDate` sebagai parameter: `DeadlineCalculator`, `StatusResolver`, `NotificationPlanner`, `MessageTemplateBuilder`, `PhoneNumberNormalizer`, `NextSweepCalculator`, `SantriCsvParser`, `SantriSearch`.
- `data/` — entitas Room, DAO, `AppDatabase`, `IzinRepository`, `SantriRepository`, `SantriSeeder` (memuat `santri_master.csv`), penyimpanan bukti (`BuktiStorage`), `SettingsStore` (jam pengingat, template, `lastSweepDate`).
- `notify/` — `AlarmScheduler`, `SweepReceiver`, `BootReceiver`, `NotificationHelper`.
- `ui/` — layar Compose dan ViewModel per layar: `izin`, `form`, `terlambat`, `konfirmasi`, `notifikasi`, `pengaturan`.

Aturan batas: `ui` dan `notify` memakai `domain` melalui repository; `domain` tidak mengimpor kelas Android.

## 9. Penanganan kesalahan dan pengujian

Penanganan kesalahan:
- Validasi form menahan penyimpanan dan menampilkan pesan di kolom terkait.
- Gagal menyalin file bukti: konfirmasi tidak disimpan, pengurus diberi pesan dan bisa mencoba lagi.
- WhatsApp tidak terpasang atau `wa.me` gagal dibuka: jatuh ke jalur salin dan bagikan teks.
- Izin notifikasi ditolak: banner di tab Terlambat yang menjelaskan bahwa pengingat tidak akan muncul, dengan tombol ke pengaturan.

Pengujian otomatis (unit test pada `domain/`, dengan tanggal tetap):
- `DeadlineCalculator`: 1/2/3 hari, `>3 hari` valid dan tidak valid, melewati pergantian bulan dan tahun.
- `StatusResolver`: semua 6 status, hari persis pada `batasAwal` (belum terlambat), hari setelahnya (terlambat), perpanjangan berulang, kembali tepat di batas, kembali terlambat dengan dan tanpa konfirmasi.
- `NotificationPlanner`: jenis notifikasi yang benar, tidak ganda pada hari yang sama, pengingat berhenti selama konfirmasi aktif, muncul lagi setelah tanggal baru terlewat, tidak ada notifikasi untuk santri yang sudah kembali.
- `PhoneNumberNormalizer`: `0812...`, `+62 812-...`, `62812...`, `812...`, terlalu pendek, berisi huruf, kosong.
- `MessageTemplateBuilder`: pengisian semua penampung, format tanggal Indonesia, pemilihan varian A atau B.
- `SantriCsvParser`: baris header, spasi ganda dan spasi tepi pada nama, kamar kosong (baris dilewati), baris kosong, nama berapostrof (contoh `DRIYA NUR 'AZMI`).
- `SantriSearch`: cocok sebagian nama, tanpa membedakan huruf besar-kecil, tidak ada hasil, santri nonaktif tidak ikut.
- `NextSweepCalculator`: jam di depan hari ini, jam sudah lewat, sapuan hari ini sudah jalan.
- Aturan satu izin aktif per santri, dan izin berstatus terlambat langsung saat `tanggalPulang` lampau.

Pengujian manual di HP target (Vivo, Android 9): 275 santri termuat pada pembukaan pertama dan pencarian terasa lancar; notifikasi muncul saat aplikasi ditutup; alarm tetap hidup setelah restart; tombol Kirim template membuka chat WhatsApp dengan teks terisi; bukti file tersimpan dan bisa dibuka kembali.

## 10. Keputusan desain yang perlu Anda periksa

Ini hal yang tidak Anda sebut secara eksplisit dan saya putuskan sendiri; ubah jika tidak sesuai:

1. Perbandingan batas waktu memakai **tanggal**, bukan jam. Santri baru dianggap terlambat pada hari **setelah** `batasAwal`.
2. Notifikasi muncul sekali sehari pada **jam pengingat (default 08:00)**, bukan tepat di saat batas lewat.
3. Santri yang belum kembali dan sudah lewat `batasAktif` mendapat **pengingat harian** sampai ada konfirmasi baru atau ditandai kembali.
4. Satu santri paling banyak satu notifikasi per hari.
5. Izin yang salah input bisa **dihapus** (beserta turunannya) lewat tombol Edit.
6. Template pesan dianggap untuk **putra** ("putra Bapak/Ibu"), sesuai contoh Anda; teksnya bisa diubah di Pengaturan.

Tambahan pada revisi 1 (perlu Anda periksa):

7. Nama santri **dipilih dari data master** (275 santri), bukan diketik bebas. Hanya `nama` dan `kamar` yang diimpor; kolom No. WA Ortu di Excel (terisi untuk 157 santri) sengaja tidak diambil sesuai permintaan Anda.
8. **JENJEN ABDUSSALAM** tidak dimasukkan ke data master karena ia pengurus wisma, bukan penghuni asrama. Dari 276 baris di Excel, 275 yang diimpor.
9. Satu santri hanya boleh punya satu izin aktif sekaligus.
10. Ada layar **Data Santri** minimal (tambah, ubah kamar, nonaktifkan) agar santri baru atau pindah kamar tidak perlu memperbarui aplikasi. Jika tidak ingin, bagian 5.7 bisa dibuang.
11. Izin yang sudah terlambat saat diinput (data lama) **tidak** memicu notifikasi seketika; ia muncul di tab Terlambat dan dinotifikasi pada sapuan berikutnya.

## 11. Arah desain UI

Tujuan: tampilan modern dan bersih, tidak terkesan lama, dan cepat dipakai dengan satu tangan.

- **Sistem desain**: Material Design 3 (versi stabil `androidx.compose.material3`). Komponen *Expressive* yang masih alfa tidak dipakai, supaya build tetap stabil.
- **Warna**: skema warna sendiri (terang dan gelap, mengikuti pengaturan sistem). Warna dinamis dari wallpaper hanya ada di Android 12 ke atas, jadi tidak tersedia di HP Android 9 Anda. **Warna utama `#551F20`** (merah marun tua), warna identitas Asrama As-Salam. Tema terang memakai `#551F20` langsung sebagai `primary`; tema gelap memakai nada yang lebih terang dari warna yang sama (diturunkan dengan Material Theme Builder) agar tetap terbaca. Latar netral hangat.
- **Warna status** (selalu disertai ikon dan teks, tidak hanya warna):
  - hijau: kembali tepat waktu;
  - hijau dengan lencana oranye "Terlambat N hari": kembali terlambat;
  - biru netral: izin berjalan;
  - kuning-oranye: terlambat dan terkonfirmasi;
  - merah: terlambat dan belum dikonfirmasi, atau konfirmasi habis. Merah status sengaja lebih terang dan lebih jenuh daripada warna utama agar tidak tertukar, dan selalu disertai ikon peringatan.
- **Bentuk dan jarak**: kartu bersudut membulat 16dp, kelipatan jarak 8dp, area ketuk minimal 48dp, aksi utama di separuh bawah layar (tombol tambah mengambang, aksi di kartu).
- **Tipografi**: skala tipe Material 3 dengan font sistem. Jika memakai font khusus, berkas font dibundel di `res/font` (aplikasi tidak memakai internet).
- **Layar penuh (edge-to-edge)** dengan penanganan inset status bar dan navigasi.
- **Keadaan kosong dan galat**: setiap daftar punya teks keadaan kosong yang singkat dan ramah, bahasa Indonesia sehari-hari.
- **Skill desain untuk agen**: agen di Antigravity disarankan memuat satu skill desain Material 3 untuk Compose (lihat rencana implementasi) agar komponen konsisten dengan panduan terbaru.
