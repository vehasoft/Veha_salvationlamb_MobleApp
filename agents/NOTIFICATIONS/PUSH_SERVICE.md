# PUSH_SERVICE Module Agent

> Team: **NOTIFICATIONS** · Reports to: `agents/NOTIFICATIONS/NOTIFICATIONS_LEAD.md`
> Scope: this agent knows **every detail** of the FCM receive path — service, token rotation,
> tray notification, deep-link intents. It may only edit the files under **Owned files**.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name (user-facing) | **none — this module has no UI of its own.** It renders into the Android **system notification tray** and launches other screens |
| Class | `com.veha.service.NotificationService` (47 LOC, Java, extends `FirebaseMessagingService`) + `com.veha.service.NotificationHelper` (82 LOC, Java, static helper) |
| Layout | none |
| Manifest entry | `<service android:name="com.veha.service.NotificationService" android:exported="false">` with `<intent-filter><action android:name="com.google.firebase.MESSAGING_EVENT" /></intent-filter>` |
| Entered from | the OS / Firebase — `onMessageReceived` on a push, `onNewToken` on token rotation. **Never** called from app code |
| Exits to | `ViewPostActivity` (FEED) · `ViewProfileActivity`, `ApproveRequestActivity` (PROFILE) · or **nowhere** when the type is unknown or a gate denies (`NP3`) |

> **This is the only component in the app that runs with no Activity alive.** `Util.*` statics are
> therefore frequently empty here — which is the root of `NP1` and `NP2` below.

## 2. UI inventory

No layout. The notification posted to the tray is built in code:

| Element | Value | Notes |
|---|---|---|
| Channel | `Util.CHANNEL_ID` = `"VEHA"` | **this module never creates the channel** — see `NP5` |
| Small icon | `R.mipmap.fav_icon_round` | |
| Accent colour | `context.getColor(R.color.primary_blue)` | |
| Title | `message.getNotification().getTitle()` | server-supplied |
| Body | `message.getNotification().getBody()` | server-supplied |
| Large icon | — | `setLargeIcon` is **commented out**, along with the `BitmapFactory` decode above it |
| Priority | `NotificationCompat.PRIORITY_DEFAULT` | |
| Flags | `Notification.FLAG_AUTO_CANCEL` | |
| Notification id | **constant `1`** | every push replaces the previous one (`NP4` / `N-9`) |

## 3. Actions / event handlers

| Trigger | Behaviour |
|---|---|
| `onMessageReceived(RemoteMessage)` | `super.onMessageReceived` → **`if (message.getNotification() != null)`** → 3 `Log.e` calls dumping title, body and the whole data map → a no-op bare `message.getData();` statement → `NotificationHelper.displayNotification(appContext, title, text, data)`. **A data-only push (no `notification` block) is silently dropped** (`NP6`) |
| `onNewToken(String)` | `super.onNewToken` → `updateToken(token)` |
| `updateToken(String)` | builds `UserPreferences(this)`, builds a `JsonObject` with `userID` = `Util.userId`, `token` = new token, `oldToken` = `String.valueOf(userPreferences.getFcmToken())` … and then **does nothing with it**. No Retrofit call; `userPreferences.savefcmToken(token)` is **commented out** (`NP1` / `N-4`) |
| Tray notification tapped | the `PendingIntent` built in `displayNotification` fires, or **nothing happens** if it is `null` (`NP3`) |

### 3a. `NotificationHelper.displayNotification` routing

`PendingIntent pendingIntent = null;` then a 4-branch `if/else if` on `data.get("type")`:

| `type` | Gate | Destination | Extras | Flags |
|---|---|---|---|---|
| `post` | `POST`/`Read` | `ViewPostActivity` | `postId` = `data.get("id")`, `type` | `NEW_TASK \| CLEAR_TASK` |
| `user` | **NONE** (`NP2`) | `ViewProfileActivity` | `userId` = `data.get("id")` | `NEW_TASK \| CLEAR_TASK` |
| `announcement` | `ANNOUNCEMENT`/`Read` | `ViewPostActivity` | `type`, `postId` = `data.get("id")` | `NEW_TASK \| CLEAR_TASK` |
| `warrior` | `USER`/`Edit` | `ApproveRequestActivity` | `userId` = `data.get("id")` | `NEW_TASK \| CLEAR_TASK` |
| `file`, `event` | — | **not handled at all** — the in-app adapter routes these to `PdfActivity2` / `WebViewActivity`, the tray does not (`NP7`) |
| unknown / missing | — | `pendingIntent` stays `null` → the notification posts but is **inert on tap** (`NP3`) |

