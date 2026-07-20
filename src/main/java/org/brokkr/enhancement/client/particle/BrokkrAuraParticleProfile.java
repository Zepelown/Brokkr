package org.brokkr.enhancement.client.particle;

import net.minecraft.util.RandomSource;

public enum BrokkrAuraParticleProfile {
    SPARK(8, 12, 0.08F, 1.0F, 0.88F, 0.45F, 0.0F, 0.92F, 0.85F, 0.04F),
    EMBER(12, 18, 0.10F, 1.0F, 0.48F, 0.08F, -0.01F, 0.90F, 0.90F, 0.03F),
    RUNE(14, 20, 0.12F, 0.95F, 0.78F, 0.32F, 0.0F, 0.88F, 0.90F, 0.025F),
    ENERGY_STREAK(8, 12, 0.20F, 1.0F, 0.62F, 0.12F, 0.0F, 0.86F, 0.75F, 0.08F),
    BURST(10, 16, 0.24F, 1.0F, 0.85F, 0.28F, 0.0F, 0.84F, 0.95F, 0.06F);

    private final int minLifetime;
    private final int maxLifetime;
    private final float scale;
    private final float red;
    private final float green;
    private final float blue;
    private final float gravity;
    private final float friction;
    private final float alpha;
    private final float maxRotationSpeed;

    BrokkrAuraParticleProfile(
            int minLifetime,
            int maxLifetime,
            float scale,
            float red,
            float green,
            float blue,
            float gravity,
            float friction,
            float alpha,
            float maxRotationSpeed
    ) {
        this.minLifetime = minLifetime;
        this.maxLifetime = maxLifetime;
        this.scale = scale;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.gravity = gravity;
        this.friction = friction;
        this.alpha = alpha;
        this.maxRotationSpeed = maxRotationSpeed;
    }

    public int lifetime(RandomSource random) {
        return minLifetime + random.nextInt(maxLifetime - minLifetime + 1);
    }

    public float scale(RandomSource random) {
        return scale * (0.85F + random.nextFloat() * 0.3F);
    }

    public float rotationSpeed(RandomSource random) {
        return (random.nextFloat() * 2.0F - 1.0F) * maxRotationSpeed;
    }

    public float red() {
        return red;
    }

    public float green() {
        return green;
    }

    public float blue() {
        return blue;
    }

    public float gravity() {
        return gravity;
    }

    public float friction() {
        return friction;
    }

    public float alpha() {
        return alpha;
    }
}
