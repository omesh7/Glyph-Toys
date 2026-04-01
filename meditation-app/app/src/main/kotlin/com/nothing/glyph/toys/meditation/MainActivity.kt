package com.nothing.glyph.toys.meditation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var playButton: Button
    private lateinit var resetButton: Button
    private lateinit var progressSlider: SeekBar
    private lateinit var statusText: TextView
    private lateinit var phaseName: TextView
    private lateinit var phaseDescription: TextView
    private lateinit var durationButtons: List<Button>

    private var selectedDurationMs = AniccaService.DEFAULT_DURATION_MS
    private var sessionPlaying = false
    private var sessionProgress = 0f
    private var userScrubbing = false
    private var syncingSlider = false

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != AniccaService.ACTION_STATE) return

            sessionPlaying = intent.getBooleanExtra(AniccaService.EXTRA_IS_PLAYING, false)
            sessionProgress = intent.getFloatExtra(AniccaService.EXTRA_PROGRESS, 0f).coerceIn(0f, 1f)
            selectedDurationMs = intent.getLongExtra(
                AniccaService.EXTRA_DURATION,
                selectedDurationMs
            ).coerceAtLeast(AniccaService.MIN_DURATION_MS)

            renderUi()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playButton = findViewById(R.id.playBtn)
        resetButton = findViewById(R.id.resetBtn)
        progressSlider = findViewById(R.id.progSlider)
        statusText = findViewById(R.id.statusText)
        phaseName = findViewById(R.id.pname)
        phaseDescription = findViewById(R.id.pdesc)

        durationButtons = listOf(
            findViewById(R.id.btn10),
            findViewById(R.id.btn30),
            findViewById(R.id.btn45),
            findViewById(R.id.btn60)
        )

        bindControls()
        renderUi()
        requestServiceState()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(AniccaService.ACTION_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(stateReceiver, filter)
        }
        requestServiceState()
    }

    override fun onStop() {
        unregisterReceiver(stateReceiver)
        super.onStop()
    }

    private fun bindControls() {
        playButton.setOnClickListener {
            if (sessionPlaying) {
                sendServiceAction(AniccaService.COMMAND_PAUSE)
            } else {
                sendServiceAction(AniccaService.COMMAND_START) {
                    putExtra(AniccaService.EXTRA_DURATION, selectedDurationMs)
                }
            }
        }

        resetButton.setOnClickListener {
            sendServiceAction(AniccaService.COMMAND_RESET) {
                putExtra(AniccaService.EXTRA_DURATION, selectedDurationMs)
            }
        }

        progressSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (syncingSlider) return

                val normalizedProgress = progress / progressSlider.max.toFloat()
                if (fromUser) {
                    sessionProgress = normalizedProgress
                    renderLabels(sessionProgress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                userScrubbing = true
                if (sessionPlaying) {
                    sendServiceAction(AniccaService.COMMAND_PAUSE)
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                userScrubbing = false
                val normalizedProgress = (seekBar?.progress ?: 0) / progressSlider.max.toFloat()
                sendServiceAction(AniccaService.COMMAND_PREVIEW) {
                    putExtra(AniccaService.EXTRA_DURATION, selectedDurationMs)
                    putExtra(AniccaService.EXTRA_PROGRESS, normalizedProgress)
                }
            }
        })

        val durations = listOf(10L, 30L, 45L, 60L)
        durationButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                val durationMs = durations[index] * 60_000L
                if (durationMs == selectedDurationMs) return@setOnClickListener

                selectedDurationMs = durationMs
                sessionProgress = 0f
                sendServiceAction(AniccaService.COMMAND_RESET) {
                    putExtra(AniccaService.EXTRA_DURATION, selectedDurationMs)
                }
                renderUi()
            }
        }
    }

    private fun renderUi() {
        playButton.text = if (sessionPlaying) "Pause" else "Start session"
        updateDurationButtons()
        renderLabels(sessionProgress)

        if (!userScrubbing) {
            syncingSlider = true
            progressSlider.progress = (sessionProgress * progressSlider.max).toInt()
            syncingSlider = false
        }
    }

    private fun renderLabels(progress: Float) {
        val totalMinutes = selectedDurationMs / 60_000L
        val elapsedMinutes = (progress * totalMinutes).toInt().coerceIn(0, totalMinutes.toInt())
        statusText.text = "$elapsedMinutes / $totalMinutes min"

        when {
            progress < 0.42f -> {
                phaseName.text = "Phase 1 - Settling"
                phaseDescription.text = "Scattered sensation softens and moves toward center."
            }
            progress < 0.76f -> {
                phaseName.text = "Phase 2 - Deepening"
                phaseDescription.text = "Ripples thin out as attention becomes quieter and steadier."
            }
            else -> {
                phaseName.text = "Phase 3 - Still"
                phaseDescription.text = "A calm core remains, with only a faint breath at the edge."
            }
        }
    }

    private fun updateDurationButtons() {
        val selectedMinutes = (selectedDurationMs / 60_000L).toInt()
        val durations = listOf(10, 30, 45, 60)
        durationButtons.forEachIndexed { index, button ->
            button.alpha = if (durations[index] == selectedMinutes) 1f else 0.5f
        }
    }

    private fun requestServiceState() {
        sendServiceAction(AniccaService.COMMAND_QUERY_STATE)
    }

    private fun sendServiceAction(
        command: String,
        extras: (Intent.() -> Unit)? = null
    ) {
        val intent = Intent(this, AniccaService::class.java).apply {
            putExtra(AniccaService.EXTRA_COMMAND, command)
            extras?.invoke(this)
        }
        startService(intent)
    }
}
