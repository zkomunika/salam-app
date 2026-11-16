# Slide System Reference

Brand-compliant presentations using design tokens, Chart.js, and the contextual decision system hosted in `.agents/skills/design-system/`.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Source of Truth](#source-of-truth)
- [Slide Search (BM25)](#slide-search-bm25)
- [Decision System CSVs](#decision-system-csvs)
- [Contextual Decision Flow](#contextual-decision-flow)
- [Pattern Breaking (Duarte Sparkline)](#pattern-breaking-duarte-sparkline)
- [Slide Requirements](#slide-requirements)
- [Chart.js Integration](#chartjs-integration)
- [Token Compliance](#token-compliance)

## Prerequisites

Slides import `assets/design-tokens.css` from the project root so colors, typography, and spacing stay consistent across decks. If `assets/design-tokens.css` does not exist in your project yet, generate it from the bundled starter template:

1. Copy `.agents/skills/design-system/templates/design-tokens-starter.json` to `assets/design-tokens.json`.
2. Generate the CSS variables file from the project root:
   ```bash
   node .agents/skills/design-system/scripts/generate-tokens.cjs --config assets/design-tokens.json -o assets/design-tokens.css
   ```
   (Run `node .agents/skills/design-system/scripts/generate-tokens.cjs --help` first if you need Tailwind or custom output options.)

## Source of Truth

| File | Purpose |
|------|---------|
| `docs/brand-guidelines.md` | Brand identity, voice, colors (optional project file) |
| `assets/design-tokens.json` | Token definitions (primitive → semantic → component) |
| `assets/design-tokens.css` | CSS variables imported by generated slides |

## Slide Search (BM25)

```bash
# Basic search (auto-detect domain)
python .agents/skills/design-system/scripts/search-slides.py "investor pitch"

# Domain-specific search
python .agents/skills/design-system/scripts/search-slides.py "problem agitation" -d copy
python .agents/skills/design-system/scripts/search-slides.py "revenue growth" -d chart

# Contextual search (Premium System)
python .agents/skills/design-system/scripts/search-slides.py "problem slide" --context --position 2 --total 9
python .agents/skills/design-system/scripts/search-slides.py "cta" --context --position 9 --prev-emotion frustration
```

## Decision System CSVs

Located under `.agents/skills/design-system/data/`:

| File | Purpose |
|------|---------|
| `slide-strategies.csv` | 15 deck structures + emotion arcs + sparkline beats |
| `slide-layouts.csv` | 25 layouts + component variants + animations |
| `slide-layout-logic.csv` | Goal → Layout + `break_pattern` flag |
| `slide-typography.csv` | Content type → Typography scale |
| `slide-color-logic.csv` | Emotion → Color treatment |
| `slide-backgrounds.csv` | Slide type → Image category (Pexels/Unsplash) |
| `slide-copy.csv` | 25 copywriting formulas (PAS, AIDA, FAB) |
| `slide-charts.csv` | 25 chart types with Chart.js config |

## Contextual Decision Flow

```
1. Parse goal/context from user request
        ↓
2. Search .agents/skills/design-system/data/slide-strategies.csv → Get strategy + emotion beats
        ↓
3. For each slide:
   a. Query slide-layout-logic.csv → layout + break_pattern
   b. Query slide-typography.csv → type scale
   c. Query slide-color-logic.csv → color treatment
   d. Query slide-backgrounds.csv → image if needed
   e. Apply entrance/transition styles defined in your HTML template
        ↓
4. Generate HTML with design tokens (`assets/design-tokens.css`)
        ↓
5. Validate with:
   python .agents/skills/design-system/scripts/slide-token-validator.py <output.html>
```

## Pattern Breaking (Duarte Sparkline)

Premium decks alternate between emotions for engagement:
```
"What Is" (frustration) ↔ "What Could Be" (hope)
```

System calculates pattern breaks at 1/3 and 2/3 positions.

## Slide Requirements

Follow these rules so every generated deck renders consistently and stays themeable:

1. Import `assets/design-tokens.css` as the single source of truth for visual styling.
2. Use CSS variables (`var(--color-primary)`, `var(--slide-bg)`, etc.) instead of hardcoded hex or font strings.
3. Use Chart.js for data charts rather than hand-coded CSS bars so axes, legends, and tooltips remain accurate.
4. Include deck navigation (keyboard arrows, click controls, and a progress bar).
5. Center align primary slide stage content inside the 16:9 viewport.
6. Structure slide copy around persuasion and clear calls to action.

## Chart.js Integration

```html
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>

<canvas id="revenueChart"></canvas>
<script>
new Chart(document.getElementById('revenueChart'), {
    type: 'line',
    data: {
        labels: ['Sep', 'Oct', 'Nov', 'Dec'],
        datasets: [{
            data: [5, 12, 28, 45],
            borderColor: '#FF6B6B',  // Replace with computed token color if theming dynamically
            backgroundColor: 'rgba(255, 107, 107, 0.1)',
            fill: true,
            tension: 0.4
        }]
    }
});
</script>
```

## Token Compliance

```css
/* Correct - uses design tokens so light/dark themes switch cleanly */
background: var(--slide-bg);
color: var(--color-primary);
font-family: var(--typography-font-heading);

/* Avoid - hardcoded values bypass project tokens */
background: #0D0D0D;
color: #FF6B6B;
font-family: 'Space Grotesk';
```
