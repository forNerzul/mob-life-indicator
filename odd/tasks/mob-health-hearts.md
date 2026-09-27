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
- [ ] T3 Damage listener + display tracker (show/refresh/hide timer, name preservation, PDC restore, death/disable cleanup). Route: delegated.
- [ ] T4 README with build/deploy instructions. Route: delegated.

## Acceptance criteria
- `./gradlew build` produces a jar; `./gradlew test` passes.
- Hitting a mob shows hearts; after 5 s without hits the original name returns.
- Name-tagged mobs keep their name after the bar hides.

## Delivery
Strategy: ask-on-risk. Forecast: ~350 authored lines.

## Progress / Evidence
- T1: Gradle scaffold (wrapper 9.7.1, paper-api 26.2.build.129-stable, JUnit 5, plugin.yml, empty JavaPlugin main class). `./gradlew build` green. Commit: 532953e (`build: scaffold paper plugin gradle project`).
- T2: `HeartBar` pure renderer, TDD strict. RED observed (compile error: `cannot find symbol class HeartBar` before implementation existed). GREEN after implementation: 11/11 tests pass (`./gradlew test`), covering all cases from the spec (full/partial/very-low/fractional/zero health, >max clamp, 4-slot and 10-slot scaling, 5/5 split, zero and negative maxHealth). Commit: (recorded after commit below).

## Next step
T3.
