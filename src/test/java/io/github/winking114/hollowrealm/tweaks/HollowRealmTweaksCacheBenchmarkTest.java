package io.github.winking114.hollowrealm.tweaks;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class HollowRealmTweaksCacheBenchmarkTest {
    @BeforeAll static void version() { SharedConstants.tryDetectVersion(); }

    private Map<String, byte[]> fixtures() throws Exception {
        var map = new HashMap<String, byte[]>();
        map.put("crittersandcompanions:" + ClamConditionPatch.RESOURCE_PATH,
                getClass().getResourceAsStream("/fixtures/inject_clams-original.json").readAllBytes());
        map.put("legendary_monsters:" + BottleRecipePatch.RESOURCE_PATH,
                getClass().getResourceAsStream("/fixtures/bottle-correct.json").readAllBytes());
        map.put("legendary_delicacies:" + BottleRecipePatch.RESOURCE_PATH,
                getClass().getResourceAsStream("/fixtures/bottle-broken.json").readAllBytes());
        map.put("fallen_gems_affixes:data/fallen_gems_affixes/affixes/gun/attribute/bullet_damage.json",
                getClass().getResourceAsStream("/fixtures/fga/bullet_damage.json").readAllBytes());
        map.put("fallen_gems_affixes:data/fallen_gems_affixes/affixes/gun/attribute/projectile_speed.json",
                getClass().getResourceAsStream("/fixtures/fga/projectile_speed.json").readAllBytes());
        map.put("fallen_gems_affixes:data/fallen_gems_affixes/affixes/gun/attribute/reload_speed.json",
                getClass().getResourceAsStream("/fixtures/fga/reload_speed.json").readAllBytes());
        map.put("fallen_gems_affixes:data/fallen_gems_affixes/affixes/gun/attribute/spread_reduction.json",
                getClass().getResourceAsStream("/fixtures/fga/spread_reduction.json").readAllBytes());
        map.put("fallen_gems_affixes:data/fallen_gems_affixes/" + CompatGemPatch.SPELL_LIGHTNING_PATH,
                getClass().getResourceAsStream("/fixtures/fga/spell_lightning.json").readAllBytes());
        map.put("fallen_gems_affixes:data/fallen_gems_affixes/" + CompatGemPatch.HEMOLITH_SHARD_PATH,
                getClass().getResourceAsStream("/fixtures/fga/hemolith_shard.json").readAllBytes());
        map.put("fallen_gems_affixes:data/fallen_gems_affixes/" + CompatGemPatch.HEMOLITH_SHARD_LEGGINGS_PATH,
                getClass().getResourceAsStream("/fixtures/fga/hemolith_shard_leggings.json").readAllBytes());
        return Map.copyOf(map);
    }

    @Test void resolvesOnceAndCachesOverridesAcrossRepeatedPackFinderCalls() throws Exception {
        var resources = fixtures();
        var reads = new AtomicInteger();
        HollowRealmTweaks.ResourceReader reader = (modId, path) -> {
            reads.incrementAndGet();
            return Optional.ofNullable(resources.get(modId + ":" + path));
        };
        HollowRealmTweaks.ModState state = new HollowRealmTweaks.ModState() {
            @Override public boolean isLoaded(String modId) { return false; }
            @Override public String version(String modId) { return "placebo".equals(modId) ? "8.6.3" : ""; }
        };

        var resolver = new HollowRealmTweaks.OverrideResolver(reader, state);
        var first = resolver.resolve();
        assertEquals(9, first.size());
        int initialReads = reads.get();
        assertEquals(10, initialReads);

        for (int i = 0; i < 10; i++) {
            assertSame(first, resolver.resolve());
        }
        assertEquals(initialReads, reads.get(), "Subsequent pack-finder resolutions must reuse cached overrides");
    }

    @Test void skipsAdditionalAttributesGemOverridesWhenAdditionalAttributesIsLoaded() throws Exception {
        var resources = fixtures();
        HollowRealmTweaks.ResourceReader reader = (modId, path) ->
                Optional.ofNullable(resources.get(modId + ":" + path));
        HollowRealmTweaks.ModState state = new HollowRealmTweaks.ModState() {
            @Override public boolean isLoaded(String modId) {
                return CompatGemPatch.ADDITIONAL_ATTRIBUTES_MOD_ID.equals(modId);
            }
            @Override public String version(String modId) { return "placebo".equals(modId) ? "8.6.3" : ""; }
        };

        var overrides = HollowRealmTweaks.buildOverrides(reader, state, false);
        assertEquals(7, overrides.size());
        assertFalse(overrides.containsKey(ResourceLocation.fromNamespaceAndPath("fallen_gems_affixes", CompatGemPatch.SPELL_LIGHTNING_PATH)));
        assertFalse(overrides.containsKey(ResourceLocation.fromNamespaceAndPath("fallen_gems_affixes", CompatGemPatch.HEMOLITH_SHARD_PATH)));
        assertTrue(overrides.containsKey(ResourceLocation.fromNamespaceAndPath("fallen_gems_affixes", CompatGemPatch.HEMOLITH_SHARD_LEGGINGS_PATH)));
    }

    @Test void benchmarkCachedResolutionVersusUncachedPerEventRecomputation() throws Exception {
        var resources = fixtures();
        HollowRealmTweaks.ResourceReader reader = (modId, path) ->
                Optional.ofNullable(resources.get(modId + ":" + path));
        HollowRealmTweaks.ModState state = new HollowRealmTweaks.ModState() {
            @Override public boolean isLoaded(String modId) { return false; }
            @Override public String version(String modId) { return "placebo".equals(modId) ? "8.6.3" : ""; }
        };
        var resolver = new HollowRealmTweaks.OverrideResolver(reader, state);
        resolver.resolve();

        final int warmup = 1000;
        final int iterations = 10000;
        long sink = 0;
        for (int i = 0; i < warmup; i++) {
            sink += HollowRealmTweaks.buildOverrides(reader, state, false).size();
            sink += resolver.resolve().size();
        }

        long t0 = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            sink += HollowRealmTweaks.buildOverrides(reader, state, false).size();
        }
        long uncachedNs = System.nanoTime() - t0;

        long t1 = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            sink += resolver.resolve().size();
        }
        long cachedNs = System.nanoTime() - t1;

        double speedup = (double) uncachedNs / Math.max(1L, cachedNs);
        System.out.printf("[BENCHMARK-TWEAKS] iterations=%d uncached_ms=%.3f cached_ms=%.3f speedup=%.2fx sink=%d%n",
                iterations, uncachedNs / 1_000_000.0D, cachedNs / 1_000_000.0D, speedup, sink);
        assertTrue(sink > 0);
        assertTrue(cachedNs < uncachedNs, "Expected cached resolution (" + cachedNs + " ns) < uncached (" + uncachedNs + " ns)");
    }
}
