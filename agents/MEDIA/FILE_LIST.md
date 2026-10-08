# FILE_LIST Module Agent

> Team: **MEDIA** · Reports to: `agents/MEDIA/MEDIA_LEAD.md`
> This agent knows **every detail** of the folder-contents screen **and** of `FileAdapter` — the
> component that renders every file row and decides where a tap goes.
> It may only edit the files listed in §9 **Owned files**.

---

## ⚠ Shared-component warning

`FileAdapter.java` is owned here but is **also used by `FilesFragment`** (MEDIA/FILES_BROWSER).
Any change to row rendering or tap routing affects **both** screens — notify FILES_BROWSER via the
lead. It is also one of only **three Java files** in the app.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name | **Folder contents** |
| Classes | `com.veha.activity.FileListActivity` (157 lines) + `com.veha.adapter.FileAdapter` (Java) |
| Layouts | `activity_file_list.xml` (6 ids), `child_folders.xml` (3 ids) |
| Manifest | `<activity android:name=".FileListActivity" android:exported="false"/>` |
| View access | `findViewById` |
| Required extra | **`folderId`** |
| Entered from | `FileAdapter` itself — tapping a row whose `type == "folder"`, from either this screen or `FilesFragment` |
| Exits to | `FileListActivity` (**itself**, for nested folders) and `PdfActivity2` |
| Progress | `SpotsDialog`, "Please Wait" |

**This screen recurses into itself.** Each folder tap starts a *new* `FileListActivity` instance, so
the back stack grows one entry per folder level — which is also how "back" navigation works, since
there is no breadcrumb (issue FL-1).

---

## 2. UI inventory

### `activity_file_list.xml`

| View id | Type | Purpose |
|---|---|---|
| `header_main` | container | header bar |
| `prod_logo` | `ImageView` | tap -> `MainActivity` |
| `file_head_layout` | container | header strip (same as the fragment) |
| `view_icon` | `ImageView` | list/grid toggle |
| `recycler_view` | `RecyclerView` | contents |
| `no_data` | view | empty state |

The layout is `fragment_files.xml` **plus a header** — the two screens are near-identical by design.

### `child_folders.xml` — one row

| View id | Type | Bound to |
|---|---|---|
| `file_linear` | `LinearLayout` | row container |
| `icon_view` | `ImageView` | `folder_icon` **or** `ic_baseline_insert_drive_file_24` |
| `file_name_text_view` | `TextView` | `selectedFile.getName()` |

**No size, date, type label or thumbnail** is shown — only an icon and a name (issue FL-2), even
though `FilesAndFolders` carries `size`, `createdAt`, `createdBy` and `permission`.

---

## 3. `FileAdapter` — the routing decision

```java
if (selectedFile.getType().equals("folder")) {
    holder.imageView.setImageResource(R.drawable.folder_icon);
} else {
    holder.imageView.setImageResource(R.drawable.ic_baseline_insert_drive_file_24);
}

holder.itemView.setOnClickListener(v -> {
    Log.e("type", selectedFile.getType());
    if (selectedFile.getType().equals("folder")) {
        Intent i = new Intent(context, FileListActivity.class);
        i.putExtra("folderId", selectedFile.getId());
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    } else {
        Intent i = new Intent(context, PdfActivity2.class);
        i.putExtra("url", selectedFile.getUrl());
        i.putExtra("fileName", selectedFile.getName());
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
});
```

| Fact | Detail |
|---|---|
| **Binary routing** | `"folder"` -> folder screen; **everything else -> the PDF viewer** (team issue M-2) |
| Consequence | an image, audio, video or `.docx` in the library opens in **`PdfActivity2`** and fails there with no message (issue FL-3) |
| `FLAG_ACTIVITY_NEW_TASK` | set on both intents — needed because the adapter may hold an **application** context (`FileListActivity` passes `applicationContext`), but it distorts the back stack (issue FL-4) |
| `getType()` null | `.equals("folder")` on a null type throws **NPE** (issue FL-5) |
| Debug log | `Log.e("type", ...)` left in the row click handler (issue FL-6) |
| Contract out | `PdfActivity2` expects exactly **`url`** and **`fileName`** |

---

## 4. API contract

Identical to FILES_BROWSER's, with a real folder id:

| Item | Value |
|---|---|
| Retrofit | `getFilesAndFolders("Bearer $token", folderID)` |
| HTTP | `GET api/v1/files/{folderId}` |
| Argument | `intent.getStringExtra("folderId").toString()` — the literal `"null"` if missing (issue FL-7) |
| Envelope | **`files`** (not `results`) -> `FilesAndFolders` |
| Guard / token | `isNetworkAvailable` + the **always-true `||` guard** (issue FL-8) |
| Non-200 | **no `else` branch** — nothing happens (issue FL-9) |

`getFilesAndFolder` here is a **near-exact copy** of the one in `FilesFragment` (team issue M-6);
the only differences are the folder id and that this one passes **`applicationContext`** to the
adapter (issue FL-10).

