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

- [ ] Toggle "Filtros avançados" hides/shows advanced filters
- [ ] Sort row always visible
- [ ] Default sort is "Por data" (closest first) when no preference saved
- [ ] App launches in dark mode by default
- [ ] Dark theme uses minimalistic muted colors
- [ ] Filter/sort preferences persist across restarts
- [ ] Validate via [TESTING.md](TESTING.md) (Sprint 10 section).

**Status:** Not started.

**Lessons learned:** (to be filled when sprint is done.)

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

- [ ] Reminder list shows next notification date (or "MELHORADO" for completed non-recurring)
- [ ] Snoozed reminders show snooze time as next notification
- [ ] Recurring reminders show calculated next occurrence
- [ ] Settings screen has auto-delete toggle
- [ ] When enabled, non-recurring reminders are deleted after notification fires
- [ ] When enabled, all past non-recurring reminders are deleted immediately
- [ ] Setting persists across app restarts
- [ ] Validate via [TESTING.md](TESTING.md) (Sprint 11.5 section).

**Status:** Not started.

**Lessons learned:** (to be filled when sprint is done.)

---

## Sprint 12 – Routine Type for Melhores

**Goal:** Add a new Melhore type called "Routine" (Rotine) that directs users to set up tasks for the day when triggered.

**Deliverables:**

- **core:database/entity/RecurrenceType.kt:**
  - Add `ROUTINE` to `RecurrenceType` enum

- **feature:reminders/ui/addedit/AddReminderScreen.kt:**
  - Update recurrence dropdown to show "Rotina" option (maps to `RecurrenceType.ROUTINE`)
  - Update recurrence label display to show "Rotina" for Routine type

- **core:scheduling/ReminderAlarmReceiver.kt:**
  - Handle Routine type - when fired, set intent flag or use notification action to navigate to task setup
  - Routine reminders behave similarly to one-time reminders but with special action on notification tap

- **app/ui/navigation/MelhoreNavHost.kt:**
  - Add deep link or navigation action for Routine reminder tap
  - When Routine reminder notification is tapped, navigate user to app (or specific screen) to set up tasks for the day

- **core:scheduling/RecurrenceHelper.kt:**
  - Update `nextOccurrenceMillis()` to handle `ROUTINE` type (similar to `NONE` - no automatic recurrence, but can be manually rescheduled)

**Implementation details:**

- **RecurrenceType.kt:**
  ```kotlin
  enum class RecurrenceType {
      NONE,
      DAILY,
      WEEKLY,
      BIWEEKLY,
      MONTHLY,
      ROUTINE  // New
  }
  ```
- **AddReminderScreen.kt:**
  - Update recurrence dropdown options:
    - "Rotina" → `RecurrenceType.ROUTINE` (new)
- **ReminderAlarmReceiver.kt:**
  - When Routine reminder fires, notification tap should navigate to task setup screen
  - Use notification intent extras or deep link to identify Routine type and trigger navigation
- **RecurrenceHelper.kt:**
  - `ROUTINE` case: return `null` (no automatic next occurrence, but reminder can be manually rescheduled)

**Done criteria:**

- [ ] Routine option appears in recurrence dropdown
- [ ] Routine reminders can be created and saved
- [ ] When Routine reminder fires, notification appears
- [ ] Tapping Routine notification navigates user to task setup screen
- [ ] Routine reminders show "Rotina" label in reminder list
- [ ] Validate via [TESTING.md](TESTING.md) (Sprint 12 section).

**Status:** Not started.

**Lessons learned:** (to be filled when sprint is done.)

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

- **feature:settings:** Add UI section to enable/disable and customize snooze options (15 minutos, 1 hora, Personalizar).
- **core:common (AppPreferences):** Add preferences to store enabled snooze options.
- **core:scheduling:** Update `ReminderAlarmReceiver` to read enabled snooze options from preferences and only show enabled options in notifications.

**Implementation details:**

- **AppPreferences.kt:**
  - Add `KEY_ENABLED_SNOOZE_OPTIONS = "enabled_snooze_options"`
  - Add `getEnabledSnoozeOptions(): Set<String>` (returns set of enabled option keys: "15_min", "1_hour", "personalizar")
  - Add `setEnabledSnoozeOptions(options: Set<String>)`
  - Default: all options enabled
- **SettingsViewModel.kt:**
  - Add `enabledSnoozeOptions: StateFlow<Set<String>>`
  - Add `setSnoozeOptionEnabled(option: String, enabled: Boolean)`
  - Load from `AppPreferences` on init
  - Persist on change
