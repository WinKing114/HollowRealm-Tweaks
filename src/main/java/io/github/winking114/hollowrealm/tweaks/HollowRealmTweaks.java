package io.github.winking114.hollowrealm.tweaks;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Mod(HollowRealmTweaks.MOD_ID)
public final class HollowRealmTweaks {
    public static final String MOD_ID = "hollowrealm_tweaks";
    private static final Logger LOGGER = LogUtils.getLogger();

    private final OverrideResolver resolver = new OverrideResolver(HollowRealmTweaks::readModResource, new ForgeModState());

    public HollowRealmTweaks() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::addCompatibilityPack);
    }

    private void addCompatibilityPack(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        Map<ResourceLocation, byte[]> overrides = resolver.resolve();
        if (overrides.isEmpty()) return;
        var pack = CompatPackResources.createPack(overrides);
        if (pack == null) throw new IllegalStateException("Built-in compatibility pack metadata could not be read");
        event.addRepositorySource(consumer -> consumer.accept(pack));
        LOGGER.info("[HollowRealm Tweaks] REGISTER {}: {} audited overrides", CompatPackResources.PACK_ID, overrides.size());
    }

    private static Optional<byte[]> readModResource(String modId, String path) {
        var target = ModList.get().getModFileById(modId);
        if (target == null) return Optional.empty();
        Path resource = target.getFile().findResource(path);
        if (resource == null || !Files.isRegularFile(resource)) return Optional.empty();
        try {
            return Optional.of(Files.readAllBytes(resource));
        } catch (IOException failure) {
            LOGGER.warn("[HollowRealm Tweaks] SKIP {} {}: cannot read target resource", modId, path, failure);
            return Optional.empty();
        }
    }

    @FunctionalInterface
    interface ResourceReader {
        Optional<byte[]> read(String modId, String path);
    }

    interface ModState {
        boolean isLoaded(String modId);
        String version(String modId);
    }

    private static final class ForgeModState implements ModState {
        @Override public boolean isLoaded(String modId) {
            return ModList.get().isLoaded(modId);
        }

        @Override public String version(String modId) {
            return ModList.get().getModContainerById(modId).map(mod -> mod.getModInfo().getVersion().toString()).orElse("");
        }
    }

    static final class OverrideResolver {
        private final ResourceReader reader;
        private final ModState modState;
        private volatile Map<ResourceLocation, byte[]> cached;

        OverrideResolver(ResourceReader reader, ModState modState) {
            this.reader = reader;
            this.modState = modState;
        }

        Map<ResourceLocation, byte[]> resolve() {
            Map<ResourceLocation, byte[]> snapshot = this.cached;
            if (snapshot != null) return snapshot;
            synchronized (this) {
                snapshot = this.cached;
                if (snapshot != null) return snapshot;
                snapshot = buildOverrides(reader, modState, true);
                this.cached = snapshot;
                return snapshot;
            }
        }
    }

    static Map<ResourceLocation, byte[]> buildOverrides(ResourceReader reader, ModState modState) {
        return buildOverrides(reader, modState, true);
    }

    static Map<ResourceLocation, byte[]> buildOverrides(ResourceReader reader, ModState modState, boolean logDecisions) {
        var overrides = new HashMap<ResourceLocation, byte[]>();

        registerOverride(overrides,
                ResourceLocation.fromNamespaceAndPath("crittersandcompanions", "loot_modifiers/inject_clams.json"),
                reader.read("crittersandcompanions", ClamConditionPatch.RESOURCE_PATH).flatMap(ClamConditionPatch::apply),
                "clam_conditions", "three condition IDs only", logDecisions);

        var author = reader.read("legendary_monsters", BottleRecipePatch.RESOURCE_PATH);
        var addon = reader.read("legendary_delicacies", BottleRecipePatch.RESOURCE_PATH);
        registerOverride(overrides,
                ResourceLocation.fromNamespaceAndPath("legendary_monsters", "recipes/bottle_of_annihilation.json"),
                author.flatMap(bytes -> addon.flatMap(other -> BottleRecipePatch.apply(bytes, other))),
                "bottle_recipe", "audited author recipe, 8 bottles", logDecisions);

        String placeboVersion = modState.version("placebo");
        boolean scguns = modState.isLoaded("scguns");
        if (scguns || !"8.6.3".equals(placeboVersion)) {
            if (logDecisions) {
                LOGGER.info("[HollowRealm Tweaks] SKIP gun_conditions: Scguns installed or Placebo version not audited");
            }
        } else {
            for (String name : GunAffixPatch.resourceNames().stream().sorted().toList()) {
                String path = "affixes/gun/attribute/" + name + ".json";
                registerOverride(overrides,
                        ResourceLocation.fromNamespaceAndPath("fallen_gems_affixes", path),
                        reader.read("fallen_gems_affixes", "data/fallen_gems_affixes/" + path)
                                .flatMap(bytes -> GunAffixPatch.apply(name, bytes, false, placeboVersion)),
                        "gun_conditions", name + " requires scguns", logDecisions);
            }
        }
        return Map.copyOf(overrides);
    }

    private static void registerOverride(
            Map<ResourceLocation, byte[]> overrides,
            ResourceLocation id,
            Optional<byte[]> patched,
            String patchName,
            String applyDetail,
            boolean logDecisions) {
        if (patched.isPresent()) {
            overrides.put(id, patched.get());
            if (logDecisions) {
                LOGGER.info("[HollowRealm Tweaks] APPLY {}: {}", patchName, applyDetail);
            }
        } else if (logDecisions) {
            LOGGER.info("[HollowRealm Tweaks] SKIP {}: target absent or resource differs from audited input", patchName);
        }
    }
}
