# Validation scope

## Automated checks

`./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`

- 11 tests for elapsed work, short upgrades, potion stacking, early cancellation, boost expiration, research/pet exclusion, and progress limits.
- 9 import tests including a real-export-shaped anonymized fixture, unknown IDs, validation errors, stale timestamps, Goblin slots, unsupported boost warnings, and a loop over Town Hall levels 1–18.
- 10 reminder planner tests covering future-only arming, one-time completion delivery, potion/timer rescheduling, deletion, category filtering, old snapshot imports, missed alarms after reboot, and separate village keys.

The TH1–18 test varies the Town Hall field in a fixed fixture. It verifies parser flexibility; it does not validate all possible real village layouts, units, special upgrade types, or future game changes.

## Device checks

Android 16 / API 36 x86_64 emulator:

- Install/update and launch the app with preserved data.
- Paste → preview → import an export, including Town Hall artwork and 13 upgrade timers.
- Home Village and Builder Base tab selection.
- Pin the widget with imported data, artwork, scrolling rows, and live countdowns.
- Notification permission and exact-alarm settings flows.
- A scheduled ten-second notification arrived while the app was in the background.
- A one-minute upgrade created through the UI survived an emulator reboot and produced its completion notification without reopening the app. The completed timer was removed from the armed schedule afterward.
- Notification dismissal through its action button.
- A scheduled alarm-mode notification used the repeating alarm flag; **Stop alarm** canceled it before its timeout. The emulator was started with host audio disabled, so physical speaker/vibration and Do Not Disturb behavior still require a real-device check.

Further Android versions, physical devices, manufacturer battery policies, accessibility font scales, rotation states, and multiple launcher implementations still need coverage before claiming broad production stability.

## Known data limits

- The app works from a pasted snapshot; it does not observe game activity continuously.
- Builder Potions are simulated. Research/Pet Potions, Builder Bite, helpers, and Clock Tower effects are not simulated.
- Unknown IDs retain their timers under editable placeholder names and generic artwork.
- Building art is illustrative and may show a different level from the exact upgrade.
- Progress starts from the remaining work at import, rather than a complete upgrade duration database.
- Notification delivery can be delayed by Android or device restrictions. Alarm tones respect Android sound settings and stop automatically after one minute.
