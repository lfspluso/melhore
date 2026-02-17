# MelhoreApp – Sprint Plan

Each sprint ends with a runnable, testable slice. Update this file after each sprint (done criteria, lessons learned).

**Status summary:** **Done:** Sprints 0–11.5, 12, 12.1, 12.2, 12.2.1, 12.3, 13–19. **Not started:** Sprint 20.

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

- [x] User can view reminders grouped by tag (or flat).
- [x] Toggle is discoverable; sections are clearly labeled.
- [x] Validate via [TESTING.md](TESTING.md) (Sprint 7 section).

**Status:** Done.

**Lessons learned:** Grouping in the ViewModel (`groupedSections` from `remindersWithChecklist` + categories) keeps the UI simple; section order "Sem tag" first then alphabetical by tag name. Persisting "group by tag" in AppPreferences restores the view mode across restarts. Section headers use `semantics { heading() }` and content description for accessibility.

---

## Sprint 8 – Templates ("Chegando em breve")

**Goal:** Placeholder screen for future reminder templates; single "Chegando em breve" (Coming soon) screen with a short, friendly message; discoverable entry point that does not block main flows.

**Deliverables:**

- New screen (e.g. in **feature:reminders** or **feature:templates**): "Templates de lembretes em breve" (or similar); optional illustration or icon; no backend.
- Entry point: menu item, secondary FAB, or dedicated tab (not replacing Reminders as start destination).

**Done criteria:**

- [x] User can reach the coming-soon screen from a clear entry point.
- [x] Message is friendly and sets expectation; no broken or placeholder logic.
- [x] Validate via [TESTING.md](TESTING.md) (Sprint 8 section).

**Status:** Done.

**Lessons learned:** Entry via an icon in the Reminders screen TopAppBar (Modelos de lembretes) keeps the bottom nav unchanged and avoids a full tab for a placeholder. The coming-soon screen lives in feature:reminders under `ui/templates/` with no new module; route `reminders/templates` keeps the back stack and tab selection correct.

---

## Sprint 9 – Integrations tab

**Goal:** New bottom tab "Integrações" with options to send/share reminders or messages to Telegram, Slack, WhatsApp (scope: deep links + share, or API integrations per product decision).

**Deliverables:**

- **feature:integrations** (or similar): New bottom-nav tab; each integration (Telegram, Slack, WhatsApp) as row/card with "Configure" or "Send to…" (deep link to app or share sheet).
- Document in ARCHITECTURE and CONTEXT whether "send messages" is share reminder text via system share or out-of-app API (e.g. bot).

**Done criteria:**

- [x] Integrações tab visible in bottom nav; at least one integration path (e.g. share to Telegram/Slack/WhatsApp) works.
- [x] Chosen approach (share vs API) documented in docs.
- [x] Validate via [TESTING.md](TESTING.md) (Sprint 9 section).

**Status:** Done.

**Lessons learned:** Using the system share sheet (Intent.ACTION_SEND + createChooser) keeps scope simple and works for Telegram, Slack, WhatsApp, and any other app that accepts text—no API keys or bot setup. feature:integrations has no ViewModel; the screen uses LocalContext to launch the share intent. The chosen approach (share-only, no out-of-app API) is documented in ARCHITECTURE and CONTEXT.

---

## Sprint 10 – UI Improvements: Filter/Sort Toggle & Default Dark Mode

**Goal:** Improve filter/sort UX with collapsible advanced filters and set minimalistic dark mode as default.

**Deliverables:**

- **feature:reminders:** Add "Filtros avançados" toggle button in `ReminderListScreen`; when collapsed, show only sort chips; when expanded, show sort + filter rows + group-by row.
- **core:common (AppPreferences):** Add `showAdvancedFilters: Boolean` preference; persist and restore on app start.
- **app/ui/theme:** Update `MelhoreAppTheme` to default to `darkTheme = true` (was `isSystemInDarkTheme()`); create minimalistic dark color scheme with muted colors.
- Default sort: `DUE_DATE_ASC` (closest to notify first) when no saved preference.

**Implementation details:**

- **ReminderListScreen.kt:**
  - Add `showAdvancedFilters: Boolean` state (default `false`)
  - Add toggle button "Filtros avançados" above sort row
  - Conditionally show `ReminderFilterRow` and `ReminderGroupByRow` only when `showAdvancedFilters = true`
  - Always show `ReminderSortRow` (sort options always visible)
- **ReminderListViewModel.kt:**
  - Add `showAdvancedFilters: StateFlow<Boolean>`
  - Load from `AppPreferences.getShowAdvancedFilters()` on init
  - Persist on toggle change
  - Update `loadInitialSortOrder()` to return `DUE_DATE_ASC` if no saved preference
- **AppPreferences.kt:**
  - Add `KEY_SHOW_ADVANCED_FILTERS = "show_advanced_filters"`
  - Add `getShowAdvancedFilters(): Boolean` (default `false`)
  - Add `setShowAdvancedFilters(show: Boolean)`
- **Theme.kt:**
  - Change `MelhoreAppTheme(darkTheme: Boolean = true, ...)` (was `isSystemInDarkTheme()`)
  - Update `DarkColorScheme` with minimalistic colors:
    - `primary = Color(0xFF6B9BD2)` (muted blue-gray)
    - `secondary = Color(0xFF8FA8B8)` (subtle gray-blue)
    - `tertiary = Color(0xFF9E9E9E)` (neutral gray)
    - `surface = Color(0xFF121212)` (near-black)
    - `background = Color(0xFF000000)` (true black)
    - `onSurface = Color(0xFFE0E0E0)` (light gray)
    - `onBackground = Color(0xFFE0E0E0)`

**Done criteria:**

- [x] Toggle "Filtros avançados" hides/shows advanced filters
- [x] Sort row always visible
- [x] Default sort is "Por data" (closest first) when no preference saved
- [x] App launches in dark mode by default
- [x] Dark theme uses minimalistic muted colors
- [x] Filter/sort preferences persist across restarts
- [x] Validate via [TESTING.md](TESTING.md) (Sprint 10 section).

**Status:** Done.

**Lessons learned:** Hiding advanced filters behind a toggle reduces clutter while keeping sort options always visible. Default dark mode and DUE_DATE_ASC (closest first) improve UX; filter/sort/group-by preferences are persisted in AppPreferences and restored on app start.

---

## Sprint 11 – Extended Recurrence Types (Biweekly & Monthly)

**Goal:** Add biweekly and monthly recurrence options to reminders.

**Deliverables:**

- **core:database:** Add `BIWEEKLY` and `MONTHLY` to `RecurrenceType` enum.
- **core:scheduling:** Update `RecurrenceHelper.nextOccurrenceMillis()` to handle `BIWEEKLY` (add 2 weeks) and `MONTHLY` (add 1 month, handles variable month lengths).
- **feature:reminders:** Update recurrence dropdown in `AddReminderScreen` to show "Quinzenal" (biweekly) and "Mensal" (monthly); update reminder list item display to show recurrence labels.

