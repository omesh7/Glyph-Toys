// Copy into a toy's own package (e.g. com.nothing.glyph.toys.<name>) and
// adjust the package declaration below.
//
// Original clean-room implementation for this repo, informed by the
// device-detection approach used in the community's Nothing-Developer-Programme
// / GlyphMarquee toy (see DEVELOPER_GUIDE.md "Community Reference").
package com.nothing.glyph.toys.shared

import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph

/**
 * Detects which supported Glyph Matrix device is running instead of
 * hardcoding Glyph.DEVICE_25111p, so a toy can scale its rendering to
 * whichever matrix it actually finds itself on.
 */
object GlyphDeviceUtils {

    /** Side length of the square matrix on this device (13 for Phone (4a) Pro, 25 for Phone (3)). */
    fun matrixLength(): Int = Common.getDeviceMatrixLength()

    /** The device id to pass to GlyphMatrixManager.register(...) for this device. */
    fun deviceId(): String = when {
        Common.is25111p() -> Glyph.DEVICE_25111p
        Common.is23112() -> Glyph.DEVICE_23112
        else -> Glyph.DEVICE_25111p // repo baseline: Nothing Phone (4a) Pro
    }

    /** True on Nothing Phone (4a) Pro, which has no screen-on Glyph Toy mode — AOD is the only trigger. */
    fun isAodOnlyDevice(): Boolean = Common.is25111p()
}
