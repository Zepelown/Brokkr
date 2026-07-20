package org.brokkr.enhancement;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public final class EnhancementParticles {
    private EnhancementParticles() {
    }

    public static void spawn(ServerLevel level, LivingEntity target, EnhancementTier tier) {
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.5D;
        double z = target.getZ();

        switch (tier) {
            case LOW -> spawn(level, ParticleTypes.CRIT, x, y, z, 6, 0.25D, 0.35D, 0.25D, 0.05D);
            case MID -> {
                spawn(level, ParticleTypes.CRIT, x, y, z, 8, 0.3D, 0.4D, 0.3D, 0.06D);
                spawn(level, ParticleTypes.ENCHANT, x, y, z, 8, 0.35D, 0.45D, 0.35D, 0.04D);
            }
            case HIGH -> {
                spawn(level, ParticleTypes.ENCHANT, x, y, z, 12, 0.4D, 0.5D, 0.4D, 0.05D);
                spawn(level, ParticleTypes.WITCH, x, y, z, 10, 0.35D, 0.45D, 0.35D, 0.04D);
            }
            case VERY_HIGH -> {
                spawn(level, ParticleTypes.CRIT, x, y, z, 14, 0.45D, 0.55D, 0.45D, 0.08D);
                spawn(level, ParticleTypes.FLAME, x, y, z, 12, 0.35D, 0.45D, 0.35D, 0.03D);
            }
            case MAX -> {
                spawn(level, ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 18, 0.55D, 0.7D, 0.55D, 0.08D);
                spawn(level, ParticleTypes.END_ROD, x, y, z, 16, 0.45D, 0.6D, 0.45D, 0.05D);
            }
        }
    }

    private static void spawn(ServerLevel level, ParticleOptions type, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        level.sendParticles(type, x, y, z, count, dx, dy, dz, speed);
    }
}
