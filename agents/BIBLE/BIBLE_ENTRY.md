# BIBLE_ENTRY Module Agent

> Team: **BIBLE** · Reports to: `agents/BIBLE/BIBLE_LEAD.md`
> Scope: this agent knows **every detail** of this screen — views, actions, API, storage,
> validation, messages, navigation. It may only edit the files under **Owned files**.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name (user-facing) | **Bible tab** — the third bottom tab in `MainActivity` (icon `@drawable/bible_book`, tab tag `"Bible"`) |
| Class | `com.veha.fragments.BibleFragment` (63 LOC, Kotlin, extends `androidx.fragment.app.Fragment`) |
| Layout | `res/layout/fragment_bible.xml` (218 LOC) — `FrameLayout` root; `tools:context` wrongly says `com.veha.fragments.FilesFragment` (`BE8`) |
| Manifest entry | none — it is a fragment, hosted by `MainActivity` |
| Entered from | `TabAdapter.getItem(2)` → `BibleFragment()` — no arguments, no `newInstance`/`companion object`. Tab added in `MainActivity` at index 2 (`tabLayout.addTab(bibleBook, 2)`) |
| Exits to | `BibleActivity` only (two cards, extra `type` = `"old"` \| `"new"`, `FLAG_ACTIVITY_NEW_TASK`) |

**One-line summary:** a static two-card chooser (Old Testament / New Testament) that does nothing
except launch the reader. It makes **no network call**, reads **no storage**, holds **no state**,
and — unlike every sibling tab — performs **no permission check**.

## 2. UI inventory

| View id | Type | Text / hint | Notes |
|---|---|---|---|
| `@id/bible_shimmer_layout` | `com.facebook.shimmer.ShimmerFrameLayout` | — | `visibility="gone"` in XML; bound to `shimmerFrameLayout` in code but **never shown, hidden or started** (`BE2`). Contains 9 hard-coded disabled `TextInputLayout`/`TextInputEditText` placeholder rows copied from `fragment_files.xml` |
| `@id/bible_layout` | `LinearLayout` (vertical, `@color/white`) | — | The real content. Bound to `bibleLayout` in code and then **never used** (`BE2`) |
| `@id/oldd` | `ConstraintLayout`, `layout_weight=".5"` | — | **Card 1** — clickable; the Old-Testament tile |
| — (child of `oldd`) | `ImageView` | `src="@drawable/old_bible"` | Full-width cover art; no id, no `contentDescription` (`BE7`) |
| — (child of `oldd`) | `TextView` | `@string/oldBible` = **"பழைய ஏற்பாடு"** | 20**dp** text size (should be `sp`, `BE6`), bold, `@color/black`, opaque `@color/white` strip over the image |
| `@id/neww` | `ConstraintLayout`, `layout_weight=".5"` | — | **Card 2** — clickable; the New-Testament tile |
| — (child of `neww`) | `ImageView` | `src="@drawable/new_bible"` | same as above, no id |
| — (child of `neww`) | `TextView` | `@string/newBible` = **"புதிய ஏற்பாடு"** | same styling as the Old card |

**Declared but unused fields** (`BE2`): `userPreferences: UserPreferences` (constructed in
`onCreateView`, never read), `viewPager: ViewPager` and `shimmerFrameLayout: ShimmerFrameLayout`
(the first is never even `findViewById`-ed — a `lateinit` that can only ever throw),
`bibleLayout: LinearLayout`.

### Static text (no id)

| Text | Notes |
|---|---|
| `@string/oldBible` — `பழைய ஏற்பாடு` | Tamil text living in the **default** `values/strings.xml` (no `values-ta/`) — `B-20`. Also the **first segment of the bookmark key** written by `BIBLE_READER` (`B-4`) |
| `@string/newBible` — `புதிய ஏற்பாடு` | same |
| 9 empty disabled `TextInputEditText` rows | inside the never-shown shimmer block |

## 3. Actions / event handlers

