# Social Photos Design Guide

Design social media images via HTML/CSS rendering and Playwright PNG export. Coordinates `ui-ux-pro-max`, `brand`, and `design-system` skills alongside `.agents/skills/banner-design/scripts/export-png.py`.

## Platform Sizes

| Platform | Type | Size (px) | Aspect |
|----------|------|-----------|--------|
| Instagram | Post | 1080 x 1080 | 1:1 |
| Instagram | Story/Reel | 1080 x 1920 | 9:16 |
| Instagram | Carousel | 1080 x 1350 | 4:5 |
| Facebook | Post | 1200 x 630 | ~1.9:1 |
| Facebook | Story | 1080 x 1920 | 9:16 |
| Twitter/X | Post | 1200 x 675 | 16:9 |
| Twitter/X | Card | 800 x 418 | ~1.91:1 |
| LinkedIn | Post | 1200 x 627 | ~1.91:1 |
| LinkedIn | Article | 1200 x 644 | ~1.86:1 |
| Pinterest | Pin | 1000 x 1500 | 2:3 |
| YouTube | Thumbnail | 1280 x 720 | 16:9 |
| TikTok | Cover | 1080 x 1920 | 9:16 |
| Threads | Post | 1080 x 1080 | 1:1 |

## Workflow

### Step 1: Analyze Requirements

Parse user input for:
- **Subject/topic** — what the social photo represents
- **Target platforms** — which sizes needed (default: Instagram Post 1:1 + Story 9:16)
- **Visual style** — minimalist, bold, gradient, photo-based, etc.
- **Brand context** — read from `docs/brand-guidelines.md` if exists
- **Content elements** — headline, subtext, CTA, images, icons
- **Quantity** — how many variations (default: 3)

### Step 2: Generate Ideas

Create 3-5 concept ideas that:
- Match the input prompt/requirements
- Consider platform-specific best practices
- Vary in composition, color, typography approach
- Align with brand guidelines if available

Present ideas directly in chat and wait for the user's approval before continuing with the design.

### Step 3: Design HTML Files

Use these skills in sequence:

1. **`brand` skill** — Extract brand colors, fonts, voice from user's project
2. **`design-system` skill** — Get design tokens (spacing, typography scale, color palette)
3. **Layout and styling** — Use the `ui-ux-pro-max` skill when choosing a style, color palette, or font pairing; use the `frontend-design` skill (if available) when executing the layout.

For each approved idea + each target size, create an HTML file:

```
output/social-photos/
├── idea-1-instagram-post-1080x1080.html
├── idea-1-instagram-story-1080x1920.html
├── idea-2-instagram-post-1080x1080.html
├── idea-2-instagram-story-1080x1920.html
└── ...
```

#### HTML Design Rules

- **Viewport** — Set exact pixel dimensions matching target size
- **Self-contained** — Inline all CSS, embed fonts via Google Fonts CDN
- **No scrolling** — Everything fits in one viewport
- **High contrast** — Text readable at thumbnail size
- **Brand-aligned** — Use extracted brand colors/fonts
- **Safe zones** — Critical content within central 80% area
- **Typography** — Min 24px for headlines, min 16px for body at 1080px width
- **Visual hierarchy** — One focal point, clear reading flow

#### HTML Template Structure

```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width={WIDTH}, initial-scale=1.0">
  <link href="https://fonts.googleapis.com/css2?family={FONT}&display=swap" rel="stylesheet">
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    html, body {
      width: {WIDTH}px;
      height: {HEIGHT}px;
      overflow: hidden;
      font-family: '{FONT}', sans-serif;
    }
    .canvas {
      width: {WIDTH}px;
      height: {HEIGHT}px;
      position: relative;
      /* Background: gradient, solid, or image */
    }
    /* Design tokens from brand/design-system */
  </style>
</head>
<body>
  <div class="canvas">
    <!-- Content layers -->
  </div>
</body>
</html>
```

### Step 4: PNG Export

Use the bundled Playwright exporter (`.agents/skills/banner-design/scripts/export-png.py`) to render each HTML file at exact pixel dimensions with retina scaling (`--scale 2` by default):

```bash
python .agents/skills/banner-design/scripts/export-png.py \
  --html "output/social-photos/idea-1-instagram-post-1080x1080.html" \
  --width 1080 --height 1080 \
  --output "output/social-photos/exports/idea-1-instagram-post-1080x1080.png"
```

Keep web fonts clean and wait conditions automatic (`networkidle` + `document.fonts.ready` are handled inside `export-png.py`). If an environment uses custom Chrome CLI instead, pass `--window-size="${WIDTH},${HEIGHT}"` and `--hide-scrollbars`.

### Step 5: Verify & Fix Designs

Open each exported PNG directly (or view it with your file/image inspection tool) and verify visual quality:

1. Check font rendering, brand color accuracy, and thumbnail legibility
2. Confirm there is no overflow, text clipping, or safe-zone infringement (keep key elements inside the central 80%)
3. Confirm text contrast meets WCAG AA (4.5:1)
4. If issues are found, fix the HTML/CSS source, re-run `export-png.py`, and inspect the new PNG again

### Step 6: Present Results

Present the finalized PNG paths, target platforms, pixel dimensions, and brief design rationale directly to the user in chat. Clean up or organize intermediate HTML files alongside the exported assets in `output/social-photos/`.

## Design Best Practices

### Platform-Specific Tips

- **Instagram** — Visual-first, minimal text (<20%), strong colors, lifestyle feel
- **Facebook** — Informative, can have more text, eye-catching in feed
- **Twitter/X** — Bold headlines, contrast for dark/light mode, clear message
- **LinkedIn** — Professional, clean, data-driven visuals, thought leadership
- **Pinterest** — Vertical format, text overlay on images, how-to style
- **YouTube** — Face close-ups perform best, bright colors, readable at small size
- **TikTok** — Trendy, energetic, bold typography, youth-oriented

### Art Direction Styles (Reuse from Banner)

| Style | Best For | Key Elements |
|-------|----------|--------------|
| Minimalist | SaaS, tech, luxury | Whitespace, single accent color, clean type |
| Bold Typography | Announcements, quotes | Large type, high contrast, minimal imagery |
| Gradient Mesh | Modern brands, apps | Fluid color transitions, floating elements |
| Photo-Based | Lifestyle, e-commerce | Hero image, subtle overlay, text on image |
| Geometric | Tech, fintech | Shapes, patterns, structured layouts |
| Glassmorphism | SaaS, modern apps | Frosted glass, blur effects, transparency |
| Flat Illustration | Education, health | Custom illustrations, friendly, approachable |
| Duotone | Creative, editorial | Two-color treatment on photos |
| Collage | Fashion, culture | Mixed media, overlapping elements |
| 3D/Isometric | Tech, product | Depth, shadows, modern perspective |

### Color & Contrast

- Ensure WCAG AA contrast ratio (4.5:1 min) for all text
- Test designs at 50% size to verify readability
- Consider platform dark/light mode compatibility
- Use brand primary color as dominant, secondary as accent

### Typography Hierarchy

| Element | Min Size (at 1080px) | Weight |
|---------|---------------------|--------|
| Headline | 48px | Bold/Black |
| Subheadline | 32px | Semibold |
| Body | 24px | Regular |
| Caption | 18px | Regular/Light |
| CTA | 28px | Bold |

## Security & Scope

This sub-skill handles social media image design only. Does NOT handle:
- Video content creation
- Animation/motion graphics
- Print production files (CMYK, bleed)
- Direct social media posting/scheduling
- AI image generation (only compose user-supplied images or CSS/SVG visuals)
