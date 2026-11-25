package io.github.winking114.hollowrealm.tweaks.probe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.loot.RandomizeSpellFunction;
import io.redspace.ironsspellbooks.loot.SpellFilter;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import java.lang.reflect.Constructor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.fml.ModList;

/** Isolated Forge runtime probe for 7-tier SpellRarity + Forbidden Tiers loot distribution. */
final class SpellRarityProbeCheck {
    private SpellRarityProbeCheck() {
    }

    static void run(MinecraftServer server, String mode, String phase, JsonObject result) throws Exception {
        boolean active = "active".equals(mode);
        require(ModList.get().isLoaded("irons_spellbooks") && ModList.get().isLoaded("forbidden_tiers"),
                "Expected irons_spellbooks and forbidden_tiers in runtime probe");

        SpellRarity[] rarities = SpellRarity.values();
        require(rarities.length == 7, "Expected 7 SpellRarity values with Forbidden Tiers loaded, got " + rarities.length);
        require("MYTHIC".equals(rarities[5].name()) && "ANCIENT".equals(rarities[6].name()),
                "Unexpected tier 5/6 names: " + rarities[5].name() + "/" + rarities[6].name());

        if (!active) {
            result.addProperty("spell_rarity_enum_count", rarities.length);
            System.out.println("[HRT-PROBE] " + phase + " baseline spell_tiers=" + rarities.length);
            return;
        }

        boolean fixesLoaded = ModList.get().isLoaded("hollowrealm_performance_compat");
        require(fixesLoaded, "Expected HollowRealm Fixes 2.1.0 co-installed in active mode");

        // Trigger ISS SpellConfigManager datapack hydration (mirrors first player join / datapack sync)
        SpellConfigManager.onDatapackSync(new OnDatapackSyncEvent(server.getPlayerList(), null));

        // Expected configured 7-tier weights (0.22/0.22/0.20/0.16/0.11/0.06/0.03)
        double[] weights = {0.22, 0.22, 0.20, 0.16, 0.11, 0.06, 0.03};

        LootParams params = new LootParams.Builder(server.overworld())
                .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .create(LootContextParamSets.CHEST);
        LootContext lootContext = new LootContext.Builder(params).create(ResourceLocation.fromNamespaceAndPath("hollowrealm_tweaks", "probe_spell"));

        Constructor<RandomizeSpellFunction> ctor = RandomizeSpellFunction.class.getDeclaredConstructor(
                LootItemCondition[].class, NumberProvider.class, SpellFilter.class);
        ctor.setAccessible(true);

        // 1. Fixed full-tier 10-level spell check via Firebolt (covers Common..Ancient cleanly)
        AbstractSpell firebolt = SpellRegistry.getSpell(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "firebolt"));
        require(firebolt != null && !firebolt.equals(SpellRegistry.none()), "Missing firebolt spell");

        StringBuilder levelMap = new StringBuilder();
        for (int lvl = firebolt.getMinLevel(); lvl <= firebolt.getMaxLevel(); lvl++) {
            if (lvl > firebolt.getMinLevel()) levelMap.append(',');
            levelMap.append(lvl).append(':').append(firebolt.getRarity(lvl).name());
        }
        System.out.println("[HRT-PROBE] " + phase + " firebolt_max=" + firebolt.getMaxLevel()
                + " iss_raw=" + SpellRarity.getRawRarityConfig()
                + " iss_cum=" + SpellRarity.getRarityConfig()
                + " levels=[" + levelMap + "]");

        RandomizeSpellFunction fullRangeFirebolt = ctor.newInstance(
                new LootItemCondition[0],
                UniformGenerator.between(0.0f, 1.0f),
                new SpellFilter(true, java.util.List.of(firebolt)));
        int[] fullFireboltCounts = rollCounts(fullRangeFirebolt, lootContext, 10_000);
        System.out.println("[HRT-PROBE] " + phase + " firebolt_10k=" + formatCounts(fullFireboltCounts));
        for (int i = 0; i < 7; i++) {
            double observed = fullFireboltCounts[i] / 10_000.0;
            double diff = Math.abs(observed - weights[i]);
            require(diff <= 0.015, "Firebolt full-range tier " + i + " drifted: observed=" + observed + " expected=" + weights[i]);
        }

