package com.convenu.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily

@Composable
fun WalletAddressText(
    address: String,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    val displayAddress = if (address.length > 12) {
        "${address.take(6)}...${address.takeLast(6)}"
    } else {
        address
    }

    Text(
        text = displayAddress,
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.clickable {
            clipboardManager.setText(AnnotatedString(address))
        },
    )
}
