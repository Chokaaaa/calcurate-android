# Picker iOS-Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring the currency/crypto picker dialog to visual parity with the iOS reference (screenshot dated 2026-05-24 13:48): 3-column row layout with branded icon + bold name + ticker + rate; light-grey outer card with nested white inner card; always-visible search; iOS-style pill segmented control; tab-conditional column-header labels.

**Architecture:** Six small phases, smallest visual change first. Each phase touches at most 2-3 files. After every phase: build dev debug + invoke `code-review:code-review` skill on the commit (per user's standing request).

**Tech Stack:** Kotlin, Android Data Binding (existing), ConstraintLayout, Material `TabLayout` (Phase 6 replaces it), `Vibrator` (unchanged from prior plan).

**Reference (iOS source for parity):** screenshot at `/Users/nursultanyelemessov/Downloads/IMG_1133.PNG` (iOS Crypto tab open).

**Branch:** continue on `feature/CalcurateCrypto`.

**Out of scope:** keyboard handling on the always-visible search, custom segmented-control accessibility (TalkBack), animation between tabs.

---

## Phase 1: Three-column row layout

**Goal:** Restructure `currency_item.xml` so the icon lives in its own left column (not overlaying the favorite star) and the right side has both a ticker label and a rate value. Adapter populates both columns for crypto rows; fiat rows unchanged.

**Files:**
- Modify: `app/src/main/res/layout/currency_item.xml`
- Modify: `app/src/main/java/com/thecalcurate/android/model/CurrencyItem.kt` (add `ticker` field)
- Modify: `app/src/main/java/com/thecalcurate/android/ui/CurrencyRecyclerViewAdapter.kt`

- [ ] **Step 1.1: Add `ticker` field to CurrencyItem**

Edit `app/src/main/java/com/thecalcurate/android/model/CurrencyItem.kt`. Below the existing `iconResId` field, add:

```kotlin
    // Set by CurrencyDialog when rendering crypto rows; empty for fiat.
    var ticker: String = ""
```

- [ ] **Step 1.2: Rewrite currency_item.xml as 3-column layout**

Replace the whole file body with this layout. Icon column has its own dedicated 56dp area; chbFav lives inside it and is swapped with imgIcon via visibility; name takes the centre; ticker + rate are stacked-inline on the right.

```xml
<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools">

    <data>
        <variable
            name="item"
            type="com.thecalcurate.android.model.CurrencyItem" />
    </data>

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="60dp"
        android:background="@color/white">

        <androidx.appcompat.widget.AppCompatCheckBox
            android:id="@+id/chbFav"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:layout_marginStart="12dp"
            android:button="@drawable/favourite"
            android:checked="@{item.isFavorite2}"
            android:theme="@style/checkBoxStyle"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

        <androidx.appcompat.widget.AppCompatImageView
            android:id="@+id/imgIcon"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:visibility="gone"
            app:layout_constraintBottom_toBottomOf="@id/chbFav"
            app:layout_constraintEnd_toEndOf="@id/chbFav"
            app:layout_constraintStart_toStartOf="@id/chbFav"
            app:layout_constraintTop_toTopOf="@id/chbFav"
            tools:src="@drawable/crypto_btc"
            tools:visibility="visible" />

        <androidx.appcompat.widget.AppCompatTextView
            android:id="@+id/txvName"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:gravity="start|center_vertical"
            android:paddingStart="12dp"
            android:text="@{item.name}"
            android:textAllCaps="true"
            android:textColor="@color/grey"
            android:textSize="16sp"
            android:textStyle="bold"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toStartOf="@id/txvTicker"
            app:layout_constraintStart_toEndOf="@id/chbFav"
            app:layout_constraintTop_toTopOf="parent"
            tools:text="BITCOIN" />

        <androidx.appcompat.widget.AppCompatTextView
            android:id="@+id/txvTicker"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:paddingEnd="8dp"
            android:text="@{item.ticker}"
            android:textColor="@color/grey2"
            android:textSize="13sp"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toStartOf="@id/txvRate"
            app:layout_constraintTop_toTopOf="parent"
            tools:text="BTC" />

        <androidx.appcompat.widget.AppCompatTextView
            android:id="@+id/txvRate"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginEnd="12dp"
            android:gravity="end|center_vertical"
            android:minWidth="80dp"
            android:text="@{item.rateStr}"
            android:textColor="@color/grey2"
            android:textSize="14sp"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            tools:text="0.00001299" />

        <View
            android:id="@+id/viewPadding"
            android:visibility="gone"
            android:layout_width="match_parent"
            android:layout_height="@dimen/dialog_padding"
            app:layout_constraintTop_toBottomOf="@id/txvName" />
    </androidx.constraintlayout.widget.ConstraintLayout>
</layout>
```

- [ ] **Step 1.3: Update CurrencyRecyclerViewAdapter row binding for ticker column**

Edit `app/src/main/java/com/thecalcurate/android/ui/CurrencyRecyclerViewAdapter.kt`. In `onBindViewHolder`, the existing `if (isCrypto)` branch keeps imgIcon visible. Change the rate-hiding behavior — rate must stay visible for crypto now. Replace the existing branch:

```kotlin
            val isCrypto = (item?.iconResId ?: 0) != 0
            if (isCrypto) {
                holder.binding.imgIcon.setImageResource(item!!.iconResId)
                holder.binding.imgIcon.visibility = View.VISIBLE
                // INVISIBLE (not GONE) keeps the column width stable so txvName
                // doesn't shift between fiat and crypto rows.
                holder.binding.chbFav.visibility = View.INVISIBLE
                holder.binding.txvRate.visibility = View.GONE
            } else {
                holder.binding.imgIcon.visibility = View.GONE
                holder.binding.chbFav.visibility = View.VISIBLE
                holder.binding.txvRate.visibility = View.VISIBLE
            }
```

With:

```kotlin
            val isCrypto = (item?.iconResId ?: 0) != 0
            if (isCrypto) {
                holder.binding.imgIcon.setImageResource(item!!.iconResId)
                holder.binding.imgIcon.visibility = View.VISIBLE
                // Hide the favorite checkbox entirely — crypto rows aren't favouritable.
                holder.binding.chbFav.visibility = View.INVISIBLE
                holder.binding.txvTicker.visibility = View.VISIBLE
            } else {
                holder.binding.imgIcon.visibility = View.GONE
                holder.binding.chbFav.visibility = View.VISIBLE
                holder.binding.txvTicker.visibility = View.GONE
            }
```

Note: `txvRate` stays VISIBLE in both branches (rate column is universal now); `txvTicker` is shown only for crypto.

- [ ] **Step 1.4: Build**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`. The data-binding layout regenerates `CurrencyItemBinding` to include `imgIcon` and `txvTicker`. If "Unresolved reference: txvTicker" surfaces, confirm Step 1.2 saved the XML correctly.

- [ ] **Step 1.5: Commit**

```bash
git add app/src/main/res/layout/currency_item.xml app/src/main/java/com/thecalcurate/android/model/CurrencyItem.kt app/src/main/java/com/thecalcurate/android/ui/CurrencyRecyclerViewAdapter.kt
git commit -m "fix(picker): 3-column row layout (icon | name | ticker+rate)"
```

- [ ] **Step 1.6: Code-review checkpoint**

Invoke `code-review:code-review`. Concerns: name-column overlap with ticker on long names; vertical centering with the new fixed 60dp height.

---

## Phase 2: Real crypto rates in the picker

**Goal:** Crypto rows currently show `rate = 0.0`. iOS shows the actual rate (`BTC 0.00001299`, `ETH 0.00047118`, ...) — these are already in `viewModel.cryptoRates`. Pass them to the dialog at show-time so `buildCryptoRows` can populate `CurrencyItem.rate` and `getRateStr()` renders the value.

**Files:**
- Modify: `app/src/main/java/com/thecalcurate/android/ui/CurrencyDialog.kt`
- Modify: `app/src/main/java/com/thecalcurate/android/MainActivity.kt` (set the field before `show()`)

- [ ] **Step 2.1: Add cryptoRates field to CurrencyDialog**

Edit `app/src/main/java/com/thecalcurate/android/ui/CurrencyDialog.kt`. Near the existing fields (after `var list: MutableList<CurrencyItem>? = null`), add:

```kotlin
    /** Populated by MainActivity from viewModel.cryptoRates before show(). */
    var cryptoRates: Map<String, Double> = emptyMap()
```

- [ ] **Step 2.2: Use cryptoRates in buildCryptoRows**

In the same file, replace the existing `buildCryptoRows` body:

```kotlin
    private fun buildCryptoRows(): MutableList<CurrencyItem> {
        return CryptoItem.getList().map { c ->
            CurrencyItem(c.name, c.code, 0.0).also {
                it.isFavorite2 = false
                it.iconResId = c.iconResId
            }
        }.toMutableList()
    }
```

With:

```kotlin
    private fun buildCryptoRows(): MutableList<CurrencyItem> {
        return CryptoItem.getList().map { c ->
            CurrencyItem(c.name, c.code, cryptoRates[c.code] ?: 0.0).also {
                it.isFavorite2 = false
                it.iconResId = c.iconResId
                it.ticker = c.code
            }
        }.toMutableList()
    }
```

- [ ] **Step 2.3: Pass cryptoRates from MainActivity into the dialog**

Edit `app/src/main/java/com/thecalcurate/android/MainActivity.kt`. Find the block (`showDialog()` around line 955):

```kotlin
            newFragment.listToShow = listToShow
            newFragment.list = list
            newFragment.show(supportFragmentManager, "dialog")
```

Change to:

```kotlin
            newFragment.listToShow = listToShow
            newFragment.list = list
            newFragment.cryptoRates = viewModel.cryptoRates.value ?: emptyMap()
            newFragment.show(supportFragmentManager, "dialog")
```

- [ ] **Step 2.4: Build**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2.5: Commit**

```bash
git add app/src/main/java/com/thecalcurate/android/ui/CurrencyDialog.kt app/src/main/java/com/thecalcurate/android/MainActivity.kt
git commit -m "fix(picker): pass live cryptoRates into the dialog so crypto rows show actual rate"
```

- [ ] **Step 2.6: Code-review checkpoint**

Invoke `code-review:code-review`. Concern: when `cryptoRates` is empty (first launch, no cache, no fetch yet), all crypto rows show `0` — acceptable, matches the pre-fix behaviour but worth noting.

---

## Phase 2.5: Crypto-aware rate formatter (interstitial fix)

**Goal:** Phase 2's code-review surfaced a real defect — the existing `%.3f` formatter (from the earlier "3 decimals to match iOS" change) renders most crypto rates as `0.000`: BTC=0.00001299 → `0.000`, ETH=0.00047118 → `0.000`, BNB=0.00151554 → `0.002`, SOL=0.01155135 → `0.012`. iOS uses up to 8 decimals for crypto. Fix the formatter so crypto rows use 8 decimals with trailing zeros trimmed (matching iOS exactly: USDT=`1`, XRP=`0.73303035`).

**Files:**
- Modify: `app/src/main/java/com/thecalcurate/android/model/CurrencyItem.kt`

- [ ] **Step 2.5.1: Branch the formatter on iconResId**

Edit `app/src/main/java/com/thecalcurate/android/model/CurrencyItem.kt`. Replace the `getRateStr()` body:

```kotlin
    fun getRateStr(): String {
        return if (rate == .0) "0"
        else "%.3f".format(rate)
    }
```

With:

```kotlin
    fun getRateStr(): String {
        if (rate == .0) return "0"
        // Crypto rows (set via iconResId) get up to 8 decimals to match iOS
        // crypto formatter (USDT="1", BTC="0.00001299", ADA="4.06669378").
        // Fiat keeps 3 decimals per the "3 decimals" iOS build variant.
        return if (iconResId != 0) {
            "%.8f".format(rate).trimEnd('0').trimEnd('.')
        } else {
            "%.3f".format(rate)
        }
    }
```

- [ ] **Step 2.5.2: Build**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2.5.3: Commit**

```bash
git add app/src/main/java/com/thecalcurate/android/model/CurrencyItem.kt
git commit -m "fix(picker): crypto rates use 8-decimal precision (trimmed) to match iOS"
```

- [ ] **Step 2.5.4: Code-review checkpoint**

Invoke `code-review:code-review`. Concerns: locale (Turkish dot vs comma) — `%.8f` uses platform locale; trimEnd('.') handles values like `1.00000000` → `1` cleanly.

---

## Phase 3: Always-visible search bar

**Goal:** iOS shows the search bar on both fiat AND crypto tabs. Currently the tab listener hides `edtSearch` + `imvClear` when crypto tab is active. Remove that.

**Files:**
- Modify: `app/src/main/java/com/thecalcurate/android/ui/CurrencyDialog.kt`

- [ ] **Step 3.1: Stop hiding the search bar on crypto tab**

In `CurrencyDialog.onCreateDialog`, find the `tabLayout?.addOnTabSelectedListener` block. Inside `onTabSelected`, the `if (isCryptoTab)` branch hides `edtSearch` and `imvClear`. Change to keep them visible:

Replace:

```kotlin
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        isCryptoTab = (tab?.position == 1)
                        if (isCryptoTab) {
                            edtSearch.visibility = View.GONE
                            imvClear.visibility = View.GONE
                            txvFav?.visibility = View.GONE
                            txvCurr?.visibility = View.GONE
                            txvRates?.visibility = View.GONE
                            adapter?.setList(buildCryptoRows())
                            adapter?.notifyDataSetChanged()
                        } else {
                            edtSearch.visibility = View.VISIBLE
                            txvFav?.visibility = View.VISIBLE
                            txvCurr?.visibility = View.VISIBLE
                            txvRates?.visibility = View.VISIBLE
                            adapter?.setList(listToShow!!)
                            adapter?.notifyDataSetChanged()
                        }
                    }
