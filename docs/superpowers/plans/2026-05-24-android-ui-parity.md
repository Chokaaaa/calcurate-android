# Android UI Parity Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the remaining UI gaps with iOS — picker background styling, remove scroll-cycle behavior, haptic feedback on long-press, button press animations, and port the ~140 missing flag/crypto image assets.

**Architecture:** Five independent phases, smallest → biggest. Each phase commits with build green, then a code-review skill checkpoint validates the diff before moving on (per user request "after every step of the fix ask superpower code review").

**Tech Stack:** Kotlin, Android resources (drawable, layout), Android Vibrator API, ValueAnimator/ViewPropertyAnimator. No new libraries.

**Reference (iOS source for parity):** `/Users/nursultanyelemessov/Desktop/Desktop - Nursultan's MacBook Pro (2)/Projects/Freelance/Shakhin Zhursinbek/CalcurateApp/CalcuRate 3 decimals/CalcuRateApp/Calcura+e/`

**Branch:** continue on `feature/CalcurateCrypto` (or new branch if user wants; ask if uncertain).

**Code-review:** after every phase commit, invoke skill `code-review:code-review` with the diff from that phase. If review surfaces a real defect, fix and amend before the next phase.

---

## Phase 1: Picker background styling

**Goal:** Replace the default white-fullscreen AlertDialog with a rounded white card + 50% black scrim, matching iOS popup styling.

**Files:**
- Create: `app/src/main/res/drawable/dialog_rounded_bg.xml`
- Modify: `app/src/main/res/layout/currency_search.xml` (root background, padding)
- Modify: `app/src/main/java/com/thecalcurate/android/ui/CurrencyDialog.kt` (window properties)

- [ ] **Step 1.1: Create the rounded background drawable**

Create `app/src/main/res/drawable/dialog_rounded_bg.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/white" />
    <corners android:radius="16dp" />
</shape>
```

- [ ] **Step 1.2: Update currency_search.xml root background and padding**

Edit `app/src/main/res/layout/currency_search.xml`. Replace the `android:background="@color/white"` on the root ConstraintLayout with the new drawable and add bottom padding to balance the top/start/end padding that already exists:

Change line 7 from:

```xml
android:background="@color/white"
```

To:

```xml
android:background="@drawable/dialog_rounded_bg"
```

Also add `android:paddingBottom="@dimen/dialog_padding"` to the root, just below the existing `android:paddingEnd` line. So the existing block becomes:

```xml
android:paddingStart="@dimen/dialog_padding"
android:paddingTop="@dimen/dialog_padding"
android:paddingEnd="@dimen/dialog_padding"
android:paddingBottom="@dimen/dialog_padding">
```

- [ ] **Step 1.3: Configure dialog window for transparent backdrop + scrim**

Edit `app/src/main/java/com/thecalcurate/android/ui/CurrencyDialog.kt`. Inside `onCreateDialog`, after `builder.create()` and before the closing brace, change the return to capture the dialog and set window properties. Replace:

```kotlin
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
```

With:

```kotlin
            val dialog = builder.create()
            dialog.window?.apply {
                // Match iOS: transparent window so the rounded shape shows through, plus 50% black scrim.
                setBackgroundDrawableResource(android.R.color.transparent)
                setDimAmount(0.5f)
            }
            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
```

- [ ] **Step 1.4: Build dev debug**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 1.5: Commit**

```bash
git add app/src/main/res/drawable/dialog_rounded_bg.xml app/src/main/res/layout/currency_search.xml app/src/main/java/com/thecalcurate/android/ui/CurrencyDialog.kt
git commit -m "fix(picker): rounded white card + 50% scrim to match iOS popup"
```

- [ ] **Step 1.6: Code-review checkpoint**

Invoke the `code-review:code-review` skill against the Phase 1 commit. If the review flags a real defect, fix and amend in a new commit before Phase 2. (Subjective style nits can be deferred.)

---

## Phase 2: Remove scroll behavior (tutorial + main usage)

**Goal:** Remove the vertical swipe-cycles-currency behavior (`scrollCurrency`) and the tutorial step that demonstrates it. User confirmed iOS does not have this.

**Files:**
- Modify: `app/src/main/java/com/thecalcurate/android/MainActivity.kt` (delete `scrollCurrency` and 6 `onSwipeTop/Bottom` overrides; remove tutorial step 3 progression)
- Modify: `app/src/main/res/layout/activity_main.xml` (remove `@id/txvSwipeUp`, `@id/txvSwipeDown`, `@id/txvScroll` views)

