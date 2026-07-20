# Weapon Enhancement MVP 5 Custom Aura Particles Implementation Plan

## Purpose

This document turns `docs/spec/weapon-enhancement-mvp5-custom-aura-particles.md` into an implementation-ready plan.

MVP 5 replaces the MVP 4 held weapon aura's vanilla particles with Brokkr custom particle sprites and adds thin energy streaks from `+15` onward.

Primary goals:

- Keep MVP 4's held-aura trigger and throttling structure.
- Replace held-aura vanilla particles with Brokkr custom particles.
- Generate and add particle PNG assets.
- Register particle JSON definitions.
- Register custom particle types and client providers.
- Keep aura bracket transitions aligned to current-level enhancement chance boundaries.
- Preserve existing hit particles and enhancement UI effects.

## Current Baseline

Existing held aura files:

```text
src/main/java/org/brokkr/enhancement/client/BrokkrClientEvents.java
src/main/java/org/brokkr/enhancement/client/HeldWeaponAuraEffects.java
src/main/java/org/brokkr/enhancement/client/HeldWeaponAuraBracket.java
```

Current flow:

```text
ClientTickEvent.Post
  -> BrokkrClientEvents.clientTick(...)
  -> HeldWeaponAuraEffects.tick(Minecraft.getInstance())
  -> client level players
  -> main-hand item
  -> WeaponEnhancementProfiles.find(stack)
  -> EnhancementData.getLevel(stack)
  -> HeldWeaponAuraBracket.fromLevel(level)
  -> ClientLevel.addParticle(vanilla particle, ...)
```

Current aura bracket boundaries:

```text
+1  .. +4   FIRST
+5  .. +9   SECOND
+10 .. +14  THIRD
+15 .. +20  FOURTH
+20         FOURTH + max flourish
```

These boundaries must remain unchanged because they match `EnhancementChance.successChanceForCurrentLevel(currentLevel)`.

## API Verification Notes

The following APIs were checked against the local NeoForge/Minecraft 1.21.6 artifacts:

```text
net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent
  <T extends ParticleOptions> void registerSpecial(ParticleType<T>, ParticleProvider<T>)
  <T extends ParticleOptions> void registerSprite(ParticleType<T>, ParticleProvider.Sprite<T>)
  <T extends ParticleOptions> void registerSpriteSet(ParticleType<T>, ParticleEngine.SpriteParticleRegistration<T>)

net.minecraft.core.particles.SimpleParticleType
  SimpleParticleType(boolean overrideLimiter)

net.minecraft.core.registries.Registries
  PARTICLE_TYPE

net.minecraft.client.particle.TextureSheetParticle
  TextureSheetParticle(ClientLevel, double, double, double, double, double, double)
  void pickSprite(SpriteSet)
  ParticleRenderType getRenderType()

net.minecraft.client.particle.Particle
  void tick()
  void setColor(float, float, float)
  void setLifetime(int)
  void scale(float)
  protected void setAlpha(float)

net.minecraft.client.particle.ParticleRenderType
  PARTICLE_SHEET_TRANSLUCENT
```

Vanilla particle JSON examples in the local Minecraft resources use the same structure planned for Brokkr:

```json
{
  "textures": [
    "minecraft:glow"
  ]
}
```

This means MVP 5 can use normal NeoForge particle registration, no reflection, and no custom renderer pipeline.

## Readiness Review

Implementation blockers and decisions:

- Particle registry is available through `Registries.PARTICLE_TYPE`.
- `SimpleParticleType(false)` is usable for basic no-payload particles.
- Client provider registration is available through `RegisterParticleProvidersEvent.registerSpriteSet(...)`.
- `TextureSheetParticle` exposes the required subclass hooks for sprite selection, alpha fade, scale, rotation, and translucent rendering.
- Particle JSON texture references use `brokkr:aura_spark`, not `brokkr:particle/aura_spark`.
- All MVP 5 particles are no-payload particle types; no custom `ParticleOptions` codec or network stream codec is needed.
- Energy streaks are implemented as elongated transparent sprites on normal particle quads, not as custom mesh rendering.
- Server-side safety is handled by registering only `ModParticles` from common code and registering providers only from the client event subscriber.

## Implementation Strategy

Use one custom particle implementation class with per-type visual profiles.

Reason:

- All MVP 5 aura particles share the same basic behavior: sprite particle, short lifetime, fade-out, small drift, optional rotation.
- Differences can be represented by a small profile enum instead of separate renderer classes.
- This keeps the first custom particle implementation compact and easier to tune after screenshots.

Add:

```text
src/main/java/org/brokkr/enhancement/particle/ModParticles.java
src/main/java/org/brokkr/enhancement/client/particle/BrokkrAuraParticle.java
src/main/java/org/brokkr/enhancement/client/particle/BrokkrAuraParticleProfile.java
src/main/java/org/brokkr/enhancement/client/particle/BrokkrAuraParticleProviders.java
```

Update:

```text
src/main/java/org/brokkr/Brokkr.java
src/main/java/org/brokkr/enhancement/client/BrokkrClientEvents.java
src/main/java/org/brokkr/enhancement/client/HeldWeaponAuraBracket.java
src/main/java/org/brokkr/enhancement/client/HeldWeaponAuraEffects.java
```

Add resources:

```text
src/main/resources/assets/brokkr/textures/particle/aura_spark.png
src/main/resources/assets/brokkr/textures/particle/aura_ember.png
src/main/resources/assets/brokkr/textures/particle/aura_rune.png
src/main/resources/assets/brokkr/textures/particle/aura_energy_streak.png
src/main/resources/assets/brokkr/textures/particle/aura_burst.png

src/main/resources/assets/brokkr/particles/aura_spark.json
src/main/resources/assets/brokkr/particles/aura_ember.json
src/main/resources/assets/brokkr/particles/aura_rune.json
src/main/resources/assets/brokkr/particles/aura_energy_streak.json
src/main/resources/assets/brokkr/particles/aura_burst.json
```

## Particle Registration Plan

Create `ModParticles`:

```java
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
```

Register in `Brokkr` constructor:

```java
ModParticles.PARTICLE_TYPES.register(modEventBus);
```

Keep `ModParticles` common-side safe:

- No `net.minecraft.client.*` imports.
- No particle provider references.

## Client Provider Registration Plan

Update `BrokkrClientEvents`:

```java
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.brokkr.enhancement.client.particle.BrokkrAuraParticleProviders;

@SubscribeEvent
public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
    BrokkrAuraParticleProviders.register(event);
}
```

`RegisterParticleProvidersEvent` is an `IModBusEvent`, so this belongs in the existing client event subscriber next to `RegisterMenuScreensEvent` and `FMLClientSetupEvent`.

Create `BrokkrAuraParticleProviders`:

```java
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
```

Client provider classes must remain under `org.brokkr.enhancement.client` or `org.brokkr.enhancement.client.particle` and must only be referenced from client event code.

## Particle Class Plan

Create `BrokkrAuraParticleProfile`.

Responsibilities:

- Store min/max lifetime.
- Store base scale.
- Store RGB color.
- Store gravity.
- Store friction.
- Store initial alpha.
- Store rotation speed range.

Concrete first pass:

```text
SPARK          lifetime 8-12,  scale 0.08, color pale gold,  gravity 0.00,  friction 0.92, alpha 0.85
EMBER          lifetime 12-18, scale 0.10, color orange,     gravity -0.01, friction 0.90, alpha 0.90
RUNE           lifetime 14-20, scale 0.12, color warm gold,  gravity 0.00,  friction 0.88, alpha 0.90
ENERGY_STREAK  lifetime 8-12,  scale 0.20, color gold,       gravity 0.00,  friction 0.86, alpha 0.75
BURST          lifetime 10-16, scale 0.24, color bright gold, gravity 0.00,  friction 0.84, alpha 0.95
```

Concrete profile skeleton:

```java
package org.brokkr.enhancement.client.particle;

import net.minecraft.util.RandomSource;

public enum BrokkrAuraParticleProfile {
    SPARK(8, 12, 0.08F, 1.0F, 0.88F, 0.45F, 0.00F, 0.92F, 0.85F, 0.04F),
    EMBER(12, 18, 0.10F, 1.0F, 0.48F, 0.08F, -0.01F, 0.90F, 0.90F, 0.03F),
    RUNE(14, 20, 0.12F, 0.95F, 0.78F, 0.32F, 0.00F, 0.88F, 0.90F, 0.025F),
    ENERGY_STREAK(8, 12, 0.20F, 1.0F, 0.62F, 0.12F, 0.00F, 0.86F, 0.75F, 0.08F),
    BURST(10, 16, 0.24F, 1.0F, 0.85F, 0.28F, 0.00F, 0.84F, 0.95F, 0.06F);

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
```

