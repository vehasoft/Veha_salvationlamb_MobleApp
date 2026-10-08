# EDIT_PROFILE Module Agent

> Team: **PROFILE** · Reports to: `agents/PROFILE/PROFILE_LEAD.md`
> This agent knows **every detail** of the Edit Profile screen — the **largest file in the app**
> (738 lines): 31 views, a 14-field form, three cascading geo spinners, an image-crop pipeline and
> four API calls.
> It may only edit the files listed in §12 **Owned files**.

---

## ⚠ Size warning

At **738 lines**, `EditProfileActivity` is roughly 40% larger than the next biggest file
(`HomeAdapter`, 680). It mixes six concerns in one class:

1. a 14-field form with validation,
2. a change-detection pass (`isDataChanged`),
3. three cascading spinners (country -> state -> city),
4. gallery/camera capture plus `CropImage`,
5. four network calls,
6. the duplicated app-wide overflow menu.

**Expect any change here to be riskier than elsewhere.** Read the relevant section fully before
editing (team issue F-4).

> **Layout note:** `activity_edit_profile.xml` shares **14 view ids** with `activity_register.xml`,
> which is why `RegisterActivity` has to patch its synthetic imports (REGISTER R2). Adding an id here
> can break **Register** — notify AUTH via PM.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name | **Edit Profile** |
| Class | `com.veha.activity.EditProfileActivity` (**738 lines**) |
| Layout | `app/src/main/res/layout/activity_edit_profile.xml` (**31 ids**) |
| Manifest | `<activity android:name=".EditProfileActivity" android:exported="true"/>` — **exported with no intent-filter** (BUILD_CONFIG B-7) |
| View access | `findViewById` **and** Kotlin synthetics, mixed in the same class (issue EP1) |
| Entered from | the overflow menu (`edit_profile`) on 6+ screens, and `AboutActivity`'s row |
| Exits to | `MainActivity` (logo), overflow destinations, `CropImageActivity` |
| Progress | `SpotsDialog`, "Please Wait" |
| StrictMode | `onCreate` sets a permissive `VmPolicy` via `StrictMode.setVmPolicy(VmPolicy.Builder().build())` — **disabling the default file-URI protections** so `getImageUri` can pass a raw `file://` URI (issue EP2) |

---

## 2. UI inventory (31 ids)

| Group | View ids |
|---|---|
| Header | `header_main`, `prod_logo`, `menu` |
| Avatar | `profile_pic_edit` |
| Name | `fname_op` + `fname`, `lname_op` + `lname` |
| Contact | `email_op` + `email`, `mobile_op` + `mobile` |
| DOB | `date` |
| Gender | `gender` (RadioGroup) + `male`, `female`, **`other`** |
| Address | `address_op` + `address`, `pincode_layout` + `pincode` |
| Geo | `country_layout` + `country`, `state_layout` + `state`, `city_layout` + `city` |
| Other | `language_layout` + `language` |
| Actions | `warrior`, `save_btn`, `cancel_btn` |

- The three geo fields are **`AutoCompleteTextView`-style** inputs driven by `onItemClickListener`,
  not plain `Spinner`s.
- **`other`** gender exists here but is **commented out in Register** — the two screens disagree on
  the available options (issue EP3).
- `warrior` is hidden when `Util.isWarrior`; it triggers `Commons().makeWarrior` (whose return value
  is broken — COMMONS C-12).

---

## 3. The cascading geo spinners

```
getCountries()                 GET api/v1/Country   (no auth)
   `-> user picks a country -> getStates(countrystr)   GET api/v1/state/{countryID}
          `-> user picks a state -> getCities(statestr) GET api/v1/city/{stateId}
```

