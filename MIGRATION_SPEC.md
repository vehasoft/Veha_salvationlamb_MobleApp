# SalvationLamb — React Native Migration Spec

> **Audience:** the developer(s) rebuilding this product in React Native.
> **Source of truth:** the Kotlin app at `salvation_lamb_permissions_final_1` (v1.2.0,
> `versionCode 22`) — 37 screens, 12.7k LOC, 44 endpoints.
> **Status:** the Android app is **deprecated**. It stays in production until the RN app ships.
> **Created:** 2026-10-08 (T-027) · PM-owned · read this **before** `agents/`.

---

## 0. How to use this document

This is the **what and why**. The 54 agent docs under `agents/` are the **exact detail** — every
view id, every string, every defect, per screen. Read this first, then open the specific agent
doc for whatever you are porting that day.

| You want… | Go to |
|---|---|
| The big picture, traps, build order | **this file** |
| Every detail of one screen | `agents/<TEAM>/<MODULE>.md` |
| The full defect register (851 entries) | `agents/BUG_NOTES.md` |
| Who owns what | `AGENTS.md` §3 |

> ⚠️ **Do not port the Kotlin architecture.** It has no ViewModels, no repository layer, no DI,
> and mutable global state in `Util.java` that every screen reads and writes. Port the
> **behaviour and the API contract**, not the structure.

---

## 1. Non-negotiable constraints

Fixed by the live app. Get these wrong and existing users break.

| Constraint | Value | Why it cannot change |
|---|---|---|
| `applicationId` | **`com.veha.activity`** | Must match, or Play treats the RN app as a **new app** and existing users never get the update |
| Signing key | **`app/Key/key.jks`** (in this repo) | Same reason. Rotation was considered and **declined** (T-020, `G11`). This is the permanent signing identity — **back it up outside the repo** |
| `versionCode` | must start **> 22** | v1.2.0 is live |
| `minSdk` | 24 — safe to raise, never lower | current install base |
| Firebase project | **`salvationlamb-a9717`**, client registered to `com.veha.activity` | reuse for `@react-native-firebase`; existing push tokens keep working |
| API base URL | **`https://server.salvationlamb.com`** | same backend, unchanged |
| Second host | `https://files.salvationlamb.com/` | Bible JSON only |

---

## 2. The five traps

Each has already caused a bug in the Kotlin app. A naive port reproduces all five.

### 2.1 🔴 Every API field is a **string**, including booleans

```kotlin
val isWarrior: String      // "true" / "false"
val blocked: String
val isVerified: String
val likesCount: String     // "42"
```

In Kotlin `"false".toBoolean()` is `false`. **In JavaScript `Boolean("false")` is `true`.**
A direct port makes **every user a warrior, every account blocked, every post liked**.

```ts
// REQUIRED at the API boundary — not scattered through components
const asBool = (v?: string) => v === 'true';
const asInt  = (v?: string) => Number.parseInt(v ?? '0', 10) || 0;
```

### 2.2 🔴 Five different response envelopes

| Shape | Key | Used by |
|---|---|---|
| single object | `result` | auth, `GET users/{id}` — 16 sites |
| array + `count` | `results` | feed, profile, files, announcements — 27 sites |
| array | `files` | `GET files/{folderId}` |
| **object** of `users` + `posts` | `results` | `GET search` |
| **object** of `user` + `updateRequest` | `results` | `GET review/{userId}` |

Guess wrong and you get **an empty list with no error** — the most common failure mode in the
Kotlin app. Write one `unwrap` per shape and pin which endpoint uses which.

### 2.3 🔴 `isWarrior` is parsed two contradictory ways

| Where | Expression | `""`/`null` → | `"TRUE"` → |
|---|---|---|---|
| Login, Splash, HomeFragment | `isNullOrEmpty() \|\| != "false"` | **warrior** | **warrior** |
| MainActivity, ProfileFragment | `.toBoolean()` | normal | **normal** |

A user's role depends on which screen refreshed it last. **Pick one rule** (recommended
`=== 'true'`, fail-closed) and confirm with the backend what it actually sends.

