package com.example.ledgerscanner.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.ledgerscanner.database.dao.ExamDao
import com.example.ledgerscanner.database.dao.ScanResultDao
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.database.entity.ScanResultEntity

@TypeConverters(TypeConverter::class)
@Database(entities = [ExamEntity::class, ScanResultEntity::class], version = 4, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun examDao(): ExamDao
    abstract fun scanResultDao(): ScanResultDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exams ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
                db.execSQL("ALTER TABLE scan_results ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scan_results ADD COLUMN enrollmentNumber TEXT DEFAULT NULL")
            }
        }

    }
}
