package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ComposableSticker(
    name: String,
    modifier: Modifier = Modifier
) {
    // Beautiful vector-like styled sticker container using gradients and layered emojis
    val gradient = when (name) {
        "love_spark" -> Brush.radialGradient(listOf(Color(0xFFFFF0F5), Color(0xFFFFB6C1)))
        "hug_bear" -> Brush.radialGradient(listOf(Color(0xFFFFF8DC), Color(0xFFDEB887)))
        "laugh_heart" -> Brush.linearGradient(listOf(Color(0xFFFFFACD), Color(0xFFFFE4E1)))
        "music_love" -> Brush.linearGradient(listOf(Color(0xFFE6E6FA), Color(0xFFD8BFD8)))
        "sweet_bubble" -> Brush.radialGradient(listOf(Color(0xFFF0FFFF), Color(0xFFAEEEEE)))
        else -> Brush.linearGradient(listOf(Color.White, Color(0xFFF5F5F5)))
    }

    val emoji = when (name) {
        "love_spark" -> "💖✨"
        "hug_bear" -> "🧸🐻"
        "laugh_heart" -> "😂💞"
        "music_love" -> "🎵💘"
        "sweet_bubble" -> "💬🍨"
        else -> "⭐"
    }

    val tagText = when (name) {
        "love_spark" -> "Spark of Love"
        "hug_bear" -> "Warm Bear Hug"
        "laugh_heart" -> "Giggle Hearts"
        "music_love" -> "Sweet Beats"
        "sweet_bubble" -> "Cherry Cream"
        else -> "Sticker"
    }

    val tagColor = when (name) {
        "love_spark" -> Color(0xFFFF1493)
        "hug_bear" -> Color(0xFF8B4513)
        "laugh_heart" -> Color(0xFFD02090)
        "music_love" -> Color(0xFF4B0082)
        "sweet_bubble" -> Color(0xFF008080)
        else -> Color.DarkGray
    }

    Box(
        modifier = modifier
            .width(130.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 44.sp
                )
            }
            Text(
                text = tagText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = tagColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.8f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
