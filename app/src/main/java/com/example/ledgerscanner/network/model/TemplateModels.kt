package com.example.ledgerscanner.network.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class TemplateResponseDto(
    @SerializedName(value = "templateId", alternate = ["template_id"]) val templateId: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("version") val version: JsonElement?,
    @SerializedName(value = "updatedAt", alternate = ["updated_at"]) val updatedAt: JsonElement?,
    @SerializedName(value = "imageUrl", alternate = ["image_url"]) val imageUrl: String?,
    @SerializedName(value = "templateJson", alternate = ["template_json"]) val templateJson: JsonElement?
)
