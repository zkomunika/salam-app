# Pantau Izin Santri Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Di Antigravity, jika skill superpowers tidak tersedia, kerjakan task berurutan sendiri dan jalankan langkah uji sebelum tiap commit.

**Goal:** Aplikasi Android lokal untuk satu pengurus asrama yang mencatat izin pulang santri, memantau kembali dan keterlambatan, mengirim notifikasi harian, mencatat konfirmasi wali, dan menyiapkan pesan WhatsApp untuk wali.

**Architecture:** Kotlin + Jetpack Compose (Material 3 stabil) + Room. Logika inti (batas waktu, status, perencanaan notifikasi, template) berada di paket `domain` tanpa dependensi Android dan menerima `today` sebagai parameter, sehingga dapat diuji unit. Satu alarm harian menjalankan "sapuan" yang membuat notifikasi. Tanpa server dan tanpa internet.

**Tech Stack:** Kotlin, Compose BOM stabil terbaru, Material3 stabil (bukan alfa/Expressive), Room + KSP, DataStore Preferences, Navigation Compose, JUnit4, Robolectric, Compose UI test. Versi dicatat di `gradle/libs.versions.toml` pada saat eksekusi (pakai rilis stabil terbaru).

**Spec:** `docs/superpowers/specs/2026-11-05-pemantauan-izin-santri-design.md` (baca lebih dulu; rencana ini mengacu pada nomor bagiannya).

## Global Constraints

- `applicationId` dan paket dasar: `app.pantauizin`. `minSdk` 28. `targetSdk`/`compileSdk`: versi stabil terbaru di SDK terpasang.
- Tanggal dan waktu memakai `java.time`. Semua logika memakai tanggal (`LocalDate`), bukan jam, kecuali jam sapuan.
- Tidak ada izin `INTERNET`, tidak ada panggilan jaringan.
- Bahasa antarmuka Indonesia. Format tanggal `d MMMM yyyy` dengan `Locale.forLanguageTag("id-ID")` (contoh "4 November 2026").
- Jam pengingat default `08:00`, dapat diubah di Pengaturan.
- Warna utama `#551F20` (`Color(0xFF551F20)`) sebagai `primary` tema terang. Hanya Material3 stabil.
- Area ketuk minimal 48dp; jarak kelipatan 8dp; kartu bersudut 16dp; status selalu dengan ikon + teks, tidak hanya warna.
- Satu santri hanya boleh punya satu izin aktif (belum kembali).
- Data master: `app/src/main/assets/santri_master.csv`, kolom `nama,kamar`, 275 baris (berkas dari pemilik: `seed/santri_master.csv`). Hanya nama dan kamar yang dipakai.
- Teks notifikasi persis: `{nama} kamar {kamar} sudah melewati batas waktu izin` / `{nama} kamar {kamar} sudah melewati tanggal kembali yang dikonfirmasi` / `{nama} kamar {kamar} belum kembali dan belum ada konfirmasi terbaru`.
- Teks template pesan varian A dan B: salin persis dari spec bagian 6.1.
- Menyimpan atau mengubah izin tidak boleh memicu sapuan.

## Review Focus

Masukan atau kondisi yang tersirat di spec tetapi mudah terlewat, dan tes yang mengikatnya:

1. **Batal centang kembali** (salah centang): status dihitung ulang dan santri bisa dinotifikasi lagi di sapuan berikutnya. Tes: Task 9 (`batalKembali`) dan Task 3.
2. **Hapus izin**: konfirmasi, riwayat notifikasi, dan file bukti ikut terhapus, tanpa file yatim. Tes: Task 7 (cascade) dan Task 9 (`hapusIzin`).
3. **Nomor WA beragam format** (`+62 812-3456-7890`, `(0812) 345-6789`, `812...`, huruf, terlalu pendek). Tes: Task 4.
4. **HP mati saat jam sapuan lalu menyala siang**: sapuan dikejar sekali tanpa notifikasi ganda. Tes: Task 6 dan Task 10.
5. **Pencarian santri**: huruf besar-kecil, spasi di tepi atau ganda pada kata kunci, nama berapostrof (`DRIYA NUR 'AZMI`), santri nonaktif. Tes: Task 5.

---

## File Structure

```
app/src/main/java/app/pantauizin/
  domain/   Types.kt DeadlineCalculator.kt StatusResolver.kt NotificationPlanner.kt
            PhoneNumberNormalizer.kt MessageTemplateBuilder.kt DefaultTemplates.kt
            SantriCsvParser.kt SantriSearch.kt SweepSchedule.kt IzinFormValidator.kt
            IzinFilter.kt NotifRange.kt SendActionPlanner.kt
  data/     Entities.kt Daos.kt AppDatabase.kt Converters.kt SantriRepository.kt SantriSeeder.kt
            IzinRepository.kt BuktiStorage.kt NotifikasiRepository.kt SettingsStore.kt Views.kt
  notify/   NotificationHelper.kt SweepRunner.kt AlarmScheduler.kt SweepReceiver.kt BootReceiver.kt
  ui/       theme/ (Color.kt Theme.kt)  nav/ AppNav.kt  izin/ form/ terlambat/ konfirmasi/
            notifikasi/ pengaturan/ (satu paket per layar: Screen + ViewModel)
  PantauIzinApp.kt  AppContainer.kt  MainActivity.kt
app/src/test/java/app/pantauizin/...  (mirror paket)
app/src/main/assets/santri_master.csv
```

Skill desain (opsional): sebelum Task 11, pasang satu skill Material 3 untuk Compose (spec bagian 11). Rencana ini sudah memuat aturan desain yang perlu, jadi aplikasi tetap bisa dibangun tanpa skill itu.

---

### Task 1: Scaffold proyek, tipe bersama, DeadlineCalculator

**Files:**
- Create: proyek Android Gradle (Kotlin DSL) di akar repo dengan modul `app`; `app/src/main/AndroidManifest.xml` (tanpa `INTERNET`); `MainActivity.kt` kosong berisi `Text("Pantau Izin Santri")`.
- Create: `domain/Types.kt`, `domain/DeadlineCalculator.kt`
- Test: `app/src/test/java/app/pantauizin/domain/DeadlineCalculatorTest.kt`

