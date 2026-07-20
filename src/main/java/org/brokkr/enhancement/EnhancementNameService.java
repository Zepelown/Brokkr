package org.brokkr.enhancement;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.brokkr.enhancement.text.EnhancementTextKeys;

public final class EnhancementNameService {
    private EnhancementNameService() {
    }

    public static void refreshName(ItemStack stack, int level) {
        if (level <= EnhancementData.MIN_LEVEL) {
            stack.remove(DataComponents.ITEM_NAME);
            return;
        }

        Component baseName = Component.translatable(stack.getItem().getDescriptionId());
        stack.set(DataComponents.ITEM_NAME, Component.translatable(EnhancementTextKeys.ITEM_NAME_PREFIX, level, baseName));
    }
}
