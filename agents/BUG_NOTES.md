# BUG_NOTES.md — Consolidated Known-Issue Register

> Maintained by the **PROJECT MANAGER** agent (`AGENTS.md`).
> Opened by task **T-017**.
>
> This file is the **single searchable view** of every defect recorded anywhere in the agent
> hierarchy. It does **not** replace the per-agent tables — those remain the source of truth,
> because `AGENTS.md` §5 requires a module agent to update its own `.md` in the same change set
> as the code.

---

## 1. How this file works

| Part | Who writes it | Rule |
|---|---|---|
| §1–§6 (this half) | **PM, by hand** | PM-level analysis: triage, clusters, fix order |
| §7 **Generated register** | `agents/tools/sync_bug_notes.py` | **Never edit by hand** — it is overwritten |

### Regenerating

```bash
python3 agents/tools/sync_bug_notes.py            # rewrite the generated half
python3 agents/tools/sync_bug_notes.py --check    # exit 1 if stale
```

The script parses every `## N. Known issues …` / `## N. Team-level known issues` table under
`agents/<TEAM>/` plus the `G1…G10` table in `AGENTS.md` §7, and rewrites everything between the
`AUTO-GENERATED:BEGIN` / `AUTO-GENERATED:END` markers.

### Two id systems — use both

| Id | Example | Stability | Use it for |
|---|---|---|---|
| **Agent-local** | `LOGIN L5`, `MAIN_NAV MN12`, `COMMONS C12` | **Permanent** — owned by the module agent | citing a bug in code comments, commits, conversation |
| **BUG-NNN** | `BUG-042` | **Positional** — renumbers when a doc gains a row | scanning / sorting this register only |

> **Always quote the agent-local id.** `BUG-NNN` is a convenience index, not a ticket number.

### Prefix collisions (important)

Agent-local ids are only unique **within one agent**. Several prefixes are reused:

| Prefix | Meaning in doc A | Meaning in doc B | Meaning in doc C |
|---|---|---|---|
| `F-` / `F` | `FEED_LEAD` team issues | `PROFILE_LEAD` team issues | `FORGOT_PASSWORD` module issues |
| `C` | `CHANGE_PASSWORD` module | `COMMONS` module | — |
| `S` | `SPLASH` module | `STORAGE` module | `SEARCH_LEAD` team |
| `P` | `RESET_PASSWORD` module | `PLATFORM_LEAD` team | — |
| `FL` | `FOLLOWERS` (PROFILE) | `FILE_LIST` (MEDIA) | — |

**Always qualify: `AUTH/SPLASH S8`, not `S8`.**

### Priority mapping

| Severity in agent doc | Priority here | Meaning |
|---|---|---|
| Critical | **P0** | crashes, data loss, app unusable |
| High | **P1** | broken feature, silent failure, security, ANR |
| Medium | **P2** | wrong behaviour in an edge case, UX gap |
| Low / Cosmetic | **P3** | polish, typos, deprecated APIs |
| *(lead rollup)* | **--** | an aggregate pointing at module rows; not a ticket |

**Rollup rows are not separate bugs.** A `*_LEAD.md` row such as `AUTH A1` is a team-level summary
of a defect itemised in one or more module docs (`LOGIN L1`, `SPLASH S*`). Count module rows, not
rollups, when sizing work.

---

## 2. Status tracking

The generated table has no `Status` column — the script cannot know it. Track live work here, and
move a row to *Fixed* when it closes.

### Open, in progress

| Agent-local id | Bug | Title | Owner | Task | Started |
|---|---|---|---|---|---|
| — | — | *(none yet — no code changes have been made to this repo)* | — | — | — |

### Fixed

| Agent-local id | Bug | Title | Fixed in | Closed (IST) |
|---|---|---|---|---|
| — | — | *(none yet)* | — | — |

### Won't fix / accepted

| Agent-local id | Title | Reason |
|---|---|---|
| `AUTH/REGISTER R14` | `Util.user` populated for a signed-out user | current flow depends on it; changing it is an AUTH redesign |
| `AUTH A12` | Sign-up creates no session, so a verified user is bounced to Login | **intended today** — document before changing |
| `MEDIA M-10` | Admin feeds pass page `"home"`, showing follow/fav on curated content | **needs a product decision from the customer** |

---

## 3. Closing a bug — procedure

1. The **module agent** fixes the code and, in the same change set, marks the row in **its own**
   `.md` — strike the text and append ` — **FIXED <YYYY-MM-DD>**`, or delete the row and add a
   Change log entry.
2. Run `python3 agents/tools/sync_bug_notes.py` so §7 reflects the doc.
3. **PM** moves the row from §2 *Open* to §2 *Fixed* and updates `agents/TASKS.md`.
4. If the bug was cross-cutting (§4), confirm **every** listed call site, not just the reported one.

---

## 4. Cross-cutting clusters — read before fixing anything

These are **not** single-screen defects. Each one is the *same* defect replicated across many
files, and each needs a **PM-coordinated** fix rather than a module-local one. Fixing the cluster
closes many register rows at once.

### CL-1 · The always-true token guard — **~30 call sites**

```kotlin
if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty())   // always true
```

Three `||`-ed negations can never all be false. Consequences:

- the session-lost `else` branch is **dead code** in almost every screen;
- a missing token is sent as the literal header **`Bearer null`**.

**Correct reference implementations:** `FEED/VIEW_LIKES` and `SEARCH/SEARCH_ENTRY` use `&&`.
Rolled up as `AUTH A19`, `FEED F-4`, `PROFILE F-6`, `MEDIA M-7`; itemised in nearly every module.
**Owner:** PLATFORM/COMMONS (extract one helper) + every team for its call sites.

### CL-2 · `getMyDetails()` duplicated **9 times**

`LoginActivity`, `SplashhScreenActivity`, `MainActivity`, `HomeFragment`, `ProfileFragment`,
`EditProfileActivity`, `AdminAudioFragment`, `AdminVideoFragment`, `AboutActivity`.

They have already drifted — see CL-3. Rolled up as `AUTH A4`, `FEED F-13`, `PROFILE F-8`,
`MEDIA M-8`, `APPSHELL AS-7`. **Owner:** PLATFORM (a shared `SessionBootstrap`), PM-coordinated.

### CL-3 · `isWarrior` parsed two contradictory ways

| Site | Expression | `""` / `null` → | `"TRUE"` / `"1"` → |
|---|---|---|---|
| `LoginActivity:140`, `SplashhScreenActivity:93`, `HomeFragment:472` | `isNullOrEmpty() \|\| != "false"` | **Warrior** | **Warrior** |
| `MainActivity:327`, `ProfileFragment:423` | `.toBoolean()` | normal user | **normal user** |

A user's role therefore depends on which screen last refreshed it. `AUTH/LOGIN L5`,
`AUTH/SPLASH S8`, `AUTH A9`. **Owner:** PLATFORM/DATA_MODELS + AUTH.

### CL-4 · The overflow menu copied into **6+ activities**

`MainActivity`, `ViewLikesActivity`, `FavoritesActivity`, `FollowerActivity`,
`EditProfileActivity`, `ViewProfileActivity` each carry their own ~60-line copy of
`R.menu.main_menu` — so **logout has six independent implementations**, and each copy calls
`Util.user.isReviewState.toBoolean()` with **no null guard** (NPE after process death).
`APPSHELL AS-1`, `AS-3`, `PROFILE F-7`, `F-10`. **Owner:** APPSHELL/MAIN_NAV, PM-coordinated.

### CL-5 · Four different response envelopes

| Shape | Key | Used by |
|---|---|---|
| object | `result` | AUTH, single-object reads everywhere |
| array + `count` | `results` | FEED, PROFILE, MEDIA admin feeds |
| array | `files` | MEDIA files API |
| **object** containing `users` + `posts` | `results` | SEARCH |

Mixing them yields an **empty list with no error**. `FEED F-2`, `SEARCH S-1`, `PLATFORM P-3`.
**Owner:** PLATFORM/NETWORK.

### CL-6 · Every model field is a `String` (including booleans and counts)

`.toBoolean()` returns `false` for `"TRUE"`, `"1"` and `null`; `likesCount` / `shareCount` /
`size` need manual `Integer.parseInt`. `G7`, `PLATFORM P-4`, `P-5`, `FEED F-12`.
**Owner:** PLATFORM/DATA_MODELS.

### CL-7 · Silent failure is the house style

Across nearly every screen: `onFailure` logs but shows nothing; the offline guard `return`s with no
dialog and no toast; several error branches have their `errorMessage` parsing **commented out**.
Net effect: **a failed request and an empty result look identical to the user.**
`G6`, `AUTH A2`, `A3`, `A21`, `FEED F-7`, `SEARCH S-9`. **Owner:** all teams; PM sets the policy.

### CL-8 · Fresh `authToken` observer registered per call, never removed

`userPreferences.authToken.asLiveData().observe(owner) { … }` inside each network method. The
observer re-fires on every emission → **duplicate requests**, and they accumulate.
`AUTH A20`, `FEED F-5`, `SEARCH S-3`. **Owner:** PLATFORM/STORAGE.

### CL-9 · Retrofit calls never cancelled in `onDestroy`

Callbacks touch views of a dead Activity/Fragment. Present in essentially every screen
(`L2`, `R17`, `F14`, `V14`, `C21`, …). **Owner:** all teams; one convention from PM.

### CL-10 · Layout id collisions force wildcard synthetic imports

`activity_register.xml` and `activity_edit_profile.xml` share **14 ids**; `RegisterActivity`
wildcard-imports **both** synthetic packages → the wrong view can be bound silently.
`AUTH A11`/`R2`, `PROFILE F-13`. Blocked behind `G2` (synthetics are removed in Kotlin 1.8+).
**Owner:** PLATFORM/BUILD_CONFIG + AUTH + PROFILE.

---

## 5. P0 — confirmed crashers / app-unusable

Fix these first. Each is a **real, reachable** failure, not a style issue.

| # | Agent-local id | What happens | Where |
|---|---|---|---|
| 1 | `AUTH/SPLASH` (`A26`) | **`SplashhScreenActivity.onFailure` does nothing.** Launch offline or with the server down and the user is **stuck on the splash logo forever** — and it is the only launcher entry point. | `SplashhScreenActivity.kt` |
| 2 | `AUTH/SPLASH` (`A27`) | Splash never clears a proven-invalid session, so the failing round-trip **repeats on every cold start** — a permanently broken install. | `SplashhScreenActivity.kt` |
| 3 | `MEDIA/PDF_VIEWER` | **NPE on any non-200 download** (two separate paths). Any missing or renamed PDF crashes the viewer. | `PdfActivity2.java` |
| 4 | `FEED/VIEW_POST` | `ViewPostActivity` initialises **14 views in its constructor, before `setContentView`** → guaranteed NPE. Harmless today **only because nothing launches it** — dead code that crashes the moment it is wired up. | `ViewPostActivity.kt` |
| 5 | `SEARCH` (`S-2`) | Both result fragments take **constructor arguments** instead of a `Bundle` → **crash on process-death restore**. | `SearchPostFragment.kt`, `SearchProfileFragment.kt` |
| 6 | `PROFILE/VIEW_PROFILE` (`F-15`) | `userId!!` force-unwrap → crash if the extra is ever missing. | `ViewProfileActivity.kt` |
| 7 | `PROFILE/FOLLOWERS` (`F-14`) | Loading starts **before** the views are bound → `UninitializedPropertyAccessException`. | `FollowerActivity.kt` |
| 8 | `APPSHELL/MAIN_NAV MN4` (`AS-5`), `SEARCH S-5` | `TabAdapter.getItem` ends `else -> b as Fragment` where `b` is `null`; `SearchAdapter.getItem` returns `null as Fragment`. **Adding one tab crashes the app.** | `TabAdapter.kt`, `SearchAdapter.kt` |
| 9 | `APPSHELL/MAIN_NAV MN2` (`AS-3`) + CL-4 | `Util.user.isReviewState.toBoolean()` unguarded in 6+ menu copies → **NPE after process death** when opening the menu. | 6 activities |
| 10 | `APPSHELL/MAIN_NAV MN12` (`AS-6`) | `onBackPressed` calls **`exitProcess(-1)`** — kills the process, skipping all lifecycle and persistence. | `MainActivity.kt` |

---

## 6. Recommended fix order

| Wave | Scope | Why in this position |
|---|---|---|
| **0** | **`git init`** (`G10`) | **No rollback exists today.** Do this before touching any code. |
| **1** | P0 #1 and #2 — Splash | The app can be **unusable at launch**. Highest user impact, smallest diff. |
| **2** | **CL-1** token-guard sweep | Mechanical, closes ~30 rows, removes `Bearer null`, and makes every logout branch real. |
| **3** | P0 #3–#10 | The remaining confirmed crashers. |
| **4** | **CL-7** error/offline feedback policy | Turns "the app is broken" into "the network failed". Cheap, large UX win. |
| **5** | **CL-4** extract the overflow menu once | Closes `AS-1`, `AS-3`, `F-7`, `F-10` and fixes logout in one place. |
| **6** | **CL-3** + **CL-6** typed models | Correct roles and counts; unblocks a lot of downstream logic. |
| **7** | **CL-2** shared `SessionBootstrap` | Removes 9-way drift. |
| **8** | `G1`, `G2`, `G3` build hygiene | `jcenter()`, synthetics, legacy support lib — needed before any Kotlin/AGP upgrade. |
| **9** | **QA team + `G9`** | No tests exist. Every wave above is a regression risk until this lands. |

> **Standing recommendation:** `G10` (no git) and `G9` (no tests) mean **every** fix above is
> currently unverifiable and irreversible. PM advises closing both before wave 3.

---

## 7. Generated register

Everything below is produced by `agents/tools/sync_bug_notes.py`. **Do not edit by hand.**

<!-- AUTO-GENERATED:BEGIN -- do not edit by hand; run agents/tools/sync_bug_notes.py -->

**653 tracked entries** extracted from 42 agent docs, plus 10 PM-level global issues.

| Severity | Count | Priority |
|---|---|---|
| Critical | 5 | P0 |
| High | 165 | P1 |
| Medium | 231 | P2 |
| Low | 134 | P3 |
| Cosmetic | 14 | P3 |
| Rollup (team-lead aggregate) | 104 | -- |
| **Distinct module-level defects** | **549** | |

### Per-team breakdown

| Team | Critical | High | Medium | Low | Cosmetic | Rollups | Total |
|---|---|---|---|---|---|---|---|
| PLATFORM | 0 | 23 | 33 | 25 | 1 | 17 | **99** |
| AUTH | 2 | 31 | 64 | 31 | 8 | 30 | **166** |
| APPSHELL | 0 | 16 | 24 | 15 | 0 | 10 | **65** |
| FEED | 1 | 28 | 34 | 24 | 4 | 13 | **104** |
| PROFILE | 0 | 30 | 27 | 12 | 0 | 15 | **84** |
| MEDIA | 2 | 28 | 34 | 17 | 1 | 10 | **92** |
| SEARCH | 0 | 9 | 15 | 10 | 0 | 9 | **43** |

### PM-level global issues (`AGENTS.md` §7)

