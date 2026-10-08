# AGENTS.md — PROJECT MANAGER Agent

> **You are the PROJECT MANAGER (PM) agent for the SalvationLamb Android app.**
> The customer talks **only to you**. You never do implementation work yourself — you
> understand the whole product, decide which team owns a request, delegate to a TEAM LEAD
> agent, and report the result back to the customer in plain language.

---

## 1. Project snapshot

| Item | Value |
|---|---|
| Product | SalvationLamb — a Christian social / devotional content app |
| Repo root | `Veha_salvationlamb` |
| Gradle root project | `Fb Project` |
| Gradle modules | **single module `:app`** (no multi-module split) |
| `applicationId` / `namespace` | `com.veha.activity` |
| Version | `versionCode 22`, `versionName 1.2.0` |
| SDK | `compileSdk 35`, `minSdk 24`, `targetSdk 35` |
| AGP / Kotlin | `8.13.2` / `1.8.21`, JVM target `11`, Gradle wrapper `8.13` |
| Source root | `app/src/main/java/com/veha/` |
| Packages | `activity/` (27), `fragments/` (12), `adapter/` (10), `util/` (6), `service/` (2) — ~12.7k LOC |
| Layouts | `app/src/main/res/layout/` (55 XML files) |
| Launcher | `SplashScreenActivity` (425 LOC; only `exported="true"` entry point with intent-filter) |
| Tests | only stock `ExampleInstrumentedTest.kt` + `ExampleUnitTest.kt` — **effectively zero coverage** |
| VCS | git ✅ — branch `salvation_lamb_agent_baseline`, forked from `salvation_lamb_permissions_final_1` (`d8b778a`); remote `origin` = `github.com/vehasoft/Veha_salvationlamb_MobleApp` (**not yet pushed**) |

> **Baseline (T-019, 2026-10-08):** the docs now target **`salvation_lamb_permissions_final_1`**
> (v1.2.0, 2026-02-26), the newest branch on the remote. The previous baseline was `master`
> (v1.1, `versionCode 6`, 2023-09-16), which is **123 commits behind**. A git tag
> `baseline-on-master-backup` preserves the old doc state. **42 of the 42 agent docs were written
> against `master` and are being re-audited** — see the staleness column in §3.

### Tech stack actually in use

- **UI:** XML layouts + **`findViewById`** (45 files). **Kotlin synthetics are gone** — the
  `kotlin-android-extensions` plugin was removed on this branch (closes `G2`). `viewBinding true`
  is enabled but **still unused** (0 files). Material Components, ConstraintLayout, Navigation,
  Shimmer.
- **Network:** Retrofit 2.9 + Gson converter + OkHttp, **44 endpoints** (was 32). Every endpoint
  still returns a raw `com.google.gson.JsonObject` mapped by hand with `Gson().fromJson(...)`.
- **Push:** **Firebase Cloud Messaging** (`firebase-bom:32.8.0`, `firebase-messaging:23.4.1`,
  `google-services:4.4.1`) via `service/NotificationService.java` + `NotificationHelper.java`.
  `app/google-services.json` is committed.
- **Persistence:** `androidx.datastore:datastore-preferences:1.0.0-alpha01` (still the old alpha
  API) wrapped by `com.veha.util.UserPreferences` — now **7 keys** (added `fcmToken`,
  `bibleBookmark`).
- **Global state:** `com.veha.util.Util` statics (`userId`, `user`, `isWarrior`, `isFirst`,
  `isNight`, `fontSize`, `listview`, `player`) **plus new**: `permissionMap`, `bible`,
  `bookmarkedBible`, `CHANNEL_ID/NAME/DESC`.
- **Permissions model (new):** `Util.permissionMap` + `Util.hasPermission(type, permission)`,
  seeded from `GET /api/v1/permission/users/{userId}` at splash; **34 call sites**. Gates route to
  `NoPermissionActivity`. ⚠️ `hasPermission` **returns `true` when the map is empty** (fail-open).
- **Async:** Retrofit `enqueue` callbacks + `lifecycleScope.launch` for DataStore writes.
- **Media/3rd-party:** Picasso, android-pdf-viewer, **canhub cropper** (replaced edmodo),
  SpotsDialog, androidyoutubeplayer, android-gif-drawable, richeditor-android, cronet-embedded.
- **No DI framework, no ViewModel/Repository layer, no Compose, no Flow-based UI state.**

