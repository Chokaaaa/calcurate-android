# Restore Scroll, Remove Swipe Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reintroduce the vertical-swipe (scroll) behavior on slot buttons — up/down cycles through favorites or all currencies — and remove the horizontal-swipe (left/right slot swap) behavior. Tutorial step 2 swaps from a swipe demo to a scroll demo. The result-text backspace swipe on `imvResult` is OUT OF SCOPE and stays untouched.

**Architecture:** Four short phases. The MainActivity slot OnSwipeListener overrides change shape; `scrollCurrency()` returns; `switchMain()` is removed since its only callers were the swipe handlers. Tutorial XML overlays swap correspondingly. Two iOS PNGs port over (`swipe.up.imageset`, `swipe.down.imageset`). Code-review checkpoint after every commit per the established workflow.

**Tech Stack:** Kotlin, ConstraintLayout, existing `OnSwipeListener` class (untouched). No new dependencies.

**Reference (iOS source for parity):** swipe-up/down image assets at `/Users/nursultanyelemessov/Desktop/Desktop - Nursultan's MacBook Pro (2)/Projects/Freelance/Shakhin Zhursinbek/CalcurateApp/CalcuRate 3 decimals/CalcuRateApp/Calcura+e/Assets.xcassets/swipe.up.imageset/Group 10-2.png` and `…/swipe.down.imageset/Group 10-7.png`.

**Branch:** working on `bug/ReturningScrollandRemovingSwipe` (already created, tracking `origin/bug/ReturningScrollandRemovingSwipe`).

**Per-phase workflow:** build dev debug → commit → invoke `code-review:code-review` skill → fix any CONFIRMED/PLAUSIBLE finding as an interstitial fix BEFORE the next phase (per saved feedback memory `feedback-fix-review-findings-before-next-phase`).

---

## Phase 1: Restore scroll infrastructure (drawables + Kotlin)

**Goal:** Bring back what's needed for vertical-swipe to do something: the up/down drawables, the `scrollCurrency()` helper, `UP`/`DOWN` constants, and the three `onSwipeTop()` / `onSwipeBottom()` overrides on the slot buttons.

**Files:**
- Create: `app/src/main/res/drawable/swipe_up.png` (copied from iOS)
- Create: `app/src/main/res/drawable/swipe_down.png` (copied from iOS)
- Modify: `app/src/main/java/com/thecalcurate/android/MainActivity.kt` (constants, function, three listener overrides)

- [ ] **Step 1.1: Port the scroll PNGs from iOS**

Run from the project root:

```bash
ANDROID_DRAWABLE="app/src/main/res/drawable"
IOS_ASSETS="/Users/nursultanyelemessov/Desktop/Desktop - Nursultan’s MacBook Pro (2)/Projects/Freelance/Shakhin Zhursinbek/CalcurateApp/CalcuRate 3 decimals/CalcuRateApp/Calcura+e/Assets.xcassets"
cp "$IOS_ASSETS/swipe.up.imageset/Group 10-2.png"   "$ANDROID_DRAWABLE/swipe_up.png"
cp "$IOS_ASSETS/swipe.down.imageset/Group 10-7.png" "$ANDROID_DRAWABLE/swipe_down.png"
ls -la "$ANDROID_DRAWABLE/swipe_up.png" "$ANDROID_DRAWABLE/swipe_down.png"
```

- [ ] **Step 1.2: Add UP/DOWN constants and lateinit views in MainActivity**

Edit `app/src/main/java/com/thecalcurate/android/MainActivity.kt`. Find the existing `val VIBRATION_MILIS = 100L` field (added in a prior phase, around line 93). Add two constants directly above it:

```kotlin
    val UP = 1
    val DOWN = 2
    val VIBRATION_MILIS = 100L
```

Also add three lateinit View fields below the existing `lateinit var txvSwipeLeft: View` (around line 59):