```

With:

```kotlin
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        isCryptoTab = (tab?.position == 1)
                        if (isCryptoTab) {
                            // Search bar stays visible (matches iOS); crypto list has only 8 rows
                            // so search is functionally a no-op but the bar is part of the layout.
                            adapter?.setList(buildCryptoRows())
                        } else {
                            adapter?.setList(listToShow!!)
                        }
                        adapter?.notifyDataSetChanged()
                    }
```

The column-header label switching is handled by Phase 4, not here — for now the existing "Favorites / Currencies / Rates" labels remain visible in both tabs.

- [ ] **Step 3.2: Build**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3.3: Commit**

```bash
git add app/src/main/java/com/thecalcurate/android/ui/CurrencyDialog.kt
git commit -m "fix(picker): keep search bar visible on crypto tab to match iOS"
```

- [ ] **Step 3.4: Code-review checkpoint**

Invoke `code-review:code-review`.

---

## Phase 4: Tab-conditional column-header labels

**Goal:** On the crypto tab, the three header labels show `Crypto` (left) and `Rate` (right) per iOS. On the fiat tab, they continue to show `Favorites / Currencies / Rates` (existing strings).

**Files:**
- Modify: `app/src/main/res/values/strings.xml` (add new strings)
- Modify: `app/src/main/java/com/thecalcurate/android/ui/CurrencyDialog.kt`

- [ ] **Step 4.1: Add new strings**

Edit `app/src/main/res/values/strings.xml`. Add two entries inside `<resources>`:

```xml
    <string name="header_crypto">Crypto</string>
    <string name="header_rate">Rate</string>