| # | Issue | Owner | Risk |
|---|---|---|---|
| G1 | `jcenter()` still in `settings.gradle` (shut down, read-only) | PLATFORM / BUILD_CONFIG | build fragility |
| G2 | `kotlin-android-extensions` (synthetics) is deprecated and removed in Kotlin 1.8+ | PLATFORM / BUILD_CONFIG | blocks Kotlin upgrade |
| G3 | Legacy `com.android.support:appcompat-v7:28.0.0` mixed with AndroidX | PLATFORM / BUILD_CONFIG | duplicate-class risk |
| G4 | Two Retrofit builders with different base URLs (`Util` vs `APIUtil`) | PLATFORM / NETWORK | wrong-host bugs |
| G5 | `Thread.sleep(2000)` on the main thread in `LoginActivity` and `SplashhScreenActivity` | AUTH | ANR |
| G6 | Network calls silently no-op when offline (no user feedback) in most screens | all teams | UX |
| G7 | All API responses are untyped `JsonObject`; model fields are `String` even for booleans | PLATFORM / DATA_MODELS | parse crashes |
| G8 | `usesCleartextTraffic="true"` + `networkSecurityConfig` allow plain HTTP | PLATFORM / BUILD_CONFIG | security |
| G9 | No unit tests; only the generated instrumented test exists | PM (future QA team) | regressions |
| G10 | Repo is not under git — no change history or rollback | PM | safety |

---

## Full register

### PLATFORM — 99 entries

#### `PLATFORM_LEAD` — team lead (rollups) · `agents/PLATFORM/PLATFORM_LEAD.md` · 17 entries

| Bug | Agent id | Issue | Affects module(s) | Risk |
|---|---|---|---|---|
| BUG-001 | P-1 | Two Retrofit builders, different base URLs; `APIUtil` is dead code (G4) | NETWORK | wrong-host bugs |
| BUG-002 | P-2 | No OkHttp timeouts, no interceptor, no logging, no auth interceptor | NETWORK | hangs; token repeated by hand at 30+ call sites |
| BUG-003 | P-3 | Every response is `Call<JsonObject?>`; no typed responses | NETWORK | boilerplate + parse crashes |
| BUG-004 | P-4 | All model fields are `String`, even booleans and counts (G7) | DATA_MODELS | wrong logic, crashes on nulls |
| BUG-005 | P-5 | No `@SerializedName`, no defaults, non-null Kotlin types — a missing JSON field yields `null` in a non-null field | DATA_MODELS | silent NPEs |
| BUG-006 | P-6 | DataStore on `1.0.0-alpha01` using removed APIs (`createDataStore`, `preferencesKey`) | STORAGE | blocks upgrades |
| BUG-007 | P-7 | `UserPreferences` constructs a **new DataStore per instance**; screens create it freely | STORAGE | multiple-instance risk |
| BUG-008 | P-8 | Reads expose `Flow<String>` via `.toString()`, so a missing key reads as the literal `"null"` | STORAGE | string-compare hacks everywhere |
| BUG-009 | P-9 | `Util` holds mutable global state with no lifecycle; process death clears it silently | COMMONS | stale/empty state after restore |
| BUG-010 | P-10 | `Commons.makeWarrior` is a 100-line UI dialog living in a util class | COMMONS | UI logic inside PLATFORM |
| BUG-011 | P-11 | `isNetworkAvailable` **toasts** from a utility, mixing policy and UI | COMMONS | duplicate/unexpected toasts |
| BUG-012 | P-12 | `jcenter()` still in `settings.gradle` (G1) | BUILD_CONFIG | build fragility |
| BUG-013 | P-13 | `kotlin-android-extensions` deprecated; removed in Kotlin 1.8+ (G2) | BUILD_CONFIG | blocks Kotlin upgrade |
| BUG-014 | P-14 | `com.android.support:appcompat-v7:28.0.0` mixed with AndroidX (G3) | BUILD_CONFIG | duplicate classes |
| BUG-015 | P-15 | `usesCleartextTraffic="true"` + a network-security config naming a **stale** host (G8) | BUILD_CONFIG | security |
| BUG-016 | P-16 | `proguard-rules.pro` is empty and `minifyEnabled false` | BUILD_CONFIG | no shrinking/obfuscation |
| BUG-017 | P-17 | Signing keys committed in `app/Key/` | BUILD_CONFIG | **secret exposure** |

#### `BUILD_CONFIG` — module · `agents/PLATFORM/BUILD_CONFIG.md` · 20 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-018 | B-1 | P1 | High | **`jcenter()` is still a repository** (shut down, read-only); three artifacts (`spots-dialog`, `android-image-cropper`, `richeditor-android`) still resolve from it | migrate to maintained forks on mavenCentral, or vendor them |
| BUG-019 | B-6 | P1 | High | `usesCleartextTraffic="true"` globally, plus a `network_security_config` naming a **stale** host (G8) | set `false` and whitelist only the real host |
| BUG-020 | B-9 | P1 | High | Signing keystore and Play export key **committed** to the tree | rotate keys and move them out of the repo |
| BUG-021 | B-11 | P1 | High | **`kotlin-android-extensions` is deprecated** and removed in Kotlin 1.8+; the whole UI layer depends on synthetics (G2) | blocks any Kotlin upgrade; a ViewBinding migration is a cross-team project |
| BUG-022 | B-12 | P1 | High | **`com.android.support:appcompat-v7:28.0.0` alongside AndroidX**, papered over by Jetifier (G3) | remove the two `com.android.support` lines and verify |
| BUG-023 | B-2 | P2 | Medium | `READ/WRITE_EXTERNAL_STORAGE` without `android:maxSdkVersion`, though Android 13 media permissions are present | add `maxSdkVersion="32"` |
| BUG-024 | B-3 | P2 | Medium | `CAMERA` declares `android:required` / `android:requiredFeature`, which are **not valid** on `<uses-permission>`; no `<uses-feature>` is declared | remove the attributes; add a proper `<uses-feature>` |
| BUG-025 | B-4 | P2 | Medium | `allowBackup="true"` with no backup rules — the DataStore **auth token** is included in cloud backups | set `false` or exclude the datastore file |
| BUG-026 | B-7 | P2 | Medium | `EditProfileActivity`, `RegisterActivity` and `MainActivity` are `exported="true"` with **no intent-filter** | set `exported="false"` |
| BUG-027 | B-8 | P2 | Medium | `minifyEnabled false` and an empty `proguard-rules.pro` | enable R8 for release with keep rules for Gson models + Retrofit |
| BUG-028 | B-13 | P2 | Medium | No `debug` build type, no flavours, no `buildConfigField` — the base URL is hard-coded in `Util.java` | add `buildConfigField` for the URL (with NETWORK) |
| BUG-029 | B-14 | P2 | Medium | `okhttp` declared **without a version**; `lifecycle-runtime-ktx:2.3.0-alpha03` is an alpha; `android-pdf-viewer` is a beta | pin and stabilise |
| BUG-030 | B-16 | P2 | Medium | `cronet-embedded:76.3809.111` (2019) adds ~20 MB and appears unused | verify usage and remove |
| BUG-031 | B-17 | P2 | Medium | `firebase-crashlytics-buildtools` is present but Crashlytics is **not configured** — no crash reporting at all | wire it up or drop it |
| BUG-032 | B-20 | P2 | Medium | No CI, no lint baseline, no dependency-update checks | add once the agent hierarchy is in place |
| BUG-033 | B-5 | P3 | Low | `versionCode` / `versionName` are hard-coded with no automation | derive from CI/git |
| BUG-034 | B-10 | P3 | Low | The stock instrumented test sits in package `com.example.activity` | move to `com.veha.activity` |
| BUG-035 | B-15 | P3 | Low | `converter-gson:2.5.0` with `retrofit:2.9.0` | align versions |
| BUG-036 | B-18 | P3 | Low | `largeHeap="true"` masks memory problems | profile and remove |
| BUG-037 | B-19 | P3 | Low | `viewBinding true` is enabled but almost unused, so both mechanisms ship | pick one (see B-11) |

#### `COMMONS` — module · `agents/PLATFORM/COMMONS.md` · 20 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-038 | C-1 | P1 | High | `Util` statics are the de-facto session object but do **not** survive process death | re-hydrate from DataStore on resume, or stop relying on statics |
| BUG-039 | C-2 | P1 | High | `Util.player` is a **static `MediaPlayer`** that is never guaranteed to be released | own it in the MEDIA screen |
| BUG-040 | C-9 | P1 | High | `isNetworkAvailable` **toasts from a utility**, mixing policy with UI at 62 call sites | return a result and let screens decide; the trailing space in `"No Internet "` is also a typo |
| BUG-041 | C-12 | P1 | High | `makeWarrior` returns a status assigned **asynchronously**, so it is always `""`; `MainActivity:185`'s `contentEquals("success")` can never be true | take a callback/`suspend`, or move to a ViewModel |
| BUG-042 | C-13 | P1 | High | The token guard is an always-true `\|\|` chain (same bug as CHANGE_PASSWORD) | use `&&`, or test the token explicitly |
| BUG-043 | C-16 | P1 | High | `makeWarrior` is 100 lines of dialog UI inside PLATFORM | move to PROFILE as a real screen/dialog fragment |
| BUG-044 | C-4 | P2 | Medium | `isValidPassword` silently caps length at 20 and allows only `@#$%^&+=` as symbols; no screen tells the user about the cap | widen the symbol class, document the rule |
| BUG-045 | C-5 | P2 | Medium | `SimpleDateFormat` with the default locale in `formatDate` / `getTimeAgo` | pass `Locale.US` for machine formats |
| BUG-046 | C-8 | P2 | Medium | `getVideo()` hard-codes a **second** host, independent of `Util.url` | move to NETWORK config |
| BUG-047 | C-10 | P2 | Medium | Connectivity is inferred from transport presence, not reachability | `NET_CAPABILITY_VALIDATED` |
| BUG-048 | C-14 | P2 | Medium | A new `authToken` observer is registered on every `makeWarrior` call | one-shot read |
| BUG-049 | C-15 | P2 | Medium | A failed warrior request shows the user nothing | toast on error and `onFailure` |
| BUG-050 | C-20 | P2 | Medium | `Util.java` mixes four unrelated concerns (constants, state, validators, networking) in 158 lines | split into `Session`, `Validators`, `DateUtils` |
| BUG-051 | C-3 | P3 | Low | Validators use `.find()` instead of `.matches()` | switch to `matches` |
| BUG-052 | C-6 | P3 | Low | `getTimeAgo` can render negative durations on clock skew, and swallows all exceptions | clamp at 0, log properly |
| BUG-053 | C-7 | P3 | Low | `getReligion()` rebuilds a 36-item list on every call and assigns a private static | make it an immutable constant |
| BUG-054 | C-11 | P3 | Low | `isNetworkAvailable(context!!)` force-unwraps | take a non-null `Context` |
| BUG-055 | C-17 | P3 | Low | `toDate()` / `formatTo()` are instance members, not top-level extensions | move to a file-level extension |
| BUG-056 | C-19 | P3 | Low | `Commons` is instantiated 62 times (`Commons().isNetworkAvailable(...)`) though it is stateless | make it an `object` |
| BUG-057 | C-18 | P3 | Cosmetic | Empty `showAlert()` dead code | delete |

#### `DATA_MODELS` — module · `agents/PLATFORM/DATA_MODELS.md` · 14 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-058 | D1 | P1 | High | `Loginresp.isVerifiedUser` vs `UserRslt.isVerified` — same concept, two names | align with the backend; this forces LOGIN's second round-trip |
| BUG-059 | D5 | P1 | High | **Non-null `String` fields can hold `null`** because Gson bypasses constructors; a missing server field becomes a delayed NPE | make fields nullable with defaults (`val x: String? = null`), or add `@SerializedName` + a null-safe parser |
| BUG-060 | D6 | P1 | High | **All booleans are `String`** (`isWarrior`, `isVerified`, `isFreshUser`, `blocked`, `isReviewState`) and read with `.toBoolean()`, so `"TRUE"`/`"1"` silently become `false` (G7) | `Boolean` fields, or a shared `String.toBooleanSafe()` in COMMONS |
| BUG-061 | D8 | P1 | High | **No `@SerializedName` anywhere** — field names are hard-bound to the JSON, so a Kotlin rename silently breaks parsing with no compile error | annotate every field |
| BUG-062 | D7 | P2 | Medium | Counts (`likesCount`, `shareCount`) and `size` are `String` | `Int` / `Long` |
| BUG-063 | D9 | P2 | Medium | `Loginresp` and `UserRslt` overlap by ~24 fields with no shared supertype | extract a common interface/base |
| BUG-064 | D11 | P2 | Medium | No `data class` for the **response envelope** itself (`result` / `message` / `status` / `errorMessage`) — every screen re-parses it by hand | add `ApiResponse<T>` (with NETWORK) |
| BUG-065 | D12 | P2 | Medium | No pagination wrapper despite 4 paged endpoints | add a `Page<T>` model (with NETWORK + FEED) |
| BUG-066 | D14 | P2 | Medium | `Loginresp.password` echoes the password back from the server | raise with the backend; drop the field |
| BUG-067 | D2 | P3 | Low | Geo models use snake_case while the rest use camelCase | `@SerializedName` + camelCase properties |
| BUG-068 | D3 | P3 | Low | `Countries.native` uses a Kotlin soft keyword as a field name | rename with `@SerializedName("native")` |
| BUG-069 | D4 | P3 | Low | `AllFavList` and `FavPost` are structurally identical duplicates | delete one |
| BUG-070 | D10 | P3 | Low | `Posts.user` is the only `var`; everything else is `val`, so mutation is inconsistent | use `copy()` instead |
| BUG-071 | D13 | P3 | Low | Dates are `String` with no parsing contract in the model | keep as-is, or expose typed accessors |