**Implementation details:**

- **RecurrenceType.kt:**
  ```kotlin
  enum class RecurrenceType {
      NONE,
      DAILY,
      WEEKLY,
      BIWEEKLY,  // New
      MONTHLY    // New
  }
  ```
- **RecurrenceHelper.kt:**
  - Add cases for `BIWEEKLY` and `MONTHLY` in `nextOccurrenceMillis()`:
    - `BIWEEKLY`: `instant.plusWeeks(2)`
    - `MONTHLY`: `instant.plusMonths(1)` (Java Time handles variable month lengths automatically)
  - Ensure loop advances until future time (same pattern as DAILY/WEEKLY)
- **AddReminderScreen.kt:**
  - Update recurrence dropdown options:
    - "Nenhuma" → `RecurrenceType.NONE`
    - "Diária" → `RecurrenceType.DAILY`
    - "Semanal" → `RecurrenceType.WEEKLY`
    - "Quinzenal" → `RecurrenceType.BIWEEKLY` (new)
    - "Mensal" → `RecurrenceType.MONTHLY` (new)
- **ReminderListScreen.kt (ReminderItem):**
  - Update recurrence label display:
    - `RecurrenceType.BIWEEKLY` → "Quinzenal"
    - `RecurrenceType.MONTHLY` → "Mensal"

**Done criteria:**

- [x] Biweekly reminders fire every 2 weeks correctly
- [x] Monthly reminders fire every month (handles Feb 28→Mar 28, Jan 31→Feb 28, etc.)
- [x] UI dropdown shows new options
- [x] Reminder list displays recurrence labels correctly
- [x] Scheduling works for new types (alarms reschedule correctly)
- [x] Validate via [TESTING.md](TESTING.md) (Sprint 11 section).

**Status:** Done.

**Lessons learned:** Changing `ReminderAlarmReceiver` to check `!= NONE` instead of explicitly listing `DAILY || WEEKLY` future-proofs the code for any additional recurrence types. Java Time's `plusMonths()` automatically handles variable month lengths (e.g., Jan 31 → Feb 28/29, Feb 28 → Mar 28), eliminating the need for manual edge case handling. No database migration was required since Room stores enum values as strings/integers.

---

## Sprint 11.5 – Next Notification Date Display & Auto-Delete Setting

**Goal:** Display the next notification date for each Melhore (or "MELHORADO" for completed non-recurring reminders) and add a setting to auto-delete completed non-recurring reminders.

**Deliverables:**

- **feature:reminders/ui/list/ReminderListScreen.kt:**
  - Update `ReminderItem` composable to calculate and display next notification date instead of raw `dueAt`
  - Logic:
    - If `snoozedUntil` is set and in the future → show `snoozedUntil` formatted
    - If recurring (`type != NONE`) → calculate next occurrence using `RecurrenceHelper.nextOccurrenceMillis(dueAt, type)` and show formatted date
    - If non-recurring (`type == NONE`):
      - If `dueAt` is in the past → show "MELHORADO"
      - If `dueAt` is in the future → show formatted `dueAt`
  - Create helper function `getNextNotificationDate(reminder: ReminderEntity): Pair<Long?, String>` that returns (timestamp, displayText)

- **core:common/preferences/AppPreferences.kt:**
  - Add `KEY_AUTO_DELETE_COMPLETED_REMINDERS = "auto_delete_completed_reminders"`
  - Add `getAutoDeleteCompletedReminders(): Boolean` (default `false`)
  - Add `setAutoDeleteCompletedReminders(enabled: Boolean)`

- **feature:settings/ui/SettingsScreen.kt:**
  - Add new section "Lembretes" with toggle "Excluir automaticamente lembretes concluídos sem recorrência"
  - Use `Switch` component for the toggle

- **feature:settings/ui/SettingsViewModel.kt:**
  - Add `autoDeleteCompletedReminders: StateFlow<Boolean>`
  - Add `setAutoDeleteCompletedReminders(enabled: Boolean)`
  - Load initial value from `AppPreferences` on init
  - When user enables auto-delete toggle:
    - Delete all existing non-recurring reminders where `dueAt < now` and `type == NONE`
    - Cancel alarms for deleted reminders via `ReminderScheduler`

- **core:scheduling/ReminderAlarmReceiver.kt:**
  - After showing notification, if reminder is non-recurring (`type == NONE`):
    - Check `AppPreferences.getAutoDeleteCompletedReminders()`
    - If enabled, delete the reminder: `app.database.reminderDao().deleteById(reminderId)`
    - Cancel any scheduled alarms: `app.reminderScheduler.cancelReminder(reminderId)`
    - Don't reschedule (return early)

**Done criteria:**

- [x] Reminder list shows next notification date (or "MELHORADO" for completed non-recurring)
- [x] Snoozed reminders show snooze time as next notification
- [x] Recurring reminders show calculated next occurrence
- [x] Settings screen has auto-delete toggle
- [x] When enabled, non-recurring reminders are deleted after notification fires
- [x] When enabled, all past non-recurring reminders are deleted immediately
- [x] Setting persists across app restarts
- [x] Validate via [TESTING.md](TESTING.md) (Sprint 11.5 section).

**Status:** Done.

**Lessons learned:** Next notification date is computed in `getNextNotificationDate()` (snoozed → recurring next occurrence → non-recurring dueAt or empty for past). The auto-delete behavior was reworked in Sprint 13 to "delete after completion" (COMPLETED status only), implemented via the Settings toggle "Excluir automaticamente lembretes concluídos" and ReminderAlarmReceiver/ReminderScheduler.

---

## Sprint 12 – Routine Type for Melhores and Custom Recurrence

**Goal:** Transform Rotina (Routine) from a recurrence type into a **type of Melhore**, and add **custom recurrence** support for selecting specific days of the week. Routines can have any recurrence pattern (daily, weekly, biweekly, monthly, or custom), and custom recurrence is also available for regular Melhores.

**Deliverables:**

- **core:database/entity/ReminderEntity.kt:**
  - Add `isRoutine: Boolean` field (default `false`) to distinguish Routine reminders from regular Melhores
  - Add `customRecurrenceDays: String?` field to store custom days of week as comma-separated string (e.g., "MONDAY,WEDNESDAY,FRIDAY")
  - Database version: 3 → 4

- **core:database/entity/RecurrenceType.kt:**
  - Add `CUSTOM` enum value for custom recurrence patterns

- **core:database/DatabaseModule.kt:**
  - Add migration 3→4: add `isRoutine INTEGER NOT NULL DEFAULT 0` and `customRecurrenceDays TEXT` columns

- **core:common/RecurrenceDaysConverter.kt:**
  - Create utility for serializing/deserializing Set<DayOfWeek> to/from comma-separated string

- **core:scheduling/RecurrenceHelper.kt:**
  - Update `nextOccurrenceMillis()` to handle `CUSTOM` type with customRecurrenceDaysString parameter
  - Add `nextCustomOccurrenceMillis()` helper function to find next occurrence matching specified days of week

