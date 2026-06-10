package com.stefanoneve.ultimatenotes.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.stefanoneve.ultimatenotes.R

/** Serif display family (Lora): headers, titles, anything expressive. */
val SerifFamily = FontFamily(
    Font(R.font.lora_medium, FontWeight.Medium),
    Font(R.font.lora_medium, FontWeight.Normal),
    Font(R.font.lora_semibold, FontWeight.SemiBold),
    Font(R.font.lora_bold, FontWeight.Bold),
    Font(R.font.lora_italic, FontWeight.Medium, FontStyle.Italic),
)

/** Rounded sans family (Nunito): body text, labels, UI chrome. */
val SansFamily = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.Medium),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = SerifFamily, fontWeight = FontWeight.Bold, fontSize = 48.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = SerifFamily, fontWeight = FontWeight.Bold, fontSize = 40.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = SerifFamily, fontWeight = FontWeight.Bold, fontSize = 34.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = SerifFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = SerifFamily, fontWeight = FontWeight.SemiBold, fontSize = 26.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = SerifFamily, fontWeight = FontWeight.SemiBold, fontSize = 23.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = SerifFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = SansFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = SansFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = SansFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = SansFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = SansFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = SansFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
    ),
)
