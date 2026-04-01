# Anicca Meditation Timer

A Glyph Toy for the Nothing Phone (4a) Pro that visualizes a meditation session on the 13×13 circular LED matrix. Named after the Pali word "Anicca" (impermanence).

## How it works

Three-phase visualization over a configurable duration (default 30 minutes):

1. **Settling (0–42%)** — Scattered dots spiral inward using golden angle distribution, gradually fading as a center point grows brighter
2. **Deepening (42–76%)** — A single ripple radiates outward from center, fading over time. Center stays bright
3. **Still (76–100%)** — Gentle cosine pulse radiates from center. On completion, a "bloom" animation expands outward

## Controls

- `START` — Begin the meditation timer
- `STOP` — Pause and turn off LEDs
- `RESET` — Return to start
- `PREVIEW` — Jump to a specific progress point (0.0–1.0)

## Specs

- **Trigger:** AOD (always-on display)
- **Frame rate:** 100ms intervals (10 FPS)
- **Duration:** Configurable, default 30 minutes
- **Grid:** 13×13 circular matrix (~138 active LEDs)