| Trigger | Behaviour |
|---|---|
| Tap card `@id/oldd` | `Intent(contexts, BibleActivity::class.java)`, `putExtra("type", "old")`, `setFlags(FLAG_ACTIVITY_NEW_TASK)`, `contexts.startActivity(intent)`. **No permission gate, no `Util.bible` null check** (`BE1`, `BE3`) |
| Tap card `@id/neww` | identical, with `putExtra("type", "new")` |
| `onCreate(Bundle?)` | overridden; body is **only** `super.onCreate(...)` — dead override (`BE9`) |
| `onCreateView` | `contexts = container!!.context` (**force-unwrap**, `BE4`); inflates `fragment_bible`; builds `UserPreferences(contexts)`; binds 4 views; wires the 2 listeners; returns the view |

There is **no** `onViewCreated`, `onResume`, `onDestroyView`, no state save/restore and no
`setArguments` contract — the fragment is entirely stateless.

## 4. Validation rules

| Field | Rule | Failure message |
|---|---|---|
| — | none — the screen has no inputs | none |

> **Missing pre-condition check:** the reader it launches dereferences `Util.bible` (a static
> `JSONObject` seeded only by `SplashScreenActivity`). This fragment does **not** verify
> `Util.bible != null` before launching, which is the proximate trigger for the team's worst crash
> (`B-7` / `BE3`).

## 5. API contracts

**none.** `BibleFragment` makes no Retrofit call of any kind. (It imports `com.veha.util.Util` but
never references it — `BE2`.)

## 6. Storage read / written

| Key | Type | Operation | Value |
|---|---|---|---|
| — | — | **none** | `UserPreferences(contexts)` is instantiated and assigned to `userPreferences`, but no `Flow` is collected and nothing is written (`BE2`) |

## 7. Global state touched

| Field | Operation | Value |
|---|---|---|
| — | — | **none**. `Util` is imported but unused; `Util.bible` / `Util.bookmarkedBible` are read only in `BibleActivity` |

## 8. User-facing messages (exact strings)

| Trigger | Message | Mechanism |
|---|---|---|
| — | none — the screen shows no toast, dialog, snackbar or inline error | — |

## 9. Navigation map

| From | Trigger | To | Extras | finish()? |
|---|---|---|---|---|
| `MainActivity` (tab 2) | tab selected / swipe to index 2 | `BibleFragment` | none (no-arg constructor) | n/a |
| `BibleFragment` | tap `@id/oldd` | `BibleActivity` | `type` = `"old"`; flags `FLAG_ACTIVITY_NEW_TASK` | no |
| `BibleFragment` | tap `@id/neww` | `BibleActivity` | `type` = `"new"`; flags `FLAG_ACTIVITY_NEW_TASK` | no |
| `BiblePostActivity` | system Back | `BibleActivity` (not here) | `type` = `<edition>` | see `BIBLE_POST.md` |

> **`FLAG_ACTIVITY_NEW_TASK` from a fragment** (`B-16` / `BE5`): the flag is unnecessary — the
> context is an Activity context — and it makes the launched `BibleActivity` land in a task whose
> affinity / back behaviour depends on OS version and launch-mode defaults. Back from the reader
> can drop the user out of the app instead of returning to the tab.

## 10. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate(Bundle?)` | `super` only — no-op override |
| `onCreateView` | binds `contexts`, inflates, builds `UserPreferences`, binds `oldd` / `neww` / `bibleLayout` / `shimmerFrameLayout`, sets 2 click listeners, returns the view |
| `onViewCreated` / `onStart` / `onResume` / `onPause` / `onDestroyView` | **not overridden** |
| Configuration change / process death | the fragment is recreated by `FragmentPagerAdapter`; it keeps no state, so it recreates cleanly — but `Util.bible` does **not** (`B-7`) |
| Pager retention | `TabAdapter` extends the deprecated `FragmentPagerAdapter`, so the instance stays alive across tab swipes (only `onDestroyView` runs) |

## 11. Dependencies

