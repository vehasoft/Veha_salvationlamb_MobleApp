# MEDIA TEAM LEAD Agent

> Reports to: **PROJECT MANAGER** (`AGENTS.md`)
> Manages the 6 module agents below. **Status: all READY.**

---

## 1. Charter

MEDIA owns **content that is not a user-authored post**: the admin file library (folders and PDFs),
the curated admin **audio** and **video** feeds, and the in-app web pages (terms & privacy).

**Boundary:** MEDIA owns the *screens*. The **post card** that renders audio and video inside those
feeds is FEED's `HomeAdapter` — MEDIA is a consumer, exactly like PROFILE. The static
`Util.player` `MediaPlayer` is PLATFORM-owned global state that both teams touch.

---

## 2. Modules owned

| Module agent | Screen / unit | Source files | Status |
|---|---|---|---|
| `FILES_BROWSER.md` | Files tab (root) | `fragments/FilesFragment.kt` (168), `res/layout/fragment_files.xml` | READY |
| `FILE_LIST.md` | Folder contents | `activity/FileListActivity.kt` (157), `adapter/FileAdapter.java`, `res/layout/activity_file_list.xml`, `child_folders.xml` | READY |
| `PDF_VIEWER.md` | PDF reader | `activity/PdfActivity2.java` (Java, `Activity`) | READY |
| `ADMIN_AUDIO.md` | Audio feed tab | `fragments/AdminAudioFragment.kt` (396) | READY |
| `ADMIN_VIDEO.md` | Video feed tab | `fragments/AdminVideoFragment.kt` (392) | READY |
| `WEBVIEW.md` | Terms / Privacy | `activity/WebViewActivity.kt` (33), `res/layout/activity_web_view.xml` | READY |

> **`FileAdapter.java` is one of only three Java files** in the app (with `PdfActivity2.java` and
> `ExpandableView.java`). It belongs to `FILE_LIST.md`, but is **also used by `FilesFragment`**.

---

## 3. Where MEDIA sits in the app

```
MainActivity bottom nav [APPSHELL] -> TabAdapter
   tab 0 HomeFragment("user")   [FEED]
   tab 1 FilesFragment()        [MEDIA]
   tab 2 AdminVideoFragment()   [MEDIA]
   tab 3 AdminAudioFragment()   [MEDIA]
   tab 4 ProfileFragment(me)    [PROFILE]

FilesFragment  -- GET files/{folderId} with the root folder id
   `-> FileAdapter row tap
          |-- type == "folder" -> FileListActivity (extra folderId)  [recurses via the same adapter]
          `-- otherwise        -> PdfActivity2 (extras url, fileName)

LoginActivity / RegisterActivity [AUTH] -> WebViewActivity (extra WebPageName = terms | privacy)
```

**MEDIA owns 3 of the 5 bottom-nav tabs** — more of the app's primary surface than any other team.

---

## 4. The audio/video twins

`AdminAudioFragment` (396) and `AdminVideoFragment` (392) are **near-identical copies**. Verified
differences:

| | Audio | Video |
|---|---|---|
| Endpoint | `GET api/v1/post/admin/audio` | `GET api/v1/post/admin/video` |
| Retrofit | `getAudioPost(head, page, size)` | `getVideoPost(head, page, size)` |
| Log tags | `AdminAudioFragment.*` | `AdminVideoFragment.*` |
| Companion | has `getInstance()` | **no companion object** |

Everything else — the 5 methods, the paging, the three state maps, the `HomeAdapter("home")`
construction, the lifecycle — is duplicated line for line (team issue M-1).

> Both pass **`"home"`** as the adapter page, so audio/video cards show the **follow and favourite
> buttons** exactly like the main feed. That is a FEED contract (FEED_LEAD §3) they inherit.

---

## 5. API surface

| Endpoint | Retrofit method | Used by |
|---|---|---|
| `GET api/v1/files/{folderId}` | `getFilesAndFolders` | FILES_BROWSER (root), FILE_LIST (subfolders) |
| `GET api/v1/post/admin/audio?page&size` | `getAudioPost` | ADMIN_AUDIO |
| `GET api/v1/post/admin/video?page&size` | `getVideoPost` | ADMIN_VIDEO |
| `GET api/v1/like/user/{userId}` | `getUserLikes` | both admin feeds |
| `GET api/v1/follows/{userId}` | `getFollowing` | both admin feeds |
| `GET api/v1/favorites/{userId}` | `getFav` | both admin feeds |
| `GET api/v1/users/{userId}` | `getUser` | both admin feeds (**7th and 8th** copies of `getMyDetails`) |

**PDF_VIEWER and WEBVIEW use no Retrofit at all** — the PDF is fetched with a raw
`HttpURLConnection` inside an `AsyncTask`, and the WebView loads a hard-coded URL.

