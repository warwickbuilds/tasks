# Working on this repo

Read `README.md` for how the app is built.

## Keep the README current

Every pull request updates `README.md` in the same PR. Before opening or updating a PR, check and edit each section the change touches:

- **Status** and **Features**: what works now.
- **Versions and changelog**: add a row for the PR. Update the app version and the dependency table when they change.
- **Architecture**: new screens, tables, migrations, folders or patterns.
- **Security and privacy**: anything that changes what is stored, how it is protected, or what leaves the phone.
- **Maintenance**: new steps for building, upgrading, migrating or releasing.
- **Future enhancements**: add every idea for later that comes up in discussion, and remove items once they ship.

## Hard rules

- Tasks stay 100% on the phone. Never add analytics, crash reporting, cloud sync, accounts, ads, AI, or background network calls. The **only** permitted network request is the manual-only GitHub Releases update check in `app/src/main/kotlin/com/warwickbuilds/tasks/lib/UpdateChecker.kt` (enforced by `core/src/test/.../NetworkGuardTest.kt`).
- Rules (grouping, parsing, Markdown, backup, versions) go in `:core` with unit tests; Android code stays thin.
- All database access goes through `TaskRepository`, which refreshes the widget after writes.
- Schema changes need a Room migration; never destructive migration. New tables must be added to the backup.
- Show no counts, badges or summaries in the UI.
- Run `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` before a PR.