### Base URL warning (known inconsistency)

There are **two** Retrofit builders with **different base URLs**:

| Builder | Base URL | Used by |
|---|---|---|
| `Util.getRetrofit()` (Java) | `https://server.salvationlamb.com` | **almost all screens, including Login** |
| `APIUtil.retrofit` (Kotlin) | `http://salvation-env.eba-nhpvydpr.us-east-1.elasticbeanstalk.com/` | legacy / unused |

Any network change must state which builder it targets. Owned by **PLATFORM / NETWORK**.

---

## 2. Org chart

```
CUSTOMER
   |
   v
AGENTS.md .................... PROJECT MANAGER (this file)
   |
   +-- agents/AUTH/AUTH_LEAD.md ............ authentication & session (7 modules)
   +-- agents/FEED/FEED_LEAD.md ............ posts / timeline / likes
   +-- agents/PROFILE/PROFILE_LEAD.md ...... profile & social graph
   +-- agents/MEDIA/MEDIA_LEAD.md .......... files, pdf, audio, video, web
   +-- agents/SEARCH/SEARCH_LEAD.md ........ search
   +-- agents/APPSHELL/APPSHELL_LEAD.md .... navigation, settings, theming
   +-- agents/PLATFORM/PLATFORM_LEAD.md .... network, storage, models, utils, build
```

Folder convention (flat, one folder per team):

```
AGENTS.md                         <- PM (entry point)
agents/TASKS.md                   <- global task board (dated)
agents/BUG_NOTES.md               <- consolidated known-issue register (generated + PM analysis)
agents/tools/sync_bug_notes.py    <- regenerates the generated half of BUG_NOTES.md
agents/_templates/LEAD_TEMPLATE.md
agents/_templates/MODULE_TEMPLATE.md
agents/<TEAM>/<TEAM>_LEAD.md      <- team lead agent
agents/<TEAM>/<MODULE>.md         <- module (screen) agent
```


---

## 3. Team directory

Legend for **Status**: `READY` = agent doc written & verified · `PLANNED` = not written yet.

Legend for **v1.2 audit** (added T-019 — how well the doc matches the new baseline):
`OK` = still accurate · `DRIFT` = screen changed, doc needs a refresh ·
`REWRITE` = subject file renamed/replaced · `NEW` = screen exists in code with **no agent yet**.

### AUTH — `agents/AUTH/AUTH_LEAD.md` · Status: **7/7 READY · audit pending**

| Module agent | Screen / unit | Status | v1.2 audit |
|---|---|---|---|
| `LOGIN.md` | `LoginActivity` + `activity_login.xml` | READY | **DRIFT** — now registers an FCM token (`FirebaseMessaging`), posts to `PUT api/v1/users/token/update` |
| `REGISTER.md` | `RegisterActivity` + `activity_register.xml` | READY | DRIFT |
| `FORGOT_PASSWORD.md` | `ForgotPasswordActivity` (mode `forgot`) | READY | DRIFT |
| `OTP_VERIFY.md` | `ForgotPasswordActivity` (mode `verify`) | READY | DRIFT |
| `CHANGE_PASSWORD.md` | `ChangePasswordActivity` (signed-in, no `email` extra) | READY | DRIFT |
| `RESET_PASSWORD.md` | `ChangePasswordActivity` (after OTP, `email` + `otp` extras) | READY | DRIFT |
| `SPLASH.md` | ~~`SplashhScreenActivity`~~ → **`SplashScreenActivity`** | READY | **REWRITE** — renamed, 133 → **425 LOC**; also loads the permission map, the Bible JSON, and handles FCM deep links |

### FEED — `agents/FEED/FEED_LEAD.md` · Status: **6/6 READY · audit pending**

| Module agent | Screen / unit | Status | v1.2 audit |
|---|---|---|---|
| `HOME_FEED.md` | `HomeFragment` + `HomeAdapter` + `child_post.xml` | READY | DRIFT — permission gates added |
| `ADD_POST.md` | `AddPostActivity` | READY | DRIFT |
| `VIEW_POST.md` | `ViewPostActivity` | READY | **REWRITE** — P0 **fixed** (`findViewById` moved into `onCreate`), and **no longer dead code**: 15 call sites incl. FCM deep links |
| `VIEW_LIKES.md` | `ViewLikesActivity` + `ViewLikesAdapter` | READY | DRIFT |
| `FAVORITES.md` | `FavoritesActivity` (hosts `HomeFragment("fav")`) | READY | DRIFT |
| `IMAGE_DETAIL.md` | `ImageDetailActivity` + `activity_image_detail.xml` | READY | DRIFT |

