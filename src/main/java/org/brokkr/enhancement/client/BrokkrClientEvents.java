package org.brokkr.enhancement.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.brokkr.Brokkr;
import org.brokkr.enhancement.menu.ModMenus;

@EventBusSubscriber(modid = Brokkr.MODID, value = Dist.CLIENT)
public final class BrokkrClientEvents {
    private BrokkrClientEvents() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ENHANCEMENT_MENU.get(), EnhancementScreen::new);
    }
}
