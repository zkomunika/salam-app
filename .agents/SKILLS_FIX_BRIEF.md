# Briefing: Perbaikan Skill di `.agents/skills/`

Dokumen ini ditujukan untuk agent (Antigravity). Kerjakan semua fase di bawah secara berurutan, lalu tulis laporan di `.agents/FIX_REPORT.md`. Seluruh perintah dijalankan dari **root project**, bukan dari dalam folder skill.

Simpan dokumen ini di `.agents/SKILLS_FIX_BRIEF.md`, di luar `.agents/skills/`, supaya tidak terbaca sebagai skill.

## 1. Latar belakang

Delapan skill di `.agents/skills/` (`banner-design`, `brand`, `caveman`, `design`, `design-system`, `slides`, `ui-styling`, `ui-ux-pro-max`) berasal dari bundel claudekit yang ditulis untuk **Claude Code**. Di Antigravity, folder workspace yang dipakai adalah `.agents/skills/<nama-skill>/SKILL.md`, jadi lokasinya sudah benar. Yang salah adalah isinya: banyak path, perintah, dan dependensi yang hanya ada di Claude Code.

Aturan Antigravity yang relevan:

- Frontmatter yang didokumentasikan hanya `name` (opsional, default nama folder) dan `description` (wajib). Field lain tidak dikenal.
- Agent melihat `name` + `description` semua skill di setiap giliran, lalu membaca `SKILL.md` penuh hanya jika relevan. Jadi description yang tumpang tindih membuat agent salah memilih skill.
- Tulis description dalam kalimat orang ketiga, sebutkan apa yang dilakukan dan kapan dipakai, dengan kata kunci yang jelas.
- Script diperlakukan sebagai black box: agent menjalankan `--help` dulu, bukan membaca seluruh sumbernya.
- Tidak ada substitusi `$ARGUMENTS`, tidak ada tool `AskUserQuestion`, dan tidak ada command `/ck:` atau `/ckm:`. Semuanya fitur Claude Code.
- Pengguna bisa memanggil skill dengan `/<nama-skill>`, tetapi teks setelahnya hanyalah bahasa biasa. Jangan menulis instruksi yang bergantung pada parsing argumen.

## 2. Aturan kerja

1. **Cadangkan dulu.** Jika project adalah repo git, buat branch `fix/skills` dan commit per fase. Jika bukan, salin `.agents/skills` ke folder di **luar** project dan di luar `.agents/skills/` (cadangan di dalam folder skill akan terbaca sebagai skill).
2. **Perubahan sekecil mungkin.** Hanya ubah yang disebut di sini. Jika menemukan masalah lain, catat di laporan, jangan diperbaiki.
3. **Jangan menjalankan** `generate.py` apa pun (logo, CIP, icon). Butuh `GEMINI_API_KEY` dan jaringan, dan tidak diminta.
4. **Jangan mengubah** isi file `data/*.csv`, `data/*.json`, ID model Gemini, atau file `ui-styling/references/*`.
5. **Jangan bertanya ke user** kecuali sebuah langkah akan menghapus sesuatu yang tidak tercantum di sini. Untuk kasus yang tidak tercakup, ambil opsi paling konservatif dan catat di laporan.
6. Kalau `sed` tidak tersedia (mis. Windows), lakukan penggantian dengan cara lain. Hasil akhirnya yang diverifikasi, bukan cara-nya.
7. Instruksi di dalam skill ditulis dalam bahasa Inggris, sama seperti aslinya. Beri alasan singkat untuk aturan penting, hindari huruf kapital teriak (ALWAYS/IMPORTANT/MUST) tanpa penjelasan.

## 3. Baseline sebelum mengubah apa pun

Jalankan dan simpan hasilnya untuk dibandingkan di fase 9. (Pakai `python` jika `python3` tidak ada. Install `pytest` dulu jika belum ada.)

