# BIBLE_READER Module Agent

> Team: **BIBLE** · Reports to: `agents/BIBLE/BIBLE_LEAD.md`
> Scope: this agent knows **every detail** of this screen — views, actions, API, storage,
> validation, messages, navigation. It may only edit the files under **Owned files**.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name (user-facing) | **Bible reader** — testament / book / chapter selectors over a verse list with multi-select |
| Class | `com.veha.activity.BibleActivity` (448 LOC, Kotlin, `AppCompatActivity`) + the **inner class `MyAdapter`** (lines 382–421) and the private `toggleSelection` helper |
| Layout | `res/layout/activity_bible.xml` (366) · row `res/layout/child_bible.xml` (27) · chapter picker `res/layout/grid_view.xml` (13) + `res/layout/grid_spinner_item.xml` (12) |
| Manifest entry | `<activity android:name=".BibleActivity" android:configChanges="orientation\|keyboardHidden\|screenSize" android:screenOrientation="sensorPortrait" android:exported="false" android:theme="@style/BibleMaterialTheme" />` |
| Entered from | `BibleFragment` (both cards, extra `type`) · `BiblePostActivity.onBackPressed` (extra `type`) — **the only two launch sites** |
| Exits to | `MainActivity` (logo tap, home tap) · `BiblePostActivity` (post) · the system chooser (share) |

**Data source:** 100 % offline. Everything comes from the static `Util.bible`
(`org.json.JSONObject`) that `SplashScreenActivity.getBible()` parsed out of
`filesDir/bible.json`. **This screen makes no network call at all.**

## 2. UI inventory

| View id | Type | Text / hint | Notes |
|---|---|---|---|
| `@id/header_main` | `ConstraintLayout`, `@color/primary_blue` | — | header bar; not bound in code |
| `@id/prod_logo` | `ImageView` 200×50dp, `@drawable/ic_sl_logo_01_svg` | — | → `logo`; tap goes to `MainActivity` **without `finish()`** (`BR7`) |
| `@id/bible_shimmer_layout` | `ShimmerFrameLayout` | — | `visibility="gone"`; bound to `shimmerFrameLayout` and **never used** (`BR13`). 9 disabled `TextInputLayout` placeholder rows copied from `fragment_files.xml` |
| `@id/bible_linear` | `LinearLayout` (vertical) | — | bound to `bibleLinear` and **never used** (`BR13`) |
| `@id/bible_to_home` | `ImageView` 30dp, `@drawable/ic_baseline_home_24` | — | → `homeBtn`; same no-`finish()` navigation (`BR7`) |
| `@id/bible` | `Spinner` (`spinnerMode="dropdown"`, `weight=.6`) | — | → `bibleDropdown`; the **edition** selector, 2 entries built in `setBibleEdition()` |
| `@id/bible_search` | `ImageView`, `@drawable/search` | — | `visibility="gone"`, **never bound** — dead search affordance (`BR13`) |
| `@id/bible_bookmark` | `ImageView` 30dp | `ic_baseline_bookmark_border_24` / `..._bookmark_24` | → `bookmarkBtn`; toggles the bookmark; icon swapped via the deprecated `getDrawable(int)` |
| `@id/prev_btn` | `ImageView` 30dp, `baseline_keyboard_arrow_left_24` | — | → `previous`; chapter − 1 |
| `@id/heading_input_layout` | `TextInputLayout` (OutlinedBox, `endIconMode="dropdown_menu"`, `weight=8`) | — | wrapper, not bound |
| `@id/heading` | `AutoCompleteTextView` (`inputType="none"`) | hint `""` | → `contentDropdown`; the **book** selector, `ArrayAdapter(simple_dropdown_item_1line, keyList)` |
| `@id/fakeSpinner` | `AutoCompleteTextView` (`inputType="none"`, `weight=2`, centred) | hint `""` | → `fakeSpinner`; the **chapter** field. `setAdapter(null)`, `inputType = 0`, `keyListener = null`, transparent dropdown background — a *fake* spinner opened by `showGridDropdown()` |
| `@id/next_btn` | `ImageView` 30dp, `baseline_keyboard_arrow_right_24` | — | → `next`; chapter + 1 |
| `@id/button_container` | `ConstraintLayout`, `@color/home_bg` | — | → `buttonContainer`; the multi-select action bar. XML says `visibility="visible"`; code hides it in `setVersesList` on every chapter load |
| `@id/copy_txt` | `ImageView` 30dp, `baseline_content_copy_24` | — | → `copy`; copy selection to clipboard (**no confirmation**, `BR12`) |
| `@id/share_txt` | `ImageView` 30dp, `baseline_share_24` | — | → `share`; `ACTION_SEND` chooser |
| `@id/post_txt` | `Button` | **`"post"`** (hard-coded, lowercase) | → `post`; opens the composer (**no permission gate**, `BR11`) |
| `@id/recycler_view` | `RecyclerView` (`layout_height="wrap_content"`, `visibility="gone"`) | — | → `recyclerView`; made `VISIBLE` + `LinearLayoutManager(this)` in `onCreate`; adapter = `MyAdapter` |
| `child_bible.xml` → `@id/child_post_layout` | `LinearLayout` (horizontal, `@color/white`) | — | row root — **id duplicates FEED's post row** (`BR9` = `B-10`) |
| `child_bible.xml` → `@id/selected` | `CheckBox` | — | `MyViewHolder.selectedCheckBox`; `VISIBLE` only while `isMultiSelect` |
| `child_bible.xml` → `@id/bible_content` | `TextView`, 20**dp**, `@color/black` | `"<n>. <verse>"` | `MyViewHolder.content`; ignores `Util.fontSize` (`BR14`) |
| `grid_view.xml` → `@id/gridView` | `GridView`, `numColumns="5"`, white bg | — | chapter picker inside a `PopupWindow` |
| `grid_spinner_item.xml` → `@id/textItem` | `TextView` 55×55dp, `rounded_border_login_register` | chapter number | grid cell |

