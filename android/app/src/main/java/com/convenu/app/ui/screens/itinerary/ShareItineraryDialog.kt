package com.convenu.app.ui.screens.itinerary

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.convenu.app.data.model.ItineraryDto

@Composable
fun ShareItineraryDialog(
    itinerary: ItineraryDto,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    // Generate share URL using the itinerary ID
    val shareUrl = "https://app.convenu.xyz/share/${itinerary.id}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Share Itinerary")
                Text(itinerary.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Shareable URL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = shareUrl,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Button(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Share URL", shareUrl))
                        copied = true
                    }) {
                        Icon(if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (copied) "Copied!" else "Copy")
                    }
                }

                // Share via system share sheet
                OutlinedButton(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Check out my itinerary: ${itinerary.title}\n$shareUrl")
                            putExtra(Intent.EXTRA_SUBJECT, itinerary.title)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Itinerary"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share via...")
                }

                val eventCount = itinerary.data.days.sumOf { it.events.size }
                Text(
                    "Sharing ${itinerary.data.days.size} days, $eventCount events",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