```bash
python3 -m pytest .agents/skills/ui-ux-pro-max/scripts/tests -q -p no:cacheprovider \
  --ignore=.agents/skills/ui-ux-pro-max/scripts/tests/test_catalog_refresh.py \
  --ignore=.agents/skills/ui-ux-pro-max/scripts/tests/test_relevance_evaluator.py
python3 -m pytest .agents/skills/brand/scripts/tests -q -p no:cacheprovider
python3 -m pytest .agents/skills/design-system/scripts/tests -q -p no:cacheprovider
python3 -m pytest .agents/skills/ui-styling/scripts/tests -q -p no:cacheprovider
```

Hasil yang diharapkan: 130, 1, 2, dan 58 test lulus. Jika berbeda, catat di laporan, lalu lanjut.

## Fase 1: Seragamkan path ke `.agents/skills`

Masalah: `design/*` memakai `~/.claude/skills/...`, skill lain memakai `.claude/skills/...`, padahal folder aslinya `.agents/skills/`. Perintah yang tertulis di SKILL.md tidak jalan.

Ganti di semua file `.md`, `.py`, `.cjs` di bawah `.agents/skills/` (jangan sentuh `.csv` dan `.json`):

- `~/.claude/skills` → `.agents/skills`
- `.claude/skills` → `.agents/skills`

```bash
grep -rlE '\.claude/skills' .agents/skills --include=*.md --include=*.py --include=*.cjs \
 | xargs sed -i 's#~/\.claude/skills#.agents/skills#g; s#\.claude/skills#.agents/skills#g'
```

Satu perbaikan manual: `brand/scripts/sync-brand-to-tokens.cjs` baris 20 menghardcode path ke `design-system/scripts/generate-tokens.cjs` relatif terhadap cwd. Baca file-nya, lalu ganti dengan path berbasis `__dirname`:

```js
const GENERATE_TOKENS_SCRIPT = path.resolve(__dirname, '..', '..', 'design-system', 'scripts', 'generate-tokens.cjs');
```

Pastikan `path` sudah di-`require` di file itu. Setelah mengganti, pastikan script masih bisa dijalankan dengan `node --check`.

Catatan: setelah fase ini, perintah di skill mengasumsikan skill dipasang di workspace dan dijalankan dari root project. Itu sesuai dengan pemakaian saat ini.

## Fase 2: Buang fitur khusus Claude Code

### 2a. Frontmatter
Hapus baris `argument-hint:` dari semua `SKILL.md`. Biarkan `license` dan `metadata`.

### 2b. `$ARGUMENTS`
Ditemukan di `slides/SKILL.md`, `brand/SKILL.md` (bagian "Routing"), `brand/references/update.md` (baris 3, `<args>$ARGUMENTS</args>`), serta `slides/references/create.md` (ditangani di fase 3). Tulis ulang bagian "Routing" sebagai decision tree bahasa biasa. Contoh untuk `brand`:

```markdown
## Routing

- User wants to change or sync brand identity (colors, fonts, voice) → read `references/update.md` and follow it.
- User wants a brand review or audit → read `references/consistency-checklist.md` and `references/approval-checklist.md`.
- User wants a new brand guideline document → start from `templates/brand-guidelines-starter.md`.
- Otherwise use the reference table above to load only the file that matches the question.
```

Untuk `slides`, bentuk yang sama, lihat fase 3. Di `brand/references/update.md`, hapus tag `<args>…</args>` dan ganti dengan kalimat "Use the user's request as the update brief."

### 2c. `AskUserQuestion`
Ganti dengan "ask the user directly in chat and wait for the answer before continuing". Lokasi: `banner-design/SKILL.md` (2 tempat), `brand/references/update.md:16`, `design/references/logo-design.md:78`, `design/references/social-photos-design.md:54`, `design/SKILL.md` (sekitar baris 69, 140, 227). Sesuaikan tata bahasa kalimat sekitarnya.

