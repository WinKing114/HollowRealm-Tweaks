# 0.4.0 Verification & Artifact Identity

Verification log for **HollowRealm Tweaks `0.4.0`** (`HollowRealm-Tweaks-0.4.0.jar`).

- **Artifact**: `HollowRealm-Tweaks-0.4.0.jar`
- **Size**: `47,510` bytes (`25` non-directory archive entries)
- **SHA-256**: `f04194a12d3dd3cfb8992f84fbc98e9ff0d8fae5a67046f2c0c2a427ba7f6c80`

## Mixin Targets & Bytecode Fingerprints

| Mixin Class | Target Class | Target SHA-256 | Required Mods / Versions |
| :--- | :--- | :--- | :--- |
| `FabledReforgingTableTileMixin` | `net/kayn/fallen_gems_affixes/adventure/reforging/FabledReforgingTableTile.class` | `d6adf620cc4bffc0886394212d9ae329fae0685b1e2d3cc200309631f935005f` | `apotheosis@7.4.8`, `ancientreforging@1.3`, `fallen_gems_affixes@1.20.1-2.1.4 / 2.1.5` (atomic 3-class check) |
| `FabledReforgingMenuMixin` | `net/kayn/fallen_gems_affixes/adventure/reforging/FabledReforgingMenu.class` | `2c6fde1038a0a7a66b7a5c2fd99c846ed7afdd12bc8463b25141d3c241de288a` | `apotheosis@7.4.8`, `ancientreforging@1.3`, `fallen_gems_affixes@1.20.1-2.1.4 / 2.1.5` |
| `FabledResultSlotMixin` | `net/kayn/fallen_gems_affixes/adventure/reforging/FabledReforgingMenu$FabledResultSlot.class` | `e9e994f309f622f6d13401ee42311d1d68865d17e6a25ea762c57bfdfe2cea2d` | `apotheosis@7.4.8`, `ancientreforging@1.3`, `fallen_gems_affixes@1.20.1-2.1.4 / 2.1.5` |
| `RandomizeSpellFunctionMixin` | `io/redspace/ironsspellbooks/loot/RandomizeSpellFunction.class` | `d5cfcd8f93dc8ca9833820389fcccb55cc2cda7feedeffff77177d74f830efb3` | `irons_spellbooks@1.20.1-3.16.4`, `forbidden_tiers@1.0.0 / 1.0.1` |

## Build & Verification Layers

| Layer | Status | Scope & Notes |
| :--- | :--- | :--- |
| **Gradle / ForgeGradle Build & Remap** | **PASS** | ForgeGradle `6.0.54` / Gradle `8.10` / Zulu JDK 17 against Forge `1.20.1-47.4.29`; zero warnings in `:compileJava` / `:compileProbeJava`; `javap` verified SRG remapping (`m_6749_`, `m_6596_`, `m_6199_`, `tile`). |
| **JUnit 5 Unit Suite** | **PASS (29/29)** | Existing `18` tests + `HRTweaksMixinPluginTest` (3), `FabledAncientReforgingTest` (3), `SpellRarityDistributionTest` (5). Verified live target JAR SHA-256s, tamper/missing-mod fail-closed gating, `1x/2x/3x` slot scaling, 7-tier JSON parsing/fallback, early chest `[0.0, 0.3]` 0% Mythic/Ancient clamp, and `[0.0, 1.0)` / `[0.5, 1.0)` distributions. |
| **Isolated Forge 47.4.29 Dedicated Server (25 Target JARs incl. Fixes 2.1.0, Ancient Reforging 1.3, Iron's Spellbooks 3.16.4, Forbidden Tiers 1.0.1, FGA 2.1.5)** | **PASS** | `verify_runtime.py` across `baseline` (`26.0 s`), `active`, and `absent` (`14.8 s`):<br>• **`baseline`**: Exhibits `8` compat error lines; `fabled_accepts_ancient=false`, `fabled_accepts_fabled=true`, `ancient_accepts_ancient=true`.<br>• **`active`**: Co-installed with `HollowRealm-Fixes-2.1.0.jar` (`fixes_coinstalled=true`); all 4 Mixins `MIXIN APPLY` + all 9 data overrides applied (`compat_error_count=0`); **`FabledReforgingTable`** + `ancient_material` generated 3 `apotheosis:ancient` choices and on take consumed `3` `ancient_material`, `10` `sigil_of_rebirth`, and single-level L125 XP (`957` points -> final level `129`, exact bit parity with `AncientReforgingMenu`), plus `fabled_material` SetAffix regression passed (`mat_cost=5`); **`RandomizeSpellFunction`** over `firebolt` `10,000` rolls yielded `[2241, 2153, 2021, 1589, 1118, 588, 290]` (initial) / `[2174, 2261, 1944, 1621, 1097, 583, 320]` (reload) matching `22/22/20/16/11/6/3`, `[0.0, 0.3]` early chest `3,000` rolls had `0` Mythic / `0` Ancient, `10,000` global pool rolls had `601`/`295` (initial) and `618`/`273` (reload) Mythic/Ancient, and `[0.5, 1.0]` vault `5,000` rolls had `688`/`276` (initial) and `600`/`312` (reload); `UNCHANGED_OTHER_AFFIX_KEYS=PASS`.<br>• **`absent`**: All 4 Mixins cleanly `MIXIN SKIP`; all 6 data patches cleanly `SKIP`. |
| **Live Full-Pack Client GUI & Gameplay** | **Pending** | Full 290-mod client startup, Fabled Table tooltip/cost rendering, JEI, and interactive spell loot/progression remain to be checked in live client session after deployment. |
