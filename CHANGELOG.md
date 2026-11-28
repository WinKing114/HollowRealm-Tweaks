# Changelog

All notable changes to **HollowRealm Tweaks** (`hollowrealm_tweaks`) are documented here.

## [1.2.0] — 2026-11-28

### Added
- **`StarterGuideBookBlocker`**: Pre-marks `spell_reaction:guide_book` at `EventPriority.HIGHEST` on `PlayerLoggedInEvent` and performs a one-time inventory sweep on the first login with 1.2.0 (including existing characters) to strip stray loose guide books (`patchouli:guide_book`, `ars_nouveau:worn_notebook`, `enigmaticlegacy:the_acknowledgment`) while preserving `eccentrictome:tome`, spell books, and all starter gear/curios.

## [1.1.0] — 2026-11-28

### Changed
- Promoted HollowRealm Tweaks to `1.1.0` for the HollowRealm pre-release final build after verifying live 1.20.1/Forge 47.4.29 loading across `clam_conditions`, `bottle_recipe`, `gun_conditions`, `compat_gem_*`, `fabled_table_ancient_material`, `spell_loot_7_tier_distribution`, and `UnifiedCritModule`.
- Synchronized version metadata across `gradle.properties`, `mods.toml`, and `hollowrealm-tweaks-manifest.json`.

## [1.0.0] — 2026-11-26

### Added
- **Unified Crit Attribute Normalization (`UnifiedCritNormalizer`, `UnifiedCritEvents`, `CuriosCritCompat`)**:
  - Replaces `fusedcritical-1.0.9.jar` by bridging secondary crit-rate (`eeeabsmobs:crit_chance`, `gtbcs_spell_lib:spell_crit_chance`, `l2damagetracker:crit_rate`) and crit-damage (`cataclysm:additional_critical_damage` scaled by `0.01`, `gtbcs_spell_lib:spell_crit_damage`, `irons_lib:crit_damage`, `l2damagetracker:crit_damage`, `obscure_api:critical_damage`) modifiers onto ApothicAttributes' canonical `attributeslib:crit_chance` and `attributeslib:crit_damage` using deterministic transient `ADDITION` modifiers.
  - Zeroes each secondary source attribute via a transient `-1.0 MULTIPLY_TOTAL` modifier so ApothicAttributes' native `AttributeEvents.apothCriticalStrike` and `vanillaCritDmg` handle all crit rolling, over-crit tiers, and particles without duplicate crits.
  - Automatically stays dormant if `attributeslib` is absent or legacy `fusedcritical` is still installed; supports Curios equipment changes when `curios` is loaded.
- Added `UnifiedCritNormalizerTest` JUnit 5 suite.

## [0.4.0] — 2026-11-24
- Initial HollowRealm Tweaks release (`clam_conditions`, `bottle_recipe`, `gun_conditions`, `compat_gem_*`, Fabled Reforging Ancient Material support, and Iron's Spellbooks + Forbidden Tiers 7-tier spell rarity distribution).