- **feature:reminders/ui/addedit/AddReminderScreen.kt:**
  - Add "Tipo" (Type) section with toggle/chips: "Melhore" (default) and "Rotina"
  - Update recurrence dropdown to include "Personalizado" option (maps to `RecurrenceType.CUSTOM`)
  - When "Personalizado" is selected, show day-of-week selector (checkboxes for Monday through Sunday)
  - Create `RoutineTypeSelector` composable for choosing between Melhore and Rotina
  - Create `CustomRecurrenceDaysSelector` composable for selecting days of week

- **feature:reminders/ui/addedit/AddReminderViewModel.kt:**
  - Add `isRoutine: StateFlow<Boolean>` state
  - Add `customRecurrenceDays: StateFlow<Set<DayOfWeek>>` state
  - Update `saveReminder()` to persist `isRoutine` and serialize `customRecurrenceDays` to comma-separated string
  - Update `init` block to load `isRoutine` and parse `customRecurrenceDays` when editing existing reminder

- **feature:reminders/ui/list/ReminderListScreen.kt:**
  - Update reminder item display to show "Rotina" badge/tag when `isRoutine == true`
  - Update recurrence label display to show custom days when `type == CUSTOM` (e.g., "Seg, Qua, Sex")
  - Update `getNextNotificationDate()` to pass `customRecurrenceDays` to `nextOccurrenceMillis()`

**Implementation details:**

- **ReminderEntity.kt:**
  ```kotlin
  data class ReminderEntity(
      // ... existing fields
      val isRoutine: Boolean = false,
      val customRecurrenceDays: String? = null,
      // ...
  )
  ```

- **RecurrenceType.kt:**
  ```kotlin
  enum class RecurrenceType {
      NONE,
      DAILY,
      WEEKLY,
      BIWEEKLY,
      MONTHLY,
      CUSTOM  // New
  }
  ```

- **RecurrenceHelper.kt:**
  - `nextOccurrenceMillis()` signature updated to accept optional `customRecurrenceDaysString: String?`
  - For CUSTOM type: finds next occurrence that matches one of the specified days of week
  - Custom recurrence logic: if current day matches and time hasn't passed, use current day; otherwise advance to next matching day in the week

- **AddReminderScreen.kt:**
  - Routine type selector uses FilterChip components for "Melhore" vs "Rotina"
  - Custom recurrence days selector shows FilterChip for each day of week (Seg, Ter, Qua, Qui, Sex, Sáb, Dom)
  - At least one day must be selected when CUSTOM recurrence is chosen

**Done criteria:**

- [x] Routine type selector appears in add/edit reminder screen
- [x] Routine reminders can be created and saved with any recurrence pattern
- [x] Custom recurrence option appears in recurrence dropdown
- [x] Custom recurrence days selector appears when "Personalizado" is selected
- [x] Custom recurrence reminders fire on correct days
- [x] Routine badge appears in reminder list for Routine reminders
- [x] Custom recurrence days display correctly in reminder list (e.g., "Seg, Qua, Sex")
- [x] Database migration 3→4 runs successfully
- [x] Editing existing reminders preserves isRoutine and customRecurrenceDays
- [x] Validate via [TESTING.md](TESTING.md) (Sprint 12 section).

**Status:** Done.

**Lessons learned:** Storing custom recurrence days as a comma-separated string in the database provides a simple, cloud-sync-friendly format while keeping the data model flexible. Using FilterChip components for both Routine type selection and custom days selection provides a consistent, intuitive UI. The custom recurrence logic handles edge cases like finding the next matching day within the current week or advancing to the next week if needed. Separating Routine as a type (isRoutine flag) rather than a recurrence type allows Routines to have any recurrence pattern, making them more flexible and useful.

---

## Sprint 12.1 – Database Rework: Parent-Child Relationship and ID Generation

**Goal:** Rework database to have proper ID generation and clear parent-child relationship between Rotina reminders and their daily task reminders.

**Deliverables:**

- **core:database/entity/ReminderEntity.kt:**
  - Add `parentReminderId: Long?` field (nullable, foreign key to parent Rotina reminder)
  - Add `startTime: Long?` field (nullable, for task start time - epoch milliseconds)
  - Add `checkupFrequencyHours: Int?` field (nullable, for checkup frequency in hours)
  - Add `isTask: Boolean` field (default `false`) to distinguish task reminders from regular reminders
  - Database version: 4 → 5

- **core:database/DatabaseModule.kt:**
  - Add migration 4→5: add columns and indexes for parent-child relationship

- **core:database/dao/ReminderDao.kt:**
  - Add query methods: `getTasksByParentReminderId()` and `getTasksByParentReminderIdOnce()`
  - Add query method: `getAllRemindersExcludingTasks()` to filter out task reminders

- **core:database/MelhoreDatabase.kt:**
  - Update version to 5

**Done criteria:**

- [x] Database migration 4→5 runs successfully
- [x] `parentReminderId`, `startTime`, `checkupFrequencyHours`, and `isTask` fields added to ReminderEntity
- [x] Foreign key relationship established (via entity annotation)
- [x] Indexes created for efficient queries
- [x] DAO methods added to query tasks by parent reminder ID
- [x] Existing queries continue to work (backward compatible)

**Status:** Done.

**Lessons learned:** SQLite doesn't support adding foreign keys via ALTER TABLE, but Room enforces them for new tables via entity annotations. Foreign key cascade delete (`onDelete = ForeignKey.CASCADE`) ensures child task reminders are automatically deleted when parent Rotina is deleted. **Database optimization (post-implementation):** Migration 5→6 added indexes for query performance: single-column indexes on `status` and `startTime` for filtering and sorting; composite indexes on `(status, dueAt)` for `getUpcomingActiveReminders()` queries, `(isTask, status)` for task filtering, and `(parentReminderId, startTime, dueAt)` for efficient parent-child queries with ordering. Indexes are designed to match actual query patterns and prevent full table scans as data grows.

---

## Sprint 12.2 – Rotina Notification Behavior Development

**Goal:** Implement Rotina notification behavior: when Rotina notification fires, clicking it navigates to a screen to add daily tasks (Tarefas) as child reminders. Tasks have start times and checkup frequencies, reusing snooze logic for checkups.

**Deliverables:**

- **core:scheduling/ReminderAlarmReceiver.kt:**
  - Detect when notification is for a Rotina reminder (`isRoutine == true`)
  - Update notification content intent to navigate to task setup screen instead of main app
  - Add "Skip day" action button to Rotina notifications

- **core:scheduling/RoutineSkipReceiver.kt (new):**
  - Handle "Skip day" action from Rotina notification
  - When confirmed: advance Rotina reminder to next occurrence (skip current day)
  - Update `dueAt` in database and reschedule next occurrence

