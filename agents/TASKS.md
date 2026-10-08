# TASKS.md — Global Task Board

> Maintained by the **PROJECT MANAGER** agent (`AGENTS.md`).
> Every customer request is logged here **with date & time** before work starts, and updated
> again when it is done. Timezone: **IST**. Format: `YYYY-MM-DD HH:MM:SS`.

---

## Active / recent tasks

| Task ID | Opened (IST) | Request | Team | Module agent(s) | Status | Closed (IST) | Files touched |
|---|---|---|---|---|---|---|---|
| T-001 | 2026-10-01 11:13:24 | Set up the agent hierarchy (PM -> team leads -> module agents) for the whole project | PM | — | DONE | 2026-10-01 11:30:00 | `AGENTS.md`, `agents/TASKS.md`, `agents/_templates/LEAD_TEMPLATE.md`, `agents/_templates/MODULE_TEMPLATE.md` |
| T-002 | 2026-10-01 11:13:24 | Create the AUTH team lead agent | AUTH | — | DONE | 2026-10-01 11:30:00 | `agents/AUTH/AUTH_LEAD.md` |
| T-003 | 2026-10-01 11:13:24 | Document the LOGIN module agent in full detail (fields, buttons, links, API, storage, errors, UI) | AUTH | `LOGIN.md` | DONE — verified by customer | 2026-10-01 11:30:00 | `agents/AUTH/LOGIN.md` |
| T-004 | 2026-10-01 11:47:10 | Document the REGISTER module agent in full detail | AUTH | `REGISTER.md` | DONE — verified by customer | 2026-10-01 11:58:00 | `agents/AUTH/REGISTER.md`, `agents/AUTH/AUTH_LEAD.md` (modules table, flow diagram, issues A11/A12, change log) |
| T-005 | 2026-10-01 16:05:00 | Document the FORGOT_PASSWORD module agent in full detail | AUTH | `FORGOT_PASSWORD.md` | DONE — verified by customer | 2026-10-01 16:18:00 | `agents/AUTH/FORGOT_PASSWORD.md`, `agents/AUTH/AUTH_LEAD.md` (modules table, flow diagram, issues A13/A14/A15, change log), `AGENTS.md` (status) |
| T-006 | 2026-10-01 16:32:00 | Document the OTP_VERIFY module agent in full detail | AUTH | `OTP_VERIFY.md` | DONE — verified by customer | 2026-10-01 16:45:00 | `agents/AUTH/OTP_VERIFY.md`, `agents/AUTH/AUTH_LEAD.md` (modules table, flow diagram, issues A16/A17/A18, change log), `AGENTS.md` (status) |
| T-007 | 2026-10-01 17:02:00 | Document the CHANGE_PASSWORD module agent in full detail (both signed-in and reset modes) | AUTH | `CHANGE_PASSWORD.md` | SUPERSEDED by T-008 | 2026-10-01 17:20:00 | `agents/AUTH/CHANGE_PASSWORD.md`, `agents/AUTH/AUTH_LEAD.md`, `AGENTS.md` |
| T-008 | 2026-10-01 17:34:00 | Customer instruction: split the dual-mode Change Password agent into **two** agents, one per mode | AUTH | `CHANGE_PASSWORD.md`, `RESET_PASSWORD.md` | DONE — verified by customer | 2026-10-01 17:52:00 | `agents/AUTH/CHANGE_PASSWORD.md` (rewritten, signed-in only), `agents/AUTH/RESET_PASSWORD.md` (new), `agents/AUTH/AUTH_LEAD.md` (7-module table, dual-class warning block, API surface, delegation rules, issues A23-A25, change log), `AGENTS.md` (org chart, AUTH table, new dual-mode rule) |
| T-009 | 2026-10-01 18:05:00 | Document the SPLASH module agent in full detail — final AUTH module | AUTH | `SPLASH.md` | DONE — verified by customer | 2026-10-01 18:22:00 | `agents/AUTH/SPLASH.md` (new), `agents/AUTH/AUTH_LEAD.md` (status all READY, issues A26-A30, change log), `AGENTS.md` (AUTH marked COMPLETE 7/7) |
| T-010 | 2026-10-01 18:40:00 | Customer: bug fixes deferred — prioritise agent creation. Build the **PLATFORM team** (lead + 5 module agents) | PLATFORM | all 5 | DONE — verified by customer | 2026-10-01 19:15:00 | `agents/PLATFORM/PLATFORM_LEAD.md`, `NETWORK.md`, `DATA_MODELS.md`, `STORAGE.md`, `COMMONS.md`, `BUILD_CONFIG.md` (all new), `AGENTS.md` (PLATFORM marked COMPLETE 5/5) |
| T-011 | 2026-10-01 19:30:00 | Build the **FEED team**: lead + the largest module agent (HomeFragment 528 + HomeAdapter 680 LOC) | FEED | `FEED_LEAD.md`, `HOME_FEED.md` | DONE — verified by customer | 2026-10-01 20:05:00 | `agents/FEED/FEED_LEAD.md` (new), `agents/FEED/HOME_FEED.md` (new), `AGENTS.md` (FEED 1/6 READY) |
| T-012 | 2026-10-01 20:20:00 | Complete the FEED team — remaining 5 module agents | FEED | `ADD_POST.md`, `VIEW_POST.md`, `VIEW_LIKES.md`, `FAVORITES.md`, `IMAGE_DETAIL.md` | DONE — verified by customer | 2026-10-01 21:10:00 | 5 new agent files in `agents/FEED/`, `agents/FEED/FEED_LEAD.md` (module table + change log), `AGENTS.md` (FEED marked COMPLETE 6/6) |
| T-013 | 2026-10-01 21:25:00 | Build the **PROFILE team** (lead + 4 module agents, incl. the 738-line EditProfileActivity) | PROFILE | all 4 | DONE — verified by customer | 2026-10-01 22:30:00 | `agents/PROFILE/PROFILE_LEAD.md`, `MY_PROFILE.md`, `VIEW_PROFILE.md`, `EDIT_PROFILE.md`, `FOLLOWERS.md` (all new), `AGENTS.md` (PROFILE marked COMPLETE 4/4) |
| T-014 | 2026-10-01 22:45:00 | Build the **MEDIA team** (lead + 6 module agents) | MEDIA | all 6 | DONE — verified by customer | 2026-10-02 00:05:00 | `agents/MEDIA/MEDIA_LEAD.md`, `FILES_BROWSER.md`, `FILE_LIST.md`, `PDF_VIEWER.md`, `ADMIN_AUDIO.md`, `ADMIN_VIDEO.md`, `WEBVIEW.md` (all new), `AGENTS.md` (MEDIA marked COMPLETE 6/6) |
| T-015 | 2026-10-02 00:20:00 | Build the **SEARCH team** (lead + 3 module agents) | SEARCH | all 3 | DONE — verified by customer | 2026-10-02 01:00:00 | `agents/SEARCH/SEARCH_LEAD.md`, `SEARCH_ENTRY.md`, `SEARCH_POSTS.md`, `SEARCH_PROFILES.md` (all new), `AGENTS.md` (SEARCH marked COMPLETE 3/3) |
| T-016 | 2026-10-02 01:15:00 | Build the **APPSHELL team** (lead + 4 module agents) — the final team | APPSHELL | all 4 | DONE — awaiting customer verification | 2026-10-02 02:10:00 | `agents/APPSHELL/APPSHELL_LEAD.md`, `MAIN_NAV.md`, `SETTINGS.md`, `ABOUT.md`, `THEMING.md` (all new), `AGENTS.md` (APPSHELL marked COMPLETE 4/4) |
| T-017 | 2026-10-08 00:00:00 | Customer: "We can create a BUG_NOTES.md to maintain the known bugs" — consolidate **every** known issue scattered across the 42 agent docs into one searchable register | PM | — (reads all teams) | DONE | 2026-10-08 00:00:00 | `agents/BUG_NOTES.md` (new, 1175 lines), `agents/tools/sync_bug_notes.py` (new, 369 lines), `AGENTS.md` (§2 folder convention, §5 new hard rule, §7 pointer) |
| T-018 | 2026-10-08 11:50:00 | Close `G10` (no VCS): connect this working folder to the customer's GitHub remote `vehasoft/Veha_salvationlamb_MobleApp` and make a safe baseline commit without leaking signing keys | PM | — | **DONE (local)** — committed on new branch `salvation_lamb_agent_baseline`, **not yet pushed** (awaiting customer go-ahead) | 2026-10-08 12:20:00 | `.git/` (initialised, `origin` added), `.gitignore` (IDE noise + new-keystore block), commit `b75c249` on top of `78e9b5c` |
| T-019 | 2026-10-08 12:02:00 | **Discovery raised by T-018:** remote has a branch `salvation_lamb_permissions_final_1` (v1.2.0, `versionCode 22`, 2026-02-26) that is **123 commits ahead** of the code this folder holds. All 42 agent docs describe remote `master` (v1.1, `versionCode 6`, 2023-09-16) — i.e. **the documentation is ~2.5 years stale**. Decision needed on re-baselining. | PM | all teams (potential re-doc) | **OPEN — awaiting customer decision** | — | *(nothing written yet — investigation only)* |
| T-020 | 2026-10-08 12:20:00 | **Security finding from T-018:** `app/Key/key.jks` + `app/Key/private_key.pepk` are **already committed to remote history** since `3d34164` (2023-08-29) and are present on every branch. Removing them needs a history rewrite + key rotation. | PLATFORM | `BUILD_CONFIG.md` | **OPEN — needs customer decision (rotate keys?)** | — | *(nothing written yet)* |


