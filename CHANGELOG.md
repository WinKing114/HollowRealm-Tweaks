# Changelog

All notable changes to **HollowRealm Tweaks** (mod ID `hollowrealm_tweaks`) are documented here using [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) conventions.

---

## [0.2.1] — 2026-11-19 (First Public Release Candidate)

### Added
- Added `displayTest="IGNORE_ALL_VERSION"` in `META-INF/mods.toml` so server-data-only installations do not trigger false multiplayer version mismatches.
- Added machine-readable `hollowrealm-tweaks-manifest.json` describing the pack ID and all audited resource SHA-256 patches.
- Added JUnit 5 coverage for trailing-slash / exact-path `listResources`, `GunAffixPatch` existing-condition preservation, and `OverrideResolver` caching + micro-benchmark (`HollowRealmTweaksCacheBenchmarkTest`).
- Added isolated Forge `47.4.29` dedicated-server co-installation verification alongside `HollowRealm Fixes 2.0.1` and refreshed runtime targets for `legendary_delicacies-1.1.1.jar`.

### Changed
- **Performance (`HollowRealmTweaks.OverrideResolver`)**: Cached resolved override bytes once per JVM across `AddPackFindersEvent(SERVER_DATA)` firings instead of re-reading JAR files, hashing, and parsing JSON on every world/reload event (measured **1265.05x** faster in JUnit micro-benchmark: `417.214 ms` → `0.330 ms` over 10,000 resolutions).
- **Compatibility (`mods.toml`)**: Widened Forge version range from `[47.4.29,47.4.30)` to `[47.4.16,48)` and listed all optional `AFTER` target mods consistently (`crittersandcompanions`, `legendary_monsters`, `legendary_delicacies`, `fallen_gems_affixes`, `placebo`).
- **Documentation**: Rewrote `README.md`, CurseForge HTML/Markdown pages, and full changelog aligned with `HollowRealm Fixes` styling.

### Fixed
- Fixed `HollowRealmTweaks.readModResource` throwing and logging `NoSuchFileException` stack traces when a target path does not exist inside a mod file; now checks `Files.isRegularFile(resource)` first.
- Fixed `CompatPackResources.listResources` handling of trailing slashes (`"loot_modifiers/"`) and exact-resource path queries.
- Fixed `GunAffixPatch.addScgunsCondition` overwriting pre-existing `"conditions"` arrays if present on a candidate resource.

[CurseForge file `9703214`](https://www.curseforge.com/minecraft/mc-mods/hollowrealm-tweaks/files/9703214) · [GitHub tag `v0.2.1`](https://github.com/WinKing114/HollowRealm-Tweaks/releases/tag/v0.2.1) · [Release notes](docs/RELEASE-0.2.1.md) · [Verification notes](docs/VERIFICATION-0.2.1.md)

---

## [0.2.0] — 2026-11-16 (Local Isolated Candidate)

### Added
- Restored `legendary_monsters:bottle_of_annihilation` crafting recipe (8 bottles) when shadowed by `legendary_delicacies`'s empty-result override after matching both upstream SHA-256 fingerprints (`BottleRecipePatch`).
- Added native `forge:mod_loaded("scguns")` conditions to four optional gun attribute affixes (`bullet_damage`, `projectile_speed`, `reload_speed`, `spread_reduction`) in `fallen_gems_affixes` when `scguns` is absent and `placebo` `8.6.3` is present (`GunAffixPatch`).

### Changed
- Retained the `clam_conditions` patch and `hollowrealm_tweaks:compat` pack ID while making every patch independently skippable on missing targets or fingerprint drift.
- Verified in 8 JUnit tests and an 11-JAR isolated Forge `47.4.29` dedicated server (initial + reload + absent target group); not published to GitHub/CurseForge and not deployed into the formal instance by that session.

---

## [0.1.0] — 2026-11-15 (Initial Local Build)

### Added
- Initial `AddPackFindersEvent(SERVER_DATA)` implementation registering a top-priority in-memory `CompatPackResources` (`hollowrealm_tweaks:compat`), avoiding Forge mod-pack cross-namespace override limits.
- Implemented `ClamConditionPatch` rewriting three `neoforge:loot_table_id` condition IDs to `forge:loot_table_id` in `crittersandcompanions:loot_modifiers/inject_clams.json` while preserving the original 10% chance and tables.
- Verified with 4 JUnit tests and an isolated Forge `47.4.29` dedicated server; not published to GitHub/CurseForge.
