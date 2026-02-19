package com.example.ledgerscanner.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.ledgerscanner.database.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {

    @Query("SELECT * FROM templates ORDER BY updatedAt DESC, name ASC")
    fun observeTemplates(): Flow<List<TemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(template: TemplateEntity): Long

    @Query("SELECT * FROM templates WHERE templateId = :templateId LIMIT 1")
    suspend fun getById(templateId: String): TemplateEntity?

    @Query(
        """
        UPDATE templates
        SET name = :name,
            version = :version,
            updatedAt = :updatedAt,
            imageUrl = :imageUrl
        WHERE templateId = :templateId
        """
    )
    suspend fun updateMetadataOnly(
        templateId: String,
        name: String,
        version: String,
        updatedAt: Long,
        imageUrl: String?
    )

    @Query(
        """
        UPDATE templates
        SET name = :name,
            version = :version,
            updatedAt = :updatedAt,
            imageUrl = :imageUrl,
            templateJson = :templateJson
        WHERE templateId = :templateId
        """
    )
    suspend fun updateIncludingJson(
        templateId: String,
        name: String,
        version: String,
        updatedAt: Long,
        imageUrl: String?,
        templateJson: String
    )
}
