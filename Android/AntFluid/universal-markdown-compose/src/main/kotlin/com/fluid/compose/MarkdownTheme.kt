package com.fluid.compose

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class MarkdownTheme(
    val textColor: Color = Color(0xFFE0E0E0),
    val headingColor: Color = textColor,
    val linkColor: Color = Color(0xFF6CB4EE),
    val listBulletColor: Color = textColor,
    val codeTextColor: Color = Color(0xFFE0E0E0),
    val codeBgColor: Color = Color(0xFF1E1E1E),
    val codeBorderColor: Color = Color(0xFF3C3C3C),
    val codeLabelColor: Color = Color(0xFF9CDCFE),
    val inlineCodeTextColor: Color = Color(0xFFCE9178),
    val inlineCodeBgColor: Color = Color(0xFF2D2D2D),
    val blockQuoteBorderColor: Color = Color(0xFF4A4A4A),
    val blockQuoteTextColor: Color = Color(0xFFB0B0B0),
    val blockQuoteBgColor: Color = Color.Transparent,
    val tableHeaderBgColor: Color = Color(0xFF252525),
    val tableBodyBgColor: Color = Color(0xFF1E1E1E),
    val tableBorderColor: Color = Color(0xFF3C3C3C),
    val tableHeaderTextColor: Color = Color(0xFFE0E0E0),
    val tableBodyTextColor: Color = Color(0xFFCCCCCC),
    val hrColor: Color = Color(0xFF4A4A4A),
    val checkboxColor: Color = Color(0xFF6CB4EE),
    val strikethroughColor: Color = Color(0xFF999999),
    val bodyStyle: TextStyle = TextStyle(fontSize = 14.sp, color = textColor, textDirection = TextDirection.Content),
    val codeStyle: TextStyle = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = codeTextColor),
    val headingSizes: List<Float> = listOf(24f, 20f, 18f, 16f, 14f, 13f),
    val headingWeight: FontWeight = FontWeight.Bold,
    val blockQuoteBarWidth: Dp = 3.dp,
    val codeCornerRadius: Dp = 8.dp,
    val tableCornerRadius: Dp = 6.dp,
)