### 2d. Command `/ck:`, `/ckm:`, `/slides:create`, `/brand update`, `/ui-ux-pro-max`
- Di `design/SKILL.md` (sekitar baris 69 dan 228) dan `design/references/social-photos-design.md` (baris 60 sampai 62): ganti `/ckm:brand`, `/ckm:design-system`, `/ck:ui-ux-pro-max`, `/ck:frontend-design`, dan `/ui-ux-pro-max` dengan "use the `brand` skill", "use the `design-system` skill", dan seterusnya (agent membaca SKILL.md skill itu, bukan menjalankan slash command).
- Hapus kalimat "randomly invoke ONE of … OR …". Ganti dengan kriteria: pakai `ui-ux-pro-max` bila perlu memilih style, palet, atau font; pakai `frontend-design` bila perlu eksekusi layout. Jika skill `frontend-design` tidak ada di `.agents/skills/` maupun di skill global, tulis "if available".
- Di `design-system/SKILL.md` bagian "Command" (`/slides:create "..."`): ganti dengan contoh permintaan bahasa biasa, misalnya `Create a 10-slide investor pitch for ClaudeKit Marketing`.
- Di `brand/SKILL.md` (`/brand update` dan sejenisnya): ganti dengan "when the user asks to update brand identity".

### 2e. `caveman`
- Hapus kalimat `` `/caveman ultra` and `/caveman wenyan` are aliases: follow the `ultracave` or `megacave` skill… `` karena skill itu tidak ada.
- Pada `/caveman status`: hapus rujukan ke "hook". Sisakan: laporkan mode yang sedang kamu ikuti, atau `off` jika caveman dimatikan atau belum pernah aktif.
- Di "When to break the rules" butir 5, hapus "`/caveman-compress` exempt."
- Jangan ubah aturan lain.

## Fase 3: Hapus duplikasi dan tetapkan satu pemilik per topik

Pembagian peran yang dituju:

| Topik | Pemilik |
|---|---|
| Presentasi | `slides` |
| Banner dan cover | `banner-design` |
| Logo, CIP, icon, social photos | `design` |
| Design token dan spesifikasi komponen | `design-system` |
| shadcn/Tailwind | `ui-styling` |
| Brand voice/identitas | `brand` |

### 3a. Verifikasi lalu hapus file duplikat
Sebelum menghapus, pastikan file-nya memang identik:

```bash
cd .agents/skills
for p in "copywriting-formulas:slides-copywriting-formulas" "html-template:slides-html-template" \
         "layout-patterns:slides-layout-patterns" "slide-strategies:slides-strategies"; do
  diff -q slides/references/${p%%:*}.md design/references/${p##*:}.md
done
diff -q banner-design/references/banner-sizes-and-styles.md design/references/banner-sizes-and-styles.md
```

Jika semuanya identik (tidak ada output), hapus dari `design/references/`: `slides.md`, `slides-create.md`, `slides-layout-patterns.md`, `slides-html-template.md`, `slides-copywriting-formulas.md`, `slides-strategies.md`, dan `banner-sizes-and-styles.md`. Jika ada yang berbeda, gabungkan ke versi di `slides/` atau `banner-design/` lalu hapus salinannya, dan catat di laporan.

### 3b. Rapikan `design/SKILL.md`
- Hapus bagian "Slides (Built-in)" dan "Banner Design (Built-in)" beserta tabel pengetahuan dan referensinya. Tambahkan satu baris pengganti di tabel routing: presentasi → skill `slides`, banner → skill `banner-design`.
- Pada tabel routing, kolom "External skill" untuk `brand`, `design-system`, dan `ui-styling` diganti menjadi "Sibling skill in `.agents/skills/`". Ketiganya ada di folder yang sama.
- Hapus baris tabel References dan Scripts yang menunjuk file yang baru dihapus. Di "Complete Brand Package", langkah presentasi menjadi "use the `slides` skill".
- Baca `design/references/design-routing.md`, lalu samakan dengan pembagian peran di atas.
- Sisanya (logo, CIP, icon, social photos) tidak diubah.

### 3c. Jadikan `slides` pemilik workflow presentasi
Mesin slide yang sebenarnya (script dan CSV) ada di `design-system`, sedangkan narasi workflow-nya tercecer. Lakukan:

