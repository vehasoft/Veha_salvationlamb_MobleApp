# FILES_BROWSER Module Agent

> Team: **MEDIA** · Reports to: `agents/MEDIA/MEDIA_LEAD.md`
> This agent knows **every detail** of the Files tab — the root of the admin file library: its
> list/grid toggle, the folder fetch, and the handoff to `FileAdapter`.
> It may only edit the files listed in §9 **Owned files**.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name | **Files** (bottom-nav tab 1) |
| Class | `com.veha.fragments.FilesFragment` (168 lines) |
| Layout | `app/src/main/res/layout/fragment_files.xml` (4 ids) |
| Instantiation | `FilesFragment()` — **no arguments, no `getInstance`**, unlike the other tab fragments |
| Hosted by | `TabAdapter` position **1** [APPSHELL] |
| View access | `findViewById` |
| Exits to | `FileListActivity` (folder) and `PdfActivity2` (file) — **both launched by `FileAdapter`, not by this fragment** |
| Progress | `SpotsDialog`, "Please Wait", dismissed immediately after creation |

---

## 2. UI inventory (`fragment_files.xml`)

| View id | Type | Purpose |
|---|---|---|
| `file_head_layout` | container | header strip |
| `view_icon` | `ImageView` | **list/grid toggle** — see §3 |
| `recycler_view` | `RecyclerView` | the file/folder list |
| `no_data` | view | empty state (the variable is named `noFilesText`) |

A commented-out `header_main` lookup (with `visibility = GONE`) remains in `onCreateView`
(issue FB1).

---

## 3. The list/grid toggle

```kotlin
if (Util.listview) listIcon.setImageResource(R.drawable.ic_baseline_list_24)
else               listIcon.setImageResource(R.drawable.ic_baseline_grid_on_24)

listIcon.setOnClickListener {
    Util.listview = !Util.listview
    requireFragmentManager().beginTransaction().detach(this).attach(this).commit()
}
```

| Fact | Detail |
|---|---|
| State | **`Util.listview`** — a global `boolean` in PLATFORM/COMMONS, default `true` |
| Applied in | the response handler: `LinearLayoutManager` when true, `GridLayoutManager(context, 3)` when false |
| Toggle mechanism | **`detach().attach()`** — a full fragment recreate that **re-fetches the whole folder** just to change the layout manager (issue FB2) |
| Deprecated | `requireFragmentManager()` (issue FB3) |
| Icon logic | the icon shows the **current** mode, not the one you would switch to — arguably inverted (issue FB4) |
| Persistence | `Util.listview` is **not persisted**, so the choice resets on process death (issue FB5) |

---

## 4. API contract — `getFilesAndFolder(folderID, context)`

| Item | Value |
|---|---|
| Retrofit | `getFilesAndFolders("Bearer $token", folderID)` |
| HTTP | `GET api/v1/files/{folderId}` |
| Argument here | **`""`** — an empty string, so the request path is `api/v1/files/` to fetch the root (issue FB6) |
| Guard | `Commons().isNetworkAvailable(context)` |
| Token | fresh `authToken` observer + the **always-true `||` guard** (issue FB7) |

### ⚠ The envelope is `files`, not `results`

```kotlin
val loginresp: JsonArray = Gson().fromJson(resp?.get("files"), JsonArray::class.java)
```

Every other list in the app reads **`results`** (FEED) or **`result`** (AUTH). This endpoint uses a
**third key, `files`** — unique to the file API, and shared only with `FILE_LIST.md` (issue FB8).
Each element is parsed as `FilesAndFolders`.

**Success (200)** — rebuilds the list; empty -> `no_data` visible, otherwise sets the layout manager
per `Util.listview` and assigns a **new** `FileAdapter` on every load (issue FB9).

**Non-200** — **no `else` branch at all**: the dialog is dismissed and the screen keeps whatever was
on it, with no message (issue FB10).

**`onFailure`** — dismisses the dialog and logs. No user feedback.

---

## 5. Storage & global state

| Item | Operation |
|---|---|
| DataStore `token` | read (observed) |
| DataStore `token` + `userId` | deleted in the unreachable logout branch |
| **`Util.listview`** | **read and written** — the toggle |

This fragment does **not** touch `Util.player` (no audio here) or `Util.user`.

---