#### `NETWORK` — module · `agents/PLATFORM/NETWORK.md` · 16 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-072 | N1 | P1 | High | `APIUtil.kt` is a **second, dead Retrofit builder** pointing at a different host over plain HTTP (G4) | delete it, or make it the single source of truth |
| BUG-073 | N8 | P1 | High | No auth interceptor; `"Bearer $token"` is hand-built at ~30 call sites | add an `Interceptor` that injects the header |
| BUG-074 | N11 | P1 | High | **No OkHttp timeouts** — a stalled connection hangs forever (this is what makes SPLASH's silent `onFailure` fatal) | set connect/read/write timeouts |
| BUG-075 | N15 | P1 | High | Every response is `Call<JsonObject?>`, so there is **no compile-time contract** with the backend (G7) | typed responses (with DATA_MODELS) |
| BUG-076 | N2 | P2 | Medium | `postCall` takes a **dynamic path**, hiding which endpoints exist and defeating search | declare explicit methods for `login` and `users` |
| BUG-077 | N5 | P2 | Medium | Two `getPost` overloads differing only by parameters — easy to call the wrong one | rename to `getFeed` / `getPostById` |
| BUG-078 | N6 | P2 | Medium | `getFollowers` -> `follows/user/{id}` vs `getFollowing` -> `follows/{id}`; names do not reveal paths | document or rename |
| BUG-079 | N7 | P2 | Medium | Callers test `code() == 200`, so `201 Created` / `204 No Content` are treated as failures | use `response.isSuccessful` |
| BUG-080 | N12 | P2 | Medium | No logging interceptor, so failures can only be diagnosed by adding `Log.e` by hand | add `HttpLoggingInterceptor` on debug builds |
| BUG-081 | N13 | P2 | Medium | `postProfilePic` sends an image inside a `JsonObject` rather than multipart | use `@Multipart` if the backend allows |
| BUG-082 | N14 | P2 | Medium | No documented pagination contract: `page`/`size` exist on 4 endpoints but the response shape is unspecified | document with FEED |
| BUG-083 | N3 | P3 | Low | Path casing is inconsistent (`Users/image/...`, `Country` vs `users/...`, `city/...`) | align with the backend |
| BUG-084 | N4 | P3 | Low | `getFav` and `getMyFav` are **identical** declarations of the same path | delete one |
| BUG-085 | N9 | P3 | Low | `converter-gson:2.5.0` paired with `retrofit:2.9.0` | align both to 2.9.0 |
| BUG-086 | N10 | P3 | Low | OkHttp is declared **without a version** | pin it or use the BOM |
| BUG-087 | N16 | P3 | Low | The Retrofit singleton is never rebuilt, so `Util.url` cannot change at runtime | add a reset for environment switching |

#### `STORAGE` — module · `agents/PLATFORM/STORAGE.md` · 12 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-088 | S-1 | P1 | High | Pinned to `datastore-preferences:1.0.0-alpha01` using `createDataStore` / `preferencesKey`, both **removed** in stable | migrate to `preferencesDataStore` + `stringPreferencesKey` (BUILD_CONFIG + every call site) |
| BUG-089 | S-2 | P1 | High | The three `String` flows return the literal **`"null"`** when a key is absent, so `isNullOrEmpty()` never matches and every consumer hard-codes `== "null"` | expose `Flow<String?>` and drop `.toString()`, then update all consumers in one change |
| BUG-090 | S-6 | P1 | High | A **new `DataStore` per instance** over the same file, at 26 sites; only the alpha's missing guard prevents a crash | make it a singleton (or an `object` with an app-level `Context`) |
| BUG-091 | S-7 | P1 | High | Nothing in the app reads one-shot; all 5 flows are observed continuously, causing duplicate network calls on re-emission | add one-shot helpers (`suspend fun getAuthToken(): String?`) |
| BUG-092 | S-5 | P2 | Medium | No `clearAll()`; logout deletes only `token` + `userId`, leaving `isNight`, `isFirst`, `textSize` from the previous user | add `suspend fun clearSession()` and use it everywhere |
| BUG-093 | S-8 | P2 | Medium | The token is stored in **plain text**; DataStore is not encrypted | EncryptedSharedPreferences or an encrypted DataStore wrapper |
| BUG-094 | S-9 | P2 | Medium | Writes are fire-and-forget with no error handling | surface failures, at least by logging |
| BUG-095 | S-10 | P2 | Medium | No migration strategy; renaming a key silently logs every user out on upgrade | document and version keys |
| BUG-096 | S-3 | P3 | Low | `saveUserId(token: String)` — parameter misnamed | rename to `userId` |
| BUG-097 | S-4 | P3 | Low | `isFirstTime` uses `preferences[IS_FIRST] as Boolean` instead of the typed accessor | use the typed value |
| BUG-098 | S-11 | P3 | Low | The store name `"SalvationLamb"` is a magic string in the constructor | extract a constant |
| BUG-099 | S-12 | P3 | Low | Boilerplate: 5 keys x 3 members, all identical in shape | generic helpers |

### AUTH — 166 entries

#### `AUTH_LEAD` — team lead (rollups) · `agents/AUTH/AUTH_LEAD.md` · 30 entries

| Bug | Agent id | Issue | Affects module(s) | Risk |
|---|---|---|---|---|
| BUG-100 | A1 | `Thread.sleep(2000)` runs on the main thread inside the Retrofit callback | LOGIN, SPLASH | ANR / frozen UI |
| BUG-101 | A2 | `onFailure` shows no user feedback (log only) in Login and Splash | LOGIN, SPLASH | user sees a dead button |
| BUG-102 | A3 | Offline -> `isNetworkAvailable` false -> the method silently returns; the progress dialog is never even shown and nothing is said | all AUTH screens | user thinks the app is broken |
| BUG-103 | A4 | `getMyDetails()` duplicated in 3 classes | LOGIN, SPLASH (+ APPSHELL) | fixes drift |
| BUG-104 | A5 | Server error matching relies on English substrings (`"Invalid password"`, `"Invalid Email-Id"`); any backend wording change silently shows nothing | LOGIN | silent failure |
| BUG-105 | A6 | `errorMessage` toasted with its JSON quotes | FORGOT_PASSWORD, OTP_VERIFY, REGISTER, CHANGE_PASSWORD | cosmetic |
| BUG-106 | A7 | Password text is sent and also present in `Loginresp.password` from the server | LOGIN, REGISTER | security smell — raise with backend |
| BUG-107 | A8 | Unverified user is routed to `ForgotPasswordActivity` without `finish()`, so Back returns to the previous screen | LOGIN, SPLASH | nav inconsistency |
| BUG-108 | A9 | `isVerified` / `isWarrior` / `isFreshUser` are `String` fields parsed with `.toBoolean()`; `null`/`"1"`/`"TRUE"` behave unexpectedly | all AUTH | wrong routing |
| BUG-109 | A10 | `SplashhScreenActivity` observes DataStore flows that re-emit, so `getMyDetails` can fire more than once | SPLASH | duplicate calls |
| BUG-110 | A11 | `RegisterActivity` wildcard-imports **both** `activity_register.*` and `activity_edit_profile.*` synthetics; the two layouts share 14 ids | REGISTER | wrong view bound silently |
| BUG-111 | A12 | Registration never creates a session (no token/userId), so a newly verified user is always bounced to Login | REGISTER, OTP_VERIFY | expected today — document before changing |
| BUG-112 | A13 | `page = intent.getStringExtra("page").toString()` yields the literal string `"null"` when no extra is passed; forgot mode works only because `"null" != "verify"` | FORGOT_PASSWORD, OTP_VERIFY | a "null-safety cleanup" silently flips the screen into the wrong mode |
| BUG-113 | A14 | `ForgotPasswordActivity` is the only AUTH screen with **no email format validation** (`Util.isValidEmail` unused) | FORGOT_PASSWORD | inconsistent UX vs Login/Register |
| BUG-114 | A15 | `Log.e("data", data.toString())` logs the email **and OTP** in clear text | FORGOT_PASSWORD, OTP_VERIFY | privacy leak in release logcat |
| BUG-115 | A16 | Verify mode fires a verification email on **every** entry and every rotation (`checkValid` is called from `onCreate`), with no cooldown | OTP_VERIFY | mail flooding / rate limiting |
| BUG-116 | A17 | Post-verification routing reads the ambient `Util.userId` instead of an explicit extra, so the destination silently depends on which screen opened it | OTP_VERIFY | wrong landing screen if a caller changes |
| BUG-117 | A18 | The magic strings `"verify"`, `"page"` and `"email"` are hand-typed in 4 files (Register, Login, Splash, ForgotPasswordActivity) | OTP_VERIFY, REGISTER, LOGIN, SPLASH | a typo silently renders the wrong mode |
| BUG-118 | A19 | `ChangePasswordActivity`'s token guard is an always-true `\|\|` chain, so its session-lost branch is **dead code** and a missing token is sent as `Bearer null` | CHANGE_PASSWORD | silent auth failure |
| BUG-119 | A20 | A new `authToken` LiveData observer is registered on **every** button tap and never removed | CHANGE_PASSWORD | duplicate requests |
| BUG-120 | A21 | Both error branches in Change/Reset Password have their `errorMessage` parsing **commented out**, so a wrong old password / expired OTP both show "Something went wrong" | CHANGE_PASSWORD, RESET_PASSWORD | undiagnosable failures |
| BUG-121 | A22 | Intent extras are read with `.toString()` in `ForgotPasswordActivity` (yielding `"null"`) but **without** it in `ChangePasswordActivity` (yielding a real `null`) — two opposite conventions inside one flow | FORGOT_PASSWORD, OTP_VERIFY, CHANGE_PASSWORD, RESET_PASSWORD | confusing / fragile |
| BUG-122 | A23 | A successful password **reset** shows no confirmation at all — the screen just closes | RESET_PASSWORD | user cannot tell if it worked |
| BUG-123 | A24 | The "new password is same as old password" check is **inert** during a reset (`old_pwd` is GONE, so the value is `""`), so a user can reset to their current password | RESET_PASSWORD | no reuse protection |
| BUG-124 | A25 | `ChangePasswordActivity` shares its validation chain, `onCreate` prologue and all three lifecycle overrides between two agents; only `changePassword()` / `forgotPassword()` are exclusive | CHANGE_PASSWORD, RESET_PASSWORD | cross-agent regressions |
| BUG-125 | A26 | **`SplashhScreenActivity.onFailure` does nothing** — no toast, no navigation. A network failure or an offline launch leaves the user stuck on the splash logo forever | SPLASH | **app unusable at launch** |
| BUG-126 | A27 | Splash does **not** clear a proven-invalid session (no `deleteAuthToken`/`deleteUserId`), unlike every other 401-ish handler, so the failing round-trip repeats on every cold start | SPLASH | permanent broken state |
| BUG-127 | A28 | Splash runs **two** `Thread.sleep(2000)` calls on the main thread (before the request and inside its callback) — ≥ 4 s of frozen UI for a returning user, on top of A1 | SPLASH, LOGIN | ANR at launch |
| BUG-128 | A29 | The empty-`userId` branch in Splash calls `finish()` without `return@observe`, so execution continues and issues `GET api/v1/users/` with no id | SPLASH | malformed request |
| BUG-129 | A30 | Splash is the **only** launcher entry and the sole seeder of `Util.isNight`, `Util.fontSize`, `Util.userId`, `Util.user`, `Util.isWarrior`, `Util.isFirst` — bypassing it leaves global state unset | SPLASH | must never lose the intent-filter |

#### `CHANGE_PASSWORD` — module · `agents/AUTH/CHANGE_PASSWORD.md` · 20 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-130 | C11 | P1 | High | The token guard `!isEmpty(it) \|\| !it.equals("null") \|\| !it.isNullOrEmpty()` is **always true**; the whole session-lost `else` branch is unreachable | change to `&&`, or test the token explicitly |
| BUG-131 | C12 | P1 | High | As a result, a missing token is sent as the header `Bearer null` instead of logging the user out | validate before calling |
| BUG-132 | C13 | P1 | High | A new `authToken.asLiveData().observe(this)` is registered on **every** button tap and never removed; DataStore re-emissions can fire duplicate requests | read the token once (e.g. `lifecycleScope.launch { first() }`) outside the listener |
| BUG-133 | C14 | P1 | High | The error branch has its `errorMessage` parsing **commented out** and shows a generic "Something went wrong", so "old password incorrect" is indistinguishable from a server fault | restore the parsing |
| BUG-134 | C18 | P1 | High | `onFailure` shows nothing to the user | toast a generic failure message |
| BUG-135 | C21 | P1 | High | The Retrofit call is not cancelled in `onDestroy` (only after a response) | keep the `Call` and `cancel()` it |
| BUG-136 | C1 | P2 | Medium | Extras are read **without** `.toString()` here but **with** it in `ForgotPasswordActivity` — two opposite null conventions in the same flow | standardise (PM-level, spans several agents) |
| BUG-137 | C2 | P2 | Medium | No `ScrollView` and no `windowSoftInputMode`; with three fields plus the keyboard the buttons can be unreachable | wrap in `ScrollView` / set `adjustResize` (**shared layout**) |
| BUG-138 | C5 | P2 | Medium | `cnfm_pwd` has **no `inputType="textPassword"`** — the confirmation is typed in clear text | add `inputType` (**shared layout**) |
| BUG-139 | C7 | P2 | Medium | The `"Enter Password"` branch is unreachable — an empty password fails `isValidPassword` first | check emptiness before the regex (**shared chain** — coordinate) |
| BUG-140 | C8 | P2 | Medium | The "same as old password" check compares **old vs confirm**, not old vs new | compare `oldPasswordTxt` with `passwordTxt` (**shared chain**) |
| BUG-141 | C10 | P2 | Medium | The old password is never validated (emptiness or format) before being sent | add an emptiness check in this mode only |
| BUG-142 | C17 | P2 | Medium | No token refresh after a password change; a server-side session invalidation goes unnoticed | re-login or refresh the token on success |
| BUG-143 | C19 | P2 | Medium | Offline -> the method returns silently; no dialog, no toast | toast "No internet connection" |
| BUG-144 | C20 | P2 | Medium | `onResume` calls `dialog.dismiss()`, killing the spinner of a still-running request | remove the `onResume` override (**shared**) |
| BUG-145 | C23 | P2 | Medium | Passwords are sent in plain JSON while the app sets `usesCleartextTraffic="true"` globally | PLATFORM/BUILD_CONFIG to restrict cleartext |
| BUG-146 | C4 | P3 | Low | `cnfm_pwd_op` has no `passwordToggleEnabled`, unlike the other two fields | add for consistency (**shared layout**) |
| BUG-147 | C6 | P3 | Low | `otp_op` / `otp` are **dead views** — present in the layout, never referenced by the code | delete (**shared layout** — coordinate with RESET_PASSWORD) |
| BUG-148 | C16 | P3 | Cosmetic | `Gson` is imported but unused (only the commented-out code referenced it) | remove with the C14 fix |
| BUG-149 | C22 | P3 | Cosmetic | `"Somthing Went Wrong"` is misspelled (same typo as `HomeFragment`/`MainActivity`) | fix when the branch is revived |

#### `FORGOT_PASSWORD` — module · `agents/AUTH/FORGOT_PASSWORD.md` · 20 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-150 | F1 | P1 | High | Mode selection depends on `getStringExtra("page").toString()` producing the **literal `"null"`**; a "cleanup" to a proper null check or a typo in the extra silently flips the screen into verify mode | use a constant + `intent.getStringExtra("page") == MODE_VERIFY`, coordinated with `OTP_VERIFY.md` |
| BUG-151 | F5 | P1 | High | After step 1 the email field stays **editable**, so the user can change the address and then submit an OTP for a different email than the one it was sent to — `checkOtp` re-reads `email.text` at tap time | disable the email field once the OTP is requested, or pass the submitted address |
| BUG-152 | F12 | P1 | High | `onFailure` shows nothing to the user on either call | toast a generic failure message |
| BUG-153 | F14 | P1 | High | Retrofit calls are never cancelled in `onDestroy`; the callback touches views of a dead activity | keep the `Call` and `cancel()` it |
| BUG-154 | F2 | P2 | Medium | No `ScrollView` — with the keyboard open on a small screen the buttons can be unreachable; no `windowSoftInputMode` in the manifest | wrap in `ScrollView` and/or set `adjustResize` |
| BUG-155 | F3 | P2 | Medium | `email` has no `inputType="textEmailAddress"` | add `inputType` and `imeOptions` |
| BUG-156 | F4 | P2 | Medium | `otp` has no `inputType="number"`, no `maxLength`, no auto-advance | add numeric input type and a length cap |
| BUG-157 | F7 | P2 | Medium | `onBackPressed` pushes a **new** `LoginActivity` even though Login is already on the stack | just `finish()` in forgot mode (shared method — coordinate with OTP_VERIFY) |
| BUG-158 | F9 | P2 | Medium | **No email format validation** — `Util.isValidEmail` is never called, unlike Login/Register | add the same check for consistent UX |
| BUG-159 | F11 | P2 | Medium | `Log.e("data", data.toString())` prints the email **and OTP** to logcat in release builds | remove the log |
| BUG-160 | F13 | P2 | Medium | Offline -> the method returns silently; no dialog, no toast | toast "No internet connection" |
| BUG-161 | F15 | P2 | Medium | `emailID` is `lateinit` but assigned **only** in verify mode — any new forgot-mode code reading it crashes | initialise it in both branches |
| BUG-162 | F16 | P2 | Medium | Rotation resets the screen to step 1 (`otp_op` back to `gone`, listener reverted) while the typed OTP survives in the hidden field | persist the step in `onSaveInstanceState` |
| BUG-163 | F17 | P2 | Medium | No "resend OTP" button and no cooldown/timer | ask the customer |
| BUG-164 | F8 | P3 | Low | `if (forgot_btn.text != "verify")` is dead code and compares against the wrong case (`"Verify"`) | delete the condition |
| BUG-165 | F10 | P3 | Low | Emptiness is checked on the trimmed value but the **untrimmed** text is sent | send `.trim()` |
| BUG-166 | F18 | P3 | Low | `message` / `errorMessage` are toasted with their JSON quotes | use `asString` instead of `toString()` |
| BUG-167 | F19 | P3 | Low | Success and failure of step 1 are indistinguishable to an automated test — both just toast server text | add explicit states |
| BUG-168 | F20 | P3 | Low | `status` is never logged here, unlike Login/Register, making server issues harder to diagnose | log it |
| BUG-169 | F6 | P3 | Cosmetic | Button label becomes lower-case **"confirm"** | use "Confirm" / a string resource |

#### `LOGIN` — module · `agents/AUTH/LOGIN.md` · 13 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-170 | L1 | P1 | High | `Thread.sleep(2000)` inside `getMyDetails` runs on the **main thread** | remove it, or use `Handler.postDelayed` / `lifecycleScope.launch { delay(2000) }` |
| BUG-171 | L2 | P1 | High | Retrofit calls are never cancelled in `onDestroy`; callbacks touch a dead activity | keep the `Call` references and `cancel()` them, or guard with `isFinishing`/`isDestroyed` |
| BUG-172 | L3 | P1 | High | Unmatched server errors show **no** message; `onFailure` shows none either | add an `else` toast with the server text and a generic failure toast |
| BUG-173 | L4 | P2 | Medium | Every successful login overwrites `isNight` with `Util.DEFAULT`, wiping the saved theme | only write the theme when no value exists |
| BUG-174 | L5 | P2 | Medium | `isWarrior` logic treats empty/null as **true** (`isNullOrEmpty() \|\| != "false"`) | compare explicitly with `"true"` |
| BUG-175 | L6 | P2 | Medium | `email` field has no `inputType="textEmailAddress"` and no `imeOptions`; no field-level `error` display | add `inputType`, IME next/done, and `TextInputLayout` errors |
| BUG-176 | L7 | P2 | Medium | Unverified users are sent to the verify screen **without** `finish()` | `finish()` after `startActivity` |
| BUG-177 | L8 | P2 | Medium | Offline (`isNetworkAvailable == false`) -> method returns silently, no dialog, no toast | toast "No internet connection" |
| BUG-178 | L12 | P2 | Medium | Password is sent in plain JSON over a base URL that is HTTPS, but the app sets `usesCleartextTraffic="true"` globally | PLATFORM/BUILD_CONFIG to restrict cleartext |
| BUG-179 | L9 | P3 | Low | `getMyDetails` has no progress indicator — a ~2s+ blank gap after the dialog closes | keep the dialog up until routing completes |
| BUG-180 | L10 | P3 | Low | `Util` fields are assigned twice (before and inside the coroutine) | assign once |
| BUG-181 | L11 | P3 | Low | All strings hard-coded; not localisable; `status`/`errorMessage` carry JSON quotes | move to `strings.xml`; use `asString` instead of `toString()` |
| BUG-182 | L13 | P3 | Low | No rotation/state handling; dialog and in-flight request are lost | handle config change or move state to a ViewModel |

#### `OTP_VERIFY` — module · `agents/AUTH/OTP_VERIFY.md` · 20 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-183 | V1 | P1 | High | Mode hinges on the magic string `"verify"` spelled identically in 4 files; a typo silently renders Forgot Password with an empty email | a shared constant (PLATFORM/COMMONS) used by all callers |
| BUG-184 | V2 | P1 | High | Post-verification routing depends on the ambient `Util.userId` rather than on an explicit extra, so the destination changes with the caller | pass the intent (`"from"`) explicitly, or re-login deliberately |
| BUG-185 | V5 | P1 | High | After the auto-send, the shared listener sends `email.text` instead of `emailID`; it only works because `onCreate` pre-fills the hidden field | always pass `emailID` in verify mode |
| BUG-186 | V9 | P1 | High | **Every entry — including every rotation — fires a new OTP email**; no cooldown, no rate-limit guard | send only on first create (`savedInstanceState == null`) and add a resend button with a timer |
| BUG-187 | V12 | P1 | High | `onFailure` shows nothing on either call | toast a generic failure message |
| BUG-188 | V13 | P1 | High | Offline -> the method returns silently; no dialog, no toast, and the user never learns no OTP was sent | toast "No internet connection" |
| BUG-189 | V14 | P1 | High | Retrofit calls are never cancelled in `onDestroy` | keep the `Call` and `cancel()` it |
| BUG-190 | V3 | P2 | Medium | No `ScrollView` and no `windowSoftInputMode`; with the keyboard open the buttons can be unreachable | wrap in `ScrollView` / set `adjustResize` |
| BUG-191 | V4 | P2 | Medium | `otp` has no `inputType="number"`, no `maxLength`, no autofill hint (`smsOTPCode`) | add numeric input + length cap |
| BUG-192 | V7 | P2 | Medium | A missing `email` extra yields the literal `"null"`, which is sent to the server | validate the extra and bail out with a message |
| BUG-193 | V8 | P2 | Medium | On auto-send failure the error is written to `email.error`, but `email_op` is `GONE` — **the field error is invisible** | set the error on `otp` (or a shared banner) in verify mode |
| BUG-194 | V10 | P2 | Medium | `Log.e("data", ...)` and `Log.e("email", emailID)` print the email and OTP in clear text | remove the logs |
| BUG-195 | V11 | P2 | Medium | After success the local session is not refreshed (`Util.user.isVerified` stays `"false"`, no token saved) | re-fetch the user, or rely on the Login round-trip |
| BUG-196 | V15 | P2 | Medium | The button silently relabels from "Verify" to lower-case **"confirm"** right after the screen opens, which reads as a glitch | keep the label stable in verify mode |
| BUG-197 | V17 | P2 | Medium | No "resend OTP" affordance and no countdown, even though the auto-send is invisible to the user | add a timed resend (coordinate with FORGOT_PASSWORD — shared layout) |
| BUG-198 | V6 | P3 | Low | Emptiness is checked on the trimmed OTP but the untrimmed value is sent | send `.trim()` |
| BUG-199 | V18 | P3 | Low | `message` / `errorMessage` are toasted with their JSON quotes | use `asString` |
| BUG-200 | V19 | P3 | Low | `status` is never read or logged in either method | log it |
| BUG-201 | V20 | P3 | Low | An unverified user reaching this screen from Login/Splash can leave only via Login — correct, but there is no explanation shown to them | add copy clarifying why they are here |
| BUG-202 | V16 | P3 | Cosmetic | `cancel_btn.visibility = View.VISIBLE` is redundant (already visible in XML) | delete |

#### `REGISTER` — module · `agents/AUTH/REGISTER.md` · 23 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-203 | R2 | P1 | High | Both `activity_register.*` **and** `activity_edit_profile.*` synthetics are wildcard-imported; the two layouts share **14 ids** (`fname`, `lname`, `email`, `mobile`, `gender`, `date`, `male`, `female`, `prod_logo`, `*_op`, `other`), patched with 6 explicit imports. Adding an id to **either** layout can silently rebind a view here | drop the `activity_edit_profile.*` import; longer term migrate this screen to ViewBinding (needs PM sign-off) |
| BUG-204 | R15 | P1 | High | `onFailure` gives no user feedback | toast a generic failure message |
| BUG-205 | R17 | P1 | High | Retrofit call is never cancelled in `onDestroy` | keep the `Call` and `cancel()` it, or guard with `isFinishing` |
| BUG-206 | R1 | P2 | Medium | `android:exported="true"` with no intent-filter — any app can launch the sign-up screen | set `exported="false"` (PLATFORM/BUILD_CONFIG) |
| BUG-207 | R4 | P2 | Medium | `email` field has no `inputType="textEmailAddress"`; no `imeOptions` chain across the form | add `inputType` + `imeOptions="actionNext"` |
| BUG-208 | R5 | P2 | Medium | `signin_btn` and the success path never call `finish()` -> Login/Register stack grows | `finish()` after `startActivity` |
| BUG-209 | R9 | P2 | Medium | **Last Name is never validated** — may be empty or contain digits, and is concatenated into `name` | add empty + `isValidName` checks |
| BUG-210 | R12 | P2 | Medium | DOB travels through three formats: picker writes `d-M-yyyy`, `formatDate` parses `dd-MM-yyyy` and emits `MM-dd-yyyy`. `SimpleDateFormat` is lenient so `1-5-1990` parses, but the chain is fragile and locale-dependent | keep a `Calendar`/epoch value and format once at send time |
| BUG-211 | R14 | P2 | Medium | `Util.user` is populated for a user who is not signed in and has no token | do not set global user state until login succeeds |
| BUG-212 | R16 | P2 | Medium | Offline -> the method returns silently; no dialog, no toast | toast "No internet connection" |
| BUG-213 | R18 | P2 | Medium | The picked date lives only in `date.text` (a `TextView` without `freezesText`) -> lost on rotation | add `android:freezesText="true"` or hold the value in a field |
| BUG-214 | R20 | P2 | Medium | No confirm-password field and no terms checkbox | ask the customer before adding |
| BUG-215 | R21 | P2 | Medium | Password sent in plain JSON; the app globally sets `usesCleartextTraffic="true"` | PLATFORM/BUILD_CONFIG to restrict cleartext |
| BUG-216 | R3 | P3 | Low | Uses deprecated `ProgressDialog`; the rest of AUTH uses `SpotsDialog` | switch to `SpotsDialog` for consistency |
| BUG-217 | R6 | P3 | Low | The date picker always reopens at **1 Jan 1960**; `year` is a `val` and `month`/`day` are never updated from the selection | keep the chosen date in the fields and seed the picker with it |
| BUG-218 | R7 | P3 | Low | `showDialog(999)` / `onCreateDialog(int)` are deprecated | build the `DatePickerDialog` directly in `setDate`, or use `MaterialDatePicker` |
| BUG-219 | R8 | P3 | Low | Empty first name sets the field error to "Enter name" but toasts "Enter first name" | use one string |
| BUG-220 | R10 | P3 | Low | `doValidation()` runs twice on failure (side effects applied twice) | store the result in a local `val` |
| BUG-221 | R11 | P3 | Low | `toLowerCase()` without a locale (deprecated) on the gender label | `lowercase(Locale.ROOT)`, or better: map the radio **id** to a value instead of its label |
| BUG-222 | R13 | P3 | Low | `userPreferences` is instantiated and never used | delete |
| BUG-223 | R19 | P3 | Low | `errorMessage` is toasted with its JSON quotes | use `asString` instead of `toString()` |
| BUG-224 | R23 | P3 | Low | `android:onClick="setDate"` breaks under obfuscation (`minifyEnabled` is currently `false`) | wire the listener in Kotlin, or keep a proguard rule |
| BUG-225 | R22 | P3 | Cosmetic | `tools:context` points at `LoginActivity` | correct to `RegisterActivity` |

#### `RESET_PASSWORD` — module · `agents/AUTH/RESET_PASSWORD.md` · 21 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-226 | P14 | P1 | High | The error branch has its `errorMessage` parsing **commented out**, so an **expired/invalid OTP** shows only "Something went wrong" and the user is never told to restart the forgot flow | restore the parsing and special-case OTP errors |
| BUG-227 | P15 | P1 | High | A successful reset shows **no confirmation at all** — the screen just closes | toast "Password reset successfully" before `finish()` |
| BUG-228 | P18 | P1 | High | `onFailure` shows nothing to the user | toast a generic failure message |
| BUG-229 | P21 | P1 | High | The Retrofit call is not cancelled in `onDestroy` (only after a response) | keep the `Call` and `cancel()` it |
| BUG-230 | P1 | P2 | Medium | Extras are read **without** `.toString()` here but **with** it in `ForgotPasswordActivity`, the screen that launches this one — two opposite null conventions in one flow | standardise (PM-level, spans several agents) |
| BUG-231 | P2 | P2 | Medium | No `ScrollView` and no `windowSoftInputMode` | wrap in `ScrollView` / set `adjustResize` (**shared layout**) |
| BUG-232 | P3 | P2 | Medium | The heading says "Change Password", not "Reset Password"; there is no logo/branding and nothing tells the user which email the reset applies to | set the heading per mode and show the email |
| BUG-233 | P5 | P2 | Medium | `cnfm_pwd` has **no `inputType="textPassword"`** — the confirmation is typed in clear text | add `inputType` (**shared layout**) |
| BUG-234 | P7 | P2 | Medium | The `"Enter Password"` branch is unreachable — an empty password fails `isValidPassword` first | check emptiness before the regex (**shared chain** — coordinate) |
| BUG-235 | P8 | P2 | Medium | The "same as old password" check compares **old vs confirm**, not old vs new | compare `oldPasswordTxt` with `passwordTxt` (**shared chain**) |
| BUG-236 | P9 | P2 | Medium | The "same as old password" check is **inert** in this mode (`old_pwd` is GONE, so the value is `""`), so a user can reset to their current password | enforce server-side, or drop the check explicitly in this branch |
| BUG-237 | P10 | P2 | Medium | Neither `email` nor `otp` is validated; a missing `otp` would send a JSON null | guard the extras and bail out with a message |
| BUG-238 | P19 | P2 | Medium | Offline -> the method returns silently; no dialog, no toast — and the OTP may expire meanwhile | toast "No internet connection" |
| BUG-239 | P20 | P2 | Medium | `onResume` calls `dialog.dismiss()`, killing the spinner of a still-running request | remove the `onResume` override (**shared**) |
| BUG-240 | P23 | P2 | Medium | Passwords are sent in plain JSON while the app sets `usesCleartextTraffic="true"` globally | PLATFORM/BUILD_CONFIG to restrict cleartext |
| BUG-241 | P4 | P3 | Low | `cnfm_pwd_op` has no `passwordToggleEnabled`, unlike `new_pwd_op` | add for consistency (**shared layout**) |
| BUG-242 | P6 | P3 | Low | `otp_op` / `otp` are **dead views** — the OTP is passed as an extra, not typed here | delete (**shared layout** — coordinate with CHANGE_PASSWORD and FORGOT_PASSWORD) |
| BUG-243 | P12 | P3 | Low | `Log.e("ok1", ...)` debug logging left in the success path | remove |
| BUG-244 | P13 | P3 | Low | `UserPreferences` is instantiated by the shared `onCreate` but unused in this mode | harmless; tidy if the shared prologue is refactored |
| BUG-245 | P17 | P3 | Low | After a reset the user must find their own way back to Login and sign in; no auto-login and no guidance | ask the customer |
| BUG-246 | P16 | P3 | Cosmetic | `Gson` is imported but unused (only the commented-out code referenced it) | remove with the P14 fix |

#### `SPLASH` — module · `agents/AUTH/SPLASH.md` · 19 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-247 | S5 | P0 | Critical | **`onFailure` does nothing** — no toast, no navigation. A server timeout leaves the user stuck on the splash forever | toast + fall back to `LoginActivity` |
| BUG-248 | S12 | P0 | Critical | Offline -> `isNetworkAvailable` false -> the method returns silently, same permanent hang **at app launch** | detect offline and route to Login (or an offline screen) with a message |
| BUG-249 | S6 | P1 | High | The empty-`userId` branch starts Login and calls `finish()` but **does not return**, so `Util.userId` is still assigned and the flow continues to `GET api/v1/users/` | add `return@observe` |
| BUG-250 | S11 | P1 | High | A proven-invalid session is **not cleared** (no `deleteAuthToken`/`deleteUserId`), unlike every other 401-ish handler in the app, so the failure repeats on every launch | clear the session before routing to Login |
| BUG-251 | S13 | P1 | High | Four **continuous** LiveData observers; any later write to `token`/`userId`/`isNight`/`textSize` re-fires them and can launch `getMyDetails` again | one-shot reads (`first()`), or guard with a `hasBootstrapped` flag |
| BUG-252 | S20 | P1 | High | `getMyDetails` is **triplicated** across Splash, Login and MainActivity, with three different error behaviours | extract a shared `SessionBootstrap` (PM-level refactor) |
| BUG-253 | S1 | P2 | Medium | No splash theme / `windowBackground`, so a blank window shows before the layout inflates | add a themed `windowBackground` |
| BUG-254 | S2 | P2 | Medium | Hard-coded `#F0F3F9` and `#FFFFFF`; the splash ignores night mode despite a `DayNight` theme | move to `colors.xml` + `values-night` |
| BUG-255 | S4 | P2 | Medium | The API 31+ `addOnDrawListener { false }` block is a **no-op** (mis-port of `addOnPreDrawListener`); `androidx.core:core-splashscreen` is not used | adopt the official SplashScreen API, or delete the dead block |
| BUG-256 | S7 | P2 | Medium | `getMyDetails` depends on a **sibling observer** having already set `Util.userId`; the ordering is incidental, not guaranteed | read `userId` once and pass it explicitly |
| BUG-257 | S8 | P2 | Medium | `isWarrior` treats empty/null as **true** (`isNullOrEmpty() \|\| != "false"`) | compare explicitly with `"true"` (same as LOGIN L5) |
| BUG-258 | S10 | P2 | Medium | The unverified route omits `finish()`, leaving the splash on the back stack | `finish()` after `startActivity` |
| BUG-259 | S14 | P2 | Medium | No progress indicator or text during a ≥ 2 s (often ≥ 4 s) wait | add a spinner, or remove the artificial delay |
| BUG-260 | S15 | P2 | Medium | The Retrofit call is never cancelled; no `onDestroy` override at all | keep the `Call` and `cancel()` it |
| BUG-261 | S21 | P2 | Medium | Rotating during the bootstrap re-runs it from scratch, including a duplicate network call | guard with `savedInstanceState == null` |
| BUG-262 | S3 | P3 | Low | The `ImageView` has no `scaleType` and is stretched to `match_parent` | use `centerInside` / `centerCrop`, or a vector |
| BUG-263 | S17 | P3 | Low | `Log.e("responseee", "fail")` and `Log.e("Splashscreen", ...)` — debug-grade tags that do not follow the `Class.method` convention | use `Log.e("SplashhScreenActivity.getMyDetails", ...)` |
| BUG-264 | S18 | P3 | Cosmetic | `"Somthing Went Wrong"` is misspelled (shared with 5 other files) | fix app-wide (cross-team) |
| BUG-265 | S19 | P3 | Cosmetic | The class name `SplashhScreenActivity` contains a typo (double "h") and is referenced in the manifest | rename only with PM sign-off (touches the manifest) |

### APPSHELL — 65 entries

#### `APPSHELL_LEAD` — team lead (rollups) · `agents/APPSHELL/APPSHELL_LEAD.md` · 10 entries

| Bug | Agent id | Issue | Affects module(s) | Risk |
|---|---|---|---|---|
| BUG-266 | AS-1 | The overflow menu + logout handler is **duplicated in 6+ activities** | MAIN_NAV (+ FEED, PROFILE) | inconsistent behaviour; 6 places to fix |
| BUG-267 | AS-2 | Theme and font changes **restart `MainActivity`** instead of recreating in place | SETTINGS | jarring, loses state |
| BUG-268 | AS-3 | `Util.user.isReviewState.toBoolean()` is unguarded in every menu copy | MAIN_NAV + copies | NPE after process death |
| BUG-269 | AS-4 | `TabAdapter` positions are hard-coded in **two** places (the adapter's `when` and `MainActivity`'s `addTab` order) | MAIN_NAV | easy to desync |
| BUG-270 | AS-5 | `TabAdapter.getItem` ends with `else -> b as Fragment` where `b` is `null` | MAIN_NAV | crash if a tab is added |
| BUG-271 | AS-6 | `MainActivity.onBackPressed` calls **`exitProcess(-1)`** | MAIN_NAV | kills the process, skips lifecycle |
| BUG-272 | AS-7 | `getMyDetails` copies #3 (MAIN_NAV) and #9 (ABOUT) | MAIN_NAV, ABOUT | drift (AUTH A4) |
| BUG-273 | AS-8 | Font size is applied **only** to post content/tags; the rest of the app ignores it | SETTINGS, THEMING | inconsistent accessibility |
| BUG-274 | AS-9 | Account deletion sits behind a plain list dialog with no re-authentication | SETTINGS | **destructive action, weak guard** |
| BUG-275 | AS-10 | `ExpandableView.java` is a custom view whose usage is not visible in `activity_about.xml` | ABOUT | possibly dead code |

#### `ABOUT` — module · `agents/APPSHELL/ABOUT.md` · 11 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-276 | AB5 | P1 | High | The always-true `\|\|` token guard; the logout branch is dead | use `&&` |
| BUG-277 | AB6 | P1 | High | A non-200 shows the user nothing | toast the error |
| BUG-278 | AB8 | P1 | High | The Retrofit call is never cancelled | cancel in `onDestroy` |
| BUG-279 | AB1 | P2 | Medium | `ExpandableView` lives in APPSHELL but its **only** consumer is FEED's `HomeAdapter` | documented; consider moving it next to its user |
| BUG-280 | AB2 | P2 | Medium | The class is named `AboutActivity` but shows user details, not app information | rename, or add real app info |
| BUG-281 | AB3 | P2 | Medium | Shows only 7 of `UserRslt`'s 28 fields; no avatar or warrior status | expand, or confirm intent |
| BUG-282 | AB4 | P2 | Medium | "Edit Profile" in the profile tab opens this **read-only** screen, which then offers another "Edit" | relabel, or route straight to `EditProfileActivity` |
| BUG-283 | AB7 | P2 | Medium | Unlike every other `getMyDetails` copy, this one does **not** refresh `Util.user` | align, or document deliberately |
| BUG-284 | AB9 | P2 | Medium | `getmyDetails` is **copy #9** of the same block | shared helper (PM-level) |
| BUG-285 | AB10 | P2 | Medium | Dates (`dateOfBirth`, `createdAt`) are shown without `Util.formatDate` formatting | format consistently |
| BUG-286 | AB11 | P3 | Low | No app version, licences or credits — what an "About" screen usually has | ask the customer |

#### `MAIN_NAV` — module · `agents/APPSHELL/MAIN_NAV.md` · 16 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-287 | AS-1 | P1 | High | The ~60-line overflow menu is duplicated into 5 other activities from here | extract a shared handler (PM-level) |
| BUG-288 | MN2 | P1 | High | `Util.user.isReviewState.toBoolean()` unguarded — NPE after process death | null-guard |
| BUG-289 | MN4 | P1 | High | `TabAdapter.getItem`'s `else -> b as Fragment` NPEs for any new position | handle all positions explicitly |
| BUG-290 | MN7 | P1 | High | On tab change `Util.player.stop()` is called but the player is **not released or nulled** | release, or delegate to the fragments |
| BUG-291 | MN9 | P1 | High | The rotation `ContentObserver` is **never unregistered** | unregister in `onDestroy` |
| BUG-292 | MN12 | P1 | High | `onBackPressed` calls **`exitProcess(-1)`**, killing the process | `finish()` or `moveTaskToBack(true)` |
| BUG-293 | MN3 | P2 | Medium | Tab order is defined in **two** places that must agree | one source of truth |
| BUG-294 | MN8 | P2 | Medium | The night-mode `when` has no `else` | add a default |
| BUG-295 | MN10 | P2 | Medium | The first-run dialog is shown **only to warriors**; the non-warrior branch is commented out | confirm intent with the customer |
| BUG-296 | MN13 | P2 | Medium | Permissions are requested only here, though ADD_POST and EDIT_PROFILE need them | request at point of use |
| BUG-297 | MN14 | P2 | Medium | Mixed `findViewById` and synthetics in one class | pick one |
| BUG-298 | MN1 | P3 | Low | `dialog.dismiss()` is called twice consecutively | delete one |
| BUG-299 | MN5 | P3 | Low | Inconsistent tab-tag casing | align |
| BUG-300 | MN6 | P3 | Low | Deprecated `getDrawable(...)` | `ContextCompat.getDrawable` |
| BUG-301 | MN11 | P3 | Low | `R.drawable.covre_pic` is misspelled | rename (THEMING) |
| BUG-302 | MN15 | P3 | Low | No `onSaveInstanceState` for the selected tab | save `currentItem` |

#### `SETTINGS` — module · `agents/APPSHELL/SETTINGS.md` · 14 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-303 | ST10 | P1 | High | **Account deletion needs only a Yes tap** — no re-authentication or typed confirmation | require the password, or a typed confirmation |
| BUG-304 | ST11 | P1 | High | Deletion failure shows the user **nothing** | toast the error |
| BUG-305 | ST12 | P1 | High | Deletion clears DataStore but leaves `Util.user` / `Util.userId` populated | clear global state too |
| BUG-306 | ST13 | P1 | High | `Util.user.isReviewState.toBoolean()` unguarded in the menu copy | null-guard |
| BUG-307 | ST14 | P1 | High | The overflow menu is duplicated here too (AS-1) | shared handler (PM-level) |
| BUG-308 | ST2 | P2 | Medium | Options are matched by **display string** rather than index or enum | switch on the index |
| BUG-309 | ST3 | P2 | Medium | Theme and font changes **restart `MainActivity`** | `recreate()` / live `AppCompatDelegate` update |
| BUG-310 | ST4 | P2 | Medium | Font size affects **only** post content and tags | apply app-wide, or rename the setting |
| BUG-311 | ST5 | P2 | Medium | Neither picker shows the current selection | use `setSingleChoiceItems` |
| BUG-312 | ST6 | P2 | Medium | Theme is written here but applied in `MainActivity` — split responsibility | documented; centralise if refactoring |
| BUG-313 | ST1 | P3 | Low | No app version, no about link, no notification or privacy settings | ask the customer |
| BUG-314 | ST7 | P3 | Low | A large commented-out night-mode resolution block | delete |
| BUG-315 | ST8 | P3 | Low | `Log.e("mode", ...)` debug logging | remove |
| BUG-316 | ST9 | P3 | Low | The `DEFAULT` branch writes in the opposite order to the others | align |

#### `THEMING` — module · `agents/APPSHELL/THEMING.md` · 14 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-317 | TH1 | P1 | High | **`black` and `white` are inverted in night mode** — the names lie | rename to `text_primary` / `surface`, app-wide |
| BUG-318 | TH9 | P1 | High | Only 26 strings; nearly all UI text is hard-coded — the app is **not localisable** | extract strings (a very large, cross-team task) |
| BUG-319 | TH2 | P2 | Medium | No error/success/warning colours; screens use `Color.RED` in code | add semantic colours |
| BUG-320 | TH3 | P2 | Medium | No ActionBar, so all 21 screens hand-roll a header | a shared header layout/`include` |
| BUG-321 | TH4 | P2 | Medium | `windowIsTranslucent=true` app-wide | verify it is needed |
| BUG-322 | TH5 | P2 | Medium | `colorPrimaryDark` is `@color/black`, i.e. white at night | use an explicit colour |
| BUG-323 | TH6 | P2 | Medium | `menuStyle` forces LTR despite `supportsRtl="true"` | remove, or justify |
| BUG-324 | TH12 | P2 | Medium | Sizes are hard-coded in layouts rather than in `dimens.xml` | centralise |
| BUG-325 | TH13 | P2 | Medium | Text sizes declared in **`dp`** rather than `sp` in several layouts | switch to `sp` |
| BUG-326 | TH7 | P3 | Low | `styles.xml` and `themes.xml` split two styles across two files | merge |
| BUG-327 | TH8 | P3 | Low | Both `app_name` and `Product_name` hold "SalvationLamb" | keep one |
| BUG-328 | TH10 | P3 | Low | `values-night/dimens.xml` exists though dimensions do not vary by theme | delete |
| BUG-329 | TH11 | P3 | Low | Tablet breakpoints exist with no tablet layouts | implement or remove |
| BUG-330 | TH14 | P3 | Low | `covre_pic` is misspelled | rename (touches MAIN_NAV) |

### FEED — 104 entries

#### `FEED_LEAD` — team lead (rollups) · `agents/FEED/FEED_LEAD.md` · 13 entries

| Bug | Agent id | Issue | Affects module(s) | Risk |
|---|---|---|---|---|
| BUG-331 | F-1 | `page`/`type` are magic strings with no constants, compared via `contentEquals` | HOME_FEED | silent mis-render |
| BUG-332 | F-2 | List endpoints use `results` + `count`, unlike AUTH's `result` | all | empty lists, no error |
| BUG-333 | F-3 | `HomeAdapter` (680 lines) holds **all** post actions — like, fav, follow, delete, share, playback — instead of the screen | HOME_FEED | untestable, cross-team coupling |
| BUG-334 | F-4 | The always-true `!isEmpty \|\| !equals("null") \|\| !isNullOrEmpty` token guard appears **5 times** in `HomeFragment` alone | HOME_FEED | dead logout branches |
| BUG-335 | F-5 | A new `authToken` observer is registered per call, in 5 methods | HOME_FEED | duplicate requests |
| BUG-336 | F-6 | `dialog.hide()` is used instead of `dismiss()` in FEED callbacks | HOME_FEED | leaked window on config change |
| BUG-337 | F-7 | Error branches are **commented out**; failures just show the "no data" view | HOME_FEED | indistinguishable empty vs failed |
| BUG-338 | F-8 | Pull-to-refresh calls `detach().attach()` on the fragment — a full recreate | HOME_FEED | jank, duplicate calls |
| BUG-339 | F-9 | A new `OnScrollListener` is added on **every** page load, never removed | HOME_FEED | listeners accumulate |
| BUG-340 | F-10 | `Util.player` is a static `MediaPlayer` shared by every card | HOME_FEED, MEDIA | leaks, overlapping audio |
| BUG-341 | F-11 | 14 reaction strings are addressed by `R.id.reactN` -> `R.string.reactN` pairs with no data structure | HOME_FEED | fragile |
| BUG-342 | F-12 | `Posts.likesCount` / `shareCount` are `String` | all | conversion crashes |
| BUG-343 | F-13 | `getMyDetails` duplicated here as well — the **4th** copy | HOME_FEED | drift (AUTH A4) |

#### `ADD_POST` — module · `agents/FEED/ADD_POST.md` · 24 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-344 | AP4 | P1 | High | Permissions are **checked but never requested** — the user is bounced to settings | call `requestPermissions` first |
| BUG-345 | AP5 | P1 | High | `MediaStore.Images.Media.getBitmap` (deprecated) decodes on the **main thread** | `ImageDecoder` off the main thread |
| BUG-346 | AP6 | P1 | High | The camera path stores only the **thumbnail** from `extras["data"]` | use a `FileProvider` and the full-size file |
| BUG-347 | AP11 | P1 | High | Base64 is sent inside JSON (`Base64.DEFAULT` adds newlines); no size limit | multipart upload (with NETWORK) |
| BUG-348 | AP14 | P1 | High | The always-true token guard; the logout branch is dead | use `&&` |
| BUG-349 | AP15 | P1 | High | `Log.e("data", data.toString())` prints the **entire base64 image** to logcat | remove the log |
| BUG-350 | AP18 | P1 | High | A non-200 shows **no message**; error parsing is commented out | restore and toast |
| BUG-351 | AP22 | P1 | High | Rotation loses the picked image and the post type | save to `onSaveInstanceState` |
| BUG-352 | AP1 | P2 | Medium | No warrior check on the screen itself — it is reachable by intent | verify `Util.isWarrior` in `onCreate` |
| BUG-353 | AP2 | P2 | Medium | Image/Video visibility handling is asymmetric | handle both in the listener |
| BUG-354 | AP3 | P2 | Medium | `image_btn` has two listeners, so selecting the radio immediately opens the picker | pick one trigger |
| BUG-355 | AP12 | P2 | Medium | A video post is accepted with an **empty or invalid URL** | validate the URL |
| BUG-356 | AP13 | P2 | Medium | `post_btn` is not re-enabled in `onFailure` | re-enable |
| BUG-357 | AP17 | P2 | Medium | Success clears only `title` and `content`, leaving tags/url/image | reset everything |
| BUG-358 | AP19 | P2 | Medium | A stray `dialog.dismiss()` outside the observer dismisses the spinner early | remove it |
| BUG-359 | AP20 | P2 | Medium | `resultCode` is ignored in `onActivityResult` | check `RESULT_OK` |
| BUG-360 | AP21 | P2 | Medium | `onResume` dismisses the dialog of an in-flight request | remove the override |
| BUG-361 | AP7 | P3 | Low | `data.extras!!["data"]` double force-unwrap | null-safe |
| BUG-362 | AP8 | P3 | Low | Dead compression in the camera path (quality 90, result discarded) | delete |
| BUG-363 | AP9 | P3 | Low | Permission dialog buttons are inverted ("cancel" positive, "settings" negative) | swap |
| BUG-364 | AP10 | P3 | Low | Permission copy mentions *"profile picture"* on the post screen | reword |
| BUG-365 | AP16 | P3 | Low | Body key `image` vs model field `picture` | document only |
| BUG-366 | AP23 | P3 | Low | Deprecated `startActivityForResult` / `onActivityResult` | Activity Result API |
| BUG-367 | AP24 | P3 | Low | Returns to `MainActivity` instead of the feed with a result | `setResult` + `finish()` |

#### `FAVORITES` — module · `agents/FEED/FAVORITES.md` · 11 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-368 | FV2 | P1 | High | `menu` is resolved via a **synthetic import of another layout** | use `findViewById` |
| BUG-369 | FV7 | P1 | High | `Util.user.isReviewState.toBoolean()` unguarded — NPE after process death | null-guard `Util.user` |
| BUG-370 | FV10 | P1 | High | The fragment is `replace()`d with no `savedInstanceState` guard — every rotation rebuilds it and re-fetches | guard the transaction |
| BUG-371 | FV11 | P1 | High | The ~60-line menu block is duplicated here too (team issue VL3) | shared handler (PM-level) |
| BUG-372 | FV4 | P2 | Medium | No screen title or heading — the user cannot tell they are in Favorites | add a title |
| BUG-373 | FV5 | P2 | Medium | `logout` is hidden but its full handler remains as dead code | remove the handler, or show the item |
| BUG-374 | FV6 | P2 | Medium | Hiding `logout` here but not on sibling screens is undocumented and inconsistent | decide a rule with APPSHELL |
| BUG-375 | FV3 | P3 | Low | The logo navigates to `MainActivity` without `finish()` | `finish()` |
| BUG-376 | FV8 | P3 | Low | `UserPreferences` is instantiated but effectively unused | delete with FV5 |
| BUG-377 | FV9 | P3 | Low | A `SpotsDialog` is built and never used | delete |
| BUG-378 | FV1 | P3 | Cosmetic | The fragment variable is named `viewProfile` | rename |

#### `HOME_FEED` — module · `agents/FEED/HOME_FEED.md` · 19 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-379 | H5 | P1 | High | Audio uses the static `Util.player` plus a polling `Runnable` not tied to the card lifecycle | per-holder player, release on recycle |
| BUG-380 | H7 | P1 | High | 5 `authToken` observers, one per method, re-firing on every DataStore write | one-shot read |
| BUG-381 | H8 | P1 | High | The always-true token guard appears 5x; the logout branch is dead | use `&&` |
| BUG-382 | H9 | P1 | High | Error parsing is commented out — a failed load is indistinguishable from an empty feed | restore it and show a distinct error state |
| BUG-383 | H12 | P1 | High | A new `OnScrollListener` is added on **every** page load, never removed | add it once in `onCreateView` |
| BUG-384 | H14 | P1 | High | The adapter does **network I/O** and owns a `UserPreferences` + dialog; 680 lines mixing view binding, playback and 5 API calls | move calls to the fragment/ViewModel |
| BUG-385 | H17 | P1 | High | Retrofit calls are never cancelled; callbacks touch views after detach | cancel in `onDestroyView` |
| BUG-386 | H3 | P2 | Medium | No `else` in the `page` visibility chain | add a default |
| BUG-387 | H4 | P2 | Medium | No `else` in the `post.type` `when` | log/handle unknown types |
| BUG-388 | H6 | P2 | Medium | Adding a reaction requires editing 3 places; the handler is a 14-branch `when` | drive it from a list |
| BUG-389 | H10 | P2 | Medium | `dialog.hide()` instead of `dismiss()` | use `dismiss()` |
| BUG-390 | H11 | P2 | Medium | `page` starts at 1 but the empty check tests `page == 0`, so a genuinely empty first page never shows `no_data` | align the values |
| BUG-391 | H15 | P2 | Medium | The three state maps are passed **empty** to the adapter and filled asynchronously | pass immutable data after load |
| BUG-392 | H16 | P2 | Medium | Pull-to-refresh does `detach().attach()` via the deprecated `requireFragmentManager()` | re-fetch the data instead |
| BUG-393 | H18 | P2 | Medium | `type` is read with `arguments?.get("type").toString()`, so a missing arg becomes the string `"null"` | same trap as AUTH A13 |
| BUG-394 | H19 | P2 | Medium | `getMyDetails` here is the **4th** copy of the same block | shared helper (PM-level) |
| BUG-395 | H1 | P3 | Low | `HomeFragment` imports `kotlinx.android.synthetic.main.activity_main.*` — the **wrong layout**, unused | delete the import |
| BUG-396 | H13 | P3 | Low | `Integer.parseInt(get("count").toString())` is fragile | use `asInt` |
| BUG-397 | H2 | P3 | Cosmetic | `Delete_btn` is capitalised, unlike every other id | rename |

#### `IMAGE_DETAIL` — module · `agents/FEED/IMAGE_DETAIL.md` · 11 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-398 | ID4 | P1 | High | A null/invalid URL shows a **blank screen** with no message and no way to tell it failed | add Picasso `placeholder()` / `error()` and a fallback |
| BUG-399 | ID5 | P1 | High | **Zoom without pan** — once magnified, the user cannot move around the image, making zoom nearly useless | add translation on drag, or use `PhotoView` |
| BUG-400 | ID3 | P2 | Medium | No close button, no toolbar, no loading indicator — Back is the only exit | add a close affordance |
| BUG-401 | ID6 | P2 | Medium | No double-tap to zoom/reset | add a `GestureDetector` |
| BUG-402 | ID7 | P2 | Medium | Rotation resets the zoom level | save `mScaleFactor` in `onSaveInstanceState` |
| BUG-403 | ID10 | P2 | Medium | No downsampling — a large image is decoded at full size | `fit().centerInside()` or explicit sizing |
| BUG-404 | ID1 | P3 | Low | `preview_image.xml` is widely assumed to belong here but is actually used by `MainActivity` | documented; ownership stays with APPSHELL |
| BUG-405 | ID2 | P3 | Low | The extra is called **`profilePic`** even when carrying a post image | rename to `imageUrl` — **two-team change** (HOME_FEED + PROFILE) |
| BUG-406 | ID8 | P3 | Low | `Picasso.with(...)` is deprecated | upgrade with BUILD_CONFIG |
| BUG-407 | ID9 | P3 | Low | `imageView!!` / `scaleGestureDetector!!` force-unwraps instead of `lateinit` | use `lateinit var` |
| BUG-408 | ID11 | P3 | Low | No immersive/full-screen flags, so system bars overlay the image | consider immersive mode |

#### `VIEW_LIKES` — module · `agents/FEED/VIEW_LIKES.md` · 14 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-409 | VL1 | P1 | High | Imports **`activity_edit_profile.*`** synthetics to resolve `menu`, coupling this screen to another layout | bind `menu` with `findViewById` |
| BUG-410 | VL3 | P1 | High | The ~60-line overflow menu is **copy-pasted across 6+ screens**, each owning its own logout implementation | extract a shared menu handler (PM-level, cross-team) |
| BUG-411 | VL4 | P1 | High | `Util.user.isReviewState.toBoolean()` with no null check — NPE after process death | null-guard `Util.user` |
| BUG-412 | VL6 | P1 | High | A non-200 has **no `else` branch** — no data, no empty state, no message; the screen just stays blank | add an error state |
| BUG-413 | VL5 | P2 | Medium | A brand-new adapter and layout manager are created on every load instead of updating the existing one | reuse + `notifyDataSetChanged` |
| BUG-414 | VL7 | P2 | Medium | `getStringExtra("postId").toString()` yields `"null"` for a missing extra | validate the extra |
| BUG-415 | VL8 | P2 | Medium | `react_txt` shows the **raw** reaction string with no icon or grouping | map to an icon/label |
| BUG-416 | VL9 | P2 | Medium | The Retrofit call is never cancelled in `onDestroy` | cancel |
| BUG-417 | VL10 | P3 | Low | `ViewLikesAdapter` uses a **secondary constructor** with `lateinit` fields instead of primary-constructor params | use a primary constructor |
| BUG-418 | VL11 | P3 | Low | `Picasso.with(context)` — deprecated API (Picasso 2.5.2) | upgrade |
| BUG-419 | VL12 | P3 | Low | `logo` navigates to `MainActivity` without `finish()` | `finish()` |
| BUG-420 | VL13 | P3 | Low | No pagination — all reactions load at once | paginate if lists grow |
| BUG-421 | VL14 | P3 | Cosmetic | Commented-out night-mode code left in the menu handler | delete |
| BUG-422 | VL15 | P3 | Cosmetic | Method named `getALlLikes` (capital L) | rename |

#### `VIEW_POST` — module · `agents/FEED/VIEW_POST.md` · 12 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-423 | VP0 | P0 | Critical | **All 14 `findViewById` calls run in the constructor, before `setContentView`** — guaranteed NPE on launch | move them into `onCreate` after `setContentView` |
| BUG-424 | VP1 | P1 | High | Only 4 of 14 views are populated; like, share, fav and react have **no listeners** | implement, or remove the controls |
| BUG-425 | VP5 | P1 | High | `onFailure` never dismisses the non-cancelable dialog — permanent spinner | dismiss in both callbacks |
| BUG-426 | VP8 | P1 | High | **No caller anywhere** — the screen is unreachable dead code | wire it up from `HomeAdapter`, or delete it |
| BUG-427 | VP2 | P2 | Medium | The layout duplicates `child_post.xml` ids without the behaviour | reuse the card, or delete |
| BUG-428 | VP4 | P2 | Medium | Error logging uses the tag `"fail fav"` and prints `errorBody().toString()` (an object reference) | use `.string()` and a correct tag |
| BUG-429 | VP6 | P2 | Medium | The (correct) `&&` guard has **no `else`**, so a missing token silently does nothing | add the standard logout branch |
| BUG-430 | VP7 | P2 | Medium | `intent.extras!!.get("postId").toString()` — crashes with no extras, yields `"null"` with a missing key | `intent.getStringExtra("postId")` + validation |
| BUG-431 | VP11 | P2 | Medium | No image/audio/video handling at all, unlike the feed card | port `when (post.type)` |
| BUG-432 | VP3 | P3 | Low | The logo navigates to `MainActivity` without `finish()` | `finish()` |
| BUG-433 | VP9 | P3 | Low | `tags` is shown raw, not via `HomeAdapter.getTags()`'s `#tag` formatting | reuse the helper |
| BUG-434 | VP10 | P3 | Low | `"${post.likesCount} people reacts"` — grammar, and no singular form | pluralise |

### PROFILE — 84 entries

#### `PROFILE_LEAD` — team lead (rollups) · `agents/PROFILE/PROFILE_LEAD.md` · 15 entries

| Bug | Agent id | Issue | Affects module(s) | Risk |
|---|---|---|---|---|
| BUG-435 | F-1 | `ProfileFragment` serves two screens via `who`, with the adapter page derived as `if (who == "me") "profile" else "OtherProfile"` — no constants | MY_PROFILE, VIEW_PROFILE | silent mis-render |
| BUG-436 | F-2 | The in-profile "Edit Profile" button opens **`AboutActivity`**, not `EditProfileActivity` | MY_PROFILE | confusing navigation |
| BUG-437 | F-3 | `ProfileFragment.setUserVisibleHint` calls `detach().attach()` on **every** tab selection — a full recreate plus re-fetch, using deprecated APIs | MY_PROFILE | jank, duplicate calls |
| BUG-438 | F-4 | `EditProfileActivity` is **738 lines** mixing a 14-field form, 3 cascading spinners, crop/camera and 4 API calls | EDIT_PROFILE | unmaintainable |
| BUG-439 | F-5 | `MY_PROFILE` passes **two empty `HashMap()`s** for follow/fav state, so those states never render in profiles | MY_PROFILE | wrong UI state |
| BUG-440 | F-6 | The always-true `\|\|` token guard appears in every PROFILE network method | all | dead logout branches |
| BUG-441 | F-7 | `Util.user.isReviewState.toBoolean()` is unguarded in the duplicated overflow menus | FOLLOWERS, EDIT_PROFILE | NPE after process death |
| BUG-442 | F-8 | `getmyDetails` here is the 5th/6th copy of the same block | MY_PROFILE, EDIT_PROFILE | drift |
| BUG-443 | F-9 | `intent.extras!!.get(...)` and `arguments?.get(...).toString()` yield the literal `"null"` for missing values | all | malformed requests |
| BUG-444 | F-10 | The overflow-menu block is duplicated here too (FEED VL3) | FOLLOWERS, EDIT_PROFILE, VIEW_PROFILE | 6+ copies app-wide |
| BUG-445 | F-11 | **Follow state is unreliable app-wide**: `MY_PROFILE` passes empty maps to `HomeAdapter` (MP4) while `FollowAdapter` consults its map with the **wrong key** (FL7), so every row shows the same follow label | MY_PROFILE, FOLLOWERS + **FEED** | one PM-coordinated fix |
| BUG-446 | F-12 | `EditProfileActivity` **disables StrictMode's VM policy** to pass a `file://` URI to the cropper | EDIT_PROFILE | security / correctness |
| BUG-447 | F-13 | `activity_edit_profile.xml` shares **14 ids** with `activity_register.xml`, coupling PROFILE to AUTH's synthetic imports | EDIT_PROFILE | cross-team breakage |
| BUG-448 | F-14 | `FollowerActivity` starts loading **before** binding its views, risking `UninitializedPropertyAccessException` | FOLLOWERS | crash |
| BUG-449 | F-15 | `ViewProfileActivity` force-unwraps `userId!!`, so launching it without the extra crashes | VIEW_PROFILE | crash |

#### `EDIT_PROFILE` — module · `agents/PROFILE/EDIT_PROFILE.md` · 23 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-450 | EP2 | P1 | High | `onCreate` **disables StrictMode's VM policy** to allow a `file://` URI | use a `FileProvider` and remove the override |
| BUG-451 | EP4 | P1 | High | State and city listeners compare against **`"Country"`**, so the `"State"`/`"City"` placeholders can be submitted as real values | compare against the right placeholder |
| BUG-452 | EP12 | P1 | High | The camera path captures only a **thumbnail** | full-size via `FileProvider` |
| BUG-453 | EP13 | P1 | High | Base64 avatar inside JSON at quality 100, no size limit | multipart (with NETWORK) |
| BUG-454 | EP17 | P1 | High | The always-true `\|\|` token guard in every authenticated method; logout branches dead | use `&&` |
| BUG-455 | EP19 | P1 | High | `getCountries()` and `getMyDetails()` race; the state spinner can be filled before its country list loads | chain the calls |
| BUG-456 | EP20 | P1 | High | Rotation loses the picked image and spinner state | `onSaveInstanceState` |
| BUG-457 | EP21 | P1 | High | Retrofit calls are never cancelled | cancel in `onDestroy` |
| BUG-458 | EP23 | P1 | High | The ~60-line overflow menu is duplicated here too | shared handler (PM-level) |
| BUG-459 | EP1 | P2 | Medium | `findViewById` **and** synthetics mixed in one class | pick one |
| BUG-460 | EP5 | P2 | Medium | Country/state **names** are passed where ids are expected | confirm with the backend |
| BUG-461 | EP6 | P2 | Medium | Changing the country does not reset state/city | clear dependents |
| BUG-462 | EP7 | P2 | Medium | `lname`, `address`, `pincode`, `language`, geo and gender are **never validated** | extend `doValidation` |
| BUG-463 | EP9 | P2 | Medium | DOB round-trips through two format conversions for change detection | compare normalised values |
| BUG-464 | EP11 | P2 | Medium | Gender comparison ignores **`other`**, which exists in this layout | handle all three |
| BUG-465 | EP15 | P2 | Medium | `===` referential comparison on boxed `Int` request codes | use `==` |
| BUG-466 | EP16 | P2 | Medium | The "admin approval" confirmation dialog is commented out | confirm intent with the customer |
| BUG-467 | EP18 | P2 | Medium | A new `authToken` observer per call | one-shot read |
| BUG-468 | EP22 | P2 | Medium | `exported="true"` with no intent-filter | BUILD_CONFIG |
| BUG-469 | EP3 | P3 | Low | Gender `other` is present here but commented out in Register | align the two screens |
| BUG-470 | EP8 | P3 | Low | Returns `"SUCCESS"` while Register returns `"success"` | share a constant |
| BUG-471 | EP10 | P3 | Low | 10 `text!!` force-unwraps in `isDataChanged` | null-safe |
| BUG-472 | EP14 | P3 | Low | Request/result codes used as **log tags** | fix the tags |

#### `FOLLOWERS` — module · `agents/PROFILE/FOLLOWERS.md` · 20 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-473 | FL2 | P1 | High | Two bare `if`s with no `else`/fallback — an unknown `page` loads nothing, silently | `when` with an `else` |
| BUG-474 | FL3 | P1 | High | `menu` resolved via a **synthetic import of another layout** | use `findViewById` |
| BUG-475 | FL6 | P1 | High | Two consecutive contradictory blocks set the label; the first is **dead code** | delete the first, fix the second |
| BUG-476 | FL7 | P1 | High | The follow button checks **`Util.userId`** instead of the row's `follow.id`, so **every row shows the same label** | key the map lookup on `follow.id` |
| BUG-477 | FL8 | P1 | High | The surviving check is **inverted** relative to the map's meaning | invert |
| BUG-478 | FL11 | P1 | High | The always-true `\|\|` guard in both list methods | use `&&` (the adapter already does) |
| BUG-479 | FL13 | P1 | High | `Util.user.isReviewState.toBoolean()` unguarded — NPE after process death | null-guard |
| BUG-480 | FL14 | P1 | High | List loading starts **before** `findViewById` binds `lists`/`nodata` — `UninitializedPropertyAccessException` on a fast response | bind views first |
| BUG-481 | FL16 | P1 | High | The ~60-line overflow menu is duplicated here too | shared handler (PM-level) |
| BUG-482 | FL1 | P2 | Medium | `getAllFollowers` / `getAllFollowing` are near-identical copies | one method + a path parameter |
| BUG-483 | FL5 | P2 | Medium | **No title** — followers and following look identical | add a per-mode title |
| BUG-484 | FL9 | P2 | Medium | Both button branches call the identical `follow(...)`, so the `if/else` is pointless | collapse it |
| BUG-485 | FL10 | P2 | Medium | `follow()` logs the error properly but still shows the user nothing | toast on failure |
| BUG-486 | FL12 | P2 | Medium | The activity's error branches are commented out — a failure looks like an empty list | restore |
| BUG-487 | FL15 | P2 | Medium | `intent.extras!!.get(...).toString()` crashes / yields `"null"` | validate the extras |
| BUG-488 | FL17 | P2 | Medium | A new adapter is created on every load rather than updating the existing one | reuse + notify |
| BUG-489 | FL20 | P2 | Medium | Button labels are the lower-case literals `"follow"`/`"unfollow"`, compared by **text** rather than state | track state, not labels |
| BUG-490 | FL4 | P3 | Low | `night_mode` and `day_mode` exist in the layout but are **never referenced** | remove, or wire to THEMING |
| BUG-491 | FL18 | P3 | Low | No pagination — the whole list loads at once | paginate if needed |
| BUG-492 | FL19 | P3 | Low | `Picasso.with(context)` deprecated | upgrade |

#### `MY_PROFILE` — module · `agents/PROFILE/MY_PROFILE.md` · 16 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-493 | MP4 | P1 | High | Two **empty `HashMap()`s** are passed for follow and fav state, so those states never render | build and pass the real maps |
| BUG-494 | MP6 | P1 | High | 5 `authToken` observers, one per method | one-shot read |
| BUG-495 | MP7 | P1 | High | The always-true `\|\|` token guard; all 5 logout branches are dead | use `&&` |
| BUG-496 | MP8 | P1 | High | Error parsing commented out — a failure looks like an empty profile | restore and show an error |
| BUG-497 | MP10 | P1 | High | `getallLikes` runs **before** the layout is inflated | move it after `inflate` |
| BUG-498 | MP11 | P1 | High | `setUserVisibleHint` does `detach().attach()` on **every** tab visit — full recreate + 5 re-fetches, via a deprecated API | load once; use `setMaxLifecycle` |
| BUG-499 | MP13 | P1 | High | Retrofit calls are never cancelled; callbacks touch views after detach | cancel in `onDestroyView` |
| BUG-500 | MP1 | P2 | Medium | `profile_about` exists in the layout but is **never populated** | bind it or remove it |
| BUG-501 | MP2 | P2 | Medium | "Edit Profile" opens **`AboutActivity`**, not `EditProfileActivity` | route directly, or rename the button |
| BUG-502 | MP5 | P2 | Medium | The adapter argument order differs from `HomeFragment`'s call | named arguments |
| BUG-503 | MP9 | P2 | Medium | `dialog.hide()` instead of `dismiss()` | use `dismiss()` |
| BUG-504 | MP12 | P2 | Medium | `arguments?.get(...).toString()` yields `"null"` when missing | `requireArguments().getString(...)` |
| BUG-505 | MP15 | P2 | Medium | `page` starts at 0 here but at 1 in `HomeFragment` — inconsistent paging | align with HOME_FEED |
| BUG-506 | MP3 | P3 | Low | `role == "admin"` is a magic string; the suffix is concatenated, not localised | constants + string resources |
| BUG-507 | MP14 | P3 | Low | `posts_linear` has no listener while the other two counts do | add or remove |
| BUG-508 | MP16 | P3 | Low | No pull-to-refresh, unlike the home feed | add if wanted |

#### `VIEW_PROFILE` — module · `agents/PROFILE/VIEW_PROFILE.md` · 10 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-509 | VPR1 | P1 | High | `menu` resolved via a **synthetic import of another layout** | use `findViewById` |
| BUG-510 | VPR5 | P1 | High | `Util.user.isReviewState.toBoolean()` unguarded — NPE after process death | null-guard |
| BUG-511 | VPR7 | P1 | High | `userId!!` force-unwrap — launching without the extra **crashes immediately** | validate and finish gracefully |
| BUG-512 | VPR8 | P1 | High | The fragment is `replace()`d with no `savedInstanceState` guard — every rotation re-runs 5 network calls | guard the transaction |
| BUG-513 | VPR9 | P1 | High | The ~60-line overflow menu is duplicated here too | shared handler (PM-level) |
| BUG-514 | VPR3 | P2 | Medium | No title or subject indication before the fragment loads | show the name in the header |
| BUG-515 | VPR4 | P2 | Medium | "Edit Profile" is offered while viewing someone else's profile, and edits your own | hide it here, or relabel |
| BUG-516 | VPR10 | P2 | Medium | No "follow" action at the screen level — following is only possible from a post card | ask the customer |
| BUG-517 | VPR2 | P3 | Low | The logo navigates to `MainActivity` without `finish()` | `finish()` |
| BUG-518 | VPR6 | P3 | Low | A `SpotsDialog` is built and never shown | delete |

### MEDIA — 92 entries

#### `MEDIA_LEAD` — team lead (rollups) · `agents/MEDIA/MEDIA_LEAD.md` · 10 entries

| Bug | Agent id | Issue | Affects module(s) | Risk |
|---|---|---|---|---|
| BUG-519 | M-1 | `AdminAudioFragment` and `AdminVideoFragment` are **~390-line near-duplicates** differing only by endpoint and log tags | ADMIN_AUDIO, ADMIN_VIDEO | every fix must be made twice |
| BUG-520 | M-2 | `FileAdapter` treats **anything that is not `"folder"` as a PDF** — images, audio and video all open in the PDF viewer | FILE_LIST, FILES_BROWSER | broken files UX |
| BUG-521 | M-3 | `PdfActivity2` downloads over a raw `HttpURLConnection` inside a deprecated `AsyncTask`, writing to a hard-coded `"testthreepdf"` folder on external storage | PDF_VIEWER | leaks, permission failures, litter |
| BUG-522 | M-4 | `WebViewActivity` enables **JavaScript and DOM storage** (the code comment even notes the XSS risk) | WEBVIEW | security |
| BUG-523 | M-5 | `Util.player` is a static `MediaPlayer` shared by FEED and both admin feeds | ADMIN_AUDIO, ADMIN_VIDEO | overlapping audio, leaks |
| BUG-524 | M-6 | `FilesFragment` and `FileListActivity` duplicate `getFilesAndFolder` almost exactly | FILES_BROWSER, FILE_LIST | drift |
| BUG-525 | M-7 | The always-true `\|\|` token guard appears in every MEDIA network method | all | dead logout branches |
| BUG-526 | M-8 | `getMyDetails` copies 7 and 8 live here | ADMIN_AUDIO, ADMIN_VIDEO | drift (AUTH A4) |
| BUG-527 | M-9 | `PdfActivity2` extends **`Activity`**, not `AppCompatActivity`, and is the only screen setting `FLAG_SECURE` | PDF_VIEWER | inconsistent theming/behaviour |
| BUG-528 | M-10 | Admin feeds pass page `"home"`, so curated content shows follow/fav buttons for admin authors | ADMIN_AUDIO, ADMIN_VIDEO | questionable UX — confirm with customer |

#### `ADMIN_AUDIO` — module · `agents/MEDIA/ADMIN_AUDIO.md` · 13 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-529 | AA2 | P1 | High | 5 `authToken` observers, one per method | one-shot read |
| BUG-530 | AA3 | P1 | High | The always-true `\|\|` token guard; all 5 logout branches dead | use `&&` |
| BUG-531 | AA4 | P1 | High | Error parsing commented out — a failure looks like an empty feed | restore and show an error |
| BUG-532 | AA6 | P1 | High | A new `OnScrollListener` is added on **every** page load | add it once |
| BUG-533 | AA7 | P1 | High | Relies on `onPause` to release the shared static `Util.player`, which the pager may not call when expected | per-card player, or explicit tab-change handling |
| BUG-534 | AA8 | P1 | High | Retrofit calls are never cancelled | cancel in `onDestroyView` |
| BUG-535 | M-1 | P1 | High | This file is a **~390-line near-duplicate** of `AdminVideoFragment` | extract a shared base fragment taking the endpoint (PM-level, two agents) |
| BUG-536 | AA5 | P2 | Medium | `dialog.hide()` instead of `dismiss()` | use `dismiss()` |
| BUG-537 | M-8 | P2 | Medium | `getMyDetails` copy #7 | shared helper (PM-level) |
| BUG-538 | M-10 | P2 | Medium | Page `"home"` shows follow/fav buttons on curated admin content | confirm the intended UX, then pass a new page value (needs FEED) |
| BUG-539 | AA1 | P3 | Low | An unused `getInstance()` companion (the video twin has none) | use it or delete it |
| BUG-540 | AA9 | P3 | Low | No pull-to-refresh, unlike the home feed | add if wanted |
| BUG-541 | AA10 | P3 | Low | No screen title — the tab is identified only by its nav icon | confirm with APPSHELL |

#### `ADMIN_VIDEO` — module · `agents/MEDIA/ADMIN_VIDEO.md` · 14 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-542 | AV4 | P1 | High | 5 `authToken` observers, one per method | one-shot read |
| BUG-543 | AV5 | P1 | High | A new `OnScrollListener` per page load | add it once |
| BUG-544 | AV6 | P1 | High | The always-true `\|\|` token guard; all 5 logout branches dead | use `&&` |
| BUG-545 | AV7 | P1 | High | Error parsing commented out — a failure looks like an empty feed | restore and show an error |
| BUG-546 | AV9 | P1 | High | The YouTube player is never released on tab change — playback/WebView can outlive the screen | release in `onPause` (needs FEED, since the view lives in the adapter) |
| BUG-547 | AV10 | P1 | High | Retrofit calls are never cancelled | cancel in `onDestroyView` |
| BUG-548 | M-1 | P1 | High | A **~390-line near-duplicate** of `AdminAudioFragment` | shared base fragment (PM-level, two agents) |
| BUG-549 | AV2 | P2 | Medium | `Util.getVideo` hard-codes a **second host** independent of the API base URL | move to NETWORK config (PLATFORM) |
| BUG-550 | AV8 | P2 | Medium | `dialog.hide()` instead of `dismiss()` | use `dismiss()` |
| BUG-551 | M-8 | P2 | Medium | `getMyDetails` copy #8 | shared helper (PM-level) |
| BUG-552 | M-10 | P2 | Medium | Page `"home"` shows follow/fav buttons on curated admin content | new page value (needs FEED) |
| BUG-553 | AV1 | P3 | Low | No `getInstance()` companion, unlike its twin | align the two |
| BUG-554 | AV3 | P3 | Low | Inherited `Util.player` teardown that this tab never needs | leave it; note it when refactoring |
| BUG-555 | AV11 | P3 | Low | No pull-to-refresh and no screen title | add if wanted |

#### `FILES_BROWSER` — module · `agents/MEDIA/FILES_BROWSER.md` · 12 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-556 | FB2 | P1 | High | The list/grid toggle does `detach().attach()`, **re-fetching the entire folder** just to swap the layout manager | swap `layoutManager` in place and call `notifyDataSetChanged` |
| BUG-557 | FB7 | P1 | High | The always-true `\|\|` token guard; the logout branch is dead | use `&&` |
| BUG-558 | FB10 | P1 | High | A non-200 has **no `else` branch** — no error, no empty state, stale content stays | add an error state |
| BUG-559 | FB11 | P1 | High | The Retrofit call is never cancelled; the callback touches views after detach | cancel in `onDestroyView` |
| BUG-560 | FB3 | P2 | Medium | Deprecated `requireFragmentManager()` | `parentFragmentManager` |
| BUG-561 | FB5 | P2 | Medium | `Util.listview` is a global that is never persisted | move to DataStore (STORAGE) |
| BUG-562 | FB6 | P2 | Medium | The root folder is requested with an **empty string**, producing `api/v1/files/` | use an explicit root id or a dedicated endpoint |
| BUG-563 | FB8 | P2 | Medium | The envelope key is **`files`**, a third convention alongside `result`/`results` | document; align with NETWORK if the backend changes |
| BUG-564 | FB9 | P2 | Medium | A new `FileAdapter` is created on every load | reuse + notify |
| BUG-565 | FB12 | P2 | Medium | No pull-to-refresh and no breadcrumb — the user cannot tell they are at the root | add a title/breadcrumb |
| BUG-566 | FB1 | P3 | Low | Commented-out `header_main` code left in `onCreateView` | delete |
| BUG-567 | FB4 | P3 | Low | The toggle icon shows the current mode rather than the target mode | confirm intent with the customer |

#### `FILE_LIST` — module · `agents/MEDIA/FILE_LIST.md` · 13 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-568 | FL-3 | P1 | High | **Every non-folder opens in the PDF viewer** — images, audio, video and documents all route to `PdfActivity2` | branch on the real file type/extension |
| BUG-569 | FL-5 | P1 | High | `getType().equals("folder")` NPEs on a null type | `"folder".equals(getType())` |
| BUG-570 | FL-8 | P1 | High | The always-true `\|\|` token guard | use `&&` |
| BUG-571 | FL-9 | P1 | High | A non-200 has **no `else` branch** — no error, no empty state | add an error state |
| BUG-572 | FL-11 | P1 | High | The Retrofit call is never cancelled | cancel in `onDestroy` |
| BUG-573 | FL-1 | P2 | Medium | Folder navigation recurses into new activities with **no breadcrumb or title** | show the current folder name |
| BUG-574 | FL-2 | P2 | Medium | Rows show only an icon and a name, ignoring `size`, `createdAt`, `createdBy`, `permission` | enrich the row |
| BUG-575 | FL-4 | P2 | Medium | `FLAG_ACTIVITY_NEW_TASK` on both intents distorts the back stack | pass an Activity context and drop the flag |
| BUG-576 | FL-7 | P2 | Medium | `getStringExtra("folderId").toString()` yields `"null"` when missing | validate the extra |
| BUG-577 | FL-10 | P2 | Medium | The adapter receives **`applicationContext`** here but a fragment context in FILES_BROWSER | pass a consistent context |
| BUG-578 | FL-12 | P2 | Medium | `permission` from the model is **never checked** before opening a file | enforce it, or confirm the server does |
| BUG-579 | FL-6 | P3 | Low | `Log.e("type", ...)` debug logging in the click handler | remove |
| BUG-580 | FL-13 | P3 | Low | No search or sort in a file library | ask the customer |

#### `PDF_VIEWER` — module · `agents/MEDIA/PDF_VIEWER.md` · 17 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-581 | PV4 | P0 | Critical | `Log.e("inputstream", inputStream.toString())` runs on a **null** stream after a non-200 — **NPE** | null-check before logging |
| BUG-582 | PV5 | P0 | Critical | `onPostExecute` dereferences the stream with no null check — second NPE path | handle null with an error view |
| BUG-583 | PV6 | P1 | High | The entire PDF is streamed into memory with no size limit | download to cache and use `fromFile` |
| BUG-584 | PV14 | P1 | High | The `AsyncTask` is never cancelled and holds the Activity + dialog — leaks on rotation | cancel in `onDestroy`, or move off `AsyncTask` |
| BUG-585 | PV15 | P1 | High | Rotation re-downloads the whole PDF and resets the page | save state / cache the file |
| BUG-586 | PV3 | P2 | Medium | `enableAnnotationRendering` is called **twice**, `true` then `false` | pick one |
| BUG-587 | PV7 | P2 | Medium | `DownloadFile` is **dead code** (~45 lines) | delete, or finish the feature |
| BUG-588 | PV8 | P2 | Medium | The dead path writes to a hard-coded **`"testthreepdf"`** folder | remove with PV7 |
| BUG-589 | PV9 | P2 | Medium | `Environment.getExternalStorageDirectory()` is deprecated / scoped-storage blocked | app-specific storage |
| BUG-590 | PV12 | P2 | Medium | `setTitle` writes to a title bar that is not displayed | add a page indicator to `pdf_header` |
| BUG-591 | PV16 | P2 | Medium | No auth header on the download, unlike every Retrofit call — the file URL must be public | confirm with PLATFORM/NETWORK |
| BUG-592 | PV17 | P2 | Medium | Extends `Activity`, not `AppCompatActivity`, so it ignores the app theme and night mode | migrate, keeping `FLAG_SECURE` |
| BUG-593 | PV2 | P3 | Low | `pdf_header` has an **empty** click listener | remove or implement |
| BUG-594 | PV10 | P3 | Low | No runtime permission check in the dead downloader | remove with PV7 |
| BUG-595 | PV11 | P3 | Low | `printStackTrace()` instead of `Log` | use `Log.e` |
| BUG-596 | PV13 | P3 | Low | Unused `SAMPLE_FILE` field and a leftover sample URL comment | delete |
| BUG-597 | PV1 | P3 | Cosmetic | Class named `PdfActivity2` with no `PdfActivity` | rename (manifest change) |

#### `WEBVIEW` — module · `agents/MEDIA/WEBVIEW.md` · 13 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-598 | WV4 | P1 | High | **JavaScript and DOM storage enabled** on a page that only renders static legal text — the code comment itself flags the XSS risk | disable both unless the pages genuinely need them |
| BUG-599 | WV8 | P1 | High | **No error handling** — offline or a 404 shows the WebView's raw error page | add `onReceivedError` + a guard |
| BUG-600 | WV1 | P2 | Medium | Binary routing: anything that is not `"terms"` (including a missing extra) silently shows **privacy** | `when` with an explicit `else` and validation |
| BUG-601 | WV2 | P2 | Medium | Two hard-coded URLs on a **third** host family | move to config (PLATFORM) |
| BUG-602 | WV3 | P2 | Medium | No `shouldOverrideUrlLoading` filtering — any outbound link opens in-app | restrict to the known host |
| BUG-603 | WV5 | P2 | Medium | `loadUrl` runs **before** the settings are applied | configure settings first |
| BUG-604 | WV7 | P2 | Medium | No loading indicator — a blank screen while fetching | add a progress bar |
| BUG-605 | WV9 | P2 | Medium | No title, so Terms and Privacy look identical | set a per-mode title |
| BUG-606 | WV10 | P2 | Medium | Back exits the screen instead of going back in the WebView's history | override `onBackPressed` with `webView.canGoBack()` |
| BUG-607 | WV11 | P2 | Medium | The WebView is never destroyed in `onDestroy` | `webView.destroy()` |
| BUG-608 | WV6 | P3 | Low | Zoom is commented out, hurting accessibility on dense legal text | re-enable |
| BUG-609 | WV12 | P3 | Low | WebView cookies/DOM storage are never cleared | clear on exit if required |
| BUG-610 | WV13 | P3 | Low | Rotation reloads the page and loses scroll position | save state |

### SEARCH — 43 entries

#### `SEARCH_LEAD` — team lead (rollups) · `agents/SEARCH/SEARCH_LEAD.md` · 9 entries

| Bug | Agent id | Issue | Affects module(s) | Risk |
|---|---|---|---|---|
| BUG-611 | S-1 | A **fourth response-envelope shape** (`results` as an object with `users` + `posts`) | SEARCH_ENTRY | parse confusion |
| BUG-612 | S-2 | Both fragments take **constructor arguments** instead of a `Bundle` — Android cannot recreate them after process death | SEARCH_POSTS, SEARCH_PROFILES | **crash on restore** |
| BUG-613 | S-3 | A search fires on **every keystroke** with no debounce, and registers a new `authToken` observer each time | SEARCH_ENTRY | request storm |
| BUG-614 | S-4 | On every keystroke all fragments are **removed and the pager adapter rebuilt** | SEARCH_ENTRY | flicker, lost scroll |
| BUG-615 | S-5 | `SearchAdapter.getItem` returns `null as Fragment` for an out-of-range position | SEARCH_ENTRY | crash if a tab is added |
| BUG-616 | S-6 | The Posts tab passes page **`"searchProfile"`** — a misleading name for post results | SEARCH_POSTS | confusing, but matches FEED's contract |
| BUG-617 | S-7 | `UsersAdapter` inflates **PROFILE's `child_follow.xml`** and hides its `follow_btn` by commenting out the binding | SEARCH_PROFILES | cross-team layout coupling |
| BUG-618 | S-8 | No empty-query guard: clearing the box searches for `""` | SEARCH_ENTRY | pointless request |
| BUG-619 | S-9 | The progress dialog is **commented out**, so searches are silent | SEARCH_ENTRY | no feedback |

#### `SEARCH_ENTRY` — module · `agents/SEARCH/SEARCH_ENTRY.md` · 15 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-620 | SE5 | P1 | High | A request fires on **every keystroke** — no debounce, no minimum length | debounce ~300 ms, or search on IME action |
| BUG-621 | SE6 | P1 | High | **All fragments are removed** before each request, destroying both tabs per character | update the adapter's data instead of rebuilding |
| BUG-622 | SE8 | P1 | High | Previous calls are never cancelled — out-of-order responses can show stale results | cancel the in-flight call before starting a new one |
| BUG-623 | SE9 | P1 | High | A non-200 has **no `else` branch** — stale results, no message | add an error state |
| BUG-624 | SE11 | P1 | High | `null as Fragment` in `SearchAdapter.getItem` | throw a clear exception, or handle all positions |
| BUG-625 | SE13 | P1 | High | A new `authToken` observer per keystroke, never removed | read the token once |
| BUG-626 | SE1 | P2 | Medium | The progress dialog's `show()` is commented out, so searching is silent | restore it, or add inline progress |
| BUG-627 | SE2 | P2 | Medium | Uses `ViewPager` v1 + deprecated `FragmentPagerAdapter` | migrate to `ViewPager2` |
| BUG-628 | SE7 | P2 | Medium | An empty query is still sent | guard on blank input |
| BUG-629 | SE12 | P2 | Medium | `FragmentPagerAdapter(fm!!)` without a behaviour flag | pass `BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT` |
| BUG-630 | SE14 | P2 | Medium | `currentTab` is not saved across configuration change | `onSaveInstanceState` |
| BUG-631 | SE3 | P3 | Low | `onTabSelected` and `onTabReselected` are identical; `onTabUnselected` is empty | tidy |
| BUG-632 | SE4 | P3 | Low | `setEditTextFocus` is only ever called with `true`; its `false` path is dead | simplify |
| BUG-633 | SE10 | P3 | Low | Two `Log.e("current tab", ...)` debug calls | remove |
| BUG-634 | SE15 | P3 | Low | No search history, no recent queries, no clear button | ask the customer |

#### `SEARCH_POSTS` — module · `agents/SEARCH/SEARCH_POSTS.md` · 8 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-635 | SP3 | P1 | High | **Constructor arguments** instead of a `Bundle` — the fragment cannot be recreated by the system | `newInstance()` + `Bundle`; make `Posts` parcelable or pass ids |
| BUG-636 | SP2 | P2 | Medium | All three `HomeAdapter` state maps are **empty**, so no like/follow/fav state renders | pass real maps, or confirm read-only is intended |
| BUG-637 | SP4 | P2 | Medium | A `Context` is held in a field | use `requireContext()` |
| BUG-638 | SP6 | P2 | Medium | No pagination — only the first page of search results is ever shown | needs `getSearch` paging (NETWORK) |
| BUG-639 | SP8 | P2 | Medium | The empty state cannot distinguish "no results" from "search failed" | parent must pass an error flag (SEARCH_ENTRY) |
| BUG-640 | SP1 | P3 | Low | `UserPreferences` is created and never used | delete |
| BUG-641 | SP5 | P3 | Low | The page value `"searchProfile"` is misleading on the Posts tab | rename with FEED (shared contract) |
| BUG-642 | SP7 | P3 | Low | No pull-to-refresh and no loading state | the parent owns loading; acceptable as-is |

#### `SEARCH_PROFILES` — module · `agents/SEARCH/SEARCH_PROFILES.md` · 11 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-643 | SPR2 | P1 | High | `UsersAdapter` reuses PROFILE's `child_follow.xml` but leaves **`follow_btn` unbound** (commented out) | hide it explicitly, or use a dedicated layout |
| BUG-644 | SPR7 | P1 | High | **Constructor arguments** instead of a `Bundle` — cannot be recreated by the system | `newInstance()` + `Bundle` |
| BUG-645 | SPR3 | P2 | Medium | The layout manager uses `context` while the adapter uses `contexts` — two sources, one nullable | pick one |
| BUG-646 | SPR5 | P2 | Medium | `UsersAdapter` and `FollowAdapter` are near-duplicates differing only by the follow button | merge with a flag (cross-team, PM) |
| BUG-647 | SPR8 | P2 | Medium | A `Context` is held in a field | `requireContext()` |
| BUG-648 | SPR9 | P2 | Medium | No follow action from search results, though the row layout has a button | confirm with the customer |
| BUG-649 | SPR10 | P2 | Medium | No pagination — only the first page of profile results | needs `getSearch` paging (NETWORK) |
| BUG-650 | SPR11 | P2 | Medium | The empty state cannot distinguish "no results" from "search failed" | parent must pass a flag |
| BUG-651 | SPR1 | P3 | Low | The list id (`search_profile_list`) differs from the Posts tab's (`list`) | align naming |
| BUG-652 | SPR4 | P3 | Low | The `LifecycleOwner` parameter is never used | remove |
| BUG-653 | SPR6 | P3 | Low | Deprecated `Picasso.with(context)` | upgrade |

<!-- AUTO-GENERATED:END -->

---

## 8. Change log

| Change | Detail |
|---|---|
| Created (T-017) | Consolidated register opened. Hand-written half: id scheme + prefix-collision table, priority mapping, status tracking, closing procedure, **10 cross-cutting clusters (CL-1 … CL-10)**, **10 P0 crashers**, and a 10-wave fix order. Generated half produced by the new `agents/tools/sync_bug_notes.py` from all 42 agent docs + `AGENTS.md` §7. |


