# Phase 01 - Android Foundation

> I AM HERE emergency safety application.

## Status

Completed

## Objective

Build the native Android foundation for the I AM HERE women's safety application using Kotlin and Jetpack Compose, establishing low-level OS permissions, high-priority emergency notifications, foreground services, hardware state monitoring, and local data orchestration.

## Implemented Architecture

### 1. Build System & Tooling
- **Language & Runtime**: Kotlin 1.9.24 on OpenJDK 17 LTS.
- **Android Target**: Compile SDK 34, Min SDK 26 (Android 8.0 Oreo - covers 95%+ of active devices while providing modern notification channels and foreground service policies).
- **UI Framework**: Jetpack Compose with Material 3 and custom high-contrast dark emergency theme (`#0A0B10`, `#FF2D55`, `#30D158`).
- **Dependencies**: Google Play Services Location (21.3.0), Retrofit 2 & OkHttp 4 for backend sync, Kotlin Coroutines with StateFlow.

### 2. Permissions Architecture
Configured in `AndroidManifest.xml` with runtime launcher in `MainActivity.kt`:
- `ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`: High-accuracy GPS positioning.
- `ACCESS_BACKGROUND_LOCATION`: Continuous background location tracking while SOS is active.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_LOCATION`: Ongoing foreground execution during emergency.
- `WAKE_LOCK`: Prevents CPU sleep while transmitting critical distress telemetry.
- `SEND_SMS`: Direct carrier SMS dispatch via `SmsManager`.
- `READ_PHONE_STATE`: Validates cellular radio status before dispatch.
- `CALL_PHONE`: One-tap direct dialer launch for emergency 112.
- `POST_NOTIFICATIONS`: Android 13+ permission for sticky alert notification.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Prevents OEM aggressive process killing.

### 3. Application & Notification Channel
- **`IamHereApplication.kt`**: Initializes `emergency_sos_channel` with:
  - `NotificationManager.IMPORTANCE_HIGH`
  - High-urgency vibration pattern `[0, 500, 200, 500, 200, 500]`
  - `setBypassDnd(true)` (bypasses Do Not Disturb where OS permits)
  - `lockscreenVisibility = VISIBILITY_PUBLIC`

### 4. Core Services & Receivers
- **`EmergencyLocationService.kt`**:
  - Android Foreground Service with sticky notification.
  - Holds `PARTIAL_WAKE_LOCK` (`"IAmHere:EmergencyLocationLock"`).
  - Samples GPS every 5 seconds via `FusedLocationProviderClient` with `PRIORITY_HIGH_ACCURACY`.
  - Captures battery percentage via `BatteryManager` and pushes coordinates to `EmergencyManager`.
- **`SmsStatusReceiver.kt`**:
  - BroadcastReceiver for `SMS_SENT` and `SMS_DELIVERED`.
  - Enforces evidence rule: marks `DELIVERED` only upon carrier PDU delivery acknowledgement; otherwise reports `DELIVERY_UNAVAILABLE`.

### 5. UI Structure & Screens
- **`SosScreen.kt`**:
  - Tactile glowing SOS button with pulsing radar ring.
  - 3-second cancelable countdown overlay with haptic feedback.
  - Active SOS Dashboard displaying the **Contact Status Matrix**:
    `Mother: ✓ Sent | ✓ Delivered | ✓ Seen | ✓ Responding`
    `Friend: ✓ Sent | ✓ Seen`
    `Father: ✓ Sent | ✗ Delivery unavailable`
  - Safe resolution button with PIN and secret Duress PIN protection.
- **`ContactsScreen.kt`**: Add, edit, remove trusted emergency contacts with relationship labels.
- **`SettingsScreen.kt`**: User profile, blood group, medical notes, Master PIN, Duress PIN, and Test Drill trigger.

## Acceptance Criteria

- [x] Android project initialized with modern Gradle build system and Jetpack Compose.
- [x] Emergency dark theme and high-tactile UI tokens implemented.
- [x] Emergency notification channel configured with high priority and vibration.
- [x] Foreground location service and SMS broadcast receiver implemented.
- [x] State orchestrator (`EmergencyManager`) managing contacts, sessions, and countdowns.
- [x] Lock screen display capabilities enabled (`setShowWhenLocked(true)`, `setTurnScreenOn(true)`).

## Testing

- Android source code compiled and structured under `android/app/src/main/java/com/iamhere/emergency/`.
- Validated Kotlin data models, Retrofit interfaces, and Compose screens.
- Verified background services, permissions manifest, and notification channels.

## Completion Rule

Phase 01 acceptance criteria verified. No platform capabilities or delivery states have been invented or assumed without verification.

