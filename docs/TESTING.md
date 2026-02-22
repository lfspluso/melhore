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

### Date alignment (list vs add/edit)

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Create a one-time reminder and set a **specific date** (e.g. a future date) via the date picker. Save. | The reminder is saved and you return to the list. |
| 2 | On the list (Melhores home screen). | The **date shown on the card** matches the date you selected (same calendar day and time). |
| 3 | Tap the reminder to **edit** it. | The add/edit screen opens with the **same date** pre-selected; opening the date picker shows that same day. |

**Note:** To catch timezone regressions, repeat in a timezone behind UTC (e.g. Americas, Brazil BRT) where the bug was most visible before the fix.

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

**Goal:** Display next notification date for each Melhore (or "MELHORADO" for completed non-recurring reminders) and add setting to auto-delete completed non-recurring reminders. *Note: The auto-delete behaviour is implemented as the "Excluir automaticamente lembretes concluídos" (delete after completion) setting; Sprint 13 reworked it to apply to COMPLETED reminders only.*

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

## Sprint 12 – Routine Type for Melhores and Custom Recurrence

**Goal:** Transform Rotina (Routine) from a recurrence type into a type of Melhore, and add custom recurrence support for selecting specific days of the week.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | On add reminder screen, observe the **Tipo** (Type) section. | Two FilterChips appear: **"Melhore"** (default selected) and **"Rotina"**. |
| 2 | Select **"Rotina"** chip. | "Rotina" chip becomes selected; "Melhore" chip becomes unselected. |
| 3 | Open **Repetir** (Repeat) dropdown. | Dropdown shows options: "Nenhuma", "Diário", "Semanal", "Quinzenal", "Mensal", **"Personalizado"**. |
| 4 | Select **"Personalizado"**. | Day-of-week selector appears below with FilterChips for each day (Seg, Ter, Qua, Qui, Sex, Sáb, Dom). |
| 5 | Select multiple days (e.g., Seg, Qua, Sex). | Selected days become highlighted; error message disappears if it was showing. |
| 6 | Save the reminder. | Reminder is saved; list shows **"Rotina"** badge and recurrence label shows selected days (e.g., "Seg, Qua, Sex"). |
| 7 | Create a regular Melhore (not Routine) with **"Personalizado"** recurrence. | Reminder is saved; list shows custom days but no Routine badge. |
| 8 | Create a Routine reminder with daily recurrence. | Reminder is saved; list shows "Rotina" badge and "Diário" recurrence label. |
| 9 | Wait until a custom recurrence reminder fires (notification appears). | Notification appears on one of the selected days at due time. |
| 10 | Wait for the next occurrence of a custom recurrence reminder. | Notification appears on the next matching day (e.g., if set for Mon/Wed/Fri and today is Tuesday, next notification is Wednesday). |
| 11 | Edit an existing Routine reminder. | Edit screen shows "Rotina" selected in Tipo section; recurrence and custom days (if applicable) are preserved. |
| 12 | Force stop app and relaunch with Routine and custom recurrence reminders. | All reminders persist correctly; Routine badges and custom recurrence labels display correctly. |

**Validation:** Routine type selector works; Routine reminders can be created with any recurrence pattern; custom recurrence option appears and works correctly; custom recurrence fires on correct days; Routine badge appears in list; custom days display correctly; editing preserves Routine type and custom recurrence; data persists across app restarts.

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

**Goal:** Add settings UI to customize which snooze options appear in reminder notifications. Maximum 3 options can be selected at once.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Open **Settings** screen. Scroll to "Notificações" section. | Section shows "Duração padrão do adiamento" and new section **"Opções de adiamento"**. |
| 2 | In "Opções de adiamento" section, observe the checkboxes. | Checkboxes shown for: **"5 minutos"**, **"15 minutos"**, **"30 minutos"**, **"1 hora"**, **"2 horas"**, **"1 dia"**, **"Personalizar"**. Default: **"5 minutos"**, **"15 minutos"**, **"1 hora"** are checked (enabled). |
| 3 | Try to check a fourth option (e.g. **"30 minutos"**). | Checkbox cannot be checked (validation prevents selecting more than 3 options). Other checkboxes (except the 3 already selected) are disabled. |
| 4 | Uncheck **"1 hora"** and then check **"30 minutos"**. | "1 hora" becomes unchecked; "30 minutos" becomes checked. Now "5 minutos", "15 minutos", and "30 minutos" are selected. |
| 5 | Create a reminder for 1–2 minutes from now. Wait until notification appears. | Notification shows only **"5 minutos"**, **"15 minutos"**, and **"30 minutos"** actions (no other actions). |
| 6 | Go back to Settings. Uncheck **"5 minutos"** and check **"1 hora"**. | "5 minutos" becomes unchecked; "1 hora" becomes checked. Now "15 minutos", "30 minutos", and "1 hora" are selected. |
| 7 | Create another reminder for 1–2 minutes from now. Wait until notification appears. | Notification shows only **"15 minutos"**, **"30 minutos"**, and **"1 hora"** actions. |
| 8 | Try to uncheck all options in Settings. | At least one option must remain checked (validation prevents disabling all options). |
| 9 | Force stop app and relaunch. Go to Settings. | Previously selected snooze options are still set (preferences persist). |
| 10 | Create a reminder and check notification. | Notification shows only the enabled snooze options from Settings (up to 3). |

