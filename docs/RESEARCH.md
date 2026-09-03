# Market, licensing and competitor research

Gathered 2026-09-03. Sources inline. Distinguishes what was verified from what is inference —
keep that distinction if you extend this.

## Licensing — the open blocker on selling

`LICENSE.md` in the SDK repo, §2.2:

> **"Commercial use of the Software is strictly prohibited unless you obtain prior written
> permission from Nothing."**

§9 points at `GDKsupport@nothing.tech`. §2.1(b) forbids reverse engineering the aar; §2.1(e) says
it is licensed closed-source.

The file was renamed from `EULA.md` on **13 Aug 2025**, around the Phone (3) SDK launch — so this
restriction predates every paid third-party toy on the market.

Paid toys nonetheless ship openly (Glyphify, Glyph Museum's supporter tier). From outside it is
impossible to tell whether those developers got permission, are unenforced, or structure their
payment as a donation. **No enforcement action turned up anywhere.**

Practical position: not blocked, but un-cleared. Emailing costs nothing, Nothing actively courts
third-party toys (they run Nothing Playground as a first-party submission hub), and shipping free
first is the right sequence anyway since discovery is harder than pricing. This is not legal
advice.

## Market size — small, and worth knowing up front

- Nothing shipped ~2M devices total (all products) in 2025, +25–31% YoY
- Phone (3): est. 15k–35k units in India H2 2025; no global figure found
- Phone (4a) series: ~150k globally since Mar 2026. Phone (4b): ~20k since Jul 2026
- Jul 2026 reports: Nothing weighing exits from ~12 markets, ~40% headcount cut (specifics denied,
  restructuring confirmed)

**Realistic toy-capable base is low hundreds of thousands worldwide.** This is a craft-and-
reputation play, not a volume business. Price premium, expect modest units.

## Competitors

| App | Model | Traction |
|---|---|---|
| **Glyphify** (Fr4nKB) | **paid ~$2–5 one-time** | 4.53★, 640–700+ ratings. Best paid proof point |
| **Glyph Museum** (pauwma) | freemium + one-time "Supporter" | **15,000 MAU, 12,000 designs.** 2.0 shipped Aug 2026 |
| **Glyph Toybox** (Singularity) | free/freemium | coin flip, battery, Pomodoro, affirmations |
| **GlyphBeat** (pauwma) | free OSS | music visualizer; the "dancing duck" is beloved |
| **Glyph Catch** (equalparts) | free OSS | Pokémon catcher, rewards *not* looking at your phone. The viral one |
| **GlyphWorks** | free | community hack adding interactive toys to the 4a Pro |
| ~20 more | mostly free | Nothing Playground download counts run 19–243 |

Nothing runs **[Nothing Playground](https://playground.nothing.tech/toys)** — a first-party hub
where users browse and install toys, 12k+ submissions. Both major competitors are *builders*
(editors). A curated collection of original, well-made toys is the uncontested position.

## What users actually say

**Loved (mentioned unprompted, repeatedly):** battery percentage while charging face-down. This is
*the* validated use case, which is why Charge was toy #1.

**Ignored:** stock mini-games — Spin the Bottle, Magic 8 Ball, Rock Paper Scissors. Reviewers call
them "just kind of there", ~5 minutes of novelty.

**Recurring complaints, all sourced:**
- "It's just a gimmick" — three separate Android Authority pieces over different months
- Clunky activation: must press the rear button each time, "takes as long as picking up the phone"
- AOD battery fear — active threads on nothing.community
- The official companion app has **no tutorial**, no image/animation editor, poor alert controls
- Only Google Calendar and Uber built real integrations in a year+ of public SDK, and Calendar's
  is called limited
- **The single most-repeated request: give real Glyph Toys to non-Phone-3 devices**

That last one is the opening. A Phone (4a) Pro ships with **four** stock toys while Phone (3) gets
nine plus a community. Our dev device is the underserved one.

## Ranked opportunities (AOD-compatible, no touch)

1. **Now-playing + album art** — GlyphBeat proved demand. Album art at 137 LEDs is the one case
   where dithering clearly wins, and *no project reviewed implements dithering at all* (all do
   `(r+g+b)/3` plus a hard threshold, discarding antialiasing). Strongest technical edge available.
2. **Clock set** — stock gives one. Pure craft differentiator, no API risk.
3. **Weather ambient** — repeatedly requested, nobody has done it well.
4. **Next-event countdown** — directly answers the "Calendar integration is too limited" complaint.
5. **Focus / Pomodoro** — fits the "attention filter" framing Nothing markets.

Deprioritise: games and novelty toys (5-minute lifespan), anything needing a button.

Two structural gaps worth repeating: **nobody bundles a collection**, and **every companion UI is
ugly or incomplete — including Nothing's own.** Press on Glyph Museum said outright "Nothing, take
note!"

## Monetization

- Play Billing **9.1.0**; BL8+ mandatory since 31 Aug 2026 (no 7→9 direct jump)
- Play's 2026 policy update **prohibits subscriptions for one-time benefits** → one-time IAP
- Recommend: free tier of 2–3 toys to seed installs and reviews, then a single **"Unlock All
  Toys"** at **$4.99–$9.99**. Above icon-pack pricing ($0.99–3.49), below impulse resistance
- Avoid KWGT's engine-key-plus-packs structure; users resent the double purchase
- **Do not** use `<uses-feature required="true">` for Glyph — it delists the app from every
  non-Nothing device. Detect at runtime and show an unsupported state
- Disclose the hardware dependency in the listing (Play Minimum Functionality policy)
- Staged rollout 1–5% → hold 24h on Vitals → 5/20/50/100

## Fonts — legal constraint

**Ndot** and **NType 82** are Colophon Foundry, licensed to Nothing exclusively. Neither can ship
in a third-party app UI. Open substitute: **Matrix Sans** (OFL, purpose-built 5×7 dot matrix);
also Departure Mono, Space Mono, JetBrains Mono.

Rendering Ndot *on the matrix* via the SDK's bundled `letter_*` resources is fine — that is
Nothing's SDK on Nothing's hardware.

## If Compose returns

Verified current as of Sept 2026: Compose BOM 2026.08.00, material3 1.4.0 stable, M3 Expressive
still `1.5.0-alpha27` (don't build on it), lifecycle 2.11.0, activity-compose 1.13.0,
datastore 1.2.1, Room 2.8.4 (stay on 2.x; room3 is KMP-first).

Gallery UX worth copying: live on-device miniature previews per card rather than screenshots, a
"Try live on your Glyph" action on the detail screen (the conversion moment), favourites kept
separate from enabled, and locked toys still previewable.

## Key sources

Android Authority ([toys overview](https://www.androidauthority.com/nothing-phone-3-glyph-matrix-3572917/),
[gimmick](https://www.androidauthority.com/nothing-glyph-matrix-gimmick-3602057/),
[too complicated](https://www.androidauthority.com/nothing-glyph-matrix-too-complicated-3581237/),
[Glyphify](https://www.androidauthority.com/nothing-phone-glyphify-3596711/)) ·
[Beebom Glyph Matrix guide](https://gadgets.beebom.com/guides/nothing-phone-3-glyph-matrix-guide) ·
[Pocket-lint](https://www.pocket-lint.com/nothing-phone-3-glyph-matrix-toys-i-genuinely-love/) ·
[NothingTec on Glyph Museum 2.0](https://nothingtec.com/en/2026/08/07/glyph-museum-2-0-nothing-phone-app/) ·
[Nothing Playground](https://playground.nothing.tech/toys) ·
[XDA GlyphWorks thread](https://xdaforums.com/t/glyphworks-adding-glyph-toys-support-to-nothing-phone-4a-pro.4798907/) ·
[9to5google on market exits](https://9to5google.com/2026/07/24/nothing-exiting-global-markets-report/) ·
[SDK LICENSE.md](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/blob/main/LICENSE.md)