**Interfaces:**
- Produces (`Types.kt`, dipakai semua task berikutnya):
```kotlin
enum class AlasanJenis { SAKIT, LAIN_LAIN }
enum class BatasJenis { HARI_1, HARI_2, HARI_3, LEBIH_3_HARI }
enum class IzinStatus { IZIN_BERJALAN, TERLAMBAT_BELUM_KONFIRMASI, TERLAMBAT_TERKONFIRMASI,
    TERLAMBAT_KONFIRMASI_HABIS, KEMBALI_TEPAT, KEMBALI_TERLAMBAT }
enum class NotifJenis { LEWAT_BATAS, LEWAT_TANGGAL_BARU, PENGINGAT_HARIAN }
data class KonfirmasiFact(val tanggalKembaliBaru: LocalDate?)
data class IzinSnapshot(val izinId: Long, val batasAwal: LocalDate,
    val tanggalKembali: LocalDate?, val konfirmasi: List<KonfirmasiFact>)
```
- Produces: `object DeadlineCalculator { fun batasAwal(tanggalPulang: LocalDate, jenis: BatasJenis, tanggalPilihan: LocalDate? = null): LocalDate }` — melempar `IllegalArgumentException` jika `LEBIH_3_HARI` dan `tanggalPilihan` null atau tidak **setelah** `tanggalPulang + 3 hari`.

- [ ] **Step 1: Buat scaffold** — proyek kosong yang membangun `./gradlew :app:assembleDebug` dengan sukses; tambahkan dependensi uji (JUnit4) dan buat `Types.kt` persis seperti di atas.
- [ ] **Step 2: Tulis tes gagal** `DeadlineCalculatorTest`:
```kotlin
@Test fun satuDuaTigaHari() {
  val p = LocalDate.of(2026, 11, 1)
  assertEquals(LocalDate.of(2026, 11, 2), DeadlineCalculator.batasAwal(p, BatasJenis.HARI_1))
  assertEquals(LocalDate.of(2026, 11, 3), DeadlineCalculator.batasAwal(p, BatasJenis.HARI_2))
  assertEquals(LocalDate.of(2026, 11, 4), DeadlineCalculator.batasAwal(p, BatasJenis.HARI_3))
}
@Test fun lintasBulanTahunDanKabisat() {
  assertEquals(LocalDate.of(2027, 1, 2), DeadlineCalculator.batasAwal(LocalDate.of(2026, 12, 30), BatasJenis.HARI_3))
  assertEquals(LocalDate.of(2028, 2, 29), DeadlineCalculator.batasAwal(LocalDate.of(2028, 2, 28), BatasJenis.HARI_1))
}
@Test fun lebihTigaHariMemakaiTanggalPilihan() {
  val p = LocalDate.of(2026, 11, 1)
  assertEquals(LocalDate.of(2026, 11, 20), DeadlineCalculator.batasAwal(p, BatasJenis.LEBIH_3_HARI, LocalDate.of(2026, 11, 20)))
}
@Test fun lebihTigaHariTidakValid() {
  val p = LocalDate.of(2026, 11, 1)
  assertThrows(IllegalArgumentException::class.java) { DeadlineCalculator.batasAwal(p, BatasJenis.LEBIH_3_HARI, null) }
  assertThrows(IllegalArgumentException::class.java) { DeadlineCalculator.batasAwal(p, BatasJenis.LEBIH_3_HARI, LocalDate.of(2026, 11, 4)) } // tepat p+3 tidak valid
}
```
- [ ] **Step 3: Jalankan** `./gradlew :app:testDebugUnitTest --tests "app.pantauizin.domain.DeadlineCalculatorTest"` — Expected: FAIL (`Unresolved reference: DeadlineCalculator`).
- [ ] **Step 4: Implement `DeadlineCalculator.batasAwal`** di `domain/DeadlineCalculator.kt` sesuai signature di atas dan spec 4.4.
- [ ] **Step 5: Jalankan tes lagi** — Expected: PASS (4 tes).
- [ ] **Step 6: Commit**
```bash
git add . && git commit -m "feat: scaffold proyek, tipe bersama, DeadlineCalculator"
```

### Task 2: StatusResolver

**Files:** Create `domain/StatusResolver.kt`; Test `domain/StatusResolverTest.kt`

**Interfaces:**
- Consumes: `IzinSnapshot`, `IzinStatus`, `KonfirmasiFact` (Task 1).
- Produces: `object StatusResolver { fun batasAktif(s: IzinSnapshot): LocalDate; fun status(s: IzinSnapshot, today: LocalDate): IzinStatus; fun hariTerlambat(s: IzinSnapshot, today: LocalDate): Int }`. `batasAktif` = terbesar dari `batasAwal` dan semua `tanggalKembaliBaru` non-null. `hariTerlambat` = (`tanggalKembali` ?: `today`) − `batasAwal` dalam hari, minimal 0.

- [ ] **Step 1: Tulis tes gagal** dengan `batasAwal = 2026-11-04`:
  - hari ini 11-04 belum kembali → `IZIN_BERJALAN` (batas hari itu belum terlambat); hari ini 11-05 tanpa konfirmasi → `TERLAMBAT_BELUM_KONFIRMASI`.
  - konfirmasi baru 11-08: hari ini 11-06 → `TERLAMBAT_TERKONFIRMASI`; hari ini 11-08 → `TERLAMBAT_TERKONFIRMASI`; hari ini 11-09 → `TERLAMBAT_KONFIRMASI_HABIS`. Dua konfirmasi (11-08 lalu 11-12), hari ini 11-10 → `TERLAMBAT_TERKONFIRMASI`, `batasAktif` = 11-12.
  - kembali 11-04 → `KEMBALI_TEPAT`; kembali 11-06 → `KEMBALI_TERLAMBAT`, juga jika ada konfirmasi yang menyebut 11-10 (perpanjangan tidak mengubah status terlambat).
  - `hariTerlambat`: kembali 11-06 → 2; belum kembali hari ini 11-07 → 3; kembali 11-03 → 0.
- [ ] **Step 2: Jalankan** `--tests "app.pantauizin.domain.StatusResolverTest"` — Expected: FAIL (unresolved reference).
- [ ] **Step 3: Implement ketiga fungsi di `StatusResolver`** sesuai tabel spec 4.5.
- [ ] **Step 4: Jalankan** — Expected: PASS.
- [ ] **Step 5: Commit** `git add . && git commit -m "feat: StatusResolver"`

### Task 3: NotificationPlanner

**Files:** Create `domain/NotificationPlanner.kt`; Test `domain/NotificationPlannerTest.kt`

**Interfaces:**
- Consumes: `IzinSnapshot`, `StatusResolver.batasAktif`, `NotifJenis`.
- Produces:
```kotlin
data class ExistingNotif(val izinId: Long, val tanggalKey: LocalDate)
data class PlannedNotif(val izinId: Long, val jenis: NotifJenis)
object NotificationPlanner { fun plan(today: LocalDate, izin: List<IzinSnapshot>, existing: List<ExistingNotif>): List<PlannedNotif> }
```
Aturan spec 7.1: hanya santri belum kembali dengan `today > batasAktif`; lewati jika sudah ada `ExistingNotif(izinId, today)`; `LEWAT_BATAS` jika tanpa konfirmasi dan tanpa notifikasi apa pun; `LEWAT_TANGGAL_BARU` jika ada konfirmasi dan tidak ada notifikasi dengan `tanggalKey > batasAktif`; selain itu `PENGINGAT_HARIAN`.

