# 📸 ReceiptScannerApp

An AI-powered Android app to scan and manage receipts. Upload images of receipts, extract key information using OCR and NLP embeddings, store data locally, and gain insights with summaries and smart spending tips.

[![Made with Kotlin](https://img.shields.io/badge/Made%20with-Kotlin-blueviolet)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack-Compose-blue)](https://developer.android.com/jetpack/compose)

---

## ✨ Features

- 📤 Upload receipt as an image (PDF support coming soon)
- 🔍 Extract text using Google ML Kit OCR
- 💾 Store items and totals in a local Room database
- 🔎 Semantic search using Hugging Face embeddings
- 🌓 Dark mode support
- 📊 Monthly spending summary with bar charts
- 💡 Smart monthly saving tips based on spending habits
- 🔐 Biometric login (Fingerprint authentication)

---

## 🛠️ Built With

- **Jetpack Compose** – UI toolkit
- **Room DB** – Local data persistence
- **ML Kit (Text Recognition)** – OCR for extracting receipt data
- **Hugging Face Embeddings API** – Semantic similarity in search
- **Navigation Component** – Seamless screen transitions
- **Material 3 Design** – UI/UX styling
- **Fingerprint API** – Secure biometric login

---

## 🚀 Getting Started

### 1. Clone the repo

```bash
git clone https://github.com/PremPanchal1224/ReceiptScannerApp.git
cd ReceiptScannerApp

Add Hugging Face API Key
This key is used for generating semantic embeddings for receipt search.

Add to local.properties
HF_API_KEY=your_huggingface_api_key

Then access it in EmbeddingUtil.kt like this:
val apiKey = BuildConfig.HF_API_KEY

Make sure your build.gradle includes:
buildConfigField "String", "HF_API_KEY", "\"${project.properties['HF_API_KEY']}\""

