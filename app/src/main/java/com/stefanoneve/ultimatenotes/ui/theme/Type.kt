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

/** Fantasy/epic serif (Cinzel). */
val CinzelFamily = FontFamily(
    Font(R.font.cinzel_regular, FontWeight.Normal),
    Font(R.font.cinzel_regular, FontWeight.Medium),
    Font(R.font.cinzel_bold, FontWeight.SemiBold),
    Font(R.font.cinzel_bold, FontWeight.Bold),
)

/** Pixel/terminal font (VT323). */
val PixelFamily = FontFamily(
    Font(R.font.vt323_regular, FontWeight.Normal),
    Font(R.font.vt323_regular, FontWeight.Medium),
    Font(R.font.vt323_regular, FontWeight.SemiBold),
    Font(R.font.vt323_regular, FontWeight.Bold),
)

/** Handwritten font (Caveat). */
val HandFamily = FontFamily(
    Font(R.font.caveat_regular, FontWeight.Normal),
    Font(R.font.caveat_regular, FontWeight.Medium),
    Font(R.font.caveat_bold, FontWeight.SemiBold),
    Font(R.font.caveat_bold, FontWeight.Bold),
)

/**
 * Builds the Material typography from a theme's font pairing:
 * display/headline/title use the display font, body/label the body font.
 */
fun themeTypography(display: FontFamily, body: FontFamily): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 48.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 40.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 34.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 30.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 26.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 23.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 16.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 14.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 14.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
    ),
)

/** Default typography (Lora + Nunito), used as a fallback. */
val AppTypography = themeTypography(SerifFamily, SansFamily)
