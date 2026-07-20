# Weapon Enhancement MVP 4 Weapon Aura Implementation Plan

## Purpose

This document turns `docs/spec/weapon-enhancement-mvp4-weapon-aura.md` into an implementation-ready plan.

MVP 4 adds a persistent held-weapon aura for enhanced weapons while preserving the existing hit particles.

Primary implementation goals:

- Keep existing hit particles unchanged.
- Add client-side aura particles around the main-hand weapon/hand area.
- Match aura brackets to enhancement success chance brackets.
- Add only one extra flourish for `+20`.
- Keep the effect lightweight enough to run continuously.
- Avoid custom particle textures for this MVP.

## Current Code Baseline

Relevant existing files:

```text
src/main/java/org/brokkr/Brokkr.java
src/main/java/org/brokkr/enhancement/EnhancementData.java
src/main/java/org/brokkr/enhancement/EnhancementChance.java
src/main/java/org/brokkr/enhancement/EnhancementTier.java
src/main/java/org/brokkr/enhancement/EnhancementParticles.java
src/main/java/org/brokkr/enhancement/event/EnhancementCombatEvents.java
src/main/java/org/brokkr/enhancement/profile/EnhancedWeaponProfile.java
src/main/java/org/brokkr/enhancement/profile/WeaponEnhancementProfiles.java
src/main/java/org/brokkr/enhancement/client/BrokkrClientEvents.java
```

Current hit particle flow:

```text
LivingIncomingDamageEvent
  -> EnhancementCombatEvents.spawnHitParticles(...)
  -> player main-hand item
  -> WeaponEnhancementProfiles.find(stack)
  -> EnhancementData.getLevel(stack)
  -> profile.tier(level)
  -> EnhancementParticles.spawn(serverLevel, target, tier)
```

Current limitations:

- Particles appear only on hit.
- Particles spawn around the damaged target.
- There is no visual aura while simply holding an enhanced weapon.

## Implementation Strategy

Use client-side world particles first.

Reasons:

- Aura is visual-only.
- No gameplay state needs to change.
- It avoids server particle spam.
- Visible players should have synced main-hand item stacks on the client.
- It keeps multiplayer protocol unchanged for MVP 4.

Do not modify:

```text
EnhancementCombatEvents.spawnHitParticles(...)
EnhancementParticles.spawn(...)
EnhancementAttemptService
EnhancementData storage format
WeaponEnhancementProfiles
```

Add new aura-specific code:

```text
src/main/java/org/brokkr/enhancement/client/HeldWeaponAuraBracket.java
src/main/java/org/brokkr/enhancement/client/HeldWeaponAuraEffects.java
```

Wire it from:

```text
src/main/java/org/brokkr/enhancement/client/BrokkrClientEvents.java
```

## API Verification Notes

The following APIs were checked against the local NeoForge/Minecraft 1.21.6 sources:

```text
net.neoforged.neoforge.client.event.ClientTickEvent
  ClientTickEvent.Pre
  ClientTickEvent.Post

net.minecraft.client.multiplayer.ClientLevel
  List<AbstractClientPlayer> players()
  void addParticle(ParticleOptions, double, double, double, double, double, double)

net.minecraft.world.entity.player.Player
  ItemStack getMainHandItem()
  boolean isSpectator()
  HumanoidArm getMainArm()

net.minecraft.world.entity.Entity / LivingEntity
  boolean isRemoved()
  boolean isAlive()
  boolean isInvisible()
  float getBbHeight()
  float getYRot()
  Vec3 getLookAngle()
```

This means the implementation can avoid reflection, networking, or custom renderer hooks for MVP 4.

## Readiness Review

Reviewed implementation blockers and resolutions:

- Client tick API exists as `ClientTickEvent.Post`.
- Client player iteration exists as `ClientLevel.players()`.
- Client particle spawning exists as `ClientLevel.addParticle(...)`.
- Main-hand item and main arm access exist on `Player`.
- No custom packet, custom particle registration, or renderer hook is required for MVP 4.
- Remaining validation is in-game visual tuning, not API discovery.

## Event Hook Plan

Use the confirmed NeoForge client tick event.

Hook location:

```text
BrokkrClientEvents
```

Implementation shape:

```java
import net.minecraft.client.Minecraft;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@SubscribeEvent
public static void clientSetup(FMLClientSetupEvent event) {
    NeoForge.EVENT_BUS.addListener(BrokkrClientEvents::clientTick);
}

private static void clientTick(ClientTickEvent.Post event) {
    HeldWeaponAuraEffects.tick(Minecraft.getInstance());
}
```

Confirmed event type:

```text
net.neoforged.neoforge.client.event.ClientTickEvent.Post
```

Implementation caution:

- `RegisterMenuScreensEvent` is already handled from `BrokkrClientEvents`.
- `EventBusSubscriber` in NeoForge 21.6.20-beta exposes only `modid` and `value`; it does not expose a `bus` attribute.
- Use `FMLClientSetupEvent` from the client subscriber to register `ClientTickEvent.Post` on `NeoForge.EVENT_BUS`.
- Keep the actual tick method private and thin.

## Aura Bracket Plan

Do not reuse `EnhancementTier.fromLevel(level)` for held aura.

Reason:

- Existing `EnhancementTier` currently maps visual tiers as `+1..+5`, `+6..+10`, `+11..+15`, `+16..+19`, `+20`.
- MVP 4 aura must match the current-level enhancement chance brackets: `+1..+4`, `+5..+9`, `+10..+14`, `+15..+20`.
- `+20` is not its own base bracket. It receives an additional flourish only.

Add enum:

```text
HeldWeaponAuraBracket
  FIRST   +1..+4
  SECOND  +5..+9
  THIRD   +10..+14
  FOURTH  +15..+20
```

Concrete API:

```java
public enum HeldWeaponAuraBracket {
    FIRST(10, 1, 1),
    SECOND(8, 1, 2),
    THIRD(6, 2, 2),
    FOURTH(5, 2, 3);

    public static Optional<HeldWeaponAuraBracket> fromLevel(int level)
    public static boolean isMaxLevel(int level)
    public int intervalTicks()
    public int particleCount(RandomSource random)
    public ParticleOptions particle(RandomSource random)
}
```

Concrete bracket behavior:

```text
FIRST   +1..+4,   interval 10, count 1, particles CRIT
SECOND  +5..+9,   interval 8,  count 1-2, particles ENCHANT + CRIT
THIRD   +10..+14, interval 6,  count 2, particles ENCHANT + WITCH
FOURTH  +15..+20, interval 5,  count 2-3, particles FLAME + CRIT
+20 flourish interval 24, count 1, particle END_ROD
```

Keep particle choices in code, not resources, because MVP 4 uses Minecraft built-in particles only.

Required imports:

```java
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
```

## Aura Tick Flow

`HeldWeaponAuraEffects.tick(Minecraft client)` should:

1. Return if `client.level == null`.
2. Return if `client.player == null`.
3. Increment a local tick counter.
4. Iterate visible players in `client.level.players()`.
5. Skip invalid players:
   - removed
   - dead
   - spectator
   - invisible
6. Read `player.getMainHandItem()`.
7. Resolve profile with `WeaponEnhancementProfiles.find(stack)`.
8. Skip unsupported or empty stack.
9. Read `EnhancementData.getLevel(stack)`.
10. Resolve `HeldWeaponAuraBracket.fromLevel(level)`.
11. Skip level `<= 0`.
12. If the current tick is not aligned to the bracket interval, skip.
13. Compute hand/weapon-adjacent particle position.
14. Spawn base aura particles client-side.
15. If `level >= EnhancementData.MAX_LEVEL`, separately throttle and spawn max flourish.

Concrete loop type:

```java
for (AbstractClientPlayer player : client.level.players()) {
    // validate and spawn aura
}
```

Required imports:

```java
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.ItemStack;
```

## Positioning Plan

MVP 4 should approximate the main-hand weapon position without custom render hooks.

Use player-relative world positioning:

```text
base = player.position()
vertical = player.getBbHeight() * 0.62
forward = player's look/body direction * 0.35
side = right/left vector * 0.35 based on main arm
jitter = small random x/y/z offset
```

Target:

- Around hand/forearm/held weapon area.
- Not at feet.
- Not at target entity.
- Not directly on the crosshair in first-person.

Implementation detail:

- Use yaw/body rotation for side direction when available.
- Use `player.getMainArm()` to choose left/right side.
- Keep the position helper isolated so it can be tuned after screenshots.

Recommended helper methods:

```java
private static Vec3 auraOrigin(AbstractClientPlayer player, RandomSource random)
private static Vec3 horizontalForward(AbstractClientPlayer player)
private static Vec3 handSide(AbstractClientPlayer player, Vec3 forward)
private static double jitter(RandomSource random, double radius)
```

Concrete first-pass formula:

```java
private static final double HAND_SIDE_OFFSET = 0.34D;
private static final double HAND_FORWARD_OFFSET = 0.42D;
private static final double HAND_HEIGHT_FACTOR = 0.58D;
private static final double JITTER_RADIUS = 0.08D;

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
```

Required import:

```java
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
```

## Client Particle Spawn Plan

Use built-in particles with `client.level.addParticle(...)`.

Confirmed shape:

```java
clientLevel.addParticle(
    particle,
    x,
    y,
    z,
    velocityX,
    velocityY,
    velocityZ
);
```

Velocity:

- Low levels: tiny upward drift.
- Mid/high: small swirl or side drift.
- Max flourish: slightly stronger upward motion.

Keep velocity small:

```text
velocity x/z: -0.015 .. 0.015
velocity y:    0.01  .. 0.04
```

Recommended helper:

```java
private static void spawnParticle(ClientLevel level, ParticleOptions particle, Vec3 origin, RandomSource random) {
    double vx = jitter(random, 0.015D);
    double vy = 0.01D + random.nextDouble() * 0.03D;
    double vz = jitter(random, 0.015D);
    level.addParticle(particle, origin.x, origin.y, origin.z, vx, vy, vz);
}
```

## Max-Level Flourish Plan

`+20` gets:

- Normal `FOURTH` bracket aura.
- Additional flourish every 24 ticks.

Flourish particle:

```text
END_ROD
```

First implementation:

- Spawn one `END_ROD` particle around the hand every 24 ticks.

Avoid:

- Constant max-only spam.

## File-Level Changes

### `HeldWeaponAuraBracket.java`

Add under:

```text
src/main/java/org/brokkr/enhancement/client/HeldWeaponAuraBracket.java
```

Responsibilities:

- Map enhancement level to chance-aligned aura bracket.
- Own base interval and count ranges.
- Expose max-level flourish check.

Required methods:

```java
static Optional<HeldWeaponAuraBracket> fromLevel(int level)
static boolean isMaxLevel(int level)
int intervalTicks()
int minParticles()
int maxParticles()
```

### `HeldWeaponAuraEffects.java`

Add under:

```text
src/main/java/org/brokkr/enhancement/client/HeldWeaponAuraEffects.java
```

Responsibilities:

- Client tick entry point.
- Player scan.
- Main-hand enhanced weapon detection.
- Position computation.
- Particle type selection.
- Particle spawning.
- Throttling.

Keep all runtime state private:

```java
private static final int TICK_COUNTER_CYCLE = 1_200;
private static int tickCounter;
private static final RandomSource RANDOM = RandomSource.create();
```

No server state, no persistent data, no NBT changes.

Required imports:

```java
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.brokkr.enhancement.EnhancementData;
import org.brokkr.enhancement.profile.WeaponEnhancementProfiles;
```

### `BrokkrClientEvents.java`

Update to register client tick handling.

Existing role:

- Register menu screen.

New role:

- Delegate client tick to `HeldWeaponAuraEffects.tick(...)`.

Keep it thin:

```java
public static void clientTick(ClientTickEvent.Post event) {
    HeldWeaponAuraEffects.tick(Minecraft.getInstance());
}
```

### Existing hit particle files

No changes planned:

```text
EnhancementParticles.java
EnhancementCombatEvents.java
```

If shared helper logic emerges, avoid refactoring during MVP 4 unless necessary.

## Implementation Phases

### Phase 1: Wire Confirmed Client Tick API

- Import `net.neoforged.neoforge.client.event.ClientTickEvent`.
- Add `BrokkrClientEvents.onClientTick(ClientTickEvent.Post event)`.
- Call `HeldWeaponAuraEffects.tick(Minecraft.getInstance())`.
- Add `HeldWeaponAuraEffects` with a no-op `tick(Minecraft client)` first, then fill it in.

Exit criteria:

- Build succeeds with the client tick hook.
- No client class is referenced from common/server-only event code.

### Phase 2: Add Bracket Enum

- Add `HeldWeaponAuraBracket`.
- Implement level mapping:
  - `+1..+4` -> `FIRST`
  - `+5..+9` -> `SECOND`
  - `+10..+14` -> `THIRD`
  - `+15..+20` -> `FOURTH`
- Implement `isMaxLevel(level)`.

Exit criteria:

- Mapping is simple and isolated.
- No existing `EnhancementTier` behavior changes.

### Phase 3: Add Aura Tick Scanner

- Add `HeldWeaponAuraEffects.tick(Minecraft client)`.
- Return safely when client/level/player are null.
- Iterate client level players.
- Skip unsupported or unenhanced weapons.
- Throttle by bracket interval.
- Use `AbstractClientPlayer` from `client.level.players()`.
- Use `player.isRemoved()`, `player.isAlive()`, `player.isSpectator()`, and `player.isInvisible()` as skip checks.
- Do not add debug particles in this phase; scanner correctness is verified by build and by the bracket particle phase.

Exit criteria:

- Scanner compiles against `ClientLevel.players()`.
- No particles spawn before Phase 5.
- Build succeeds.

### Phase 4: Add Positioning