1. Buat `slides/references/slide-system.md` berisi bagian-bagian dari `design-system/SKILL.md` berikut, dipindahkan apa adanya: Slide Search (BM25), Decision System CSVs, Contextual Decision Flow, Pattern Breaking, Slide Requirements, Chart.js Integration, Token Compliance. Beri daftar isi di awal file. Perbaiki contoh perintah menjadi path lengkap, misalnya `python .agents/skills/design-system/scripts/search-slides.py "investor pitch"`, bukan `scripts/search-slides.py`.
2. Di `design-system/SKILL.md`, ganti bagian-bagian itu dengan 3 sampai 4 baris: untuk membuat presentasi gunakan skill `slides`; script dan CSV slide ada di folder ini dan dipakai oleh skill itu. Pertahankan tabel Scripts dan Templates.
3. Tulis ulang `slides/references/create.md` (sekarang hanya berisi "Invoke `slides` skill", yaitu memanggil dirinya sendiri). Isi dengan workflow nyata: (a) pahami tujuan, audiens, dan jumlah slide dari permintaan user; (b) pilih strategi dengan `search-slides.py`; (c) untuk tiap slide tentukan layout, tipografi, dan warna dari CSV; (d) buat HTML dari `html-template.md` dengan token dan Chart.js; (e) validasi dengan `slide-token-validator.py`. Tanpa `<task>$ARGUMENTS</task>`.
4. Ubah `slides/SKILL.md`: hapus bagian "Subcommands", ganti "Routing" dengan decision tree (jika user minta presentasi baru → `references/create.md`; jika butuh layout → `references/layout-patterns.md`; dan seterusnya), tambahkan `slide-system.md` ke tabel referensi.

### 3d. Prasyarat project untuk slide
Skill mewajibkan slide mengimpor `assets/design-tokens.css`, tetapi file itu tidak dibundel dan hanya ada jika project sudah punya. Di `slides/references/slide-system.md`, tambahkan bagian "Prerequisites":

- Jika `assets/design-tokens.css` belum ada di root project, buat dari starter: salin `design-system/templates/design-tokens-starter.json` ke `assets/design-tokens.json`, lalu jalankan `node .agents/skills/design-system/scripts/generate-tokens.cjs --config assets/design-tokens.json -o assets/design-tokens.css`. Verifikasi dulu argumen script dengan `--help` atau membaca bagian argumen-nya.
- `assets/css/slide-animations.css` dan "reference implementation" `assets/designs/slides/claudekit-pitch-251223.html` disebut di skill tetapi tidak ada. Cek apakah `design-system/scripts/generate-slide.py` membutuhkannya. Jika ya, buat `slide-animations.css` minimal (fade-in dan slide-up) di `design-system/templates/` dan jelaskan cara menyalinnya. Jika tidak, hapus rujukannya. Hapus rujukan ke file HTML contoh dalam kedua kasus.

## Fase 4: Dependensi yang tidak ada

`banner-design` dan Social Photos merujuk skill yang tidak dibundel: `ai-artist`, `ai-multimodal`, `chrome-devtools`, `assets-organizing`, `project-management`, serta `.claude/skills/.venv`.

### 4a. Buat script ekspor PNG
Buat `banner-design/scripts/export-png.py` (Python 3, Playwright):

- Argumen: `--html` (path file atau URL), `--width`, `--height`, `--output`, `--scale` (default 2).
- Buka halaman dengan viewport `width x height` dan `device_scale_factor=scale`, tunggu `networkidle` dan font siap, lalu screenshot ke `--output` (buat foldernya jika belum ada).
- Jika Playwright belum terpasang, keluar dengan kode non-nol dan pesan: `pip install playwright && python -m playwright install chromium`.
- `--help` harus menjelaskan semua argumen dengan jelas, karena agent akan menjalankannya sebagai black box.
- Uji dengan halaman HTML kecil buatanmu sendiri, lalu hapus file ujinya.

