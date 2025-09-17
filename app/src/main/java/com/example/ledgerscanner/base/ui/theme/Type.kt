package com.example.ledgerscanner.base.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.Font
import com.example.ledgerscanner.R

val Inter = FontFamily(
    Font(R.font.inter, weight = FontWeight.Thin),
    Font(R.font.inter, weight = FontWeight.ExtraLight),
    Font(R.font.inter, weight = FontWeight.Light),
    Font(R.font.inter, weight = FontWeight.Normal),
    Font(R.font.inter, weight = FontWeight.Medium),
    Font(R.font.inter, weight = FontWeight.SemiBold),
    Font(R.font.inter, weight = FontWeight.Bold),
    Font(R.font.inter, weight = FontWeight.ExtraBold),
    Font(R.font.inter, weight = FontWeight.Black),
)


val Typography = Typography(
    // Display
    displayLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 80.sp,
        lineHeight = 82.sp,
        letterSpacing = (-2.4).sp
    ),
    displayMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 80.sp,
        lineHeight = 82.sp,
        letterSpacing = (-2.4).sp
    ),
    displaySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 64.sp,
        lineHeight = 70.sp,
        letterSpacing = (-1.92).sp
    ),

    // Titles
    titleLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 64.sp,
        lineHeight = 70.sp,
        letterSpacing = (-1.92).sp
    ),
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 40.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.8).sp
    ),
    titleSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 40.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.8).sp
    ),

    // Headings (H1, H2, H3, H4)
    headlineLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.56).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.26).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.24).sp
    ),

    // Body
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 18.sp,
        lineHeight = 28.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),

    // Labels
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.18).sp
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.16).sp
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 16.sp,
        letterSpacing = (-0.14).sp
    )
)