### `lateinit` field inventory — **17 declarations: 15 views + 2 helpers**

`logo`, `recyclerView`, `bibleLinear`*, `copy`, `share`, `next`, `previous`, `homeBtn`,
`bookmarkBtn`, `post`, `buttonContainer`, `bibleDropdown`, `contentDropdown`, `fakeSpinner`,
`shimmerFrameLayout`* (\* = bound but never used) + `adapterr: MyAdapter` and
`userPreferences: UserPreferences`. A commented-out 16th view, `chapterDropdown: Spinner`
(line 56, layout id `@id/chapter`), plus its dead `next` / `previous` bodies (lines 131–134,
143–146) and the dead `findViewById` (line 93), are still in the file — the `fakeSpinner` +
`PopupWindow` grid replaced it (`BR13`). All 15 views are bound in `onCreate` **before** any use,
so the usual "lateinit accessed before binding" P0 does **not** apply here.

### Non-view state

| Field | Type | Purpose |
|---|---|---|
| `companion object var selectedText` | `ArrayList<String>` | **static, process-wide** list of selected verse strings (`"<n>. <text>"`), mutated from the inner adapter (`BR4` = `B-6`) |
| `bibleMap` | `val HashMap<String, JsonArray>` | book name → its `C` (chapters) array; **never cleared**, accumulates both testaments (`BR15`) |
| `chapterMap` | `val HashMap<String, JsonArray>` | chapter number (`"1"`, `"2"`, …) → its `V` (verses) array; **never cleared** between books (`BR15`) |
| `keyList` | `ArrayList<String>` | book names of the current testament, rebuilt by `setHeading` |
| `chapterList` | `ArrayList<String>` | `"1"`…`"N"` for the current book, rebuilt by `loadChapterList` |
| `bibleList` | `ArrayList<String>` | the 2 edition labels (`R.string.oldBible`, `R.string.newBible`) |
| `type` | `String` = `"old"` | the launch extra; **never updated when the spinner changes** (`BR10` = `B-5`) |
| `bookmarkedEdition` / `bookmarkedContent` / `bookmarkedChapter` | `String` = `"DUMMY"` | parsed from `Util.bookmarkedBible`; `"DUMMY"` is the "no bookmark" sentinel |
| `onLoad` | `Boolean` = `true` | set `false` the first time a bookmarked chapter is applied — guards the **chapter** only, not the book/edition (`BR16`) |
| `isMultiSelect` | `Boolean` = `false` | selection mode; drives `buttonContainer` + the row checkboxes |

### Static text (no id)

| Text | Notes |
|---|---|
| `"post"` on `@id/post_txt` | hard-coded lowercase, not a string resource (`B-20`) |
| `"Please select atleast one"` | hard-coded toast, typo (`B-20`) |
| `"choose one"` | hard-coded share-chooser title |
| `"Salvation Lamb"` (subject), `"Let me recommend you this application"`, `https://salvationlamb.com/redirect` | hard-coded English share body, appended regardless of app locale |
| `"bible"` | the `ClipData` label |

## 3. Actions / event handlers