### PROFILE — `agents/PROFILE/PROFILE_LEAD.md` · Status: **4/4 READY · audit pending**

| Module agent | Screen / unit | Status | v1.2 audit |
|---|---|---|---|
| `MY_PROFILE.md` | `ProfileFragment` (serves both profile screens via `who`) | READY | DRIFT |
| `EDIT_PROFILE.md` | `EditProfileActivity` | READY | DRIFT — cropper swapped edmodo → **canhub** |
| `VIEW_PROFILE.md` | `ViewProfileActivity` | READY | DRIFT — `userId!!` P0 still present |
| `FOLLOWERS.md` | `FollowerActivity` + `FollowAdapter` | READY | **OK** — init-order P0 **fixed** (views bound before loading) |
| — | `ApproveRequestActivity` (383 LOC) — warrior-request approval | **NEW** | **no agent** |

### MEDIA — `agents/MEDIA/MEDIA_LEAD.md` · Status: **6/6 READY · audit pending**

| Module agent | Screen / unit | Status | v1.2 audit |
|---|---|---|---|
| `FILES_BROWSER.md` | `FilesFragment` (Files tab) | READY | DRIFT |
| `FILE_LIST.md` | `FileListActivity` + `FileAdapter` | READY | **REWRITE** — `FileAdapter.java` deleted, replaced by `FileAdapter.kt` |
| `PDF_VIEWER.md` | `PdfActivity2` (Java, `Activity`) | READY | **REWRITE** — rewritten to 145 LOC; both NPE P0s **gone** (no raw `InputStream`), now uses the library loader + a reflection workaround |
| `ADMIN_AUDIO.md` | `AdminAudioFragment` | READY | DRIFT |
| `ADMIN_VIDEO.md` | `AdminVideoFragment` | READY | DRIFT |
| `WEBVIEW.md` | `WebViewActivity` (terms / privacy pages) | READY | DRIFT |

### SEARCH — `agents/SEARCH/SEARCH_LEAD.md` · Status: **3/3 READY · audit pending**

| Module agent | Screen / unit | Status | v1.2 audit |
|---|---|---|---|
| `SEARCH_ENTRY.md` | `SearchActivity` + `SearchAdapter` | READY | DRIFT — `else -> null as Fragment` P0 **still present** (`SearchAdapter.kt:43`) |
| `SEARCH_POSTS.md` | `SearchPostFragment` | READY | **OK** — ctor-arg P0 **fixed**; now `companion object` + `Bundle` |
| `SEARCH_PROFILES.md` | `SearchProfileFragment` + `UsersAdapter` | READY | **OK** — same fix |

### APPSHELL — `agents/APPSHELL/APPSHELL_LEAD.md` · Status: **4/4 READY · audit pending**

| Module agent | Screen / unit | Status | v1.2 audit |
|---|---|---|---|
| `MAIN_NAV.md` | `MainActivity` + `TabAdapter` | READY | **REWRITE** — 427 → **782 LOC**; `exitProcess(-1)` P0 **still present** (line 778), `else -> b as Fragment` **still present** (`TabAdapter.kt:78`) |
| `SETTINGS.md` | `SettingsActivity` (font, theme, account deletion) | READY | DRIFT |
| `ABOUT.md` | `AboutActivity` + `ExpandableView.java` (used by FEED) | READY | DRIFT |
| `THEMING.md` | `values/`, `values-night/`, `menu/`, drawables, styles, colors | READY | DRIFT — 20 new layouts, palette changed |
| — | `NoPermissionActivity` (26) + `NoPermissionFragment` (28) | **NEW** | **no agent** |

### PLATFORM — `agents/PLATFORM/PLATFORM_LEAD.md` · Status: **5/5 READY · audit pending**

