package com.iamhere.emergency.safety

import android.content.Context
import android.content.Intent
import android.net.Uri

data class SafePoi(
    val id: String,
    val name: String,
    val category: String, // "POLICE", "HOSPITAL", "WOMEN_HELPLINE", "TRANSIT"
    val address: String,
    val phone: String,
    val approxDistanceKm: Double,
    val latitude: Double,
    val longitude: Double,
    val is24x7: Boolean = true
)

object SafePoiRepository {

    fun getNearbySafePoints(): List<SafePoi> {
        return listOf(
            SafePoi(
                id = "poi_1",
                name = "Central Police Station & Women Help Desk",
                category = "POLICE",
                address = "Connaught Place Sector 1, New Delhi",
                phone = "112",
                approxDistanceKm = 0.8,
                latitude = 28.6315,
                longitude = 77.2167,
                is24x7 = true
            ),
            SafePoi(
                id = "poi_2",
                name = "National Women Helpline (1091 / 181)",
                category = "WOMEN_HELPLINE",
                address = "Emergency Response Support Center",
                phone = "1091",
                approxDistanceKm = 1.2,
                latitude = 28.6250,
                longitude = 77.2190,
                is24x7 = true
            ),
            SafePoi(
                id = "poi_3",
                name = "City General Hospital - 24/7 Trauma Emergency",
                category = "HOSPITAL",
                address = "Ring Road Emergency Wing",
                phone = "102",
                approxDistanceKm = 1.9,
                latitude = 28.6190,
                longitude = 77.2280,
                is24x7 = true
            ),
            SafePoi(
                id = "poi_4",
                name = "Metro Station Police Chowki & Security Post",
                category = "TRANSIT",
                address = "Metro Interchange Level 1, CCTV Monitored",
                phone = "112",
                approxDistanceKm = 0.4,
                latitude = 28.6330,
                longitude = 77.2150,
                is24x7 = true
            )
        )
    }

    fun openNavigation(context: Context, poi: SafePoi) {
        try {
            val uri = Uri.parse("geo:${poi.latitude},${poi.longitude}?q=${Uri.encode(poi.name)}")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
            mapIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dialPhone(context: Context, phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
