package org.brokkr.enhancement;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.brokkr.enhancement.item.ModItems;
import org.brokkr.enhancement.profile.EnhancedWeaponProfile;
import org.brokkr.enhancement.profile.WeaponEnhancementProfiles;

import java.util.Optional;

public final class EnhancementAttemptService {
    private EnhancementAttemptService() {
    }

    public static EnhancementResult attempt(ItemStack weapon, ItemStack stone, RandomSource random) {
        Optional<EnhancedWeaponProfile> profile = WeaponEnhancementProfiles.find(weapon);
        if (profile.isEmpty()) {
            return new EnhancementResult(EnhancementResult.Type.NOT_SUPPORTED_WEAPON, 0, 0, 0);
        }

        int previousLevel = EnhancementData.getLevel(weapon);
        if (previousLevel >= profile.get().maxLevel()) {
            return new EnhancementResult(EnhancementResult.Type.MAX_LEVEL, previousLevel, previousLevel, 0);
        }

        int successChance = EnhancementChance.successChanceForCurrentLevel(previousLevel);
        if (stone.isEmpty() || !stone.is(ModItems.ENHANCEMENT_STONE.get())) {
            return new EnhancementResult(EnhancementResult.Type.NO_STONE, previousLevel, previousLevel, successChance);
        }

        stone.shrink(1);
        if (EnhancementChance.rollSuccess(previousLevel, random)) {
            int newLevel = EnhancementData.increment(weapon);
            return new EnhancementResult(EnhancementResult.Type.SUCCESS, previousLevel, newLevel, successChance);
        }

        return new EnhancementResult(EnhancementResult.Type.FAILED_ROLL, previousLevel, previousLevel, successChance);
    }
}
