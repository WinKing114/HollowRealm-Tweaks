package io.github.winking114.hollowrealm.tweaks;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CompatPackResourcesTest {
    private static final ResourceLocation CLAMS = ResourceLocation.fromNamespaceAndPath("crittersandcompanions", "loot_modifiers/inject_clams.json");
    @BeforeAll static void version() { SharedConstants.tryDetectVersion(); }

    @Test void independentTopPackWinsAgainstBrokenBaseAndSurvivesRepositoryReload() throws Exception {
        PackRepository repository = new PackRepository(consumer -> {
            consumer.accept(Pack.readMetaAndCreate("original", Component.literal("Original"), false,
                    id -> new CompatPackResources(id, Map.of(CLAMS, "broken".getBytes(StandardCharsets.UTF_8))),
                    PackType.SERVER_DATA, Pack.Position.BOTTOM, PackSource.BUILT_IN));
            consumer.accept(CompatPackResources.createPack(Map.of(CLAMS, "repaired".getBytes(StandardCharsets.UTF_8))));
        });
        for (int reload = 0; reload < 2; reload++) {
            repository.reload();
            repository.setSelected(List.of("original"));
            assertTrue(repository.getSelectedIds().contains(CompatPackResources.PACK_ID));
            try (var manager = new MultiPackResourceManager(PackType.SERVER_DATA, repository.openAllSelected())) {
                var winner = manager.getResource(CLAMS).orElseThrow();
                assertEquals(CompatPackResources.PACK_ID, winner.sourcePackId());
                try (var input = winner.open()) { assertEquals("repaired", new String(input.readAllBytes(), StandardCharsets.UTF_8)); }
            }
        }
    }

    @Test void exposesOnlyExactServerResourcesAndProtectsInputBytes() throws Exception {
        byte[] original = "fixed".getBytes(StandardCharsets.UTF_8);
        try (var pack = new CompatPackResources("test", Map.of(CLAMS, original))) {
            original[0] = 'X';
            assertEquals(java.util.Set.of("crittersandcompanions"), pack.getNamespaces(PackType.SERVER_DATA));
            assertTrue(pack.getNamespaces(PackType.CLIENT_RESOURCES).isEmpty());
            assertNull(pack.getResource(PackType.CLIENT_RESOURCES, CLAMS));
            assertNull(pack.getResource(PackType.SERVER_DATA, ResourceLocation.fromNamespaceAndPath("minecraft", "loot_modifiers/inject_clams.json")));
            var listed = new ArrayList<ResourceLocation>();
            pack.listResources(PackType.SERVER_DATA, "crittersandcompanions", "loot_modifiers", (id, source) -> listed.add(id));
            assertEquals(List.of(CLAMS), listed);
            pack.listResources(PackType.SERVER_DATA, "minecraft", "", (id, source) -> fail("Wrong namespace leaked"));
            try (var input = pack.getResource(PackType.SERVER_DATA, CLAMS).get()) {
                assertEquals("fixed", new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    @Test void normalizesTrailingSlashAndExactPathInListResources() {
        byte[] payload = "ok".getBytes(StandardCharsets.UTF_8);
        try (var pack = new CompatPackResources("test", Map.of(CLAMS, payload))) {
            var withSlash = new ArrayList<ResourceLocation>();
            pack.listResources(PackType.SERVER_DATA, "crittersandcompanions", "loot_modifiers/", (id, source) -> withSlash.add(id));
            assertEquals(List.of(CLAMS), withSlash);

            var exact = new ArrayList<ResourceLocation>();
            pack.listResources(PackType.SERVER_DATA, "crittersandcompanions", "loot_modifiers/inject_clams.json", (id, source) -> exact.add(id));
            assertEquals(List.of(CLAMS), exact);

            var partialMismatch = new ArrayList<ResourceLocation>();
            pack.listResources(PackType.SERVER_DATA, "crittersandcompanions", "loot_mod", (id, source) -> partialMismatch.add(id));
            assertTrue(partialMismatch.isEmpty());
        }
    }
}