| Fact | Detail |
|---|---|
| Seeding | `state` is pre-seeded with the literal `"State"` and `city` with `"City"` as placeholder rows |
| Adapters | `ArrayAdapter(this, R.layout.spinner_text, list)` with a dropdown resource |
| Guards | each listener ignores the value when it equals **`"Country"`** — even the state and city listeners compare against `"Country"`, so the `"State"`/`"City"` placeholders are **accepted as real values** (issue EP4) |
| Identifiers | `getStates(countrystr)` passes the **country name**, not an id, into a path expecting `{countryID}` — it works only because the backend accepts names (issue EP5) |
| Resets | changing the country does **not** clear the previously chosen state/city (issue EP6) |
| Auth | all three endpoints are **unauthenticated** (NETWORK §3.2) |

---

## 4. Validation — `doValidation(): String`

An `if / else if` chain returning `"SUCCESS"` or the first error; each branch also sets the field
`.error`.

| Order | Field | Rule | Message |
|---|---|---|---|
| 1 | `fname` | empty | `Enter name` |
| 2 | `fname` | `!Util.isValidName` | `Enter valid name` |
| 3 | `email` | empty | `Enter email` |
| 4 | `email` | `!Util.isValidEmail` | `Invalid Email` |
| 5 | `mobile` | empty | `Enter mobile number` |
| 6 | `mobile` | `!Util.isValidMobile` | `Enter valid mobile number` |
| 7 | `date` | empty **or** equals `"Date of birth"` | `Select Date of birth` |
| — | | all pass | `"SUCCESS"` |

**Not validated:** `lname`, `address`, `pincode`, `language`, country/state/city, gender. This is the
**same gap as REGISTER** (R9) — last name is never checked (issue EP7). The sentinel return value is
the string `"SUCCESS"` here but `"success"` (lower-case) in `RegisterActivity` (issue EP8).

### `isDataChanged(): Boolean`

A 13-branch comparison of every editable field against `myDetails` (the loaded `UserRslt`), used to
avoid a pointless `PUT`. Notable:

- It compares the **displayed** date against
  `Util.formatDate(myDetails.dateOfBirth, "dd-MM-yyyy", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")`, so the DOB
  round-trips through **two** format conversions (issue EP9).
- `text!!.contentEquals(...)` force-unwraps 10 times (issue EP10).
- Gender is compared against the lower-case literals `"male"` / `"female"`, and **`other` is not
  handled at all** (issue EP11).

---

## 5. Image pipeline

```
profile_pic_edit tap
  |-- galleryIntent()  ACTION_GET_CONTENT -> onActivityResult -> CropImage.activity(data.data).start()
  `-- cameraIntent()   ACTION_IMAGE_CAPTURE -> onActivityResult
          -> getImageUri(extras!!["data"] as Bitmap)   (writes to external storage)
          -> CropImage.activity(uri).start()
                `-- CROP_IMAGE_ACTIVITY_REQUEST_CODE -> CropImage.getActivityResult(data).uri
                       -> bitmap -> encodeTobase64() -> POST Users/image/{userId}
```

| Fact | Detail |
|---|---|
| Cropper | `com.theartofdev.edmodo:android-image-cropper:2.8.0` — **abandoned**, jcenter-era (BUILD_CONFIG B-1) |
| `getImageUri` | writes the camera bitmap to external storage to obtain a `file://` URI — the reason StrictMode is relaxed (EP2) |
| Camera source | again only the **thumbnail** from `extras["data"]`, as in ADD_POST (issue EP12) |
| Encoding | `encodeTobase64` -> JPEG **100** -> `Base64.DEFAULT`, sent inside JSON (issue EP13) |
| Debug logs | `Log.e(requestCode.toString(), ...)` and `Log.e(resultCode.toString(), ...)` — request codes used as **log tags** (issue EP14) |
| Equality | `requestCode === CropImage...` uses **referential** `===` on boxed `Int`s (issue EP15) |
| Commented-out | an "admin approval" confirmation dialog is disabled in the source (issue EP16) |

---

## 6. API calls (6)

