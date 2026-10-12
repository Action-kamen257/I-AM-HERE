# Phase 10 - Motion and Shake Detection

> I AM HERE emergency safety application.

## Status

Completed.

## Objective

Implement physics-based vigorous shake detection and fall/hard impact detection using the Android hardware Accelerometer, an unmissable 10-second cancelable false-alarm interceptor countdown with auditory/vibratory alerts, customizable sensitivity controls in settings, and a validated feature extraction pipeline adhering strictly to the project training rule.

## Requirements

1. **Hardware Accelerometer Integration**:
   - Continuous vector magnitude calculation: $a = \sqrt{x^2 + y^2 + z^2} / 9.80665$.
   - Direction reversal tracking to eliminate casual motion (e.g., walking, jogging, car potholes).
   - Require minimum 3-4 acceleration direction reversals within 700-900ms above threshold.
   - Sensitivity configurations: Low ($3.2g$), Medium ($2.7g$), High ($2.2g$).
2. **Fall & Hard Impact Detection**:
   - Freefall detection: Low gravity dip ($a < 0.25g$) lasting $\ge 120\text{ms}$.
   - Followed within $600\text{ms}$ by a sharp impact peak ($a \ge 3.5g$).
3. **False-Alarm Interception Window**:
   - On shake or fall detection, an immediate 10-second countdown overlay is displayed across the app.
   - Warning tone beeps and haptic pulses fire on each second.
   - Prominent "I AM SAFE - CANCEL SOS" tactile button immediately cancels distress.
   - If timer reaches 0 without cancellation, full SOS immediately triggers.
4. **Machine Learning Pipeline Safety (Training Rule Compliance)**:
   - "If future ML is introduced for motion detection, never automatically start training. Prepare and validate the pipeline, then stop and report that manual training is ready."
   - Implemented `MotionFeatureExtractor.kt` calculating a 10-dimensional statistical/spectral proxy feature vector (mean magnitude, std dev, peak-to-peak, axis means, variance energy, zero-crossing rate, max jerk, dominant axis).
   - Validated pipeline readiness via `validatePipeline()` without triggering any automated training routines.

## Acceptance Criteria

- [x] Shake detection filters casual motion and walking under $1.5g$.
- [x] Multi-directional vigorous shake confirms at $\ge 2.7g$ with 3 reversals within $800\text{ms}$.
- [x] Fall detection registers freefall dip followed by impact spike.
- [x] 10-second interceptor countdown allows user to cancel false alarms before SMS/SOS dispatch.
- [x] Feature extraction generates 10-dimensional non-NaN feature vectors.
- [x] Zero automated ML training is executed; manual training readiness is verified and reported.
- [x] Settings screen offers Shake toggle, Sensitivity selector (Low/Medium/High), and Impact toggle.

## Testing

Verified via automated test suite `src/__tests__/motion_and_shake.test.ts`:
- `rejects casual motion or walking (under threshold)` (PASS)
- `detects vigorous multi-directional shake (3 reversals at > 2.7g within 800ms)` (PASS)
- `detects freefall drop followed by hard impact` (PASS)
- `computes 10-dimensional feature vector without NaN or Infinity` (PASS)
- `enforces training rule: validates pipeline readiness without starting automated training` (PASS)
- `initiates SOS with motion trigger reason and records trigger source` (PASS)

## Implementation Notes

- [ShakeDetector.kt](file:///d:/tushar/I_AM_HERE/android/app/src/main/java/com/iamhere/emergency/sensor/ShakeDetector.kt): SensorEventListener measuring g-force, direction reversals, and impact dynamics.
- [MotionFeatureExtractor.kt](file:///d:/tushar/I_AM_HERE/android/app/src/main/java/com/iamhere/emergency/sensor/MotionFeatureExtractor.kt): Sliding window buffer producing 10-D feature vectors.
- [EmergencyManager.kt](file:///d:/tushar/I_AM_HERE/android/app/src/main/java/com/iamhere/emergency/data/repository/EmergencyManager.kt): Integrated shake listener, 10s countdown coroutine, warning sound & haptics.
- [MainActivity.kt](file:///d:/tushar/I_AM_HERE/android/app/src/main/java/com/iamhere/emergency/MainActivity.kt): Root false-alarm interceptor UI overlay with high-contrast cancel button.
- [SettingsScreen.kt](file:///d:/tushar/I_AM_HERE/android/app/src/main/java/com/iamhere/emergency/ui/screens/SettingsScreen.kt): UI controls for sensitivity and simulation test trigger.
