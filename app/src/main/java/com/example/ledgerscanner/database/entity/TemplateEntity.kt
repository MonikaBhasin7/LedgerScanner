package com.example.ledgerscanner.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey
    val templateId: String,
    val name: String,
    val version: String,
    val updatedAt: Long,
    val imageUrl: String?,
    val templateJson: String
)
