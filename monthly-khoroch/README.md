# Monthly Khoroch (মাসিক খরচ)

A lightweight, privacy-focused monthly expense tracking and budgeting app with a Bangla UI, built with Jetpack Compose and Room.

## Run locally

**Prerequisites:** JDK 17, Android SDK platform 36 (and Android Studio, optional).

1. Open the `monthly-khoroch/` directory (this is the Gradle project root).
2. Generate the debug keystore once (the build expects it at the project root):

   ```sh
   keytool -genkey -v -keystore debug.keystore -storepass android \
     -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 \
     -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
   ```

3. Build a debug APK:

   ```sh
   chmod +x gradlew
   ./gradlew :app:assembleDebug
   ```

   Output: `app/build/outputs/apk/debug/app-debug.apk`
4. Run the app on an emulator or a physical device.

## CI (GitHub Actions)

Workflows live in `.github/workflows/` at the repository root.

- **CI** (`ci.yml`) — runs `:app:testDebugUnitTest` on every push/PR to `main`/`master`, plus advisory lint. No secrets needed.
- **Release APK** (`release.yml`) — run manually from the **Actions** tab. Builds a **signed release APK**, publishes a GitHub Release containing:
  - `MonthlyKhoroch.apk`
  - `update.json` (versionCode, versionName, apkUrl, sha256, notes)

## Self-update flow (no Google Play)

1. The app checks `https://github.com/nurulhudaturag-droid/Monthly-Khoroch-Android-App/releases/latest/download/update.json` each time it opens (silently skips on failure), and also from **Settings → অ্যাপ আপডেট → আপডেট চেক করুন**.
2. If the release `versionCode` is higher than the installed one, a Bangla dialog appears.
3. On **আপডেট করুন** the APK is downloaded to app cache, its **sha256 is verified** against `update.json`, then the **system installer** opens (user confirms; never a silent install).

### One-time release setup (manual, done by the repo owner)

1. Create the release keystore (alias **must** be `upload`):

   ```sh
   keytool -genkey -v -keystore upload-keystore.jks -storepass <choose> \
     -alias upload -keypass <choose> -keyalg RSA -keysize 2048 \
     -validity 10000 -dname "CN=Monthly Khoroch,O=Dev,C=BD"
   ```

2. Add these repository **Settings → Secrets and variables → Actions** secrets (names only):

   | Secret | Value |
   |---|---|
   | `KEYSTORE_BASE64` | `base64 -w0 upload-keystore.jks` output |
   | `STORE_PASSWORD` | keystore password |
   | `KEY_PASSWORD` | key password |

3. Each release: **Actions → Release APK → Run workflow**, fill `versionName` (e.g. `1.1`), `versionCode` (e.g. `2`, always increasing), and Bangla release notes.

Notes:
- Keep `upload-keystore.jks` safe and out of git (it is not gitignored — never commit it).
- Old installs only update if they were installed with the **same** release keystore.
