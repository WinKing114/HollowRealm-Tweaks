package io.github.winking114.hollowrealm.tweaks;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Only the audited 2.3.5 resource is eligible; upstream changes require a new audit. */
final class ClamConditionPatch {
    static final String RESOURCE_PATH = "data/crittersandcompanions/loot_modifiers/inject_clams.json";
    private static final String AUDITED_SHA256 = "4273eaa880484a7f3b3ee8567d45b780929822f98b64a785a6ccbce054449355";

    private ClamConditionPatch() {}

    static Optional<byte[]> apply(byte[] original) {
        if (!ResourceFingerprint.matches(original, AUDITED_SHA256)) return Optional.empty();
        return Optional.of(new String(original, StandardCharsets.UTF_8)
                .replace("neoforge:loot_table_id", "forge:loot_table_id")
                .getBytes(StandardCharsets.UTF_8));
    }
}
