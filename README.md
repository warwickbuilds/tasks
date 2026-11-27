<p align="center">
  <a href="https://github.com/warwickbuilds/tasks/releases/latest"><img src="https://img.shields.io/github/v/release/warwickbuilds/tasks?style=for-the-badge&label=Latest&logo=github" alt="Latest release" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/github/license/warwickbuilds/tasks?style=for-the-badge&label=License" alt="MIT License" /></a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/tasks-100%25%20on%20your%20phone-2F6B5A?style=flat-square&logo=android&logoColor=white" alt="Tasks stay on your phone" />
  <img src="https://img.shields.io/badge/account-not%20needed-2F6B5A?style=flat-square" alt="No account needed" />
  <img src="https://img.shields.io/badge/tracking%20%26%20analytics-zero-2F6B5A?style=flat-square" alt="Zero tracking" />
  <img src="https://img.shields.io/badge/ads-none-2F6B5A?style=flat-square" alt="No ads" />
  <a href="https://github.com/warwickbuilds/tasks/actions/workflows/ci.yml"><img src="https://img.shields.io/github/actions/workflow/status/warwickbuilds/tasks/ci.yml?branch=main&style=flat-square&label=CI&logo=githubactions&logoColor=white" alt="CI status" /></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android 8.0 and newer" />
  <img src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin and Jetpack Compose" />
</p>

# Tasks

**A deliberately simple, open-source to-do list for Android.** A task is a title, an optional Markdown description and an optional due date. Tags like **Today**, **Movies to watch** and **Books to read** group them, one tap switches between groups, and a home-screen widget shows what's on for today.

Everything stays on your phone: no account, no login, no cloud, no ads, no analytics. The only network request Tasks can ever make is checking GitHub Releases when you tap **Settings → Check for updates**.

## Contents

