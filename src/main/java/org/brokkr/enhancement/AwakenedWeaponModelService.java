package org.brokkr.enhancement;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.brokkr.Brokkr;

public final class AwakenedWeaponModelService {
    private static final String AWAKENED_PREFIX = "awakened_";

    private AwakenedWeaponModelService() {
    }

    public static boolean refreshModel(ItemStack stack, int level) {
        if (stack.isEmpty()) {
            return false;
        }

        Optional<String> material = materialKey(stack);
        if (material.isEmpty()) {
            return false;
        }

        int clampedLevel = EnhancementData.clampLevel(level);
        if (clampedLevel < 10) {
            if (hasAwakenedModel(stack)) {
                stack.remove(DataComponents.ITEM_MODEL);
                return true;
            }
            return false;
        }

        ResourceLocation model = ResourceLocation.fromNamespaceAndPath(
                Brokkr.MODID,
                AWAKENED_PREFIX + material.get() + "_sword_" + stageKey(clampedLevel)
        );
        if (!model.equals(stack.get(DataComponents.ITEM_MODEL))) {
            stack.set(DataComponents.ITEM_MODEL, model);
            return true;
        }
        return false;
    }

    public static boolean needsModelRefresh(ItemStack stack) {
        return EnhancementData.hasEnhancement(stack) || hasAwakenedModel(stack);
    }

    public static boolean hasAwakenedModel(ItemStack stack) {
        ResourceLocation model = stack.get(DataComponents.ITEM_MODEL);
        return model != null && Brokkr.MODID.equals(model.getNamespace()) && model.getPath().startsWith(AWAKENED_PREFIX);
    }

    private static Optional<String> materialKey(ItemStack stack) {
        if (stack.is(Items.WOODEN_SWORD)) {
            return Optional.of("wooden");
        }
        if (stack.is(Items.STONE_SWORD)) {
            return Optional.of("stone");
        }
        if (stack.is(Items.IRON_SWORD)) {
            return Optional.of("iron");
        }
        if (stack.is(Items.GOLDEN_SWORD)) {
            return Optional.of("golden");
        }
        if (stack.is(Items.DIAMOND_SWORD)) {
            return Optional.of("diamond");
        }
        if (stack.is(Items.NETHERITE_SWORD)) {
            return Optional.of("netherite");
        }
        return Optional.empty();
    }

    private static String stageKey(int level) {
        if (level <= 14) {
            return "white";
        }
        if (level <= 19) {
            return "orange";
        }
        return "final";
    }
}
