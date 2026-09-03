# Plan

Facts live elsewhere so they can't drift: [docs/SDK.md](docs/SDK.md) for the SDK and hardware,
[docs/RESEARCH.md](docs/RESEARCH.md) for market and licensing, [CLAUDE.md](CLAUDE.md) for how to
work in the repo. This file is the roadmap and the decisions behind it.

## Product

One app, one Play listing, many toys. A curated collection of well-made toys — not another
editor. Both serious competitors (Nothing Playground, Glyph Museum) are builders; nobody has
bundled a polished collection, and every companion UI in the field is criticised, including
Nothing's own.

Sharper wedge: **the Phone (4a) Pro is the underserved device.** It ships four stock toys against
Phone (3)'s nine plus a community, and the most-repeated request in the research is more toys for
non-Phone-3 devices. It is also the device we own and can test.

## Status

| | |
|---|---|
| Render core | done, 6 tests, pure JVM |
| Toy contract | done, verified running on a real 4a Pro |
| Build | green locally and in CI |
| Toys | 1 of ~6 (Charge) |
| Companion UI | stub — no gallery yet |
| Monetization | not started, blocked on licensing |

## Decisions made

**Greenfield, not migrated.** Neither original app implemented the toy contract — both returned
`null` from `onBind` — and one used manifest keys that don't exist. Nothing was salvageable.

**Drawing core is pure Kotlin.** No Android imports, so it is unit-testable without an emulator.
This is what let the geometry and gamma be verified before any hardware existed.

**Anti-aliased rasterisation over thresholding.** Every community renderer reviewed does
`(r+g+b)/3` plus a hard threshold, discarding antialiasing. We rasterise from a signed distance
field and encode through gamma. At 137 LEDs that is most of the apparent resolution.

**Compose removed, deliberately.** Three androidx dependencies were declared and never imported,
and Compose existed to draw three labels — 28MB of dex for a stub. The app now has zero runtime
dependencies beyond the SDK aar and the debug APK is 2.5MB instead of 30MB. Compose returns when
the gallery is real and earns it.

**Raw frame path over the object path.** One value per LED with no scaling in between, which is
what the AA renderer needs. Costs us the documented 0–255 guarantee — hence the open brightness
question.

## Next

1. **Toy previews rendered from `MatrixCanvas`.** Stock toys show real dot-matrix renders; ours is
   a smooth vector and looks foreign in the picker. Fix once, scales to every future toy.
2. **Now-playing + album art toy.** Album art at 137 LEDs is the one case where dithering clearly
   wins, and no reviewed project implements dithering at all. Strongest available edge, and
   GlyphBeat proved the demand. Needs `RECORD_AUDIO` handling with a staleness watchdog.
3. **Clock set**, 3–4 well-drawn faces. Stock gives one; pure craft.
4. **Weather ambient**, then **next-event countdown**.
5. **Gallery UI** — Compose returns here. Live miniature matrix previews per card, "Try live on
   your Glyph" on detail, favourites separate from enabled.
6. **Billing**, once licensing is answered. One-time "Unlock All Toys", $4.99–$9.99, 2–3 toys free.

## Open questions

- **Brightness ceiling.** `maxBrightness` is 255; the evidence suggests it may be ~4080. Needs an
  on-device A/B. Everything visual depends on this.
- **Commercial permission** from Nothing under LICENSE.md §2.2. Email sent? Not yet.
- **Phone (3) is untested.** Geometry is verified by test; behaviour on 25×25 hardware is not.
- **Sustainable frame rate** under thermal load on AOD. Unknown.