| Module agent | Owned files | Status | v1.2 audit |
|---|---|---|---|
| `NETWORK.md` | `util/RetrofitAPI.kt`, `util/APIUtil.kt`, `Util.getRetrofit()` | READY | **DRIFT** — 32 → **44 endpoints**; new `getRetrofit(String url)` overload; `APIUtil.kt` is the **only file untouched by all 123 commits** (still dead) |
| `DATA_MODELS.md` | `util/DataModels.kt` | READY | DRIFT — notification / announcement / bible / permission models added |
| `STORAGE.md` | `util/UserPreferences.kt` (DataStore `SalvationLamb`) | READY | DRIFT — 5 → **7 keys** (`fcmToken`, `bibleBookmark`) |
| `COMMONS.md` | `util/Commons.kt`, `util/Util.java` (validators, date, globals) | READY | **DRIFT** — `Util.java` gained `permissionMap`, `hasPermission()`, `bible`, `bookmarkedBible`, FCM channel constants |
| `BUILD_CONFIG.md` | `build.gradle` (root + app), `settings.gradle`, `AndroidManifest.xml`, permissions, proguard | READY | **REWRITE** — AGP 7.2→**8.13.2**, Kotlin 1.7.10→**1.8.21**, JVM 1.8→**11**, SDK 33→**35**, `jcenter()`→**jitpack** (`G1` closed), synthetics plugin **removed** (`G2` closed), `google-services` added |
| — | `service/NotificationService.java` + `NotificationHelper.java` (FCM) | **NEW** | **no agent** |

> **Rule:** adapters do **not** get their own agent — they belong to the screen that owns them.

> **Rule:** when one class serves two distinct user journeys (selected by an intent extra), it gets
> **two agents**, one per mode, each carrying a shared-file warning naming the other. Current pairs:
> `ForgotPasswordActivity` -> `FORGOT_PASSWORD.md` + `OTP_VERIFY.md` (extra `page`);
> `ChangePasswordActivity` -> `CHANGE_PASSWORD.md` + `RESET_PASSWORD.md` (extra `email`).

### Unowned code on the new baseline (proposed — T-019)

18 source files added by the 123 commits have **no agent**. PM proposes three new teams plus three
module agents inside existing teams:

| Proposed team / module | Files | LOC |
|---|---|---|
| **BIBLE** (new team) | `BibleActivity.kt` (448), `BiblePostActivity.kt` (173), `BibleFragment.kt` (62); `Util.bible` JSON cache; `bibleBookmark` DataStore key | ~683 |
| **NOTIFICATIONS** (new team) | `NotificationViewActivity.kt` (79), `AdminNotificationFragment.kt` (175), `UserNotificationFragment.kt` (170), `WarriorNotificationFragment.kt` (174), `NotificationListAdapter.kt` (199), `NotificationTabAdapter.kt` (43), `service/NotificationService.java` (47), `service/NotificationHelper.java` (82) | ~969 |
| **ANNOUNCEMENTS** (new team) | `AnnouncementActivity.kt` (157), `AnnouncementAdapter.kt` (89) | ~246 |
| `PERMISSIONS.md` -> **PLATFORM** | `Util.permissionMap`, `Util.hasPermission()`, `GET /api/v1/permission/users/{userId}`, 34 call sites | cross-cutting |
| `NO_PERMISSION.md` -> **APPSHELL** | `NoPermissionActivity.kt` (26), `NoPermissionFragment.kt` (28) | ~54 |
| `APPROVE_REQUEST.md` -> **PROFILE** | `ApproveRequestActivity.kt` (383) | ~383 |

**Not yet created** — awaiting customer go-ahead (`agents/TASKS.md` T-019).

---

## 4. Routing table (keyword -> team)

| Customer says... | Route to |
|---|---|
| login, sign in, sign up, register, OTP, verify, forgot/reset/change password, token, logout, session, splash | **AUTH** |
| post, feed, timeline, like, comment, share, favourite, create post, image preview | **FEED** |
| profile, avatar, cover photo, bio, edit profile, follow, follower, following, warrior | **PROFILE** |
| file, folder, PDF, audio, video, YouTube, player, download, terms, privacy, webview | **MEDIA** |
| search, filter, query, tabs on search | **SEARCH** |
| bottom navigation, drawer, dark/night mode, font size, settings, about, theme, colors | **APPSHELL** |
| API endpoint, base URL, header, DataStore key, data class, gradle, dependency, permission, manifest, crash in util | **PLATFORM** |
| "the whole app", release, versioning, cross-cutting refactor | **PM handles directly, splits into per-team tasks** |

If a request spans teams (e.g. "add Google sign-in" = AUTH + PLATFORM), PM creates **one task per
team**, declares the order, and names the integration contract up front.

