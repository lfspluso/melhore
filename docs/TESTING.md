# MelhoreApp – Testing and validation

This document describes how to test and validate each sprint (or step) of the app, with the expected behaviour for each check. Use it to confirm that a step is complete and working as intended.

**Prerequisites:** Android Studio (or compatible IDE), JDK 17, Android SDK (compileSdk 34, minSdk 26). See [README.md](../README.md) for build and run instructions.

---

## Automated tests

*(To be added when automated tests exist.)* Run unit tests with `./gradlew test` (or `gradlew.bat test` on Windows). Run instrumented/UI tests with `./gradlew connectedCheck`. Document here which modules or flows are covered by automated tests; use the manual steps below for anything not yet automated.

---

## Build and launch (baseline)

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Run `./gradlew assembleDebug` (or `gradlew.bat assembleDebug` on Windows) from the project root. | Build finishes with **BUILD SUCCESSFUL**. No compilation or Hilt errors. |
| 2 | Install and run the app on a device or emulator (e.g. **Run** in Android Studio). | App opens without crashing. You see the main screen (content depends on sprint; see below). |

---

## Sprint 0 – Project bootstrap

**Goal:** App builds and launches with a placeholder Compose screen.z

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Build the app (see baseline). | Build succeeds. |
| 2 | Launch the app. | App opens; no white/blank screen without any structure. You see a **Reminders** screen with at least a title (e.g. “Reminders” in an app bar or similar) and a clear content area (empty list or placeholder text). |
| 3 | (Optional) Rotate device or trigger configuration change. | App does not crash; same screen is visible. |

**Validation:** The first run shows a recognizable “Reminders” screen (not a completely empty or broken UI). No business logic is required yet.

---

## Sprint 1 – Core data and one-time reminders

**Goal:** Create and list one-time reminders; data persists in Room.

### Reminder list screen

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Launch the app (fresh install or cleared data). | You see the **Reminders** screen with a top bar titled “Reminders”, a **+** floating action button (FAB), and an **empty state**: “No reminders yet” and text like “Tap + to add a one-time reminder”. |
| 2 | Tap the **+** FAB. | Navigation goes to the **New reminder** (add) screen. Back arrow returns to the list. |

### Add one-time reminder

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | On the add screen, enter a **title** (e.g. “Test reminder”). | Title is accepted; field shows the text. |
| 2 | Tap the **date** button. | A date picker dialog opens. Selecting a date and confirming updates the shown date. |
| 3 | Tap the **time** button. | A time picker opens. Selecting a time and confirming updates the shown time. |
| 4 | (Optional) Open **Category** or **List** dropdown. | Dropdown opens; “None” and any existing categories/lists are shown. Selecting an option updates the field. (If none exist yet, only “None” may appear.) |
| 5 | (Optional) Open **Priority** dropdown. | Options (e.g. Low, Medium, High, Urgent) appear; selection updates the field. |
| 6 | Tap **Save reminder**. | App navigates **back to the Reminders list**. No crash. |
| 7 | On the list screen. | The new reminder appears as a **card** with: the title, due date/time (e.g. “MMM d, yyyy · HH:mm”), and priority if not Medium. A **delete** (trash) icon is visible on the card. |

### List and delete

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Add 2–3 reminders with different titles and times. | All appear in the list, ordered by due date/time. |
| 2 | Tap the **delete** icon on one reminder. | That reminder **disappears** from the list immediately (or after a brief update). |
| 3 | Leave at least one reminder in the list. | List continues to show the remaining items. |

### Data persistence (process death)

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Ensure at least one reminder exists in the list. | List shows the reminder(s). |
| 2 | Send app to background (Home), then in system settings **Force stop** the app (or use “Stop” in Android Studio). | App process is killed. |
| 3 | Launch the app again. | App opens on the **Reminders list**. **Previously saved reminders are still there**; list is not empty (unless you had deleted all). No data loss. |

**Validation:** You can create one-time reminders, see them in the list, delete them, and after process death the remaining data is still present (Room persistence).

---

## Sprint 2 – Notifications and scheduling

**Goal:** One-time reminders trigger a notification at (or near) the due time, including when the app is in background or has been killed.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Grant notification permission when prompted (or in app/settings). | Permission is requested and can be granted. On API 31+, exact-alarm permission (SCHEDULE_EXACT_ALARM) may need to be granted in system settings for on-time delivery. |
| 2 | Create a one-time reminder set for **1–2 minutes** from now. Save and (optional) leave the list screen or force-stop the app. | Reminder is saved. |
| 3 | Wait until the due time (or slightly after). App may be in background or killed (force-stopped). | A **notification** appears at (or near) the due time with the reminder title (or relevant content). This holds even when the app was killed before the due time. Tapping the notification opens the app. |
| 4 | (Optional) Reboot the device with a future reminder scheduled. | After boot, the reminder still fires at due time (BootReceiver reschedules alarms from the database). |

**Validation:** Scheduling and notifications work for one-time reminders; notifications fire at due time even when the app was killed.

---

## Sprint 3 – Recurring reminders and snooze

