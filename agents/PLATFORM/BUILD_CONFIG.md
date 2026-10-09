# BUILD_CONFIG Module Agent

> Team: **PLATFORM** · Reports to: `agents/PLATFORM/PLATFORM_LEAD.md`
> This agent knows **every detail** of how the app is built, configured, permissioned and shipped:
> both gradle files, settings, properties, the manifest, proguard and the network-security config.
> It may only edit the files listed in §8 **Owned files**.

---

## ⚠ Blast-radius warning

This module controls **whether the app compiles, installs and launches at all**. A bad dependency
bump, a removed permission or a deleted manifest entry breaks the entire product, not one screen.

It also governs **secrets**: `app/Key/key.jks` and `app/Key/private_key.pepk` are committed in the
tree **and in remote git history**. **Never open, print, move or modify them.** The exposure is an
**accepted risk** as of T-020 (2026-10-08) — see §4.

Every change needs PM sign-off.

---

## 1. Build identity

| Item | Value |
|---|---|
| Root project | `Fb Project` (`settings.gradle`) |
| Modules | **`:app` only** — single-module app |
| `applicationId` / `namespace` | `com.veha.activity` |
| Version | `versionCode 6`, `versionName "1.1"` |
| `compileSdk` / `targetSdk` | 33 / 33 |
| `minSdk` | **23** (Android 6.0) |
| AGP | `com.android.tools.build:gradle:7.2.0` |
| Kotlin | `org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10` |
| Java / JVM target | `VERSION_1_8` / `1.8` |
| Plugins | `com.android.application`, `kotlin-android`, **`kotlin-android-extensions`** |
| Build features | `viewBinding true` (enabled but barely used — synthetics dominate) |
| Build types | `release` only, with **`minifyEnabled false`** |
| Packaging | excludes `META-INF/DEPENDENCIES` |
| Test runner | `androidx.test.runner.AndroidJUnitRunner` |

### `gradle.properties`

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.enableJetifier=true        # needed because of the legacy support lib
kotlin.code.style=official
```

### `settings.gradle`

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral(); jcenter() }   // jcenter is dead (B-1)
}
rootProject.name = "Fb Project"
include ':app'
```

---

## 2. Dependencies (`app/build.gradle`)

| Group | Artifacts |
|---|---|
| AndroidX core | `core-ktx:1.3.2`, `appcompat:1.2.0`, `constraintlayout:2.0.4`, `legacy-support-v4:1.0.0` |
| **Legacy support (conflict)** | `com.android.support:appcompat-v7:28.0.0`, `com.android.support.constraint:constraint-layout:2.0.4` |
| Material | `com.google.android.material:material:1.3.0` |
| Navigation | `navigation-fragment-ktx:2.3.5`, `navigation-ui-ktx:2.3.5` |
| Lifecycle | `lifecycle-runtime-ktx:2.3.0-alpha03`, `lifecycle-livedata-ktx:2.2.0` |
| Coroutines | `kotlinx-coroutines-android:1.6.1` |
| Network | `retrofit:2.9.0`, `converter-gson:2.5.0`, `okhttp` (**no version**) |
| Storage | `androidx.datastore:datastore-preferences:1.0.0-alpha01` |
| Images | `picasso:2.5.2`, `android-image-cropper:2.8.0` (`api`), `android-gif-drawable:1.2.17` |
| Media | `androidyoutubeplayer:core:12.0.0`, `cronet-embedded:76.3809.111` |
| Documents | `android-pdf-viewer:3.2.0-beta.1` |
| UI extras | `richeditor-android:2.0.0`, `spots-dialog:1.1@aar` |
| Firebase | `firebase-crashlytics-buildtools:2.5.2` (**build tool only — Crashlytics is not actually wired up**) |
| Test | `junit:4.+`, `androidx.test.ext:junit:1.1.2`, `espresso-core:3.3.0` |

**Three jcenter-era artifacts** (`spots-dialog`, `android-image-cropper`, `richeditor-android`) are
the practical reason `jcenter()` is still listed (B-1).

---

## 3. `AndroidManifest.xml`

### Permissions (7)

| Permission | Note |
|---|---|
| `INTERNET` | required by everything |
| `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` | legacy storage; no `maxSdkVersion` (B-2) |
| `READ_MEDIA_IMAGES` / `READ_MEDIA_AUDIO` / `READ_MEDIA_VIDEO` | Android 13 replacements |
| `CAMERA` | declared with **`android:required="true"` and `android:requiredFeature="true"`** — invalid attributes on `<uses-permission>` (B-3) |

### `<application>` flags

`allowBackup="true"` (B-4) · `icon/roundIcon=@mipmap/fav_icon` · `label=@string/app_name` ·
`networkSecurityConfig=@xml/network_security_config` · `requestLegacyExternalStorage="true"` ·
`supportsRtl="true"` · `theme=@style/AppTheme` · `largeHeap="true"` (B-5) ·
**`usesCleartextTraffic="true"`** (B-6)

