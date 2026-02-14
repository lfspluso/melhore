# MelhoreApp – Sprint Plan

Each sprint ends with a runnable, testable slice. Update this file after each sprint (done criteria, lessons learned).

---

## Sprint 0 – Project bootstrap (no app logic)

**Goal:** Project and module structure; app builds and launches with an empty Compose screen.

**Deliverables:**

- Root project: Kotlin, Gradle Kotlin DSL, version catalog (`gradle/libs.versions.toml`).
- Modules: `:app`, `:core:common`, `:core:database`, `:core:notifications`, `:core:scheduling`, `:feature:reminders`, `:feature:categories`, `:feature:lists`, `:feature:settings`.
- Compose, Hilt, Room (DB + entities + DAOs), WorkManager wired in `:app` and relevant modules.
- Empty Compose screen (e.g. Reminder list placeholder) shown from main navigation.

**Done criteria:**

- [x] App builds (e.g. `./gradlew assembleDebug` or build from Android Studio).
- [x] App launches and shows the placeholder Compose screen.

**Status:** Done. First build/run successful.

---

## Sprint 1 – Core data and one-time reminders

**Goal:** Persist reminders locally; add/edit one-time reminder; list reminders.

**Deliverables:**

- **core:database:** Reminder, Category, List entities and DAOs in use; migrations if needed.
- **feature:reminders:** Add/edit one-time reminder (title, date/time, optional category, list, priority); list screen showing reminders.

**Done criteria:**

- [x] Create and list one-time reminders.
- [x] Data survives process death (Room).

**Status:** Done. Reminder list screen with Scaffold, FAB, empty state; Add reminder screen with title, date/time pickers, category/list/priority dropdowns; navigation list ↔ add; DAOs provided via Hilt in DatabaseModule.

---

## Sprint 2 – Notifications and scheduling

**Goal:** One-time reminders trigger a notification at (or near) the due time.

**Deliverables:**

- **core:notifications:** Channels, permission flow (POST_NOTIFICATIONS), show notification.
- **core:scheduling:** Schedule one-time WorkManager (or AlarmManager) when a reminder is created/updated; worker shows notification.

**Done criteria:**

- [ ] Create a one-time reminder for 1–2 minutes later; receive notification at (or near) time.

---

## Sprint 3 – Recurring reminders and snooze

**Goal:** Recurring reminders (e.g. daily, weekly) and snooze.

**Deliverables:**

- Extend Reminder model and UI for recurrence (daily, weekly).
- Schedule recurring WorkManager (or repeating alarms); handle snooze (reschedule at `snoozedUntil`).

**Done criteria:**

- [ ] Recurring reminder fires on next occurrence.
- [ ] Snooze delays next notification.

---

## Sprint 4 – Categories and lists

**Goal:** CRUD categories and lists; assign to reminders; filter by list/category.

**Deliverables:**

- **feature:categories:** CRUD categories; assign category to reminder in add/edit.
- **feature:lists:** CRUD lists; assign list to reminder; optional list filter on reminder list.

**Done criteria:**

- [ ] Create categories and lists.
- [ ] Filter reminders by list/category.

---

## Sprint 5 – Priority and polish

**Goal:** Priority visible and sortable; basic settings.

**Deliverables:**

- Priority in model and UI (badge or sort); optional sort by priority/due date.
- **feature:settings:** Notification settings (e.g. default channel, default snooze duration).

**Done criteria:**

- [ ] Priority visible; sort works.
- [ ] Settings persist.

---

## Sprint 6 – Documentation and release prep

**Goal:** Docs and release readiness.

**Deliverables:**

- Finalize `docs/CONTEXT.md`, `docs/ARCHITECTURE.md`, `docs/SPRINTS.md`.
- Update `.cursor/rules/` and `README.md`.
- Minimal store listing text; version/versionCode; optional signed build.

**Done criteria:**

- [ ] New developer (or AI) can onboard using docs and run tests.

---

*Update "Status" and checkboxes as you complete each sprint.*
