# Glyph Toys — Build Plan

Research date 2026-09-03. Every claim below is sourced from Nothing's public repos or from
community source actually read. Unverified items are marked.

---

## 1. Hardware reality

| | Phone (3) — A024 | **Phone (4a) Pro — A069P** |
|---|---|---|
| SDK const | `Glyph.DEVICE_23112` | `Glyph.DEVICE_25111p` |
| Grid | 25×25, 489 lit | **13×13, 137 lit** |
| Glyph Touch | Yes | **None** |
| Events | `EVENT_ACTION_DOWN/UP`, `EVENT_CHANGE` | **`EVENT_AOD` only, ~1×/min** |
| Manifest | — | **`aod_support=1` required** |

We own a 4a Pro. Phone (3) is build-blind — anything 25×25 ships untested until we borrow a device.

**Consequence:** interaction is impossible on our hardware. `EVENT_AOD` is a *kick*, not a clock —
start our own render loop from it (proven by GlyphMarquee; glyph-water-sim reaches ~66fps).
So: **ambient/generative toys first, games later and only for Phone (3).**

## 2. Commercial licensing — OPEN BLOCKER

SDK `LICENSE.md` §2.2: *"Commercial use of the Software is strictly prohibited unless you obtain
prior written permission from Nothing."* §9 → `GDKsupport@nothing.tech`.

Action: email Nothing now. Until answered, build free-first; keep paid unlock behind a flag.

## 3. Market position

- Toy-capable install base: **low hundreds of thousands** worldwide. Small.
- Best paid proof point: **Glyphify**, $2–5 one-time, 4.5★, ~700 ratings.
- Competition is *builders*: Nothing Playground (first-party, 12k+ submissions), Glyph Museum
  (free, 15k MAU). Our thesis must be **curated original content + polish**, not another editor.
- Validated gap, repeatedly: **nobody bundles a collection, and every companion UI is ugly —
  including Nothing's own.** That is the whole opportunity.
- Validated daily-use case, mentioned unprompted everywhere: **battery % while charging face-down.**

## 4. Architecture — one module, fewest files (ponytail)

No premature multi-module split. No custom render engine — the SDK already composites 3 layers,
converts bitmaps, scrolls marquee text, draws progress arcs, and ships the Ndot dot-font.

```
app/
  libs/glyph-matrix-sdk-2.0.aar          vendored (no Maven distribution exists)
  src/main/kotlin/dev/omesh/glyphtoys/
    MainActivity.kt                      Compose gallery host
    glyph/
      GlyphToyService.kt                 abstract base: Messenger, events, lifecycle, render loop
      Matrix.kt                          runtime device detect + dims + circular mask
      Dither.kt                          Floyd–Steinberg / Bayer  ← our edge
      Render.kt                          Bitmap → GlyphMatrixObject helpers
    toys/  <OneFilePerToy>Service.kt
    ui/    Gallery.kt  Detail.kt  Theme.kt
```

Package: **`dev.omesh.glyphtoys`** — never `com.nothing.*` (squatting; live third-party toys all
use their own namespace).

### Service contract (from Nothing's official sample, verbatim shape)
```kotlin
abstract class GlyphToyService : Service() {
    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == GlyphToy.MSG_GLYPH_TOY) when (msg.data?.getString("data")) {
                GlyphToy.EVENT_AOD         -> onAod()
                GlyphToy.EVENT_ACTION_DOWN -> onTouchDown()
                GlyphToy.EVENT_ACTION_UP   -> onTouchUp()
                GlyphToy.EVENT_CHANGE      -> onLongPress()
            }
        }
    }
    private val messenger = Messenger(handler)
    final override fun onBind(intent: Intent?): IBinder? { /* init + register */ return messenger.binder }
    final override fun onUnbind(intent: Intent?): Boolean { turnOff(); unInit(); return false }
}
```
Bound service — **no `startForeground()`**. The OS holds the binding. (Only auxiliary services,
e.g. a music-detection watcher, need foreground.)

### Manifest (variant A — the only one that exists)
```xml
<uses-permission android:name="com.nothing.ketchum.permission.ENABLE" />
<meta-data android:name="NothingKey" android:value="test" />
<service android:name=".toys.FooService" android:exported="true" tools:ignore="ExportedService">
    <intent-filter><action android:name="com.nothing.glyph.TOY" /></intent-filter>
    <meta-data android:name="com.nothing.glyph.toy.name"        android:resource="@string/toy_foo" />
    <meta-data android:name="com.nothing.glyph.toy.image"       android:resource="@drawable/toy_foo" />
    <meta-data android:name="com.nothing.glyph.toy.summary"     android:resource="@string/toy_foo_sum" />
    <meta-data android:name="com.nothing.glyph.toy.aod_support" android:value="1" />
</service>
```
`com.nothing.glyph.toy.SERVICE` / `.preview` / `.category` / `.support_devices` are **invented** —
they appear in no official doc, no sample, and no shipped app. Do not use them.

