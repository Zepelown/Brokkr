package org.brokkr.enhancement.item;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.brokkr.Brokkr;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Brokkr.MODID);

    public static final DeferredItem<Item> ENHANCEMENT_STONE = ITEMS.registerSimpleItem("enhancement_stone");

    private ModItems() {
    }
}
