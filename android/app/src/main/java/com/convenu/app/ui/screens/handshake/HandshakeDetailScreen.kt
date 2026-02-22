package com.convenu.app.ui.screens.handshake

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.convenu.app.data.model.HandshakeDto
import com.convenu.app.ui.components.HandshakeStatusBadge

@Composable
fun HandshakeDetailScreen(
    handshakeId: String,
    onBack: () -> Unit,
    viewModel: HandshakeViewModel = hiltViewModel(),
) {
    val listState by viewModel.listState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    val handshake = remember(listState.handshakes, handshakeId) {
        listState.handshakes.find { it.id == handshakeId }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Back button
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
            Text(
                text = "Handshake Details",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(Modifier.height(16.dp))

        if (handshake == null) {
            Text(
                text = "Handshake not found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
            return@Column
        }

        // Status
        HandshakeStatusBadge(status = handshake.status)
        Spacer(Modifier.height(16.dp))

        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow(
                    icon = Icons.Filled.Person,
                    label = "Contact",
                    value = handshake.receiverIdentifier,
                )
                handshake.eventTitle?.let {
                    Spacer(Modifier.height(8.dp))
                    DetailRow(
                        icon = Icons.Filled.CalendarToday,
                        label = "Event",
                        value = it,
                    )
                }
                Spacer(Modifier.height(8.dp))
                DetailRow(
                    label = "Created",
                    value = handshake.createdAt.take(10),
                )
                Spacer(Modifier.height(8.dp))
                DetailRow(
                    label = "Expires",
                    value = handshake.expiresAt.take(10),
                )
                if (handshake.pointsAwarded > 0) {
                    Spacer(Modifier.height(8.dp))
                    DetailRow(
                        label = "Points",
                        value = "+${handshake.pointsAwarded}",
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Action buttons based on status
        when (handshake.status) {
            "matched" -> {
                Button(
                    onClick = { viewModel.mintHandshake(handshake.id) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !actionState.isProcessing,
                ) {
                    if (actionState.isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(actionState.currentAction ?: "Processing...")
                    } else {
                        Text("Mint Proof of Handshake")
                    }
                }
            }
        }

        // Action feedback
        actionState.success?.let { success ->
            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Text(
                    text = success,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        actionState.error?.let { error ->
            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
