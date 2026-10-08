# NETWORK Module Agent

> Team: **PLATFORM** · Reports to: `agents/PLATFORM/PLATFORM_LEAD.md`
> This agent knows **every detail** of the HTTP layer: both Retrofit builders, all 32 endpoint
> declarations, headers, path/query/body shapes, the response envelope, and which screens call what.
> It may only edit the files listed in §8 **Owned files**.

---

## ⚠ Blast-radius warning

This is the **most widely consumed file in the app**. `Util.getRetrofit()` has **55 call sites**
across all 7 teams. Any signature change to a `RetrofitAPI` method breaks every caller at compile
time; any path or body change breaks them at runtime, silently.

**Every change here needs PM sign-off plus a named list of affected screens.** Prefer **adding** a
method over editing an existing one.

---

## 1. Identity

| Item | Value |
|---|---|
| Role | HTTP/API layer for the entire app |
| Files | `util/RetrofitAPI.kt` (178 lines, the interface), `util/APIUtil.kt` (26 lines, **dead**), `Util.getRetrofit()` + `Util.url` inside `util/Util.java` |
| Stack | Retrofit 2.9.0 + `converter-gson` 2.5.0 + OkHttp (declared without a version) |
| Return type | **every** method returns `Call<JsonObject?>?` (the three geo methods return `Call<JsonObject>?`) |
| Async style | `call.enqueue(object : retrofit2.Callback<JsonObject?> { ... })` — no coroutines, no RxJava |
| Endpoint count | **32 declarations** (`@POST` 11, `@GET` 17, `@PUT` 2, `@DELETE` 2) |

---

## 2. The two builders

### 2.1 `Util.getRetrofit()` — the real one (Java, in `Util.java`)

```java
public static String url = "https://server.salvationlamb.com";
// commented out: "https://salvationlamb-env.eba-smicznsb.ap-south-1.elasticbeanstalk.com"

public static RetrofitAPI getRetrofit() {
    if (retrofitAPI != null) return retrofitAPI;          // lazy singleton
    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();   // all defaults
    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build();
    retrofitAPI = retrofit.create(RetrofitAPI.class);
    return retrofitAPI;
}
```

| Fact | Detail |
|---|---|
| Call sites | **55** across the app |
| Caching | lazy singleton in a `private static RetrofitAPI` — built once per process, **never reset** |
| OkHttp config | **completely default**: no connect/read/write timeouts, no interceptors, no logging, no retry, no cache |
| Auth | **none at the client level** — every call that needs it passes `"Bearer $token"` as a `@Header` by hand |
| Thread safety | not synchronised; two simultaneous first calls could each build an instance (harmless but wasteful) |

> **Ownership note:** `getRetrofit()`, the `retrofitAPI` field and `Util.url` live in `Util.java`,
> which is otherwise owned by `COMMONS.md`. **This agent owns those three members only.**

### 2.2 `APIUtil.retrofit` — dead code (Kotlin)

```kotlin
object APIUtil {
    var url = "http://salvation-env.eba-nhpvydpr.us-east-1.elasticbeanstalk.com/"
    var header = ""
    ...identical builder...
}
```

**Verified: zero call sites outside its own file.** It points at a *different* host over **plain
HTTP**, and its `header` field is never read. It is a trap for anyone searching for "the Retrofit
setup" (issue N1).

---

## 3. Endpoint catalogue

Base URL `https://server.salvationlamb.com`; every path below is prefixed with `api/v1/`.
"Auth" = the method declares an `Authorization` header parameter.

### 3.1 POST

| Retrofit method | Path | Auth | Body | Primary callers |
|---|---|---|---|---|
| `postCall(url, body)` | `{url}` — **dynamic `@Path`** | no | `JsonObject` | `"login"` (LOGIN), `"users"` (REGISTER) |
| `postCallHead(head, url, body)` | `{url}` — dynamic | yes | `JsonObject` | generic authenticated POST |
| `postForgotPassword(body)` | `password/forgot-password` | no | `{email}` (+`isVerifyMail`) | FORGOT_PASSWORD, OTP_VERIFY |
| `postChangeForgotPassword(body)` | `password/confirm-password` | no | `{email, otp, password}` | RESET_PASSWORD |
| `postForgotPasswordOtp(body)` | `password/verify-otp` | no | `{email, otp}` | FORGOT_PASSWORD |
| `postVerifyUser(body)` | `users/verify-otp` | no | `{email, otp}` | OTP_VERIFY |
| `postChangePassword(head, body)` | `users/change-password` | yes | `{userId, oldPassword, newPassword}` | CHANGE_PASSWORD |
| `postWarrior(head, body)` | `users/warrior` | yes | `{userId, isWarrior, religion, churchName, gift}` | COMMONS `makeWarrior` |
| `postFollow(head, body)` | `follows` | yes | `{userId, followerId}` | PROFILE |
| `postFav(head, body)` | `favorites` | yes | `{userId, postId}` | FEED |
| `postProfilePic(head, userId, body)` | `Users/image/{userId}` | yes | `JsonObject` | PROFILE |

