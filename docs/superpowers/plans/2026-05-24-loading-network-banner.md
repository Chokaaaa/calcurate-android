# Loading Spinner & Network Warning Banner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add (1) a cold-start "Loading rates..." spinner overlay that mirrors iOS exactly, and (2) a top network-status banner with red (no internet) / yellow (metered = "bad") / hidden states that auto-collapses after 15s — matching iOS `networkMonitor.swift` behavior.

**Architecture:** New `NetworkMonitor` Kotlin class wraps `ConnectivityManager.NetworkCallback` and exposes a `LiveData<NetworkQuality>` (NONE / METERED / HEALTHY). MainActivity observes it and toggles a banner XML overlay. A separate loading-overlay XML view is shown on `onCreate`, hidden when the first fetch completes OR after a 5s timeout (whichever first). All changes are additive to existing screens; no refactor of the calc/picker work.

**Tech Stack:** Kotlin, Android `ConnectivityManager` (API 23+), `Handler.postDelayed`, AndroidX LiveData, ConstraintLayout overlays. No new dependencies.

**Reference (iOS source for parity):**
- Loading spinner: `Calcura+e/View/Home.swift:32, 537-565`
- Network banner: `Calcura+e/View/networkMonitor.swift` + `Home.swift:~270-320, ~1380-1420`

**Branch:** continue on `feature/CalcurateCrypto`.

**Per-phase workflow:** build dev debug → commit → invoke `code-review:code-review` skill → fix any CONFIRMED/PLAUSIBLE finding as an interstitial fix BEFORE the next phase (per saved feedback memory).

**Permissions:** `ACCESS_NETWORK_STATE` already declared in `AndroidManifest.xml:6` — no manifest changes needed.

---

## Phase 1: NetworkMonitor utility

**Goal:** Encapsulate Android network-state detection in one class with a LiveData stream. Defines 3 states matching iOS `ConnectivityQuality` enum (`noConnection`, `bad`, `okay`/`great` collapsed into `HEALTHY` since iOS only renders the negative two visually).

**Files:**
- Create: `app/src/main/java/com/thecalcurate/android/data/NetworkMonitor.kt`

- [ ] **Step 1.1: Create NetworkMonitor**

Create `app/src/main/java/com/thecalcurate/android/data/NetworkMonitor.kt`:

```kotlin
package com.thecalcurate.android.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Mirrors iOS networkMonitor.swift's ConnectivityQuality enum:
 *  - NONE  = no internet (iOS .noConnection → red banner)
 *  - METERED = constrained connection like cellular/Data Saver (iOS .bad → yellow banner)
 *  - HEALTHY = unmetered + connected (iOS .okay/.great → no banner)
 */
enum class NetworkQuality { NONE, METERED, HEALTHY }

class NetworkMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _quality = MutableLiveData<NetworkQuality>(currentQuality())
    val quality: LiveData<NetworkQuality> get() = _quality

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { update() }
        override fun onLost(network: Network) { update() }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { update() }
        override fun onUnavailable() { update() }
    }

    fun start() {
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(req, callback)
    }

    fun stop() {
        try { cm.unregisterNetworkCallback(callback) } catch (e: IllegalArgumentException) { /* not registered */ }
    }

    private fun update() {
        val q = currentQuality()
        _quality.postValue(q)
    }

    private fun currentQuality(): NetworkQuality {
        val net = cm.activeNetwork ?: return NetworkQuality.NONE
        val caps = cm.getNetworkCapabilities(net) ?: return NetworkQuality.NONE
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return NetworkQuality.NONE
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) return NetworkQuality.NONE
        // NET_CAPABILITY_NOT_METERED absent → metered (cellular, hotspot, Data Saver) → iOS "bad"
        return if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
            NetworkQuality.HEALTHY
        } else {
            NetworkQuality.METERED
        }
    }
}
```

- [ ] **Step 1.2: Build dev debug**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 1.3: Commit**

```bash
git add app/src/main/java/com/thecalcurate/android/data/NetworkMonitor.kt
git commit -m "feat(network): NetworkMonitor LiveData wrapper for ConnectivityManager.NetworkCallback"
```

- [ ] **Step 1.4: Code-review checkpoint**

Invoke `code-review:code-review`.

---

## Phase 2: Network banner UI

**Goal:** Add the banner XML overlay at the top of `activity_main.xml`. Two banner colors (red / yellow), an icon, a title, and a description. Initially GONE.

