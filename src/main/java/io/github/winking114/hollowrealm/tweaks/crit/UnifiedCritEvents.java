package io.github.winking114.hollowrealm.tweaks.crit;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * Registers server-side attribute synchronization hooks for {@link UnifiedCritNormalizer}.
 * Automatically stays dormant if {@code attributeslib} is missing or legacy {@code fusedcritical} is still installed.
 */
public final class UnifiedCritEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    private UnifiedCritEvents() {
    }

    public static void register() {
        ModList mods = ModList.get();
        if (!mods.isLoaded("attributeslib")) {
            LOGGER.info("[HollowRealm Tweaks] SKIP UnifiedCritModule: attributeslib is not loaded");
            return;
        }
        if (mods.isLoaded("fusedcritical")) {
            LOGGER.warn("[HollowRealm Tweaks] SKIP UnifiedCritModule: legacy fusedcritical is still installed");
            return;
        }

        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.addListener((EntityJoinLevelEvent event) -> {
            if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
                UnifiedCritNormalizer.normalize(living);
            }
        });
        bus.addListener((LivingEquipmentChangeEvent event) -> UnifiedCritNormalizer.normalize(event.getEntity()));
        bus.addListener((PlayerEvent.PlayerRespawnEvent event) -> UnifiedCritNormalizer.normalize(event.getEntity()));
        bus.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> UnifiedCritNormalizer.normalize(event.getEntity()));
        bus.addListener((MobEffectEvent.Added event) -> UnifiedCritNormalizer.normalize(event.getEntity()));
        bus.addListener((MobEffectEvent.Remove event) -> UnifiedCritNormalizer.normalize(event.getEntity()));
        bus.addListener((MobEffectEvent.Expired event) -> UnifiedCritNormalizer.normalize(event.getEntity()));

        if (mods.isLoaded("curios")) {
            CuriosCritCompat.register(bus);
        }

        LOGGER.info("[HollowRealm Tweaks] ENABLED UnifiedCritModule (canonical attributeslib:crit_chance & crit_damage)");
    }
}