```

- [ ] **Step 4.2: Hide the middle header and swap text in the tab listener**

In `CurrencyDialog.onCreateDialog`, inside the `tabLayout?.addOnTabSelectedListener` (modified in Phase 3), extend `onTabSelected` so the column labels change with the active tab. Replace the Phase-3 body with:

```kotlin
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        isCryptoTab = (tab?.position == 1)
                        if (isCryptoTab) {
                            adapter?.setList(buildCryptoRows())
                            // iOS layout: just "Crypto" on the left and "Rate" on the right.
                            txvFav?.let { (it as? android.widget.TextView)?.text = getString(R.string.header_crypto) }
                            txvFav?.visibility = View.VISIBLE
                            txvCurr?.visibility = View.GONE
                            txvRates?.visibility = View.VISIBLE
                        } else {
                            adapter?.setList(listToShow!!)
                            // Restore the fiat labels.
                            txvFav?.let { (it as? android.widget.TextView)?.text = getString(R.string.favorites) }
                            txvFav?.visibility = View.VISIBLE
                            txvCurr?.visibility = View.VISIBLE
                            txvRates?.visibility = View.VISIBLE
                        }
                        adapter?.notifyDataSetChanged()
                    }
```

Note: `txvFav` was looked up as `View` in current code. The cast `as? android.widget.TextView` lets us call `setText`. Alternatively, change the lookup type to `TextView` at the top of `onCreateDialog`:

```kotlin
                val txvFav = dialogView.findViewById<android.widget.TextView>(R.id.txvFav)
                val txvCurr = dialogView.findViewById<android.widget.TextView>(R.id.txvCurr)
                val txvRates = dialogView.findViewById<android.widget.TextView>(R.id.txvRates)
