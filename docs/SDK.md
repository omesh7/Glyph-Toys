# Glyph Matrix SDK — verified reference

Everything here was confirmed against Nothing's public repos and, where noted, against real
community apps. Sources at the bottom. Anything unconfirmed is labelled.

The SDK is vendored at `app/libs/glyph-matrix-sdk-2.0.aar`. Nothing publishes no Maven artifact,
so every project in the ecosystem vendors the aar.

## Devices

| Constant | Code | Device | Grid | Lit LEDs | Glyph Touch |
|---|---|---|---|---|---|
| `DEVICE_23112` | A024 | Phone (3), Jul 2025 | 25×25 = 625 | 489 | yes |
| `DEVICE_25111p` | A069P | Phone (4a) Pro, Mar 2026 | 13×13 = 169 | **137** | **none — AOD only** |
| `DEVICE_25111` | A069 | Phone (4a) | 6-zone strip | — | n/a, not toy-capable |
| `DEVICE_24111` | A059 | | 36 zones | | |
| `DEVICE_23111` / `23113` | A142 / A142P | Phone (2a) / (2a) Plus | 26 zones | | |
| `DEVICE_22111` | A065 | Phone (2) | 33 zones | | |
| `DEVICE_20111` | A063 | Phone (1) | 15 zones | | |

Only the two matrix devices run Glyph Toys. Detect at runtime with `Common.is23112()` /
`Common.is25111p()`, or `Common.getDeviceMatrixLength()`.

The lit-LED counts are reproduced exactly by a circular mask of `radius = size / 2` centred on
`(size-1)/2`. Covered by `MatrixCanvasTest`.

## Manifest

This is the only registration that exists. Confirmed three independent ways: Nothing's README,
Nothing's official sample app, and shipped third-party apps (GlyphMarquee, glyph-matrix-lab).

```xml
<uses-permission android:name="com.nothing.ketchum.permission.ENABLE" />
<meta-data android:name="NothingKey" android:value="test" />

<service android:name=".toys.MyToyService" android:exported="true"
    tools:ignore="ExportedService">
    <intent-filter><action android:name="com.nothing.glyph.TOY" /></intent-filter>
    <meta-data android:name="com.nothing.glyph.toy.name"    android:resource="@string/..." />
    <meta-data android:name="com.nothing.glyph.toy.image"   android:resource="@drawable/..." />
    <meta-data android:name="com.nothing.glyph.toy.summary" android:resource="@string/..." />
    <meta-data android:name="com.nothing.glyph.toy.aod_support" android:value="1" />
</service>
```

| Key | Required | Notes |
|---|---|---|
| `.name` | yes | `@string`. Shown in the toy picker |
| `.image` | yes | `@drawable`. Picker thumbnail |
| `.summary` | no | `@string` |
| `.introduction` | no | `android:value` = fully-qualified Activity class name |
| `.longpress` | no, default 0 | `1` enables `EVENT_CHANGE` delivery |
| `.aod_support` | **required = 1 on A069P** | AOD is that device's only mode |

`android:permission="com.nothing.ketchum.permission.BIND_GLYPH_TOY"` appears on GlyphMarquee's
service but not in Nothing's sample — optional, not required.

**Does not exist:** `com.nothing.glyph.toy.SERVICE`, `.preview`, `.category`, `.support_devices`.
These were invented in this repo's earlier history and appear in no doc, sample or shipped app.

## Service contract

A toy is a **bound** service. No `startForeground`, no `foregroundServiceType`. The OS binds it
when the user selects the toy and unbinds when they leave.

```kotlin
private val handler = object : Handler(Looper.getMainLooper()) {
    override fun handleMessage(msg: Message) {
        if (msg.what != GlyphToy.MSG_GLYPH_TOY) return
        when (msg.data?.getString(GlyphToy.MSG_GLYPH_TOY_DATA)) {   // key is "data"
            GlyphToy.EVENT_AOD         -> ...
            GlyphToy.EVENT_ACTION_DOWN -> ...
            GlyphToy.EVENT_ACTION_UP   -> ...
            GlyphToy.EVENT_CHANGE      -> ...
        }
    }
}
private val messenger = Messenger(handler)

override fun onBind(intent: Intent?): IBinder? {
    GlyphMatrixManager.getInstance(applicationContext)?.also { it.init(callback) }
    return messenger.binder          // returning null means you get no events, ever
}
override fun onUnbind(intent: Intent?): Boolean {
    manager?.run { turnOff(); unInit() }; return false
}
// callback.onServiceConnected -> manager.register(Glyph.DEVICE_25111p)
```

`STATUS_PREPARE` / `STATUS_START` / `STATUS_END` exist in the aar but appear in no public doc,
sample or community app. Do not branch on them.

### Events per device

- **Phone (3):** short press cycles toys at system level and is *not* delivered to you. Long press
  → `EVENT_CHANGE`, only with `.longpress=1`. Press/release → `EVENT_ACTION_DOWN` / `_UP`.
  One toy active at a time.
