package com.convenu.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.convenu.app.ui.theme.ConvenuBlue
import com.convenu.app.ui.theme.ConvenuGreen
import com.convenu.app.ui.theme.ConvenuYellow
import com.convenu.app.ui.theme.Slate400

@Composable
fun HandshakeStatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status) {
        "pending" -> Triple(ConvenuYellow.copy(alpha = 0.2f), ConvenuYellow, "Pending")
        "claimed" -> Triple(ConvenuBlue.copy(alpha = 0.2f), ConvenuBlue, "Claimed")
        "matched" -> Triple(Color(0xFFF97316).copy(alpha = 0.2f), Color(0xFFF97316), "Matched")
        "minted" -> Triple(ConvenuGreen.copy(alpha = 0.2f), ConvenuGreen, "Minted")
        "expired" -> Triple(Slate400.copy(alpha = 0.2f), Slate400, "Expired")
        else -> Triple(Slate400.copy(alpha = 0.2f), Slate400, status.replaceFirstChar { it.uppercase() })
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
        )
    }
}
