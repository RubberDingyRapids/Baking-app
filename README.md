# Baking

An Android app for baking and general recipes. Recipes are built as a
flow chart: each method step takes ingredients (or the result of an earlier
step) and turns them into something new, so the app always knows what is in
the bowl.

## Features

- Recipe tiles with search across names, descriptions, tags and ingredients.
- Recipe editor: name, description, tag, then ingredients and method.
- Ingredient entry with inline autocomplete for common ingredients and a unit
  switcher (g, kg, oz, lb, ml, l, cup, tbsp, tsp, pieces) that converts amounts
  where it can.
- Flow-chart method editor: mix, whisk, melt, add, bake, wait or a custom
  action. Steps use whole items or a part of them; intermediates get an
  auto-generated, editable name. Drag to reorder, swipe to delete.
- Bake steps have a time and temperature (°C/°F) and an optional preheat
  reminder. Wait steps have a timer.
- Overview with recipe scaling and a pinned Start button.
- Cook mode: tick off ingredients, then work through the flow one step at a
  time with built-in timers that ring even when the app is in the background.
  The screen stays awake while cooking.
- Everything is stored on the device as JSON, ready for export and sharing
  later.

## Project layout

- `core/` – pure Kotlin: data model, unit conversions, the flow engine,
  storage, search and autocomplete. Unit tested; builds without the Android SDK
  (`./gradlew -Pbaking.coreOnly :core:test`).
- `app/` – Jetpack Compose UI (Material 3), navigation, timers and
  notifications.

## Building

Open the project in Android Studio, or run:

```
./gradlew :app:assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/`. The GitHub Actions
workflow in `.github/workflows/android.yml` runs the tests and uploads the
same APK on every push.