Create `BrokkrAuraParticle extends TextureSheetParticle`.

Constructor responsibilities:

- Call super with position and velocity.
- Store profile.
- Pick sprite.
- Set color.
- Set lifetime from profile.
- Set quad size from profile.
- Disable physics.
- Set friction.
- Set gravity.
- Set initial alpha.
- Set random initial roll.
- Set per-particle rotation speed.

Tick behavior:

- Call `super.tick()`.
- Fade alpha based on remaining lifetime.
- Apply slow rotation.
- Remove when age reaches lifetime.

Render type:

```java
@Override
public ParticleRenderType getRenderType() {
    return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
}
```

Provider shape:

```java
public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites, BrokkrAuraParticleProfile profile) {
    return (type, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
            new BrokkrAuraParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, profile);
}
```

Concrete particle skeleton:

```java
package org.brokkr.enhancement.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class BrokkrAuraParticle extends TextureSheetParticle {
    private final BrokkrAuraParticleProfile profile;
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
        this.profile = profile;
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
```

Implementation detail:

- `Particle.alpha`, `Particle.hasPhysics`, `Particle.gravity`, `Particle.friction`, `Particle.roll`, and `SingleQuadParticle.quadSize` are protected and available inside `BrokkrAuraParticle`.
- Use direct `this.alpha = ...` instead of calling `setAlpha(...)` from outside the subclass.
- Do not try non-uniform quad scaling for `ENERGY_STREAK` in MVP 5. The streak shape comes from the transparent PNG image itself.

## Resource JSON Plan

Each particle JSON should be minimal.

Example:

```json
{
  "textures": [
    "brokkr:aura_spark"
  ]
}
```

Resource mapping:

```text
assets/brokkr/particles/aura_spark.json          -> brokkr:aura_spark
assets/brokkr/particles/aura_ember.json          -> brokkr:aura_ember
assets/brokkr/particles/aura_rune.json           -> brokkr:aura_rune
assets/brokkr/particles/aura_energy_streak.json  -> brokkr:aura_energy_streak
assets/brokkr/particles/aura_burst.json          -> brokkr:aura_burst
```

Concrete files:

```json
// src/main/resources/assets/brokkr/particles/aura_spark.json
{
  "textures": [
    "brokkr:aura_spark"
  ]
}
```

Use the same shape for:

```text
aura_ember.json          -> "brokkr:aura_ember"
aura_rune.json           -> "brokkr:aura_rune"
aura_energy_streak.json  -> "brokkr:aura_energy_streak"
aura_burst.json          -> "brokkr:aura_burst"
```

Texture lookup rule:

- Particle JSON texture entry `brokkr:aura_spark` resolves to `assets/brokkr/textures/particle/aura_spark.png`.
- Do not include `particle/` in the JSON texture value.
- Do not put these PNG files under `textures/particles`.

## Image Generation Plan

Generate five transparent PNG particle sprites:

```text
aura_spark.png
- 32x32.
- Small pale gold/white spark.
- Transparent edges.

aura_ember.png
- 32x32.
- Orange/gold ember.
- Hot center, soft edge.

aura_rune.png
- 32x32.
- Compact rune shard.
- Warm gold with slight cyan accent.

aura_energy_streak.png
- 32x32.
- Thin elongated diagonal slash.
- Gold/orange core with transparent falloff.
- Must read as a streak, not a square.

aura_burst.png
- 32x32.
- Short radial pulse or starburst.
- Bright gold center, transparent outer fade.
```

Concrete generation requirements:

```text
canvas size: 32x32 for every MVP 5 particle
file format: PNG RGBA
background: fully transparent
style: pixel-compatible sprite with soft alpha edge
max opaque area:
  aura_spark          <= 10x10 center area
  aura_ember          <= 14x14 center area
  aura_rune           <= 16x16 center area
  aura_energy_streak  <= 28x8 diagonal/near-diagonal visible area
  aura_burst          <= 24x24 radial visible area
```

Deterministic asset generation approach:

