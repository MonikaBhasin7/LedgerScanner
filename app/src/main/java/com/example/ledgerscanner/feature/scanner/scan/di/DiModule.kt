package com.example.ledgerscanner.feature.scanner.scan.di

import android.content.Context
import com.example.ledgerscanner.database.dao.ScanResultDao
import com.example.ledgerscanner.feature.scanner.scan.repo.ScanResultRepository
import com.example.ledgerscanner.feature.scanner.scan.utils.AnswerEvaluator
import com.example.ledgerscanner.feature.scanner.scan.utils.OmrProcessor
import com.example.ledgerscanner.feature.scanner.scan.utils.TemplateProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(ActivityComponent::class)
class ActivityDiModule {

    @Provides
    fun provideOmrProcessor(): OmrProcessor = OmrProcessor()

    @Provides
    fun provideTemplateProcessor(): TemplateProcessor = TemplateProcessor()

    @Provides
    fun provideAnswerEvaluator(): AnswerEvaluator = AnswerEvaluator()

    @Provides
    fun provideExamRepository(
        dao: ScanResultDao,
        @ApplicationContext context: Context
    ): ScanResultRepository =
        ScanResultRepository(dao, context)
}