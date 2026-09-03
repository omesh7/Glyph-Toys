package dev.omesh.glyphtoys.glyph

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToneTest {

    private fun argb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    @Test
    fun `linearize and encode round trip`() {
        for (v in 0..255 step 5) {
            val round = Tone.encode(Tone.linearize(v)) * 255f
            assertTrue(abs(round - v) < 1f, "channel $v round-tripped to $round")
        }
    }

    @Test
    fun `mid grey stays mid grey, not darkened`() {
        // sRGB 128 is ~21% linear light. A pipeline that averages in linear and re-encodes must
        // give 128 back; one that forgets to re-encode collapses it to ~54 and looks muddy.
        val pixels = IntArray(4) { argb(128, 128, 128) }
        val out = Tone.downscale(pixels, 2, 2, 1)
        assertEquals(128, (out[0] * 255).toInt(), "mid grey should survive the round trip")
    }

    /**
     * The bug this whole file exists to avoid: averaging black and white in sRGB gives ~128,
     * but half the light is 50% linear, which is ~188 encoded. Getting this wrong makes every
     * downscaled image too dark.
     */
    @Test
    fun `black and white average in linear light, not sRGB`() {
        val pixels = intArrayOf(argb(0, 0, 0), argb(255, 255, 255))
        val out = Tone.downscale(pixels, 2, 1, 1)
        val encoded = (out[0] * 255).toInt()
        assertTrue(encoded in 180..195, "expected ~188 for half the light, got $encoded")
    }

    @Test
    fun `luma weights green above blue`() {
        val green = Tone.downscale(IntArray(1) { argb(0, 255, 0) }, 1, 1, 1)[0]
        val blue = Tone.downscale(IntArray(1) { argb(0, 0, 255) }, 1, 1, 1)[0]
        val red = Tone.downscale(IntArray(1) { argb(255, 0, 0) }, 1, 1, 1)[0]
        assertTrue(green > red && red > blue, "green $green > red $red > blue $blue")
    }

    @Test
    fun `downscale covers every source pixel and fills the grid`() {
        // A lone bright pixel in the top-left must not vanish when shrinking 26x26 to 13x13.
        val pixels = IntArray(26 * 26) { argb(0, 0, 0) }
        pixels[0] = argb(255, 255, 255)
        val out = Tone.downscale(pixels, 26, 26, 13)
        assertEquals(169, out.size)
        assertTrue(out[0] > 0f, "top-left detail was dropped")
        assertEquals(0f, out[168], "bottom-right should stay black")
    }

    @Test
    fun `autoContrast stretches a dull image to the full range`() {
        val values = floatArrayOf(0.40f, 0.45f, 0.50f, 0.55f, 0.60f)
        Tone.autoContrast(values, clipLow = 0f, clipHigh = 0f)
        assertEquals(0f, values.first())
        assertEquals(1f, values.last())
        assertTrue(values.toList() == values.sortedArray().toList(), "order must be preserved")
    }

    @Test
    fun `autoContrast leaves a flat image alone rather than amplifying noise`() {
        val values = floatArrayOf(0.50f, 0.50f, 0.51f)
        Tone.autoContrast(values)
        assertTrue(values.all { it in 0.49f..0.52f }, "flat input should be untouched, got ${values.toList()}")
    }

    @Test
    fun `floydSteinberg quantises to the requested levels and conserves total light`() {
        val size = 8
        val values = FloatArray(size * size) { 0.5f }
        val before = values.sum()
        Tone.floydSteinberg(values, size, levels = 2)

        assertTrue(values.all { it == 0f || it == 1f }, "two levels means black or white only")
        // Error diffusion should preserve average brightness to within a few percent.
        assertTrue(abs(values.sum() - before) / before < 0.1f, "lost light: ${values.sum()} vs $before")
    }
}
