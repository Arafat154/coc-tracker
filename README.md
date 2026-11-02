# Clash Ledger

An offline Android companion for Clash of Clans upgrade timers. A compact home screen widget keeps builders, laboratory research, pets, and Builder Potion boosts in view.

## Features

- A redesigned native dashboard with offline Clash of Clans artwork, Town Hall overview, builder status cards, and an active potion countdown.
- Illustrated upgrade cards, Home Village / Builder Base tabs, and a matching resizable home screen widget.
- **Import JSON**: paste an in-game village export to sync active upgrades and Builder Potion timing, with a review before replacement.
- Add, edit, complete, and remove upgrade timers, with target levels and finish dates.
- Home Village and Builder Base timers; Goblin builder and research slots stay distinct.
- Builder availability, laboratory and pet sections, progress bars, live widget countdowns.
- Record Builder Potions: **10× builder speed for one hour**, with duration stacking.
- Correct an active potion's remaining time or end it early without losing work already earned.
- Local storage, no account, network permission, ads, or telemetry.
- Optional completion reminders, category controls, vibration, notification sounds, and dismissible alarm tones. Android permissions are requested when you enable the feature.
- Android 8.0+ (API 26). Android 12+ uses direct RemoteViews collections; older versions use a RemoteViewsService.

## Try it

Build the project or install the debug APK from the GitHub Actions artifact:

1. Open Clash Ledger and tap the village name to set its name and builder capacity.
2. Tap the gold **Import village JSON** button in the village card. In-game, copy Settings → More Settings → Data Export, then tap **Paste from clipboard**, **Preview import**, and **Import village**. You can also use **Add upgrade** for manual timers.
3. Tap the four-square widget icon at the top, or **Take your village home** below the queue, and accept your launcher's pin prompt. Alternatively long-press the home screen, open Widgets, and choose Clash Ledger.
4. A pasted export imports `boosts.builder_boost` automatically. After using another Builder Potion **in the game**, import a fresh export or tap **Record a potion**. Recording a potion does not use an item in-game.
5. Tap a timer to correct it. Enter zero time to mark it ready. Long-press to remove it.

JSON import replaces the saved snapshot after review; pasting again does not add duplicate timers or stack the same potion twice. It uses the export timestamp, so an old paste does not restart timers. The preview reports snapshot age, categories, potion time remaining, and unsupported boosts. Your export is parsed on-device and is not uploaded. New actions in-game require a fresh paste or a manual correction.

There is no game login, API integration, or continuous game sync. For manual timers, enter work at normal speed; do not enter a potion-adjusted finish estimate as normal-speed work. Only home village builder timers receive Builder Potion effects. Research/Pet Potions, Builder's Apprentice, Builder Bite, Clock Tower boosts, and event multipliers are not simulated. Recognized active unsupported boosts produce an import warning. Unknown game item IDs retain their timer under an editable placeholder name. Enter any event-discounted normal-speed duration shown in-game yourself.

## Build

Use JDK 17 or 21, Android SDK platform 36, and Android SDK Build Tools. Android Studio can install the SDK and generate `local.properties`; on the command line set `ANDROID_HOME` to your SDK directory.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On Windows, use `gradlew.bat`. The checked-in wrapper downloads Gradle 8.13 and verifies its SHA-256 checksum. The project uses Android Gradle Plugin 8.13.2 and Java 17 source compatibility. Release builds require your own signing configuration; never commit signing keys.

## Timer behavior

The model stores normal-speed work and a timestamp. During a potion, each elapsed minute removes ten minutes of builder work. Finish estimates account for the return to normal speed when the potion ends. For example, a 24-hour upgrade with a new one-hour potion finishes in 15 real hours, saving nine hours. Short upgrades finish during the boost rather than receiving a flat nine-hour subtraction.

Before starting, extending, correcting, or ending a boost, the app settles elapsed work at the previous rate. New upgrades use only the boost time still available. The tests cover stacking, early cancellation, short upgrades, boost expiration, and exclusion of lab/pet timers.