### 2.4 🟠 The permission model fails closed — keep it that way

`Util.hasPermission(type, permission)` gates **34 call sites**. The map is
`{ "POST": "Read,Edit,Delete,Create", … }`, fetched once at splash from
`GET /api/v1/permission/users/{userId}` (envelope `results`).

It used to return `true` when the map was empty, so a failed fetch granted everything. **Fixed
2026-10-08** (`G13`, T-025): it now denies, and the splash blocks routing until the map loads.
`app/src/test/java/com/veha/util/UtilPermissionTest.kt` holds **10 tests that are a portable
spec** — translate them to Jest *first*, then write the implementation.

```
PermissionType: POST | USER | PROFILE | FILES | AUDIO | VIDEO | WARRIOR | ANNOUNCEMENT
Permission:     All | Read | Edit | Delete | Create      ("All" wins)
Denial       →  the "no permission" screen
```

### 2.5 🟠 The auth-token guard is inverted in 29 places

```kotlin
if (!isEmpty(t) || !t.equals("null") || !t.isNullOrEmpty())   // ALWAYS TRUE
```

Three OR-ed negations can never all be false, so every "session lost" branch in those 29 files is
**dead code**, and a missing token goes out as the literal header `Bearer null`. The other 29 call
sites use `&&` correctly. **Don't copy auth logic from a random screen** — write one authenticated
client and use it everywhere.

---

## 3. API surface — 44 endpoints

Base `https://server.salvationlamb.com`. Auth is an `Authorization: Bearer <token>` header set
**per call** — there is no interceptor today. **Add one.**

### Auth & account

| Method | Path | Envelope | Notes |
|---|---|---|---|
| POST | `api/v1/login` ¹ | `result` | returns user + `token` |
| POST | `api/v1/users` ¹ | `result` | register |
| POST | `api/v1/users/verify-otp` | `result` | email OTP |
| POST | `api/v1/password/forgot-password` | `result` | |
| POST | `api/v1/password/verify-otp` | `result` | |
| POST | `api/v1/password/confirm-password` | `result` | |
| POST | `api/v1/users/change-password` | `result` | signed-in |
| GET | `api/v1/users/{userId}` | `result` | **the session bootstrap** |
| PUT | `api/v1/users/{userId}` | `result` | edit profile |
| PUT | `api/v1/users/freshUser/{userId}` | `result` | clears the first-run flag |
| PUT | `api/v1/users/token/update` ³ | `result` | **FCM token** — see §4.4 |
| DELETE | `api/v1/users/{userId}` | `result` | account deletion |
| POST | `api/v1/Users/image/{userId}` | `result` | avatar (**capital `U`**) |
| POST | `api/v1/users/warrior` | `result` | request the warrior role |

¹ Called through a generic `postCall("{url}", body)` helper; the real paths are `login` and `users`.

³ Declared in Retrofit as **`/api/v1/users/token/update`** — note the **leading slash**, which
makes it absolute and drops any base-path prefix. Same for `/api/v1/permission/users/{userId}`.
Harmless today because the base URL has no path, but do not copy the inconsistency.

### Content

| Method | Path | Envelope | Notes |
|---|---|---|---|
| GET | `api/v1/post?page&size` | `results`+`count` | main feed |
| GET | `api/v1/post/{postId}` | `result` | detail |
| GET | `api/v1/post/user/{userId}?page&size` | `results`+`count` | a user's posts |
| GET | `api/v1/post/admin/video?page&size` | `results`+`count` | curated video |
| GET | `api/v1/post/admin/audio?page&size` | `results`+`count` | curated audio |
| POST | `api/v1/post` ² | `result` | create |
| DELETE | `api/v1/post/{postId}` | `result` | |
| POST | `api/v1/like` ² | `result` | react |
| GET | `api/v1/like/post/{postId}` | `results` | who reacted |
| GET | `api/v1/like/user/{userId}` | `results` | my reactions |
| POST | `api/v1/favorites` | `result` | |
| GET | `api/v1/favorites/{userId}?page&size` | `results`+`count` | |

