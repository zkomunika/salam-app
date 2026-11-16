# Laporan Perbaikan Skill `.agents/skills/`

Tanggal: 2026-11-16
Branch Git: `fix/skills`
Cadangan awal: dibuat di `d:\Projek\pantau-santri-skills-backup` sebelum modifikasi dan dicatat pada commit baseline `1193ba5`.

---

## 1. Daftar File yang Diubah, per Fase

### Persiapan & Baseline (`1193ba5`)
- `.gitignore`: menghapus baris `.agents/` agar seluruh perbaikan skill di `.agents/skills/` dan `.agents/SKILLS_FIX_BRIEF.md` dapat di-commit per fase di branch `fix/skills`.

### Fase 1 — Seragamkan Path ke `.agents/skills` (`b61a9dd`)
- `.agents/skills/banner-design/SKILL.md`: mengganti `.claude/skills` → `.agents/skills`.
- `.agents/skills/brand/references/approval-checklist.md`: mengganti `.claude/skills` → `.agents/skills`.
- `.agents/skills/brand/references/update.md`: mengganti `.claude/skills` → `.agents/skills`.
- `.agents/skills/brand/scripts/extract-colors.cjs`: mengganti `.claude/skills` → `.agents/skills`.
- `.agents/skills/brand/scripts/sync-brand-to-tokens.cjs`: mengubah `GENERATE_TOKENS_SCRIPT` menjadi `path.resolve(__dirname, '..', '..', 'design-system', 'scripts', 'generate-tokens.cjs')` dan menghapus `path.resolve(process.cwd(), ...)` pada baris 232.
- `.agents/skills/design/SKILL.md`: mengganti `~/.claude/skills` dan `.claude/skills` → `.agents/skills`.
- `.agents/skills/design/references/cip-design.md`: mengganti `~/.claude/skills` → `.agents/skills`.
- `.agents/skills/design/references/design-routing.md`: mengganti `.claude/skills` → `.agents/skills`.
- `.agents/skills/design/references/icon-design.md`: mengganti `~/.claude/skills` → `.agents/skills`.
- `.agents/skills/design/references/logo-design.md`: mengganti `~/.claude/skills` → `.agents/skills`.
- `.agents/skills/design/references/slides-copywriting-formulas.md`: mengganti `.claude/skills` → `.agents/skills`.
- `.agents/skills/design/references/slides-layout-patterns.md`: mengganti `.claude/skills` → `.agents/skills`.
- `.agents/skills/design/references/slides-strategies.md`: mengganti `.claude/skills` → `.agents/skills`.
- `.agents/skills/design/scripts/cip/generate.py`: mengganti `~/.claude/skills` → `.agents/skills`.
- `.agents/skills/slides/references/copywriting-formulas.md`: mengganti `.claude/skills` → `.agents/skills`.
- `.agents/skills/slides/references/layout-patterns.md`: mengganti `.claude/skills` → `.agents/skills`.
- `.agents/skills/slides/references/slide-strategies.md`: mengganti `.claude/skills` → `.agents/skills`.

### Fase 2 — Buang Fitur Khusus Claude Code (`5892cad`)
- `.agents/skills/banner-design/SKILL.md`: menghapus `argument-hint:` dan mengganti `AskUserQuestion` dengan instruksi bertanya langsung di chat.
- `.agents/skills/brand/SKILL.md`: menghapus `argument-hint:`, menghapus tabel `## Subcommands`, mengganti `(or use /brand update)`, dan menulis ulang `## Routing` sebagai decision tree bahasa biasa tanpa `$ARGUMENTS`.
- `.agents/skills/brand/references/update.md`: mengganti `<args>$ARGUMENTS</args>`, `AskUserQuestion`, dan contoh `/brand:update`.
- `.agents/skills/caveman/SKILL.md`: menghapus rujukan alias `ultracave`/`megacave`, menghapus rujukan ke "hook", dan menghapus `` `/caveman-compress` exempt. ``.
- `.agents/skills/caveman/README.md`: menghapus blok tabel/contoh sibling skill `ultracave`, `megacave`, dan konfigurasi hook khusus Claude Code.
- `.agents/skills/design/SKILL.md`: menghapus `argument-hint:`, mengganti `AskUserQuestion`, serta mengganti `/ckm:*`, `/ck:*`, `/ui-ux-pro-max`, dan instruksi acak menjadi routing berbasis kriteria.
- `.agents/skills/design/references/logo-design.md`: mengganti `AskUserQuestion` dan `/ui-ux-pro-max`.
- `.agents/skills/design/references/social-photos-design.md`: mengganti `AskUserQuestion`, `/ckm:*`, `/ck:*`, dan instruksi acak menjadi urutan skill dengan kriteria.
- `.agents/skills/design-system/SKILL.md`: menghapus `argument-hint:` dan mengganti `/slides:create` dengan contoh permintaan bahasa biasa.
- `.agents/skills/slides/SKILL.md`: menghapus `argument-hint:`.
- `.agents/skills/ui-styling/SKILL.md`: menghapus `argument-hint:`.

