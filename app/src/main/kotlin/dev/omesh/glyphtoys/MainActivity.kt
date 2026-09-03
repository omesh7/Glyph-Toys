package dev.omesh.glyphtoys

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.nothing.ketchum.Common
import dev.omesh.glyphtoys.media.GlyphNotificationListener

/**
 * Companion screen: says whether this phone has a Glyph Matrix, and gets the user to the two
 * places Android will not let an app go on its own — the toy picker and notification access.
 *
 * Deliberately plain framework views. This screen is a few labels and two buttons, and Compose
 * costs ~28MB of dex to draw it. When the toy gallery is real — live previews, per-toy settings,
 * shared-element transitions — Compose earns its place and comes back.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    /** Permissions are granted in Settings, so re-read them every time we come back. */
    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val supported = runCatching { Common.is23112() || Common.is25111p() }.getOrDefault(false)

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundColor(Color.BLACK)
                setPadding(dp(32), dp(32), dp(32), dp(32))
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)

                if (!supported) {
                    addView(heading(R.string.hardware_unsupported))
                    addView(body(R.string.hardware_unsupported_body))
                    return@apply
                }

                addView(heading(R.string.hardware_supported))
                addView(body(R.string.activation_hint))
                addView(button(R.string.open_toy_settings, topGap = 28) { openToySettings() })

                if (GlyphNotificationListener.isEnabled(this@MainActivity)) {
                    addView(body(R.string.notification_access_granted, topGap = 40))
                } else {
                    addView(body(R.string.notification_access_hint, topGap = 40))
                    addView(button(R.string.grant_notification_access) { openNotificationAccess() })
                }
            }
        )
    }

    /**
     * An app cannot activate its own toy — selection is always manual, in Nothing's settings.
     * The best we can do is take the user straight there.
     */
    private fun openToySettings() = open(
        Intent().setComponent(
            ComponentName(
                "com.nothing.thirdparty",
                "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity",
            )
        ),
        R.string.toy_settings_unavailable,
    )

    private fun openNotificationAccess() = open(
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
        R.string.notification_settings_unavailable,
    )

    private fun open(intent: Intent, failureMessage: Int) {
        runCatching { startActivity(intent) }.onFailure {
            Toast.makeText(this, failureMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun heading(textId: Int) = TextView(this).apply {
        setText(textId)
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        gravity = Gravity.CENTER
    }

    private fun body(textId: Int, topGap: Int = 12) = TextView(this).apply {
        setText(textId)
        setTextColor(Color.parseColor("#B3FFFFFF"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        gravity = Gravity.CENTER
        layoutParams = gap(topGap)
    }

    private fun button(textId: Int, topGap: Int = 16, onClick: () -> Unit) = Button(this).apply {
        setText(textId)
        layoutParams = gap(topGap)
        setOnClickListener { onClick() }
    }

    private fun gap(topGap: Int) =
        LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(topGap) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
