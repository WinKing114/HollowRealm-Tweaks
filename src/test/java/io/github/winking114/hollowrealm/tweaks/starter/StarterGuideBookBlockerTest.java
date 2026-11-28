package io.github.winking114.hollowrealm.tweaks.starter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarterGuideBookBlockerTest {
    @Test
    void matchesOnlyLooseGuideBooksAndPreservesEccentricTomeAndGear() {
        assertTrue(StarterGuideBookBlocker.isLooseGuideBookId("patchouli:guide_book"));
        assertTrue(StarterGuideBookBlocker.isLooseGuideBookId("ars_nouveau:worn_notebook"));
        assertTrue(StarterGuideBookBlocker.isLooseGuideBookId("enigmaticlegacy:the_acknowledgment"));

        assertFalse(StarterGuideBookBlocker.isLooseGuideBookId("eccentrictome:tome"));
        assertFalse(StarterGuideBookBlocker.isLooseGuideBookId("irons_spellbooks:copper_spell_book"));
        assertFalse(StarterGuideBookBlocker.isLooseGuideBookId("ars_nouveau:novice_spell_book"));
        assertFalse(StarterGuideBookBlocker.isLooseGuideBookId("enigmaticlegacy:enigmatic_amulet"));
        assertFalse(StarterGuideBookBlocker.isLooseGuideBookId("minecraft:wooden_sword"));
        assertFalse(StarterGuideBookBlocker.isLooseGuideBookId(null));
    }
}
