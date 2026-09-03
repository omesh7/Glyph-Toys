package dev.omesh.glyphtoys.toys

import android.os.BatteryManager
import dev.omesh.glyphtoys.glyph.GlyphToyService
import dev.omesh.glyphtoys.glyph.MatrixCanvas
import kotlin.math.sin

/**
 * Charge level as a ring.
 *
 * This is the one Glyph use case reviewers consistently call genuinely useful: put the phone
 * face down, plug it in, read the charge without touching the screen. So it is toy number one.
 *
 * A dim full ring is the track; a bright arc from twelve o'clock is the level. While charging,
 * the arc breathes and a dot pulses in the middle, so a glance tells you charging from resting
 * without reading the number.
 */
class ChargeToyService : GlyphToyService() {

    private val battery by lazy { getSystemService(BatteryManager::class.java) }

    private var level = 0f
    private var charging = false
    private var lastPollMs = Long.MIN_VALUE

    override fun draw(canvas: MatrixCanvas, elapsedMs: Long) {
        poll(elapsedMs)

        val c = (geometry.size - 1) / 2f
        val radius = geometry.size / 2f - 1.5f
        val thickness = if (geometry.size >= 25) 2f else 1f

        canvas.ring(c, c, radius, thickness, TRACK_INTENSITY)

        // Breathe between half and full brightness while charging; hold steady otherwise.
        val breath = if (charging) 0.75f + 0.25f * sin(elapsedMs / BREATH_PERIOD_MS).toFloat() else 1f
        canvas.arc(c, c, radius, thickness, level, breath)

        if (charging) {
            val pulse = 0.5f + 0.5f * sin(elapsedMs / BREATH_PERIOD_MS).toFloat()
            canvas.disc(c, c, radius * 0.18f, pulse)
        }
    }

    private fun poll(elapsedMs: Long) {
        if (elapsedMs - lastPollMs < POLL_INTERVAL_MS) return
        lastPollMs = elapsedMs
        val manager = battery ?: return
        level = (manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) / 100f)
            .coerceIn(0f, 1f)
        charging = manager.isCharging
    }

    private companion object {
        const val TRACK_INTENSITY = 0.12f
        const val POLL_INTERVAL_MS = 2_000L

        /** Divisor on elapsed ms, so a full breath is a shade under four seconds. */
        const val BREATH_PERIOD_MS = 600.0
    }
}
