# UI Transition & Alignment Fix

The goal is to fix the jumping animation and UI leftovers when navigating to the Usage Limits screen.

## Root Causes
1. **Nested Scaffolds & AnimatedContent**: `MainScreen` uses `AnimatedContent` to switch between tabs. The Usage Limits tab (`AppLimitsScreen`) now handles its own `TopAppBar` and `Scaffold`, while other tabs rely on the `MainScreen`'s `Scaffold`. This difference in hierarchy causes the "leftover" UI (the `MainScreen` TopAppBar doesn't disappear instantly or overlaps) and "jumping" (layout shifts as the new Scaffold initializes).
2. **Padding Desync**: `MainScreen` padding vs. `AppLimitsScreen` custom padding logic.

## Proposed Strategy
1. **Unify TopAppBar Control**: Move the "Usage Limits" custom header logic back into `MainScreen`'s `TopAppBar` slot, OR make `MainScreen`'s TopAppBar completely reactive to the current tab's needs.
2. **Refactor AppLimitsScreen**: Remove the internal `Scaffold` for the main list view, keeping it only for sub-views (Picker/Edit).
3. **Synchronize Transitions**: Ensure `AnimatedContent` handles the crossfade/slide without conflicting with the TopAppBar state.

## Verification
- Manual check of tab transitions.
- Verify TopAppBar title alignment across all 4 tabs.
