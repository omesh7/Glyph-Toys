# Third-Party Notices

## shared/AodLifecycleGuard.kt, shared/GlyphDeviceUtils.kt

Adapted from [`bluehomewu/GlyphMarquee`](https://github.com/bluehomewu/GlyphMarquee) — the AOD event handling in `MarqueeService` (event dedup, screen-on detection, flicker-delay on stop/start) and the multi-device registration logic in `onServiceConnected`. Ported out of a single service into standalone, reusable helpers for this repo's toys.

Credit: [bluehomewu](https://github.com/bluehomewu).
