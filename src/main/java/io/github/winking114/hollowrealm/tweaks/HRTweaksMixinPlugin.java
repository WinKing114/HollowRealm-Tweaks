package io.github.winking114.hollowrealm.tweaks;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/**
 * Version- and bytecode-fingerprint-gated Mixin plugin for HollowRealm Tweaks.
 * Every integration mixin fails closed when any required mod is absent, outside audited versions,
 * or has unexpected target class bytecode.
 */
public final class HRTweaksMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOG = LogManager.getLogger("HollowRealm Tweaks");

    private static final List<String> APOTHEOSIS_VERSIONS = List.of("7.4.8");
    private static final List<String> ANCIENT_REFORGING_VERSIONS = List.of("1.3");
    // fallen_gems_affixes-1.20.1-2.1.5.jar declares 1.20.1-2.1.4 in mods.toml.
    private static final List<String> FGA_VERSIONS = List.of("1.20.1-2.1.4", "1.20.1-2.1.5");
    private static final List<String> ISS_VERSIONS = List.of("1.20.1-3.16.4");
    // forbidden_tiers-1.20.1-1.0.1.jar declares 1.0.0 in mods.toml.
    private static final List<String> FORBIDDEN_TIERS_VERSIONS = List.of("1.0.0", "1.0.1");

    static final String FABLED_TILE_CLASS = "net/kayn/fallen_gems_affixes/adventure/reforging/FabledReforgingTableTile.class";
    static final String FABLED_TILE_SHA256 = "d6adf620cc4bffc0886394212d9ae329fae0685b1e2d3cc200309631f935005f";

    static final String FABLED_MENU_CLASS = "net/kayn/fallen_gems_affixes/adventure/reforging/FabledReforgingMenu.class";
    static final String FABLED_MENU_SHA256 = "2c6fde1038a0a7a66b7a5c2fd99c846ed7afdd12bc8463b25141d3c241de288a";

    static final String FABLED_RESULT_SLOT_CLASS = "net/kayn/fallen_gems_affixes/adventure/reforging/FabledReforgingMenu$FabledResultSlot.class";
    static final String FABLED_RESULT_SLOT_SHA256 = "e9e994f309f622f6d13401ee42311d1d68865d17e6a25ea762c57bfdfe2cea2d";

    static final String RANDOMIZE_SPELL_CLASS = "io/redspace/ironsspellbooks/loot/RandomizeSpellFunction.class";
    static final String RANDOMIZE_SPELL_SHA256 = "d5cfcd8f93dc8ca9833820389fcccb55cc2cda7feedeffff77177d74f830efb3";

    private static final ResourceLookup DEFAULT_LOOKUP = path -> {
        try (InputStream stream = MixinService.getService().getResourceAsStream(path)) {
            return stream == null ? null : stream.readAllBytes();
        }
    };

    private static volatile Map<String, String> installedVersions;
    private static volatile ResourceLookup resourceLookup = DEFAULT_LOOKUP;

    private final Map<String, Decision> decisions = new ConcurrentHashMap<>();

    @FunctionalInterface
    interface ResourceLookup {
        byte[] load(String path) throws IOException;
    }

    static final class Decision {
        final boolean enabled;
        final String reason;
        final boolean warn;

        Decision(boolean enabled, String reason, boolean warn) {
            this.enabled = enabled;
            this.reason = reason;
            this.warn = warn;
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        Decision decision = decisions.computeIfAbsent(mixinClassName, this::evaluate);
        if (decision.enabled) {
            LOG.info("[HollowRealm Tweaks] MIXIN APPLY {}: {}", shortName(mixinClassName), decision.reason);
        } else if (decision.warn) {
            LOG.warn("[HollowRealm Tweaks] MIXIN SKIP {}: {}", shortName(mixinClassName), decision.reason);
        } else {
            LOG.info("[HollowRealm Tweaks] MIXIN SKIP {}: {}", shortName(mixinClassName), decision.reason);
        }
        return decision.enabled;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private Decision evaluate(String mixinClassName) {
        String name = shortName(mixinClassName);
        if ("FabledReforgingTableTileMixin".equals(name)
                || "FabledReforgingMenuMixin".equals(name)
                || "FabledResultSlotMixin".equals(name)) {
            Decision gate = firstGate(
                    gate("apotheosis", APOTHEOSIS_VERSIONS),
                    gate("ancientreforging", ANCIENT_REFORGING_VERSIONS),
                    gate("fallen_gems_affixes", FGA_VERSIONS));
            if (gate != null) return gate;
            Decision tileHash = checkClassHash(FABLED_TILE_CLASS, FABLED_TILE_SHA256, "tile bytecode verified");
            if (!tileHash.enabled) return tileHash;
            Decision menuHash = checkClassHash(FABLED_MENU_CLASS, FABLED_MENU_SHA256, "menu bytecode verified");
            if (!menuHash.enabled) return menuHash;
            String reason = switch (name) {
                case "FabledReforgingTableTileMixin" -> "FabledReforgingTableTile accepts apotheosis:ancient_material";
                case "FabledReforgingMenuMixin" -> "FabledReforgingMenu routes apotheosis:ancient_material via Apotheosis ancient reforging";
                default -> "FabledResultSlot charges Apotheosis scaled XP and refreshes tile state after ancient reforging";
            };
            return checkClassHash(FABLED_RESULT_SLOT_CLASS, FABLED_RESULT_SLOT_SHA256, reason);
        }
        if ("RandomizeSpellFunctionMixin".equals(name)) {
            Decision gate = firstGate(
                    gate("irons_spellbooks", ISS_VERSIONS),
                    gate("forbidden_tiers", FORBIDDEN_TIERS_VERSIONS));
            if (gate != null) return gate;
            return checkClassHash(RANDOMIZE_SPELL_CLASS, RANDOMIZE_SPELL_SHA256,
                    "RandomizeSpellFunction applies configured 7-tier rarity weights across ISS + Forbidden Tiers");
        }
        return new Decision(false, "unrecognized mixin class " + name, true);
    }

    private static Decision firstGate(Decision... gates) {
        for (Decision g : gates) {
            if (g != null) return g;
        }
        return null;
    }

    private static Decision gate(String modId, List<String> allowed) {
        String ver = versions().get(modId);
        if (ver == null) {
            return new Decision(false, modId + " is not installed", false);
        }
        if (allowed.contains(ver)) {
            return null;
        }
        return new Decision(false, modId + " " + ver + " is not audited (expected " + String.join(" / ", allowed) + ")", true);
    }

    private static Decision checkClassHash(String classPath, String expectedSha256, String okReason) {
        try {
            byte[] bytes = resourceLookup.load(classPath);
            if (bytes == null) {
                return new Decision(false, "target class missing: " + classPath, true);
            }
            String actual = sha256Hex(bytes);
            if (!expectedSha256.equals(actual)) {
                return new Decision(false, "bytecode fingerprint mismatch for " + classPath + " (actual=" + actual + ")", true);
            }
            return new Decision(true, okReason, false);
        } catch (IOException ex) {
            return new Decision(false, "failed reading " + classPath + ": " + ex.getMessage(), true);
        }
    }

    private static Map<String, String> versions() {
        Map<String, String> cached = installedVersions;
        if (cached != null) return cached;
        LoadingModList modList = LoadingModList.get();
        if (modList == null) return Map.of();
        Map<String, String> collected = new LinkedHashMap<>();
        for (ModInfo info : modList.getMods()) {
            collected.put(info.getModId(), info.getVersion().toString());
        }
        if (!collected.isEmpty()) {
            installedVersions = Map.copyOf(collected);
        }
        return collected;
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private static String shortName(String fqn) {
        int idx = fqn.lastIndexOf('.');
        return idx >= 0 ? fqn.substring(idx + 1) : fqn;
    }

    static void setInstalledVersionsForTests(Map<String, String> mockVersions) {
        installedVersions = Map.copyOf(mockVersions);
    }

    static void setResourceLookupForTests(ResourceLookup lookup) {
        resourceLookup = lookup == null ? DEFAULT_LOOKUP : lookup;
    }

    static void resetVersionCacheForTests() {
        installedVersions = null;
        resourceLookup = DEFAULT_LOOKUP;
    }
}
