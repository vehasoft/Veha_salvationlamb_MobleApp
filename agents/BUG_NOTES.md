# BUG_NOTES.md — Consolidated Known-Issue Register

> Maintained by the **PROJECT MANAGER** agent (`AGENTS.md`).
> Opened by task **T-017**.
>
> This file is the **single searchable view** of every defect recorded anywhere in the agent
> hierarchy. It does **not** replace the per-agent tables — those remain the source of truth,
> because `AGENTS.md` §5 requires a module agent to update its own `.md` in the same change set
> as the code.

---

> ⚠️ **Baseline moved on 2026-10-08 (T-019).** Every entry in §7 below was extracted from agent docs
> written against remote `master` (v1.1). The project now builds from
> **`salvation_lamb_permissions_final_1` (v1.2.0)**, 123 commits ahead. PM spot-checked the 10 P0s
> on the new code — see §5, where each is marked **STILL PRESENT** or **FIXED UPSTREAM**. Treat
> line numbers and file names in §7 as **`master`-era** until each agent is re-audited; the
> per-agent staleness grid lives in `AGENTS.md` §3 ("v1.2 audit" column).

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
| `AUTH/SPLASH S23` | — | `getBible()` rethrows `RuntimeException` from a Retrofit callback on a malformed payload | AUTH | — | found 2026-10-08 (T-024) |
| `PLATFORM/COMMONS C-22` | — | `Util.permissionMap` is still a public mutable static, so `permissionsLoaded` can be bypassed | PLATFORM | — | found 2026-10-08 (T-025) |
| `G11` | — | Signing keys in remote git history — needs history rewrite + key rotation | PLATFORM | T-020 | 2026-10-08 |

### Fixed

| Agent-local id | Bug | Title | Fixed in | Closed (IST) |
|---|---|---|---|---|
| `AUTH/SPLASH S5` | P0 #1 | `onFailure` does nothing → user stuck on the splash forever | T-024 — `bailToLogin()` | 2026-10-08 16:45 |
| `AUTH/SPLASH S12` | P0 #1 | Offline → silent return → same permanent hang at launch | T-024 — `else { bailToLogin(splash_offline) }` | 2026-10-08 16:45 |
| `AUTH/SPLASH S11` | P0 #2 | Proven-invalid session never cleared → failure repeats every cold start | T-024 — `bailToLogin()` clears token + userId + `Util` state | 2026-10-08 16:45 |
| `AUTH/SPLASH S9`/`S16` | `G5` (part) | `Thread.sleep(2000)` on the main thread | T-024 — deleted (0 left in this file) | 2026-10-08 16:45 |
| `AUTH/SPLASH S18` | — | `"Somthing Went Wrong"` typo *(in this file only; 5 others remain)* | T-024 — replaced with a string resource | 2026-10-08 16:45 |
| `AUTH/SPLASH S19` | — | `SplashhScreenActivity` class-name typo | fixed upstream on the v1.2.0 branch | 2026-10-08 12:40 |
| `AUTH/SPLASH S22` | P0 #11 | Permission map fetched **after** routing → destination screens saw an empty map | T-025 — `getMyPermission(token) { … }` gates routing | 2026-10-08 16:45 |
| `PLATFORM/COMMONS C-21` | P0 #11 / `G13` | **`Util.hasPermission()` failed open** — empty map granted all 34 gates | T-025 — fail-closed + `permissionsLoaded`; 10 unit tests | 2026-10-08 16:45 |
| `G10` | — | Repo not under version control | T-018 — branch `salvation_lamb_agent_baseline` | 2026-10-08 12:20 |
| `MEDIA/PDF_VIEWER PV4`/`PV5` | P0 #3 | Two NPE paths on a non-200 PDF download | fixed upstream on the v1.2.0 branch | 2026-10-08 12:40 |
| `FEED/VIEW_POST VP0` | P0 #4 | 14 `findViewById` calls in the constructor | fixed upstream on the v1.2.0 branch | 2026-10-08 12:40 |
| `SEARCH S-2` | P0 #5 | Fragments took constructor args → crash on process death | fixed upstream on the v1.2.0 branch | 2026-10-08 12:40 |
| `PROFILE F-14` | P0 #7 | `FollowerActivity` loaded before binding views | fixed upstream on the v1.2.0 branch | 2026-10-08 12:40 |

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

### CL-1 · The always-true token guard — **29 call sites (verified on v1.2.0)**

```kotlin
if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty())   // always true
```

Three `||`-ed negations can never all be false. Consequences:

- the session-lost `else` branch is **dead code** in almost every screen;
- a missing token is sent as the literal header **`Bearer null`**.

> **v1.2.0 re-count (T-019):** exactly **29 broken `||` guards remain**, against **29 correct `&&`
> guards** — the codebase is split half-and-half, so the correct form is already established
> convention. Worst offenders: `HomeFragment.kt` (6), `ProfileFragment.kt` (5),
> `AdminVideoFragment.kt` (5), `AdminAudioFragment.kt` (5), then 1 each in `Commons.kt`,
> `WarriorNotificationFragment.kt`, `UserNotificationFragment.kt`, `AdminNotificationFragment.kt`,
> `ChangePasswordActivity.kt`, `BiblePostActivity.kt`, `AnnouncementActivity.kt`, `AddPostActivity.kt`.
> Note the **new** Bible/Notification/Announcement screens copied the broken form.

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

> **All 10 re-verified in the v1.2.0 code on 2026-10-08 (T-019).** 4 were fixed upstream by the
> 123 commits, **6 still reproduced**, and one got *worse* (`#4` is no longer dead code).
>
> **Update 2026-10-08 16:45 (T-024 / T-025):** `#1`, `#2` and the new `#11` are now **fixed and
> tested**. **4 P0s remain open: `#6`, `#8`, `#9`, `#10`.**

| # | Agent-local id | What happens | Where | Status |
|---|---|---|---|---|
| 1 | `AUTH/SPLASH` (`A26`) | **`onFailure` does nothing.** Launch offline or with the server down and the user is **stuck on the splash logo forever** — the only launcher entry point. | `SplashScreenActivity.kt` | ✅ **FIXED (T-024)** — `bailToLogin()` |
| 2 | `AUTH/SPLASH` (`A27`) | Splash never clears a proven-invalid session, so the failing round-trip **repeats on every cold start**. | `SplashScreenActivity.kt` | ✅ **FIXED (T-024)** — session cleared before routing |
| 3 | `MEDIA/PDF_VIEWER` | **NPE on any non-200 download** (two separate paths). | `PdfActivity2.java` | ✅ **FIXED UPSTREAM** — rewritten, no raw `InputStream` |
| 4 | `FEED/VIEW_POST` | 14 `findViewById` calls **in the constructor, before `setContentView`** → guaranteed NPE. | `ViewPostActivity.kt` | ✅ **FIXED UPSTREAM** — binding moved into `onCreate`; now reachable from 15 call sites |
| 5 | `SEARCH` (`S-2`) | Both result fragments take **constructor arguments** → crash on process-death restore. | `SearchPostFragment.kt`, `SearchProfileFragment.kt` | ✅ **FIXED UPSTREAM** — `companion object` + `Bundle` |
| 6 | `PROFILE/VIEW_PROFILE` (`F-15`) | `userId!!` force-unwrap → crash if the extra is missing. | `ViewProfileActivity.kt:20` | 🔴 **OPEN** |
| 7 | `PROFILE/FOLLOWERS` (`F-14`) | Loading starts **before** views are bound → `UninitializedPropertyAccessException`. | `FollowerActivity.kt` | ✅ **FIXED UPSTREAM** |
| 8 | `APPSHELL/MAIN_NAV MN4` (`AS-5`), `SEARCH S-5` | `TabAdapter.getItem` ends `else -> b as Fragment` (`b` is null); `SearchAdapter.getItem` returns `null as Fragment`. **Adding one tab crashes the app.** | `TabAdapter.kt:78`, `SearchAdapter.kt:43` | 🔴 **OPEN — both** |
| 9 | `APPSHELL/MAIN_NAV MN2` (`AS-3`) + CL-4 | `Util.user.isReviewState.toBoolean()` unguarded → **NPE after process death** when opening the menu. | 9 call sites | 🔴 **OPEN** |
| 10 | `APPSHELL/MAIN_NAV MN12` (`AS-6`) | `onBackPressed` calls **`exitProcess(-1)`** — kills the process, skipping all lifecycle and persistence. | `MainActivity.kt:778` | 🔴 **OPEN** |
| 11 | `G13` / `COMMONS C-21` / `SPLASH S22` | **`Util.hasPermission()` failed open** — an empty map granted all 34 gates, and the map was fetched *after* routing. | `Util.java`, `SplashScreenActivity.kt` | ✅ **FIXED (T-025)** — fail-closed + 10 unit tests |

---

## 6. Recommended fix order

| Wave | Scope | Why in this position |
|---|---|---|
| **0** | ~~**`git init`** (`G10`)~~ — **DONE 2026-10-08 (T-018/T-019)** | Rollback now exists: branch `salvation_lamb_agent_baseline` off **`salvation_lamb_permissions_final_1`** (`d8b778a`, v1.2.0). |
| **1** | **P0 #1 and #2 — Splash** (`SplashScreenActivity.kt:354`) | 🔴 confirmed still present on v1.2.0. The app can be **unusable at launch**. Highest user impact, smallest diff. |
| **2** | **`G13` fail-open permissions** (`Util.java:207`) | 🆕 **promoted to wave 2.** A failed splash fetch silently grants all 34 permission gates — a security hole that did not exist on `master`. |
| **3** | **CL-1** token-guard sweep — **29 sites** | Mechanical, removes `Bearer null`, makes every logout branch real. The correct `&&` form already exists in 29 other places: copy it. |
| **4** | **Remaining P0s: #6, #8, #9, #10** | 🔴 the 4 crashers that survived the upgrade (`userId!!`, two `as Fragment` casts, 9× `isReviewState`, `exitProcess`). #3, #4, #5, #7 are ✅ already fixed upstream. |
| **5** | **CL-7** error/offline feedback policy | Turns "the app is broken" into "the network failed". Cheap, large UX win. |
| **6** | **CL-4** extract the overflow menu once | Closes `AS-1`, `AS-3`, `F-7`, `F-10` and fixes logout in one place. Now **9** `isReviewState` call sites, not 6. |
| **7** | **CL-3** + **CL-6** typed models | Correct roles and counts; unblocks a lot of downstream logic. |
| **8** | **CL-2** shared `SessionBootstrap` | Removes 9-way drift. |
| **9** | ~~`G1`, `G2`~~ **closed upstream** · `G3` only | Only the legacy `com.android.support` mix remains; `jcenter()` and synthetics were fixed by the v1.2 branch. |
| **10** | **QA team + `G9`** | No tests exist. Every wave above is a regression risk until this lands. |

> **Standing recommendation:** ~~`G10` (no git)~~ **closed 2026-10-08**. `G9` (no tests) still
> stands, so every fix above remains **unverifiable** (though now reversible). PM advises closing
> `G9` before wave 4.
>
> **Why the order changed (T-019):** re-baselining onto v1.2.0 closed `G1`, `G2` and 4 of the 10
> P0s for free, but surfaced `G13` (fail-open permissions) and `G11` (keys in history). The
> remaining work is roughly **40% smaller** than the `master`-era plan.

---

## 7. Generated register

Everything below is produced by `agents/tools/sync_bug_notes.py`. **Do not edit by hand.**

<!-- AUTO-GENERATED:BEGIN -- do not edit by hand; run agents/tools/sync_bug_notes.py -->

**657 tracked entries** extracted from 42 agent docs, plus 14 PM-level global issues.

| Severity | Count | Priority |
|---|---|---|
| Critical | 7 | P0 |
| High | 166 | P1 |
| Medium | 232 | P2 |
| Low | 134 | P3 |
| Cosmetic | 14 | P3 |
| Rollup (team-lead aggregate) | 104 | -- |
| **Distinct module-level defects** | **553** | |

### Per-team breakdown

| Team | Critical | High | Medium | Low | Cosmetic | Rollups | Total |
|---|---|---|---|---|---|---|---|
| PLATFORM | 1 | 23 | 34 | 25 | 1 | 17 | **101** |
| AUTH | 3 | 32 | 64 | 31 | 8 | 30 | **168** |
| APPSHELL | 0 | 16 | 24 | 15 | 0 | 10 | **65** |
| FEED | 1 | 28 | 34 | 24 | 4 | 13 | **104** |
| PROFILE | 0 | 30 | 27 | 12 | 0 | 15 | **84** |
| MEDIA | 2 | 28 | 34 | 17 | 1 | 10 | **92** |
| SEARCH | 0 | 9 | 15 | 10 | 0 | 9 | **43** |

### PM-level global issues (`AGENTS.md` §7)

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

## Full register

### PLATFORM — 101 entries

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