```kotlin
    lateinit var txvSwipe: View
    lateinit var txvSwipeRight: View
    lateinit var txvSwipeLeft: View
    lateinit var txvSroll: View
    lateinit var txvSwipeUp: View
    lateinit var txvSwipeDown: View
```

(The first three already exist — only the last three are new.)

- [ ] **Step 1.3: Add scrollCurrency() function**

In `MainActivity.kt`, add this function just before the existing `switchMain()` definition (around line 884) — same indentation as other `private fun` declarations:

```kotlin
    private fun scrollCurrency(btn: ImageView, scrollType: Int) {
        try {
            val sharedPref =
                getSharedPreferences(getString(R.string.preference_file), Context.MODE_PRIVATE)
            val favourites =
                sharedPref?.getString(getString(R.string.saved_favourites_key), "") ?: ""

            favList = favourites.split(",").filter { it != "" }.toMutableList()

            val code = btn.getTag(R.id.code_tag_name)
            var toCode = ""

            if (favList.isNotEmpty()) {
                var index = favList.indexOf(code)
                if (index == -1 && scrollType == DOWN) index = 0
                toCode = if (scrollType == UP) {
                    favList[if (index == favList.size - 1) 0 else index + 1]
                } else {
                    favList[if (index == 0) favList.size - 1 else index - 1]
                }
            } else {
                val list = CurrencyItem.getList()
                val index = list.indexOf(list.find { it.code == code })
                toCode = if (scrollType == UP) {
                    list[if (index == list.size - 1) 0 else index + 1].code
                } else {
                    list[if (index == 0) list.size - 1 else index - 1].code
                }
            }
            setCur(btn, toCode)
            if (btn.id == secondarySelectedId) {
                txvResult.setResult(convertToSec(savedMainVal, toCode))
            }
            vibrate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
```

- [ ] **Step 1.4: Add onSwipeTop/onSwipeBottom to all three slot listeners**

In `setCurrencyBtn()` (lines ~733-784), modify each of the three slot button `setOnTouchListener` blocks. For `btn_main` (line ~733), inside the anonymous `OnSwipeListener` object, add two new overrides anywhere alongside the existing `onSwipeRight` / `onLongPress`:

```kotlin
                override fun onSwipeTop() {
                    super.onSwipeTop()
                    scrollCurrency(btn_main, UP)
                }

                override fun onSwipeBottom() {
                    super.onSwipeBottom()
                    scrollCurrency(btn_main, DOWN)
                }
```

For `btn_secondary1` listener (line ~750):

```kotlin
                override fun onSwipeTop() {
                    super.onSwipeTop()
                    scrollCurrency(btn_secondary1, UP)
                }

                override fun onSwipeBottom() {
                    super.onSwipeBottom()
                    scrollCurrency(btn_secondary1, DOWN)
                }
```

For `btn_secondary2` listener (line ~766):

```kotlin
                override fun onSwipeTop() {
                    super.onSwipeTop()
                    scrollCurrency(btn_secondary2, UP)
                }

                override fun onSwipeBottom() {
                    super.onSwipeBottom()
                    scrollCurrency(btn_secondary2, DOWN)
                }
```

