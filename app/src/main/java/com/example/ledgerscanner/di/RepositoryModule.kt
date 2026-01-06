package com.example.ledgerscanner.di

import android.content.Context
import com.example.ledgerscanner.database.dao.ExamDao
import com.example.ledgerscanner.database.dao.ScanResultDao
import com.example.ledgerscanner.feature.scanner.exam.repo.ExamRepository
import com.example.ledgerscanner.feature.scanner.exam.repo.TemplateSelectionRepository
import com.example.ledgerscanner.feature.scanner.results.repo.ScanResultRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides singleton repository instances for the entire application.
 *
 * Repositories are singletons because they:
 * - Wrap database DAOs (which are singletons)
 * - Provide Flows that should be shared across the app
 * - Maintain single source of truth for data
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideExamRepository(dao: ExamDao): ExamRepository =
        ExamRepository(dao)

    @Provides
    @Singleton
    fun provideScanResultRepository(
        dao: ScanResultDao,
        @ApplicationContext context: Context
    ): ScanResultRepository =
        ScanResultRepository(dao, context)

    @Provides
    @Singleton
    fun provideTemplateSelectionRepository(
        @ApplicationContext context: Context
    ): TemplateSelectionRepository =
        TemplateSelectionRepository(context)
}