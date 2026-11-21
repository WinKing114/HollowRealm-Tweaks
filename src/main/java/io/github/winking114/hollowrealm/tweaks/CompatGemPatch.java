package io.github.winking114.hollowrealm.tweaks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Audited compat patches for fallen_gems_affixes 2.1.5:
 * 1. gems/compat/spell_lightning.json: add forge:mod_loaded(additional_attributes) when additional_attributes is absent.
 * 2. gems/compat/hemolith_shard.json: strip the additional_attributes devour bonus when additional_attributes is absent,
 *    preserving the 2 irons_spellbooks bonuses so hemolith_shard and its extra gem bonuses register cleanly.
 * 3. extra_gem_bonuses/compat/hemolith_shard_leggings.json: gate with forge:false because traveloptics:assassin
 *    was removed in traveloptics 7.2.0.
 */
final class CompatGemPatch {
    static final String SPELL_LIGHTNING_PATH = "gems/compat/spell_lightning.json";
    static final String SPELL_LIGHTNING_SHA256 = "2ef303f9a95d8c5d7bd4165fe25f7de115f5c2cfeab83df7ddc9e7181d092c10";

    static final String HEMOLITH_SHARD_PATH = "gems/compat/hemolith_shard.json";
    static final String HEMOLITH_SHARD_SHA256 = "7e310acd45eaa6154a89ded4f69e1b4630ca4c14d928ec8309cc41d0cb1dbdf3";

    static final String HEMOLITH_SHARD_LEGGINGS_PATH = "extra_gem_bonuses/compat/hemolith_shard_leggings.json";
    static final String HEMOLITH_SHARD_LEGGINGS_SHA256 = "d17e0220769a120d7fddd4ce4ae14b14651adc9d7000b0db1ba3743b7c53dbb7";

    static final String ADDITIONAL_ATTRIBUTES_MOD_ID = "additional_attributes";
    static final String DEVOUR_ATTRIBUTE = "additional_attributes:innate_spell/irons_spellbooks/devour";

    private CompatGemPatch() {}

    static Optional<byte[]> applySpellLightning(byte[] source, boolean additionalAttributesPresent) {
        if (additionalAttributesPresent || source == null
                || !ResourceFingerprint.matches(source, SPELL_LIGHTNING_SHA256)) {
            return Optional.empty();
        }
        try {
            var parsed = JsonParser.parseString(new String(source, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) return Optional.empty();
            var json = parsed.getAsJsonObject();
            if (!addModLoadedCondition(json, ADDITIONAL_ATTRIBUTES_MOD_ID)) return Optional.empty();
            return Optional.of(json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (JsonParseException | IllegalStateException ignored) {
            return Optional.empty();
        }
    }

    static Optional<byte[]> applyHemolithShard(byte[] source, boolean additionalAttributesPresent) {
        if (additionalAttributesPresent || source == null
                || !ResourceFingerprint.matches(source, HEMOLITH_SHARD_SHA256)) {
            return Optional.empty();
        }
        try {
            var parsed = JsonParser.parseString(new String(source, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) return Optional.empty();
            var json = parsed.getAsJsonObject();
            if (!stripAttributeBonus(json, DEVOUR_ATTRIBUTE)) return Optional.empty();
            return Optional.of(json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (JsonParseException | IllegalStateException ignored) {
            return Optional.empty();
        }
    }

    static Optional<byte[]> applyHemolithShardLeggings(byte[] source) {
        if (source == null || !ResourceFingerprint.matches(source, HEMOLITH_SHARD_LEGGINGS_SHA256)) {
            return Optional.empty();
        }
        try {
            var parsed = JsonParser.parseString(new String(source, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) return Optional.empty();
            var json = parsed.getAsJsonObject();
            if (!replaceWithFalseCondition(json)) return Optional.empty();
            return Optional.of(json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (JsonParseException | IllegalStateException ignored) {
            return Optional.empty();
        }
    }

    static boolean addModLoadedCondition(JsonObject json, String modId) {
        JsonArray conditions;
        if (json.has("conditions")) {
            JsonElement existing = json.get("conditions");
            if (!existing.isJsonArray()) return false;
            conditions = existing.getAsJsonArray();
            for (JsonElement element : conditions) {
                if (element.isJsonObject()) {
                    JsonObject obj = element.getAsJsonObject();
                    if (obj.has("type") && "forge:mod_loaded".equals(obj.get("type").getAsString())
                            && obj.has("modid") && modId.equals(obj.get("modid").getAsString())) {
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
        condition.addProperty("modid", modId);
        conditions.add(condition);
        return true;
    }

    static boolean stripAttributeBonus(JsonObject json, String attributeId) {
        if (!json.has("bonuses") || !json.get("bonuses").isJsonArray()) {
            return false;
        }
        JsonArray original = json.getAsJsonArray("bonuses");
        JsonArray filtered = new JsonArray();
        boolean removed = false;
        for (JsonElement element : original) {
            if (!element.isJsonObject()) {
                return false;
            }
            JsonObject bonus = element.getAsJsonObject();
            if (bonus.has("attribute") && bonus.get("attribute").isJsonPrimitive()
                    && attributeId.equals(bonus.get("attribute").getAsString())) {
                removed = true;
            } else {
                filtered.add(bonus);
            }
        }
        if (!removed || filtered.isEmpty()) {
            return false;
        }
        json.add("bonuses", filtered);
        return true;
    }

    static boolean replaceWithFalseCondition(JsonObject json) {
        if (json.has("conditions") && !json.get("conditions").isJsonArray()) {
            return false;
        }
        var falseCondition = new JsonObject();
        falseCondition.addProperty("type", "forge:false");
        var conditions = new JsonArray();
        conditions.add(falseCondition);
        json.add("conditions", conditions);
        return true;
    }
}
