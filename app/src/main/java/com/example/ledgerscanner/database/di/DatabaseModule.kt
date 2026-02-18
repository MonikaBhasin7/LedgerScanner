package com.example.ledgerscanner.database.di

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
object DatabaseModule {

    private const val DB_NAME = "ledger_scanner_db"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DB_NAME
        ).addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3
        )
            .build()
    }

    @Provides
    fun provideExamDao(db: AppDatabase): ExamDao = db.examDao()

    @Provides
    fun provideScanResultDao(db: AppDatabase): ScanResultDao = db.scanResultDao()
}
