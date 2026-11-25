# 0.4.0 Release Notes

**HollowRealm Tweaks 0.4.0** expands the mod beyond in-memory `SERVER_DATA` compatibility patches with two strictly version- and SHA-256-bytecode-gated cross-mod Mixin integrations while leaving pure crash, concurrency, and performance fixes isolated in **HollowRealm Fixes 2.1.0**.

## Highlights

- **Fabled Reforging Table + `apotheosis:ancient_material` Dual-Mode Support** (`FabledReforgingTableTileMixin`, `FabledReforgingMenuMixin`, `FabledResultSlotMixin`, `FabledAncientReforgingHelper`):
  - Keeps `fallen_gems_affixes:fabled_material` behavior 100% intact (Fabled rarity + `SetAffix` crafting, 5 materials / 10 sigils / 50 levels).
  - Accepts `apotheosis:ancient_material` in `FabledReforgingTableTile`, resolves `apotheosis:reforging` recipe (`3` materials, `10` sigils, `125` levels base, `1x / 2x / 3x` slot scaling), generates 3 deterministic Ancient choices via `LootController.createLootItem`, consumes materials/sigils and Apotheosis-scaled single-level XP (`EnchantmentUtils.chargeExperience` + `ApothMiscUtil.getExpCostForSlot`), and refreshes the menu/block entity cleanly after pickup.
- **7-Tier Spell Loot Distribution for Iron's Spellbooks + Forbidden Tiers** (`RandomizeSpellFunctionMixin`, `SpellRarityDistribution`):
  - Enforces a configurable 7-tier rarity table via `config/hollowrealm-tweaks-spell-rarities.json` (default `0.22 / 0.22 / 0.20 / 0.16 / 0.11 / 0.06 / 0.03` for `common`..`ancient`), clamps each spell to its valid rarity band `[observedMinRarity, observedMaxRarity]`, prevents low-quality `[0.0, 0.3]` chests from leaking Mythic/Ancient scrolls, and adds Mythic/Ancient filter weights to `RandomizeSpellFunction.getWeightFromRarity`.
- **Fail-Closed Gates**: All 4 Mixins in `HRTweaksMixinPlugin` verify installed mod versions and exact SHA-256 bytecode fingerprints before applying, and `displayTest="IGNORE_SERVER_VERSION"` guards multiplayer version checks.

## Compatibility

- Co-install tested alongside `HollowRealm-Fixes-2.1.0.jar` (`hollowrealm_performance_compat`) with zero Mixin conflicts and zero overlap.
- All 9 existing `hollowrealm_tweaks:compat` `SERVER_DATA` overrides preserved (`clam_conditions`, `bottle_recipe`, 4 `gun_conditions`, 3 `compat_gem` patches).
