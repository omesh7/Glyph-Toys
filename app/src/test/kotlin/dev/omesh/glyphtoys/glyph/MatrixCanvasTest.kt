package dev.omesh.glyphtoys.glyph

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatrixCanvasTest {

    /**
     * The load-bearing claim: `radius = size / 2` reproduces Nothing's published LED counts.
     * If this breaks, every toy is drawing into cells that don't physically exist.
     */
    @Test
    fun `circular mask matches published LED counts`() {
        assertEquals(489, MatrixGeometry.PHONE_3.litCount, "Phone (3) 25x25")
        assertEquals(137, MatrixGeometry.PHONE_4A_PRO.litCount, "Phone (4a) Pro 13x13")
    }

    @Test
    fun `unlit cells stay dark`() {
        val g = MatrixGeometry.PHONE_4A_PRO
        val c = MatrixCanvas(g)
        c.disc(6f, 6f, 100f) // far larger than the panel
        val frame = c.toFrame()
        assertEquals(g.litCount, frame.count { it > 0 }, "only physical LEDs should light")
        assertEquals(0, frame[g.index(0, 0)], "corner has no LED behind it")
    }

    @Test
    fun `anti-aliasing produces partial coverage`() {
        val c = MatrixCanvas(MatrixGeometry.PHONE_4A_PRO)
        c.disc(6f, 6f, 3.5f)
        val frame = c.toFrame()
        val partial = frame.count { it > 0 && it < 255 }
        assertTrue(partial > 0, "a soft edge should produce values between off and full, got $frame")
    }

    @Test
    fun `gamma encoding is monotonic and hits both endpoints`() {
        val g = MatrixGeometry(13)
        // A horizontal ramp across the middle row via increasing-intensity discs.
        val values = (0..10).map { step ->
            val c = MatrixCanvas(g)
            c.disc(6f, 6f, 1f, step / 10f)
            c.toFrame()[g.index(6, 6)]
        }
        assertEquals(0, values.first(), "zero intensity must be fully off")
        assertEquals(255, values.last(), "full intensity must be full scale")
        assertEquals(values.sorted(), values, "encoding must be monotonic: $values")
    }

    @Test
    fun `full sweep arc equals a ring`() {
        val g = MatrixGeometry.PHONE_4A_PRO
        val arc = MatrixCanvas(g).apply { arc(6f, 6f, 4f, 1f, 1f) }.toFrame()
        val ring = MatrixCanvas(g).apply { ring(6f, 6f, 4f, 1f) }.toFrame()
        assertContentEquals(ring, arc)
    }

    @Test
    fun `arc grows with sweep and starts at twelve o'clock`() {
        val g = MatrixGeometry.PHONE_4A_PRO
        fun litAt(sweep: Float) = MatrixCanvas(g)
            .apply { arc(6f, 6f, 4f, 1f, sweep) }
            .toFrame().count { it > 0 }

        assertEquals(0, litAt(0f), "no sweep draws nothing")
        assertTrue(litAt(0.25f) < litAt(0.5f), "a quarter turn lights fewer LEDs than a half")
        assertTrue(litAt(0.5f) < litAt(1f), "a half turn lights fewer LEDs than a full")

        // A small sweep must light the top of the ring, not the right or bottom.
        val small = MatrixCanvas(g).apply { arc(6f, 6f, 4f, 1f, 0.05f) }.toFrame()
        assertTrue(small[g.index(6, 2)] > 0, "12 o'clock should be lit")
        assertEquals(0, small[g.index(6, 10)], "6 o'clock should be dark")
    }
}
