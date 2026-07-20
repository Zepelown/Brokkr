package org.brokkr.enhancement.client;

import java.util.Optional;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import org.brokkr.enhancement.EnhancementData;

public enum HeldWeaponAuraBracket {
    FIRST(10, 1, 1),
    SECOND(8, 1, 2),
    THIRD(6, 2, 2),
    FOURTH(5, 2, 3);

    private final int intervalTicks;
    private final int minParticles;
    private final int maxParticles;

    HeldWeaponAuraBracket(int intervalTicks, int minParticles, int maxParticles) {
        this.intervalTicks = intervalTicks;
        this.minParticles = minParticles;
        this.maxParticles = maxParticles;
    }

    public static Optional<HeldWeaponAuraBracket> fromLevel(int level) {
        int clampedLevel = EnhancementData.clampLevel(level);
        if (clampedLevel <= EnhancementData.MIN_LEVEL) {
            return Optional.empty();
        }
        if (clampedLevel <= 4) {
            return Optional.of(FIRST);
        }
        if (clampedLevel <= 9) {
            return Optional.of(SECOND);
        }
        if (clampedLevel <= 14) {
            return Optional.of(THIRD);
        }
        return Optional.of(FOURTH);
    }

    public static boolean isMaxLevel(int level) {
        return EnhancementData.clampLevel(level) >= EnhancementData.MAX_LEVEL;
    }

    public int intervalTicks() {
        return intervalTicks;
    }

    public int particleCount(RandomSource random) {
        return minParticles + random.nextInt(maxParticles - minParticles + 1);
    }

    public ParticleOptions particle(RandomSource random) {
        return switch (this) {
            case FIRST -> ParticleTypes.CRIT;
            case SECOND -> random.nextBoolean() ? ParticleTypes.ENCHANT : ParticleTypes.CRIT;
            case THIRD -> random.nextBoolean() ? ParticleTypes.ENCHANT : ParticleTypes.WITCH;
            case FOURTH -> random.nextBoolean() ? ParticleTypes.FLAME : ParticleTypes.CRIT;
        };
    }
}