### Fase 3 — Hapus Duplikasi & Tetapkan Satu Pemilik per Topik (`27633d1`)
- `.agents/skills/design/SKILL.md`: menghapus bagian `Slides (Built-in)` dan `Banner Design (Built-in)` beserta baris tabel referensinya, memperbarui kolom `External skill` menjadi `Sibling skill in .agents/skills/`, mengarahkan presentasi/banner ke `slides` dan `banner-design`, serta melunakkan kata kapital teriak (`ALWAYS`, `IMPORTANT`).
- `.agents/skills/design/references/logo-design.md`: mengganti `**ALWAYS** use white background...` dengan penjelasan alasan singkat.
- `.agents/skills/design/references/design-routing.md`: menyamakan pembagian peran pemilik topik (`slides`, `banner-design`, `design`, `design-system`, `ui-styling`, `brand`).
- `.agents/skills/design-system/SKILL.md`: memindahkan narasi Slide System ke `slides/references/slide-system.md`, menyisakan ringkasan singkat yang menunjuk ke `slides`, dan merapikan path tabel `Scripts` (`scripts/...`) dan `Templates` (`templates/...`).
- `.agents/skills/slides/SKILL.md`: menghapus `## Subcommands`, menambah `references/slide-system.md` ke tabel referensi, dan mengganti `## Routing` dengan decision tree tanpa `$ARGUMENTS`.
- `.agents/skills/slides/references/create.md`: menulis ulang dari placeholder `<task>$ARGUMENTS</task>` menjadi alur kerja 5 langkah nyata (termasuk cek `assets/design-tokens.css`, `search-slides.py`, CSV, HTML/Chart.js, dan `slide-token-validator.py`).

### Fase 4 — Dependensi yang Tidak Ada (`f7a49c7`)
- `.agents/skills/banner-design/SKILL.md`: menghapus `ai-artist`, `ai-multimodal`, `chrome-devtools`, `assets-organizing`, `.venv`, riset Pinterest wajib, dan bagian `## Security`; mengganti ekspor menjadi `.agents/skills/banner-design/scripts/export-png.py`.
- `.agents/skills/design/SKILL.md`: memperbarui alur `Social Photos (Built-in)` dan bagian `## Integration` agar tidak merujuk `project-management`, `assets-organizing`, `chrome-devtools`, atau `ai-multimodal`.
- `.agents/skills/design/references/social-photos-design.md`: menghapus `project-management`, `assets-organizing`, `chrome-devtools`, `ai-artist`, dan `plans/reports/`; mengganti langkah ekspor dan verifikasi dengan `.agents/skills/banner-design/scripts/export-png.py`.
- `.agents/skills/brand/scripts/extract-colors.cjs`: menghapus rujukan `ai-multimodal` dan `gemini_batch_process.py`, menggantinya dengan saran generik penggunaan alat analisis gambar yang tersedia.

### Fase 5 — Perbaiki `ui-ux-pro-max/SKILL.md` (`f7b0291`)
- `.agents/skills/ui-ux-pro-max/SKILL.md`: menghapus seluruh rujukan `Quick Reference` yang tidak ada, menghapus seluruh teks dan tanda baca Mandarin, menambahkan contoh pemicu berbahasa Indonesia, mengganti ID rule fiktif di `Common Sticking Points` dengan perintah pencarian konkret (`--domain`), menyertakan aturan ikon Phosphor (`data/icons.csv`), serta meringkas `Pre-Delivery Checklist (Web)` ke 8 butir dengan rujukan ke `references/native-app-checklist.md`.
- `.agents/skills/caveman/README.md`: menghapus sisa kata `ultracave` dan `megacave` pada baris 10 agar pencarian Fase 9a bersih.

