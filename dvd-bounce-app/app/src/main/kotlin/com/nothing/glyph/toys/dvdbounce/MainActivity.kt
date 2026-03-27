package com.nothing.glyph.toys.dvdbounce

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.content.ComponentName
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val creditsText = findViewById<TextView>(R.id.creditsText)
        creditsText.text = Html.fromHtml(
            "Designed and Developed by <a href='https://github.com/omesh7'>Omesh</a>",
            Html.FROM_HTML_MODE_LEGACY
        )
        creditsText.movementMethod = LinkMovementMethod.getInstance()

        findViewById<Button>(R.id.btn_open_glyph_tools).setOnClickListener {
            val activityName = if (com.nothing.ketchum.Common.is25111p()) {
                "com.nothing.thirdparty.matrix.toys.manager.AodToySelectActivity"
            } else {
                "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity"
            }
            try {
                val intent = Intent().apply {
                    component = ComponentName(
                        "com.nothing.thirdparty",
                        activityName
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to open Glyph Toys: ${e.message}")
                try {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                } catch (e2: Exception) {
                    Toast.makeText(this, "Could not open settings", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