## Backlog (agreed build order)

The customer verifies each module agent before the next one is written.

| # | Deliverable | Team | Status |
|---|---|---|---|
| 1 | `LOGIN.md` | AUTH | **DONE — verified** |
| 2 | `REGISTER.md` | AUTH | **DONE — verified** |
| 3 | `FORGOT_PASSWORD.md` | AUTH | **DONE — verified** |
| 4 | `OTP_VERIFY.md` | AUTH | **DONE — verified** |
| 5 | `CHANGE_PASSWORD.md` (signed-in mode) | AUTH | **DONE — verified** |
| 6 | `RESET_PASSWORD.md` (after-OTP mode) | AUTH | **DONE — verified** |
| 7 | `SPLASH.md` | AUTH | **DONE — verified** |
| — | **AUTH TEAM COMPLETE — 7/7 module agents READY** | AUTH | ✅ |
| 8 | `PLATFORM` team (lead + NETWORK, DATA_MODELS, STORAGE, COMMONS, BUILD_CONFIG) | PLATFORM | **DONE — verified** |
| — | **PLATFORM TEAM COMPLETE — 5/5 module agents READY** | PLATFORM | ✅ |
| 9 | `FEED` team — `FEED_LEAD.md` + `HOME_FEED.md` | FEED | **DONE — verified** |
| 10 | `FEED` remaining 5: ADD_POST, VIEW_POST, VIEW_LIKES, FAVORITES, IMAGE_DETAIL | FEED | **DONE — verified** |
| — | **FEED TEAM COMPLETE — 6/6 module agents READY** | FEED | ✅ |
| 11 | `PROFILE` team (lead + 4 modules) | PROFILE | **DONE — verified** |
| — | **PROFILE TEAM COMPLETE — 4/4 module agents READY** | PROFILE | ✅ |
| 12 | `MEDIA` team (lead + 6 modules) | MEDIA | **DONE — verified** |
| — | **MEDIA TEAM COMPLETE — 6/6 module agents READY** | MEDIA | ✅ |
| 13 | `SEARCH` team (lead + 3 modules) | SEARCH | **DONE — verified** |
| — | **SEARCH TEAM COMPLETE — 3/3 module agents READY** | SEARCH | ✅ |
| 14 | `APPSHELL` team (lead + 4 modules) — **the final team** | APPSHELL | **DONE — awaiting verification** |
| — | **APPSHELL TEAM COMPLETE — 4/4 module agents READY** | APPSHELL | ✅ |
| — | **🎉 AGENT HIERARCHY COMPLETE — 7 teams, 7 leads, 38 agents** | ALL | ✅ |
| 15 | `BUG_NOTES.md` — consolidated known-issue register + generator | PM | **DONE — awaiting verification** |


## How to log a task (PM reference)

1. On receiving a request, append a row with a new `T-NNN` id and the **current date & time (IST)**,
   status `OPEN`.
2. Fill `Team` and `Module agent(s)` once routing is decided; status -> `IN PROGRESS`.
3. When the module agent reports completion, set status `DONE`, add the closing timestamp and the
   list of files touched.
4. If a request is rejected or deferred, use status `BLOCKED` / `DEFERRED` and add the reason in the
   Request cell.

### Status values

`OPEN` · `IN PROGRESS` · `BLOCKED` · `DEFERRED` · `DONE`