- [ ] **Step 1: Tulis tes gagal** (`batasAwal = 11-04`): hari ini 11-05 tanpa konfirmasi dan tanpa riwayat → `LEWAT_BATAS`; hari ini 11-06 dengan riwayat (11-05) → `PENGINGAT_HARIAN`; hari ini 11-05 dengan riwayat `(id, 11-05)` → kosong; santri sudah kembali → kosong; konfirmasi baru 11-08 dan riwayat sampai 11-06: hari ini 11-07 → kosong (belum lewat), hari ini 11-09 → `LEWAT_TANGGAL_BARU`, hari ini 11-10 dengan riwayat (11-09) → `PENGINGAT_HARIAN`; **batal centang kembali**: snapshot dengan `tanggalKembali = null` dan riwayat lama dari sebelumnya → dinotifikasi lagi sesuai aturan.
- [ ] **Step 2: Jalankan** `--tests "app.pantauizin.domain.NotificationPlannerTest"` — Expected: FAIL.
- [ ] **Step 3: Implement `NotificationPlanner.plan`.**
- [ ] **Step 4: Jalankan** — Expected: PASS.
- [ ] **Step 5: Commit** `git add . && git commit -m "feat: NotificationPlanner"`

### Task 4: PhoneNumberNormalizer dan MessageTemplateBuilder

**Files:** Create `domain/PhoneNumberNormalizer.kt`, `domain/MessageTemplateBuilder.kt`, `domain/DefaultTemplates.kt`; Test `PhoneNumberNormalizerTest.kt`, `MessageTemplateBuilderTest.kt`

**Interfaces:**
- Consumes: `IzinStatus` (Task 1).
- Produces:
```kotlin
sealed interface PhoneResult { data object Empty : PhoneResult; data class Valid(val value: String) : PhoneResult; data object Invalid : PhoneResult }
object PhoneNumberNormalizer { fun normalize(input: String?): PhoneResult }
enum class TemplateVariant { A, B }
object DefaultTemplates { const val A: String; const val B: String }   // teks spec 6.1, memakai {nama} {kamar} {batas_awal} {batas_aktif}
object MessageTemplateBuilder {
  fun variantFor(status: IzinStatus): TemplateVariant   // A: TERLAMBAT_BELUM_KONFIRMASI; B: TERLAMBAT_TERKONFIRMASI, TERLAMBAT_KONFIRMASI_HABIS; selain itu IllegalArgumentException
  fun formatTanggal(d: LocalDate): String               // "d MMMM yyyy", id-ID
  fun build(template: String, nama: String, kamar: String, batasAwal: LocalDate, batasAktif: LocalDate): String
}
```
Normalisasi (spec 6.2): buang non-angka; awalan `0`→`62`; awalan `62` tetap; awalan `8`→`628`; valid jika berawalan `62` dan 10–15 digit; kosong/blank → `Empty`.

- [ ] **Step 1: Tulis tes gagal** `PhoneNumberNormalizerTest`: `"0812-3456-7890"`, `"+62 812 3456 7890"`, `"(0812) 345-6789"`, `"6281234567890"`, `"81234567890"` → `Valid` berawalan `62` (tiga yang pertama persis `"6281234567890"` kecuali `(0812) 345-6789` → `"628123456789"`); `""`, `"   "`, `null` → `Empty`; `"abc"`, `"0812"`, `"0812345678901234567"` → `Invalid`. `MessageTemplateBuilderTest`: `formatTanggal(2026-01-05)` = `"5 Januari 2026"`; `build(DefaultTemplates.A, "ZAKY", "11", 2026-11-04, 2026-11-04)` memuat `"ZAKY (kamar 11)"` dan `"4 November 2026"` dan tidak memuat `{`; varian B memuat `"paling lambat 8 November 2026"` untuk `batasAktif = 2026-11-08`; `variantFor(KEMBALI_TEPAT)` melempar `IllegalArgumentException`; `variantFor` untuk tiga status terlambat sesuai di atas.
- [ ] **Step 2: Jalankan** kedua kelas tes — Expected: FAIL.
- [ ] **Step 3: Implement ketiga objek.** `DefaultTemplates.A/B` salin persis dari spec 6.1.
- [ ] **Step 4: Jalankan** — Expected: PASS.
- [ ] **Step 5: Commit** `git add . && git commit -m "feat: normalisasi nomor WA dan pembuat template"`

### Task 5: SantriCsvParser dan SantriSearch

**Files:** Create `domain/SantriCsvParser.kt`, `domain/SantriSearch.kt`; Test `SantriCsvParserTest.kt`, `SantriSearchTest.kt`

**Interfaces:**
- Produces:
```kotlin
data class SantriRow(val nama: String, val kamar: String)
object SantriCsvParser { fun parse(csv: String): List<SantriRow> }   // lewati header, baris kosong, nama/kamar kosong, nama kembar (simpan yang pertama); trim + rapikan spasi ganda; dukung field berkutip
data class SantriItem(val id: Long, val nama: String, val kamar: String, val aktif: Boolean)
object SantriSearch { fun filter(all: List<SantriItem>, query: String, kamar: String? = null): List<SantriItem>; fun kamarOptions(all: List<SantriItem>): List<String> }
  // filter: hanya aktif; cocok sebagian, tanpa beda huruf; query di-trim dan spasi ganda dirapikan; query kosong = semua aktif; `kamar` non-null membatasi ke kamar itu; urut nama
  // kamarOptions: kamar unik dari santri aktif, terurut numerik (angka dulu secara numerik, lalu teks non-angka secara abjad)
```

- [ ] **Step 1: Tulis tes gagal** parser: header `nama,kamar` dilewati; `"DRIYA  NUR  'AZMI,12"` → `SantriRow("DRIYA NUR 'AZMI","12")`; baris `JENJEN ABDUSSALAM,` dilewati (kamar kosong); baris kosong dilewati; nama kembar berbeda huruf kecil hanya satu; field berkutip `"A, B",3` terbaca utuh. Tes search: `"azmi"` menemukan `DRIYA NUR 'AZMI`; `"  driya   nur "` menemukan yang sama; santri `aktif=false` tidak muncul; `"zzz"` → kosong; `""` → semua aktif terurut; `filter(all, "ra", kamar = "10")` hanya memuat santri kamar 10; `kamarOptions` untuk kamar `["10","2","1","11"]` → `["1","2","10","11"]` (numerik, bukan abjad).
- [ ] **Step 2: Jalankan** kedua kelas — Expected: FAIL.
- [ ] **Step 3: Implement** `parse` dan `filter` sesuai komentar signature.
- [ ] **Step 4: Jalankan** — Expected: PASS.
- [ ] **Step 5: Commit** `git add . && git commit -m "feat: parser CSV santri dan pencarian"`

