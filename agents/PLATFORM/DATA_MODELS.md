# DATA_MODELS Module Agent

> Team: **PLATFORM** · Reports to: `agents/PLATFORM/PLATFORM_LEAD.md`
> This agent knows **every detail** of `DataModels.kt`: all 12 data classes, every field, which
> endpoint produces each one, which screens consume them, and the parsing rules.
> It may only edit the files listed in §8 **Owned files**.

---

## ⚠ Blast-radius warning

These 12 classes are the **shape of every server response in the app**. Because they are Kotlin
`data class`es with **non-null `String`** fields and **no defaults**, a single rename or type change
can:

- break callers at compile time (renames), or
- insert a `null` into a non-null field at runtime (Gson bypasses constructors), crashing later at an
  unrelated line.

**Every change here needs PM sign-off and a named list of affected screens.** Prefer **adding** a
nullable field with a default over changing an existing one.

---

## 1. Identity

| Item | Value |
|---|---|
| Role | response models for the whole app |
| File | `app/src/main/java/com/veha/util/DataModels.kt` (170 lines) |
| Contents | **12 `data class`es**, no methods, no companions, no annotations |
| Parser | `com.google.gson.Gson().fromJson(json, X::class.java)` at each call site |
| Conventions | every field is `val` (two exceptions), every field is `String` (except 5 nested objects), **no `@SerializedName`**, **no defaults**, **no nullable types** |

---

## 2. The 12 models

| # | Class | Lines | Fields | Produced by | Consumed by |
|---|---|---|---|---|---|
| 1 | `Loginresp` | 3–30 | 28 | `POST api/v1/login` | LOGIN |
| 2 | `PostLikes` | 32–40 | 7 | `GET like/post/{postId}` | VIEW_LIKES |
| 3 | `AllFavList` | 41–48 | 6 | `GET favorites/{userId}` | FAVORITES, FEED |
| 4 | `AllFollowerList` | 49–56 | 6 | `GET follows/user/{id}`, `GET follows/{id}` | FOLLOWERS |
| 5 | `FilesAndFolders` | 57–68 | 10 | `GET files/{folderId}` | FILES_BROWSER, FILE_LIST |
| 6 | `Countries` | 69–84 | 14 | `GET Country` | EDIT_PROFILE |
| 7 | `State` | 85–95 | 9 | `GET state/{countryID}` | EDIT_PROFILE |
| 8 | `City` | 96–108 | 11 | `GET city/{stateId}` | EDIT_PROFILE |
| 9 | `UserRslt` | 109–138 | 28 | `GET users/{userId}`, `POST users` | AUTH, PROFILE, APPSHELL |
| 10 | `Posts` | 139–153 | 13 | `GET post`, `post/user/{id}`, `post/admin/*` | FEED, PROFILE, SEARCH, MEDIA |
| 11 | `PostUser` | 155–161 | 5 | nested inside `Posts`, `PostLikes`, `AllFollowerList` | FEED, PROFILE, SEARCH |
| 12 | `FavPost` | 162–169 | 6 | `GET favorites/{userId}` | FAVORITES |

**Composition graph**

```
Posts            --> user: PostUser          (var, the only mutable field)
PostLikes        --> user: PostUser
AllFollowerList  --> user: PostUser
AllFavList       --> posts: Posts --> PostUser
FavPost          --> posts: Posts --> PostUser
```

`PostUser` is the most reused type in the file — a **lightweight user** (`id`, `name`, `picture`,
`isWarrior`, `email`) distinct from the full `UserRslt`.

---

## 3. Field reference

### 3.1 `Loginresp` (28 fields) — login only

`id`, `loginSource`, `role`, `name`, `email`, `gender`, **`password`**, `mobile`, `dateOfBirth`,
`updatedAt`, `createdAt`, `picture`, `coverPicture`, `address`, `isWarrior`, `isReviewState`,
`state`, `pinCode`, `country`, `churchName`, `religion`, `language`, `isFreshUser`,
**`isVerifiedUser`**, `blocked`, **`token`**

| Note | Detail |
|---|---|
| `token` | the **only** place the auth token is delivered |
| `password` | the server returns the password field — a security smell (AUTH A7) |
| `isVerifiedUser` | **named differently from `UserRslt.isVerified`** — the single most confusing inconsistency in this file (issue D1) |
| Missing vs `UserRslt` | no `firstName`, `lastName`, `city`, `userGroup` |

### 3.2 `UserRslt` (28 fields) — the app-wide user object