| Method | Endpoint | Purpose |
|---|---|---|
| `getMyDetails(context)` | `GET users/{userId}` | populates all 14 fields; stores `myDetails`; triggers `getStates(loginresp.country)` |
| `edit(data)` | `PUT users/{userId}` | saves the form |
| `updateProfilePic(context, data)` | `POST Users/image/{userId}` | uploads the base64 avatar |
| `getCountries()` | `GET Country` | country list |
| `getStates(country)` | `GET state/{countryID}` | state list |
| `getCities(state)` | `GET city/{stateId}` | city list |

All authenticated calls use the **always-true `||` token guard** and a fresh `authToken` observer
(issues EP17, EP18). `getMyDetails` is the **6th** copy of the `GET users/{userId}` block (team
issue F-8).

---

## 7. Storage & global state

| Item | Operation |
|---|---|
| DataStore `token` | read (observed, once per call) |
| DataStore `token` + `userId` | deleted in dead logout branches |
| `Util.userId` | read -> all three write paths |
| `Util.isWarrior` | read -> `warrior` button visibility |
| `Util.user` | read by the overflow menu (`isReviewState`) |
| `myDetails: UserRslt` | the local snapshot used by `isDataChanged` |

---

## 8. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | **relax StrictMode** -> layout -> `UserPreferences` -> SpotsDialog -> bind buttons -> **`getCountries()` and `getMyDetails(this)` fired immediately** -> bind spinners/logo -> seed placeholder rows -> wire 3 spinner listeners, the avatar, save, cancel and the overflow menu |
| `onActivityResult` | gallery / camera / crop results (deprecated API) |

`getCountries()` and `getMyDetails()` race: the details callback calls `getStates(loginresp.country)`
while the country list may still be loading, so the state spinner can be populated before its
country list exists (issue EP19).

No rotation handling — the picked image, the crop result and the spinner selections are **lost**
(issue EP20).

---

## 9. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| `RetrofitAPI` (6 endpoints) | PLATFORM / NETWORK | all calls |
| `UserRslt`, `Countries`, `State`, `City` | PLATFORM / DATA_MODELS | parsing |
| `Util.isValidName/Email/Mobile`, `Util.formatDate`, `Util.isWarrior` | PLATFORM / COMMONS | validation + formatting |
| `Commons().makeWarrior` | PLATFORM / COMMONS | the warrior button |
| `UserPreferences` | PLATFORM / STORAGE | token |
| `android-image-cropper` + `CropImageActivity` | PLATFORM / BUILD_CONFIG | cropping |
| `CAMERA`, storage/media permissions | PLATFORM / BUILD_CONFIG | capture |
| `R.layout.spinner_text`, `main_menu.xml` | APPSHELL / THEMING | spinner rows + menu |
| `activity_register.xml` (**14 shared ids**) | AUTH / REGISTER | synthetic-import coupling |
| `MainActivity`, `SettingsActivity`, `FavoritesActivity` | APPSHELL / FEED | menu destinations |

---

## 10. Field -> body mapping (`PUT users/{userId}`)

The form maps to the same shape `UserRslt` returns: `firstName`, `lastName`, `name` (concatenated),
`email`, `mobile`, `dateOfBirth` (reformatted), `gender`, `address`, `pinCode`, `country`, `state`,
`city`, `language`. The avatar is **not** part of this body — it has its own endpoint.

---