**Validation:** Settings screen shows "Opções de adiamento" section with all 7 options; user can enable/disable each snooze option (maximum 3 at once); enabled options persist across restarts; reminder notifications only show enabled options; at least one option must be enabled; default: 3 options enabled ("5 minutos", "15 minutos", "1 hora").

---

## Sprint 15.5 – Warning Section for Pending Confirmation Tasks

**Goal:** Add a warning section above all other tasks displaying reminders tagged as "PENDENTE CONFIRMAÇÃO" with a subtitle message.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Create a non-recurring reminder with a due date in the past (e.g., 1 hour ago). Ensure it is ACTIVE (not completed or cancelled) and not snoozed. | Reminder is saved and appears in the list. |
| 2 | On the reminder list screen, observe the top of the list. | A **warning section** appears above all other tasks with: |
| 2a | | Title: **"PENDENTE CONFIRMAÇÃO"** (in orange/yellow warning color) |
| 2b | | Subtitle: **"É importante não deixar Melhores sem estarem completos, agendados ou cancelados"** |
| 2c | | List of pending reminders (the reminder you just created appears in this section) |
| 3 | Verify the warning section is visually distinct. | Warning section uses orange/yellow theme colors (matching the "PENDENTE CONFIRMAÇÃO" tag) and has elevated surface. |
| 4 | Tap on a reminder in the warning section. | Reminder opens for editing (same behavior as regular reminders). |
| 5 | Mark the reminder as complete (tap checkmark, confirm). | Warning section **disappears** (no more pending confirmation reminders). |
| 6 | Create another non-recurring reminder with past due date. | Warning section **reappears** with the new pending reminder. |
| 7 | Enable "Agrupar por tag" (group by tag) on the reminder list. | Warning section still appears **above** the grouped sections. |
| 8 | Disable "Agrupar por tag" (flat list). | Warning section still appears **above** the flat list. |
| 9 | Create a recurring reminder (e.g., daily) with past due date. | Recurring reminder does **not** appear in the warning section (only non-recurring reminders appear). |
| 10 | Snooze a pending reminder (tap snooze option on notification). | Reminder **disappears** from warning section (snoozed reminders are not considered pending). |
| 11 | Wait until snooze expires (or advance time in tests). | Reminder **reappears** in warning section (snooze expired, reminder is pending again). |
| 12 | Cancel a pending reminder (open for edit, tap "Cancelar melhore", confirm). | Warning section **disappears** if this was the only pending reminder. |
| 13 | Create multiple pending reminders (3–4 reminders with past due dates). | All pending reminders appear in the warning section, listed vertically. |
| 14 | Complete all pending reminders. | Warning section **disappears** completely. |

**Validation:** Warning section appears above all other tasks when there are pending confirmation reminders; warning section displays title and subtitle message; warning section shows all pending confirmation reminders; warning section is visually distinct (warning colors); pending reminders in warning section are clickable and functional; warning section works correctly with both grouped and flat list views; warning section disappears when all pending reminders are completed/cancelled; only non-recurring reminders with past due dates appear in warning section; snoozed reminders do not appear until snooze expires.

---

## Sprint 16 – Authentication Foundation (Google Sign-In)

**Goal:** Google Sign-In authentication and user session management; app shows login when not signed in, main app when signed in; sign out from Settings.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Build and install the app (with valid or placeholder `google-services.json` in `app/`). | Build succeeds. App launches. |
| 2 | Launch the app when **not** signed in (fresh install or after sign out). | **Login screen** is shown with title "Melhore", subtitle text, and **"Entrar com Google"** button. No bottom navigation or reminder list. |
| 3 | Tap **"Entrar com Google"**. | Google Sign-In flow starts (account picker or browser). If `default_web_client_id` / Firebase are not configured, an error may appear; use real Firebase config for full flow. |
| 4 | Complete Google Sign-In successfully (with valid Firebase project and Web client ID). | After sign-in, app shows **main app** (Reminders list with bottom nav: Melhores, Tags, Integrações, Configurações). Login screen is no longer visible. |
| 5 | Force stop the app and launch again (still signed in). | App opens directly on **main app** (no login screen). User session persisted. |
| 6 | Open **Configurações** (Settings) tab. Scroll to **"Conta"** section. | Section shows **"Sair"** button. |
| 7 | Tap **"Sair"**. | App returns to **login screen** (no main app). Sign out succeeded; auth gate reacted to `currentUser` becoming null. |
| 8 | Sign in again. | Main app is shown again. |
| 9 | (Optional) On login screen, trigger a sign-in error (e.g. cancel account picker or use invalid config). | Error message is shown (e.g. "Erro ao entrar" or exception message). **"Tentar novamente"** button allows retry. |

