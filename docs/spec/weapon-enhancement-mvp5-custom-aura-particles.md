# Weapon Enhancement MVP 5 Custom Aura Particles Spec

## Goal

MVP 5 replaces the MVP 4 held weapon aura's Minecraft built-in particles with Brokkr-specific custom particles and custom particle textures.

The player-facing goal:

- Enhanced weapons should feel like Brokkr-forged magical weapons.
- Aura visuals should use custom sprites, not only vanilla particles.
- Aura brackets should continue to match the current-level enhancement chance brackets.
- `+15` and above should gain a thin energy-streak effect around the held weapon area.
- `+20` should keep the final aura bracket and add a special max-level burst.
- Existing hit particles and enhancement UI effects should remain unchanged.

## Current Baseline

MVP 4 added client-side held weapon aura particles.

Current behavior:

- Aura runs on client tick.
- Aura scans visible players in the client level.
- Aura checks the main-hand item.
- Aura uses `WeaponEnhancementProfiles.find(stack)` and `EnhancementData.getLevel(stack)`.
- Aura bracket mapping is aligned to `EnhancementChance.successChanceForCurrentLevel(currentLevel)`.
- Current aura uses vanilla particle types.

Relevant files:

```text
src/main/java/org/brokkr/enhancement/client/BrokkrClientEvents.java
src/main/java/org/brokkr/enhancement/client/HeldWeaponAuraEffects.java
src/main/java/org/brokkr/enhancement/client/HeldWeaponAuraBracket.java
src/main/java/org/brokkr/enhancement/EnhancementChance.java
src/main/java/org/brokkr/enhancement/EnhancementData.java
src/main/java/org/brokkr/enhancement/profile/WeaponEnhancementProfiles.java
```

## MVP 5 Scope

Included:

- Add Brokkr custom particle types for held weapon aura.
- Add custom particle texture PNG assets.
- Add particle JSON definitions.
- Register particle providers on the client.
- Replace vanilla held aura particles with custom aura particles.
- Keep current aura bracket boundaries:

```text
+1  .. +4   FIRST   100% next-attempt chance bracket
+5  .. +9   SECOND  70% next-attempt chance bracket
+10 .. +14  THIRD   40% next-attempt chance bracket
+15 .. +20  FOURTH  10% next-attempt chance bracket
+20         FOURTH base aura + max-level burst
```

- Add a thin energy-streak particle for `+15..+20`.
- Add a stronger or more noticeable special burst for `+20`.
- Keep aura lightweight enough for continuous client-side use.

Excluded:

- Shader effects.
- Dynamic light emission.
- Custom weapon models.
- Full weapon-bound renderer overlays.
- True blade-attached trails.
- Swing trail rendering.
- Offhand aura support.
- Config UI for particle intensity.
- Server-driven aura packets unless MVP 5 testing proves client-only data is insufficient.

## Visual Direction

The custom aura should feel like forged energy leaking from a weapon.

Theme:

- Brokkr forge identity.
- Gold, orange, ember, and hot-metal tones.
- Small rune or gem-like magical accents.
- Thin energy streaks at high enhancement levels.

Avoid:

- Large screen-filling sprites.
- Dense smoke.
- Potion-like bubbles.
- Purple-dominant visuals.
- Effects that hide the weapon or crosshair.
- Heavy particles every tick.

## Custom Particle Asset Plan

All particle images should be transparent PNG files under:

```text
src/main/resources/assets/brokkr/textures/particle/
```

Recommended texture size:

```text
16x16 or 32x32 PNG
transparent background
pixel-art compatible
soft alpha edges
```

Required MVP 5 assets:

```text
aura_spark.png
aura_ember.png
aura_rune.png
aura_energy_streak.png
aura_burst.png
```

Asset roles:

```text
aura_spark.png          small pale gold/white forged spark
aura_ember.png          orange/gold ember mote
aura_rune.png           compact arcane rune shard, cyan-gold or warm-gold
aura_energy_streak.png  thin elongated gold/orange energy slash
aura_burst.png          short-lived max-level pulse or starburst
```

