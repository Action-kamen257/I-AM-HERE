# 🔄 Automatic Version Update & Continuous Release Protocol

This document outlines the **Automated Version Update & Release Architecture** for the **I AM HERE** emergency system.

---

## 📌 Architecture Overview

Whenever a code change, version bump, or release tag is pushed to the repository, the project automatically:

1. **Builds the Android Application Package (APK)** via GitHub Actions CI/CD.
2. **Publishes the New Version to GitHub Releases** with both versioned (`iamhere-emergency-vX.Y.Z.apk`) and continuous (`iamhere-latest.apk`) binaries.
3. **Updates the In-App Auto-Update Feed** ([`backend/public/api/app/version.json`](../backend/public/api/app/version.json)) with new `versionCode`, `versionName`, and release notes.
4. **Deploys the Live Web App & Redirects** to Firebase Hosting ([`https://iamhere-safety.web.app`](https://iamhere-safety.web.app)).
5. **Notifies Existing Android Devices**: Installed apps automatically discover the update via [`AppUpdateManager.kt`](../android/app/src/main/java/com/iamhere/emergency/update/AppUpdateManager.kt) and prompt the user to install the latest build.

---

## 🚀 How to Trigger a Version Update

### Method 1: Git Release Tag (Recommended)

To release a new version to both the repository and the live web app:

```bash
# 1. Bump version in android/app/build.gradle.kts (versionCode & versionName)
# 2. Commit your changes
git add .
git commit -m "chore(release): bump version to v1.3.0"

# 3. Create and push a git tag
git tag v1.3.0
git push origin v1.3.0
```

GitHub Actions will automatically pick up the tag, build the APK, attach it to GitHub Releases, update the download links, and sync Firebase.

### Method 2: Manual Trigger via GitHub Actions

1. Go to **Actions** in GitHub: `https://github.com/Action-kamen257/I-AM-HERE/actions`.
2. Select **Auto Build, Release & Firebase Deploy**.
3. Click **Run workflow** -> Select `main` branch.

---

## 📱 How Android Devices Auto-Update

The Android application implements [`AppUpdateManager.kt`](../android/app/src/main/java/com/iamhere/emergency/update/AppUpdateManager.kt):

1. **Discovery**: On app launch or manual trigger from Settings, the app calls `GET /api/app/version`.
2. **Comparison**: It compares the remote `versionCode` against the local `currentVersionCode`.
3. **Background Download**: If `remoteVersionCode > currentVersionCode`, it downloads the APK from `/api/app/latest.apk` (or GitHub Releases) to the local cache.
4. **Silent Verification**: Verifies package integrity and triggers the Android `ACTION_VIEW` package installer prompt.

---

## 🌐 Public Distribution Endpoints

| Endpoint | Target / Behavior |
|---|---|
| `https://iamhere-safety.web.app/iamhere-emergency.apk` | 302 Redirect to latest GitHub Release APK |
| `https://iamhere-safety.web.app/api/app/latest.apk` | 302 Redirect to `iamhere-latest.apk` |
| `https://iamhere-safety.web.app/api/app/version` | Returns latest version metadata JSON |
| `https://github.com/Action-kamen257/I-AM-HERE/releases/latest` | GitHub Releases download page |
