package io.github.winking114.hollowrealm.tweaks.mixin;

import io.github.winking114.hollowrealm.tweaks.FabledAncientReforgingHelper;
import net.kayn.fallen_gems_affixes.adventure.reforging.FabledReforgingTableTile;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = FabledReforgingTableTile.class, remap = false)
public abstract class FabledReforgingTableTileMixin {
    @Inject(
            method = "isValidRarityMat(Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("RETURN"),
            cancellable = true,
            require = 1,
            remap = false)
    private void hrTweaks$acceptAncientMaterial(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && FabledAncientReforgingHelper.isAncientMaterial(stack)) {
            cir.setReturnValue(true);
        }
    }
}
