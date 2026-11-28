package io.github.winking114.hollowrealm.tweaks;

import io.github.winking114.hollowrealm.tweaks.crit.UnifiedCritNormalizer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class UnifiedCritNormalizerTest {
    private static Attribute dummyAttr(String name, double def) {
        return new RangedAttribute(name, def, 0.0, 1024.0);
    }

    @Test
    void bridgesAdditionsMultipliersAndCataclysmScaleWhileZeroingSource() {
        AttributeInstance target = new AttributeInstance(dummyAttr("attributeslib.crit_chance", 0.05), i -> {});
        AttributeInstance source = new AttributeInstance(dummyAttr("l2.crit_rate", 0.0), i -> {});
        var spec = new UnifiedCritNormalizer.SourceSpec(
                ResourceLocation.fromNamespaceAndPath("l2damagetracker", "crit_rate"), 1.0);

        // Base-only: no bridge modifier, source still zeroed
        UnifiedCritNormalizer.syncSingle(source, target, spec);
        assertNull(target.getModifier(spec.bridgeUuid()));
        assertNotNull(source.getModifier(spec.zeroUuid()));
        assertEquals(0.0, source.getValue(), 1.0e-9);
        assertEquals(0.05, target.getValue(), 1.0e-9);

        // Add +0.15 ADDITION and +0.20 MULTIPLY_BASE -> raw = 0.15 * 1.20 = 0.18 -> target = 0.05 + 0.18 = 0.23
        UUID gearId = UUID.randomUUID();
        UUID buffId = UUID.randomUUID();
        source.addTransientModifier(new AttributeModifier(gearId, "gear", 0.15, AttributeModifier.Operation.ADDITION));
        source.addTransientModifier(new AttributeModifier(buffId, "buff", 0.20, AttributeModifier.Operation.MULTIPLY_BASE));

        UnifiedCritNormalizer.syncSingle(source, target, spec);
        assertEquals(0.0, source.getValue(), 1.0e-9);
        assertEquals(0.23, target.getValue(), 1.0e-9);
        assertFalse(target.getModifier(spec.bridgeUuid()) == null);

        // Idempotent on repeat call
        UnifiedCritNormalizer.syncSingle(source, target, spec);
        assertEquals(0.23, target.getValue(), 1.0e-9);

        // Removing gear removes bridge modifier cleanly
        source.removeModifier(gearId);
        source.removeModifier(buffId);
        UnifiedCritNormalizer.syncSingle(source, target, spec);
        assertNull(target.getModifier(spec.bridgeUuid()));
        assertEquals(0.05, target.getValue(), 1.0e-9);
    }

    @Test
    void scalesCataclysmAdditionalCriticalDamageByHundredth() {
        AttributeInstance target = new AttributeInstance(dummyAttr("attributeslib.crit_damage", 1.5), i -> {});
        AttributeInstance cataclysm = new AttributeInstance(dummyAttr("cataclysm.add_crit_dmg", 0.0), i -> {});
        var spec = new UnifiedCritNormalizer.SourceSpec(
                ResourceLocation.fromNamespaceAndPath("cataclysm", "additional_critical_damage"), 0.01);

        cataclysm.addTransientModifier(new AttributeModifier(UUID.randomUUID(), "weapon", 35.0, AttributeModifier.Operation.ADDITION));
        UnifiedCritNormalizer.syncSingle(cataclysm, target, spec);

        assertEquals(0.0, cataclysm.getValue(), 1.0e-9);
        assertEquals(1.85, target.getValue(), 1.0e-9);
    }
}
