# MobLifeIndicator

Paper plugin for sv-01 that shows a floating heart bar above a mob after it
takes damage, so players can see how much health it has left without a
scoreboard or a client mod. The bar hides itself 5 seconds after the last
hit, and the mob's original name (or lack of one) is preserved throughout.

## What it does

- Any `LivingEntity` except players and armor stands can show a heart bar.
- On damage, the mob's custom name becomes a row of hearts: full (red),
  half (gold), empty (dark gray). Up to 10 heart slots are used; mobs with
  more than 20 max HP are scaled proportionally so the bar still fits in 10
  hearts (e.g. a 300 HP boss uses 30 HP per heart).
- Healing/regen while the bar is visible refreshes the displayed hearts, but
  only damage resets the 5-second hide timer.
- Hiding restores the mob's original custom name and name-visibility exactly
  as they were before the bar was ever shown (including "no custom name" and
  "name set but not visible").
- The original name/visibility is restored on hide, on death (before any
  other plugin reads the death message), and on plugin disable.
- Crash safety: the original state is saved in the entity's persistent data
  container while a bar is showing. If the server stops uncleanly while a
  bar is up, it is restored automatically the next time that entity loads.

## Build

```sh
./gradlew build
```

The plugin jar is produced at `build/libs/mob-life-indicator-0.1.0.jar`.

Run the tests on their own with:

```sh
./gradlew test
```

## Deploy

1. Copy the built jar into the server's plugin folder:
   ```sh
   cp build/libs/mob-life-indicator-0.1.0.jar data/plugins/
   ```
2. Restart the Minecraft container:
   ```sh
   docker compose restart mc
   ```

Never use `/reload` to load or update this plugin — Paper plugin reloads are
unsupported and can corrupt plugin/world state; always restart the container.

## Known limitations

- **Renaming a mob while its bar is showing**: if a player uses a name tag
  on a mob whose heart bar is currently visible, vanilla immediately
  overwrites the displayed name with the tag text (the bar reappears on the
  next hit, or is restored correctly when it eventually hides). The
  *restored* name after hide/death is correctly the new tag name, not the
  pre-rename one, but there's a brief window (until the next hit) where the
  tag text is shown instead of the heart bar.
- **Attribute-less entities**: if a `LivingEntity` somehow has no
  `MAX_HEALTH` attribute registered (not expected for normal mobs), its
  current health is used as its max health, so it always renders as full.
- Bukkit-facing logic (the listener and tracker) is verified by compiling
  against the pinned `paper-api` and by manual in-game testing, not by unit
  tests — MockBukkit is intentionally not used. The pure heart-bar math in
  `HeartBar` has full unit test coverage (`./gradlew test`).
