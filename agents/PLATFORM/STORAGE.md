# STORAGE Module Agent

> Team: **PLATFORM** · Reports to: `agents/PLATFORM/PLATFORM_LEAD.md`
> This agent knows **every detail** of `UserPreferences.kt`: the DataStore file, all 5 keys, their
> read/write/delete APIs, the defaults, and which screens touch them.
> It may only edit the files listed in §8 **Owned files**.

---

## ⚠ Blast-radius warning

This single class is the app's **only persistence**. There is no database, no SharedPreferences, no
file cache. It holds the **auth token** — corrupting it logs every user out; losing it on upgrade
logs every user out.

`UserPreferences(...)` is instantiated at **26 sites**. Every change needs PM sign-off.

---

## 1. Identity

| Item | Value |
|---|---|
| Role | the app's only persistent storage |
| File | `app/src/main/java/com/veha/util/UserPreferences.kt` (106 lines) |
| Backing store | Jetpack **DataStore Preferences**, file name **`SalvationLamb`** |
| Dependency | `androidx.datastore:datastore-preferences:1.0.0-alpha01` — a **very old alpha** |
| API style | `applicationContext.createDataStore(name = ...)` + `preferencesKey<T>(...)` — **both removed in stable DataStore** (issue S-1) |
| Exposure | reads are `Flow<T>` properties; writes/deletes are `suspend fun` |
| Instantiation | `UserPreferences(context)` — a plain class, **not** a singleton; 26 call sites |

---

## 2. The 5 keys

| Property | Key string | Type | Default when missing | Written by | Read by |
|---|---|---|---|---|---|
| `authToken` | `"token"` | String | the literal **`"null"`** | LOGIN | SPLASH, CHANGE_PASSWORD, COMMONS |
| `userId` | `"userId"` | String | the literal **`"null"`** | LOGIN | SPLASH |
| `isNightModeEnabled` | `"isNight"` | String | the literal **`"null"`** | LOGIN, SPLASH, SETTINGS | SPLASH, MAIN_NAV, SETTINGS |
| `isFirstTime` | `"isFirst"` | Boolean | **`true`** (explicit) | LOGIN, SPLASH | PROFILE, APPSHELL |
| `textSize` | `"textSize"` | Float | **`10.0F`** (explicit) | SETTINGS | SPLASH, THEMING |

Each key has exactly three members: a `Flow` getter, a `save…` suspend function, and a `delete…`
suspend function — 15 members in total.

### The `"null"` string problem (most important fact here)

```kotlin
val authToken: Flow<String>
    get() = dataStorePref.data.map { preferences ->
        preferences[AUTH_TOKEN].toString()      // <-- null becomes "null"
    }
```

The three `String` keys call `.toString()` on a nullable value, so **a missing key reads back as the
4-character string `"null"`, never as a Kotlin `null`.** This is why consumers across the app are
littered with:

```kotlin
if (TextUtils.isEmpty(it) || it.equals("null") || it.isNullOrEmpty()) { ... }
```

It also means `isNullOrEmpty()` is **always false** for these flows — the only effective check is
`== "null"`. Any "cleanup" that removes the `"null"` comparison will break session detection in
SPLASH and the (already dead) logout branch in CHANGE_PASSWORD (issue S-2).

The two typed keys behave correctly: `isFirstTime` returns `true` when absent, `textSize` returns
`10.0F`.

---

## 3. API surface

