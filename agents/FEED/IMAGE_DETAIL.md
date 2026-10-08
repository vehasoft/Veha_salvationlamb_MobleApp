# IMAGE_DETAIL Module Agent

> Team: **FEED** · Reports to: `agents/FEED/FEED_LEAD.md`
> This agent knows **every detail** of the full-screen image viewer: its single view, the
> pinch-to-zoom gesture handling, its two callers and the extra they pass.
> It may only edit the files listed in §8 **Owned files**.

---

## ⚠ Layout correction (important)

The team directory originally paired this screen with `preview_image.xml`. **That is wrong**, and
verified so:

| Layout | Actually used by |
|---|---|
| **`activity_image_detail.xml`** (`idIVImage`) | **this screen** |
| `preview_image.xml` (`iv_preview_image`, `btnIvClose`) | **`MainActivity`** — inflated into a `Dialog` at `MainActivity.kt:150` [APPSHELL] |

`preview_image.xml` is **not owned by this agent**; it belongs to APPSHELL/MAIN_NAV. The two are
unrelated image viewers that happen to do similar things (issue ID1).

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name | **Image viewer** (full-screen, pinch-to-zoom) |
| Class | `com.veha.activity.ImageDetailActivity` (43 lines — the **smallest activity in the app**) |
| Layout | `app/src/main/res/layout/activity_image_detail.xml` (1 id) |
| Manifest | `<activity android:name=".ImageDetailActivity" android:exported="false"/>` |
| View access | `findViewById` |
| Required extra | **`profilePic`** — a String URL |
| Entered from | `HomeAdapter` (post image tap, `post.picture`) and `ProfileFragment` (avatar tap, `picture`) |
| Exits to | nothing — system Back only |
| Progress | **none** |
| Network | **none** — Picasso loads directly from the URL |

> The extra is named **`profilePic`** even when it carries a **post** image, because the screen was
> written for avatars first and reused for posts (issue ID2).

---

## 2. UI inventory

| View id | Type | Purpose |
|---|---|---|
| `idIVImage` | `ImageView` | the whole screen; the image is loaded into it and scaled by gesture |

That is the entire UI: **one view, no toolbar, no close button, no loading indicator, no error
state** (issue ID3).

---

## 3. Behaviour

```kotlin
imgPath = intent.getStringExtra("profilePic")        // nullable, read correctly
imageView = findViewById(R.id.idIVImage)
scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
if (imgPath != null) { Picasso.with(this).load(imgPath).into(imageView) }
```

| Aspect | Detail |
|---|---|
| Extra read | `getStringExtra` **without** `.toString()` — a genuine `null`, correctly guarded. One of the few screens that gets this right (contrast AUTH A13, VIEW_LIKES VL7) |
| Image load | `Picasso.with(this)` — the **deprecated** 2.5.2 API; no `placeholder()`, no `error()`, no `fit()` |
| Null path | if `imgPath == null` the screen opens **blank** with no message (issue ID4) |

### Pinch-to-zoom

```kotlin
override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
    scaleGestureDetector!!.onTouchEvent(motionEvent)
    return true
}
private inner class ScaleListener : SimpleOnScaleGestureListener() {
    override fun onScale(d: ScaleGestureDetector): Boolean {
        mScaleFactor *= d.scaleFactor
        mScaleFactor = 0.1f.coerceAtLeast(mScaleFactor.coerceAtMost(10.0f))   // clamp 0.1x .. 10x
        imageView!!.scaleX = mScaleFactor
        imageView!!.scaleY = mScaleFactor
        return true
    }
}
```

| Aspect | Detail |
|---|---|
| Zoom range | **0.1x to 10x**, clamped |
| Implementation | manual `scaleX`/`scaleY` on the `ImageView` |
| **No panning** | once zoomed in, the user **cannot move around the image** — the zoomed region is fixed to the centre (issue ID5) |
| No double-tap | no double-tap-to-zoom or reset gesture (issue ID6) |
| No reset | `mScaleFactor` persists until the activity is destroyed |
| Touch handling | `onTouchEvent` always returns `true`, consuming **all** touch events |

---

## 4. Storage & global state

| Item | Operation |
|---|---|
| — | **none** |

This is the only FEED screen that touches no DataStore, no `Util` globals and no network.

---

