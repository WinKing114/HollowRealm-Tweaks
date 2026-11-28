package io.github.winking114.hollowrealm.tweaks.crit;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Bridges secondary mod crit-rate and crit-damage attributes into ApothicAttributes'
 * canonical {@code attributeslib:crit_chance} and {@code attributeslib:crit_damage},
 * while zeroing the source attributes via a transient {@code -1.0 MULTIPLY_TOTAL} modifier
 * so no mod rolls a duplicate crit.
 */
public final class UnifiedCritNormalizer {
    public static final ResourceLocation CANONICAL_CRIT_CHANCE =
            ResourceLocation.fromNamespaceAndPath("attributeslib", "crit_chance");
    public static final ResourceLocation CANONICAL_CRIT_DAMAGE =
            ResourceLocation.fromNamespaceAndPath("attributeslib", "crit_damage");

    public record SourceSpec(ResourceLocation id, double scale) {
        public UUID zeroUuid() {
            return UUID.nameUUIDFromBytes(("hollowrealm_tweaks:crit_zero:" + id).getBytes(StandardCharsets.UTF_8));
        }

        public UUID bridgeUuid() {
            return UUID.nameUUIDFromBytes(("hollowrealm_tweaks:crit_bridge:" + id).getBytes(StandardCharsets.UTF_8));
        }
    }

    public static final List<SourceSpec> RATE_SOURCES = List.of(
            new SourceSpec(ResourceLocation.fromNamespaceAndPath("eeeabsmobs", "crit_chance"), 1.0),
            new SourceSpec(ResourceLocation.fromNamespaceAndPath("gtbcs_spell_lib", "spell_crit_chance"), 1.0),
            new SourceSpec(ResourceLocation.fromNamespaceAndPath("l2damagetracker", "crit_rate"), 1.0)
    );

    public static final List<SourceSpec> DAMAGE_SOURCES = List.of(
            new SourceSpec(ResourceLocation.fromNamespaceAndPath("cataclysm", "additional_critical_damage"), 0.01),
            new SourceSpec(ResourceLocation.fromNamespaceAndPath("gtbcs_spell_lib", "spell_crit_damage"), 1.0),
            new SourceSpec(ResourceLocation.fromNamespaceAndPath("irons_lib", "crit_damage"), 1.0),
            new SourceSpec(ResourceLocation.fromNamespaceAndPath("l2damagetracker", "crit_damage"), 1.0),
            new SourceSpec(ResourceLocation.fromNamespaceAndPath("obscure_api", "critical_damage"), 1.0)
    );

    private static final double EPSILON = 1.0e-9;
    private static final ThreadLocal<Boolean> REENTRANCY = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private UnifiedCritNormalizer() {
    }

    public static void normalize(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (Boolean.TRUE.equals(REENTRANCY.get())) {
            return;
        }
        Attribute targetChance = ForgeRegistries.ATTRIBUTES.getValue(CANONICAL_CRIT_CHANCE);
        Attribute targetDamage = ForgeRegistries.ATTRIBUTES.getValue(CANONICAL_CRIT_DAMAGE);
        if (targetChance == null && targetDamage == null) {
            return;
        }
        REENTRANCY.set(Boolean.TRUE);
        try {
            if (targetChance != null) {
                AttributeInstance chanceInst = entity.getAttribute(targetChance);
                if (chanceInst != null) {
                    syncGroup(entity, chanceInst, RATE_SOURCES);
                }
            }
            if (targetDamage != null) {
                AttributeInstance damageInst = entity.getAttribute(targetDamage);
                if (damageInst != null) {
                    syncGroup(entity, damageInst, DAMAGE_SOURCES);
                }
            }
        } finally {
            REENTRANCY.set(Boolean.FALSE);
        }
    }

    private static void syncGroup(LivingEntity entity, AttributeInstance targetInst, List<SourceSpec> sources) {
        for (SourceSpec spec : sources) {
            Attribute sourceAttr = ForgeRegistries.ATTRIBUTES.getValue(spec.id());
            if (sourceAttr == null) {
                continue;
            }
            AttributeInstance sourceInst = entity.getAttribute(sourceAttr);
            if (sourceInst == null) {
                continue;
            }
            syncSingle(sourceInst, targetInst, spec);
        }
    }

    /**
     * Synchronizes a single source {@link AttributeInstance} into {@code targetInst}.
     * Package/public for direct unit testing without full Forge world setup.
     */
    public static void syncSingle(AttributeInstance sourceInst, AttributeInstance targetInst, SourceSpec spec) {
        UUID zeroUuid = spec.zeroUuid();
        UUID bridgeUuid = spec.bridgeUuid();
        double raw = computeValueExcluding(sourceInst, zeroUuid);
        double delta = (raw - sourceInst.getBaseValue()) * spec.scale();

        AttributeModifier existingBridge = targetInst.getModifier(bridgeUuid);
        if (Math.abs(delta) < EPSILON) {
            if (existingBridge != null) {
                targetInst.removeModifier(bridgeUuid);
            }
        } else if (existingBridge == null || Math.abs(existingBridge.getAmount() - delta) >= EPSILON) {
            if (existingBridge != null) {
                targetInst.removeModifier(bridgeUuid);
            }
            targetInst.addTransientModifier(new AttributeModifier(
                    bridgeUuid,
                    "hollowrealm_tweaks:crit_bridge:" + spec.id(),
                    delta,
                    AttributeModifier.Operation.ADDITION
            ));
        }

        AttributeModifier existingZero = sourceInst.getModifier(zeroUuid);
        if (existingZero == null) {
            sourceInst.addTransientModifier(new AttributeModifier(
                    zeroUuid,
                    "hollowrealm_tweaks:crit_zero:" + spec.id(),
                    -1.0,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }

    public static double computeValueExcluding(AttributeInstance inst, UUID excludedUuid) {
        double base = inst.getBaseValue();
        double add = base;
        for (AttributeModifier mod : inst.getModifiers(AttributeModifier.Operation.ADDITION)) {
            if (!excludedUuid.equals(mod.getId())) {
                add += mod.getAmount();
            }
        }
        double multBase = 0.0;
        for (AttributeModifier mod : inst.getModifiers(AttributeModifier.Operation.MULTIPLY_BASE)) {
            if (!excludedUuid.equals(mod.getId())) {
                multBase += mod.getAmount();
            }
        }
        double result = add * (1.0 + multBase);
        for (AttributeModifier mod : inst.getModifiers(AttributeModifier.Operation.MULTIPLY_TOTAL)) {
            if (!excludedUuid.equals(mod.getId())) {
                result *= (1.0 + mod.getAmount());
            }
        }
        return result;
    }
}
