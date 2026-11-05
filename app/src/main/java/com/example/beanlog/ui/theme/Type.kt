package com.example.beanlog.ui.theme

import com.example.beanlog.R
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val Inter = GoogleFont("Inter")
private val InterFontFamily = FontFamily(
    Font(googleFont = Inter, fontProvider = googleFontProvider, weight = FontWeight.W400),
    Font(googleFont = Inter, fontProvider = googleFontProvider, weight = FontWeight.W500),
    Font(googleFont = Inter, fontProvider = googleFontProvider, weight = FontWeight.W600),
    Font(googleFont = Inter, fontProvider = googleFontProvider, weight = FontWeight.W700)
)

val Typography = Typography(defaultFontFamily = InterFontFamily)
