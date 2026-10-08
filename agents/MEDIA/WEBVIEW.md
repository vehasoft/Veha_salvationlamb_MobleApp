# WEBVIEW Module Agent

> Team: **MEDIA** · Reports to: `agents/MEDIA/MEDIA_LEAD.md`
> This agent knows **every detail** of the in-app web page used for Terms and Privacy.
> It may only edit the files listed in §8 **Owned files**.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name | **Terms and conditions** / **Privacy Policy** |
| Class | `com.veha.activity.WebViewActivity` (**33 lines** — the second-smallest activity) |
| Layout | `app/src/main/res/layout/activity_web_view.xml` (1 id: `webView`) |
| Manifest | `<activity android:name=".WebViewActivity" android:exported="false"/>` |
| View access | **Kotlin synthetics** (`activity_web_view.*`) |
| Required extra | **`WebPageName`** — `"terms"` or anything else |
| Entered from | `LoginActivity` (two links) and `RegisterActivity` (one link) — **all three in AUTH** |
| Exits to | nothing — system Back only |
| Network | **none via Retrofit** — the WebView loads the URL directly |

---

## 2. The whole screen

```kotlin
val page: String = intent.getStringExtra("WebPageName").toString()
val url: String = if (page.contentEquals("terms")) "https://salvationlamb.com/terms"
                  else                             "https://salvationlamb.com/privacy"

webView.webViewClient = WebViewClient()
webView.loadUrl(url)
webView.settings.javaScriptEnabled = true
webView.settings.domStorageEnabled = true
// webView.settings.setSupportZoom(true)   <- commented out
```

| Fact | Detail |
|---|---|
| Routing | **binary** — only `"terms"` is matched; **everything else falls through to privacy**, including a missing extra (which becomes the literal `"null"`) (issue WV1) |
| URLs | **two hard-coded `https://salvationlamb.com/...` links** — a *third* host family, alongside `Util.url` and `Util.getVideo` (issue WV2) |
| `WebViewClient` | a bare instance, so navigation stays in-app — but with **no `shouldOverrideUrlLoading` filtering**, any link on those pages also opens in-app (issue WV3) |
| **JavaScript** | **enabled** — the source comment literally says *"it can also allow xss vulnerabilities"* (issue WV4) |
| DOM storage | enabled (issue WV4) |
| Order | `loadUrl` is called **before** the settings are applied, so the first load may run without JS/DOM storage (issue WV5) |
| Zoom | `setSupportZoom` is commented out, so these legal pages **cannot be zoomed** (issue WV6) |

---

## 3. What is missing

| Missing | Consequence |
|---|---|
| Progress indicator | a blank white screen while the page loads (issue WV7) |
| Error handling | no `onReceivedError` — **an offline user sees the WebView's default error page**, not an app message (issue WV8) |
| Title / toolbar | the user cannot tell whether they are reading Terms or Privacy (issue WV9) |
| Back handling | `onBackPressed` is not overridden, so **Back exits the screen instead of navigating the WebView's history** (issue WV10) |
| `isNetworkAvailable` guard | the only screen in the app without one |
| Lifecycle | no `onDestroy` -> the WebView is never destroyed; a known leak source (issue WV11) |

---

## 4. Storage & global state

| Item | Operation |
|---|---|
| — | **none** |

No DataStore, no `Util`, no Retrofit, no auth. Along with `ImageDetailActivity` and
`PdfActivity2`, one of only three screens that touch no app state at all.

> It does, however, let the WebView create **its own** cookies and DOM storage, which persist in the
> app's data directory and are never cleared (issue WV12).

---

## 5. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | layout -> read `WebPageName` -> pick the URL -> set `WebViewClient` -> `loadUrl` -> enable JS and DOM storage |
| others | **none overridden** |

**No rotation handling:** the activity is recreated and the page **reloads from the network**,
losing scroll position (issue WV13).

---

## 6. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| Android `WebView` | platform | everything |
| `LoginActivity`, `RegisterActivity` | **AUTH** | the three entry points and the `WebPageName` contract |
| `usesCleartextTraffic` / `network_security_config.xml` | PLATFORM / BUILD_CONFIG | the pages are HTTPS, so not required today |

**No PLATFORM/NETWORK, DATA_MODELS, STORAGE or COMMONS dependency.**

---

## 7. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| WV4 | **JavaScript and DOM storage enabled** on a page that only renders static legal text — the code comment itself flags the XSS risk | **High** (security) | disable both unless the pages genuinely need them |
| WV8 | **No error handling** — offline or a 404 shows the WebView's raw error page | **High (UX)** | add `onReceivedError` + a guard |
| WV1 | Binary routing: anything that is not `"terms"` (including a missing extra) silently shows **privacy** | Medium | `when` with an explicit `else` and validation |
| WV10 | Back exits the screen instead of going back in the WebView's history | Medium | override `onBackPressed` with `webView.canGoBack()` |
| WV11 | The WebView is never destroyed in `onDestroy` | Medium | `webView.destroy()` |
| WV5 | `loadUrl` runs **before** the settings are applied | Medium | configure settings first |
| WV7 | No loading indicator — a blank screen while fetching | Medium | add a progress bar |
| WV9 | No title, so Terms and Privacy look identical | Medium | set a per-mode title |
| WV2 | Two hard-coded URLs on a **third** host family | Medium | move to config (PLATFORM) |
| WV3 | No `shouldOverrideUrlLoading` filtering — any outbound link opens in-app | Medium | restrict to the known host |
| WV13 | Rotation reloads the page and loses scroll position | Low | save state |
| WV12 | WebView cookies/DOM storage are never cleared | Low | clear on exit if required |
| WV6 | Zoom is commented out, hurting accessibility on dense legal text | Low | re-enable |

---

## 8. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/WebViewActivity.kt` | **exclusive** |
| `app/src/main/res/layout/activity_web_view.xml` | **exclusive** |
| `<activity android:name=".WebViewActivity">` in the manifest | shared with PLATFORM/BUILD_CONFIG |

**Not owned (escalate):** `LoginActivity.kt` / `RegisterActivity.kt` (**AUTH — they supply the
`WebPageName` extra**), the web pages themselves (server-side content, not in this repo).

---

## 9. How to make common changes

**Disable JavaScript (WV4):** set `javaScriptEnabled = false` and test both pages — if the legal
pages are plain HTML, nothing breaks and the XSS surface disappears. Local; report the behaviour
change to PM.

**Add a title (WV9):** add a `TextView`/toolbar to the layout and set it from the `WebPageName`
extra. Local.

**Add error handling (WV8):** subclass `WebViewClient` and override `onReceivedError` to show a
message. Consider adding `Commons().isNetworkAvailable` for consistency with the rest of the app
(note it toasts internally — COMMONS C-9).

**Add a third page** (e.g. "About us"): extend the routing to a `when`, add the URL, and ask the
**AUTH** agents (or whichever screen links to it) to pass the new `WebPageName` value. Two-team
change via PM.

**Change a URL (WV2):** both are hard-coded here. If they should follow the API host, that becomes a
PLATFORM/NETWORK config item — escalate.

---

## 10. Change log

| Change | Detail |
|---|---|
| Created | Initial WEBVIEW module agent documented from `WebViewActivity.kt` (33 lines) and `activity_web_view.xml` (1 id): the binary `WebPageName` routing that defaults to privacy, the two hard-coded URLs on a **third host family**, **JavaScript and DOM storage enabled with an XSS comment in the source**, the settings-after-`loadUrl` ordering, and 13 known issues including no error handling, no title and no Back navigation. |