(Don't remove the `onSwipeLeft`/`onSwipeRight` overrides yet — Phase 3 deletes them.)

- [ ] **Step 1.5: Build**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`. If "Unresolved reference: txvSwipeUp / txvSwipeDown / txvSroll" — those views don't exist in the layout yet but the `lateinit` declarations don't trigger compile errors. The build should pass because lateinit deferred refs only fail at runtime. The findViewById calls aren't added until Phase 2.

If you DO see compile errors here, comment out the three new `lateinit var` lines you added in Step 1.2 (txvSroll, txvSwipeUp, txvSwipeDown) — they'll be re-added cleanly in Phase 2.

- [ ] **Step 1.6: Commit**

```bash
git add app/src/main/res/drawable/swipe_up.png app/src/main/res/drawable/swipe_down.png app/src/main/java/com/thecalcurate/android/MainActivity.kt
git commit -m "feat(slots): restore scrollCurrency + UP/DOWN swipe handlers

Brings back vertical-swipe-to-cycle-currency on all three slot buttons.
Up swipes the next favorite (or next currency if no favorites); down
swipes the previous. Drawables swipe_up.png / swipe_down.png ported
from iOS swipe.up.imageset / swipe.down.imageset. The horizontal swipe
handlers (onSwipeLeft/onSwipeRight) and switchMain() are removed in a
later phase."
```

- [ ] **Step 1.7: Code-review checkpoint**

Invoke `code-review:code-review`.

---

## Phase 2: Add scroll tutorial views to activity_main.xml

**Goal:** Restore the three views the scroll demo step needs — `txvSwipeUp`, `txvSwipeDown`, `txvScroll` — and wire `findViewById` calls in MainActivity. The swipe tutorial views (`txvSwipe`, `txvSwipeLeft`, `txvSwipeRight`) stay in the layout for now; Phase 4 removes them.

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/java/com/thecalcurate/android/MainActivity.kt`

- [ ] **Step 2.1: Add the three scroll views to activity_main.xml**

Edit `app/src/main/res/layout/activity_main.xml`. Find the `<TextView android:id="@+id/txvSwipe" .../>` block (around line 424, the "swipe" label that disappears after Phase 4). Add this directly AFTER its closing `/>` and BEFORE the `<!--    </androidx.constraintlayout.widget.ConstraintLayout>-->` comment:

```xml
    <ImageView
        android:id="@+id/txvSwipeUp"
        android:layout_width="wrap_content"
        android:layout_height="60dp"
        android:layout_marginStart="-20dp"
        android:src="@drawable/swipe_up"
        android:layout_marginBottom="12dp"
        android:visibility="gone"
        app:layout_constraintBottom_toBottomOf="@id/btn_main"
        app:layout_constraintStart_toEndOf="@id/btn_main"
        tools:visibility="visible" />

    <ImageView
        android:id="@+id/txvSwipeDown"
        android:layout_width="wrap_content"
        android:layout_height="60dp"
        android:layout_marginBottom="12dp"
        android:layout_marginEnd="-44dp"
        android:src="@drawable/swipe_down"
        android:visibility="gone"
        app:layout_constraintBottom_toBottomOf="@id/btn_secondary2"
        app:layout_constraintEnd_toStartOf="@id/btn_secondary2"
        tools:visibility="visible" />

    <TextView
        android:id="@+id/txvScroll"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/txt_scroll"
        android:textColor="@color/white"
        android:layout_marginTop="-13dp"
        android:visibility="gone"
        app:layout_constraintBottom_toTopOf="@+id/btn_multi"
        app:layout_constraintEnd_toEndOf="@id/btn_secondary2"
        app:layout_constraintStart_toStartOf="@id/btn_main"
        app:layout_constraintTop_toBottomOf="@id/btn_main"
        tools:visibility="visible" />
```

`@string/txt_scroll` already exists in `strings.xml` (kept from before). Verify with `grep "txt_scroll" app/src/main/res/values/strings.xml`.

- [ ] **Step 2.2: Add findViewById calls in viewSetup**

In `MainActivity.kt`, find the existing `txvSwipeLeft = findViewById(R.id.txvSwipeLeft)` line (around line 429 inside `viewSetup()`). Add three lines directly after:

```kotlin
            txvSwipeLeft = findViewById(R.id.txvSwipeLeft)
            txvSroll = findViewById(R.id.txvScroll)
            txvSwipeUp = findViewById(R.id.txvSwipeUp)
            txvSwipeDown = findViewById(R.id.txvSwipeDown)
```

If Step 1.2's lateinit lines were commented out, uncomment them now.

- [ ] **Step 2.3: Build**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`. Unresolved references mean the new views' IDs don't match — re-grep `@+id/txvSwipeUp` etc.

- [ ] **Step 2.4: Commit**

```bash
git add app/src/main/res/layout/activity_main.xml app/src/main/java/com/thecalcurate/android/MainActivity.kt
git commit -m "feat(tutorial): restore txvSwipeUp/txvSwipeDown/txvScroll views (still hidden by default)"
```

- [ ] **Step 2.5: Code-review checkpoint**

Invoke `code-review:code-review`.

---

## Phase 3: Remove slot horizontal swipe + switchMain

**Goal:** Delete the three `onSwipeLeft` / `onSwipeRight` overrides on the slot buttons, and delete the now-orphaned `switchMain()` function. The result-text swipe (on `imvResult`) is NOT touched — it stays as backspace-via-swipe.

**Files:**
- Modify: `app/src/main/java/com/thecalcurate/android/MainActivity.kt`

- [ ] **Step 3.1: Delete onSwipeRight on btn_main**

In `setCurrencyBtn()`, find the block inside `btn_main.setOnTouchListener(...)` (around line 733). Remove the `onSwipeRight` override (and the `tutorialStep == 2` bump inside it, which is now handled by the new scroll step in Phase 4):

```kotlin
                override fun onSwipeRight() {
                    super.onSwipeRight()
                    switchMain(secondarySelectedId)
                    if (!isTutorialViewed && tutorialStep == 2) {
                        tutorialStep++
                        nextTutorialStep()
                    }
                }
```

After Step 1.4, btn_main has onSwipeRight, onSwipeTop, onSwipeBottom, onLongPress. After this step: only onSwipeTop, onSwipeBottom, onLongPress.

- [ ] **Step 3.2: Delete onSwipeLeft on btn_secondary1**

Find the block inside `btn_secondary1.setOnTouchListener(...)` (around line 750). Remove:

```kotlin
                override fun onSwipeLeft() {
                    super.onSwipeLeft()
//                if (isTutorialViewed)
                    switchMain(R.id.btn_secondary1)
                }
```

- [ ] **Step 3.3: Delete onSwipeLeft on btn_secondary2**

Find the block inside `btn_secondary2.setOnTouchListener(...)` (around line 768). Remove:

```kotlin
                override fun onSwipeLeft() {
                    super.onSwipeLeft()
                    switchMain(R.id.btn_secondary2)
                    if (!isTutorialViewed && tutorialStep == 2) {
                        tutorialStep++
                        nextTutorialStep()
                    }
                }
```

- [ ] **Step 3.4: Delete switchMain function**

Find `private fun switchMain(secId: Int = 0) { ... }` (around line 884). Delete the entire function body — about 20 lines. After Steps 3.1-3.3 nobody calls it.

Verify there are no remaining callers:

```bash
grep -n "switchMain" app/src/main/java/com/thecalcurate/android/MainActivity.kt
```

Expected: no matches.

- [ ] **Step 3.5: Add scroll-step tutorial advance to onSwipeTop/onSwipeBottom**

The tutorial flow now advances on a scroll instead of a swipe. In `btn_main`'s `onSwipeTop` override (just added in Phase 1), extend it with the tutorial bump:

Replace:

```kotlin
                override fun onSwipeTop() {
                    super.onSwipeTop()
                    scrollCurrency(btn_main, UP)
                }
```

With:

```kotlin
                override fun onSwipeTop() {
                    super.onSwipeTop()
                    scrollCurrency(btn_main, UP)
                    if (!isTutorialViewed && tutorialStep == 2) {
                        tutorialStep++
                        nextTutorialStep()
                    }
                }
```

Do the same for `onSwipeBottom` on `btn_main`:

```kotlin
                override fun onSwipeBottom() {
                    super.onSwipeBottom()
                    scrollCurrency(btn_main, DOWN)
                    if (!isTutorialViewed && tutorialStep == 2) {
                        tutorialStep++
                        nextTutorialStep()
                    }
                }
```

Same for `btn_secondary2` (both onSwipeTop and onSwipeBottom):

```kotlin
                override fun onSwipeTop() {
                    super.onSwipeTop()
                    scrollCurrency(btn_secondary2, UP)
                    if (!isTutorialViewed && tutorialStep == 2) {
                        tutorialStep++
                        nextTutorialStep()
                    }
                }

                override fun onSwipeBottom() {
                    super.onSwipeBottom()
                    scrollCurrency(btn_secondary2, DOWN)
                    if (!isTutorialViewed && tutorialStep == 2) {
                        tutorialStep++
                        nextTutorialStep()
                    }
                }
```

`btn_secondary1`'s overrides don't get the tutorial bump (mirroring the prior swipe-tutorial behaviour where btn_secondary1 swipes didn't advance the tutorial either).

- [ ] **Step 3.6: Build**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3.7: Commit**

```bash
git add app/src/main/java/com/thecalcurate/android/MainActivity.kt
git commit -m "refactor(slots): remove horizontal-swipe (switchMain) on slots; scroll now advances the tutorial"
```

- [ ] **Step 3.8: Code-review checkpoint**

Invoke `code-review:code-review`.

---

## Phase 4: Update tutorial step 2 + clean up swipe tutorial views/drawables

**Goal:** The tutorial step 2 now demonstrates the scroll gesture (up/down) instead of the swipe gesture (left/right). Old swipe tutorial views and their PNG drawables are removed.

**Files:**
- Modify: `app/src/main/java/com/thecalcurate/android/MainActivity.kt` (nextTutorialStep, lateinit cleanup)
- Modify: `app/src/main/res/layout/activity_main.xml` (delete swipe views)
- Delete: `app/src/main/res/drawable/swipe_left.png`
- Delete: `app/src/main/res/drawable/swipe_right.png`

- [ ] **Step 4.1: Update nextTutorialStep to show scroll views on step 2**

In `MainActivity.kt`, find `nextTutorialStep()` (around line 790). Replace the `tutorialStep == 2` branch's body:

```kotlin
        if (tutorialStep == 2) {
            txvHold.visibility = View.GONE
            imgHold.visibility = View.GONE
//            btn_secondary1.visibility = View.VISIBLE
//            btn_secondary2.visibility = View.VISIBLE
//            txvSwipe.visibility = View.VISIBLE
////            txvSwipeRight.visibility = View.VISIBLE
//            txvSwipeLeft.visibility = View.VISIBLE

            fadeOut(txvHold)
            fadeOut(imgHold)

            btn_secondary2.callOnClick()
            btn_secondary2_tut.callOnClick()

            fadeIn(btn_secondary2)
            fadeIn(txvSwipe)
            fadeIn(txvSwipeLeft)
            fadeIn(txvSwipeRight)

        } else if (tutorialStep == 3) {
            // Scroll-step was removed (iOS doesn't have it); step 3 now finishes the tutorial.
            fadeOut(txvSwipe)
            fadeOut(txvSwipeLeft)
            fadeOut(txvSwipeRight)
            fadeIn(imvCloseTutorial)
        }
```

With:

```kotlin
        if (tutorialStep == 2) {
            txvHold.visibility = View.GONE
            imgHold.visibility = View.GONE

            fadeOut(txvHold)
            fadeOut(imgHold)

            btn_secondary2.callOnClick()
            btn_secondary2_tut.callOnClick()

            fadeIn(btn_secondary2)
            fadeIn(txvSroll)
            fadeIn(txvSwipeUp)
            fadeIn(txvSwipeDown)

        } else if (tutorialStep == 3) {
            fadeOut(txvSroll)
            fadeOut(txvSwipeUp)
            fadeOut(txvSwipeDown)
            fadeIn(imvCloseTutorial)
        }
```

- [ ] **Step 4.2: Remove txvSwipe/txvSwipeLeft/txvSwipeRight lateinit fields and findViewById**

In `MainActivity.kt`, delete the three lateinit lines:

```kotlin
    lateinit var txvSwipe: View
    lateinit var txvSwipeRight: View
    lateinit var txvSwipeLeft: View
```

And the three `findViewById` lines in `viewSetup()`:

```kotlin
            txvSwipe = findViewById(R.id.txvSwipe)
            txvSwipeRight = findViewById(R.id.txvSwipeRight)
            txvSwipeLeft = findViewById(R.id.txvSwipeLeft)
```

- [ ] **Step 4.3: Remove the swipe views from activity_main.xml**

Delete the three `<ImageView android:id="@+id/txvSwipeRight" .../>`, `<ImageView android:id="@+id/txvSwipeLeft" .../>`, `<TextView android:id="@+id/txvSwipe" .../>` blocks (lines ~400-435).

After deletion, verify nothing in the layout references them:

```bash
grep -n "txvSwipe[^UD]\|txvSwipeLeft\|txvSwipeRight\|@id/txvSwipe\b" app/src/main/res/layout/activity_main.xml
```

Expected: no matches (the `[^UD]` excludes txvSwipeUp/Down which we want to keep).

- [ ] **Step 4.4: Delete orphan swipe drawables**

```bash
rm app/src/main/res/drawable/swipe_left.png app/src/main/res/drawable/swipe_right.png
```

- [ ] **Step 4.5: Build**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`. Clean rebuild because macOS file-duplicate dex pollution can foul incremental builds after large file removals.

- [ ] **Step 4.6: Verify the unused `activity_tutorial.xml`** (TutorialActivity is dead code per prior analysis but its XML still must compile)

```bash
grep -n "swipe_left\|swipe_right" app/src/main/res/layout/activity_tutorial.xml
```

If matches, delete those `<ImageView>` blocks too. If no matches, this step is a no-op.

- [ ] **Step 4.7: Commit**

```bash
git add app/src/main/java/com/thecalcurate/android/MainActivity.kt app/src/main/res/layout/activity_main.xml app/src/main/res/drawable/swipe_left.png app/src/main/res/drawable/swipe_right.png app/src/main/res/layout/activity_tutorial.xml
git commit -m "feat(tutorial): step 2 now demos scroll (was swipe); cleanup swipe assets

- nextTutorialStep step 2 fades in scroll views (txvSwipeUp/Down/Scroll)
  instead of the old txvSwipeLeft/Right/Swipe
- Removes the three swipe-tutorial views, their lateinit fields, their
  findViewById calls, and the swipe_left.png / swipe_right.png drawables
- TutorialActivity layout cleaned up if it referenced the deleted drawables"
```

- [ ] **Step 4.8: Code-review checkpoint**

Invoke `code-review:code-review`.

---

## Self-review notes

Spec coverage:
- Bring scroll back → Phase 1 (drawables + Kotlin) + Phase 2 (layout views) ✓
- Remove slot horizontal swipe → Phase 3 (delete handlers + switchMain) ✓
- Tutorial: step 2 demos scroll → Phase 4 (nextTutorialStep + clean swipe tutorial views) ✓
- Preserve imvResult swipe (backspace) → NOT touched in any phase ✓

Risk areas:
- **Phase 1 Step 1.2's lateinit declarations** create fields that won't have findViewById until Phase 2. Kotlin lateinit doesn't fail at declaration — only at access. Phase 1 doesn't access them, so the build passes. If a defensive engineer wants to delay them to Phase 2, the alternative is in Step 1.5's fallback note.
- **Phase 3 Step 3.5 (tutorial-bump in onSwipeTop/Bottom):** btn_secondary1's overrides intentionally don't bump the tutorial — mirroring the prior btn_secondary1.onSwipeLeft behaviour (also didn't bump). Verify in smoke testing that scrolling on btn_main OR btn_secondary2 advances the tutorial.
- **Phase 4 Step 4.1 layout strings:** the `txt_swipe` string in strings.xml becomes orphaned after the swipe views are deleted. Leaving it as orphan is harmless (small APK overhead). Cleanup is out of scope.

Out of scope (deferred):
- `imvResult` swipe behaviour (backspace on calc display)
- The deprecated `txt_swipe` string in strings.xml
- TutorialActivity itself (dead code, not invoked)
