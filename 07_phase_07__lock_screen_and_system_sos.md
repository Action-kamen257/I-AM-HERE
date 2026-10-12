# Phase 07 - Lock Screen and System SOS

> I AM HERE emergency safety application.

## Status

Completed

## Objective

Enable rapid distress activation through Android system surfaces without requiring the user to unlock their phone, including Quick Settings tiles, hardware power button sequences, and home screen widgets.

## Implemented Architecture

### 1. Android Quick Settings Tile (`EmergencyTileService.kt`)
- Extends Android `TileService` with `permission="android.permission.BIND_QUICK_SETTINGS_TILE"`.
- Appears in the top notification pull-down shade even when the device is locked.
- Single tap initiates instant SOS activation and wakes up screen.
- Dynamic tile state reflects active SOS status (`Tile.STATE_ACTIVE` with red glow).

### 2. Hardware Power Button Listener (`PowerButtonSosReceiver.kt`)
- Listens to system broadcasts for `Intent.ACTION_SCREEN_ON` and `Intent.ACTION_SCREEN_OFF`.
- Sliding 3-second window tracks rapid hardware power button presses.
- When 4 rapid toggles occur, immediately triggers emergency SOS and launches `MainActivity` with `FLAG_ACTIVITY_REORDER_TO_FRONT`.

### 3. Lock Screen Visibility & Screen Turn-On
- Configured in `MainActivity`:
  - `setShowWhenLocked(true)` and `setTurnScreenOn(true)` on Android 8.1+ (API 27+).
  - Legacy `FLAG_SHOW_WHEN_LOCKED` and `FLAG_TURN_SCREEN_ON` for older Android releases.
  - Allows victim to view emergency status, dial 112, or resolve SOS directly over lock screen.

### 4. Home Screen 1-Tap Widget (`SosWidgetProvider.kt`)
- Android `AppWidgetProvider` with `widget_sos.xml` and `sos_widget_info.xml`.
- Displays the tactile app logo with "1-TAP SOS" overlay.
- Clicking dispatches an immediate SOS trigger intent.

## Acceptance Criteria

- [x] Quick Settings Tile service implemented and declared in manifest.
- [x] Hardware power button rapid toggle listener operational.
- [x] Lock screen display and screen turn-on capabilities enabled.
- [x] 1-tap home screen emergency widget configured with layout and provider metadata.

## Testing

- Verified Quick Settings Tile lifecycle methods and intent handling.
- Verified PowerButton toggle sliding window algorithm.
- Verified AppWidget layout and configuration XML.

## Completion Rule

Phase 07 acceptance criteria verified. No platform capabilities or delivery states have been invented or assumed without verification.

