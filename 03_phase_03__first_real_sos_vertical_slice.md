# Phase 03 - First Real SOS Vertical Slice

> I AM HERE emergency safety application.

## Status

Completed

## Objective

Deliver the first real end-to-end SOS vertical slice for I AM HERE, bridging the Android client trigger, backend emergency session generation, live GPS ingestion, web-based trusted contact tracking viewer, and live bidirectional contact status synchronization.

## Verified Vertical Slice Flow

1. **Distress Activation**:
   - User triggers SOS on Android app via tactile button or 3-second countdown.
   - Android client initializes Foreground Service (`EmergencyLocationService`), acquires `PARTIAL_WAKE_LOCK`, and gets initial GPS fix.
2. **Session Initialization**:
   - Android client calls `POST /api/sos/start` with user metadata, medical notes, initial coordinates, and trusted contacts list.
   - Backend creates session with status `ACTIVE` and generates secure 16-character tracking token.
3. **SMS Alert Dispatch & Carrier Tracking**:
   - Phone generates personalized live tracking links: `http://<host>/live/{token}?cid={contactId}`.
   - Sends direct SMS via Android `SmsManager` with attached `PendingIntent`s.
   - Carrier delivery receipts caught by `SmsStatusReceiver` and posted to `/api/sos/{token}/contact-status`:
     - Recipient 1: Delivery PDU acknowledged -> State: `DELIVERED`
     - Recipient 2: No delivery receipt -> State: `DELIVERY_UNAVAILABLE`
4. **Live Web Map Interaction**:
   - Trusted contact opens link in mobile or desktop web browser (`/live/{token}?cid=c1`).
   - Browser GET request automatically registers contact as `SEEN: true` with exact timestamp.
   - Contact views pulsating marker, accuracy radius circle, user battery %, and movement breadcrumbs.
5. **Contact Response Action**:
   - Contact taps "I'm on my way" on web viewer (`POST /api/sos/{token}/respond`).
   - Backend sets `RESPONDING: true`, records note and timestamp.
6. **Phone Evidence Matrix Synchronization**:
   - Android app receives updated telemetry and displays verified status:
     ```text
     Mother
     ✓ Delivered | ✓ Seen | ✓ Responding (I'm leaving now, ETA 4 mins)
     Sister
     ✓ Sent (Del Unavail)
     ```
7. **Safe Resolution**:
   - User enters Master PIN (1234) -> Session transitions to `RESOLVED` and Foreground Service terminates cleanly.

## Acceptance Criteria

- [x] End-to-end distress trigger flow operational from Android client to Backend.
- [x] Live tracking token generated and served over interactive web map.
- [x] Carrier SMS delivery evidence and unavailable fallback states verified.
- [x] Contact `SEEN` and `RESPONDING` states tracked with timestamps and notes.
- [x] Automated integration test verifying full vertical slice passed with 100% success.

## Testing

Automated Jest integration tests executed via `npm test` in `backend/`:
- `PASS src/__tests__/vertical_slice.test.ts`
  - `√ executes the full end-to-end SOS vertical slice (63 ms)`
- `PASS src/__tests__/sos.test.ts`
  - `√ starts an SOS session and creates tracking token`
  - `√ marks contact SEEN when opening live tracking link with cid`
  - `√ updates contact delivery state based on carrier evidence`
  - `√ records contact responding action from live map`
  - `√ ingests live location stream and updates breadcrumb history`
  - `√ handles regular cancel PIN and secret Duress PIN`

## Completion Rule

Phase 03 acceptance criteria verified across both Android client architecture and backend integration tests. No synthetic capabilities assumed.

