package com.iamhere.emergency.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    // Default to 127.0.0.1:4000 when using 'adb reverse tcp:4000 tcp:4000' for USB device,
    // or 10.0.2.2:4000 for standard Android Emulator.
    var baseUrl: String = "http://127.0.0.1:4000/"
        set(value) {
            field = if (!value.endsWith("/")) "$value/" else value
            retrofitInstance = null
        }

    private var retrofitInstance: Retrofit? = null

    private fun getRetrofit(): Retrofit {
        if (retrofitInstance == null) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            retrofitInstance = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofitInstance!!
    }

    val api: EmergencyApiService
        get() = getRetrofit().create(EmergencyApiService::class.java)
}