| Trigger | Behaviour |
|---|---|
| `onCreate` | `setContentView(activity_bible)` → `UserPreferences(this)` → `onLoad = true` → binds 15 views → configures `fakeSpinner` as a fake dropdown → wires listeners → `recyclerView.visibility = VISIBLE` + `LinearLayoutManager` → reads `intent.extras!!.getString("type")` (**force-unwrap**, `BR3` = `B-15`) → parses `Util.bookmarkedBible` → `setBibleEdition()` → wires the bookmark toggle |
| Tap `@id/prod_logo` / `@id/bible_to_home` | `startActivity(Intent(this, MainActivity::class.java))` — **no `finish()`, no `CLEAR_TOP`** → duplicate `MainActivity` instances stack up (`BR7` = `B-17`) |
| Tap `@id/fakeSpinner` **or** it gains focus | `showGridDropdown(chapterList)` — **both** listeners fire on the first tap, so the `PopupWindow` can be built twice (`BR17`) |
| Select edition in `@id/bible` | `onItemSelected` → `pos == 0` ? `setHeading(Gson().fromJson(Util.bible.get("Old").toString(), JsonArray::class.java))` : the same with `"New"`. **`Util.bible` is dereferenced unguarded** (`BR1` = `B-7`) and a whole testament is re-serialized + re-parsed on the main thread (`BR8` = `B-18`) |
| Select book in `@id/heading` | `setOnItemClickListener` → `loadChapterList(bibleMap[keyList[pos]]!!)` (`!!` on a map lookup, `BR18`) |
| Pick a chapter in the grid | `fakeSpinner.setText(chapterList[position], false)`; `popupWindow.dismiss()`; `setVersesList(position)` |
| Tap `@id/next_btn` | `if (fakeSpinner.text.toString().toInt() < chapterList.size) { selectedText = ArrayList(); count = …toInt(); fakeSpinner.setText((count+1).toString(), false); setVersesList(count) }` — **`toInt()` on free text** (`BR2` = `B-14`) |
| Tap `@id/prev_btn` | `if (…toInt() > 1) { selectedText = ArrayList(); count = …toInt() - 2; fakeSpinner.setText((count+1).toString(), false); setVersesList(count) }` — same `toInt()` risk |
| **Long-press a verse row** | if `!isMultiSelect`: `isMultiSelect = true`, `buttonContainer.visibility = VISIBLE`, `adapterr.notifyDataSetChanged()`; then `toggleSelection(holder, text)` |
| **Tap a verse row** | acts only while `isMultiSelect`: `toggleSelection(holder, text)` |
| `toggleSelection(holder, text)` | adds/removes `text` in the static `selectedText` and sets the row checkbox; when the list becomes empty it leaves multi-select (`isMultiSelect = false`, hide `buttonContainer`, `notifyDataSetChanged()`) |
| Tap `@id/copy_txt` | sorts `selectedText` by `substringBefore('.').toInt()`, joins with `"\n"`, `ClipData.newPlainText("bible", text.trim())` → `ClipboardManager.setPrimaryClip`. **No toast, and the selection is *not* cleared** (`BR12`, `BR5`) |
| Tap `@id/share_txt` | builds `"<book>, <chapter>\n\n"` + the sorted verses, wraps it as `"<text> \n\n\n\nLet me recommend you this application\n\n" + "https://salvationlamb.com/redirect"`, fires `ACTION_SEND` (`text/plain`, subject `"Salvation Lamb"`) via `Intent.createChooser(…, "choose one")`; the `catch` only logs; afterwards `selectedText = ArrayList()` |
| Tap `@id/post_txt` | if `selectedText.size <= 0` → toast `"Please select atleast one"`; else sorts + joins with `"\n\n"` and starts `BiblePostActivity` with `edition` = **`type`** (the launch extra, `BR10`), `content` = trimmed text, `tags` = `"<heading text>, <fakeSpinner text>"`. `selectedText = ArrayList()` runs **outside** the `else`. **No permission gate** (`BR11` = `B-9`) |
| Tap `@id/bible_bookmark` | if `Util.bookmarkedBible == "<spinner>,<heading>,<chapter>"` → `bookmarkedBible = "Dummy"`, border icon, `lifecycleScope { userPreferences.deleteBibleBookmark() }`; else → store the triple in `Util.bookmarkedBible`, filled icon, `lifecycleScope { userPreferences.saveBibleBookmark(triple) }` (`BR6` on the `"Dummy"` sentinel) |
| System Back | if `isMultiSelect` → leave selection mode and `return` (consumes Back); else `super.onBackPressed()` |

### Core methods (the whole render pipeline)

| Method | Contract |
|---|---|
| `setBibleEdition()` | Clears `bibleDropdown.adapter`, rebuilds `bibleList` = [`R.string.oldBible`, `R.string.newBible`], attaches an `ArrayAdapter(simple_spinner_dropdown_item)`. Pre-selection: if `bookmarkedEdition != "DUMMY"` → `setSelection(bibleList.indexOf(bookmarkedEdition))` — **`indexOf` returns −1 for a renamed / retranslated label** (`BR19` = `B-4`); otherwise `setSelection(if (type.contentEquals("old")) 0 else 1)`. Then installs the `OnItemSelectedListener` that calls `setHeading` with `Util.bible.get("Old"\|"New")` |
| `setHeading(list: JsonArray)` | Rebuilds `keyList`; for each book object adds `key.get("n").asString` to `keyList` and `bibleMap[n] = key.get("C").asJsonArray`. Attaches `ArrayAdapter(simple_dropdown_item_1line, keyList)` to `contentDropdown`. Then: bookmark path → `contentDropdown.setText(bookmarkedContent, false)` + `loadChapterList(bibleMap[keyList[keyList.indexOf(bookmarkedContent)]]!!)` — **`indexOf` = −1 on a cross-testament bookmark → `IndexOutOfBoundsException`** (`BR20` = `B-8`); else → `keyList[0]` + `loadChapterList(bibleMap[keyList[0]]!!)`. Finally wires `contentDropdown.setOnItemClickListener` |
| `loadChapterList(items: JsonArray)` | Rebuilds `chapterList` as `"1"…"N"` over `items` and fills `chapterMap[n] = key.get("V").asJsonArray` — **chapter numbers are the 1-based index**, nothing in the JSON carries them. Then: if `bookmarkedChapter != "DUMMY" && onLoad` → `fakeSpinner.setText(bookmarkedChapter, false)`, `onLoad = false`; else `setText("1", false)`. Ends with `setVersesList(fakeSpinner.text.toString().toInt() - 1)` — **another unguarded `toInt()`**, and a bookmarked chapter larger than the new book's chapter count → `IndexOutOfBoundsException` (`BR21`) |
| `showGridDropdown(items: ArrayList<String>)` | Inflates `grid_view.xml`, builds an anonymous `BaseAdapter` over `items` — whose `getView` actually reads **`chapterList[position]`, not `items[position]`** (`BR22`) — wraps it in a `PopupWindow(MATCH_PARENT, WRAP_CONTENT, focusable = true)`, sets `isOutsideTouchable` / `isFocusable`, `showAsDropDown(fakeSpinner)`. On item click: set the text, dismiss, `setVersesList(position)` |
| `setVersesList(position: Int)` | `isMultiSelect = false`; hides `buttonContainer`; **resets the static `selectedText`**; `adapterr = MyAdapter(chapterMap[chapterList[if (position < 0) 0 else position]]!!)`; `recyclerView.adapter = adapterr`. Then compares `Util.bookmarkedBible` with the live `"<spinner>,<heading>,<chapter>"` triple and sets the filled or border bookmark icon |
| `inner class MyAdapter(bibleArray: JsonArray)` | `onCreateViewHolder` inflates `child_bible.xml`. `onBindViewHolder` builds `text = "" + (position + 1) + ". " + bible.get("V").asString` — **verse numbers are the 1-based index too** — sets it on `holder.content`, (re)attaches the long-click/click listeners **inside `onBind`** (`BR23`), and syncs `selectedCheckBox.visibility` / `isChecked` from the static `selectedText`. `getItemCount() = bibleArray.size()`; the row cast is `bibleArray[position] as JsonObject` (`BR24`) |
| `MyViewHolder` | `content = itemView.findViewById(R.id.bible_content)`, `selectedCheckBox = itemView.findViewById(R.id.selected)` |