### Task 6: SweepSchedule

**Files:** Create `domain/SweepSchedule.kt`; Test `SweepScheduleTest.kt`

**Interfaces:**
- Produces: `object SweepSchedule { fun next(now: LocalDateTime, jam: LocalTime, lastSweepDate: LocalDate?): LocalDateTime; fun isDue(now: LocalDateTime, jam: LocalTime, lastSweepDate: LocalDate?): Boolean }`. `next` (spec 7.3): hari ini pada `jam` jika `jam` masih di depan `now` dan `lastSweepDate != now.toLocalDate()`; selain itu besok pada `jam`. `isDue`: `now.toLocalTime() >= jam` dan `lastSweepDate != hari ini`.

- [ ] **Step 1: Tulis tes gagal** (`jam = 08:00`, hari ini 2026-11-05): 07:00 + last 11-04 → `next` = 11-05 08:00, `isDue` false; 09:00 + last 11-04 → `next` = 11-06 08:00, `isDue` true (**dikejar setelah HP menyala siang**); 07:00 + last 11-05 → `next` = 11-06 08:00; 09:00 + last 11-05 → `isDue` false; jam diganti 10:00 pada 09:00 dengan last null → `next` = 11-05 10:00; `last = null` pada 08:00 tepat → `isDue` true.
- [ ] **Step 2: Jalankan** `--tests "app.pantauizin.domain.SweepScheduleTest"` — Expected: FAIL.
- [ ] **Step 3: Implement** kedua fungsi.
- [ ] **Step 4: Jalankan** — Expected: PASS.
- [ ] **Step 5: Commit** `git add . && git commit -m "feat: SweepSchedule"`

### Task 7: Lapisan Room

**Files:** Create `data/Entities.kt`, `data/Converters.kt`, `data/Daos.kt`, `data/AppDatabase.kt`; Test `app/src/test/java/app/pantauizin/data/AppDatabaseTest.kt` (Robolectric + Room in-memory)

**Interfaces:**
- Consumes: enum Task 1.
- Produces entitas: `SantriEntity(id, nama, kamar, aktif)` dengan indeks unik `nama COLLATE NOCASE`; `IzinEntity(id, santriId FK→santri RESTRICT, tanggalPulang, alasanJenis, alasanDetail, batasJenis, batasAwal, noWaWali?, tanggalKembali?, dibuatPada)`; `KonfirmasiEntity(id, izinId FK CASCADE, tanggalKembaliBaru?, catatan?, buktiPath?, dibuatPada)`; `NotifikasiEntity(id, izinId FK CASCADE, jenis, pesan, tanggalKey, dibuatPada, dibaca)` dengan indeks unik (`izinId`, `tanggalKey`). `LocalDate` disimpan sebagai epochDay (Long), `Instant` sebagai epochMilli (Long). Nama kolom sesuai spec 4.0 sampai 4.3.
- DAO:
```kotlin
interface SantriDao { suspend fun insertAll(l: List<SantriEntity>); suspend fun count(): Int; fun observeAll(): Flow<List<SantriEntity>>
  suspend fun insert(e: SantriEntity): Long; suspend fun update(e: SantriEntity); suspend fun findByNama(nama: String): SantriEntity? }
interface IzinDao { suspend fun insert(e: IzinEntity): Long; suspend fun update(e: IzinEntity); suspend fun delete(id: Long)
  fun observeAll(): Flow<List<IzinWithKonfirmasi>>; suspend fun findActiveBySantri(santriId: Long): IzinEntity?; suspend fun get(id: Long): IzinWithKonfirmasi? }
interface KonfirmasiDao { suspend fun insert(e: KonfirmasiEntity): Long }
interface NotifikasiDao { suspend fun insertIgnore(e: NotifikasiEntity): Long; suspend fun forIzin(izinId: Long): List<NotifikasiEntity>
  fun observeBetween(fromMillis: Long, toMillis: Long): Flow<List<NotifikasiWithSantri>>; suspend fun markRead(id: Long); suspend fun markAllRead() }
```
(`IzinWithKonfirmasi` = `@Embedded IzinEntity` + `@Relation` konfirmasi + santri; `NotifikasiWithSantri` memuat `NotifikasiEntity` + nama dan kamar.) `insertIgnore` mengembalikan `-1` jika melanggar indeks unik.

- [ ] **Step 1: Tulis tes gagal** `AppDatabaseTest`: dua santri `"ABC"` dan `"abc"` → insert kedua melempar (unik tanpa beda huruf); hapus izin menghapus konfirmasi dan notifikasinya (cascade); `insertIgnore` dua kali dengan (`izinId`, `tanggalKey`) sama → kedua mengembalikan `-1`; `findActiveBySantri` mengembalikan izin dengan `tanggalKembali = null` dan `null` setelah diisi.
- [ ] **Step 2: Jalankan** `--tests "app.pantauizin.data.AppDatabaseTest"` — Expected: FAIL (kelas belum ada).
- [ ] **Step 3: Implement entitas, converter, DAO, `AppDatabase`** (aktifkan `PRAGMA foreign_keys`, ekspor skema Room).
- [ ] **Step 4: Jalankan** — Expected: PASS.
- [ ] **Step 5: Commit** `git add . && git commit -m "feat: database Room"`

### Task 8: SettingsStore, SantriSeeder, SantriRepository

**Files:** Create `data/SettingsStore.kt`, `data/SantriSeeder.kt`, `data/SantriRepository.kt`; copy `seed/santri_master.csv` → `app/src/main/assets/santri_master.csv`; Test `data/SantriSeederTest.kt`, `data/SantriRepositoryTest.kt`

**Interfaces:**
- Consumes: `SantriDao`, `SantriCsvParser`, `DefaultTemplates`.
- Produces:
```kotlin
class SettingsStore(context: Context) { val jamPengingat: Flow<LocalTime>   // default 08:00
  val templateA: Flow<String>; val templateB: Flow<String>               // default DefaultTemplates
  val lastSweepDate: Flow<LocalDate?>
  suspend fun setJamPengingat(t: LocalTime); suspend fun setTemplate(v: TemplateVariant, text: String)
  suspend fun resetTemplate(v: TemplateVariant); suspend fun setLastSweepDate(d: LocalDate) }
class SantriSeeder(dao: SantriDao, assetReader: () -> String) { suspend fun seedIfEmpty(): Int }   // mengembalikan jumlah yang dimuat
sealed interface SantriResult { data object Ok : SantriResult; data object NamaKembar : SantriResult; data object Invalid : SantriResult }
class SantriRepository(dao: SantriDao) { fun observe(): Flow<List<SantriItem>>   // termasuk nonaktif, untuk layar Data Santri
  suspend fun tambah(nama: String, kamar: String): SantriResult; suspend fun ubah(id: Long, nama: String, kamar: String): SantriResult
  suspend fun setAktif(id: Long, aktif: Boolean) }
```
`tambah/ubah` merapikan spasi, menolak nama atau kamar kosong (`Invalid`) dan nama kembar tanpa beda huruf (`NamaKembar`).

