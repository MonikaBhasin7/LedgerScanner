package com.example.ledgerscanner.base.di

import android.content.Context
import androidx.room.Room
import com.example.ledgerscanner.database.AppDatabase
import com.example.ledgerscanner.database.dao.ExamDao
import com.example.ledgerscanner.database.dao.ScanResultDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppDiModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "ledger_scanner_db")
            .build()
    }

    @Provides
    @Singleton
    fun provideExamDao(db: AppDatabase): ExamDao = db.examDao()

    @Provides
    @Singleton
    fun provideScanResultDao(db: AppDatabase): ScanResultDao = db.scanResultDao()
}