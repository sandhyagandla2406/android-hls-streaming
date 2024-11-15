package com.canadore.hlsstreaming.network

import com.canadore.hlsstreaming.BuildConfig
import com.canadore.hlsstreaming.model.HealthResponse
import com.canadore.hlsstreaming.model.StreamItem
import com.canadore.hlsstreaming.model.StreamListResponse
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ─── Retrofit API Interface ───────────────────────────────────────────────────

interface StreamingApiService {

    /** List all available streams from the catalog. */
    @GET("api/streams")
    suspend fun getStreams(
        @Query("page")  page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<StreamListResponse>

    /** Fetch a single stream's metadata by ID. */
    @GET("api/streams/{id}")
    suspend fun getStream(
        @Path("id") id: String
    ): Response<StreamItem>

    /** Backend health check. */
    @GET("health")
    suspend fun health(): Response<HealthResponse>
}

// ─── Network Client Builder ───────────────────────────────────────────────────

object NetworkConfig {

    /** Base URL — resolves 10.0.2.2 to host machine when using the Android emulator. */
    val BASE_URL: String = BuildConfig.BASE_URL

    fun buildOkHttpClient(
        bandwidthLimitBps: Long = 0L,   // 0 = no throttle
        latencyMs: Int = 0
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BASIC
            else
                HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(NetworkThrottleInterceptor(bandwidthLimitBps, latencyMs))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun buildRetrofit(client: OkHttpClient = buildOkHttpClient()): Retrofit {
        val gson = GsonBuilder().setLenient().create()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun buildApiService(client: OkHttpClient = buildOkHttpClient()): StreamingApiService =
        buildRetrofit(client).create(StreamingApiService::class.java)
}