- [ ] **Step 1: Tulis tes gagal**: seeder dengan berkas aset asli memuat tepat **275** baris dan panggilan kedua mengembalikan `0` (tabel tidak kosong); `tambah("  zaky  fauzi ", "5")` tersimpan sebagai `"zaky fauzi"` tanpa spasi ganda; `tambah("ABC","")` → `Invalid`; `tambah` nama yang sudah ada dengan huruf berbeda → `NamaKembar`; `setAktif(false)` membuat santri hilang dari `SantriSearch.filter`.
- [ ] **Step 2: Jalankan** kedua kelas tes — Expected: FAIL.
- [ ] **Step 3: Implement ketiga kelas** (DataStore Preferences untuk `SettingsStore`; simpan `LocalTime` dan `LocalDate` sebagai String ISO).
- [ ] **Step 4: Jalankan** — Expected: PASS.
- [ ] **Step 5: Commit** `git add . && git commit -m "feat: data master santri dan pengaturan"`

### Task 9: BuktiStorage dan IzinRepository

**Files:** Create `data/Views.kt`, `data/BuktiStorage.kt`, `data/IzinRepository.kt`, `data/NotifikasiRepository.kt`; Test `data/IzinRepositoryTest.kt`

**Interfaces:**
- Consumes: DAO Task 7, `DeadlineCalculator`, `PhoneNumberNormalizer`, `StatusResolver`, `IzinSnapshot`.
- Produces:
```kotlin
data class KonfirmasiView(val id: Long, val tanggalKembaliBaru: LocalDate?, val catatan: String?, val buktiPath: String?, val dibuatPada: Instant)
data class IzinView(val id: Long, val santriId: Long, val nama: String, val kamar: String, val tanggalPulang: LocalDate,
  val alasanJenis: AlasanJenis, val alasanDetail: String, val batasJenis: BatasJenis, val batasAwal: LocalDate,
  val noWaWali: String?, val tanggalKembali: LocalDate?, val konfirmasi: List<KonfirmasiView>) { fun toSnapshot(): IzinSnapshot }
data class IzinInput(val santriId: Long, val tanggalPulang: LocalDate, val alasanJenis: AlasanJenis, val alasanDetail: String,
  val batasJenis: BatasJenis, val tanggalBatasPilihan: LocalDate?, val noWaWali: String?)
sealed interface SaveIzinResult { data class Ok(val id: Long) : SaveIzinResult
  data class SantriSudahPunyaIzinAktif(val izinId: Long) : SaveIzinResult; data object BatasTidakValid : SaveIzinResult; data object NomorWaTidakValid : SaveIzinResult }
interface BuktiStorage { suspend fun salin(source: Uri): String /*path relatif*/; fun hapus(path: String); fun file(path: String): File }
class IzinRepository(izinDao: IzinDao, konfirmasiDao: KonfirmasiDao, bukti: BuktiStorage) {
  fun observe(): Flow<List<IzinView>>; suspend fun get(id: Long): IzinView?
  suspend fun tambah(input: IzinInput): SaveIzinResult; suspend fun ubah(id: Long, input: IzinInput): SaveIzinResult
  suspend fun hapus(id: Long); suspend fun tandaiKembali(id: Long, tanggal: LocalDate); suspend fun batalKembali(id: Long)
  suspend fun tambahKonfirmasi(izinId: Long, tanggalBaru: LocalDate?, catatan: String?, bukti: Uri?) }
class NotifikasiRepository(dao: NotifikasiDao) { fun observeBetween(from: Instant, to: Instant): Flow<List<NotifikasiView>>; suspend fun markRead(id: Long); suspend fun markAllRead() }
```
`tambah/ubah` menghitung `batasAwal` lewat `DeadlineCalculator`, menyimpan `noWaWali` ternormalisasi, dan menolak santri yang masih punya izin aktif (kecuali izin itu sendiri saat `ubah`). `tambahKonfirmasi` menyalin file lewat `BuktiStorage` sebelum menyimpan; jika penyalinan gagal tidak ada baris tersimpan. `hapus` menghapus file bukti semua konfirmasinya.

- [ ] **Step 1: Tulis tes gagal** (Robolectric, `BuktiStorage` palsu yang mencatat penghapusan): `tambah` kedua untuk santri yang sama sebelum kembali → `SantriSudahPunyaIzinAktif`; setelah `tandaiKembali` boleh `tambah` lagi; `tanggalPulang` lampau diterima dan `batasAwal` benar; `ubah` mengganti `tanggalPulang` menghitung ulang `batasAwal` dan **mempertahankan konfirmasi**; `noWaWali` `"0812-3456-7890"` tersimpan `"6281234567890"` dan `"abc"` → `NomorWaTidakValid`; `LEBIH_3_HARI` tanpa tanggal valid → `BatasTidakValid`; **`batalKembali`** mengembalikan `tanggalKembali = null`; **`hapus`** menghapus konfirmasi dan memanggil `hapus(path)` untuk setiap bukti; `tambahKonfirmasi` dengan `BuktiStorage` yang melempar tidak menambah baris.
- [ ] **Step 2: Jalankan** `--tests "app.pantauizin.data.IzinRepositoryTest"` — Expected: FAIL.
- [ ] **Step 3: Implement** kelas di atas. `BuktiStorage` nyata menyalin ke `filesDir/bukti/` dengan nama unik dan mengembalikan path relatif.
- [ ] **Step 4: Jalankan** — Expected: PASS.
- [ ] **Step 5: Commit** `git add . && git commit -m "feat: repository izin dan penyimpanan bukti"`

### Task 10: Sapuan dan notifikasi sistem

**Files:** Create `notify/SweepRunner.kt`, `notify/NotificationHelper.kt`, `notify/AlarmScheduler.kt`, `notify/SweepReceiver.kt`, `notify/BootReceiver.kt`; Modify `AndroidManifest.xml`; Test `notify/SweepRunnerTest.kt`

