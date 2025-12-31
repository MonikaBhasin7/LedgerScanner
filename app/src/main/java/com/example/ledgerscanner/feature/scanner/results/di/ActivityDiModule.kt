package com.example.ledgerscanner.feature.scanner.results.di

import android.content.Context
import com.example.ledgerscanner.database.dao.ScanResultDao
import com.example.ledgerscanner.feature.scanner.results.repo.ScanResultRepository
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
    fun provideScanResultRepository(
        dao: ScanResultDao,
        @ApplicationContext context: Context
    ): ScanResultRepository =
        ScanResultRepository(dao, context)
}