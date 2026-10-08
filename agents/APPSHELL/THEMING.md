# THEMING Module Agent

> Team: **APPSHELL** · Reports to: `agents/APPSHELL/APPSHELL_LEAD.md`
> This agent owns **every shared resource in the app**: colours, styles, themes, strings, menus,
> dimensions, drawables and all qualified `values-*` variants.
> It may only edit the files listed in §9 **Owned files**.

---

## ⚠ Every team depends on this agent

THEMING has **no screen and no code** — only resources. But its files are referenced by all 37
other agents. The seven-way escalation ("that's THEMING") lands here.

A one-line colour change is visible on every screen; a renamed string breaks compilation; a changed
menu item affects **6+ activities**.

---

## 1. Scope

| Folder | Contents |
|---|---|
| `res/values/` | `colors.xml`, `strings.xml`, `styles.xml`, `themes.xml`, `dimens.xml`, `fav_icon_background.xml`, `login-bg_background.xml` |
| `res/values-night/` | `colors.xml`, `styles.xml`, `themes.xml`, `dimens.xml` |
| `res/values-land/`, `values-w600dp/`, `values-w1240dp/` | size/orientation overrides |
| `res/menu/` | `main_menu.xml`, `react_menu.xml` |
| `res/drawable*/` | icons, backgrounds, the splash PNG, `covre_pic` |
| `res/mipmap-*/` | launcher icons (`fav_icon`, `fav_icon_round`) |
| `res/xml/` | **not owned** — `network_security_config.xml` is BUILD_CONFIG's |

---

## 2. The colour system — 7 colours, 2 themes

| Name | Day | Night | Note |
|---|---|---|---|
| `primary_blue` | `#0F52BA` | `#3B5999` | brand colour; also the status bar |
| `secondary_blue` | `#8BA9E9` | `#8BA9E9` | **identical in both** |
| `home_bg` | `#F0F3F9` | `#18243E` | screen background |
| `hintcolor` | `#52595D` | `#CCCCCC` | secondary text |
| **`black`** | `#FF000000` | **`#FFFFFFFF`** | **inverted in night mode** |
| **`white`** | `#FFFFFFFF` | **`#131C30`** | **inverted in night mode** |
| `always_white` | `#FFFFFFFF` | `#FFFFFFFF` | the escape hatch for genuinely-white content |

> **The most important fact in this document:** `black` and `white` are **semantic, not literal**.
> In night mode `@color/black` renders **white** and `@color/white` renders **dark navy**. Code or
> layouts that assume literal colours will be wrong in one theme (issue TH1).
>
> `always_white` exists precisely because of this — use it when a colour must stay white.

**Only 7 colours** for the whole app, so there is no error/success/warning colour; screens use
`Color.RED` in code instead (e.g. `Commons.makeWarrior`) (issue TH2).

---

## 3. Styles and themes

```xml
<style name="AppTheme" parent="Theme.AppCompat.DayNight">
    <item name="colorPrimary">@color/primary_blue</item>
    <item name="colorPrimaryDark">@color/black</item>
    <item name="windowActionBar">false</item>
    <item name="windowNoTitle">true</item>
    <item name="android:windowIsTranslucent">true</item>
    <item name="android:statusBarColor">@color/primary_blue</item>
</style>
<style name="menuStyle">
    <item name="android:layoutDirection">ltr</item>
</style>
```

| Fact | Detail |
|---|---|
| Base | `Theme.AppCompat.DayNight` — enables the whole night-mode mechanism |
| No ActionBar | `windowActionBar=false` + `windowNoTitle=true`, which is why every screen hand-rolls a `header_main` (issue TH3) |
| `windowIsTranslucent` | true app-wide — unusual, and it can cause transition artefacts (issue TH4) |
| `colorPrimaryDark` | `@color/black`, which is **white in night mode** (see TH1) (issue TH5) |
| `menuStyle` | forces **LTR** on the overflow menu, breaking RTL despite `supportsRtl="true"` in the manifest (issue TH6) |
| `themes.xml` | a separate file holding **one** style, alongside `styles.xml` — two files doing one job (issue TH7) |

---

## 4. Strings — 26 entries

| Group | Content |
|---|---|
| Product | `app_name`, `Product_name` ("SalvationLamb") — **two strings for one concept** (issue TH8) |
| Reactions | **`react1`–`react14`** — the feed's reaction list (a 15th is commented out) |
| Warrior | `make_me_warrior` — the long consent text in `Commons.makeWarrior` |
| Misc | a handful of labels |

**26 strings for a 21-screen app** means the overwhelming majority of UI text is **hard-coded in
layouts and Kotlin** — every module agent records this for its own screen. The app is effectively
**not localisable** (issue TH9).

### The reaction list is a shared contract

