package org.brokkr.enhancement;

import net.minecraft.util.RandomSource;

public final class EnhancementChance {
    private EnhancementChance() {
    }

    public static int successChanceForCurrentLevel(int currentLevel) {
        int clamped = EnhancementData.clampLevel(currentLevel);
        if (clamped <= 4) {
            return 100;
        }
        if (clamped <= 9) {
            return 70;
        }
        if (clamped <= 14) {
            return 40;
        }
        return 10;
    }

    public static boolean rollSuccess(int currentLevel, RandomSource random) {
        return random.nextInt(100) < successChanceForCurrentLevel(currentLevel);
    }
}