### The `bible.json` shape this screen depends on

```
{ "Old": [ { "n": "<book name>", "C": [ { "V": [ { "V": "<verse text>" }, … ] }, … ] }, … ],
  "New": [ … same … ] }
```

Chapter numbers = index in `C` (+1). Verse numbers = index in `V` (+1). The key `"V"` is used
twice with two different meanings (the verse array on a chapter, the verse text on a verse).
**Nothing is numbered in the payload** — an RN rewrite must keep array order byte-stable.

## 4. Validation rules

| Field | Rule | Failure message |
|---|---|---|
| `@id/post_txt` tap | `selectedText.size > 0` | toast `"Please select atleast one"` |
| `@id/copy_txt` / `@id/share_txt` tap | **none** — with an empty selection, copy writes an empty clip and share opens a chooser carrying only the boilerplate footer (`BR5`) | none |
| `@id/fakeSpinner` content | implicitly assumed to be a parseable `Int` in 3 places (`next`, `previous`, `loadChapterList`); the field is not `inputType="number"` and its `keyListener` is nulled, but `setText` is also called with the raw bookmark segment | `NumberFormatException` crash (`BR2`) |
| Launch extra `type` | `if (!intent.extras!!.getString("type").isNullOrEmpty())` — guards the **value** but not the **`extras` bundle** | NPE (`BR3`) |
| `Util.bookmarkedBible` | `!isNullOrEmpty() && split(",").size == 3` | silently ignored otherwise |
| `Util.bible` | **no check at all** | NPE (`BR1`) |

## 5. API contracts

**none.** `BibleActivity` performs **zero** network calls — no `Retrofit`, no
`Commons().isNetworkAvailable`, no callbacks. Its data is the already-cached `Util.bible`
(downloaded once by `SplashScreenActivity.getBible()` from `https://files.salvationlamb.com/`;
see `BIBLE_LEAD.md` §5 and `B-2`). The only outbound request in the team belongs to
`BIBLE_POST.md`.

## 6. Storage read / written

| Key | Type | Operation | Value |
|---|---|---|---|
| `bibleBookmark` | `String` (DataStore `SalvationLamb`) | **write** — `userPreferences.saveBibleBookmark(…)` inside `lifecycleScope.launch` | `"<bibleDropdown.selectedItem>,<contentDropdown.text>,<fakeSpinner.text>"`, e.g. `"பழைய ஏற்பாடு,ஆதியாகமம்,3"` |
| `bibleBookmark` | `String` | **delete** — `userPreferences.deleteBibleBookmark()` inside `lifecycleScope.launch` | removes the key; `Util.bookmarkedBible` is simultaneously set to the literal `"Dummy"` (`BR6`) |
| `bibleBookmark` | `String` | **read** — *not here*: `SplashScreenActivity` and `MainActivity` observe the flow and assign `Util.bookmarkedBible`; this screen only reads the static | — |
| any other key | — | none | — |

> **Flow quirk (PLATFORM/STORAGE):** `UserPreferences.bibleBookmark` maps the value with
> `preferences[BIBLE_BOOKMARK].toString()`, so after a delete the observers receive the **string
> `"null"`**, not `null`. The `size == 3` split check happens to reject it, so the sentinel never
> reaches the UI — but `Util.bookmarkedBible` can hold three different "empty" values over its
> lifetime: `null`, `"null"` and `"Dummy"` (`BR6`).

## 7. Global state touched

