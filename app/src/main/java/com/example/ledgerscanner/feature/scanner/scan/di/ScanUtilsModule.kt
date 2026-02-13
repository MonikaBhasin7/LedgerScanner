package com.example.ledgerscanner.feature.scanner.scan.di

import com.example.ledgerscanner.feature.scanner.scan.utils.AnchorGeometryValidator
import com.example.ledgerscanner.feature.scanner.scan.utils.AnswerEvaluator
import com.example.ledgerscanner.feature.scanner.scan.utils.BubbleAnalyzer
import com.example.ledgerscanner.feature.scanner.scan.utils.EnrollmentReader
import com.example.ledgerscanner.feature.scanner.scan.utils.FrameStabilityTracker
import com.example.ledgerscanner.feature.scanner.scan.utils.ImageQualityChecker
import com.example.ledgerscanner.feature.scanner.scan.utils.OmrProcessor
import com.example.ledgerscanner.feature.scanner.scan.utils.TemplateProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

/**
 * Provides scanning and OMR processing utilities.
 *
 * Activity-scoped because these are:
 * - Stateless utilities
 * - Only needed during active scanning
 * - Created fresh per scan session
 */
@Module
@InstallIn(ActivityComponent::class)
object ScanProcessingModule {

    @Provides
    fun provideOmrProcessor(): OmrProcessor = OmrProcessor()

    @Provides
    fun provideTemplateProcessor(): TemplateProcessor = TemplateProcessor()

    @Provides
    fun provideAnswerEvaluator(): AnswerEvaluator = AnswerEvaluator()

    @Provides
    fun provideImageQualityChecker(): ImageQualityChecker = ImageQualityChecker()

    @Provides
    fun provideBubbleAnalyzer(): BubbleAnalyzer = BubbleAnalyzer()

    @Provides
    fun provideFrameStabilityTracker(): FrameStabilityTracker = FrameStabilityTracker()

    @Provides
    fun provideAnchorGeometryValidator(): AnchorGeometryValidator = AnchorGeometryValidator()

    @Provides
    fun provideEnrollmentReader(): EnrollmentReader = EnrollmentReader()
}