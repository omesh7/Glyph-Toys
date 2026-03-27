package com.nothing.glyph.toys.dvdbounce

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphException
import kotlin.math.*

class BounceService : Service(), SensorEventListener {

    private lateinit var gmManager: GlyphMatrixManager
    private lateinit var sensorManager: SensorManager
    private var gravitySensor: Sensor? = null
    
    private var isFlipped = false
    private val handler = Handler(Looper.getMainLooper())
    private val frameRateMs = 60L 

    // Physics State - Slow & Deliberate
    private var posX = 5.0f
    private var posY = 5.0f
    private var velX = 0.19f
    private var velY = 0.15f
    private val logoSize = 2

    override fun onCreate() {
        super.onCreate()
        gmManager = GlyphMatrixManager.getInstance(this)
        gmManager.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(componentName: android.content.ComponentName) {
                gmManager.register(Glyph.DEVICE_25111p)
            }
            override fun onServiceDisconnected(componentName: android.content.ComponentName) {}
        })

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_UI)
    }

    private val bounceRunnable = object : Runnable {
        override fun run() {
            if (isFlipped) {
                updatePhysics()
                renderFrame()
                handler.postDelayed(this, frameRateMs)
            }
        }
    }

    private fun updatePhysics() {
        val nextX = posX + velX
        val nextY = posY + velY

        // Check both axes from the SAME starting position
        val xHits = ballHitsWall(nextX, posY)
        val yHits = ballHitsWall(posX, nextY)

        if (xHits && yHits) {
            // Corner bounce — reverse both axes
            velX *= -1
            velY *= -1
        } else if (xHits) {
            velX *= -1
            posY = nextY
        } else if (yHits) {
            posX = nextX
            velY *= -1
        } else {
            posX = nextX
            posY = nextY
        }

        // Safety: if float drift ever lands the ball on a wall, reset to center
        if (ballHitsWall(posX, posY)) {
            posX = 5.0f
            posY = 5.0f
        }
    }

    // Check if any of the ball's 2x2 pixels would overlap a border LED or leave the circle
    private fun ballHitsWall(px: Float, py: Float): Boolean {
        val bx = px.toInt()
        val by = py.toInt()
        for (i in 0 until logoSize) {
            for (j in 0 until logoSize) {
                val tx = bx + i
                val ty = by + j
                if (!isValidLed(tx, ty) || isBorderLed(tx, ty)) {
                    return true
                }
            }
        }
        return false
    }

    // Returns true if (x,y) is a physical LED on the 137-LED circular matrix
    private fun isValidLed(x: Int, y: Int): Boolean {
        if (x < 0 || x > 12 || y < 0 || y > 12) return false
        val dx = x - 6.0
        val dy = y - 6.0
        return sqrt(dx * dx + dy * dy) <= 6.5
    }

    // Returns true if (x,y) is on the outer edge of the circle
    // (a valid LED with at least one invalid/missing neighbor)
    private fun isBorderLed(x: Int, y: Int): Boolean {
        if (!isValidLed(x, y)) return false
        return !isValidLed(x - 1, y) || !isValidLed(x + 1, y) ||
               !isValidLed(x, y - 1) || !isValidLed(x, y + 1)
    }

    private fun renderFrame() {
        val frame = IntArray(169) { 0 }
        val ballX = posX.toInt()
        val ballY = posY.toInt()

        // 1. Draw the border — every outermost LED in the 137-LED circle
        for (y in 0 until 13) {
            for (x in 0 until 13) {
                if (isBorderLed(x, y)) {
                    frame[y * 13 + x] = 80
                }
            }
        }

        // 2. Draw the Ball
        for (i in 0 until logoSize) {
            for (j in 0 until logoSize) {
                val tx = ballX + i
                val ty = ballY + j
                if (tx in 0..12 && ty in 0..12) {
                    val idx = ty * 13 + tx
                    frame[idx] = 255 
                }
            }
        }

        try {
            gmManager.setMatrixFrame(frame)
        } catch (e: GlyphException) {
            Log.e("Bounce", "Error: ${e.message}")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_GRAVITY) {
            val z = event.values[2]
            val currentlyFlipped = z < -8.5f
            
            if (currentlyFlipped && !isFlipped) {
                isFlipped = true
                resetBall()
                handler.post(bounceRunnable)
            } else if (!currentlyFlipped && isFlipped) {
                isFlipped = false
                handler.removeCallbacks(bounceRunnable)
                gmManager.turnOff()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        gmManager.turnOff()
        gmManager.unInit()
        super.onDestroy()
    }

    private fun resetBall() {
        val random = java.util.Random()
        val speeds = listOf(0.15f, 0.19f, 0.21f, 0.25f)
        velX = speeds.random() * if (random.nextBoolean()) 1f else -1f
        velY = speeds.random() * if (random.nextBoolean()) 1f else -1f
        
        // Ensure starting position is safely inside the center (4 to 7)
        posX = 4.0f + random.nextFloat() * 3.0f
        posY = 4.0f + random.nextFloat() * 3.0f
    }
}