- **SettingsScreen.kt:**
  - Add new section "Opções de adiamento" under "Notificações"
  - Show checkboxes for each snooze option: "15 minutos", "1 hora", "Personalizar"
  - Allow user to enable/disable each option
  - Show description: "Escolha quais opções aparecem nas notificações"
- **ReminderAlarmReceiver.kt:**
  - Update `buildSnoozeActions()` to read `AppPreferences.getEnabledSnoozeOptions()`
  - Only add actions for enabled options
  - Ensure at least one option is always enabled (prevent all disabled)

**Done criteria:**

- [ ] Settings screen shows "Opções de adiamento" section with checkboxes for each snooze option
- [ ] User can enable/disable "15 minutos", "1 hora", and "Personalizar" options
- [ ] Enabled options persist across app restarts
- [ ] Reminder notifications only show enabled snooze options
- [ ] At least one option must be enabled (validation)
- [ ] Default: all options enabled
- [ ] Validate via [TESTING.md](TESTING.md) (Sprint 15 section).

**Status:** Not started.

**Lessons learned:** (to be filled when sprint is done.)

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

- [ ] User can sign in with Google
- [ ] User session persists across app restarts
- [ ] App shows login screen when not signed in
- [ ] App shows main app when signed in
- [ ] Sign out works (add to Settings screen in future sprint)
- [ ] Firebase project configured (user provides `google-services.json`)
- [ ] Validate via [TESTING.md](TESTING.md) (Sprint 16 section).

**Status:** Not started.

**Lessons learned:** (to be filled when sprint is done.)

---

## Sprint 17 – Database Migration & User Scoping

**Goal:** Add `userId` to all entities and scope all queries by current user.

**Deliverables:**

- **core:database:** Database migration 2→3: add `userId: String` column to `ReminderEntity`, `CategoryEntity`, `ChecklistItemEntity`.
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
  - Update version: `@Database(version = 3, ...)`
  - Add migration 2→3:
    ```kotlin
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add userId column (nullable initially for migration)
            database.execSQL("ALTER TABLE reminders ADD COLUMN userId TEXT")
            database.execSQL("ALTER TABLE categories ADD COLUMN userId TEXT")
            database.execSQL("ALTER TABLE checklist_items ADD COLUMN userId TEXT")
            // For existing data: assign temporary userId or leave null (handle in app logic)
            // Option: Set userId = "local_${deviceId}" for existing rows
        }
    }
    ```
- **All DAOs (ReminderDao, CategoryDao, ChecklistItemDao):**
  - Update all query methods to accept `userId: String` parameter
  - Add `WHERE userId = :userId` to all queries
  - Example: `getAllReminders(userId: String): Flow<List<ReminderEntity>>`
- **All ViewModels:**
  - Inject `AuthRepository`
  - Get current user: `authRepository.currentUser.value?.userId`
  - Pass `userId` to all DAO calls
  - Handle case when user is null (show login screen)
- **MigrationHelper.kt (new in core:common or core:database):**
  - On app start, if migration 2→3 ran and userId is null for some rows:
    - If user signed in: assign current userId to all null rows
    - If user not signed in: assign temporary userId `"local_${deviceId}"` (will be migrated on sign-in)

**Done criteria:**

- [ ] Database migration 2→3 runs successfully
- [ ] All entities have `userId` field
- [ ] All DAO queries filter by `userId`
- [ ] All ViewModels pass `userId` to DAOs
- [ ] Existing data handled gracefully (assigned userId)
- [ ] Queries return only current user's data
- [ ] Validate via [TESTING.md](TESTING.md) (Sprint 17 section).

**Status:** Not started.

**Lessons learned:** (to be filled when sprint is done.)

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

- [ ] Data syncs to Firestore on create/update/delete
- [ ] Data downloads from Firestore on app start
- [ ] Changes sync across devices (test with 2 devices)
- [ ] Works offline (queues changes, syncs when online)
- [ ] Conflict resolution works (cloud wins)
- [ ] Sync errors handled gracefully
- [ ] Validate via [TESTING.md](TESTING.md) (Sprint 14 section).

**Status:** Not started.

**Lessons learned:** (to be filled when sprint is done.)

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

- [ ] User sees migration dialog on first sign-in
- [ ] User can choose migration strategy
- [ ] Migration completes successfully for all 3 options
- [ ] Sync status visible in UI
- [ ] Sync errors show retry option
- [ ] Sign out works from Settings
- [ ] Validate via [TESTING.md](TESTING.md) (Sprint 15 section).

**Status:** Not started.

**Lessons learned:** (to be filled when sprint is done.)

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

*Update "Status" and checkboxes as you complete each sprint.*
