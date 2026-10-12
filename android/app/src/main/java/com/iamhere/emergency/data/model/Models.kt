package com.iamhere.emergency.data.model

import com.google.gson.annotations.SerializedName

enum class DeliveryState {
    @SerializedName("QUEUED") QUEUED,
    @SerializedName("SENT") SENT,
    @SerializedName("DELIVERED") DELIVERED,
    @SerializedName("DELIVERY_UNAVAILABLE") DELIVERY_UNAVAILABLE,
    @SerializedName("FAILED") FAILED
}

data class Contact(
    val id: String,
    val name: String,
    val phone: String,
    val relationship: String,
    val isPrimary: Boolean = true
)

data class ContactAlertStatus(
    val contactId: String,
    val name: String,
    val phone: String,
    val relationship: String,
    var deliveryState: DeliveryState = DeliveryState.QUEUED,
    var deliveryEvidence: String? = null,
    var sentAt: Long? = null,
    var deliveredAt: Long? = null,
    var seen: Boolean = false,
    var seenAt: Long? = null,
    var responding: Boolean = false,
    var respondingAt: Long? = null,
    var responseNote: String? = null
)

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val speed: Float? = null,
    val heading: Float? = null,
    val altitude: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val batteryLevel: Int? = null,
    val provider: String = "fused"
)

data class SosSession(
    val id: String,
    val token: String,
    val userId: String,
    val userName: String,
    val userPhone: String,
    val medicalNotes: String?,
    val bloodGroup: String?,
    val status: String,
    val startedAt: Long,
    val endedAt: Long?,
    val lastLocation: LocationPoint?,
    val locations: List<LocationPoint> = emptyList(),
    val contacts: List<ContactAlertStatus> = emptyList(),
    val batteryLevel: Int?,
    val isTestMode: Boolean = false
)

data class StartSosResponse(
    val success: Boolean,
    val session: SosSession,
    val liveUrl: String
)

data class LocationUpdateResponse(
    val success: Boolean,
    val lastLocation: LocationPoint?,
    val batteryLevel: Int?
)

data class ContactStatusUpdateResponse(
    val success: Boolean,
    val contacts: List<ContactAlertStatus>
)

data class StopSosResponse(
    val success: Boolean,
    val status: String,
    val message: String
)

data class AppVersionInfo(
    val success: Boolean = true,
    val versionCode: Long = 1,
    val versionName: String = "1.0.0",
    val minSupportedVersionCode: Long = 1,
    val apkUrl: String = "/api/app/latest.apk",
    val fileSizeBytes: Long = 0,
    val releaseNotes: String = "",
    val publishedAt: String = ""
)

