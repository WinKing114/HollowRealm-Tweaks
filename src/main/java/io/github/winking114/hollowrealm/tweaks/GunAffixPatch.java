package io.github.winking114.hollowrealm.tweaks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Placebo 8.6.3 checks native conditions before decoding optional affix payloads. */
final class GunAffixPatch {
    private static final Map<String, String> HASHES = Map.of(
            "bullet_damage", "1632d01fd3ea33d1140c2967c591ad4a3e82ee52ce7aeb4056cd9b9321166b0d",
            "projectile_speed", "c42ed1c13e5ce758e0133c308ced0099a2d2e2e57befdf58247fa08e4e83ada6",
            "reload_speed", "7cecd11d1f1442cfe72764d9c64b8b29d9e5a58d7ef7d94a98cd82d4e98660fe",
            "spread_reduction", "2985fd0ddae54448e9805b97c8294b88cc4e70f66bbc0c471c98a9198d11561f");
    private GunAffixPatch() {}
    static Set<String> resourceNames() { return HASHES.keySet(); }

    static Optional<byte[]> apply(String name, byte[] source, boolean scgunsPresent, String placeboVersion) {
        if (scgunsPresent || !"8.6.3".equals(placeboVersion) || !HASHES.containsKey(name)
                || !ResourceFingerprint.matches(source, HASHES.get(name))) return Optional.empty();
        try {
            var parsed = JsonParser.parseString(new String(source, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) return Optional.empty();
            var json = parsed.getAsJsonObject();
            if (!addScgunsCondition(json)) return Optional.empty();
            return Optional.of(json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (JsonParseException | IllegalStateException ignored) {
            return Optional.empty();
        }
    }

    static boolean addScgunsCondition(JsonObject json) {
        JsonArray conditions;
        if (json.has("conditions")) {
            JsonElement existing = json.get("conditions");
            if (!existing.isJsonArray()) return false;
            conditions = existing.getAsJsonArray();
            for (JsonElement element : conditions) {
                if (element.isJsonObject()) {
                    JsonObject obj = element.getAsJsonObject();
                    if (obj.has("type") && "forge:mod_loaded".equals(obj.get("type").getAsString())
                            && obj.has("modid") && "scguns".equals(obj.get("modid").getAsString())) {
                        return true;
                    }
                }
            }
        } else {
            conditions = new JsonArray();
            json.add("conditions", conditions);
        }
        var condition = new JsonObject();
        condition.addProperty("type", "forge:mod_loaded");
        condition.addProperty("modid", "scguns");
        conditions.add(condition);
        return true;
    }
}
