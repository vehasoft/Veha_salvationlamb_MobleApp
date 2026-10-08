# ADD_POST Module Agent

> Team: **FEED** · Reports to: `agents/FEED/FEED_LEAD.md`
> This agent knows **every detail** of the Create Post screen: the four inputs, the type radio
> group, the image picker with its permission flow, base64 encoding, the single API call, and
> navigation.
> It may only edit the files listed in §11 **Owned files**.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name | **Create Post** |
| Class | `com.veha.activity.AddPostActivity` (292 lines) |
| Layout | `app/src/main/res/layout/activity_add_post.xml` (10 ids) |
| Manifest | `<activity android:name=".AddPostActivity" android:exported="false"/>` |
| View access | **`findViewById`** — no synthetics, no ViewBinding |
| Entered from | `HomeFragment`'s `add_post` FAB — **visible only when `Util.isWarrior`** |
| Exits to | `MainActivity` on success; `LoginActivity` in the (dead) session-lost branch |
| Progress | `SpotsDialog`, "Please Wait", dismissed immediately after creation |
| Gating | **warrior-only**, enforced by the caller hiding the FAB — this screen performs **no role check of its own** (issue AP1) |

---

## 2. UI inventory

| View id | Type | Hint / text | Notes |
|---|---|---|---|
| `createpostLinear` | `LinearLayout` | heading **"Create Post"** | card container |
| `title` | `EditText` | hint **"Title"** | optional — never validated |
| `tags` | `EditText` | hint **"Tags"** | comma-separated; rendered as `#tag` by `HomeAdapter.getTags` |
| `content` | `EditText` | hint **"Content"** | the only validated field, and only for text posts |
| `post_type` | `RadioGroup` | — | nothing selected by default |
| `image_btn` | `RadioButton` | **"Image"** | also wired with its own click listener -> `addImg()` |
| `video_btn` | `RadioButton` | **"Video"** | reveals `video`, hides `post_img` |
| `video` | `EditText` | hint **"Video Url"** | a YouTube URL; hidden until Video is chosen |
| `post_img` | `ImageView` | — | preview of the picked image; hidden until one is chosen |
| `add_img_btn` | `Button` | **"Add Image"** | -> `addImg()` |
| `post_btn` | `Button` | **"Post"** | submits |

### Type selection behaviour

| Action | `postTypeStr` | Visibility changes |
|---|---|---|
| initial | **`"text"`** | `video` and `post_img` hidden |
| tap `image_btn` | `"image"` | *(no visibility change here)* — but its click listener also opens the picker |
| tap `video_btn` | `"video"` | `video` VISIBLE, `post_img` GONE |
| image picked | `"image"` | `post_img` VISIBLE, `video` GONE |

**Asymmetry:** the `video_btn` branch changes visibility, the `image_btn` branch does not — the image
layout only appears once a picture is actually chosen (issue AP2). `image_btn` carries **two**
listeners (the radio-group change *and* its own `setOnClickListener`), so selecting it fires the
picker immediately (issue AP3).

---

## 3. Image capture flow

```
addImg()
  |-- permission granted (READ_EXTERNAL_STORAGE OR READ_MEDIA_IMAGES)
  |     dialog "Add Picture!" with 3 items
  |        "Take Photo"          -> cameraIntent()  requestCode 150
  |        "Choose from Gallery" -> galleryIntent() requestCode 100
  |        "Cancel"              -> dismiss
  `-- not granted
        dialog "Permission Restricted"
          positive "cancel"   -> cancel
          negative "settings" -> ACTION_APPLICATION_DETAILS_SETTINGS
