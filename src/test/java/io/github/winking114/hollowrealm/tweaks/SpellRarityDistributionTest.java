package io.github.winking114.hollowrealm.tweaks;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class SpellRarityDistributionTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void resetCache() {
        SpellRarityDistribution.resetCacheForTests();
    }

    @Test
    void defaultWeightsSumExactlyToOneAndMatchSevenTierCurve() {
        double[] weights = SpellRarityDistribution.defaultWeights();
        assertArrayEquals(new double[]{0.22, 0.22, 0.20, 0.16, 0.11, 0.06, 0.03}, weights);
        double sum = 0.0;
        for (double w : weights) {
            sum += w;
        }
        assertEquals(1.0, sum, 0.0);
    }

    @Test
    void pickTargetRarityIndexMatchesConfiguredDistributionAndClampsEarlyChests() {
        double[] w = SpellRarityDistribution.defaultWeights();

        // Low-quality early chest sweep [0.0, 0.30]: never leaks Mythic (5) or Ancient (6)
        for (int i = 0; i <= 300; i++) {
            double q = i / 1000.0;
            int idx = SpellRarityDistribution.pickTargetRarityIndex(q, 0, 6, w);
            assertTrue(idx <= 1, "Expected Common/Uncommon for quality " + q + ", got " + idx);

            int gatedIdx = SpellRarityDistribution.pickTargetRarityIndex(q, 3, 6, w);
            assertEquals(3, gatedIdx, "High-minRarity spell should clamp at Epic on low-quality chest, not leak Mythic/Ancient");
        }

        // Uniform [0.0, 1.0) sweep across 10,000 samples
        int[] counts = new int[SpellRarityDistribution.TIER_COUNT];
        for (int i = 0; i < 10_000; i++) {
            double q = (i + 0.5) / 10_000.0;
            counts[SpellRarityDistribution.pickTargetRarityIndex(q, 0, 6, w)]++;
        }
        assertEquals(2200, counts[0], 5);
        assertEquals(2200, counts[1], 5);
        assertEquals(2000, counts[2], 5);
        assertEquals(1600, counts[3], 5);
        assertEquals(1100, counts[4], 5);
        assertEquals(600, counts[5], 5);
        assertEquals(300, counts[6], 5);

        // High-end vault chest [0.5, 1.0): Mythic ~12%, Ancient ~6%
        int[] highCounts = new int[SpellRarityDistribution.TIER_COUNT];
        for (int i = 0; i < 10_000; i++) {
            double q = 0.5 + ((i + 0.5) / 10_000.0) * 0.5;
            highCounts[SpellRarityDistribution.pickTargetRarityIndex(q, 0, 6, w)]++;
        }
        assertEquals(1200, highCounts[5], 5);
        assertEquals(600, highCounts[6], 5);

        assertEquals(0, SpellRarityDistribution.pickTargetRarityIndex(Double.NaN, 0, 6, w));
        assertEquals(6, SpellRarityDistribution.pickTargetRarityIndex(2.5, 0, 6, w));
    }

    @Test
    void pickLevelForRarityHandlesMultiLevelSingleLevelAndSkippedTiers() {
        IntUnaryOperator tenLevelMap = lvl -> {
            if (lvl <= 2) return 0;
            if (lvl <= 4) return 1;
            if (lvl <= 6) return 2;
            if (lvl == 7) return 3;
            if (lvl == 8) return 4;
            if (lvl == 9) return 5;
            return 6;
        };

        assertEquals(1, SpellRarityDistribution.pickLevelForRarity(1, 10, tenLevelMap, 0, 0.0));
        assertEquals(2, SpellRarityDistribution.pickLevelForRarity(1, 10, tenLevelMap, 0, 0.99));
        assertEquals(5, SpellRarityDistribution.pickLevelForRarity(1, 10, tenLevelMap, 2, 0.1));
        assertEquals(7, SpellRarityDistribution.pickLevelForRarity(1, 10, tenLevelMap, 3, 0.5));
        assertEquals(8, SpellRarityDistribution.pickLevelForRarity(1, 10, tenLevelMap, 4, 0.5));
        assertEquals(9, SpellRarityDistribution.pickLevelForRarity(1, 10, tenLevelMap, 5, 0.5));
        assertEquals(10, SpellRarityDistribution.pickLevelForRarity(1, 10, tenLevelMap, 6, 0.5));

        // Single-level high-tier spell
        assertEquals(1, SpellRarityDistribution.pickLevelForRarity(1, 1, lvl -> 6, 0, 0.5));

        // Skipped tier 3 -> nearest valid rarity (2 -> level 3, or 4 -> level 4; level 3 comes first with dist 1)
        IntUnaryOperator skippedTierMap = lvl -> lvl < 4 ? lvl - 1 : 4;
        int picked = SpellRarityDistribution.pickLevelForRarity(1, 4, skippedTierMap, 3, 0.5);
        assertTrue(picked == 3 || picked == 4);
    }

    @Test
    void loadWeightsFromFileParsesNestedAndFlatJsonAndFallsBackOnInvalid() throws Exception {
        Path nested = tempDir.resolve("nested.json");
        Files.writeString(nested, """
                {
                  "rarities": {
                    "common": 0.25,
                    "uncommon": 0.25,
                    "rare": 0.20,
                    "epic": 0.15,
                    "legendary": 0.10,
                    "mythic": 0.03,
                    "ancient": 0.02
                  }
                }
                """, StandardCharsets.UTF_8);
        assertArrayEquals(new double[]{0.25, 0.25, 0.20, 0.15, 0.10, 0.03, 0.02}, SpellRarityDistribution.loadWeightsFromFile(nested), 1.0e-9);

        Path flat = tempDir.resolve("flat.json");
        Files.writeString(flat, """
                {
                  "common": 22, "uncommon": 22, "rare": 20, "epic": 16,
                  "legendary": 11, "mythic": 6, "ancient": 3
                }
                """, StandardCharsets.UTF_8);
        // Sum 100 != [0.99, 1.01] -> rejects unnormalized integers outside probability domain and falls back cleanly
        assertArrayEquals(SpellRarityDistribution.defaultWeights(), SpellRarityDistribution.loadWeightsFromFile(flat));

        Path flatNorm = tempDir.resolve("flat-norm.json");
        Files.writeString(flatNorm, """
                {
                  "common": 0.20, "uncommon": 0.20, "rare": 0.20, "epic": 0.20,
                  "legendary": 0.10, "mythic": 0.06, "ancient": 0.04
                }
                """, StandardCharsets.UTF_8);
        assertArrayEquals(new double[]{0.20, 0.20, 0.20, 0.20, 0.10, 0.06, 0.04}, SpellRarityDistribution.loadWeightsFromFile(flatNorm), 1.0e-9);

        Path invalid = tempDir.resolve("invalid.json");
        Files.writeString(invalid, "{ broken", StandardCharsets.UTF_8);
        assertArrayEquals(SpellRarityDistribution.defaultWeights(), SpellRarityDistribution.loadWeightsFromFile(invalid));
        assertArrayEquals(SpellRarityDistribution.defaultWeights(), SpellRarityDistribution.loadWeightsFromFile(tempDir.resolve("missing.json")));
    }

    @Test
    void getSpellFilterWeightSupportsAllSevenTiers() {
        assertEquals(40, SpellRarityDistribution.getSpellFilterWeight(-1));
        assertEquals(40, SpellRarityDistribution.getSpellFilterWeight(0));
        assertEquals(30, SpellRarityDistribution.getSpellFilterWeight(1));
        assertEquals(15, SpellRarityDistribution.getSpellFilterWeight(2));
        assertEquals(8, SpellRarityDistribution.getSpellFilterWeight(3));
        assertEquals(4, SpellRarityDistribution.getSpellFilterWeight(4));
        assertEquals(2, SpellRarityDistribution.getSpellFilterWeight(5));
        assertEquals(1, SpellRarityDistribution.getSpellFilterWeight(6));
        assertEquals(1, SpellRarityDistribution.getSpellFilterWeight(99));
    }
}
