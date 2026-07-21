package org.brokkr.enhancement;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.brokkr.Brokkr;

public final class EnhancementData {
    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 20;
    private static final String LEVEL_KEY = Brokkr.MODID + ":enhancement_level";

    private EnhancementData() {
    }

    public static int getLevel(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        return clampLevel(tag.getIntOr(LEVEL_KEY, MIN_LEVEL));
    }

    public static void setLevel(ItemStack stack, int level) {
        int clamped = clampLevel(level);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (clamped <= MIN_LEVEL) {
                tag.remove(LEVEL_KEY);
            } else {
                tag.putInt(LEVEL_KEY, clamped);
            }
        });
        EnhancementNameService.refreshName(stack, clamped);
        AwakenedWeaponModelService.refreshModel(stack, clamped);
    }

    public static int increment(ItemStack stack) {
        int nextLevel = clampLevel(getLevel(stack) + 1);
        setLevel(stack, nextLevel);
        return nextLevel;
    }

    public static boolean hasEnhancement(ItemStack stack) {
        return getLevel(stack) > MIN_LEVEL;
    }

    public static int clampLevel(int level) {
        return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
    }
}