```

Prefer the explicit lookup type — cleaner. Make that change instead of the inline cast.

- [ ] **Step 4.3: Build**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4.4: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/java/com/thecalcurate/android/ui/CurrencyDialog.kt
git commit -m "fix(picker): swap column headers per tab (Crypto/Rate on crypto, Favorites/Currencies/Rates on fiat)"
```

- [ ] **Step 4.5: Code-review checkpoint**

Invoke `code-review:code-review`.

---

## Phase 5: Light-grey outer + white inner card

**Goal:** Add a light-grey wrapping background (`#F2F2F7` ≈ iOS systemGroupedBackground) under everything, and keep an inner white card for the list/search area.

**Files:**
- Modify: `app/src/main/res/values/colors.xml` (add color)
- Create: `app/src/main/res/drawable/dialog_inner_card_bg.xml` (white rounded inner card)
- Modify: `app/src/main/res/drawable/dialog_rounded_bg.xml` (change to light grey)
- Modify: `app/src/main/res/layout/currency_search.xml` (wrap search + list in an inner card)

- [ ] **Step 5.1: Add the light-grey color**

Edit `app/src/main/res/values/colors.xml`. Add inside `<resources>`:

```xml
    <color name="grouped_bg">#F2F2F7</color>
```

- [ ] **Step 5.2: Change the outer drawable to grey**

