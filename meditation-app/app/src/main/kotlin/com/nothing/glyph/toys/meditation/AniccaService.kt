package com.nothing.glyph.toys.meditation

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphMatrixManager
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class AniccaService : Service() {

    private lateinit var glyphManager: GlyphMatrixManager
    private val handler = Handler(Looper.getMainLooper())

    private var progress = 0f
    private var isPlaying = false
    private var durationMs = DEFAULT_DURATION_MS
    private var lastFrameUptimeMs = 0L
    private var lastStateDispatchMs = 0L
    private var bloomStep = -1

    override fun onCreate() {
        super.onCreate()
        glyphManager = GlyphMatrixManager.getInstance(this)
        glyphManager.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(componentName: ComponentName) {
                glyphManager.register(Glyph.DEVICE_25111p)
            }

            override fun onServiceDisconnected(componentName: ComponentName) = Unit
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val command = intent?.getStringExtra(EXTRA_COMMAND) ?: COMMAND_START
        val requestedDuration = intent?.getLongExtra(EXTRA_DURATION, durationMs) ?: durationMs
        durationMs = requestedDuration.coerceAtLeast(MIN_DURATION_MS)

        when (command) {
            COMMAND_START -> startSession()
            COMMAND_PAUSE -> pauseSession()
            COMMAND_RESET -> resetSession()
            COMMAND_PREVIEW -> previewSession(
                intent?.getFloatExtra(EXTRA_PROGRESS, progress) ?: progress
            )
            COMMAND_QUERY_STATE -> dispatchState(force = true)
            else -> Log.w(TAG, "Unknown command: $command")
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        cancelAnimationLoops()
        runCatching { glyphManager.turnOff() }
        glyphManager.unInit()
        super.onDestroy()
    }

    private fun startSession() {
        if (progress >= 1f) {
            progress = 0f
        }

        cancelBloom()
        isPlaying = true
        lastFrameUptimeMs = SystemClock.elapsedRealtime()
        handler.removeCallbacks(frameRunnable)
        handler.post(frameRunnable)
        renderFrame(progress)
        dispatchState(force = true)
    }

    private fun pauseSession() {
        if (!isPlaying) {
            dispatchState(force = true)
            return
        }

        advanceProgress(SystemClock.elapsedRealtime())
        isPlaying = false
        handler.removeCallbacks(frameRunnable)
        renderFrame(progress)
        dispatchState(force = true)
    }

    private fun resetSession() {
        cancelAnimationLoops()
        progress = 0f
        renderFrame(progress)
        dispatchState(force = true)
    }

    private fun previewSession(requestedProgress: Float) {
        cancelAnimationLoops()
        progress = requestedProgress.coerceIn(0f, 1f)
        renderFrame(progress)
        dispatchState(force = true)
    }

    private fun cancelAnimationLoops() {
        isPlaying = false
        handler.removeCallbacks(frameRunnable)
        cancelBloom()
    }

    private fun cancelBloom() {
        bloomStep = -1
        handler.removeCallbacks(bloomRunnable)
    }

    private val frameRunnable = object : Runnable {
        override fun run() {
            if (!isPlaying) return

            val now = SystemClock.elapsedRealtime()
            advanceProgress(now)
            renderFrame(progress)
            dispatchState()

            if (progress >= 1f) {
                isPlaying = false
                startBloom()
                dispatchState(force = true)
            } else {
                handler.postDelayed(this, FRAME_INTERVAL_MS)
            }
        }
    }

    private fun advanceProgress(nowMs: Long) {
        if (lastFrameUptimeMs == 0L) {
            lastFrameUptimeMs = nowMs
            return
        }

        val deltaMs = max(0L, nowMs - lastFrameUptimeMs)
        lastFrameUptimeMs = nowMs
        progress = min(1f, progress + deltaMs.toFloat() / durationMs.toFloat())
    }

    private fun startBloom() {
        bloomStep = 0
        handler.post(bloomRunnable)
    }

    private val bloomRunnable = object : Runnable {
        override fun run() {
            if (bloomStep !in 0 until BLOOM_STEPS) return

            renderBloomFrame(bloomStep)
            bloomStep += 1

            if (bloomStep < BLOOM_STEPS) {
                handler.postDelayed(this, BLOOM_INTERVAL_MS)
            } else {
                bloomStep = -1
            }
        }
    }

    private fun dispatchState(force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastStateDispatchMs < STATE_INTERVAL_MS) return

        lastStateDispatchMs = now
        sendBroadcast(Intent(ACTION_STATE).apply {
            setPackage(packageName)
            putExtra(EXTRA_IS_PLAYING, isPlaying)
            putExtra(EXTRA_PROGRESS, progress)
            putExtra(EXTRA_DURATION, durationMs)
        })
    }

    private fun renderFrame(sessionProgress: Float) {
        val frame = IntArray(MATRIX_SIZE * MATRIX_SIZE)

        when {
            sessionProgress < PHASE_TWO_START -> renderSettling(frame, sessionProgress / PHASE_TWO_START)
            sessionProgress < PHASE_THREE_START -> renderDeepening(
                frame,
                (sessionProgress - PHASE_TWO_START) / (PHASE_THREE_START - PHASE_TWO_START)
            )
            else -> renderStill(
                frame,
                (sessionProgress - PHASE_THREE_START) / (1f - PHASE_THREE_START)
            )
        }

        pushFrame(frame)
    }

    private fun renderSettling(frame: IntArray, phaseProgress: Float) {
        val particleCount = max(4, (14 - phaseProgress * 10f).roundToInt())
        val centerWeight = (120 + 135 * phaseProgress).roundToInt()

        for (i in 0 until particleCount) {
            val orbit = i.toFloat() / particleCount.toFloat()
            val angle = (phaseProgress * 2.5f + orbit * GOLDEN_TURN) * (2f * PI.toFloat())
            val radius = (1f - phaseProgress) * (5.6f - orbit * 1.8f)
            val x = (CENTER + cos(angle) * radius).roundToInt()
            val y = (CENTER + sin(angle) * radius).roundToInt()
            val brightness = (185 - phaseProgress * 110f - orbit * 40f).roundToInt()
            plot(frame, x, y, brightness)
        }

        val haloRadius = 1.2f + (1f - phaseProgress) * 1.2f
        fillDisc(frame, haloRadius, (24 + phaseProgress * 36f).roundToInt())
        plot(frame, CENTER, CENTER, centerWeight)
    }

    private fun renderDeepening(frame: IntArray, phaseProgress: Float) {
        fillDisc(frame, 1.4f, 210)
        val rippleRadius = 1.6f + phaseProgress * 4.2f
        val secondaryRadius = max(1.2f, rippleRadius - 1.6f)

        forEachValidLed { x, y, distance ->
            val primary = ringBrightness(distance, rippleRadius, width = 0.8f)
            val secondary = ringBrightness(distance, secondaryRadius, width = 0.65f)
            val falloff = (1f - phaseProgress * 0.7f).coerceIn(0.25f, 1f)
            val brightness = max(
                (primary * 120f * falloff).roundToInt(),
                (secondary * 65f * (1f - phaseProgress)).roundToInt()
            )
            plot(frame, x, y, brightness)
        }

        plot(frame, CENTER, CENTER, 255)
    }

    private fun renderStill(frame: IntArray, phaseProgress: Float) {
        forEachValidLed { x, y, distance ->
            val breath = ((1f + cos((distance * 0.9f) - phaseProgress * 2f * PI.toFloat())) / 2f)
            val radialFade = (1f - distance / 6.2f).coerceAtLeast(0f)
            val ambient = (12f + 38f * breath * radialFade).roundToInt()
            plot(frame, x, y, ambient)
        }

        fillDisc(frame, 1.25f, 88)
        plot(frame, CENTER, CENTER, 255)
    }

    private fun renderBloomFrame(step: Int) {
        val frame = IntArray(MATRIX_SIZE * MATRIX_SIZE)
        val radius = 1.4f + step * 1.2f
        val strength = max(70, 220 - step * 28)

        forEachValidLed { x, y, distance ->
            if (distance <= radius) {
                val edgeFade = (1f - distance / max(radius, 0.001f)).coerceAtLeast(0f)
                plot(frame, x, y, (strength * edgeFade).roundToInt())
            }
        }

        plot(frame, CENTER, CENTER, 255)
        pushFrame(frame)
    }

    private fun fillDisc(frame: IntArray, radius: Float, brightness: Int) {
        forEachValidLed { x, y, distance ->
            if (distance <= radius) {
                val fade = (1f - distance / max(radius, 0.001f)).coerceAtLeast(0f)
                plot(frame, x, y, (brightness * fade).roundToInt())
            }
        }
    }

    private fun ringBrightness(distance: Float, radius: Float, width: Float): Float {
        val delta = abs(distance - radius)
        if (delta >= width) return 0f
        return 1f - delta / width
    }

    private fun plot(frame: IntArray, x: Int, y: Int, brightness: Int) {
        if (!isValidLed(x, y)) return
        val index = y * MATRIX_SIZE + x
        frame[index] = max(frame[index], brightness.coerceIn(0, 255))
    }

    private fun pushFrame(frame: IntArray) {
        try {
            glyphManager.setMatrixFrame(frame)
        } catch (error: GlyphException) {
            Log.e(TAG, "setMatrixFrame failed", error)
        }
    }

    private fun forEachValidLed(block: (x: Int, y: Int, distance: Float) -> Unit) {
        for (y in 0 until MATRIX_SIZE) {
            for (x in 0 until MATRIX_SIZE) {
                if (!isValidLed(x, y)) continue
                val dx = x - CENTER.toFloat()
                val dy = y - CENTER.toFloat()
                block(x, y, sqrt(dx * dx + dy * dy))
            }
        }
    }

    private fun isValidLed(x: Int, y: Int): Boolean {
        if (x !in 0 until MATRIX_SIZE || y !in 0 until MATRIX_SIZE) return false
        val dx = x - CENTER
        val dy = y - CENTER
        return sqrt((dx * dx + dy * dy).toFloat()) <= 6.5f
    }

    companion object {
        const val ACTION_STATE = "com.nothing.glyph.toys.meditation.STATE"

        const val EXTRA_COMMAND = "EXTRA_COMMAND"
        const val EXTRA_DURATION = "EXTRA_DURATION"
        const val EXTRA_PROGRESS = "EXTRA_PROGRESS"
        const val EXTRA_IS_PLAYING = "EXTRA_IS_PLAYING"

        const val COMMAND_START = "START"
        const val COMMAND_PAUSE = "PAUSE"
        const val COMMAND_RESET = "RESET"
        const val COMMAND_PREVIEW = "PREVIEW"
        const val COMMAND_QUERY_STATE = "QUERY_STATE"

        const val DEFAULT_DURATION_MS = 30L * 60_000L
        const val MIN_DURATION_MS = 60_000L

        private const val TAG = "AniccaService"
        private const val MATRIX_SIZE = 13
        private const val CENTER = 6
        private const val FRAME_INTERVAL_MS = 100L
        private const val STATE_INTERVAL_MS = 300L
        private const val BLOOM_INTERVAL_MS = 180L
        private const val BLOOM_STEPS = 5
        private const val PHASE_TWO_START = 0.42f
        private const val PHASE_THREE_START = 0.76f
        private const val GOLDEN_TURN = 0.618034f
    }
}
