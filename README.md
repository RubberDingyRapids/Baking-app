# Cookbook

An Android app for cooking and baking recipes. Recipes are built as a
flow chart: each method step takes ingredients (or the result of an earlier
step) and turns them into something new, so the app always knows what is in
the pan. Steps that use different things run side by side as separate lanes,
and a step that uses the results of two lanes joins them back together, so
"cut and fry the chips while the fish cooks" reads the way you'd actually
cook it.

## Features

- Recipe tiles with search across names, descriptions, tags and ingredients.
- Recipe editor: name, description, tag, then ingredients and method.
- Ingredient entry with inline autocomplete for common ingredients and a unit
  switcher (g, kg, oz, lb, ml, l, cup, tbsp, tsp, pieces) that converts amounts
  where it can.
- Flow-chart method editor: chop, mix, whisk, add, melt, fry, boil, simmer,
  bake, roast, wait, serve or a custom action. Serve ends a flow: its result is
  the finished dish. Steps use whole items or a part of
  them; intermediates get an auto-generated, editable name. Independent
  steps are laid out in parallel lanes automatically.
- Cooking steps can carry a time; bake and roast take a temperature (°C/°F)
  and an optional preheat reminder. Wait steps have a timer.
- Overview with recipe scaling and a pinned Start button.
- Cook mode: tick off ingredients, then work through the flow. Every step
  whose inputs are ready is active at once, each with its own timer that
  rings even when the app is in the background. The screen stays awake while
  cooking.
- Sharing: send a recipe as a `.cookbook.json` file (opens straight into the
  app on the other phone), as plain text for people without the app, or as a
  link published to a small JSON store. Export and import the whole library
  as one file for backups.
- Everything is stored on the device as JSON.

## Sharing links and your own server

"Share a link" uploads the recipe to the server set in Settings and hands
you a link. It defaults to [JsonBin](https://jsonbin.io); paste your JsonBin
master key into Settings and it works. The protocol is tiny, so the same
setting can point at a home server instead:

- `POST <server url>` with the recipe JSON as the request body. Reply with
  JSON containing an id, either `{"id": "..."}` or
  `{"metadata": {"id": "..."}}`.
- `GET <server url>/<id>` returns the recipe JSON, optionally wrapped as
  `{"record": ...}`.
- If an API key is set in the app it is sent as the `X-Master-Key` header
  on both requests. Two informational headers are also sent on publish:
  `X-Bin-Name` (the recipe name) and `X-Bin-Private: false`.

The app imports links on the "Import from link" menu item (pre-filled from
the clipboard), and taps on `api.jsonbin.io` links where Android allows it.

## Project layout

- `core/` – pure Kotlin: data model, unit conversions, the flow engine,
  storage, search and autocomplete. Unit tested; builds without the Android SDK
  (`./gradlew -Pbaking.coreOnly :core:test`).
- `app/` – Jetpack Compose UI (Material 3), navigation, timers and
  notifications.

## Installing the latest build

Every push publishes a debug APK to the `dev-latest` pre-release:

https://github.com/RubberDingyRapids/Baking-app/releases/download/dev-latest/baking-debug.apk

Open that link on an Android phone, allow installs from your browser when
asked, and the app installs. Reinstalling over an older build keeps your
recipes.

## Building

Open the project in Android Studio, or run:

```
./gradlew :app:assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/`. The GitHub Actions
workflow in `.github/workflows/android.yml` runs the tests and uploads the
same APK on every push.
