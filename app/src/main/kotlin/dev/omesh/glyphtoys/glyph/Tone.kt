package dev.omesh.glyphtoys.glyph

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Turning a picture into 137 LEDs.
 *
 * The mistake every community renderer makes is averaging pixels in sRGB space and calling
 * `(r + g + b) / 3` brightness. sRGB is a perceptual encoding, not light — averaging it directly
 * loses energy, which is why downscaled album art on these panels comes out muddy and too dark.
 *
 * So: decode to linear light, average there with perceptual luma weights, stretch the result to
 * use the full range (album art is often low-contrast and 137 LEDs cannot afford to waste any),
 * then re-encode to the perceptual 0..1 that [MatrixCanvas] expects.
 *
 * Pure Kotlin, so all of this is testable without a device.
 */
object Tone {

    /** sRGB channel (0..255) to linear light (0..1). */
    fun linearize(channel: Int): Float {
        val c = channel.coerceIn(0, 255) / 255f
        return if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    }

    /** Linear light (0..1) back to a perceptual 0..1. */
    fun encode(linear: Float): Float {
        val c = linear.coerceIn(0f, 1f)
        return if (c <= 0.0031308f) c * 12.92f else 1.055f * c.pow(1f / 2.4f) - 0.055f
    }

    /**
     * Box-downscale ARGB pixels to a [size] x [size] grid of perceptual luma.
     *
     * Averaging happens in linear light; the Rec. 709 weights are what the eye actually does
     * with green versus blue.
     */
    fun downscale(argb: IntArray, width: Int, height: Int, size: Int): FloatArray {
        require(width > 0 && height > 0) { "empty image" }
        require(argb.size >= width * height) { "pixel buffer smaller than $width x $height" }

        val out = FloatArray(size * size)
        for (ty in 0 until size) {
            // Source rows covered by this output row, at least one.
            val y0 = ty * height / size
            val y1 = (((ty + 1) * height + size - 1) / size).coerceAtLeast(y0 + 1).coerceAtMost(height)
            for (tx in 0 until size) {
                val x0 = tx * width / size
                val x1 = (((tx + 1) * width + size - 1) / size).coerceAtLeast(x0 + 1).coerceAtMost(width)

                var sum = 0f
                var n = 0
                for (y in y0 until y1) for (x in x0 until x1) {
                    val p = argb[y * width + x]
                    sum += 0.2126f * linearize((p shr 16) and 0xFF) +
                        0.7152f * linearize((p shr 8) and 0xFF) +
                        0.0722f * linearize(p and 0xFF)
                    n++
                }
                out[ty * size + tx] = encode(if (n == 0) 0f else sum / n)
            }
        }
        return out
    }

    /**
     * Stretch [values] so the darkest [clipLow] and brightest [clipHigh] fractions saturate.
     *
     * Clipping a little at each end is what keeps a dull cover from rendering as uniform grey.
     * Left alone when the image is nearly flat, since stretching noise only amplifies it.
     */
    fun autoContrast(values: FloatArray, clipLow: Float = 0.02f, clipHigh: Float = 0.02f) {
        if (values.isEmpty()) return
        val sorted = values.sortedArray()
        val lo = sorted[(sorted.size * clipLow).toInt().coerceIn(0, sorted.size - 1)]
        val hi = sorted[(sorted.size * (1f - clipHigh)).toInt().coerceIn(0, sorted.size - 1)]
        val span = hi - lo
        if (span < MIN_CONTRAST_SPAN) return
        for (i in values.indices) values[i] = ((values[i] - lo) / span).coerceIn(0f, 1f)
    }

    /**
     * Floyd-Steinberg error diffusion, quantising to [levels] evenly spaced steps.
     *
     * Only worth using if the panel turns out to resolve far fewer than 256 brightness steps —
     * with the full range available this is a no-op worth skipping. Kept because that is an
     * open question that needs real hardware to settle.
     */
    fun floydSteinberg(values: FloatArray, size: Int, levels: Int) {
        require(levels >= 2) { "need at least two levels, got $levels" }
        require(values.size == size * size) { "expected ${size * size} values, got ${values.size}" }
        val step = 1f / (levels - 1)
        for (y in 0 until size) for (x in 0 until size) {
            val i = y * size + x
            val old = values[i]
            val new = (old / step).roundToInt() * step
            values[i] = new.coerceIn(0f, 1f)
            val err = old - new
            if (err == 0f) continue
            spread(values, size, x + 1, y, err * 7f / 16f)
            spread(values, size, x - 1, y + 1, err * 3f / 16f)
            spread(values, size, x, y + 1, err * 5f / 16f)
            spread(values, size, x + 1, y + 1, err * 1f / 16f)
        }
    }

    private fun spread(values: FloatArray, size: Int, x: Int, y: Int, err: Float) {
        if (x !in 0 until size || y !in 0 until size) return
        values[y * size + x] += err
    }

    private const val MIN_CONTRAST_SPAN = 0.05f
}
