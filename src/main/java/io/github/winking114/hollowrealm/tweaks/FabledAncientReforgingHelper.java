package io.github.winking114.hollowrealm.tweaks;

import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingRecipe;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Shared helpers enabling Fallen Gems & Affixes' Fabled Reforging Table to perform
 * Apotheosis Ancient reforging when supplied with apotheosis:ancient_material.
 */
public final class FabledAncientReforgingHelper {
    public static final ResourceLocation ANCIENT_MATERIAL_ID = ResourceLocation.fromNamespaceAndPath("apotheosis", "ancient_material");
    public static final ResourceLocation ANCIENT_RARITY_ID = ResourceLocation.fromNamespaceAndPath("apotheosis", "ancient");

    private FabledAncientReforgingHelper() {
    }

    /** Returns true only when stack is non-empty and its registered item ID is apotheosis:ancient_material. */
    public static boolean isAncientMaterial(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return ANCIENT_MATERIAL_ID.equals(key);
    }

    /** Matches AncientReforgingMenu cost scaling across the 3 choice slots: baseCost * (slot + 1). */
    public static int scaleAncientCost(int baseCost, int slotIndex) {
        int clampedSlot = Math.max(0, slotIndex);
        return Math.max(0, baseCost) * (clampedSlot + 1);
    }

    public static LootRarity resolveAncientRarity() {
        return RarityRegistry.INSTANCE.getValue(ANCIENT_RARITY_ID);
    }

    /** Looks up the Apotheosis reforging recipe targeting apotheosis:ancient in the given level. */
    public static ReforgingRecipe findAncientRecipe(Level level) {
        LootRarity ancient = resolveAncientRarity();
        if (level == null || ancient == null) {
            return null;
        }
        for (ReforgingRecipe recipe : level.getRecipeManager().getAllRecipesFor(Apoth.RecipeTypes.REFORGING)) {
            if (recipe.rarity().isBound() && recipe.rarity().get() == ancient) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * Generates one deterministic ancient-rarity candidate stack using the same seeding scheme as
     * AncientReforgingMenu, preserving input enchantments/durability via LootController.createLootItem.
     */
    public static ItemStack buildAncientChoice(ItemStack input, int seed, int slotIndex, LootRarity ancientRarity) {
        if (input == null || input.isEmpty() || ancientRarity == null) {
            return ItemStack.EMPTY;
        }
        LootCategory category = LootCategory.forItem(input);
        if (category.isNone()) {
            return ItemStack.EMPTY;
        }
        ResourceLocation itemKey = ForgeRegistries.ITEMS.getKey(input.getItem());
        int itemHash = itemKey != null ? itemKey.hashCode() : 0;
        RandomSource random = new XoroshiroRandomSource(0L);
        random.setSeed((long) (seed ^ (itemHash + slotIndex)));
        return LootController.createLootItem(input.copy(), ancientRarity, random);
    }
}
