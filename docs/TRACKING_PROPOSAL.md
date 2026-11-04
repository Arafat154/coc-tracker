# Easier tracking — proposal only

The foreground clipboard Review/Dismiss suggestion is implemented in v1.3.0. The inventory/start-upgrade workflow and silent imports remain proposals.

## Implemented: optional clipboard import suggestion

1. Add an off-by-default “Look for village exports when I open the app” preference.
2. Check only while the app is visible and focused. Android restricts background clipboard access and may show a clipboard access notice. No background monitor or accessibility service.
3. Accept only bounded text matching the export schema. Parse off the UI thread without displaying the entire JSON in an editor.
4. Match the player tag, timestamp. Ignore already-imported content; warn about older exports; never silently replace a different village.
5. Show “New export found for [village] — Review / Dismiss”. Review uses the existing import summary and commit step.
6. Nothing is changed until Import village is confirmed. Manual Forge timers remain separate until their export representation has been verified. Undo snapshots are a possible later enhancement.

This saves paste taps; the user still exports from Clash after in-game changes. A later automatic-import preference could apply only to recognized, strictly newer exports for saved accounts, with an Undo message. It should follow experience with the review-first version.

## Later: inventory and Start upgrade

The app currently stores ongoing jobs, not a complete inventory. This is a larger addition:

1. Save item IDs, levels, counts, village type and crafted module hierarchy from the full export. Keep duplicates (e.g. multiple Cannons at different levels) individually selectable.
2. Add search and a “Start upgrade” action. Choose the item and slot (regular/Goblin) and confirm the target level and remaining duration. Keep current level unchanged while upgrading; offer explicit Complete/Cancel/Undo.
3. Start with user-confirmed durations. Automatic estimates need an accurate, versioned duration catalogue and settings for Gold Pass, events and temporary boosts. Discounts already present in an imported timer must not be applied twice.
4. Potions and helper assignments remain explicit actions. The app cannot infer a potion used inside the game. Record a use or import a fresh export.
5. Reconcile the next export against manual changes, displaying differences before replacement. Never label stale inventory as fully synchronized.

Next possible step: inventory with confirmed durations. Treat fully inferred durations/event adjustments as a separate phase.

Reference: [Android clipboard behavior](https://developer.android.com/develop/ui/views/touch-and-input/copy-paste).