`id`, `name`, `firstName`, `lastName`, `gender`, `email`, `mobile`, `picture`, `coverPicture`,
`address`, `dateOfBirth`, `loginSource`, `role`, `createdAt`, `updatedAt`, `isWarrior`,
`isReviewState`, `state`, `pinCode`, `country`, `churchName`, `religion`, `city`, `language`,
`userGroup`, **`isVerified`**, `isFreshUser`, `blocked`

Cached globally in `Util.user`. Read by LOGIN, SPLASH, MAIN_NAV (routing on `isVerified`), PROFILE
and EDIT_PROFILE (every field).

### 3.3 `Posts` (13 fields) — the feed item

`id`, `title`, `content`, `tags`, `userId`, `picture`, `type`, `url`, `likesCount`, `shareCount`,
`createdAt`, `updatedAt`, **`var user: PostUser`**

| Note | Detail |
|---|---|
| `var user` | the **only `var`** in the file — adapters reassign it after fetching the author |
| `type` | discriminates text / image / audio / video; values are not documented here (FEED owns the meaning) |
| `likesCount` / `shareCount` | **`String`, not `Int`** — arithmetic requires `.toInt()` |
| `url` | media URL; for videos `Util.getVideo(url)` prefixes `https://salvationlamb.com/video/` |

### 3.4 `FilesAndFolders` (10 fields)

`id`, `parentId`, `name`, `type`, `size`, `url`, `permission`, `createdBy`, `createdAt`, `updatedAt`
— `type` distinguishes file from folder; `size` is a `String`.

### 3.5 Geo models — `Countries` (14), `State` (9), `City` (11)

The only classes using **snake_case** field names (`numeric_code`, `phone_code`, `currency_name`,
`country_id`, `state_code`, `wikidataid`), because they mirror the backend verbatim. Everything else
in the file is camelCase (issue D2). `Countries.native` is a **Kotlin soft keyword** used as a
field name — legal, but it must be escaped in some contexts (issue D3).

### 3.6 Join/wrapper models

| Class | Shape | Note |
|---|---|---|
| `PostLikes` | `id`, `userId`, `postId`, `reaction`, `createdAt`, `updatedAt`, `user` | `reaction` is a `String` |
| `AllFavList` | `id`, `userId`, `postId`, `createdAt`, `updatedAt`, `posts` | |
| `FavPost` | `id`, `userId`, `postId`, `createdAt`, `updatedAt`, `posts` | **structurally identical to `AllFavList`** (issue D4) |
| `AllFollowerList` | `id`, `userId`, `followerId`, `createdAt`, `updatedAt`, `user` | serves both followers and following |

---

## 4. Parsing rules (how screens actually use these)

```kotlin
// single object
val user: UserRslt = Gson().fromJson(resp?.get("result"), UserRslt::class.java)

// list
val type = object : TypeToken<ArrayList<Posts>>() {}.type
val posts: ArrayList<Posts> = Gson().fromJson(resp?.get("result"), type)
```

| Rule | Detail |
|---|---|
| Payload key | always `result` |
| Instantiation | **Gson uses Unsafe allocation**, bypassing the Kotlin constructor — so non-null fields *can* hold `null` |
| Missing field | silently becomes `null` (objects) or is skipped; **no exception at parse time** |
| Booleans | stored as `String`, read with `.toBoolean()` — `"true"` -> true; `"TRUE"`, `"1"`, `null` -> **false** |
| Numbers | `likesCount`, `shareCount`, `size` are `String`; callers must convert |
| Dates | ISO strings formatted by `Util.getTimeAgo` / `Util.formatDate` (COMMONS) |

### The null trap (most important fact in this doc)

```kotlin
data class UserRslt(val churchName: String, ...)   // declared non-null
// server omits churchName  ->  Gson leaves it null
user.churchName.length                              // NPE, far from the real cause
```

Kotlin's null-safety gives **no protection** here. This is the root cause of parse crashes across the
app (issue D5) and the reason screens defensively call `isNullOrEmpty()` on supposedly non-null
fields — e.g. `LoginActivity`'s `loginresp.isWarrior.isNullOrEmpty()`, which the compiler flags as
always-false yet is genuinely needed.

---

## 5. Cross-model inconsistencies to know

| Topic | `Loginresp` | `UserRslt` | `PostUser` |
|---|---|---|---|
| verified flag | `isVerifiedUser` | `isVerified` | — |
| name fields | `name` only | `name` + `firstName` + `lastName` | `name` |
| warrior flag | `isWarrior` | `isWarrior` | `isWarrior` |
| city | **absent** | `city` | — |
| token | `token` | **absent** | — |

This is why LOGIN must make **two** calls: `login` returns the token but a differently-named verified
flag, so the screen re-fetches `UserRslt` to decide routing.

---

