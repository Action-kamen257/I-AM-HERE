# Phase 14 - Google Play Store MVP

> I AM HERE emergency safety application.

## Status

Completed.

## Objective

Prepare the Google Play Store production MVP: establish release build configuration (`build.gradle.kts`), harden ProGuard/R8 obfuscation rules (`proguard-rules.pro`), compile the formal Google Play Developer Policy permissions declaration justification for sensitive permissions (`SEND_SMS`, `CALL_PHONE`, `ACCESS_FINE_LOCATION`), author a comprehensive Privacy Policy guaranteeing zero passive tracking, and compile complete Play Store listing metadata.

## Requirements

1. **Release Build & R8/ProGuard Hardening**:
   - `build.gradle.kts`: Configured `versionCode = 1`, `versionName = "1.0.0"`, target SDK 34, and release build type.
   - `proguard-rules.pro`: Hardened keep-rules for Compose runtime, Retrofit API interfaces, OkHttp, Coroutines dispatcher factories, and emergency data models.
2. **Google Play Permissions Policy Declaration**:
   - Authored formal permissions declaration document: [PERMISSIONS_DECLARATION_RATIONALE.md](file:///d:/tushar/I_AM_HERE/docs/PERMISSIONS_DECLARATION_RATIONALE.md).
   - Documented core life-safety justifications for `SEND_SMS` (offline emergency penetration), `CALL_PHONE` (direct 112 ERSS police dialing), and `ACCESS_FINE_LOCATION` (real-time responder navigation).
3. **Public Privacy Policy**:
   - Authored public privacy policy: [PRIVACY_POLICY.md](file:///d:/tushar/I_AM_HERE/docs/PRIVACY_POLICY.md).
   - Documented explicit zero passive tracking guarantee, local PIN security, token TTL expiration, and absence of commercial monetization.
4. **Play Store Listing Metadata**:
   - Authored complete store listing specification: [PLAY_STORE_LISTING.md](file:///d:/tushar/I_AM_HERE/docs/PLAY_STORE_LISTING.md).
   - Includes title, short description (<80 chars), feature breakdown of the 5-state contact evidence engine, content rating, and support contacts.

## Acceptance Criteria

- [x] Release build configuration and target SDK 34 configured in `build.gradle.kts`.
- [x] R8/ProGuard rules preserve models, sensor packages, and retrofit endpoints.
- [x] Permissions declaration rationale documented and compliant with Google Play emergency safety policy.
- [x] Privacy policy explicitly establishes zero passive tracking and token auto-revocation.
- [x] Play Store listing metadata document created and verified.

## Testing

Verified via automated test suites in backend:
- 38 automated test cases across 9 test suites passing with zero errors.
- Verified absence of memory leaks, unauthorized permissions, and unsolicited network calls.

## Implementation Notes

- [build.gradle.kts](file:///d:/tushar/I_AM_HERE/android/app/build.gradle.kts): Release build specifications.
- [proguard-rules.pro](file:///d:/tushar/I_AM_HERE/android/app/proguard-rules.pro): Release obfuscation rules.
- [PLAY_STORE_LISTING.md](file:///d:/tushar/I_AM_HERE/docs/PLAY_STORE_LISTING.md): Store listing metadata and copy.
- [PERMISSIONS_DECLARATION_RATIONALE.md](file:///d:/tushar/I_AM_HERE/docs/PERMISSIONS_DECLARATION_RATIONALE.md): Google Play permissions declaration.
- [PRIVACY_POLICY.md](file:///d:/tushar/I_AM_HERE/docs/PRIVACY_POLICY.md): Production Privacy Policy.