```

| Fact | Detail |
|---|---|
| Permission check | `checkSelfPermission` only — **it never requests anything**; the user is sent to system settings instead (issue AP4) |
| Gallery result (100) | `MediaStore.Images.Media.getBitmap(contentResolver, data.data)` — **deprecated** API, decoded on the **main thread** (issue AP5) |
| Camera result (150) | reads `data.extras!!["data"]` — a **thumbnail**, not the full image (issue AP6), with `!!` on `extras` (issue AP7) |
| Encoding | `encodeTobase64(bitmap)` -> JPEG quality **100** -> `Base64.DEFAULT` string |
| Camera compress | the camera path also compresses to a `ByteArrayOutputStream` at quality 90 — **and then discards it**, calling `encodeTobase64` anyway (issue AP8) |
| Button labels | the permission dialog has **"cancel" as positive and "settings" as negative** — inverted (issue AP9) |
| Copy | the message says *"updating your profile picture"* on the **create-post** screen (issue AP10) |

`Base64.DEFAULT` inserts line breaks into the encoded string, which is then embedded in JSON
(issue AP11).

---

## 4. Validation

Only two rules, both in the `post_btn` listener:

| Order | Rule | Effect |
|---|---|---|
| 1 | `postPicStr.isNullOrEmpty() && postTypeStr == "image"` | silently downgrades the type back to **`"text"`** |
| 2 | `content.text.trim().isNullOrEmpty() && postTypeStr == "text"` | `content.error = "Content must not be empty"` |

**Not validated:** `title` (may be empty), `tags` (free text), and — critically — **`video`**: a
video post with an empty or malformed URL is accepted and posted (issue AP12). There is no length
limit on any field.

`post_btn.isEnabled = false` is set before the call and restored in both response branches — a
double-submit guard, though **not** restored in `onFailure` (issue AP13).

---

## 5. API contract

| Item | Value |
|---|---|
| Retrofit | `postCallHead("Bearer $token", "post", data)` — the **dynamic-path** method (NETWORK N2) |
| HTTP | `POST api/v1/post` |
| Guard | `Commons().isNetworkAvailable(this)` |
| Token | `userPreferences.authToken.asLiveData().observe(this)` with the **always-true** guard (issue AP14) |
| Debug log | `Log.e("data", data.toString())` — **logs the full base64 image** to logcat (issue AP15) |

**Body (7 keys)**

| Key | Value |
|---|---|
| `title` | `title` text (untrimmed) |
| `content` | `content` text (untrimmed) |
| `tags` | `tags` text |
| `image` | the base64 string, or `""` |
| `url` | `video` text, or `""` |
| `type` | `"text"` / `"image"` / `"video"` |
| `userId` | `Util.userId` |

> The body key is **`image`**, but `Posts` exposes the field as **`picture`** — the server renames
> it. Do not "align" them without checking with PLATFORM/NETWORK (issue AP16).

**Success (200)** — clears `title` and `content` (**not** `tags`, `video` or the image — issue AP17),
re-enables the button, starts `MainActivity` and `finish()`es.

**Error (non-200)** — re-enables the button. **Nothing else**: the error parsing is commented out and
**no message is shown** (issue AP18).

**`onFailure`** — dismisses the dialog and logs. No user feedback, and the button stays disabled.

A stray `if (dialog.isShowing) dialog.dismiss()` sits **outside** the observer, running synchronously
before the response arrives — so the progress dialog is usually dismissed immediately (issue AP19).

---

## 6. Storage & global state

| Item | Operation |
|---|---|
| DataStore `token` | read (observed) |
| DataStore `token` + `userId` | deleted in the unreachable logout branch |
| `Util.userId` | read -> body |
| `Util.isWarrior` | **not read here** — gating is the caller's job |

---

## 7. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | layout -> SpotsDialog (then dismissed) -> `UserPreferences` -> 10 `findViewById`s -> wire the radio group and 3 buttons |
| `onActivityResult` | handles 100 (gallery) and 150 (camera); **ignores `resultCode`**, so a cancelled pick still runs (issue AP20) |
| `onPause` / `onResume` / `onDestroy` | all three just `dialog.dismiss()` — `onResume` kills the spinner of an in-flight request (issue AP21) |

**No rotation handling:** `EditText`s keep their text, but `postPicStr`, `postTypeStr` and the
preview bitmap are **lost** — the user silently posts text instead of an image (issue AP22).

`startActivityForResult` / `onActivityResult` are **deprecated** in favour of the Activity Result
API (issue AP23).

---

## 8. Navigation

| Trigger | Destination | `finish()`? |
|---|---|---|
| Post 200 | `MainActivity` | **yes** |
| Session lost | `LoginActivity` | no (**dead code**) |
| Back | previous screen (default) | — |

> Going to `MainActivity` rather than back to the feed means the new post appears only because the
> feed reloads from scratch; no result is passed back (issue AP24).

---

## 9. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| `RetrofitAPI.postCallHead` + `Util.getRetrofit()` | PLATFORM / NETWORK | the post call |
| `UserPreferences` | PLATFORM / STORAGE | token |
| `Util.userId`, `Commons()` | PLATFORM / COMMONS | body + guard |
| `CAMERA`, `READ_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES` | PLATFORM / BUILD_CONFIG | image capture |
| `HomeFragment` | FEED / HOME_FEED | the only entry point; owns warrior gating |
| `MainActivity` | APPSHELL / MAIN_NAV | success destination |

---

## 10. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| AP15 | `Log.e("data", data.toString())` prints the **entire base64 image** to logcat | **High** | remove the log |
| AP18 | A non-200 shows **no message**; error parsing is commented out | **High (UX)** | restore and toast |
| AP14 | The always-true token guard; the logout branch is dead | **High** | use `&&` |
| AP6 | The camera path stores only the **thumbnail** from `extras["data"]` | **High** | use a `FileProvider` and the full-size file |
| AP5 | `MediaStore.Images.Media.getBitmap` (deprecated) decodes on the **main thread** | **High** | `ImageDecoder` off the main thread |
| AP22 | Rotation loses the picked image and the post type | **High** | save to `onSaveInstanceState` |
| AP4 | Permissions are **checked but never requested** — the user is bounced to settings | **High (UX)** | call `requestPermissions` first |
| AP11 | Base64 is sent inside JSON (`Base64.DEFAULT` adds newlines); no size limit | **High** | multipart upload (with NETWORK) |
| AP19 | A stray `dialog.dismiss()` outside the observer dismisses the spinner early | Medium | remove it |
| AP13 | `post_btn` is not re-enabled in `onFailure` | Medium | re-enable |
| AP12 | A video post is accepted with an **empty or invalid URL** | Medium | validate the URL |
| AP20 | `resultCode` is ignored in `onActivityResult` | Medium | check `RESULT_OK` |
| AP17 | Success clears only `title` and `content`, leaving tags/url/image | Medium | reset everything |
| AP21 | `onResume` dismisses the dialog of an in-flight request | Medium | remove the override |
| AP3 | `image_btn` has two listeners, so selecting the radio immediately opens the picker | Medium | pick one trigger |
| AP2 | Image/Video visibility handling is asymmetric | Medium | handle both in the listener |
| AP1 | No warrior check on the screen itself — it is reachable by intent | Medium | verify `Util.isWarrior` in `onCreate` |
| AP9 | Permission dialog buttons are inverted ("cancel" positive, "settings" negative) | Low | swap |
| AP10 | Permission copy mentions *"profile picture"* on the post screen | Low | reword |
| AP8 | Dead compression in the camera path (quality 90, result discarded) | Low | delete |
| AP7 | `data.extras!!["data"]` double force-unwrap | Low | null-safe |
| AP23 | Deprecated `startActivityForResult` / `onActivityResult` | Low | Activity Result API |
| AP24 | Returns to `MainActivity` instead of the feed with a result | Low | `setResult` + `finish()` |
| AP16 | Body key `image` vs model field `picture` | Low | document only |

---

## 11. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/AddPostActivity.kt` | **exclusive** |
| `app/src/main/res/layout/activity_add_post.xml` | **exclusive** |
| `<activity android:name=".AddPostActivity">` in the manifest | shared with PLATFORM/BUILD_CONFIG |

