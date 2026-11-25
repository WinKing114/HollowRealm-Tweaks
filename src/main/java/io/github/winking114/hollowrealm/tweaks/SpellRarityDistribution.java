package io.github.winking114.hollowrealm.tweaks;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import net.minecraft.util.RandomSource;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

/**
 * Seven-tier spell loot rarity distribution spanning Iron's Spellbooks (Common..Legendary)
 * and Forbidden Tiers (Mythic, Ancient).
 */
public final class SpellRarityDistribution {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int TIER_COUNT = 7;
    public static final String CONFIG_FILE_NAME = "hollowrealm-tweaks-spell-rarities.json";
    public static final String[] TIER_KEYS = {
            "common", "uncommon", "rare", "epic", "legendary", "mythic", "ancient"
    };
    private static final double[] DEFAULT_WEIGHTS = {0.22, 0.22, 0.20, 0.16, 0.11, 0.06, 0.03};
    private static final int[] SPELL_FILTER_WEIGHTS = {40, 30, 15, 8, 4, 2, 1};

    private static volatile double[] cachedWeights;

    private SpellRarityDistribution() {
    }

    public static double[] defaultWeights() {
        return DEFAULT_WEIGHTS.clone();
    }

    public static double[] getActiveWeights() {
        double[] current = cachedWeights;
        if (current != null) {
            return current;
        }
        synchronized (SpellRarityDistribution.class) {
            if (cachedWeights == null) {
                Path configPath = resolveConfigPath();
                cachedWeights = configPath != null ? loadWeightsFromFile(configPath) : DEFAULT_WEIGHTS.clone();
            }
            return cachedWeights;
        }
    }

    static void resetCacheForTests() {
        cachedWeights = null;
    }

    static Path resolveConfigPath() {
        try {
            Path configDir = FMLPaths.CONFIGDIR.get();
            return configDir != null ? configDir.resolve(CONFIG_FILE_NAME) : null;
        } catch (Throwable unavailableOutsideFml) {
            return null;
        }
    }

