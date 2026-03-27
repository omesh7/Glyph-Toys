package com.nothing.glyph.toys.meditation

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphException
import kotlin.math.*

class AniccaService : Service() {

    private lateinit var gmManager: GlyphMatrixManager
    private var progress = 0.0f
    private var isPlaying = false
    private var durationMs = 30 * 60 * 1000L // Default 30 min
    private val handler = Handler(Looper.getMainLooper())
    private val frameRateMs = 100L // 10 FPS for smooth transitions

    override fun onCreate() {
        super.onCreate()
        gmManager = GlyphMatrixManager.getInstance(this)
        gmManager.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(componentName: android.content.ComponentName) {
                Log.d("Anicca", "Glyph Service Connected")
                gmManager.register(Glyph.DEVICE_25111p) // Targeting Phone (4a) Pro
            }
            override fun onServiceDisconnected(componentName: android.content.ComponentName) {
                Log.d("Anicca", "Glyph Service Disconnected")
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra("ACTION")
        val newDuration = intent?.getLongExtra("DURATION", durationMs) ?: durationMs
        
        durationMs = newDuration

        when (action) {
            "START" -> startSession()
            "STOP" -> stopSession()
            "RESET" -> resetSession()
            "PREVIEW" -> {
                isPlaying = false
                handler.removeCallbacks(updateRunnable)
                val p = intent?.getFloatExtra("PROGRESS", 0.0f) ?: 0.0f
                progress = p
                renderFrame(p)
            }
        }
        return START_STICKY
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isPlaying && progress < 1.0f) {
                progress += (frameRateMs.toFloat() / durationMs.toFloat())
                renderFrame(progress)
                handler.postDelayed(this, frameRateMs)
            } else if (progress >= 1.0f) {
                isPlaying = false
                renderBloom() // Final "Bloom" on completion
            }
        }
    }

    private fun startSession() {
        isPlaying = true
        handler.post(updateRunnable)
    }

    private fun stopSession() {
        isPlaying = false
        handler.removeCallbacks(updateRunnable)
        gmManager.turnOff()
    }

    private fun resetSession() {
        stopSession()
        progress = 0.0f
        renderFrame(0.0f)
    }

    // --- Core Visualization Logic (13x13 Matrix) ---

    private fun renderFrame(p: Float) {
        val frame = IntArray(169) { 0 }
        val W = 13
        val centerX = 6
        val centerY = 6

        when {
            // Phase 1: Settling (0% - 42%)
            p < 0.42f -> {
                val t = p / 0.42f
                val count = (20 * (1 - t)).toInt() // More dots at start
                for (i in 0 until count) {
                    val angle = (i * 137.5).toDouble() // Golden angle distribution
                    val dist = (1 - t) * 6.0 * (i.toDouble() / count)
                    val x = (centerX + dist * cos(angle)).toInt().coerceIn(0, 12)
                    val y = (centerY + dist * sin(angle)).toInt().coerceIn(0, 12)
                    frame[y * W + x] = (150 * (1 - t)).toInt()
                }
                // Center point starts appearing
                frame[centerY * W + centerX] = (255 * t).toInt()
            }

            // Phase 2: Deepening (42% - 76%)
            p < 0.76f -> {
                val t = (p - 0.42f) / 0.34f
                val rippleRadius = (t * 6.0) % 6.0
                for (y in 0 until 13) {
                    for (x in 0 until 13) {
                        val dx = x - centerX
                        val dy = y - centerY
                        val d = sqrt((dx * dx + dy * dy).toDouble())
                        if (abs(d - rippleRadius) < 1.0) {
                            frame[y * W + x] = (100 * (1 - t)).toInt()
                        }
                    }
                }
                frame[centerY * W + centerX] = 255
            }

            // Phase 3: Still (76% - 100%)
            else -> {
                val t = (p - 0.76f) / 0.24f
                for (y in 0 until 13) {
                    for (x in 0 until 13) {
                        val dx = (x - centerX).toDouble()
                        val dy = (y - centerY).toDouble()
                        val d = sqrt(dx * dx + dy * dy)
                        if (d < 6.0) {
                            val pulse = (1 + cos(d * PI * 0.5 - t * PI)).toFloat() / 2f
                            frame[y * W + x] = (50 * pulse * (1 - d / 6.0)).toInt()
                        }
                    }
                }
                frame[centerY * W + centerX] = 255
            }
        }
        
        try {
            gmManager.setMatrixFrame(frame)
        } catch (e: GlyphException) {
            Log.e("Anicca", "Glyph Exception: ${e.message}")
        }
    }

    private fun renderBloom() {
        val bloomHandler = Handler(Looper.getMainLooper())
        for (i in 0 until 5) {
            bloomHandler.postDelayed({
                val bloomFrame = IntArray(169) { idx ->
                    val x = (idx % 13).toDouble()
                    val y = (idx / 13).toDouble()
                    val d = sqrt((x - 6.0).pow(2.0) + (y - 6.0).pow(2.0))
                    if (d < i * 2) (255 / (i + 1)) else 0
                }
                try {
                    gmManager.setMatrixFrame(bloomFrame)
                } catch (e: GlyphException) {
                    Log.e("Anicca", "Bloom Exception: ${e.message}")
                }
            }, (i * 200).toLong())
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        gmManager.turnOff()
        gmManager.unInit()
        super.onDestroy()
    }
}
