// Copy into a toy's own package (e.g. com.nothing.glyph.toys.<name>) and
// adjust the package declaration below.
//
// Original clean-room implementation for this repo, capturing lifecycle
// gotchas documented from the community's Nothing-Developer-Programme /
// GlyphMarquee toy (see DEVELOPER_GUIDE.md "Community Reference"):
//   - GlyphToy.EVENT_AOD fires roughly once/minute; only the first such
//     event in a session should (re)start rendering, or the animation
//     jumps back to frame 0 every minute.
//   - onUnbind is not reliably called on every screen wake, so "the user
//     woke the screen" has to be detected separately (e.g. an
//     ACTION_SCREEN_ON BroadcastReceiver) and fed into onScreenOn().
//   - Turning off LEDs immediately on unbind/screen-off causes visible
//     flicker from brief system rebind churn during AOD transitions —
//     delay it instead.
package com.nothing.glyph.toys.shared

import android.os.Handler

/**
 * Wraps a toy's AOD start/stop around the above gotchas.
 *
 * Wire it up as:
 *  - call [onAodEvent] from the Messenger handler for GlyphToy.EVENT_AOD
 *  - call [onScreenOn] from an ACTION_SCREEN_ON BroadcastReceiver
 *  - call [onUnbind] from the service's onUnbind/onDestroy
 */
class AodLifecycleGuard(
    private val handler: Handler,
    private val stopDelayMs: Long = 500L,
    private val timeoutMs: Long? = null,
    private val onStart: () -> Unit,
    private val onStop: () -> Unit,
) {
    private var isRunning = false
    private var timedOut = false

    private val timeoutRunnable = Runnable {
        onStop()
        isRunning = false
        timedOut = true
    }

    private val delayedStopRunnable = Runnable {
        if (isRunning) {
            onStop()
            isRunning = false
        }
    }

    /** Call from the GlyphToy.EVENT_AOD Messenger handler. */
    fun onAodEvent() {
        if (isRunning || timedOut) return
        isRunning = true
        onStart()
        timeoutMs?.let { handler.postDelayed(timeoutRunnable, it) }
    }

    /** Call from an ACTION_SCREEN_ON BroadcastReceiver — onUnbind alone is not reliable here. */
    fun onScreenOn() {
        handler.removeCallbacks(timeoutRunnable)
        handler.postDelayed(delayedStopRunnable, stopDelayMs)
        timedOut = false
    }

    /** Call from onUnbind/onDestroy for a clean shutdown outside the AOD flow. */
    fun onUnbind() {
        handler.removeCallbacks(timeoutRunnable)
        handler.postDelayed(delayedStopRunnable, stopDelayMs)
    }
}