All `PendingIntent.getActivity(context, 0, intent, FLAG_IMMUTABLE)` use **request code `0`**, so
each new intent **replaces** the previous one (`NP8`).

Finally: `NotificationManagerCompat.from(context)`, then
`if (checkSelfPermission(POST_NOTIFICATIONS) == PERMISSION_GRANTED) notify(1, notification)` —
when the runtime permission is absent the notification is **silently discarded** (`NP9`).

## 4. Validation rules

| Field | Rule | Failure message |
|---|---|---|
| `data.get("type")` | compared with `Objects.equals` against 4 `NotificationType` values | none — unmatched types fall through silently |
| `data.get("id")` | `.toString()` on the raw map value | **no null check** — a push without `id` throws `NullPointerException` inside the service (`NP10`) |

## 5. API contracts

### `updateToken(String token)` — **the call that never happens**

| Item | Value |
|---|---|
| Retrofit method | **none** |
| HTTP | intended: `PUT api/v1/users/token/update` (the endpoint exists in `RetrofitAPI.putToken`) |
| Base URL source | n/a |
| Headers | n/a |
| Request body | built but discarded: `{ "userID": Util.userId, "token": <new>, "oldToken": <Flow.toString()> }` |
| Success (200) | n/a |
| Error | n/a |
| Failure (`onFailure`) | n/a — the whole body is wrapped in `try/catch` that only logs `"error while updating token"` |

> **Consequence:** FCM rotates tokens on reinstall, app-data clear, restore, and periodically.
> When that happens the server keeps the stale token and the user **silently stops receiving
> push** until they log in again (`LoginActivity` is the only place that ever calls `putToken`).

> Note also that `oldToken` uses `String.valueOf(userPreferences.getFcmToken())` — `getFcmToken()`
> returns a **`Flow<String>`**, so this stringifies the *Flow object*, not the token (`NP11`).

## 6. Storage read / written

| Key | Type | Operation | Value |
|---|---|---|---|
| `fcmToken` | `Flow<String>` | **read incorrectly** | `String.valueOf(flow)` → something like `kotlinx.coroutines.flow.FlowKt…@1a2b3c`, never the token (`NP11`) |
| `fcmToken` | `String` | **never written** | `savefcmToken(token)` is commented out (`NP1`) |
| — | — | — | no other DataStore access; `token` / `userId` are **not** read here |

## 7. Global state touched

| Field | Operation | Value |
|---|---|---|
| `Util.userId` | read | `userID` in the unsent payload — **usually `null`** here, since the service can run with no Activity alive |
| `Util.permissionMap` | read via `Util.hasPermission` | 3 of the 4 tray branches |
| `Util.CHANNEL_ID` | read | `NotificationCompat.Builder(context, …)` |
| `Util.CHANNEL_NAME` / `CHANNEL_DESC` | **not used here** | they are only read by `LoginActivity`, which creates the channel |

> **`Util` is a cold statics bag in this context.** After a process start caused by a push, every
> `Util` field is at its default: `userId == null`, `user == null`, and — since T-025 made
> `hasPermission` **fail closed** — `permissionMap` is empty, so **all three gated branches deny**
> and `pendingIntent` stays `null`. See `NP2` and the interaction note in §12.

## 8. User-facing messages (exact strings)

| Trigger | Message | Mechanism |
|---|---|---|
| Any push with a `notification` block | server-supplied `title` + `body` | system notification tray |
| Gate denied / unknown type | notification still posts, but **tapping does nothing** | — (`NP3`) |
| `POST_NOTIFICATIONS` not granted | **nothing at all** | silently dropped (`NP9`) |
| Any internal failure | nothing — `Log.e("error while updating token", …)` only | logcat |

## 9. Navigation map

| From | Trigger | To | Extras | finish()? |
|---|---|---|---|---|
| tray | `type=post` + `POST`/`Read` | `ViewPostActivity` | `postId`, `type` | n/a — `CLEAR_TASK` wipes the stack |
| tray | `type=user` (**no gate**) | `ViewProfileActivity` | `userId` | n/a — `CLEAR_TASK` |
| tray | `type=announcement` + `ANNOUNCEMENT`/`Read` | `ViewPostActivity` | `postId`, `type` | n/a — `CLEAR_TASK` |
| tray | `type=warrior` + `USER`/`Edit` | `ApproveRequestActivity` | `userId` | n/a — `CLEAR_TASK` |
| tray | gate denied, unknown type, or `file`/`event` | **nowhere** | — | — |

