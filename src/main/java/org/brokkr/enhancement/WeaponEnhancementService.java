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
        Optional<EnhancedWeaponProfile> profile = WeaponEnhancementProfiles.find(weapon);
        if (profile.isEmpty()) {
            return new EnhancementResult(EnhancementResult.Type.NOT_SUPPORTED_WEAPON, 0, 0, 0);
        }

        int previousLevel = EnhancementData.getLevel(weapon);
        if (previousLevel >= profile.get().maxLevel()) {
            return new EnhancementResult(EnhancementResult.Type.MAX_LEVEL, previousLevel, previousLevel, 0);
        }

        int successChance = EnhancementChance.successChanceForCurrentLevel(previousLevel);
        if (!consumeEnhancementStone(player)) {
            return new EnhancementResult(EnhancementResult.Type.NO_STONE, previousLevel, previousLevel, successChance);
        }

        if (EnhancementChance.rollSuccess(previousLevel, player.getRandom())) {
            int newLevel = EnhancementData.increment(weapon);
            return new EnhancementResult(EnhancementResult.Type.SUCCESS, previousLevel, newLevel, successChance);
        }

        return new EnhancementResult(EnhancementResult.Type.FAILED_ROLL, previousLevel, previousLevel, successChance);
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

    private static boolean consumeEnhancementStone(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.ENHANCEMENT_STONE.get())) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }
}
