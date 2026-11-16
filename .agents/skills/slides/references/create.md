# Create Strategic Presentation Slides

Workflow for building persuasive, token-compliant HTML presentations using the slide decision data in `.agents/skills/design-system/`.

## Workflow

### Step 1: Understand the Brief and Prepare Tokens

1. Extract goal, target audience, key message, and approximate slide count from the user's request (default: 8–10 slides if unspecified).
2. Check if `assets/design-tokens.css` exists at the project root. If not, follow the Prerequisites section in `references/slide-system.md` to generate it from `.agents/skills/design-system/templates/design-tokens-starter.json`.

### Step 2: Select Deck Strategy

Query the BM25 strategy database from the project root to pick a narrative structure and emotion arc:

```bash
python .agents/skills/design-system/scripts/search-slides.py "<deck goal or type>" -d strategy
```

Optionally inspect `references/slide-strategies.md` for full strategy tables (investor pitch, sales deck, product launch, quarterly review).

### Step 3: Plan Each Slide (Layout, Typography, Color, Copy)

For each slide in the deck, query contextual recommendations from `.agents/skills/design-system/data/`:

```bash
python .agents/skills/design-system/scripts/search-slides.py "<slide purpose>" --context --position <n> --total <total_slides> [--prev-emotion <emotion>]
```

- Consult `references/layout-patterns.md` for grid/composition structures.
- Consult `references/copywriting-formulas.md` (PAS, AIDA, FAB, BAB) for headlines and bullets.
- For charts, query chart configs with `-d chart` rather than building CSS-only bars.

### Step 4: Build HTML Presentation

1. Use `references/html-template.md` as the structural base (keyboard arrow navigation, click controls, progress indicator, 16:9 stage).
2. Import `assets/design-tokens.css` and reference semantic CSS variables (`var(--slide-bg)`, `var(--color-primary)`, `var(--typography-font-heading)`).
3. Embed Chart.js via CDN (`https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js`) for any data visualizations.
4. Save the finished HTML deck (for example `assets/designs/slides/<deck-slug>.html`).

### Step 5: Validate Token Compliance

Run the validator from the project root to confirm no hardcoded hex colors or un-tokenized styles slipped in:

```bash
python .agents/skills/design-system/scripts/slide-token-validator.py <path-to-slide.html>
```

Inspect any reported violations, replace hardcoded values with token `var(--...)` references, and re-run until validation passes.