**Validation:** Login screen when not signed in; Google Sign-In leads to main app; session persists across app restarts; Sign out in Settings returns to login screen; errors on login are shown and retry is possible. Replace placeholder `google-services.json` and `default_web_client_id` in `app/src/main/res/values/strings.xml` with values from Firebase Console for real sign-in.

---

## Sprint 17 – Database Migration & User Scoping

**Goal:** All reminder, category, and checklist data is scoped by user; migration 6→7 adds `userId`; existing data receives `'local'` and is migrated to signed-in user on sign-in; boot reschedule uses last signed-in userId.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Build and install the app. | Build succeeds. App launches. |
| 2 | Sign in with Google. Create a reminder and a tag. | Reminder and tag are saved and appear in the list. |
| 3 | Open Reminders list and Tags list. | Only the current user's reminders and tags are shown. |
| 4 | (Optional) Sign out and sign in with a different account (or same). | After sign-in, list shows data for the signed-in user. Pre-sign-in datasa created with `'local'` is migrated to the signed-in user on first sign-in. |
| 5 | Create a reminder, force stop the app, reboot the device (with app still installed). | After reboot, open the app and sign in. Reminders are rescheduled (boot uses lastUserId from AppPreferences). |
| 6 | (Optional) Install an older build (pre–Sprint 17), create reminders/tags, then install Sprint 17 build and open app. | Migration 6→7 runs; existing rows get `userId = 'local'`. After sign-in, data is migrated to current user and appears in the list. |

**Validation:** Database migration 6→7 runs without error; all entities have `userId`; DAO queries return only current user's data; existing data is backfilled with `'local'` and migrated to signed-in user on sign-in; boot reschedule uses last signed-in userId; no regressions in reminder list, tags, add/edit, settings, or notifications.

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
- **Sprint 12:** Routine type for Melhores and custom recurrence.
- **Sprint 12.2:** Rotina notification behavior; task setup screen; task reminders with checkup notifications.
- **Sprint 12.2.1:** Rotina current period restriction; navigation to home page after save.
- **Sprint 13:** Snooze/completion logic; marking as complete/cancelled; 30-minute notifications; completed filter.
- **Sprint 14:** New snooze options ("15 minutos", "1 hora", "Personalizar"); "Fazendo" follow-up with completion check.
- **Sprint 15:** Snooze options settings; customize which options appear in notifications.
- **Sprint 15.5:** Warning section for pending confirmation tasks appears above all other tasks.
- **Sprint 16:** Google Sign-In; login screen when not signed in; main app when signed in; session persistence; Sign out in Settings.
- **Sprint 17:** Database migration 6→7; user-scoped entities and DAOs; lastUserId for boot reschedule; local data migrated on sign-in.
- **Sprint 18:** Cloud sync implementation (see section below).
- **Sprint 19:** Data migration & sync polish (see section below).
- **Sprint 19.5:** Local-only option: "Continuar sem entrar" on login; data stays on device; "Apenas neste aparelho" row; Sign out returns to login, data persists.
- **Sprint 19.75:** Weekday daily trigger: "Dias úteis" recurrence type fires Monday-Friday, skipping weekends; Rotinas with weekdays fire every weekday for task setup.
- **Sprint 20:** Bug Fixes and UI Improvements (Tarefas visibility, Rotina notification prevention, filter/sort icons, sync banner overlay, hourly pending confirmation check, "Melhore" branding).
- **Sprint 21.1–21.4:** Sprint 21.1–21.4 done (Rotina skip day, PENDENTE CONFIRMAÇÃO for all tasks, app icon, "Finais de semana" recurrence). See [SPRINTS.md](SPRINTS.md).
- **Sprint 22:** (Future) Documentation and release prep.

Update this file when you add new behaviour or change acceptance criteria (e.g. new screens or flows). Keep [SPRINTS.md](SPRINTS.md) and this file in sync so “done” in SPRINTS matches what is validated here.

---

## Sprint 18 – Cloud sync implementation

**Goal:** Sync reminder, category, and checklist data to/from Firebase Firestore; cloud wins on conflict; offline support.

