# SEARCH_POSTS Module Agent

> Team: **SEARCH** · Reports to: `agents/SEARCH/SEARCH_LEAD.md`
> This agent knows **every detail** of the Posts results tab — a 47-line fragment that renders a
> list it is handed.
> It may only edit the files listed in §8 **Owned files**.

---

## ⚠ This fragment makes no network call

`SearchPostFragment` receives its data through its **constructor** from `SearchAdapter`
(SEARCH_ENTRY). It performs **no** request, no parsing and no paging.

If results are wrong, missing or stale, the cause is in **`SEARCH_ENTRY.md`**. If the *cards* look
or behave wrong, the cause is in **`HOME_FEED.md`**. This agent owns only the thin layer between
them.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name | **Search -> Posts** (tab 0) |
| Class | `com.veha.fragments.SearchPostFragment` (47 lines) |
| Layout | `app/src/main/res/layout/fragment_search_post.xml` (2 ids) |
| Instantiation | **`SearchPostFragment(contexts: Context, postList: ArrayList<Posts>)`** — constructor arguments, no `Bundle` |
| Hosted by | `SearchAdapter` position **0** |
| View access | `findViewById` |
| Content | **FEED's `HomeAdapter`** with page `"searchProfile"` |
| Network | **none** |
| Progress | none |

---

## 2. UI inventory (`fragment_search_post.xml`)

| View id | Type | Purpose |
|---|---|---|
| `list` | `RecyclerView` | the post results |
| `no_data` | `LinearLayout` | empty state |

---

## 3. The whole fragment

```kotlin
userPreferences = UserPreferences(contexts)          // created, never used (issue SP1)
val view = inflater.inflate(R.layout.fragment_search_post, container, false)
list   = view.findViewById(R.id.list)
nodata = view.findViewById(R.id.no_data)

if (postList.size <= 0) {
    list.visibility = View.GONE; nodata.visibility = View.VISIBLE
} else {
    list.visibility = View.VISIBLE; nodata.visibility = View.GONE
    list.layoutManager = LinearLayoutManager(contexts)
    list.adapter = HomeAdapter(postList, contexts, "searchProfile", HashMap(), HashMap(), HashMap(), this)
}
```

### The adapter handoff

| Argument | Value | Consequence |
|---|---|---|
| posts | `postList` | the real results, passed straight through |
| page | **`"searchProfile"`** | hides follow, delete, fav and save (FEED_LEAD §3) |
| 3 state maps | **`HashMap()`, `HashMap()`, `HashMap()`** | **all three empty** — no like, follow or fav state renders (issue SP2) |

> The page value is **`"searchProfile"`** even though this is the **Posts** tab. It happens to
> produce the right "read-only card" behaviour, but the name is misleading (team issue S-6).

Because all three maps are empty, search results show posts with **no reaction state at all**. That
is arguably acceptable for a read-only tab, but it is accidental rather than designed — MY_PROFILE
has the same pattern (MP4).

---

## 4. Storage & global state

| Item | Operation |
|---|---|
| `UserPreferences` | **instantiated and never used** (issue SP1) |
| everything else | none directly; `HomeAdapter` touches `Util.*` on its own |

---

## 5. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | `super` only |
| `onCreateView` | the code in §3 — bind, branch on emptiness, build the adapter |
| others | **none overridden** |

### The constructor-argument problem

```kotlin
class SearchPostFragment(private val contexts: Context, private val postList: ArrayList<Posts>) : Fragment()
```

Android recreates fragments with a **no-argument constructor** after a configuration change or
process death. This class has none, so a restore **throws
`InstantiationException` / "Fragment ... could not be instantiated"** (team issue S-2, issue SP3).

Today the crash is masked because `SearchActivity` removes every fragment on each keystroke and
rebuilds the adapter — but a rotation with results on screen is enough to hit it.

Holding a `Context` in a field is a second leak risk (issue SP4).

---

## 6. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| **`HomeAdapter` + `child_post.xml`** | **FEED / HOME_FEED** | the entire card rendering |
| `Posts` | PLATFORM / DATA_MODELS | the list type |
| `UserPreferences` | PLATFORM / STORAGE | unused |
| `SearchActivity` / `SearchAdapter` | SEARCH / SEARCH_ENTRY | supplies the data |

---

## 7. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| SP3 | **Constructor arguments** instead of a `Bundle` — the fragment cannot be recreated by the system | **High** | `newInstance()` + `Bundle`; make `Posts` parcelable or pass ids |
| SP2 | All three `HomeAdapter` state maps are **empty**, so no like/follow/fav state renders | Medium | pass real maps, or confirm read-only is intended |
| SP4 | A `Context` is held in a field | Medium | use `requireContext()` |
| SP6 | No pagination — only the first page of search results is ever shown | Medium | needs `getSearch` paging (NETWORK) |
| SP8 | The empty state cannot distinguish "no results" from "search failed" | Medium | parent must pass an error flag (SEARCH_ENTRY) |
| SP1 | `UserPreferences` is created and never used | Low | delete |
| SP5 | The page value `"searchProfile"` is misleading on the Posts tab | Low | rename with FEED (shared contract) |
| SP7 | No pull-to-refresh and no loading state | Low | the parent owns loading; acceptable as-is |

---

## 8. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/fragments/SearchPostFragment.kt` | **exclusive** |
| `app/src/main/res/layout/fragment_search_post.xml` | **exclusive** |

**Not owned (escalate):** `HomeAdapter.kt` / `child_post.xml` (**FEED**), `SearchActivity.kt` /
`SearchAdapter.kt` (SEARCH_ENTRY), `DataModels.kt` (PLATFORM).

---

## 9. How to make common changes

**Fix the recreation crash (SP3) — highest value here:** add a
`companion object { fun newInstance(...) }` that stores the data in a `Bundle`, and give the class a
no-arg constructor. `Posts` would need to be `Parcelable`/`Serializable` (a **PLATFORM/DATA_MODELS**
change) or the fragment could re-query by id. Coordinate with SEARCH_ENTRY, which currently
constructs it directly — and apply the same fix to `SEARCH_PROFILES.md`.

**Show reaction state (SP2):** requires the same like/follow/fav maps `HomeFragment` builds, which
means network calls this fragment does not make. Ask PM whether search results should be
interactive at all.

**Change how a result card looks:** that is `HomeAdapter` — **FEED/HOME_FEED via PM**. From here you
can only change the `page` string.

**Distinguish "no results" from "error" (SP8):** `SearchActivity` would need to pass a state flag —
a two-agent change inside SEARCH.

---

## 10. Change log

| Change | Detail |
|---|---|
| Created | Initial SEARCH_POSTS module agent documented from `SearchPostFragment.kt` (47 lines) and `fragment_search_post.xml` (2 ids): the pass-through role with **no network call**, the `HomeAdapter("searchProfile")` handoff with **three empty state maps**, the unused `UserPreferences`, and the **constructor-argument recreation crash**. 8 known issues. |

