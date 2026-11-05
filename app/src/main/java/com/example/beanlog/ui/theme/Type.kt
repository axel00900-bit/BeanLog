package com.example.beanlog.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont

// Load the Inter typeface from Google Fonts and apply it throughout Material typography.
private val Inter = GoogleFont("Inter")
private val InterFontFamily = FontFamily(
    Font(googleFont = Inter, weight = FontWeight.W400),
    Font(googleFont = Inter, weight = FontWeight.W500),
    Font(googleFont = Inter, weight = FontWeight.W600),
    Font(googleFont = Inter, weight = FontWeight.W700)
)

val Typography = Typography(defaultFontFamily = InterFontFamily)
