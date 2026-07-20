package org.brokkr.enhancement.profile;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class WeaponEnhancementProfiles {
    private static final List<EnhancedWeaponProfile> PROFILES = List.of(new SwordEnhancementProfile());

    private WeaponEnhancementProfiles() {
    }

    public static Optional<EnhancedWeaponProfile> find(ItemStack stack) {
        return PROFILES.stream().filter(profile -> profile.supports(stack)).findFirst();
    }

    public static boolean isSupported(ItemStack stack) {
        return find(stack).isPresent();
    }
}
