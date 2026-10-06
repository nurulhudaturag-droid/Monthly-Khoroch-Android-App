# 📱 Monthly Khoroch (মাসিক খরচ)

A lightweight, privacy-focused **monthly expense & budget tracker** in Bangla.
No Play Store, no Google account, no cloud — all data stays on your device,
and the app can update itself directly from this repo's **Releases**.

## ✨ Features

- Monthly budget + spending tracker
- Quick expense entry (**নাম / পরিমাণ / দাম** — quantity never multiplies the amount)
- Monthly overview, remaining budget & usage %
- Expense history, search, filter, edit, delete
- Export / restore backup
- 100% offline, Bangla interface

## ⬇️ Download & Install

Open the latest release on your **phone's browser**:

```
https://github.com/nurulhudaturag-droid/Monthly-Khoroch-Android-App/releases/latest
```

Or grab the APK directly:

```
https://github.com/nurulhudaturag-droid/Monthly-Khoroch-Android-App/releases/latest/download/MonthlyKhoroch.apk
```

1. Download `MonthlyKhoroch.apk`
2. Open it → **Install** (allow "install from this source" if asked)
3. Open **Monthly Khoroch**

Future versions can also be installed from inside the app
(**Settings → অ্যাপ আপডেট → আপডেট চেক করুন**).

## 🧪 Verification

Wireless in-app downloads are verified with **SHA-256** against `update.json`
before installing — and every release is tested by repository CI before it ships.

## 🛠️ Build from source

Prerequisites: **JDK 17+** and **Android SDK 36**.

```sh
cd monthly-khoroch
chmod +x gradlew
./gradlew :app:assembleDebug
```

## 💾 Data safety

Keep a backup: use **Export** in the app periodically, especially before
uninstalling or changing devices.

## ❓ Troubleshooting

- **Update dialog not showing?** A dialog only appears if the release has a
  higher `versionCode`. Re-check from **Settings → অ্যাপ আপডেট**.
- **Android blocks the APK?** Allow "install unknown apps" for your browser,
  then install again.

---

## 📂 Repository

```text
monthly-khoroch/     Android app (Jetpack Compose + Room, single :app)
.github/workflows/   CI (tests) + Release APK (signed build + update.json)
```

*Made simple: **Budget → Expense Entry → Automatic Tracking → Monthly Overview***.