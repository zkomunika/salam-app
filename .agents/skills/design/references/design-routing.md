# Design Routing Guide

When to use each design sub-skill.

## Skill Overview

| Skill | Purpose | Owner |
|-------|---------|-------|
| `slides` | Strategic HTML presentations with Chart.js | `.agents/skills/slides/` |
| `banner-design` | Banners and covers for social, ads, web, print | `.agents/skills/banner-design/` |
| `design` | Logo, CIP mockups, SVG icons, social photos | `.agents/skills/design/` |
| `design-system` | Token architecture and component specs | `.agents/skills/design-system/` |
| `ui-styling` | shadcn/ui and Tailwind CSS implementation | `.agents/skills/ui-styling/` |
| `brand` | Brand voice, visual identity, assets | `.agents/skills/brand/` |

## Routing by Task Type

### Brand Identity Tasks
**→ brand**

- Define brand colors and typography
- Create logo usage guidelines
- Establish brand voice and tone
- Organize and validate assets
- Create messaging frameworks
- Audit brand consistency

### Token System Tasks
**→ design-system**

- Create design tokens JSON
- Generate CSS variables
- Define component specifications
- Map tokens to Tailwind config
- Validate token usage in code
- Document state and variants

### Implementation Tasks
**→ ui-styling**

- Add shadcn/ui components
- Style with Tailwind classes
- Implement dark mode
- Create responsive layouts
- Build accessible components

### Logo Design Tasks
**→ design (`references/logo-design.md`)**

- Create logos with AI (Gemini Nano Banana)
- Search logo styles, color palettes, industry guidelines
- Generate design briefs
- Explore 55+ styles (minimalist, vintage, luxury, geometric, etc.)

### Corporate Identity Program Tasks
**→ design (`references/cip-design.md`)**

- Generate CIP deliverables (business cards, letterheads, signage, vehicles, apparel)
- Create CIP briefs with industry/style analysis
- Generate mockups with/without logo (Gemini Flash/Pro)
- Render HTML presentations from CIP mockups

### Presentation Tasks
**→ slides**

- Create strategic HTML presentations
- Data visualization with Chart.js
- Apply copywriting formulas to slide content
- Use layout patterns and design tokens

### Banner and Cover Tasks
**→ banner-design**

- Design banners for social media (Facebook, Twitter, LinkedIn, YouTube, Instagram)
- Create ad banners (Google Ads, Meta Ads)
- Website hero banners and headers
- Print banners and covers
- 22 art direction styles (minimalist, bold typography, gradient, glassmorphism, etc.)

### Icon & Social Photos Tasks
**→ design (`references/icon-design.md`, `references/social-photos-design.md`)**

- Generate SVG icons with AI (Gemini 3.1 Pro Preview)
- Multi-size SVG icon export (16px, 24px, 32px, 48px)
- Multi-platform social media images exported via Playwright

## Routing by Question Type

| Question | Skill |
|----------|-------|
| "What color should this be?" | `brand` |
| "How do I create a token for X?" | `design-system` |
| "How do I build a button component?" | `ui-styling` |
| "Is this on-brand?" | `brand` |
| "Should I use a CSS variable here?" | `design-system` |
| "How do I add dark mode?" | `ui-styling` |
| "Create a logo for my brand" | `design` |
| "Generate business card mockups" | `design` |
| "Create a pitch deck" | `slides` |
| "Design brand identity package" | `design` |
| "What logo style fits my industry?" | `design` |
| "Design a Facebook cover" | `banner-design` |
| "Create ad banners for Google" | `banner-design` |
| "Make a website hero banner" | `banner-design` |
| "Generate a settings icon" | `design` |
| "Create SVG icons for my app" | `design` |
| "Create social media post images" | `design` |

## Multi-Skill Workflows

### New Project Setup

```
1. brand → Define identity
   - Colors, typography, voice

2. design-system → Create tokens
   - Primitive, semantic, component

3. ui-styling → Implement
   - Configure Tailwind, add components
```

### Design System Migration

```
1. brand → Audit existing
   - Extract brand colors, fonts

2. design-system → Formalize tokens
   - Create three-layer architecture

3. ui-styling → Update code
   - Replace hardcoded values
```

### Component Creation

```
1. design-system → Reference specs
   - Button states, sizes, variants

2. ui-styling → Implement
   - Build with shadcn/ui + Tailwind
```

## Skill Dependencies

```
brand
    ↓ (colors, typography)
design-system
    ↓ (tokens, specs)
ui-styling
    ↓ (components)
Application Code
```

## Quick Commands

**Brand:**
```bash
node .agents/skills/brand/scripts/inject-brand-context.cjs
node .agents/skills/brand/scripts/validate-asset.cjs <path>
```

**Tokens:**
```bash
node .agents/skills/design-system/scripts/generate-tokens.cjs -c tokens.json
node .agents/skills/design-system/scripts/validate-tokens.cjs -d src/
```

**Components:**
```bash
npx shadcn@latest add button card input
```

## When to Use Multiple Skills

Use **`design` + `slides` + `brand` + `design-system`** when:
- Building a complete brand package from scratch (logo → CIP → presentation)

Use **`brand` + `design-system` + `ui-styling`** when:
- Setting up and implementing a design system in React/Tailwind

Use **`banner-design` + `brand`** when:
- Producing branded banners and covers across social platforms

Use **`design` + `design-system`** when:
- Creating custom SVG icon sets or social photos aligned with design tokens

Use **`brand` + `design-system`** when:
- Defining visual language without component code implementation

Use **`design-system` + `ui-styling`** when:
- Implementing an existing brand in code or building a component library
