package io.github.winking114.hollowrealm.tweaks;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class HRTweaksMixinPluginTest {
    private static final String PKG = "io.github.winking114.hollowrealm.tweaks.mixin.";

    @AfterEach
    void tearDown() {
        HRTweaksMixinPlugin.resetVersionCacheForTests();
    }

    private static byte[] loadClassBytesFromMods(String classPath) throws IOException {
        Path modsDir = Path.of(System.getProperty("hrtweaks.mods", "C:/Users/WinKing/Documents/.minecraft/versions/HollowRealm/mods"));
        assertTrue(Files.isDirectory(modsDir), "Missing mods directory for fingerprint check: " + modsDir);
        String[] candidateJars = {
                "fallen_gems_affixes-1.20.1-2.1.5.jar",
                "[Iron的法术与魔法书] irons_spellbooks-1.20.1-3.16.4.jar"
        };
        for (String jarName : candidateJars) {
            Path jarPath = modsDir.resolve(jarName);
            if (!Files.isRegularFile(jarPath)) {
                continue;
            }
            try (ZipFile zip = new ZipFile(jarPath.toFile())) {
                ZipEntry entry = zip.getEntry(classPath);
                if (entry != null) {
                    try (InputStream in = zip.getInputStream(entry)) {
                        return in.readAllBytes();
                    }
                }
            }
        }
        return null;
    }

    @Test
    void appliesAllThreeMixinsWhenAuditedVersionsAndBytecodeMatch() throws Exception {
        HRTweaksMixinPlugin.setInstalledVersionsForTests(Map.of(
                "apotheosis", "7.4.8",
                "ancientreforging", "1.3",
                "fallen_gems_affixes", "1.20.1-2.1.4",
                "irons_spellbooks", "1.20.1-3.16.4",
                "forbidden_tiers", "1.0.0"
        ));
        HRTweaksMixinPlugin.setResourceLookupForTests(HRTweaksMixinPluginTest::loadClassBytesFromMods);

        HRTweaksMixinPlugin plugin = new HRTweaksMixinPlugin();
        assertTrue(plugin.shouldApplyMixin("net.kayn.fallen_gems_affixes.adventure.reforging.FabledReforgingTableTile", PKG + "FabledReforgingTableTileMixin"));
        assertTrue(plugin.shouldApplyMixin("net.kayn.fallen_gems_affixes.adventure.reforging.FabledReforgingMenu", PKG + "FabledReforgingMenuMixin"));
        assertTrue(plugin.shouldApplyMixin("net.kayn.fallen_gems_affixes.adventure.reforging.FabledReforgingMenu$FabledResultSlot", PKG + "FabledResultSlotMixin"));
        assertTrue(plugin.shouldApplyMixin("io.redspace.ironsspellbooks.loot.RandomizeSpellFunction", PKG + "RandomizeSpellFunctionMixin"));
    }

    @Test
    void acceptsAlternateAuditedModStrings() {
        HRTweaksMixinPlugin.setInstalledVersionsForTests(Map.of(
                "apotheosis", "7.4.8",
                "ancientreforging", "1.3",
                "fallen_gems_affixes", "1.20.1-2.1.5",
                "irons_spellbooks", "1.20.1-3.16.4",
                "forbidden_tiers", "1.0.1"
        ));
        HRTweaksMixinPlugin.setResourceLookupForTests(HRTweaksMixinPluginTest::loadClassBytesFromMods);

        HRTweaksMixinPlugin plugin = new HRTweaksMixinPlugin();
        assertTrue(plugin.shouldApplyMixin("net.kayn.fallen_gems_affixes.adventure.reforging.FabledReforgingTableTile", PKG + "FabledReforgingTableTileMixin"));
        assertTrue(plugin.shouldApplyMixin("net.kayn.fallen_gems_affixes.adventure.reforging.FabledReforgingMenu", PKG + "FabledReforgingMenuMixin"));
        assertTrue(plugin.shouldApplyMixin("net.kayn.fallen_gems_affixes.adventure.reforging.FabledReforgingMenu$FabledResultSlot", PKG + "FabledResultSlotMixin"));
        assertTrue(plugin.shouldApplyMixin("io.redspace.ironsspellbooks.loot.RandomizeSpellFunction", PKG + "RandomizeSpellFunctionMixin"));
    }

    @Test
    void failsClosedOnMissingModOrVersionMismatchOrBytecodeTamper() {
        HRTweaksMixinPlugin.setInstalledVersionsForTests(Map.of(
                "apotheosis", "7.4.8",
                "fallen_gems_affixes", "1.20.1-2.1.4",
                "irons_spellbooks", "1.20.1-3.16.4",
                "forbidden_tiers", "2.0.0"
        ));
        HRTweaksMixinPlugin.setResourceLookupForTests(HRTweaksMixinPluginTest::loadClassBytesFromMods);

        HRTweaksMixinPlugin missingModPlugin = new HRTweaksMixinPlugin();
        assertFalse(missingModPlugin.shouldApplyMixin("x", PKG + "FabledReforgingTableTileMixin"));
        assertFalse(missingModPlugin.shouldApplyMixin("x", PKG + "FabledReforgingMenuMixin"));
        assertFalse(missingModPlugin.shouldApplyMixin("x", PKG + "FabledResultSlotMixin"));
        assertFalse(missingModPlugin.shouldApplyMixin("x", PKG + "RandomizeSpellFunctionMixin"));
        assertFalse(missingModPlugin.shouldApplyMixin("x", PKG + "UnknownMixin"));

        HRTweaksMixinPlugin.setInstalledVersionsForTests(Map.of(
                "apotheosis", "7.4.8",
                "ancientreforging", "1.3",
                "fallen_gems_affixes", "1.20.1-2.1.4",
                "irons_spellbooks", "1.20.1-3.16.4",
                "forbidden_tiers", "1.0.0"
        ));
        HRTweaksMixinPlugin.setResourceLookupForTests(path -> {
            byte[] real = loadClassBytesFromMods(path);
            if (HRTweaksMixinPlugin.FABLED_RESULT_SLOT_CLASS.equals(path) && real != null) {
                byte[] copy = real.clone();
                copy[copy.length - 1] ^= 0x5A;
                return copy;
            }
            return real;
        });

        HRTweaksMixinPlugin tamperedSlotPlugin = new HRTweaksMixinPlugin();
        assertFalse(tamperedSlotPlugin.shouldApplyMixin("x", PKG + "FabledReforgingTableTileMixin"));
        assertFalse(tamperedSlotPlugin.shouldApplyMixin("x", PKG + "FabledReforgingMenuMixin"));
        assertFalse(tamperedSlotPlugin.shouldApplyMixin("x", PKG + "FabledResultSlotMixin"));
        assertTrue(tamperedSlotPlugin.shouldApplyMixin("x", PKG + "RandomizeSpellFunctionMixin"));
    }
}