## 6. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| `com.google.code.gson` (via `converter-gson`) | PLATFORM / BUILD_CONFIG | reflection-based parsing |
| `RetrofitAPI` | PLATFORM / NETWORK | produces the `JsonObject` these are parsed from |
| `Util.user` | PLATFORM / COMMONS | caches a `UserRslt` globally |

**No Android dependency** — this file is pure Kotlin and would be unit-testable if tests existed.

---

## 7. Known issues / tech debt

| # | Issue | Severity | Suggested fix |
|---|---|---|---|
| D5 | **Non-null `String` fields can hold `null`** because Gson bypasses constructors; a missing server field becomes a delayed NPE | **High** | make fields nullable with defaults (`val x: String? = null`), or add `@SerializedName` + a null-safe parser |
| D6 | **All booleans are `String`** (`isWarrior`, `isVerified`, `isFreshUser`, `blocked`, `isReviewState`) and read with `.toBoolean()`, so `"TRUE"`/`"1"` silently become `false` (G7) | **High** | `Boolean` fields, or a shared `String.toBooleanSafe()` in COMMONS |
| D1 | `Loginresp.isVerifiedUser` vs `UserRslt.isVerified` — same concept, two names | **High** | align with the backend; this forces LOGIN's second round-trip |
| D7 | Counts (`likesCount`, `shareCount`) and `size` are `String` | Medium | `Int` / `Long` |
| D8 | **No `@SerializedName` anywhere** — field names are hard-bound to the JSON, so a Kotlin rename silently breaks parsing with no compile error | **High** | annotate every field |
| D4 | `AllFavList` and `FavPost` are structurally identical duplicates | Low | delete one |
| D9 | `Loginresp` and `UserRslt` overlap by ~24 fields with no shared supertype | Medium | extract a common interface/base |
| D2 | Geo models use snake_case while the rest use camelCase | Low | `@SerializedName` + camelCase properties |
| D3 | `Countries.native` uses a Kotlin soft keyword as a field name | Low | rename with `@SerializedName("native")` |
| D10 | `Posts.user` is the only `var`; everything else is `val`, so mutation is inconsistent | Low | use `copy()` instead |
| D11 | No `data class` for the **response envelope** itself (`result` / `message` / `status` / `errorMessage`) — every screen re-parses it by hand | Medium | add `ApiResponse<T>` (with NETWORK) |
| D12 | No pagination wrapper despite 4 paged endpoints | Medium | add a `Page<T>` model (with NETWORK + FEED) |
| D13 | Dates are `String` with no parsing contract in the model | Low | keep as-is, or expose typed accessors |
| D14 | `Loginresp.password` echoes the password back from the server | Medium (security) | raise with the backend; drop the field |

---

## 8. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/util/DataModels.kt` | **exclusive** |

**Not owned (escalate):** `RetrofitAPI.kt` (NETWORK), `Util.java` / `Commons.kt` (COMMONS),
`UserPreferences.kt` (STORAGE), the gson dependency (BUILD_CONFIG), and **every screen** that parses
these models.

---

## 9. How to make common changes

**Add a field (safe path):** append `val newField: String? = null` to the class. Nullable + default
means old responses still parse and no caller breaks. Add it to §3 here and tell PM which screens
will read it.

**Add a field (unsafe path — avoid):** adding a **non-null** field with no default means every
response lacking it yields `null` in a non-null type. Only do this when the backend guarantees the
field, and say so explicitly in the report to PM.

**Rename a field:** this is a **breaking change twice over** — callers break at compile time *and*
parsing breaks silently, because the Kotlin name is the JSON name (D8). Add
`@SerializedName("<oldJsonName>")` so the wire format is preserved, then update every caller in the
same change set. PM sign-off required.

**Change a type (e.g. `likesCount: String` -> `Int`):** find every caller
(`grep -rn "likesCount" app/src/main/java`), confirm the server always sends a number, and update all
of them together. A `String` holding `""` will throw on `toInt()`.

**Add a new model:** append a `data class` in the existing style (trailing comma, `val`, `String`
fields) and record its producing endpoint and consuming screens in §2.

**Do not** introduce `kotlinx.serialization`, sealed classes or a different parser — that is an
architecture change requiring PM approval and coordination with NETWORK.

---

## 10. Change log

| Change | Detail |
|---|---|
| Created | Initial DATA_MODELS module agent documented from `DataModels.kt` (170 lines): all 12 data classes with field counts, producing endpoints and consuming screens, the composition graph, the full field reference for the 5 major models, parsing rules, the Gson null trap, the `Loginresp` vs `UserRslt` inconsistency table, and 14 known issues. |



