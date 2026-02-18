package com.example.ledgerscanner.di

import android.content.Context
import com.example.ledgerscanner.auth.AuthRepository
import com.example.ledgerscanner.auth.TokenStore
import com.example.ledgerscanner.database.dao.ExamDao
import com.example.ledgerscanner.database.dao.ScanResultDao
import com.example.ledgerscanner.feature.scanner.exam.data.repository.ExamRepository
import com.example.ledgerscanner.feature.scanner.exam.data.repository.TemplateSelectionRepository
import com.example.ledgerscanner.feature.scanner.results.repo.ScanResultRepository
import com.example.ledgerscanner.network.AuthApi
import com.example.ledgerscanner.sync.SyncManager
import com.google.gson.Gson
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
    fun provideExamRepository(
        dao: ExamDao,
        syncManager: SyncManager
    ): ExamRepository =
        ExamRepository(dao, syncManager)

    @Provides
    @Singleton
    fun provideScanResultRepository(
        scanResultDao: ScanResultDao,
        examDao: ExamDao,
        @ApplicationContext context: Context,
        syncManager: SyncManager
    ): ScanResultRepository =
        ScanResultRepository(scanResultDao, examDao, context, syncManager)

    @Provides
    @Singleton
    fun provideTemplateSelectionRepository(
        @ApplicationContext context: Context
    ): TemplateSelectionRepository =
        TemplateSelectionRepository(context)

    @Provides
    @Singleton
    fun provideAuthRepository(
        authApi: AuthApi,
        tokenStore: TokenStore,
        gson: Gson
    ): AuthRepository =
        AuthRepository(authApi, tokenStore, gson)
}