- [ ] **Step 2.1: Find the scrollCurrency function and remove it**

Run: `grep -n "fun scrollCurrency" app/src/main/java/com/thecalcurate/android/MainActivity.kt`

This locates the function definition (around line 851 per prior survey). Read the function body (typically ~40 lines — `scrollCurrency(view: View, direction: Int)`).

Delete the entire function body. Use Edit with the exact function block as `old_string` and an empty string (or comment marker) as `new_string`. The body looks roughly like:

```kotlin
    private fun scrollCurrency(view: View, direction: Int) {
        // ...body using selectedCurList, UP/DOWN constants, setCur, fetchCurrencyRates...
    }
```

Verify with grep after: `grep -n "scrollCurrency" app/src/main/java/com/thecalcurate/android/MainActivity.kt` — should return 0 lines after Step 2.3.

- [ ] **Step 2.2: Remove onSwipeTop and onSwipeBottom overrides for all three slot buttons**

In `MainActivity.kt`, three `OnSwipeListener` anonymous objects (one each for `btn_main`, `btn_secondary1`, `btn_secondary2`) override `onSwipeTop` and `onSwipeBottom`. Lines (approx, per prior grep): 655–671 (main), 688–700 (sec1), 719–737 (sec2).

For EACH of the three listener blocks, delete the two override blocks. Pattern to remove:

```kotlin
                override fun onSwipeTop() {
                    super.onSwipeTop()
                    scrollCurrency(btn_main, UP)
                    if (!isTutorialViewed && tutorialStep > 2) {
                        tutorialStep++
                        nextTutorialStep()
                    }
                }

                override fun onSwipeBottom() {
                    super.onSwipeBottom()
                    scrollCurrency(btn_main, DOWN)
                    if (!isTutorialViewed && tutorialStep > 2) {
                        tutorialStep++
                        nextTutorialStep()
                    }
                }
```

(Same pattern for `btn_secondary1` and `btn_secondary2` with their respective button references.) After deletion, the listeners retain `onSwipeLeft`, `onSwipeRight`, and `onLongPress` overrides.

- [ ] **Step 2.3: Remove tutorial step 3 progression and tutorial scroll-image views**

In `MainActivity.kt`, find `nextTutorialStep()` (function that handles tutorial progression). Around lines 745–800, there's a `when (tutorialStep)` or `if (tutorialStep == ...)` block. Read it. Remove any branch that references `txvSwipeUp`, `txvSwipeDown`, or `txvSroll`. The step counter should now top out at 2 (Hold → Swipe), no Step 3 (Scroll).

Also delete the three `lateinit var` declarations (lines ~60–62):

```kotlin
    lateinit var txvSroll: View
    lateinit var txvSwipeUp: View
    lateinit var txvSwipeDown: View
```

And the three `findViewById` lines (~421–423):

```kotlin
            txvSroll = findViewById(R.id.txvScroll)
            txvSwipeUp = findViewById(R.id.txvSwipeUp)
            txvSwipeDown = findViewById(R.id.txvSwipeDown)
```

- [ ] **Step 2.4: Remove the three scroll views from activity_main.xml**

Read `app/src/main/res/layout/activity_main.xml`. Find the three views with ids `@+id/txvScroll`, `@+id/txvSwipeUp`, `@+id/txvSwipeDown`. Delete each `<...View android:id="@+id/txvScroll" ... />` block entirely.

After deletion, run `grep -n "txvScroll\|txvSwipeUp\|txvSwipeDown" app/src/main/res/layout/activity_main.xml` — should return 0 lines.

If any OTHER view's constraint references these (e.g., `app:layout_constraintTop_toBottomOf="@id/txvSwipeUp"`), reroute that constraint to whatever was above the deleted view — typically the previous sibling or `parent`. Read the layout carefully; this can break visual layout. Grep first: `grep -n "@id/txvScroll\|@id/txvSwipeUp\|@id/txvSwipeDown\|@+id/txvScroll" app/src/main/res/layout/activity_main.xml`.

- [ ] **Step 2.5: Delete the unused swipe_up.png / swipe_down.png drawables**

These PNGs are no longer referenced. Delete to keep the asset tree tidy:

```bash
rm app/src/main/res/drawable/swipe_up.png app/src/main/res/drawable/swipe_down.png
```

