package org.brokkr.enhancement.event;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.brokkr.enhancement.AwakenedWeaponModelService;
import org.brokkr.enhancement.EnhancementData;

public final class EnhancementInventoryEvents {
    private static final int MODEL_REFRESH_INTERVAL_TICKS = 40;

    private EnhancementInventoryEvents() {
    }

    public static void refreshAwakenedModels(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % MODEL_REFRESH_INTERVAL_TICKS != 0) {
            return;
        }

        Inventory inventory = player.getInventory();
        boolean changed = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            changed |= refreshStack(inventory.getItem(slot));
        }

        if (changed) {
            inventory.setChanged();
        }
    }

    private static boolean refreshStack(ItemStack stack) {
        if (stack.isEmpty() || !AwakenedWeaponModelService.needsModelRefresh(stack)) {
            return false;
        }

        return AwakenedWeaponModelService.refreshModel(stack, EnhancementData.getLevel(stack));
    }
}
