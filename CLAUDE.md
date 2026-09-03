# Working in this repo

A collection of Glyph Toys for Nothing phones, shipped as one app.

**Read [docs/SDK.md](docs/SDK.md) before touching anything under `glyph/` or a manifest
`<service>`.** It is the verified SDK reference. [docs/RESEARCH.md](docs/RESEARCH.md) holds the
market, licensing and competitor findings. [PLAN.md](PLAN.md) is the roadmap.

All three exist so nobody re-runs research that has already been done. If you find something new,
add it there rather than leaving it in a commit message.

## Build

```sh
./gradlew testDebugUnitTest    # drawing core — pure JVM, no emulator
./gradlew assembleDebug        # APK at app/build/outputs/apk/debug/
```

Needs an Android SDK with **platform `android-37.0`** (Android ships minor platform versions now;
`android-37` does not exist) and `build-tools;37.0.0`. If `local.properties` is missing, install
the SDK and write `sdk.dir=<path>` — it is gitignored because it is machine-specific.

## Non-negotiables

These were each expensive to establish. Do not "fix" them without reading the docs first.

1. **Manifest action is `com.nothing.glyph.TOY`.** `com.nothing.glyph.toy.SERVICE` does not exist —
   it appears in no official doc, sample, or shipped app. Same for `.preview`, `.category`,
   `.support_devices`. The real meta-data keys are `.name`, `.image`, `.summary`, `.longpress`,
   `.aod_support`.
2. **A toy is a plain bound service.** `onBind` returns `Messenger(handler).binder`. Never call
   `startForeground` — the OS holds the binding. Both original apps in this repo's history
   returned `null` from `onBind`, which is why neither ever received a toy event.
3. **AGP 9 applies Kotlin itself.** Applying `org.jetbrains.kotlin.android` is a hard failure. The
   Compose compiler plugin, if reintroduced, must match the KGP version AGP bundles — read AGP's
   POM, do not assume.
4. **`compileSdk` is 37, `targetSdk` is 36.** 37 is forced by the androidx artifacts' AAR
   metadata, not a preference. 36 is what Play requires. They are independent axes.
5. **Package is `dev.omesh.glyphtoys`.** Never `com.nothing.*`.
6. **The drawing core imports nothing from Android.** `glyph/Matrix.kt` and `glyph/Canvas.kt` are
   pure Kotlin so they stay unit-testable without an emulator. Keep it that way.
7. **`radius = size / 2`** is the LED mask for both panels and is covered by a test. It reproduces
   Nothing's published counts exactly (489 lit of 625; 137 of 169). Don't hand-tune it.

## Device reality

Development phone is a **Phone (4a) Pro**: 13×13, 137 lit LEDs, **no Glyph button**, AOD only.
It receives `EVENT_AOD` about once a minute and nothing else — that is a heartbeat that *starts*
our render loop, never a frame clock.

**The 25×25 Phone (3) path has never run on hardware.** The geometry is verified by test; the
behaviour is not. Say so rather than implying it works.

## Conventions

Style follows the "lazy senior dev" rule the owner asked for: reuse before writing, platform
feature before dependency, deletion over addition, fewest files. Compose and three androidx
dependencies were removed once it was clear a stub screen did not justify 28MB of dex — the app
currently has **zero runtime dependencies** beyond the vendored SDK aar. Add one back only when
something genuinely needs it.

Non-trivial logic leaves one runnable check behind. `MatrixCanvasTest` is the pattern: it asserts
the things that would silently ruin every toy if they broke.

Write a toy by subclassing `GlyphToyService` and implementing `draw`. See README.

## Verified vs not

**Verified** — device codes and LED counts; the manifest contract; the Messenger event contract;
that the toy appears and runs on a real 4a Pro; that the build is green locally and in CI.

**Not verified** — the exact usable brightness ceiling (see `GlyphToyService.maxBrightness`);
anything on Phone (3); whether the OS applies its own gamma; sustainable frame rate under
thermal load.

## Open items

- `maxBrightness` is 255. Nothing's own demo writes raw values up to 2046 and the SDK's bitmap
  path scales luminance by 16, so the ceiling may be ~4080. Needs an on-device A/B.
- Toy preview drawables should be rendered from `MatrixCanvas` so the picker shows a real dot
  matrix. The current `toy_charge.xml` is a smooth vector and looks out of place next to stock
  toys.
- Commercial use needs written permission from Nothing — see docs/RESEARCH.md.