(Keep `swipe_left.png` and `swipe_right.png` — still used by the swipe step of the tutorial.)

- [ ] **Step 2.6: Build dev debug**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`. Any unresolved references mean a `txvSroll`/`txvSwipeUp`/`txvSwipeDown` was missed somewhere — re-grep and fix.

- [ ] **Step 2.7: Commit**

```bash
git add app/src/main/java/com/thecalcurate/android/MainActivity.kt app/src/main/res/layout/activity_main.xml app/src/main/res/drawable/swipe_up.png app/src/main/res/drawable/swipe_down.png
git commit -m "feat(parity): drop vertical-swipe currency cycling + matching tutorial step

iOS doesn't have this behavior. Removes scrollCurrency() helper, six
onSwipeTop/Bottom overrides (one pair per slot button), tutorial step 3
(scroll demo) UI, and the now-orphaned swipe_up/down drawables."
```

- [ ] **Step 2.8: Code-review checkpoint**

Invoke `code-review:code-review` on the Phase 2 commit. Expected concerns: did we break tutorial step counting, did we orphan any constraint, did we miss a scrollCurrency caller.

---

## Phase 3: Haptic feedback on long-press

**Goal:** When the user holds a slot to open the picker, vibrate briefly. Matches iOS `UIImpactFeedbackGenerator(style: .heavy)`.

**Files:**
- Modify: `app/src/main/java/com/thecalcurate/android/MainActivity.kt` (add a helper + call from three `onLongPress` overrides)

- [ ] **Step 3.1: Add a haptic-pulse helper**

In `MainActivity.kt`, after the existing `VIBRATION_MILIS` constant (~line 97) and the `vibe: Vibrator` field, add a helper function. Location: just above `private fun viewSetup()` (around line 388) — anywhere in the class as long as it has access to `vibe`. Add:

```kotlin
    private fun hapticHeavy() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibe.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibe.vibrate(35)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
```

The 35ms duration is roughly equivalent to iOS heavy impact (which is ~25-40ms perception window).

- [ ] **Step 3.2: Add imports if missing**

Verify `MainActivity.kt` already imports `android.os.VibrationEffect` and `android.os.Build`. Search:

```bash
grep -n "import android.os.VibrationEffect\|import android.os.Build" app/src/main/java/com/thecalcurate/android/MainActivity.kt
```

Both are already imported (Build is used at line ~432 for vibrator setup, VibrationEffect appears in the same conditional). If either is missing after Step 3.1, add to the import block at the top of the file.

- [ ] **Step 3.3: Call hapticHeavy() from each onLongPress override**

In MainActivity, three places call `showDialog()` from `onLongPress`. Per prior grep: lines 673–678 (btn_main), 700–706 (btn_secondary1), 737–743 (btn_secondary2).

For each, add `hapticHeavy()` as the FIRST line inside the override body. Example for btn_main:

```kotlin
                override fun onLongPress() {
                    super.onLongPress()
                    hapticHeavy()
                    longClickedId = R.id.btn_main
                    showDialog()
                    // ...existing code...
                }
```

Repeat for btn_secondary1 and btn_secondary2.

- [ ] **Step 3.4: Build dev debug**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3.5: Commit**

```bash
git add app/src/main/java/com/thecalcurate/android/MainActivity.kt
git commit -m "feat(haptics): 35ms vibration on slot long-press to match iOS heavy impact"
```

- [ ] **Step 3.6: Code-review checkpoint**

Invoke `code-review:code-review` on the Phase 3 commit.

---

## Phase 4: Button press flash animation

**Goal:** Add iOS-style press-feedback animation to calculator buttons. iOS animates a white flash → original color over 0.1s for numbers, 0.2s for operators. Android equivalent: alpha dip + restore using ViewPropertyAnimator.

**Files:**
- Modify: `app/src/main/java/com/thecalcurate/android/MainActivity.kt` (add helper + call from existing click listeners)

- [ ] **Step 4.1: Add the flash-animation helper**

In `MainActivity.kt`, after `hapticHeavy()` from Phase 3 (or alongside it), add:

```kotlin
    /**
     * Mirrors iOS press animation (NumberView.swift:51 / ActionView.swift:91).
     * Quick alpha dip to ~0.55 and back to 1.0 over [totalMs] total.
     */
    private fun pressFlash(view: View, totalMs: Long) {
        val half = totalMs / 2
        view.animate().cancel()
        view.animate()
            .alpha(0.55f)
            .setDuration(half)
            .withEndAction {
                view.animate()
                    .alpha(1.0f)
                    .setDuration(half)
                    .start()
            }
            .start()
    }
