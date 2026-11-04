# Clash Ledger 1.3.0

All improvements since the published 1.2.1 release are included here.

## Tracking and artwork

- Import crafted-defense modules as individual builder jobs, with boosts, helpers, reminders and inherited Goblin flags.
- Add manual Forge / Auto Forge timers with Capital Gold artwork, widgets and reminders. Regular Forge shares Home Village builders and uses Builder Potion speedups; Auto Forge uses neither. Manual Forge jobs survive re-import without restarting.
- Add/edit Goblin builder and researcher jobs separately from normal slots, with capacity checks.
- Fix Builder Base builder occupancy and Next free when switching tabs. Include normal Forge jobs in Home builder availability.
- Correct village-specific artwork for shared building names and show Builder Bite and other boosts with their own icons.
- Import an optional village name while preserving an existing custom name.

## Imports and reliability

- Keep large pasted JSON outside the visible text layout, showing an Export loaded summary. Clipboard-button, native-paste and keyboard-paste paths preserve the complete export for review.
- Optional clipboard suggestions when opening the app: Review / Dismiss, tag matching, duplicate suppression, older-export warnings and remembered dismissals. Off by default; enable in Settings → Import & update preferences. Import village remains the confirmation step.
- Local Crash diagnostics includes app/device/Android versions and stack frames. Reports exclude exports, player tags and exception messages; sharing requires Copy report.

## Reminders and app updates

- Choose custom notification/alarm sounds, including audio files, with separate choices for each mode.
- Schedule upgrade alerts 0–1440 minutes before completion. Early alerts replace completion alerts; boost expiries are unchanged. Already-passed alert windows do not generate retroactive alerts.
- Automatically check GitHub at most once daily when opening the app, enabled by default with an opt-out. Show Update available for a newer stable release with an uploaded APK. Manual Settings → Check for updates remains available. Download and installation stay user-controlled.

## Known limits

- If the app closes unexpectedly, reopen it and use Settings → Crash diagnostics → Copy report to help investigate. Reports stay local until you choose to share them.
- Crafted module tests use synthetic active timers based on the observed export structure; other export shapes still need verification.
- Automatic Forge import needs a real export containing active Forge jobs. Builder Bite Forge effects are not assumed. Review retained manual Forge timers after in-game changes.
- New game actions still require an export or manual edit. Inventory-based tracking and silent clipboard imports remain proposals.

92 unit tests pass; debug build, lint and Android emulator workflows are covered in [validation notes](VALIDATION.md).

## Updating and privacy

Install this release once to receive future automatic update notices. Installing it over a compatible existing installation keeps your saved villages and settings.

Village data stays local. Internet access is used for app-update checks; requests contain no village data. Android may show a clipboard-access notice when the optional suggestion feature reads the clipboard.