### Data syncs to Firestore on create/update/delete

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Sign in with Google. Create a new reminder (title, date/time), save. | Reminder is saved locally and appears in the list. |
| 2 | Open Firebase Console → Firestore Database → `users/{your-uid}/reminders`. | A document with the reminder id exists; fields (title, dueAt, type, etc.) match. |
| 3 | Create a tag (category), save. | Tag appears in Tags list. |
| 4 | In Firestore, check `users/{your-uid}/categories`. | A document for the new category exists. |
| 5 | Edit the reminder (e.g. change title), save. | Reminder updates locally. |
| 6 | In Firestore, refresh `users/{your-uid}/reminders`. | The reminder document reflects the new title. |
| 7 | Delete the reminder from the app (or mark complete and delete if enabled). | Reminder is removed from the list. |
| 8 | In Firestore, refresh. | The reminder document is deleted (or no longer present). |

### Data downloads from Firestore on app start

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | From another device or Firebase Console, add or edit a reminder/category under `users/{your-uid}/...`. | Data is in Firestore. |
| 2 | On the first device, force-stop the app and reopen (or sign out and sign back in). | App runs `syncAll` on start. |
| 3 | Open Reminders or Tags list. | The data added/edited on the other source appears locally (cloud wins on merge). |

### Cross-device sync

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Sign in with the same Google account on two devices (or emulator + device). | Both show the main app. |
| 2 | On device A: create a reminder, save. | Reminder appears on device A. |
| 3 | On device B: open or refresh the reminder list (or restart app). | The new reminder appears on device B (syncAll on start or real-time listener). |
| 4 | On device B: edit the reminder, save. | Reminder updates on device B. |
| 5 | On device A: open or refresh the list. | The updated reminder is shown (cloud wins). |

### Offline support

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Turn off network (airplane mode or no Wi-Fi). | App remains usable. |
| 2 | Create or edit a reminder, save. | Save succeeds locally; UI updates. |
| 3 | Turn network back on. | Firestore persistence queues writes; data syncs to cloud. |
| 4 | In Firestore Console (or on another device), verify. | The reminder/category appears or is updated. |

### Conflict resolution (cloud wins)

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | On device A: edit a reminder (e.g. title = "A"). On device B: edit the same reminder (title = "B") and save. | Both devices have different local state. |
| 2 | On device A: reopen app or trigger sync (e.g. pull or restart). | `syncAll` downloads from Firestore; merge uses cloud version (e.g. title = "B" if B uploaded after A). |
| 3 | Verify reminder title on device A. | Shows the cloud version (e.g. "B"). |

### Sync errors handled gracefully

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | With invalid credentials or no network, perform an action that triggers sync (e.g. create reminder and save). | App does not crash; reminder is saved locally. Sync may fail in background; errors can be logged. |
| 2 | Restore network / sign in again and reopen app. | `syncAll` on start can retry; data eventually syncs. |

**Validation:** All tables above pass for the implemented behaviour. Optional: add sync status UI (Sprint 19) and retry on error.

---

## Sprint 19 – Data migration & sync polish

**Goal:** First-time sign-in migration dialog (upload / merge / start fresh), sync status in UI, retry on sync error.

### Migration dialog on first sign-in with local data

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Without signing in, create one or more reminders (or tags) so local data exists (`userId = 'local'`). | Data is stored locally. |
| 2 | Sign in with Google. | Migration dialog appears with title "Dados neste aparelho" and three options: "Fazer upload para esta conta", "Mesclar com dados da nuvem", "Começar do zero". |
| 3 | Choose "Fazer upload para esta conta". | Dialog shows "Sincronizando…"; then dialog closes and main app (reminder list) is shown. Local data is now in the cloud (check Firestore). |
| 4 | Sign out, add local data again (or use another device/account), sign in. Choose "Mesclar com dados da nuvem". | Dialog closes after sync; list shows merged data (cloud wins on conflict). |
| 5 | With local data, sign in and choose "Começar do zero". | Local data is cleared; list shows only what is in the cloud (or empty). |

### No migration dialog when no local data or already migrated

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Fresh install, sign in (no reminders/tags created before sign-in). | No migration dialog; normal sync runs and main app is shown. |
| 2 | After completing migration once for an account, sign out and sign back in. | No migration dialog; sync runs as usual. |

### Sync status and retry

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Sign in and open the Reminder list (Tarefas or Rotinas). | A sync status row appears below the app bar: "Sincronizando…" then "Sincronizado" (or stays Idle if no sync has run). |
| 2 | Simulate sync error (e.g. turn off network, trigger sync; or rely on existing error handling). | Status shows "Erro de sincronização" with "Tentar novamente" button. |
| 3 | Tap "Tentar novamente" (with network restored if needed). | Sync runs again; status moves to Syncing then Synced (or Error if it fails again). |

### Sign out (regression)

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Open Settings, tap "Sair". | User is signed out; login screen is shown. Session does not persist. |

**Validation:** All tables above pass for the implemented behaviour.

---

## Sprint 19.5 – Local-only option

**Goal:** User can use the app without signing in with Google; data stays local only; sync row shows "Apenas neste aparelho"; Sign out returns to login and local data persists.