**Not owned (escalate):** `RetrofitAPI.kt`, `UserPreferences.kt`, `Util.java`, `Commons.kt`,
manifest permissions, `HomeFragment` (the FAB that launches this screen).

---

## 12. How to make common changes

**Add a post type:** add a `RadioButton` to `post_type`, extend the `setOnCheckedChangeListener`
chain, set `postTypeStr`, and decide which inputs to show. **Then update `HOME_FEED.md`'s
`when (post.type)`** so the new type renders — a two-agent change inside FEED. Confirm the `type`
string with PLATFORM/NETWORK.

**Validate the video URL (AP12):** add an `else if` before the body builder, setting `video.error`.
Local to this agent.

**Switch to multipart upload (AP11):** needs a new `@Multipart` endpoint — **PLATFORM/NETWORK task
via PM** — plus rewriting `encodeTobase64` here.

**Request permissions properly (AP4):** add `ActivityCompat.requestPermissions` and
`onRequestPermissionsResult` before falling back to the settings dialog. Local, though the permission
list itself is BUILD_CONFIG.

**Return to the feed with the new post (AP24):** use `setResult(RESULT_OK)` + `finish()` and have
`HOME_FEED.md` refresh on result — coordinate with HOME_FEED.

---

## 13. Change log

| Change | Detail |
|---|---|
| Created | Initial ADD_POST module agent documented from `AddPostActivity.kt` (292 lines) and `activity_add_post.xml` (10 ids): all inputs, the type radio group with its asymmetric visibility handling, the full image flow (permission check, camera/gallery, base64), the 2 validation rules, the 7-key `POST api/v1/post` body, lifecycle, and 24 known issues. |



