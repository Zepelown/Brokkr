package org.brokkr.enhancement.profile;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.brokkr.enhancement.EnhancementData;
import org.brokkr.enhancement.EnhancementTier;

import java.util.Set;

public final class SwordEnhancementProfile implements EnhancedWeaponProfile {
    private static final Set<Item> SUPPORTED_ITEMS = Set.of(
            Items.WOODEN_SWORD,
            Items.STONE_SWORD,
            Items.IRON_SWORD,
            Items.GOLDEN_SWORD,
            Items.DIAMOND_SWORD,
            Items.NETHERITE_SWORD
    );

    @Override
    public String id() {
        return "sword";
    }

    @Override
    public boolean supports(ItemStack stack) {
        return !stack.isEmpty() && SUPPORTED_ITEMS.contains(stack.getItem());
    }

    @Override
    public int maxLevel() {
        return EnhancementData.MAX_LEVEL;
    }

    @Override
    public float bonusDamage(int level) {
        return EnhancementData.clampLevel(level) * 0.2f;
    }

    @Override
    public EnhancementTier tier(int level) {
        return EnhancementTier.fromLevel(EnhancementData.clampLevel(level));
    }
}