`react1`–`react14` are read by **FEED** (`HomeAdapter`'s 14-branch `when`) and displayed by
**FEED/VIEW_LIKES**. Adding one means editing `react_menu.xml` **and** `strings.xml` **and**
`HomeAdapter` — three files, two teams (FEED H6).

---

## 5. Menus

| File | Items | Consumers |
|---|---|---|
| `main_menu.xml` | `warrior`, `logout`, `edit_profile`, `fav`, `settings` | **6+ activities**, each with its own copy of the handler (AS-1) |
| `react_menu.xml` | `react1`–`react14` | `HomeAdapter`'s like popup [FEED] |

THEMING owns the **menu resources**; the **handlers** belong to the screens. That split is why a
single menu change ripples into six files.

---

## 6. Dimensions and qualified variants

`dimens.xml` exists in `values/`, `values-night/`, `values-land/`, `values-w600dp/` and
`values-w1240dp/`.

| Observation | Note |
|---|---|
| `values-night/dimens.xml` | **dimensions do not change with the theme** — almost certainly an accident of the project template (issue TH10) |
| `values-w600dp` / `w1240dp` | tablet breakpoints exist, but no screen uses a tablet-specific layout (issue TH11) |
| Layouts | nearly all sizes are hard-coded `dp`/`sp` **in the layouts**, not referenced from `dimens.xml` (issue TH12) |
| `textSize` | layouts use **`dp`** for text in several places instead of `sp`, ignoring the user's font scale (issue TH13) |

---

## 7. Drawables & assets

| Asset | Note |
|---|---|
| `splash_screen.png` | a **PNG**, stretched with no `scaleType` (SPLASH S3) |
| **`covre_pic`** | misspelled "cover" — the first-run dialog image (MAIN_NAV MN11) |
| Tab icons | `ic_baseline_home_24`, `ic_baseline_folder_24`, `ic_baseline_video_library_24`, `ic_baseline_audio_file_24`, `profile` |
| List/grid icons | `ic_baseline_list_24`, `ic_baseline_grid_on_24` (MEDIA) |
| File icons | `folder_icon`, `ic_baseline_insert_drive_file_24` (MEDIA) |
| Shapes | `rounded_border_login_register` — the card background on every AUTH screen |
| Launcher | `fav_icon`, `fav_icon_round` (mipmap, all densities) |

---

## 8. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| TH1 | **`black` and `white` are inverted in night mode** — the names lie | **High** | rename to `text_primary` / `surface`, app-wide |
| TH9 | Only 26 strings; nearly all UI text is hard-coded — the app is **not localisable** | **High** | extract strings (a very large, cross-team task) |
| TH2 | No error/success/warning colours; screens use `Color.RED` in code | Medium | add semantic colours |
| TH13 | Text sizes declared in **`dp`** rather than `sp` in several layouts | Medium (accessibility) | switch to `sp` |
| TH6 | `menuStyle` forces LTR despite `supportsRtl="true"` | Medium | remove, or justify |
| TH3 | No ActionBar, so all 21 screens hand-roll a header | Medium | a shared header layout/`include` |
| TH5 | `colorPrimaryDark` is `@color/black`, i.e. white at night | Medium | use an explicit colour |
| TH12 | Sizes are hard-coded in layouts rather than in `dimens.xml` | Medium | centralise |
| TH4 | `windowIsTranslucent=true` app-wide | Medium | verify it is needed |
| TH10 | `values-night/dimens.xml` exists though dimensions do not vary by theme | Low | delete |
| TH11 | Tablet breakpoints exist with no tablet layouts | Low | implement or remove |
| TH8 | Both `app_name` and `Product_name` hold "SalvationLamb" | Low | keep one |
| TH7 | `styles.xml` and `themes.xml` split two styles across two files | Low | merge |
| TH14 | `covre_pic` is misspelled | Low | rename (touches MAIN_NAV) |

---

## 9. Owned files

| Path | Ownership |
|---|---|
| `app/src/main/res/values/**` | **exclusive** |
| `app/src/main/res/values-night/**` | **exclusive** |
| `app/src/main/res/values-land/**`, `values-w600dp/**`, `values-w1240dp/**` | **exclusive** |
| `app/src/main/res/menu/main_menu.xml`, `react_menu.xml` | **exclusive** (resources only — handlers belong to screens) |
| `app/src/main/res/drawable*/**`, `mipmap-*/**` | **exclusive** |

**Not owned (escalate):** every `res/layout/**` file (each belongs to its screen's agent),
`res/xml/network_security_config.xml` (**PLATFORM/BUILD_CONFIG**), `res/navigation/**` (unused),
and all source code.

---

## 10. How to make common changes

**Change a colour:** edit **both** `values/colors.xml` and `values-night/colors.xml`, then check a
screen in each mode. Remember `black`/`white` are inverted (TH1) — if you need literally white, use
`always_white`.

**Add a colour:** add it to both files. If it is semantic (error, success), say so in the name and
tell PM, since screens currently hard-code `Color.RED`.

**Add or change a string:** add it to `strings.xml`. If it replaces hard-coded text on a screen,
that screen's agent must make the layout/code change — **two-agent task**.

**Add a reaction (FEED H6):** add the item to `react_menu.xml` **and** the string to `strings.xml`
here, then FEED adds the `when` branch in `HomeAdapter`. **Always PM-coordinated.**

**Change the overflow menu:** editing `main_menu.xml` is local, but **every handler is a copy** in
6+ activities (AS-1). Adding an item means 6 follow-up edits across FEED, PROFILE and APPSHELL —
PM first.

**Change the theme:** `AppTheme` is applied app-wide by the manifest and switched by MAIN_NAV.
Verify in Day, Night **and** Default (follow-system) modes.

**Add a drawable:** drop it in the right density folders and tell the consuming agent. Prefer
vectors over PNGs (the splash is still a PNG).

---

## 11. Change log

| Change | Detail |
|---|---|
| Created | Initial THEMING module agent documented from `res/values/`, `res/values-night/`, `res/menu/` and the drawable set: the **7-colour system with `black`/`white` deliberately inverted at night**, `AppTheme` (`DayNight`, no ActionBar, translucent window) and `menuStyle` (forced LTR), the **26 strings** including the `react1`–`react14` contract shared with FEED, both menu files and their 6+ consumers, the qualified `dimens` variants, and 14 known issues. |


