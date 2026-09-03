package dev.omesh.glyphtoys.glyph

import kotlin.math.hypot

/**
 * Geometry of a Glyph Matrix panel.
 *
 * The panel is addressed as a square grid, but only the LEDs inside a circle are physically
 * present. `radius = size / 2` reproduces Nothing's published counts exactly on both panels:
 * 25x25 -> 489 lit, 13x13 -> 137 lit.
 *
 * Pure Kotlin on purpose: no SDK types here, so the geometry is unit-testable off-device.
 */
class MatrixGeometry(val size: Int) {

    val cellCount: Int = size * size

    private val center = (size - 1) / 2f
    private val radius = size / 2f

    /** True if a physical LED exists at (x, y). Cells outside the circle are never displayed. */
    fun isLit(x: Int, y: Int): Boolean =
        x in 0 until size && y in 0 until size && hypot(x - center, y - center) <= radius

    /** Row-major index into the frame array the SDK expects. */
    fun index(x: Int, y: Int): Int = y * size + x

    /** Number of physical LEDs. 489 on Phone (3), 137 on Phone (4a) Pro. */
    val litCount: Int by lazy {
        var n = 0
        for (y in 0 until size) for (x in 0 until size) if (isLit(x, y)) n++
        n
    }

    /** Zeroes every cell that has no physical LED behind it. */
    fun maskInPlace(frame: IntArray) {
        require(frame.size == cellCount) { "frame is ${frame.size}, expected $cellCount" }
        for (y in 0 until size) for (x in 0 until size) {
            if (!isLit(x, y)) frame[index(x, y)] = 0
        }
    }

    companion object {
        /** Nothing Phone (3), device code A024. */
        val PHONE_3 = MatrixGeometry(25)

        /** Nothing Phone (4a) Pro, device code A069P. */
        val PHONE_4A_PRO = MatrixGeometry(13)
    }
}