- [What it does](#what-it-does)
- [Install](#install)
- [Getting started](#getting-started)
- [Your data and privacy](#your-data-and-privacy)
- [For developers](#for-developers): [status](#status), [features in detail](#features), [changelog](#versions-and-changelog), [architecture](#architecture), [security and privacy](#security-and-privacy), [releases](#releases), [running it](#running-it), [maintenance](#maintenance), [future enhancements](#future-enhancements)
- [License](#license)

## What it does

- **Tasks, nothing else.** A title, plus an optional description in Markdown (headings, bold and italic, lists, checklists, links, code) and an optional due date shown as a quiet label.
- **Tags as groups.** Chips across the top: **All**, **Due**, **Today**, then your tags. Tap a chip or swipe sideways to switch. No counts, badges or summaries, just the list.
- **Tag as you type.** `#books Dune` adds "Dune" tagged Books to read. Unknown tags are created for you. Adding a task while you're in a tag gives it that tag.
- **Today.** Tasks tagged Today, plus anything due today or overdue. Long-press the Today chip to **Clear Today** for a fresh start.
- **Pin one task.** It sits at the top of every group it belongs to, with its description, and first on the widget.
- **Tick lists inside tasks.** `- [ ]` checklist items in a description can be ticked straight from the task (and from a pinned task's card).
- **Home-screen widget.** Your Today list: tick tasks off right there, tap one to open it, or tap **+** to add one to Today.
- **Backup to a file.** Export everything to a readable JSON file you keep, and import it on any phone.
- **Calm by default.** Soft sage green Material 3 theme, light and dark. Switch on **Match wallpaper colours** for Material You.

## Install

Tasks is installed straight from GitHub Releases (no Play Store account needed).

1. On your Android phone (8.0 or newer), open the [latest release](https://github.com/warwickbuilds/tasks/releases/latest) and download `tasks-<version>.apk`.
2. Open it. Android asks whether your browser (or Files app) may install unknown apps: tap **Settings**, turn on **Allow from this source**, go back and tap **Install**.
3. **Play Protect** may say the app is unrecognised because it isn't from the Play Store. Tap **More details → Install anyway**.
4. Open Tasks. There's no sign-up.

**Updating:** Tasks never checks the network by itself. Tap **Settings → Check for updates** whenever you like, or watch the repo's releases on GitHub. Installing a new APK over the old one keeps your tasks. Every release is signed with the same key; if Android ever says the app "conflicts with an existing package", **don't uninstall to get around it** (that deletes your tasks). Export a backup first.

Builds from pull requests are signed with a debug key and can't be installed over a release.

## Getting started

1. Tap **New task**, type a title and press Enter. Keep typing to add more. Add `#today` or `#books` to tag as you go.
2. Tap **Add details** (or tap a task, then the pencil) for the full editor: description with a Write/Preview toggle and a formatting toolbar, due date, pin and tags.
3. Long-press a task to pin it, change its tags or delete it.
4. Long-press and hold on your home screen, choose **Widgets → Tasks → Today**, and drop it where you like.
5. Go to **Settings → Export tasks** now and then, and keep the file somewhere safe.

## Your data and privacy

- **Nothing leaves your phone.** No servers, no accounts, no analytics, crash reporting, ads or tracking SDKs.
- **One network call, only when you ask.** **Settings → Check for updates** sends a single unauthenticated GET to `api.github.com` for this repo's latest release. It sends nothing about you or your tasks. A test fails the build if network code shows up anywhere else.
- **No Google cloud backup.** Android's automatic backup and device-to-device transfer are switched off, so your tasks never end up in someone else's cloud. Use **Export** instead.
- **Minimal permissions.** Internet, used only for that manual update check. The widget library also needs Android's standard "keep awake" and "run at startup" permissions so the widget keeps updating; they don't touch your data or the network.

The full [Privacy policy](PRIVACY.md) is short, because there's nothing to collect.

---

## For developers

### Status

Version **0.1.0**, the first build. Everything in phase 1 of the plan, plus `#tag` typing, Clear Today and swiping between groups from phase 2.

Built on GitHub Actions; it has not been run on a phone yet. Try the debug APK from a pull request's CI run (Actions → the run → **tasks-debug-apk**).

### Features

| Area | What works |
| --- | --- |
| Task list | Chip row (All, Due, Today, your tags, **+** to add a tag); swipe between groups; newest first (Due: soonest first); pinned card on top with its description; collapsed Done section with Delete done; tap to open, long-press for Pin, Tags and Delete; tick to complete with Undo; delete with Undo |
| Quick add | Bottom sheet, Enter adds and stays open; `#tag` parsing with a live preview of the tags it'll get; the current group's tag is added; **Add details** opens the full editor |
| Task view | Title, due date, tags, rendered Markdown with tickable checklists, Mark done, pin, edit, delete |
| Editor | Title (also takes `#tags`), Write/Preview, toolbar (heading, bold, italic, checklist, bullets, link, code), Enter continues lists, due date picker, pin, tag picker with new tags, discard check |
| Tags | Starter set: Today, Movies to watch, Books to read, TV shows, Projects. Add, rename, recolour (8 colours), reorder, delete. Today can't be renamed or deleted |
| Today | Tagged Today, or open and due today or earlier. Clear Today (long-press its chip) removes the tag from every task |
| Widget | Glance widget, resizable scrolling list of Today with the pinned task first; tick to complete; tap to open; **+** adds to Today. Refreshes on every change and hourly (so the date rolls over) |
| Settings | Tags, Match wallpaper colours (Android 12+), Export and Import JSON, manual update check, About with links |

**Defaults picked for open questions** (easy to change):

- Tasks due today or overdue appear in **Today** automatically, without the tag. Done ones drop out.
- The widget always shows the **pinned task first**, even if it isn't tagged Today.
- There's no separate pinned-task widget in this version.
- Due dates are a label only, no reminder notifications.
- Licence: MIT, the same as Cards.

### Versions and changelog

| Version | Date | Pull request | Changes |
| --- | --- | --- | --- |
| 0.1.0 | 2026-11-27 | First PR | First version: tasks, Markdown descriptions, due dates, pin, tags and groups, `#tag` quick add, Today widget, JSON backup, manual update check, About |

**Dependencies**

| What | Version |
| --- | --- |
| Android Gradle plugin | 8.13.0 |
| Gradle | 8.14.3 |
| Kotlin, Compose compiler, serialization | 2.2.21 |
| KSP | 2.2.21-2.0.4 |
| Compose BOM (Material 3) | 2025.06.00 |
| Room | 2.7.2 |
| Glance (app widget, Material 3) | 1.1.1 |
| Navigation Compose | 2.9.0 |
| Lifecycle | 2.9.1 |
| Activity Compose | 1.10.1 |
| Core KTX | 1.16.0 |
| commonmark-java (+ task list, strikethrough) | 0.24.0 |
| kotlinx-serialization-json | 1.9.0 |
| Robolectric (tests) | 4.16 |

`compileSdk` and `targetSdk` 36, `minSdk` 26 (Android 8.0).

### Architecture

Two Gradle modules:

- **`core/`**: plain Kotlin, no Android. Everything with rules in it, so it's fast to test anywhere with a JDK:
  - `Model.kt`: `Task`, `Tag`, the starter tags (`today` has a fixed id).
  - `Grouping.kt`: the groups (All, Due, one per tag), which tasks belong where, sort order, and what the widget lists.
  - `QuickAdd.kt`: reads `#tags` out of a typed title.
  - `Markdown.kt`: commonmark-java turns a description into simple blocks (headings, paragraphs, list and checklist items with their source line, quotes, code) that Compose draws; also checklist ticking and one-line previews.
  - `MarkdownEditing.kt`: the editor toolbar and list continuation, as pure text functions.
  - `DueLabel.kt`: "Today", "Tomorrow", "Sat", "28 Nov".
  - `Backup.kt`: the JSON backup format and the merge used by import.
  - `Updates.kt`: version parsing and GitHub release checks.
- **`app/`**: the Android app.
  - `data/`: Room database `tasks.db` with three tables: `task` (id, title, description, dueDate, pinned, completedAt, sortOrder, createdAt, updatedAt), `tag` (id, name, color, sortOrder) and `task_tag` (taskId, tagId). `TaskRepository` is the only way in, for the app and the widget, and refreshes the widget after every write.
  - `ui/`: Jetpack Compose screens: `list/` (main list and quick add), `task/` (view and editor), `tags/`, `settings/`, shared `components/` (Markdown view, dialogs) and `theme/` (Calm colours and tag colours). Navigation Compose routes: `list`, `task/{id}`, `edit?id&title&tag`, `tags`, `settings`.
  - `widget/`: the Glance Today widget and its tick-to-complete action.
  - `lib/UpdateChecker.kt`: the one network call.
  - `TasksApplication` holds the database, repository, settings (SharedPreferences: last group, wallpaper colours) and an app-wide coroutine scope for writes.

Only one task can be pinned: pinning one unpins the rest, and finishing a task unpins it.

### Security and privacy

- Stored on the phone: the Room database (`tasks.db`) and a small preferences file. Nothing else.
- `android:allowBackup="false"` and `data_extraction_rules.xml` exclude everything from Google cloud backup and device transfer.
- Exports are plain JSON written only to a file you pick with Android's file picker (Storage Access Framework). No storage permission.
- Permissions: `INTERNET`, plus `WAKE_LOCK` and `RECEIVE_BOOT_COMPLETED` that Glance's WorkManager adds for widget updates. `ACCESS_NETWORK_STATE` and `FOREGROUND_SERVICE`, which WorkManager also adds, are removed in the manifest. CI fails if the built APK asks for anything else.
- Network: `INTERNET` is used only by `lib/UpdateChecker.kt` (a GET to `api.github.com/repos/warwickbuilds/tasks/releases/latest`, 10 s timeout). Download links are only offered if they're HTTPS on `github.com` under this repo's releases. `NetworkGuardTest` fails if network code appears in any other file, if the checker talks to any other host, or if the manifest asks for another permission.
- Links in descriptions open in your browser only when you tap them.
- No `dependenciesInfo` blob in the APK.

### Releases

**One-time setup:** make a release keystore and add it to the repo's Actions secrets:

```bash
keytool -genkeypair -v -keystore tasks-release.jks -alias tasks -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 tasks-release.jks   # → ANDROID_KEYSTORE_BASE64
```

Secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, and optionally `ANDROID_CERT_SHA256` (the build fails if the APK's certificate doesn't match). Keep the keystore and passwords safe: losing them means people can't update without uninstalling.

**Each release:** bump `appVersion` in `gradle.properties`, add a changelog row above, merge, then tag `vX.Y.Z` on main (or publish a release with that tag on GitHub). `.github/workflows/android-release.yml` runs the checks, builds and signs `tasks-X.Y.Z.apk`, verifies its permissions and certificate, and publishes the release. `versionCode` is worked out from the version (1.2.3 → 10203).

### Running it

Needs JDK 17 and the Android SDK (Android Studio is easiest).

```bash
./gradlew :core:test :app:testDebugUnitTest   # all tests
./gradlew :app:lintDebug                        # Android lint
./gradlew :app:installDebug                     # build and install on a connected phone
./gradlew -PcoreOnly=true :core:test            # core tests only, no Android SDK needed
```

### Maintenance

- **Before a PR:** run the tests and lint above (CI runs them too, plus a debug build and a permission check). Update this README in the same PR (see `CLAUDE.md`).
- **Database changes:** bump the Room `version`, add a `Migration`, and turn on `exportSchema` with a `room.schemaLocation` so schemas are committed. Never use destructive migration. New tables must also go into the backup (`core/Backup.kt`, `TaskRepository.exportJson/importJson`).
- **Upgrading dependencies:** versions live in `build.gradle.kts` (plugins), `app/build.gradle.kts` and `core/build.gradle.kts`. Kotlin, the Compose compiler and serialization plugins share one version; KSP must match it. Update the dependency table above.
- **Network rule:** the only allowed network code is `lib/UpdateChecker.kt`. Don't add analytics, crash reporting, sync, ads or AI.

### Future enhancements

- Pinned-task widget showing one task's full Markdown description (blocks drawn with Glance)
- A widget that can show any tag, picked when you place it
- Reminder notification on the due date
- Search
- Markdown export (one readable file per task)
- Drag to reorder tasks (the `sortOrder` column is ready)
- Tick checklist items from the widget
- Voice add with the on-device speech recogniser
- Gemini adding tasks via AppFunctions (still an experimental preview, Android 16+, needs Google's approval)
- Accessibility pass, F-Droid listing

## License

[MIT](LICENSE).
