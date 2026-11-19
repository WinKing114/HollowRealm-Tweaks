package io.github.winking114.hollowrealm.tweaks;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class BottleRecipePatchTest {
    private byte[] fixture(String name) throws Exception {
        try (var input = getClass().getResourceAsStream("/fixtures/" + name)) {
            assertNotNull(input);
            return input.readAllBytes();
        }
    }

    @Test void restoresTheExactAuthorRecipeOnlyForTheAuditedCollision() throws Exception {
        var correct = fixture("bottle-correct.json");
        var broken = fixture("bottle-broken.json");
        var result = BottleRecipePatch.apply(correct, broken).orElseThrow();
        assertArrayEquals(correct, result);
        var json = JsonParser.parseString(new String(result, StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals("legendary_monsters:bottle_of_annihilation", json.getAsJsonObject("result").get("item").getAsString());
        assertEquals(8, json.getAsJsonObject("result").get("count").getAsInt());
        assertEquals("#", json.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("i", json.getAsJsonArray("pattern").get(1).getAsString());
        result[0] = '!';
        assertArrayEquals(fixture("bottle-correct.json"), correct);
    }

    @Test void refusesUpstreamRecipeChangesAndAlreadyFixedAddon() throws Exception {
        var correct = fixture("bottle-correct.json");
        var broken = fixture("bottle-broken.json");
        assertTrue(BottleRecipePatch.apply(correct, correct).isEmpty());
        assertTrue(BottleRecipePatch.apply(new String(correct, StandardCharsets.UTF_8).replace("8", "4").getBytes(StandardCharsets.UTF_8), broken).isEmpty());
        assertTrue(BottleRecipePatch.apply(correct, new byte[0]).isEmpty());
    }
}
