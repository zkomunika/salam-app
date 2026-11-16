---
name: design-system
description: Token architecture, component specifications, and slide generation. Three-layer tokens (primitive→semantic→component), CSS variables, spacing/typography scales, component specs, strategic slide creation. Use for design tokens, systematic design, brand-compliant presentations.
license: MIT
metadata:
  author: claudekit
  version: "1.0.0"
---

# Design System

Token architecture, component specifications, systematic design, slide generation.

## When to Use

- Design token creation
- Component state definitions
- CSS variable systems
- Spacing/typography scales
- Design-to-code handoff
- Tailwind theme configuration
- **Slide/presentation generation**

## Token Architecture

Load: `references/token-architecture.md`

### Three-Layer Structure

```
Primitive (raw values)
       ↓
Semantic (purpose aliases)
       ↓
Component (component-specific)
```

**Example:**
```css
/* Primitive */
--color-blue-600: #2563EB;

/* Semantic */
--color-primary: var(--color-blue-600);

/* Component */
--button-bg: var(--color-primary);
```

## Quick Start

**Generate tokens:**
```bash
node scripts/generate-tokens.cjs --config tokens.json -o tokens.css
```

**Validate usage:**
```bash
node scripts/validate-tokens.cjs --dir src/
```

## References

| Topic | File |
|-------|------|
| Token Architecture | `references/token-architecture.md` |
| Primitive Tokens | `references/primitive-tokens.md` |
| Semantic Tokens | `references/semantic-tokens.md` |
| Component Tokens | `references/component-tokens.md` |
| Component Specs | `references/component-specs.md` |
| States & Variants | `references/states-and-variants.md` |
| Tailwind Integration | `references/tailwind-integration.md` |

## Component Spec Pattern

| Property | Default | Hover | Active | Disabled |
|----------|---------|-------|--------|----------|
| Background | primary | primary-dark | primary-darker | muted |
| Text | white | white | white | muted-fg |
| Border | none | none | none | muted-border |
| Shadow | sm | md | none | none |

## Scripts

| Script | Purpose |
|--------|---------|
| `scripts/generate-tokens.cjs` | Generate CSS from JSON token config |
| `scripts/validate-tokens.cjs` | Check for hardcoded values in code |
| `scripts/search-slides.py` | BM25 search + contextual recommendations (used by `slides`) |
| `scripts/slide-token-validator.py` | Validate slide HTML for token compliance (used by `slides`) |
| `scripts/fetch-background.py` | Fetch images from Pexels/Unsplash (used by `slides`) |

## Templates

| Template | Purpose |
|----------|---------|
| `templates/design-tokens-starter.json` | Starter JSON with three-layer structure |

## Integration

**With brand:** Extract primitives from brand colors/typography
**With ui-styling:** Component tokens → Tailwind config
**With slides:** Hosts the slide search scripts and CSV data used by the `slides` skill

**Skill Dependencies:** brand, ui-styling

## Slide System

To create HTML presentations or pitch decks, use the `slides` skill (`Create a 10-slide investor pitch for ClaudeKit Marketing`). The slide-generation scripts (`scripts/search-slides.py`, `scripts/slide-token-validator.py`, `scripts/generate-slide.py`) and decision CSVs in this folder are maintained here and invoked by the `slides` skill.

## Best Practices

1. Never use raw hex in components - reference semantic or primitive tokens instead so themes switch cleanly.
2. Semantic layer enables theme switching (light/dark).
3. Component tokens enable per-component customization.
4. Use HSL format for opacity control.
5. Document every token's purpose.
6. Ensure slides generated via `slides` import `assets/design-tokens.css` and use `var()` exclusively so branding stays centralized.