- **feature:reminders/ui/routine/RotinaTaskSetupScreen.kt (new):**
  - New screen shown when Rotina notification is clicked
  - Display Rotina reminder title and date
  - Allow user to add multiple tasks (Tarefas) for the day
  - For each task: task title/name input, start time picker, checkup frequency picker
  - "Save tasks" button creates child reminder entities
  - "Skip day" button (with confirmation)

- **feature:reminders/ui/routine/RotinaTaskSetupViewModel.kt (new):**
  - Load parent Rotina reminder
  - Manage list of tasks to create (title, startTime, checkupFrequencyHours)
  - `saveTasks()`: Create child reminder entities for each task
  - `skipDay()`: Advance Rotina to next occurrence

- **core:scheduling/ReminderScheduler.kt:**
  - Extend `scheduleReminder()` to handle task reminders with checkup frequency
  - Schedule initial notification at `startTime`
  - After initial notification, schedule checkup notifications every `checkupFrequencyHours`

- **core:scheduling/TaskCheckupReceiver.kt (new):**
  - Handle checkup notifications for task reminders
  - Show notification with "Done", "Snooze", "Continue" actions
  - Schedule next checkup based on user action

- **app/ui/navigation/MelhoreNavHost.kt:**
  - Add route: `"reminders/routine/{reminderId}/setup"`

- **core:notifications/NotificationHelper.kt:**
  - Update `showReminderNotification()` to accept optional `isRoutine: Boolean` parameter
  - When `isRoutine == true`, add "Skip day" action button
  - Update content intent to navigate to task setup screen for Rotina reminders

**Done criteria:**

- [x] Rotina notification click navigates to task setup screen
- [x] Task setup screen allows adding multiple tasks with start times and checkup frequencies
- [x] Child reminder entities created with proper parent relationship
- [x] Task reminders scheduled at start time
- [x] Checkup notifications fire every X hours after start time
- [x] Checkup notifications have "Done", "Snooze", "Continue" actions
- [x] "Skip day" action works with confirmation
- [x] Skip day advances Rotina to next occurrence
- [x] Validate via [TESTING.md](TESTING.md) (Sprint 12.2 section).

**Status:** Done.

**Lessons learned:** Rotina notifications navigate to task setup screen via deep link intent extras. Task reminders are created as child entities with parent-child relationship. Checkup notifications reuse existing snooze logic and scheduling infrastructure. The task setup screen allows users to add multiple tasks with start times and checkup frequencies. Skip day functionality advances Rotina to next occurrence using RecurrenceHelper. Task checkup notifications provide "Done", "Snooze", and "Continue" actions for flexible task management.

---

## Sprint 12.2.1 – Rotina Current Period Tasks and Navigation

**Goal:** Restrict Rotina task creation to only the current period (day/week/month based on recurrence type) and navigate back to Melhore home page after tasks are saved.

**Deliverables:**

- **feature:reminders/ui/routine/RotinaTaskSetupViewModel.kt:**
  - Add period calculation logic based on parent reminder's recurrence type:
    - Daily: current day (start of day to end of day)
    - Weekly: current week (start of week to end of week)
    - Biweekly: current biweekly period
    - Monthly: current month (start of month to end of month)
    - Custom: current period based on custom recurrence days
  - Add helper functions `getCurrentPeriodStart()` and `getCurrentPeriodEnd()` to calculate period boundaries using `java.time` APIs
  - Update `addTask()` to set default start time within current period boundaries
  - Update `updateTask()` to validate that task start time is within current period
  - Add validation in `saveTasks()` to ensure all tasks fall within the current period before saving

- **feature:reminders/ui/routine/RotinaTaskSetupScreen.kt:**
  - Add visual indicator showing the current period (e.g., "Tasks for: [date range]")
  - Update time picker to restrict selection to current period only
  - Show validation message/error if user tries to set task outside current period
  - Display period boundaries clearly in the UI

- **app/ui/navigation/MelhoreNavHost.kt:**
  - Update `onSaved` callback for RotinaTaskSetupScreen to navigate to reminders home (`Tab.Reminders.route`) instead of just `popBackStack()`
  - Use `popUpTo` with appropriate flags to clear back stack and return to home page

**Done criteria:**

- [x] Period calculation works correctly for all recurrence types (daily, weekly, biweekly, monthly, custom)
- [x] Tasks can only be created/scheduled within the current period boundaries
- [x] Time picker restricts selection to current period
- [x] Visual indicator shows current period in the UI
- [x] Validation prevents saving tasks outside current period
- [x] After saving tasks, navigation returns to Melhore home page (reminders list)
- [x] Back stack is properly cleared when navigating home
- [x] Validate via [TESTING.md](TESTING.md) (Sprint 12.2.1 section).

**Status:** Done.

**Lessons learned:** Period boundaries are computed in RotinaTaskSetupViewModel with `getCurrentPeriodStart()` and `getCurrentPeriodEnd()` using `java.time` (ZoneId, ZonedDateTime, WeekFields for locale-based week). Date picker uses Material 3 `SelectableDates` to restrict selectable dates to the current period; time picker clamps the result to period bounds. Validation in `updateTask()` and `saveTasks()` prevents saving tasks outside the period and surfaces a single error message. Navigation to Melhore home after save/skip was already implemented in MelhoreNavHost via `popUpTo(startDestination) { inclusive = true }`.

---

## Sprint 12.3 – UI Tabs: Separate Tarefas and Rotinas

**Goal:** Add tabs at the bottom of the Melhores screen to separate Tarefas (task reminders) and Rotinas (routine reminders), reducing visual clutter.

**Deliverables:**

- **feature:reminders/ui/list/ReminderListScreen.kt:**
  - Add tab row below top app bar
  - Two tabs: "Tarefas" (Tasks) and "Rotinas" (Routines)
  - Default tab: "Tarefas" (shows regular reminders only: excludes child task reminders and Rotinas; i.e. `isTask == false` and `isRoutine == false`)
  - "Rotinas" tab: shows only Rotina reminders (`isRoutine == true` and `isTask == false`)

- **feature:reminders/ui/list/ReminderListViewModel.kt:**
  - Add `selectedTab: StateFlow<ReminderTab>` state
  - Update `remindersWithChecklist` flow to filter based on selected tab
  - Persist selected tab in `AppPreferences`

- **feature:reminders/ui/list/ReminderTab.kt (new enum):**
  - Enum with TAREfas and ROTINAS values

- **core:common/preferences/AppPreferences.kt:**
  - Add methods to persist/restore selected reminder tab

**Done criteria:**

- [x] Tab row appears below top app bar in ReminderListScreen
- [x] Two tabs: "Tarefas" and "Rotinas"
- [x] "Tarefas" tab shows regular reminders (excludes child task reminders)
- [x] "Rotinas" tab shows only Rotina reminders
- [x] Tab selection persists across app restarts
- [x] Empty states shown when no reminders in selected tab
- [x] Visual distinction between active and inactive tabs
- [x] Validate via [TESTING.md](TESTING.md) (Sprint 12.3 section).

**Status:** Done.

