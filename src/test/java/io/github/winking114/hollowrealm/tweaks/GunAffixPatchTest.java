package io.github.winking114.hollowrealm.tweaks;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GunAffixPatchTest {
    private static final List<String> FILES = List.of("bullet_damage", "projectile_speed", "reload_speed", "spread_reduction");
    private byte[] fixture(String name) throws Exception {
        try (var input = getClass().getResourceAsStream("/fixtures/fga/" + name + ".json")) {
            assertNotNull(input);
            return input.readAllBytes();
        }
    }

    @Test void addsOnlyTheNativeModLoadedConditionToAllFourAuditedAffixes() throws Exception {
        for (String name : FILES) {
            var source = fixture(name);
            var result = GunAffixPatch.apply(name, source, false, "8.6.3").orElseThrow();
            var actual = JsonParser.parseString(new String(result, StandardCharsets.UTF_8)).getAsJsonObject();
            var conditions = actual.remove("conditions").getAsJsonArray();
            assertEquals(1, conditions.size());
            var condition = conditions.get(0).getAsJsonObject();
            assertEquals("forge:mod_loaded", condition.get("type").getAsString());
            assertEquals("scguns", condition.get("modid").getAsString());
            assertEquals(JsonParser.parseString(new String(source, StandardCharsets.UTF_8)), actual);
        }
    }

    @Test void leavesInstalledDependencyAndUnauditedLoaderUntouched() throws Exception {
        for (String name : FILES) {
            assertTrue(GunAffixPatch.apply(name, fixture(name), true, "8.6.3").isEmpty());
            assertTrue(GunAffixPatch.apply(name, fixture(name), false, "8.6.4").isEmpty());
            assertTrue(GunAffixPatch.apply(name, new byte[0], false, "8.6.3").isEmpty());
        }
        assertTrue(GunAffixPatch.apply("unknown", fixture("bullet_damage"), false, "8.6.3").isEmpty());
    }

    @Test void preservesExistingConditionsAndRejectsInvalidShapes() {
        var obj = JsonParser.parseString("""
                {"conditions":[{"type":"forge:true"}],"type":"apotheosis:attribute"}""").getAsJsonObject();
        assertTrue(GunAffixPatch.addScgunsCondition(obj));
        var arr = obj.getAsJsonArray("conditions");
        assertEquals(2, arr.size());
        assertEquals("forge:true", arr.get(0).getAsJsonObject().get("type").getAsString());
        assertEquals("scguns", arr.get(1).getAsJsonObject().get("modid").getAsString());
        // Idempotent if scguns condition already exists
        assertTrue(GunAffixPatch.addScgunsCondition(obj));
        assertEquals(2, obj.getAsJsonArray("conditions").size());

        var bad = JsonParser.parseString("""
                {"conditions":"invalid"}""").getAsJsonObject();
        assertFalse(GunAffixPatch.addScgunsCondition(bad));
    }
}