**Files:**
- Create: `app/src/main/res/drawable/banner_bg_red.xml`
- Create: `app/src/main/res/drawable/banner_bg_yellow.xml`
- Modify: `app/src/main/res/values/colors.xml` (add banner shades if needed)
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/layout/activity_main.xml`

- [ ] **Step 2.1: Add string resources**

Edit `app/src/main/res/values/strings.xml`. Add inside `<resources>`:

```xml
    <string name="banner_no_connection_title">No Network Connection</string>
    <string name="banner_no_connection_short">No Connection</string>
    <string name="banner_no_connection_desc">Connect to the internet to fetch live rates.</string>
    <string name="banner_bad_title">Bad Connectivity</string>
    <string name="banner_bad_short">Poor Connectivity</string>
    <string name="banner_bad_desc">Your network is constrained — rates may be slow to refresh.</string>
```

- [ ] **Step 2.2: Add banner background drawables**

Create `app/src/main/res/drawable/banner_bg_red.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#D9342B" />
</shape>
```

Create `app/src/main/res/drawable/banner_bg_yellow.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#F5C518" />
</shape>
```

- [ ] **Step 2.3: Add the banner overlay block to activity_main.xml**

Edit `app/src/main/res/layout/activity_main.xml`. Add this block as the LAST child of the root ConstraintLayout, right before the closing `</androidx.constraintlayout.widget.ConstraintLayout>` (line 486):

```xml
    <LinearLayout
        android:id="@+id/networkBanner"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@drawable/banner_bg_red"
        android:elevation="8dp"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:paddingStart="16dp"
        android:paddingTop="12dp"
        android:paddingEnd="16dp"
        android:paddingBottom="12dp"
        android:visibility="gone"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        tools:visibility="visible">

        <androidx.appcompat.widget.AppCompatImageView
            android:id="@+id/bannerIcon"
            android:layout_width="20dp"
            android:layout_height="20dp"
            android:layout_marginEnd="12dp"
            android:src="@android:drawable/stat_sys_warning"
            app:tint="@color/white" />

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical">

            <androidx.appcompat.widget.AppCompatTextView
                android:id="@+id/bannerTitle"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/banner_no_connection_title"
                android:textColor="@color/white"
                android:textSize="14sp"
                android:textStyle="bold"
                tools:text="No Network Connection" />

            <androidx.appcompat.widget.AppCompatTextView
                android:id="@+id/bannerDesc"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="2dp"
                android:text="@string/banner_no_connection_desc"
                android:textColor="@color/white"
                android:textSize="12sp"
                tools:text="Connect to the internet to fetch live rates." />
        </LinearLayout>
    </LinearLayout>
```

- [ ] **Step 2.4: Build dev debug**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2.5: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/drawable/banner_bg_red.xml app/src/main/res/drawable/banner_bg_yellow.xml app/src/main/res/layout/activity_main.xml
git commit -m "feat(network): top banner UI overlay (initially hidden) for connectivity warnings"
```

- [ ] **Step 2.6: Code-review checkpoint**

Invoke `code-review:code-review`.

---

## Phase 3: Wire banner to NetworkMonitor

**Goal:** Instantiate `NetworkMonitor` in `MainActivity`, observe `quality`, and toggle the banner's color/text/visibility. Auto-collapse the description text after 15 seconds; tap the banner to re-expand.

**Files:**
- Modify: `app/src/main/java/com/thecalcurate/android/MainActivity.kt`

- [ ] **Step 3.1: Add field and lifecycle hooks**

Edit `MainActivity.kt`. Near the other field declarations (around line 91 where `viewModel` lives), add:

```kotlin
    private lateinit var networkMonitor: com.thecalcurate.android.data.NetworkMonitor
    private val bannerCollapseHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var bannerCollapseRunnable: Runnable? = null
```

- [ ] **Step 3.2: Initialize and observe in onCreate**

In `onCreate`, after `viewModel.cryptoRates.observe(this) { ... }` (around line 519 after the existing crypto observer added in earlier work), add:

```kotlin
        networkMonitor = com.thecalcurate.android.data.NetworkMonitor(this)
        networkMonitor.start()
        networkMonitor.quality.observe(this) { quality ->
            applyNetworkBanner(quality)
        }

        // Tap collapsed banner → re-expand description for another 15s.
        findViewById<View>(R.id.networkBanner).setOnClickListener {
            findViewById<View>(R.id.bannerDesc).visibility = View.VISIBLE
            scheduleBannerCollapse()
        }
```

- [ ] **Step 3.3: Stop monitor in onDestroy**

Override `onDestroy` near the bottom of the class. If `onDestroy` already exists, add the line inside it. Otherwise append:

```kotlin
    override fun onDestroy() {
        super.onDestroy()
        if (this::networkMonitor.isInitialized) networkMonitor.stop()
        bannerCollapseRunnable?.let { bannerCollapseHandler.removeCallbacks(it) }
    }
```

(If `onDestroy` already exists, just add the two new lines inside it.)

- [ ] **Step 3.4: Add applyNetworkBanner + scheduleBannerCollapse helpers**

Add these private functions inside `MainActivity` (near other helpers like `hapticHeavy()`):