### Rendering
Use the object path, brightness **0–255**:
```kotlin
GlyphMatrixFrame.Builder()
    .addTop(GlyphMatrixObject.Builder().setImageSource(bmp).setBrightness(255).build())
    .build(context)                       // Context required in SDK 2.0+
```
Max 3 layers (top/mid/low). Avoid the raw `IntArray` path — its range is ambiguous
(object path 0–255; raw is wider, ~0–4080) and undocumented.

**Render off the main thread; hop to main only for `setMatrixFrame`.** Community toys all render
on the main Handler — free jank win for us.

## 5. Our three technical edges

1. **Dithering.** None of the 6 codebases read (Android or JS) implement Floyd–Steinberg or Bayer.
   All do `(r+g+b)/3` + hard threshold, discarding antialiasing. ~60 lines, visibly better output.
2. **Off-main-thread render pipeline.** Nobody does it.
3. **AOD robustness.** The most-patched bug class in the field — see §6.

## 6. Known pitfalls (from community changelogs/issues)

1. Toy goes dark after switching toys in Settings — OS bug, affects stock toys too.
2. Apps **cannot** programmatically activate their own toy. Activation is always manual in
   Settings → Glyph Interface. Ship a deep link:
   `ComponentName("com.nothing.thirdparty", "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity")`
3. System doesn't reliably rebind after screen wake → register an `ACTION_SCREEN_ON` receiver.
4. `EVENT_AOD` re-entrancy causes stuck-on LEDs / dead animation → explicit state machine guards.
5. SDK 2.0 was a breaking change (raw IntArray era → Builder era).
6. `GlyphMatrixFrame.Builder.build()` needs a `Context` in 2.0+.
7. `NothingKey="test"` works today; API-key enforcement removed in Android 16. Keep the meta-data.

## 7. Toy roadmap (4a Pro / AOD-first)

Ship order, chosen for *validated* demand and no-input feasibility:
1. **Charge/battery ambient** — the one universally praised use case
2. **Clock set** — several genuinely well-drawn faces (craft as the differentiator)
3. **Weather ambient**
4. **Music visualizer** — audio-reactive, needs no touch. Guard `RECORD_AUDIO`: without it
   `Visualizer` constructs fine but never fires → staleness watchdog required.
5. **Generative/ambient art**

Deferred to Phone (3): games, anything using `EVENT_ACTION_*` / `EVENT_CHANGE`.

## 8. Stack (verified current, Sept 2026)

Kotlin 2.3.20 · AGP 9.3.0 · Gradle 9.7.1 · Compose BOM 2026.08.00 · material3 1.4.0
core-ktx 1.19.0 · lifecycle 2.11.0 · activity-compose 1.13.0 · datastore 1.2.1 · Room 2.8.4
compileSdk/targetSdk **36** (Play minimum since Aug 31 2026) · minSdk 34 · Hilt · Play Billing 9.1.0

M3 Expressive is still `1.5.0-alpha27` — do not build the UI on it.

**Fonts:** Ndot and NType 82 are Colophon Foundry, licensed to Nothing exclusively — we cannot ship
either in our UI. Use **Matrix Sans** (OFL, purpose-built 5×7 dot matrix). Rendering Ndot *on the
matrix* through the SDK is fine.

## 9. UI direction

Gallery grid → shared-element morph → detail. Each card shows a **live miniature matrix render**,
not a screenshot. Detail has **"Try live on your Glyph"** (fires real hardware — the conversion
moment). Favourites separate from enabled. Locked toys stay live-previewable.

Anti-slop non-negotiables: radius/elevation tied to hierarchy not one flat value; motion duration
scales with distance; red accent for exactly one semantic class; designed empty states; dot-grid
loading motif; distinct error copy per failure.

## 10. Monetization (pending §2)

One-time **"Unlock All Toys"**, $4.99–$9.99, free tier of 2–3 toys. Play's 2026 policy bars
subscriptions for one-time benefits. Do **not** use `<uses-feature required="true">` for Glyph —
it would delist us from every non-Nothing device; detect at runtime and show an unsupported state.
