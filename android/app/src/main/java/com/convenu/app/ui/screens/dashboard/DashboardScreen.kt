package com.convenu.app.ui.screens.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.convenu.app.BuildConfig
import com.convenu.app.data.model.HandshakeDto
import com.convenu.app.data.model.PointEntry
import com.convenu.app.data.model.TrustScoreFull
import com.convenu.app.ui.components.HandshakeStatusBadge
import com.convenu.app.ui.components.TrustLevelIndicator
import com.convenu.app.ui.theme.*

@Composable
fun DashboardScreen(
    onNavigateToHandshakes: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onLogout: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        // Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Dashboard", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Row {
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Filled.Refresh, "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { showLogoutConfirm = true }) {
                    Icon(Icons.Filled.Logout, "Logout", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (uiState.isLoading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Trust Score Card
            val trust = uiState.trust
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Shield, null, Modifier.size(24.dp), tint = ConvenuPurple)
                        Spacer(Modifier.width(8.dp))
                        Text("Trust Score", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (trust != null) {
                        TrustLevelIndicator(level = trust.trustLevel)
                        Spacer(Modifier.height(8.dp))
                        Row {
                            Icon(Icons.Filled.Star, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            Text("Level ${trust.trustLevel}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(16.dp))
                            Icon(Icons.Filled.Handshake, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            Text("${trust.totalHandshakes} handshakes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccountBalanceWallet, null, Modifier.size(16.dp), tint = if (trust.walletConnected) ConvenuGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (trust.walletConnected) "Wallet Connected" else "No Wallet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (trust.walletConnected) ConvenuGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (!trust.walletConnected) {
                                Spacer(Modifier.width(8.dp))
                                Text("Connect", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { onNavigateToWallet() })
                            }
                        }

                        // Trust signal breakdown
                        uiState.trustFull?.let { full ->
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(12.dp))
                            Text("Trust Signals", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            TrustSignalRow("Telegram Premium", full.telegramPremium)
                            TrustSignalRow("Profile Photo", full.hasProfilePhoto)
                            TrustSignalRow("Username Set", full.hasUsername)
                            TrustSignalRow("Account Age > 30d", (full.telegramAccountAgeDays ?: 0) > 30)
                            TrustSignalRow("Wallet Connected", full.walletConnected)
                            TrustSignalRow("3+ Handshakes", full.totalHandshakes >= 3)
                        }
                    } else {
                        Text("Unable to load trust score", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Points Card
            if (uiState.totalPoints > 0 || uiState.pointsHistory.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.EmojiEvents, null, Modifier.size(24.dp), tint = ConvenuYellow)
                            Spacer(Modifier.width(8.dp))
                            Text("Points", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.weight(1f))
                            Text("${uiState.totalPoints} total", style = MaterialTheme.typography.titleMedium, color = ConvenuGreen)
                        }
                        if (uiState.pointsHistory.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            uiState.pointsHistory.take(5).forEach { entry ->
                                PointEntryRow(entry)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Recent Handshakes
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent Handshakes", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text("View All", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { onNavigateToHandshakes() })
                    }
                    Spacer(Modifier.height(12.dp))
                    if (uiState.recentHandshakes.isEmpty()) {
                        Text("No handshakes yet. Start by connecting with a contact!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        uiState.recentHandshakes.take(5).forEach { handshake ->
                            HandshakeRow(handshake, context)
                        }
                    }
                }
            }

            // Error
            uiState.error?.let { error ->
                Spacer(Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(error, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }

    // Logout confirmation
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Logout?") },
            text = { Text("You'll need to log in again via Telegram.") },
            confirmButton = {
                TextButton(onClick = { viewModel.logout(); onLogout(); showLogoutConfirm = false }, colors = ButtonDefaults.textButtonColors(contentColor = ConvenuRed)) {
                    Text("Logout")
                }
            },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun TrustSignalRow(label: String, active: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (active) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            null, Modifier.size(16.dp),
            tint = if (active) ConvenuGreen else Slate500,
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PointEntryRow(entry: PointEntry) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Text(entry.createdAt.take(10), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("+${entry.points}", style = MaterialTheme.typography.titleSmall, color = ConvenuGreen)
    }
}

@Composable
private fun HandshakeRow(handshake: HandshakeDto, context: android.content.Context) {
    val cluster = BuildConfig.SOLANA_NETWORK
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                handshake.initiatorName ?: handshake.receiverIdentifier,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            handshake.eventTitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (handshake.pointsAwarded > 0) {
                Text("+${handshake.pointsAwarded} pts", style = MaterialTheme.typography.bodySmall, color = ConvenuGreen)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            HandshakeStatusBadge(status = handshake.status)
            // Solana Explorer link for minted
            if (handshake.status == "minted") {
                val nft = handshake.initiatorNftAddress ?: handshake.receiverNftAddress
                if (nft != null) {
                    val clusterParam = if (cluster != "mainnet-beta") "?cluster=$cluster" else ""
                    Text(
                        "View on Explorer",
                        style = MaterialTheme.typography.labelSmall,
                        color = ConvenuBlue,
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://explorer.solana.com/tx/$nft$clusterParam")))
                        },
                    )
                }
            }
        }
    }
}
