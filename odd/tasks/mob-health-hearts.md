# Feature: mob-health-hearts

## Objective
Paper plugin for sv-01 that shows a floating heart bar (custom name) above a mob after it takes damage, hidden again ~5 seconds after the last hit.

## Problem / Why
Players cannot see how much health a mob has left. The bar must only appear on damaged mobs to keep the world clean and the server light (4 GB, arm64).

## Scope
- Mobs = any `LivingEntity` except players and armor stands.
- Show on damage, refresh on heal/regen while visible, hide 5 s after the last damage.
- Hearts: full `❤` (red), half `❤` (gold), empty `❤` (dark gray). Max 10 heart slots; mobs with more than 20 max HP are scaled proportionally to 10 slots.
- Preserve the mob's original custom name and name visibility (name tags, named pets) and restore them on hide, death and plugin disable.
- Crash safety: original state saved in the entity PDC so mobs left with a stale bar (server crash) are restored on entity load.

## Constraints (from sv-01 docs/plugin-development.md)
- Paper 26.2, `io.papermc.paper:paper-api:26.2.build.129-stable` (pinned), Java 25, `api-version: '26.2'`.
- Deploy: copy jar to `data/plugins/`, `docker compose restart mc`. Never `/reload`.

## TDD
- Mode: strict (source: global session config "Strict TDD Mode: enabled").
- Runner: `./gradlew test` (JUnit 5). Pure logic tested without a server; Bukkit glue verified by build + manual in-game check.

## Tasks
- [x] T1 Scaffold Gradle build (wrapper, paper-api, JUnit 5, plugin.yml, empty main class). Route: delegated (writer trigger: 2+ files).
- [x] T2 `HeartBar` pure renderer (health/maxHealth -> hearts Component) with tests. Route: delegated.
- [x] T3 Damage listener + display tracker (show/refresh/hide timer, name preservation, PDC restore, death/disable cleanup). Route: delegated.
- [x] T4 README with build/deploy instructions. Route: delegated.

## Acceptance criteria
- `./gradlew build` produces a jar; `./gradlew test` passes.
- Hitting a mob shows hearts; after 5 s without hits the original name returns.
- Name-tagged mobs keep their name after the bar hides.

## Delivery
Strategy: ask-on-risk. Forecast: ~350 authored lines.

## Progress / Evidence
- T1: Gradle scaffold (wrapper 9.7.1, paper-api 26.2.build.129-stable, JUnit 5, plugin.yml, empty JavaPlugin main class). `./gradlew build` green. Commit: 532953e (`build: scaffold paper plugin gradle project`).
- T2: `HeartBar` pure renderer, TDD strict. RED observed (compile error: `cannot find symbol class HeartBar` before implementation existed). GREEN after implementation: 11/11 tests pass (`./gradlew test`), covering all cases from the spec (full/partial/very-low/fractional/zero health, >max clamp, 4-slot and 10-slot scaling, 5/5 split, zero and negative maxHealth). Commit: 430d6a0 (`feat: add heart bar renderer`).
- T3: `HealthDisplayTracker` (PDC-backed original name/visibility capture and restore, per-entity `EntityScheduler` hide timer at 100 ticks, crash-recovery restore, disable-time restore-all, name-tag rename fixup) + `DamageListener` (`EntityDamageEvent` at MONITOR computing post-hit health as `getHealth() - getFinalDamage()` rather than a next-tick read, to avoid a one-tick stale-health flash and an extra scheduled task per hit; `EntityRegainHealthEvent` refresh only if tracked; `EntityDeathEvent` restore before any MONITOR-priority observer of the death message; `PlayerInteractEntityEvent` + NAME_TAG capturing the renamed name one tick later; `EntityAddToWorldEvent` crash recovery). Verified by `./gradlew build` (compiles against the pinned paper-api and passes existing tests); no Bukkit mocking used per design note ("Tracker/listener logic that touches Bukkit is verified by compile"). Entity eligibility (`!Player && !ArmorStand`) kept as a small static method in the listener, per design note. Known limitation documented in README: renaming a tracked (bar-visible) mob with a name tag briefly shows the tag text instead of the bar until the next damage tick, though the *restored* name after hide/death is correctly the new tag name. Commit: 193896f (`feat: show hearts above damaged mobs`).
- Fix (still under T3, before README): heal/regen was reusing `show()`, which reset the 5s hide timer on every heal — but the spec ties the timer to "the last damage" only. Split `HealthDisplayTracker.show()` (damage: reschedules timer) from a new `refresh()` (heal/regen: updates displayed hearts only, no timer touch); `DamageListener.onRegainHealth` now calls `refresh()`. `./gradlew build` green after the change. Commit: e38c7bc (`fix: keep heal refresh from resetting the damage hide timer`).
- T4: README added (what it does, `./gradlew build`/`./gradlew test`, deploy via `cp` to `data/plugins/` + `docker compose restart mc` (never `/reload`), known limitations: name-tag rename display window, attribute-less entity fallback, no-MockBukkit testing approach). Commit: (recorded after commit below).

## Next step
None — T1-T4 complete. Final verification results recorded below; awaiting user review before any push (not requested).