Particle definition files:

```text
src/main/resources/assets/brokkr/particles/aura_spark.json
src/main/resources/assets/brokkr/particles/aura_ember.json
src/main/resources/assets/brokkr/particles/aura_rune.json
src/main/resources/assets/brokkr/particles/aura_energy_streak.json
src/main/resources/assets/brokkr/particles/aura_burst.json
```

Each JSON should point to the matching texture.

## Aura Bracket Visual Design

Aura brackets are based on the current enhancement level because enhancement chance is calculated from the current level before the next attempt.

```text
+1  .. +4   FIRST
+5  .. +9   SECOND
+10 .. +14  THIRD
+15 .. +20  FOURTH
```

Bracket behavior:

```text
FIRST
- Uses aura_spark.
- Low frequency.
- 1 small particle per interval.
- Subtle white/gold spark.

SECOND
- Uses aura_ember, with occasional aura_spark.
- Slightly higher frequency.
- 1-2 particles per interval.
- Orange/gold forged ember feel.

THIRD
- Uses aura_rune and aura_ember.
- Moderate frequency.
- 2 particles per interval.
- More magical and deliberate.

FOURTH
- Uses aura_ember and aura_energy_streak.
- Higher frequency, still controlled.
- 2-3 particles per interval.
- Thin energy streak becomes part of the base aura.

+20 extra
- Keeps FOURTH behavior.
- Adds aura_burst on a separate throttle.
- Burst should be special and occasional.
```

Target intensity:

```text
+1  .. +4   every 10 ticks, 1 particle
+5  .. +9   every 8 ticks,  1-2 particles
+10 .. +14  every 6 ticks,  2 particles
+15 .. +20  every 5 ticks,  2-3 particles, includes energy streaks
+20 extra   every 24 ticks, 1 burst particle
```

## Thin Energy Streak Requirement

From `+15` onward, the aura must include a thin energy-streak effect.

Implementation target:

- Use `aura_energy_streak.png`.
- Render as a custom particle sprite with elongated shape.
- Spawn near the main-hand weapon area.
- Use short lifetime.
- Use alpha fade-out.
- Use random rotation.
- Use small upward or side drift.
- Keep count low to avoid screen clutter.

The effect is a particle-based approximation, not a blade-attached renderer.

Acceptance for MVP 5:

- At `+15`, the player can clearly notice a thin energy-streak visual that was not present in `+10..+14`.
- The streak appears around the held weapon or hand area.
- The streak does not constantly cover the crosshair.
- The streak is visible in third-person.

## Technical Direction

Add a particle registration layer:

```text
src/main/java/org/brokkr/enhancement/particle/ModParticles.java
```

Add client particle classes:

```text
src/main/java/org/brokkr/enhancement/client/particle/BrokkrAuraParticle.java
src/main/java/org/brokkr/enhancement/client/particle/BrokkrAuraParticleProvider.java
```

Alternative if separate classes become clearer:

```text
BrokkrAuraParticle
BrokkrEnergyStreakParticle
BrokkrBurstParticle
```

Preferred first implementation:

- Use one particle class with factory/provider variants.
- Use particle type to choose sprite.
- Use code-side size, lifetime, alpha, rotation, and motion behavior.

The existing `HeldWeaponAuraEffects` should continue to own:

- player scanning
- enhanced weapon detection
- position calculation
- spawn throttling
- bracket selection

The new particle classes should own:

- visual lifetime
- alpha fade
- sprite selection
- rotation behavior
- particle size

## Registration Requirements

Register custom particle types through a deferred register.

Expected registry owner:

```text
ModParticles
```

Expected particle type IDs:

```text
brokkr:aura_spark
brokkr:aura_ember
brokkr:aura_rune
brokkr:aura_energy_streak
brokkr:aura_burst
```

Client provider registration should happen from client setup/client event code.

`Brokkr` mod constructor must register particle deferred registers on the mod event bus.

Client-only particle provider classes must not be referenced from server-only code paths.

## Behavior Requirements

When holding no enhanced weapon:

- No custom aura particles spawn.