#### `COMMONS` — module · `agents/PLATFORM/COMMONS.md` · 22 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-038 | C-21 | P0 | Critical | **NEW (T-025, `G13`)** — ~~`Util.hasPermission()` **failed open**: `if (permissionMap == null \|\| permissionMap.isEmpty()) return true;`. A failed `GET /api/v1/permission/users/{userId}` at splash left the map empty, so **all 34 permission gates granted access**, including admin-only screens~~ — **FIXED 2026-10-08**: now fail-CLOSED, backed by a new `permissionsLoaded` flag that distinguishes "not fetched yet" from "fetched, user has nothing". New API: `setPermissionMap()`, `clearPermissions()`, `isPermissionsLoaded()`. Covered by **10 unit tests** in `app/src/test/java/com/veha/util/UtilPermissionTest.kt` | — |
| BUG-039 | C-1 | P1 | High | `Util` statics are the de-facto session object but do **not** survive process death | re-hydrate from DataStore on resume, or stop relying on statics |
| BUG-040 | C-2 | P1 | High | `Util.player` is a **static `MediaPlayer`** that is never guaranteed to be released | own it in the MEDIA screen |
| BUG-041 | C-9 | P1 | High | `isNetworkAvailable` **toasts from a utility**, mixing policy with UI at 62 call sites | return a result and let screens decide; the trailing space in `"No Internet "` is also a typo |
| BUG-042 | C-12 | P1 | High | `makeWarrior` returns a status assigned **asynchronously**, so it is always `""`; `MainActivity:185`'s `contentEquals("success")` can never be true | take a callback/`suspend`, or move to a ViewModel |
| BUG-043 | C-13 | P1 | High | The token guard is an always-true `\|\|` chain (same bug as CHANGE_PASSWORD) | use `&&`, or test the token explicitly |
| BUG-044 | C-16 | P1 | High | `makeWarrior` is 100 lines of dialog UI inside PLATFORM | move to PROFILE as a real screen/dialog fragment |
| BUG-045 | C-4 | P2 | Medium | `isValidPassword` silently caps length at 20 and allows only `@#$%^&+=` as symbols; no screen tells the user about the cap | widen the symbol class, document the rule |
| BUG-046 | C-5 | P2 | Medium | `SimpleDateFormat` with the default locale in `formatDate` / `getTimeAgo` | pass `Locale.US` for machine formats |
| BUG-047 | C-8 | P2 | Medium | `getVideo()` hard-codes a **second** host, independent of `Util.url` | move to NETWORK config |
| BUG-048 | C-10 | P2 | Medium | Connectivity is inferred from transport presence, not reachability | `NET_CAPABILITY_VALIDATED` |
| BUG-049 | C-14 | P2 | Medium | A new `authToken` observer is registered on every `makeWarrior` call | one-shot read |
| BUG-050 | C-15 | P2 | Medium | A failed warrior request shows the user nothing | toast on error and `onFailure` |
| BUG-051 | C-20 | P2 | Medium | `Util.java` mixes four unrelated concerns (constants, state, validators, networking) in 158 lines | split into `Session`, `Validators`, `DateUtils` |
| BUG-052 | C-22 | P2 | Medium | **NEW (T-025)** — `permissionMap` is still a **public mutable static**, so any screen can bypass `setPermissionMap()` and leave `permissionsLoaded` stale | make the field private and route all writes through the setter |
| BUG-053 | C-3 | P3 | Low | Validators use `.find()` instead of `.matches()` | switch to `matches` |
| BUG-054 | C-6 | P3 | Low | `getTimeAgo` can render negative durations on clock skew, and swallows all exceptions | clamp at 0, log properly |
| BUG-055 | C-7 | P3 | Low | `getReligion()` rebuilds a 36-item list on every call and assigns a private static | make it an immutable constant |
| BUG-056 | C-11 | P3 | Low | `isNetworkAvailable(context!!)` force-unwraps | take a non-null `Context` |
| BUG-057 | C-17 | P3 | Low | `toDate()` / `formatTo()` are instance members, not top-level extensions | move to a file-level extension |
| BUG-058 | C-19 | P3 | Low | `Commons` is instantiated 62 times (`Commons().isNetworkAvailable(...)`) though it is stateless | make it an `object` |
| BUG-059 | C-18 | P3 | Cosmetic | Empty `showAlert()` dead code | delete |

#### `DATA_MODELS` — module · `agents/PLATFORM/DATA_MODELS.md` · 14 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-060 | D1 | P1 | High | `Loginresp.isVerifiedUser` vs `UserRslt.isVerified` — same concept, two names | align with the backend; this forces LOGIN's second round-trip |
| BUG-061 | D5 | P1 | High | **Non-null `String` fields can hold `null`** because Gson bypasses constructors; a missing server field becomes a delayed NPE | make fields nullable with defaults (`val x: String? = null`), or add `@SerializedName` + a null-safe parser |
| BUG-062 | D6 | P1 | High | **All booleans are `String`** (`isWarrior`, `isVerified`, `isFreshUser`, `blocked`, `isReviewState`) and read with `.toBoolean()`, so `"TRUE"`/`"1"` silently become `false` (G7) | `Boolean` fields, or a shared `String.toBooleanSafe()` in COMMONS |
| BUG-063 | D8 | P1 | High | **No `@SerializedName` anywhere** — field names are hard-bound to the JSON, so a Kotlin rename silently breaks parsing with no compile error | annotate every field |
| BUG-064 | D7 | P2 | Medium | Counts (`likesCount`, `shareCount`) and `size` are `String` | `Int` / `Long` |
| BUG-065 | D9 | P2 | Medium | `Loginresp` and `UserRslt` overlap by ~24 fields with no shared supertype | extract a common interface/base |
| BUG-066 | D11 | P2 | Medium | No `data class` for the **response envelope** itself (`result` / `message` / `status` / `errorMessage`) — every screen re-parses it by hand | add `ApiResponse<T>` (with NETWORK) |
| BUG-067 | D12 | P2 | Medium | No pagination wrapper despite 4 paged endpoints | add a `Page<T>` model (with NETWORK + FEED) |
| BUG-068 | D14 | P2 | Medium | `Loginresp.password` echoes the password back from the server | raise with the backend; drop the field |
| BUG-069 | D2 | P3 | Low | Geo models use snake_case while the rest use camelCase | `@SerializedName` + camelCase properties |
| BUG-070 | D3 | P3 | Low | `Countries.native` uses a Kotlin soft keyword as a field name | rename with `@SerializedName("native")` |
| BUG-071 | D4 | P3 | Low | `AllFavList` and `FavPost` are structurally identical duplicates | delete one |
| BUG-072 | D10 | P3 | Low | `Posts.user` is the only `var`; everything else is `val`, so mutation is inconsistent | use `copy()` instead |
| BUG-073 | D13 | P3 | Low | Dates are `String` with no parsing contract in the model | keep as-is, or expose typed accessors |