Widget countdowns run inside the launcher. Progress bars and status labels refresh on app changes, the refresh button, Android's periodic widget update (about 30 minutes), and a best-effort alarm at the next timer/boost boundary. Android may delay updates in Doze or under battery restrictions. A countdown can temporarily pass below zero or a boost label remain stale until the next refresh; the absolute finish date is also displayed. The widget itself uses best-effort refreshes. Optional user-enabled reminders can use exact alarms with Android's Alarms & reminders permission; no foreground service is used. Editing the device clock changes wall-clock-based remaining work.

## Alarms and notifications

Open the gear icon → **Alarms & notifications**. Reminders start off until you enable them.

- Choose quiet notifications, notification sound, or an alarm tone that repeats until dismissed or automatically stops after one minute.
- Choose vibration and individual categories: Builders, Laboratory, Pets, Builder Base, Star Laboratory, and Builder Potion expiry.
- Allow notifications on Android 13+. The screen shows if notifications are blocked.
- **Precise alarm timing** opens Android's Alarms & reminders settings on Android 12+. Without this access, the app uses inexact alarms and explains that delivery may be delayed.
- **Sound & notification settings** opens the active Android notification channel, where you can change its tone and behavior. Android volume and Do Not Disturb settings remain in effect.
- **Test reminder in 10 seconds** lets you verify delivery. Exact permission is needed for the ten-second timing to be reliable.

Only timers that are still running when reminders are enabled are armed. Already-completed imports do not generate a burst of alerts. Changing a timer or potion reschedules reminders; deleting a timer or disabling its category cancels it. Imports replace the schedule. Pending reminders are restored after reboot or app updates; overdue armed completions can be delivered after startup. Android force-stop suspends app delivery until you reopen it. If exact-alarm access is revoked, reopen the app to schedule the fallback alarms.

Alarms are local estimates based on your saved export and supported boosts. New actions in-game require a fresh import. Other potion/snack/helper effects are not simulated, so their finish estimates need correction.

## Validation scope

Thirty unit tests cover timer math, import validation, Town Hall levels 1–18, and reminder planning. Town Hall coverage is a synthetic parser test using the same export shape, not proof that every real village and game feature has been exercised. The app has been tested with an actual TH16 export on Android 16. See [validation notes](docs/VALIDATION.md) for device checks and remaining gaps.

## Project layout

- `Upgrade.java`: Android-independent work and boost math.
- `Ledger.java`: local persistence, snapshot replacement, and boost changes.
- `VillageImport.java`: validated export parsing, timestamp anchoring, and boost import.
- `app/src/main/assets/item_names.json`: offline item ID/name lookup.
- `MainActivity.java`: dashboard, import, village and reminder settings, and widget pinning.
- `ReminderPlan.java`: Android-independent scheduling and duplicate prevention.
- `Reminders.java` / `ReminderReceiver.java`: notification channels, alarm delivery, and reboot restoration.
- `UpgradeWidget.java`: widget rendering, interactions, and boundary refresh scheduling.
- `WidgetRowsService.java`: shared row rendering and the pre-Android 12 collection adapter.
- `app/src/test/`: timer calculation tests.

## Validation and contributions

Run the build, tests, and lint command above before opening a pull request. The redesigned dashboard is checked on an Android 16 emulator, including JSON import and Home Village / Builder Base navigation. Physical-device testing across launchers remains useful, especially widget resizing, persistence after reboot, and battery restrictions. Add regression tests for changes to timer math. Import regression tests use an anonymized real-export-shaped fixture, plus the bundled item name lookup. Supercell artwork is bundled for offline display. See [artwork sources and terms](docs/ARTWORK.md); game images are not covered by the source code's MIT license.

## License and attribution

MIT; see [LICENSE](LICENSE). The Gradle wrapper is redistributed under Apache-2.0; see [NOTICE](NOTICE).

This is an unofficial fan project, not affiliated with, endorsed, sponsored, or specifically approved by Supercell. Clash of Clans and its trademarks belong to Supercell. See [Supercell's Fan Content Policy](https://supercell.com/en/fan-content-policy/).

Builder Potion behavior reference: [Supercell support — Magic Items & The Trader](https://sc-clashofclans.zendesk.com/hc/en-us/articles/49529083243291-Magic-Items-The-Trader). Widget update behavior: [Android Developers — Advanced widgets](https://developer.android.com/develop/ui/views/appwidgets/advanced).