**Lessons learned:** Tab state and filtering are applied in ReminderListViewModel via `remindersWithChecklistForTab` (combine of `remindersWithChecklist` and `_selectedTab`); filtering is in-memory (TAREFAS: `!isTask && !isRoutine` so only regular melhores; ROTINAS: `isRoutine && !isTask`), consistent with existing priority/date filters. Selected tab is persisted in AppPreferences and restored on load. Pending-confirmation warning section is shown only on Tarefas tab. Material 3 PrimaryTabRow with Tab composables provides clear selected/unselected styling.

---

## Sprint 13 – Snooze/Completion Logic for Melhores

**Goal:** Implement comprehensive snooze/completion logic where Melhores never deactivate themselves. Users can mark reminders as complete (with confirmation), snooze them, or cancel them. Melhores remind users every 30 minutes until manually completed or cancelled.

**Deliverables:**

- **core:database:** Add `ReminderStatus` enum (ACTIVE, COMPLETED, CANCELLED); update `ReminderEntity` with `status` field; database migration 2→3.
- **core:scheduling:** Remove auto-deactivation logic; implement 30-minute recurring notifications for ACTIVE reminders; update `ReminderAlarmReceiver` to schedule next notification every 30 minutes.
- **feature:reminders:** Add completion flow with confirmation modal ("Você tem certeza?"); update UI to show completed reminders with low-contrast styling, "MELHORADO" tag, and delete button only when completed; add cancellation option in edit screen with confirmation; add filter option to show/hide completed reminders.
- **feature:settings:** Rework auto-delete setting to "delete after completion" (only deletes COMPLETED reminders, not just notified ones).

**Done criteria:**

- [x] ReminderStatus enum created and ReminderEntity updated with status field
- [x] Database migration 2→3 successfully migrates existing data
- [x] Auto-deactivation logic removed from ReminderAlarmReceiver
- [x] Users can mark reminders as complete with confirmation modal
- [x] Completed reminders show low-contrast styling, "MELHORADO" tag, and delete button
- [x] Users can cancel reminders from edit screen with confirmation
- [x] Cancelled reminders show "CANCELADO" tag
- [x] Reminders notify every 30 minutes until completed or cancelled
- [x] Filter option to show/hide completed reminders works and persists
- [x] Auto-delete setting reworked to only delete COMPLETED reminders
- [x] Validate via [TESTING.md](TESTING.md) (Sprint 13 section).

**Status:** Done.

**Lessons learned:** Using ReminderStatus enum instead of boolean `isActive` provides clearer semantics and better extensibility. The 30-minute notification logic ensures reminders persist until explicitly completed or cancelled, improving user engagement. The completion confirmation modal prevents accidental completions. Filter persistence for completed reminders improves UX by allowing users to hide completed items while keeping them accessible.

---

## Sprint 14 – New Snooze Options

**Goal:** Implement new snooze options: "Fazendo" (1-hour follow-up with completion check), "15 minutos", "1 hora", and "Personalizar" (custom duration). Replace old snooze options (5 min, 1 day).

**Deliverables:**

- **core:scheduling:** Update snooze options to "Fazendo", "15 min", "1 hora", "Personalizar"; implement "Fazendo" special flow with 1-hour follow-up notification asking "Você estava fazendo {task name}, você completou?"; create `CompletionCheckReceiver` to handle completion and snooze actions from follow-up notification.
- **core:notifications:** Add method to show completion check notification with custom message; add new string resources for snooze options and follow-up messages.
- **feature:settings:** (Optional for Sprint 14) Add UI to customize snooze options. If too complex, defer to Sprint 15.

**Implementation details:**

- **SnoozeReceiver.kt:**
  - Update `SNOOZE_DURATIONS_MS` to: 15 min, 1 hour (remove 5 min, 1 day)
  - Update `SNOOZE_PRESET_COUNT` to 4
  - Add special handling for "Fazendo" action (`EXTRA_IS_FAZENDO` flag)
  - When "Fazendo" selected, schedule special follow-up notification in 1 hour with `isFazendoFollowup=true`
  - Handle "Personalizar" action (`EXTRA_IS_CUSTOM` flag) - for Sprint 14, use default duration (15 min) or placeholder
- **ReminderAlarmReceiver.kt:**
  - Update snooze actions to show: "Fazendo", "15 min", "1 hora", "Personalizar"
  - Add special handling for "Fazendo" follow-up notifications
  - When follow-up fires (`isFazendoFollowup=true`), show notification: "Você estava fazendo {title}, você completou?"
  - Follow-up notification actions: "Sim" (mark complete), "+15 min", "+1 hora", "Personalizar"
- **CompletionCheckReceiver.kt (new):**
  - Handle "Sim" action from follow-up notification → mark reminder as COMPLETED
  - Cancel all scheduled alarms for completed reminder
  - Handle "+15 min", "+1 hora", "Personalizar" actions from follow-up → snooze with respective durations
- **NotificationHelper.kt:**
  - Add `showCompletionCheckNotification()` method for "Fazendo" follow-up
  - Support different notification text for completion check vs regular reminder
- **strings.xml:**
  - Add: `snooze_fazendo`, `snooze_15_min`, `snooze_1_hour`, `snooze_personalizar`
  - Add: `fazendo_followup_message`, `fazendo_complete`, `fazendo_snooze_15_min`, `fazendo_snooze_1_hour`, `fazendo_snooze_personalizar`

**Done criteria:**

- [x] "Fazendo" option schedules 1-hour follow-up notification
- [x] Follow-up notification shows completion check message: "Você estava fazendo {task name}, você completou?"
- [x] Follow-up notification has "Sim", "+15 min", "+1 hora", "Personalizar" actions
- [x] "Sim" marks reminder as COMPLETED
- [x] "+15 min" and "+1 hora" snooze options work from follow-up
- [x] "15 minutos" and "1 hora" snooze options work from regular notification
- [x] "Personalizar" uses default duration (15 min) or placeholder
- [x] Old snooze options ("5 min", "1 dia") removed
- [x] String resources added
- [x] Validate via [TESTING.md](TESTING.md) (Sprint 14 section).

**Status:** Done.

**Lessons learned:** Android notifications have a hard limit of 3 visible actions. We show "15 minutos", "1 hora", and "Personalizar" in regular notifications. "Fazendo" was removed from regular notification actions but remains available via the follow-up flow. The "Fazendo" follow-up notification shows all 4 actions ("Sim", "+15 min", "+1 hora", "Personalizar") and works correctly.

---

## Sprint 15 – Snooze Options Settings

**Goal:** Add settings UI to customize snooze options shown in reminder notifications.

**Deliverables:**

- **feature:settings:** Add UI section to enable/disable and customize snooze options (5 minutos, 15 minutos, 30 minutos, 1 hora, 2 horas, 1 dia, Personalizar). Maximum 3 options can be selected at once.
- **core:common (AppPreferences):** Add preferences to store enabled snooze options.
- **core:scheduling:** Update `ReminderAlarmReceiver` to read enabled snooze options from preferences and only show enabled options in notifications.

