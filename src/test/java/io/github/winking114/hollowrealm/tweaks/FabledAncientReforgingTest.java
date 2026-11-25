package io.github.winking114.hollowrealm.tweaks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FabledAncientReforgingTest {
    @Test
    void identifiersMatchApotheosisAncientIds() {
        assertEquals("apotheosis:ancient_material", FabledAncientReforgingHelper.ANCIENT_MATERIAL_ID.toString());
        assertEquals("apotheosis:ancient", FabledAncientReforgingHelper.ANCIENT_RARITY_ID.toString());
    }

    @Test
    void scalesCostsDeterministicallyAcrossAllThreeChoiceSlots() {
        assertEquals(3, FabledAncientReforgingHelper.scaleAncientCost(3, 0));
        assertEquals(6, FabledAncientReforgingHelper.scaleAncientCost(3, 1));
        assertEquals(9, FabledAncientReforgingHelper.scaleAncientCost(3, 2));

        assertEquals(10, FabledAncientReforgingHelper.scaleAncientCost(10, 0));
        assertEquals(20, FabledAncientReforgingHelper.scaleAncientCost(10, 1));
        assertEquals(30, FabledAncientReforgingHelper.scaleAncientCost(10, 2));

        assertEquals(125, FabledAncientReforgingHelper.scaleAncientCost(125, 0));
        assertEquals(250, FabledAncientReforgingHelper.scaleAncientCost(125, 1));
        assertEquals(375, FabledAncientReforgingHelper.scaleAncientCost(125, 2));

        assertEquals(5, FabledAncientReforgingHelper.scaleAncientCost(5, -2));
        assertEquals(0, FabledAncientReforgingHelper.scaleAncientCost(-4, 1));
    }

    @Test
    void nullMaterialReturnsFalseSafely() {
        assertFalse(FabledAncientReforgingHelper.isAncientMaterial(null));
    }
}
