# MelhoreApp – Sprint Plan

Each sprint ends with a runnable, testable slice. Update this file after each sprint (done criteria, lessons learned).

---

## Sprint 0 – Project bootstrap (no app logic)

**Goal:** Project and module structure; app builds and launches with an empty Compose screen.

**Deliverables:**

- Root project: Kotlin, Gradle Kotlin DSL, version catalog (`gradle/libs.versions.toml`).
- Modules: `:app`, `:core:common`, `:core:database`, `:core:notifications`, `:core:scheduling`, `:feature:reminders`, `:feature:categories`, `:feature:settings`.
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
- **core:scheduling:** Schedule one-time AlarmManager (exact alarm) when a reminder is created/updated; `ReminderAlarmReceiver` shows notification at trigger time; `BootReceiver` reschedules after reboot.

**Done criteria:**

- [x] Create a one-time reminder for 1–2 minutes later; receive notification at (or near) time.

**Status:** Done. One-time AlarmManager scheduling via `ReminderScheduler` (schedule on create, cancel on delete); notification at due time even when app is killed; `SCHEDULE_EXACT_ALARM` and `RECEIVE_BOOT_COMPLETED`; permission flow in MainActivity (API 33+); tap opens app.

---

## Sprint 3 – Recurring reminders and snooze

**Goal:** Recurring reminders (e.g. daily, weekly) and snooze.

**Deliverables:**

- Extend Reminder model and UI for recurrence (daily, weekly).
- Schedule recurring WorkManager (or repeating alarms); handle snooze (reschedule at `snoozedUntil`).

**Done criteria:**

- [x] Recurring reminder fires on next occurrence.
- [x] Snooze delays next notification.

**Status:** Done. Recurring reminders (daily, weekly) reschedule on each fire via AlarmManager; add screen has Repeat dropdown. Snooze action on notification sets `snoozedUntil` and reschedules one alarm; when it fires, snooze is cleared and recurring continues. Boot reschedule considers all active reminders (including recurring and snoozed). Snooze options: 5 min, 15 min, 1 hour, 1 day (user picks one).

---

## Sprint 4 – Categories and lists (later: Tags only, Lists tab removed)

**Goal:** CRUD for organizing reminders; assign to reminders; filter reminders.

**Deliverables:**

- **feature:categories:** CRUD (shown as **Tags** in the UI); assign tag to reminder in add/edit; filter reminders by tag.
- Lists tab was removed; reminder list no longer filters by list.

**Done criteria:**

- [x] Create tags (categories in code).
- [x] Filter reminders by tag.
- [x] Validate via [TESTING.md](TESTING.md) (Sprint 4 section).

**Status:** Done. Tags CRUD in feature:categories (list + add/edit screens, ViewModels); bottom navigation bar (Reminders, Tags, Settings); reminder list filter by tag only (filter row with “All” chip and tag dropdown).

**Lessons learned:** (to be filled when sprint is done.)

---

## Sprint 5 – Priority and polish

**Goal:** Priority visible and sortable; basic settings.

**Deliverables:**

- Priority in model and UI (badge or sort); optional sort by priority/due date.
- **feature:settings:** Notification settings (e.g. default channel, default snooze duration).

**Done criteria:**

- [x] Priority visible; sort works.
- [x] Settings persist.
- [x] Validate via [TESTING.md](TESTING.md) (Sprint 5 section).

**Status:** Done. Reminder list shows priority badge on every item; sort chips "By date" / "By priority" in ViewModel; Settings screen (fourth tab) with default snooze duration (5/10/15 min, 1 h, 1 day) persisted via AppPreferences in core:common; SnoozeReceiver reads default when intent has no duration.

**Lessons learned:** SharedPreferences in core:common (AppPreferences) keeps settings accessible to both feature:settings and core:scheduling (SnoozeReceiver) without new dependencies. Sort applied in ViewModel over existing DAO flows avoids DB/schema changes.

---

## Sprint 5.5 – Checklists as parts of tasks

**Goal:** Make checklists part of a task: each reminder can have checklist items (sub-tasks) that users can add, reorder, and check off. Existing "List" remains for optional grouping/filter.

**Deliverables:**

- **core:database:** `ChecklistItemEntity` (reminderId, label, sortOrder, checked); `ChecklistItemDao`; DB version 2 with migration; DI for ChecklistItemDao.
- **feature:reminders:** Add/Edit reminder screen with Checklist section (add/remove items, toggle checked, strikethrough when checked); edit-reminder flow (route `reminders/edit/{reminderId}`, load reminder + checklist items, save updates reminder and checklist); ReminderListViewModel combines reminders with checklist items for progress; reminder list card shows checklist progress (e.g. "2/5") and tap opens edit.

**Done criteria:**

- [x] New reminder can have checklist items (add/remove; toggle checked).
- [x] Existing reminder can be opened for edit; checklist items can be added, removed, and toggled; reminder updates and reschedule work.
- [x] Reminder list card shows checklist progress when items exist.
- [x] Checklist data persists (Room); survives process death.
- [x] Docs CONTEXT, ARCHITECTURE, SPRINTS, TESTING updated.