---

## 5. Storage & global state

| Item | Operation |
|---|---|
| DataStore `token` | read (observed) |
| `Util.listview` | read -> `LinearLayoutManager` vs `GridLayoutManager(context, 3)` |

---

## 6. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | layout -> `UserPreferences` -> SpotsDialog -> bind views -> read `folderId` -> `getFilesAndFolder(path, this)` |
| `onPause` / `onResume` / `onDestroy` | `dialog.dismiss()` |

No rotation handling; the folder is re-fetched. The Retrofit call is never cancelled (issue FL-11).

---

## 7. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| `RetrofitAPI.getFilesAndFolders` | PLATFORM / NETWORK | the fetch |
| `FilesAndFolders` | PLATFORM / DATA_MODELS | parsing |
| `UserPreferences` | PLATFORM / STORAGE | token |
| `Util.listview`, `Commons()` | PLATFORM / COMMONS | layout mode + guard |
| `folder_icon`, `ic_baseline_insert_drive_file_24` | APPSHELL / THEMING | row icons |
| `FilesFragment` | MEDIA / FILES_BROWSER | **co-user of `FileAdapter`** |
| `PdfActivity2` | MEDIA / PDF_VIEWER | file destination |
| `MainActivity` | APPSHELL / MAIN_NAV | logo |

---

## 8. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| FL-3 | **Every non-folder opens in the PDF viewer** — images, audio, video and documents all route to `PdfActivity2` | **High** | branch on the real file type/extension |
| FL-5 | `getType().equals("folder")` NPEs on a null type | **High** | `"folder".equals(getType())` |
| FL-9 | A non-200 has **no `else` branch** — no error, no empty state | **High** | add an error state |
| FL-8 | The always-true `||` token guard | **High** | use `&&` |
| FL-11 | The Retrofit call is never cancelled | **High** | cancel in `onDestroy` |
| FL-4 | `FLAG_ACTIVITY_NEW_TASK` on both intents distorts the back stack | Medium | pass an Activity context and drop the flag |
| FL-10 | The adapter receives **`applicationContext`** here but a fragment context in FILES_BROWSER | Medium | pass a consistent context |
| FL-1 | Folder navigation recurses into new activities with **no breadcrumb or title** | Medium | show the current folder name |
| FL-7 | `getStringExtra("folderId").toString()` yields `"null"` when missing | Medium | validate the extra |
| FL-2 | Rows show only an icon and a name, ignoring `size`, `createdAt`, `createdBy`, `permission` | Medium | enrich the row |
| FL-12 | `permission` from the model is **never checked** before opening a file | Medium | enforce it, or confirm the server does |
| FL-6 | `Log.e("type", ...)` debug logging in the click handler | Low | remove |
| FL-13 | No search or sort in a file library | Low (product) | ask the customer |

---

## 9. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/FileListActivity.kt` | **exclusive** |
| `app/src/main/java/com/veha/adapter/FileAdapter.java` | **owned, but shared-use** — `FilesFragment` also instantiates it; notify FILES_BROWSER |
| `app/src/main/res/layout/activity_file_list.xml` | **exclusive** |
| `app/src/main/res/layout/child_folders.xml` | **owned, but shared-use** — rendered in both screens |
| `<activity android:name=".FileListActivity">` in the manifest | shared with PLATFORM/BUILD_CONFIG |

**Not owned (escalate):** `FilesFragment.kt` (FILES_BROWSER), `PdfActivity2.java` (PDF_VIEWER),
`Util.java`, all PLATFORM files.

---

## 10. How to make common changes

**Fix file-type routing (FL-3) — the highest-value change in MEDIA:** replace the binary `if` in
`FileAdapter` with a check on the file extension or a real `type` value, routing images to
`ImageDetailActivity` [FEED], audio/video appropriately, and only PDFs to `PdfActivity2`. This
touches the **shared** adapter — notify FILES_BROWSER, and ask PLATFORM/NETWORK what values `type`
can actually take.

**Show file metadata (FL-2):** add views to `child_folders.xml` and bind `size` / `createdAt` in
`onBindViewHolder`. Shared layout — coordinate.

**Add a folder title (FL-1):** add a `TextView` to `activity_file_list.xml` and set it from a new
`folderName` extra, which `FileAdapter` would also need to pass. Shared adapter — coordinate.

**De-duplicate the fetch (M-6):** the two copies of `getFilesAndFolder` could become one helper, but
they live in **two different agents' files** — propose it to PM rather than editing across the line.

---

## 11. Change log

| Change | Detail |
|---|---|
| Created | Initial FILE_LIST module agent documented from `FileListActivity.kt` (157 lines), `FileAdapter.java` and both layouts: the self-recursing folder navigation, the 3-id row, and the **binary type routing** that sends every non-folder to the PDF viewer. Recorded `FileAdapter` as shared with FILES_BROWSER, the `files` envelope, the `FLAG_ACTIVITY_NEW_TASK` / `applicationContext` coupling, and 13 known issues. |


