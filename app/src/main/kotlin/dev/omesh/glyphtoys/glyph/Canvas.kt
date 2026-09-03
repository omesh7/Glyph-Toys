package dev.omesh.glyphtoys.glyph

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt

private const val TAU = (2 * Math.PI).toFloat()

/**
 * A small anti-aliased drawing surface for the Glyph Matrix.
 *
 * Two things the community toys don't do, and why they look chunky:
 *
 *  1. **Coverage, not thresholding.** Shapes are rasterised from a signed distance field, so an
 *     edge that falls between LEDs lights both partially. At 137 LEDs a hard threshold wastes
 *     most of the apparent resolution.
 *  2. **Gamma.** An LED's light output is proportional to its PWM duty cycle, but perceived
 *     brightness is not. Writing a linear ramp straight to the panel makes the dark end crawl and
 *     the bright end flatten. [toFrame] encodes through `v^gamma` so a linear ramp *looks* linear.
 *
 * Intensities are perceptual, 0..1. Pure Kotlin so it is unit-testable off-device.
 */
class MatrixCanvas(val geometry: MatrixGeometry) {

    private val size = geometry.size
    private val buf = FloatArray(geometry.cellCount)

    fun clear() = buf.fill(0f)

    /** Brightest-wins blend, so overlapping shapes never bloom past full scale. */
    private fun plot(x: Int, y: Int, v: Float) {
        if (v <= 0f || !geometry.isLit(x, y)) return
        val i = geometry.index(x, y)
        if (v > buf[i]) buf[i] = v.coerceAtMost(1f)
    }

    /** Rasterise [coverage] over every lit cell. Returns coverage in 0..1 for a cell centre. */
    private inline fun fill(v: Float, coverage: (Float, Float) -> Float) {
        for (y in 0 until size) for (x in 0 until size) {
            plot(x, y, v * coverage(x.toFloat(), y.toFloat()).coerceIn(0f, 1f))
        }
    }

    /** Filled circle, anti-aliased over the last half-cell. */
    fun disc(cx: Float, cy: Float, r: Float, v: Float = 1f) =
        fill(v) { x, y -> r + 0.5f - hypot(x - cx, y - cy) }

    /** Circle outline of the given [thickness], anti-aliased on both edges. */
    fun ring(cx: Float, cy: Float, r: Float, thickness: Float = 1f, v: Float = 1f) =
        fill(v) { x, y -> thickness / 2f + 0.5f - abs(hypot(x - cx, y - cy) - r) }

    /**
     * Arc of a ring, sweeping clockwise from 12 o'clock.
     *
     * [sweep] is a fraction of a full turn in 0..1 — pass a battery level straight in.
     */
    fun arc(cx: Float, cy: Float, r: Float, thickness: Float, sweep: Float, v: Float = 1f) {
        if (sweep <= 0f) return
        if (sweep >= 1f) return ring(cx, cy, r, thickness, v)
        val sweepRad = sweep.coerceIn(0f, 1f) * TAU
        fill(v) { x, y ->
            // Angle clockwise from straight up, in 0..TAU.
            var a = atan2(x - cx, cy - y)
            if (a < 0f) a += TAU
            if (a > sweepRad) 0f
            else thickness / 2f + 0.5f - abs(hypot(x - cx, y - cy) - r)
        }
    }

    /** Line segment of the given [thickness], anti-aliased. */
    fun line(x0: Float, y0: Float, x1: Float, y1: Float, thickness: Float = 1f, v: Float = 1f) {
        val dx = x1 - x0
        val dy = y1 - y0
        val lenSq = dx * dx + dy * dy
        fill(v) { x, y ->
            val t = if (lenSq == 0f) 0f else (((x - x0) * dx + (y - y0) * dy) / lenSq).coerceIn(0f, 1f)
            thickness / 2f + 0.5f - hypot(x - (x0 + t * dx), y - (y0 + t * dy))
        }
    }

    /**
     * Encode to the array the SDK expects.
     *
     * [max] is 255 to match `GlyphMatrixObject.setBrightness`, which is the documented range.
     * [gamma] of 2.2 is the standard display exponent; calibrate against real hardware before
     * treating it as final — see PLAN.md.
     */
    fun toFrame(gamma: Float = 2.2f, max: Int = 255): IntArray =
        IntArray(buf.size) { (buf[it].pow(gamma) * max).roundToInt().coerceIn(0, max) }
}
