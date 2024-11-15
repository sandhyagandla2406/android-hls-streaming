package com.canadore.hlsstreaming.network

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.Okio
import java.io.IOException

/**
 * OkHttp interceptor that simulates bandwidth throttling and artificial latency.
 * Used to test ExoPlayer's adaptive bitrate (ABR) behaviour under different
 * network profiles (Wi-Fi, 4G LTE, 3G, Poor).
 *
 * @param maxBytesPerSecond  0 = no throttle
 * @param artificialLatencyMs  extra delay added before the response body is consumed
 */
class NetworkThrottleInterceptor(
    private val maxBytesPerSecond: Long = 0L,
    private val artificialLatencyMs: Int = 0
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (artificialLatencyMs > 0) {
            Thread.sleep(artificialLatencyMs.toLong())
        }

        if (maxBytesPerSecond <= 0L) return response

        val originalBody = response.body ?: return response
        val throttledBody = ThrottledResponseBody(originalBody, maxBytesPerSecond)
        return response.newBuilder().body(throttledBody).build()
    }
}

/**
 * Wraps a ResponseBody and throttles the read speed to [maxBytesPerSecond].
 */
private class ThrottledResponseBody(
    private val delegate: okhttp3.ResponseBody,
    private val maxBytesPerSecond: Long
) : okhttp3.ResponseBody() {

    private val buffer = Buffer()

    override fun contentType() = delegate.contentType()
    override fun contentLength() = delegate.contentLength()

    override fun source(): BufferedSource {
        return object : okio.ForwardingSource(delegate.source()) {
            private var bytesRead = 0L
            private var windowStart = System.nanoTime()

            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesReadNow = super.read(sink, byteCount)
                if (bytesReadNow == -1L) return -1L

                bytesRead += bytesReadNow
                val elapsed = System.nanoTime() - windowStart
                val expectedNs = (bytesRead.toDouble() / maxBytesPerSecond * 1_000_000_000).toLong()
                val sleepNs = expectedNs - elapsed
                if (sleepNs > 0) Thread.sleep(sleepNs / 1_000_000, (sleepNs % 1_000_000).toInt())

                return bytesReadNow
            }
        }.buffer()
    }
}
