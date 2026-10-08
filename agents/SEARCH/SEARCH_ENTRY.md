# SEARCH_ENTRY Module Agent

> Team: **SEARCH** · Reports to: `agents/SEARCH/SEARCH_LEAD.md`
> This agent knows **every detail** of the search screen: the query box, the two tabs, the
> per-keystroke request, the one endpoint and the pager that feeds both result fragments.
> It may only edit the files listed in §10 **Owned files**.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name | **Search** |
| Classes | `com.veha.activity.SearchActivity` (176 lines) + `com.veha.adapter.SearchAdapter` (33 lines) |
| Layout | `app/src/main/res/layout/activity_search.xml` (3 ids) |
| Manifest | `<activity android:name=".SearchActivity" android:windowSoftInputMode="stateVisible" android:exported="false"/>` — **one of only two activities with a `windowSoftInputMode`** |
| View access | `findViewById` |
| Required extras | **none** |
| Entered from | `MainActivity`'s search icon [APPSHELL] — the only caller |
| Exits to | `ViewProfileActivity` (via the Profiles tab), plus whatever `HomeAdapter` offers in the Posts tab |
| Progress | a `SpotsDialog` is built but **its `show()` is commented out** (issue SE1) |

---

## 2. UI inventory (`activity_search.xml`)

| View id | Type | Purpose |
|---|---|---|
| `search` | `EditText` | the query box — focused on open via `setEditTextFocus(search, true)` |
| `tabLayout` | `TabLayout` | two tabs, built in code |
| `viewPager` | `ViewPager` (**v1**, not `ViewPager2`) | hosts the two result fragments (issue SE2) |

### The tabs (created in `onCreate`)

| Position | `tag` | `text` | Fragment |
|---|---|---|---|
| 0 | `"Posts"` | **Posts** | `SearchPostFragment` |
| 1 | `"Profiles"` | **Profiles** | `SearchProfileFragment` |

`tabGravity = GRAVITY_FILL`. Tab and pager are wired both ways:
`viewPager.addOnPageChangeListener(TabLayoutOnPageChangeListener(tabLayout))` plus an
`OnTabSelectedListener` that sets `viewPager.currentItem` and records `currentTab`.

`onTabSelected` and `onTabReselected` contain **identical bodies**, and `onTabUnselected` is empty
(issue SE3).

### Keyboard handling

`setEditTextFocus(editText, isFocused)` toggles `isCursorVisible`, `isFocusable` and
`isFocusableInTouchMode`, then either requests focus or hides the IME. It is **only ever called with
`true`** (issue SE4). A commented-out `showSoftInput(..., SHOW_FORCED)` block also remains.

---

## 3. The search trigger

```kotlin
search.addTextChangedListener {
    if (it != null) {
        for (fragment in supportFragmentManager.fragments) {
            supportFragmentManager.beginTransaction().remove(fragment).commit()
        }
        getContent(it.toString())
    }
}
```

| Fact | Detail |
|---|---|
| Trigger | **every keystroke** — no debounce, no minimum length, no submit action (issue SE5) |
| Teardown | **every** fragment in the manager is removed before each request, so both tabs are destroyed and rebuilt per character (issue SE6) |
| Empty query | typing then deleting everything sends `query=""` (issue SE7) |
| Cancellation | the previous Retrofit call is **never cancelled**, so responses can arrive out of order and a stale result can overwrite a newer one (issue SE8) |

---

## 4. API contract — `getContent(text)`

| Item | Value |
|---|---|
| Retrofit | `getSearch("Bearer $token", text)` |
| HTTP | `GET api/v1/search?query=<text>` |
| Guard | `Commons().isNetworkAvailable(this)` |
| Token guard | **`&&`** — correct, like VIEW_LIKES and VIEW_POST |
| Progress | the `dialog.show()` call is **commented out**; only `dismiss()` remains (issue SE1) |

### The response shape (unique in the app)

```kotlin
val results: JsonObject = Gson().fromJson(resp?.get("results"), JsonObject::class.java)
val allProfiles: JsonArray = Gson().fromJson(results.get("users"), JsonArray::class.java)
val allPosts:    JsonArray = Gson().fromJson(results.get("posts"), JsonArray::class.java)
```

`results` is an **object**, not an array — a fourth envelope convention (team issue S-1). `posts`
become `Posts`, `users` become `PostUser`.

**Success (200)** — rebuilds both lists, then assigns a **brand-new `SearchAdapter`** to the pager
and restores `viewPager.currentItem = currentTab`.

**Non-200** — **no `else` branch**; the previous results stay on screen with no message (issue SE9).

**`onFailure`** — dismisses the dialog and logs. No user feedback.

**Token missing** — because the guard is `&&`, this branch is **reachable**: toast
`"Somthing Went Wrong \nLogin again to continue"`, clear the session, open `LoginActivity`.

Two leftover `Log.e("current tab", currentTab.toString())` calls remain (issue SE10).

---

## 5. `SearchAdapter`

```kotlin
internal class SearchAdapter(context, fm, totalTabs, profilelist, postlist) : FragmentPagerAdapter(fm!!) {
    override fun getItem(position: Int): Fragment = when (position) {
        1 -> SearchProfileFragment(context, profilelist)
        0 -> SearchPostFragment(context, postlist)
        else -> null as Fragment            // <-- throws if ever reached
    }
    override fun getCount(): Int = totalTabs
}
```

