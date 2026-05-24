# CalcuRate

A fast, offline-friendly Android calculator that converts between fiat currencies and cryptocurrencies in real time.

![Min SDK](https://img.shields.io/badge/Min%20SDK-33-blue)
![Target SDK](https://img.shields.io/badge/Target%20SDK-34-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-1.8.0-7F52FF?logo=kotlin)
![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)

## Features

- **3-slot conversion** — type once, see the value in your base currency and two secondary currencies simultaneously.
- **170+ fiat currencies** sourced from the [open ExchangeRate API](https://www.exchangerate-api.com/) (no API key required).
- **8 cryptocurrencies** with live prices from Binance: BTC, ETH, USDT, BNB, SOL, XRP, ADA, DOGE.
- **Crypto-as-base support** — pick a coin as your base and see fiat values for it.
- **Smart formatting** — fiat rounded to 3 decimals; crypto uses up to 8 decimals with trailing zeros trimmed.
- **Offline cache** — rates persist in SharedPreferences; stale values shown immediately on cold start while a fresh fetch runs in the background.
- **Connectivity awareness** — top banner shows red when offline, yellow on metered connections, auto-collapses after 15 seconds.
- **Long-press picker** — hold any slot to open a search-enabled picker; opens on the segment (Currencies / Crypto) matching the slot's current type.
- **Tactile feedback** — haptic pulse on long-press, press animation on calculator keys.
- **Tutorial** — onboarding gestures shown on first launch.

## Tech stack

- **Language:** 100% Kotlin
- **Architecture:** MVVM with LiveData
- **Networking:** Retrofit 2 + OkHttp + Coroutines
- **UI:** XML layouts + Data Binding (no Jetpack Compose)
- **Caching:** SharedPreferences + Gson
- **Connectivity:** `ConnectivityManager.NetworkCallback`
- **Build:** Gradle 8.2, AGP 8.2.2, JDK 17+

## Build flavors

| Flavor | Application ID | Notes |
|---|---|---|
| `dev` | `com.thecalcurate.android.dev` | Development build, suffixed app ID so it installs alongside other variants. |
| `stg` | `com.thecalcurate.android.stg` | Staging. |
| `prod` | `com.thecalcurate.android` | Production. |

## Running locally

```bash
# Open in Android Studio (Iguana or newer) — opens project root.
# Or build from the command line:
./gradlew :app:assembleDevDebug      # dev flavor, debug
./gradlew :app:installDevDebug        # install to connected device
./gradlew :app:assembleProdDebug      # production app id, debug signed (shareable APK)
```

JDK 17 is required for AGP 8.2.2. If you have Android Studio installed, you can point Gradle at its bundled JBR:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

The connected device must run **Android 13 (API 33)** or newer.

## Project structure

```
app/
├── src/main/
│   ├── java/com/thecalcurate/android/
│   │   ├── MainActivity.kt              # calculator screen + slot handling
│   │   ├── DataRepository.kt            # repository: fiat + crypto fetch logic
│   │   ├── TutorialActivity.kt
│   │   ├── data/
│   │   │   ├── CryptoRatesCache.kt      # SharedPreferences cache for crypto rates
│   │   │   └── NetworkMonitor.kt        # ConnectivityManager wrapper
│   │   ├── data/remote/
│   │   │   ├── CurrencyService.kt       # Retrofit interface (open ExchangeRate API)
│   │   │   ├── BinanceService.kt        # Retrofit interface (Binance ticker price)
│   │   │   └── Network.kt
│   │   ├── model/
│   │   │   ├── CurrencyItem.kt          # fiat row model
│   │   │   ├── CryptoItem.kt            # crypto row model
│   │   │   └── CurrencyListResponse.kt
│   │   ├── ui/
│   │   │   ├── CurrencyDialog.kt        # long-press picker
│   │   │   ├── CurrencyRecyclerViewAdapter.kt
│   │   │   ├── CurrencyButton.kt        # flag drawable mapping
│   │   │   ├── MainTextView.kt
│   │   │   └── OnSwipeListener.kt
│   │   └── viewmodel/
│   │       └── CurrencyListViewModel.kt
│   └── res/
│       ├── drawable/                    # ~160 fiat flag PNGs + 8 crypto icons + UI shapes
│       └── layout/
└── build.gradle
```

## License

[MIT](LICENSE) © Chokaaaa
