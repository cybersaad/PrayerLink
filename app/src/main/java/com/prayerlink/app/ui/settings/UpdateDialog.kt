package com.prayerlink.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prayerlink.app.data.model.UpdateResult
import com.prayerlink.app.data.model.GitHubRelease

@Composable
fun UpdateDialog(
    currentVersion: String,
    result: UpdateResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    when (result) {
        is UpdateResult.NewUpdateAvailable -> {
            val release = result.release
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Update Available") },
                text = {
                    Column {
                        Text("A new version of PrayerLink is available!")
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Current Version: $currentVersion")
                        Text("Latest Version: ${release.version}", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("What's New:", fontWeight = FontWeight.Bold)
                        Text(
                            text = release.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.releaseUrl))
                        context.startActivity(intent)
                        onDismiss()
                    }) {
                        Text("Update Now")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Later")
                    }
                }
            )
        }
        is UpdateResult.UpToDate -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Up to Date") },
                text = { Text("You are already using the latest version of PrayerLink ($currentVersion).") },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            )
        }
        is UpdateResult.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Update Check Failed") },
                text = { Text(result.message) },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            )
        }
    }
}
