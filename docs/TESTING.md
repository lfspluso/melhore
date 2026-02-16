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
| 4 | (Optional) Open **Tag** dropdown. | Dropdown opens; “None” and any existing tags are shown. Selecting an option updates the field. (If none exist yet, only “None” may appear.) |
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

## Sprint 4 – Tags (organize and filter reminders)

**Goal:** CRUD for tags; assign tag to reminders; filter reminders by tag.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Tap **Tags** in the bottom navigation bar. Tap the **+** FAB. Enter a name and tap **Save**. | Tag is created and appears in the list. You can tap an item to edit, or the delete icon to remove it. |
| 2 | On the **Reminders** tab, tap **+** to add a reminder. Open **Tag** dropdown and select one you created. Save. | Reminder is saved with the chosen tag. |
| 3 | On the reminder list, use the filter row: tap **Filter by tag**, choose a tag (or **None**). Tap **All** to clear the filter. | List shows only reminders matching the selected tag; **All** shows every reminder. |

**Validation:** Tags can be managed (CRUD via bottom nav) and used to organize and filter reminders.

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

## Sprint 6 – Filtering and sorting

**Goal:** Richer filters and sort; filter/sort persistence.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Change filter (e.g. select one or more tags, or a priority, or "Próximos 7 dias" / "Este mês") and/or sort (e.g. "Por título", "Por criação", "Mais recentes"). Force stop app and relaunch. | Last-used filter and sort are restored; list shows same filter/sort state. |
| 2 | Tap "Todos" and "Por data". Relaunch app. | List shows all reminders sorted by date; state persists. |
| 3 | Apply filters so no reminders match (e.g. tag with no items, or narrow date range). | Empty state shows "Nenhum melhore com esses filtros" and "Limpar filtros" button; tapping it clears filters. |

**Validation:** Filter and sort choices persist; multi-tag, priority, date range, and all sort options work as specified; defaults remain "All" and "By date" when cleared.

---

## Sprint 7 – Grouping by tag

**Goal:** Reminder list can be grouped by tag; optional flat/grouped toggle.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Enable "Agrupar por tag" on the reminder list (Exibir row). | List shows sections per tag (and "Sem tag" for untagged); sections are clearly labeled with heading semantics. |
| 2 | Switch to "Lista plana" (or disable grouping). | List shows all reminders in single list (existing sort still applies). |
| 3 | With "Agrupar por tag" selected, force stop app and relaunch. | Grouped view is restored (preference persisted in AppPreferences). |

**Validation:** Grouped view and flat view both work; sections have clear visual hierarchy; toggle is discoverable; group-by preference persists.

---

## Sprint 8 – Templates "Chegando em breve"

**Goal:** Coming-soon placeholder screen for templates; discoverable entry point.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | On the **Melhores** (Reminders) screen, tap the **Modelos de lembretes** icon in the top app bar (dashboard/template icon). | Navigate to the "Templates de lembretes" / coming-soon screen. |
| 2 | Read the screen content. | Friendly message: "Chegando em breve" and "Em breve você poderá usar modelos para criar melhores mais rápido."; no broken or placeholder logic. |
| 3 | Tap the back arrow. | Return to the Reminders list. |

**Validation:** Screen is reachable from the Reminders app bar and sets correct expectation.

---

## Sprint 9 – Integrations tab

**Goal:** Integrações tab with send/share to Telegram, Slack, WhatsApp.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Tap **Integrações** in the bottom navigation bar. | Integrations screen opens; Telegram, Slack, WhatsApp are listed as cards. |
| 2 | Tap **Enviar para…** on any card (e.g. Telegram, Slack, or WhatsApp). | The **system share sheet** opens; user can choose Telegram, Slack, WhatsApp, or any other app that accepts text. The shared text is a default message (e.g. "Lembrete do MelhoreApp"). |

**Validation:** Integrations tab visible; at least one integration path (share sheet) works as documented.

---

## Sprint 10 – UI Improvements: Filter/Sort Toggle & Default Dark Mode

