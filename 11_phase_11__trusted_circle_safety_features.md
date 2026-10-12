# Phase 11 - Trusted Circle Safety Features

> I AM HERE emergency safety application.

## Status

Completed.

## Objective

Deliver proactive trusted circle safety companion tools: "Walk With Me" timed journey companion with automated check-in enforcement and missed check-in escalation, a discreet simulated incoming fake call generator to deter harassment and exit uncomfortable situations, and nearby verified 24/7 safe havens and emergency desks with one-tap directions and telephone dialing.

## Requirements

1. **"Walk With Me" Journey Companion**:
   - Timed journey monitoring with configurable total duration (15m, 30m, 45m, 60m) and periodic safety check-ins (5m, 10m, 15m).
   - "Check-In Required" prompt with a 60-second grace period countdown.
   - If user confirms "I Am Safe", next check-in window automatically advances.
   - If user fails to check in before grace period expires, automatically triggers full emergency SOS to trusted contacts with reason: `"Missed Journey Check-in (Walk With Me)"`.
   - Safe early arrival / journey termination without triggering distress.
2. **Discreet Simulated Fake Call Generator**:
   - Realistic incoming phone call simulation to deter stalking or exit uncomfortable environments.
   - Configurable caller persona ("Dad", "Boss", "Inspector Sharma", "Mom") and delay countdown (5s, 15s, 30s, 60s).
   - Full-screen incoming call UI with native ringtone and vibration pattern.
   - In-call connected screen with real-time call timer (00:01, 00:02...), mute/speaker controls, and hangup.
3. **Nearby Safe Points of Interest (POIs)**:
   - Curated directory of 24/7 safe spaces: Central Police Stations & Chowkis, National Women Helpline Centers (1091 / 181), 24/7 Hospital Trauma Wings, and Metro Transit Posts.
   - 1-tap Google Maps GPS navigation via `geo:` intent and 1-tap direct dialing via `tel:` intent.
4. **Intuitive UI Integration**:
   - Seamlessly integrated into `ContactsScreen.kt` via top sub-tabs: "Trusted Circle" & "Safety Tools".
   - Full-screen `FakeCallOverlay` integrated in `MainActivity.kt`.

## Acceptance Criteria

- [x] "Walk With Me" journey tracks active timing and advances check-ins on user confirmation.
- [x] Missed check-in grace period timeout automatically escalates to emergency SOS dispatch.
- [x] Discreet Fake Call generator schedules, rings with native ringtone/haptics, connects, tracks call duration, and hangs up cleanly.
- [x] Safe POIs display address, distance, 24/7 status, 1-tap map navigation, and 1-tap telephone dialing.
- [x] Tested and verified via automated test suite `src/__tests__/trusted_circle_and_safety_tools.test.ts`.

## Testing

Verified via automated test suite `src/__tests__/trusted_circle_and_safety_tools.test.ts`:
- `initializes journey session with check-in intervals and tracks timing` (PASS)
- `triggers automatic SOS when grace period expires on missed check-in` (PASS)
- `allows graceful journey completion when user arrives safely` (PASS)
- `schedules fake incoming call and handles ringing and answered states` (PASS)
- `contains verified emergency phone numbers and coordinates for 1-tap dispatch` (PASS)
- `formats valid geo navigation and tel URI links` (PASS)

## Implementation Notes

- [JourneyManager.kt](file:///d:/tushar/I_AM_HERE/android/app/src/main/java/com/iamhere/emergency/safety/JourneyManager.kt): Manages journey sessions, interval check-ins, 60s grace countdown, and automated SOS escalation.
- [FakeCallManager.kt](file:///d:/tushar/I_AM_HERE/android/app/src/main/java/com/iamhere/emergency/safety/FakeCallManager.kt): Manages pending timers, default ringtone playback, vibration waveforms, answered call timers, and tear-down.
- [SafePoiRepository.kt](file:///d:/tushar/I_AM_HERE/android/app/src/main/java/com/iamhere/emergency/safety/SafePoiRepository.kt): Repository of 24/7 safe havens and intent handlers for navigation and telephone calls.
- [FakeCallOverlay.kt](file:///d:/tushar/I_AM_HERE/android/app/src/main/java/com/iamhere/emergency/ui/screens/FakeCallOverlay.kt): Realistic full-screen incoming and active call overlay.
- [SafetyToolsScreen.kt](file:///d:/tushar/I_AM_HERE/android/app/src/main/java/com/iamhere/emergency/ui/screens/SafetyToolsScreen.kt): Comprehensive UI for Walk With Me, Fake Call scheduler, and Nearby Safe Havens.
- [ContactsScreen.kt](file:///d:/tushar/I_AM_HERE/android/app/src/main/java/com/iamhere/emergency/ui/screens/ContactsScreen.kt): Added top sub-navigation between "Trusted Circle" and "Safety Tools".
- [MainActivity.kt](file:///d:/tushar/I_AM_HERE/android/app/src/main/java/com/iamhere/emergency/MainActivity.kt): Embedded `FakeCallOverlay` at application root.