| Field | Operation | Value |
|---|---|---|
| `Util.bible` (static `JSONObject`) | **read, unguarded** — `Util.bible.get("Old")` / `.get("New")` in the spinner listener | the whole document; `null` until `SplashScreenActivity.getBible()` has run (`BR1`) |
| `Util.bookmarkedBible` (static `String`) | **read** in `onCreate` + `setVersesList`; **written** by the bookmark toggle (triple or `"Dummy"`) | `"edition,book,chapter"` |
| `BibleActivity.selectedText` (`companion object`, static) | read + written from the `onCreate` listeners, `setVersesList` and the inner adapter | reassigned to a fresh `ArrayList` in **6** places; never `clear()`ed (`BR4`) |
| `Util.userId` / `Util.user` / `Util.permissionMap` | **never touched** — no permission gate anywhere in this file (`BR11`) | — |
| `Util.fontSize` | **never applied** — verse text is fixed at 20dp (`BR14`) | — |

## 8. User-facing messages (exact strings)

| Trigger | Message | Mechanism |
|---|---|---|
| Tap **post** with no verses selected | `Please select atleast one` | `Toast.LENGTH_LONG` (typo, hard-coded) |
| Share chooser title | `choose one` | `Intent.createChooser` (hard-coded, lowercase) |
| Share subject | `Salvation Lamb` | `Intent.EXTRA_SUBJECT` |
| Share body suffix | `<verses> \n\n\n\nLet me recommend you this application\n\nhttps://salvationlamb.com/redirect` | `Intent.EXTRA_TEXT` |
| Copy succeeded | **none** — silent (`BR12`) | — |
| Bookmark added / removed | **none** — only the icon changes (`BR12`) | — |
| `Util.bible == null`, bad chapter text, cross-testament bookmark | **none** — the app crashes instead (`BR1`, `BR2`, `BR20`) | — |
| Share `catch` | `Log.e("exception", e.toString())` only | logcat |

## 9. Navigation map

| From | Trigger | To | Extras | finish()? |
|---|---|---|---|---|
| `BibleFragment` | tap a testament card | **`BibleActivity`** | `type` = `"old"` \| `"new"`, `FLAG_ACTIVITY_NEW_TASK` | no |
| `BiblePostActivity` | system Back | **`BibleActivity`** | `type` = `<edition>`, `FLAG_ACTIVITY_NEW_TASK` | no (plus `super.onBackPressed()`) |
| `BibleActivity` | tap `@id/prod_logo` | `MainActivity` | none | **no** (`BR7`) |
| `BibleActivity` | tap `@id/bible_to_home` | `MainActivity` | none | **no** (`BR7`) |
| `BibleActivity` | tap `@id/post_txt` with a selection | `BiblePostActivity` | `edition` = `type`, `content`, `tags` | no |
| `BibleActivity` | tap `@id/share_txt` | system chooser (`ACTION_SEND`) | text/plain payload | no |
| `BibleActivity` | Back while in multi-select | — (consumed: exits selection mode) | — | no |
| `BibleActivity` | Back otherwise | previous activity | — | yes (`super`) |

## 10. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | everything (see §3) — ~155 lines of setup in one method (`BR25`) |
| `onBackPressed` | overridden — consumes Back while `isMultiSelect` |
| `onStart` / `onResume` / `onPause` / `onStop` / `onDestroy` | **not overridden** |
| `onSaveInstanceState` / restore | **not implemented** — the selected edition / book / chapter and the selection are lost on recreation |
| Rotation | `android:configChanges="orientation\|keyboardHidden\|screenSize"` + `screenOrientation="sensorPortrait"` → the activity is **not** recreated on rotation; the layout only re-measures |
| Process death | `Util.bible` and `Util.bookmarkedBible` are **not** restored by this screen → `BR1` |
| `PopupWindow` | never dismissed from a lifecycle callback — leaving the screen with the chapter grid open leaks the window (`BR26`) |

## 11. Dependencies

| Dependency | Owner team | Used for |
|---|---|---|
| `Util.bible` (static `JSONObject`) | **PLATFORM / COMMONS** (filled by **AUTH / SPLASH**) | the entire content of the screen |
| `Util.bookmarkedBible` (static `String`) | **PLATFORM / COMMONS** | bookmark round-trip (also assigned by `SplashScreenActivity` and `MainActivity`) |
| `UserPreferences.saveBibleBookmark` / `deleteBibleBookmark` / key `bibleBookmark` | **PLATFORM / STORAGE** | persisting the bookmark |
| `R.string.oldBible` / `R.string.newBible` | APPSHELL / THEMING | the edition spinner **and the bookmark's first segment** (`BR19`) |
| `@style/BibleMaterialTheme`, `@color/primary_blue` / `home_bg` / `white` / `black`, `ic_baseline_bookmark*`, `baseline_keyboard_arrow_*`, `baseline_content_copy_24`, `baseline_share_24`, `rounded_border_login_register` | APPSHELL / THEMING | chrome |
| `activity/MainActivity` | APPSHELL / MAIN_NAV | logo + home navigation |
| `activity/BiblePostActivity` | **BIBLE / BIBLE_POST** | the composer |
| `fragments/BibleFragment` | **BIBLE / BIBLE_ENTRY** | the launcher |
| Gson (`JsonArray` / `JsonObject`), `com.facebook.shimmer` | PLATFORM / BUILD_CONFIG | parsing (shimmer unused) |
| `Util.hasPermission` / `NoPermissionActivity` | PLATFORM + APPSHELL | **not used — `BR11`** |