#### `NETWORK` — module · `agents/PLATFORM/NETWORK.md` · 16 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-074 | N1 | P1 | High | `APIUtil.kt` is a **second, dead Retrofit builder** pointing at a different host over plain HTTP (G4) | delete it, or make it the single source of truth |
| BUG-075 | N8 | P1 | High | No auth interceptor; `"Bearer $token"` is hand-built at ~30 call sites | add an `Interceptor` that injects the header |
| BUG-076 | N11 | P1 | High | **No OkHttp timeouts** — a stalled connection hangs forever (this is what makes SPLASH's silent `onFailure` fatal) | set connect/read/write timeouts |
| BUG-077 | N15 | P1 | High | Every response is `Call<JsonObject?>`, so there is **no compile-time contract** with the backend (G7) | typed responses (with DATA_MODELS) |
| BUG-078 | N2 | P2 | Medium | `postCall` takes a **dynamic path**, hiding which endpoints exist and defeating search | declare explicit methods for `login` and `users` |
| BUG-079 | N5 | P2 | Medium | Two `getPost` overloads differing only by parameters — easy to call the wrong one | rename to `getFeed` / `getPostById` |
| BUG-080 | N6 | P2 | Medium | `getFollowers` -> `follows/user/{id}` vs `getFollowing` -> `follows/{id}`; names do not reveal paths | document or rename |
| BUG-081 | N7 | P2 | Medium | Callers test `code() == 200`, so `201 Created` / `204 No Content` are treated as failures | use `response.isSuccessful` |
| BUG-082 | N12 | P2 | Medium | No logging interceptor, so failures can only be diagnosed by adding `Log.e` by hand | add `HttpLoggingInterceptor` on debug builds |
| BUG-083 | N13 | P2 | Medium | `postProfilePic` sends an image inside a `JsonObject` rather than multipart | use `@Multipart` if the backend allows |
| BUG-084 | N14 | P2 | Medium | No documented pagination contract: `page`/`size` exist on 4 endpoints but the response shape is unspecified | document with FEED |
| BUG-085 | N3 | P3 | Low | Path casing is inconsistent (`Users/image/...`, `Country` vs `users/...`, `city/...`) | align with the backend |
| BUG-086 | N4 | P3 | Low | `getFav` and `getMyFav` are **identical** declarations of the same path | delete one |
| BUG-087 | N9 | P3 | Low | `converter-gson:2.5.0` paired with `retrofit:2.9.0` | align both to 2.9.0 |
| BUG-088 | N10 | P3 | Low | OkHttp is declared **without a version** | pin it or use the BOM |
| BUG-089 | N16 | P3 | Low | The Retrofit singleton is never rebuilt, so `Util.url` cannot change at runtime | add a reset for environment switching |

#### `STORAGE` — module · `agents/PLATFORM/STORAGE.md` · 12 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-090 | S-1 | P1 | High | Pinned to `datastore-preferences:1.0.0-alpha01` using `createDataStore` / `preferencesKey`, both **removed** in stable | migrate to `preferencesDataStore` + `stringPreferencesKey` (BUILD_CONFIG + every call site) |
| BUG-091 | S-2 | P1 | High | The three `String` flows return the literal **`"null"`** when a key is absent, so `isNullOrEmpty()` never matches and every consumer hard-codes `== "null"` | expose `Flow<String?>` and drop `.toString()`, then update all consumers in one change |
| BUG-092 | S-6 | P1 | High | A **new `DataStore` per instance** over the same file, at 26 sites; only the alpha's missing guard prevents a crash | make it a singleton (or an `object` with an app-level `Context`) |
| BUG-093 | S-7 | P1 | High | Nothing in the app reads one-shot; all 5 flows are observed continuously, causing duplicate network calls on re-emission | add one-shot helpers (`suspend fun getAuthToken(): String?`) |
| BUG-094 | S-5 | P2 | Medium | No `clearAll()`; logout deletes only `token` + `userId`, leaving `isNight`, `isFirst`, `textSize` from the previous user | add `suspend fun clearSession()` and use it everywhere |
| BUG-095 | S-8 | P2 | Medium | The token is stored in **plain text**; DataStore is not encrypted | EncryptedSharedPreferences or an encrypted DataStore wrapper |
| BUG-096 | S-9 | P2 | Medium | Writes are fire-and-forget with no error handling | surface failures, at least by logging |
| BUG-097 | S-10 | P2 | Medium | No migration strategy; renaming a key silently logs every user out on upgrade | document and version keys |
| BUG-098 | S-3 | P3 | Low | `saveUserId(token: String)` — parameter misnamed | rename to `userId` |
| BUG-099 | S-4 | P3 | Low | `isFirstTime` uses `preferences[IS_FIRST] as Boolean` instead of the typed accessor | use the typed value |
| BUG-100 | S-11 | P3 | Low | The store name `"SalvationLamb"` is a magic string in the constructor | extract a constant |
| BUG-101 | S-12 | P3 | Low | Boilerplate: 5 keys x 3 members, all identical in shape | generic helpers |

### AUTH — 168 entries

#### `AUTH_LEAD` — team lead (rollups) · `agents/AUTH/AUTH_LEAD.md` · 30 entries

| Bug | Agent id | Issue | Affects module(s) | Risk |
|---|---|---|---|---|
| BUG-102 | A1 | `Thread.sleep(2000)` runs on the main thread inside the Retrofit callback | LOGIN, SPLASH | ANR / frozen UI |
| BUG-103 | A2 | `onFailure` shows no user feedback (log only) in Login and Splash | LOGIN, SPLASH | user sees a dead button |
| BUG-104 | A3 | Offline -> `isNetworkAvailable` false -> the method silently returns; the progress dialog is never even shown and nothing is said | all AUTH screens | user thinks the app is broken |
| BUG-105 | A4 | `getMyDetails()` duplicated in 3 classes | LOGIN, SPLASH (+ APPSHELL) | fixes drift |
| BUG-106 | A5 | Server error matching relies on English substrings (`"Invalid password"`, `"Invalid Email-Id"`); any backend wording change silently shows nothing | LOGIN | silent failure |
| BUG-107 | A6 | `errorMessage` toasted with its JSON quotes | FORGOT_PASSWORD, OTP_VERIFY, REGISTER, CHANGE_PASSWORD | cosmetic |
| BUG-108 | A7 | Password text is sent and also present in `Loginresp.password` from the server | LOGIN, REGISTER | security smell — raise with backend |
| BUG-109 | A8 | Unverified user is routed to `ForgotPasswordActivity` without `finish()`, so Back returns to the previous screen | LOGIN, SPLASH | nav inconsistency |
| BUG-110 | A9 | `isVerified` / `isWarrior` / `isFreshUser` are `String` fields parsed with `.toBoolean()`; `null`/`"1"`/`"TRUE"` behave unexpectedly | all AUTH | wrong routing |
| BUG-111 | A10 | `SplashhScreenActivity` observes DataStore flows that re-emit, so `getMyDetails` can fire more than once | SPLASH | duplicate calls |
| BUG-112 | A11 | `RegisterActivity` wildcard-imports **both** `activity_register.*` and `activity_edit_profile.*` synthetics; the two layouts share 14 ids | REGISTER | wrong view bound silently |
| BUG-113 | A12 | Registration never creates a session (no token/userId), so a newly verified user is always bounced to Login | REGISTER, OTP_VERIFY | expected today — document before changing |
| BUG-114 | A13 | `page = intent.getStringExtra("page").toString()` yields the literal string `"null"` when no extra is passed; forgot mode works only because `"null" != "verify"` | FORGOT_PASSWORD, OTP_VERIFY | a "null-safety cleanup" silently flips the screen into the wrong mode |
| BUG-115 | A14 | `ForgotPasswordActivity` is the only AUTH screen with **no email format validation** (`Util.isValidEmail` unused) | FORGOT_PASSWORD | inconsistent UX vs Login/Register |
| BUG-116 | A15 | `Log.e("data", data.toString())` logs the email **and OTP** in clear text | FORGOT_PASSWORD, OTP_VERIFY | privacy leak in release logcat |
| BUG-117 | A16 | Verify mode fires a verification email on **every** entry and every rotation (`checkValid` is called from `onCreate`), with no cooldown | OTP_VERIFY | mail flooding / rate limiting |
| BUG-118 | A17 | Post-verification routing reads the ambient `Util.userId` instead of an explicit extra, so the destination silently depends on which screen opened it | OTP_VERIFY | wrong landing screen if a caller changes |
| BUG-119 | A18 | The magic strings `"verify"`, `"page"` and `"email"` are hand-typed in 4 files (Register, Login, Splash, ForgotPasswordActivity) | OTP_VERIFY, REGISTER, LOGIN, SPLASH | a typo silently renders the wrong mode |
| BUG-120 | A19 | `ChangePasswordActivity`'s token guard is an always-true `\|\|` chain, so its session-lost branch is **dead code** and a missing token is sent as `Bearer null` | CHANGE_PASSWORD | silent auth failure |
| BUG-121 | A20 | A new `authToken` LiveData observer is registered on **every** button tap and never removed | CHANGE_PASSWORD | duplicate requests |
| BUG-122 | A21 | Both error branches in Change/Reset Password have their `errorMessage` parsing **commented out**, so a wrong old password / expired OTP both show "Something went wrong" | CHANGE_PASSWORD, RESET_PASSWORD | undiagnosable failures |
| BUG-123 | A22 | Intent extras are read with `.toString()` in `ForgotPasswordActivity` (yielding `"null"`) but **without** it in `ChangePasswordActivity` (yielding a real `null`) — two opposite conventions inside one flow | FORGOT_PASSWORD, OTP_VERIFY, CHANGE_PASSWORD, RESET_PASSWORD | confusing / fragile |
| BUG-124 | A23 | A successful password **reset** shows no confirmation at all — the screen just closes | RESET_PASSWORD | user cannot tell if it worked |
| BUG-125 | A24 | The "new password is same as old password" check is **inert** during a reset (`old_pwd` is GONE, so the value is `""`), so a user can reset to their current password | RESET_PASSWORD | no reuse protection |
| BUG-126 | A25 | `ChangePasswordActivity` shares its validation chain, `onCreate` prologue and all three lifecycle overrides between two agents; only `changePassword()` / `forgotPassword()` are exclusive | CHANGE_PASSWORD, RESET_PASSWORD | cross-agent regressions |
| BUG-127 | A26 | **`SplashhScreenActivity.onFailure` does nothing** — no toast, no navigation. A network failure or an offline launch leaves the user stuck on the splash logo forever | SPLASH | **app unusable at launch** |
| BUG-128 | A27 | Splash does **not** clear a proven-invalid session (no `deleteAuthToken`/`deleteUserId`), unlike every other 401-ish handler, so the failing round-trip repeats on every cold start | SPLASH | permanent broken state |
| BUG-129 | A28 | Splash runs **two** `Thread.sleep(2000)` calls on the main thread (before the request and inside its callback) — ≥ 4 s of frozen UI for a returning user, on top of A1 | SPLASH, LOGIN | ANR at launch |
| BUG-130 | A29 | The empty-`userId` branch in Splash calls `finish()` without `return@observe`, so execution continues and issues `GET api/v1/users/` with no id | SPLASH | malformed request |
| BUG-131 | A30 | Splash is the **only** launcher entry and the sole seeder of `Util.isNight`, `Util.fontSize`, `Util.userId`, `Util.user`, `Util.isWarrior`, `Util.isFirst` — bypassing it leaves global state unset | SPLASH | must never lose the intent-filter |

#### `CHANGE_PASSWORD` — module · `agents/AUTH/CHANGE_PASSWORD.md` · 20 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-132 | C11 | P1 | High | The token guard `!isEmpty(it) \|\| !it.equals("null") \|\| !it.isNullOrEmpty()` is **always true**; the whole session-lost `else` branch is unreachable | change to `&&`, or test the token explicitly |
| BUG-133 | C12 | P1 | High | As a result, a missing token is sent as the header `Bearer null` instead of logging the user out | validate before calling |
| BUG-134 | C13 | P1 | High | A new `authToken.asLiveData().observe(this)` is registered on **every** button tap and never removed; DataStore re-emissions can fire duplicate requests | read the token once (e.g. `lifecycleScope.launch { first() }`) outside the listener |
| BUG-135 | C14 | P1 | High | The error branch has its `errorMessage` parsing **commented out** and shows a generic "Something went wrong", so "old password incorrect" is indistinguishable from a server fault | restore the parsing |
| BUG-136 | C18 | P1 | High | `onFailure` shows nothing to the user | toast a generic failure message |
| BUG-137 | C21 | P1 | High | The Retrofit call is not cancelled in `onDestroy` (only after a response) | keep the `Call` and `cancel()` it |
| BUG-138 | C1 | P2 | Medium | Extras are read **without** `.toString()` here but **with** it in `ForgotPasswordActivity` — two opposite null conventions in the same flow | standardise (PM-level, spans several agents) |
| BUG-139 | C2 | P2 | Medium | No `ScrollView` and no `windowSoftInputMode`; with three fields plus the keyboard the buttons can be unreachable | wrap in `ScrollView` / set `adjustResize` (**shared layout**) |
| BUG-140 | C5 | P2 | Medium | `cnfm_pwd` has **no `inputType="textPassword"`** — the confirmation is typed in clear text | add `inputType` (**shared layout**) |
| BUG-141 | C7 | P2 | Medium | The `"Enter Password"` branch is unreachable — an empty password fails `isValidPassword` first | check emptiness before the regex (**shared chain** — coordinate) |
| BUG-142 | C8 | P2 | Medium | The "same as old password" check compares **old vs confirm**, not old vs new | compare `oldPasswordTxt` with `passwordTxt` (**shared chain**) |
| BUG-143 | C10 | P2 | Medium | The old password is never validated (emptiness or format) before being sent | add an emptiness check in this mode only |
| BUG-144 | C17 | P2 | Medium | No token refresh after a password change; a server-side session invalidation goes unnoticed | re-login or refresh the token on success |
| BUG-145 | C19 | P2 | Medium | Offline -> the method returns silently; no dialog, no toast | toast "No internet connection" |
| BUG-146 | C20 | P2 | Medium | `onResume` calls `dialog.dismiss()`, killing the spinner of a still-running request | remove the `onResume` override (**shared**) |
| BUG-147 | C23 | P2 | Medium | Passwords are sent in plain JSON while the app sets `usesCleartextTraffic="true"` globally | PLATFORM/BUILD_CONFIG to restrict cleartext |
| BUG-148 | C4 | P3 | Low | `cnfm_pwd_op` has no `passwordToggleEnabled`, unlike the other two fields | add for consistency (**shared layout**) |
| BUG-149 | C6 | P3 | Low | `otp_op` / `otp` are **dead views** — present in the layout, never referenced by the code | delete (**shared layout** — coordinate with RESET_PASSWORD) |
| BUG-150 | C16 | P3 | Cosmetic | `Gson` is imported but unused (only the commented-out code referenced it) | remove with the C14 fix |
| BUG-151 | C22 | P3 | Cosmetic | `"Somthing Went Wrong"` is misspelled (same typo as `HomeFragment`/`MainActivity`) | fix when the branch is revived |

#### `FORGOT_PASSWORD` — module · `agents/AUTH/FORGOT_PASSWORD.md` · 20 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-152 | F1 | P1 | High | Mode selection depends on `getStringExtra("page").toString()` producing the **literal `"null"`**; a "cleanup" to a proper null check or a typo in the extra silently flips the screen into verify mode | use a constant + `intent.getStringExtra("page") == MODE_VERIFY`, coordinated with `OTP_VERIFY.md` |
| BUG-153 | F5 | P1 | High | After step 1 the email field stays **editable**, so the user can change the address and then submit an OTP for a different email than the one it was sent to — `checkOtp` re-reads `email.text` at tap time | disable the email field once the OTP is requested, or pass the submitted address |
| BUG-154 | F12 | P1 | High | `onFailure` shows nothing to the user on either call | toast a generic failure message |
| BUG-155 | F14 | P1 | High | Retrofit calls are never cancelled in `onDestroy`; the callback touches views of a dead activity | keep the `Call` and `cancel()` it |
| BUG-156 | F2 | P2 | Medium | No `ScrollView` — with the keyboard open on a small screen the buttons can be unreachable; no `windowSoftInputMode` in the manifest | wrap in `ScrollView` and/or set `adjustResize` |
| BUG-157 | F3 | P2 | Medium | `email` has no `inputType="textEmailAddress"` | add `inputType` and `imeOptions` |
| BUG-158 | F4 | P2 | Medium | `otp` has no `inputType="number"`, no `maxLength`, no auto-advance | add numeric input type and a length cap |
| BUG-159 | F7 | P2 | Medium | `onBackPressed` pushes a **new** `LoginActivity` even though Login is already on the stack | just `finish()` in forgot mode (shared method — coordinate with OTP_VERIFY) |
| BUG-160 | F9 | P2 | Medium | **No email format validation** — `Util.isValidEmail` is never called, unlike Login/Register | add the same check for consistent UX |
| BUG-161 | F11 | P2 | Medium | `Log.e("data", data.toString())` prints the email **and OTP** to logcat in release builds | remove the log |
| BUG-162 | F13 | P2 | Medium | Offline -> the method returns silently; no dialog, no toast | toast "No internet connection" |
| BUG-163 | F15 | P2 | Medium | `emailID` is `lateinit` but assigned **only** in verify mode — any new forgot-mode code reading it crashes | initialise it in both branches |
| BUG-164 | F16 | P2 | Medium | Rotation resets the screen to step 1 (`otp_op` back to `gone`, listener reverted) while the typed OTP survives in the hidden field | persist the step in `onSaveInstanceState` |
| BUG-165 | F17 | P2 | Medium | No "resend OTP" button and no cooldown/timer | ask the customer |
| BUG-166 | F8 | P3 | Low | `if (forgot_btn.text != "verify")` is dead code and compares against the wrong case (`"Verify"`) | delete the condition |
| BUG-167 | F10 | P3 | Low | Emptiness is checked on the trimmed value but the **untrimmed** text is sent | send `.trim()` |
| BUG-168 | F18 | P3 | Low | `message` / `errorMessage` are toasted with their JSON quotes | use `asString` instead of `toString()` |
| BUG-169 | F19 | P3 | Low | Success and failure of step 1 are indistinguishable to an automated test — both just toast server text | add explicit states |
| BUG-170 | F20 | P3 | Low | `status` is never logged here, unlike Login/Register, making server issues harder to diagnose | log it |
| BUG-171 | F6 | P3 | Cosmetic | Button label becomes lower-case **"confirm"** | use "Confirm" / a string resource |

#### `LOGIN` — module · `agents/AUTH/LOGIN.md` · 13 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-172 | L1 | P1 | High | `Thread.sleep(2000)` inside `getMyDetails` runs on the **main thread** | remove it, or use `Handler.postDelayed` / `lifecycleScope.launch { delay(2000) }` |
| BUG-173 | L2 | P1 | High | Retrofit calls are never cancelled in `onDestroy`; callbacks touch a dead activity | keep the `Call` references and `cancel()` them, or guard with `isFinishing`/`isDestroyed` |
| BUG-174 | L3 | P1 | High | Unmatched server errors show **no** message; `onFailure` shows none either | add an `else` toast with the server text and a generic failure toast |
| BUG-175 | L4 | P2 | Medium | Every successful login overwrites `isNight` with `Util.DEFAULT`, wiping the saved theme | only write the theme when no value exists |
| BUG-176 | L5 | P2 | Medium | `isWarrior` logic treats empty/null as **true** (`isNullOrEmpty() \|\| != "false"`) | compare explicitly with `"true"` |
| BUG-177 | L6 | P2 | Medium | `email` field has no `inputType="textEmailAddress"` and no `imeOptions`; no field-level `error` display | add `inputType`, IME next/done, and `TextInputLayout` errors |
| BUG-178 | L7 | P2 | Medium | Unverified users are sent to the verify screen **without** `finish()` | `finish()` after `startActivity` |
| BUG-179 | L8 | P2 | Medium | Offline (`isNetworkAvailable == false`) -> method returns silently, no dialog, no toast | toast "No internet connection" |
| BUG-180 | L12 | P2 | Medium | Password is sent in plain JSON over a base URL that is HTTPS, but the app sets `usesCleartextTraffic="true"` globally | PLATFORM/BUILD_CONFIG to restrict cleartext |
| BUG-181 | L9 | P3 | Low | `getMyDetails` has no progress indicator — a ~2s+ blank gap after the dialog closes | keep the dialog up until routing completes |
| BUG-182 | L10 | P3 | Low | `Util` fields are assigned twice (before and inside the coroutine) | assign once |
| BUG-183 | L11 | P3 | Low | All strings hard-coded; not localisable; `status`/`errorMessage` carry JSON quotes | move to `strings.xml`; use `asString` instead of `toString()` |
| BUG-184 | L13 | P3 | Low | No rotation/state handling; dialog and in-flight request are lost | handle config change or move state to a ViewModel |

#### `OTP_VERIFY` — module · `agents/AUTH/OTP_VERIFY.md` · 20 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-185 | V1 | P1 | High | Mode hinges on the magic string `"verify"` spelled identically in 4 files; a typo silently renders Forgot Password with an empty email | a shared constant (PLATFORM/COMMONS) used by all callers |
| BUG-186 | V2 | P1 | High | Post-verification routing depends on the ambient `Util.userId` rather than on an explicit extra, so the destination changes with the caller | pass the intent (`"from"`) explicitly, or re-login deliberately |
| BUG-187 | V5 | P1 | High | After the auto-send, the shared listener sends `email.text` instead of `emailID`; it only works because `onCreate` pre-fills the hidden field | always pass `emailID` in verify mode |
| BUG-188 | V9 | P1 | High | **Every entry — including every rotation — fires a new OTP email**; no cooldown, no rate-limit guard | send only on first create (`savedInstanceState == null`) and add a resend button with a timer |
| BUG-189 | V12 | P1 | High | `onFailure` shows nothing on either call | toast a generic failure message |
| BUG-190 | V13 | P1 | High | Offline -> the method returns silently; no dialog, no toast, and the user never learns no OTP was sent | toast "No internet connection" |
| BUG-191 | V14 | P1 | High | Retrofit calls are never cancelled in `onDestroy` | keep the `Call` and `cancel()` it |
| BUG-192 | V3 | P2 | Medium | No `ScrollView` and no `windowSoftInputMode`; with the keyboard open the buttons can be unreachable | wrap in `ScrollView` / set `adjustResize` |
| BUG-193 | V4 | P2 | Medium | `otp` has no `inputType="number"`, no `maxLength`, no autofill hint (`smsOTPCode`) | add numeric input + length cap |
| BUG-194 | V7 | P2 | Medium | A missing `email` extra yields the literal `"null"`, which is sent to the server | validate the extra and bail out with a message |
| BUG-195 | V8 | P2 | Medium | On auto-send failure the error is written to `email.error`, but `email_op` is `GONE` — **the field error is invisible** | set the error on `otp` (or a shared banner) in verify mode |
| BUG-196 | V10 | P2 | Medium | `Log.e("data", ...)` and `Log.e("email", emailID)` print the email and OTP in clear text | remove the logs |
| BUG-197 | V11 | P2 | Medium | After success the local session is not refreshed (`Util.user.isVerified` stays `"false"`, no token saved) | re-fetch the user, or rely on the Login round-trip |
| BUG-198 | V15 | P2 | Medium | The button silently relabels from "Verify" to lower-case **"confirm"** right after the screen opens, which reads as a glitch | keep the label stable in verify mode |
| BUG-199 | V17 | P2 | Medium | No "resend OTP" affordance and no countdown, even though the auto-send is invisible to the user | add a timed resend (coordinate with FORGOT_PASSWORD — shared layout) |
| BUG-200 | V6 | P3 | Low | Emptiness is checked on the trimmed OTP but the untrimmed value is sent | send `.trim()` |
| BUG-201 | V18 | P3 | Low | `message` / `errorMessage` are toasted with their JSON quotes | use `asString` |
| BUG-202 | V19 | P3 | Low | `status` is never read or logged in either method | log it |
| BUG-203 | V20 | P3 | Low | An unverified user reaching this screen from Login/Splash can leave only via Login — correct, but there is no explanation shown to them | add copy clarifying why they are here |
| BUG-204 | V16 | P3 | Cosmetic | `cancel_btn.visibility = View.VISIBLE` is redundant (already visible in XML) | delete |

#### `REGISTER` — module · `agents/AUTH/REGISTER.md` · 23 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-205 | R2 | P1 | High | Both `activity_register.*` **and** `activity_edit_profile.*` synthetics are wildcard-imported; the two layouts share **14 ids** (`fname`, `lname`, `email`, `mobile`, `gender`, `date`, `male`, `female`, `prod_logo`, `*_op`, `other`), patched with 6 explicit imports. Adding an id to **either** layout can silently rebind a view here | drop the `activity_edit_profile.*` import; longer term migrate this screen to ViewBinding (needs PM sign-off) |
| BUG-206 | R15 | P1 | High | `onFailure` gives no user feedback | toast a generic failure message |
| BUG-207 | R17 | P1 | High | Retrofit call is never cancelled in `onDestroy` | keep the `Call` and `cancel()` it, or guard with `isFinishing` |
| BUG-208 | R1 | P2 | Medium | `android:exported="true"` with no intent-filter — any app can launch the sign-up screen | set `exported="false"` (PLATFORM/BUILD_CONFIG) |
| BUG-209 | R4 | P2 | Medium | `email` field has no `inputType="textEmailAddress"`; no `imeOptions` chain across the form | add `inputType` + `imeOptions="actionNext"` |
| BUG-210 | R5 | P2 | Medium | `signin_btn` and the success path never call `finish()` -> Login/Register stack grows | `finish()` after `startActivity` |
| BUG-211 | R9 | P2 | Medium | **Last Name is never validated** — may be empty or contain digits, and is concatenated into `name` | add empty + `isValidName` checks |
| BUG-212 | R12 | P2 | Medium | DOB travels through three formats: picker writes `d-M-yyyy`, `formatDate` parses `dd-MM-yyyy` and emits `MM-dd-yyyy`. `SimpleDateFormat` is lenient so `1-5-1990` parses, but the chain is fragile and locale-dependent | keep a `Calendar`/epoch value and format once at send time |
| BUG-213 | R14 | P2 | Medium | `Util.user` is populated for a user who is not signed in and has no token | do not set global user state until login succeeds |
| BUG-214 | R16 | P2 | Medium | Offline -> the method returns silently; no dialog, no toast | toast "No internet connection" |
| BUG-215 | R18 | P2 | Medium | The picked date lives only in `date.text` (a `TextView` without `freezesText`) -> lost on rotation | add `android:freezesText="true"` or hold the value in a field |
| BUG-216 | R20 | P2 | Medium | No confirm-password field and no terms checkbox | ask the customer before adding |
| BUG-217 | R21 | P2 | Medium | Password sent in plain JSON; the app globally sets `usesCleartextTraffic="true"` | PLATFORM/BUILD_CONFIG to restrict cleartext |
| BUG-218 | R3 | P3 | Low | Uses deprecated `ProgressDialog`; the rest of AUTH uses `SpotsDialog` | switch to `SpotsDialog` for consistency |
| BUG-219 | R6 | P3 | Low | The date picker always reopens at **1 Jan 1960**; `year` is a `val` and `month`/`day` are never updated from the selection | keep the chosen date in the fields and seed the picker with it |
| BUG-220 | R7 | P3 | Low | `showDialog(999)` / `onCreateDialog(int)` are deprecated | build the `DatePickerDialog` directly in `setDate`, or use `MaterialDatePicker` |
| BUG-221 | R8 | P3 | Low | Empty first name sets the field error to "Enter name" but toasts "Enter first name" | use one string |
| BUG-222 | R10 | P3 | Low | `doValidation()` runs twice on failure (side effects applied twice) | store the result in a local `val` |
| BUG-223 | R11 | P3 | Low | `toLowerCase()` without a locale (deprecated) on the gender label | `lowercase(Locale.ROOT)`, or better: map the radio **id** to a value instead of its label |
| BUG-224 | R13 | P3 | Low | `userPreferences` is instantiated and never used | delete |
| BUG-225 | R19 | P3 | Low | `errorMessage` is toasted with its JSON quotes | use `asString` instead of `toString()` |
| BUG-226 | R23 | P3 | Low | `android:onClick="setDate"` breaks under obfuscation (`minifyEnabled` is currently `false`) | wire the listener in Kotlin, or keep a proguard rule |
| BUG-227 | R22 | P3 | Cosmetic | `tools:context` points at `LoginActivity` | correct to `RegisterActivity` |

#### `RESET_PASSWORD` — module · `agents/AUTH/RESET_PASSWORD.md` · 21 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-228 | P14 | P1 | High | The error branch has its `errorMessage` parsing **commented out**, so an **expired/invalid OTP** shows only "Something went wrong" and the user is never told to restart the forgot flow | restore the parsing and special-case OTP errors |
| BUG-229 | P15 | P1 | High | A successful reset shows **no confirmation at all** — the screen just closes | toast "Password reset successfully" before `finish()` |
| BUG-230 | P18 | P1 | High | `onFailure` shows nothing to the user | toast a generic failure message |
| BUG-231 | P21 | P1 | High | The Retrofit call is not cancelled in `onDestroy` (only after a response) | keep the `Call` and `cancel()` it |
| BUG-232 | P1 | P2 | Medium | Extras are read **without** `.toString()` here but **with** it in `ForgotPasswordActivity`, the screen that launches this one — two opposite null conventions in one flow | standardise (PM-level, spans several agents) |
| BUG-233 | P2 | P2 | Medium | No `ScrollView` and no `windowSoftInputMode` | wrap in `ScrollView` / set `adjustResize` (**shared layout**) |
| BUG-234 | P3 | P2 | Medium | The heading says "Change Password", not "Reset Password"; there is no logo/branding and nothing tells the user which email the reset applies to | set the heading per mode and show the email |
| BUG-235 | P5 | P2 | Medium | `cnfm_pwd` has **no `inputType="textPassword"`** — the confirmation is typed in clear text | add `inputType` (**shared layout**) |
| BUG-236 | P7 | P2 | Medium | The `"Enter Password"` branch is unreachable — an empty password fails `isValidPassword` first | check emptiness before the regex (**shared chain** — coordinate) |
| BUG-237 | P8 | P2 | Medium | The "same as old password" check compares **old vs confirm**, not old vs new | compare `oldPasswordTxt` with `passwordTxt` (**shared chain**) |
| BUG-238 | P9 | P2 | Medium | The "same as old password" check is **inert** in this mode (`old_pwd` is GONE, so the value is `""`), so a user can reset to their current password | enforce server-side, or drop the check explicitly in this branch |
| BUG-239 | P10 | P2 | Medium | Neither `email` nor `otp` is validated; a missing `otp` would send a JSON null | guard the extras and bail out with a message |
| BUG-240 | P19 | P2 | Medium | Offline -> the method returns silently; no dialog, no toast — and the OTP may expire meanwhile | toast "No internet connection" |
| BUG-241 | P20 | P2 | Medium | `onResume` calls `dialog.dismiss()`, killing the spinner of a still-running request | remove the `onResume` override (**shared**) |
| BUG-242 | P23 | P2 | Medium | Passwords are sent in plain JSON while the app sets `usesCleartextTraffic="true"` globally | PLATFORM/BUILD_CONFIG to restrict cleartext |
| BUG-243 | P4 | P3 | Low | `cnfm_pwd_op` has no `passwordToggleEnabled`, unlike `new_pwd_op` | add for consistency (**shared layout**) |
| BUG-244 | P6 | P3 | Low | `otp_op` / `otp` are **dead views** — the OTP is passed as an extra, not typed here | delete (**shared layout** — coordinate with CHANGE_PASSWORD and FORGOT_PASSWORD) |
| BUG-245 | P12 | P3 | Low | `Log.e("ok1", ...)` debug logging left in the success path | remove |
| BUG-246 | P13 | P3 | Low | `UserPreferences` is instantiated by the shared `onCreate` but unused in this mode | harmless; tidy if the shared prologue is refactored |
| BUG-247 | P17 | P3 | Low | After a reset the user must find their own way back to Login and sign in; no auto-login and no guidance | ask the customer |
| BUG-248 | P16 | P3 | Cosmetic | `Gson` is imported but unused (only the commented-out code referenced it) | remove with the P14 fix |

#### `SPLASH` — module · `agents/AUTH/SPLASH.md` · 21 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-249 | S5 | P0 | Critical | ~~**`onFailure` does nothing**~~ — **FIXED 2026-10-08 (T-024)**: `onFailure` now calls `bailToLogin(R.string.splash_server_unreachable)` | — |
| BUG-250 | S12 | P0 | Critical | ~~Offline -> `isNetworkAvailable` false -> silent return, permanent hang at launch~~ — **FIXED 2026-10-08 (T-024)**: the `if` now has an `else { bailToLogin(R.string.splash_offline) }` | — |
| BUG-251 | S22 | P0 | Critical | **NEW (T-025, `G13`)** — ~~`getMyPermission` was fire-and-forget **after** routing, so the destination screen read an empty `permissionMap`; combined with the fail-open `Util.hasPermission` this granted every permission~~ — **FIXED 2026-10-08**: `getMyPermission(token) { … }` now takes a completion lambda, routing happens **inside** it, and any failure calls `bailToLogin` | — |
| BUG-252 | S6 | P1 | High | The empty-`userId` branch starts Login and calls `finish()` but **does not return**, so `Util.userId` is still assigned and the flow continues to `GET api/v1/users/` | add `return@observe` |
| BUG-253 | S11 | P1 | High | ~~A proven-invalid session is **not cleared**~~ — **FIXED 2026-10-08 (T-024)**: `bailToLogin` calls `deleteAuthToken()` + `deleteUserId()` and clears `Util.userId` / `Util.user` / `Util.clearPermissions()` before routing | — |
| BUG-254 | S13 | P1 | High | Four **continuous** LiveData observers; any later write to `token`/`userId`/`isNight`/`textSize` re-fires them and can launch `getMyDetails` again. *(Partially mitigated by the new `bailedOut` guard, which makes the failure path idempotent; the happy path can still re-fire.)* | one-shot reads (`first()`), or guard with a `hasBootstrapped` flag |
| BUG-255 | S20 | P1 | High | `getMyDetails` is **triplicated** across Splash, Login and MainActivity, with three different error behaviours. *(T-025 removed MainActivity's broken `SplashScreenActivity().getMyPermission(it)` call, but the duplication itself remains.)* | extract a shared `SessionBootstrap` (PM-level refactor) |
| BUG-256 | S23 | P1 | High | **NEW (T-024)** — `getBible()` throws `RuntimeException(e)` from inside a Retrofit callback on a malformed payload, and its `onFailure` only logs | handle the parse failure without rethrowing |
| BUG-257 | S1 | P2 | Medium | No splash theme / `windowBackground`, so a blank window shows before the layout inflates | add a themed `windowBackground` |
| BUG-258 | S2 | P2 | Medium | Hard-coded `#F0F3F9` and `#FFFFFF`; the splash ignores night mode despite a `DayNight` theme | move to `colors.xml` + `values-night` |
| BUG-259 | S4 | P2 | Medium | The API 31+ `addOnDrawListener { false }` block is a **no-op** (mis-port of `addOnPreDrawListener`); `androidx.core:core-splashscreen` is not used | adopt the official SplashScreen API, or delete the dead block |
| BUG-260 | S7 | P2 | Medium | `getMyDetails` depends on a **sibling observer** having already set `Util.userId`; the ordering is incidental, not guaranteed | read `userId` once and pass it explicitly |
| BUG-261 | S8 | P2 | Medium | `isWarrior` treats empty/null as **true** (`isNullOrEmpty() \|\| != "false"`) | compare explicitly with `"true"` (same as LOGIN L5) |
| BUG-262 | S10 | P2 | Medium | The unverified route omits `finish()`, leaving the splash on the back stack | `finish()` after `startActivity` |
| BUG-263 | S14 | P2 | Medium | No progress indicator during the bootstrap wait *(the artificial 2 s delay is gone, but a slow network still shows a static logo)* | add a spinner |
| BUG-264 | S15 | P2 | Medium | The Retrofit call is never cancelled; no `onDestroy` override at all | keep the `Call` and `cancel()` it |
| BUG-265 | S21 | P2 | Medium | Rotating during the bootstrap re-runs it from scratch, including a duplicate network call | guard with `savedInstanceState == null` |
| BUG-266 | S3 | P3 | Low | The `ImageView` has no `scaleType` and is stretched to `match_parent` | use `centerInside` / `centerCrop`, or a vector |
| BUG-267 | S17 | P3 | Low | ~~`Log.e("responseee", …)` / `Log.e("Splashscreen", …)` debug-grade tags~~ — **FIXED 2026-10-08 (T-024)** in `getMyDetails` / `getMyPermission` / `bailToLogin`; `getBible` and `readNotification` still use ad-hoc tags | finish the sweep in the two remaining methods |
| BUG-268 | S18 | P3 | Cosmetic | ~~`"Somthing Went Wrong"` is misspelled~~ — **FIXED here 2026-10-08 (T-024)**: replaced by `@string/splash_server_unreachable`. Still present in 5 other files | fix the remaining files app-wide (cross-team) |
| BUG-269 | S19 | P3 | Cosmetic | ~~The class name `SplashhScreenActivity` contains a typo (double "h")~~ — **FIXED upstream** on the v1.2.0 baseline: renamed to `SplashScreenActivity` | — |

### APPSHELL — 65 entries

#### `APPSHELL_LEAD` — team lead (rollups) · `agents/APPSHELL/APPSHELL_LEAD.md` · 10 entries

| Bug | Agent id | Issue | Affects module(s) | Risk |
|---|---|---|---|---|
| BUG-270 | AS-1 | The overflow menu + logout handler is **duplicated in 6+ activities** | MAIN_NAV (+ FEED, PROFILE) | inconsistent behaviour; 6 places to fix |
| BUG-271 | AS-2 | Theme and font changes **restart `MainActivity`** instead of recreating in place | SETTINGS | jarring, loses state |
| BUG-272 | AS-3 | `Util.user.isReviewState.toBoolean()` is unguarded in every menu copy | MAIN_NAV + copies | NPE after process death |
| BUG-273 | AS-4 | `TabAdapter` positions are hard-coded in **two** places (the adapter's `when` and `MainActivity`'s `addTab` order) | MAIN_NAV | easy to desync |
| BUG-274 | AS-5 | `TabAdapter.getItem` ends with `else -> b as Fragment` where `b` is `null` | MAIN_NAV | crash if a tab is added |
| BUG-275 | AS-6 | `MainActivity.onBackPressed` calls **`exitProcess(-1)`** | MAIN_NAV | kills the process, skips lifecycle |
| BUG-276 | AS-7 | `getMyDetails` copies #3 (MAIN_NAV) and #9 (ABOUT) | MAIN_NAV, ABOUT | drift (AUTH A4) |
| BUG-277 | AS-8 | Font size is applied **only** to post content/tags; the rest of the app ignores it | SETTINGS, THEMING | inconsistent accessibility |
| BUG-278 | AS-9 | Account deletion sits behind a plain list dialog with no re-authentication | SETTINGS | **destructive action, weak guard** |
| BUG-279 | AS-10 | `ExpandableView.java` is a custom view whose usage is not visible in `activity_about.xml` | ABOUT | possibly dead code |

#### `ABOUT` — module · `agents/APPSHELL/ABOUT.md` · 11 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-280 | AB5 | P1 | High | The always-true `\|\|` token guard; the logout branch is dead | use `&&` |
| BUG-281 | AB6 | P1 | High | A non-200 shows the user nothing | toast the error |
| BUG-282 | AB8 | P1 | High | The Retrofit call is never cancelled | cancel in `onDestroy` |
| BUG-283 | AB1 | P2 | Medium | `ExpandableView` lives in APPSHELL but its **only** consumer is FEED's `HomeAdapter` | documented; consider moving it next to its user |
| BUG-284 | AB2 | P2 | Medium | The class is named `AboutActivity` but shows user details, not app information | rename, or add real app info |
| BUG-285 | AB3 | P2 | Medium | Shows only 7 of `UserRslt`'s 28 fields; no avatar or warrior status | expand, or confirm intent |
| BUG-286 | AB4 | P2 | Medium | "Edit Profile" in the profile tab opens this **read-only** screen, which then offers another "Edit" | relabel, or route straight to `EditProfileActivity` |
| BUG-287 | AB7 | P2 | Medium | Unlike every other `getMyDetails` copy, this one does **not** refresh `Util.user` | align, or document deliberately |
| BUG-288 | AB9 | P2 | Medium | `getmyDetails` is **copy #9** of the same block | shared helper (PM-level) |
| BUG-289 | AB10 | P2 | Medium | Dates (`dateOfBirth`, `createdAt`) are shown without `Util.formatDate` formatting | format consistently |
| BUG-290 | AB11 | P3 | Low | No app version, licences or credits — what an "About" screen usually has | ask the customer |

#### `MAIN_NAV` — module · `agents/APPSHELL/MAIN_NAV.md` · 16 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-291 | AS-1 | P1 | High | The ~60-line overflow menu is duplicated into 5 other activities from here | extract a shared handler (PM-level) |
| BUG-292 | MN2 | P1 | High | `Util.user.isReviewState.toBoolean()` unguarded — NPE after process death | null-guard |
| BUG-293 | MN4 | P1 | High | `TabAdapter.getItem`'s `else -> b as Fragment` NPEs for any new position | handle all positions explicitly |
| BUG-294 | MN7 | P1 | High | On tab change `Util.player.stop()` is called but the player is **not released or nulled** | release, or delegate to the fragments |
| BUG-295 | MN9 | P1 | High | The rotation `ContentObserver` is **never unregistered** | unregister in `onDestroy` |
| BUG-296 | MN12 | P1 | High | `onBackPressed` calls **`exitProcess(-1)`**, killing the process | `finish()` or `moveTaskToBack(true)` |
| BUG-297 | MN3 | P2 | Medium | Tab order is defined in **two** places that must agree | one source of truth |
| BUG-298 | MN8 | P2 | Medium | The night-mode `when` has no `else` | add a default |
| BUG-299 | MN10 | P2 | Medium | The first-run dialog is shown **only to warriors**; the non-warrior branch is commented out | confirm intent with the customer |
| BUG-300 | MN13 | P2 | Medium | Permissions are requested only here, though ADD_POST and EDIT_PROFILE need them | request at point of use |
| BUG-301 | MN14 | P2 | Medium | Mixed `findViewById` and synthetics in one class | pick one |
| BUG-302 | MN1 | P3 | Low | `dialog.dismiss()` is called twice consecutively | delete one |
| BUG-303 | MN5 | P3 | Low | Inconsistent tab-tag casing | align |
| BUG-304 | MN6 | P3 | Low | Deprecated `getDrawable(...)` | `ContextCompat.getDrawable` |
| BUG-305 | MN11 | P3 | Low | `R.drawable.covre_pic` is misspelled | rename (THEMING) |
| BUG-306 | MN15 | P3 | Low | No `onSaveInstanceState` for the selected tab | save `currentItem` |

#### `SETTINGS` — module · `agents/APPSHELL/SETTINGS.md` · 14 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-307 | ST10 | P1 | High | **Account deletion needs only a Yes tap** — no re-authentication or typed confirmation | require the password, or a typed confirmation |
| BUG-308 | ST11 | P1 | High | Deletion failure shows the user **nothing** | toast the error |
| BUG-309 | ST12 | P1 | High | Deletion clears DataStore but leaves `Util.user` / `Util.userId` populated | clear global state too |
| BUG-310 | ST13 | P1 | High | `Util.user.isReviewState.toBoolean()` unguarded in the menu copy | null-guard |
| BUG-311 | ST14 | P1 | High | The overflow menu is duplicated here too (AS-1) | shared handler (PM-level) |
| BUG-312 | ST2 | P2 | Medium | Options are matched by **display string** rather than index or enum | switch on the index |
| BUG-313 | ST3 | P2 | Medium | Theme and font changes **restart `MainActivity`** | `recreate()` / live `AppCompatDelegate` update |
| BUG-314 | ST4 | P2 | Medium | Font size affects **only** post content and tags | apply app-wide, or rename the setting |
| BUG-315 | ST5 | P2 | Medium | Neither picker shows the current selection | use `setSingleChoiceItems` |
| BUG-316 | ST6 | P2 | Medium | Theme is written here but applied in `MainActivity` — split responsibility | documented; centralise if refactoring |
| BUG-317 | ST1 | P3 | Low | No app version, no about link, no notification or privacy settings | ask the customer |
| BUG-318 | ST7 | P3 | Low | A large commented-out night-mode resolution block | delete |
| BUG-319 | ST8 | P3 | Low | `Log.e("mode", ...)` debug logging | remove |
| BUG-320 | ST9 | P3 | Low | The `DEFAULT` branch writes in the opposite order to the others | align |

#### `THEMING` — module · `agents/APPSHELL/THEMING.md` · 14 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-321 | TH1 | P1 | High | **`black` and `white` are inverted in night mode** — the names lie | rename to `text_primary` / `surface`, app-wide |
| BUG-322 | TH9 | P1 | High | Only 26 strings; nearly all UI text is hard-coded — the app is **not localisable** | extract strings (a very large, cross-team task) |
| BUG-323 | TH2 | P2 | Medium | No error/success/warning colours; screens use `Color.RED` in code | add semantic colours |
| BUG-324 | TH3 | P2 | Medium | No ActionBar, so all 21 screens hand-roll a header | a shared header layout/`include` |
| BUG-325 | TH4 | P2 | Medium | `windowIsTranslucent=true` app-wide | verify it is needed |
| BUG-326 | TH5 | P2 | Medium | `colorPrimaryDark` is `@color/black`, i.e. white at night | use an explicit colour |
| BUG-327 | TH6 | P2 | Medium | `menuStyle` forces LTR despite `supportsRtl="true"` | remove, or justify |
| BUG-328 | TH12 | P2 | Medium | Sizes are hard-coded in layouts rather than in `dimens.xml` | centralise |
| BUG-329 | TH13 | P2 | Medium | Text sizes declared in **`dp`** rather than `sp` in several layouts | switch to `sp` |
| BUG-330 | TH7 | P3 | Low | `styles.xml` and `themes.xml` split two styles across two files | merge |
| BUG-331 | TH8 | P3 | Low | Both `app_name` and `Product_name` hold "SalvationLamb" | keep one |
| BUG-332 | TH10 | P3 | Low | `values-night/dimens.xml` exists though dimensions do not vary by theme | delete |
| BUG-333 | TH11 | P3 | Low | Tablet breakpoints exist with no tablet layouts | implement or remove |
| BUG-334 | TH14 | P3 | Low | `covre_pic` is misspelled | rename (touches MAIN_NAV) |

### FEED — 104 entries

#### `FEED_LEAD` — team lead (rollups) · `agents/FEED/FEED_LEAD.md` · 13 entries

| Bug | Agent id | Issue | Affects module(s) | Risk |
|---|---|---|---|---|
| BUG-335 | F-1 | `page`/`type` are magic strings with no constants, compared via `contentEquals` | HOME_FEED | silent mis-render |
| BUG-336 | F-2 | List endpoints use `results` + `count`, unlike AUTH's `result` | all | empty lists, no error |
| BUG-337 | F-3 | `HomeAdapter` (680 lines) holds **all** post actions — like, fav, follow, delete, share, playback — instead of the screen | HOME_FEED | untestable, cross-team coupling |
| BUG-338 | F-4 | The always-true `!isEmpty \|\| !equals("null") \|\| !isNullOrEmpty` token guard appears **5 times** in `HomeFragment` alone | HOME_FEED | dead logout branches |
| BUG-339 | F-5 | A new `authToken` observer is registered per call, in 5 methods | HOME_FEED | duplicate requests |
| BUG-340 | F-6 | `dialog.hide()` is used instead of `dismiss()` in FEED callbacks | HOME_FEED | leaked window on config change |
| BUG-341 | F-7 | Error branches are **commented out**; failures just show the "no data" view | HOME_FEED | indistinguishable empty vs failed |
| BUG-342 | F-8 | Pull-to-refresh calls `detach().attach()` on the fragment — a full recreate | HOME_FEED | jank, duplicate calls |
| BUG-343 | F-9 | A new `OnScrollListener` is added on **every** page load, never removed | HOME_FEED | listeners accumulate |
| BUG-344 | F-10 | `Util.player` is a static `MediaPlayer` shared by every card | HOME_FEED, MEDIA | leaks, overlapping audio |
| BUG-345 | F-11 | 14 reaction strings are addressed by `R.id.reactN` -> `R.string.reactN` pairs with no data structure | HOME_FEED | fragile |
| BUG-346 | F-12 | `Posts.likesCount` / `shareCount` are `String` | all | conversion crashes |
| BUG-347 | F-13 | `getMyDetails` duplicated here as well — the **4th** copy | HOME_FEED | drift (AUTH A4) |

#### `ADD_POST` — module · `agents/FEED/ADD_POST.md` · 24 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-348 | AP4 | P1 | High | Permissions are **checked but never requested** — the user is bounced to settings | call `requestPermissions` first |
| BUG-349 | AP5 | P1 | High | `MediaStore.Images.Media.getBitmap` (deprecated) decodes on the **main thread** | `ImageDecoder` off the main thread |
| BUG-350 | AP6 | P1 | High | The camera path stores only the **thumbnail** from `extras["data"]` | use a `FileProvider` and the full-size file |
| BUG-351 | AP11 | P1 | High | Base64 is sent inside JSON (`Base64.DEFAULT` adds newlines); no size limit | multipart upload (with NETWORK) |
| BUG-352 | AP14 | P1 | High | The always-true token guard; the logout branch is dead | use `&&` |
| BUG-353 | AP15 | P1 | High | `Log.e("data", data.toString())` prints the **entire base64 image** to logcat | remove the log |
| BUG-354 | AP18 | P1 | High | A non-200 shows **no message**; error parsing is commented out | restore and toast |
| BUG-355 | AP22 | P1 | High | Rotation loses the picked image and the post type | save to `onSaveInstanceState` |
| BUG-356 | AP1 | P2 | Medium | No warrior check on the screen itself — it is reachable by intent | verify `Util.isWarrior` in `onCreate` |
| BUG-357 | AP2 | P2 | Medium | Image/Video visibility handling is asymmetric | handle both in the listener |
| BUG-358 | AP3 | P2 | Medium | `image_btn` has two listeners, so selecting the radio immediately opens the picker | pick one trigger |
| BUG-359 | AP12 | P2 | Medium | A video post is accepted with an **empty or invalid URL** | validate the URL |
| BUG-360 | AP13 | P2 | Medium | `post_btn` is not re-enabled in `onFailure` | re-enable |
| BUG-361 | AP17 | P2 | Medium | Success clears only `title` and `content`, leaving tags/url/image | reset everything |
| BUG-362 | AP19 | P2 | Medium | A stray `dialog.dismiss()` outside the observer dismisses the spinner early | remove it |
| BUG-363 | AP20 | P2 | Medium | `resultCode` is ignored in `onActivityResult` | check `RESULT_OK` |
| BUG-364 | AP21 | P2 | Medium | `onResume` dismisses the dialog of an in-flight request | remove the override |
| BUG-365 | AP7 | P3 | Low | `data.extras!!["data"]` double force-unwrap | null-safe |
| BUG-366 | AP8 | P3 | Low | Dead compression in the camera path (quality 90, result discarded) | delete |
| BUG-367 | AP9 | P3 | Low | Permission dialog buttons are inverted ("cancel" positive, "settings" negative) | swap |
| BUG-368 | AP10 | P3 | Low | Permission copy mentions *"profile picture"* on the post screen | reword |
| BUG-369 | AP16 | P3 | Low | Body key `image` vs model field `picture` | document only |
| BUG-370 | AP23 | P3 | Low | Deprecated `startActivityForResult` / `onActivityResult` | Activity Result API |
| BUG-371 | AP24 | P3 | Low | Returns to `MainActivity` instead of the feed with a result | `setResult` + `finish()` |

#### `FAVORITES` — module · `agents/FEED/FAVORITES.md` · 11 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-372 | FV2 | P1 | High | `menu` is resolved via a **synthetic import of another layout** | use `findViewById` |
| BUG-373 | FV7 | P1 | High | `Util.user.isReviewState.toBoolean()` unguarded — NPE after process death | null-guard `Util.user` |
| BUG-374 | FV10 | P1 | High | The fragment is `replace()`d with no `savedInstanceState` guard — every rotation rebuilds it and re-fetches | guard the transaction |
| BUG-375 | FV11 | P1 | High | The ~60-line menu block is duplicated here too (team issue VL3) | shared handler (PM-level) |
| BUG-376 | FV4 | P2 | Medium | No screen title or heading — the user cannot tell they are in Favorites | add a title |
| BUG-377 | FV5 | P2 | Medium | `logout` is hidden but its full handler remains as dead code | remove the handler, or show the item |
| BUG-378 | FV6 | P2 | Medium | Hiding `logout` here but not on sibling screens is undocumented and inconsistent | decide a rule with APPSHELL |
| BUG-379 | FV3 | P3 | Low | The logo navigates to `MainActivity` without `finish()` | `finish()` |
| BUG-380 | FV8 | P3 | Low | `UserPreferences` is instantiated but effectively unused | delete with FV5 |
| BUG-381 | FV9 | P3 | Low | A `SpotsDialog` is built and never used | delete |
| BUG-382 | FV1 | P3 | Cosmetic | The fragment variable is named `viewProfile` | rename |

#### `HOME_FEED` — module · `agents/FEED/HOME_FEED.md` · 19 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-383 | H5 | P1 | High | Audio uses the static `Util.player` plus a polling `Runnable` not tied to the card lifecycle | per-holder player, release on recycle |
| BUG-384 | H7 | P1 | High | 5 `authToken` observers, one per method, re-firing on every DataStore write | one-shot read |
| BUG-385 | H8 | P1 | High | The always-true token guard appears 5x; the logout branch is dead | use `&&` |
| BUG-386 | H9 | P1 | High | Error parsing is commented out — a failed load is indistinguishable from an empty feed | restore it and show a distinct error state |
| BUG-387 | H12 | P1 | High | A new `OnScrollListener` is added on **every** page load, never removed | add it once in `onCreateView` |
| BUG-388 | H14 | P1 | High | The adapter does **network I/O** and owns a `UserPreferences` + dialog; 680 lines mixing view binding, playback and 5 API calls | move calls to the fragment/ViewModel |
| BUG-389 | H17 | P1 | High | Retrofit calls are never cancelled; callbacks touch views after detach | cancel in `onDestroyView` |
| BUG-390 | H3 | P2 | Medium | No `else` in the `page` visibility chain | add a default |
| BUG-391 | H4 | P2 | Medium | No `else` in the `post.type` `when` | log/handle unknown types |
| BUG-392 | H6 | P2 | Medium | Adding a reaction requires editing 3 places; the handler is a 14-branch `when` | drive it from a list |
| BUG-393 | H10 | P2 | Medium | `dialog.hide()` instead of `dismiss()` | use `dismiss()` |
| BUG-394 | H11 | P2 | Medium | `page` starts at 1 but the empty check tests `page == 0`, so a genuinely empty first page never shows `no_data` | align the values |
| BUG-395 | H15 | P2 | Medium | The three state maps are passed **empty** to the adapter and filled asynchronously | pass immutable data after load |
| BUG-396 | H16 | P2 | Medium | Pull-to-refresh does `detach().attach()` via the deprecated `requireFragmentManager()` | re-fetch the data instead |
| BUG-397 | H18 | P2 | Medium | `type` is read with `arguments?.get("type").toString()`, so a missing arg becomes the string `"null"` | same trap as AUTH A13 |
| BUG-398 | H19 | P2 | Medium | `getMyDetails` here is the **4th** copy of the same block | shared helper (PM-level) |
| BUG-399 | H1 | P3 | Low | `HomeFragment` imports `kotlinx.android.synthetic.main.activity_main.*` — the **wrong layout**, unused | delete the import |
| BUG-400 | H13 | P3 | Low | `Integer.parseInt(get("count").toString())` is fragile | use `asInt` |
| BUG-401 | H2 | P3 | Cosmetic | `Delete_btn` is capitalised, unlike every other id | rename |

#### `IMAGE_DETAIL` — module · `agents/FEED/IMAGE_DETAIL.md` · 11 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-402 | ID4 | P1 | High | A null/invalid URL shows a **blank screen** with no message and no way to tell it failed | add Picasso `placeholder()` / `error()` and a fallback |
| BUG-403 | ID5 | P1 | High | **Zoom without pan** — once magnified, the user cannot move around the image, making zoom nearly useless | add translation on drag, or use `PhotoView` |
| BUG-404 | ID3 | P2 | Medium | No close button, no toolbar, no loading indicator — Back is the only exit | add a close affordance |
| BUG-405 | ID6 | P2 | Medium | No double-tap to zoom/reset | add a `GestureDetector` |
| BUG-406 | ID7 | P2 | Medium | Rotation resets the zoom level | save `mScaleFactor` in `onSaveInstanceState` |
| BUG-407 | ID10 | P2 | Medium | No downsampling — a large image is decoded at full size | `fit().centerInside()` or explicit sizing |
| BUG-408 | ID1 | P3 | Low | `preview_image.xml` is widely assumed to belong here but is actually used by `MainActivity` | documented; ownership stays with APPSHELL |
| BUG-409 | ID2 | P3 | Low | The extra is called **`profilePic`** even when carrying a post image | rename to `imageUrl` — **two-team change** (HOME_FEED + PROFILE) |
| BUG-410 | ID8 | P3 | Low | `Picasso.with(...)` is deprecated | upgrade with BUILD_CONFIG |
| BUG-411 | ID9 | P3 | Low | `imageView!!` / `scaleGestureDetector!!` force-unwraps instead of `lateinit` | use `lateinit var` |
| BUG-412 | ID11 | P3 | Low | No immersive/full-screen flags, so system bars overlay the image | consider immersive mode |

#### `VIEW_LIKES` — module · `agents/FEED/VIEW_LIKES.md` · 14 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-413 | VL1 | P1 | High | Imports **`activity_edit_profile.*`** synthetics to resolve `menu`, coupling this screen to another layout | bind `menu` with `findViewById` |
| BUG-414 | VL3 | P1 | High | The ~60-line overflow menu is **copy-pasted across 6+ screens**, each owning its own logout implementation | extract a shared menu handler (PM-level, cross-team) |
| BUG-415 | VL4 | P1 | High | `Util.user.isReviewState.toBoolean()` with no null check — NPE after process death | null-guard `Util.user` |
| BUG-416 | VL6 | P1 | High | A non-200 has **no `else` branch** — no data, no empty state, no message; the screen just stays blank | add an error state |
| BUG-417 | VL5 | P2 | Medium | A brand-new adapter and layout manager are created on every load instead of updating the existing one | reuse + `notifyDataSetChanged` |
| BUG-418 | VL7 | P2 | Medium | `getStringExtra("postId").toString()` yields `"null"` for a missing extra | validate the extra |
| BUG-419 | VL8 | P2 | Medium | `react_txt` shows the **raw** reaction string with no icon or grouping | map to an icon/label |
| BUG-420 | VL9 | P2 | Medium | The Retrofit call is never cancelled in `onDestroy` | cancel |
| BUG-421 | VL10 | P3 | Low | `ViewLikesAdapter` uses a **secondary constructor** with `lateinit` fields instead of primary-constructor params | use a primary constructor |
| BUG-422 | VL11 | P3 | Low | `Picasso.with(context)` — deprecated API (Picasso 2.5.2) | upgrade |
| BUG-423 | VL12 | P3 | Low | `logo` navigates to `MainActivity` without `finish()` | `finish()` |
| BUG-424 | VL13 | P3 | Low | No pagination — all reactions load at once | paginate if lists grow |
| BUG-425 | VL14 | P3 | Cosmetic | Commented-out night-mode code left in the menu handler | delete |
| BUG-426 | VL15 | P3 | Cosmetic | Method named `getALlLikes` (capital L) | rename |

#### `VIEW_POST` — module · `agents/FEED/VIEW_POST.md` · 12 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-427 | VP0 | P0 | Critical | **All 14 `findViewById` calls run in the constructor, before `setContentView`** — guaranteed NPE on launch | move them into `onCreate` after `setContentView` |
| BUG-428 | VP1 | P1 | High | Only 4 of 14 views are populated; like, share, fav and react have **no listeners** | implement, or remove the controls |
| BUG-429 | VP5 | P1 | High | `onFailure` never dismisses the non-cancelable dialog — permanent spinner | dismiss in both callbacks |
| BUG-430 | VP8 | P1 | High | **No caller anywhere** — the screen is unreachable dead code | wire it up from `HomeAdapter`, or delete it |
| BUG-431 | VP2 | P2 | Medium | The layout duplicates `child_post.xml` ids without the behaviour | reuse the card, or delete |
| BUG-432 | VP4 | P2 | Medium | Error logging uses the tag `"fail fav"` and prints `errorBody().toString()` (an object reference) | use `.string()` and a correct tag |
| BUG-433 | VP6 | P2 | Medium | The (correct) `&&` guard has **no `else`**, so a missing token silently does nothing | add the standard logout branch |
| BUG-434 | VP7 | P2 | Medium | `intent.extras!!.get("postId").toString()` — crashes with no extras, yields `"null"` with a missing key | `intent.getStringExtra("postId")` + validation |
| BUG-435 | VP11 | P2 | Medium | No image/audio/video handling at all, unlike the feed card | port `when (post.type)` |
| BUG-436 | VP3 | P3 | Low | The logo navigates to `MainActivity` without `finish()` | `finish()` |
| BUG-437 | VP9 | P3 | Low | `tags` is shown raw, not via `HomeAdapter.getTags()`'s `#tag` formatting | reuse the helper |
| BUG-438 | VP10 | P3 | Low | `"${post.likesCount} people reacts"` — grammar, and no singular form | pluralise |

### PROFILE — 84 entries

#### `PROFILE_LEAD` — team lead (rollups) · `agents/PROFILE/PROFILE_LEAD.md` · 15 entries

| Bug | Agent id | Issue | Affects module(s) | Risk |
|---|---|---|---|---|
| BUG-439 | F-1 | `ProfileFragment` serves two screens via `who`, with the adapter page derived as `if (who == "me") "profile" else "OtherProfile"` — no constants | MY_PROFILE, VIEW_PROFILE | silent mis-render |
| BUG-440 | F-2 | The in-profile "Edit Profile" button opens **`AboutActivity`**, not `EditProfileActivity` | MY_PROFILE | confusing navigation |
| BUG-441 | F-3 | `ProfileFragment.setUserVisibleHint` calls `detach().attach()` on **every** tab selection — a full recreate plus re-fetch, using deprecated APIs | MY_PROFILE | jank, duplicate calls |
| BUG-442 | F-4 | `EditProfileActivity` is **738 lines** mixing a 14-field form, 3 cascading spinners, crop/camera and 4 API calls | EDIT_PROFILE | unmaintainable |
| BUG-443 | F-5 | `MY_PROFILE` passes **two empty `HashMap()`s** for follow/fav state, so those states never render in profiles | MY_PROFILE | wrong UI state |
| BUG-444 | F-6 | The always-true `\|\|` token guard appears in every PROFILE network method | all | dead logout branches |
| BUG-445 | F-7 | `Util.user.isReviewState.toBoolean()` is unguarded in the duplicated overflow menus | FOLLOWERS, EDIT_PROFILE | NPE after process death |
| BUG-446 | F-8 | `getmyDetails` here is the 5th/6th copy of the same block | MY_PROFILE, EDIT_PROFILE | drift |
| BUG-447 | F-9 | `intent.extras!!.get(...)` and `arguments?.get(...).toString()` yield the literal `"null"` for missing values | all | malformed requests |
| BUG-448 | F-10 | The overflow-menu block is duplicated here too (FEED VL3) | FOLLOWERS, EDIT_PROFILE, VIEW_PROFILE | 6+ copies app-wide |
| BUG-449 | F-11 | **Follow state is unreliable app-wide**: `MY_PROFILE` passes empty maps to `HomeAdapter` (MP4) while `FollowAdapter` consults its map with the **wrong key** (FL7), so every row shows the same follow label | MY_PROFILE, FOLLOWERS + **FEED** | one PM-coordinated fix |
| BUG-450 | F-12 | `EditProfileActivity` **disables StrictMode's VM policy** to pass a `file://` URI to the cropper | EDIT_PROFILE | security / correctness |
| BUG-451 | F-13 | `activity_edit_profile.xml` shares **14 ids** with `activity_register.xml`, coupling PROFILE to AUTH's synthetic imports | EDIT_PROFILE | cross-team breakage |
| BUG-452 | F-14 | `FollowerActivity` starts loading **before** binding its views, risking `UninitializedPropertyAccessException` | FOLLOWERS | crash |
| BUG-453 | F-15 | `ViewProfileActivity` force-unwraps `userId!!`, so launching it without the extra crashes | VIEW_PROFILE | crash |

#### `EDIT_PROFILE` — module · `agents/PROFILE/EDIT_PROFILE.md` · 23 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-454 | EP2 | P1 | High | `onCreate` **disables StrictMode's VM policy** to allow a `file://` URI | use a `FileProvider` and remove the override |
| BUG-455 | EP4 | P1 | High | State and city listeners compare against **`"Country"`**, so the `"State"`/`"City"` placeholders can be submitted as real values | compare against the right placeholder |
| BUG-456 | EP12 | P1 | High | The camera path captures only a **thumbnail** | full-size via `FileProvider` |
| BUG-457 | EP13 | P1 | High | Base64 avatar inside JSON at quality 100, no size limit | multipart (with NETWORK) |
| BUG-458 | EP17 | P1 | High | The always-true `\|\|` token guard in every authenticated method; logout branches dead | use `&&` |
| BUG-459 | EP19 | P1 | High | `getCountries()` and `getMyDetails()` race; the state spinner can be filled before its country list loads | chain the calls |
| BUG-460 | EP20 | P1 | High | Rotation loses the picked image and spinner state | `onSaveInstanceState` |
| BUG-461 | EP21 | P1 | High | Retrofit calls are never cancelled | cancel in `onDestroy` |
| BUG-462 | EP23 | P1 | High | The ~60-line overflow menu is duplicated here too | shared handler (PM-level) |
| BUG-463 | EP1 | P2 | Medium | `findViewById` **and** synthetics mixed in one class | pick one |
| BUG-464 | EP5 | P2 | Medium | Country/state **names** are passed where ids are expected | confirm with the backend |
| BUG-465 | EP6 | P2 | Medium | Changing the country does not reset state/city | clear dependents |
| BUG-466 | EP7 | P2 | Medium | `lname`, `address`, `pincode`, `language`, geo and gender are **never validated** | extend `doValidation` |
| BUG-467 | EP9 | P2 | Medium | DOB round-trips through two format conversions for change detection | compare normalised values |
| BUG-468 | EP11 | P2 | Medium | Gender comparison ignores **`other`**, which exists in this layout | handle all three |
| BUG-469 | EP15 | P2 | Medium | `===` referential comparison on boxed `Int` request codes | use `==` |
| BUG-470 | EP16 | P2 | Medium | The "admin approval" confirmation dialog is commented out | confirm intent with the customer |
| BUG-471 | EP18 | P2 | Medium | A new `authToken` observer per call | one-shot read |
| BUG-472 | EP22 | P2 | Medium | `exported="true"` with no intent-filter | BUILD_CONFIG |
| BUG-473 | EP3 | P3 | Low | Gender `other` is present here but commented out in Register | align the two screens |
| BUG-474 | EP8 | P3 | Low | Returns `"SUCCESS"` while Register returns `"success"` | share a constant |
| BUG-475 | EP10 | P3 | Low | 10 `text!!` force-unwraps in `isDataChanged` | null-safe |
| BUG-476 | EP14 | P3 | Low | Request/result codes used as **log tags** | fix the tags |

#### `FOLLOWERS` — module · `agents/PROFILE/FOLLOWERS.md` · 20 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-477 | FL2 | P1 | High | Two bare `if`s with no `else`/fallback — an unknown `page` loads nothing, silently | `when` with an `else` |
| BUG-478 | FL3 | P1 | High | `menu` resolved via a **synthetic import of another layout** | use `findViewById` |
| BUG-479 | FL6 | P1 | High | Two consecutive contradictory blocks set the label; the first is **dead code** | delete the first, fix the second |
| BUG-480 | FL7 | P1 | High | The follow button checks **`Util.userId`** instead of the row's `follow.id`, so **every row shows the same label** | key the map lookup on `follow.id` |
| BUG-481 | FL8 | P1 | High | The surviving check is **inverted** relative to the map's meaning | invert |
| BUG-482 | FL11 | P1 | High | The always-true `\|\|` guard in both list methods | use `&&` (the adapter already does) |
| BUG-483 | FL13 | P1 | High | `Util.user.isReviewState.toBoolean()` unguarded — NPE after process death | null-guard |
| BUG-484 | FL14 | P1 | High | List loading starts **before** `findViewById` binds `lists`/`nodata` — `UninitializedPropertyAccessException` on a fast response | bind views first |
| BUG-485 | FL16 | P1 | High | The ~60-line overflow menu is duplicated here too | shared handler (PM-level) |
| BUG-486 | FL1 | P2 | Medium | `getAllFollowers` / `getAllFollowing` are near-identical copies | one method + a path parameter |
| BUG-487 | FL5 | P2 | Medium | **No title** — followers and following look identical | add a per-mode title |
| BUG-488 | FL9 | P2 | Medium | Both button branches call the identical `follow(...)`, so the `if/else` is pointless | collapse it |
| BUG-489 | FL10 | P2 | Medium | `follow()` logs the error properly but still shows the user nothing | toast on failure |
| BUG-490 | FL12 | P2 | Medium | The activity's error branches are commented out — a failure looks like an empty list | restore |
| BUG-491 | FL15 | P2 | Medium | `intent.extras!!.get(...).toString()` crashes / yields `"null"` | validate the extras |
| BUG-492 | FL17 | P2 | Medium | A new adapter is created on every load rather than updating the existing one | reuse + notify |
| BUG-493 | FL20 | P2 | Medium | Button labels are the lower-case literals `"follow"`/`"unfollow"`, compared by **text** rather than state | track state, not labels |
| BUG-494 | FL4 | P3 | Low | `night_mode` and `day_mode` exist in the layout but are **never referenced** | remove, or wire to THEMING |
| BUG-495 | FL18 | P3 | Low | No pagination — the whole list loads at once | paginate if needed |
| BUG-496 | FL19 | P3 | Low | `Picasso.with(context)` deprecated | upgrade |

#### `MY_PROFILE` — module · `agents/PROFILE/MY_PROFILE.md` · 16 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-497 | MP4 | P1 | High | Two **empty `HashMap()`s** are passed for follow and fav state, so those states never render | build and pass the real maps |
| BUG-498 | MP6 | P1 | High | 5 `authToken` observers, one per method | one-shot read |
| BUG-499 | MP7 | P1 | High | The always-true `\|\|` token guard; all 5 logout branches are dead | use `&&` |
| BUG-500 | MP8 | P1 | High | Error parsing commented out — a failure looks like an empty profile | restore and show an error |
| BUG-501 | MP10 | P1 | High | `getallLikes` runs **before** the layout is inflated | move it after `inflate` |
| BUG-502 | MP11 | P1 | High | `setUserVisibleHint` does `detach().attach()` on **every** tab visit — full recreate + 5 re-fetches, via a deprecated API | load once; use `setMaxLifecycle` |
| BUG-503 | MP13 | P1 | High | Retrofit calls are never cancelled; callbacks touch views after detach | cancel in `onDestroyView` |
| BUG-504 | MP1 | P2 | Medium | `profile_about` exists in the layout but is **never populated** | bind it or remove it |
| BUG-505 | MP2 | P2 | Medium | "Edit Profile" opens **`AboutActivity`**, not `EditProfileActivity` | route directly, or rename the button |
| BUG-506 | MP5 | P2 | Medium | The adapter argument order differs from `HomeFragment`'s call | named arguments |
| BUG-507 | MP9 | P2 | Medium | `dialog.hide()` instead of `dismiss()` | use `dismiss()` |
| BUG-508 | MP12 | P2 | Medium | `arguments?.get(...).toString()` yields `"null"` when missing | `requireArguments().getString(...)` |
| BUG-509 | MP15 | P2 | Medium | `page` starts at 0 here but at 1 in `HomeFragment` — inconsistent paging | align with HOME_FEED |
| BUG-510 | MP3 | P3 | Low | `role == "admin"` is a magic string; the suffix is concatenated, not localised | constants + string resources |
| BUG-511 | MP14 | P3 | Low | `posts_linear` has no listener while the other two counts do | add or remove |
| BUG-512 | MP16 | P3 | Low | No pull-to-refresh, unlike the home feed | add if wanted |

#### `VIEW_PROFILE` — module · `agents/PROFILE/VIEW_PROFILE.md` · 10 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-513 | VPR1 | P1 | High | `menu` resolved via a **synthetic import of another layout** | use `findViewById` |
| BUG-514 | VPR5 | P1 | High | `Util.user.isReviewState.toBoolean()` unguarded — NPE after process death | null-guard |
| BUG-515 | VPR7 | P1 | High | `userId!!` force-unwrap — launching without the extra **crashes immediately** | validate and finish gracefully |
| BUG-516 | VPR8 | P1 | High | The fragment is `replace()`d with no `savedInstanceState` guard — every rotation re-runs 5 network calls | guard the transaction |
| BUG-517 | VPR9 | P1 | High | The ~60-line overflow menu is duplicated here too | shared handler (PM-level) |
| BUG-518 | VPR3 | P2 | Medium | No title or subject indication before the fragment loads | show the name in the header |
| BUG-519 | VPR4 | P2 | Medium | "Edit Profile" is offered while viewing someone else's profile, and edits your own | hide it here, or relabel |
| BUG-520 | VPR10 | P2 | Medium | No "follow" action at the screen level — following is only possible from a post card | ask the customer |
| BUG-521 | VPR2 | P3 | Low | The logo navigates to `MainActivity` without `finish()` | `finish()` |
| BUG-522 | VPR6 | P3 | Low | A `SpotsDialog` is built and never shown | delete |

### MEDIA — 92 entries

#### `MEDIA_LEAD` — team lead (rollups) · `agents/MEDIA/MEDIA_LEAD.md` · 10 entries

| Bug | Agent id | Issue | Affects module(s) | Risk |
|---|---|---|---|---|
| BUG-523 | M-1 | `AdminAudioFragment` and `AdminVideoFragment` are **~390-line near-duplicates** differing only by endpoint and log tags | ADMIN_AUDIO, ADMIN_VIDEO | every fix must be made twice |
| BUG-524 | M-2 | `FileAdapter` treats **anything that is not `"folder"` as a PDF** — images, audio and video all open in the PDF viewer | FILE_LIST, FILES_BROWSER | broken files UX |
| BUG-525 | M-3 | `PdfActivity2` downloads over a raw `HttpURLConnection` inside a deprecated `AsyncTask`, writing to a hard-coded `"testthreepdf"` folder on external storage | PDF_VIEWER | leaks, permission failures, litter |
| BUG-526 | M-4 | `WebViewActivity` enables **JavaScript and DOM storage** (the code comment even notes the XSS risk) | WEBVIEW | security |
| BUG-527 | M-5 | `Util.player` is a static `MediaPlayer` shared by FEED and both admin feeds | ADMIN_AUDIO, ADMIN_VIDEO | overlapping audio, leaks |
| BUG-528 | M-6 | `FilesFragment` and `FileListActivity` duplicate `getFilesAndFolder` almost exactly | FILES_BROWSER, FILE_LIST | drift |
| BUG-529 | M-7 | The always-true `\|\|` token guard appears in every MEDIA network method | all | dead logout branches |
| BUG-530 | M-8 | `getMyDetails` copies 7 and 8 live here | ADMIN_AUDIO, ADMIN_VIDEO | drift (AUTH A4) |
| BUG-531 | M-9 | `PdfActivity2` extends **`Activity`**, not `AppCompatActivity`, and is the only screen setting `FLAG_SECURE` | PDF_VIEWER | inconsistent theming/behaviour |
| BUG-532 | M-10 | Admin feeds pass page `"home"`, so curated content shows follow/fav buttons for admin authors | ADMIN_AUDIO, ADMIN_VIDEO | questionable UX — confirm with customer |

#### `ADMIN_AUDIO` — module · `agents/MEDIA/ADMIN_AUDIO.md` · 13 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-533 | AA2 | P1 | High | 5 `authToken` observers, one per method | one-shot read |
| BUG-534 | AA3 | P1 | High | The always-true `\|\|` token guard; all 5 logout branches dead | use `&&` |
| BUG-535 | AA4 | P1 | High | Error parsing commented out — a failure looks like an empty feed | restore and show an error |
| BUG-536 | AA6 | P1 | High | A new `OnScrollListener` is added on **every** page load | add it once |
| BUG-537 | AA7 | P1 | High | Relies on `onPause` to release the shared static `Util.player`, which the pager may not call when expected | per-card player, or explicit tab-change handling |
| BUG-538 | AA8 | P1 | High | Retrofit calls are never cancelled | cancel in `onDestroyView` |
| BUG-539 | M-1 | P1 | High | This file is a **~390-line near-duplicate** of `AdminVideoFragment` | extract a shared base fragment taking the endpoint (PM-level, two agents) |
| BUG-540 | AA5 | P2 | Medium | `dialog.hide()` instead of `dismiss()` | use `dismiss()` |
| BUG-541 | M-8 | P2 | Medium | `getMyDetails` copy #7 | shared helper (PM-level) |
| BUG-542 | M-10 | P2 | Medium | Page `"home"` shows follow/fav buttons on curated admin content | confirm the intended UX, then pass a new page value (needs FEED) |
| BUG-543 | AA1 | P3 | Low | An unused `getInstance()` companion (the video twin has none) | use it or delete it |
| BUG-544 | AA9 | P3 | Low | No pull-to-refresh, unlike the home feed | add if wanted |
| BUG-545 | AA10 | P3 | Low | No screen title — the tab is identified only by its nav icon | confirm with APPSHELL |

#### `ADMIN_VIDEO` — module · `agents/MEDIA/ADMIN_VIDEO.md` · 14 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-546 | AV4 | P1 | High | 5 `authToken` observers, one per method | one-shot read |
| BUG-547 | AV5 | P1 | High | A new `OnScrollListener` per page load | add it once |
| BUG-548 | AV6 | P1 | High | The always-true `\|\|` token guard; all 5 logout branches dead | use `&&` |
| BUG-549 | AV7 | P1 | High | Error parsing commented out — a failure looks like an empty feed | restore and show an error |
| BUG-550 | AV9 | P1 | High | The YouTube player is never released on tab change — playback/WebView can outlive the screen | release in `onPause` (needs FEED, since the view lives in the adapter) |
| BUG-551 | AV10 | P1 | High | Retrofit calls are never cancelled | cancel in `onDestroyView` |
| BUG-552 | M-1 | P1 | High | A **~390-line near-duplicate** of `AdminAudioFragment` | shared base fragment (PM-level, two agents) |
| BUG-553 | AV2 | P2 | Medium | `Util.getVideo` hard-codes a **second host** independent of the API base URL | move to NETWORK config (PLATFORM) |
| BUG-554 | AV8 | P2 | Medium | `dialog.hide()` instead of `dismiss()` | use `dismiss()` |
| BUG-555 | M-8 | P2 | Medium | `getMyDetails` copy #8 | shared helper (PM-level) |
| BUG-556 | M-10 | P2 | Medium | Page `"home"` shows follow/fav buttons on curated admin content | new page value (needs FEED) |
| BUG-557 | AV1 | P3 | Low | No `getInstance()` companion, unlike its twin | align the two |
| BUG-558 | AV3 | P3 | Low | Inherited `Util.player` teardown that this tab never needs | leave it; note it when refactoring |
| BUG-559 | AV11 | P3 | Low | No pull-to-refresh and no screen title | add if wanted |

#### `FILES_BROWSER` — module · `agents/MEDIA/FILES_BROWSER.md` · 12 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-560 | FB2 | P1 | High | The list/grid toggle does `detach().attach()`, **re-fetching the entire folder** just to swap the layout manager | swap `layoutManager` in place and call `notifyDataSetChanged` |
| BUG-561 | FB7 | P1 | High | The always-true `\|\|` token guard; the logout branch is dead | use `&&` |
| BUG-562 | FB10 | P1 | High | A non-200 has **no `else` branch** — no error, no empty state, stale content stays | add an error state |
| BUG-563 | FB11 | P1 | High | The Retrofit call is never cancelled; the callback touches views after detach | cancel in `onDestroyView` |
| BUG-564 | FB3 | P2 | Medium | Deprecated `requireFragmentManager()` | `parentFragmentManager` |
| BUG-565 | FB5 | P2 | Medium | `Util.listview` is a global that is never persisted | move to DataStore (STORAGE) |
| BUG-566 | FB6 | P2 | Medium | The root folder is requested with an **empty string**, producing `api/v1/files/` | use an explicit root id or a dedicated endpoint |
| BUG-567 | FB8 | P2 | Medium | The envelope key is **`files`**, a third convention alongside `result`/`results` | document; align with NETWORK if the backend changes |
| BUG-568 | FB9 | P2 | Medium | A new `FileAdapter` is created on every load | reuse + notify |
| BUG-569 | FB12 | P2 | Medium | No pull-to-refresh and no breadcrumb — the user cannot tell they are at the root | add a title/breadcrumb |
| BUG-570 | FB1 | P3 | Low | Commented-out `header_main` code left in `onCreateView` | delete |
| BUG-571 | FB4 | P3 | Low | The toggle icon shows the current mode rather than the target mode | confirm intent with the customer |

#### `FILE_LIST` — module · `agents/MEDIA/FILE_LIST.md` · 13 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-572 | FL-3 | P1 | High | **Every non-folder opens in the PDF viewer** — images, audio, video and documents all route to `PdfActivity2` | branch on the real file type/extension |
| BUG-573 | FL-5 | P1 | High | `getType().equals("folder")` NPEs on a null type | `"folder".equals(getType())` |
| BUG-574 | FL-8 | P1 | High | The always-true `\|\|` token guard | use `&&` |
| BUG-575 | FL-9 | P1 | High | A non-200 has **no `else` branch** — no error, no empty state | add an error state |
| BUG-576 | FL-11 | P1 | High | The Retrofit call is never cancelled | cancel in `onDestroy` |
| BUG-577 | FL-1 | P2 | Medium | Folder navigation recurses into new activities with **no breadcrumb or title** | show the current folder name |
| BUG-578 | FL-2 | P2 | Medium | Rows show only an icon and a name, ignoring `size`, `createdAt`, `createdBy`, `permission` | enrich the row |
| BUG-579 | FL-4 | P2 | Medium | `FLAG_ACTIVITY_NEW_TASK` on both intents distorts the back stack | pass an Activity context and drop the flag |
| BUG-580 | FL-7 | P2 | Medium | `getStringExtra("folderId").toString()` yields `"null"` when missing | validate the extra |
| BUG-581 | FL-10 | P2 | Medium | The adapter receives **`applicationContext`** here but a fragment context in FILES_BROWSER | pass a consistent context |
| BUG-582 | FL-12 | P2 | Medium | `permission` from the model is **never checked** before opening a file | enforce it, or confirm the server does |
| BUG-583 | FL-6 | P3 | Low | `Log.e("type", ...)` debug logging in the click handler | remove |
| BUG-584 | FL-13 | P3 | Low | No search or sort in a file library | ask the customer |

#### `PDF_VIEWER` — module · `agents/MEDIA/PDF_VIEWER.md` · 17 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-585 | PV4 | P0 | Critical | `Log.e("inputstream", inputStream.toString())` runs on a **null** stream after a non-200 — **NPE** | null-check before logging |
| BUG-586 | PV5 | P0 | Critical | `onPostExecute` dereferences the stream with no null check — second NPE path | handle null with an error view |
| BUG-587 | PV6 | P1 | High | The entire PDF is streamed into memory with no size limit | download to cache and use `fromFile` |
| BUG-588 | PV14 | P1 | High | The `AsyncTask` is never cancelled and holds the Activity + dialog — leaks on rotation | cancel in `onDestroy`, or move off `AsyncTask` |
| BUG-589 | PV15 | P1 | High | Rotation re-downloads the whole PDF and resets the page | save state / cache the file |
| BUG-590 | PV3 | P2 | Medium | `enableAnnotationRendering` is called **twice**, `true` then `false` | pick one |
| BUG-591 | PV7 | P2 | Medium | `DownloadFile` is **dead code** (~45 lines) | delete, or finish the feature |
| BUG-592 | PV8 | P2 | Medium | The dead path writes to a hard-coded **`"testthreepdf"`** folder | remove with PV7 |
| BUG-593 | PV9 | P2 | Medium | `Environment.getExternalStorageDirectory()` is deprecated / scoped-storage blocked | app-specific storage |
| BUG-594 | PV12 | P2 | Medium | `setTitle` writes to a title bar that is not displayed | add a page indicator to `pdf_header` |
| BUG-595 | PV16 | P2 | Medium | No auth header on the download, unlike every Retrofit call — the file URL must be public | confirm with PLATFORM/NETWORK |
| BUG-596 | PV17 | P2 | Medium | Extends `Activity`, not `AppCompatActivity`, so it ignores the app theme and night mode | migrate, keeping `FLAG_SECURE` |
| BUG-597 | PV2 | P3 | Low | `pdf_header` has an **empty** click listener | remove or implement |
| BUG-598 | PV10 | P3 | Low | No runtime permission check in the dead downloader | remove with PV7 |
| BUG-599 | PV11 | P3 | Low | `printStackTrace()` instead of `Log` | use `Log.e` |
| BUG-600 | PV13 | P3 | Low | Unused `SAMPLE_FILE` field and a leftover sample URL comment | delete |
| BUG-601 | PV1 | P3 | Cosmetic | Class named `PdfActivity2` with no `PdfActivity` | rename (manifest change) |

#### `WEBVIEW` — module · `agents/MEDIA/WEBVIEW.md` · 13 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-602 | WV4 | P1 | High | **JavaScript and DOM storage enabled** on a page that only renders static legal text — the code comment itself flags the XSS risk | disable both unless the pages genuinely need them |
| BUG-603 | WV8 | P1 | High | **No error handling** — offline or a 404 shows the WebView's raw error page | add `onReceivedError` + a guard |
| BUG-604 | WV1 | P2 | Medium | Binary routing: anything that is not `"terms"` (including a missing extra) silently shows **privacy** | `when` with an explicit `else` and validation |
| BUG-605 | WV2 | P2 | Medium | Two hard-coded URLs on a **third** host family | move to config (PLATFORM) |
| BUG-606 | WV3 | P2 | Medium | No `shouldOverrideUrlLoading` filtering — any outbound link opens in-app | restrict to the known host |
| BUG-607 | WV5 | P2 | Medium | `loadUrl` runs **before** the settings are applied | configure settings first |
| BUG-608 | WV7 | P2 | Medium | No loading indicator — a blank screen while fetching | add a progress bar |
| BUG-609 | WV9 | P2 | Medium | No title, so Terms and Privacy look identical | set a per-mode title |
| BUG-610 | WV10 | P2 | Medium | Back exits the screen instead of going back in the WebView's history | override `onBackPressed` with `webView.canGoBack()` |
| BUG-611 | WV11 | P2 | Medium | The WebView is never destroyed in `onDestroy` | `webView.destroy()` |
| BUG-612 | WV6 | P3 | Low | Zoom is commented out, hurting accessibility on dense legal text | re-enable |
| BUG-613 | WV12 | P3 | Low | WebView cookies/DOM storage are never cleared | clear on exit if required |
| BUG-614 | WV13 | P3 | Low | Rotation reloads the page and loses scroll position | save state |

### SEARCH — 43 entries

#### `SEARCH_LEAD` — team lead (rollups) · `agents/SEARCH/SEARCH_LEAD.md` · 9 entries

| Bug | Agent id | Issue | Affects module(s) | Risk |
|---|---|---|---|---|
| BUG-615 | S-1 | A **fourth response-envelope shape** (`results` as an object with `users` + `posts`) | SEARCH_ENTRY | parse confusion |
| BUG-616 | S-2 | Both fragments take **constructor arguments** instead of a `Bundle` — Android cannot recreate them after process death | SEARCH_POSTS, SEARCH_PROFILES | **crash on restore** |
| BUG-617 | S-3 | A search fires on **every keystroke** with no debounce, and registers a new `authToken` observer each time | SEARCH_ENTRY | request storm |
| BUG-618 | S-4 | On every keystroke all fragments are **removed and the pager adapter rebuilt** | SEARCH_ENTRY | flicker, lost scroll |
| BUG-619 | S-5 | `SearchAdapter.getItem` returns `null as Fragment` for an out-of-range position | SEARCH_ENTRY | crash if a tab is added |
| BUG-620 | S-6 | The Posts tab passes page **`"searchProfile"`** — a misleading name for post results | SEARCH_POSTS | confusing, but matches FEED's contract |
| BUG-621 | S-7 | `UsersAdapter` inflates **PROFILE's `child_follow.xml`** and hides its `follow_btn` by commenting out the binding | SEARCH_PROFILES | cross-team layout coupling |
| BUG-622 | S-8 | No empty-query guard: clearing the box searches for `""` | SEARCH_ENTRY | pointless request |
| BUG-623 | S-9 | The progress dialog is **commented out**, so searches are silent | SEARCH_ENTRY | no feedback |

#### `SEARCH_ENTRY` — module · `agents/SEARCH/SEARCH_ENTRY.md` · 15 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-624 | SE5 | P1 | High | A request fires on **every keystroke** — no debounce, no minimum length | debounce ~300 ms, or search on IME action |
| BUG-625 | SE6 | P1 | High | **All fragments are removed** before each request, destroying both tabs per character | update the adapter's data instead of rebuilding |
| BUG-626 | SE8 | P1 | High | Previous calls are never cancelled — out-of-order responses can show stale results | cancel the in-flight call before starting a new one |
| BUG-627 | SE9 | P1 | High | A non-200 has **no `else` branch** — stale results, no message | add an error state |
| BUG-628 | SE11 | P1 | High | `null as Fragment` in `SearchAdapter.getItem` | throw a clear exception, or handle all positions |
| BUG-629 | SE13 | P1 | High | A new `authToken` observer per keystroke, never removed | read the token once |
| BUG-630 | SE1 | P2 | Medium | The progress dialog's `show()` is commented out, so searching is silent | restore it, or add inline progress |
| BUG-631 | SE2 | P2 | Medium | Uses `ViewPager` v1 + deprecated `FragmentPagerAdapter` | migrate to `ViewPager2` |
| BUG-632 | SE7 | P2 | Medium | An empty query is still sent | guard on blank input |
| BUG-633 | SE12 | P2 | Medium | `FragmentPagerAdapter(fm!!)` without a behaviour flag | pass `BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT` |
| BUG-634 | SE14 | P2 | Medium | `currentTab` is not saved across configuration change | `onSaveInstanceState` |
| BUG-635 | SE3 | P3 | Low | `onTabSelected` and `onTabReselected` are identical; `onTabUnselected` is empty | tidy |
| BUG-636 | SE4 | P3 | Low | `setEditTextFocus` is only ever called with `true`; its `false` path is dead | simplify |
| BUG-637 | SE10 | P3 | Low | Two `Log.e("current tab", ...)` debug calls | remove |
| BUG-638 | SE15 | P3 | Low | No search history, no recent queries, no clear button | ask the customer |

#### `SEARCH_POSTS` — module · `agents/SEARCH/SEARCH_POSTS.md` · 8 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-639 | SP3 | P1 | High | **Constructor arguments** instead of a `Bundle` — the fragment cannot be recreated by the system | `newInstance()` + `Bundle`; make `Posts` parcelable or pass ids |
| BUG-640 | SP2 | P2 | Medium | All three `HomeAdapter` state maps are **empty**, so no like/follow/fav state renders | pass real maps, or confirm read-only is intended |
| BUG-641 | SP4 | P2 | Medium | A `Context` is held in a field | use `requireContext()` |
| BUG-642 | SP6 | P2 | Medium | No pagination — only the first page of search results is ever shown | needs `getSearch` paging (NETWORK) |
| BUG-643 | SP8 | P2 | Medium | The empty state cannot distinguish "no results" from "search failed" | parent must pass an error flag (SEARCH_ENTRY) |
| BUG-644 | SP1 | P3 | Low | `UserPreferences` is created and never used | delete |
| BUG-645 | SP5 | P3 | Low | The page value `"searchProfile"` is misleading on the Posts tab | rename with FEED (shared contract) |
| BUG-646 | SP7 | P3 | Low | No pull-to-refresh and no loading state | the parent owns loading; acceptable as-is |

#### `SEARCH_PROFILES` — module · `agents/SEARCH/SEARCH_PROFILES.md` · 11 entries

| Bug | Agent id | Pri | Severity | Issue | Suggested fix |
|---|---|---|---|---|---|
| BUG-647 | SPR2 | P1 | High | `UsersAdapter` reuses PROFILE's `child_follow.xml` but leaves **`follow_btn` unbound** (commented out) | hide it explicitly, or use a dedicated layout |
| BUG-648 | SPR7 | P1 | High | **Constructor arguments** instead of a `Bundle` — cannot be recreated by the system | `newInstance()` + `Bundle` |
| BUG-649 | SPR3 | P2 | Medium | The layout manager uses `context` while the adapter uses `contexts` — two sources, one nullable | pick one |
| BUG-650 | SPR5 | P2 | Medium | `UsersAdapter` and `FollowAdapter` are near-duplicates differing only by the follow button | merge with a flag (cross-team, PM) |
| BUG-651 | SPR8 | P2 | Medium | A `Context` is held in a field | `requireContext()` |
| BUG-652 | SPR9 | P2 | Medium | No follow action from search results, though the row layout has a button | confirm with the customer |
| BUG-653 | SPR10 | P2 | Medium | No pagination — only the first page of profile results | needs `getSearch` paging (NETWORK) |
| BUG-654 | SPR11 | P2 | Medium | The empty state cannot distinguish "no results" from "search failed" | parent must pass a flag |
| BUG-655 | SPR1 | P3 | Low | The list id (`search_profile_list`) differs from the Posts tab's (`list`) | align naming |
| BUG-656 | SPR4 | P3 | Low | The `LifecycleOwner` parameter is never used | remove |
| BUG-657 | SPR6 | P3 | Low | Deprecated `Picasso.with(context)` | upgrade |

<!-- AUTO-GENERATED:END -->

---

## 8. Change log

| Change | Detail |
|---|---|
| Re-baselined (T-019, 2026-10-08) | Baseline moved from remote `master` (v1.1) to **`salvation_lamb_permissions_final_1`** (v1.2.0, 123 commits ahead). PM re-verified the **10 P0s** in the new code: **4 fixed upstream** (PDF NPEs, `ViewPostActivity` ctor binding, Search fragment ctor args, `FollowerActivity` init order), **6 still reproduce**. Re-counted **CL-1** (29 broken `||` vs 29 correct `&&`) and **CL-4** (6 → 9 `isReviewState` sites). Added **`G13`** (fail-open `Util.hasPermission`) as a new P0 candidate and **`G14`** (committed `google-services.json`). `G1`/`G2` closed upstream. Fix order rewritten to 10 waves. §7 still holds `master`-era line numbers until each agent is re-audited. |
| Created (T-017) | Consolidated register opened. Hand-written half: id scheme + prefix-collision table, priority mapping, status tracking, closing procedure, **10 cross-cutting clusters (CL-1 … CL-10)**, **10 P0 crashers**, and a 10-wave fix order. Generated half produced by the new `agents/tools/sync_bug_notes.py` from all 42 agent docs + `AGENTS.md` §7. |