² also reached via `postCallHead(token, "post"\|"like", body)`.

### Social & moderation

| Method | Path | Envelope |
|---|---|---|
| POST | `api/v1/follows` | `result` |
| GET | `api/v1/follows/user/{userId}` | `results` — **followers** |
| GET | `api/v1/follows/{userId}` | `results` — **following** |
| GET | `api/v1/search?query=` | `results` → `{users, posts}` |
| GET | `api/v1/review/{userId}` | `results` → `{user, updateRequest}` |
| POST | `api/v1/review/{status}/{userId}` | `result` — `status` = `approve`\|`reject`, **no body** |

> ⚠️ `follows/user/{id}` and `follows/{id}` differ by one path segment and mean opposite things.

### Notifications & announcements

| Method | Path | Envelope |
|---|---|---|
| GET | `api/v1/notifications/{userId}?page&size&type` | **`notification`** (singular) + `count` |
| GET | `api/v1/notifications/count/{userId}` | `result` |
| PUT | `api/v1/notifications/{id}` | `result` — body `{"isVisited": true}` |
| GET | `api/v1/announcements?page&size` | `results`+`count` |
| GET | `api/v1/announcements/{postId}` | `result` — **declared, never called** |

`type` is `user` \| `admin` \| `warrior` — the three notification tabs.

### Files, reference data, misc

| Method | Path | Envelope | Notes |
|---|---|---|---|
| GET | `api/v1/files/{folderId}` | **`files`** | folder tree |
| POST | `api/v1/files/access/{fileId}` | `result` | password-protected file |
| GET | `/api/v1/permission/users/{userId}` | `results` | **the permission map** |
| GET | `api/v1/Country` | `results` | **no auth header** |
| GET | `api/v1/state/{countryID}` | `results` | **no auth header** |
| GET | `api/v1/city/{stateId}` | `results` | **no auth header** |
| GET | `api/v1/version` | `result` | force-update check |
| GET | `salvationlamb-images/bible.json` | raw JSON | **different host** |

---

## 4. Core domain models & flows

### 4.1 The user object (`UserRslt`, 28 fields — all strings)

`id · name · firstName · lastName · gender · email · mobile · picture · coverPicture · address ·
dateOfBirth · loginSource · role · createdAt · updatedAt · isWarrior · isReviewState · state ·
pinCode · country · churchName · religion · city · language · userGroup · isVerified ·
isFreshUser · blocked`

Four of these drive routing and must be modelled as real booleans in RN:

| Field | Meaning | Effect |
|---|---|---|
| `isVerified` | email OTP done | `false` → OTP screen, not the app |
| `isFreshUser` | first-time user | `true` → first-run flow |
| `blocked` | account blocked | `true` → toast + back to Login |
| `isReviewState` | profile change under moderation | hides parts of the overflow menu |

`role == "admin"` also forces warrior privileges.

### 4.2 The post object (`Posts`)

`id · title · content · message · tags · userId · picture · type · url · colorCode ·
contentURL · likesCount · shareCount · role · isDeleted · createdAt · updatedAt · user`

`type` ∈ `text` \| `image` \| `audio` \| `video`. `user` is a nested `PostUser`
(`id · name · picture · isWarrior · email`).

### 4.3 Session bootstrap — the single most important flow

The splash is the **only** launcher entry point and seeds all global state. The Kotlin version was
badly broken and was fixed on 2026-10-08 (T-024/T-025); **port the fixed behaviour**:

```
launch
  └─ read token + userId from storage
       ├─ missing ──────────────────────────────→ Login
       └─ present → GET users/{userId}
             ├─ 200 + blocked ──────────────────→ toast + Login
             ├─ 200 + !isVerified ──────────────→ OTP screen
             ├─ 200 + verified
             │     └─ GET permission/users/{id}   ← MUST complete first
             │           ├─ ok ─────────────────→ deep-link target, else Home
             │           └─ fail ───────────────→ clear session + Login
             ├─ 401 ────────────────────────────→ clear session + Login
             └─ non-200 / network error ────────→ clear session + Login
```