### Login and local-only entry

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Launch the app when not signed in (fresh install or after sign out). | Login screen is shown with "Melhore", subtitle, **"Entrar com Google"** button and **"Continuar sem entrar"** text button. |
| 2 | Tap **"Continuar sem entrar"**. | App shows main app (Reminder list with bottom nav). No migration dialog. |
| 3 | Open Reminder list (Tarefas or Rotinas). | A status row below the app bar shows **"Apenas neste aparelho"** (no "Sincronizado" or sync status). |
| 4 | Create a reminder and a tag. Save. | Reminder and tag appear in the list; data is stored locally only. |
| 5 | Force stop the app and launch again. | App opens directly on main app (no login screen). Same reminder and tag are shown. |
| 6 | Open **Configurações**, tap **"Sair"**. | App returns to login screen. |
| 7 | Tap **"Continuar sem entrar"** again. | Main app is shown; the same reminder and tag from step 4 are still present (data persisted). |

### Regression: migration when signing in after local data

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | With local-only data (reminders/tags created in steps above), tap "Sair" to return to login. | Login screen is shown. |
| 2 | Tap **"Entrar com Google"** and complete sign-in. | Migration dialog appears (local data exists); user can choose upload, merge, or start fresh. |

**Validation:** "Continuar sem entrar" enters the app with local-only mode; no sync; "Apenas neste aparelho" is visible; Sign out and re-entry preserve data; signing in with Google after local data shows migration dialog.

---

## Sprint 22 – Documentation and release prep *(to be implemented)*

**Goal:** Docs and release readiness.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Hand off to a new developer (or follow docs yourself): open project, read README and `docs/`. | They can understand structure, run the app, and run tests from the docs. |
| 2 | Run any automated tests (if added). | Tests pass. |

**Validation:** Onboarding and tests are possible using the documentation and project as-is.

---

## Sprint 12.2 – Rotina Notification Behavior Development

**Goal:** Rotina notification click navigates to task setup screen; tasks are created as child reminders with checkup notifications.

### Rotina notification and task setup

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Create a Rotina reminder (isRoutine = true) with daily recurrence, schedule it for 1-2 minutes in the future. | Rotina reminder is created and scheduled. |
| 2 | Wait for Rotina notification to fire (or adjust system time). | Rotina notification appears with "Skip day" action button. |
| 3 | Tap the Rotina notification (content area, not actions). | App opens and navigates to **RotinaTaskSetupScreen** showing Rotina title and date. |
| 4 | Tap "Adicionar tarefa" button. | New task input row appears with title field, start time, and checkup frequency fields. |
| 5 | Enter task title, tap start time field, select a time. | Time picker dialog appears; selected time updates the field. |
| 6 | Tap frequency field, select frequency (e.g., 2h). | Frequency picker dialog appears; selected frequency updates the field. |
| 7 | Add another task with different start time and frequency. | Second task row appears. |
| 8 | Tap "Salvar tarefas" button. | Tasks are saved; screen closes and returns to reminder list. |
| 9 | Check reminder list (may need to filter or refresh). | Task reminders appear as child reminders (can verify via database or UI if displayed). |

### Skip day functionality

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | From RotinaTaskSetupScreen, tap "Pular dia" button. | Confirmation dialog appears: "Pular dia? Esta ação avançará a Rotina para o próximo dia. Deseja continuar?" |
| 2 | Tap "Sim" in confirmation dialog. | Rotina dueAt advances to next occurrence; screen closes. |
| 3 | Verify Rotina reminder in database or list. | Rotina dueAt is updated to next occurrence date/time. |

### Task reminder notifications

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Create a task reminder with startTime = 1-2 minutes in future and checkupFrequencyHours = 1. | Task reminder is created and scheduled. |
| 2 | Wait for initial notification at startTime. | Initial notification appears for the task. |
| 3 | Wait for checkup notification (startTime + checkupFrequencyHours). | Checkup notification appears with "Concluído", "Adiar", and "Continuar" actions. |
| 4 | Tap "Continuar" action. | Notification dismisses; next checkup is scheduled for checkupFrequencyHours later. |
| 5 | Wait for next checkup notification. | Another checkup notification appears. |
| 6 | Tap "Concluído" action. | Task reminder status changes to COMPLETED; all alarms cancelled; no more notifications. |
| 7 | Tap "Adiar" action (alternative test). | Snooze logic applies; task is snoozed for default duration. |

**Validation:** Rotina notifications navigate to task setup screen; tasks are created with correct parent relationship; task reminders schedule initial and checkup notifications correctly; skip day advances Rotina to next occurrence; checkup actions work as expected.

---

## Sprint 12.2.1 – Rotina Current Period Tasks and Navigation

**Goal:** Restrict Rotina task creation to only the current period and navigate back to Melhore home page after tasks are saved.