### Fase 6 — Tulis Ulang `description` Frontmatter (`15d6a7f`)
- Memperbarui field `description` pada frontmatter ke kalimat orang ketiga (apa + kapan dipakai):
  - `.agents/skills/banner-design/SKILL.md`
  - `.agents/skills/brand/SKILL.md`
  - `.agents/skills/design/SKILL.md`
  - `.agents/skills/design-system/SKILL.md`
  - `.agents/skills/slides/SKILL.md`
  - `.agents/skills/ui-styling/SKILL.md`
  - `.agents/skills/ui-ux-pro-max/SKILL.md`
- `.agents/skills/caveman/SKILL.md`: tidak diubah.

---

## 2. Daftar File yang Dihapus

### Fase 3 (duplikat identik di `design/references/`, commit `27633d1`)
- `.agents/skills/design/references/slides.md`
- `.agents/skills/design/references/slides-create.md`
- `.agents/skills/design/references/slides-layout-patterns.md`
- `.agents/skills/design/references/slides-html-template.md`
- `.agents/skills/design/references/slides-copywriting-formulas.md`
- `.agents/skills/design/references/slides-strategies.md`
- `.agents/skills/design/references/banner-sizes-and-styles.md`

### Fase 7 (kebersihan paket, commit `5af41b3`)
- `.agents/skills/ui-styling/scripts/tests/coverage-ui.json`
- `.agents/skills/ui-ux-pro-max/scripts/tests/test_catalog_refresh.py`
- `.agents/skills/ui-ux-pro-max/scripts/tests/test_relevance_evaluator.py`
- Seluruh 13 file fixture di `.agents/skills/ui-ux-pro-max/scripts/tests/fixtures/` (`catalogs/google-api.json`, `catalogs/google-catalog.json`, `catalogs/google-existing.csv`, `catalogs/google-metadata.json`, `catalogs/google-overrides.json`, `catalogs/icons-curated.csv`, `catalogs/phosphor-core.json`, `catalogs/phosphor-package.json`, `catalogs/phosphor-react-exports.json`, `catalogs/phosphor-react-package.json`, `relevance-baseline.json`, `relevance-cases.json`, `relevance-thresholds.json`) setelah diverifikasi tidak dirujuk oleh 8 file test lainnya.
- Seluruh folder `__pycache__/` dan file `*.pyc` di bawah `.agents/skills/` (dibersihkan kembali setelah verifikasi Fase 9).

---

## 3. File Baru yang Dibuat

1. `.agents/skills/slides/references/slide-system.md` (Fase 3) — Berisi Daftar Isi, bagian `Prerequisites` untuk menghasilkan `assets/design-tokens.css` dari `.agents/skills/design-system/templates/design-tokens-starter.json`, serta seluruh dokumentasi Slide Search BM25, CSV, Contextual Decision Flow, Pattern Breaking, Slide Requirements, Chart.js Integration, dan Token Compliance yang dipindahkan dari `design-system/SKILL.md`.
2. `.agents/skills/banner-design/scripts/export-png.py` (Fase 4) — Script Python 3 berbasis Playwright dengan argumen `--html`, `--width`, `--height`, `--output`, `--scale` (default `2`), dan `--help`. Teruji merender HTML ke PNG sebesar 8.517 byte menggunakan browser Chromium/Chrome sistem, lalu file uji HTML/PNG sementara dihapus.
3. `.agents/skills/ui-ux-pro-max/references/native-app-checklist.md` (Fase 5) — Menampung bagian `Common Rules for Professional UI` dan `Pre-Delivery Checklist` khusus App UI (iOS/Android/React Native/Flutter), dengan aturan ikon Phosphor diterjemahkan ke bahasa Inggris dan menunjuk ke `data/icons.csv`.
4. `.agents/FIX_REPORT.md` — Laporan akhir ini.