```

- [ ] **Step 4.2: Hook pressFlash into onNumberClickListener (100ms)**

Locate `private val onNumberClickListener = View.OnClickListener {` (around line 147). The FIRST line inside the try block is `play()`. Add `pressFlash(it, 100L)` right after `play()`. Final shape:

```kotlin
    private val onNumberClickListener = View.OnClickListener {
        try {
            play()
            pressFlash(it, 100L)
            // ...existing code from getNumberClicked() onward...
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
```

- [ ] **Step 4.3: Hook pressFlash into onDotClickListener (100ms)**

Same change for `onDotClickListener` (around line 165). Add `pressFlash(it, 100L)` after `play()`.

- [ ] **Step 4.4: Hook pressFlash into onActionClickListener (200ms)**

For `onActionClickListener` (around line 182), add `pressFlash(it, 200L)` after `play()`. Operators get the longer 200ms duration matching iOS ActionView.swift:91-94.

- [ ] **Step 4.5: Hook pressFlash into onPercentageClickListener (200ms)**

For `onPercentageClickListener` (around line 201), add `pressFlash(it, 200L)` after `play()`.

- [ ] **Step 4.6: Hook pressFlash into onEqualClickListener (200ms)**

For `onEqualClickListener` (around line 220), add `pressFlash(it, 200L)` after `play()`.

- [ ] **Step 4.7: Hook pressFlash into onClearClickListener (200ms)**

Search for `onClearClickListener`:

```bash
grep -n "onClearClickListener" app/src/main/java/com/thecalcurate/android/MainActivity.kt
```

Add `pressFlash(it, 200L)` after `play()` (if `play()` is called; otherwise as the first line of the click action).

- [ ] **Step 4.8: Build dev debug**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4.9: Commit**

```bash
git add app/src/main/java/com/thecalcurate/android/MainActivity.kt
git commit -m "feat(anim): button press alpha-dip animation matching iOS timing (100/200ms)"
```

- [ ] **Step 4.10: Code-review checkpoint**

Invoke `code-review:code-review` on the Phase 4 commit. Concerns: does cancel() on rapid repeated presses look correct, is alpha=0.55 close enough to iOS visually.

---

## Phase 5: Port image assets from iOS

**Goal:** For every iOS currency imageset that doesn't have an Android equivalent, copy the PNG to `app/src/main/res/drawable/<code>.png` and add a mapping entry in `CurrencyButton.kt`. Also replace the 8 colored-XML crypto stubs with the actual iOS crypto PNGs.

**Files:**
- Create: ~140 new `app/src/main/res/drawable/<currency_code>.png` files
- Delete + Replace: 8 `crypto_*.xml` → `crypto_*.png`
- Modify: `app/src/main/java/com/thecalcurate/android/ui/CurrencyButton.kt` (uncomment and complete the mapping)
- Modify: `app/src/main/java/com/thecalcurate/android/model/CryptoItem.kt` (no change required — iconResId stays; the new PNG replaces the XML at the same name)

**Mapping source:** iOS `Calcura+e/Core/CurrencyEnums.swift` lines 229–377 (the `image()` switch maps each `CurrencyChoice` enum case to its imageset name).

- [ ] **Step 5.1: Build a code→imageset mapping table**

Read iOS source `CurrencyEnums.swift` and extract the mapping. Run:

```bash
cd "/Users/nursultanyelemessov/Desktop/Desktop - Nursultan’s MacBook Pro (2)/Projects/Freelance/Shakhin Zhursinbek/CalcurateApp/CalcuRate 3 decimals/CalcuRateApp/Calcura+e"
awk '
  /case \./ && /=/ {
    # extract code: case .X = "CODE"
    match($0, /= "([A-Z]+)"/, a)
    if (a[1] != "") last_code = a[1]
  }
  /return Image\(/ {
    match($0, /Image\("([^"]+)"\)/, b)
    if (b[1] != "" && last_code != "") {
      print last_code "\t" b[1]
      last_code = ""
    }
  }
' Core/CurrencyEnums.swift > /tmp/calcurate_code_to_imageset.tsv

wc -l /tmp/calcurate_code_to_imageset.tsv  # expect ~150 rows
head -10 /tmp/calcurate_code_to_imageset.tsv
```

Expected sample output:
```
USD	usa
EUR	euro
GBP	pound
AFN	afghani
ALL	albanian
...
```

If the awk script doesn't produce a complete mapping (e.g., due to `case` clauses spanning multiple lines), fall back to manually transcribing from `CurrencyEnums.swift:229–377` by reading the file directly. The mapping is well-formed in source — each enum case has a one-line `return Image("name")`.

- [ ] **Step 5.2: Copy missing imagesets to Android drawables**

For each row in the TSV, target Android filename is `<code.lowercase()>.png`. Skip codes that already have an Android drawable. Script:

```bash
ANDROID_DRAWABLE="/Users/nursultanyelemessov/Desktop/Desktop - Nursultan’s MacBook Pro (2)/Projects/Freelance/Shakhin Zhursinbek/CalcuRate Android/Calcurate-Android/app/src/main/res/drawable"
IOS_ASSETS="/Users/nursultanyelemessov/Desktop/Desktop - Nursultan’s MacBook Pro (2)/Projects/Freelance/Shakhin Zhursinbek/CalcurateApp/CalcuRate 3 decimals/CalcuRateApp/Calcura+e/Assets.xcassets"

while IFS=$'\t' read -r code imageset; do
    target_lower=$(echo "$code" | tr '[:upper:]' '[:lower:]')
    target_path="$ANDROID_DRAWABLE/${target_lower}.png"
    if [[ -f "$target_path" ]]; then
        echo "SKIP (exists): $code -> $target_lower.png"
        continue
    fi
    # iOS imageset folder may be PascalCase or lowercase; check both
    src_dir=""
    for candidate in "$IOS_ASSETS/${imageset}.imageset" "$IOS_ASSETS/$(echo "$imageset" | sed 's/.*/\u&/').imageset"; do
        if [[ -d "$candidate" ]]; then src_dir="$candidate"; break; fi
    done
    if [[ -z "$src_dir" ]]; then
        echo "MISS (no imageset): $code expected $imageset.imageset"
        continue
    fi
    # The PNG inside is named like {imageset}.png or {imageset}@2x.png; prefer the highest-resolution
    src_png=$(ls -S "$src_dir"/*.png 2>/dev/null | head -1)
    if [[ -z "$src_png" ]]; then
        echo "MISS (no PNG): $code in $src_dir"
        continue
    fi
    cp "$src_png" "$target_path"
    echo "COPY: $code <- $(basename "$src_png") -> ${target_lower}.png"
done < /tmp/calcurate_code_to_imageset.tsv
```

Note: Android drawable filenames must be lowercase and start with a letter — `code.lowercase()` is always safe (3-letter ISO codes). After the loop, run:

```bash
ls "$ANDROID_DRAWABLE"/*.png | wc -l
```

Expect ~150+ PNGs.

- [ ] **Step 5.3: Manually port any "MISS" entries**

If Step 5.2 reported any MISS rows (imageset name mismatch — iOS folder naming isn't perfectly consistent), inspect each one and copy manually. Common causes: iOS uses `peso.imageset` for MXN but the Swift code says `Image("peso")` while the enum is `MexicanPeso = "MXN"`. The awk script extracts code=MXN, image=peso correctly, but the imageset folder might be `Mexico.imageset` instead of `peso.imageset`. For each MISS:

```bash
ls "$IOS_ASSETS" | grep -i <stem>
```

— manually find the correct folder and `cp` it to `<code.lowercase()>.png`.

- [ ] **Step 5.4: Replace crypto XML stubs with iOS PNGs**

The 8 crypto drawables are currently colored-circle XML stubs. Replace each with the iOS branded PNG:

```bash
ANDROID_DRAWABLE="/Users/nursultanyelemessov/Desktop/Desktop - Nursultan’s MacBook Pro (2)/Projects/Freelance/Shakhin Zhursinbek/CalcuRate Android/Calcurate-Android/app/src/main/res/drawable"
IOS_ASSETS="/Users/nursultanyelemessov/Desktop/Desktop - Nursultan’s MacBook Pro (2)/Projects/Freelance/Shakhin Zhursinbek/CalcurateApp/CalcuRate 3 decimals/CalcuRateApp/Calcura+e/Assets.xcassets"

for sym in btc eth usdt bnb sol xrp ada doge; do
    src_dir="$IOS_ASSETS/${sym}.imageset"
    if [[ ! -d "$src_dir" ]]; then echo "MISS: $sym"; continue; fi
    src_png=$(ls -S "$src_dir"/*.png 2>/dev/null | head -1)
    if [[ -z "$src_png" ]]; then echo "MISS PNG: $sym"; continue; fi
    rm -f "$ANDROID_DRAWABLE/crypto_${sym}.xml"
    cp "$src_png" "$ANDROID_DRAWABLE/crypto_${sym}.png"
    echo "Replaced crypto_${sym}: XML -> PNG"
done
```

This removes the XML stubs and replaces them with branded PNGs at the same resource name (`R.drawable.crypto_btc` etc.) — no Kotlin changes needed because `CryptoItem.iconResId` already points to those resource ids.

- [ ] **Step 5.5: Update CurrencyButton.kt with the full mapping**

Edit `app/src/main/java/com/thecalcurate/android/ui/CurrencyButton.kt`. The current `when` has ~30 active entries and ~140 commented-out lines (one per currency). The new PNGs are at `R.drawable.<code.lowercase()>`. Uncomment each commented `"<CODE>" -> R.drawable.<code>` line.

This is a single-file mechanical change. Run:

```bash
sed -i '' 's|//                "\([A-Z]\{3\}\)" -> R.drawable.\([a-z]\{3,4\}\)$|                "\1" -> R.drawable.\2|g' app/src/main/java/com/thecalcurate/android/ui/CurrencyButton.kt
```

Then verify by reading the file: `grep -c '^//.*R.drawable' app/src/main/java/com/thecalcurate/android/ui/CurrencyButton.kt` — should drop close to 0.

Manually review any remaining commented lines — they may reference resource names that don't match the lowercase-of-code pattern (e.g., `R.drawable.tryn` for TRY, intentional renaming because `try` is reserved in Java). Leave those commented OR fix the resource name to match what was actually copied in Step 5.2 (`try.png` → `tryn.png` rename, OR uncomment with the actual filename).

- [ ] **Step 5.6: Build dev debug**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`. Unresolved references in `CurrencyButton.kt` mean Step 5.5 uncommented a line whose target PNG wasn't actually created in Step 5.2 — re-check and either copy the missing PNG or re-comment the line.

- [ ] **Step 5.7: Commit**

```bash
git add app/src/main/res/drawable/ app/src/main/java/com/thecalcurate/android/ui/CurrencyButton.kt
git commit -m "feat(assets): port iOS flag and crypto PNGs; complete CurrencyButton mapping

Adds ~140 missing fiat flag PNGs from iOS Assets.xcassets and replaces the
8 colored-XML crypto stubs with iOS branded coin icons. CurrencyButton.kt
fallback to worldCountryData library remains for any code not in the iOS
imageset list."
```

- [ ] **Step 5.8: Code-review checkpoint**

Invoke `code-review:code-review` on the Phase 5 commit. Concerns: missing PNGs causing fallback, ProGuard/R8 implications (none — assets aren't shrunk in debug), commit size (large but isolated to assets).

---

## Self-review notes

Coverage check:
- Issue 1 (image assets) → Phase 5 ✓
- Issue 2 (remove scroll) → Phase 2 ✓
- Issue 3 (weird picker background) → Phase 1 ✓
- Extra: haptic on long-press → Phase 3 ✓
- Extra: button press animation → Phase 4 ✓

Risk areas:
- **Phase 2 Step 2.4 (layout constraint rerouting):** Removing views from a ConstraintLayout can break the visual flow if other views' constraints anchored to the deleted views. Implementation must grep for `@id/txvSroll`, `@id/txvSwipeUp`, `@id/txvSwipeDown` references before deletion and reroute each.
- **Phase 5 Step 5.5 (sed mapping):** Naming edge cases (TRY → tryn because `try` is reserved, possibly others). Manual review after the sed pass is required, not optional.
- **Phase 4 (animation):** `view.animate()` queues separate animations; the `cancel()` + chained withEndAction pattern is correct but readers may not recognize it. If rapid repeated taps cause stuck alpha values, switch to AnimatorSet for atomicity (likely not needed — withEndAction always fires).

Out of scope (deferred):
- Operator-button "selected" pink-border state from iOS
- Network connectivity offline alert
- Ad banner
- Localization
