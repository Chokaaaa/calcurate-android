# Android Crypto Support — Design Spec

**Date:** 2026-05-24
**Branch:** `feature/CalcurateCrypto`
**Goal:** Bring the Android CalcuRate app to crypto-feature parity with the iOS app.

## Context

CalcuRate is a currency/unit/crypto rate calculator. The iOS app (SwiftUI) already supports 8 cryptocurrencies via Binance. The Android app (Kotlin/MVVM) supports ~160 fiat currencies but no crypto. This branch was created for the port; no crypto code exists in it yet.

**Canonical source for behavior parity:**
`/Users/nursultanyelemessov/Desktop/Desktop - Nursultan’s MacBook Pro (2)/Projects/Freelance/Shakhin Zhursinbek/CalcurateApp/CalcuRate 3 decimals/CalcuRateApp/Calcura+e/`

Specifically:
- `Core/CurrencyEnums.swift` — `CryptoChoice` enum + math helpers
- `ViewModal/FetchData.swift` — Binance fetch, USDT-bridge math, caching

## Scope

**In:**
1. Crypto support — 8 coins (BTC, ETH, USDT, BNB, SOL, XRP, ADA, DOGE), Binance API, USDT-bridge math, including crypto-as-base support.
2. Decimal precision fix — change rate formatter from `%.4f` to `%.3f` to match the iOS "3 decimals" build.
3. Drop hardcoded ExchangeRate API key — switch to iOS open endpoint (`https://open.exchangerate-api.com/v6/latest?base={code}`, no key).

**Out (deferred):**
- Room DB migration (SharedPreferences stays)
- App Clip / Instant App equivalent
- Localization
- IAP / paywall
- Analytics / crash reporting

## Architecture decisions

- **Mirror iOS exactly.** Parallel boolean flags per slot (`isBaseCrypto`, `isFirstCrypto`, `isSecondCrypto`) + per-slot crypto code, rather than a sealed `Asset` type. Reasons: lower-risk diff; easier QA against iOS; matches iOS `@AppStorage` shape.
- **Rate map semantics:** `cryptoRates[symbol]` = "amount of crypto per 1 baseCurrency" — same as iOS `FetchData.thirdCryptoRates`.
- **Crypto-base path:** when base slot is a crypto, fetch its USDT price + USD-base fiat rates, then `fiatPerCrypto = priceInUSDT * fiatPerUSD`. Mirrors iOS `fetchCoinbaseBaseRates`.
- **Caching:** SharedPreferences under keys `crypto_rates_v1` and `conversion_data_v1` (JSON via Gson). Mirrors iOS UserDefaults keys.
- **No retry abstraction yet:** iOS has 2-retry exponential backoff. Android will replicate inline in the repository for now; not factoring out unless we need it elsewhere.

## Components

### New files
| Path | Purpose |
|---|---|
| `app/src/main/java/com/thecalcurate/android/model/CryptoItem.kt` | Data class + static list of 8 coins (code, name, symbol, iconResId) |
| `app/src/main/java/com/thecalcurate/android/data/remote/BinanceService.kt` | Retrofit interface for `api/v3/ticker/price` |
| `app/src/main/java/com/thecalcurate/android/data/CryptoRatesCache.kt` | SharedPreferences-backed cache for `Map<String, Double>` |
| `app/src/main/res/drawable/crypto_btc.xml` ... `crypto_doge.xml` | 8 vector drawables for coin icons |

### Modified files
| Path | Change |
|---|---|
| `app/src/main/java/com/thecalcurate/android/data/remote/Network.kt` | Add Binance Retrofit instance; switch fiat base URL to open endpoint |
| `app/src/main/java/com/thecalcurate/android/data/remote/CurrencyService.kt` | Drop API key path segment; switch to `?base={code}` query format |
| `app/src/main/java/com/thecalcurate/android/DataRepository.kt` | Add `fetchCryptoRates(base)` and `fetchCryptoBaseRates(base)` |
| `app/src/main/java/com/thecalcurate/android/viewmodel/CurrencyListViewModel.kt` | Per-slot crypto state; `cryptoRates` LiveData; route base-change to fiat or crypto path |
| `app/src/main/java/com/thecalcurate/android/MainActivity.kt` | Branch icon/code rendering on `isCrypto`; change `getRateStr()` from `%.4f` to `%.3f`; persist new slot keys |
| `app/src/main/java/com/thecalcurate/android/ui/CurrencyDialog.kt` | Two-tab picker (Fiat / Crypto); selection callback gains `isCrypto: Boolean` |
| `app/src/main/res/layout/currency_search.xml` | TabLayout above the existing list |
| `app/src/main/res/layout/activity_main.xml` | If any layout hardcoding referenced fiat-only, generalize (audit during implementation) |

## Data flow

1. **App start** → load cached `cryptoRates` + `conversionData` from SharedPreferences → ViewModel exposes immediately, UI renders with stale data.
2. **Base slot change (fiat → fiat):** `fetchFiatRates(base)` + `fetchCryptoRates(base)` in parallel.
3. **Base slot change (fiat → crypto):** `fetchCryptoBaseRates(base)` + `fetchCryptoRates(base)` in parallel.
4. **Secondary slot change:** existing fiat path reused; if secondary is crypto, read from `cryptoRates`.
5. **Conversion math at render time:**
   - fiat → fiat: `input * rates[targetCode]` (existing)
   - fiat → crypto: `input * cryptoRates[targetCode]` (new — same shape as iOS `convertFromBase`)
   - crypto → fiat: handled via base swap to crypto path above
   - crypto → crypto: `input * cryptoRates[targetCode]` works because rates are already normalized per 1 base

## Error handling

- Match iOS: 2-retry exponential backoff (1s, 2s) on Binance + ExchangeRate calls.
- If Binance fetch fails entirely AND cache is empty: surface error string to ViewModel; UI shows toast/snackbar (TBD — keep silent like iOS does for now).
- If Binance fetch fails but cache exists: keep stale rates, no user-facing error (iOS behavior).
- If ExchangeRate fails on crypto-base path: that conversion shows last-known rate from cache.

## Testing

No existing unit tests in the project. Manual verification plan:
1. App launches, loads from cache, no crash with empty cache.
2. Pick BTC in first slot — value shows in BTC, 3 decimals.
3. Pick BTC in base slot — fiat slots recompute with crypto base.
4. Airplane mode → cached values still display.
5. Compare rate values against iOS app side-by-side for at least 4 conversion combinations.

## Open items / risks

- **Vector drawables vs PNG for crypto icons:** iOS uses PNGs from Assets.xcassets. For Android, vector drawables preferred (single asset, all densities). Can copy iOS PNG assets as fallback if vector sources unavailable.
- **CurrencyDialog tab UI:** existing dialog layout is simple list + search bar. Adding TabLayout shifts other elements; small UX risk that needs visual check in implementation phase.
- **`app/build.gradle` API URL config:** `buildConfigField` currently injects `https://v6.exchangerate-api.com`. Need to update to `https://open.exchangerate-api.com` across dev/stg/prod flavors.
