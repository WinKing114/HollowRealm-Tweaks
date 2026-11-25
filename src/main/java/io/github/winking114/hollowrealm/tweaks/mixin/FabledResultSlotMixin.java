package io.github.winking114.hollowrealm.tweaks.mixin;

import dev.shadowsoffire.apotheosis.util.ApothMiscUtil;
import dev.shadowsoffire.placebo.util.EnchantmentUtils;
import io.github.winking114.hollowrealm.tweaks.FabledAncientReforgingHelper;
import net.kayn.fallen_gems_affixes.adventure.reforging.FabledReforgingMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = FabledReforgingMenu.FabledResultSlot.class, remap = false)
public abstract class FabledResultSlotMixin {
    @Shadow(remap = false)
    @Final
    FabledReforgingMenu this$0;

    @Unique
    private boolean hrTweaks$takingAncient;

    @Unique
    private int hrTweaks$ancientLevelCost;

    @Inject(
            method = "m_142406_(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"),
            require = 1,
            remap = false)
    private void hrTweaks$captureAncientMode(Player player, ItemStack takenStack, CallbackInfo ci) {
        if (player == null || player.level().isClientSide) {
            this.hrTweaks$takingAncient = false;
            this.hrTweaks$ancientLevelCost = 0;
            return;
        }
        boolean ancient = FabledAncientReforgingHelper.isAncientMaterial(this.this$0.getSlot(1).getItem());
        this.hrTweaks$takingAncient = ancient;
        if (ancient) {
            int slotIdx = ((SlotItemHandler) (Object) this).getSlotIndex();
            this.hrTweaks$ancientLevelCost = this.this$0.getLevelCost(slotIdx);
        } else {
            this.hrTweaks$ancientLevelCost = 0;
        }
    }

    @Redirect(
            method = "m_142406_(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;m_6749_(I)V"),
            require = 1,
            remap = false)
    private void hrTweaks$chargeAncientXpOrVanillaLevels(Player targetPlayer, int negativeLevelCost) {
        if (this.hrTweaks$takingAncient) {
            int slotIdx = ((SlotItemHandler) (Object) this).getSlotIndex();
            int xpPoints = ApothMiscUtil.getExpCostForSlot(this.hrTweaks$ancientLevelCost, slotIdx);
            EnchantmentUtils.chargeExperience(targetPlayer, xpPoints);
            return;
        }
        targetPlayer.giveExperienceLevels(negativeLevelCost);
    }

    @Inject(
            method = "m_142406_(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("RETURN"),
            require = 1,
            remap = false)
    private void hrTweaks$syncAfterAncientTake(Player player, ItemStack takenStack, CallbackInfo ci) {
        if (!this.hrTweaks$takingAncient) {
            return;
        }
        this.hrTweaks$takingAncient = false;
        this.hrTweaks$ancientLevelCost = 0;
        ((AbstractContainerMenu) this.this$0).slotsChanged(null);
    }
}
