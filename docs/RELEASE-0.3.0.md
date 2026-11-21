# HollowRealm Tweaks 0.3.0

Update to **HollowRealm Tweaks** (`hollowrealm_tweaks`), expanding the SHA-256-audited in-memory `SERVER_DATA` compatibility pack (`hollowrealm_tweaks:compat`) from 6 to **9 audited overrides** by repairing 3 broken compat files in `fallen_gems_affixes-1.20.1-2.1.5.jar` that threw `ERROR` stack traces on every world load.

## What's New in 0.3.0

- **`CompatGemPatch` (`fallen_gems_affixes` 2.1.5 compat repairs)**:
  1. **`compat_gem_spell_lightning`** (`data/fallen_gems_affixes/gems/compat/spell_lightning.json`, upstream SHA-256 `2ef303f9a95d8c5d7bd4165fe25f7de115f5c2cfeab83df7ddc9e7181d092c10`): appends `{"type": "forge:mod_loaded", "modid": "additional_attributes"}` to `"conditions"` when `additional_attributes` is not loaded so `GemRegistry` (`Placebo` `DynamicRegistry`) skips it cleanly before decoding.
  2. **`compat_gem_hemolith_shard`** (`data/fallen_gems_affixes/gems/compat/hemolith_shard.json`, upstream SHA-256 `7e310acd45eaa6154a89ded4f69e1b4630ca4c14d928ec8309cc41d0cb1dbdf3`): when `additional_attributes` is not loaded, strips the unguarded 3rd bonus (`"attribute": "additional_attributes:innate_spell/irons_spellbooks/devour"`) while preserving the 2 valid `irons_spellbooks` bonuses (`irons_spellbooks:blood_spell_power` and `irons_spellbooks:blood_magic_resist`), allowing `fallen_gems_affixes:compat/hemolith_shard` to register cleanly and enabling its `hemolith_shard_light` (`traveloptics:vigor_siphon`) and `hemolith_shard_ranged` (`cataclysm_spellbooks:hemophilia`) extra gem bonuses.
  3. **`compat_extra_gem_bonus_hemolith_leggings`** (`data/fallen_gems_affixes/extra_gem_bonuses/compat/hemolith_shard_leggings.json`, upstream SHA-256 `d17e0220769a120d7fddd4ce4ae14b14651adc9d7000b0db1ba3743b7c53dbb7`): overrides `"conditions"` with `[{"type": "forge:false"}]` so `fallen_lib`'s `ExtraGemBonusRegistry` (`Serialization.checkConditions`) skips the removed `traveloptics:assassin` mob effect (`traveloptics-7.2.0-1.20.1.jar`) without throwing `Unknown registry key: traveloptics:assassin`.
- **Early mod-state fallback**: Added `LoadingModList` fallback to `HollowRealmTweaks.ForgeModState` if `ModList.get()` is not yet populated.
- **Expanded test & runtime verification**: Added `CompatGemPatchTest` (5 JUnit tests) and expanded `HollowRealmTweaksCacheBenchmarkTest` (18 / 18 JUnit tests total), plus 22-JAR isolated dedicated-server verification including `irons_spellbooks-1.20.1-3.16.4.jar` and `traveloptics-7.2.0-1.20.1.jar`.

See [VERIFICATION-0.3.0.md](VERIFICATION-0.3.0.md) for SHA-256 digests and exact test evidence.