**Implementation details:**

- **AppPreferences.kt:**
  - Add `KEY_ENABLED_SNOOZE_OPTIONS = "enabled_snooze_options"`
  - Add `getEnabledSnoozeOptions(): Set<String>` (returns set of enabled option keys: "5_min", "15_min", "30_min", "1_hour", "2_hours", "1_day", "personalizar")
  - Add `setEnabledSnoozeOptions(options: Set<String>)`
  - Default: 3 options enabled ("5_min", "15_min", "1_hour")
- **SettingsViewModel.kt:**
  - Add `enabledSnoozeOptions: StateFlow<Set<String>>`
  - Add `setSnoozeOptionEnabled(option: String, enabled: Boolean)`
  - Load from `AppPreferences` on init
  - Persist on change
  - Validation: maximum 3 options can be enabled at once; at least 1 option must remain enabled
- **SettingsScreen.kt:**
  - Add new section "Opções de adiamento" under "Notificações"
  - Show checkboxes for each snooze option: "5 minutos", "15 minutos", "30 minutos", "1 hora", "2 horas", "1 dia", "Personalizar"
  - Allow user to enable/disable each option (up to 3 at once)
  - Disable checkboxes when 3 options are already selected (except for already selected ones)
  - Show description: "Escolha até 3 opções que aparecem nas notificações"
- **ReminderAlarmReceiver.kt:**
  - Update `buildSnoozeActions()` to read `AppPreferences.getEnabledSnoozeOptions()`
  - Only add actions for enabled options
  - Support all 7 snooze options with correct durations
  - Ensure at least one option is always enabled (prevent all disabled)

**Done criteria:**

- [x] Settings screen shows "Opções de adiamento" section with checkboxes for each snooze option
- [x] User can enable/disable all 7 options: "5 minutos", "15 minutos", "30 minutos", "1 hora", "2 horas", "1 dia", "Personalizar"
- [x] Maximum 3 options can be selected at once (validation)
- [x] Enabled options persist across app restarts
- [x] Reminder notifications only show enabled snooze options
- [x] At least one option must be enabled (validation)
- [x] Default: 3 options enabled ("5 minutos", "15 minutos", "1 hora")
- [x] Validate via [TESTING.md](TESTING.md) (Sprint 15 section).

**Status:** Done.

**Lessons learned:** Storing enabled snooze options as a Set<String> in SharedPreferences (serialized as comma-separated string) provides a simple and flexible way to persist user preferences. The validation in SettingsViewModel ensures at least one option remains enabled and maximum 3 options can be selected, preventing edge cases where no snooze actions would be available or too many actions would clutter notifications (Android limits notifications to 3 visible actions). The fallback logic in ReminderAlarmReceiver.buildSnoozeActions() defaults to 3 options if preferences are empty, ensuring notifications always have at least one action. This pattern of reading preferences at notification time (rather than caching) ensures settings changes take effect immediately for new notifications. The UI disables checkboxes when 3 options are already selected (except for already selected ones) to provide clear feedback to users about the selection limit.

---

## Sprint 15.5 – Warning Section for Pending Confirmation Tasks

**Goal:** Add a warning section above all other tasks in the reminder list, displaying reminders tagged as "PENDENTE CONFIRMAÇÃO" with a subtitle message encouraging users to complete, schedule, or cancel pending reminders.

**Deliverables:**

- **feature:reminders:** Add warning section composable (`PendingConfirmationWarningSection`) that displays pending confirmation reminders above all other tasks; warning section shows title "PENDENTE CONFIRMAÇÃO", subtitle "É importante não deixar Melhores sem estarem completos, agendados ou cancelados", and list of pending reminders.
- **ReminderListViewModel:** Add `pendingConfirmationReminders: StateFlow<List<ReminderWithChecklist>>` that filters reminders matching pending confirmation criteria (ACTIVE status, past due date, not snoozed or snooze expired, non-recurring).

**Implementation details:**

- **ReminderListViewModel.kt:**
  - Add `pendingConfirmationReminders: StateFlow<List<ReminderWithChecklist>>` derived from `remindersWithChecklist`
  - Filter reminders where:
    - `status == ReminderStatus.ACTIVE`
    - `dueAt <= now` (past due date)
    - `snoozedUntil == null || snoozedUntil <= now` (not snoozed or snooze expired)
    - `type == RecurrenceType.NONE` (non-recurring)
- **ReminderListScreen.kt:**
  - Create `PendingConfirmationWarningSection` composable:
    - Takes list of `ReminderWithChecklist` items
    - Displays warning card/surface with orange/yellow theme (matching existing "PENDENTE CONFIRMAÇÃO" tag colors)
    - Shows title "PENDENTE CONFIRMAÇÃO"
    - Shows subtitle "É importante não deixar Melhores sem estarem completos, agendados ou cancelados"
    - Lists pending reminders using `ReminderItem` composable (reusable)
  - In `LazyColumn`, add warning section as first item (`item(key = "warning_section")`) when `pendingConfirmationReminders.isNotEmpty()`
  - Warning section appears above all other tasks regardless of grouping mode (grouped or flat list)

**Done criteria:**

- [x] Warning section appears above all other tasks when there are pending confirmation reminders
- [x] Warning section displays the subtitle message
- [x] Warning section shows all pending confirmation reminders
- [x] Warning section is visually distinct (warning colors)
- [x] Pending reminders in warning section are clickable and functional
- [x] Warning section works correctly with both grouped and flat list views
- [x] Warning section disappears when all pending reminders are completed/cancelled
- [x] Docs CONTEXT, ARCHITECTURE, SPRINTS, TESTING updated.

**Status:** Done.

**Lessons learned:** The warning section provides clear visual feedback to users about reminders that need attention. By filtering reminders based on pending confirmation criteria (ACTIVE, past due, not snoozed, non-recurring) and displaying them prominently at the top of the list, users are encouraged to take action. The warning section uses the same `ReminderItem` composable as the regular list, ensuring consistency and reusability. The section automatically appears/disappears based on the filtered list, providing a dynamic user experience that adapts to the current state of reminders.

---

## Sprint 16 – Authentication Foundation (Google Sign-In)

**Goal:** Add Google Sign-In authentication and user session management.

**Deliverables:**

- **Firebase setup:** Add Firebase dependencies and configure project (user provides `google-services.json`).
- **core:auth:** New module with `AuthRepository` for Google Sign-In, `CurrentUser` data class, `AuthModule` (Hilt).
- **feature:auth:** Login screen with Google Sign-In button.
- **app/ui/navigation:** Add auth check; show `LoginScreen` if not signed in, main app if signed in.

**Implementation details:**

- **Root build.gradle.kts:**
  ```kotlin
  plugins {
      // ... existing
      alias(libs.plugins.google.services) apply false
  }
  ```
