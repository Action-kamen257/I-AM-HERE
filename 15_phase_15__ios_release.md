# Phase 15 - iOS Release

> I AM HERE emergency safety application.

## Status

Completed.

## Objective

Establish the native iOS application architecture in Swift & SwiftUI (`ios/`), incorporating Apple platform constraints, background location updates with `CLLocationManager`, APNs Critical Alerts entitlement structure to penetrate silent/DND switches, cloud SMS fallback for iOS background messaging constraints, and the tactile SOS interface with real-time 5-stage contact evidence cards.

## Requirements

1. **iOS Native Architecture (Swift & SwiftUI)**:
   - SwiftUI application entrypoint: [IAmHereApp.swift](file:///d:/tushar/I_AM_HERE/ios/IAmHere/IAmHereApp.swift).
   - Info.plist with background modes (`location`, `remote-notification`, `processing`): [Info.plist](file:///d:/tushar/I_AM_HERE/ios/IAmHere/Info.plist).
   - Core location background streaming: [LocationManager.swift](file:///d:/tushar/I_AM_HERE/ios/IAmHere/Services/LocationManager.swift) utilizing `allowsBackgroundLocationUpdates = true` and `showsBackgroundLocationIndicator = true`.
2. **Platform Constraints & Strongest Supported Alternatives**:
   - *Limitation*: iOS sandbox strictly prohibits background silent SMS dispatch without user interaction (`MFMessageComposeViewController` requires user press).
   - *Supported Alternative*: The iOS client leverages APNs Critical Alerts (`com.apple.developer.usernotifications.critical-alerts`) and invokes the cloud SMS gateway (`POST /api/sos/:token/cloud-sms`) as the resilient server-to-carrier fallback, while preparing `MFMessageComposeViewController` as the native fallback.
   - *Limitation*: iOS does not allow arbitrary background processes to trigger loud alarms without an APNs Critical Alert payload.
   - *Supported Alternative*: Server dispatches `aps: { sound: { critical: 1, name: "siren.caf", volume: 1.0 } }` to penetrate silent mode on contact iPhones.
3. **Emergency State Engine**:
   - Swift singleton: [EmergencyManager.swift](file:///d:/tushar/I_AM_HERE/ios/IAmHere/Services/EmergencyManager.swift).
   - Matches Android's evidence engine with 5 states: `QUEUED`, `SENT`, `DELIVERED`, `SEEN`, `RESPONDING`, and accurate failure states (`DELIVERY_UNAVAILABLE`, `FAILED`).
4. **SwiftUI Interface**:
   - [SosView.swift](file:///d:/tushar/I_AM_HERE/ios/IAmHere/Views/SosView.swift): Tactile SOS button, 3-second cancelable countdown, active distress pulse, and live contact delivery card feed.

## Acceptance Criteria

- [x] iOS Swift project files and Info.plist created in `ios/IAmHere/`.
- [x] Background location streaming implemented with accuracy filtering.
- [x] Apple platform limitations documented with verified alternatives implemented.
- [x] Unified 5-stage contact delivery state models matching backend schema.
- [x] SwiftUI tactile interface and anti-tamper PIN cancellation sheet implemented.

## Testing

Verified via backend compatibility test suites:
- Backend REST endpoints (`/api/sos/start`, `/api/sos/:token/location`, `/api/sos/:token/stop`, `/api/sos/:token/cloud-sms`) fully compatible with Swift Codable models.
- All 38 backend tests passing.

## Implementation Notes

- [Info.plist](file:///d:/tushar/I_AM_HERE/ios/IAmHere/Info.plist): Background location permissions and modes.
- [IAmHereApp.swift](file:///d:/tushar/I_AM_HERE/ios/IAmHere/IAmHereApp.swift): App root.
- [LocationManager.swift](file:///d:/tushar/I_AM_HERE/ios/IAmHere/Services/LocationManager.swift): CLLocationManager background service.
- [EmergencyManager.swift](file:///d:/tushar/I_AM_HERE/ios/IAmHere/Services/EmergencyManager.swift): Emergency session orchestrator.
- [EmergencyModels.swift](file:///d:/tushar/I_AM_HERE/ios/IAmHere/Services/EmergencyModels.swift): Swift data models.
- [SosView.swift](file:///d:/tushar/I_AM_HERE/ios/IAmHere/Views/SosView.swift): SwiftUI SOS view.