`FLAG_ACTIVITY_NEW_TASK \| FLAG_ACTIVITY_CLEAR_TASK` **destroys the existing back stack**, so a
user mid-way through composing a post loses it by tapping a notification (`NP12`). Note the
contrast with `SplashScreenActivity`'s cold-start deep link, which routes through the splash and
therefore *does* load the permission map first.

## 10. Lifecycle

| Callback | Behaviour |
|---|---|
| `onMessageReceived` | §3. Runs on a background thread owned by the FCM SDK |
| `onNewToken` | §3 — effectively a no-op (`NP1`) |
| `onCreate` / `onDestroy` | not overridden |
| `onDeletedMessages` | **not overridden** — FCM message overflow is not handled |

## 11. Dependencies

| Dependency | Owner team | Used for |
|---|---|---|
| `firebase-messaging:23.4.1` (via `firebase-bom:32.8.0`) | PLATFORM/BUILD_CONFIG | `FirebaseMessagingService`, `RemoteMessage` |
| `app/google-services.json` | PLATFORM/BUILD_CONFIG | Firebase project `salvationlamb-a9717` (`G14`) |
| `Util.CHANNEL_ID`, `Util.userId`, `Util.hasPermission` | PLATFORM/COMMONS | channel, payload, gates |
| `UserPreferences.fcmToken` | PLATFORM/STORAGE | intended token bookkeeping |
| `NotificationType`, `PermissionType`, `Permission` enums | PLATFORM/DATA_MODELS | routing + gates |
| `ViewPostActivity` | FEED | `post` / `announcement` destination |
| `ViewProfileActivity`, `ApproveRequestActivity` | PROFILE | `user` / `warrior` destination |
| `LoginActivity` | **AUTH** | **creates the notification channel** and performs the only real `putToken` call |
| `POST_NOTIFICATIONS` permission | PLATFORM/BUILD_CONFIG | declared in the manifest; requested in `MainActivity` |

## 12. Known issues / tech debt

| # | Issue | Severity | Suggested fix |
|---|---|---|---|
| NP1 | **`updateToken` never sends anything** (`N-4`). It builds the payload, then returns; `savefcmToken` is commented out. FCM rotates tokens on reinstall, data-clear, restore and periodically — after any rotation the server holds a dead token and the user **silently stops receiving push** until the next login | **Critical** | call `RetrofitAPI.putToken` and persist via `savefcmToken`; the endpoint already exists |
| NP13 | **Post-T-025 interaction:** `hasPermission` now fails closed, and in a push-started process `permissionMap` is always empty — so `post`, `announcement` and `warrior` notifications are now **always inert on tap**, while the ungated `user` branch still works. The tray's behaviour is the inverse of what the gates intend | **Critical** | route every tray tap through `SplashScreenActivity`, which loads the map before routing |
| NP2 | **`type=user` has no permission gate in the tray** (`N-2`), while the in-app adapter gates it with `USER`/`Read`. The tray is therefore a **bypass of a gate the UI enforces** | **High** | add the gate, matching `NotificationListAdapter` |
| NP3 | A denied gate or an unknown `type` leaves `pendingIntent == null`; the notification still posts and **does nothing on tap** (`N-3`) | **High** | fall back to a splash / `MainActivity` intent so a tap always does something |
| NP5 | This module **never creates the notification channel** (`N-12`). `Util.CHANNEL_ID` is used by the builder, but `createNotificationChannel` lives in `LoginActivity.onCreate` (AUTH). On API 26+ a push arriving before any login is **dropped by the OS** | **High** | create the channel in an `Application` subclass or in the service |
| NP6 | `onMessageReceived` only acts `if (message.getNotification() != null)` — **data-only pushes are silently dropped**, and data-only is the usual way to deliver a background deep link | **High** | handle data-only messages |
| NP14 | The routing here is **copy 2 of 3** (`N-1`); `NotificationListAdapter` has 6 branches, `SplashScreenActivity` has its own variant. They have already diverged on gates (`NP2`) and coverage (`NP7`) | **High** | extract one `NotificationRouter` used by all three |
| NP7 | `file` and `event` types are **not handled in the tray** at all, though the in-app list routes both | Medium | add both branches, or document the asymmetry |
| NP10 | `data.get("id").toString()` with **no null check** — a push missing `id` throws inside the service | Medium | null-guard before `toString()` |
| NP11 | `String.valueOf(userPreferences.getFcmToken())` stringifies a **`Flow` object**, not the token, so `oldToken` would be garbage even if the call were made | Medium | collect the Flow (`first()`) in a coroutine |
| NP4 | `notify(1, notification)` uses a **constant id** (`N-9`) — each push overwrites the previous, so only one notification is ever visible | Medium | use a unique id derived from the server id |
| NP8 | All `PendingIntent.getActivity(context, 0, …)` use **request code 0**, so intents replace each other even with distinct extras | Medium | pass a unique request code |
| NP9 | When `POST_NOTIFICATIONS` is not granted the notification is **silently discarded** with no log and no fallback | Medium | log it; surface an in-app prompt |
| NP12 | `FLAG_ACTIVITY_NEW_TASK \| FLAG_ACTIVITY_CLEAR_TASK` **wipes the back stack**, discarding unsaved work such as a half-composed post | Medium | drop `CLEAR_TASK`, or use `TaskStackBuilder` |
| NP15 | `onMessageReceived` logs the **title, body and entire data map** at `Log.e` on every push — notification content in logcat on release builds | Medium | remove, or guard with `BuildConfig.DEBUG` |
| NP16 | `onDeletedMessages` is not overridden, so an FCM overflow is never reconciled with the server | Low | override and refetch the list |
| NP17 | Dead code: a bare `message.getData();` statement, a commented-out `CHANNEL_ID`/`NAME`/`DESC` block duplicating `Util`, and a commented-out `setLargeIcon` + `BitmapFactory` decode | Low | delete |
| NP18 | `displayNotification(Context, String, String, Map)` takes a **raw `Map`** | Low | parameterise as `Map<String, String>` |
| NP19 | Log tags are ad-hoc (`"notification"`, `"error while updating token"`) rather than `<Class>.<method>` | Low | follow the convention |

