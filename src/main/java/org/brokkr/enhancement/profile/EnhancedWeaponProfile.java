package org.brokkr.enhancement.profile;

import net.minecraft.world.item.ItemStack;
import org.brokkr.enhancement.EnhancementTier;

public interface EnhancedWeaponProfile {
    String id();

    boolean supports(ItemStack stack);

    int maxLevel();

    float bonusDamage(int level);

    EnhancementTier tier(int level);
}