## 6. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | `super` only |
| `onCreateView` | inflate -> `UserPreferences` -> SpotsDialog (dismissed) -> bind `view_icon` + set its image from `Util.listview` -> wire the toggle -> bind `recycler_view` and `no_data` -> **`getFilesAndFolder("", contexts)`** |
| `onPause` / `onResume` / `onDestroy` | `dialog.dismiss()` |

No `setUserVisibleHint` override (unlike `ProfileFragment`), so the tab does **not** reload on every
visit — only when the toggle is tapped.

No rotation handling: the fragment is recreated and the folder re-fetched.

---

## 7. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| `FileAdapter` | **MEDIA / FILE_LIST** | renders rows and owns all navigation |
| `RetrofitAPI.getFilesAndFolders` | PLATFORM / NETWORK | the fetch |
| `FilesAndFolders` | PLATFORM / DATA_MODELS | parsing |
| `UserPreferences` | PLATFORM / STORAGE | token |
| **`Util.listview`**, `Commons()` | PLATFORM / COMMONS | toggle state + guard |
| `ic_baseline_list_24`, `ic_baseline_grid_on_24` | APPSHELL / THEMING | toggle icons |
| `TabAdapter` | APPSHELL / MAIN_NAV | hosting |
| `FileListActivity`, `PdfActivity2` | MEDIA siblings | destinations (via the adapter) |

---

## 8. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| FB2 | The list/grid toggle does `detach().attach()`, **re-fetching the entire folder** just to swap the layout manager | **High** | swap `layoutManager` in place and call `notifyDataSetChanged` |
| FB10 | A non-200 has **no `else` branch** — no error, no empty state, stale content stays | **High** | add an error state |
| FB7 | The always-true `||` token guard; the logout branch is dead | **High** | use `&&` |
| FB11 | The Retrofit call is never cancelled; the callback touches views after detach | **High** | cancel in `onDestroyView` |
| FB6 | The root folder is requested with an **empty string**, producing `api/v1/files/` | Medium | use an explicit root id or a dedicated endpoint |
| FB8 | The envelope key is **`files`**, a third convention alongside `result`/`results` | Medium | document; align with NETWORK if the backend changes |
| FB5 | `Util.listview` is a global that is never persisted | Medium | move to DataStore (STORAGE) |
| FB9 | A new `FileAdapter` is created on every load | Medium | reuse + notify |
| FB3 | Deprecated `requireFragmentManager()` | Medium | `parentFragmentManager` |
| FB12 | No pull-to-refresh and no breadcrumb — the user cannot tell they are at the root | Medium | add a title/breadcrumb |
| FB4 | The toggle icon shows the current mode rather than the target mode | Low | confirm intent with the customer |
| FB1 | Commented-out `header_main` code left in `onCreateView` | Low | delete |

---

## 9. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/fragments/FilesFragment.kt` | **exclusive** |
| `app/src/main/res/layout/fragment_files.xml` | **exclusive** |

**Not owned (escalate):** `FileAdapter.java` (**MEDIA / FILE_LIST** — it owns row rendering and
navigation), `FileListActivity.kt`, `PdfActivity2.java`, `Util.java` (`listview`), all PLATFORM
files, `TabAdapter.kt` (APPSHELL).

---

## 10. How to make common changes

**Fix the toggle (FB2) — highest value here:** replace the `detach().attach()` with
`recyclerView.layoutManager = if (Util.listview) LinearLayoutManager(ctx) else GridLayoutManager(ctx, 3)`
followed by `adapter?.notifyDataSetChanged()`. No re-fetch needed. Local to this agent.

**Persist the toggle (FB5):** needs a new DataStore key — **PLATFORM/STORAGE task via PM** — then
read it here instead of `Util.listview`.

**Add an error state (FB10):** add an `else` to the `code() == 200` check showing `no_data` plus a
message.

**Change the grid column count:** the literal `3` in `GridLayoutManager(context, 3)` is local to this
file (and duplicated in `FILE_LIST.md` — keep them in sync).

**"Files open in the wrong viewer" / "folders don't open":** **not this agent.** That routing lives
in `FileAdapter` — route to `FILE_LIST.md` (issue M-2).

---

## 11. Change log

| Change | Detail |
|---|---|
| Created | Initial FILES_BROWSER module agent documented from `FilesFragment.kt` (168 lines) and `fragment_files.xml` (4 ids): the `Util.listview` list/grid toggle and its full-recreate implementation, the root fetch with an **empty folder id**, the **`files` envelope key** (a third convention alongside `result`/`results`), the missing error branch, and 12 known issues. |


