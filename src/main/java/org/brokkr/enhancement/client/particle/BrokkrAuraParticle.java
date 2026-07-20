package org.brokkr.enhancement.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class BrokkrAuraParticle extends TextureSheetParticle {
    private final float initialAlpha;
    private final float rotationSpeed;

    protected BrokkrAuraParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            SpriteSet sprites,
            BrokkrAuraParticleProfile profile
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.initialAlpha = profile.alpha();
        this.rotationSpeed = profile.rotationSpeed(this.random);
        this.hasPhysics = false;
        this.friction = profile.friction();
        this.gravity = profile.gravity();
        this.lifetime = profile.lifetime(this.random);
        this.quadSize = profile.scale(this.random);
        this.setColor(profile.red(), profile.green(), profile.blue());
        this.alpha = initialAlpha;
        this.roll = this.random.nextFloat() * (float) (Math.PI * 2.0D);
        this.oRoll = this.roll;
        this.pickSprite(sprites);
    }

    public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites, BrokkrAuraParticleProfile profile) {
        return (type, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
                new BrokkrAuraParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, profile);
    }

    @Override
    public void tick() {
        super.tick();
        this.oRoll = this.roll;
        this.roll += rotationSpeed;
        float progress = Math.min(1.0F, (float) this.age / (float) this.lifetime);
        this.alpha = initialAlpha * (1.0F - progress);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
