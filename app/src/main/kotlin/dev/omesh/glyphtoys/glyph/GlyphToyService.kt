package dev.omesh.glyphtoys.glyph

import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.SystemClock
import android.util.Log
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphToy

/**
 * Base class for a Glyph Toy.
 *
 * Register subclasses in the manifest with action `com.nothing.glyph.TOY`. The OS binds the
 * service when the user selects the toy; there is no foreground service and no `startForeground`.
 *
 * Composition runs on a dedicated thread and only the SDK call hops to main, which is what keeps
 * the UI thread free — most community toys render entirely on the main Handler.
 *
 * Device differences that shape this class:
 *  - Phone (3): delivers touch and long-press events.
 *  - Phone (4a) Pro: no touch hardware at all. Only [GlyphToy.EVENT_AOD], roughly once a minute.
 *    That is a heartbeat, not a frame clock, so it only ever *starts* our own loop.
 */
abstract class GlyphToyService : Service() {

    private val tag = javaClass.simpleName

    protected lateinit var geometry: MatrixGeometry
        private set

    private var manager: GlyphMatrixManager? = null
    private var canvas: MatrixCanvas? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var renderThread: HandlerThread? = null
    private var renderHandler: Handler? = null

    private var running = false
    private var startedAt = 0L

    /** Target frame interval. Nothing's own animation sample paces itself at 30ms. */
    protected open val frameIntervalMs: Long = 33L

    /**
     * Upper bound written to the panel.
     *
     * `GlyphMatrixObject.setBrightness` is documented as 0..255, but the raw frame path is wider:
     * Nothing's own button demo fills a raw frame with values up to 2046, and the SDK's own bitmap
     * conversion scales luminance by 16 (so 0..4080). We use the raw path because it is exactly one
     * value per LED with no scaling in between, which is what an anti-aliased renderer needs.
     *
     * Calibrate this on real hardware: if the panel looks dim, raise it toward 4080.
     */
    protected open val maxBrightness: Int = 255

    /** Draw one frame. Called off the main thread. [elapsedMs] is time since the toy started. */
    protected abstract fun draw(canvas: MatrixCanvas, elapsedMs: Long)

    /** Phone (3) only — the Glyph button was pressed. */
    protected open fun onTouchDown() = Unit

    /** Phone (3) only — the Glyph button was released. */
    protected open fun onTouchUp() = Unit

    /** Phone (3) only — long press. Requires `com.nothing.glyph.toy.longpress=1` in the manifest. */
    protected open fun onLongPress() = Unit

    // --- Toy event plumbing ---------------------------------------------------------------

    private val eventHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what != GlyphToy.MSG_GLYPH_TOY) return
            when (msg.data?.getString(GlyphToy.MSG_GLYPH_TOY_DATA)) {
                // Fires about once a minute. Re-entrant: guarded by `running` so a second AOD
                // tick never stacks a second render loop. Stuck LEDs from stacked loops are the
                // single most-patched bug in community toys.
                GlyphToy.EVENT_AOD -> start()
                GlyphToy.EVENT_ACTION_DOWN -> onTouchDown()
                GlyphToy.EVENT_ACTION_UP -> onTouchUp()
                GlyphToy.EVENT_CHANGE -> onLongPress()
            }
        }
    }

    private val messenger = Messenger(eventHandler)

    /**
     * The system does not reliably rebind the toy after a screen wake on current Nothing OS
     * builds, which leaves the panel dark until the user reselects the toy.
     */
    private val screenOnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_ON) start()
        }
    }

    private val managerCallback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(name: ComponentName?) {
            val manager = manager ?: return
            val device = currentDevice()
            if (device == null) {
                Log.w(tag, "no Glyph Matrix on this device; toy will not render")
                return
            }
            manager.register(device.code)
            geometry = MatrixGeometry(device.size)
            canvas = MatrixCanvas(geometry)
            start()
        }

        override fun onServiceDisconnected(name: ComponentName?) = stop()
    }

    final override fun onBind(intent: Intent?): IBinder? {
        registerReceiver(screenOnReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
        manager = GlyphMatrixManager.getInstance(applicationContext)?.also { it.init(managerCallback) }
        return messenger.binder
    }

    final override fun onUnbind(intent: Intent?): Boolean {
        stop()
        runCatching { unregisterReceiver(screenOnReceiver) }
        manager?.run { turnOff(); unInit() }
        manager = null
        return false
    }

    // --- Render loop ----------------------------------------------------------------------

    private fun start() {
        if (running || canvas == null) return
        running = true
        startedAt = SystemClock.elapsedRealtime()
        val thread = HandlerThread("$tag-render").also { it.start() }
        renderThread = thread
        renderHandler = Handler(thread.looper).also { it.post(frameLoop) }
    }

    private fun stop() {
        running = false
        renderHandler?.removeCallbacksAndMessages(null)
        renderThread?.quitSafely()
        renderHandler = null
        renderThread = null
    }

    private val frameLoop = object : Runnable {
        override fun run() {
            if (!running) return
            val canvas = canvas ?: return
            canvas.clear()
            draw(canvas, SystemClock.elapsedRealtime() - startedAt)
            val frame = canvas.toFrame(max = maxBrightness)
            mainHandler.post {
                // The panel can be torn down between composing and pushing.
                if (running) runCatching { manager?.setMatrixFrame(frame) }
                    .onFailure { Log.w(tag, "setMatrixFrame failed: ${it.message}") }
            }
            renderHandler?.postDelayed(this, frameIntervalMs)
        }
    }

    private class Device(val code: String, val size: Int)

    private fun currentDevice(): Device? = when {
        Common.is23112() -> Device(Glyph.DEVICE_23112, 25)   // Phone (3), A024
        Common.is25111p() -> Device(Glyph.DEVICE_25111p, 13) // Phone (4a) Pro, A069P
        else -> null
    }
}