> **`postCall` is a dynamic-path method** — the first argument is substituted into `api/v1/{url}`.
> Two entirely different features (login, registration) share one declaration, so searching for the
> literal `"login"` is the only way to find its call sites (issue N2).

> **Casing inconsistency:** `Users/image/{userId}` and `Country` are capitalised while every other
> path is lower-case (issue N3).

### 3.2 GET

| Retrofit method | Path | Auth | Query | Primary callers |
|---|---|---|---|---|
| `getUser(head, userId)` | `users/{userId}` | yes | — | LOGIN, SPLASH, MAIN_NAV |
| `getFav(head, userId)` | `favorites/{userId}` | yes | — | FEED |
| `getMyFav(head, userId)` | `favorites/{userId}` | yes | — | **duplicate of `getFav`** (issue N4) |
| `getPostLike(head, postId)` | `like/post/{postId}` | yes | — | VIEW_LIKES |
| `getUserLikes(head, userId)` | `like/user/{userId}` | yes | — | FEED |
| `getMyPosts(head, userId, page, size)` | `post/user/{userId}` | yes | `page`, `size` | PROFILE |
| `getPost(head, page, size)` | `post` | yes | `page`, `size` | HOME_FEED |
| `getPost(head, postId)` | `post/{postId}` | yes | — | VIEW_POST — **overload** (issue N5) |
| `getVideoPost(head, page, size)` | `post/admin/video` | yes | `page`, `size` | ADMIN_VIDEO |
| `getAudioPost(head, page, size)` | `post/admin/audio` | yes | `page`, `size` | ADMIN_AUDIO |
| `getFollowers(head, userId)` | `follows/user/{userId}` | yes | — | FOLLOWERS |
| `getFollowing(head, userId)` | `follows/{userId}` | yes | — | FOLLOWERS |
| `getSearch(head, query)` | `search` | yes | `query` | SEARCH |
| `getFilesAndFolders(head, folderId)` | `files/{folderId}` | yes | — | FILES_BROWSER, FILE_LIST |
| `getCountries()` | `Country` | **no** | — | EDIT_PROFILE |
| `getState(countryID)` | `state/{countryID}` | **no** | — | EDIT_PROFILE |
| `getCity(stateId)` | `city/{stateId}` | **no** | — | EDIT_PROFILE |

> `getFollowers` uses `follows/user/{id}` while `getFollowing` uses `follows/{id}` — easy to swap by
> mistake; the names give no hint which is which (issue N6).

> The three geo endpoints are the **only** GETs without an auth header.

### 3.3 PUT / DELETE

| Retrofit method | Verb + path | Auth | Body | Callers |
|---|---|---|---|---|
| `putUser(head, userId, body)` | `PUT users/{userId}` | yes | full user object | EDIT_PROFILE |
| `putFreshUser(head, userId)` | `PUT users/freshUser/{userId}` | yes | **none** | PROFILE / APPSHELL |
| `deletePost(head, postId)` | `DELETE post/{postId}` | yes | — | FEED |
| `deleteUser(head, userId)` | `DELETE users/{userId}` | yes | — | SETTINGS |

---

## 4. Conventions every caller follows

```kotlin
if (Commons().isNetworkAvailable(ctx)) {                 // 1. guard (toasts internally)
    val retrofit = Util.getRetrofit()                    // 2. singleton
    val call: Call<JsonObject?>? = retrofit.getUser("Bearer $token", id)
    call!!.enqueue(object : retrofit2.Callback<JsonObject?> {   // 3. !! then enqueue
        override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
            if (response.code() == 200) {                // 4. literal 200, not isSuccessful
                val x = Gson().fromJson(response.body()?.get("result"), X::class.java)
            } else {
                val e = Gson().fromJson(response.errorBody()?.string(), JsonObject::class.java)
                val msg = e.get("errorMessage").toString()   // 5. keeps JSON quotes
            }
        }
        override fun onFailure(call: Call<JsonObject?>, t: Throwable) { Log.e(...) }
    })
}
```

| Convention | Detail |
|---|---|
| Success check | `response.code() == 200` — **201/204 are treated as errors** (issue N7) |
| Success payload | always under the `result` key |
| Error payload | `errorBody()` -> `{status, errorMessage}` |
| Header format | the string `"Bearer $token"` is built **at each of ~30 call sites** (issue N8) |
| Nullability | `call!!` is used everywhere; the interface's `?` returns are never actually null |
| Cancellation | a few screens call `call.cancel()` inside `onResponse`; almost none cancel on destroy |

---

## 5. Storage / global state

This module reads `Util.url` and caches `retrofitAPI`. It performs **no DataStore access** — tokens
are passed in by callers, which is precisely why there is no auth interceptor.

---