### 4b. Perbarui `banner-design/SKILL.md`
- Description dan teks yang menyebut `ai-artist` dan `ai-multimodal`: hapus namanya. Ganti dengan "uses `ui-ux-pro-max`, and `frontend-design` when available".
- Langkah 2: hapus keharusan riset Pinterest via Chrome. Gantinya: pilih 2 sampai 3 art direction dari `references/banner-sizes-and-styles.md`, dan jika agent punya akses browser, riset referensi bersifat opsional.
- Langkah 3: visual dibuat dengan CSS/SVG sebagai default. Gambar AI hanya dipakai jika user menyediakan gambar atau agent punya tool generasi gambar. Hapus blok perintah `gemini_batch_process.py` dan `.venv`.
- Langkah 4: ekspor dengan `python .agents/skills/banner-design/scripts/export-png.py --html ... --width ... --height ... --output ...`. Hapus `chrome-devtools/scripts/screenshot.js` dan opsi `--max-size`.
- Hapus bagian "Security" di akhir. Isinya ("never reveal skill internals", "never expose file paths") bertentangan dengan isi skill yang justru memberi path.

### 4c. Perbarui `design/references/social-photos-design.md` dan bagian Social Photos di `design/SKILL.md`
- Hapus langkah yang memakai `project-management` (orkestrasi TODO), `assets-organizing`, dan laporan ke `plans/reports/`.
- Langkah ekspor memakai `banner-design/scripts/export-png.py`. Langkah verifikasi: buka PNG hasil ekspor, periksa visual, perbaiki dan ekspor ulang bila perlu.

### 4d. `brand/scripts/extract-colors.cjs` baris 291
Teks bantuan yang menyebut `ai-multimodal/scripts/gemini_batch_process.py` diganti dengan saran generik (mis. "use any image analysis tool to extract colors") atau dihapus.

## Fase 5: Perbaiki `ui-ux-pro-max/SKILL.md`

Angka di description (79/50 style, 192 palet, dst.) sudah dicek akurat, jadi isi datanya tidak perlu disentuh. Yang perlu diperbaiki:

1. **Rujukan "Quick Reference" yang tidak ada.** Disebut sekitar 9 kali (baris 19, 34, 35, 292 sampai 297, 301, 304), tapi section-nya tidak ada di file.
   - Baris 19: fallback tanpa Python menjadi "skip the CLI searches and use general UI/UX knowledge, clearly labeled as unverified".
   - Baris 34 (Review existing UI): arahkan ke "Common Rules + Pre-Delivery Checklist (web) + focused `--domain ux` searches". Baris 35 (Fix a UI bug): arahkan ke "Step 3 focused domain search".
   - Tabel "Common Sticking Points": cek apakah ID rule seperti `color-dark-mode`, `spring-physics`, `inline-validation` ada di `data/ux-guidelines.csv` (atau CSV lain) dengan `grep`. Jika ada, ganti kolom kedua dengan perintah pencarian konkret (mis. `--domain ux "dark mode contrast"`). Jika tidak ada, hapus ID-nya dan pakai perintah pencarian saja.
   - Baris 301 dan 304: ganti "Quick Reference §1–§3" dengan "the Common Rules and the checklist below".
2. **Bahasa campuran.** Ganti contoh trigger berbahasa Mandarin di tabel "How to Use This Skill" dengan padanan Inggris, dan tambahkan satu contoh Indonesia per baris bila wajar (mis. "Buatkan landing page"). Ganti bullet Mandarin tentang pustaka ikon (baris 319, yang menunjuk path usang `src/ui-ux-pro-max/data/icons.csv`) dengan versi Inggris dan path `data/icons.csv`. Isi bullet: pustaka ikon default Phosphor (`@phosphor-icons/react`), `data/icons.csv` hanya daftar pilihan umum, pilih dari set Phosphor lengkap bila tidak ada yang cocok, dan Heroicons sebagai cadangan dengan gaya yang konsisten.
3. **Checklist khusus aplikasi native.** Pindahkan "Common Rules for Professional UI" dan "Pre-Delivery Checklist" ke `references/native-app-checklist.md` (isi tidak diubah). Di `SKILL.md`, ganti dengan "Pre-Delivery Checklist (web)" yang pendek, maksimal 8 butir, diturunkan dari isi skill sendiri: kontras teks ≥ 4.5:1, fokus keyboard terlihat, semua kontrol bernama, uji di lebar 375px, dukung reduced-motion, tanpa emoji sebagai ikon, tanpa layout shift, tema gelap diuji terpisah. Beri petunjuk: untuk UI native iOS/Android baca `references/native-app-checklist.md`.
4. Description dipersingkat sesuai fase 6 (jangan diulang persis di badan file).

