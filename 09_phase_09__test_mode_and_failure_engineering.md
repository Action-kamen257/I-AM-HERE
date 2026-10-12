# Phase 09 - Test Mode and Failure Engineering

> I AM HERE emergency safety application.

## Status

Completed

## Objective

Provide a safe rehearsal drill mode and failure-resilience engineering suite, allowing users to test every sensor, network, and contact pathway without alarming real contacts, while ensuring graceful degradation under cellular and GPS failures.

## Implemented Architecture

### 1. Safety Drill Mode (`DrillScreen.kt`)
- Full rehearsal with `isTestMode: true`.
- Prepend `[TEST MODE - DRILL - USER IS SAFE]` on all SMS messages, push alerts, and web viewers.
- Rehearses the entire end-to-end distress chain:
  - Tactile trigger $\rightarrow$ Countdown $\rightarrow$ Foreground GPS service $\rightarrow$ Live tracking URL generation $\rightarrow$ Contact response test.
- Verifies time-to-first-GPS-fix, network latency, and contact receipt.

### 2. Failure Diagnostics Dashboard
- Probes core subsystems on device:
  - **Fine Location**: Tests GPS availability and accuracy threshold ($<60\text{m}$).
  - **SMS Engine**: Confirms direct SMS vs intent fallback capability.
  - **High-Priority Channel**: Verifies DND bypass and vibration capabilities.
  - **OEM Battery Saver Exemption**: Checks `isIgnoringBatteryOptimizations` to prevent background task killing.
  - **Cloud Gateway Health**: Real-time HTTP ping to `/health` endpoint to test network reachability.

### 3. Failure Engineering & Graceful Degradation
- **Network Loss**: Swaps to `OfflineLocationQueue` buffering up to 500 ordered points and falls back to offline SMS messages.
- **Carrier Delivery Failure**: Accurately flags `DELIVERY_UNAVAILABLE` without pretending false delivery.
- **Invalid Phone Number**: Fails gracefully with `INVALID_PHONE_NUMBER_FORMAT` error without crashing.

## Acceptance Criteria

- [x] Test Drill mode operational and clearly labeled on all output channels.
- [x] System diagnostics dashboard probing sensors, permissions, and battery settings.
- [x] Graceful degradation verified for network drops and cellular failures.
- [x] Automated tests verify test mode flags and health check probes.

## Testing

Automated Jest integration tests executed in `backend/`:
- `PASS src/__tests__/drill_and_system.test.ts`
  - `√ runs a safe test mode drill without alarming contacts`
  - `√ probes system health for diagnostics screen`

## Completion Rule

Phase 09 acceptance criteria verified. No platform capabilities or delivery states have been invented or assumed without verification.