List responses use **`results`** + `count` (FEED_LEAD §5).

---

## 6. Shared state

| State | Who | Note |
|---|---|---|
| **`Util.player`** | both admin feeds stop/reset/release/null it in `onPause` and `onDestroy` | the static `MediaPlayer` shared with FEED (COMMONS C-2) |
| `Util.user` | written by both admin feeds | copies 7 and 8 of `getMyDetails` |
| `Util.userId`, `Util.isWarrior`, `Util.fontSize` | read | |
| DataStore `token` | read per call (fresh observer each time) | |
| **No storage of its own** | — | downloaded PDFs go to external storage, not DataStore |

---

## 7. Cross-team dependencies

| Needs | Owner | Escalate when |
|---|---|---|
| `HomeAdapter` + `child_post.xml` (renders every audio/video card) | **FEED / HOME_FEED** | always |
| `Util.player` lifecycle | PLATFORM / COMMONS + FEED | always |
| `Util.getVideo(url)` — the **second hard-coded host** | PLATFORM / COMMONS | always |
| any endpoint change | PLATFORM / NETWORK | always |
| `FilesAndFolders`, `Posts` fields | PLATFORM / DATA_MODELS | always |
| `android-pdf-viewer`, `androidyoutubeplayer`, `cronet` | PLATFORM / BUILD_CONFIG | always |
| storage permissions for PDF download | PLATFORM / BUILD_CONFIG | always |
| tab hosting + `TabAdapter` positions | APPSHELL / MAIN_NAV | always |
| Terms/Privacy entry points | AUTH (LOGIN, REGISTER) | when `WebPageName` changes |

---

## 8. Definition of done (team-level)

- [ ] A change to one admin feed is **mirrored in the other** (they are twins) or deliberately noted.
- [ ] `Util.player` is still released in `onPause`/`onDestroy` if audio code was touched.
- [ ] `results` + `count` envelope respected.
- [ ] File-type routing (`"folder"` vs everything else) still correct.
- [ ] Module `.md` updated in the same change, including its Change log.

---

## 9. Team-level known issues

| # | Issue | Module | Risk |
|---|---|---|---|
| M-1 | `AdminAudioFragment` and `AdminVideoFragment` are **~390-line near-duplicates** differing only by endpoint and log tags | ADMIN_AUDIO, ADMIN_VIDEO | every fix must be made twice |
| M-2 | `FileAdapter` treats **anything that is not `"folder"` as a PDF** — images, audio and video all open in the PDF viewer | FILE_LIST, FILES_BROWSER | broken files UX |
| M-3 | `PdfActivity2` downloads over a raw `HttpURLConnection` inside a deprecated `AsyncTask`, writing to a hard-coded `"testthreepdf"` folder on external storage | PDF_VIEWER | leaks, permission failures, litter |
| M-4 | `WebViewActivity` enables **JavaScript and DOM storage** (the code comment even notes the XSS risk) | WEBVIEW | security |
| M-5 | `Util.player` is a static `MediaPlayer` shared by FEED and both admin feeds | ADMIN_AUDIO, ADMIN_VIDEO | overlapping audio, leaks |
| M-6 | `FilesFragment` and `FileListActivity` duplicate `getFilesAndFolder` almost exactly | FILES_BROWSER, FILE_LIST | drift |
| M-7 | The always-true `||` token guard appears in every MEDIA network method | all | dead logout branches |
| M-8 | `getMyDetails` copies 7 and 8 live here | ADMIN_AUDIO, ADMIN_VIDEO | drift (AUTH A4) |
| M-9 | `PdfActivity2` extends **`Activity`**, not `AppCompatActivity`, and is the only screen setting `FLAG_SECURE` | PDF_VIEWER | inconsistent theming/behaviour |
| M-10 | Admin feeds pass page `"home"`, so curated content shows follow/fav buttons for admin authors | ADMIN_AUDIO, ADMIN_VIDEO | questionable UX — confirm with customer |

---

## 10. Change log

| Change | Detail |
|---|---|
| Created | Initial MEDIA team lead agent: charter, 6 modules, the tab map (MEDIA owns 3 of 5 bottom-nav tabs), the audio/video twin analysis, a 7-endpoint API surface, `Util.player` sharing with FEED, and 10 team-level issues. |
| MEDIA team complete | All 6 module agents written and verified. Key findings escalated to PM: **`PdfActivity2` crashes with an NPE on any non-200 download** (two paths) and carries ~45 lines of dead downloader writing to a hard-coded `"testthreepdf"` folder; `FileAdapter` sends **every non-folder to the PDF viewer**; `WebViewActivity` enables **JavaScript with an XSS comment in the source**; the files API uses a **third envelope key `files`**; and `Util.getVideo` is a **third hard-coded host**. **All 6 MEDIA module agents are READY.** |

