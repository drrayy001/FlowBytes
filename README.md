# FlowBytes: Internet Speed & Data Usage Monitor

<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="128" height="128" alt="FlowBytes Logo">
</p>

<p align="center">
  <b>Real-time network speed monitoring with detailed data usage analytics.</b><br>
  Stay in control of your internet with precision and clarity.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%2010%2B-brightgreen.svg" alt="Android 10+">
  <img src="https://img.shields.io/badge/Language-Kotlin-blue.svg" alt="Kotlin">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/Design-Material%203-7b1fa2.svg" alt="Material 3">
  <img src="https://img.shields.io/badge/License-GPLv3-orange.svg" alt="GPLv3">
  <br><br>
  <a href="https://www.paypal.com/paypalme/SPICYH"><img src="https://img.shields.io/badge/Donate-PayPal-00457C.svg?logo=paypal&logoColor=white" alt="Donate with PayPal"></a>
</p>

---

## 📥 Download

Stay in control of your data by downloading FlowBytes from your preferred source:

<p align="center">

<a href="https://github.com/drrayy001/FlowBytes/releases/latest"><img src="https://raw.githubusercontent.com/machiav3lli/oandbackupx/master/badge_github.png" alt="Get it on GitHub" height="80" border="0"></a>
<a href="https://play.google.com/store/apps/details?id=com.ray.flowmeter">
<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="80">
</a>
</p>

## 🚀 Key Features

*   **Real-time Speed Monitoring:** Displays live download and upload speeds in the status bar, persistent notification, and a dedicated **Quick Settings Tile**.
*   **App Blocking:** Advanced network control using Android's **VPN Service** to automatically restrict internet access for applications that reach their limit.
*   **High Data Alerts:** Detects sustained high-traffic spikes and background data leaks with customizable speed and duration thresholds.
*   **Weekly Activity Charts:** Visualize your data consumption with high-precision charts supporting **Soft-Scaling** and filtering by Combined, Wi-Fi, or Mobile data.
*   **Usage Insight & Forecast:** Get projected data consumption forecasts based on your current habits to stay ahead of your daily limits.
*   **Comprehensive Data Plans:** Set flexible **Daily and Monthly System Plans** globally or configure granular **App Limits** for specific applications.
*   **Alerts History:** Track all limit breaches and high-traffic detections in a filterable history log with categorized alerts.
*   **Modern Material 3 UI:** A fluid interface featuring **Material You** dynamic colors, **AMOLED** dark mode, and customizable notification icon scaling.
*   **Privacy First:** 100% offline. No telemetry, no tracking—all network analysis and storage happen strictly on your device.

---

## 📸 Screenshots

<p align="center">
  <i>Screenshots coming soon!</i>
</p>

---

## 🛠 Technical Implementation

FlowBytes is built using modern Android standards with a focus on performance and accuracy:

*   **Network Sampling:** High-frequency traffic analysis using `NetworkStatsManager` and `TrafficStats` for real-time speed calculation and per-app usage tracking.
*   **Traffic Management:** Implementation of `VpnService` for on-device traffic interception, enabling precise per-UID internet restriction.
*   **Background Processing:** Robust `ForegroundService` ensuring continuous monitoring with minimal battery impact and system-wide visibility.
*   **Reactive UI:** Built entirely with **Jetpack Compose** following **MVVM** architecture, utilizing Kotlin Coroutines and StateFlow for seamless data updates.
*   **Data Persistence:** Atomic storage of usage history and configuration using **Room Database** and **Jetpack DataStore**.
*   **Custom Rendering:** Dynamic notification icons and status bar indicators rendered via `Canvas` API for sharp, real-time speed display.

---

## 📋 Requirements

*   **OS:** Android 10 (API level 29) or higher.
*   **Permissions Required:**
    *   `POST_NOTIFICATIONS`: For real-time speed display and urgent alerts.
    *   `PACKAGE_USAGE_STATS`: To accurately calculate per-app network consumption.
    *   `QUERY_ALL_PACKAGES`: To list and monitor all installed applications for usage tracking and blocking.
    *   `FOREGROUND_SERVICE`: To ensure uninterrupted, accurate monitoring.
    *   **VPN Service**: Required for the **App Blocking** feature to intercept and restrict traffic for apps exceeding limits.
    *   **Battery Optimization**: Recommended to exclude FlowBytes from battery restrictions for reliable background tracking.

---

## 📱 How to Use & Customize

1.  **Grant Permissions:** Follow the setup wizard to enable *Usage Access* and *Notifications*.
2.  **Initialize:** Toggle monitoring from the Home screen or the **Quick Settings Tile**.
3.  **Configure Data Plans:**
    *   Go to **Data Plans** to set global **System Plans** (Daily/Monthly).
    *   Add specific applications to **App Limits** for granular control and automatic **App Blocking**.
4.  **Personalize Appearance:**
    *   Enable **Material You** in Settings for system-matched colors.
    *   Toggle **AMOLED Mode** for deep blacks on supported screens.
    *   Adjust **Icon Size** for your preferred status bar visibility.
5.  **Fine-tune Alerts:** Adjust **High Data Alerts** (High Traffic Detection) thresholds in Settings to match your data plan's sensitivity.

---

## ❤️ Support the Project

If FlowBytes helps you stay in control of your data, consider supporting its development!

### 💰 In-App Support
Support the project directly via **Settings > Donate** using Google Play.

### 💳 PayPal
<a href="https://www.paypal.com/paypalme/SPICYH" target="_blank">
  <img src="https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_111x69.jpg" alt="Donate with PayPal" height="50">
</a>

### 🪙 Crypto (USDT)
Address: `TGx1Sbsejmt8e74zy4j4RCwMm9SaT7w1zo` (Network: **TRC20**)

---

## 📄 License

This project is licensed under the **GPLv3 License**. See the [LICENSE](LICENSE) file for details.
