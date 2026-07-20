package org.brokkr;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.brokkr.enhancement.event.EnhancementCombatEvents;
import org.brokkr.enhancement.event.EnhancementCommandEvents;
import org.brokkr.enhancement.event.EnhancementTooltipEvents;
import org.brokkr.enhancement.item.ModItems;

@Mod(Brokkr.MODID)
public class Brokkr {
    public static final String MODID = "brokkr";

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BROKKR_TAB = CREATIVE_MODE_TABS.register("brokkr_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.brokkr"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModItems.ENHANCEMENT_STONE.get().getDefaultInstance())
            .displayItems((parameters, output) -> output.accept(ModItems.ENHANCEMENT_STONE.get()))
            .build());

    public Brokkr(IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(EnhancementCommandEvents::registerCommands);
        NeoForge.EVENT_BUS.addListener(EnhancementTooltipEvents::addTooltip);
        NeoForge.EVENT_BUS.addListener(EnhancementCombatEvents::addAttackDamage);
        NeoForge.EVENT_BUS.addListener(EnhancementCombatEvents::spawnHitParticles);
    }
}
