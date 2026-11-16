---
name: banner-design
description: "Designs banners and cover images for social media, display ads, website heroes, and print, offering several art directions per request as HTML/CSS exported to PNG at exact dimensions. Use when the user asks for a banner, cover, header, hero image, ad creative, or a graphic at a specific platform size."
license: MIT
metadata:
  author: claudekit
  version: "1.0.0"
---

# Banner Design - Multi-Format Creative Banner System

Design banners across social, ads, web, and print formats. Generates multiple art direction options per request with AI-powered visual elements. This skill handles banner design only. Does NOT handle video editing, full website design, or print production.

## When to Activate

- User requests banner, cover, or header design
- Social media cover/header creation
- Ad banner or display ad design
- Website hero section visual design
- Event/print banner design
- Creative asset generation for campaigns

## Prerequisites

**Python:** This skill uses Python scripts. On Windows, use `python` instead of `python3` (e.g., `python scripts/search.py` instead of `python3 scripts/search.py`).

## Workflow

### Step 1: Gather Requirements

Ask the user directly in chat and wait for the answer before continuing to collect:
1. **Purpose** — social cover, ad banner, website hero, print, or creative asset?
2. **Platform/size** — which platform or custom dimensions?
3. **Content** — headline, subtext, CTA, logo placement?
4. **Brand** — existing brand guidelines? (check `docs/brand-guidelines.md`)
5. **Style preference** — any art direction? (show style options if unsure)
6. **Quantity** — how many options to generate? (default: 3)

### Step 2: Art Direction Selection

1. Use the `ui-ux-pro-max` skill for design intelligence (palette, typography pairing, visual hierarchy).
2. Select 2-3 complementary art direction styles from `references/banner-sizes-and-styles.md`. If you have browser access and need visual inspiration, optional reference research can supplement the selection.

### Step 3: Design & Generate Options

For each art direction option:

1. **Create HTML/CSS banner** using `ui-ux-pro-max`, and `frontend-design` when available:
   - Use exact platform dimensions from `references/banner-sizes-and-styles.md`
   - Apply safe zone rules (critical content in central 70-80%)
   - Max 2 typefaces, single CTA, 4.5:1 contrast ratio
   - Inject brand context via `node .agents/skills/brand/scripts/inject-brand-context.cjs` if brand guidelines exist

2. **Create visual elements with CSS/SVG by default**:
   - Build geometric patterns, gradients, layered shapes, badges, and illustrations directly in inline CSS and SVG so banners render reliably without external dependencies.
   - Use raster or AI-generated imagery only when the user supplies assets or an image generation tool is available in the current environment.
   - Keep generated or supplied background imagery free of baked-in text so headlines, CTAs, and logos remain crisp HTML/CSS layers.

3. **Compose final banner** — overlay text, CTA, and logo cleanly on top of the visual layer in HTML/CSS.

### Step 4: Export Banners to PNG

After designing the HTML banners, export each one to PNG from the project root using the bundled Playwright exporter:

```bash
python .agents/skills/banner-design/scripts/export-png.py \
  --html "assets/banners/{campaign}/{variant}-{size}.html" \
  --width 1500 --height 500 \
  --output "assets/banners/{campaign}/{variant}-{size}.png"
```

Run `python .agents/skills/banner-design/scripts/export-png.py --help` to see all options (`--scale`, default `2`, handles retina-resolution output).

**Output path convention:**
```
assets/banners/{campaign}/
├── minimalist-1500x500.png
├── gradient-1500x500.png
├── bold-type-1500x500.png
├── minimalist-1080x1080.png    # if multi-size requested
└── ...
```

- Use kebab-case for filenames: `{style}-{width}x{height}.{ext}`
- Add a date prefix for time-sensitive campaigns: `{YYMMDD}-{style}-{size}.png`
- Group all campaign variants inside one folder

### Step 5: Present Options & Iterate

Open and visually inspect each exported PNG to verify contrast, safe zones, and typography clipping. Present all exported images side-by-side with:
- Art direction style name
- Exported PNG path and exact dimensions
- Key design rationale

Iterate based on user feedback until approved.

## Banner Size Quick Reference

| Platform | Type | Size (px) | Aspect Ratio |
|----------|------|-----------|--------------|
| Facebook | Cover | 820 × 312 | ~2.6:1 |
| Twitter/X | Header | 1500 × 500 | 3:1 |
| LinkedIn | Personal | 1584 × 396 | 4:1 |
| YouTube | Channel art | 2560 × 1440 | 16:9 |
| Instagram | Story | 1080 × 1920 | 9:16 |
| Instagram | Post | 1080 × 1080 | 1:1 |
| Google Ads | Med Rectangle | 300 × 250 | 6:5 |
| Google Ads | Leaderboard | 728 × 90 | 8:1 |
| Website | Hero | 1920 × 600-1080 | ~3:1 |

Full reference: `references/banner-sizes-and-styles.md`

## Art Direction Styles (Top 10)

| Style | Best For | Key Elements |
|-------|----------|--------------|
| Minimalist | SaaS, tech | White space, 1-2 colors, clean type |
| Bold Typography | Announcements | Oversized type as hero element |
| Gradient | Modern brands | Mesh gradients, chromatic blends |
| Photo-Based | Lifestyle, e-com | Full-bleed photo + text overlay |
| Geometric | Tech, fintech | Shapes, grids, abstract patterns |
| Retro/Vintage | F&B, craft | Distressed textures, muted colors |
| Glassmorphism | SaaS, apps | Frosted glass, blur, glow borders |
| Neon/Cyberpunk | Gaming, events | Dark bg, glowing neon accents |
| Editorial | Media, luxury | Grid layouts, pull quotes |
| 3D/Sculptural | Product, tech | Rendered objects, depth, shadows |

Full 22 styles: `references/banner-sizes-and-styles.md`

## Design Rules

- **Safe zones**: critical content in central 70-80% of canvas
- **CTA**: one per banner, bottom-right, min 44px height, action verb
- **Typography**: max 2 fonts, min 16px body, ≥32px headline
- **Text ratio**: under 20% for ads (Meta penalizes heavy text)
- **Print**: 300 DPI, CMYK, 3-5mm bleed
- **Brand**: inject via `node .agents/skills/brand/scripts/inject-brand-context.cjs` so brand palette and fonts stay consistent
