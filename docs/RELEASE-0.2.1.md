# HollowRealm Tweaks 0.2.1

First public release candidate of **HollowRealm Tweaks** (`hollowrealm_tweaks`), maintaining all data-layer fixes from `0.1.0` and `0.2.0` while hardening multiplayer compatibility, resource enumeration, and startup reload caching.

## What's New in 0.2.1

- **Multiplayer metadata (`mods.toml`)**: Added `displayTest="IGNORE_ALL_VERSION"` (preventing false `MATCH_VERSION` rejections for server-data-only installs) and widened Forge version range to `[47.4.16, 48)`.
- **Cached override resolution (`HollowRealmTweaks.OverrideResolver`)**: Resolves and hashes target mod JSONs once per JVM process instead of re-reading and re-parsing on every `AddPackFindersEvent(SERVER_DATA)` call (measured **1265.05x** faster in JUnit micro-benchmark: `417.214 ms` → `0.330 ms` over 10,000 resolutions).
- **No-exception missing-resource checks**: `readModResource` now verifies `Files.isRegularFile(resource)` before `Files.readAllBytes`, eliminating `NoSuchFileException` stack-trace warnings when an optional resource is absent.
- **Resource listing & condition-merge fixes**:
  - `CompatPackResources.listResources` now normalizes trailing slashes on `prefix` and handles exact resource paths cleanly.
  - `GunAffixPatch.addScgunsCondition` preserves any pre-existing `"conditions"` array instead of overwriting it.
- **Manifest ledger**: Added `hollowrealm-tweaks-manifest.json` documenting all 3 audited patches and resource fingerprints.

## Included Data Compatibility Patches (from 0.1.0 – 0.2.1)

1. **`clam_conditions`**: Repairs 3 `neoforge:loot_table_id` → `forge:loot_table_id` condition IDs in `crittersandcompanions:loot_modifiers/inject_clams.json` (preserving 10% chance and pools).
2. **`bottle_recipe`**: Restores `legendary_monsters:bottle_of_annihilation` (8 bottles) when shadowed with `minecraft:air` by `legendary_delicacies`.
3. **`gun_conditions`**: Adds `forge:mod_loaded("scguns")` conditions to 4 optional gun attribute affixes in `fallen_gems_affixes` when `scguns` is absent and `placebo` `8.6.3` is loaded.

## Verification Summary

- **12 / 12 JUnit 5 tests pass** (including `HollowRealmTweaksCacheBenchmarkTest` micro-benchmark, `CompatPackResourcesTest`, `GunAffixPatchTest`, `ClamConditionPatchTest`, `BottleRecipePatchTest`).
- **Isolated Forge 47.4.29 dedicated-server verification** (standalone 11-JAR target set + co-installed with `HollowRealm Fixes 2.0.1`) across `baseline`, `active`, `absent`, and `coinstall` scenarios, both initial load and `/reload`.
- **Pending**: Live 282-mod client GUI / JEI playthrough and multiplayer verification.

See [VERIFICATION-0.2.1.md](VERIFICATION-0.2.1.md) for SHA-256 digests and exact test evidence.