```kotlin
    private fun applyNetworkBanner(quality: com.thecalcurate.android.data.NetworkQuality) {
        val banner = findViewById<View>(R.id.networkBanner)
        val title = findViewById<android.widget.TextView>(R.id.bannerTitle)
        val desc = findViewById<android.widget.TextView>(R.id.bannerDesc)
        when (quality) {
            com.thecalcurate.android.data.NetworkQuality.NONE -> {
                banner.setBackgroundResource(R.drawable.banner_bg_red)
                title.text = getString(R.string.banner_no_connection_title)
                desc.text = getString(R.string.banner_no_connection_desc)
                title.setTextColor(android.graphics.Color.WHITE)
                desc.setTextColor(android.graphics.Color.WHITE)
                desc.visibility = View.VISIBLE
                banner.visibility = View.VISIBLE
                scheduleBannerCollapse()
            }
            com.thecalcurate.android.data.NetworkQuality.METERED -> {
                banner.setBackgroundResource(R.drawable.banner_bg_yellow)
                title.text = getString(R.string.banner_bad_title)
                desc.text = getString(R.string.banner_bad_desc)
                // iOS yellow banner uses black text for contrast.
                title.setTextColor(android.graphics.Color.BLACK)
                desc.setTextColor(android.graphics.Color.BLACK)
                desc.visibility = View.VISIBLE
                banner.visibility = View.VISIBLE
                scheduleBannerCollapse()
            }
            com.thecalcurate.android.data.NetworkQuality.HEALTHY -> {
                banner.visibility = View.GONE
                bannerCollapseRunnable?.let { bannerCollapseHandler.removeCallbacks(it) }
            }
        }
    }

    private fun scheduleBannerCollapse() {
        bannerCollapseRunnable?.let { bannerCollapseHandler.removeCallbacks(it) }
        val r = Runnable {
            findViewById<View>(R.id.bannerDesc).visibility = View.GONE
        }
        bannerCollapseRunnable = r
        bannerCollapseHandler.postDelayed(r, 15_000)
    }
```

- [ ] **Step 3.5: Build dev debug**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3.6: Commit**

```bash
git add app/src/main/java/com/thecalcurate/android/MainActivity.kt
git commit -m "feat(network): wire NetworkMonitor to top banner with 15s auto-collapse + tap-to-expand"
```

- [ ] **Step 3.7: Code-review checkpoint**

Invoke `code-review:code-review`. Concerns: leak from observer if onDestroy doesn't fire; collapse handler scheduled multiple times racing.

---

## Phase 4: Loading overlay UI

**Goal:** Add a full-screen scrim + centered card with circular indeterminate ProgressBar and "Loading rates..." text. Initially GONE in XML; MainActivity shows it on app launch.

**Files:**
- Create: `app/src/main/res/drawable/loading_card_bg.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/layout/activity_main.xml`

- [ ] **Step 4.1: Add string**

Edit `app/src/main/res/values/strings.xml`. Add inside `<resources>`:

```xml
    <string name="loading_rates">Loading rates…</string>
```

- [ ] **Step 4.2: Add loading-card background drawable**

Create `app/src/main/res/drawable/loading_card_bg.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#99000000" />
    <corners android:radius="12dp" />
</shape>
```

- [ ] **Step 4.3: Add the loading overlay block to activity_main.xml**

Edit `app/src/main/res/layout/activity_main.xml`. Add this block AFTER the `networkBanner` block from Phase 2, still inside the root ConstraintLayout. Banner stays on top of overlay because banner has `android:elevation="8dp"` (higher) and overlay has none, but they're both visible at z-order based on declaration. Loading overlay covers the WHOLE screen including (technically) under the banner — that's fine, the banner remains visible on top.

```xml
    <FrameLayout
        android:id="@+id/loadingOverlay"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#4D000000"
        android:clickable="true"
        android:focusable="true"
        android:visibility="gone"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        tools:visibility="visible">

        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:background="@drawable/loading_card_bg"
            android:gravity="center"
            android:orientation="vertical"
            android:padding="24dp">

            <ProgressBar
                android:id="@+id/loadingSpinner"
                android:layout_width="48dp"
                android:layout_height="48dp"
                android:indeterminateTint="@color/white" />

            <androidx.appcompat.widget.AppCompatTextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:text="@string/loading_rates"
                android:textColor="@color/white"
                android:textSize="12sp" />
        </LinearLayout>
    </FrameLayout>
```

`android:clickable="true"` ensures the overlay blocks touches to the calculator beneath it (matches iOS behaviour where the spinner overlay swallows interactions).