- Generate simple RGBA sprites programmatically with Java/Python/image tooling.
- Use transparent background.
- Draw small radial gradients, diagonal streaks, and star shapes.
- Keep generated assets in the same resource paths.

Image QA checklist:

- PNG has alpha channel.
- Corners are transparent.
- Sprite is visible at 32x32.
- `aura_energy_streak` reads as a thin line, not a square glow.
- No black/opaque background is present.

Implementation note:

- Use generated images as source assets.
- Post-process generated images into transparent, pixel-compatible sprites before wiring them into resources.
- Verify the alpha channel before the first build.
- Avoid large opaque backgrounds.

## Aura Selection Plan

Change `HeldWeaponAuraBracket` so it no longer imports vanilla `ParticleTypes`.

Current:

```java
public ParticleOptions particle(RandomSource random)
```

MVP 5 target:

```java
public SimpleParticleType particle(RandomSource random)
```

Concrete `HeldWeaponAuraBracket` target:

```java
package org.brokkr.enhancement.client;

import java.util.Optional;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.brokkr.enhancement.EnhancementData;
import org.brokkr.enhancement.particle.ModParticles;

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

    public SimpleParticleType particle(RandomSource random) {
        return switch (this) {
            case FIRST -> ModParticles.AURA_SPARK.get();
            case SECOND -> random.nextInt(4) == 0 ? ModParticles.AURA_SPARK.get() : ModParticles.AURA_EMBER.get();
            case THIRD -> random.nextInt(5) < 3 ? ModParticles.AURA_RUNE.get() : ModParticles.AURA_EMBER.get();
            case FOURTH -> random.nextInt(20) < 11 ? ModParticles.AURA_ENERGY_STREAK.get() : ModParticles.AURA_EMBER.get();
        };
    }
}
```

Particle selection:

```text
FIRST
- AURA_SPARK

SECOND
- 75% AURA_EMBER
- 25% AURA_SPARK

THIRD
- 60% AURA_RUNE
- 40% AURA_EMBER

FOURTH
- 55% AURA_ENERGY_STREAK
- 45% AURA_EMBER
```

Keep count/intervals unchanged:

```text
FIRST   +1..+4,   interval 10, count 1
SECOND  +5..+9,   interval 8,  count 1-2
THIRD   +10..+14, interval 6,  count 2
FOURTH  +15..+20, interval 5,  count 2-3
```

Max flourish:

```text
+20 every 24 ticks -> AURA_BURST
```

Update `HeldWeaponAuraEffects`:

- Replace `ParticleTypes.END_ROD` with `ModParticles.AURA_BURST.get()`.
- Keep `spawnParticle(ClientLevel, ParticleOptions, Vec3, RandomSource)` signature because `SimpleParticleType` is a `ParticleOptions`.
- Use the existing random origin/velocity helper for energy streak particles in MVP 5.

## File-Level Changes

### `Brokkr.java`

Add:

```java
import org.brokkr.enhancement.particle.ModParticles;
```

Register:

```java
ModParticles.PARTICLE_TYPES.register(modEventBus);
```

### `BrokkrClientEvents.java`

Add:

```java
@SubscribeEvent
public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
    BrokkrAuraParticleProviders.register(event);
}
```

Concrete target shape:

```java
package org.brokkr.enhancement.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.brokkr.Brokkr;
import org.brokkr.enhancement.client.particle.BrokkrAuraParticleProviders;
import org.brokkr.enhancement.menu.ModMenus;

@EventBusSubscriber(modid = Brokkr.MODID, value = Dist.CLIENT)
public final class BrokkrClientEvents {
    private BrokkrClientEvents() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ENHANCEMENT_MENU.get(), EnhancementScreen::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        BrokkrAuraParticleProviders.register(event);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.addListener(BrokkrClientEvents::clientTick);
    }

    private static void clientTick(ClientTickEvent.Post event) {
        HeldWeaponAuraEffects.tick(Minecraft.getInstance());
    }
}
```

### `HeldWeaponAuraBracket.java`

Change particle selection from vanilla particles to `ModParticles`.

Expected imports:

```java
import net.minecraft.core.particles.SimpleParticleType;
import org.brokkr.enhancement.particle.ModParticles;
```

Remove:

```java
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
```

### `HeldWeaponAuraEffects.java`

Change max flourish:

```java
spawnParticle(level, ModParticles.AURA_BURST.get(), auraOrigin(player, RANDOM), RANDOM);
```

