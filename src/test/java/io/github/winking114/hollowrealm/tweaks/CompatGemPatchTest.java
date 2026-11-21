package io.github.winking114.hollowrealm.tweaks;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class CompatGemPatchTest {
    private byte[] fixture(String name) throws Exception {
        try (var input = getClass().getResourceAsStream("/fixtures/fga/" + name + ".json")) {
            assertNotNull(input);
            return input.readAllBytes();
        }
    }

    @Test void addsAdditionalAttributesConditionToSpellLightningWhenAbsent() throws Exception {
        var source = fixture("spell_lightning");
        assertTrue(ResourceFingerprint.matches(source, CompatGemPatch.SPELL_LIGHTNING_SHA256));
        var patched = CompatGemPatch.applySpellLightning(source, false).orElseThrow();
        var actual = JsonParser.parseString(new String(patched, StandardCharsets.UTF_8)).getAsJsonObject();
        var expected = JsonParser.parseString(new String(source, StandardCharsets.UTF_8)).getAsJsonObject();

        var conditions = actual.remove("conditions").getAsJsonArray();
        expected.remove("conditions");
        assertEquals(2, conditions.size());
        assertEquals("forge:mod_loaded", conditions.get(0).getAsJsonObject().get("type").getAsString());
        assertEquals("irons_spellbooks", conditions.get(0).getAsJsonObject().get("modid").getAsString());
        assertEquals("forge:mod_loaded", conditions.get(1).getAsJsonObject().get("type").getAsString());
        assertEquals("additional_attributes", conditions.get(1).getAsJsonObject().get("modid").getAsString());
        assertEquals(expected, actual);
    }

    @Test void stripsOnlyDevourBonusFromHemolithShardWhenAdditionalAttributesAbsent() throws Exception {
        var source = fixture("hemolith_shard");
        assertTrue(ResourceFingerprint.matches(source, CompatGemPatch.HEMOLITH_SHARD_SHA256));
        var patched = CompatGemPatch.applyHemolithShard(source, false).orElseThrow();
        var actual = JsonParser.parseString(new String(patched, StandardCharsets.UTF_8)).getAsJsonObject();
        var expected = JsonParser.parseString(new String(source, StandardCharsets.UTF_8)).getAsJsonObject();

        var bonuses = actual.getAsJsonArray("bonuses");
        assertEquals(2, bonuses.size());
        assertEquals("irons_spellbooks:blood_spell_power",
                bonuses.get(0).getAsJsonObject().get("attribute").getAsString());
        assertEquals("irons_spellbooks:blood_magic_resist",
                bonuses.get(1).getAsJsonObject().get("attribute").getAsString());

        expected.getAsJsonArray("bonuses").remove(2);
        assertEquals(expected, actual);
    }

    @Test void replacesHemolithShardLeggingsConditionWithForgeFalse() throws Exception {
        var source = fixture("hemolith_shard_leggings");
        assertTrue(ResourceFingerprint.matches(source, CompatGemPatch.HEMOLITH_SHARD_LEGGINGS_SHA256));
        var patched = CompatGemPatch.applyHemolithShardLeggings(source).orElseThrow();
        var actual = JsonParser.parseString(new String(patched, StandardCharsets.UTF_8)).getAsJsonObject();
        var expected = JsonParser.parseString(new String(source, StandardCharsets.UTF_8)).getAsJsonObject();

        var conditions = actual.remove("conditions").getAsJsonArray();
        expected.remove("conditions");
        assertEquals(1, conditions.size());
        assertEquals("forge:false", conditions.get(0).getAsJsonObject().get("type").getAsString());
        assertEquals(expected, actual);
    }

    @Test void skipsSpellLightningAndHemolithShardWhenAdditionalAttributesIsPresent() throws Exception {
        assertTrue(CompatGemPatch.applySpellLightning(fixture("spell_lightning"), true).isEmpty());
        assertTrue(CompatGemPatch.applyHemolithShard(fixture("hemolith_shard"), true).isEmpty());
    }

    @Test void failsClosedOnHashMismatchAndMalformedJsonShapes() throws Exception {
        byte[] tam = "{\"conditions\":[]}".getBytes(StandardCharsets.UTF_8);
        assertTrue(CompatGemPatch.applySpellLightning(tam, false).isEmpty());
        assertTrue(CompatGemPatch.applySpellLightning(new byte[0], false).isEmpty());
        assertTrue(CompatGemPatch.applySpellLightning(null, false).isEmpty());

        assertTrue(CompatGemPatch.applyHemolithShard(tam, false).isEmpty());
        assertTrue(CompatGemPatch.applyHemolithShard(new byte[0], false).isEmpty());
        assertTrue(CompatGemPatch.applyHemolithShard(null, false).isEmpty());

        assertTrue(CompatGemPatch.applyHemolithShardLeggings(tam).isEmpty());
        assertTrue(CompatGemPatch.applyHemolithShardLeggings(new byte[0]).isEmpty());
        assertTrue(CompatGemPatch.applyHemolithShardLeggings(null).isEmpty());

        var badCond = JsonParser.parseString("{\"conditions\":\"invalid\"}").getAsJsonObject();
        assertFalse(CompatGemPatch.addModLoadedCondition(badCond, "additional_attributes"));
        assertFalse(CompatGemPatch.replaceWithFalseCondition(badCond));

        var noBonuses = JsonParser.parseString("{\"conditions\":[]}").getAsJsonObject();
        assertFalse(CompatGemPatch.stripAttributeBonus(noBonuses, CompatGemPatch.DEVOUR_ATTRIBUTE));

        var singleDevourOnly = JsonParser.parseString("""
                {"bonuses":[{"attribute":"additional_attributes:innate_spell/irons_spellbooks/devour"}]}""").getAsJsonObject();
        assertFalse(CompatGemPatch.stripAttributeBonus(singleDevourOnly, CompatGemPatch.DEVOUR_ATTRIBUTE));
    }
}
