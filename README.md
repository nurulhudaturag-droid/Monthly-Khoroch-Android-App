# 📱 Monthly Khoroch --- মাসিক খরচ

**Monthly Khoroch (মাসিক খরচ)** is a lightweight, privacy-focused
monthly expense tracking and budgeting app designed for simple everyday
expense management in Bangla.

The app works primarily with local device storage and does not require a
cloud account, Google Sheets, or a complicated online setup.

This repository contains the full Android app source code
(`monthly-khoroch/`, Jetpack Compose + Room). Ready-to-install APKs are
published on the **Releases** page of this repository, and the app can
update itself directly from those releases.

------------------------------------------------------------------------

## ✨ About the App

**Monthly Khoroch (মাসিক খরচ)** helps you:

-   Set a monthly budget
-   Record daily expenses
-   Track total monthly spending
-   See remaining budget
-   Monitor budget usage percentage
-   Review monthly expense history
-   Search and filter expenses
-   Edit or delete expenses
-   Export and restore your data
-   Use the app offline
-   Manage expenses in a simple Bangla interface

### 💰 Simple Expense Entry

The expense form is intentionally simple:

1.  **পণ্যের নাম (Product Name)**
2.  **পরিমাণ (Quantity)**
3.  **দাম / Amount**

There is **no Unit field** and **no Unit Price field**.

For example:

> Product: আলু\
> Quantity: 2.5\
> Amount: ৳100

The actual expense is **৳100**, not ৳250.

Quantity is informational only and does not affect the financial
calculation.

------------------------------------------------------------------------

## 🔐 Privacy First

Monthly Khoroch is designed as a **local-first and privacy-focused**
personal finance application.

The app is designed without unnecessary:

-   Cloud database
-   Google account login
-   Google Sheets integration
-   Advertising SDKs
-   Analytics/tracking
-   Unnecessary background services
-   Unnecessary permissions

Your everyday expense data is intended to remain under your control on
the device, with manual backup/export available when needed.

------------------------------------------------------------------------

## ⚡ Lightweight & Fast

The app has been optimized for a smooth and responsive experience.

Performance improvements include:

-   Lightweight in-memory caching
-   Room/SQLite local database
-   Room as the single source of truth
-   Background database operations
-   Optimized monthly queries
-   Database indexes
-   Reduced unnecessary database loading
-   Current-month fast-path filtering
-   Optimized Jetpack Compose recomposition
-   Stable list keys
-   Lazy expense lists
-   Reduced startup workload
-   Lightweight animations and UI rendering
-   Reduced unnecessary memory usage

The goal is to keep the app **fast, smooth, lightweight, and
practical**, including on lower-end Android devices.

------------------------------------------------------------------------

# ⬇️ Download & Install

APKs are published on the **Releases** page:

1. Open **Releases** and download `MonthlyKhoroch.apk` from the latest release.
2. Open the downloaded file on your Android phone.
3. Allow install from this source if Android asks, then tap **Install**.

------------------------------------------------------------------------

# 🔄 In-app Updates

The app does not use the Play Store. It checks this repository's latest
release every time it opens (and manually from
**Settings → অ্যাপ আপডেট**):

-   If a newer version exists, a Bangla dialog offers the update.
-   The downloaded APK is verified with SHA-256 against `update.json` before installing.
-   Installation always goes through the Android system installer and requires your confirmation.

------------------------------------------------------------------------

# 🛠️ Build from source

Prerequisites: **JDK 17** and **Android SDK platform 36**.

```sh
cd monthly-khoroch
chmod +x gradlew
./gradlew :app:assembleDebug
```

Output: `monthly-khoroch/app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions runs the unit tests on every push (`ci.yml`). The
**Release APK** workflow (`release.yml`, run manually from the
**Actions** tab) builds the signed release APK and publishes it
together with the `update.json` update manifest.


# 🧾 Backup & Data Safety

Because this is a personal expense application, keeping backups is
recommended.

Use the app's backup/export feature periodically and keep your backup
file somewhere safe.

Before uninstalling the application or changing devices, make sure you
have a current backup.

------------------------------------------------------------------------

# 🛠️ Troubleshooting

## Update dialog does not appear

The dialog only appears when a newer `versionCode` exists on the
Releases page. Check your internet connection, or check again from
**Settings → অ্যাপ আপডেট**.

## Android blocks the APK install

Allow "Install from this source / Unknown apps" for the browser or file
manager you opened the APK from, then install again.


# 📂 Files

``` text
.
├── .github/workflows/   → CI and release workflows
├── monthly-khoroch/     → Android Gradle project (single :app module)
└── README.md
```


# 🎯 Project Goal

**Monthly Khoroch (মাসিক খরচ)** is built with one simple goal:

> **Make personal monthly expense tracking simple, fast, private, and
> easy to use.**

No unnecessary complexity.

Just:

**Budget → Expense Entry → Automatic Tracking → Monthly Overview**

------------------------------------------------------------------------

## 📱 App

**Monthly Khoroch (মাসিক খরচ)**\
*A lightweight, privacy-focused monthly expense tracking and budgeting
app in Bangla.*