- **Phone (4a) Pro:** no touch events at all. Only `EVENT_AOD`, roughly once per minute. Users
  enable it under Settings → Glyph Interface → Flip to Glyph → Always-on Glyph Toy.

`EVENT_AOD` is far too coarse to animate from. Use it to start your own loop.

## Rendering

Two paths, both real:

**Object path** (documented, brightness 0–255):
```kotlin
GlyphMatrixFrame.Builder()
    .addTop(GlyphMatrixObject.Builder().setImageSource(bitmap).setBrightness(255).build())
    .build(context)                       // Context is required in SDK 2.0+
```
Max **3 layers** (top / mid / low).

**Raw path** — `setMatrixFrame(IntArray)` of exactly `size*size`, straight to
`IGlyphService.setMatrixColors` with no scaling. This repo uses it because it is one value per LED
with nothing in between, which is what an anti-aliased renderer needs.

**Brightness range is unsettled.** `GlyphMatrixObject.setBrightness` is documented 0–255. But
Nothing's own `GlyphButtonDemoService` fills a raw frame with `Random.nextInt(2047)`, and
glyph-water-sim clamps to 4096. `glyph-matrix-lab`'s notes insist 0–255 is correct and that
0–4095 is "a common misunderstanding". Both can be true — the object path scales by 16 internally.
**Calibrate on device.** See `GlyphToyService.maxBrightness`.

No OS-side gamma curve is documented either way.

### Useful SDK helpers
`GlyphMatrixUtils` has `convertToGlyphMatrix`, `toGrayscaleArray`, `dilation`, `erosion`,
`generateMatrixProgress` (progress arcs), `createBitmapWithText`, `drawableToBitmap`, and
`ARROW_3x2` / `ARROW_5x6`. `GlyphMatrixFrameWithMarquee` does scrolling text. The aar also bundles
the Ndot dot-font as ~60 `letter_*` string resources — usable through the SDK on the matrix, which
sidesteps the font licensing problem entirely (see RESEARCH.md).

## Frame rate

No documented limit. Nothing's `AnimationDemoService` paces at `delay(30)` ≈ 33fps.
glyph-water-sim's author found below ~15ms "slows down animation", i.e. ~66fps is a practical
ceiling. GlyphBeat renders at 20fps. `setGlyphMatrixTimeout` exists in the aar with zero public
mentions — treat as internal.

Render off the main thread and hop to main only for the SDK call. Most community toys render
entirely on the main Handler; not copying that is free jank avoidance.

## NothingKey

`android:value="test"` works in production today — Nothing's own 2026 sample and shipped
third-party apps use it. API-key enforcement was removed in Android 16. Keep the meta-data for
cross-version stability. A real key comes from `GDKsupport@nothing.tech`.

## Pitfalls other people hit

1. **Toy goes dark after switching toys in Settings.** OS bug, affects stock toys too. Workaround
   is toggling Glyph Interface off and on.
2. **An app cannot activate its own toy.** Selection is always manual. Deep-link instead:
   `ComponentName("com.nothing.thirdparty", "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity")`
3. **The system doesn't reliably rebind after screen wake** on newer builds, leaving the panel
   dark. Register an `ACTION_SCREEN_ON` receiver as a resync.
4. **`EVENT_AOD` re-entrancy** causes stuck-on LEDs and dead animation — the single most-patched
   bug class in community toys. Guard with explicit state.
5. **SDK 2.0 was a breaking change** (raw IntArray era → Builder era).
6. **`RECORD_AUDIO` not granted** → `Visualizer` constructs fine but never fires, so a naive
   implementation shows a frozen meter instead of an error. Needs a staleness watchdog.
7. **`Visualizer.setCaptureSize` throws** if called while enabled — disable, configure, enable.

## Sources

- [GlyphMatrix-Developer-Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit) — README, LICENSE.md, the aar
- [GlyphMatrix-Example-Project](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Example-Project) — official sample; `GlyphMatrixService.kt`, `GlyphButtonDemoService.kt`, `AnimationDemoService.kt`
- [Glyph-Developer-Kit](https://github.com/Nothing-Developer-Programme/Glyph-Developer-Kit) — older kit, same aar, documents NothingKey
- [bluehomewu/GlyphMarquee](https://github.com/bluehomewu/GlyphMarquee) — shipped toy, screen-wake and AOD-timeout workarounds
- [alex-1121/glyph-matrix-lab](https://github.com/alex-1121/glyph-matrix-lab) — 13×13 toy, has an `AGENTS.md` with SDK notes
- [pauwma/GlyphBeat](https://github.com/pauwma/GlyphBeat) — visualizer, binding refcount, Visualizer watchdogs
- [vlad-mod/glyph-water-sim](https://github.com/vlad-mod/glyph-water-sim) — raw IntArray path, frame timing