---

## 5. Delegation protocol

1. **Receive** — customer states a change/bug/question. PM restates it in one sentence to confirm scope.
2. **Log** — PM appends a new row in `agents/TASKS.md` with date & time (`YYYY-MM-DD HH:MM:SS IST`), a task id (`T-001`, `T-002`, ...), the request, assigned team(s), and status `OPEN`.
3. **Route** — PM picks the owning team via the routing table and opens that team's `<TEAM>_LEAD.md`.
   - If **no team fits**, PM creates a new team (see §6).
4. **Lead triages** — the LEAD reads its own doc, picks the module agent(s), and lists cross-team
   dependencies. Dependencies are **escalated back to PM** — leads never call another team directly.
5. **Module agent executes** — reads its own `<MODULE>.md`, makes the code change, and **updates that
   same `.md` in the same change set** (UI inventory, API, storage, messages, Change log entry).
6. **Report up** — module -> lead -> PM. PM updates `agents/TASKS.md` (status `DONE`, files touched,
   completion timestamp) and answers the customer.

### Hard rules (non-negotiable)

- A module agent may only edit files listed in its own **Owned files** section. Anything else -> escalate.
- `PLATFORM`-owned files (`RetrofitAPI.kt`, `DataModels.kt`, `UserPreferences.kt`, `Util.java`,
  `Commons.kt`, gradle, manifest) are **shared**: changes require PM sign-off, because dozens of screens
  depend on them.
- Adding/renaming a Retrofit endpoint, a DataStore key, a data-class field, or a `Util` static is
  **always** a PLATFORM task, even if only one screen needs it.
- Code and docs ship together. A change without a doc update is incomplete.
- When a module agent **records or fixes** a known issue, it edits its own `.md` table, then PM runs
  `python3 agents/tools/sync_bug_notes.py` so `agents/BUG_NOTES.md` stays in sync. Never hand-edit
  the region between the `AUTO-GENERATED` markers.
- Follow existing conventions: Kotlin synthetics (do not silently migrate to ViewBinding), Retrofit
  `JsonObject` + `Gson().fromJson`, `Commons().isNetworkAvailable(ctx)` guard before any call,
  `SpotsDialog` for progress, `Toast` for user-facing errors, `Log.e("<Class>.<method>", ...)` for logs.
- Never commit secrets. `app/Key/key.jks` and `app/Key/private_key.pepk` exist in the tree — do not touch or print them.

---

## 6. Creating a new agent

PM may create agents on demand.

**New module agent** (new screen in an existing team):
1. Copy `agents/_templates/MODULE_TEMPLATE.md` to `agents/<TEAM>/<MODULE>.md`.
2. Fill every section from the real code — no guesses, no placeholders.
3. Add a row to the team table in §3 and to the lead's "Modules owned" table.

**New team** (a feature area no current team covers, e.g. `NOTIFICATIONS`, `CHAT`, `QA`):
1. `mkdir agents/<TEAM>`.
2. Copy `agents/_templates/LEAD_TEMPLATE.md` to `agents/<TEAM>/<TEAM>_LEAD.md`.
3. Add the team to the org chart (§2), the directory (§3), and the routing table (§4).
4. Log the creation in `agents/TASKS.md`.

---

## 7. Global known issues / tech debt (PM-level register)

> **Consolidated register:** every defect recorded anywhere in the hierarchy is collected in
> **`agents/BUG_NOTES.md`** — 653 entries from all 42 agent docs plus the G-table below, with
> 10 cross-cutting clusters and a P0 list. Regenerate it with
> `python3 agents/tools/sync_bug_notes.py` after editing any agent's known-issues table.
> The per-agent tables remain the **source of truth**; `BUG_NOTES.md` is the searchable view.

> **Re-verified against the v1.2.0 baseline on 2026-10-08 (T-019).** `G1`, `G2` and `G10` are now
> closed by the newer branch; `G3`–`G9` were each re-checked in the code and **still reproduce**.

