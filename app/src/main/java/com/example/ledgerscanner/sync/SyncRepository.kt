package com.example.ledgerscanner.sync

import android.util.Log
import com.example.ledgerscanner.auth.TokenStore
import com.example.ledgerscanner.database.dao.ExamDao
import com.example.ledgerscanner.database.dao.ScanResultDao
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.network.SyncApi
import com.example.ledgerscanner.network.model.ExamSyncRequest
import com.example.ledgerscanner.network.model.ScanResultSyncRequest
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val syncApi: SyncApi,
    private val examDao: ExamDao,
    private val scanResultDao: ScanResultDao,
    private val tokenStore: TokenStore
) {
    private val gson = Gson()

    suspend fun syncExams(): Boolean {
        val instituteId = tokenStore.getInstituteId()
        val memberId = tokenStore.getMemberId()
        if (instituteId == null || memberId == null) {
            Log.w(TAG, "Missing member/institute for exam sync")
            return false
        }

        val unsyncedExams = examDao.getUnsyncedExams()
        if (unsyncedExams.isEmpty()) return true

        return try {
            val requests = unsyncedExams.map { ExamSyncRequest.from(it, instituteId, memberId) }
            val responses = syncApi.syncExams(requests)

            responses.forEach { response ->
                val localId = response.localId.toIntOrNull()
                if (localId != null) {
                    examDao.markExamSynced(localId)
                }
            }

            Log.d(TAG, "Synced ${responses.size} exams")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync exams", e)
            false
        }
    }

    suspend fun syncScanResults(): Boolean {
        val instituteId = tokenStore.getInstituteId()
        val memberId = tokenStore.getMemberId()
        if (instituteId == null || memberId == null) {
            Log.w(TAG, "Missing member/institute for scan result sync")
            return false
        }

        val unsyncedResults = scanResultDao.getUnsyncedScanResults()
        if (unsyncedResults.isEmpty()) return true

        return try {
            val requests = unsyncedResults.map { result ->
                val fileNames = buildFileNames(result)
                ScanResultSyncRequest.from(
                    entity = result,
                    instituteId = instituteId,
                    memberId = memberId,
                    clickedRawImageFile = fileNames.clickedRaw,
                    scannedImageFile = fileNames.scanned,
                    thumbnailFile = fileNames.thumb,
                    debugImages = fileNames.debug
                )
            }

            val jsonData = gson.toJson(requests)
            val dataBody = jsonData.toRequestBody("application/json".toMediaTypeOrNull())

            val imageParts = buildImageParts(unsyncedResults)
            val responses = syncApi.syncScanResults(dataBody, imageParts)

            responses.forEach { response ->
                val localId = response.localId.toIntOrNull()
                if (localId != null) {
                    scanResultDao.markScanResultSynced(localId)
                }
            }

            Log.d(TAG, "Synced ${responses.size} scan results")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync scan results", e)
            false
        }
    }

    private data class FileNames(
        val clickedRaw: String,
        val scanned: String,
        val thumb: String?,
        val debug: Map<String, String>?
    )

    private fun buildFileNames(result: ScanResultEntity): FileNames {
        val base = "scan_${result.id}"
        val clickedRaw = fileNameWithExt(result.clickedRawImagePath, "${base}_raw")
        val scanned = fileNameWithExt(result.scannedImagePath, "${base}_scanned")
        val thumb = result.thumbnailPath?.let { fileNameWithExt(it, "${base}_thumb") }

        val debugMap = result.debugImagesPath?.map { (key, path) ->
            val name = fileNameWithExt(path, "${base}_debug_${key}")
            key to name
        }?.toMap()

        return FileNames(clickedRaw, scanned, thumb, debugMap)
    }

    private fun fileNameWithExt(path: String, baseName: String): String {
        val extension = File(path).extension.ifEmpty { "jpg" }
        return "$baseName.$extension"
    }

    private fun buildImageParts(results: List<ScanResultEntity>): List<MultipartBody.Part> {
        val parts = mutableListOf<MultipartBody.Part>()

        results.forEach { result ->
            val base = "scan_${result.id}"
            addImagePart(parts, result.clickedRawImagePath, "${base}_raw")
            addImagePart(parts, result.scannedImagePath, "${base}_scanned")
            result.thumbnailPath?.let { path ->
                addImagePart(parts, path, "${base}_thumb")
            }

            result.debugImagesPath?.forEach { (key, path) ->
                addImagePart(parts, path, "${base}_debug_${key}")
            }
        }

        return parts
    }

    private fun addImagePart(
        parts: MutableList<MultipartBody.Part>,
        filePath: String,
        partName: String
    ) {
        val file = File(filePath)
        if (file.exists()) {
            val extension = file.extension.ifEmpty { "jpg" }
            val mediaType = when (extension.lowercase()) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }.toMediaTypeOrNull()

            val requestBody = file.asRequestBody(mediaType)
            parts.add(
                MultipartBody.Part.createFormData(
                    "files",
                    "$partName.$extension",
                    requestBody
                )
            )
        }
    }

    companion object {
        private const val TAG = "SyncRepository"
    }
}