    /**
     * Parses custom 7-tier rarity weights from JSON; falls back cleanly to defaults if missing or invalid.
     */
    public static double[] loadWeightsFromFile(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return DEFAULT_WEIGHTS.clone();
        }
        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            double[] parsed = parseWeightsObject(root);
            if (parsed == null) {
                LOGGER.warn("[HollowRealm Tweaks] Invalid spell rarity weights in {}; falling back to defaults.", path.getFileName());
                return DEFAULT_WEIGHTS.clone();
            }
            return parsed;
        } catch (IOException | RuntimeException ex) {
            LOGGER.warn("[HollowRealm Tweaks] Failed reading {}; falling back to default 7-tier spell weights.", path.getFileName(), ex);
            return DEFAULT_WEIGHTS.clone();
        }
    }

    public static double[] parseWeightsObject(JsonObject root) {
        if (root == null) {
            return null;
        }
        JsonObject source = root.has("rarities") && root.get("rarities").isJsonObject()
                ? root.getAsJsonObject("rarities")
                : root;
        double[] values = new double[TIER_COUNT];
        double sum = 0.0;
        for (int i = 0; i < TIER_COUNT; i++) {
            JsonElement elem = source.get(TIER_KEYS[i]);
            if (elem == null || !elem.isJsonPrimitive() || !elem.getAsJsonPrimitive().isNumber()) {
                return null;
            }
            double v = elem.getAsDouble();
            if (!Double.isFinite(v) || v < 0.0) {
                return null;
            }
            values[i] = v;
            sum += v;
        }
        if (sum < 0.99 || sum > 1.01) {
            return null;
        }
        for (int i = 0; i < TIER_COUNT; i++) {
            values[i] = values[i] / sum;
        }
        return values;
    }

    /**
     * Maps loot quality in [0.0, 1.0] against the global cumulative 7-tier thresholds, then clamps
     * the chosen rarity into [minRarity, maxRarity] so early chests never leak Mythic/Ancient scrolls
     * even for spells whose minimum rarity starts above Common.
     */
    public static int pickTargetRarityIndex(double quality, int minRarity, int maxRarity, double[] weights) {
        int lo = Math.max(0, Math.min(TIER_COUNT - 1, minRarity));
        int hi = Math.max(lo, Math.min(TIER_COUNT - 1, maxRarity));
        double[] w = (weights != null && weights.length == TIER_COUNT) ? weights : DEFAULT_WEIGHTS;
        double clampedQuality = Math.max(0.0, Math.min(1.0, Double.isFinite(quality) ? quality : 0.0));

        double cumulative = 0.0;
        int chosen = TIER_COUNT - 1;
        for (int i = 0; i < TIER_COUNT; i++) {
            cumulative += w[i];
            if (clampedQuality <= cumulative + 1.0e-12) {
                chosen = i;
                break;
            }
        }
        return Math.max(lo, Math.min(hi, chosen));
    }

    /**
     * Selects a concrete spell level within [minLevel, maxLevel] whose rarity matches targetRarity
     * (or the nearest available rarity if a low-maxLevel spell skips an intermediate tier).
     */
    public static int pickLevelForRarity(
            int minLevel,
            int maxLevel,
            IntUnaryOperator levelToRarity,
            int targetRarity,
            double subRoll) {
        int safeMin = Math.max(1, minLevel);
        int safeMax = Math.max(safeMin, maxLevel);
        List<Integer> exact = new ArrayList<>();
        int bestLevel = safeMin;
        int bestDist = Integer.MAX_VALUE;

        for (int lvl = safeMin; lvl <= safeMax; lvl++) {
            int r = levelToRarity.applyAsInt(lvl);
            if (r == targetRarity) {
                exact.add(lvl);
            }
            int dist = Math.abs(r - targetRarity);
            if (dist < bestDist) {
                bestDist = dist;
                bestLevel = lvl;
            }
        }

        if (exact.isEmpty()) {
            return bestLevel;
        }
        double clampedRoll = Math.max(0.0, Math.min(0.999999999999, Double.isFinite(subRoll) ? subRoll : 0.0));
        int idx = (int) (clampedRoll * exact.size());
        return exact.get(Math.min(idx, exact.size() - 1));
    }

    /** Fallback spell-pool weight supporting Mythic(5) and Ancient(6) without throwing IncompatibleClassChangeError. */
    public static int getSpellFilterWeight(int rarityValue) {
        if (rarityValue < 0) return SPELL_FILTER_WEIGHTS[0];
        if (rarityValue >= TIER_COUNT) return SPELL_FILTER_WEIGHTS[TIER_COUNT - 1];
        return SPELL_FILTER_WEIGHTS[rarityValue];
    }

    public static int selectSpellLevel(AbstractSpell spell, float quality, RandomSource random) {
        int minLevel = Math.max(1, spell.getMinLevel());
        int maxLevel = Math.max(minLevel, spell.getMaxLevel());

        int observedMinRarity = TIER_COUNT - 1;
        int observedMaxRarity = 0;
        for (int lvl = minLevel; lvl <= maxLevel; lvl++) {
            int r = clampTier(spell.getRarity(lvl).getValue());
            if (r < observedMinRarity) observedMinRarity = r;
            if (r > observedMaxRarity) observedMaxRarity = r;
        }
        int effectiveMinRarity = Math.min(observedMinRarity, observedMaxRarity);

        int targetRarity = pickTargetRarityIndex(quality, effectiveMinRarity, observedMaxRarity, getActiveWeights());
        double subRoll = random != null ? random.nextDouble() : 0.0;
        return pickLevelForRarity(
                minLevel,
                maxLevel,
                lvl -> clampTier(spell.getRarity(lvl).getValue()),
                targetRarity,
                subRoll);
    }

    private static int clampTier(int value) {
        return Math.max(0, Math.min(TIER_COUNT - 1, value));
    }
}