**Interfaces:**
- Consumes: `NotificationPlanner`, `SweepSchedule`, `SettingsStore`, `IzinRepository`, `NotifikasiDao`, `StatusResolver`.
- Produces:
```kotlin
interface Notifier { fun show(izinId: Long, text: String) }          // NotificationHelper mengimplementasikannya
interface Scheduler { fun schedule(at: LocalDateTime) }              // AlarmScheduler mengimplementasikannya
class SweepRunner(izin: IzinRepository, notif: NotifikasiDao, settings: SettingsStore, notifier: Notifier,
                  scheduler: Scheduler, clock: () -> LocalDateTime) {
  suspend fun runIfDue(): Boolean      // menjalankan sapuan bila SweepSchedule.isDue; mencatat lastSweepDate; menjadwalkan sapuan berikutnya
  suspend fun rescheduleOnly() }       // dipakai BootReceiver dan perubahan jam
```
Isi sapuan: `NotificationPlanner.plan` → tiap `PlannedNotif` disimpan lewat `insertIgnore` (hanya yang berhasil, tidak `-1`, yang dimunculkan) dengan teks sesuai Global Constraints. `NotificationHelper`: saluran "Santri terlambat" berprioritas tinggi; satu notifikasi per santri (id = `izinId`); mengetuknya membuka `MainActivity` dengan extra `open_tab=terlambat` dan menandai riwayat dibaca. Manifest: `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`; `AlarmScheduler` memakai `setExactAndAllowWhileIdle`, dan `setAndAllowWhileIdle` bila izin alarm tepat tidak diberikan.

- [ ] **Step 1: Tulis tes gagal** `SweepRunnerTest` (Notifier dan Scheduler palsu, jam disuntik): sebelum jam pengingat tidak ada notifikasi; pada 08:00 santri terlambat mendapat satu notifikasi `LEWAT_BATAS` dengan teks `"ZAKY kamar 11 sudah melewati batas waktu izin"`; `runIfDue` kedua pada hari yang sama mengembalikan `false` tanpa notifikasi baru; **HP menyala pukul 13:00 dengan sapuan hari itu belum jalan → tepat satu sapuan, tanpa duplikat saat dibuka ulang**; santri yang sudah kembali tidak dinotifikasi; setelah sapuan, `scheduler.schedule` dipanggil dengan waktu besok 08:00; `tambah` izin (repository) tidak memanggil `Notifier`.
- [ ] **Step 2: Jalankan** `--tests "app.pantauizin.notify.SweepRunnerTest"` — Expected: FAIL.
- [ ] **Step 3: Implement `SweepRunner`** dan kelas Android pendukungnya; `SweepReceiver` memakai `goAsync()` lalu memanggil `runIfDue()`; `BootReceiver` memanggil `rescheduleOnly()`.
- [ ] **Step 4: Jalankan** — Expected: PASS.
- [ ] **Step 5: Commit** `git add . && git commit -m "feat: sapuan harian dan notifikasi"`

### Task 11: Fondasi UI (tema, navigasi, wiring)

**Files:** Create `ui/theme/Color.kt`, `ui/theme/Theme.kt`, `ui/nav/AppNav.kt`, `AppContainer.kt`, `PantauIzinApp.kt`; Modify `MainActivity.kt`, manifest (`android:name=".PantauIzinApp"`); Test `ui/theme/ThemeTest.kt`, `ui/nav/AppNavTest.kt` (Compose UI test + Robolectric)

**Interfaces:**
- Produces: `PantauIzinTheme(content)` dengan skema terang dan gelap (mengikuti sistem, tanpa warna dinamis); `object StatusColors { val Hijau = Color(0xFF2E7D32); val Oranye = Color(0xFFEF6C00); val Biru = Color(0xFF1565C0); val Amber = Color(0xFFF9A825); val Merah = Color(0xFFD32F2F) }`; `class AppContainer(context)` berisi semua repository dan `SweepRunner`; `AppNav` dengan empat tab berlabel `"Izin"`, `"Terlambat"`, `"Notifikasi"`, `"Pengaturan"`; `MainActivity` membuka tab Terlambat bila extra `open_tab == "terlambat"`.
- `PantauIzinApp.onCreate`: `SantriSeeder.seedIfEmpty()`, `SweepRunner.runIfDue()` dan jadwalkan alarm; `MainActivity` meminta `POST_NOTIFICATIONS` (Android 13+) saat pertama dibuka.

- [ ] **Step 1: Tulis tes gagal**: `lightScheme.primary == Color(0xFF551F20)`; `AppNav` menampilkan empat label di atas (`onNodeWithText(...).assertExists()`); mengetuk "Terlambat" berpindah tab.
- [ ] **Step 2: Jalankan** `--tests "app.pantauizin.ui.*"` — Expected: FAIL.
- [ ] **Step 3: Implement** tema (`primary = 0xFF551F20`, `onPrimary` putih; skema gelap memakai nada lebih terang dari warna yang sama yang diturunkan dengan Material Theme Builder; latar netral hangat), `AppNav` (layar isi sementara `Text`), `AppContainer`, `PantauIzinApp`, dan permintaan izin notifikasi. Terapkan aturan desain spec bagian 11 (edge-to-edge dengan inset, kartu 16dp, jarak 8dp).
- [ ] **Step 4: Jalankan** — Expected: PASS; lalu `./gradlew :app:assembleDebug` — Expected: BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `git add . && git commit -m "feat: tema, navigasi, wiring aplikasi"`

### Task 12: Form izin (tambah dan edit)

**Files:** Create `domain/IzinFormValidator.kt`, `ui/form/IzinFormScreen.kt`, `ui/form/IzinFormViewModel.kt`; Test `domain/IzinFormValidatorTest.kt`, `ui/form/IzinFormViewModelTest.kt`

**Interfaces:**
- Consumes: `IzinInput`, `SaveIzinResult`, `SantriSearch`, `DeadlineCalculator`, `PhoneNumberNormalizer`.
- Produces:
```kotlin
enum class FormField { SANTRI, ALASAN_DETAIL, BATAS_TANGGAL, NO_WA }
data class IzinFormState(val santriId: Long?, val tanggalPulang: LocalDate, val alasanJenis: AlasanJenis?, val alasanDetail: String,
  val batasJenis: BatasJenis?, val tanggalBatasPilihan: LocalDate?, val noWa: String)
object IzinFormValidator { fun validate(s: IzinFormState): Map<FormField, String> }   // pesan Indonesia; kosong = valid
data class BatasPreview(val batasAwal: LocalDate, val hariLewat: Int)                  // hariLewat > 0 jika batasAwal < today
object BatasPreviewer { fun of(s: IzinFormState, today: LocalDate): BatasPreview? }   // null jika belum cukup terisi
```