Replace the contents of `app/src/main/res/drawable/dialog_rounded_bg.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/grouped_bg" />
    <corners android:radius="20dp" />
</shape>
```

- [ ] **Step 5.3: Create the inner white card drawable**

Create `app/src/main/res/drawable/dialog_inner_card_bg.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/white" />
    <corners android:radius="12dp" />
</shape>
```

- [ ] **Step 5.4: Wrap the RecyclerView in the inner card via background**

Edit `app/src/main/res/layout/currency_search.xml`. The `<androidx.recyclerview.widget.RecyclerView android:id="@+id/recyclerview" ...>` element currently has no background. Add:

```xml
        android:background="@drawable/dialog_inner_card_bg"
        android:clipToOutline="true"
```

— inserted alongside the other RecyclerView attributes. The `clipToOutline` ensures row backgrounds don't bleed past the rounded corners.

Also: each list row's root ConstraintLayout has `android:background="@color/white"` (from Phase 1). Keep that — the inner card drawable is white too, so visually it's seamless; the clipping just rounds the bottom of the last row and top of the first.

- [ ] **Step 5.5: Build**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5.6: Commit**

```bash
git add app/src/main/res/values/colors.xml app/src/main/res/drawable/dialog_rounded_bg.xml app/src/main/res/drawable/dialog_inner_card_bg.xml app/src/main/res/layout/currency_search.xml
git commit -m "fix(picker): light-grey outer card with white rounded inner card matches iOS"
```