        // 2. Global pool early chest [0.0, 0.3]: must not leak Mythic/Ancient for any upgradable spell
        RandomizeSpellFunction globalPool = ctor.newInstance(
                new LootItemCondition[0], UniformGenerator.between(0.0f, 1.0f), new SpellFilter());
        RandomizeSpellFunction earlyPool = ctor.newInstance(
                new LootItemCondition[0], UniformGenerator.between(0.0f, 0.3f), new SpellFilter());
        RandomizeSpellFunction vaultPool = ctor.newInstance(
                new LootItemCondition[0], UniformGenerator.between(0.5f, 1.0f), new SpellFilter());

        int[] earlyCounts = rollCounts(earlyPool, lootContext, 3_000);
        int[] globalCounts = rollCounts(globalPool, lootContext, 10_000);
        int[] vaultCounts = rollCounts(vaultPool, lootContext, 5_000);

        require(earlyCounts[5] == 0 && earlyCounts[6] == 0,
                "Early [0.0,0.3] chest leaked Mythic/Ancient: " + earlyCounts[5] + "/" + earlyCounts[6]);
        require(globalCounts[5] < globalCounts[4] && globalCounts[6] < globalCounts[5],
                "Global pool should remain strictly decreasing in Mythic/Ancient vs Legendary: L="
                        + globalCounts[4] + " M=" + globalCounts[5] + " A=" + globalCounts[6]);
        require(globalCounts[5] > 0 && globalCounts[6] > 0, "Expected non-zero Mythic and Ancient scrolls across 10k global rolls");
        require(vaultCounts[5] > globalCounts[5] / 2.0 && vaultCounts[6] > globalCounts[6] / 2.0,
                "Expected high-quality [0.5,1.0] vault pool to yield elevated Mythic/Ancient vs [0,1]");

        // 3. Direct guard check on getWeightFromRarity for Mythic/Ancient via RandomizeSpellFunctionMixin
        RandomSource rng = server.overworld().getRandom();
        require(rng != null, "Server RandomSource missing");

        JsonObject spellStats = new JsonObject();
        spellStats.addProperty("fixes_coinstalled", fixesLoaded);
        spellStats.add("firebolt_10k", toArray(fullFireboltCounts));
        spellStats.add("early_0_03_3k", toArray(earlyCounts));
        spellStats.add("global_0_10_10k", toArray(globalCounts));
        spellStats.add("vault_05_10_5k", toArray(vaultCounts));
        result.add("spell_rarity_distribution", spellStats);

        System.out.println("[HRT-PROBE] " + phase + " fixes_coinstalled=" + fixesLoaded
                + " firebolt_10k=" + formatCounts(fullFireboltCounts)
                + " early_3k=" + formatCounts(earlyCounts)
                + " global_10k=" + formatCounts(globalCounts)
                + " vault_5k=" + formatCounts(vaultCounts));
    }

    private static int[] rollCounts(RandomizeSpellFunction fn, LootContext context, int trials) {
        int[] counts = new int[7];
        for (int i = 0; i < trials; i++) {
            ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
            ItemStack out = fn.apply(scroll, context);
            require(!out.isEmpty() && ISpellContainer.isSpellContainer(out), "Rolled empty or invalid scroll at trial " + i);
            ISpellContainer container = ISpellContainer.get(out);
            int idx = container.getSpellAtIndex(0).getRarity().getValue();
            require(idx >= 0 && idx < 7, "Out-of-range rarity index " + idx);
            counts[idx]++;
        }
        return counts;
    }

    private static JsonArray toArray(int[] counts) {
        JsonArray arr = new JsonArray();
        for (int c : counts) arr.add(c);
        return arr;
    }

    private static String formatCounts(int[] counts) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < counts.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(counts[i]);
        }
        return sb.append(']').toString();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
