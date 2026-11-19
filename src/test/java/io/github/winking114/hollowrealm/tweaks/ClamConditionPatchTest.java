package io.github.winking114.hollowrealm.tweaks;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class ClamConditionPatchTest {
    private byte[] original() throws Exception {
        try (var input = getClass().getResourceAsStream("/fixtures/inject_clams-original.json")) {
            assertNotNull(input);
            return input.readAllBytes();
        }
    }

    @Test void repairsOnlyTheThreeConditionIdsOnTheAuditedInput() throws Exception {
        byte[] source = original();
        byte[] result = ClamConditionPatch.apply(source).orElseThrow();
        var actual = JsonParser.parseString(new String(result, StandardCharsets.UTF_8)).getAsJsonObject();
        var expected = JsonParser.parseString(new String(source, StandardCharsets.UTF_8)).getAsJsonObject();
        var terms = expected.getAsJsonArray("conditions").get(0).getAsJsonObject().getAsJsonArray("terms");
        assertEquals(3, terms.size());
        for (var term : terms) term.getAsJsonObject().addProperty("condition", "forge:loot_table_id");
        assertEquals(expected, actual);
        assertEquals(0.1, actual.getAsJsonArray("conditions").get(1).getAsJsonObject().get("chance").getAsDouble());
        assertArrayEquals(original(), source);
    }

    @Test void refusesChangedOrAlreadyRepairedResources() throws Exception {
        String source = new String(original(), StandardCharsets.UTF_8);
        assertTrue(ClamConditionPatch.apply(source.replace("0.1", "0.2").getBytes(StandardCharsets.UTF_8)).isEmpty());
        assertTrue(ClamConditionPatch.apply(source.replace("neoforge:loot_table_id", "forge:loot_table_id").getBytes(StandardCharsets.UTF_8)).isEmpty());
        assertTrue(ClamConditionPatch.apply(new byte[0]).isEmpty());
    }
}
