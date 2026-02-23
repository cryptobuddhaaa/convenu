package com.convenu.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.convenu.app.ui.theme.ConvenuGreen
import com.convenu.app.ui.theme.ConvenuPurple
import com.convenu.app.ui.theme.ConvenuYellow
import com.convenu.app.ui.theme.Slate700

@Composable
fun TrustLevelIndicator(level: Int, maxLevel: Int = 5) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(maxLevel) { index ->
            val isFilled = index < level
            val color = when {
                !isFilled -> Slate700
                level <= 2 -> ConvenuYellow
                level <= 3 -> ConvenuPurple
                else -> ConvenuGreen
            }
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
            )
        }
    }
}
