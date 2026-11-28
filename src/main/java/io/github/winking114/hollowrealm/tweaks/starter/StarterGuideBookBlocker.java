package io.github.winking114.hollowrealm.tweaks.starter;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ensures new players receive only Eccentric Tome instead of loose mod guide books.
 * <ul>
 *   <li>Pre-marks {@code spell_reaction:guide_book} on login at HIGHEST priority before
 *       {@code spell_reaction}'s NORMAL-priority {@code GuideBookEvent} runs.</li>
 *   <li>Runs a one-time first-join inventory sweep (2 ticks after login) to remove any
 *       stray loose guide books while preserving {@code eccentrictome:tome}, spell books,
 *       and all starter equipment/curios. Never repeats on subsequent logins.</li>
 * </ul>
 */
public final class StarterGuideBookBlocker {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final String SPELL_REACTION_TAG = "spell_reaction:guide_book";
    static final String CLEANUP_DONE_TAG = "hollowrealm:starter_guide_cleanup_v1";

    private static final Set<String> LOOSE_GUIDE_BOOK_IDS = Set.of(
            "patchouli:guide_book",
            "ars_nouveau:worn_notebook",
            "enigmaticlegacy:the_acknowledgment"
    );

    private final Map<UUID, Integer> pendingCleanups = new ConcurrentHashMap<>();

    private StarterGuideBookBlocker() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new StarterGuideBookBlocker());
        LOGGER.info("[HollowRealm Tweaks] ENABLED StarterGuideBookBlocker (Eccentric Tome only starter)");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        CompoundTag persisted = getOrCreatePersisted(serverPlayer);
        if (!persisted.getBoolean(SPELL_REACTION_TAG)) {
            persisted.putBoolean(SPELL_REACTION_TAG, true);
        }
        if (!persisted.getBoolean(CLEANUP_DONE_TAG)) {
            pendingCleanups.put(serverPlayer.getUUID(), 2);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        UUID uuid = serverPlayer.getUUID();
        Integer remaining = pendingCleanups.get(uuid);
        if (remaining == null) {
            return;
        }
        if (remaining > 0) {
            pendingCleanups.put(uuid, remaining - 1);
            return;
        }
        pendingCleanups.remove(uuid);
        CompoundTag persisted = getOrCreatePersisted(serverPlayer);
        if (persisted.getBoolean(CLEANUP_DONE_TAG)) {
            return;
        }
        int removed = stripLooseGuideBooks(serverPlayer.getInventory());
        persisted.putBoolean(CLEANUP_DONE_TAG, true);
        if (removed > 0) {
            serverPlayer.inventoryMenu.broadcastChanges();
            LOGGER.info("[HollowRealm Tweaks] Removed {} stray starter guide book(s) for {}",
                    removed, serverPlayer.getGameProfile().getName());
        }
    }

    static int stripLooseGuideBooks(Inventory inventory) {
        int removed = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (key != null && isLooseGuideBookId(key.toString())) {
                inventory.setItem(i, ItemStack.EMPTY);
                removed++;
            }
        }
        return removed;
    }

    public static boolean isLooseGuideBookId(String itemId) {
        return itemId != null && LOOSE_GUIDE_BOOK_IDS.contains(itemId);
    }

    private static CompoundTag getOrCreatePersisted(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG, CompoundTag.TAG_COMPOUND)) {
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }
}