**Status:** Done. ChecklistItem entity/DAO and migration in core:database; AddReminderScreen has Checklist section and supports add/edit mode via SavedStateHandle(reminderId); navigation to `reminders/edit/{reminderId}` from list; list card shows "checked/total" and is tappable to edit.

**Lessons learned:** Single AddReminderScreen with add vs edit mode (reminderId from route) keeps UI consistent. ChecklistItemDao.getAllItems() combined with reminders flow in ViewModel avoids N+1 and gives list progress per reminder.

---

## Sprint 6 – Filtering and sorting improvements

**Goal:** Richer filters and sort options; persist last-used filter and sort so the list opens in the same state.

**Deliverables:**

- **feature:reminders:** Optional filters (e.g. multi-tag, priority, date range) and additional sort options (e.g. by title, creation date); keep default "All" + "By date" to avoid overwhelming users.
- **core:common (AppPreferences):** Persist last-used filter and sort; ReminderListViewModel restores them on load.
- Filter row/chips consistent with current "Filtrar por tag" pattern.

**Done criteria:**

- [x] User can filter by more than one dimension (e.g. tag + priority) and/or sort by additional options.
- [x] Last filter and sort choice persist across app restarts.
- [x] Validate via [TESTING.md](TESTING.md) (Sprint 6 section).

**Status:** Done.

**Lessons learned:** In-memory filter for priority and date range keeps the DAO simple (only multi-tag uses `getRemindersByCategoryIds`). AppPreferences for filter/sort mirrors the Settings pattern; ReminderListViewModel restores on init and persists on every filter/sort change. Control strip (Surface + section labels) and empty state when filtered ("Nenhum melhore com esses filtros" + "Limpar filtros") improve UX.

---

## Sprint 7 – Visualization and grouping by tag

**Goal:** Reminder list can be grouped by tag (sections or expandable groups) with clear visual hierarchy; optional toggle between "Group by tag" and flat list.

**Deliverables:**

- **feature:reminders:** Grouped list view (sections by tag name; "Sem tag" for untagged); optional "Group by tag" vs "Flat list" toggle; accessibility (headings/semantics for sections).

**Done criteria:**

- [ ] User can view reminders grouped by tag (or flat).
- [ ] Toggle is discoverable; sections are clearly labeled.
- [ ] Validate via [TESTING.md](TESTING.md) (Sprint 7 section).

**Status:** Not started.

**Lessons learned:** (to be filled when sprint is done.)

---

## Sprint 8 – Templates ("Chegando em breve")

**Goal:** Placeholder screen for future reminder templates; single "Chegando em breve" (Coming soon) screen with a short, friendly message; discoverable entry point that does not block main flows.

**Deliverables:**

- New screen (e.g. in **feature:reminders** or **feature:templates**): "Templates de lembretes em breve" (or similar); optional illustration or icon; no backend.
- Entry point: menu item, secondary FAB, or dedicated tab (not replacing Reminders as start destination).

**Done criteria:**

- [ ] User can reach the coming-soon screen from a clear entry point.
- [ ] Message is friendly and sets expectation; no broken or placeholder logic.
- [ ] Validate via [TESTING.md](TESTING.md) (Sprint 8 section).

**Status:** Not started.

**Lessons learned:** (to be filled when sprint is done.)

---

## Sprint 9 – Integrations tab

**Goal:** New bottom tab "Integrações" with options to send/share reminders or messages to Telegram, Slack, WhatsApp (scope: deep links + share, or API integrations per product decision).

**Deliverables:**

- **feature:integrations** (or similar): New bottom-nav tab; each integration (Telegram, Slack, WhatsApp) as row/card with "Configure" or "Send to…" (deep link to app or share sheet).
- Document in ARCHITECTURE and CONTEXT whether "send messages" is share reminder text via system share or out-of-app API (e.g. bot).

**Done criteria:**

- [ ] Integrações tab visible in bottom nav; at least one integration path (e.g. share to Telegram/Slack/WhatsApp) works.
- [ ] Chosen approach (share vs API) documented in docs.
- [ ] Validate via [TESTING.md](TESTING.md) (Sprint 9 section).

**Status:** Not started.

**Lessons learned:** (to be filled when sprint is done.)

---

## Sprint 10 – Documentation and release prep

**Goal:** Docs and release readiness.

**Deliverables:**

- Finalize `docs/CONTEXT.md`, `docs/ARCHITECTURE.md`, `docs/SPRINTS.md`.
- Update `.cursor/rules/` and `README.md`.
- Minimal store listing text; version/versionCode; optional signed build.

**Done criteria:**

- [ ] New developer (or AI) can onboard using docs and run tests.
- [ ] Validate via [TESTING.md](TESTING.md) (Sprint 10 section).

**Status:** Not started.

**Lessons learned:** (to be filled when sprint is done.)

---

*Update "Status" and checkboxes as you complete each sprint.*