## 11. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| EP2 | `onCreate` **disables StrictMode's VM policy** to allow a `file://` URI | **High** | use a `FileProvider` and remove the override |
| EP19 | `getCountries()` and `getMyDetails()` race; the state spinner can be filled before its country list loads | **High** | chain the calls |
| EP4 | State and city listeners compare against **`"Country"`**, so the `"State"`/`"City"` placeholders can be submitted as real values | **High** | compare against the right placeholder |
| EP17 | The always-true `||` token guard in every authenticated method; logout branches dead | **High** | use `&&` |
| EP13 | Base64 avatar inside JSON at quality 100, no size limit | **High** | multipart (with NETWORK) |
| EP12 | The camera path captures only a **thumbnail** | **High** | full-size via `FileProvider` |
| EP20 | Rotation loses the picked image and spinner state | **High** | `onSaveInstanceState` |
| EP21 | Retrofit calls are never cancelled | **High** | cancel in `onDestroy` |
| EP23 | The ~60-line overflow menu is duplicated here too | **High** | shared handler (PM-level) |
| EP7 | `lname`, `address`, `pincode`, `language`, geo and gender are **never validated** | Medium | extend `doValidation` |
| EP11 | Gender comparison ignores **`other`**, which exists in this layout | Medium | handle all three |
| EP5 | Country/state **names** are passed where ids are expected | Medium | confirm with the backend |
| EP6 | Changing the country does not reset state/city | Medium | clear dependents |
| EP9 | DOB round-trips through two format conversions for change detection | Medium | compare normalised values |
| EP1 | `findViewById` **and** synthetics mixed in one class | Medium | pick one |
| EP18 | A new `authToken` observer per call | Medium | one-shot read |
| EP16 | The "admin approval" confirmation dialog is commented out | Medium (product) | confirm intent with the customer |
| EP15 | `===` referential comparison on boxed `Int` request codes | Medium | use `==` |
| EP22 | `exported="true"` with no intent-filter | Medium (security) | BUILD_CONFIG |
| EP14 | Request/result codes used as **log tags** | Low | fix the tags |
| EP10 | 10 `text!!` force-unwraps in `isDataChanged` | Low | null-safe |
| EP8 | Returns `"SUCCESS"` while Register returns `"success"` | Low | share a constant |
| EP3 | Gender `other` is present here but commented out in Register | Low | align the two screens |

---

## 12. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/EditProfileActivity.kt` | **exclusive** |
| `app/src/main/res/layout/activity_edit_profile.xml` | **exclusive**, but **14 ids are shared by name with `activity_register.xml`** — notify AUTH/REGISTER before adding or renaming ids |
| `<activity android:name=".EditProfileActivity">` in the manifest | shared with PLATFORM/BUILD_CONFIG |

**Not owned (escalate):** `RetrofitAPI.kt`, `DataModels.kt`, `UserPreferences.kt`, `Util.java`,
`Commons.kt`, `activity_register.xml` (AUTH), `spinner_text.xml` / `main_menu.xml` (APPSHELL), the
cropper dependency (BUILD_CONFIG).

---

## 13. How to make common changes

**Add a form field:** add the `TextInputLayout` + input to the layout — **first check the id does not
collide with `activity_register.xml`** (14 already do). Bind it, populate it in `getMyDetails`, add a
branch to `isDataChanged`, add validation to `doValidation`, and add the key to the `PUT` body.
Five places; confirm the body key with PLATFORM/NETWORK.

**Fix the spinner placeholder bug (EP4):** change the state listener to compare against `"State"` and
the city listener against `"City"`. Small, local and high-value.

**Reset dependent spinners (EP6):** clear `statestr`/`citystr` and their lists when the country
changes.

**Remove the StrictMode override (EP2):** requires replacing `getImageUri`'s external-storage write
with a `FileProvider`, which needs a manifest `<provider>` — **BUILD_CONFIG coordination via PM**.

**Switch the avatar to multipart (EP13):** a new `@Multipart` endpoint — **PLATFORM/NETWORK via PM**.

**Validate the remaining fields (EP7):** extend `doValidation` in the existing style. Note
`Util.isValid*` lives in PLATFORM/COMMONS — adding a **new** validator is a PLATFORM task.

---

## 14. Change log

| Change | Detail |
|---|---|
| Created | Initial EDIT_PROFILE module agent documented from `EditProfileActivity.kt` (**738 lines — the largest file in the app**) and `activity_edit_profile.xml` (31 ids): all views grouped by purpose, the three cascading geo spinners with their placeholder-comparison bug, the 7-rule `doValidation` and 13-branch `isDataChanged`, the full crop/camera pipeline including the **StrictMode relaxation**, 6 API calls, and 23 known issues. Recorded the **14-id collision with `activity_register.xml`** that couples this layout to AUTH/REGISTER. |