| Dependency | Owner team | Used for |
|---|---|---|
| `MainActivity` + `adapter/TabAdapter.kt` (`getItem(2)`) | **APPSHELL / MAIN_NAV** | the host; tab index, icon `@drawable/bible_book`, tag `"Bible"` |
| `activity/BibleActivity` | **BIBLE / BIBLE_READER** | the only destination |
| `R.string.oldBible`, `R.string.newBible`, `@drawable/old_bible`, `@drawable/new_bible`, `@color/white` | APPSHELL / THEMING | card labels + art |
| `com.facebook.shimmer.ShimmerFrameLayout` | PLATFORM / BUILD_CONFIG | declared in the layout, never used |
| `util/UserPreferences` | PLATFORM / STORAGE | instantiated, never used |
| `Util.hasPermission` / `NoPermissionFragment` | PLATFORM / COMMONS + APPSHELL | **not used — that is the bug** (`BE1`) |

## 12. Known issues / tech debt

| # | Issue | Severity | Suggested fix |
|---|---|---|---|
| BE1 | **No permission gate** (= `B-1`). `TabAdapter.getItem` gates positions 0, 1, 3, 4 and 5 with `Util.hasPermission(...)` and falls back to `NoPermissionFragment()`; **position 2 returns `BibleFragment()` unconditionally**. There is no `PermissionType.BIBLE` enum value either, so no server policy can switch the tab off | High | Decide the policy with PM: add a gate in `TabAdapter` branch 2 (an APPSHELL change) or explicitly document the tab as public |
| BE2 | **Dead members everywhere**: `viewPager` is a `lateinit` that is never assigned (any read throws `UninitializedPropertyAccessException`); `shimmerFrameLayout` and `bibleLayout` are bound but never touched; `userPreferences` is constructed but never read; `Util` is imported but unused; the 148-line shimmer block is `gone` and never started | Medium | Delete `viewPager`, `shimmerFrameLayout`, `bibleLayout`, `userPreferences`, the unused imports and the `@id/bible_shimmer_layout` subtree (≈148 of 218 layout lines) |
| BE3 | **Launches the reader without checking `Util.bible`** (feeds `B-7`). After process death the OS restores `MainActivity` → tab 2 → tapping a card opens `BibleActivity`, whose first spinner callback does `Util.bible.get("Old")` on a `null` static → NPE | High | Guard here (`if (Util.bible == null)` → toast + route to splash / re-download) **and** fix the root cause in `BIBLE_READER` / AUTH |
| BE4 | `contexts = container!!.context` force-unwraps the `ViewGroup?`. `onCreateView` can legitimately be called with a `null` container → NPE before the layout is inflated | Medium | Use `requireContext()` and drop the `contexts` field |
| BE5 | **`FLAG_ACTIVITY_NEW_TASK` from a fragment** on both cards (= `B-16`). Unnecessary with an Activity context; makes the back stack version-dependent | Medium | Remove `setFlags(...)`; plain `startActivity(intent)` |
| BE6 | Both card labels use `android:textSize="20dp"` (**dp, not sp**) and the screen ignores `Util.fontSize` / the `textSize` DataStore key that `SettingsActivity` controls | Low | Change to `sp`; apply `Util.fontSize` the way `HomeAdapter` does |
| BE7 | No `contentDescription` on either cover `ImageView`; the clickable cards are plain `ConstraintLayout`s with no `focusable` / `clickable` / ripple — TalkBack announces nothing and there is no touch feedback | Low | Add `contentDescription`, `android:focusable="true"`, `?attr/selectableItemBackground` |
| BE8 | `fragment_bible.xml` declares `tools:context="com.veha.fragments.FilesFragment"` — leftover from the copy of `fragment_files.xml` (same 9-row shimmer block) | Cosmetic | Set it to `com.veha.fragments.BibleFragment` |
| BE9 | Empty `onCreate` override whose body is only `super.onCreate(savedInstanceState)` | Cosmetic | Delete the override |
| BE10 | The two cards are near-identical 30-line XML blocks plus two near-identical 6-line listeners; the only difference is one string | Cosmetic | Extract an `<include>` + a single `openBible(type: String)` helper |

