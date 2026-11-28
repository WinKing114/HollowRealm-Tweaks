package io.github.winking114.hollowrealm.tweaks.crit;

import net.minecraftforge.eventbus.api.IEventBus;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

/**
 * Isolated Curios listener class so Curios types are only resolved when Curios is loaded.
 */
final class CuriosCritCompat {
    private CuriosCritCompat() {
    }

    static void register(IEventBus bus) {
        bus.addListener((CurioChangeEvent event) -> UnifiedCritNormalizer.normalize(event.getEntity()));
    }
}
