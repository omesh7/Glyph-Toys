# Glyph Toys

A collection of Glyph Toys for Nothing phones, in one app.

## Hardware

| | Phone (3) | Phone (4a) Pro |
|---|---|---|
| Device code | A024 | A069P |
| Grid | 25×25, 489 lit LEDs | 13×13, 137 lit LEDs |
| Glyph button | yes | **none** |
| Events | touch, long press | `EVENT_AOD` only, ~1×/min |

The panel is addressed as a square grid but only the LEDs inside a circle exist. The mask is
exactly `radius = size / 2`, which reproduces both published LED counts.

The Phone (4a) Pro has no Glyph button, so toys on it cannot be interactive. `EVENT_AOD` is a
heartbeat, not a frame clock — it starts our own render loop, which then runs freely.

## Layout

```
app/src/main/kotlin/dev/omesh/glyphtoys/
  glyph/Matrix.kt           panel geometry and the circular mask   (pure Kotlin, tested)
  glyph/Canvas.kt           anti-aliased drawing + gamma encoding  (pure Kotlin, tested)
  glyph/GlyphToyService.kt  toy contract, event routing, render loop
  toys/                     one file per toy
  MainActivity.kt           companion UI
```

`glyph/Matrix.kt` and `glyph/Canvas.kt` deliberately import nothing from Android, so the drawing
core is unit-testable without an emulator or an Android SDK.

## Writing a toy

Subclass `GlyphToyService`, implement `draw`, register it in the manifest:

```kotlin
class MyToyService : GlyphToyService() {
    override fun draw(canvas: MatrixCanvas, elapsedMs: Long) {
        val c = (geometry.size - 1) / 2f
        canvas.ring(c, c, geometry.size / 2f - 1.5f, thickness = 1f)
    }
}
```

```xml
<service android:name=".toys.MyToyService" android:exported="true"
    tools:ignore="ExportedService">
    <intent-filter><action android:name="com.nothing.glyph.TOY" /></intent-filter>
    <meta-data android:name="com.nothing.glyph.toy.name"        android:resource="@string/toy_my_name" />
    <meta-data android:name="com.nothing.glyph.toy.image"       android:resource="@drawable/toy_my" />
    <meta-data android:name="com.nothing.glyph.toy.summary"     android:resource="@string/toy_my_summary" />
    <meta-data android:name="com.nothing.glyph.toy.aod_support" android:value="1" />
</service>
```

The action is `com.nothing.glyph.TOY`. `com.nothing.glyph.toy.SERVICE` appears in no official
doc, sample, or shipped app — don't use it.

A toy is a plain **bound** service. Do not call `startForeground`.

## Build

```sh
./gradlew testDebugUnitTest   # drawing core
./gradlew assembleDebug
```

## Installing a toy

An app cannot activate its own toy. After installing, enable it yourself in
**Settings → Glyph Interface → Glyph Toys**; the app has a button that deep-links there.

## SDK

`app/libs/glyph-matrix-sdk-2.0.aar` is vendored from
[Nothing-Developer-Programme/GlyphMatrix-Developer-Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit).
Nothing publishes no Maven artifact.

> **Note:** the SDK licence §2.2 prohibits commercial use without prior written permission from
> Nothing (`GDKsupport@nothing.tech`). This repository is non-commercial pending that permission.

## Docs

| | |
|---|---|
| [CLAUDE.md](CLAUDE.md) | how to work in this repo; the non-negotiables |
| [docs/SDK.md](docs/SDK.md) | verified SDK reference — API, manifest, events, pitfalls |
| [docs/RESEARCH.md](docs/RESEARCH.md) | market, competitors, licensing, monetization |
| [PLAN.md](PLAN.md) | roadmap, decisions, open questions |

They exist so the research isn't repeated. Add findings there rather than to a commit message.