### Current period restriction

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Create a daily Rotina reminder and trigger its notification. Navigate to task setup screen. | Screen shows current period indicator (e.g., "Tasks for: [Today's date]"). |
| 2 | Tap "Adicionar tarefa" and try to set start time to tomorrow (outside current day). | Time picker restricts selection to today only; validation prevents selecting tomorrow. |
| 3 | Set task start time within today's boundaries. | Task is accepted; no validation error. |
| 4 | Try to manually edit task start time to a date outside current period. | Validation error appears; task cannot be saved. |
| 5 | Create a weekly Rotina reminder and trigger its notification. Navigate to task setup screen. | Screen shows current period indicator (e.g., "Tasks for: [Current week date range]"). |
| 6 | Try to set task start time outside current week. | Time picker restricts selection to current week only. |
| 7 | Create a monthly Rotina reminder and trigger its notification. Navigate to task setup screen. | Screen shows current period indicator (e.g., "Tasks for: [Current month date range]"). |
| 8 | Try to set task start time outside current month. | Time picker restricts selection to current month only. |
| 9 | Create a custom recurrence Rotina (e.g., Mon/Wed/Fri) and trigger its notification. Navigate to task setup screen. | Screen shows current period indicator based on custom recurrence pattern. |
| 10 | Try to set task start time outside current custom period. | Time picker restricts selection to current custom period only. |

### Navigation after save

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Create tasks for a Rotina within current period and tap "Salvar tarefas". | Tasks are saved; screen navigates back to **Melhore home page** (reminders list). |
| 2 | Verify navigation. | Back stack is cleared; tapping back button does not return to task setup screen. |
| 3 | Verify tasks were created. | Task reminders appear in the reminders list (may need to filter or check child tasks). |
| 4 | Tap "Pular dia" and confirm skip. | Rotina advances to next occurrence; screen navigates back to **Melhore home page**. |

**Validation:** Tasks can only be created within current period boundaries; time picker restricts selection appropriately; visual indicator shows current period; validation prevents saving tasks outside period; navigation returns to Melhore home page after save; back stack is properly cleared.

**Note:** Period restriction and validation apply to all recurrence types: daily, weekly, biweekly, monthly, and custom (custom uses current week as the period). The date picker disables dates outside the period; the time picker clamps the selected time to period bounds when on boundary dates.

---

## Sprint 12.3 – UI Tabs: Separate Tarefas and Rotinas

**Goal:** Melhores screen has two tabs (Tarefas and Rotinas) below the top app bar; tab selection persists; empty states are tab-specific.

### Tab row and switching

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Open the app and go to the Melhores (reminders) screen. | A tab row appears **below the top app bar** with two tabs: **"Tarefas"** and **"Rotinas"**. |
| 2 | Observe the default tab. | **Tarefas** is selected by default (clear visual distinction: selected tab is highlighted, unselected is not). |
| 3 | Tap the **Rotinas** tab. | Rotinas tab becomes selected; list shows only Rotina reminders (reminders marked as Rotina, excluding child task reminders). |
| 4 | Tap the **Tarefas** tab. | Tarefas tab becomes selected; list shows regular reminders (excluding child task reminders). |

### Tab content filtering

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Ensure you have at least one regular reminder (not a Rotina, not a task) and optionally a Rotina and/or task reminders. | Data is set up. |
| 2 | Select **Tarefas** tab. | List shows only **regular** reminders (non-task, non-Rotina): one-time and recurring melhores that are not Rotinas. Child task reminders and Rotinas do **not** appear. |
| 3 | Select **Rotinas** tab. | List shows only reminders that are **Rotina** (isRoutine) and **not** task reminders. Regular melhores do not appear. |

### Tab persistence

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Select **Rotinas** tab. | Rotinas is selected. |
| 2 | Leave the app (e.g. home button or switch app) and reopen the app (or kill and relaunch). | App reopens on Melhores screen with **Rotinas** tab still selected. |
| 3 | Select **Tarefas**, then leave and reopen again. | **Tarefas** tab is selected after reopen. |

### Empty states

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | On **Tarefas** tab, ensure no reminders (and no active filters). | Empty state shows: "Nenhum melhore ainda" and "Toque em + para adicionar um melhore". |
| 2 | Switch to **Rotinas** tab with no Rotina reminders. | Empty state shows: "Nenhuma rotina ainda" and "Crie um melhore e marque como Rotina para ver aqui". |
| 3 | With active filters applied, ensure no reminders match. | Empty state shows "Nenhum melhore com esses filtros" and "Limpar filtros" (same for both tabs when filters are active). |

### Pending confirmation section

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Have at least one reminder in "pending confirmation" state (ACTIVE, past due, one-time, not snoozed). | Warning section appears when on **Tarefas** tab. |
| 2 | Switch to **Rotinas** tab. | Pending confirmation warning section is **not** shown on Rotinas tab. |
| 3 | Switch back to Tarefas. | Warning section appears again if pending reminders exist. |