- [ ] **Step 4.4: Build dev debug**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4.5: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/drawable/loading_card_bg.xml app/src/main/res/layout/activity_main.xml
git commit -m "feat(loading): scrim + rounded card with ProgressBar + 'Loading rates…' label (initially hidden)"
```

- [ ] **Step 4.6: Code-review checkpoint**

Invoke `code-review:code-review`.

---

## Phase 5: Trigger spinner on cold start

**Goal:** On `onCreate`, show the loading overlay. Hide it when the main slot's first fetch lands (via observing `currencyMainList`) OR after a 5s timeout — whichever comes first. Matches iOS `Home.swift:537-548`.

**Files:**
- Modify: `app/src/main/java/com/thecalcurate/android/MainActivity.kt`

- [ ] **Step 5.1: Add fields**

Near the existing `bannerCollapseHandler` field (added in Phase 3), add:

```kotlin
    private val loadingTimeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var loadingTimeoutRunnable: Runnable? = null
    private var loadingDismissed: Boolean = false
```

- [ ] **Step 5.2: Show overlay early in onCreate**

In `onCreate`, immediately after `setContentView(R.layout.activity_main)` (around line 488), add:

```kotlin
        // Cold-start loading overlay (iOS Home.swift:537-548). Mirrors iOS 5s timeout.
        findViewById<View>(R.id.loadingOverlay).visibility = View.VISIBLE
        loadingTimeoutRunnable = Runnable { dismissLoadingOverlay() }
        loadingTimeoutHandler.postDelayed(loadingTimeoutRunnable!!, 5_000)
```

- [ ] **Step 5.3: Dismiss overlay when main rates land**

Find the existing `viewModel.cryptoRates.observe(this) { ... }` block (Phase 8 of the original crypto work). Right BEFORE it, add an observer on `currencyMainList`:

```kotlin
        viewModel.currencyMainList.observe(this) { list ->
            // First non-zero list emission means the main fetch landed.
            if (!loadingDismissed && list != null && list.any { it.rate != .0 }) {
                dismissLoadingOverlay()
            }
        }
```

- [ ] **Step 5.4: Add dismissLoadingOverlay helper**

Add this private function near `applyNetworkBanner` from Phase 3:

```kotlin
    private fun dismissLoadingOverlay() {
        if (loadingDismissed) return
        loadingDismissed = true
        findViewById<View>(R.id.loadingOverlay).visibility = View.GONE
        loadingTimeoutRunnable?.let { loadingTimeoutHandler.removeCallbacks(it) }
    }
```

- [ ] **Step 5.5: Cancel timeout in onDestroy**

In the existing `onDestroy` (added in Phase 3), add to the body:

```kotlin
        loadingTimeoutRunnable?.let { loadingTimeoutHandler.removeCallbacks(it) }
```

- [ ] **Step 5.6: Build dev debug**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5.7: Commit**

```bash
git add app/src/main/java/com/thecalcurate/android/MainActivity.kt
git commit -m "feat(loading): show overlay on cold start, dismiss on first main-rate land or 5s timeout"
```

- [ ] **Step 5.8: Code-review checkpoint**

Invoke `code-review:code-review`. Concerns: observer firing on cached-rate prefetch before fetch lands (any cache hit would dismiss instantly — check whether this is desired); handler leaking if onDestroy doesn't fire.

---

## Self-review notes

Spec coverage:
- iOS spinner trigger (cold start, all fetches, 5s timeout) → Phase 4 + Phase 5 ✓
- iOS banner 3 states (red/yellow/hidden) → Phase 1 + Phase 2 + Phase 3 ✓
- iOS banner auto-collapse 15s + tap-to-expand → Phase 3 ✓
- iOS bad-conn = isConstrained → Android metered (`NET_CAPABILITY_NOT_METERED` negated) → Phase 1 ✓

Risk areas:
- **Phase 5 Step 5.3 (observer firing on cache):** The repo's ViewModel sets `currencyMainList` with cached `CurrencyItem.getList()` (rate=0) BEFORE the network fetch returns. The observer only dismisses when a row has non-zero rate, so we correctly wait for the network. Verify in smoke testing that cached crypto rates from `CryptoRatesCache` don't cause an early dismiss (they populate `cryptoRates` LiveData, not `currencyMainList`).
- **NetworkMonitor double-registration:** If `MainActivity.onCreate` runs twice for some reason (config change without `android:configChanges`), we'd register the callback twice. The activity already has `configChanges="orientation|screenSize"` per AndroidManifest, so this is unlikely. `stop()` in `onDestroy` handles cleanup.
- **First state on cold launch:** `NetworkMonitor` evaluates `currentQuality()` in init, so the LiveData has a valid initial value. The MainActivity observer fires immediately on observe with that value, applying the banner if needed.

Out of scope (deferred):
- "Bad" connection heuristic beyond metered (e.g., last fetch slow)
- Animated slide-in/out of the banner
- Banner accessibility (TalkBack live region)
- Per-slot loading indicators (iOS doesn't have them either)
