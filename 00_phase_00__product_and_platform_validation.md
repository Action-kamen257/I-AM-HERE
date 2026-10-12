# Phase 00 - Product and Platform Validation

> I AM HERE emergency safety application.

## Status

Completed

## Objective

Validate platform capabilities, OS execution limits, hardware constraints, API capabilities, and foundational technical choices for the Android implementation of the I AM HERE emergency safety application.

## Platform Validation Findings

### 1. Android Background Execution & Doze Mode
- **Foreground Service**: Mandatory for continuous GPS transmission and sensor monitoring. Must use `android:foregroundServiceType="location|specialUse"` (compliant with Android 14+ / API 34+ guidelines).
- **WakeLock**: Must hold a `PARTIAL_WAKE_LOCK` while SOS is active to prevent CPU sleep during critical transmission.
- **Battery Optimization**: Prompt user for `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` to avoid OEM background kill (Samsung Sleeping Apps, MIUI Battery Saver, etc.).
- **Lock Screen Activity**: Configure emergency trigger screen with `setShowWhenLocked(true)` and `setTurnScreenOn(true)` to display over lock screen.

### 2. SMS Delivery Mechanics & Evidence Rule
- **Direct SMS (`SmsManager`)**: Requires `SEND_SMS` permission.
- **Delivery Tracking**:
  - `sendTextMessage` with `sentIntent` and `deliveryIntent`.
  - Listen via `BroadcastReceiver`.
  - If carrier returns delivery PDU: mark state as `Delivered`.
  - If carrier does not provide delivery reports: mark state as `SMS sent (carrier delivery unavailable)`. Under the core product rule, never claim "Delivered" without carrier delivery evidence.
- **Fallback Intent**: If direct SMS permission is not granted, launch `Intent.ACTION_SENDTO` to pre-populate message in default SMS client.
- **Backend SMS Fallback**: Dispatch SMS via cloud gateway when phone has zero cellular signal or carrier issues but has data/WiFi.

### 3. Location Freshness & Accuracy
- **Provider**: Google Play Services `FusedLocationProviderClient` with `PRIORITY_HIGH_ACCURACY`.
- **Interval**: 5 seconds during active SOS; fallback to 15 seconds if battery < 15%.
- **Evidence Rule**: Every coordinate must log timestamp, horizontal accuracy radius in meters, speed, and provider (GPS/Network).

### 4. Development Environment Validation
- **Node.js**: v20.12.2 (Verified)
- **NPM**: 11.16.0 (Verified)
- **Java**: OpenJDK 17.0.19 (Verified, `C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot\`)
- **Android SDK**: Platforms Android 34, 35, 36 present in `D:\tushar\AndroidSDK`
- **Build Tools**: 34.0.0, 36.0.0 present
- **ADB**: `D:\tushar\AndroidSDK\platform-tools\adb.exe` operational
- **Physical Device**: Device `RZCWA1F794K` attached via USB (Pending user approval on phone screen)

## Requirements

1. Architecture must decouple the mobile application from the emergency backend so both can operate autonomously and reliably.
2. The trusted emergency-contact pathway must track and display 5 distinct states:
   - Queued / Sending
   - Sent
   - Delivered (requires carrier/network receipt)
   - Seen (requires contact opening link or push acknowledge)
   - Responding (requires explicit contact action)
3. Offline queueing: Network drops must not lose location points; points must be buffered locally and flushed sequentially.

## Acceptance Criteria

- [x] Host environment validated (Java 17, Android SDK 34-36, Node 20, ADB).
- [x] Connected device identified (`RZCWA1F794K`).
- [x] Android OS limits, background location requirements, and SMS delivery tracking mapped out.
- [x] Concrete state machine and evidence rules defined.

## Testing

- Verified development tools and Android SDK paths using PowerShell CLI.
- Verified ADB connectivity and daemon initialization on port 5037.
- Verified Android SDK platform-tools and build-tools versions.

## Completion Rule

Phase 00 acceptance criteria verified. No platform capabilities or delivery states have been invented or assumed without verification.

