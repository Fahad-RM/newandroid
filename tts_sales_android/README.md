# TTS Field Sales — Android App

A modern, beautiful native Android application for the **TTS Field Sales** Odoo module.

## ✨ Features

| Module | Description |
|---|---|
| 📊 Dashboard | Sales goal ring chart, 7-day bar chart, quick stats |
| 🛒 Sales Orders | Create, submit for approval, confirm, create invoice |
| 🧾 Invoices | View ZATCA-compliant invoices and credit notes |
| 💰 Payments | Record payments with approval flow |
| 🔄 Returns | Create credit notes with approval |
| 👥 Customers | View, create, manage assigned customers |
| 🗺️ Routes & Visits | GPS check-in/out, visit timer, notes |
| 💸 Expenses | Submit expense claims |
| ⏰ Attendance | GPS check-in/check-out |
| 📋 Day Closing | Daily summary and report |
| 🖨️ Thermal Print | All 8 Odoo templates via Bluetooth |

## 🎨 Design

- **Theme**: Brown & Gold glassmorphic design
- **Fonts**: Poppins + Inter (Google Fonts)
- **Animations**: Smooth Jetpack Compose transitions, ring chart, bar chart
- **Paper**: Supports 3-inch (58mm) and 4-inch (80mm) thermal printers

## 🔧 Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Network**: Retrofit + OkHttp (Odoo JSON-RPC)
- **State**: ViewModel + StateFlow
- **Storage**: DataStore Preferences
- **Printing**: Android WebView → PrintManager → Bluetooth

## 🚀 Build via GitHub Actions

Push to `main` or `develop` and GitHub Actions will auto-build both **Debug** and **Release** APKs. Download from the **Actions** tab → **Artifacts**.

## ⚙️ Configuration

1. Open the app → tap **Settings** (top-right)
2. Enter your **Odoo Server URL** (e.g. `https://yourcompany.odoo.com`)
3. Enter **Database Name** and **Username**
4. Tap **Save Server Settings**
5. Return to login and sign in

## 🖨️ Printer Setup

1. Pair your Bluetooth thermal printer in Android Settings
2. Go to **Settings → Thermal Printer → Setup Bluetooth Printer**
3. Select your printer from the list
4. Choose **3" (58mm)** or **4" (80mm)** paper size
5. Print buttons appear on Orders, Invoices, Payments, Returns screens

## 📦 Requirements

- Android 8.0+ (API 26)
- Bluetooth thermal printer (ESC/POS compatible)
- Odoo 19 with `tts_field_sales` module installed