## 13. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/service/NotificationService.java` | **exclusive** |
| `app/src/main/java/com/veha/service/NotificationHelper.java` | **exclusive** |
| the `<service android:name="com.veha.service.NotificationService">` block in `AndroidManifest.xml` | shared with PLATFORM/BUILD_CONFIG — notify lead before editing |

**Not owned (escalate):** `Util.java` (channel constants, `hasPermission`), `UserPreferences.kt`,
`RetrofitAPI.putToken`, the `DataModels.kt` enums (all PLATFORM); the `createNotificationChannel`
block and the `putToken` call in `LoginActivity.kt` (**AUTH**); every destination activity;
`app/google-services.json` (PLATFORM/BUILD_CONFIG).

## 14. How to make common changes

**Make token rotation work (`NP1` — highest value):** in `updateToken`, call
`Util.getRetrofit().putToken("Bearer <token>", data)` and, on success, `savefcmToken(token)`
inside a coroutine. This needs the auth token, so the `token` DataStore key must be read here —
coordinate with PLATFORM/STORAGE. Note `Util.userId` is usually `null` in this process, so the
user id must also come from DataStore, not from `Util`.

**Add a notification type:** this is **copy 2 of 3** (`NP14` / `N-1`). Change
`NotificationListAdapter.onBindViewHolder` and `SplashScreenActivity.getMyDetails` in the same
change set, or the new type will work in one surface only. PM-coordinated.

**Fix the dead-tap problem (`NP3`/`NP13`):** rather than adding gates here, point every tray
intent at `SplashScreenActivity` with the extras it already understands — the splash loads the
permission map **before** routing (T-025), so the gate is then evaluated against real data. That
single change also removes `NP2` and `NP13`.

**Move channel creation (`NP5`):** `createNotificationChannel` belongs in an `Application`
subclass; the app has none today, so adding one is a PLATFORM/BUILD_CONFIG manifest change.

**Never log notification content (`NP15`):** remove the three `Log.e` calls in
`onMessageReceived` before any release build.

## 15. Change log

| Change | Detail |
|---|---|
| Created (T-026, 2026-10-08) | Documented the whole FCM receive path from `NotificationService.java` (47) and `NotificationHelper.java` (82) plus the manifest `<service>` block: `onMessageReceived`, `onNewToken`, the 4-branch tray routing table with gates and extras, the `PendingIntent` flags, and the builder configuration. **19 issues** recorded. Headline findings: **`updateToken` builds a payload and never sends it** (`NP1`), so push silently dies after any token rotation; **`type=user` is ungated in the tray** but gated in-app (`NP2`); and a newly identified **post-T-025 interaction** (`NP13`) — now that `hasPermission` fails closed, a push-started process has an empty permission map, so the three *gated* branches are always inert while the one *ungated* branch still works, exactly inverting the intended behaviour. Also recorded the missing channel creation (`NP5`, owned by AUTH's `LoginActivity`), data-only pushes being dropped (`NP6`), the constant notification id (`NP4`), and `file`/`event` being unroutable from the tray (`NP7`). |