## 12. Known issues / tech debt

| # | Issue | Severity | Suggested fix |
|---|---|---|---|
| BR1 | **`Util.bible` dereferenced unguarded** (= `B-7`). `setBibleEdition`'s `onItemSelected` runs `Util.bible.get("Old"\|"New")`, and `setSelection()` fires it synchronously during `onCreate`. After process death the OS restores `MainActivity`, `Util.bible` is `null` (only ever assigned by `SplashScreenActivity.getBible()`), and opening the Bible tab **crashes immediately** | Critical | Null-check `Util.bible` at the top of `onCreate`; on `null` re-read `filesDir/bible.json` or route to splash with a toast. Make `Util.bible` lazily loadable (PLATFORM + AUTH) |
| BR20 | **`keyList[keyList.indexOf(bookmarkedContent)]`** in `setHeading` (= `B-8`). When the bookmarked book is not in the selected testament `indexOf` returns **−1** → `IndexOutOfBoundsException`. Repro: bookmark an Old-Testament chapter, then open the New Testament from the tab | Critical | `val i = keyList.indexOf(bookmarkedContent); if (i >= 0) … else keyList[0]`; better, store the testament id in the bookmark and apply it only to the matching edition |
| BR2 | **`fakeSpinner.text.toString().toInt()`** in `next` (×2), `previous` (×2) and `loadChapterList` (×1) — **5 unguarded parses** (= `B-14`). The field is an `AutoCompleteTextView`; any empty or non-numeric value throws `NumberFormatException` | High | `toIntOrNull() ?: 1`, or track the chapter index in an `Int` field and use the text for display only |
| BR19 | **The bookmark's edition segment is the localized display string** (= `B-4`): `bibleDropdown.selectedItem.toString()` is `R.string.oldBible` / `newBible`. Translating the app or editing those strings orphans every stored bookmark — `bibleList.indexOf(bookmarkedEdition)` returns −1 and `Spinner.setSelection(-1)` silently clears the selection | High | Store a stable id (`"old"` / `"new"`) and map to a label for display; needs a migration for existing values — PM-coordinated with PLATFORM/STORAGE |
| BR21 | `loadChapterList` applies `bookmarkedChapter` to **whatever book is loading**, then calls `setVersesList(chapter - 1)`. A bookmark at chapter 50 applied to a 4-chapter book → `chapterList[49]` → `IndexOutOfBoundsException` | High | Clamp to `chapterList.size`; apply the bookmark only when the book matches too |
| BR11 | **No permission gate before posting** (= `B-9`). `@id/post_txt` launches `BiblePostActivity` with no `Util.hasPermission(PermissionType.POST.value, Permission.CREATE.value)` check, while `HomeFragment` gates `AddPostActivity` with exactly that | High | Add the gate here **and** in `BiblePostActivity` (defence in depth); denial → `NoPermissionActivity` |
| BR3 | `intent.extras!!.getString("type")` (= `B-15`) — launching `BibleActivity` with no extras (deep link, `am start`, restored task) NPEs before anything renders | Medium | `intent?.extras?.getString("type") ?: "old"` |
| BR4 | **`companion object var selectedText: ArrayList<String>`** (= `B-6`) — static, process-wide state mutated from the inner adapter and reassigned (never `clear()`ed) in 6 places. A second activity instance shares it; it survives `finish()` and keeps verse strings alive | Medium | Make it an instance field (or `ViewModel` / RN state) and `clear()` instead of reassigning |
| BR8 | **Whole-testament re-serialize + re-parse on the main thread** (= `B-18`): each edition change runs `Gson().fromJson(Util.bible.get("Old").toString(), JsonArray::class.java)` (`org.json` → `String` → Gson tree), then `setHeading` rebuilds `bibleMap` and `loadChapterList` rebuilds `chapterMap`. Multi-MB, visible jank, during `onCreate` | Medium | Parse `bible.json` once with Gson at download time into a typed model, or index lazily per book |
| BR7 | Logo / home taps `startActivity(MainActivity)` **without `finish()` or `CLEAR_TOP`** (= `B-17`), stacking duplicate `MainActivity` instances | Medium | `finish()` after `startActivity`, or `FLAG_ACTIVITY_CLEAR_TOP \| FLAG_ACTIVITY_SINGLE_TOP` |
| BR10 | The composer receives the **launch `type`**, not `bibleDropdown`'s current value (= `B-5`). Open the Old Testament, switch to the New inside the reader, post → the post is titled "Bible post - பழைய ஏற்பாடு" | Medium | Pass `if (bibleDropdown.selectedItemPosition == 0) "old" else "new"`, or keep `type` in sync in `onItemSelected` |
| BR15 | `bibleMap` and `chapterMap` are `val HashMap`s that are **never cleared**. `bibleMap` accumulates the books of *both* testaments (name collisions silently overwrite); `chapterMap` keeps `"1".."N"` from the **previous** book, so `setVersesList` can index a stale chapter when the new `chapterList` is shorter | Medium | `clear()` both at the start of `setHeading` / `loadChapterList`, or key them by `"<edition>/<book>"` |
| BR22 | `showGridDropdown(items)` takes an `items` parameter, but `getView` reads **`chapterList[position]`** while `getCount` / `getItem` use `items` — the parameter is a lie. They are the same list today; the day they differ the grid renders wrong numbers or throws | Medium | Use `items[position]` consistently, or drop the parameter |
| BR23 | `MyAdapter.onBindViewHolder` attaches a **new long-click + click listener on every bind**, and leaving multi-select calls `notifyDataSetChanged()`; no `DiffUtil`, no stable ids | Medium | Set listeners in `onCreateViewHolder` using `bindingAdapterPosition`; use `notifyItemChanged` |
| BR16 | The `onLoad` flag guards only the **chapter**. `bookmarkedEdition` / `bookmarkedContent` are never reset, so every later edition change re-takes the bookmark branch in `setHeading` — switching testament jumps to the bookmarked book instead of book 1 (and triggers `BR20`) | Medium | Reset `bookmarkedContent` / `bookmarkedEdition` to `"DUMMY"` after first application, mirroring `onLoad` |
| BR5 | Copy and share do **not** check that anything is selected (only post does), and copy does **not** clear `selectedText` afterwards while share and post do — inconsistent, and an empty copy silently overwrites the user's clipboard | Medium | Guard all three with the same `selectedText.isEmpty()` check; align the clearing behaviour |
| BR12 | Almost no feedback (= `B-19`): copy is silent, bookmarking is silent, every failure path is silent-until-crash, and `@id/bible_shimmer_layout` — a ready-made loading state — is never shown | Medium | Toast / snackbar on copy + bookmark; use the shimmer while the JSON loads |
| BR17 | `fakeSpinner` has **both** `setOnClickListener` and `setOnFocusChangeListener` calling `showGridDropdown`, so the first tap can build and show **two** `PopupWindow`s | Low | Keep only the click listener, or guard with `popupWindow?.isShowing` |
| BR26 | The `PopupWindow` is a local variable, never dismissed in `onPause` / `onDestroy` — backgrounding or finishing with the grid open leaks a window (`WindowLeaked`) | Low | Hold it in a field and `dismiss()` in `onPause` |
| BR6 | Four different "cleared" representations for the bookmark: `Util.bookmarkedBible = "Dummy"`, the removed DataStore key, the flow re-emitting the **string `"null"`** (`preferences[...].toString()`), and the `"DUMMY"` field sentinel | Low | Use `null` / empty consistently; make the `UserPreferences` flow emit `String?` (PLATFORM) |
| BR9 | `child_bible.xml`'s root id is **`@+id/child_post_layout`**, duplicating FEED's `child_post.xml` row id (= `B-10`) | Low | Rename to `@+id/child_bible_layout` (nothing references it) |
| BR13 | Dead code: `bibleLinear` + `shimmerFrameLayout` bound but unused; `@id/bible_search` declared `gone` and never bound; the commented-out `chapterDropdown` field, its `findViewById` and both commented `next` / `previous` bodies; the 142-line shimmer block | Low | Delete all of it (~160 XML lines + ~15 Kotlin lines) |
| BR14 | Verse text is fixed at `20dp` in `child_bible.xml` — **dp not sp** — and `Util.fontSize` / the `textSize` DataStore key (the app-wide font-size setting) is ignored, unlike `HomeAdapter` | Low | Use `sp`; apply `Util.fontSize` in `onBindViewHolder` |
| BR18 | Force-unwraps on map lookups: `bibleMap[keyList[pos]]!!` (×3) and `chapterMap[chapterList[…]]!!` (×2) — each relies on the maps staying in sync with the lists (see `BR15`) | Low | `?: return` with a logged error, or model the data so the lookup cannot fail |
| BR24 | `bibleArray[position] as JsonObject` — unchecked cast on untyped Gson data (cf. the `as Fragment` cluster); a malformed `bible.json` yields `ClassCastException` instead of a handled error | Low | `as? JsonObject ?: return` |
| BR25 | `onCreate` is ~155 lines doing binding, listener wiring, intent parsing, bookmark parsing and the first render; the file is 448 lines with **no ViewModel, no repository, and the adapter nested inside the activity** | Low | Extract a `BibleRepository` + `ViewModel`; move `MyAdapter` to `adapter/` |
| BR27 | Hard-coded user-visible strings — `"post"`, `"Please select atleast one"` (typo), `"choose one"`, `"Salvation Lamb"`, `"Let me recommend you this application"`, `https://salvationlamb.com/redirect` (= `B-20`) — and no `contentDescription` on any of the 7 `ImageView` controls | Cosmetic | Move to `strings.xml`; add content descriptions |

