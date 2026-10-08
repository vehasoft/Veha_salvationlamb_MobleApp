# PDF_VIEWER Module Agent

> Team: **MEDIA** · Reports to: `agents/MEDIA/MEDIA_LEAD.md`
> This agent knows **every detail** of the PDF reader: its two `AsyncTask`s, the raw
> `HttpURLConnection` download, the `FLAG_SECURE` window, and the dead download path.
> It may only edit the files listed in §10 **Owned files**.

---

## ⚠ This screen is unlike every other in the app

| Aspect | PdfActivity2 | Everywhere else |
|---|---|---|
| Language | **Java** | Kotlin |
| Base class | **`android.app.Activity`** | `AppCompatActivity` |
| Networking | **raw `HttpURLConnection`** | Retrofit |
| Async | **`AsyncTask`** (deprecated) | Retrofit `enqueue` / coroutines |
| Progress | **`ProgressDialog`** | `SpotsDialog` |
| Window | **`FLAG_SECURE`** (blocks screenshots/recording) | none |
| Auth | **none** — the PDF URL is fetched unauthenticated | `Bearer <token>` |

Treat its patterns as **local to this screen**; do not copy them, and do not "modernise" them
casually — `FLAG_SECURE` in particular looks deliberate.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name | **PDF viewer** |
| Class | `com.veha.activity.PdfActivity2` (Java, extends `Activity`) |
| Layout | `app/src/main/res/layout/activity_pdf2.xml` |
| Manifest | `<activity android:name=".PdfActivity2" android:exported="false"/>` |
| Required extras | **`url`** and **`fileName`** |
| Entered from | `FileAdapter` — **any row whose type is not `"folder"`** (MEDIA M-2) |
| Exits to | nothing — system Back only |
| Library | `com.github.barteksc:android-pdf-viewer:3.2.0-beta.1` (a **beta**) |

> The class name ends in **`2`**, implying an earlier `PdfActivity` that no longer exists
> (issue PV1).

---

## 2. UI inventory (`activity_pdf2.xml`)

| View id | Type | Purpose |
|---|---|---|
| `pdfView1` | `PDFView` | the rendered document |
| `pdf_header` | `TextView` | shows `fileName`; has an **empty click listener** (issue PV2) |

No toolbar, no close button, no page indicator beyond the library's scroll handle, and no error
view.

---

## 3. Load flow — the `LOADURL` AsyncTask

```java
url        = intent.getStringExtra("url");
pdfFileName = intent.getStringExtra("fileName");
tv_header.setText(pdfFileName);
new LOADURL(PdfActivity2.this).execute(url);
```

| Stage | Behaviour |
|---|---|
| `onPreExecute` | a `ProgressDialog` titled **"Please wait"** / **"Fetching PDF from server..."**, non-cancelable |
| `doInBackground` | opens an `HttpURLConnection`; on **200** wraps the stream in a `BufferedInputStream`; on `IOException` logs and returns `null` |
| `onPostExecute` | `Log.e("inputstream", ...)` then renders via `pdfView.fromStream(...)`, dismisses the dialog |

### Render configuration

`.defaultPage(pageNumber)`, `.onPageChange(this)`, `.enableAnnotationRendering(true)`, `.onLoad(this)`,
`.scrollHandle(new DefaultScrollHandle(this))`, `.spacing(10)`,
**`.enableAnnotationRendering(false)`** *(called a second time, overriding the first — issue PV3)*,
`.enableAntialiasing(true)`, `.load()`.

### The null-stream crash

`doInBackground` returns `null` on any failure — **and also when the response is not 200**, since
`inputStream` is only assigned inside that branch. Then:

- `Log.e("inputstream", inputStream.toString())` at the **end of `doInBackground`** dereferences it
  -> **NPE on a non-200 response** (issue PV4);
- `onPostExecute` dereferences it again, with **no null check** (issue PV5).

So a 404, a 500 or an expired link **crashes the activity** instead of showing an error. Given that
`FileAdapter` sends every non-PDF here (M-2), this is reachable in normal use.

### The streaming/memory model

The whole PDF is pulled into memory through a single `InputStream` with no size limit, on a device
that already sets `largeHeap="true"` (issue PV6).

---

## 4. The dead `DownloadFile` AsyncTask

A second inner class exists and is **never instantiated** (issue PV7):

```java
String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
File folder = new File(extStorageDirectory, "testthreepdf");   // hard-coded, test-looking name
folder.mkdir();
File pdfFile = new File(folder, fileName);
pdfFile.createNewFile();
downloadFile(fileUrl, pdfFile);
```

| Problem | Detail |
|---|---|
| Dead code | nothing calls `new DownloadFile().execute(...)` |
| Hard-coded folder | **`"testthreepdf"`** on external storage — a test artefact (issue PV8) |
| Deprecated API | `Environment.getExternalStorageDirectory()` is deprecated and blocked by scoped storage on API 29+ (issue PV9) |
| No permission check | would fail silently on modern Android (issue PV10) |
| Buffer | reads in 1 MB chunks (`MEGABYTE`), `printStackTrace()` on every failure (issue PV11) |

**Recommendation for the PM:** either delete it or finish it as a real "download" feature — it is
currently ~45 lines of misleading dead code.

---

## 5. Callbacks

| Callback | Behaviour |
|---|---|
| `onPageChanged(page, pageCount)` | stores `pageNumber`; `setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount))` — but with **no toolbar/ActionBar** the title is effectively invisible (issue PV12) |
| `loadComplete(nbPages)` | from `OnLoadCompleteListener` |

