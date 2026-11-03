# Validation scope

## Automated checks

`./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`

- 11 tests for elapsed work, short upgrades, potion stacking, early cancellation, boost expiration, research/pet exclusion, and progress limits.
- 9 import tests including a real-export-shaped anonymized fixture, unknown IDs, validation errors, stale timestamps, Goblin slots, research boost import, and a loop over Town Hall levels 1–18.
- 15 boost/format tests for category isolation, potion/snack overlap, Clock Tower queues, additive helpers, opt-in repeated sessions, event windows, boost serialization, the supplied Apprentice export shape, old snapshots, and finish-time boundary checks.
- 4 countdown precision tests covering clock seconds, minute/hour/day carries, subsecond rounding, completed timers, and minute-based word formats.
- 10 reminder planner tests covering future-only arming, one-time completion delivery, potion/timer rescheduling, deletion, category filtering, old snapshot imports, missed alarms after reboot, and separate village keys.

The TH1–18 test varies the Town Hall field in a fixed fixture. It verifies parser flexibility; it does not validate all possible real village layouts, units, special upgrade types, or future game changes.

## Device checks

Android 16 / API 36 x86_64 emulator:

- Install/update and launch the app with preserved data.
- Paste → preview → import an export, including Town Hall artwork and 13 upgrade timers.
- Home Village and Builder Base tab selection.
- Android text-share → preview → import using the supplied Apprentice export, preserving its level-8 assignment and original timestamp.
- JSON document picker → preview, with cancel preserving the current village.
- Pet/Research Potion recording, persisted boost windows, repeat-helper preference, and countdown format choices.
- Clock countdowns observed advancing while the app was in the background. Active Chronometers can prevent UI Automator from reaching idle, so widget ticking is also checked with emulator screenshots.
- README screenshots captured directly from the API 36 emulator with anonymized demo data.
- Pin the widget with imported data, artwork, scrolling rows, and live countdowns.
- Notification permission and exact-alarm settings flows.
- A scheduled ten-second notification arrived while the app was in the background.
- A one-minute upgrade created through the UI survived an emulator reboot and produced its completion notification without reopening the app. The completed timer was removed from the armed schedule afterward.
- Notification dismissal through its action button.
- A scheduled alarm-mode notification used the repeating alarm flag; **Stop alarm** canceled it before its timeout. The emulator was started with host audio disabled, so physical speaker/vibration and Do Not Disturb behavior still require a real-device check.

Further Android versions, physical devices, manufacturer battery policies, accessibility font scales, rotation states, and multiple launcher implementations still need coverage before claiming broad production stability.

## Known data limits

- The app works from an imported snapshot (paste, file or Android share); it does not observe game activity continuously.
- Builder, Research and Pet Potions, Builder Bite, Clock Tower and Study Soup are modeled. Active Apprentice/Lab Assistant assignments use exported helper data; future repeated sessions are optional forecasts. Other helper types are not modeled.
- The active Apprentice case is checked against a supplied real export. Other boost combinations have synthetic coverage; a real active Pet Potion export is still needed to confirm its export key. Manual Pet Potion recording is available.
- Event multipliers are manual forward-looking adjustments, off by default. Imported timers may already include event/Gold Pass duration discounts; applying those again would shorten estimates incorrectly.
- Unknown IDs retain their timers under editable placeholder names and generic artwork.
- Building art is illustrative and may show a different level from the exact upgrade.
- Progress starts from the remaining work at import, rather than a complete upgrade duration database.
- Notification delivery can be delayed by Android or device restrictions. Alarm tones respect Android sound settings and stop automatically after one minute.


## Android 1.2.0

- 65 unit tests: the existing 49 plus account snapshot migration/isolation/selection/removal, compact-grid capacity/overflow, and boosted next-research summaries.
- Debug APK build and lint pass on JDK 21 / SDK 36.
- API 36 emulator: verified migration retains all 13 existing timers and boost/helper settings; imported a second TH18 test village; refreshed the same tag without duplication; switched both ways while preserving the other snapshot.
- OLED uses #000000 for the app and widget surfaces. The detailed and compact layouts remain selectable. Progress is relative to work remaining at import, as before.
- Compact grid uses portrait/landscape widget dimensions, fixed-size portraits and a bounded bitmap payload. Overflow is explicit. Different launcher sizing, font scaling and older Android RemoteViews implementations still need real-device feedback.
- Guardians Logger, Longshot and Smasher now have bundled source artwork. Missing spell/trap artwork was added where present in the upstream catalogue. Helper Hut, Crafting Station and a few other unmapped items still use the existing fallback because matching art was not available in that catalogue.
- Legacy single-village preferences are retained as a migration fallback; the new versioned account book is authoritative after migration. Reminders aggregate accounts with independent identifiers.

- Final emulator checks: compact Logger tap opens the editor, an editor survives background/resume and saves safely, notification-style intents select the correct village, and the reminder schedule contains independent keys for two villages. Resizing adapts the visible grid and overflow. Temporary demo accounts are removed afterward and original data/preferences restored.

## Android 1.2.1

- 69 unit tests pass, including cycling/wrapping, stable ordering after reload, the one-village case and removal.
- A broadcast-backed switch button is shared by compact and detailed widgets; it only appears with multiple saved villages. Cycling changes the selected account without editing snapshots.
- Debug build and lint pass. Widget switching refreshes all widgets and keeps reminder scheduling for all accounts.