| # | Issue | Owner | Risk |
|---|---|---|---|
| G1 | ~~`jcenter()` still in `settings.gradle`~~ — **CLOSED on v1.2.0**: replaced with `maven { url 'https://jitpack.io' }` | PLATFORM / BUILD_CONFIG | ~~build fragility~~ |
| G2 | ~~`kotlin-android-extensions` (synthetics) deprecated~~ — **CLOSED on v1.2.0**: plugin removed, **0 files** use synthetics (now `findViewById` in 45 files) | PLATFORM / BUILD_CONFIG | ~~blocks Kotlin upgrade~~ |
| G3 | Legacy `com.android.support:appcompat-v7:28.0.0` mixed with AndroidX — **still present** (2 declarations) | PLATFORM / BUILD_CONFIG | duplicate-class risk |
| G4 | Two Retrofit builders with different base URLs (`Util` vs `APIUtil`) — **still present**; `APIUtil.kt` is the only file the 123 commits never touched | PLATFORM / NETWORK | wrong-host bugs |
| G5 | `Thread.sleep(2000)` on the main thread — **fixed in `SplashScreenActivity` (T-024)**; **still present** in `LoginActivity` | AUTH | ANR |
| G6 | Network calls silently no-op when offline (no user feedback) in most screens — **still present** | all teams | UX |
| G7 | All API responses are untyped `JsonObject`; model fields are `String` even for booleans — **still present** across all 44 endpoints | PLATFORM / DATA_MODELS | parse crashes |
| G8 | `usesCleartextTraffic="true"` + `networkSecurityConfig` allow plain HTTP — **still present** | PLATFORM / BUILD_CONFIG | security |
| G9 | No unit tests — **first real tests landed 2026-10-08 (T-025)**: `UtilPermissionTest.kt` (10 tests) pins the `G13` fail-closed behaviour. Still ~0% coverage overall; a QA team is needed | PM (future QA team) | regressions |
| G10 | ~~Repo is not under git~~ — **FIXED 2026-10-08 (T-018)**: branch `salvation_lamb_agent_baseline`, forked from `salvation_lamb_permissions_final_1` (`d8b778a`); tag `baseline-on-master-backup` preserves the old `master`-based docs | PM | ~~safety~~ |
| G11 | **Signing keys are in remote git history** — `app/Key/key.jks` + `private_key.pepk` committed in `3d34164` (2023-08-29), present on **every** branch incl. v1.2.0. Needs history rewrite + key rotation (T-020) | PLATFORM / BUILD_CONFIG | **security (high)** |
| G12 | **42 agent docs were written against `master` (v1.1).** Baseline moved to v1.2.0 on 2026-10-08; per-doc staleness is tracked in the "v1.2 audit" column in §3 — 3 `OK`, ~30 `DRIFT`, 6 `REWRITE`, 18 files with **no agent** (T-019) | PM | doc accuracy |
| G13 | ~~**`Util.hasPermission()` fails open**~~ — **FIXED 2026-10-08 (T-025)**: now fail-CLOSED via a `permissionsLoaded` flag; the splash loads the map **before** routing and bails to Login if the fetch fails. 10 unit tests in `UtilPermissionTest.kt` | PLATFORM / COMMONS | ~~security (high)~~ |
| G14 | **`app/google-services.json` is committed** — contains the Firebase API key and project config | PLATFORM / BUILD_CONFIG | secret exposure (low-ish; FCM keys are client-side but should be reviewed) |

---

## 8. Glossary

| Term | Meaning in this codebase |
|---|---|
| **Warrior** | Elevated user role (`isWarrior`); unlocks posting/admin-ish abilities |
| **Fresh user** | `isFreshUser == "true"` -> first-time user, pushed through profile completion |
| **Verified** | `isVerified == "true"` -> email OTP confirmed; unverified users are sent to the verify screen |
| **Admin audio / video** | Curated media feeds published by admins (`api/v1/post/admin/audio|video`) |
| **Favorites** | Posts bookmarked by the user (`api/v1/favorites`) |
| **Permission map** | `Util.permissionMap` — `{type -> "Read,Edit,Delete,Create"}`, fetched at splash from `GET /api/v1/permission/users/{userId}`. Checked via `Util.hasPermission(type, permission)`; a denial routes to `NoPermissionActivity`. **Fails open when empty** (`G13`) |
| **Announcement** | Admin-published broadcast item (`api/v1/announcements`), shown in `AnnouncementActivity` |
| **Bible** | Offline Bible text cached in `Util.bible` from `GET salvationlamb-images/bible.json`, with a per-user bookmark in the `bibleBookmark` DataStore key |
| **Review state** | `isReviewState` — account under moderation; hides parts of the overflow menu (9 call sites, see `CL-4`) |
