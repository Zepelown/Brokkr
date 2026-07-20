package org.brokkr.enhancement.client;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.brokkr.enhancement.EnhancementData;
import org.brokkr.enhancement.particle.ModParticles;
import org.brokkr.enhancement.profile.WeaponEnhancementProfiles;

public final class HeldWeaponAuraEffects {
    private static final int TICK_COUNTER_CYCLE = 1_200;
    private static final int MAX_FLOURISH_INTERVAL_TICKS = 24;
    private static final double HAND_SIDE_OFFSET = 0.34D;
    private static final double HAND_FORWARD_OFFSET = 0.42D;
    private static final double HAND_HEIGHT_FACTOR = 0.58D;
    private static final double JITTER_RADIUS = 0.08D;
    private static final RandomSource RANDOM = RandomSource.create();

    private static int tickCounter;

    private HeldWeaponAuraEffects() {
    }

    public static void tick(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null || client.player == null) {
            return;
        }

        tickCounter = (tickCounter + 1) % TICK_COUNTER_CYCLE;
        for (AbstractClientPlayer player : level.players()) {
            if (shouldSkip(player)) {
                continue;
            }

            ItemStack stack = player.getMainHandItem();
            if (stack.isEmpty() || WeaponEnhancementProfiles.find(stack).isEmpty()) {
                continue;
            }

            int enhancementLevel = EnhancementData.getLevel(stack);
            Optional<HeldWeaponAuraBracket> bracket = HeldWeaponAuraBracket.fromLevel(enhancementLevel);
            if (bracket.isEmpty()) {
                continue;
            }

            HeldWeaponAuraBracket auraBracket = bracket.get();
            if (tickCounter % auraBracket.intervalTicks() == 0) {
                spawnBaseAura(level, player, auraBracket);
            }
            if (HeldWeaponAuraBracket.isMaxLevel(enhancementLevel) && tickCounter % MAX_FLOURISH_INTERVAL_TICKS == 0) {
                spawnParticle(level, ModParticles.AURA_BURST.get(), auraOrigin(player, RANDOM), RANDOM);
            }
        }
    }

    private static boolean shouldSkip(AbstractClientPlayer player) {
        return player.isRemoved() || !player.isAlive() || player.isSpectator() || player.isInvisible();
    }

    private static void spawnBaseAura(ClientLevel level, AbstractClientPlayer player, HeldWeaponAuraBracket bracket) {
        int count = bracket.particleCount(RANDOM);
        for (int i = 0; i < count; i++) {
            spawnParticle(level, bracket.particle(RANDOM), auraOrigin(player, RANDOM), RANDOM);
        }
    }

    private static Vec3 auraOrigin(AbstractClientPlayer player, RandomSource random) {
        Vec3 forward = horizontalForward(player);
        Vec3 side = handSide(player, forward);
        return player.position()
                .add(0.0D, player.getBbHeight() * HAND_HEIGHT_FACTOR, 0.0D)
                .add(forward.scale(HAND_FORWARD_OFFSET))
                .add(side.scale(HAND_SIDE_OFFSET))
                .add(jitter(random, JITTER_RADIUS), jitter(random, JITTER_RADIUS * 0.6D), jitter(random, JITTER_RADIUS));
    }

    private static Vec3 horizontalForward(AbstractClientPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.lengthSqr() < 1.0E-4D) {
            double yaw = Math.toRadians(player.getYRot());
            flat = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        }
        return flat.normalize();
    }

    private static Vec3 handSide(AbstractClientPlayer player, Vec3 forward) {
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        return player.getMainArm() == HumanoidArm.LEFT ? right.scale(-1.0D) : right;
    }

    private static void spawnParticle(ClientLevel level, ParticleOptions particle, Vec3 origin, RandomSource random) {
        double vx = jitter(random, 0.015D);
        double vy = 0.01D + random.nextDouble() * 0.03D;
        double vz = jitter(random, 0.015D);
        level.addParticle(particle, origin.x, origin.y, origin.z, vx, vy, vz);
    }

    private static double jitter(RandomSource random, double radius) {
        return (random.nextDouble() * 2.0D - 1.0D) * radius;
    }
}
