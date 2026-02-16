package com.example.ledgerscanner.feature.scanner.exam.data.repository

import android.content.Context
import com.example.ledgerscanner.base.network.OperationResult
import com.example.ledgerscanner.base.utils.AssetUtils
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import javax.inject.Inject

class TemplateSelectionRepository @Inject constructor(private val context: Context) {
    suspend fun loadTemplatesFromAssets(): Result<List<Template>> {
        return try {
            val names = AssetUtils.listJsonAssets(context)
            val list = mutableListOf<Template>()
            for (name in names) {
                when (val res = Template.loadOmrTemplateSafe(context, name)) {
                    is OperationResult.Success -> res.data?.let { list.add(it) }
                    is OperationResult.Error -> {
                    }
                }
            }
            if (list.isEmpty()) Result.failure(Exception("No templates found"))
            else Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}