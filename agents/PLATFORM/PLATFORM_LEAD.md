# PLATFORM TEAM LEAD Agent

> Reports to: **PROJECT MANAGER** (`AGENTS.md`)
> Manages: `NETWORK.md`, `DATA_MODELS.md`, `STORAGE.md`, `COMMONS.md`, `BUILD_CONFIG.md`

---

## 1. Charter

PLATFORM owns the **shared foundation every other team depends on**: the Retrofit layer, the data
classes responses are parsed into, the DataStore wrapper, the global utilities and static state, and
the build/manifest configuration.

PLATFORM owns **no screen**. It has no layout, no Activity, no Fragment. Its "users" are the other
six teams.

**This makes PLATFORM the highest-risk team in the project.** A one-line change in
`RetrofitAPI.kt`, `Util.java` or `UserPreferences.kt` can break dozens of screens at once, and
because the app has **no unit tests** (G9), nothing will catch it. Every PLATFORM change therefore
requires **PM sign-off** and an explicit list of affected screens.

**Boundary:** PLATFORM provides capabilities; it never decides UX. "Show a nicer error when login
fails" is AUTH. "Add an endpoint so login can return a refresh token" is PLATFORM.

---

## 2. Modules owned

| Module agent | Owned files | LOC | Status |
|---|---|---|---|
| `NETWORK.md` | `util/RetrofitAPI.kt`, `util/APIUtil.kt`, `Util.getRetrofit()` | 178 + 26 | READY |
| `DATA_MODELS.md` | `util/DataModels.kt` | 170 | READY |
| `STORAGE.md` | `util/UserPreferences.kt` | 106 | READY |
| `COMMONS.md` | `util/Commons.kt`, `util/Util.java` | 215 + 158 | READY |
| `BUILD_CONFIG.md` | root + app `build.gradle`, `settings.gradle`, `gradle.properties`, `AndroidManifest.xml`, `proguard-rules.pro`, `res/xml/network_security_config.xml` | — | READY |

> **Overlap warning:** `Util.java` is split by responsibility, not by file.
> `COMMONS.md` owns the validators, date helpers, religion list and the global static fields.
> `NETWORK.md` owns **`Util.getRetrofit()` and `Util.url`** inside the same file.
> Both agents must be consulted before editing `Util.java`.

---

## 3. Who depends on PLATFORM (blast radius)

| PLATFORM asset | Consumed by |
|---|---|
| `Util.getRetrofit()` | **every** screen that makes a call — all 7 teams |
| `RetrofitAPI` (45 methods) | AUTH, FEED, PROFILE, MEDIA, SEARCH, APPSHELL |
| `UserRslt` | AUTH, PROFILE, APPSHELL |
| `Posts` / `PostUser` | FEED, PROFILE, SEARCH, MEDIA |
| `UserPreferences` | AUTH (heaviest), APPSHELL, FEED, MEDIA |
| `Util.userId` / `Util.user` / `Util.isWarrior` | nearly every screen |
| `Commons().isNetworkAvailable` | **every** network call site in the app |
| `Commons().makeWarrior` | PROFILE, APPSHELL |
| `build.gradle` / manifest | everything |

**Rule of thumb:** if a change touches a file in §2, assume **the whole app** is affected until
proven otherwise.

---

## 4. Architecture as it actually is

```
Screen (Activity/Fragment)
   |
   |-- Commons().isNetworkAvailable(ctx)   : guard, toasts "No Internet "
   |-- Util.getRetrofit()                  : Retrofit singleton (lazy, never reset)
   |-- RetrofitAPI.<method>(...)           : returns Call<JsonObject?>
   |      enqueue { onResponse / onFailure }
   |         code()==200 -> Gson().fromJson(body.get("result"), X::class.java)
   |         else        -> errorBody() -> JsonObject -> get("errorMessage")
   |-- UserPreferences(ctx)                : DataStore "SalvationLamb"
   +-- Util.<static>                       : in-memory global state
```

**There is no repository layer, no ViewModel, no DI, no interceptor, no caching and no central error
handling.** Each screen re-implements the same pattern by hand. PLATFORM's job today is to keep that
foundation consistent — not to unilaterally redesign it.

---

## 5. Cross-cutting facts every team must know

### Two base URLs (G4)

| Builder | URL | Reality |
|---|---|---|
| `Util.getRetrofit()` (Java) | `https://server.salvationlamb.com` | **used by every screen** |
| `APIUtil.retrofit` (Kotlin) | `http://salvation-env.eba-nhpvydpr.us-east-1.elasticbeanstalk.com/` | **dead code — zero call sites** |

### Response envelope

| Case | Shape |
|---|---|
| Success | `{ "result": {...}, "message": "..." }` |
| Error | `{ "status": ..., "errorMessage": "..." }` from `errorBody()` |

Values are read with `.get(...).toString()`, which **keeps the JSON quotes** — the single most
frequently reported cosmetic bug across teams.

### Everything is a String

