package com.iamhere.emergency

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.iamhere.emergency.data.repository.EmergencyManager
import com.iamhere.emergency.service.PowerButtonSosReceiver
import com.iamhere.emergency.ui.screens.PhoneLoginScreen
import com.iamhere.emergency.ui.screens.ContactsScreen
import com.iamhere.emergency.ui.screens.DrillScreen
import com.iamhere.emergency.ui.screens.FakeCallOverlay
import com.iamhere.emergency.ui.screens.SafetyToolsScreen
import com.iamhere.emergency.ui.screens.SettingsScreen
import com.iamhere.emergency.ui.screens.SosScreen
import com.iamhere.emergency.ui.theme.*


class MainActivity : ComponentActivity() {

    private var powerButtonReceiver: PowerButtonSosReceiver? = null

    private val requiredPermissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.CALL_PHONE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions granted/denied handled gracefully by services
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureLockScreenDisplay()
        checkAndRequestPermissions()
        registerPowerButtonListener()
        EmergencyManager.getInstance().initShakeDetector(this)
        handleLaunchIntent(intent)

        setContent {
            IAmHereTheme {
                val isLoggedIn by EmergencyManager.getInstance().isLoggedIn.collectAsState()
                if (!isLoggedIn) {
                    PhoneLoginScreen(
                        onLoginSuccess = { /* Automatically navigates via isLoggedIn flow */ }
                    )
                } else {
                    MainAppScaffold()
                }
                FakeCallOverlay()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("EXTRA_TRIGGER_SOS_IMMEDIATE", false) == true) {
            EmergencyManager.getInstance().triggerSosImmediate(isTestMode = false)
        }
    }

    private fun registerPowerButtonListener() {
        if (powerButtonReceiver == null) {
            powerButtonReceiver = PowerButtonSosReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            registerReceiver(powerButtonReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        powerButtonReceiver?.let {
            unregisterReceiver(it)
            powerButtonReceiver = null
        }
    }

    private fun configureLockScreenDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    private fun checkAndRequestPermissions() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }
}

@Composable
fun MainAppScaffold() {
    // Default to SOS tab in center (index 2)
    var selectedTab by remember { mutableIntStateOf(2) }
    val manager = remember { EmergencyManager.getInstance() }
    val motionCountdown by manager.motionCountdown.collectAsState()
    val motionReason by manager.motionTriggerReason.collectAsState()

    val navItems = remember {
        listOf(
            GlassNavItem(Icons.Default.Shield, "Tools"),
            GlassNavItem(Icons.Default.People, "Circle"),
            GlassNavItem(Icons.Default.Warning, "SOS", isEmergencyButton = true),
            GlassNavItem(Icons.Default.HealthAndSafety, "Drill"),
            GlassNavItem(Icons.Default.Settings, "Settings")
        )
    }

    GlassBackground(showMapCanvas = (selectedTab == 2)) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                GlassNavBar(
                    items = navItems,
                    selectedIndex = selectedTab,
                    onSelectIndex = { selectedTab = it }
                )
            }
        ) { _ ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                when (selectedTab) {
                    0 -> SafetyToolsScreen()
                    1 -> ContactsScreen()
                    2 -> SosScreen()
                    3 -> DrillScreen(onStartDrill = { selectedTab = 2 })
                    4 -> SettingsScreen()
                }

                if (motionCountdown != null) {
                    MotionCountdownOverlay(
                        countdown = motionCountdown!!,
                        reason = motionReason,
                        onCancel = { manager.cancelMotionCountdown() }
                    )
                }
            }
        }
    }
}

@Composable
fun MotionCountdownOverlay(
    countdown: Int,
    reason: String?,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassPill(
                    text = "POTENTIAL EMERGENCY",
                    tintColor = EmergencyRed,
                    icon = Icons.Default.Warning
                )
                Text(
                    text = reason ?: "Vigorous Motion Detected",
                    color = TextSecondaryLight,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "$countdown",
                    color = CoralPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 72.sp,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "Emergency SOS will trigger automatically in $countdown seconds.\nTap below immediately if this is a false alarm.",
                    color = TextPrimaryLight,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                GlassButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    backgroundColor = CoralPrimary,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        "I AM SAFE • CANCEL SOS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

