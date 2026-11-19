# 0.2.1 Verification & Artifact Identity

Verification log for **HollowRealm Tweaks `0.2.1`** (`HollowRealm-Tweaks-0.2.1.jar`).

- **Artifact**: `HollowRealm-Tweaks-0.2.1.jar`
- **Size**: `19,565` bytes (`14` archive entries)
- **SHA-256**: `0ffba8da842c4b561893a60e7d5cc05410240cb436e6e0d51432acd248e0c394`

## Build & Verification Layers

| Layer | Status | Scope & Notes |
| :--- | :--- | :--- |
| **Gradle / ForgeGradle Build & Remap** | **PASS** | ForgeGradle `6.0.54` / Gradle `8.10` / Zulu JDK 17 against Forge `1.20.1-47.4.29`; SRG-reobfuscated production JAR + separate test probe JAR. |
| **JUnit 5 Unit Suite** | **PASS (12/12)** | `BottleRecipePatchTest` (2), `ClamConditionPatchTest` (2), `CompatPackResourcesTest` (3, including trailing-slash & exact-path `listResources`), `GunAffixPatchTest` (3, including existing-condition preservation & malformed-JSON rejection), `HollowRealmTweaksCacheBenchmarkTest` (2). |
| **Same-Environment Benchmark** | **PASS** | `HollowRealmTweaksCacheBenchmarkTest`: 10,000 iterations, uncached `buildOverrides` (`417.214 ms`) vs cached `OverrideResolver.resolve()` (`0.330 ms`) = **1265.05x speedup**. |
| **Isolated Forge 47.4.29 Standalone Dedicated Server** | **PASS** | `verify_runtime.py` across `baseline` (`17.8 s`), `active` (`17.7 s`), and `absent` (`13.9 s`) using current instance target JARs (`legendary_delicacies-1.1.1.jar`): `hollowrealm_tweaks:compat` wins priority on initial load + `/reload`; clam GLM decoded (`50` shipwreck positive / `0` non-target negative across `512` seeds); annihilation bottle recipe crafts `8` and rejects invalid ingredients; `4` gun affixes skipped cleanly (`invalid_gun_registered=0`); all `98` other affixes unchanged (`UNCHANGED_OTHER_AFFIX_KEYS=PASS`). |
| **Isolated Forge 47.4.29 Co-Installed Server (with Fixes 2.0.1)** | **PASS** | Co-installed alongside `HollowRealm-Fixes-2.0.1.jar` + L2 Hostility + KubeJS (`21` mods, `24.2 s`): all 6 overrides active across initial + `/reload`, zero conflicts or attributable errors. |
| **Live 282-Mod Client GUI & Multiplayer** | **Pending** | Full-pack 282-mod client startup, JEI recipe display, natural exploration loot drops, and live multiplayer server sync (`displayTest="IGNORE_ALL_VERSION"`) remain to be verified after instance deployment. |