```kotlin
class UserPreferences(context: Context) {
    private val applicationContext = context.applicationContext
    private val dataStorePref: DataStore<Preferences> =
        applicationContext.createDataStore(name = "SalvationLamb")

    val authToken: Flow<String>            // + userId, isNightModeEnabled : Flow<String>
    val isFirstTime: Flow<Boolean>
    val textSize: Flow<Float>

    suspend fun saveAuthToken(token: String)      ; suspend fun deleteAuthToken()
    suspend fun saveUserId(token: String)         ; suspend fun deleteUserId()
    suspend fun saveIsNightModeEnabled(s: String) ; suspend fun deleteIsNightModeEnabled()
    suspend fun saveIsFirstTime(b: Boolean)       ; suspend fun deleteIsFirstTime()
    suspend fun saveTextSize(f: Float)            ; suspend fun deleteTextSize()

    companion object {
        private val AUTH_TOKEN = preferencesKey<String>("token")
        private val USER_ID    = preferencesKey<String>("userId")
        private val IS_NIGHT   = preferencesKey<String>("isNight")
        private val IS_FIRST   = preferencesKey<Boolean>("isFirst")
        private val TEXT_SIZE  = preferencesKey<Float>("textSize")
    }
}
```

| Detail | Note |
|---|---|
| `context.applicationContext` | correctly avoids leaking an Activity |
| `saveUserId(token: String)` | the parameter is misnamed `token` (copy-paste, issue S-3) |
| `isFirstTime` getter | casts with `preferences[IS_FIRST] as Boolean` instead of using the typed accessor (issue S-4) |
| No `clearAll()` | logout must call `deleteAuthToken()` **and** `deleteUserId()` separately; `isNight`, `isFirst` and `textSize` **survive logout** (issue S-5) |

---

## 4. How screens consume it

Two patterns, both in active use:

```kotlin
// A. observe as LiveData (SPLASH, CHANGE_PASSWORD, COMMONS)
userPreferences.authToken.asLiveData().observe(this) { token -> ... }

// B. write inside a coroutine (LOGIN, SPLASH, MAIN_NAV)
lifecycleScope.launch { userPreferences.saveAuthToken(resp.token) }
```

| Consequence | Detail |
|---|---|
| Observers are **continuous** | any later write re-fires every observer — the cause of SPLASH's duplicate `getMyDetails` (AUTH A10) and CHANGE_PASSWORD's repeated requests (AUTH A20) |
| No one-shot read | nothing in the app uses `.first()`; every read is a subscription |
| Writes are fire-and-forget | no `await`, no error handling; a failed write is silent |

### Session contract (defined by AUTH, enforced here)

| Event | Keys touched |
|---|---|
| Login success | write `token`, `userId`, `isNight` (reset to `"Default"`), `isFirst` |
| Logout (MAIN_NAV, FEED, MEDIA) | delete `token`, `userId` |
| Splash non-200 | **nothing deleted** — the known gap (AUTH A27) |

---

## 5. Lifecycle & threading

- `createDataStore` is called in the **constructor**, so each `UserPreferences(context)` builds a new
  `DataStore` instance over the same file. Stable DataStore throws
  `IllegalStateException: There are multiple DataStores active for the same file` in this situation;
  this alpha version does not, which is the only reason 26 instantiations work today (issue S-6).
- All writes are `suspend` and run on DataStore's own IO dispatcher.
- Reads are cold `Flow`s; `.asLiveData()` binds them to the observer's lifecycle.

---

## 6. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| `androidx.datastore:datastore-preferences:1.0.0-alpha01` | PLATFORM / BUILD_CONFIG | the store itself |
| `kotlinx-coroutines` / `Flow` | PLATFORM / BUILD_CONFIG | async reads and writes |
| `androidx.lifecycle:lifecycle-livedata-ktx` | PLATFORM / BUILD_CONFIG | `.asLiveData()` at call sites |
| `Util.DEFAULT` / `Util.DAY` / `Util.NIGHT` | PLATFORM / COMMONS | the values stored under `isNight` |

This class has **no dependency on NETWORK or DATA_MODELS** — it stores primitives only.

---

## 7. Known issues / tech debt