> Inherited team issues that surface here: **`B-1`** (= `BE1`), **`B-16`** (= `BE5`),
> **`B-20`** (Tamil strings in the default `values/` folder).

## 13. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/fragments/BibleFragment.kt` | **exclusive** |
| `app/src/main/res/layout/fragment_bible.xml` | **exclusive** |
| `app/src/main/java/com/veha/adapter/TabAdapter.kt` | **not owned** — APPSHELL / MAIN_NAV. Any permission-gate change (`BE1`) must be escalated |
| `res/drawable/old_bible.*`, `new_bible.*`, `bible_book.*`, `R.string.oldBible` / `newBible` | **not owned** — APPSHELL / THEMING |
| `app/src/main/java/com/veha/activity/BibleActivity.kt` | **not owned** — `BIBLE_READER.md` |

## 14. How to make common changes

Recipes for the most likely change requests on this screen.

- **Add a third card (e.g. "Bookmarks"):** add a `ConstraintLayout` inside `@id/bible_layout`
  (adjust the `layout_weight`s — they currently sum to 1.0 across two children), bind it in
  `onCreateView`, launch with a new `type` value. **`BibleActivity` only understands `"old"` /
  `"new"`**: anything else falls through `type.contentEquals("old")` and silently opens the New
  Testament. Coordinate with `BIBLE_READER.md`.
- **Gate the tab behind a permission (`BE1`):** this is a **`TabAdapter.kt` edit → APPSHELL task**.
  Mirror branch 1: `if (Util.hasPermission(PermissionType.X.value, Permission.READ.value)) BibleFragment() else NoPermissionFragment()`. A new `PermissionType` value is a **PLATFORM** change;
  PM must sequence PLATFORM → APPSHELL → BIBLE.
- **Show a loading / empty state while `bible.json` is missing:** reuse the currently dead
  `@id/bible_shimmer_layout` — toggle it on `Util.bible == null` and ask AUTH/SPLASH for a
  re-download entry point (`B-2`).
- **Change a card label:** edit `R.string.oldBible` / `R.string.newBible` — **THEMING-owned, and a
  breaking change for every stored bookmark** (`B-4`), because the reader stores the *display
  string* as the bookmark's edition segment. PM-coordinated with `BIBLE_READER.md` +
  PLATFORM/STORAGE.
- **Fix the back-stack behaviour (`BE5`):** drop `FLAG_ACTIVITY_NEW_TASK` here **and** the matching
  flag in `BiblePostActivity.onBackPressed` — both, otherwise the composer still creates a stray task.
- **Delete the dead shimmer (`BE2`):** safe — nothing outside this fragment references
  `@id/bible_shimmer_layout` in `fragment_bible.xml`; remove the field, the `findViewById` and the
  XML subtree.

## 15. Change log

| Change | Detail |
|---|---|
| Created (T-026, 2026-10-08) | Documented the Bible tab entry fragment (63 LOC + 218 LOC layout) against the v1.2.0 baseline: the two testament cards, the `type` = `"old"` / `"new"` contract into `BibleActivity`, the complete absence of network / storage / global-state usage, and the dead shimmer, `viewPager` and `userPreferences` members. Key defects: **`BE1`** — the Bible tab is the only `TabAdapter` branch with **no `Util.hasPermission` gate** (positions 0/1/3/4/5 all fall back to `NoPermissionFragment`), confirming `B-1`; **`BE3`** — the cards launch the reader without checking the `Util.bible` static, the trigger for the `B-7` post-process-death NPE; **`BE5`** — `FLAG_ACTIVITY_NEW_TASK` used from a fragment (`B-16`). Plus the `container!!` force-unwrap, a never-assigned `lateinit viewPager`, and a 148-line copy-pasted shimmer block that is `gone` and never started. |
