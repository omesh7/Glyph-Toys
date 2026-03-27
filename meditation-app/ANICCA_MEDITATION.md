# Anicca Meditation Toy - Documentation

## 🧘 Overview
**Anicca** (Pali: *impermanence*) is a meditation toy for the Nothing Phone (4a) Pro. It uses the Glyph Matrix to visualize the meditative journey across three distinct phases, mapping internal focus to external light.

## 🕰️ The 3 Phases (Adapted for 13x13 Matrix)

### **Phase 1: Settling (0% - 42% Progress)**
*   **Visual:** "Sankharas" (scattered thoughts) represented as flickering dots that slowly drift toward the center of the 13x13 matrix.
*   **Intensity:** High randomness initially, with dots fading out as they approach the center.
*   **Aesthetic:** Brownian-like motion on the Glyph Matrix.

### **Phase 2: Deepening (42% - 76% Progress)**
*   **Visual:** Concentric ripples expanding from the center.
*   **Intensity:** Subtle, rhythmic pulsing.
*   **Aesthetic:** Waves of light that "subtilize" (get softer) as progress increases.

### **Phase 3: Still (76% - 100% Progress)**
*   **Visual:** The "Untouched Mind" — a steady, very dim central core that occasionally "blooms" toward the edges.
*   **Intensity:** Very low brightness (AOD-friendly).
*   **Aesthetic:** A circular petal-like geometry on the 13x13 grid.

## ⚙️ Control Mapping
- **UI:** A mobile app interface (modeled on the provided HTML) allows users to set duration (10, 30, 45, 60 min).
- **Physical Integration (4a Pro):**
    - **Short Press:** Switch between the main Meditation App and other toys.
    - **Long Press:** Toggle "Play/Pause" for the meditation session.
    - **Haptic:** A subtle vibration when transitioning between phases.

## 📐 Matrix Mapping (13x13)
The original HTML preview uses a 25x25 grid (625 pixels). For the Phone (4a) Pro, we will:
1.  **Downsample:** Map the 25x25 coordinate system to a 13x13 array (169 length).
2.  **Circular Mask:** Since the 4a Pro's matrix is circular, we'll zero-out corners of the 13x13 grid where LEDs are physically absent (totaling 137 active LEDs).

## 🚀 Implementation Steps
1.  **Skeleton Service:** Implement `AniccaService.kt` extending `Service`.
2.  **Timer Engine:** A `Handler` or `Coroutine` that updates progress based on the selected duration.
3.  **Frame Generator:** A function that takes `progress (0.0 to 1.0)` and returns an `IntArray(169)` for the SDK.
4.  **AOD Update:** Hook into `EVENT_AOD_UPDATE` for 1-minute battery-efficient updates.
