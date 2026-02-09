package com.example.ledgerscanner.network

import com.example.ledgerscanner.network.model.ExamSyncRequest
import com.example.ledgerscanner.network.model.SyncResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface SyncApi {

    @POST("sync/exams")
    suspend fun syncExams(@Body requests: List<ExamSyncRequest>): List<SyncResponse>

    @Multipart
    @POST("sync/scan-results")
    suspend fun syncScanResults(
        @Part("data") data: RequestBody,
        @Part files: List<MultipartBody.Part>
    ): List<SyncResponse>
}