| Fact | Detail |
|---|---|
| `else -> null as Fragment` | an **immediate `NullPointerException`** if a third tab is ever added (issue SE11) |
| Branch order | `1` is listed before `0` — harmless but odd |
| Data passing | result lists go in as **constructor arguments**, so the fragments cannot be recreated by the system (team issue S-2) |
| Base class | deprecated `FragmentPagerAdapter` with the no-arg `super(fm)` constructor (issue SE12) |
| `getItemPosition` | not overridden, so a rebuilt adapter may reuse stale fragments — masked here because the whole adapter is replaced |

---

## 6. Storage & global state

| Item | Operation |
|---|---|
| DataStore `token` | read — **a new observer per keystroke** (issue SE13) |
| DataStore `token` + `userId` | deleted in the reachable session-lost branch |
| `Util.*` | not touched directly |

---

## 7. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | layout -> log -> `UserPreferences` -> SpotsDialog -> bind `tabLayout` and `search` -> focus the box -> build 2 tabs -> bind `viewPager` -> wire tab/pager listeners -> attach the text watcher |
| `onDestroy` | `dialog.dismiss()` |

**No rotation handling** — and because the fragments were constructed with arguments, a rotation or
process-death restore can resurrect them **without their data** (team issue S-2). `currentTab` is
also not saved.

---

## 8. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| `RetrofitAPI.getSearch` | PLATFORM / NETWORK | the only call |
| `Posts`, `PostUser` | PLATFORM / DATA_MODELS | parsing |
| `UserPreferences` | PLATFORM / STORAGE | token |
| `Commons().isNetworkAvailable` | PLATFORM / COMMONS | guard |
| `SearchPostFragment`, `SearchProfileFragment` | SEARCH siblings | the two tabs |
| `MainActivity` | APPSHELL / MAIN_NAV | the only entry point |
| `LoginActivity` | AUTH | session-lost target |

---

## 9. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| SE5 | A request fires on **every keystroke** — no debounce, no minimum length | **High** | debounce ~300 ms, or search on IME action |
| SE6 | **All fragments are removed** before each request, destroying both tabs per character | **High** | update the adapter's data instead of rebuilding |
| SE8 | Previous calls are never cancelled — out-of-order responses can show stale results | **High** | cancel the in-flight call before starting a new one |
| SE13 | A new `authToken` observer per keystroke, never removed | **High** | read the token once |
| SE11 | `null as Fragment` in `SearchAdapter.getItem` | **High** | throw a clear exception, or handle all positions |
| SE9 | A non-200 has **no `else` branch** — stale results, no message | **High** | add an error state |
| SE1 | The progress dialog's `show()` is commented out, so searching is silent | Medium | restore it, or add inline progress |
| SE7 | An empty query is still sent | Medium | guard on blank input |
| SE2 | Uses `ViewPager` v1 + deprecated `FragmentPagerAdapter` | Medium | migrate to `ViewPager2` |
| SE12 | `FragmentPagerAdapter(fm!!)` without a behaviour flag | Medium | pass `BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT` |
| SE14 | `currentTab` is not saved across configuration change | Medium | `onSaveInstanceState` |
| SE4 | `setEditTextFocus` is only ever called with `true`; its `false` path is dead | Low | simplify |
| SE3 | `onTabSelected` and `onTabReselected` are identical; `onTabUnselected` is empty | Low | tidy |
| SE10 | Two `Log.e("current tab", ...)` debug calls | Low | remove |
| SE15 | No search history, no recent queries, no clear button | Low (product) | ask the customer |

---

## 10. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/SearchActivity.kt` | **exclusive** |
| `app/src/main/java/com/veha/adapter/SearchAdapter.kt` | **exclusive** |
| `app/src/main/res/layout/activity_search.xml` | **exclusive** |
| `<activity android:name=".SearchActivity">` in the manifest | shared with PLATFORM/BUILD_CONFIG |

**Not owned (escalate):** `SearchPostFragment.kt` / `SearchProfileFragment.kt` (SEARCH siblings),
`HomeAdapter.kt` (FEED), `UsersAdapter.kt` (SEARCH_PROFILES), all PLATFORM files,
`MainActivity.kt` (APPSHELL).

---

## 11. How to make common changes

**Add debounce (SE5) — highest value here:** keep a `Handler`/coroutine job, cancel it on each
keystroke and run the search after ~300 ms of silence. Also cancel the in-flight Retrofit call
(SE8). Local to this agent and it fixes the request storm at its source.

**Stop destroying the tabs (SE6):** instead of removing fragments and building a new
`SearchAdapter`, give the fragments a `setData(...)` method and call `notifyDataSetChanged()`. That
touches both sibling agents — coordinate through the lead.

**Add a third tab:** add it to `tabLayout`, extend `SearchAdapter.getItem` (**and remove the
`null as Fragment` branch**), and create the fragment. New fragments should take a `Bundle`, not
constructor arguments (team issue S-2).

**Show progress (SE1):** uncomment the `dialog.show()`, or better, add an inline indicator so the
screen does not block while typing.

**Change the response parsing:** the `results { users, posts }` shape is a **NETWORK contract** —
escalate before changing it.

---

## 12. Change log

| Change | Detail |
|---|---|
| Created | Initial SEARCH_ENTRY module agent documented from `SearchActivity.kt` (176 lines), `SearchAdapter.kt` (33 lines) and `activity_search.xml` (3 ids): the two code-built tabs, the **per-keystroke search with full fragment teardown**, the unique `results { users, posts }` envelope, the `null as Fragment` branch, the correct `&&` token guard with a reachable logout path, and 15 known issues. |


