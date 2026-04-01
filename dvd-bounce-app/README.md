# DVD Bounce

A Glyph Toy for the Nothing Phone (4a) Pro that recreates the classic DVD screensaver bounce animation on the 13×13 circular LED matrix.

## How it works

- A 2×2 pixel "ball" bounces around inside the circular LED grid
- Border LEDs glow dimly (80) as the boundary wall
- Ball glows at full brightness (255)
- Activates when phone is flipped face-down (gravity sensor z < -8.5)
- Turns off when phone is flipped back up
- Ball resets with random speed/direction on each flip

## Specs

- **Trigger:** Flip (face-down)
- **Frame rate:** 60ms intervals
- **Physics:** Corner-aware bounce with float-precision positioning
- **Grid:** 13×13 circular matrix (~138 active LEDs)