### Activities (21 declared)

| Exported | Activities |
|---|---|
| `exported="true"` **with** launcher intent-filter | `SplashhScreenActivity` — the only legitimate entry point |
| `exported="true"` **without** any intent-filter | `EditProfileActivity`, `RegisterActivity`, `MainActivity` — **unnecessarily reachable by other apps** (B-7) |
| `exported="false"` | the other 16 |
| Third-party | `com.theartofdev.edmodo.cropper.CropImageActivity` with `theme=@style/Base.Theme.AppCompat` |

`MainActivity` also declares
`configChanges="orientation|screenSize|screenLayout|keyboardHidden|uiMode"`, and `SearchActivity`
sets `windowSoftInputMode="stateVisible"` — the only two activities with such config.

### `res/xml/network_security_config.xml`

```xml
<domain-config cleartextTrafficPermitted="true">
    <domain includeSubdomains="true">salvationlamb-env.eba-smicznsb.ap-south-1.elasticbeanstalk.com</domain>
</domain-config>
```

**The whitelisted host is stale** — it matches neither the live `server.salvationlamb.com` nor
`APIUtil`'s `salvation-env.eba-nhpvydpr.us-east-1...`. Combined with the global
`usesCleartextTraffic="true"`, the config achieves nothing (B-6).

### `proguard-rules.pro`

**Effectively empty** (only the default comment block), and `minifyEnabled false` means it is never
applied (B-8).

---

## 4. Secrets (do not touch)

| File | Note |
|---|---|
| `app/Key/key.jks` | release signing keystore, **committed** |
| `app/Key/private_key.pepk` | Play App Signing export key, **committed** |

No `signingConfigs` block exists in `app/build.gradle`, so these are used manually (or by an
external process). **Never open, print, move, rename or modify them.**

> **T-020 (2026-10-08) — customer decision: the key exposure is an ACCEPTED RISK.** Both files
> have been in remote git history since `3d34164` (2023-08-29) and sit on every branch. There will
> be **no history rewrite and no key rotation**. Consequence for the React Native migration: the
> new app must ship under the same `applicationId` (`com.veha.activity`) to reach existing users
> as an update, so it must be signed with **this same keystore**. Treat it as the permanent
> signing identity, keep it out of any new public surface, and do not purge it (`B-9`).

---

## 5. Build commands

| Task | Command |
|---|---|
| Debug APK | `./gradlew assembleDebug` |
| Release APK | `./gradlew assembleRelease` |
| Install debug | `./gradlew installDebug` |
| Clean | `./gradlew clean` |
| Unit tests | `./gradlew test` — **there are none** |
| Instrumented tests | `./gradlew connectedAndroidTest` — only the stock `ExampleInstrumentedTest.kt` |
| Lint | `./gradlew lint` |

> The stock instrumented test lives under `app/src/androidTest/java/com/example/activity/`, i.e.
> package **`com.example.activity`**, not `com.veha.activity` (B-10).

---

## 6. Dependencies on this module

Everything. Specifically:

| Consumer | Needs |
|---|---|
| NETWORK | retrofit, converter-gson, okhttp, `network_security_config.xml` |
| STORAGE | datastore-preferences, lifecycle-livedata-ktx |
| DATA_MODELS | gson (transitively) |
| COMMONS | appcompat, lifecycle |
| Every screen | `kotlin-android-extensions` (synthetics), material, constraintlayout, its `<activity>` entry |
| MEDIA | pdf-viewer, youtubeplayer, gif-drawable, cronet, media permissions |
| PROFILE | image-cropper + its `CropImageActivity` manifest entry, camera/storage permissions |

---

## 7. Known issues / tech debt

