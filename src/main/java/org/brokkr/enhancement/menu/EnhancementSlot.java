package org.brokkr.enhancement.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.brokkr.enhancement.profile.WeaponEnhancementProfiles;

public class EnhancementSlot extends Slot {
    public EnhancementSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return WeaponEnhancementProfiles.isSupported(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