Add import:

```java
import org.brokkr.enhancement.particle.ModParticles;
```

### `ModParticles.java`

New common-side particle registry class.

### `client/particle/*`

New client-only particle implementation and provider registration classes.

### Resources

Add PNGs and JSON particle definitions.

## Implementation Phases

### Phase 1: Register Particle Types

- Add `ModParticles`.
- Register `PARTICLE_TYPES` in `Brokkr`.
- Build.

Exit criteria:

- Compile succeeds.
- Server startup does not load client particle provider classes.

### Phase 2: Add Placeholder Particle JSON And PNG Assets

- Add particle JSON files.
- Generate/add PNG textures.
- Confirm files are under the exact resource paths listed in this document.

Exit criteria:

- Resource processing succeeds.
- Jar contains `assets/brokkr/particles/*.json`.
- Jar contains `assets/brokkr/textures/particle/*.png`.

### Phase 3: Add Client Particle Class And Providers

- Add `BrokkrAuraParticleProfile`.
- Add `BrokkrAuraParticle`.
- Add `BrokkrAuraParticleProviders`.
- Register providers from `BrokkrClientEvents`.
- Build.

Exit criteria:

- Compile succeeds.
- No client provider class is referenced from `Brokkr` or common event classes.

### Phase 4: Replace Held Aura Particle Selection

- Update `HeldWeaponAuraBracket.particle(...)`.
- Update max flourish in `HeldWeaponAuraEffects`.
- Keep interval/count/position logic unchanged.

Exit criteria:

- `+1` uses custom spark.
- `+5` uses custom ember/spark.
- `+10` uses custom rune/ember.
- `+15` uses custom energy streak/ember.
- `+20` uses custom burst.

### Phase 5: Build And Runtime Smoke

- Run clean build.
- Confirm jar copies to CurseForge test instance.
- Run server smoke to confirm common registration is safe.
- In client, test `+1`, `+5`, `+10`, `+15`, `+20`.

Exit criteria:

- Build succeeds.
- Server reaches `Done`.
- Client does not show missing particle texture errors.
- Aura particles render in game.

## Manual Verification

Build:

```text
./gradlew.bat clean build --no-daemon --no-configuration-cache
```

Jar content checks:

```text
jar tf build/libs/brokkr-1.0-SNAPSHOT.jar | findstr /i "aura_spark aura_ember aura_rune aura_energy_streak aura_burst ModParticles BrokkrAuraParticle"
```

Server smoke:

```text
./gradlew.bat runServer --no-daemon --no-configuration-cache
```

In-game checks:

- `+0`: no aura.
- `+1`: custom spark.
- `+5`: ember appears.
- `+10`: rune appears.
- `+15`: thin energy streak appears.
- `+20`: burst appears occasionally.
- Unsupported item: no aura.
- Hit particles still trigger on damage.
- Enhancement UI still opens and works.

## Risks And Mitigations

Missing texture:

- Ensure particle JSON names match registered particle IDs.
- Ensure PNGs are under `textures/particle`, not `textures/particles`.
- Check jar contents after build.

Client/server class loading:

- Keep `BrokkrAuraParticle*` under client package.
- Register particle types in common code only.
- Register particle providers only from client event subscriber.
- Run server smoke after implementation.

Visual quality:

- Initial generated sprites are accepted when they pass the image QA checklist.
- Keep particle implementation constants centralized in `BrokkrAuraParticleProfile`.

First-person clutter:

- Keep `ENERGY_STREAK` lifetime short.
- Keep fourth bracket count at 2-3 particles per interval.
- Tune scale down before increasing spawn count.

Performance:

- Keep existing MVP 4 intervals.
- Avoid per-tick spawning.
- Use small PNGs and short lifetimes.

## Definition Of Done

- MVP 5 spec is implemented.
- Custom particle types are registered.
- Custom particle providers are registered on client.
- Custom PNG particle textures exist.
- Particle JSON files exist and match registry IDs.
- Held aura no longer uses vanilla particles.
- `+15..+20` includes thin energy streak particles.
- `+20` includes custom burst particles.
- Existing hit particles remain unchanged.
- Existing enhancement UI remains unchanged.
- Clean build succeeds.
- Jar copies to configured CurseForge test instance.
- Server smoke reaches startup without client class loading errors.
