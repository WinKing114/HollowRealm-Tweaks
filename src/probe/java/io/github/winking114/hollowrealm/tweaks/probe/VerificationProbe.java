package io.github.winking114.hollowrealm.tweaks.probe;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.List;

/** Executed only in a throwaway dedicated server with the real reobfuscated release candidate. */
@Mod("hollowrealm_tweaks_probe")
public final class VerificationProbe {
    private static final String PACK_ID = "hollowrealm_tweaks:compat";
    private static final JsonObject RESULTS = new JsonObject();
    public VerificationProbe() { MinecraftForge.EVENT_BUS.addListener(this::started); }

    private void started(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        try {
            verify(server, "initial");
            server.reloadResources(server.getPackRepository().getSelectedIds()).whenComplete((result, error) -> server.execute(() -> {
                try {
                    if (error != null) throw new IllegalStateException("Reload failed", error);
                    verify(server, "reload");
                    Files.writeString(Path.of("verification-result.txt"), "PASS\n" + System.getProperty("hrt.probe.mode", "active") + "\n");
                    Files.writeString(Path.of("verification-details.json"), RESULTS.toString());
                    System.out.println("[HRT-PROBE] PASS");
                } catch (Throwable failure) { fail(failure); }
                finally { server.halt(false); }
            }));
        } catch (Throwable failure) {
            fail(failure);
            server.halt(false);
        }
    }

