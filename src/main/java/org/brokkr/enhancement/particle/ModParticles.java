package org.brokkr.enhancement.particle;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.brokkr.Brokkr;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Brokkr.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AURA_SPARK =
            PARTICLE_TYPES.register("aura_spark", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AURA_EMBER =
            PARTICLE_TYPES.register("aura_ember", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AURA_RUNE =
            PARTICLE_TYPES.register("aura_rune", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AURA_ENERGY_STREAK =
            PARTICLE_TYPES.register("aura_energy_streak", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AURA_BURST =
            PARTICLE_TYPES.register("aura_burst", () -> new SimpleParticleType(false));

    private ModParticles() {
    }
}
