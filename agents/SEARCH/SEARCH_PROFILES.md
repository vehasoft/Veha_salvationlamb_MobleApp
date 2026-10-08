# SEARCH_PROFILES Module Agent

> Team: **SEARCH** · Reports to: `agents/SEARCH/SEARCH_LEAD.md`
> This agent knows **every detail** of the Profiles results tab **and** of `UsersAdapter` — the
> adapter that renders a person row and opens their profile.
> It may only edit the files listed in §9 **Owned files**.

---

## ⚠ Two ownership notes

1. **`UsersAdapter` belongs to SEARCH, not PROFILE.** It sits beside `FollowAdapter` in the
   `adapter/` package, but its **only** call site is `SearchProfileFragment`. Verified.
2. **It inflates PROFILE's layout.** `onCreateViewHolder` inflates **`R.layout.child_follow`**,
   which is owned by `FOLLOWERS.md`. Any change to that layout affects this tab — and vice versa.
   **Always escalate layout changes to PM** (team issue S-7).

Like its sibling, this fragment makes **no network call** — it renders what `SearchAdapter` hands it.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name | **Search -> Profiles** (tab 1) |
| Classes | `com.veha.fragments.SearchProfileFragment` (43 lines) + `com.veha.adapter.UsersAdapter` (56 lines) |
| Layouts | `fragment_search_profile.xml` (2 ids) + **`child_follow.xml`** (PROFILE's, 4 ids) |
| Instantiation | **`SearchProfileFragment(contexts: Context, profileList: ArrayList<PostUser>)`** — constructor arguments |
| Hosted by | `SearchAdapter` position **1** |
| View access | `findViewById` |
| Network | **none** |
| Progress | none |

---

## 2. UI inventory

### `fragment_search_profile.xml`

| View id | Type | Purpose |
|---|---|---|
| `search_profile_list` | `RecyclerView` | the profile results |
| `no_data` | `LinearLayout` | empty state |

> Note the list id differs from the Posts tab's (`list`) — the two sibling layouts are inconsistent
> (issue SPR1).

### `child_follow.xml` — **owned by PROFILE/FOLLOWERS**

| View id | Used here? | Bound to |
|---|---|---|
| `follow_list_linear` | yes | row container; tap -> `ViewProfileActivity` (extra `userId` = `follow.id`) |
| `profile_pic_fol` | yes | `follow.picture` via Picasso (only if non-empty) |
| `name_fol` | yes | `follow.name` |
| **`follow_btn`** | **no** | its binding is **commented out** in `UsersAdapter.ViewHolder` (issue SPR2) |

So a search result row reuses the follower row but **silently leaves its follow button unbound** —
the button is still inflated and visible unless the layout hides it by default.

---

## 3. The fragment

```kotlin
val view = inflater.inflate(R.layout.fragment_search_profile, container, false)
lists  = view.findViewById(R.id.search_profile_list)
nodata = view.findViewById(R.id.no_data)

if (profileList.size <= 0) {
    lists.visibility = View.GONE; nodata.visibility = View.VISIBLE
} else {
    lists.visibility = View.VISIBLE; nodata.visibility = View.GONE
    lists.layoutManager = LinearLayoutManager(context)      // <- `context`, not `contexts`
    lists.adapter = UsersAdapter(profileList, contexts, this)
}
```

| Detail | Note |
|---|---|
| Two contexts | the layout manager uses the **fragment's `context`** while the adapter gets the **constructor `contexts`** — inconsistent, and `context` can be null during recreation (issue SPR3) |
| No `UserPreferences` | unlike `SearchPostFragment`, this one does not create an unused instance |
| `this` as owner | passed as a `LifecycleOwner` to the adapter, which **never uses it** (issue SPR4) |

---

## 4. `UsersAdapter`

```kotlin
class UsersAdapter(
    private val follows: ArrayList<PostUser>,
    private val context: Context,
    private var owner: LifecycleOwner          // never used
) : RecyclerView.Adapter<UsersAdapter.ViewHolder>()
```

`onBindViewHolder` sets the name, loads the avatar when non-empty, and wires the row to
`ViewProfileActivity` with the `userId` extra. That is all — **no follow action, no network, no
state**.

### Compared with `FollowAdapter` (PROFILE)

| | `UsersAdapter` (here) | `FollowAdapter` (PROFILE) |
|---|---|---|
| Layout | `child_follow.xml` | `child_follow.xml` (**same**) |
| Row tap | -> `ViewProfileActivity` | -> `ViewProfileActivity` (**same**) |
| Follow button | **not bound** | bound, with a 3-part bug (FL6/FL7/FL8) |
| Network | none | `POST follows` |
| Constructor | primary, 3 params | primary, 4 params |

`UsersAdapter` is effectively **`FollowAdapter` minus the follow button** — and by omitting it, it
avoids that adapter's broken state logic entirely (issue SPR5).

Uses the deprecated `Picasso.with(context)` (issue SPR6).

---

## 5. Storage & global state

| Item | Operation |
|---|---|
| — | **none** |

Neither the fragment nor the adapter touches DataStore, `Util` or Retrofit — making this the
**cleanest list screen in the app**, simply because it does so little.

---

## 6. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | `super` only |
| `onCreateView` | bind, branch on emptiness, build the adapter |
| others | **none overridden** |

**Same recreation crash as its sibling:** constructor arguments mean Android cannot restore this
fragment after a configuration change or process death (team issue S-2, issue SPR7). Holding a
`Context` field is the same leak risk (issue SPR8).

---

## 7. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| **`child_follow.xml`** | **PROFILE / FOLLOWERS** | the row layout — cross-team |
| `ViewProfileActivity` + the `userId` extra | PROFILE / VIEW_PROFILE | row navigation |
| `PostUser` | PLATFORM / DATA_MODELS | the list type |
| Picasso | PLATFORM / BUILD_CONFIG | avatars |
| `SearchActivity` / `SearchAdapter` | SEARCH / SEARCH_ENTRY | supplies the data |

---

## 8. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| SPR7 | **Constructor arguments** instead of a `Bundle` — cannot be recreated by the system | **High** | `newInstance()` + `Bundle` |
| SPR2 | `UsersAdapter` reuses PROFILE's `child_follow.xml` but leaves **`follow_btn` unbound** (commented out) | **High** | hide it explicitly, or use a dedicated layout |
| SPR3 | The layout manager uses `context` while the adapter uses `contexts` — two sources, one nullable | Medium | pick one |
| SPR8 | A `Context` is held in a field | Medium | `requireContext()` |
| SPR5 | `UsersAdapter` and `FollowAdapter` are near-duplicates differing only by the follow button | Medium | merge with a flag (cross-team, PM) |
| SPR9 | No follow action from search results, though the row layout has a button | Medium (product) | confirm with the customer |
| SPR10 | No pagination — only the first page of profile results | Medium | needs `getSearch` paging (NETWORK) |
| SPR11 | The empty state cannot distinguish "no results" from "search failed" | Medium | parent must pass a flag |
| SPR4 | The `LifecycleOwner` parameter is never used | Low | remove |
| SPR6 | Deprecated `Picasso.with(context)` | Low | upgrade |
| SPR1 | The list id (`search_profile_list`) differs from the Posts tab's (`list`) | Low | align naming |

---

## 9. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/fragments/SearchProfileFragment.kt` | **exclusive** |
| `app/src/main/java/com/veha/adapter/UsersAdapter.kt` | **exclusive** (SEARCH, despite its package neighbours) |
| `app/src/main/res/layout/fragment_search_profile.xml` | **exclusive** |

**Not owned (escalate):** **`child_follow.xml` (PROFILE / FOLLOWERS)** — inflated here but owned
there; `FollowAdapter.kt` (PROFILE), `ViewProfileActivity.kt` (PROFILE), `SearchActivity.kt` /
`SearchAdapter.kt` (SEARCH_ENTRY), `DataModels.kt` (PLATFORM).

---

## 10. How to make common changes

**Fix the recreation crash (SPR7):** same remedy as `SEARCH_POSTS.md` — `newInstance()` + `Bundle`,
no-arg constructor. Do both tabs together.

**Give search results their own row layout (SPR2/SPR5):** create `child_user.xml` here instead of
borrowing `child_follow.xml`. That **removes** a cross-team dependency rather than adding one —
a good trade, but tell PM since FOLLOWERS currently assumes it is the only consumer.

**Add a follow button to search results (SPR9):** the layout already has `follow_btn`; binding it
would mean replicating `FollowAdapter.follow()` — **do not copy that code**, it carries the FL6–FL8
bug chain. Escalate so PROFILE's fix lands first.

**Change what a row shows:** if it only needs fields already in `PostUser`, bind them here; if the
**layout** must change, that is `child_follow.xml` -> **PROFILE/FOLLOWERS via PM**.

---

## 11. Change log

| Change | Detail |
|---|---|
| Created | Initial SEARCH_PROFILES module agent documented from `SearchProfileFragment.kt` (43 lines), `UsersAdapter.kt` (56 lines) and `fragment_search_profile.xml` (2 ids). Confirmed **`UsersAdapter` belongs to SEARCH** despite its package location, and that it **inflates PROFILE's `child_follow.xml` with `follow_btn` left unbound**. Compared it line-for-line with `FollowAdapter`, and recorded the shared constructor-argument recreation crash. 11 known issues. |