**Goal:** Advanced filters hidden by default; sort always visible; app launches in dark mode.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Launch the app (fresh install or cleared data). | App opens in **dark mode** (dark background, light text). |
| 2 | On the reminder list screen, observe the filter/sort area. | **Sort row** ("Ordenar: Por data, Por prioridade...") is **always visible**. |
| 3 | Look for "Filtros avançados" toggle button above the sort row. | Toggle button is visible. |
| 4 | With toggle collapsed (default), check what is shown. | Only sort row is visible; filter row and group-by row are **hidden**. |
| 5 | Tap "Filtros avançados" to expand. | Filter row (tags, priorities, date ranges) and group-by row appear. |
| 6 | Tap again to collapse. | Filter and group-by rows hide again; sort row remains visible. |
| 7 | Expand filters, set a filter (e.g. select a tag), collapse filters, then force stop and relaunch app. | App opens in dark mode; sort row visible; toggle is collapsed; filter is still applied (even though filter row is hidden). |
| 8 | Expand filters again. | Filter row shows the previously selected tag. |
| 9 | Check default sort when no preference saved (clear app data, launch). | Sort defaults to "Por data" (closest to notify first). |

**Validation:** Advanced filters are hidden by default; sort is always visible; app launches in dark mode; filter/sort preferences persist; toggle state persists.

---

## Sprint 11 – Extended Recurrence Types (Biweekly & Monthly)

**Goal:** Add biweekly and monthly recurrence options to reminders.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | On the add reminder screen, open the **Repetir** (Repeat) dropdown. | Dropdown shows options: "Nenhuma", "Diário", "Semanal", **"Quinzenal"**, **"Mensal"**. |
| 2 | Select **"Quinzenal"** (biweekly) and save a reminder for 1–2 minutes from now. | Reminder is saved; list shows recurrence label **"Quinzenal"**. |
| 3 | Wait until the reminder fires (notification appears). | Notification appears at due time. |
| 4 | Wait for the next occurrence (2 weeks later) or advance time in tests. | A notification is shown at the next occurrence (2 weeks after the first). |
| 5 | Create a new reminder with **"Mensal"** (monthly) recurrence, set for a date near month end (e.g., Jan 31, Feb 28). | Reminder is saved; list shows recurrence label **"Mensal"**. |
| 6 | Verify monthly recurrence handles edge cases: | |
| 6a | If reminder is set for Jan 31, next occurrence should be Feb 28 (or Feb 29 in leap year). | Next occurrence correctly falls on Feb 28/29 (Java Time handles this automatically). |
| 6b | If reminder is set for Feb 28, next occurrence should be Mar 28. | Next occurrence correctly falls on Mar 28. |
| 7 | On the reminder list, verify recurrence labels display correctly. | Reminders with biweekly recurrence show **"Quinzenal"**; monthly reminders show **"Mensal"**. |
| 8 | Create a biweekly reminder, force stop the app, then wait for the next occurrence. | Reminder still fires correctly after app restart (boot reschedule handles it). |

**Validation:** Biweekly reminders fire every 2 weeks correctly; monthly reminders fire every month and handle variable month lengths (Jan 31 → Feb 28/29, Feb 28 → Mar 28, etc.); UI dropdown shows new options; reminder list displays correct labels; scheduling reschedules correctly for new types.

---

## Sprint 11.5 – Next Notification Date Display & Auto-Delete Setting

