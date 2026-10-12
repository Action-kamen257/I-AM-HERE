# Phase 12 - Security and Privacy Hardening

> I AM HERE emergency safety application.

## Status

Completed.

## Objective

Implement bulletproof security and privacy controls: secret Duress PIN handling with deceptive visual de-escalation, covert background live tracking, and silent duress alerts to trusted contacts; strict zero-passive-tracking guarantees when inactive; token TTL expiration and automatic link revocation to prevent permanent location tracking data leaks; and unified error sanitization.

## Requirements

1. **Secret Duress PIN & Covert Silent Tracking**:
   - If an attacker coerces the user to cancel the alarm, entering the secret Duress PIN (default "9999", configurable) visually dismisses the red emergency screen and silences local siren audio to protect the victim from immediate retaliatory violence.
   - The backend session status transitions to `DURESS_CANCELLED`.
   - Silent, high-precision background location streaming continues uninterrupted (`addLocation` and `addLocationsBatch` remain authorized).
   - A covert priority SMS is immediately dispatched to primary trusted contacts:
     `"[URGENT DURESS ALERT] Priya Sharma entered secret duress code under force! Attacker present. Silent live tracking remains active: <link>"`.
   - The public live tracking dashboard updates with a high-visibility warning banner:
     `"⚠️ CRITICAL: USER FORCED TO CANCEL! SILENT TRACKING IS ACTIVELY STREAMING"`.
2. **Zero Passive Tracking & Privacy by Design**:
   - When legitimate Master PIN ("1234") is entered, `EmergencyLocationService` is completely stopped, GPS listeners unregister, and WakeLocks release immediately.
   - Zero background location polling or unsolicited network transmissions occur when the app is inactive.
   - Post-resolution location updates are strictly rejected with HTTP 404.
3. **Token TTL Expiration & Auto-Revocation**:
   - Resolved sessions expire after 2 hours, revoking the tracking token to protect user location history from lingering online access.
   - Absolute maximum session TTL of 24 hours enforced across all emergency sessions.
   - Expired tokens return HTTP 404 and cannot be queried or updated.
4. **Input Sanitization & Authentication**:
   - Cancellation PIN checks return uniform error messages to prevent credential enumerations.

## Acceptance Criteria

- [x] Duress PIN cancels visible alarm on screen while maintaining silent location updates.
- [x] Covert SMS dispatched to primary contacts upon duress cancellation.
- [x] Live tracking web viewer prominently displays duress alert and actively renders covert coordinates.
- [x] Master Cancel PIN terminates location ingestion and ceases all tracking activities immediately.
- [x] Token TTL expiration revokes access to resolved tracking links after 2 hours.
- [x] 24-hour absolute TTL prevents indefinitely lingering emergency sessions.
- [x] Verified via automated test suite `src/__tests__/security_and_privacy.test.ts`.

## Testing

Verified via automated test suite `src/__tests__/security_and_privacy.test.ts`:
- `accepts duress PIN, enters DURESS_CANCELLED status, and continues location ingestion` (PASS)
- `rejects incorrect PIN without leaking whether duress exists` (PASS)
- `normal resolution terminates location ingestion immediately` (PASS)
- `enforces token TTL expiration to prevent permanent location tracking links` (PASS)
- `enforces absolute 24-hour maximum TTL on any emergency session` (PASS)

## Implementation Notes

- [store.ts](file:///d:/tushar/I_AM_HERE/backend/src/store.ts): Implemented `DURESS_CANCELLED` location authorization, 2-hour post-resolution token expiry, and 24-hour absolute TTL revocation in `getSessionByToken`.
- [app.ts](file:///d:/tushar/I_AM_HERE/backend/src/app.ts): Returns updated session object with location telemetry for real-time duress status confirmation.
- [live.html](file:///d:/tushar/I_AM_HERE/backend/public/live.html): Added dedicated `#ff3b30` duress alert banner and session conclusion status handling.
- [EmergencyManager.kt](file:///d:/tushar/I_AM_HERE/android/app/src/main/java/com/iamhere/emergency/data/repository/EmergencyManager.kt): Integrated `dispatchDuressCovertSms` and bifurcated normal cancellation (zero passive tracking) from duress cancellation (covert silent background GPS streaming).
