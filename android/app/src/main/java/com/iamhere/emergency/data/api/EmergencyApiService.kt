package com.iamhere.emergency.data.api

import com.iamhere.emergency.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface EmergencyApiService {

    @POST("api/sos/start")
    suspend fun startSos(@Body body: Map<String, Any?>): Response<StartSosResponse>

    @GET("api/sos/{token}")
    suspend fun getSosStatus(@Path("token") token: String): Response<Map<String, Any>>

    @POST("api/sos/{token}/location")
    suspend fun updateLocation(
        @Path("token") token: String,
        @Body body: LocationPoint
    ): Response<LocationUpdateResponse>

    @POST("api/sos/{token}/locations/batch")
    suspend fun updateLocationsBatch(
        @Path("token") token: String,
        @Body body: Map<String, List<LocationPoint>>
    ): Response<Map<String, Any>>

    @POST("api/sos/{token}/contact-status")
    suspend fun updateContactStatus(
        @Path("token") token: String,
        @Body body: Map<String, String>
    ): Response<ContactStatusUpdateResponse>

    @POST("api/sos/{token}/stop")
    suspend fun stopSos(
        @Path("token") token: String,
        @Body body: Map<String, String>
    ): Response<StopSosResponse>

    @POST("api/auth/phone-login")
    suspend fun phoneLogin(@Body body: Map<String, Any?>): Response<Map<String, Any>>

    @GET("api/app/version")
    suspend fun getAppVersion(): Response<AppVersionInfo>
}