**Validation:** Tab row is visible and has two tabs with clear selected/unselected styling; Tarefas shows only regular reminders (excludes child tasks and Rotinas); Rotinas shows only Rotina (non-task) reminders; selected tab persists across app restarts; empty states show correct copy per tab; pending confirmation section appears only on Tarefas tab.

---

## Sprint 20 – Bug Fixes and UI Improvements

### Tarefas Visibility Fix

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Create a Rotina reminder (e.g., daily Rotina). | Rotina is created. |
| 2 | Navigate to Rotina task setup screen and add tasks for the current period. | Tasks are created successfully. |
| 3 | Navigate back to the Melhores screen and select **Tarefas** tab. | Tasks appear in the Tarefas list immediately, even if their scheduled start time is in the future. |
| 4 | Verify tasks are not shown in **Rotinas** tab. | Tasks do not appear in Rotinas tab (only Rotina reminders appear). |

### Rotina Notification Prevention

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Create a daily Rotina reminder with a notification time (e.g., 9:00 AM). | Rotina is created and scheduled. |
| 2 | Add tasks for today via the task setup screen. | Tasks are created for the current period. |
| 3 | Wait for the Rotina notification time (or manually trigger if testing). | Rotina notification does **not** appear because tasks already exist for the current period. |
| 4 | Verify Rotina advances to next occurrence. | Rotina's `dueAt` is updated to the next occurrence (e.g., tomorrow). |
| 5 | Create a weekly Rotina and add tasks for the current week. | Tasks are created. |
| 6 | Wait for Rotina notification time. | Rotina notification does **not** appear because tasks exist for the current week. |
| 7 | Delete all tasks for the current period and wait for next Rotina notification. | Rotina notification appears normally (no tasks exist). |

### Sync Banner Overlay

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Open the app and navigate to Melhores screen (while signed in). | Sync status appears as overlay strip banner at the top, covering part of the TopAppBar without pushing content down. |
| 2 | Observe sync status during sync. | Banner shows "Sincronizando…" with sync icon. |
| 3 | Wait for sync to complete. | Banner shows "Sincronizado" with check icon. |
| 4 | If sync error occurs, observe banner. | Banner shows "Erro de sincronização" with error icon and "Tentar novamente" button. |
| 5 | Verify content below is not pushed down. | Content (tabs, list) remains in the same position; banner overlays on top. |

### Filter and Sort Icons

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Navigate to Melhores screen. | Filter icon (with badge if filters are active) and sort icon appear in TopAppBar actions. |
| 2 | Tap the **filter icon**. | Bottom sheet opens with filter options (tags, priority, date, status, group-by). |
| 3 | Apply filters (e.g., select a tag, set priority). | Filters are applied; filter icon shows badge indicator. |
| 4 | Close bottom sheet and observe filter icon. | Filter icon is tinted (primary color) and shows badge when filters are active. |
| 5 | Tap the **sort icon**. | Bottom sheet opens with sort options (Por data, Por prioridade, Por título, Por criação, Mais recentes). |
| 6 | Select a sort option. | Sort is applied; bottom sheet closes automatically. |
| 7 | Verify filter/sort functionality. | Filtering and sorting work as before; UI is more compact. |

### Hourly Pending Confirmation Check

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Create a reminder with a past due date (non-recurring, ACTIVE status). | Reminder is created and is in "PENDENTE CONFIRMAÇÃO" state. |
| 2 | Close the app completely (or wait 1 hour). | App is closed. |
| 3 | Wait 1 hour (or trigger worker manually for testing). | Notification appears: "Melhores pendentes de confirmação" with list of reminder titles. |
| 4 | Tap the notification. | App opens to Tarefas tab showing pending confirmation warning section. |
| 5 | Complete or cancel the reminder. | Reminder is no longer pending. |
| 6 | Wait another hour. | Notification does **not** appear (no pending reminders). |

### "Melhore" Branding Capitalization

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Navigate through all screens and observe UI text. | All references to "Melhore" (the reminder type) have capital 'M': "Adicionar Melhore", "Novo Melhore", "Salvar Melhore", "Nenhum Melhore ainda", etc. |
| 2 | Verify app name remains unchanged. | App name "MelhoreApp" remains as-is (not changed). |
| 3 | Check empty states, dialogs, and error messages. | All UI strings consistently use "Melhore" with capital M. |

**Validation:** Tarefas tab includes task reminders; Rotinas do not notify when tasks exist; sync banner overlays without pushing content; filter/sort icons open bottom sheets; active states are visible; hourly worker notifies for pending confirmation; all UI strings use "Melhore" with capital M.

---

## Sprint 21.1 – Rotina: ao pular dia, não notificar até o dia seguinte *(validated)*

