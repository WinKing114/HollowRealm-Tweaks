package io.github.winking114.hollowrealm.tweaks.mixin;

import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingRecipe;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.placebo.cap.InternalItemHandler;
import dev.shadowsoffire.placebo.menu.BlockEntityMenu;
import io.github.winking114.hollowrealm.tweaks.FabledAncientReforgingHelper;
import java.util.List;
import net.kayn.fallen_gems_affixes.adventure.reforging.FabledReforgingMenu;
import net.kayn.fallen_gems_affixes.adventure.reforging.FabledReforgingTableTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = FabledReforgingMenu.class, remap = false)
public abstract class FabledReforgingMenuMixin extends BlockEntityMenu<FabledReforgingTableTile> {
    protected FabledReforgingMenuMixin(MenuType<?> type, int id, Inventory pInv, BlockPos pos) {
        super(type, id, pInv, pos);
    }

    @Shadow(remap = false)
    @Final
    protected Player player;

    @Shadow(remap = false)
    protected InternalItemHandler choicesInv;

    @Shadow(remap = false)
    @Final
    protected int[] costs;

    @Shadow(remap = false)
    protected int seed;

    @Shadow(remap = false)
    @Final
    private List<?> currentChoices;

    @Unique
    private boolean hrTweaks$isAncientMode() {
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        return FabledAncientReforgingHelper.isAncientMaterial(self.getSlot(1).getItem());
    }

    @Inject(method = "getSigilCost(I)I", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void hrTweaks$scaleAncientSigilCost(int slot, CallbackInfoReturnable<Integer> cir) {
        if (hrTweaks$isAncientMode()) {
            cir.setReturnValue(FabledAncientReforgingHelper.scaleAncientCost(this.costs[0], slot));
        }
    }

    @Inject(method = "getMatCost(I)I", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void hrTweaks$scaleAncientMatCost(int slot, CallbackInfoReturnable<Integer> cir) {
        if (hrTweaks$isAncientMode()) {
            cir.setReturnValue(FabledAncientReforgingHelper.scaleAncientCost(this.costs[1], slot));
        }
    }

    @Inject(method = "getLevelCost(I)I", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void hrTweaks$scaleAncientLevelCost(int slot, CallbackInfoReturnable<Integer> cir) {
        if (hrTweaks$isAncientMode()) {
            cir.setReturnValue(FabledAncientReforgingHelper.scaleAncientCost(this.costs[2], slot));
        }
    }

    @Inject(
            method = "m_6199_(Lnet/minecraft/world/Container;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private void hrTweaks$handleAncientSlotsChanged(Container container, CallbackInfo ci) {
        if (!hrTweaks$isAncientMode()) {
            return;
        }

        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        ReforgingRecipe ancientRecipe = FabledAncientReforgingHelper.findAncientRecipe(this.player.level());
        LootRarity ancientRarity = FabledAncientReforgingHelper.resolveAncientRarity();

        if (ancientRecipe != null) {
            this.costs[0] = ancientRecipe.sigilCost();
            this.costs[1] = ancientRecipe.matCost();
            this.costs[2] = ancientRecipe.levelCost();
        } else {
            this.costs[0] = 0;
            this.costs[1] = 0;
            this.costs[2] = 0;
        }

        this.currentChoices.clear();
        ItemStack input = self.getSlot(0).getItem();
        for (int slotIdx = 0; slotIdx < 3; slotIdx++) {
            ItemStack choice = (ancientRecipe != null && ancientRarity != null)
                    ? FabledAncientReforgingHelper.buildAncientChoice(input, this.seed, slotIdx, ancientRarity)
                    : ItemStack.EMPTY;
            this.choicesInv.setStackInSlot(slotIdx, choice);
        }

        super.slotsChanged(container);
        if (this.tile != null) {
            this.tile.setChanged();
        }
        self.broadcastChanges();
        ci.cancel();
    }
}
