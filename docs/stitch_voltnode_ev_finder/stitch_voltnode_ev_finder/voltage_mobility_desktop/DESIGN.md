---
name: Voltage Mobility Desktop
colors:
  surface: '#131313'
  surface-dim: '#131313'
  surface-bright: '#393939'
  surface-container-lowest: '#0e0e0e'
  surface-container-low: '#1c1b1b'
  surface-container: '#201f1f'
  surface-container-high: '#2a2a2a'
  surface-container-highest: '#353534'
  on-surface: '#e5e2e1'
  on-surface-variant: '#bccbb9'
  inverse-surface: '#e5e2e1'
  inverse-on-surface: '#313030'
  outline: '#869585'
  outline-variant: '#3d4a3d'
  surface-tint: '#4ae176'
  primary: '#4be277'
  on-primary: '#003915'
  primary-container: '#22c55e'
  on-primary-container: '#004b1e'
  inverse-primary: '#006e2f'
  secondary: '#4edea3'
  on-secondary: '#003824'
  secondary-container: '#00a572'
  on-secondary-container: '#00311f'
  tertiary: '#afc7ff'
  on-tertiary: '#002e6a'
  tertiary-container: '#82abff'
  on-tertiary-container: '#003d88'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#6bff8f'
  primary-fixed-dim: '#4ae176'
  on-primary-fixed: '#002109'
  on-primary-fixed-variant: '#005321'
  secondary-fixed: '#6ffbbe'
  secondary-fixed-dim: '#4edea3'
  on-secondary-fixed: '#002113'
  on-secondary-fixed-variant: '#005236'
  tertiary-fixed: '#d8e2ff'
  tertiary-fixed-dim: '#adc6ff'
  on-tertiary-fixed: '#001a42'
  on-tertiary-fixed-variant: '#004395'
  background: '#131313'
  on-background: '#e5e2e1'
  surface-variant: '#353534'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 64px
    fontWeight: '800'
    lineHeight: 72px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
  title-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  container-max: 1440px
  gutter: 24px
  margin-desktop: 64px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 32px
---

## Brand & Style

The design system is a high-performance, precision-oriented framework tailored for the electric mobility sector. It targets a tech-savvy audience that values efficiency, sustainability, and cutting-edge engineering. 

The aesthetic is **Corporate / Modern** with a lean toward **Minimalism**, characterized by a dark-mode first interface that emphasizes the "Electric Green" primary color. The UI should feel fast and responsive, evoking the sensation of high-torque acceleration through clean lines, high-contrast layouts, and intentional whitespace. For desktop, the focus shifts from thumb-friendly targets to high-density information architecture and precise mouse interactions.

## Colors

The palette is anchored by "Voltage Green" (#22c55e), used strategically for primary actions, success states, and brand-critical indicators. The background utilizes a deep Dark Charcoal (#121212) to minimize eye strain during long-form desktop usage and to make the primary green pop with high luminosity.

- **Primary:** Voltage Green (#22c55e) for CTAs and active states.
- **Surface High:** #1e1e1e (for cards and elevated panels).
- **Surface Low:** #121212 (for main page backgrounds).
- **Border:** #2e2e2e (low-contrast dividers to maintain a seamless look).
- **Text Primary:** #f8fafc (near-white for maximum legibility).
- **Text Secondary:** #94a3b8 (muted slate for meta-data).

## Typography

This design system utilizes **Plus Jakarta Sans** across all levels to maintain a modern, geometric, and approachable feel. For the desktop environment, typography is scaled for optimal readability at a 24-inch viewing distance. 

Headlines use bold weights and tighter letter-spacing to create a strong visual "hook," while body text uses a generous 1.5x line height to improve scanning for data-heavy mobility dashboards. Uppercase labels are reserved for secondary navigation and small metadata tags to provide visual variety without compromising hierarchy.

## Layout & Spacing

The system employs a **Fixed Grid** model for the core content area, centered within the viewport with a maximum width of 1440px. A 12-column system is used for desktop, allowing for versatile dashboard layouts (e.g., 3-column stats, 2-column data views).

- **Grid:** 12 columns / 24px gutter / 64px side margins.
- **Rhythm:** An 8px linear scale governs all padding and margins to ensure mathematical harmony.
- **Desktop Adaptation:** On wider screens, sidebars are persistent (280px width) rather than hidden, providing immediate access to fleet management or navigation tools.

## Elevation & Depth

To maintain the sleek, technical aesthetic, depth is communicated through **Tonal Layers** rather than heavy shadows. 

1. **Level 0 (Background):** Pure Dark Charcoal (#121212).
2. **Level 1 (Cards/Panels):** Raised using a slightly lighter hex (#1e1e1e) and a subtle 1px border (#2e2e2e).
3. **Level 2 (Hover States/Modals):** Subtle ambient shadows are used—specifically a 12% opacity black with a 16px blur—to indicate interactivity.

This approach ensures the UI feels flat and high-performance, avoiding the "heavy" feel of traditional skeuomorphism.

## Shapes

The shape language is **Rounded**, utilizing a 0.5rem (8px) base radius. This softens the technical nature of the dark theme, making the application feel modern and premium. 

- **Small elements (Checkboxes/Tags):** 4px (Soft).
- **Standard elements (Buttons/Inputs/Cards):** 8px (Rounded).
- **Large elements (Modals/Banners):** 16px (Extra Rounded).
- **Action items (Search bars):** Pill-shaped (Full radius) to distinguish them as high-priority interactive zones.

## Components

### Buttons
- **Primary:** Solid #22c55e with #121212 text. On hover, the background brightens slightly.
- **Secondary:** Outlined with #2e2e2e border and white text. Hover state fills the background with #1e1e1e.
- **Interaction:** All buttons utilize a 150ms ease-in-out transition on hover. Cursor is always set to pointer.

### Cards
- Desktop cards feature a 24px internal padding. 
- In dashboard views, cards should have a fixed-height or flex-grow property to maintain grid alignment across rows.

### Input Fields
- Background is set to #1a1a1a with a 1px #2e2e2e border.
- On focus, the border changes to the Primary Voltage Green with a 2px outer "glow" (0px 0px 0px 2px rgba(34, 197, 94, 0.2)).

### Data Tables (Desktop Specific)
- Rows feature a subtle hover highlight (#1e1e1e).
- Header cells use `label-sm` typography with a bottom border for clear separation from data.

### Chips & Tags
- Used for vehicle status (e.g., "Charging", "Active").
- High-contrast background (Primary Green at 10% opacity) with Primary Green text for "Active" states.