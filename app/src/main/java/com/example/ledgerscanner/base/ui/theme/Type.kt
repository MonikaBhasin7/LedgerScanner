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

    // Add these to AppTypography object

    // --- Body (add missing SemiBold and Bold variants)
    val body1SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 18.sp,
        lineHeight = 28.sp
    )

    val body1Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 18.sp,
        lineHeight = 28.sp
    )

    val body2SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )

    val body2Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )

    val body3SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    val body3Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    val body4SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    val body4Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    // --- Add body5 for smaller text (11sp)
    val body5Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )

    val body5Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )

    val body5SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )

    val body5Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )

    // --- Add label5 for extra small labels (10sp) - different from tiny
    val label5Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = (-0.1).sp
    )

    val label5Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = (-0.1).sp
    )

    val label5SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = (-0.1).sp
    )

    val label5Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = (-0.1).sp
    )
    // Add these to AppTypography object

    // --- 10sp variants (Tiny)
    val text10Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 10.sp,
        lineHeight = 12.sp
    )

    val text10Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 10.sp,
        lineHeight = 12.sp
    )

    val text10SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 10.sp,
        lineHeight = 12.sp
    )

    val text10Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 10.sp,
        lineHeight = 12.sp
    )

    // --- 11sp variants
    val text11Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )

    val text11Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )

    val text11SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )

    val text11Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )

    // --- 12sp variants (already covered by body4 and label4, but adding for consistency)
    val text12Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    val text12Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    val text12SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    val text12Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    // --- 13sp variants
    val text13Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )

    val text13Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )

    val text13SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )

    val text13Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )

    // --- 14sp variants (already covered by body3 and label3, but adding for consistency)
    val text14Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    val text14Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    val text14SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    val text14Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    // --- 15sp variants
    val text15Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 15.sp,
        lineHeight = 20.sp
    )

    val text15Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 15.sp,
        lineHeight = 20.sp
    )

    val text15SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 15.sp,
        lineHeight = 20.sp
    )

    val text15Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 15.sp,
        lineHeight = 20.sp
    )

    // --- 16sp variants (already covered by body2 and label2, but adding for consistency)
    val text16Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )

    val text16Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )

    val text16SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )

    val text16Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )

    // --- 18sp variants (already covered by body1 and label1, but adding for consistency)
    val text18Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 18.sp,
        lineHeight = 28.sp
    )

    val text18Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 18.sp,
        lineHeight = 28.sp
    )

    val text18SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 18.sp,
        lineHeight = 28.sp
    )

    val text18Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 18.sp,
        lineHeight = 28.sp
    )

    // --- 20sp variants (already covered by h4, but adding for consistency)
    val text20Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 20.sp,
        lineHeight = 26.sp
    )

    val text20Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 20.sp,
        lineHeight = 26.sp
    )

    val text20SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 20.sp,
        lineHeight = 26.sp
    )

    val text20Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 20.sp,
        lineHeight = 26.sp
    )

    // --- 22sp variants
    val text22Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.22).sp
    )

    val text22Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.22).sp
    )

    val text22SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.22).sp
    )

    val text22Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.22).sp
    )

    // --- 24sp variants (already covered by h3, but adding for consistency)
    val text24Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.24).sp
    )

    val text24Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.24).sp
    )

    val text24SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.24).sp
    )

    val text24Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.24).sp
    )

    // --- 26sp variants (already covered by h2, but adding for consistency)
    val text26Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.26).sp
    )

    val text26Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.26).sp
    )

    val text26SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.26).sp
    )

    val text26Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.26).sp
    )

    // --- 28sp variants (already covered by h1, but adding for consistency)
    val text28Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.56).sp
    )

    val text28Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.56).sp
    )

    val text28SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.56).sp
    )

    val text28Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.56).sp
    )

    // --- 30sp variants
    val text30Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.6).sp
    )

    val text30Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.6).sp
    )

    val text30SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.6).sp
    )

    val text30Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.6).sp
    )

    // --- 32sp variants (already covered by title2, but adding for consistency)
    val text32Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.64).sp
    )

    val text32Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.64).sp
    )

    val text32SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.64).sp
    )

    val text32Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.64).sp
    )

    // --- 36sp variants
    val text36Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.72).sp
    )

    val text36Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.72).sp
    )

    val text36SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.72).sp
    )

    val text36Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.72).sp
    )

    // --- 40sp variants (already covered by title1, but adding for consistency)
    val text40Regular = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W400,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.8).sp
    )

    val text40Medium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W500,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.8).sp
    )

    val text40SemiBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W600,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.8).sp
    )

    val text40Bold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.W700,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.8).sp
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