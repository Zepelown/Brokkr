package org.brokkr.enhancement;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.brokkr.enhancement.item.ModItems;
import org.brokkr.enhancement.profile.EnhancedWeaponProfile;
import org.brokkr.enhancement.profile.WeaponEnhancementProfiles;

import java.util.Optional;

public final class WeaponEnhancementService {
    private WeaponEnhancementService() {
    }

    public static EnhancementResult attempt(Player player) {
        ItemStack weapon = player.getMainHandItem();
        ItemStack stone = findEnhancementStone(player);
        return EnhancementAttemptService.attempt(weapon, stone, player.getRandom());
    }

    private static ItemStack findEnhancementStone(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.ENHANCEMENT_STONE.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static EnhancementResult setLevel(ItemStack weapon, int level) {
        Optional<EnhancedWeaponProfile> profile = WeaponEnhancementProfiles.find(weapon);
        if (profile.isEmpty()) {
            return new EnhancementResult(EnhancementResult.Type.NOT_SUPPORTED_WEAPON, 0, 0, 0);
        }
        if (level < EnhancementData.MIN_LEVEL || level > profile.get().maxLevel()) {
            int currentLevel = EnhancementData.getLevel(weapon);
            return new EnhancementResult(EnhancementResult.Type.INVALID_LEVEL, currentLevel, currentLevel, 0);
        }

        int previousLevel = EnhancementData.getLevel(weapon);
        EnhancementData.setLevel(weapon, level);
        return new EnhancementResult(EnhancementResult.Type.SUCCESS, previousLevel, level, 0);
    }

}
