# shared/

Copy-paste template snippets for new toys, not a compiled module. Each `*-app/` is an independent Gradle project (own `settings.gradle.kts`, own CI build/APK) — see the root [README](../README.md) for why. These files exist so we don't rediscover the same SDK gotchas per app.

## How to use

1. Copy the file(s) you need into `<name>-app/app/src/main/kotlin/com/nothing/glyph/toys/<name>/`.
2. Update the `package` declaration to match.
3. Wire it into your `Service.kt` per the usage notes in each file's header comment.

## What's here

| File | What it solves |
|---|---|
| [`GlyphDeviceUtils.kt`](./GlyphDeviceUtils.kt) | Detects the running device (`Common.getDeviceMatrixLength()`, `Glyph.DEVICE_*`) instead of hardcoding `DEVICE_25111p` — only needed once a toy targets more than Phone (4a) Pro. |
| [`AodLifecycleGuard.kt`](./AodLifecycleGuard.kt) | Dedupes repeated `GlyphToy.EVENT_AOD` events, handles the fact that `onUnbind` isn't reliably called on screen wake, and delays LED shutdown to avoid flicker during AOD transitions. |

These are original implementations written for this repo — see [`DEVELOPER_GUIDE.md`](../DEVELOPER_GUIDE.md#-community-reference-nothing-developer-programme--glyphmarquee) for the community project the underlying patterns were learned from (not copied — it ships with no LICENSE file, so we don't vendor its code, only the technique).

## Adding to this folder

When a new toy solves a problem that the *next* toy will also hit (a rendering helper, another lifecycle edge case, a shared preview-icon generator), pull it out here instead of leaving it buried in that app's `Service.kt`.
