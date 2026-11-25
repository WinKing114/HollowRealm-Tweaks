package io.github.winking114.hollowrealm.tweaks.mixin;

import io.github.winking114.hollowrealm.tweaks.SpellRarityDistribution;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.loot.RandomizeSpellFunction;
import io.redspace.ironsspellbooks.loot.SpellFilter;
import java.util.List;
import java.util.NavigableMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = RandomizeSpellFunction.class, remap = false)
public abstract class RandomizeSpellFunctionMixin {
    @Shadow(remap = false)
    @Final
    NumberProvider qualityRange;

    @Shadow(remap = false)
    @Final
    SpellFilter applicableSpells;

    @Shadow(remap = false)
    protected abstract NavigableMap<Integer, AbstractSpell> getWeightedSpellList(List<AbstractSpell> entries);

    @Inject(
            method = "getWeightFromRarity(Lio/redspace/ironsspellbooks/api/spells/SpellRarity;)I",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private void hrTweaks$guardExtendedRarityWeight(SpellRarity rarity, CallbackInfoReturnable<Integer> cir) {
        if (rarity != null && rarity.ordinal() >= 5) {
            cir.setReturnValue(SpellRarityDistribution.getSpellFilterWeight(rarity.getValue()));
        }
    }

    @Inject(
            method = "m_7372_(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/storage/loot/LootContext;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private void hrTweaks$applySevenTierDistribution(
            ItemStack stack, LootContext context, CallbackInfoReturnable<ItemStack> cir) {
        boolean isScroll = stack.getItem() instanceof Scroll;
        if (!isScroll && !Utils.canImbue(stack)) {
            cir.setReturnValue(stack);
            return;
        }

        ItemStack fallback = isScroll ? ItemStack.EMPTY : stack;
        List<AbstractSpell> applicable = this.applicableSpells.getApplicableSpells();
        if (applicable.isEmpty()) {
            cir.setReturnValue(fallback);
            return;
        }

        NavigableMap<Integer, AbstractSpell> weightedSpells = this.getWeightedSpellList(applicable);
        Integer totalWeight = weightedSpells.floorKey(Integer.MAX_VALUE);
        if (totalWeight == null || totalWeight <= 0) {
            cir.setReturnValue(fallback);
            return;
        }

        AbstractSpell spell = weightedSpells.higherEntry(context.getRandom().nextInt(totalWeight)).getValue();
        if (spell == null || spell.equals(SpellRegistry.none())) {
            cir.setReturnValue(fallback);
            return;
        }

        float quality = this.qualityRange.getFloat(context);
        int spellLevel = SpellRarityDistribution.selectSpellLevel(spell, quality, context.getRandom());
        if (isScroll) {
            ISpellContainer.createScrollContainer(spell, spellLevel, stack);
        } else {
            ISpellContainer.createImbuedContainer(spell, spellLevel, stack);
        }
        cir.setReturnValue(stack);
    }
}
