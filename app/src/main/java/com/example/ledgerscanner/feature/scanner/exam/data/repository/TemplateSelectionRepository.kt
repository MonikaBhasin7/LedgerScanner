package com.example.ledgerscanner.feature.scanner.exam.data.repository

import com.example.ledgerscanner.base.network.OperationResult
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TemplateSelectionRepository @Inject constructor(
    private val templateCatalogRepository: TemplateCatalogRepository
) {
    fun observeTemplates(): Flow<List<Template>> = templateCatalogRepository.observeTemplates()

    suspend fun refreshTemplates(): OperationResult<Unit> {
        return templateCatalogRepository.syncTemplatesFromServer()
    }
}