**Goal:** Recurring reminders fire on schedule; snooze delays the next notification.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | On the add reminder screen, set **Repeat** to **Daily** or **Weekly** and save. | Reminder is saved; list shows the recurrence label (e.g. "Daily"). |
| 2 | Create a recurring reminder for 1–2 minutes from now. Wait until the due time. | A notification is shown at due time. |
| 3 | Wait for the next occurrence (next day for daily, next week for weekly) or advance time in tests. | A notification is shown at the next occurrence. |
| 4 | From a reminder notification, tap one of the snooze options (**5 min**, **15 min**, **1 hour**, **1 day**). | Current notification is dismissed; a new notification appears after the chosen duration (e.g. 5 min later if you tapped "5 min"). |
| 5 | (Optional) Reboot the device with an active recurring reminder scheduled. | After boot, the reminder still fires at the next occurrence (BootReceiver reschedules). |

**Validation:** Recurring reminders fire on each occurrence; snooze lets the user choose minutes, hours, or days (5 min, 15 min, 1 hour, 1 day) and delays the next notification by that amount.

---

## Sprint 4 – Categories and lists

**Goal:** CRUD for categories and lists; assign to reminders; filter reminders.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Tap **Categories** or **Lists** in the bottom navigation bar. Tap the **+** FAB. Enter a name and tap **Save**. | Category/list is created and appears in the list. You can tap an item to edit, or the delete icon to remove it. |
| 2 | On the **Reminders** tab, tap **+** to add a reminder. Open **Category** or **List** dropdown and select one you created. Save. | Reminder is saved with the chosen category/list. |
| 3 | On the reminder list, use the filter row: tap **Filter by list** or **Filter by category**, choose a list or category (or **None**). Tap **All** to clear the filter. | List shows only reminders matching the selected filter; **All** shows every reminder. |

**Validation:** Categories and lists can be managed (CRUD via bottom nav) and used to organize and filter reminders.

---

## Sprint 5 – Priority and polish

**Goal:** Priority visible and sortable; settings persist.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | On the reminder list, confirm priority is visible (e.g. badge or label). Change sort (e.g. by priority or due date) if available. | Priority is shown; sort order changes as expected. |
| 2 | Open Settings; change a setting (e.g. default snooze duration). Restart app. | Setting value is still the one you set (persisted). |

**Validation:** Priority display/sort and settings persistence work.

---

## Sprint 5.5 – Checklists as parts of tasks

**Goal:** Reminders can have checklist (sub-task) items; add/edit/toggle on add or edit reminder; list shows progress; data persists.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | On **New reminder**, scroll to **Checklist**. Enter a sub-task in the field and tap the **+** (Add) icon. Add 2–3 items. Save the reminder. | Items appear with checkbox (unchecked); after save, app returns to list. |
| 2 | On the reminder list, find the reminder you just created. | Card shows checklist progress (e.g. "0/3"). |
| 3 | Tap the reminder card (not the delete icon). | **Edit reminder** screen opens with same title, date/time, and checklist items. |
| 4 | On edit screen: check one or two items, add another item, remove one. Tap **Save reminder**. | Changes are saved; back on list, progress updates (e.g. "2/3"). |
| 5 | Force stop the app, then relaunch. | Reminder and checklist items (and checked state) are still present. |

**Validation:** Add reminder with checklist items; edit reminder and add/toggle/remove items; list shows checklist progress; checklist data persists after process death.

---

## Sprint 6 – Documentation and release prep *(to be implemented)*

**Goal:** Docs and release readiness.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Hand off to a new developer (or follow docs yourself): open project, read README and `docs/`. | They can understand structure, run the app, and run tests from the docs. |
| 2 | Run any automated tests (if added). | Tests pass. |

**Validation:** Onboarding and tests are possible using the documentation and project as-is.

---

## Quick reference: what to check after each step

- **Sprint 0:** Build + launch → Reminders screen with clear structure (no “all white” with no content).
- **Sprint 1:** Add reminder → appears in list → delete works → force stop and relaunch → list still has data.
- **Sprint 2:** Reminder 1–2 min in future → notification at due time (even when app was killed); optional: reboot with future reminder → still fires.
- **Sprint 3:** Recurring reminder fires; snooze delays next notification.
- **Sprint 4:** Categories/lists CRUD; filter reminders by category/list.
- **Sprint 5:** Priority visible/sortable; settings persist.
- **Sprint 5.5:** Add reminder with checklist items; tap card to edit; add/toggle/remove items; list shows progress; data persists.
- **Sprint 6:** Docs support onboarding and tests.

Update this file when you add new behaviour or change acceptance criteria (e.g. new screens or flows). Keep [SPRINTS.md](SPRINTS.md) and this file in sync so “done” in SPRINTS matches what is validated here.

---

## Troubleshooting

- **Notifications do not fire at due time (especially on Android 14+):** Exact alarms require the `SCHEDULE_EXACT_ALARM` permission. On Android 12+ (API 31) the app may need the user to grant "Alarms & reminders" or use exact alarms in **Settings → Apps → MelhoreApp → Alarms & reminders** (wording may vary by device). If the permission is denied, alarms may be inexact and delivery can be delayed.
- **No notification at all:** Ensure **notification permission** (`POST_NOTIFICATIONS`) is granted when prompted (Android 13+). Check **Settings → Apps → MelhoreApp → Notifications** and ensure notifications are enabled for the app.
- **Reminders missing after reboot:** Ensure `BootReceiver` is not disabled by the user (Settings → Apps → MelhoreApp → Battery or startup) and that the device has completed boot before opening the app; the receiver reschedules from the database on `BOOT_COMPLETED`.