When holding unsupported items:

- No custom aura particles spawn.

When holding a supported `+0` weapon:

- No custom aura particles spawn.

When holding `+1..+4`:

- Spark aura appears.

When holding `+5..+9`:

- Ember aura appears.

When holding `+10..+14`:

- Rune/ember aura appears.

When holding `+15..+20`:

- Ember aura continues.
- Thin energy-streak particles appear as part of the base aura.

When holding `+20`:

- `+15..+20` base aura continues.
- Burst particle appears on separate throttle.

## Interaction With MVP 4

MVP 5 should replace the held aura's vanilla particles.

Do not remove:

- `HeldWeaponAuraEffects`
- `HeldWeaponAuraBracket`
- existing hit particles
- enhancement UI process effects

Expected refactor:

- `HeldWeaponAuraBracket` should return Brokkr particle selections instead of vanilla particle selections, or delegate to a new particle selection helper.
- `HeldWeaponAuraEffects` should call `ClientLevel.addParticle(...)` with Brokkr custom particle types.

## Performance Requirements

Custom aura particles run continuously, so they must stay conservative.

Requirements:

- No per-tick particle spam.
- Keep existing bracket intervals.
- Keep max base count at 3 particles per interval.
- Keep `+20` burst separately throttled.
- Particle lifetime should be short enough to avoid buildup.
- Texture size should remain small.

Recommended lifetimes:

```text
aura_spark          8-12 ticks
aura_ember          12-18 ticks
aura_rune           14-20 ticks
aura_energy_streak  8-12 ticks
aura_burst          10-16 ticks
```

## I18N Requirements

MVP 5 does not require new player-facing text.

If debug commands or config messages are added later, use the existing I18N files:

```text
src/main/resources/assets/brokkr/lang/en_us.json
src/main/resources/assets/brokkr/lang/ko_kr.json
```

## Acceptance Criteria

- Custom particle assets exist under `assets/brokkr/textures/particle/`.
- Particle JSON files exist under `assets/brokkr/particles/`.
- Custom particle types are registered.
- Custom particle providers are registered on the client.
- Holding `+1` or higher supported weapons shows Brokkr custom aura particles.
- Aura bracket changes at `+5`, `+10`, and `+15`.
- `+15..+20` includes a visible thin energy-streak effect.
- `+20` includes an additional burst effect.
- Existing hit particles still work.
- Existing enhancement UI still works.
- Unsupported and unenhanced items do not spawn aura particles.
- Clean build succeeds.
- Jar copies to the configured CurseForge test instance.

## Manual Test Checklist

Build:

```text
./gradlew.bat clean build --no-daemon --no-configuration-cache
```

In-game:

- Set a sword to `+0` and confirm no aura.
- Set a sword to `+1` and confirm spark aura.
- Set a sword to `+5` and confirm aura changes to ember.
- Set a sword to `+10` and confirm aura changes to rune/ember.
- Set a sword to `+15` and confirm thin energy streaks appear.
- Set a sword to `+20` and confirm burst appears occasionally.
- Switch to unsupported item and confirm aura stops.
- Hit an entity and confirm existing hit particles still appear.
- Check first-person visibility.
- Check third-person visibility.

## Risks And Open Questions

Particle registration API:

- Must be verified against local NeoForge 1.21.6 APIs before implementation.

Visual clarity:

- Generated sprites may need iteration after in-game screenshots.

Energy streak positioning:

- Particle-based streaks approximate the held weapon area.
- Exact blade-attached streaks require a future custom renderer.

First-person clutter:

- `+15..+20` streaks must remain thin and short-lived.

Multiplayer visibility:

- Client-side particle spawning should show effects for visible players if item stack enhancement data is synced.
- If other-player aura does not show, local-player aura remains MVP 5 acceptance and server packet support becomes a follow-up.

## Future Extensions

- Dedicated first-person renderer for blade-attached aura.
- Swing trail effect.
- Configurable particle intensity.
- Per-weapon aura variants.
- Custom color themes by enhancement material.
- Server-driven aura sync for multiplayer consistency.