---

## 4. Keputusan di Luar Brief dan Alasannya

1. **Mengeluarkan `.agents/` dari `.gitignore`**: Sebelumnya `.agents/` berada di `.gitignore`, sehingga `git checkout -b fix/skills` tidak dapat melacak dan membuat commit per fase. Baris `.agents/` dihapus dari `.gitignore` agar setiap fase memiliki riwayat commit terpisah yang mudah diaudit/dikembalikan.
2. **Membersihkan `caveman/README.md`**: Selain mengedit `caveman/SKILL.md` (Fase 2e), tabel/contoh/kalimat benchmark pada `caveman/README.md` yang menyebut `ultracave`, `megacave`, dan hook Claude Code ikut dibersihkan karena `README.md` adalah file `.md` di bawah `.agents/skills/` yang ikut dipindai oleh pemeriksaan Fase 9a (`grep -rnE '...ultracave|megacave...'`).
3. **Fallback Channel Browser di `export-png.py`**: `python -m playwright install chromium` sempat mengunduh arsip 207 MiB dengan kecepatan lambat; untuk memastikan `export-png.py` tetap andal di mesin Windows yang sudah terpasang Chrome atau Microsoft Edge ketika binary Playwright default belum terunduh, `export-png.py` mencoba `p.chromium.launch()`, lalu fallback ke `channel="chrome"` dan `channel="msedge"`, dan tetap keluar dengan kode `1` serta pesan `pip install playwright && python -m playwright install chromium` jika ketiganya gagal.
4. **Menghapus rujukan `slide-animations.css` dan `claudekit-pitch-251223.html`**: Pemeriksaan terhadap `design-system/scripts/generate-slide.py` memastikan script tersebut tidak membutuhkan atau merujuk `slide-animations.css`, sehingga keduanya dihapus dari dokumentasi sesuai Fase 3d.
5. **Pengujian UI checklist di `ui-ux-pro-max/SKILL.md` dan pemetaan `Common Sticking Points`**: Pengecekan pada `.agents/skills/ui-ux-pro-max/data/ux-guidelines.csv` menemukan bahwa rule ID seperti `color-dark-mode`, `spring-physics`, `inline-validation`, dan `nav-hierarchy` tidak ada di CSV; oleh karena itu seluruh kolom kedua pada tabel `Common Sticking Points` diubah menjadi perintah pencarian konkret (`--domain style`, `--domain ux`, `--domain react`).
6. **Penambahan `banner-design` ke target hapus `argument-hint:`**: Selain 5 file lain, `banner-design/SKILL.md` juga memiliki `argument-hint:` di frontmatter sehingga ikut dihapus di Fase 2a.
7. **Menjalankan `python -B -m py_compile` masih menghasilkan `__pycache__` di Python 3.10**: Setelah verifikasi Fase 9 selesai, seluruh `__pycache__` dan `*.pyc` dihapus kembali agar paket tetap bersih sebelum commit laporan akhir.

---

## 5. Hasil Verifikasi Fase 9 & Perbandingan dengan Baseline

### a. Pencarian Sisa (Fase 9a — seluruh pemeriksaan = 0 temuan)
- `path .claude/skills` (`.md`, `.py`, `.cjs`): `0 hits`
- `claude code features` (`$ARGUMENTS|AskUserQuestion|/ck:|/ckm:|argument-hint`): `0 hits`
- `missing deps` (`ai-artist|ai-multimodal|chrome-devtools|assets-organizing|project-management|ultracave|megacave|.venv`): `0 hits`
- `Quick Reference` di `ui-ux-pro-max/SKILL.md`: `0 hits`
- Karakter/tanda baca CJK di `ui-ux-pro-max/SKILL.md`: `0 hits`

### b. Frontmatter dan Tautan Internal (Fase 9b)
- Output skrip validasi YAML frontmatter & tautan (`references/`, `scripts/`, `data/`, `templates/`, dan path `.agents/skills/...`): **`OK`**