- Add hand-side aura origin helper.
- Use player-relative offsets.
- Add small jitter.
- Keep constants near top of `HeldWeaponAuraEffects`.

Exit criteria:

- Particles spawn near upper body/hand area, not feet.

### Phase 5: Add Bracket Particles

- Map each bracket to Minecraft built-in particles.
- Spawn count by bracket.
- Add light velocities.

Exit criteria:

- `+1`, `+5`, `+10`, `+15` each show distinct intensity or particle type.

### Phase 6: Add `+20` Flourish

- Add separate max-level throttle.
- Spawn one special particle around hand/weapon.

Exit criteria:

- `+20` keeps `FOURTH` aura.
- `+20` has an extra occasional flourish.
- `+15..+19` does not get the flourish.

### Phase 7: Regression Check

- Confirm existing hit particles still trigger on damage.
- Confirm enhancement UI still builds.
- Confirm no server startup/client class loading issue.
- Confirm the generated jar contains no new resources for aura because MVP 4 uses built-in particles only.

Exit criteria:

- Clean build succeeds.
- Jar copies to test instance.
- Existing hit particles still work in game.

## Manual Test Plan

Build:

```text
./gradlew.bat clean build --no-daemon --no-configuration-cache
```

In-game setup:

```text
/brokkr
/enhanceweapon set 0
/enhanceweapon set 1
/enhanceweapon set 6
/enhanceweapon set 11
/enhanceweapon set 16
/enhanceweapon set 20
```

Functional checks:

- `+0` sword shows no aura.
- `+1` sword shows subtle aura.
- `+5` changes aura bracket.
- `+10` changes aura bracket.
- `+15` changes aura bracket.
- `+20` shows `+15..+20` aura plus extra flourish.
- Switching to empty hand stops aura.
- Switching to unsupported item stops aura.
- Existing hit particles still appear when hitting a target.

Visual checks:

- First-person aura is visible but does not block the crosshair.
- Third-person aura appears near the weapon/hand side.
- Aura does not spawn at the feet.
- Aura does not spawn on the hit target unless hit particles are triggered.
- `+20` flourish is noticeable but not noisy.

Performance checks:

- Standing still with enhanced weapon does not flood the screen.
- Multiple visible players should not cause extreme particle spam.
- FPS remains acceptable in a normal test world.

## Risks And Mitigations

Client tick event wiring:

- Use `ClientTickEvent.Post`; this API exists in the local NeoForge 21.6.20-beta sources.
- Keep event hook separate from aura implementation so event-bus routing changes do not affect particle logic.
- If the combined client subscriber does not receive client tick events in runtime testing, move only the tick subscription into a dedicated client subscriber class and keep `HeldWeaponAuraEffects` unchanged.

Hand position approximation:

- Use constants that are easy to tune.
- MVP 4 uses the concrete hand-side formula documented above.
- Screenshot tuning may change only the offset constants, not the event flow or bracket mapping.

First-person obstruction:

- Keep local player particle count conservative.
- Use hand-side offset and upward drift instead of center-screen spawn.

Particle spam:

- Use interval throttling.
- Cap max base particles at 3 per interval.
- Throttle `+20` flourish separately.

Multiplayer data sync:

- Client-side scan relies on visible player item stacks including custom data.
- MVP 4 acceptance requires the local player's held enhanced weapon aura.
- Other-player aura is best-effort for this MVP because it depends on client-visible item stack data.
- If local-player aura works but other-player aura does not, keep MVP 4 complete and create a follow-up task for server-broadcast aura packets.

## Concrete Implementation Skeleton

`HeldWeaponAuraBracket`:

```java
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
        if (clampedLevel <= 0) {
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
```

`HeldWeaponAuraEffects` core flow:

```java
package org.brokkr.enhancement.client;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.brokkr.enhancement.EnhancementData;
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
            if (WeaponEnhancementProfiles.find(stack).isEmpty()) {
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
            if (HeldWeaponAuraBracket.isMaxLevel(enhancementLevel)
                    && tickCounter % MAX_FLOURISH_INTERVAL_TICKS == 0) {
                spawnParticle(level, ParticleTypes.END_ROD, auraOrigin(player, RANDOM), RANDOM);
            }
        }
    }

    private static boolean shouldSkip(AbstractClientPlayer player) {
        return player.isRemoved() || !player.isAlive() || player.isSpectator() || player.isInvisible();
    }
}
```

## Definition Of Done

- MVP 4 implementation follows chance-aligned aura brackets.
- `+20` has only an additional flourish, not a fifth base bracket.
- Existing hit particles are preserved.
- Holding enhanced weapons creates aura near hand/weapon.
- Holding unenhanced or unsupported items creates no aura.
- Clean build succeeds.
- Jar copies to the configured CurseForge test instance.