**Every** dead end clears the stored session. Before T-024 the error paths did nothing at all, so
launching offline left the user staring at the logo forever and the broken session persisted
across restarts. Do not reproduce that.

### 4.4 Push notifications (FCM)

Six notification types, each with a permission gate and a destination:

| `type` | Gate | Destination | Extras |
|---|---|---|---|
| `post` | `POST`/`Read` | post detail | `postId` |
| `user` | `USER`/`Read` | profile | `userId` |
| `warrior` | `USER`/`Edit` | approve-request | `userId` |
| `announcement` | `ANNOUNCEMENT`/`Read` | post detail | `postId` |
| `file` | none | PDF viewer | `fileName`, `url` |
| `event` | none | webview | `url` |

In the Kotlin app this table is **implemented three times** and the copies have already diverged.
**Implement it once in RN.**

> 🔴 **Known production bug — fix during the port:** `NotificationService.updateToken` builds a
> token-refresh payload and **never sends it**. FCM rotates tokens on reinstall, data-clear and
> restore, so affected users **silently stop receiving push** until they next log in. Call
> `PUT api/v1/users/token/update` on every token refresh.

### 4.5 Offline Bible

`GET salvationlamb-images/bible.json` from `files.salvationlamb.com`, fetched **once** and cached
to disk forever (never invalidated — a corrected verse never reaches an existing install; decide
whether to keep that). Document shape: `{ "Old": [...], "New": [...] }`.

The bookmark is a comma-joined triple stored as one string:

```
"<edition>,<book>,<chapter>"      e.g. "பழைய ஏற்பாடு,ஆதியாகமம்,1"
```

⚠️ The first segment is the **localised display string**. Translating the app orphans every stored
bookmark. **Store a stable key in RN**, not the label.

---

## 5. Screen inventory — 37 screens

Bottom-nav tabs (order as shipped; **every tab except Bible is permission-gated**):

| # | Tab | Gate | Agent doc |
|---|---|---|---|
| 0 | Home feed | `POST`/`Read` | `FEED/HOME_FEED.md` |
| 1 | Files | `FILES`/`Read` | `MEDIA/FILES_BROWSER.md` |
| 2 | **Bible** | **none** ⚠️ | `BIBLE/BIBLE_ENTRY.md` |
| 3 | Admin video | `POST`+`VIDEO`/`Read` | `MEDIA/ADMIN_VIDEO.md` |
| 4 | Admin audio | `POST`+`AUDIO`/`Read` | `MEDIA/ADMIN_AUDIO.md` |
| 5 | Profile | `PROFILE`/`Read` | `PROFILE/MY_PROFILE.md` |

| Area | Screens | Agent docs |
|---|---|---|
| **Auth** (7) | splash, login, register, forgot-password, OTP verify, change password, reset password | `agents/AUTH/` |
| **Feed** (6) | home, add post, view post, view likes, favourites, image detail | `agents/FEED/` |
| **Profile** (5) | my profile, view profile, edit profile, followers/following, **approve request** | `agents/PROFILE/` |
| **Media** (6) | files browser, file list, PDF viewer, admin audio, admin video, webview | `agents/MEDIA/` |
| **Search** (3) | search entry, post results, profile results | `agents/SEARCH/` |
| **Bible** (3) | tab, reader, post composer | `agents/BIBLE/` |
| **Notifications** (3) | centre + 3 tabs, push service | `agents/NOTIFICATIONS/` |
| **Announcements** (1) | list | `agents/ANNOUNCEMENTS/` |
| **Shell** (5) | main nav, settings, about, theming, **no-permission** | `agents/APPSHELL/` |

---

## 6. Recommended build order

Dependency-driven: each phase needs the one before it.