    private static void verify(MinecraftServer server, String phase) throws Exception {
        String mode = System.getProperty("hrt.probe.mode", "active");
        var id = ResourceLocation.fromNamespaceAndPath("crittersandcompanions", "loot_modifiers/inject_clams.json");
        var resource = server.getResourceManager().getResource(id);
        boolean packSelected = server.getPackRepository().getSelectedIds().contains(PACK_ID);
        if (mode.equals("absent")) {
            require(!packSelected && resource.isEmpty(), "Optional absent mod registered an override");
            require(server.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("legendary_monsters", "recipes/bottle_of_annihilation.json")).isEmpty(), "Absent recipe was injected");
            System.out.println("[HRT-PROBE] " + phase + " absent: no pack or clam resource");
            return;
        }
        require(resource.isPresent(), "Clam resource not found");
        var winner = resource.orElseThrow();
        com.google.gson.JsonObject json;
        try (var reader = winner.openAsReader()) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
            var terms = json.getAsJsonArray("conditions").get(0).getAsJsonObject().getAsJsonArray("terms");
            for (var term : terms) require(term.getAsJsonObject().get("condition").getAsString()
                    .equals(mode.equals("active") ? "forge:loot_table_id" : "neoforge:loot_table_id"), "Unexpected condition ID");
            require(json.getAsJsonArray("conditions").get(1).getAsJsonObject().get("chance").getAsDouble() == 0.1, "Drop chance changed");
        }
        require(packSelected == mode.equals("active"), "Unexpected pack selection");
        require(winner.sourcePackId().equals(PACK_ID) == mode.equals("active"), "Wrong priority winner: " + winner.sourcePackId());
        boolean clamDecoded;
        try { clamDecoded = IGlobalLootModifier.DIRECT_CODEC.parse(JsonOps.INSTANCE, json).result().isPresent(); }
        catch (RuntimeException invalidOriginal) {
            if (mode.equals("active")) throw invalidOriginal;
            clamDecoded = false;
        }
        require(clamDecoded == mode.equals("active"), "GLM decoding did not match mode");
        if (mode.equals("active")) {
            int positive = countClams(server, "chests/shipwreck_treasure");
            int negative = countClams(server, "chests/simple_dungeon");
            require(positive > 0 && negative == 0, "Unexpected scripted loot: " + positive + "/" + negative);
            System.out.println("[HRT-PROBE] " + phase + " winner=" + winner.sourcePackId() + " decoded=" + clamDecoded
                    + " scripted_positive=" + positive + " scripted_negative=" + negative + " seeds=512");
        } else System.out.println("[HRT-PROBE] " + phase + " " + mode + " decoded=" + clamDecoded + " pack_selected=" + packSelected);
        var result = new JsonObject();
        verifyBottle(server, mode, phase, result);
        verifyAffixes(server, mode, phase, result);
        verifyCompatGems(server, mode, phase, result);
        FabledReforgingProbeCheck.run(server, mode, phase, result);
        SpellRarityProbeCheck.run(server, mode, phase, result);
        RESULTS.add(phase, result);
    }

    private static void verifyBottle(MinecraftServer server, String mode, String phase, JsonObject result) throws Exception {
        var id = ResourceLocation.fromNamespaceAndPath("legendary_monsters", "bottle_of_annihilation");
        var recipe = server.getRecipeManager().byKey(id);
        boolean active = mode.equals("active");
        require(recipe.isPresent() == active, "Recipe registry presence does not match mode");
        var resource = server.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("legendary_monsters", "recipes/bottle_of_annihilation.json")).orElseThrow();
        require(resource.sourcePackId().equals(PACK_ID) == active, "Wrong bottle recipe resource winner");
        if (active) {
            require(recipe.get() instanceof CraftingRecipe, "Bottle recipe is not a crafting recipe");
            var crafting = (CraftingRecipe) recipe.get();
            var grid = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
                @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
                @Override public boolean stillValid(Player player) { return true; }
            }, 3, 3);
            grid.setItem(0, new ItemStack(Items.ENDER_EYE));
            grid.setItem(3, new ItemStack(Items.GLASS_BOTTLE));
            require(crafting.matches(grid, server.overworld()), "Original two-ingredient shape does not match");
            var output = crafting.assemble(grid, server.registryAccess());
            require(output.getCount() == 8 && id.equals(ForgeRegistries.ITEMS.getKey(output.getItem())), "Wrong crafted bottle output");
            grid.setItem(0, new ItemStack(Items.DIRT));
            require(!crafting.matches(grid, server.overworld()), "Invalid ingredient was accepted");
        }
        result.addProperty("bottle_recipe", active);
        System.out.println("[HRT-PROBE] " + phase + " bottle_recipe=" + recipe.isPresent() + " crafted_count=" + (active ? 8 : 0));
    }

    private static void verifyAffixes(MinecraftServer server, String mode, String phase, JsonObject result) throws Exception {
        boolean active = mode.equals("active");
        var registryClass = Class.forName("dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry");
        var registry = registryClass.getField("INSTANCE").get(null);
        @SuppressWarnings("unchecked")
        var keys = (Set<ResourceLocation>) registryClass.getMethod("getKeys").invoke(registry);
        for (String name : List.of("bullet_damage", "projectile_speed", "reload_speed", "spread_reduction")) {
            var id = ResourceLocation.fromNamespaceAndPath("fallen_gems_affixes", "gun/attribute/" + name);
            var path = ResourceLocation.fromNamespaceAndPath("fallen_gems_affixes", "affixes/gun/attribute/" + name + ".json");
            var resource = server.getResourceManager().getResource(path).orElseThrow();
            require(resource.sourcePackId().equals(PACK_ID) == active, "Wrong affix resource winner: " + name);
            try (var reader = resource.openAsReader()) {
                var json = JsonParser.parseReader(reader).getAsJsonObject();
                require(CraftingHelper.processConditions(json, "conditions", ICondition.IContext.EMPTY) != active, "Native condition did not gate Scguns affix");
            }
            require(!keys.contains(id), "Invalid optional gun affix registered: " + id);
        }
        var otherKeys = new JsonArray();
        keys.stream().map(ResourceLocation::toString).filter(id -> !id.startsWith("fallen_gems_affixes:gun/"))
                .sorted().forEach(otherKeys::add);
        require(otherKeys.size() > 0, "All other affixes disappeared");
        result.add("non_gun_affixes", otherKeys);
        System.out.println("[HRT-PROBE] " + phase + " gun_conditions=" + active + " invalid_gun_registered=0 other_affixes=" + otherKeys.size());
    }

    private static void verifyCompatGems(MinecraftServer server, String mode, String phase, JsonObject result) throws Exception {
        boolean active = mode.equals("active");
        var gemRegistryClass = Class.forName("dev.shadowsoffire.apotheosis.adventure.socket.gem.GemRegistry");
        var gemRegistry = gemRegistryClass.getField("INSTANCE").get(null);
        @SuppressWarnings("unchecked")
        var gemKeys = (Set<ResourceLocation>) gemRegistryClass.getMethod("getKeys").invoke(gemRegistry);

        var spellLightningPath = ResourceLocation.fromNamespaceAndPath("fallen_gems_affixes", "gems/compat/spell_lightning.json");
        var spellLightningRes = server.getResourceManager().getResource(spellLightningPath).orElseThrow();
        require(spellLightningRes.sourcePackId().equals(PACK_ID) == active, "Wrong spell_lightning resource winner");
        try (var reader = spellLightningRes.openAsReader()) {
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            var conditions = json.getAsJsonArray("conditions");
            require(conditions.size() == (active ? 2 : 1), "Unexpected spell_lightning conditions count");
            if (active) {
                require(!CraftingHelper.processConditions(json, "conditions", ICondition.IContext.EMPTY),
                        "spell_lightning condition should evaluate false when additional_attributes is absent");
            }
        }
        var spellLightningId = ResourceLocation.fromNamespaceAndPath("fallen_gems_affixes", "compat/spell_lightning");
        require(!gemKeys.contains(spellLightningId), "Broken spell_lightning gem registered: " + spellLightningId);

        var hemolithPath = ResourceLocation.fromNamespaceAndPath("fallen_gems_affixes", "gems/compat/hemolith_shard.json");
        var hemolithRes = server.getResourceManager().getResource(hemolithPath).orElseThrow();
        require(hemolithRes.sourcePackId().equals(PACK_ID) == active, "Wrong hemolith_shard resource winner");
        try (var reader = hemolithRes.openAsReader()) {
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            var bonuses = json.getAsJsonArray("bonuses");
            require(bonuses.size() == (active ? 2 : 3), "Unexpected hemolith_shard bonuses count: " + bonuses.size());
        }
        var hemolithId = ResourceLocation.fromNamespaceAndPath("fallen_gems_affixes", "compat/hemolith_shard");
        if (ModList.get().isLoaded("irons_spellbooks")) {
            require(gemKeys.contains(hemolithId) == active, "hemolith_shard registration did not match mode");
        }

        var leggingsPath = ResourceLocation.fromNamespaceAndPath("fallen_gems_affixes", "extra_gem_bonuses/compat/hemolith_shard_leggings.json");
        var leggingsRes = server.getResourceManager().getResource(leggingsPath).orElseThrow();
        require(leggingsRes.sourcePackId().equals(PACK_ID) == active, "Wrong hemolith_shard_leggings resource winner");
        try (var reader = leggingsRes.openAsReader()) {
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            var conditions = json.getAsJsonArray("conditions");
            require(conditions.size() == 1, "Unexpected hemolith_shard_leggings conditions count");
            if (active) {
                require("forge:false".equals(conditions.get(0).getAsJsonObject().get("type").getAsString()),
                        "Expected forge:false condition on hemolith_shard_leggings");
                require(!CraftingHelper.processConditions(json, "conditions", ICondition.IContext.EMPTY),
                        "hemolith_shard_leggings forge:false condition should evaluate false");
            }
        }
        var extraRegistryClass = Class.forName("net.rtxyd.fallen.lib.runtime.forgemod.network.ExtraGemBonusRegistry");
        var extraRegistry = extraRegistryClass.getField("INSTANCE").get(null);
        @SuppressWarnings("unchecked")
        var extraKeys = (Set<ResourceLocation>) extraRegistryClass.getMethod("getKeys").invoke(extraRegistry);
        var leggingsId = ResourceLocation.fromNamespaceAndPath("fallen_gems_affixes", "compat/hemolith_shard_leggings");
        require(!extraKeys.contains(leggingsId), "Broken hemolith_shard_leggings extra bonus registered: " + leggingsId);
        if (ModList.get().isLoaded("traveloptics") && active) {
            var lightId = ResourceLocation.fromNamespaceAndPath("fallen_gems_affixes", "compat/hemolith_shard_light");
            require(extraKeys.contains(lightId), "Expected hemolith_shard_light extra bonus to be registered");
        }

        result.addProperty("compat_gems", active);
        result.addProperty("registered_gems_count", gemKeys.size());
        System.out.println("[HRT-PROBE] " + phase + " compat_gems=" + active + " hemolith_registered=" + gemKeys.contains(hemolithId)
                + " spell_lightning_registered=" + gemKeys.contains(spellLightningId)
                + " hemolith_leggings_registered=" + extraKeys.contains(leggingsId)
                + " total_gems=" + gemKeys.size());
    }

    private static int countClams(MinecraftServer server, String lootPath) {
        var clam = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("crittersandcompanions", "clam"));
        require(clam != null, "Clam item not registered");
        var params = new LootParams.Builder(server.overworld()).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
        var table = server.getLootData().getLootTable(ResourceLocation.fromNamespaceAndPath("minecraft", lootPath));
        int count = 0;
        for (long seed = 0; seed < 512; seed++)
            for (var stack : table.getRandomItems(params, seed)) if (stack.is(clam)) count += stack.getCount();
        return count;
    }

    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private static void fail(Throwable error) {
        error.printStackTrace();
        System.out.println("[HRT-PROBE] FAIL " + error);
        try { Files.writeString(Path.of("verification-result.txt"), "FAIL\n" + error + "\n"); }
        catch (Exception writeFailure) { writeFailure.printStackTrace(); }
    }
}