An unused `SAMPLE_FILE = "sample.pdf"` field remains, along with the original sample URL in a
comment (issue PV13).

---

## 6. Storage & global state

| Item | Operation |
|---|---|
| — | **none** |

No DataStore, no `Util` globals, no Retrofit. The only persistence is the dead downloader's
external-storage write.

---

## 7. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | **`FLAG_SECURE`** -> layout -> bind `pdfView1` + `pdf_header` -> empty header listener -> read `url`/`fileName` -> `LOADURL.execute(url)` |
| others | **none overridden** |

- No `onDestroy`, so the `AsyncTask` is **never cancelled** — it holds a strong reference to the
  Activity and to a `ProgressDialog`, a textbook leak on rotation (issue PV14).
- **No rotation handling:** the activity is recreated and the entire PDF re-downloaded; `pageNumber`
  resets to 0 (issue PV15).

---

## 8. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| `android-pdf-viewer:3.2.0-beta.1` | PLATFORM / BUILD_CONFIG | rendering |
| `FileAdapter` | MEDIA / FILE_LIST | the only caller; supplies `url` + `fileName` |
| `INTERNET` permission | PLATFORM / BUILD_CONFIG | the raw download |
| storage permissions | PLATFORM / BUILD_CONFIG | only for the dead downloader |

**No PLATFORM/NETWORK, DATA_MODELS, STORAGE or COMMONS dependency** — the only screen in the app
that touches none of them.

---

## 9. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| PV4 | `Log.e("inputstream", inputStream.toString())` runs on a **null** stream after a non-200 — **NPE** | **Critical** | null-check before logging |
| PV5 | `onPostExecute` dereferences the stream with no null check — second NPE path | **Critical** | handle null with an error view |
| PV14 | The `AsyncTask` is never cancelled and holds the Activity + dialog — leaks on rotation | **High** | cancel in `onDestroy`, or move off `AsyncTask` |
| PV15 | Rotation re-downloads the whole PDF and resets the page | **High** | save state / cache the file |
| PV6 | The entire PDF is streamed into memory with no size limit | **High** | download to cache and use `fromFile` |
| PV7 | `DownloadFile` is **dead code** (~45 lines) | Medium | delete, or finish the feature |
| PV8 | The dead path writes to a hard-coded **`"testthreepdf"`** folder | Medium | remove with PV7 |
| PV9 | `Environment.getExternalStorageDirectory()` is deprecated / scoped-storage blocked | Medium | app-specific storage |
| PV3 | `enableAnnotationRendering` is called **twice**, `true` then `false` | Medium | pick one |
| PV12 | `setTitle` writes to a title bar that is not displayed | Medium | add a page indicator to `pdf_header` |
| PV16 | No auth header on the download, unlike every Retrofit call — the file URL must be public | Medium | confirm with PLATFORM/NETWORK |
| PV17 | Extends `Activity`, not `AppCompatActivity`, so it ignores the app theme and night mode | Medium | migrate, keeping `FLAG_SECURE` |
| PV10 | No runtime permission check in the dead downloader | Low | remove with PV7 |
| PV11 | `printStackTrace()` instead of `Log` | Low | use `Log.e` |
| PV2 | `pdf_header` has an **empty** click listener | Low | remove or implement |
| PV13 | Unused `SAMPLE_FILE` field and a leftover sample URL comment | Low | delete |
| PV1 | Class named `PdfActivity2` with no `PdfActivity` | Cosmetic | rename (manifest change) |

---

## 10. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/PdfActivity2.java` | **exclusive** |
| `app/src/main/res/layout/activity_pdf2.xml` | **exclusive** |
| `<activity android:name=".PdfActivity2">` in the manifest | shared with PLATFORM/BUILD_CONFIG |

**Not owned (escalate):** `FileAdapter.java` (FILE_LIST — it decides what arrives here), the
pdf-viewer dependency (BUILD_CONFIG), storage permissions (BUILD_CONFIG).

---

## 11. How to make common changes

**Stop the crash on a failed download (PV4/PV5) — highest value here:** remove the `Log.e` that
dereferences the stream, and null-check in `onPostExecute`, showing a message instead of rendering.
Local to this agent, and important because `FileAdapter` routes non-PDFs here.

**Delete the dead downloader (PV7–PV11):** removes ~45 lines plus the external-storage dependency.
Confirm with PM that no "download" feature is planned.

**Fix the leak (PV14):** keep a reference to the `LOADURL` task and `cancel(true)` it in a new
`onDestroy`.

**Render from a file instead of a stream (PV6):** download to `getCacheDir()` and use
`pdfView.fromFile(...)` — this also enables rotation without re-downloading.

**Support non-PDF files:** that decision is made **before** this screen, in `FileAdapter` — route to
`FILE_LIST.md` (issue M-2).

**Keep `FLAG_SECURE`** unless the customer explicitly asks otherwise; it appears to be a deliberate
content-protection choice for the library.

---

## 12. Change log

| Change | Detail |
|---|---|
| Created | Initial PDF_VIEWER module agent documented from `PdfActivity2.java`: the seven ways it differs from every other screen (Java, `Activity`, `HttpURLConnection`, `AsyncTask`, `ProgressDialog`, `FLAG_SECURE`, no auth), the `LOADURL` flow with its **two null-stream crash paths**, the full render configuration including the double `enableAnnotationRendering`, the **dead `DownloadFile` task** writing to a hard-coded `"testthreepdf"` folder, and 17 known issues. |


