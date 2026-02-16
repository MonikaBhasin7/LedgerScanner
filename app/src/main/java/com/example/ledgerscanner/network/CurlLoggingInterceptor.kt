package com.example.ledgerscanner.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.nio.charset.Charset

class CurlLoggingInterceptor(
    private val tag: String = "CURL"
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        logMultiline(request.toCurl())
        return chain.proceed(request)
    }


    private fun logMultiline(message: String) {
        val chunkSize = 3000
        val total = (message.length + chunkSize - 1) / chunkSize
        var index = 0
        var part = 1
        while (index < message.length) {
            val end = (index + chunkSize).coerceAtMost(message.length)
            val chunk = message.substring(index, end)
            Log.d(tag, "[${part}/${total}] $chunk")
            index = end
            part++
        }
    }

    private fun Request.toCurl(): String {
        val curl = StringBuilder("curl -X ")
            .append(method)
            .append(" ")

        headers.names().forEach { name ->
            curl.append("-H ")
                .append("\"")
                .append(name)
                .append(": ")
                .append(headers[name])
                .append("\" ")
        }

        body?.let { body ->
            val contentType = body.contentType()
            val contentLength = body.contentLength()

            if (!isTextLike(contentType)) {
                curl.append("--data-binary \"[omitted non-text body")
                if (contentLength >= 0) curl.append(", ").append(contentLength).append(" bytes")
                curl.append("]\" ")
            } else if (contentLength < 0) {
                curl.append("--data-raw \"[omitted body with unknown length]\" ")
            } else if (contentLength > MAX_BODY_LOG_BYTES) {
                curl.append("--data-raw \"[omitted body too large: ")
                    .append(contentLength)
                    .append(" bytes]\" ")
            } else {
                val buffer = Buffer()
                body.writeTo(buffer)
                val charset = contentType?.charset(UTF8) ?: UTF8
                val totalBytes = buffer.size
                val bytesToRead = minOf(totalBytes, MAX_BODY_LOG_BYTES.toLong()).toInt()
                val raw = buffer.readString(bytesToRead.toLong(), charset)
                val escaped = escapeForDoubleQuotedShell(raw)

                curl.append("--data-raw \"").append(escaped)
                if (totalBytes > bytesToRead) {
                    curl.append("...[truncated ")
                        .append(totalBytes - bytesToRead)
                        .append(" bytes]")
                }
                curl.append("\" ")
            }
        }

        curl.append("\"").append(url).append("\"")
        return curl.toString()
    }

    private fun isTextLike(mediaType: MediaType?): Boolean {
        if (mediaType == null) return false
        val type = mediaType.type.lowercase()
        val subtype = mediaType.subtype.lowercase()
        if (type == "text") return true
        if (subtype.contains("json")) return true
        if (subtype.contains("xml")) return true
        if (subtype.contains("x-www-form-urlencoded")) return true
        return false
    }

    private fun escapeForDoubleQuotedShell(input: String): String {
        val out = StringBuilder((input.length * 1.1).toInt())
        input.forEach { ch ->
            when (ch) {
                '\\' -> out.append("\\\\")
                '"' -> out.append("\\\"")
                '\n', '\r' -> out.append(' ')
                else -> out.append(ch)
            }
        }
        return out.toString()
    }

    companion object {
        private const val MAX_BODY_LOG_BYTES = 16_384L
        private val UTF8: Charset = Charsets.UTF_8
    }
}
