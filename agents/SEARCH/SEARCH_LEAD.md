# SEARCH TEAM LEAD Agent

> Reports to: **PROJECT MANAGER** (`AGENTS.md`)
> Manages the 3 module agents below. **Status: all READY.**

---

## 1. Charter

SEARCH owns the **single search screen** and its two result tabs: Posts and Profiles.

It is the **smallest team** — one activity, two fragments, two adapters, ~300 lines total — and the
only team whose entire feature is driven by **one endpoint**.

**Boundary:** SEARCH owns the query box, the tabs and the result lists. It renders results using
**FEED's `HomeAdapter`** (posts) and its **own `UsersAdapter`** (profiles), and it navigates into
**PROFILE**. It owns no models, no endpoints and no post behaviour.

---

## 2. Modules owned

| Module agent | Screen / unit | Source files | Status |
|---|---|---|---|
| `SEARCH_ENTRY.md` | Search screen + tabs | `activity/SearchActivity.kt` (176), `adapter/SearchAdapter.kt` (33), `res/layout/activity_search.xml` (3 ids) | READY |
| `SEARCH_POSTS.md` | Posts tab | `fragments/SearchPostFragment.kt` (47), `res/layout/fragment_search_post.xml` (2 ids) | READY |
| `SEARCH_PROFILES.md` | Profiles tab | `fragments/SearchProfileFragment.kt` (43), `adapter/UsersAdapter.kt` (56), `res/layout/fragment_search_profile.xml` (2 ids) | READY |

> **`UsersAdapter` belongs here, not to PROFILE.** It sits beside `FollowAdapter` in the `adapter/`
> package and even inflates PROFILE's `child_follow.xml`, but its **only** call site is
> `SearchProfileFragment`. Verified. Both `PROFILE_LEAD.md` and `FOLLOWERS.md` record this.

---

## 3. Architecture

```
MainActivity search icon [APPSHELL]
   `-> SearchActivity
          search EditText --(every keystroke)--> GET api/v1/search?query=...
                 |
                 | response: results { users: [...], posts: [...] }
                 v
          SearchAdapter (FragmentPagerAdapter, 2 tabs)
             tab 0 "Posts"    -> SearchPostFragment(context, postlist)
             |                      `-> HomeAdapter(page = "searchProfile")   [FEED]
             tab 1 "Profiles" -> SearchProfileFragment(context, profilelist)
                                    `-> UsersAdapter -> ViewProfileActivity   [PROFILE]
```

### One endpoint, two lists

`GET api/v1/search` returns **both** result types in a single response:

```kotlin
val results: JsonObject = Gson().fromJson(resp?.get("results"), JsonObject::class.java)
val allProfiles: JsonArray = Gson().fromJson(results.get("users"), JsonArray::class.java)
val allPosts:    JsonArray = Gson().fromJson(results.get("posts"), JsonArray::class.java)
```

> **A fourth envelope shape.** AUTH uses `result` (object), FEED uses `results` (array) + `count`,
> MEDIA uses `files` (array) — and SEARCH uses **`results` as an *object*** containing `users` and
> `posts` arrays (team issue S-1).

Results are parsed into `Posts` and `PostUser` and **passed to the fragments via constructor
arguments**, not a `Bundle` (team issue S-2).

---

## 4. API surface

| Endpoint | Retrofit method | Used by |
|---|---|---|
| `GET api/v1/search?query=<text>` | `getSearch(head, query)` | SEARCH_ENTRY — **the only call in the whole team** |

`SearchPostFragment` and `SearchProfileFragment` make **no network calls at all**; they render what
they are handed.

---

## 5. Shared state

| State | Who | Note |
|---|---|---|
| DataStore `token` | SEARCH_ENTRY | a fresh observer **per keystroke** (team issue S-3) |
| DataStore `token` + `userId` | deleted in the (reachable) session-lost branch | SEARCH_ENTRY's token guard is a correct `&&` |
| `Util.*` | only indirectly, via `HomeAdapter` | SEARCH writes no global state |

---

## 6. Cross-team dependencies

| Needs | Owner | Escalate when |
|---|---|---|
| `HomeAdapter` + `child_post.xml` (the Posts tab) | **FEED / HOME_FEED** | always |
| the `"searchProfile"` page value | FEED / HOME_FEED | always — SEARCH only passes the string |
| `child_follow.xml` (reused by `UsersAdapter`) | **PROFILE / FOLLOWERS** | always — SEARCH does not own that layout |
| `ViewProfileActivity` + the `userId` extra | PROFILE / VIEW_PROFILE | when the extra changes |
| `getSearch` endpoint or response shape | PLATFORM / NETWORK | always |
| `Posts`, `PostUser` | PLATFORM / DATA_MODELS | always |
| the search entry point (icon in `MainActivity`) | APPSHELL / MAIN_NAV | always |

---

## 7. Definition of done (team-level)

- [ ] Both tabs verified — a change to the shared response parsing affects both.
- [ ] The `results.users` / `results.posts` object shape respected (not FEED's array shape).
- [ ] If `HomeAdapter` or `child_follow.xml` is involved, escalate to FEED / PROFILE via PM.
- [ ] Module `.md` updated in the same change, including its Change log.

---

## 8. Team-level known issues

| # | Issue | Module | Risk |
|---|---|---|---|
| S-2 | Both fragments take **constructor arguments** instead of a `Bundle` — Android cannot recreate them after process death | SEARCH_POSTS, SEARCH_PROFILES | **crash on restore** |
| S-3 | A search fires on **every keystroke** with no debounce, and registers a new `authToken` observer each time | SEARCH_ENTRY | request storm |
| S-4 | On every keystroke all fragments are **removed and the pager adapter rebuilt** | SEARCH_ENTRY | flicker, lost scroll |
| S-1 | A **fourth response-envelope shape** (`results` as an object with `users` + `posts`) | SEARCH_ENTRY | parse confusion |
| S-5 | `SearchAdapter.getItem` returns `null as Fragment` for an out-of-range position | SEARCH_ENTRY | crash if a tab is added |
| S-7 | `UsersAdapter` inflates **PROFILE's `child_follow.xml`** and hides its `follow_btn` by commenting out the binding | SEARCH_PROFILES | cross-team layout coupling |
| S-6 | The Posts tab passes page **`"searchProfile"`** — a misleading name for post results | SEARCH_POSTS | confusing, but matches FEED's contract |
| S-8 | No empty-query guard: clearing the box searches for `""` | SEARCH_ENTRY | pointless request |
| S-9 | The progress dialog is **commented out**, so searches are silent | SEARCH_ENTRY | no feedback |

---

## 9. Change log

| Change | Detail |
|---|---|
| Created | Initial SEARCH team lead agent: charter, 3 modules, the single-endpoint architecture with its **fourth envelope shape**, cross-team rules (posts render via FEED, profile rows reuse PROFILE's layout, navigation lands in PROFILE), and 9 team-level issues. Confirmed `UsersAdapter` belongs to SEARCH, not PROFILE. |
| SEARCH team complete | `SEARCH_ENTRY.md`, `SEARCH_POSTS.md` and `SEARCH_PROFILES.md` written and verified. Key findings escalated to PM: **both result fragments use constructor arguments and cannot survive recreation**; the search fires on **every keystroke** while removing all fragments and rebuilding the pager; `SearchAdapter` contains a `null as Fragment` branch; and `UsersAdapter` inflates PROFILE's `child_follow.xml` with `follow_btn` left unbound. **All 3 SEARCH module agents are READY.** |