| # | Issue | Severity | Suggested fix |
|---|---|---|---|
| S-2 | The three `String` flows return the literal **`"null"`** when a key is absent, so `isNullOrEmpty()` never matches and every consumer hard-codes `== "null"` | **High** | expose `Flow<String?>` and drop `.toString()`, then update all consumers in one change |
| S-1 | Pinned to `datastore-preferences:1.0.0-alpha01` using `createDataStore` / `preferencesKey`, both **removed** in stable | **High** | migrate to `preferencesDataStore` + `stringPreferencesKey` (BUILD_CONFIG + every call site) |
| S-6 | A **new `DataStore` per instance** over the same file, at 26 sites; only the alpha's missing guard prevents a crash | **High** | make it a singleton (or an `object` with an app-level `Context`) |
| S-7 | Nothing in the app reads one-shot; all 5 flows are observed continuously, causing duplicate network calls on re-emission | **High** | add one-shot helpers (`suspend fun getAuthToken(): String?`) |
| S-5 | No `clearAll()`; logout deletes only `token` + `userId`, leaving `isNight`, `isFirst`, `textSize` from the previous user | Medium | add `suspend fun clearSession()` and use it everywhere |
| S-8 | The token is stored in **plain text**; DataStore is not encrypted | Medium (security) | EncryptedSharedPreferences or an encrypted DataStore wrapper |
| S-9 | Writes are fire-and-forget with no error handling | Medium | surface failures, at least by logging |
| S-10 | No migration strategy; renaming a key silently logs every user out on upgrade | Medium | document and version keys |
| S-4 | `isFirstTime` uses `preferences[IS_FIRST] as Boolean` instead of the typed accessor | Low | use the typed value |
| S-3 | `saveUserId(token: String)` — parameter misnamed | Low | rename to `userId` |
| S-11 | The store name `"SalvationLamb"` is a magic string in the constructor | Low | extract a constant |
| S-12 | Boilerplate: 5 keys x 3 members, all identical in shape | Low | generic helpers |

---

## 8. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/util/UserPreferences.kt` | **exclusive** |

**Not owned (escalate):** the datastore dependency line in `app/build.gradle` (BUILD_CONFIG),
`Util.java` theme constants (COMMONS), and **every screen** that instantiates `UserPreferences`.

---

## 9. How to make common changes

**Add a key (safe path):**
1. Add `private val NEW_KEY = preferencesKey<T>("newKey")` to the companion — **note the old alpha
   API**, not `stringPreferencesKey`.
2. Add the `Flow<T>` getter, `saveX()` and `deleteX()`, following the existing shape.
3. For `String` keys, decide deliberately whether to repeat the `.toString()` convention (consistent
   with the rest of the file) or return `String?` (correct but inconsistent) — say which in the
   report to PM.
4. Add a row to §2 here naming the writing and reading screens.

**Rename a key string:** this **logs every existing user out**, because the old key is orphaned.
Requires PM sign-off and a migration plan.

**Add `clearSession()` (fixes S-5):** add a suspend function that deletes all five keys, then ask PM
to coordinate APPSHELL/FEED/MEDIA so the logout call sites use it instead of two separate deletes.

**Fix the `"null"` convention (S-2):** change the three getters to `Flow<String?>` without
`.toString()`, then update **every** consumer that compares against `"null"` — SPLASH,
CHANGE_PASSWORD and `Commons.makeMeWarior` at minimum. Single change set, PM sign-off.

**Migrate off the alpha (S-1):** a BUILD_CONFIG dependency bump plus API changes here; `createDataStore`
and `preferencesKey` no longer exist in stable. Treat as a dedicated PM-approved task, not a
drive-by.

---

## 10. Change log

| Change | Detail |
|---|---|
| Created | Initial STORAGE module agent documented from `UserPreferences.kt` (106 lines): the `SalvationLamb` DataStore, all 5 keys with types/defaults/writers/readers, the 15-member API surface, both consumption patterns, the session contract, and 12 known issues. Verified **26 `UserPreferences(...)` instantiation sites** and recorded the `"null"`-string convention that session detection depends on. |