- **app/build.gradle.kts:**
  ```kotlin
  plugins {
      // ... existing
      alias(libs.plugins.google.services)
  }
  
  dependencies {
      // ... existing
      // Firebase BOM
      implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
      implementation("com.google.firebase:firebase-auth-ktx")
      implementation("com.google.firebase:firebase-firestore-ktx")
      // Google Sign-In
      implementation("com.google.android.gms:play-services-auth:20.7.0")
  }
  ```
- **gradle/libs.versions.toml:**
  ```toml
  [plugins]
  google-services = { id = "com.google.gms.google-services", version = "4.4.0" }
  ```
- **Create core:auth module:**
  - `core/auth/build.gradle.kts` (similar to other core modules)
  - `core/auth/src/main/java/com/melhoreapp/core/auth/CurrentUser.kt`:
    ```kotlin
    data class CurrentUser(
        val userId: String,  // Firebase UID
        val email: String?
    )
    ```
  - `core/auth/src/main/java/com/melhoreapp/core/auth/AuthRepository.kt`:
    - `signInWithGoogle(activity: Activity): Flow<Result<CurrentUser>>`
    - `signOut(): Flow<Result<Unit>>`
    - `currentUser: StateFlow<CurrentUser?>`
    - Uses Firebase Auth + Google Sign-In API
  - `core/auth/src/main/java/com/melhoreapp/core/auth/AuthModule.kt` (Hilt):
    - Provides `AuthRepository` as singleton
- **Create feature:auth module:**
  - `feature/auth/ui/LoginScreen.kt`:
    - "Sign in with Google" button
    - Shows loading state during sign-in
    - Navigates to main app on success
  - `feature/auth/ui/LoginViewModel.kt`:
    - Calls `AuthRepository.signInWithGoogle()`
    - Handles success/error states
- **MelhoreNavHost.kt:**
  - Inject `AuthRepository`
  - Check `authRepository.currentUser.value`
  - If null: show `LoginScreen`
  - If signed in: show main app navigation

**Done criteria:**

- [x] User can sign in with Google
- [x] User session persists across app restarts
- [x] App shows login screen when not signed in
- [x] App shows main app when signed in
- [x] Sign out works (add to Settings screen in future sprint)
- [x] Firebase project configured (user provides `google-services.json`)
- [ ] Validate via [TESTING.md](TESTING.md) (Sprint 16 section).

**Status:** Done.

**Lessons learned:** Auth gate implemented in app via root composable (`AppContent`) that collects `AuthRepository.currentUser` and shows either `LoginScreen` or `MelhoreNavHost`; no separate NavHost for auth. Google Sign-In uses `getSignInIntent(context)` + `signInWithSignInResult(resultCode, data)` so the Activity boundary stays in the UI (launcher in `LoginScreen`); `AuthRepository` does not hold an Activity reference. App module provides `GoogleSignInOptions` via `AuthConfigModule` using `default_web_client_id` from strings; placeholder `google-services.json` and string allow the project to build; replace with real Firebase config for production sign-in.

---

## Sprint 17 – Database Migration & User Scoping

**Goal:** Add `userId` to all entities and scope all queries by current user.

**Deliverables:**

- **core:database:** Database migration 6→7: add `userId: String` column to `ReminderEntity`, `CategoryEntity`, `ChecklistItemEntity`.
- **All DAOs:** Update queries to filter by `userId` parameter.
- **All ViewModels:** Inject `AuthRepository`, pass current `userId` to DAOs.
- **Migration handling:** For existing local data, assign temporary userId or prompt user to sign in.

**Implementation details:**

- **ReminderEntity.kt:**
  ```kotlin
  data class ReminderEntity(
      // ... existing fields
      val userId: String,  // New, non-null
      // ...
  )
  ```
- **CategoryEntity.kt:** Add `userId: String`
- **ChecklistItemEntity.kt:** Add `userId: String`
- **MelhoreDatabase.kt:**
  - Update version: `@Database(version = 7, ...)`
  - Add migration 6→7 (`MIGRATION_6_7`): add `userId TEXT` to reminders, categories, checklist_items; backfill existing rows with `'local'`; add indexes for user-scoped queries.
- **All DAOs (ReminderDao, CategoryDao, ChecklistItemDao):**
  - Update all query methods to accept `userId: String` parameter
  - Add `WHERE userId = :userId` to all queries
  - Example: `getAllReminders(userId: String): Flow<List<ReminderEntity>>`
- **All ViewModels:**
  - Inject `AuthRepository`
  - Get current user: `authRepository.currentUser.value?.userId`
  - Pass `userId` to all DAO calls
  - Handle case when user is null (show login screen)
- **App layer (AuthGateViewModel):** On sign-in, run one-off migration: assign current userId to all rows with `userId = 'local'` (reminders, categories, checklist_items) via DAO methods `migrateLocalUserIdTo(userId)`. `AppPreferences` stores last signed-in userId for boot reschedule (`getLastUserId` / `setLastUserId`); `ReminderScheduler.rescheduleAllUpcomingReminders()` uses it to scope `getActiveReminders(userId)`.

**Done criteria:**

- [x] Database migration 6→7 runs successfully
- [x] All entities have `userId` field
- [x] All DAO queries filter by `userId` (except `getReminderById(id)` kept for receivers)
- [x] All ViewModels pass `userId` to DAOs
- [x] Existing data handled gracefully (assigned `'local'`, migrated to Firebase UID on sign-in)
- [x] Queries return only current user's data
- [ ] Validate via [TESTING.md](TESTING.md) (Sprint 17 section).

**Status:** Done.

**Lessons learned:** Migration 6→7 adds nullable `userId`, backfills `'local'`, and adds indexes. Last signed-in userId is persisted in `AppPreferences` so `ReminderScheduler.rescheduleAllUpcomingReminders()` (e.g. after boot) can scope by user without access to `AuthRepository`. `getReminderById(id)` was kept without `userId` so broadcast receivers (which only have reminderId in the intent) do not need to be changed. One-off migration of `'local'` to Firebase UID runs in `AuthGateViewModel` when user signs in.

---

## Sprint 18 – Cloud Sync Implementation (Firebase Firestore)

**Goal:** Sync reminder, category, and checklist data to/from Firebase Firestore.

**Deliverables:**

- **core:sync:** New module with `SyncRepository` and `FirestoreSyncService`.
- **Firestore structure:** Collections `/users/{userId}/reminders`, `/users/{userId}/categories`, `/users/{userId}/checklistItems`.
- **Sync logic:** Upload local changes to Firestore; download cloud changes and merge (cloud wins conflicts); auto-sync on data changes; offline support.

**Implementation details:**

- **Create core:sync module:**
  - `core/sync/build.gradle.kts`:
    ```kotlin
    dependencies {
        implementation(project(":core:common"))
        implementation(project(":core:database"))
        implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
        implementation("com.google.firebase:firebase-firestore-ktx")
    }
    ```
