# Glyph Toys

A collection of **Glyph Toys** for the Nothing Phone (4a) Pro — small Android services that draw animations on the 13×13 circular Glyph Matrix (137 addressable LEDs).

Each toy is a standalone, independently buildable Android app. The repo is set up as a **template + instances** structure: adding a new toy mostly means copying an existing `*-app/` folder and writing **one Kotlin file**.

## Repo layout

```
Glyph-Toys/
├── DEVELOPER_GUIDE.md          # Hardware/SDK context, shared across all toys
├── dvd-bounce-app/             # Toy #1: DVD screensaver bounce
├── meditation-app/             # Toy #2: Anicca meditation timer
├── shared/                     # Copy-paste template snippets for new toys (device detection, AOD lifecycle helpers)
├── icon-templates/             # Shared XML assets for Glyph preview icons
└── .github/workflows/          # CI: builds + releases APKs per app
```

Every `*-app/` directory is a **complete, self-contained Android project**:

```
<name>-app/
├── app/
│   ├── libs/GlyphMatrixSDK.aar         # Nothing's Glyph SDK (vendored)
│   └── src/main/
│       ├── AndroidManifest.xml         # Service registration + Glyph metadata
│       ├── kotlin/.../<Name>Service.kt # ★ the toy — all LED/animation logic
│       ├── kotlin/.../MainActivity.kt  # Boilerplate launcher screen
│       └── res/                        # Strings, preview icon, theme
├── build.gradle.kts / settings.gradle.kts / gradlew
└── README.md                           # What this toy does + specs
```

## The "one file" pattern

Yes — once the project skeleton exists, everything the toy actually *does* lives in a single `Service.kt` (see `BounceService.kt`, `AniccaService.kt`). It:

- extends `Service` (registers with `GlyphMatrixManager` in `onCreate`)
- builds 13×13 / 169-length brightness frames and pushes them to the matrix
- reacts to triggers (sensor events, AOD, long-press "Action") to start/stop rendering

`MainActivity.kt`, the manifest boilerplate, and `res/` rarely change between toys — they're copy-paste scaffolding.

### Adding a new toy

1. Copy an existing `*-app/` folder (pick the one closer in behavior — sensor-driven like `dvd-bounce-app`, or timer/AOD-driven like `meditation-app`) to `<name>-app/`.
2. Rename the package (`com.nothing.glyph.toys.<name>`) in `AndroidManifest.xml`, `settings.gradle.kts`, `build.gradle.kts`, and the Kotlin package declarations.
3. Check [`shared/`](./shared/README.md) for reusable snippets (device detection, AOD lifecycle handling) before writing that logic from scratch again.
4. Rewrite `<Name>Service.kt` with your LED logic — this is the only file with real work in it.
5. Update `res/values/strings.xml` (toy name/summary) and `res/drawable/toy_preview.xml` (use `icon-templates/` as a starting point).
6. Add a `<name>-app/README.md` describing what it does (see the existing two for the expected format).
7. Push to `main` — CI auto-builds and releases the APK (see below).
8. If you wrote something the *next* toy will also need, pull it into `shared/` rather than leaving it buried in this app.

## CI / Releases

`.github/workflows/build-release.yml` watches pushes to `main` under `*-app/**`, builds only the app dirs that changed (or all, via manual `workflow_dispatch`), and publishes a GitHub Release with the built APKs attached.

## Toys

| Toy | Trigger | Docs |
|---|---|---|
| [DVD Bounce](./dvd-bounce-app/README.md) | Flip face-down | [logic](./dvd-bounce-app/BOUNCE_LOGIC.md) |
| [Anicca Meditation Timer](./meditation-app/README.md) | AOD / long-press | [logic](./meditation-app/ANICCA_MEDITATION.md) |

## Hardware & SDK reference

See [`DEVELOPER_GUIDE.md`](./DEVELOPER_GUIDE.md) for Glyph Matrix specs, SDK version, and the service/manifest contract every toy must follow — keep it updated when SDK/device details change, since every app here depends on it. It also tracks patterns and gotchas pulled from the wider Nothing Glyph Toy community (multi-device detection, AOD lifecycle quirks, newer SDK rendering APIs) so we don't relearn them the hard way.

## Credits

Designed and developed by [Omesh](https://github.com/omesh7).