### c. Script dan Test (Fase 9c)
- `python -B -m py_compile` pada semua `.py` di `.agents/skills/`: `PY_COMPILE_DONE` (semua exit code `0`, tidak ada `FAIL`)
- `node --check` pada semua `.cjs` di `.agents/skills/`: `NODE_CHECK_DONE` (semua exit code `0`, tidak ada `FAIL`)
- `python -B .agents/skills/banner-design/scripts/export-png.py --help`: exit `0`, menampilkan seluruh argumen (`--html`, `--width`, `--height`, `--output`, `--scale`)
- `python -B .agents/skills/ui-ux-pro-max/scripts/search.py "beauty spa wellness service" --design-system -p "Serenity Spa"`: exit `0`, mengembalikan desain sistem lengkap (`Soft UI Evolution`)
- `python -B .agents/skills/design/scripts/logo/search.py "tech startup modern" --design-brief -p "BrandName"`: exit `0`, mengembalikan design brief (`Hexagonal`, `Geometric`, `Slate Professional`, `Steel Industrial`)

**Perbandingan Pytest Baseline vs Fase 9c:**

| Suite Test | Baseline (sebelum ubahan, dengan `--ignore`) | Fase 9c (sesudah ubahan, tanpa `--ignore`) | Status |
|---|---|---|---|
| `.agents/skills/ui-ux-pro-max/scripts/tests` | 130 passed (7923 subtests, 16.40s) | **130 passed** (7923 subtests, 12.85s) | Cocok 100% |
| `.agents/skills/brand/scripts/tests` | 1 passed (0.16s) | **1 passed** (0.27s) | Cocok 100% |
| `.agents/skills/design-system/scripts/tests` | 2 passed (0.22s) | **2 passed** (0.31s) | Cocok 100% |
| `.agents/skills/ui-styling/scripts/tests` | 58 passed (0.66s) | **58 passed** (1.20s) | Cocok 100% |

### d. Jumlah Baris `SKILL.md` (Fase 9d — semua ≤ 500 baris)
- `banner-design/SKILL.md`: 137 baris
- `brand/SKILL.md`: 91 baris
- `caveman/SKILL.md`: 89 baris
- `design/SKILL.md`: 240 baris
- `design-system/SKILL.md`: 118 baris
- `slides/SKILL.md`: 37 baris
- `ui-styling/SKILL.md`: 323 baris
- `ui-ux-pro-max/SKILL.md`: 315 baris

---

## 6. Masalah yang Ditemukan Tetapi Sengaja Tidak Diperbaiki

1. **ID Model Gemini Tersebar di Banyak File** (sesuai Fase 8): ID model `gemini-2.5-flash-image`, `gemini-3-pro-image-preview`, dan `gemini-3.1-pro-preview` masih tersebar di `design/SKILL.md`, `design/references/cip-design.md`, `design/references/icon-design.md`, `design/scripts/cip/generate.py`, `design/scripts/icon/generate.py`, dan `design/scripts/logo/generate.py`, belum dipusatkan ke satu file konfigurasi.
2. **Script yang Memerlukan `GEMINI_API_KEY` dan Jaringan**: Script `design/scripts/logo/generate.py`, `design/scripts/cip/generate.py`, dan `design/scripts/icon/generate.py` tidak dijalankan secara langsung (hanya diperiksa sintaksnya dengan `py_compile`) sesuai aturan kerja #3 dan Fase 8.
3. **File `design-system/scripts/fetch-background.py`**: Juga membutuhkan jaringan eksternal (Pexels/Unsplash), sehingga hanya diverifikasi sintaksnya dengan `py_compile`.
4. **Rujukan ke `scripts/...` relatif di `brand/SKILL.md`, `design-system/SKILL.md`, dan `ui-styling/SKILL.md`**: Bagian `Quick Start` / `Utility Scripts` pada beberapa `SKILL.md` masih menuliskan `scripts/<nama-script>` tanpa awalan `.agents/skills/<skill>/`. Hal ini masih lolos pemeriksa tautan Fase 9b dan dibiarkan karena aturan kerja #2 membatasi perubahan agar sekecil mungkin sesuai brief.
5. **Kolom ke-4 `ui-ux-pro-max/SKILL.md` (315 baris vs target ±300 baris)**: Tetap ringkas di sekitar 300 baris dan jauh di bawah batas keras 500 baris pada Fase 9d tanpa memangkas isi checklist web atau aturan query contract.
