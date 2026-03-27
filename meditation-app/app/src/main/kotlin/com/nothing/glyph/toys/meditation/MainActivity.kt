package com.nothing.glyph.toys.meditation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var selectedDurationMs = 30 * 60 * 1000L
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val playBtn = findViewById<Button>(R.id.playBtn)
        val resetBtn = findViewById<Button>(R.id.resetBtn)
        val statusText = findViewById<TextView>(R.id.statusText)
        val progSlider = findViewById<SeekBar>(R.id.progSlider)
        val pname = findViewById<TextView>(R.id.pname)
        val pdesc = findViewById<TextView>(R.id.pdesc)

        playBtn.setOnClickListener {
            if (isPlaying) {
                stopMeditation()
                playBtn.text = "Play session"
            } else {
                startMeditation()
                playBtn.text = "Pause"
            }
            isPlaying = !isPlaying
        }

        resetBtn.setOnClickListener {
            resetMeditation()
            playBtn.text = "Play session"
            isPlaying = false
            progSlider.progress = 0
        }

        progSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val p = progress / 100f
                    updateLabels(p, pname, pdesc, statusText)
                    previewMeditation(p)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if (isPlaying) {
                    stopMeditation()
                    playBtn.text = "Play session"
                    isPlaying = false
                }
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        setupDurationButtons()
    }

    private fun updateLabels(p: Float, pname: TextView, pdesc: TextView, statusText: TextView) {
        val mins = (p * (selectedDurationMs / 60000)).toInt()
        statusText.text = "$mins / ${selectedDurationMs / 60000} min"
        
        when {
            p < 0.42f -> {
                pname.text = "Phase 1 · Settling"
                pdesc.text = "Sankharas rising and passing on the surface mind"
            }
            p < 0.76f -> {
                pname.text = "Phase 2 · Deepening"
                pdesc.text = "Stillness emerging — sensation subtilizing toward center"
            }
            else -> {
                pname.text = "Phase 3 · Still"
                pdesc.text = "The untouched mind — pure equanimous awareness"
            }
        }
    }

    private fun setupDurationButtons() {
        val btn10 = findViewById<Button>(R.id.btn10)
        val btn30 = findViewById<Button>(R.id.btn30)
        val btn45 = findViewById<Button>(R.id.btn45)
        val btn60 = findViewById<Button>(R.id.btn60)

        val btns = listOf(btn10, btn30, btn45, btn60)
        val durations = listOf(10, 30, 45, 60)

        btns.forEachIndexed { index, button ->
            button.setOnClickListener {
                selectedDurationMs = durations[index] * 60 * 1000L
                btns.forEach { it.alpha = 0.5f }
                button.alpha = 1.0f
                findViewById<TextView>(R.id.statusText).text = "0 / ${durations[index]} min"
            }
        }
    }

    private fun startMeditation() {
        val intent = Intent(this, AniccaService::class.java).apply {
            putExtra("ACTION", "START")
            putExtra("DURATION", selectedDurationMs)
        }
        startService(intent)
    }

    private fun previewMeditation(progress: Float) {
        val intent = Intent(this, AniccaService::class.java).apply {
            putExtra("ACTION", "PREVIEW")
            putExtra("PROGRESS", progress)
        }
        startService(intent)
    }

    private fun stopMeditation() {
        val intent = Intent(this, AniccaService::class.java).apply {
            putExtra("ACTION", "STOP")
        }
        startService(intent)
    }

    private fun resetMeditation() {
        val intent = Intent(this, AniccaService::class.java).apply {
            putExtra("ACTION", "RESET")
        }
        startService(intent)
    }
}
