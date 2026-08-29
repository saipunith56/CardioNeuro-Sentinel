---
name: Clinical Precision Interface
colors:
  surface: '#f8fafb'
  surface-dim: '#d8dadb'
  surface-bright: '#f8fafb'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f4f5'
  surface-container: '#eceeef'
  surface-container-high: '#e6e8e9'
  surface-container-highest: '#e1e3e4'
  on-surface: '#191c1d'
  on-surface-variant: '#41484e'
  inverse-surface: '#2e3132'
  inverse-on-surface: '#eff1f2'
  outline: '#71787f'
  outline-variant: '#c1c7cf'
  surface-tint: '#276489'
  primary: '#003550'
  on-primary: '#ffffff'
  primary-container: '#004d71'
  on-primary-container: '#86bde7'
  inverse-primary: '#96cdf7'
  secondary: '#006a6a'
  on-secondary: '#ffffff'
  secondary-container: '#8cf3f3'
  on-secondary-container: '#007070'
  tertiary: '#6b0008'
  on-tertiary: '#ffffff'
  tertiary-container: '#960010'
  on-tertiary-container: '#ff9d94'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#c9e6ff'
  primary-fixed-dim: '#96cdf7'
  on-primary-fixed: '#001e2f'
  on-primary-fixed-variant: '#004b6f'
  secondary-fixed: '#8cf3f3'
  secondary-fixed-dim: '#6fd7d6'
  on-secondary-fixed: '#002020'
  on-secondary-fixed-variant: '#004f4f'
  tertiary-fixed: '#ffdad6'
  tertiary-fixed-dim: '#ffb3ac'
  on-tertiary-fixed: '#410003'
  on-tertiary-fixed-variant: '#930010'
  background: '#f8fafb'
  on-background: '#191c1d'
  surface-variant: '#e1e3e4'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 57px
    fontWeight: '700'
    lineHeight: 64px
    letterSpacing: -0.25px
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  title-lg:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '500'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-lg:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.5px
  data-mono:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '700'
    lineHeight: 20px
    letterSpacing: 0px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  base: 8px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 40px
  container-margin: 24px
  gutter: 16px
---

## Brand & Style

The design system is engineered for high-stakes medical environments, specifically focusing on cardiovascular and neurological diagnostics. The brand personality is **authoritative, precise, and calm**, designed to instill confidence in clinicians during critical decision-making moments. 

The aesthetic blends **Modern Corporate** reliability with **Glassmorphism** accents to provide a sense of depth and focus without cluttering the diagnostic workspace. By utilizing semi-transparent "glass" layers for secondary panels and high-fidelity typography for primary data, the system distinguishes between ambient information and actionable clinical insights. The emotional response is one of controlled urgency—professional and systematic, yet technologically advanced.

## Colors

This color palette is rooted in medical semiotics. **Deep Medical Blue** serves as the primary anchor for navigation and structural elements, providing a grounded, trustworthy foundation. **Clinical Teal** is used for secondary actions and interactive states, representing modern healthcare technology.

A specialized **Risk Scale** is integrated into the core palette:
- **Low (Green):** Stable patient metrics.
- **Moderate (Yellow):** Requires monitoring.
- **High (Orange):** Immediate attention suggested.
- **Critical (Red):** Life-critical alerts/High-Alert status.

Backgrounds utilize a "Clinical White" (#FFFFFF) and "Soft Slate" (#F8FAFB) to maintain maximum contrast for text readability while reducing eye strain during long shifts.

## Typography

The typography system uses **Inter** for its exceptional legibility and neutral, systematic tone. It is optimized for rapid scanning of patient charts and diagnostic telemetry. 

- **Numerical Data:** For vital signs and lab values, use the `data-mono` role to ensure characters align vertically in tables, aiding in the comparison of historical trends.
- **Hierarchy:** Use `Headline-lg` for patient names and primary diagnoses. `Label-lg` is reserved for metadata and status badges, always set in uppercase to differentiate from narrative medical notes.
- **Line Height:** Generous line heights are maintained to ensure that dense medical documentation remains readable under high-stress conditions.

## Layout & Spacing

The layout follows a **Fixed-Fluid Hybrid** model. Diagnostic dashboards use a 12-column grid on desktop to organize complex data visualizations, while patient profiles use a centered 8-column layout for focused reading.

- **Grid:** 12 columns (Desktop), 8 columns (Tablet), 4 columns (Mobile).
- **Rhythm:** An 8px base unit governs all dimensions. Elements should be spaced in multiples of 8 (e.g., 16, 24, 40) to maintain a rigorous, systematic appearance.
- **Density:** High-density layouts are permitted for waveform data and telemetry views, but narrative sections must utilize the `xl` (40px) spacing between major sections to prevent cognitive overload.

## Elevation & Depth

This design system uses a combination of **Tonal Layers** and **Glassmorphism** to create a clear informational hierarchy:

1.  **Level 0 (Base):** Soft Slate (#F8FAFB) background.
2.  **Level 1 (Cards):** Pure White (#FFFFFF) with a very thin (1px) border in #E0E6E9. No shadow.
3.  **Level 2 (Overlays):** Translucent White (80% opacity) with a 16px backdrop-blur. This is used for "fly-out" diagnostic tools and contextual menus.
4.  **Level 3 (Modals):** Pure White with a deep, ultra-soft ambient shadow (0px 20px 40px rgba(0, 77, 113, 0.08)). 

Borders are preferred over heavy shadows to maintain a "clinical" and "clean" feel, using shadows only when an element requires immediate focus (e.g., a critical alert dialog).

## Shapes

The shape language is **Soft (0.25rem)**. This subtle rounding removes the harshness of sharp corners—making the UI feel more accessible and "human"—while remaining professional and structured. 

- **Primary Containers:** 0.25rem (4px) corner radius.
- **Patient Action Buttons:** 0.5rem (8px) for a more approachable touch target.
- **Alert Badges:** Full pill-shaped (rounded-xl) to distinguish them from data fields.

## Components

### Buttons
- **Primary:** Solid Deep Medical Blue, white text, 4px radius.
- **Secondary:** Outlined Clinical Teal with a 1px stroke.
- **High-Alert:** Solid High-Alert Red for "Stop" or "Critical Override" actions.

### Cards & Surfaces
Diagnostic cards utilize a subtle white background with a 1px Clinical Gray border. For real-time telemetry (ECG/EEG), the background may shift to a dark charcoal for higher contrast on waveforms.

### Input Fields
Strict, rectangular fields with a 4px radius. On focus, the border-weight increases to 2px in Clinical Teal. Error states use High-Alert Red for both the border and the helper text.

### Status Badges (Risk Levels)
Badges must include both a color fill and a text label to ensure accessibility.
- **Critical Status:** Pulse animation permitted on the icon only; the background remains solid High-Alert Red.

### Lists & Data Tables
Rows have a minimum height of 48px to accommodate touch interactions. Alternate rows use a subtle #F8FAFB tint to assist the eye in horizontal tracking of patient data.