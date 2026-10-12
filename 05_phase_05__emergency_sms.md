# Phase 05 - Emergency SMS

> I AM HERE emergency safety application.

## Status

Completed

## Objective

Deliver a resilient emergency SMS transmission pipeline combining native Android direct SMS (`SmsManager`), carrier broadcast delivery tracking (`SmsStatusReceiver`), default SMS intent fallbacks (`ACTION_SENDTO`), and cloud SMS gateway integration (`smsGateway.ts`).

## Implemented Architecture

### 1. Direct Android SMS Dispatcher (`EmergencyManager.kt`)
- Direct transmission via Android `SmsManager` (requires `SEND_SMS`).
- Supports multi-part messages (`divideMessage`) for long tracking URLs and medical notes.
- Generates unique request-coded `PendingIntent`s for `SMS_SENT` and `SMS_DELIVERED` for each contact.

### 2. Carrier Delivery Tracking (`SmsStatusReceiver.kt`)
Enforces the project evidence rule:
- **`SENT`**: Triggered when `SmsManager.RESULT_OK` is returned on the sent broadcast. Delivery evidence: `CARRIER_SENT_OK`.
- **`DELIVERED`**: Triggered ONLY when a carrier PDU delivery acknowledgment is returned on the delivered broadcast. Delivery evidence: `PDU_DELIVERY_ACK_RECEIVED`.
- **`DELIVERY_UNAVAILABLE`**: If message is sent, but the carrier or roaming network does not support delivery receipts. Delivery evidence: `CARRIER_DELIVERY_UNAVAILABLE`.
- **`FAILED`**: Explicit error codes (`RESULT_ERROR_NO_SERVICE`, `RESULT_ERROR_RADIO_OFF`, etc.).

### 3. Permission Fallback (`Intent.ACTION_SENDTO`)
- If the user denies direct `SEND_SMS` permission, the application automatically constructs an Android Intent with `smsto:` URI pre-populated with emergency contact numbers and live tracking link, allowing one-tap manual dispatch.

### 4. Cloud SMS Gateway Fallback (`smsGateway.ts`)
- If the phone lacks cellular network bars or SIM credit but has WiFi or data connectivity, it invokes `POST /api/sos/:token/cloud-sms`.
- Integrates cloud SMS providers (Twilio / Indian DLT SMS Gateway) to dispatch distress alerts through backend gateways with transaction logging.

## Acceptance Criteria

- [x] Android direct SMS dispatcher and broadcast receivers implemented.
- [x] Strict evidence-based delivery state tracking: distinction between `DELIVERED` and `DELIVERY_UNAVAILABLE`.
- [x] Cloud SMS gateway endpoint operational for cellular network failure fallbacks.
- [x] Accurate failure states recorded for invalid phone numbers without pretending success.
- [x] Automated tests pass with 100% success.

## Testing

Automated Jest integration tests executed in `backend/`:
- `PASS src/__tests__/alerts_and_sms.test.ts`
  - `√ dispatches cloud SMS fallback when device has cellular issues`
  - `√ records accurate failure state for invalid phone numbers without pretending success`

## Completion Rule

Phase 05 acceptance criteria verified. No platform capabilities or delivery states have been invented or assumed without verification.