- [ ] **Step 1: Tulis tes gagal**: validator menolak `santriId = null`, `alasanDetail` kosong (setelah trim), `LEBIH_3_HARI` tanpa tanggal atau tanggal ≤ pulang+3, `noWa = "abc"`; `noWa` kosong valid; `BatasPreviewer.of` untuk pulang 2026-10-28 + `HARI_3` pada today 2026-11-05 → `BatasPreview(2026-10-31, 5)`; ViewModel: memilih santri yang masih punya izin aktif menampilkan peringatan dan `simpan()` tidak menyimpan; mode edit memuat nilai izin yang ada; memilih kamar 10 mempersempit saran nama; memilih nama mengisi kamar santri itu.
- [ ] **Step 2: Jalankan** kelas-kelas tes tadi — Expected: FAIL.
- [ ] **Step 3: Implement validator, previewer, ViewModel, dan layar** sesuai spec 5.2: dropdown Kamar (`SantriSearch.kamarOptions`, terurut numerik dari 1, mempersempit saran) dan kolom Nama dengan daftar saran (`SantriSearch.filter`); memilih nama mengisi dropdown Kamar otomatis, dropdown alasan dengan label dinamis "Nama penyakit"/"Keperluan pulang", dropdown batas, kalender bila `>3 hari`, nomor WA opsional, pratinjau "Batas kembali: …" dengan peringatan "Sudah lewat N hari, santri akan langsung tercatat terlambat", tombol Hapus (dengan dialog) di mode edit.
- [ ] **Step 4: Jalankan** tes — Expected: PASS. Verifikasi manual di emulator: form terbuka dari tombol tambah (+), mengetik "azmi" memunculkan `DRIYA NUR 'AZMI`.
- [ ] **Step 5: Commit** `git add . && git commit -m "feat: form izin"`

### Task 13: Tab Izin (daftar, checklist kembali, centang konfirmasi)

**Files:** Create `domain/IzinFilter.kt`, `ui/izin/StatusStyle.kt`, `ui/izin/IzinListScreen.kt`, `ui/izin/IzinListViewModel.kt`; Test `domain/IzinFilterTest.kt`, `ui/izin/StatusStyleTest.kt`

**Interfaces:**
- Consumes: `IzinStatus`, `StatusColors`, `IzinView.toSnapshot`, `StatusResolver`.
- Produces: `enum class IzinFilter { SEMUA, BERJALAN, TERLAMBAT, KEMBALI; fun matches(s: IzinStatus): Boolean }` (`TERLAMBAT` = tiga status terlambat + `KEMBALI_TERLAMBAT`; `KEMBALI` = `KEMBALI_TEPAT` + `KEMBALI_TERLAMBAT`); `data class StatusStyle(val label: String, val icon: ImageVector, val color: Color)` dan `fun styleOf(s: IzinStatus): StatusStyle` memakai warna spec bagian 11 (hijau, hijau + lencana oranye, biru, amber, merah).

- [ ] **Step 1: Tulis tes gagal**: `matches` untuk semua kombinasi (`TERLAMBAT` memuat `KEMBALI_TERLAMBAT`; `BERJALAN` hanya `IZIN_BERJALAN`); `styleOf` mengembalikan label, ikon, dan warna unik untuk tiap dari 6 status dan tidak pernah memakai warna utama.
- [ ] **Step 2: Jalankan** kedua kelas — Expected: FAIL.
- [ ] **Step 3: Implement filter, `styleOf`, ViewModel, dan layar** sesuai spec 5.1: kartu (nama, kamar, alasan, batas aktif, lencana status), chip filter, kotak centang "Sudah kembali" dengan dialog tanggal kembali (default hari ini) dan dialog pembatalan untuk yang sudah dicentang, lencana "Terlambat N hari" memakai `StatusResolver.hariTerlambat`, kotak centang konfirmasi hanya untuk status terlambat (membuka form konfirmasi Task 14), tombol Edit, teks keadaan kosong.
- [ ] **Step 4: Jalankan** tes — Expected: PASS. Verifikasi manual: izin dengan tanggal pulang lampau langsung tampil merah dengan ikon peringatan.
- [ ] **Step 5: Commit** `git add . && git commit -m "feat: tab daftar izin"`

### Task 14: Tab Terlambat, form konfirmasi, kirim template

**Files:** Create `domain/SendActionPlanner.kt`, `ui/terlambat/TerlambatScreen.kt`, `ui/terlambat/TerlambatViewModel.kt`, `ui/konfirmasi/KonfirmasiFormScreen.kt`, `ui/konfirmasi/KonfirmasiFormViewModel.kt`, `ui/terlambat/TemplateSender.kt`; Test `domain/SendActionPlannerTest.kt`, `ui/terlambat/TerlambatSortTest.kt`, `ui/konfirmasi/KonfirmasiFormViewModelTest.kt`

**Interfaces:**
- Consumes: `MessageTemplateBuilder`, `PhoneNumberNormalizer`, `StatusResolver`, `IzinRepository.tambahKonfirmasi`, `SettingsStore.templateA/B`.
- Produces:
```kotlin
sealed interface SendAction { data class OpenWhatsApp(val url: String) : SendAction; data class ShareText(val text: String) : SendAction }
object SendActionPlanner { fun plan(templateText: String, noWaWali: String?): SendAction }
  // teks sudah terisi; ada nomor → https://wa.me/{nomor}?text={URL-encoded}; tanpa nomor → ShareText
fun sortTerlambat(items: List<IzinView>, today: LocalDate): List<IzinView>   // belum konfirmasi/konfirmasi habis di atas, lalu terkonfirmasi; tiebreak hari terlambat terbanyak
```

- [ ] **Step 1: Tulis tes gagal**: `plan(teks, "6281234567890")` → `OpenWhatsApp("https://wa.me/6281234567890?text=" + URLEncoder.encode(teks, "UTF-8"))`; `plan(teks, null)` → `ShareText(teks)`; urutan `sortTerlambat` sesuai aturan; ViewModel konfirmasi: tanggal baru wajib jika santri belum kembali dan tidak boleh sebelum hari ini, tidak ada tanggal untuk santri yang sudah kembali, bukti dan catatan opsional.
- [ ] **Step 2: Jalankan** ketiga kelas — Expected: FAIL.
- [ ] **Step 3: Implement** perencana, urutan, ViewModel, layar, dan `TemplateSender` (`ACTION_VIEW` untuk `OpenWhatsApp`; untuk `ShareText` salin ke papan klip, tampilkan "Teks disalin", lalu buka `ACTION_SEND`; bila WhatsApp gagal dibuka, jatuh ke `ShareText`). Layar Terlambat sesuai spec 5.3: tiga aksi per kartu (Kirim template, Konfirmasi, Tandai kembali), tombol kirim hanya untuk yang belum kembali, riwayat singkat konfirmasi, banner bila izin notifikasi ditolak.
- [ ] **Step 4: Jalankan** tes — Expected: PASS. Verifikasi manual: tombol Kirim template pada santri ber-nomor membuka chat WhatsApp dengan teks terisi.
- [ ] **Step 5: Commit** `git add . && git commit -m "feat: tab terlambat, konfirmasi, kirim template"`

