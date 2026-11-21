package com.conduit.msme.data.remote

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "http://10.0.2.2:8000/v1/"
    private const val CLIENT_HEADER_NAME = "X-Client-App"
    private const val CLIENT_HEADER_VALUE = "Conduit-Android"

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val clientHeaderInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestWithHeaders = original.newBuilder()
            .header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE)
            .header("Accept", "application/json")
            .build()
        chain.proceed(requestWithHeaders)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(clientHeaderInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val apiService: ConduitApiService = retrofit.create(ConduitApiService::class.java)
}