**Goal:** When user skips the day on a Rotina, no further notifications until the next occurrence (e.g. next day). When user does not skip (dismiss or open without action), 30‑min notifications continue.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Create a daily Rotina and wait for its notification (or trigger it). | Rotina notification appears with "Pular dia" action. |
| 2 | Tap **Pular dia** on the notification (or open task setup from the notification and tap "Pular dia" there). | Rotina advances to next occurrence; notification is dismissed; all alarms for that Rotina are cancelled and only the next occurrence is scheduled. |
| 3 | Wait 30 minutes (after having tapped "Pular dia"). | No notification arrives (skip day stopped 30‑min repeat). |
| 4 | Wait until the next occurrence (e.g. next day at same time). | Rotina notification appears only at the next occurrence. |
| 5 | Create a daily Rotina, wait for notification, then **dismiss** it (or open and leave without skipping/adding tasks). Wait 30 minutes. | Another notification arrives (Rotinas get 30‑min repeat until user skips day or adds tasks). |

**Validation:** After skip day, no 30‑min notification; next notification only at next scheduled occurrence. If user does not skip the day, 30‑min notifications continue. See [SPRINTS.md](SPRINTS.md) Sprint 21.1. **Status:** Manual testing completed; sprint validated.

---

## Sprint 21.2 – PENDENTE CONFIRMAÇÃO para todas as tarefas e a cada hora

**Goal:** All tasks (including Tarefas from Rotinas) trigger PENDENTE CONFIRMAÇÃO notification; notification repeats every hour until user confirms.

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Create a Rotina, add tasks for today with a due time in the past. | Tasks are created. |
| 2 | Open Melhores → Tarefas tab. | Pending confirmation warning section lists those tasks (and any other one-time past-due reminders). |
| 3 | Close app and wait 1 hour. | Notification "Melhores pendentes de confirmação" appears (worker runs hourly). |
| 4 | Complete or cancel one pending reminder. | That item no longer appears in pending section; next hourly run does not include it. |
| 5 | (Optional) Create a new task (Rotina or one-time) with due time in the past; leave app in background. | Within up to 60 minutes, a pending-confirmation check can run (alarme at dueAt+60min or next hourly worker), and "Melhores pendentes de confirmação" notification may appear. |

**Validation:** Tarefas from Rotinas appear in PENDENTE CONFIRMAÇÃO; hourly notification continues until user confirms. See [SPRINTS.md](SPRINTS.md) Sprint 21.2. **Status:** Manual testing completed; sprint validated.

---

## Sprint 21.3 – Ícone do app

**Goal:** App displays a dedicated app icon (replace generic icon).

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Install the app and view the launcher (or app drawer). | App shows the new MelhoreApp icon (not the generic placeholder). |
| 2 | Check icon on task switcher / recent apps. | Same icon is displayed. |
| 3 | Build and install from Android Studio. | Build succeeds; icon appears correctly. |

**Validation:** Dedicated icon visible; adaptive icon where supported (API 26+). See [SPRINTS.md](SPRINTS.md) Sprint 21.3. **Status:** Implemented; validate manually after install.

---

## Sprint 21.4 – Período "Finais de semana" *(validated)*

**Goal:** Recurrence option "Finais de semana" (Saturday and Sunday only).

| Step | Action | Expected behaviour |
|------|--------|--------------------|
| 1 | Create a new reminder; open recurrence dropdown. | Option "Finais de semana" is available. |
| 2 | Select "Finais de semana", set time, save. | Reminder is saved; list shows "Finais de semana" as recurrence label. |
| 3 | Set reminder for a weekday (e.g. Monday 10:00). | Next occurrence is calculated to Saturday (or next weekend day) at same time. |
| 4 | Create a Rotina with "Finais de semana". | Rotina period for task setup is current weekend (Sat–Sun) when today is Sat/Sun, or next weekend when today is Mon–Fri. |

**Validation:** WEEKENDS recurrence fires only on Saturday and Sunday; UI shows "Finais de semana". See [SPRINTS.md](SPRINTS.md) Sprint 21.4. **Status:** Manual testing completed; sprint validated.

---

## Troubleshooting

- **Notifications do not fire at due time (especially on Android 14+):** Exact alarms require the `SCHEDULE_EXACT_ALARM` permission. On Android 12+ (API 31) the app may need the user to grant "Alarms & reminders" or use exact alarms in **Settings → Apps → MelhoreApp → Alarms & reminders** (wording may vary by device). If the permission is denied, alarms may be inexact and delivery can be delayed.
- **No notification at all:** Ensure **notification permission** (`POST_NOTIFICATIONS`) is granted when prompted (Android 13+). Check **Settings → Apps → MelhoreApp → Notifications** and ensure notifications are enabled for the app.
- **Reminders missing after reboot:** Ensure `BootReceiver` is not disabled by the user (Settings → Apps → MelhoreApp → Battery or startup) and that the device has completed boot before opening the app; the receiver reschedules from the database on `BOOT_COMPLETED`.