| # | Issue | Severity | Suggested fix |
|---|---|---|---|
| B-1 | **`jcenter()` is still a repository** (shut down, read-only); three artifacts (`spots-dialog`, `android-image-cropper`, `richeditor-android`) still resolve from it | **High** | migrate to maintained forks on mavenCentral, or vendor them |
| B-11 | **`kotlin-android-extensions` is deprecated** and removed in Kotlin 1.8+; the whole UI layer depends on synthetics (G2) | **High** | blocks any Kotlin upgrade; a ViewBinding migration is a cross-team project |
| B-12 | **`com.android.support:appcompat-v7:28.0.0` alongside AndroidX**, papered over by Jetifier (G3) | **High** | remove the two `com.android.support` lines and verify |
| B-6 | `usesCleartextTraffic="true"` globally, plus a `network_security_config` naming a **stale** host (G8) | **High** (security) | set `false` and whitelist only the real host |
| B-9 | ~~Signing keystore and Play export key **committed** to the tree~~ — **CLOSED as WON'T FIX 2026-10-08 (T-020)**: customer accepted the risk; no history rewrite, no rotation. ⚠️ Treat `app/Key/key.jks` as the **permanent** signing identity — the React Native app must reuse the same `applicationId` (`com.veha.activity`) and therefore this same key to ship as an update | By design | do **not** rotate or purge without a PM decision reversing T-020 |
| B-7 | `EditProfileActivity`, `RegisterActivity` and `MainActivity` are `exported="true"` with **no intent-filter** | Medium (security) | set `exported="false"` |
| B-8 | `minifyEnabled false` and an empty `proguard-rules.pro` | Medium | enable R8 for release with keep rules for Gson models + Retrofit |
| B-13 | No `debug` build type, no flavours, no `buildConfigField` — the base URL is hard-coded in `Util.java` | Medium | add `buildConfigField` for the URL (with NETWORK) |
| B-2 | `READ/WRITE_EXTERNAL_STORAGE` without `android:maxSdkVersion`, though Android 13 media permissions are present | Medium | add `maxSdkVersion="32"` |
| B-3 | `CAMERA` declares `android:required` / `android:requiredFeature`, which are **not valid** on `<uses-permission>`; no `<uses-feature>` is declared | Medium | remove the attributes; add a proper `<uses-feature>` |
| B-4 | `allowBackup="true"` with no backup rules — the DataStore **auth token** is included in cloud backups | Medium (security) | set `false` or exclude the datastore file |
| B-14 | `okhttp` declared **without a version**; `lifecycle-runtime-ktx:2.3.0-alpha03` is an alpha; `android-pdf-viewer` is a beta | Medium | pin and stabilise |
| B-16 | `cronet-embedded:76.3809.111` (2019) adds ~20 MB and appears unused | Medium | verify usage and remove |
| B-17 | `firebase-crashlytics-buildtools` is present but Crashlytics is **not configured** — no crash reporting at all | Medium | wire it up or drop it |
| B-20 | No CI, no lint baseline, no dependency-update checks | Medium | add once the agent hierarchy is in place |
| B-15 | `converter-gson:2.5.0` with `retrofit:2.9.0` | Low | align versions |
| B-18 | `largeHeap="true"` masks memory problems | Low | profile and remove |
| B-5 | `versionCode` / `versionName` are hard-coded with no automation | Low | derive from CI/git |
| B-10 | The stock instrumented test sits in package `com.example.activity` | Low | move to `com.veha.activity` |
| B-19 | `viewBinding true` is enabled but almost unused, so both mechanisms ship | Low | pick one (see B-11) |

---

## 8. Owned files

| File | Ownership |
|---|---|
| `build.gradle` (root) | **exclusive** |
| `app/build.gradle` | **exclusive** |
| `settings.gradle` | **exclusive** |
| `gradle.properties` | **exclusive** |
| `gradle/wrapper/*` | **exclusive** |
| `app/proguard-rules.pro` | **exclusive** |
| `app/src/main/res/xml/network_security_config.xml` | **exclusive** |
| `app/src/main/AndroidManifest.xml` | **shared** — the `<application>` block, permissions and build config are this agent's; each `<activity>` entry is **co-owned with that screen's module agent** |
| `app/Key/*` | **read-only, never touch** |

**Not owned (escalate):** every source file and resource.

---

## 9. How to make common changes

**Add a dependency:** add the line to `app/build.gradle` with an explicit version, state in the
report **which team asked and why**, and confirm it resolves from `google()` or `mavenCentral()` —
**do not add anything that needs `jcenter()`**. PM sign-off required.

**Add a permission:** add `<uses-permission>` and name the requesting feature. For dangerous
permissions, confirm the **runtime request** is implemented in the owning screen — the manifest
entry alone is not enough on API 23+ (`minSdk` is 23).

**Register a new activity:** add an `<activity>` block with **`exported="false"`** unless it needs
an intent-filter. Co-sign with the screen's module agent.

**Bump `targetSdk`/`compileSdk`:** a project-wide change — behaviour changes affect storage,
permissions and background work. A PM-approved task with a test plan, not a drive-by.

**Change the base URL per build type (B-13):** add `buildConfigField "String", "BASE_URL", ...` and
have NETWORK read it in `Util.getRetrofit()`. Two-agent change.

**Enable R8 (B-8):** set `minifyEnabled true` and add keep rules for the 12 Gson data classes
(reflection-based) and the Retrofit interface, or JSON parsing breaks **in release only**.
Coordinate with DATA_MODELS and NETWORK.

**Remove the legacy support lib (B-12):** delete the two `com.android.support` lines, then do a full
build — some screens may import from the old namespace. `android.enableJetifier` may then be
removable too.

---

## 10. Change log

| Change | Detail |
|---|---|
| Created | Initial BUILD_CONFIG module agent documented from `build.gradle` (root + app), `settings.gradle`, `gradle.properties`, `AndroidManifest.xml` (7 permissions, 21 activities), `proguard-rules.pro` and `network_security_config.xml`: full build identity, the dependency inventory by group, the manifest breakdown with export status, the committed-secrets warning, build commands, and 20 known issues. |


