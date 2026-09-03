package dev.omesh.glyphtoys

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.nothing.ketchum.Common

/**
 * Companion screen: says whether this phone has a Glyph Matrix, and gets the user to the toy
 * picker.
 *
 * Deliberately plain framework views. This screen is a few lines of text and one button, and
 * Compose costs ~28MB of dex to draw it. When the toy gallery is real — live previews, per-toy
 * settings, shared-element transitions — Compose earns its place and comes back.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val supported = runCatching { Common.is23112() || Common.is25111p() }.getOrDefault(false)

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundColor(Color.BLACK)
                setPadding(dp(32), dp(32), dp(32), dp(32))
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)

                if (supported) {
                    addView(heading(R.string.hardware_supported))
                    addView(body(R.string.activation_hint))
                    addView(openSettingsButton())
                } else {
                    addView(heading(R.string.hardware_unsupported))
                    addView(body(R.string.hardware_unsupported_body))
                }
            }
        )
    }

    private fun heading(textId: Int) = TextView(this).apply {
        setText(textId)
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        gravity = Gravity.CENTER
    }

    private fun body(textId: Int) = TextView(this).apply {
        setText(textId)
        setTextColor(Color.parseColor("#B3FFFFFF"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            .apply { topMargin = dp(12) }
    }

    private fun openSettingsButton() = Button(this).apply {
        setText(R.string.open_toy_settings)
        layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            .apply { topMargin = dp(28) }
        setOnClickListener { openToySettings() }
    }

    /**
     * An app cannot activate its own toy — selection is always manual, in Nothing's settings.
     * The best we can do is take the user straight there.
     */
    private fun openToySettings() {
        val intent = Intent().setComponent(
            ComponentName(
                "com.nothing.thirdparty",
                "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity",
            )
        )
        runCatching { startActivity(intent) }.onFailure {
            Toast.makeText(this, R.string.toy_settings_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