- **FirestoreSyncService.kt:**
  - `uploadReminders(userId: String, reminders: List<ReminderEntity>): Flow<Result<Unit>>`
  - `downloadReminders(userId: String): Flow<Result<List<ReminderEntity>>>`
  - `uploadCategories(userId: String, categories: List<CategoryEntity>): Flow<Result<Unit>>`
  - `downloadCategories(userId: String): Flow<Result<List<CategoryEntity>>>`
  - `uploadChecklistItems(userId: String, items: List<ChecklistItemEntity>): Flow<Result<Unit>>`
  - `downloadChecklistItems(userId: String): Flow<Result<List<ChecklistItemEntity>>>`
  - Use Firestore offline persistence: `FirebaseFirestore.getInstance().enableNetwork()` and `setFirestoreSettings()` with `setPersistenceEnabled(true)`
- **SyncRepository.kt:**
  - `syncAll(userId: String): Flow<Result<Unit>>` - Full sync (download then upload)
  - `enableAutoSync(userId: String)` - Listen to Firestore changes, update local DB
  - `disableAutoSync()` - Stop listening
  - Conflict resolution: Cloud data wins (use `updatedAt` timestamp or last-write-wins)
- **SyncModule.kt (Hilt):**
  - Provides `SyncRepository` and `FirestoreSyncService`
- **Integration points:**
  - On app start (if user signed in): Call `syncRepository.syncAll(userId)`
  - On data changes (create/update/delete): Upload to Firestore
  - Enable Firestore listeners for real-time sync (optional, can be added later)

**Done criteria:**

- [x] Data syncs to Firestore on create/update/delete
- [x] Data downloads from Firestore on app start
- [x] Changes sync across devices (test with 2 devices)
- [x] Works offline (queues changes, syncs when online)
- [x] Conflict resolution works (cloud wins)
- [x] Sync errors handled gracefully
- [x] Validate via [TESTING.md](TESTING.md) (Sprint 18 section).

**Status:** Done.

**Lessons learned:** Document ID in Firestore = Room primary key (id) so devices share the same logical id after merge. Conflict resolution: for reminders use `updatedAt` (cloud wins when `cloud.updatedAt >= local.updatedAt`); for categories and checklist items use last-write-wins (cloud overwrites). On app start after migration, `syncAll(userId)` runs (download + merge + upload), then `enableAutoSync(userId)` registers Firestore snapshot listeners for real-time updates. After each local create/update, ViewModels call `uploadAll(userId)`; on local delete they call `deleteReminderFromCloud` / `deleteCategoryFromCloud` / `deleteChecklistItemFromCloud` so the document is removed from Firestore and other devices do not re-import it. Firestore offline persistence (default) queues writes when offline. When consuming Flow-based sync APIs that emit Loading then Success/Error, collectors must wait for the terminal emission (e.g. `.first { it !is Result.Loading }`); using `.first()` alone returns Loading and cancels the flow, so no data is written to or read from Firestore.

---

## Sprint 19 – Data Migration & Sync Polish

**Goal:** Handle first-time sign-in data migration and polish sync experience.

**Deliverables:**

- **MigrationHelper:** Dialog on first sign-in asking user to choose migration strategy (upload local, merge with cloud, or start fresh).
- **Sync status:** UI indicator showing sync status (syncing, synced, error).
- **Error handling:** Retry mechanism for failed syncs; error messages in UI.
- **Settings:** Add "Sign out" option in Settings screen.

**Implementation details:**

- **MigrationHelper.kt (core:sync or core:common):**
  - `checkAndHandleMigration(userId: String, context: Context): Flow<Result<Unit>>`
  - Detect if local data exists (check for reminders/categories without userId or with temporary userId)
  - Show dialog with 3 options:
    1. "Fazer upload para esta conta" - Upload all local data to cloud
    2. "Mesclar com dados da nuvem" - Download cloud, merge (cloud wins conflicts)
    3. "Começar do zero" - Clear local data, use cloud only
  - Execute chosen action
  - Mark migration as complete in `AppPreferences`
- **Sync status indicator:**
  - Add to `ReminderListScreen` or Settings screen
  - Show "Sincronizando...", "Sincronizado", or error icon
  - Use `SyncRepository.syncStatus: StateFlow<SyncStatus>`
- **Error handling:**
  - Retry button on sync errors
  - Snackbar messages for sync failures
  - Log sync errors for debugging
- **Settings screen:**
  - Add "Sair" (Sign out) button
  - Calls `AuthRepository.signOut()`
  - Navigates to login screen
  - Optionally clears local data or keeps it (user choice)

**Done criteria:**

- [x] User sees migration dialog on first sign-in
- [x] User can choose migration strategy
- [x] Migration completes successfully for all 3 options
- [x] Sync status visible in UI
- [x] Sync errors show retry option
- [x] Sign out works from Settings
- [x] Validate via [TESTING.md](TESTING.md) (Sprint 19 section).

**Status:** Done.

**Lessons learned:** Migration is shown only when local data exists (reminders/categories with `userId = 'local'`) and migration has not been completed for the current user (per-user flag in AppPreferences). AuthGateViewModel gates on `MigrationHelper.needsMigration(userId)`; if true it shows the dialog and defers sync until the user picks a strategy. Sync status lives in `SyncRepository.syncStatus` (Idle/Syncing/Synced/Error) and is shown in ReminderListScreen below the app bar; retry calls `retrySync(userId)` which runs `syncAll` again. Sign out was already in Settings (Sprint 16); no code change.

---

## Sprint 20 – Documentation and release prep

**Goal:** Docs and release readiness.

**Deliverables:**

- Finalize `docs/CONTEXT.md`, `docs/ARCHITECTURE.md`, `docs/SPRINTS.md`.
- Update `.cursor/rules/` and `README.md`.
- Minimal store listing text; version/versionCode; optional signed build.

**Done criteria:**

- [ ] New developer (or AI) can onboard using docs and run tests.
- [ ] Validate via [TESTING.md](TESTING.md) (Sprint 20 section).

**Status:** Not started.

**Lessons learned:** (to be filled when sprint is done.)

---

## Bug fixes (post-sprint)

- **Home screen date alignment (two root causes):**
  1. **Date picker (add/edit):** Material3 DatePicker returns UTC midnight for the selected day; converting to system zone in negative-offset timezones yielded the wrong calendar day. Fixed by using `ZoneOffset.UTC` to derive the selected calendar day and for the picker initial value in `AddReminderScreen.AddReminderDatePickerDialog` and `RotinaTaskSetupScreen.TaskDatePickerDialog`.
  2. **Next notification date (recurring):** `RecurrenceHelper.nextOccurrenceMillis()` was advancing by one period (e.g. one day) before checking if the current occurrence was still in the future, so the list always showed the following occurrence (e.g. tomorrow) even when the next fire was today. Fixed by only advancing when the current occurrence is before or equal to now, so the displayed "next" is the first occurrence at or after now. See [ARCHITECTURE.md](ARCHITECTURE.md) (Date and timezone handling) and [TESTING.md](TESTING.md) (Date alignment).

---

*Update "Status" and checkboxes as you complete each sprint.*