Target: `SKILL.md` di bawah ±300 baris.

## Fase 6: Tulis ulang description (kalimat orang ketiga, apa + kapan)

Ganti `description` di frontmatter dengan teks berikut (boleh disesuaikan sedikit jika setelah fase 3 sampai 5 cakupan skill berubah):

- **banner-design**: `Designs banners and cover images for social media, display ads, website heroes, and print, offering several art directions per request as HTML/CSS exported to PNG at exact dimensions. Use when the user asks for a banner, cover, header, hero image, ad creative, or a graphic at a specific platform size.`
- **brand**: `Defines and audits brand identity: voice and tone, visual identity, messaging framework, color and typography specs, logo usage, and asset naming and approval, and syncs brand guidelines to design tokens. Use when the user asks about brand voice, style guides, brand consistency review, or updating brand guidelines.`
- **design**: `Toolkit and router for visual identity work: logo generation, corporate identity (CIP) mockups, SVG icon generation, and multi-platform social media images, using Gemini-based scripts. Use when the user asks to design a logo, CIP or stationery mockups, icons, or social media photos. For banners use banner-design, for presentations use slides, for tokens use design-system, for UI code use ui-styling.`
- **design-system**: `Builds design token systems (primitive, semantic, component layers), CSS variables, spacing and typography scales, and component specs and states; also hosts the slide-generation scripts and data used by the slides skill. Use when the user asks for design tokens, theming, CSS variable systems, or component specifications.`
- **slides**: `Creates strategic HTML presentations (pitch decks, sales and investor decks) with Chart.js charts, design tokens, layout patterns, and copywriting formulas. Use when the user asks for slides, a presentation, a pitch deck, or a deck.`
- **ui-styling**: `Builds accessible interfaces with shadcn/ui, Radix, and Tailwind CSS: components, responsive layouts, theming, dark mode, plus canvas-based visual designs. Use when the user is implementing or styling UI in React-based stacks with Tailwind or shadcn/ui.`
- **ui-ux-pro-max**: `Provides UI/UX design intelligence for web, mobile, and desktop: design-system generation, styles, palettes, font pairings, UX and accessibility guidelines, chart choices, and stack-specific guidance from searchable local data. Use when designing, building, reviewing, or fixing interfaces.`
- **caveman**: tidak diubah.

## Fase 7: Kebersihan paket

- Hapus semua folder `__pycache__` dan file `*.pyc` di bawah `.agents/skills/`.
- Hapus `ui-styling/scripts/tests/coverage-ui.json`.
- Hapus `ui-ux-pro-max/scripts/tests/test_catalog_refresh.py` dan `test_relevance_evaluator.py`. Keduanya mencari script dari repo asal (`refresh-google-fonts.py`, `evaluate-relevance.py`) yang tidak ikut dibundel, jadi tidak bisa jalan. Setelah itu, hapus fixture yang tidak lagi dirujuk test lain, dan pastikan dulu dengan `grep -r <nama-file> .agents/skills/ui-ux-pro-max/scripts/tests`.

## Fase 8: Hal yang tidak boleh dikerjakan

- Memusatkan ID model Gemini ke satu config. Disarankan, tetapi di luar lingkup ini. Cukup catat di laporan.
- Memangkas data atau CSV.
- Mengubah `ui-styling/references/*`.
- Menjalankan script yang butuh API key atau jaringan.

## Fase 9: Verifikasi (definisi selesai)

Semua pengecekan harus lolos sebelum melapor.

