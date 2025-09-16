package com.example.ledgerscanner.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.ledgerscanner.database.dao.ExamDao
import com.example.ledgerscanner.database.entity.ExamEntity

@Database(entities = [ExamEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun examDao(): ExamDao
}