# DVD Bounce Glyph Toy - Logic Documentation

## 🎮 The Concept
Recreate the classic retro DVD logo bouncing on a screen, but for the **Nothing Phone (4a) Pro** 13x13 Glyph Matrix.

## 📐 Physics Engine (13x13 Grid)
- **Bounding Box:** 0 to 12 on both X and Y axes.
- **"DVD Logo" Size:** A 2x2 cluster of LEDs for better visibility.
- **Velocity:** Randomized initial vectors (e.g., `vx = 0.5, vy = 0.5`).
- **Collision Detection:**
    - If `x <= 0` or `x + logo_width >= 13`: Reverse `vx`.
    - If `y <= 0` or `y + logo_height >= 13`: Reverse `vy`.
- **The "Perfect Corner Hit":** If a collision occurs simultaneously on both X and Y axes, trigger a high-brightness "flash" across the entire matrix.

## 🔄 "Flip to Glyph" Integration
- **Sensor:** Use `Sensor.TYPE_GRAVITY` or `Sensor.TYPE_ACCELEROMETER`.
- **Trigger Condition:**
    - `z_axis < -9.0` (Phone is face-down).
    - `Proximity Sensor` reporting "Near" (Phone is on a surface).
- **Behavior:**
    - When flipped: Start the `BounceService`.
    - When picked up: Stop the service and `turnOff()` the matrix.

## 💡 Visual Styles
- **Normal Play:** 50% brightness for the 2x2 logo.
- **Edge Hit:** 80% brightness pulse.
- **Corner Hit:** 100% full-matrix flash for 100ms.

## 🚀 Technical Requirements
- **SDK:** Nothing GlyphMatrixSDK v2.0.
- **Target Device:** Phone (4a) Pro (137-LED Matrix).
- **Service Type:** Foreground Service (to maintain sensor listener while screen is off).