> Team issues confirmed in this file: `B-4` (= `BR19`), `B-5` (= `BR10`), `B-6` (= `BR4`),
> `B-7` (= `BR1`), `B-8` (= `BR20`), `B-9` (= `BR11`), `B-10` (= `BR9`), `B-14` (= `BR2`),
> `B-15` (= `BR3`), `B-17` (= `BR7`), `B-18` (= `BR8`), `B-19` (= `BR12`), `B-20` (= `BR27`).
> **Not present here:** the `CL-1` always-true token guard (no network code) and the `CL-8`
> observer-in-click-handler cluster — both live in `BIBLE_POST.md`.

## 13. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/BibleActivity.kt` (incl. `MyAdapter`, `MyViewHolder`, `toggleSelection`) | **exclusive** |
| `app/src/main/res/layout/activity_bible.xml` | **exclusive** |
| `app/src/main/res/layout/child_bible.xml` | **exclusive** (but see `BR9` — its root id collides with FEED's) |
| `app/src/main/res/layout/grid_view.xml` | **exclusive** — only used by `showGridDropdown` |
| `app/src/main/res/layout/grid_spinner_item.xml` | **exclusive** — only used by `showGridDropdown` |
| `util/Util.java` (`bible`, `bookmarkedBible`) | **not owned** — PLATFORM / COMMONS |
| `util/UserPreferences.kt` (`bibleBookmark`) | **not owned** — PLATFORM / STORAGE |
| `activity/SplashScreenActivity.kt` (`getBible()`) | **not owned** — AUTH / SPLASH |
| `activity/BiblePostActivity.kt` | **not owned** — `BIBLE_POST.md` |
| `R.string.oldBible` / `newBible`, `@style/BibleMaterialTheme`, drawables | **not owned** — APPSHELL / THEMING |

## 14. How to make common changes

Recipes for the most likely change requests on this screen.

- **Fix the post-process-death crash (`BR1`, the team's #1 bug):** at the top of `onCreate`, before
  `setBibleEdition()`, add `if (Util.bible == null) { … }`. The clean fix is a PLATFORM helper
  (`Util.ensureBible(context)`) that re-reads `filesDir/bible.json` synchronously — the file is
  already on disk, so no network is needed. Escalate: the loader lives in AUTH/SPLASH.
- **Fix the cross-testament bookmark crash (`BR20`):** in `setHeading`, replace
  `keyList[keyList.indexOf(bookmarkedContent)]` with an index check and fall back to `keyList[0]`.
  Add the `BIBLE_LEAD` §8 regression test: bookmark an Old chapter → open the New Testament.
- **Make chapter navigation safe (`BR2`):** introduce `private var chapterIndex = 0` as the source
  of truth; `fakeSpinner` becomes display-only. All 5 `toInt()` sites then disappear.
- **Change the bookmark format (`BR19`/`BR6`):** this touches **four** files outside this module
  (`UserPreferences.kt`, `Util.java`, `SplashScreenActivity.kt`, `MainActivity.kt`) and needs a
  migration for already-stored values → **always PM-coordinated**; see `BIBLE_LEAD.md` §7.
- **Add a permission gate to posting (`BR11`):** wrap the `post.setOnClickListener` body in
  `if (Util.hasPermission(PermissionType.POST.value, Permission.CREATE.value)) … else startActivity(Intent(this, NoPermissionActivity::class.java))`. Mirror `HomeFragment`'s call exactly and add the same gate in `BiblePostActivity` (`BIBLE_POST.md`).
- **Send the real edition to the composer (`BR10`):** replace `intent.putExtra("edition", type)`
  with the spinner's current position. `BiblePostActivity` only compares against `"old"`, so the
  value must stay `"old"` / `"new"` — coordinate with `BIBLE_POST.md`.
- **Add verse search:** `@id/bible_search` already exists (currently `gone`, unbound). Search would
  need an index over `bibleMap` — remember it currently holds **both** testaments (`BR15`).
- **Support a second language / translation:** the document has exactly two top-level keys; any
  additional edition requires a `bible.json` schema change → AUTH/SPLASH + PLATFORM + this module,
  and an invalidation strategy for the never-refreshed cache (`B-2`).
- **Change the share text:** the chooser body is built inline in the `share` listener; move it to
  `strings.xml` first (`BR27`). The redirect URL `https://salvationlamb.com/redirect` is also
  hard-coded in FEED — keep them in sync via PM.

## 15. Change log

| Change | Detail |
|---|---|
| Created (T-026, 2026-10-08) | Documented the Bible reader (448 LOC + 4 layouts, 418 XML lines) against the v1.2.0 baseline: all 15 bound views + 2 helpers, the static `companion object selectedText`, the full `setBibleEdition` → `setHeading` → `loadChapterList` → `showGridDropdown` → `setVersesList` → `MyAdapter` pipeline, the implicit-index `"Old"`/`"New"` JSON contract, the `"edition,book,chapter"` bookmark round-trip through the `bibleBookmark` DataStore key (incl. the `"Dummy"` / `"DUMMY"` / `"null"` sentinel mess), and the copy / share / next / previous / post / bookmark actions. 27 issues recorded. Top defects: **`BR1`** — `Util.bible` dereferenced unguarded in the spinner listener, so the tab **crashes after process death** (`B-7`); **`BR20`** — `keyList[keyList.indexOf(bookmarkedContent)]` returns index −1 for a cross-testament bookmark → `IndexOutOfBoundsException` (`B-8`); **`BR2`** — five unguarded `fakeSpinner.text.toString().toInt()` parses (`B-14`); plus the ungated post entry (`BR11` = `B-9`), the localized-string bookmark key (`BR19` = `B-4`), the main-thread multi-MB re-parse on every edition change (`BR8` = `B-18`), and newly found `BR15` (never-cleared `bibleMap`/`chapterMap`), `BR16` (bookmark re-applied on every edition change), `BR21` (bookmarked chapter index not clamped to the new book), `BR22` (`showGridDropdown` ignores its own parameter) and `BR17`/`BR26` (double `PopupWindow`, never dismissed). |
