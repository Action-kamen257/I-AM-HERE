package com.iamhere.emergency.service

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import com.iamhere.emergency.data.model.DeliveryState
import com.iamhere.emergency.data.repository.EmergencyManager

class SmsStatusReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SMS_SENT = "com.iamhere.emergency.SMS_SENT"
        const val ACTION_SMS_DELIVERED = "com.iamhere.emergency.SMS_DELIVERED"
        const val EXTRA_CONTACT_ID = "contact_id"
        const val EXTRA_PHONE = "phone"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        val contactId = intent.getStringExtra(EXTRA_CONTACT_ID) ?: return

        val emergencyManager = EmergencyManager.getInstance()

        when (action) {
            ACTION_SMS_SENT -> {
                if (resultCode == Activity.RESULT_OK) {
                    emergencyManager.updateContactStatus(
                        contactId = contactId,
                        deliveryState = DeliveryState.SENT,
                        evidence = "CARRIER_SENT_OK"
                    )
                } else {
                    val errorMsg = when (resultCode) {
                        SmsManager.RESULT_ERROR_NO_SERVICE -> "NO_CELLULAR_SERVICE"
                        SmsManager.RESULT_ERROR_RADIO_OFF -> "AIRPLANE_MODE_RADIO_OFF"
                        else -> "GENERIC_FAILURE_CODE_$resultCode"
                    }
                    emergencyManager.updateContactStatus(
                        contactId = contactId,
                        deliveryState = DeliveryState.FAILED,
                        evidence = errorMsg
                    )
                }
            }

            ACTION_SMS_DELIVERED -> {
                if (resultCode == Activity.RESULT_OK) {
                    emergencyManager.updateContactStatus(
                        contactId = contactId,
                        deliveryState = DeliveryState.DELIVERED,
                        evidence = "PDU_DELIVERY_ACK_RECEIVED"
                    )
                } else {
                    emergencyManager.updateContactStatus(
                        contactId = contactId,
                        deliveryState = DeliveryState.DELIVERY_UNAVAILABLE,
                        evidence = "CARRIER_DELIVERY_UNAVAILABLE"
                    )
                }
            }
        }
    }
}
