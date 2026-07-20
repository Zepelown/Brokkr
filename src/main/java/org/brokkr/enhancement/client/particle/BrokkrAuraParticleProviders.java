package org.brokkr.enhancement.client.particle;

import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.brokkr.enhancement.particle.ModParticles;

public final class BrokkrAuraParticleProviders {
    private BrokkrAuraParticleProviders() {
    }

    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.AURA_SPARK.get(),
                sprites -> BrokkrAuraParticle.provider(sprites, BrokkrAuraParticleProfile.SPARK));
        event.registerSpriteSet(ModParticles.AURA_EMBER.get(),
                sprites -> BrokkrAuraParticle.provider(sprites, BrokkrAuraParticleProfile.EMBER));
        event.registerSpriteSet(ModParticles.AURA_RUNE.get(),
                sprites -> BrokkrAuraParticle.provider(sprites, BrokkrAuraParticleProfile.RUNE));
        event.registerSpriteSet(ModParticles.AURA_ENERGY_STREAK.get(),
                sprites -> BrokkrAuraParticle.provider(sprites, BrokkrAuraParticleProfile.ENERGY_STREAK));
        event.registerSpriteSet(ModParticles.AURA_BURST.get(),
                sprites -> BrokkrAuraParticle.provider(sprites, BrokkrAuraParticleProfile.BURST));
    }
}
