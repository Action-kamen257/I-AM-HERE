package com.iamhere.emergency.data.repository

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.telephony.SmsManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iamhere.emergency.IamHereApplication
import com.iamhere.emergency.data.api.NetworkClient
import com.iamhere.emergency.data.model.*
import com.iamhere.emergency.sensor.ShakeDetector
import com.iamhere.emergency.sensor.ShakeSensitivity
import com.iamhere.emergency.service.EmergencyAudioPlayer
import com.iamhere.emergency.service.EmergencyLocationService
import com.iamhere.emergency.service.SmsStatusReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EmergencyManager private constructor(private val context: Context) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: EmergencyManager? = null

        private const val PREFS_NAME = "iamhere_emergency_prefs"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_PHONE = "key_user_phone"
        private const val KEY_MEDICAL_NOTES = "key_medical_notes"
        private const val KEY_BLOOD_GROUP = "key_blood_group"
        private const val KEY_CANCEL_PIN = "key_cancel_pin"
        private const val KEY_DURESS_PIN = "key_duress_pin"
        private const val KEY_CONTACTS_JSON = "key_contacts_json"
        private const val KEY_SHAKE_ENABLED = "key_shake_enabled"
        private const val KEY_IMPACT_ENABLED = "key_impact_enabled"
        private const val KEY_SHAKE_SENSITIVITY = "key_shake_sensitivity"
        private const val KEY_SERVER_URL = "key_server_url"

        fun getInstance(): EmergencyManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EmergencyManager(IamHereApplication.instance).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Persistent Login & Victim Identity State
    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean(KEY_IS_LOGGED_IN, false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userPhoneFlow = MutableStateFlow(prefs.getString(KEY_USER_PHONE, "") ?: "")
    val userPhoneFlow: StateFlow<String> = _userPhoneFlow.asStateFlow()

    private val _userNameFlow = MutableStateFlow(prefs.getString(KEY_USER_NAME, "") ?: "")
    val userNameFlow: StateFlow<String> = _userNameFlow.asStateFlow()

    // User Profile Backed by SharedPreferences
    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Priya Sharma") ?: "Priya Sharma"
        set(value) {
            prefs.edit().putString(KEY_USER_NAME, value).apply()
            _userNameFlow.value = value
        }

    var userPhone: String
        get() = prefs.getString(KEY_USER_PHONE, "+919876543210") ?: "+919876543210"
        set(value) {
            prefs.edit().putString(KEY_USER_PHONE, value).apply()
            _userPhoneFlow.value = value
        }

    var medicalNotes: String
        get() = prefs.getString(KEY_MEDICAL_NOTES, "Blood Group: B+, No known drug allergies") ?: ""
        set(value) = prefs.edit().putString(KEY_MEDICAL_NOTES, value).apply()

    var bloodGroup: String
        get() = prefs.getString(KEY_BLOOD_GROUP, "B+") ?: "B+"
        set(value) = prefs.edit().putString(KEY_BLOOD_GROUP, value).apply()

    var cancelPin: String
        get() = prefs.getString(KEY_CANCEL_PIN, "1234") ?: "1234"
        set(value) = prefs.edit().putString(KEY_CANCEL_PIN, value).apply()

    var duressPin: String
        get() = prefs.getString(KEY_DURESS_PIN, "9999") ?: "9999"
        set(value) = prefs.edit().putString(KEY_DURESS_PIN, value).apply()

    // Audio Player for alerts and warnings
    private val audioPlayer = EmergencyAudioPlayer(context)

    // Motion & Shake Settings
    var isShakeDetectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHAKE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SHAKE_ENABLED, value).apply()

    var shakeSensitivity: ShakeSensitivity
        get() {
            val name = prefs.getString(KEY_SHAKE_SENSITIVITY, ShakeSensitivity.MEDIUM.name)
            return try { ShakeSensitivity.valueOf(name ?: ShakeSensitivity.MEDIUM.name) } catch (e: Exception) { ShakeSensitivity.MEDIUM }
        }
        set(value) = prefs.edit().putString(KEY_SHAKE_SENSITIVITY, value.name).apply()

    var isImpactDetectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_IMPACT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_IMPACT_ENABLED, value).apply()

    private var shakeDetector: ShakeDetector? = null

    private val _motionCountdown = MutableStateFlow<Int?>(null)
    val motionCountdown: StateFlow<Int?> = _motionCountdown.asStateFlow()

    private val _motionTriggerReason = MutableStateFlow<String?>(null)
    val motionTriggerReason: StateFlow<String?> = _motionTriggerReason.asStateFlow()

    private var motionCountdownJob: Job? = null

    // Persistent Trusted Contacts List
    private val _contacts = MutableStateFlow<List<Contact>>(loadPersistedContacts())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private fun loadPersistedContacts(): List<Contact> {
        val json = prefs.getString(KEY_CONTACTS_JSON, null)
        return if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<Contact>>() {}.type
                gson.fromJson<List<Contact>>(json, type) ?: defaultContacts()
            } catch (e: Exception) {
                defaultContacts()
            }
        } else {
            defaultContacts().also { persistContacts(it) }
        }
    }

    private fun defaultContacts(): List<Contact> = listOf(
        Contact("c1", "Mother", "+919800000001", "Mother", isPrimary = true),
        Contact("c2", "Sister", "+919800000002", "Sister", isPrimary = true),
        Contact("c3", "Friend (Aditi)", "+919800000003", "Friend", isPrimary = false)
    )

    private fun persistContacts(list: List<Contact>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_CONTACTS_JSON, json).apply()
    }

    fun addContact(contact: Contact) {
        val updated = _contacts.value + contact
        _contacts.value = updated
        persistContacts(updated)
    }

    fun removeContact(id: String) {
        val updated = _contacts.value.filter { it.id != id }
        _contacts.value = updated
        persistContacts(updated)
    }

    fun updateContact(contact: Contact) {
        val updated = _contacts.value.map { if (it.id == contact.id) contact else it }
        _contacts.value = updated
        persistContacts(updated)
    }

    /**
     * Authenticate and initialize emergency victim phone identity
     */
    fun login(
        phone: String,
        name: String,
        blood: String = "B+",
        notes: String = "",
        pin: String = "1234",
        onComplete: (Boolean) -> Unit = {}
    ) {
        val cleanPhone = if (phone.startsWith("+")) phone else "+91$phone"
        val cleanName = if (name.isNotBlank()) name.trim() else "Protected User"

        userPhone = cleanPhone
        userName = cleanName
        bloodGroup = blood
        medicalNotes = notes
        cancelPin = pin

        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_PHONE, cleanPhone)
            .putString(KEY_USER_NAME, cleanName)
            .apply()

        _userPhoneFlow.value = cleanPhone
        _userNameFlow.value = cleanName
        _isLoggedIn.value = true

        // Register victim identity with emergency cloud backend
        scope.launch {
            try {
                NetworkClient.api.phoneLogin(
                    mapOf(
                        "phone" to cleanPhone,
                        "name" to cleanName,
                        "bloodGroup" to blood,
                        "medicalNotes" to notes
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        onComplete(true)
    }

    /**
     * Log out and allow switching victim phone number
     */
    fun logout() {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply()
        _isLoggedIn.value = false
    }

    fun initShakeDetector(ctx: Context) {
        if (shakeDetector == null) {
            shakeDetector = ShakeDetector(
                onShakeDetected = {
                    if (isShakeDetectionEnabled) {
                        onMotionTriggered("Vigorous Shake Detected")
                    }
                },
                onImpactDetected = {
                    if (isImpactDetectionEnabled) {
                        onMotionTriggered("Hard Fall / Impact Detected")
                    }
                }
            ).apply {
                sensitivity = shakeSensitivity
                isImpactDetectionEnabled = this@EmergencyManager.isImpactDetectionEnabled
                start(ctx)
            }
        }
    }

    fun updateShakeSettings(enabled: Boolean, sensitivity: ShakeSensitivity, impactEnabled: Boolean) {
        isShakeDetectionEnabled = enabled
        shakeSensitivity = sensitivity
        isImpactDetectionEnabled = impactEnabled
        shakeDetector?.let {
            it.sensitivity = sensitivity
            it.isImpactDetectionEnabled = impactEnabled
            if (enabled) {
                it.start(context)
            } else {
                it.stop()
            }
        }
    }

    fun onMotionTriggered(reason: String) {
        if (_isEmergencyActive.value || _motionCountdown.value != null) return

        _motionTriggerReason.value = reason
        motionCountdownJob?.cancel()
        motionCountdownJob = scope.launch {
            for (i in 10 downTo 1) {
                _motionCountdown.value = i
                audioPlayer.playWarningBeep()
                delay(1000)
            }
            _motionCountdown.value = null
            _motionTriggerReason.value = null
            triggerSosImmediate()
        }
    }

    fun cancelMotionCountdown() {
        motionCountdownJob?.cancel()
        motionCountdownJob = null
        _motionCountdown.value = null
        _motionTriggerReason.value = null
    }

    // Active Emergency SOS State
    private val _isEmergencyActive = MutableStateFlow(false)
    val isEmergencyActive: StateFlow<Boolean> = _isEmergencyActive.asStateFlow()

    private val _countdown = MutableStateFlow<Int?>(null)
    val countdown: StateFlow<Int?> = _countdown.asStateFlow()

    private val _currentSession = MutableStateFlow<SosSession?>(null)
    val currentSession: StateFlow<SosSession?> = _currentSession.asStateFlow()

    private val _contactAlertStatuses = MutableStateFlow<List<ContactAlertStatus>>(emptyList())
    val contactAlertStatuses: StateFlow<List<ContactAlertStatus>> = _contactAlertStatuses.asStateFlow()

    private var countdownJob: Job? = null
    private var syncJob: Job? = null

    /**
     * Start SOS with 3-second tactile cancelable countdown
     */
    fun startSosCountdown(onCountdownComplete: () -> Unit = {}) {
        if (_isEmergencyActive.value) return

        countdownJob?.cancel()
        countdownJob = scope.launch {
            for (i in 3 downTo 1) {
                _countdown.value = i
                delay(1000)
            }
            _countdown.value = null
            triggerSosImmediate()
            withContext(Dispatchers.Main) {
                onCountdownComplete()
            }
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _countdown.value = null
    }

    /**
     * Immediate SOS Trigger without countdown
     */
    fun triggerSosImmediate(isTestMode: Boolean = false) {
        if (_isEmergencyActive.value) return
        _isEmergencyActive.value = true

        val initialStatuses = _contacts.value.map {
            ContactAlertStatus(
                contactId = it.id,
                name = it.name,
                phone = it.phone,
                relationship = it.relationship,
                deliveryState = DeliveryState.QUEUED
            )
        }
        _contactAlertStatuses.value = initialStatuses

        // 1. Start Native Foreground GPS Service
        val serviceIntent = Intent(context, EmergencyLocationService::class.java).apply {
            action = EmergencyLocationService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        if (!isTestMode) {
            audioPlayer.startEmergencyAlarm()
        }

        // 2. Initialize Emergency Session on Backend
        scope.launch {
            try {
                val startBody = mapOf(
                    "userName" to userName,
                    "userPhone" to userPhone,
                    "medicalNotes" to medicalNotes,
                    "bloodGroup" to bloodGroup,
                    "cancelPin" to cancelPin,
                    "duressPin" to duressPin,
                    "isTestMode" to isTestMode,
                    "contacts" to _contacts.value.map {
                        mapOf(
                            "contactId" to it.id,
                            "name" to it.name,
                            "phone" to it.phone,
                            "relationship" to it.relationship
                        )
                    }
                )

                val response = NetworkClient.api.startSos(startBody)
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    _currentSession.value = result.session

                    // 3. Dispatch SMS alerts to all contacts with live tracking link
                    dispatchEmergencySms(result.session.token, result.liveUrl, isTestMode)

                    // 4. Start polling backend for SEEN and RESPONDING state updates
                    startStatusSyncLoop(result.session.token)
                } else {
                    // Fallback to offline SMS dispatch
                    dispatchOfflineEmergencySms(isTestMode)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Offline fallback
                dispatchOfflineEmergencySms(isTestMode)
            }
        }
    }

    private fun dispatchEmergencySms(token: String, liveUrl: String, isTestMode: Boolean) {
        val contactsList = _contacts.value
        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        contactsList.forEach { contact ->
            try {
                val contactLiveLink = "$liveUrl?cid=${contact.id}"
                val prefix = if (isTestMode) "[TEST DRILL - I AM SAFE] " else "EMERGENCY ALERT! "
                val message = "$prefix$userName ($userPhone) needs help. Live location: $contactLiveLink"

                val sentIntent = PendingIntent.getBroadcast(
                    context,
                    contact.id.hashCode(),
                    Intent(context, SmsStatusReceiver::class.java).apply {
                        action = SmsStatusReceiver.ACTION_SMS_SENT
                        putExtra(SmsStatusReceiver.EXTRA_CONTACT_ID, contact.id)
                        putExtra(SmsStatusReceiver.EXTRA_PHONE, contact.phone)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val deliveredIntent = PendingIntent.getBroadcast(
                    context,
                    contact.id.hashCode() + 1000,
                    Intent(context, SmsStatusReceiver::class.java).apply {
                        action = SmsStatusReceiver.ACTION_SMS_DELIVERED
                        putExtra(SmsStatusReceiver.EXTRA_CONTACT_ID, contact.id)
                        putExtra(SmsStatusReceiver.EXTRA_PHONE, contact.phone)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                smsManager.sendTextMessage(
                    contact.phone,
                    null,
                    message,
                    sentIntent,
                    deliveredIntent
                )
            } catch (e: Exception) {
                e.printStackTrace()
                updateContactStatus(contact.id, DeliveryState.FAILED, "SMS_DISPATCH_EXCEPTION: ${e.message}")
            }
        }
    }

    private fun dispatchOfflineEmergencySms(isTestMode: Boolean) {
        val contactsList = _contacts.value
        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        val prefix = if (isTestMode) "[TEST DRILL] " else "EMERGENCY! "
        val message = "$prefix$userName ($userPhone) needs immediate assistance. Please call back or reach out now."

        contactsList.forEach { contact ->
            try {
                val sentIntent = PendingIntent.getBroadcast(
                    context,
                    contact.id.hashCode(),
                    Intent(context, SmsStatusReceiver::class.java).apply {
                        action = SmsStatusReceiver.ACTION_SMS_SENT
                        putExtra(SmsStatusReceiver.EXTRA_CONTACT_ID, contact.id)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                smsManager.sendTextMessage(contact.phone, null, message, sentIntent, null)
            } catch (e: Exception) {
                updateContactStatus(contact.id, DeliveryState.FAILED, "OFFLINE_SMS_FAILED")
            }
        }
    }

    fun updateContactStatus(contactId: String, deliveryState: DeliveryState, evidence: String?) {
        val currentList = _contactAlertStatuses.value.toMutableList()
        val index = currentList.indexOfFirst { it.contactId == contactId }
        if (index != -1) {
            val updated = currentList[index].copy(
                deliveryState = deliveryState,
                deliveryEvidence = evidence
            )
            if (deliveryState == DeliveryState.SENT) updated.sentAt = System.currentTimeMillis()
            if (deliveryState == DeliveryState.DELIVERED) updated.deliveredAt = System.currentTimeMillis()
            currentList[index] = updated
            _contactAlertStatuses.value = currentList

            // Sync to backend if session active
            val token = _currentSession.value?.token
            if (token != null) {
                scope.launch {
                    try {
                        NetworkClient.api.updateContactStatus(
                            token,
                            mapOf(
                                "contactId" to contactId,
                                "deliveryState" to deliveryState.name,
                                "deliveryEvidence" to (evidence ?: "")
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private val offlineLocationQueue = OfflineLocationQueue()

    suspend fun ingestLocation(location: LocationPoint) {
        val token = _currentSession.value?.token ?: return

        // Accuracy filter: discard fixes worse than 60m radius
        if (location.accuracy > 60f) {
            return
        }

        try {
            val res = NetworkClient.api.updateLocation(token, location)
            if (res.isSuccessful) {
                if (offlineLocationQueue.size() > 0) {
                    offlineLocationQueue.flush(token)
                }
            } else {
                offlineLocationQueue.enqueue(location)
            }
        } catch (e: Exception) {
            offlineLocationQueue.enqueue(location)
        }
    }

    private fun startStatusSyncLoop(token: String) {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (isActive && _isEmergencyActive.value) {
                delay(3000)
                try {
                    val res = NetworkClient.api.getSosStatus(token)
                    if (res.isSuccessful && res.body() != null) {
                        // Parse contacts status array to update Seen and Responding indicators
                        val body = res.body()!!
                        @Suppress("UNCHECKED_CAST")
                        val sessionData = body["session"] as? Map<String, Any>
                        @Suppress("UNCHECKED_CAST")
                        val remoteContacts = sessionData?.get("contacts") as? List<Map<String, Any>>
                        
                        if (remoteContacts != null) {
                            val local = _contactAlertStatuses.value.toMutableList()
                            remoteContacts.forEach { rc ->
                                val cid = rc["contactId"] as? String
                                val seen = rc["seen"] as? Boolean ?: false
                                val responding = rc["responding"] as? Boolean ?: false
                                val responseNote = rc["responseNote"] as? String

                                val idx = local.indexOfFirst { it.contactId == cid }
                                if (idx != -1) {
                                    val item = local[idx]
                                    if (seen && !item.seen) {
                                        item.seen = true
                                        item.seenAt = (rc["seenAt"] as? Number)?.toLong()
                                    }
                                    if (responding && !item.responding) {
                                        item.responding = true
                                        item.respondingAt = (rc["respondingAt"] as? Number)?.toLong()
                                        item.responseNote = responseNote
                                    }
                                    local[idx] = item
                                }
                            }
                            _contactAlertStatuses.value = local
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun stopSos(enteredPin: String, onResult: (Boolean, String) -> Unit) {
        val token = _currentSession.value?.token
        val isDuress = (enteredPin == duressPin)

        audioPlayer.stopEmergencyAlarm()

        if (!isDuress) {
            // Normal Cancel: Stop location service completely for zero passive tracking
            val serviceIntent = Intent(context, EmergencyLocationService::class.java).apply {
                action = EmergencyLocationService.ACTION_STOP
            }
            context.stopService(serviceIntent)
            syncJob?.cancel()
        } else {
            // Secret Duress PIN: Keep location service running silently and send covert SMS
            dispatchDuressCovertSms(token)
        }

        if (token != null) {
            scope.launch {
                try {
                    val res = NetworkClient.api.stopSos(token, mapOf("pin" to enteredPin))
                    withContext(Dispatchers.Main) {
                        if (res.isSuccessful) {
                            val status = res.body()?.status ?: if (isDuress) "DURESS_CANCELLED" else "RESOLVED"
                            _isEmergencyActive.value = false
                            if (!isDuress) {
                                _currentSession.value = null
                            }
                            onResult(true, status)
                        } else {
                            onResult(false, "Incorrect PIN")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        // Offline resolution
                        if (enteredPin == cancelPin || enteredPin == duressPin) {
                            _isEmergencyActive.value = false
                            if (!isDuress) {
                                _currentSession.value = null
                            }
                            onResult(true, if (isDuress) "DURESS_CANCELLED" else "RESOLVED")
                        } else {
                            onResult(false, "Incorrect PIN")
                        }
                    }
                }
            }
        } else {
            if (enteredPin == cancelPin || enteredPin == duressPin) {
                _isEmergencyActive.value = false
                if (!isDuress) {
                    _currentSession.value = null
                }
                onResult(true, if (isDuress) "DURESS_CANCELLED" else "RESOLVED")
            } else {
                onResult(false, "Incorrect PIN")
            }
        }
    }

    private fun dispatchDuressCovertSms(token: String?) {
        val contactsList = _contacts.value.filter { it.isPrimary }
        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        val trackingLink = if (token != null) "${NetworkClient.baseUrl.replace("/api", "")}/live/$token" else "live tracker"
        val message = "[URGENT DURESS ALERT] $userName ($userPhone) entered secret duress code under force! Attacker present. Silent live tracking remains active: $trackingLink"

        contactsList.forEach { contact ->
            try {
                smsManager.sendTextMessage(contact.phone, null, message, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
