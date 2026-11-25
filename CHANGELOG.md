# Changelog

All notable changes to **HollowRealm Tweaks** (mod ID `hollowrealm_tweaks`) are documented here using [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) conventions.

---

## [0.4.0] — 2026-11-24 (Fabled Ancient Reforging + 7-Tier Spell Loot Distribution)

### Added
- **Version- & Bytecode-Gated Mixin Subsystem (`HRTweaksMixinPlugin`)**: Fails closed unless exact audited mod versions and SHA-256 class fingerprints match:
  - **`FabledReforgingTableTileMixin`, `FabledReforgingMenuMixin`, `FabledResultSlotMixin`** (`FabledAncientReforgingHelper`): Enables `fallen_gems_affixes:fabled_reforging_table` to accept `apotheosis:ancient_material`, compute Apotheosis `ancient` reforging candidates (`3` materials / `10` sigils / `125` base levels scaled `1x/2x/3x` across slots), charge single-level Apotheosis XP (`EnchantmentUtils.chargeExperience` + `ApothMiscUtil.getExpCostForSlot`), refresh menu/block entity state on pickup, and leave `fallen_gems_affixes:fabled_material` (`SetAffix`) behavior completely untouched.
  - **`RandomizeSpellFunctionMixin`** (`SpellRarityDistribution`): Rebalances `irons_spellbooks` + `forbidden_tiers` spell scroll/imbued loot across all 7 rarities via `config/hollowrealm-tweaks-spell-rarities.json` (default `0.22 / 0.22 / 0.20 / 0.16 / 0.11 / 0.06 / 0.03`), clamps spells to their legal tier band so low-quality chests do not leak Mythic/Ancient scrolls, and guards `getWeightFromRarity` for `MYTHIC(5)`/`ANCIENT(6)`.
- Added JUnit 5 suites `HRTweaksMixinPluginTest` (3), `FabledAncientReforgingTest` (3), and `SpellRarityDistributionTest` (5) — `29 / 29` JUnit tests passing — plus live Forge `47.4.29` runtime verification co-installed with `HollowRealm-Fixes-2.1.0.jar`.

### Changed
- Bumped `mods.toml` to `0.4.0`, set `displayTest="IGNORE_SERVER_VERSION"`, and added optional `AFTER` ordering on `apotheosis`, `ancientreforging`, `irons_spellbooks`, and `forbidden_tiers`.

[Release notes](docs/RELEASE-0.4.0.md) · [Verification notes](docs/VERIFICATION-0.4.0.md)

---

## [0.3.0] — 2026-11-21 (Fallen Gems & Affixes Compat Gem & Extra Bonus Repairs)

### Added
- **`CompatGemPatch`** (`fallen_gems_affixes-1.20.1-2.1.5.jar` SHA-256-audited overrides, increasing total active overrides from `6` to `9` when `scguns` and `additional_attributes` are absent):
  1. `data/fallen_gems_affixes/gems/compat/spell_lightning.json` (`2ef303f9a95d8c5d7bd4165fe25f7de115f5c2cfeab83df7ddc9e7181d092c10`): appends `{"type": "forge:mod_loaded", "modid": "additional_attributes"}` to `"conditions"` when `additional_attributes` is not loaded so `GemRegistry` skips it cleanly before decoding.
  2. `data/fallen_gems_affixes/gems/compat/hemolith_shard.json` (`7e310acd45eaa6154a89ded4f69e1b4630ca4c14d928ec8309cc41d0cb1dbdf3`): strips the unguarded 3rd bonus (`"attribute": "additional_attributes:innate_spell/irons_spellbooks/devour"`) when `additional_attributes` is not loaded while preserving both valid `irons_spellbooks` bonuses (`irons_spellbooks:blood_spell_power` and `irons_spellbooks:blood_magic_resist`), allowing `fallen_gems_affixes:compat/hemolith_shard` and its `hemolith_shard_light` (`traveloptics:vigor_siphon`) / `hemolith_shard_ranged` (`cataclysm_spellbooks:hemophilia`) extra gem bonuses to register cleanly.
  3. `data/fallen_gems_affixes/extra_gem_bonuses/compat/hemolith_shard_leggings.json` (`d17e0220769a120d7fddd4ce4ae14b14651adc9d7000b0db1ba3743b7c53dbb7`): overrides `"conditions"` with `[{"type": "forge:false"}]` so `fallen_lib`'s `ExtraGemBonusRegistry` skips the removed `traveloptics:assassin` mob effect (`traveloptics-7.2.0-1.20.1.jar`) without throwing `Unknown registry key: traveloptics:assassin`.
- Added `CompatGemPatchTest` (5 JUnit 5 tests covering all 3 resources, `additional_attributes` absent/present cases, SHA-256 fail-closed checks, and malformed JSON rejection) and expanded `HollowRealmTweaksCacheBenchmarkTest` (now 18 / 18 JUnit tests total).
- Added `LoadingModList` fallback to `ForgeModState` for early pack-finder invocations and updated `hollowrealm-tweaks-manifest.json` to `0.3.0`.

[Release notes](docs/RELEASE-0.3.0.md) · [Verification notes](docs/VERIFICATION-0.3.0.md)

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
