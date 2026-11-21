<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.20.1-5b9c51?style=flat" alt="Minecraft 1.20.1" />
  <img src="https://img.shields.io/badge/Forge-47.4.x-5b9c51?style=flat" alt="Forge 47.4.x" />
  <img src="https://img.shields.io/badge/Java-17-5b9c51?style=flat" alt="Java 17" />
  <a href="https://github.com/WinKing114/HollowRealm-Tweaks/issues"><img src="https://img.shields.io/badge/GitHub-Issues-5b9c51?style=flat" alt="GitHub Issues" /></a>
  <img src="https://img.shields.io/badge/License-All%20Rights%20Reserved-5b9c51?style=flat" alt="All Rights Reserved" />
</p>

# HollowRealm Tweaks

**HollowRealm Tweaks** is a lightweight, high-priority **in-memory server-data compatibility mod** built for the **HollowRealm** modpack on **Minecraft 1.20.1 (Forge)**. It repairs broken global loot modifier conditions, overwritten crafting recipes, and unguarded optional-mod affixes via Forge's `AddPackFindersEvent` (`hollowrealm_tweaks:compat`, positioned at `TOP`)—with zero Mixins, zero disk OpenLoader overrides, and zero gameplay rebalancing.

Install this mod if you are playing HollowRealm (or using the same mod combination) and want broken upstream data files repaired safely at runtime without losing original drops, outputs, or affix values. Code-level and Mixin crash fixes live separately in **[HollowRealm Fixes](https://www.curseforge.com/minecraft/mc-mods/hollowrealm-fixes)**.

---

## What's Included

Every patch verifies the exact **SHA-256 fingerprint** of the installed target JSON resources and **fails closed**: if an upstream mod is missing, updated, or already repaired, that patch skips cleanly without blocking other overrides.

- **Critters and Companions Clam Loot Condition Repair (`clam_conditions`)**  
  Fixes `crittersandcompanions:loot_modifiers/inject_clams.json` (v2.3.5) by rewriting three invalid `neoforge:loot_table_id` condition IDs to Forge 1.20.1's `forge:loot_table_id`, restoring clam drops in shipwrecks while keeping the original 10% chance and tables intact.
- **Annihilation Bottle Recipe Restoration (`bottle_recipe`)**  
  Detects when `legendary_delicacies` shadows `legendary_monsters:recipes/bottle_of_annihilation.json` with `minecraft:air` and restores Legendary Monsters' audited 8-bottle crafting recipe (Eye of Ender + Glass Bottles).
- **Fallen Gems & Affixes Optional Gun Affix Guard (`gun_conditions`)**  
  When `scguns` is absent and `placebo` `8.6.3` is loaded, adds native `forge:mod_loaded("scguns")` conditions to four optional gun attribute affixes (`bullet_damage`, `projectile_speed`, `reload_speed`, `spread_reduction`) so Placebo skips them before codec decoding without touching the remaining 98+ affixes.
- **Fallen Gems & Affixes Compat Gem & Extra Bonus Repairs (`compat_gem_spell_lightning`, `compat_gem_hemolith_shard`, `compat_extra_gem_bonus_hemolith_leggings`)**  
  Fixes three broken compat files in `fallen_gems_affixes` (`2.1.5`) that otherwise log `ERROR` stack traces on every world load:
  1. Adds `forge:mod_loaded("additional_attributes")` to `gems/compat/spell_lightning.json` when `additional_attributes` is absent (matching all other `spell_*` school gems).
  2. Strips the unguarded 3rd bonus (`additional_attributes:innate_spell/irons_spellbooks/devour`) from `gems/compat/hemolith_shard.json` when `additional_attributes` is absent while preserving both valid `irons_spellbooks` bonuses (`irons_spellbooks:blood_spell_power` and `irons_spellbooks:blood_magic_resist`), allowing `fallen_gems_affixes:compat/hemolith_shard` and its `hemolith_shard_light` / `hemolith_shard_ranged` extra gem bonuses to register cleanly.
  3. Gates `extra_gem_bonuses/compat/hemolith_shard_leggings.json` with `[{"type": "forge:false"}]` because its target effect `traveloptics:assassin` was removed in `traveloptics` `7.2.0-1.20.1`.

---

## Fixes vs. Tweaks — Which One Do You Need?

HollowRealm's custom maintenance is split cleanly into two complementary mods with zero overlapping classes or resource targets:

| Mod | Layer | What It Solves | Side / Multiplayer |
| :--- | :--- | :--- | :--- |
| **[HollowRealm Fixes](https://www.curseforge.com/minecraft/mc-mods/hollowrealm-fixes)** (`hollowrealm_performance_compat`) | **Code & Mixin** | Crashes, concurrency races, Goety & FancyMenu frame allocation guards, JEI socketing extension, CoO/Structurify coremod gate | `BOTH` (`IGNORE_SERVER_VERSION`) |
| **HollowRealm Tweaks** (`hollowrealm_tweaks`) | **In-Memory Server Data** | Broken clam GLM condition IDs, shadowed annihilation bottle recipe, optional Scguns affix conditions, FGA compat gem & extra-gem-bonus repairs | `BOTH` (`IGNORE_ALL_VERSION`) |

Both mods are designed and verified to run together cleanly when co-installed.

---

## Requirements & Compatibility

- **Minecraft / Loader / Java**: Minecraft **1.20.1**, Forge **`[47.4.16, 48)`** (runtime-verified on **Forge 47.4.29**), **Java 17**.
- **Multiplayer / Side**: Declares `displayTest="IGNORE_ALL_VERSION"` and registers only `SERVER_DATA` resources (`hollowrealm_tweaks:compat`), avoiding false client/server version rejections.
- **Optional Target Mods**: All target mods (`Critters and Companions`, `Legendary Monsters`, `Legendary Delicacies`, `Fallen Gems & Affixes`, `Placebo`) are optional. Overrides are resolved lazily once and cached per JVM process across `AddPackFindersEvent` calls.

---

## Related Projects

- **[HollowRealm Fixes](https://www.curseforge.com/minecraft/mc-mods/hollowrealm-fixes)** ([GitHub](https://github.com/WinKing114/HR-Fixes)) — Companion code-level & Mixin bug-fix mod for HollowRealm (`2.1.0`).
- **[CrosshairBobbing Fixed](https://www.curseforge.com/minecraft/mc-mods/crosshairbobbing-fixed)** ([GitHub](https://github.com/WinKing114/CrosshairBobbing-Fixed)) — Keeps your crosshair bobbing smoothly with view movement and playing action animations (walking, attacking, breaking blocks, charging bows/tridents/crossbows, eating, and drinking) while fixing Quilt/loader startup crashes and isolating Forge 1.20.x HUD matrix transforms so health bars and hotbars stay put.  
  *Attribution: Unofficial bug-fix fork of **Crosshair Bobbing** by **Krash220** — official original project on [CurseForge (`xbob`)](https://www.curseforge.com/minecraft/mc-mods/xbob) · [Modrinth (`xbob`)](https://modrinth.com/mod/xbob) · [GitHub (`Krash220/CrosshairBobbingMod`)](https://github.com/Krash220/CrosshairBobbingMod).*

---

## Feedback & Issue Reporting

- Browse **[Changelog](CHANGELOG.md)** for full version history (`0.1.0` – `0.3.0`) and **[CurseForge](https://www.curseforge.com/minecraft/mc-mods/hollowrealm-tweaks/files/all)** for released builds.
- Report compatibility issues or upstream mod updates on **[GitHub Issues](https://github.com/WinKing114/HollowRealm-Tweaks/issues)**. Please attach your `latest.log` / `debug.log`, Forge version, and installed mod versions.

### Building from Source (`0.3.0`)

```powershell
$env:JAVA_HOME = 'C:/Program Files/Zulu/zulu-17'
./gradlew.bat test build --no-daemon
```

Optional isolated dedicated-server regression (requires local HollowRealm target JARs):

```powershell
./gradlew.bat probeJar --no-daemon
python -X utf8 verify_runtime.py
```

---

## License & Credits

**All Rights Reserved**. Independent third-party compatibility project, not affiliated with or endorsed by the authors of the target mods. The production JAR contains only this project's Java classes and metadata with zero bundled upstream assets or code; see [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) for test-fixture attributions.