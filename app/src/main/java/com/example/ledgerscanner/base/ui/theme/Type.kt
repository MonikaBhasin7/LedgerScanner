package com.example.ledgerscanner.base.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ledgerscanner.R

object AppTypography {
    // --- Display
    val display1Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 80.sp,
        lineHeight = 82.sp,
        letterSpacing = (-2.4).sp
    )

    val display1Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 80.sp,
        lineHeight = 82.sp,
        letterSpacing = (-2.4).sp
    )

    val display2Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 64.sp,
        lineHeight = 70.sp,
        letterSpacing = (-1.92).sp
    )

    val display2Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 64.sp,
        lineHeight = 70.sp,
        letterSpacing = (-1.92).sp
    )

    // --- Title
    val title1Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 40.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.8).sp
    )

    val title1Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 40.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.8).sp
    )

    val title1ExtraBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W800,
        fontSize = 40.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.8).sp
    )

    val title2Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 32.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.64).sp
    )

    val title2Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 32.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.64).sp
    )

    val title2ExtraBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W800,
        fontSize = 32.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.64).sp
    )

    // --- Headings (H1..H4)
    val h1Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.56).sp
    )

    val h1Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.56).sp
    )

    val h1ExtraBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W800,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.56).sp
    )

    val h2Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.26).sp
    )

    val h2Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.26).sp
    )

    val h2ExtraBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W800,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.26).sp
    )

    val h3Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.24).sp
    )

    val h3Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.24).sp
    )

    val h3ExtraBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W800,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.24).sp
    )

    val h4Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    )

    val h4Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    )

    val h4ExtraBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W800,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    )

    // --- Body
    val body1Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 18.sp,
        lineHeight = 28.sp
    )

    val body1Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 18.sp,
        lineHeight = 28.sp
    )

    val body2Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )

    val body2Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )

    val body3Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    val body3Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    val body4Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    val body4Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    // --- Labels
    val label1Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.18).sp
    )

    val label1Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.18).sp
    )

    val label1SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.18).sp
    )

    val label1Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.18).sp
    )

    val label2Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.16).sp
    )

    val label2Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.16).sp
    )

    val label2SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.16).sp
    )

    val label2Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.16).sp
    )

    val label3Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 16.sp,
        letterSpacing = (-0.14).sp
    )

    val label3Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 16.sp,
        letterSpacing = (-0.14).sp
    )

    val label3SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
        lineHeight = 16.sp,
        letterSpacing = (-0.14).sp
    )

    val label3Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 14.sp,
        lineHeight = 16.sp,
        letterSpacing = (-0.14).sp
    )

    val label4Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = (-0.12).sp
    )

    val label4Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = (-0.12).sp
    )

    val label4SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = (-0.12).sp
    )

    val label4Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = (-0.12).sp
    )

    // --- Tiny
    val tinyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 10.sp,
        lineHeight = 12.sp
    )

    val tinySemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 10.sp,
        lineHeight = 12.sp
    )

    val tinyBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 10.sp,
        lineHeight = 12.sp
    )

    val tinyExtendedMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 1.sp
    )

    val tinyExtendedSemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 1.sp
    )

    val tinyExtendedBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 1.sp
    )

    val tinyUppercaseMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 10.sp,
        lineHeight = 12.sp
    )

    val tinyUppercaseBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 10.sp,
        lineHeight = 12.sp
    )
}

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

@Composable
fun AppMaterialTypography(): Typography = remember {
    Typography(
        displayLarge  = AppTypography.display1Bold,
        displayMedium = AppTypography.display2Bold,
        displaySmall  = AppTypography.title1Bold,

        headlineLarge = AppTypography.h1Bold,
        headlineMedium = AppTypography.h2Bold,
        headlineSmall = AppTypography.h3Bold,

        titleLarge = AppTypography.title1Medium,
        titleMedium = AppTypography.title2Medium,
        titleSmall = AppTypography.h4Medium,

        bodyLarge = AppTypography.body1Regular,
        bodyMedium = AppTypography.body2Regular,
        bodySmall = AppTypography.body3Regular,

        labelLarge = AppTypography.label1Medium,
        labelMedium = AppTypography.label2Medium,
        labelSmall = AppTypography.tinyMedium
    )
}