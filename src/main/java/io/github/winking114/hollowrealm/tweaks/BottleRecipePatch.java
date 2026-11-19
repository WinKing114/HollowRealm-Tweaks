package io.github.winking114.hollowrealm.tweaks;

import java.util.Optional;

/** Restore the audited author's recipe, not an invented replacement or balance change. */
final class BottleRecipePatch {
    static final String RESOURCE_PATH = "data/legendary_monsters/recipes/bottle_of_annihilation.json";
    private BottleRecipePatch() {}
    static Optional<byte[]> apply(byte[] authorRecipe, byte[] addonOverride) {
        if (!ResourceFingerprint.matches(authorRecipe, "6496d004dadf9ebc102e5b805064fdd84e08abb7f789e19f17e3c859c7384255")
                || !ResourceFingerprint.matches(addonOverride, "53b78882830c434a09353c1c37e9abaaa7ca570cd956010c0899470700da95a9"))
            return Optional.empty();
        return Optional.of(authorRecipe.clone());
    }
}
