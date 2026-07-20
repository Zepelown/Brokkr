package org.brokkr.enhancement.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
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

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.addListener(BrokkrClientEvents::clientTick);
    }

    private static void clientTick(ClientTickEvent.Post event) {
        HeldWeaponAuraEffects.tick(Minecraft.getInstance());
    }
}
