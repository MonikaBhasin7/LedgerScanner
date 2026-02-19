package com.example.ledgerscanner.network

import com.example.ledgerscanner.network.model.TemplateResponseDto
import retrofit2.http.GET

interface TemplateApi {
    @GET("templates")
    suspend fun getTemplates(): List<TemplateResponseDto>
}
