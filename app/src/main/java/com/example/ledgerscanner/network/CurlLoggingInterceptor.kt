package com.example.ledgerscanner.network

import android.util.Log
import okhttp3.Interceptor
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
            val buffer = Buffer()
            body.writeTo(buffer)
            val charset = body.contentType()?.charset(Charset.forName("UTF-8")) ?: Charsets.UTF_8
            val bodyString = buffer.readString(charset)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "")
            curl.append("--data-raw \"").append(bodyString).append("\" ")
        }

        curl.append("\"").append(url).append("\"")
        return curl.toString()
    }
}