### Task 15: Tab Notifikasi (riwayat dan filter waktu)

**Files:** Create `domain/NotifRange.kt`, `ui/notifikasi/NotifikasiScreen.kt`, `ui/notifikasi/NotifikasiViewModel.kt`; Test `domain/NotifRangeTest.kt`

**Interfaces:**
- Produces: `sealed interface NotifRange { data object HariIni; data object TujuhHari; data object TigaPuluhHari; data class Kustom(val dari: LocalDate, val sampai: LocalDate) }` dan `fun NotifRange.toInstants(today: LocalDate, zone: ZoneId): Pair<Instant, Instant>` (awal hari dari sampai akhir hari, inklusif; 7 hari = hari ini dan 6 hari sebelumnya).

- [ ] **Step 1: Tulis tes gagal**: `HariIni` mencakup 00:00 sampai 23:59:59 tanggal itu; `TujuhHari` mulai 6 hari sebelum hari ini; `Kustom` dengan `dari > sampai` ditolak (`IllegalArgumentException`).
- [ ] **Step 2: Jalankan** `--tests "app.pantauizin.domain.NotifRangeTest"` — Expected: FAIL.
- [ ] **Step 3: Implement `NotifRange`, ViewModel, dan layar** (spec 5.5): daftar terbaru di atas, penanda belum dibaca, chip filter + pemilih rentang tanggal, ketuk item menandai dibaca dan membuka tab Terlambat.
- [ ] **Step 4: Jalankan** — Expected: PASS.
- [ ] **Step 5: Commit** `git add . && git commit -m "feat: tab riwayat notifikasi"`

### Task 16: Tab Pengaturan dan layar Data Santri

**Files:** Create `ui/pengaturan/PengaturanScreen.kt`, `ui/pengaturan/PengaturanViewModel.kt`, `ui/pengaturan/DataSantriScreen.kt`, `ui/pengaturan/DataSantriViewModel.kt`; Test `ui/pengaturan/PengaturanViewModelTest.kt`, `ui/pengaturan/DataSantriViewModelTest.kt`

**Interfaces:**
- Consumes: `SettingsStore`, `SweepRunner.rescheduleOnly`, `SantriRepository`, `SantriResult`.
- Produces: `PengaturanViewModel.ubahJam(t: LocalTime)` (menyimpan lalu `rescheduleOnly()`); `simpanTemplate(v, text)`, `kembalikanTemplate(v)`; `DataSantriViewModel.tambah/ubah/setAktif` meneruskan `SantriResult` ke pesan di UI.

- [ ] **Step 1: Tulis tes gagal**: `ubahJam(LocalTime.of(10, 0))` menyimpan nilai dan memicu `rescheduleOnly` tepat sekali; `kembalikanTemplate(A)` mengembalikan `DefaultTemplates.A`; template kosong ditolak; `DataSantriViewModel.tambah` dengan nama kembar memunculkan pesan "Nama sudah ada".
- [ ] **Step 2: Jalankan** kedua kelas — Expected: FAIL.
- [ ] **Step 3: Implement** layar sesuai spec 5.6 dan 5.7: pemilih jam 24 jam (default 08:00), editor template A dan B dengan "Kembalikan bawaan", petunjuk baterai dengan tombol `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` (teks menyebut izin "mulai otomatis" di HP Vivo), dan layar Data Santri (cari, tambah, ubah, nonaktifkan atau aktifkan).
- [ ] **Step 4: Jalankan** — Expected: PASS.
- [ ] **Step 5: Commit** `git add . && git commit -m "feat: pengaturan dan data santri"`

### Task 17: Verifikasi akhir dan panduan pemasangan

**Files:** Create `INSTALL.md`

- [ ] **Step 1: Jalankan seluruh tes** `./gradlew :app:testDebugUnitTest` — Expected: semua PASS, tanpa tes yang dilewati.
- [ ] **Step 2: Bangun APK** `./gradlew :app:assembleDebug` — Expected: BUILD SUCCESSFUL; berkas di `app/build/outputs/apk/debug/app-debug.apk`.
- [ ] **Step 3: Tulis `INSTALL.md`**: aktifkan Opsi Pengembang dan Debugging USB di HP; pasang dengan `adb install -r app-debug.apk`; izinkan notifikasi; aktifkan "mulai otomatis" dan nonaktifkan penghematan baterai untuk aplikasi.
- [ ] **Step 4: Uji manual di HP (Vivo, Android 9)** sesuai spec bagian 9, masing-masing dengan hasil yang diharapkan: 275 santri termuat pada pembukaan pertama; input izin lama (tanggal pulang lampau) langsung tampil terlambat tanpa notifikasi seketika; setel jam pengingat 2 menit ke depan, tutup aplikasi, notifikasi muncul tepat waktu dan mengetuknya membuka tab Terlambat; restart HP lalu notifikasi hari berikutnya tetap muncul; Kirim template membuka chat WhatsApp dengan teks terisi; bukti file tersimpan dan terbuka kembali.
- [ ] **Step 5: Commit** `git add . && git commit -m "docs: panduan pemasangan"`

---

## Self-Review

**Spec coverage:** 4.0 Task 7, 8 · 4.1–4.3 Task 7, 9 · 4.4 Task 1, 2 · 4.5 Task 2 · 5.1 Task 13 · 5.2 Task 12 · 5.3 dan 5.4 Task 14 · 5.5 Task 15 · 5.6 dan 5.7 Task 16 · 6 Task 4, 14 · 7.1–7.3 Task 3, 6, 10 · 8 File Structure · 9 Tes di tiap task dan Task 17 · 10 keputusan tercermin di Task 9 (satu izin aktif), 10 (sapuan tanpa pemicu saat simpan), 12 (data lama) · 11 Task 11, 13. Tidak ada kebutuhan spec tanpa task.

**Konsistensi tipe:** `IzinSnapshot`, `KonfirmasiFact`, `IzinStatus`, `NotifJenis` didefinisikan di Task 1 dan dipakai identik di Task 2, 3, 9, 13. `SweepSchedule.isDue/next` (Task 6) dipakai `SweepRunner` (Task 10). `SendAction` dan `SantriResult` hanya didefinisikan sekali.

**Proporsi:** rencana memuat tanda tangan, nama tes, dan nilai spec; badan fungsi diserahkan kepada pelaksana.
