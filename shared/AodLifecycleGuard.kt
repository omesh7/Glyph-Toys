// Copy into a toy's own package (e.g. com.nothing.glyph.toys.<name>) and
// adjust the package declaration below.
//
// Ported from bluehomewu/GlyphMarquee's MarqueeService AOD handling
// (https://github.com/bluehomewu/GlyphMarquee), generalized out of a single
// service into a reusable helper. Credit: bluehomewu.
// See DEVELOPER_GUIDE.md "Community Reference" for context.
package com.nothing.glyph.toys.shared

import android.os.Handler

/**
 * Wraps a toy's AOD start/stop around the gotchas in Nothing's AOD contract:
 *  - GlyphToy.EVENT_AOD fires roughly once/minute. Only the first event of a
 *    session should (re)start rendering — restarting on every event makes
 *    the animation jump back to frame 0 each time.
 *  - onUnbind is not reliably called on every screen wake, so "the user woke
 *    the screen" has to be detected separately (an ACTION_SCREEN_ON
 *    BroadcastReceiver) and fed into [onScreenOn] to reset the AOD session.
 *  - Starting immediately on the AOD event, or stopping immediately on
 *    unbind/screen-off, causes visible flicker from brief system rebind
 *    churn during AOD transitions — both are delayed instead.
 *  - An optional auto-off timer stops rendering after [timeoutMs], and
 *    further AOD events are suppressed until the next [onScreenOn] (battery
 *    saving for long-running AOD toys).
 *
 * Wire it up as:
 *  - call [onAodEvent] from the Messenger handler for GlyphToy.EVENT_AOD
 *  - call [onScreenOn] from an ACTION_SCREEN_ON BroadcastReceiver
 *  - call [onUnbind] from the service's onUnbind
 */
class AodLifecycleGuard(
    private val handler: Handler,
    private val startDelayMs: Long = 200L,
    private val stopDelayMs: Long = 500L,
    private val timeoutMs: Long? = null,
    private val onStart: () -> Unit,
    private val onStop: () -> Unit,
) {
    private var isRunning = false
    private var timeoutScheduled = false
    private var timedOut = false

    private val startRunnable = Runnable {
        isRunning = true
        onStart()
        if (timeoutMs != null && !timeoutScheduled) {
            timeoutScheduled = true
            handler.postDelayed(timeoutRunnable, timeoutMs)
        }
    }

    private val timeoutRunnable = Runnable {
        timeoutScheduled = false
        timedOut = true
        stopNow()
    }

    private val delayedStopRunnable = Runnable { stopNow() }

    private fun stopNow() {
        handler.removeCallbacks(startRunnable)
        isRunning = false
        onStop()
    }

    /** Call from the GlyphToy.EVENT_AOD Messenger handler. */
    fun onAodEvent() {
        if (timedOut) return // blocked until the next onScreenOn
        if (timeoutScheduled) return // already running with a countdown; don't restart
        handler.removeCallbacks(delayedStopRunnable)
        handler.postDelayed(startRunnable, startDelayMs)
    }

    /** Call from an ACTION_SCREEN_ON BroadcastReceiver — onUnbind alone is not reliable here. */
    fun onScreenOn() {
        handler.removeCallbacks(timeoutRunnable)
        timeoutScheduled = false
        timedOut = false
        if (isRunning) {
            handler.postDelayed(delayedStopRunnable, stopDelayMs)
        }
    }

    /** Call from the service's onUnbind for a clean shutdown outside the AOD flow. */
    fun onUnbind() {
        handler.postDelayed(delayedStopRunnable, stopDelayMs)
    }
}