| Phase | Build | Why here |
|---|---|---|
| **0** | API client, `asBool`/`asInt`, the 5 envelope unwrappers, auth interceptor, typed models | Everything depends on this. **Write the Jest tests from §2.1–2.2 first.** |
| **1** | Permission model + the no-permission screen | 34 gates depend on it. Port `UtilPermissionTest.kt` before the implementation. |
| **2** | Splash → session bootstrap → Login/Register/OTP | Nothing else is reachable without a session. §4.3 is the exact contract. |
| **3** | App shell: tabs, theming, overflow menu | Hosts every feature screen. **Build the menu once** — Kotlin has 6 copies. |
| **4** | Home feed + post detail + create post | The core product. |
| **5** | Profile, follow graph, edit profile | Biggest single screen (`EditProfileActivity`, 738 LOC). |
| **6** | Media: files, PDF, audio, video | Self-contained. |
| **7** | Notifications + FCM + announcements | Needs deep-link targets from 4–6 to exist. |
| **8** | Bible | Fully self-contained; can run in parallel from phase 3. |
| **9** | Search, settings, about, approve-request | Leaf screens. |

---

## 7. Bugs to fix **during** the port, not copy

The register has 851 entries. These are the ones where copying the current behaviour is actively
harmful — each is cheap to get right while writing new code.

| # | Issue | Where to read more |
|---|---|---|
| 1 | FCM token refresh is **never sent** → push silently dies | `NOTIFICATIONS/PUSH_SERVICE.md` `NP1` |
| 2 | Tray notification bypasses the `USER`/`Read` gate the in-app list enforces | `PUSH_SERVICE.md` `NP2` |
| 3 | The moderation screen (approve/reject) has **no permission check of its own** | `PROFILE/APPROVE_REQUEST.md` `AR1` |
| 4 | Approve/reject is irreversible with **no confirmation and no success feedback** | `APPROVE_REQUEST.md` `AR6`, `AR7` |
| 5 | The Bible tab is **ungated**, and the Bible post composer bypasses `POST`/`Create` | `BIBLE/BIBLE_LEAD.md` `B-1`, `B-9` |
| 6 | Paging is broken 4 different ways (listener per page, wrong insert index, double increment, page-size mismatch) | `NOTIFICATIONS/NOTIFICATION_LISTS.md` `NL3`–`NL5`, `NL21` |
| 7 | The permission-denied screen never says **which** permission was denied | `APPSHELL/NO_PERMISSION.md` `NOP4` |
| 8 | Silent failure everywhere: a failed request and an empty result look identical | `BUG_NOTES.md` cluster `CL-7` |
| 9 | `onBackPressed` calls `exitProcess(-1)`, killing the process and skipping persistence | `APPSHELL/MAIN_NAV.md` `MN12` |
| 10 | `Util` global state does not survive process death → NPEs after the OS restores the app | `BUG_NOTES.md` `CL-4`, `PLATFORM/COMMONS.md` `C-1` |

---

## 8. Open questions for the customer / backend team

Worth answering before phase 0.

1. **`isWarrior` semantics** — which of the two contradictory parsings (§2.3) does the backend
   intend? What values does it actually send?
2. **Server-side authorisation** — is `POST api/v1/review/{status}/{userId}` authorised on the
   server, or does it trust the client? The Android client has **no gate** on that screen (`AR1`).
3. **Bible cache invalidation** — should a corrected verse reach existing installs? Today it
   never does.
4. **Announcement write operations** — the permission model defines `ANNOUNCEMENT`/`Create`,
   `Edit`, `Delete`, but the client is read-only. Is an admin authoring flow planned?
5. **`api/v1/version`** — is the force-update check still live, and what should the RN app do
   when it fires?
6. **Typed API** — would the backend team consider returning real booleans and one consistent
   envelope? That removes traps §2.1 and §2.2 permanently, and is far cheaper than defending
   against them on two clients.

---

## 9. Change log

| Change | Detail |
|---|---|
| Created (T-027, 2026-10-08) | Written from the v1.2.0 source and the 54 agent docs. Covers the 7 fixed constraints (`applicationId`, signing key, Firebase project, base URLs), the 5 porting traps, all 44 endpoints with their envelopes, the 4 core models, the fixed session-bootstrap contract, the 6-type FCM routing table, the 37-screen inventory, a 10-phase build order and 10 bugs to fix rather than copy. |