**Goal:** Display next notification date for each Melhore (or "MELHORADO" for completed non-recurring reminders) and add setting to auto-delete completed non-recurring reminders.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | On the reminder list, observe the date shown for each reminder. | Date shown is the **next notification date** (not the original dueAt). |
| 2 | Create a non-recurring reminder for 1–2 minutes from now. Wait until it fires (notification appears). | Reminder list shows **"MELHORADO"** instead of a date for this reminder (since it's past due and non-recurring). |
| 3 | Create a recurring reminder (e.g. daily) for 1–2 minutes from now. Wait until it fires. | Reminder list shows the **calculated next occurrence date** (e.g. tomorrow for daily). |
| 4 | Create a reminder and snooze it (tap snooze option on notification). | Reminder list shows the **snooze time** as the next notification date. |
| 5 | Open **Settings** screen. Scroll to "Lembretes" section. | Section shows toggle "Excluir automaticamente lembretes concluídos sem recorrência". |
| 6 | Enable the auto-delete toggle. | If there are any past non-recurring reminders, they are **immediately deleted** from the list. |
| 7 | Create a non-recurring reminder for 1–2 minutes from now. Wait until it fires. | After notification appears, the reminder is **automatically deleted** from the list (if auto-delete is enabled). |
| 8 | Disable the auto-delete toggle. Create a non-recurring reminder for 1–2 minutes from now. Wait until it fires. | Reminder remains in the list (shows "MELHORADO") and is not deleted. |
| 9 | Force stop app and relaunch with auto-delete enabled. | Auto-delete setting persists (toggle remains enabled). |

**Validation:** Reminder list shows next notification date (or "MELHORADO" for completed non-recurring); snoozed reminders show snooze time; recurring reminders show calculated next occurrence; auto-delete setting works and persists; when enabled, non-recurring reminders are deleted after notification fires and all past non-recurring reminders are deleted immediately.

---

## Sprint 12 – Routine Type for Melhores *(to be implemented)*

**Goal:** Add Routine type that directs users to set up tasks when triggered.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | On add reminder screen, open **Repetir** (Repeat) dropdown. | Dropdown shows **"Rotina"** option (in addition to existing options). |
| 2 | Select **"Rotina"** and save a reminder for 1–2 minutes from now. | Reminder is saved; list shows recurrence label **"Rotina"**. |
| 3 | Wait until the reminder fires (notification appears). | Notification appears at due time. |
| 4 | Tap the Routine reminder notification. | App opens and navigates to task setup screen (or appropriate screen for setting up tasks for the day). |

**Validation:** Routine option appears in dropdown; Routine reminders can be created; notification fires correctly; tapping notification navigates to task setup.

---

## Sprint 13 – Snooze/Completion Logic for Melhores

**Goal:** Melhores never deactivate themselves; users can mark as complete (with confirmation), snooze, or cancel. Reminders notify every 30 minutes until completed or cancelled.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Create a reminder for 1–2 minutes from now. Wait until it fires (notification appears). | Notification appears at due time. |
| 2 | Wait 30 minutes (or advance time in tests). | Another notification appears 30 minutes after the first. |
| 3 | Continue waiting (or advance time). | Notifications continue every 30 minutes. |
| 4 | On the reminder list, tap the checkmark icon on an ACTIVE reminder. | Confirmation dialog appears: "Você tem certeza?" with "Sim" and "Cancelar" buttons. |
| 5 | Tap "Sim" in the confirmation dialog. | Reminder is marked as COMPLETED; UI updates to low-contrast, shows "MELHORADO" tag, delete button appears, checkmark button disappears. |
| 6 | Wait 30 minutes after completion. | No more notifications appear for the completed reminder. |
| 7 | Tap "Cancelar" in the confirmation dialog (if you didn't complete). | Dialog dismisses; reminder remains ACTIVE. |
| 8 | Open an existing reminder for editing (tap the reminder card). | Edit screen opens. |
| 9 | On the edit screen, tap "Cancelar melhore" button. | Confirmation dialog appears: "Você tem certeza que deseja cancelar este melhore?" |
| 10 | Tap "Sim" in cancellation confirmation. | Reminder is marked as CANCELLED; app navigates back to list; reminder shows "CANCELADO" tag. |
| 11 | On the reminder list, expand "Filtros avançados". | Filter row shows "Mostrar concluídos" chip. |
| 12 | Tap "Mostrar concluídos" to deselect it. | Completed reminders disappear from the list. |
| 13 | Tap "Mostrar concluídos" again to select it. | Completed reminders reappear in the list. |
| 14 | Force stop app and relaunch. | Filter preference persists (completed reminders shown/hidden based on last setting). |
| 15 | Open Settings screen. Scroll to "Lembretes" section. | Section shows toggle "Excluir automaticamente após conclusão" with description "Remove automaticamente melhores marcados como concluídos". |
| 16 | Enable the "Excluir automaticamente após conclusão" toggle. | If there are any COMPLETED reminders, they are immediately deleted from the list. |
| 17 | Mark a reminder as complete (with confirmation). | Reminder is immediately deleted (if auto-delete is enabled). |
| 18 | Disable the auto-delete toggle. Mark a reminder as complete. | Reminder remains in the list (shows "MELHORADO") and is not deleted. |
| 19 | Create a recurring reminder (e.g. daily) for 1–2 minutes from now. Wait until it fires. | Notification appears at due time. |
| 20 | Wait 30 minutes. | Another notification appears (30-minute reminder). |
| 21 | Wait until the next occurrence (e.g. next day for daily). | Notification appears at the next occurrence. |
| 22 | Mark the recurring reminder as complete. | Reminder is marked as COMPLETED; no more notifications appear. |
| 23 | Snooze a reminder notification (tap snooze option on notification). | Reminder is snoozed; list shows snooze time as next notification date. |
| 24 | Wait until snooze time. | Notification appears at snooze time. |
| 25 | After snooze notification, wait 30 minutes. | Another notification appears (30-minute reminder continues). |

**Validation:** Melhores never deactivate themselves; users can mark as complete (with confirmation), snooze, or cancel; reminders notify every 30 minutes until completed/cancelled; completed reminders show low-contrast styling and "MELHORADO" tag; delete button appears only for completed reminders; cancellation works from edit screen; filter to show/hide completed works and persists; auto-delete setting only deletes COMPLETED reminders; 30-minute notifications work for both one-time and recurring reminders.

---

## Sprint 14 – New Snooze Options

**Goal:** New snooze options: "Fazendo" (1-hour follow-up with completion check), "15 minutos", "1 hora", and "Personalizar" (custom duration). Old options (5 min, 1 day) removed.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Create a reminder for 1–2 minutes from now. Wait until notification appears. | Notification shows snooze actions: **"15 minutos"**, **"1 hora"**, **"Personalizar"** (old options "5 min" and "1 dia" are not shown; "Fazendo" is not shown in regular notifications). |
| 2 | Tap **"15 minutos"** on the notification. | Current notification is dismissed; a new notification appears **15 minutes later**. |
| 3 | Create another reminder for 1–2 minutes from now. Wait until notification appears. Tap **"1 hora"**. | Current notification is dismissed; a new notification appears **1 hour later**. |
| 4 | (Note: "Fazendo" is not shown in regular notifications due to Android's 3-action limit. It can be accessed via other means if needed.) | |
| 5 | Wait **1 hour** after tapping "Fazendo". | A follow-up notification appears with message: **"Você estava fazendo {task name}, você completou?"** |
| 6 | On the follow-up notification, observe the actions. | Follow-up notification shows actions: **"Sim"**, **"+15 min"**, **"+1 hora"**, **"Personalizar"**. |
| 7 | Tap **"Sim"** on the follow-up notification. | Reminder is marked as **COMPLETED**; reminder list shows "MELHORADO" tag; no more notifications appear for this reminder. |
| 8 | Create another reminder, tap "Fazendo", wait 1 hour. On follow-up, tap **"+15 min"**. | Reminder is snoozed for 15 minutes; a new notification appears 15 minutes later. |
| 9 | Create another reminder, tap "Fazendo", wait 1 hour. On follow-up, tap **"+1 hora"**. | Reminder is snoozed for 1 hour; a new notification appears 1 hour later. |
| 10 | Tap **"Personalizar"** on a regular notification or follow-up. | For Sprint 14, uses default duration (15 min) or shows placeholder behavior. |
| 11 | Force stop app and relaunch with active "Fazendo" follow-up scheduled. | Follow-up notification still fires at scheduled time after app restart. |

**Validation:** New snooze options ("15 minutos", "1 hora", "Personalizar") work correctly in regular notifications; "Fazendo" follow-up flow works correctly with completion check message; follow-up notification has correct actions; "Sim" marks reminder as COMPLETED; old snooze options removed; all functionality persists across app restarts.

---

## Sprint 15 – Snooze Options Settings

**Goal:** Add settings UI to customize which snooze options appear in reminder notifications.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Open **Settings** screen. Scroll to "Notificações" section. | Section shows "Duração padrão do adiamento" and new section **"Opções de adiamento"**. |
| 2 | In "Opções de adiamento" section, observe the checkboxes. | Checkboxes shown for: **"15 minutos"**, **"1 hora"**, **"Personalizar"**. All are checked (enabled) by default. |
| 3 | Uncheck **"1 hora"**. | Checkbox becomes unchecked. |
| 4 | Create a reminder for 1–2 minutes from now. Wait until notification appears. | Notification shows only **"15 minutos"** and **"Personalizar"** actions (no "1 hora" action). |
| 5 | Go back to Settings and check **"1 hora"** again. Uncheck **"Personalizar"**. | "1 hora" becomes checked; "Personalizar" becomes unchecked. |
| 6 | Create another reminder for 1–2 minutes from now. Wait until notification appears. | Notification shows only **"15 minutos"** and **"1 hora"** actions (no "Personalizar" action). |
| 7 | Try to uncheck all options in Settings. | At least one option must remain checked (validation prevents disabling all options). |
| 8 | Force stop app and relaunch. Go to Settings. | Previously selected snooze options are still set (preferences persist). |
| 9 | Create a reminder and check notification. | Notification shows only the enabled snooze options from Settings. |

**Validation:** Settings screen shows "Opções de adiamento" section; user can enable/disable each snooze option; enabled options persist across restarts; reminder notifications only show enabled options; at least one option must be enabled; default: all options enabled.

---

## Quick reference: what to check after each step

- **Sprint 0:** Build + launch → Reminders screen with clear structure (no “all white” with no content).
- **Sprint 1:** Add reminder → appears in list → delete works → force stop and relaunch → list still has data.
- **Sprint 2:** Reminder 1–2 min in future → notification at due time (even when app was killed); optional: reboot with future reminder → still fires.
- **Sprint 3:** Recurring reminder fires; snooze delays next notification.
- **Sprint 4:** Tags CRUD; filter reminders by tag.
- **Sprint 5:** Priority visible/sortable; settings persist.
- **Sprint 5.5:** Add reminder with checklist items; tap card to edit; add/toggle/remove items; list shows progress; data persists.
- **Sprint 6:** Filter/sort persistence; new filter and sort options.
- **Sprint 7:** Group by tag (and flat list) work; sections labeled.
- **Sprint 8:** Templates coming-soon screen reachable from Reminders app bar (Modelos de lembretes) and clear.
- **Sprint 9:** Integrações tab; at least one integration path works.
- **Sprint 10:** Advanced filters toggle; dark mode by default; sort always visible.
- **Sprint 11:** Extended recurrence types (biweekly, monthly) work correctly.
- **Sprint 11.5:** Next notification date display; auto-delete setting works.
- **Sprint 12:** (Future) Routine type for Melhores.
- **Sprint 13:** Snooze/completion logic; marking as complete/cancelled; 30-minute notifications; completed filter.
- **Sprint 14:** New snooze options ("15 minutos", "1 hora", "Personalizar"); "Fazendo" follow-up with completion check.
- **Sprint 15:** Snooze options settings; customize which options appear in notifications.
- **Sprint 16:** (Future) Authentication foundation.
- **Sprint 17:** (Future) Database migration & user scoping.
- **Sprint 18:** (Future) Cloud sync implementation.
- **Sprint 19:** (Future) Data migration & sync polish.
- **Sprint 20:** (Future) Documentation and release prep.

Update this file when you add new behaviour or change acceptance criteria (e.g. new screens or flows). Keep [SPRINTS.md](SPRINTS.md) and this file in sync so “done” in SPRINTS matches what is validated here.

---

## Sprint 20 – Documentation and release prep *(to be implemented)*

**Goal:** Docs and release readiness.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Hand off to a new developer (or follow docs yourself): open project, read README and `docs/`. | They can understand structure, run the app, and run tests from the docs. |
| 2 | Run any automated tests (if added). | Tests pass. |

**Validation:** Onboarding and tests are possible using the documentation and project as-is.

---

## Troubleshooting

- **Notifications do not fire at due time (especially on Android 14+):** Exact alarms require the `SCHEDULE_EXACT_ALARM` permission. On Android 12+ (API 31) the app may need the user to grant "Alarms & reminders" or use exact alarms in **Settings → Apps → MelhoreApp → Alarms & reminders** (wording may vary by device). If the permission is denied, alarms may be inexact and delivery can be delayed.
- **No notification at all:** Ensure **notification permission** (`POST_NOTIFICATIONS`) is granted when prompted (Android 13+). Check **Settings → Apps → MelhoreApp → Notifications** and ensure notifications are enabled for the app.
- **Reminders missing after reboot:** Ensure `BootReceiver` is not disabled by the user (Settings → Apps → MelhoreApp → Battery or startup) and that the device has completed boot before opening the app; the receiver reschedules from the database on `BOOT_COMPLETED`.
