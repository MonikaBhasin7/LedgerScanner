package com.example.ledgerscanner.feature.scanner.exam.data.repository

import com.example.ledgerscanner.base.network.OperationResult
import com.example.ledgerscanner.database.dao.TemplateDao
import com.example.ledgerscanner.database.entity.TemplateEntity
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import com.example.ledgerscanner.network.TemplateApi
import com.example.ledgerscanner.network.model.TemplateResponseDto
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateCatalogRepository @Inject constructor(
    private val templateApi: TemplateApi,
    private val templateDao: TemplateDao,
    private val gson: Gson
) {

    fun observeTemplates(): Flow<List<Template>> {
        return templateDao.observeTemplates().map { entities ->
            entities.mapNotNull { entity ->
                runCatching {
                    val parsed = gson.fromJson(entity.templateJson, Template::class.java)
                    parsed.copy(
                        name = entity.name,
                        version = entity.version,
                        imageUrl = normalizeImageUrl(entity.imageUrl ?: parsed.imageUrl)
                    )
                }.getOrNull()
            }
        }
    }

    suspend fun syncTemplatesFromServer(): OperationResult<Unit> {
        return try {
            templateApi.getTemplates()
                .mapNotNull(::toEntityOrNull)
                .forEach { entity ->
                    val existing = templateDao.getById(entity.templateId)
                    if (existing == null) {
                        templateDao.insertIgnore(entity)
                    } else if (shouldRepairTemplateJson(existing.templateJson)) {
                        // One-time repair: replace only clearly broken stored JSON.
                        templateDao.updateIncludingJson(
                            templateId = entity.templateId,
                            name = entity.name,
                            version = entity.version,
                            updatedAt = entity.updatedAt,
                            imageUrl = entity.imageUrl,
                            templateJson = entity.templateJson
                        )
                    } else {
                        templateDao.updateMetadataOnly(
                            templateId = entity.templateId,
                            name = entity.name,
                            version = entity.version,
                            updatedAt = entity.updatedAt,
                            imageUrl = entity.imageUrl
                        )
                    }
                }
            OperationResult.Success(Unit)
        } catch (t: Throwable) {
            OperationResult.Error(
                message = t.message ?: "Failed to sync templates",
                throwable = t
            )
        }
    }

    private fun toEntityOrNull(dto: TemplateResponseDto): TemplateEntity? {
        val templateId = dto.templateId?.trim().orEmpty()
        val name = dto.name?.trim().orEmpty()
        if (templateId.isBlank() || name.isBlank()) return null

        val templateJsonString = dto.templateJson.toTemplateJsonString().orEmpty()
        if (templateJsonString.isBlank()) return null

        // Keep only templates that can actually be parsed by scanner flow.
        runCatching { gson.fromJson(templateJsonString, Template::class.java) }.getOrNull()
            ?: return null

        return TemplateEntity(
            templateId = templateId,
            name = name,
            version = dto.version.toVersionString(),
            updatedAt = dto.updatedAt.toEpochMillis(),
            imageUrl = normalizeImageUrl(dto.imageUrl),
            templateJson = templateJsonString
        )
    }

    private fun JsonElement?.toTemplateJsonString(): String? {
        if (this == null || isJsonNull) return null
        if (isJsonPrimitive && asJsonPrimitive.isString) return asString
        return gson.toJson(this)
    }

    private fun JsonElement?.toVersionString(): String {
        if (this == null || isJsonNull) return "1.0"
        return if (isJsonPrimitive) asJsonPrimitive.toString().trim('"') else toString()
    }

    private fun JsonElement?.toEpochMillis(): Long {
        val now = System.currentTimeMillis()
        if (this == null || isJsonNull) return now
        val raw = if (isJsonPrimitive) asJsonPrimitive.toString().trim('"') else toString()
        raw.toLongOrNull()?.let { return it }
        return runCatching { Instant.parse(raw).toEpochMilli() }.getOrDefault(now)
    }

    private fun shouldRepairTemplateJson(storedJson: String): Boolean {
        if (storedJson.isBlank()) return true
        if (isJacksonNodeMetadataObject(storedJson)) return true
        val parsed = runCatching { gson.fromJson(storedJson, Template::class.java) }.getOrNull()
        return parsed == null || parsed.questions.isEmpty()
    }

    private fun isJacksonNodeMetadataObject(raw: String): Boolean {
        val obj = runCatching { gson.fromJson(raw, JsonObject::class.java) }.getOrNull() ?: return false
        val looksLikeNodeBean =
            obj.has("array") &&
                obj.has("containerNode") &&
                obj.has("nodeType") &&
                obj.has("valueNode")
        val isRealTemplate = obj.has("questions") && obj.has("anchor_top_left")
        return looksLikeNodeBean && !isRealTemplate
    }

    private fun normalizeImageUrl(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return null
        if (value.startsWith("http://") || value.startsWith("https://")) return value
        if (value.startsWith("file:///")) {
            // Convert bad local-file style URL into backend relative path.
            return "/" + value.removePrefix("file:///").trimStart('/')
        }
        if (value.startsWith("template-images/")) return "/$value"
        return value
    }
}