- [ ] **Step 5.7: Code-review checkpoint**

Invoke `code-review:code-review`.

---

## Phase 6: Custom iOS-style segmented control

**Goal:** Replace the Material `TabLayout` (orange underline indicator) with a pill-shaped segmented control: rounded grey background containing two equal segments; the selected segment has a white rounded background and slight shadow.

**Files:**
- Create: `app/src/main/res/drawable/segmented_container_bg.xml` (outer pill)
- Create: `app/src/main/res/drawable/segmented_selected_bg.xml` (inner selected pill)
- Modify: `app/src/main/res/layout/currency_search.xml` (replace TabLayout block)
- Modify: `app/src/main/java/com/thecalcurate/android/ui/CurrencyDialog.kt` (replace tab-selection wiring)

- [ ] **Step 6.1: Outer container drawable**

Create `app/src/main/res/drawable/segmented_container_bg.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#E5E5EA" />
    <corners android:radius="10dp" />
</shape>
```

- [ ] **Step 6.2: Selected-segment drawable**

Create `app/src/main/res/drawable/segmented_selected_bg.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/white" />
    <corners android:radius="8dp" />
</shape>
```

- [ ] **Step 6.3: Replace TabLayout block in currency_search.xml**

In `app/src/main/res/layout/currency_search.xml`, replace the existing `<com.google.android.material.tabs.TabLayout ...>...</com.google.android.material.tabs.TabLayout>` element with a custom LinearLayout-based control:

```xml
    <LinearLayout
        android:id="@+id/segmentedControl"
        android:layout_width="match_parent"
        android:layout_height="36dp"
        android:layout_marginTop="0dp"
        android:background="@drawable/segmented_container_bg"
        android:gravity="center"
        android:orientation="horizontal"
        android:padding="2dp"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent">

        <androidx.appcompat.widget.AppCompatTextView
            android:id="@+id/segCurrencies"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="@drawable/segmented_selected_bg"
            android:gravity="center"
            android:text="Currencies"
            android:textColor="@color/grey"
            android:textSize="14sp"
            android:textStyle="bold" />

        <androidx.appcompat.widget.AppCompatTextView
            android:id="@+id/segCrypto"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="@android:color/transparent"
            android:gravity="center"
            android:text="Crypto"
            android:textColor="@color/grey"
            android:textSize="14sp" />
    </LinearLayout>
```

Hardcoded labels "Currencies" / "Crypto" are acceptable — they're the same on the existing TabLayout's TabItems. (Or extract to strings if the project's policy requires it — strings.xml has `app_name`, `favorites`, etc. so it's the convention.) Promote to strings:

```xml
    <string name="segment_currencies">Currencies</string>
    <string name="segment_crypto">Crypto</string>
```

— then change `android:text="Currencies"` → `android:text="@string/segment_currencies"` etc.

- [ ] **Step 6.4: Rewire selection in CurrencyDialog**

Edit `app/src/main/java/com/thecalcurate/android/ui/CurrencyDialog.kt`. Remove the `import com.google.android.material.tabs.TabLayout` line (if no longer needed elsewhere). Replace the entire block that starts with `val tabLayout = dialogView.findViewById<TabLayout>(R.id.tabLayout)` and ends with the closing `})` of the `addOnTabSelectedListener` call.

Replace with:

```kotlin
                val segCurrencies = dialogView.findViewById<android.widget.TextView>(R.id.segCurrencies)
                val segCrypto = dialogView.findViewById<android.widget.TextView>(R.id.segCrypto)

                fun applySegmentSelection(crypto: Boolean) {
                    isCryptoTab = crypto
                    if (crypto) {
                        segCurrencies.setBackgroundResource(android.R.color.transparent)
                        segCurrencies.setTypeface(null, android.graphics.Typeface.NORMAL)
                        segCrypto.setBackgroundResource(R.drawable.segmented_selected_bg)
                        segCrypto.setTypeface(null, android.graphics.Typeface.BOLD)
                        adapter?.setList(buildCryptoRows())
                        txvFav?.text = getString(R.string.header_crypto)
                        txvFav?.visibility = View.VISIBLE
                        txvCurr?.visibility = View.GONE
                        txvRates?.visibility = View.VISIBLE
                    } else {
                        segCurrencies.setBackgroundResource(R.drawable.segmented_selected_bg)
                        segCurrencies.setTypeface(null, android.graphics.Typeface.BOLD)
                        segCrypto.setBackgroundResource(android.R.color.transparent)
                        segCrypto.setTypeface(null, android.graphics.Typeface.NORMAL)
                        adapter?.setList(listToShow!!)
                        txvFav?.text = getString(R.string.favorites)
                        txvFav?.visibility = View.VISIBLE
                        txvCurr?.visibility = View.VISIBLE
                        txvRates?.visibility = View.VISIBLE
                    }
                    adapter?.notifyDataSetChanged()
                }

                segCurrencies.setOnClickListener { applySegmentSelection(false) }
                segCrypto.setOnClickListener { applySegmentSelection(true) }
                // Start on Currencies (fiat) tab.
                applySegmentSelection(false)
```

Also remove the `isCryptoTab` field's redundant `false` initialization if conflicting (Phase 4 may have set it elsewhere). Keep the declaration `private var isCryptoTab: Boolean = false`.

- [ ] **Step 6.5: Build**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`. Unresolved references to `TabLayout` indicate Step 6.4 missed an occurrence — re-grep and fix.

- [ ] **Step 6.6: Commit**

```bash
git add app/src/main/res/drawable/segmented_container_bg.xml app/src/main/res/drawable/segmented_selected_bg.xml app/src/main/res/layout/currency_search.xml app/src/main/res/values/strings.xml app/src/main/java/com/thecalcurate/android/ui/CurrencyDialog.kt
git commit -m "feat(picker): custom iOS-style segmented control replaces Material TabLayout"
```

- [ ] **Step 6.7: Code-review checkpoint**

Invoke `code-review:code-review`. Concerns: accessibility (no role announcement); rapid double-click stability; theme conformance (does `setBackgroundResource(android.R.color.transparent)` reset other selectors).

---

## Self-review notes

Spec coverage:
- 3-column row layout → Phase 1 ✓
- Real crypto rates shown → Phase 2 ✓
- Search bar always visible → Phase 3 ✓
- Tab-conditional column labels (Currencies/Favorites/Rates vs Crypto/Rate) → Phase 4 ✓
- Light-grey outer card + white inner card → Phase 5 ✓
- Custom iOS-style segmented control → Phase 6 ✓
- 32dp icon size → Phase 1 ✓ (set `android:layout_width="32dp"` in Step 1.2)
- Generous 60dp row height → Phase 1 ✓ (set in row root)
- Favorite star hidden on crypto → already done previously; preserved by Phase 1.3 (chbFav.INVISIBLE)
- Centered dialog → already done previously; not touched here

Risk areas:
- **Phase 1 layout change is the most invasive.** It rewrites `currency_item.xml` and the data-binding regeneration may surface unrelated build errors in `CurrencyItemBindingImpl`. Clean-build before assuming the new layout is the culprit.
- **Phase 4 + Phase 6 both manipulate `txvFav` text.** Phase 6 (segmented) supersedes Phase 4 (tab listener) — the `applySegmentSelection` function takes over. If executed out of order, both code paths could touch the same view; that's fine because Phase 6 replaces the Phase-4 listener entirely.
- **Phase 5 inner card backing the RecyclerView**: with `clipToOutline=true` on the RecyclerView, list items will be visually clipped at rounded corners. If a row's selection highlight extends to the bounds and you see hard edges, drop `clipToOutline` and live with square corners on the row backgrounds.

Out of scope (deferred):
- Custom drawable for the favorites checkbox to match iOS star
- Per-row tap animations
- Settings page / theme toggle
- Long-form rate formatting (currently `%.3f` shows `0.000` for tiny crypto/USD values — separate concern)
