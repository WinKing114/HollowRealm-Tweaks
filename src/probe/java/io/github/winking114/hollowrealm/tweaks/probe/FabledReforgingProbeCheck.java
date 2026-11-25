package io.github.winking114.hollowrealm.tweaks.probe;

import com.google.gson.JsonObject;
import com.ianm1647.ancientreforging.block.AncientReforgingTableTile;
import com.ianm1647.ancientreforging.screen.AncientReforgingMenu;
import com.mojang.authlib.GameProfile;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.util.ApothMiscUtil;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import dev.shadowsoffire.placebo.util.EnchantmentUtils;
import java.util.UUID;
import net.kayn.fallen_gems_affixes.adventure.reforging.FabledReforgingMenu;
import net.kayn.fallen_gems_affixes.adventure.reforging.FabledReforgingTableTile;
import net.kayn.fallen_gems_affixes.adventure.set.SetAffixHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/** Isolated Forge runtime probe for Fabled Reforging Table + Ancient Material support. */
final class FabledReforgingProbeCheck {
    private FabledReforgingProbeCheck() {
    }

    static void run(MinecraftServer server, String mode, String phase, JsonObject result) {
        boolean active = "active".equals(mode);
        if (!ModList.get().isLoaded("ancientreforging") || !ModList.get().isLoaded("fallen_gems_affixes")) {
            throw new IllegalStateException("Expected ancientreforging and fallen_gems_affixes in runtime probe");
        }

        ServerLevel level = server.overworld();
        BlockPos fabledPos = new BlockPos(0, 64, 0);
        BlockPos ancientPos = new BlockPos(2, 64, 0);

        Block fabledBlock = requireNonNull(ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("fallen_gems_affixes", "fabled_reforging_table")),
                "Missing fallen_gems_affixes:fabled_reforging_table");
        Block ancientBlock = requireNonNull(ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("ancientreforging", "ancient_reforging_table")),
                "Missing ancientreforging:ancient_reforging_table");

        level.setBlock(fabledPos, fabledBlock.defaultBlockState(), 3);
        level.setBlock(ancientPos, ancientBlock.defaultBlockState(), 3);

        BlockEntity fabledBe = level.getBlockEntity(fabledPos);
        if (!(fabledBe instanceof FabledReforgingTableTile fabledTile)) {
            throw new IllegalStateException("Expected FabledReforgingTableTile at " + fabledPos);
        }
        BlockEntity ancientBe = level.getBlockEntity(ancientPos);
        if (!(ancientBe instanceof AncientReforgingTableTile ancientTile)) {
            throw new IllegalStateException("Expected AncientReforgingTableTile at " + ancientPos);
        }

        Item ancientMatItem = requireNonNull(ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("apotheosis", "ancient_material")),
                "Missing apotheosis:ancient_material");
        Item fabledMatItem = requireNonNull(ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("fallen_gems_affixes", "fabled_material")),
                "Missing fallen_gems_affixes:fabled_material");
        Item sigilItem = requireNonNull(ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("apotheosis", "sigil_of_rebirth")),
                "Missing apotheosis:sigil_of_rebirth");

        ItemStack ancientMat1 = new ItemStack(ancientMatItem, 1);
        ItemStack fabledMat1 = new ItemStack(fabledMatItem, 1);
        ItemStack invalidDirt = new ItemStack(Items.DIRT, 1);

        boolean fabledAcceptsAncient = fabledTile.isValidRarityMat(ancientMat1);
        boolean fabledAcceptsFabled = fabledTile.isValidRarityMat(fabledMat1);
        boolean fabledAcceptsDirt = fabledTile.isValidRarityMat(invalidDirt);
        boolean ancientAcceptsAncient = ancientTile.isValidRarityMat(ancientMat1);

        require(fabledAcceptsAncient == active,
                "Unexpected fabledTile.isValidRarityMat(ancient_material)=" + fabledAcceptsAncient + " in mode=" + mode);
        require(fabledAcceptsFabled, "Fabled tile stopped accepting fabled_material");
        require(!fabledAcceptsDirt, "Fabled tile accepted dirt as rarity material");
        require(ancientAcceptsAncient, "Ancient tile stopped accepting ancient_material");

        JsonObject probeInfo = new JsonObject();
        probeInfo.addProperty("fabled_accepts_ancient", fabledAcceptsAncient);
        probeInfo.addProperty("fabled_accepts_fabled", fabledAcceptsFabled);
        probeInfo.addProperty("ancient_accepts_ancient", ancientAcceptsAncient);

        if (active) {
            FakePlayer fabledAncientPlayer = FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes(("hrt-ancient-" + phase).getBytes()), "hrt_probe_a"));
            fabledAncientPlayer.setPos(0.5, 65.0, 0.5);

            // 1. Test Ancient reforging inside Fabled Reforging Table
            FabledReforgingMenu menu = new FabledReforgingMenu(1, fabledAncientPlayer.getInventory(), fabledPos);
            menu.getSlot(0).set(new ItemStack(Items.NETHERITE_SWORD));
            menu.getSlot(1).set(new ItemStack(ancientMatItem, 16));
            menu.getSlot(2).set(new ItemStack(sigilItem, 32));
            menu.slotsChanged(null);

            require(menu.getSigilCost(0) == 10 && menu.getMatCost(0) == 3 && menu.getLevelCost(0) == 125,
                    "Unexpected slot 0 ancient costs: sigil=" + menu.getSigilCost(0) + " mat=" + menu.getMatCost(0) + " lvl=" + menu.getLevelCost(0));
            require(menu.getSigilCost(1) == 20 && menu.getMatCost(1) == 6 && menu.getLevelCost(1) == 250,
                    "Unexpected slot 1 ancient costs");
            require(menu.getSigilCost(2) == 30 && menu.getMatCost(2) == 9 && menu.getLevelCost(2) == 375,
                    "Unexpected slot 2 ancient costs");

            for (int idx = 0; idx < 3; idx++) {
                ItemStack choice = menu.getSlot(3 + idx).getItem();
                require(!choice.isEmpty(), "Empty ancient choice at choice slot " + idx);
                DynamicHolder<LootRarity> choiceHolder = AffixHelper.getRarity(choice);
                require(choiceHolder.isBound() && "apotheosis:ancient".equals(choiceHolder.getId().toString()),
                        "Expected apotheosis:ancient choice at slot " + idx + ", got " + choiceHolder.getId());
            }

            setPlayerLevel(fabledAncientPlayer, 130);
            int expectedXpCost = ApothMiscUtil.getExpCostForSlot(125, 0);
            require(menu.getSlot(3).mayPickup(fabledAncientPlayer), "FakePlayer with 130 levels and full mats/sigils could not pick up ancient choice 0");

            ItemStack craftedAncient = menu.getSlot(3).getItem().copy();
            menu.getSlot(3).onTake(fabledAncientPlayer, craftedAncient);

            DynamicHolder<LootRarity> takenHolder = AffixHelper.getRarity(craftedAncient);
            require(takenHolder.isBound() && "apotheosis:ancient".equals(takenHolder.getId().toString()),
                    "Taken stack lost ancient rarity: " + takenHolder.getId());
            require(!AffixHelper.getAffixes(craftedAncient).isEmpty(), "Taken ancient stack has no affixes");
            require(menu.getSlot(0).getItem().isEmpty(), "Input gear slot was not cleared");
            require(menu.getSlot(1).getItem().getCount() == 13, "Expected 13 ancient_material remaining (16-3), got " + menu.getSlot(1).getItem().getCount());
            require(menu.getSlot(2).getItem().getCount() == 22, "Expected 22 sigil_of_rebirth remaining (32-10), got " + menu.getSlot(2).getItem().getCount());
            require(fabledAncientPlayer.experienceLevel == 129,
                    "Expected player experienceLevel == 129 after slot 0 ancient charge, got " + fabledAncientPlayer.experienceLevel);

            // Drain remaining mats cleanly so slot 1 becomes empty when re-tested
            menu.getSlot(1).set(ItemStack.EMPTY);
            menu.getSlot(2).set(ItemStack.EMPTY);

            // 2. Regression: Test original Fabled + SetAffix reforging inside the same table
            FakePlayer fabledRegPlayer = FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes(("hrt-fabled-" + phase).getBytes()), "hrt_probe_f"));
            fabledRegPlayer.setPos(0.5, 65.0, 0.5);
            FabledReforgingMenu fabledMenu = new FabledReforgingMenu(2, fabledRegPlayer.getInventory(), fabledPos);
            fabledMenu.getSlot(0).set(new ItemStack(Items.NETHERITE_SWORD));
            fabledMenu.getSlot(1).set(new ItemStack(fabledMatItem, 16));
            fabledMenu.getSlot(2).set(new ItemStack(sigilItem, 32));
            fabledMenu.slotsChanged(null);

            int fabledMatCost = fabledMenu.getMatCost(0);
            int fabledSigilCost = fabledMenu.getSigilCost(0);
            int fabledLvlCost = fabledMenu.getLevelCost(0);
            ItemStack fabledChoice = fabledMenu.getSlot(3).getItem();
            require(!fabledChoice.isEmpty(), "Fabled choice 0 is empty");
            DynamicHolder<LootRarity> fabledHolder = AffixHelper.getRarity(fabledChoice);
            require(fabledHolder.isBound() && "fallen_gems_affixes:fabled".equals(fabledHolder.getId().toString()),
                    "Expected fallen_gems_affixes:fabled choice, got " + fabledHolder.getId());
            require(SetAffixHelper.hasSetAffix(fabledChoice), "Fabled regression choice missing SetAffix");

            setPlayerLevel(fabledRegPlayer, 100);
            require(fabledMenu.getSlot(3).mayPickup(fabledRegPlayer), "Player could not pick up fabled regression choice");
            ItemStack craftedFabled = fabledChoice.copy();
            fabledMenu.getSlot(3).onTake(fabledRegPlayer, craftedFabled);
            require(fabledMenu.getSlot(1).getItem().getCount() == 16 - fabledMatCost, "Fabled mat count mismatch after take");
            require(fabledMenu.getSlot(2).getItem().getCount() == 32 - fabledSigilCost, "Fabled sigil count mismatch after take");
            require(fabledRegPlayer.experienceLevel == 100 - fabledLvlCost,
                    "Fabled regression XP mismatch: expected " + (100 - fabledLvlCost) + ", got " + fabledRegPlayer.experienceLevel);

            // Clear Fabled slots cleanly
            fabledMenu.getSlot(1).set(ItemStack.EMPTY);
            fabledMenu.getSlot(2).set(ItemStack.EMPTY);

            // 3. Native Ancient Reforging Table parity check (same starting L130 -> same final XP/level/mat/sigil state)
            FakePlayer nativeAncientPlayer = FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes(("hrt-native-" + phase).getBytes()), "hrt_probe_n"));
            nativeAncientPlayer.setPos(2.5, 65.0, 0.5);
            AncientReforgingMenu ancientMenu = new AncientReforgingMenu(3, nativeAncientPlayer.getInventory(), ancientPos);
            ancientMenu.getSlot(0).set(new ItemStack(Items.NETHERITE_SWORD));
            ancientMenu.getSlot(1).set(new ItemStack(ancientMatItem, 16));
            ancientMenu.getSlot(2).set(new ItemStack(sigilItem, 32));
            ancientMenu.slotsChanged(null);
            ItemStack nativeAncientChoice = ancientMenu.getSlot(3).getItem();
            require(!nativeAncientChoice.isEmpty(), "Native AncientReforgingTable choice empty");
            DynamicHolder<LootRarity> nativeHolder = AffixHelper.getRarity(nativeAncientChoice);
            require(nativeHolder.isBound() && "apotheosis:ancient".equals(nativeHolder.getId().toString()),
                    "Native AncientReforgingTable unexpected rarity: " + nativeHolder.getId());

            setPlayerLevel(nativeAncientPlayer, 130);
            require(ancientMenu.getSlot(3).mayPickup(nativeAncientPlayer), "Native AncientReforgingMenu rejected pickup at L130");
            ancientMenu.getSlot(3).onTake(nativeAncientPlayer, nativeAncientChoice.copy());
            require(fabledAncientPlayer.experienceLevel == nativeAncientPlayer.experienceLevel
                            && Math.abs(fabledAncientPlayer.experienceProgress - nativeAncientPlayer.experienceProgress) < 1.0e-5f
                            && fabledAncientPlayer.totalExperience == nativeAncientPlayer.totalExperience,
                    "XP parity mismatch vs AncientReforgingMenu: fabled=(" + fabledAncientPlayer.experienceLevel + "," + fabledAncientPlayer.experienceProgress
                            + "," + fabledAncientPlayer.totalExperience + ") native=(" + nativeAncientPlayer.experienceLevel + "," + nativeAncientPlayer.experienceProgress
                            + "," + nativeAncientPlayer.totalExperience + ")");

            ancientMenu.getSlot(1).set(ItemStack.EMPTY);
            ancientMenu.getSlot(2).set(ItemStack.EMPTY);

            probeInfo.addProperty("ancient_xp_points_param", expectedXpCost);
            probeInfo.addProperty("ancient_final_level_from_130", fabledAncientPlayer.experienceLevel);
            probeInfo.addProperty("ancient_final_progress", fabledAncientPlayer.experienceProgress);
            probeInfo.addProperty("ancient_mat_consumed", 3);
            probeInfo.addProperty("ancient_sigil_consumed", 10);
            probeInfo.addProperty("fabled_regression_mat_cost", fabledMatCost);
            probeInfo.addProperty("native_ancient_table_ok", true);

            System.out.println("[HRT-PROBE] " + phase + " fabled_ancient_reforging=PASS mat_consumed=3 sigil_consumed=10 xp_points="
                    + expectedXpCost + " final_level=" + fabledAncientPlayer.experienceLevel + " parity=PASS fabled_regression=PASS");
        } else {
            System.out.println("[HRT-PROBE] " + phase + " baseline fabled_accepts_ancient=" + fabledAcceptsAncient
                    + " fabled_accepts_fabled=" + fabledAcceptsFabled + " ancient_accepts_ancient=" + ancientAcceptsAncient);
        }
        result.add("fabled_ancient_reforging", probeInfo);
    }

    private static void setPlayerLevel(FakePlayer player, int level) {
        player.experienceLevel = level;
        player.experienceProgress = 0.0f;
        player.totalExperience = EnchantmentUtils.getTotalExperienceForLevel(level);
    }

    private static <T> T requireNonNull(T obj, String message) {
        if (obj == null) {
            throw new IllegalStateException(message);
        }
        return obj;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
