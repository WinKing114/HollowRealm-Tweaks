# 0.3.0 Verification & Artifact Identity

Verification log for **HollowRealm Tweaks `0.3.0`** (`HollowRealm-Tweaks-0.3.0.jar`).

- **Artifact**: `HollowRealm-Tweaks-0.3.0.jar`
- **Size**: `24,077` bytes (`15` non-directory archive entries)
- **SHA-256**: `3db803740d2ce456a09453eb72acf8051219ca643ad4b9188e079df41c8bd115`

## Audited Upstream Targets Added in 0.3.0 (`fallen_gems_affixes-1.20.1-2.1.5.jar`)

| Patch ID | Resource Path | Upstream SHA-256 | Loader Condition Mechanism (`javap`-verified) | Action |
| :--- | :--- | :--- | :--- | :--- |
| `compat_gem_spell_lightning` | `data/fallen_gems_affixes/gems/compat/spell_lightning.json` | `2ef303f9a95d8c5d7bd4165fe25f7de115f5c2cfeab83df7ddc9e7181d092c10` | `GemRegistry` → `DynamicRegistry.lambda$apply$0` calls `JsonUtil.checkConditions` (`"conditions"`) before `CodecMap.decode` | Appends `{"type":"forge:mod_loaded","modid":"additional_attributes"}` when `additional_attributes` is absent |
| `compat_gem_hemolith_shard` | `data/fallen_gems_affixes/gems/compat/hemolith_shard.json` | `7e310acd45eaa6154a89ded4f69e1b4630ca4c14d928ec8309cc41d0cb1dbdf3` | `Gem.CODEC` decodes `"bonuses"` via `GemBonus.CODEC.listOf()` without per-bonus conditions | Strips 3rd bonus (`additional_attributes:innate_spell/irons_spellbooks/devour`) in Java when `additional_attributes` is absent; keeps 2 `irons_spellbooks` bonuses |
| `compat_extra_gem_bonus_hemolith_leggings` | `data/fallen_gems_affixes/extra_gem_bonuses/compat/hemolith_shard_leggings.json` | `d17e0220769a120d7fddd4ce4ae14b14651adc9d7000b0db1ba3743b7c53dbb7` | `ExtraGemBonusRegistry` → `AbstractLazyPacketBoundRegistry.lambda$apply$3` calls `Serialization.checkConditions` (`"conditions"`) before `Codec.decode` | Replaces `"conditions"` with `[{"type":"forge:false"}]` to skip removed `traveloptics:assassin` effect |

## Build & Verification Layers

| Layer | Status | Scope & Notes |
| :--- | :--- | :--- |
| **Gradle / ForgeGradle Build & Remap** | **PASS** | ForgeGradle `6.0.54` / Gradle `8.10` / Zulu JDK 17 against Forge `1.20.1-47.4.29`; SRG-reobfuscated production JAR (`HollowRealm-Tweaks-0.3.0.jar`) + separate test probe JAR (`hollowrealm-tweaks-probe-0.3.0.jar`). |
| **JUnit 5 Unit Suite** | **PASS (18/18)** | `BottleRecipePatchTest` (2), `ClamConditionPatchTest` (2), `CompatGemPatchTest` (5, covering all 3 resources, `additional_attributes` absent/present cases, SHA-256 fail-closed checks, and malformed JSON rejection), `CompatPackResourcesTest` (3), `GunAffixPatchTest` (3), `HollowRealmTweaksCacheBenchmarkTest` (3, including `additional_attributes` loaded/unloaded override counts and cache micro-benchmark). |
| **Same-Environment Benchmark** | **PASS** | `HollowRealmTweaksCacheBenchmarkTest`: 10,000 iterations over all 9 overrides, uncached `buildOverrides` (`679.065 ms`) vs cached `OverrideResolver.resolve()` (`0.461 ms`) = **1473.99x speedup**. |
| **Isolated Forge 47.4.29 Dedicated Server (22 Target JARs incl. Iron's Spellbooks 3.16.4 & Traveloptics 7.2.0)** | **PASS** | `verify_runtime.py` across `baseline` (`22.0 s`), `active` (`23.1 s`), and `absent` (`14.7 s`):<br>• **`baseline`**: Exhibits all `8` compat error lines across initial load + `/reload` (`spell_lightning`, `hemolith_shard`, `hemolith_shard_leggings` + `Unknown registry key: traveloptics:assassin`); `hemolith_registered=false`, `total_gems=55`.<br>• **`active`**: `REGISTER hollowrealm_tweaks:compat: 9 audited overrides`; **`compat_error_count=0`** across initial load + `/reload`; `hemolith_registered=true` (`total_gems=56`), `spell_lightning_registered=false`, `hemolith_leggings_registered=false`, `hemolith_shard_light` (`traveloptics:vigor_siphon`) registered; clam GLM (`53`/`52` positive, `0` negative), annihilation bottle (`8` crafted), `4` gun affixes gated (`invalid_gun_registered=0`), and all `171` non-gun affixes unchanged (`UNCHANGED_OTHER_AFFIX_KEYS=PASS`).<br>• **`absent`**: All 6 patch groups skip cleanly (`SKIP ...`), no pack or resource leaked. |
| **Live 282-Mod Client GUI & Multiplayer** | **Pending** | Full-pack client startup, JEI gem/recipe display, and live gameplay verification remain to be checked after instance deployment. |