**a. Pencarian sisa. Setiap perintah harus tidak menghasilkan apa pun:**

```bash
grep -rnE '\.claude/skills' .agents/skills --include=*.md --include=*.py --include=*.cjs
grep -rnE '\$ARGUMENTS|AskUserQuestion|/ck:|/ckm:|argument-hint' .agents/skills --include=*.md
grep -rnE 'ai-artist|ai-multimodal|chrome-devtools|assets-organizing|project-management|ultracave|megacave|\.venv' .agents/skills --include=*.md --include=*.py --include=*.cjs
grep -n 'Quick Reference' .agents/skills/ui-ux-pro-max/SKILL.md
grep -nP '[\x{4e00}-\x{9fff}]' .agents/skills/ui-ux-pro-max/SKILL.md
```

(Perintah terakhir memakai PCRE, jadi jalankan dengan `grep -P`. Jika tidak tersedia, cari karakter CJK dengan Python. Pada Windows gunakan `Select-String` yang setara.)

**b. Frontmatter dan tautan internal:**

```python
import re, pathlib, yaml
root = pathlib.Path(".agents/skills")
bad = []
for skill in sorted(root.glob("*/SKILL.md")):
    text = skill.read_text(encoding="utf-8")
    m = re.match(r"^---\n(.*?)\n---\n", text, re.S)
    fm = yaml.safe_load(m.group(1)) if m else None
    if not fm or "description" not in fm:
        bad.append(f"{skill}: frontmatter invalid")
    for ref in set(re.findall(r"`((?:references|scripts|data|templates)/[\w./\-]+)`", text)):
        if not (skill.parent / ref).exists():
            bad.append(f"{skill}: missing {ref}")
for f in root.rglob("*"):
    if f.suffix in {".md", ".py", ".cjs"} and "__pycache__" not in f.parts:
        for p in set(re.findall(r"\.agents/skills/[\w./\-]+", f.read_text(encoding="utf-8", errors="ignore"))):
            if not pathlib.Path(p.rstrip(".")).exists():
                bad.append(f"{f}: missing {p}")
print("\n".join(bad) or "OK")
```

Hasil harus `OK`. Jika ada temuan, periksa apakah itu positif palsu (mis. file yang memang dibuat saat runtime). Perbaiki yang nyata, dan catat sisanya di laporan.

**c. Script dan test:**

```bash
for f in $(find .agents/skills -name '*.py'); do python3 -m py_compile "$f" || echo "FAIL $f"; done
for f in $(find .agents/skills -name '*.cjs'); do node --check "$f" || echo "FAIL $f"; done
python3 .agents/skills/banner-design/scripts/export-png.py --help
python3 .agents/skills/ui-ux-pro-max/scripts/search.py "beauty spa wellness service" --design-system -p "Serenity Spa"
python3 .agents/skills/design/scripts/logo/search.py "tech startup modern" --design-brief -p "BrandName"
```

Lalu jalankan ulang empat perintah test dari bagian 3. Hasil harus sama dengan baseline (130 / 1 / 2 / 58), dan setelah fase 7 `ui-ux-pro-max` tidak lagi perlu opsi `--ignore`.

**d. Ukuran:** tidak ada `SKILL.md` yang melebihi 500 baris. Catat jumlah baris tiap skill di laporan.

## 10. Laporan akhir

Tulis `.agents/FIX_REPORT.md` berisi:

1. Daftar file yang diubah, per fase.
2. Daftar file yang dihapus.
3. File baru yang dibuat (`export-png.py`, `slide-system.md`, `native-app-checklist.md`, dan lainnya).
4. Keputusan yang kamu ambil sendiri di luar brief ini, beserta alasannya.
5. Hasil verifikasi fase 9 (tempel output ringkasnya) dan perbandingan dengan baseline.
6. Masalah yang ditemukan tetapi sengaja tidak diperbaiki.

Setelah itu, beri tahu user secara singkat cara memeriksa hasilnya: buka menu Customizations di panel agent untuk memastikan kedelapan skill terbaca, lalu coba satu permintaan per skill.