## 5. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | `setContentView` -> read extra -> `findViewById` -> create the gesture detector -> load with Picasso |
| `onTouchEvent` | forwards to the scale detector |
| others | **none overridden** |

**No rotation handling:** the activity is recreated, the image reloads from the network (Picasso may
serve it from cache) and **`mScaleFactor` resets to 1.0** — the zoom level is lost (issue ID7).

---

## 6. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| Picasso 2.5.2 | PLATFORM / BUILD_CONFIG | image loading |
| `HomeAdapter` | FEED / HOME_FEED | caller (post images) |
| `ProfileFragment` | **PROFILE** / MY_PROFILE | caller (avatars) |

> **Cross-team note:** one of this screen's two callers belongs to **PROFILE**. Changing the extra
> name or adding a required extra breaks that screen — escalate to PM.

No PLATFORM/NETWORK, DATA_MODELS, STORAGE or COMMONS dependency at all.

---

## 7. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| ID5 | **Zoom without pan** — once magnified, the user cannot move around the image, making zoom nearly useless | **High (UX)** | add translation on drag, or use `PhotoView` |
| ID4 | A null/invalid URL shows a **blank screen** with no message and no way to tell it failed | **High (UX)** | add Picasso `placeholder()` / `error()` and a fallback |
| ID3 | No close button, no toolbar, no loading indicator — Back is the only exit | Medium | add a close affordance |
| ID7 | Rotation resets the zoom level | Medium | save `mScaleFactor` in `onSaveInstanceState` |
| ID6 | No double-tap to zoom/reset | Medium | add a `GestureDetector` |
| ID10 | No downsampling — a large image is decoded at full size | Medium | `fit().centerInside()` or explicit sizing |
| ID2 | The extra is called **`profilePic`** even when carrying a post image | Low | rename to `imageUrl` — **two-team change** (HOME_FEED + PROFILE) |
| ID1 | `preview_image.xml` is widely assumed to belong here but is actually used by `MainActivity` | Low | documented; ownership stays with APPSHELL |
| ID8 | `Picasso.with(...)` is deprecated | Low | upgrade with BUILD_CONFIG |
| ID9 | `imageView!!` / `scaleGestureDetector!!` force-unwraps instead of `lateinit` | Low | use `lateinit var` |
| ID11 | No immersive/full-screen flags, so system bars overlay the image | Low | consider immersive mode |

---

## 8. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/ImageDetailActivity.kt` | **exclusive** |
| `app/src/main/res/layout/activity_image_detail.xml` | **exclusive** |
| `<activity android:name=".ImageDetailActivity">` in the manifest | shared with PLATFORM/BUILD_CONFIG |

**Not owned (escalate):** `preview_image.xml` (**APPSHELL/MAIN_NAV**), `HomeAdapter.kt`
(HOME_FEED), `ProfileFragment.kt` (PROFILE), the Picasso dependency (BUILD_CONFIG).

---

## 9. How to make common changes

**Add panning (ID5):** track `MotionEvent` drags alongside the scale detector and apply
`translationX`/`translationY`, clamped to the scaled bounds. Local to this agent — the single
highest-value fix here.

**Add a close button (ID3):** add an `ImageButton` to `activity_image_detail.xml` calling `finish()`.
Local. (`preview_image.xml` already has `btnIvClose`, but that layout belongs to APPSHELL — do not
reuse it.)

**Handle load failures (ID4):** chain `.placeholder(...)` and `.error(...)` onto the Picasso call,
and show a message when `imgPath` is null instead of opening blank.

**Rename the `profilePic` extra (ID2):** it is written by `HomeAdapter` (FEED) **and**
`ProfileFragment` (PROFILE). All three files must change together — **PM-coordinated, two teams**.

**Support multiple images / swipe:** would need a `ViewPager2` and a list extra — a new contract with
both callers. Escalate to PM.

---

## 10. Change log

| Change | Detail |
|---|---|
| Created | Initial IMAGE_DETAIL module agent documented from `ImageDetailActivity.kt` (43 lines) and `activity_image_detail.xml` (1 id). **Corrected the layout attribution**: `preview_image.xml` is used by `MainActivity` (APPSHELL), not this screen. Recorded both callers (`HomeAdapter` in FEED and `ProfileFragment` in PROFILE), the `profilePic` extra, the 0.1x–10x pinch zoom with **no panning**, and 11 known issues. |


