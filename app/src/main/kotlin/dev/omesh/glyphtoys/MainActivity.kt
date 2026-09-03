package dev.omesh.glyphtoys

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nothing.ketchum.Common

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val supported = runCatching { Common.is23112() || Common.is25111p() }.getOrDefault(false)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Scaffold { padding ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (supported) SupportedContent(::openToySettings) else UnsupportedContent()
                    }
                }
            }
        }
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
}

@Composable
private fun SupportedContent(onOpenSettings: () -> Unit) {
    Text(stringResource(R.string.hardware_supported), style = MaterialTheme.typography.headlineSmall)
    Text(stringResource(R.string.activation_hint), style = MaterialTheme.typography.bodyMedium)
    Button(onClick = onOpenSettings) { Text(stringResource(R.string.open_toy_settings)) }
}

@Composable
private fun UnsupportedContent() {
    Text(stringResource(R.string.hardware_unsupported), style = MaterialTheme.typography.headlineSmall)
    Text(stringResource(R.string.hardware_unsupported_body), style = MaterialTheme.typography.bodyMedium)
}