## 6. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| `com.squareup.retrofit2:retrofit:2.9.0` | PLATFORM / BUILD_CONFIG | the client |
| `com.squareup.retrofit2:converter-gson:2.5.0` | PLATFORM / BUILD_CONFIG | JSON conversion (**version skew**, issue N9) |
| `com.squareup.okhttp3:okhttp` (no version) | PLATFORM / BUILD_CONFIG | transport (issue N10) |
| `com.google.gson.JsonObject` | PLATFORM / DATA_MODELS | request bodies and raw responses |
| `Util.java` | PLATFORM / COMMONS | hosts `getRetrofit()` and `url` |
| `network_security_config.xml` | PLATFORM / BUILD_CONFIG | cleartext policy |

---

## 7. Known issues / tech debt

| # | Issue | Severity | Suggested fix |
|---|---|---|---|
| N11 | **No OkHttp timeouts** — a stalled connection hangs forever (this is what makes SPLASH's silent `onFailure` fatal) | **High** | set connect/read/write timeouts |
| N8 | No auth interceptor; `"Bearer $token"` is hand-built at ~30 call sites | **High** | add an `Interceptor` that injects the header |
| N15 | Every response is `Call<JsonObject?>`, so there is **no compile-time contract** with the backend (G7) | **High** | typed responses (with DATA_MODELS) |
| N1 | `APIUtil.kt` is a **second, dead Retrofit builder** pointing at a different host over plain HTTP (G4) | High | delete it, or make it the single source of truth |
| N12 | No logging interceptor, so failures can only be diagnosed by adding `Log.e` by hand | Medium | add `HttpLoggingInterceptor` on debug builds |
| N7 | Callers test `code() == 200`, so `201 Created` / `204 No Content` are treated as failures | Medium | use `response.isSuccessful` |
| N2 | `postCall` takes a **dynamic path**, hiding which endpoints exist and defeating search | Medium | declare explicit methods for `login` and `users` |
| N5 | Two `getPost` overloads differing only by parameters — easy to call the wrong one | Medium | rename to `getFeed` / `getPostById` |
| N6 | `getFollowers` -> `follows/user/{id}` vs `getFollowing` -> `follows/{id}`; names do not reveal paths | Medium | document or rename |
| N13 | `postProfilePic` sends an image inside a `JsonObject` rather than multipart | Medium | use `@Multipart` if the backend allows |
| N14 | No documented pagination contract: `page`/`size` exist on 4 endpoints but the response shape is unspecified | Medium | document with FEED |
| N4 | `getFav` and `getMyFav` are **identical** declarations of the same path | Low | delete one |
| N3 | Path casing is inconsistent (`Users/image/...`, `Country` vs `users/...`, `city/...`) | Low | align with the backend |
| N9 | `converter-gson:2.5.0` paired with `retrofit:2.9.0` | Low | align both to 2.9.0 |
| N10 | OkHttp is declared **without a version** | Low | pin it or use the BOM |
| N16 | The Retrofit singleton is never rebuilt, so `Util.url` cannot change at runtime | Low | add a reset for environment switching |

---

## 8. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/util/RetrofitAPI.kt` | **exclusive** |
| `app/src/main/java/com/veha/util/APIUtil.kt` | **exclusive** (dead code) |
| `Util.getRetrofit()`, the `retrofitAPI` field and `Util.url` in `util/Util.java` | **shared with `COMMONS.md`** — notify the lead |

**Not owned (escalate):** `DataModels.kt` (DATA_MODELS), `UserPreferences.kt` (STORAGE), the rest of
`Util.java` and `Commons.kt` (COMMONS), gradle dependency lines and `network_security_config.xml`
(BUILD_CONFIG), and **every screen** that calls these methods.

---

## 9. How to make common changes

**Add an endpoint:** add a method to `RetrofitAPI.kt` in the existing style — return
`Call<JsonObject?>?`, take `@Header(value = "Authorization") head: String` if it needs auth, keep the
`api/v1/` prefix. Tell PM which team requested it and which screen will call it; add a row to §3 here.

**Change a path or body:** this is a **breaking change**. List every caller first
(`grep -rn "<methodName>" app/src/main/java`), get PM sign-off, and notify each affected module agent
so they update their own `.md` in the same change set.

**Add timeouts or an interceptor (N11/N8/N12):** edit the `OkHttpClient.Builder()` chain inside
`Util.getRetrofit()`. This affects **every** request in the app — PM sign-off required. An auth
interceptor would also let ~30 call sites drop their manual header, but that is a multi-team cleanup,
not a NETWORK-only change.

**Change the base URL:** edit `Util.url`. Confirm with PM whether `network_security_config.xml`
(BUILD_CONFIG) must change too — it currently whitelists a **stale** Elastic Beanstalk host.

**Delete `APIUtil.kt` (N1):** verified to have zero external references; still needs PM sign-off
because it is a file deletion.

---

## 10. Change log

| Change | Detail |
|---|---|
| Created | Initial NETWORK module agent documented from `RetrofitAPI.kt` (178 lines), `APIUtil.kt` (26 lines) and `Util.getRetrofit()`: both builders, all 32 endpoint declarations grouped by verb with auth/body/caller columns, the 5-step caller convention, and 16 known issues. Verified **55 `getRetrofit()` call sites** and **zero** external `APIUtil` references. |