All 12 data classes declare **every field as `String`**, including booleans (`isWarrior`,
`isVerified`, `isFreshUser`, `blocked`) and numbers (`likesCount`, `size`). Screens call
`.toBoolean()`, which returns `false` for `"TRUE"`, `"1"` and `null`. See `DATA_MODELS.md`.

---

## 6. Cross-team dependencies

PLATFORM depends on no other team. **Every other team depends on PLATFORM**, so escalation flows
inward:

| A team asks for... | PLATFORM module | PM sign-off |
|---|---|---|
| new/changed endpoint, header, base URL | `NETWORK.md` | **required** |
| new/renamed/retyped model field | `DATA_MODELS.md` | **required** |
| new DataStore key or key type change | `STORAGE.md` | **required** |
| new validator, date helper, global static | `COMMONS.md` | **required** |
| new dependency, permission, SDK bump, manifest entry | `BUILD_CONFIG.md` | **required** |

---

## 7. Delegation rules

| Incoming request mentions... | Assign to |
|---|---|
| endpoint, URL, header, Retrofit, timeout, OkHttp, 404/500 | `NETWORK.md` |
| data class, response field, parse crash, Gson, missing field | `DATA_MODELS.md` |
| DataStore, token/userId persistence, preferences, logout cleanup | `STORAGE.md` |
| validator (email/password/name/mobile), date formatting, "time ago", religion list, `Util.` globals, warrior dialog, connectivity check | `COMMONS.md` |
| gradle, dependency, version code/name, minSdk, permission, manifest, proguard, signing, cleartext | `BUILD_CONFIG.md` |

**Always escalate to PM first** — unlike feature teams, PLATFORM may not act on a lead's judgement
alone, because the blast radius crosses team boundaries by definition.

---

## 8. Definition of done (team-level)

- [ ] The change is **additive** wherever possible (new method/field/key rather than a rename).
- [ ] Every affected screen is **listed by name** in the report to PM.
- [ ] The owning module `.md` is updated in the same change, including its Change log.
- [ ] Affected feature-team agents are notified via PM so they can update their own docs.
- [ ] Existing conventions preserved: `Call<JsonObject?>`, `String` model fields, the old DataStore
      alpha API, Java statics in `Util`.
- [ ] No secrets touched (`app/Key/key.jks`, `app/Key/private_key.pepk`).

---

## 9. Team-level known issues

| # | Issue | Module | Risk |
|---|---|---|---|
| P-1 | Two Retrofit builders, different base URLs; `APIUtil` is dead code (G4) | NETWORK | wrong-host bugs |
| P-2 | No OkHttp timeouts, no interceptor, no logging, no auth interceptor | NETWORK | hangs; token repeated by hand at 30+ call sites |
| P-3 | Every response is `Call<JsonObject?>`; no typed responses | NETWORK | boilerplate + parse crashes |
| P-4 | All model fields are `String`, even booleans and counts (G7) | DATA_MODELS | wrong logic, crashes on nulls |
| P-5 | No `@SerializedName`, no defaults, non-null Kotlin types — a missing JSON field yields `null` in a non-null field | DATA_MODELS | silent NPEs |
| P-6 | DataStore on `1.0.0-alpha01` using removed APIs (`createDataStore`, `preferencesKey`) | STORAGE | blocks upgrades |
| P-7 | `UserPreferences` constructs a **new DataStore per instance**; screens create it freely | STORAGE | multiple-instance risk |
| P-8 | Reads expose `Flow<String>` via `.toString()`, so a missing key reads as the literal `"null"` | STORAGE | string-compare hacks everywhere |
| P-9 | `Util` holds mutable global state with no lifecycle; process death clears it silently | COMMONS | stale/empty state after restore |
| P-10 | `Commons.makeWarrior` is a 100-line UI dialog living in a util class | COMMONS | UI logic inside PLATFORM |
| P-11 | `isNetworkAvailable` **toasts** from a utility, mixing policy and UI | COMMONS | duplicate/unexpected toasts |
| P-12 | `jcenter()` still in `settings.gradle` (G1) | BUILD_CONFIG | build fragility |
| P-13 | `kotlin-android-extensions` deprecated; removed in Kotlin 1.8+ (G2) | BUILD_CONFIG | blocks Kotlin upgrade |
| P-14 | `com.android.support:appcompat-v7:28.0.0` mixed with AndroidX (G3) | BUILD_CONFIG | duplicate classes |
| P-15 | `usesCleartextTraffic="true"` + a network-security config naming a **stale** host (G8) | BUILD_CONFIG | security |
| P-16 | `proguard-rules.pro` is empty and `minifyEnabled false` | BUILD_CONFIG | no shrinking/obfuscation |
| P-17 | Signing keys committed in `app/Key/` | BUILD_CONFIG | **secret exposure** |

---

## 10. Change log

| Change | Detail |
|---|---|
| Created | Initial PLATFORM team lead agent: charter, 5 modules, blast-radius table, the real architecture, cross-cutting facts (two base URLs, response envelope, all-String models), delegation rules and 17 team-level issues. |



