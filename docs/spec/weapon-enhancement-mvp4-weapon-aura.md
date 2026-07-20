# Weapon Enhancement MVP 4 Weapon Aura Spec

## Goal

MVP 4 adds a persistent aura effect for enhanced weapons.

The current Brokkr visual effect appears only when an enhanced weapon hits an entity. That hit effect should remain. MVP 4 adds a separate visual layer: when a player is holding an enhanced weapon, small particles should appear around the held weapon, hand, or forearm area so the weapon feels empowered even before combat starts.

The player-facing goal is simple:

- A normal weapon looks normal.
- An enhanced weapon feels visibly infused while held.
- Higher enhancement chance brackets make the held aura more noticeable.
- Only `+20` receives an additional max-level flourish beyond the normal bracket aura.
- Hit particles still provide impact feedback during combat.

## Current Baseline

Existing behavior:

- Enhancement level is stored on the item stack.
- Supported weapons currently include swords.
- Enhanced weapons gain attack damage through `ItemAttributeModifierEvent`.
- Hit particles are spawned from `LivingIncomingDamageEvent`.
- Hit particles are server-driven and appear around the damaged target.
- Current combat particles use `EnhancementTier`.
- MVP 4 aura tiers must align to the enhancement success chance brackets, not the existing five-level visual tier split.

Relevant current files:

```text
src/main/java/org/brokkr/enhancement/EnhancementData.java
src/main/java/org/brokkr/enhancement/EnhancementTier.java
src/main/java/org/brokkr/enhancement/EnhancementParticles.java
src/main/java/org/brokkr/enhancement/event/EnhancementCombatEvents.java
src/main/java/org/brokkr/enhancement/profile/WeaponEnhancementProfiles.java
```

## MVP 4 Scope

Included:

- Add held weapon aura particles for enhanced weapons.
- Keep existing hit particles unchanged.
- Aura should appear around the held weapon, hand, or forearm area.
- Aura should work for the local player in first-person and third-person where feasible.
- Aura should appear for other visible players holding enhanced weapons where feasible.
- Aura intensity and particle type should vary by enhancement chance bracket.
- `+20` should keep the final bracket aura and add one extra max-level flourish.
- Aura should be lightweight enough to run continuously.
- Aura should not require weapon texture replacement.
- Aura should not change enhancement mechanics.

Excluded:

- Custom weapon models.
- Custom armor/body rendering.
- Shader effects.
- Full trail rendering during swings.
- Per-weapon bespoke visuals.
- New gameplay stats.
- New config screen.
- New enhancement levels.
- New multiplayer protocol unless testing proves client-only detection is insufficient.

## Visual Direction

The aura should feel like energy leaking from a forged magical weapon.

Preferred style:

- Subtle at low levels.
- Clearly magical at mid/high levels.
- Striking but not screen-cluttering at `+20`.
- Concentrated around the weapon/hand, not around the entire player body.
- Movement should feel alive: slight swirl, drift, or rising sparks.

Avoid:

- Large clouds that hide the player model.
- Constant explosions.
- Particles around the feet.
- Effects that look like potion status particles.
- Effects that block first-person combat visibility.

## Aura Behavior

When the player holds no enhanced weapon:

- No held aura particles spawn.

When the player holds a supported weapon with level `+0`:

- No held aura particles spawn.

When the player holds a supported weapon with level `+1` to `+20`:

- Aura particles spawn periodically around the main-hand weapon area.
- Tier is determined by the current enhancement level.
- The effect updates automatically if the player changes weapons or the weapon level changes.

When the player switches away from the enhanced weapon:

- Aura stops immediately or within the next tick interval.

When the weapon is in offhand:

- MVP 4 should focus on main-hand first.
- Offhand support may be added later if the code path remains simple.

When the player is in inventory or enhancement UI:

- World aura behavior does not need to change.
- GUI process effects remain separate from held aura effects.

## Aura Bracket Design

Aura brackets must match the enhancement success chance brackets.

Current enhancement chance brackets:

```text
+1  .. +4   next-attempt chance source: 100% bracket
+5  .. +9   next-attempt chance source: 70% bracket
+10 .. +14  next-attempt chance source: 40% bracket
+15 .. +20  next-attempt chance source: 10% bracket
```

Important:

- `+20` does not create a fifth normal aura bracket.
- `+20` uses the `+15..+20` aura as its base.
- `+20` adds a separate max-level flourish on top of that base aura.
- The max-level flourish should be occasional and special, not a constant extra particle flood.

Recommended aura effects:

```text
+1  .. +4   subtle crit/spark glints, low frequency
+5  .. +9   enchant particles mixed with small glints
+10 .. +14  enchant/witch-like magical motes, moderate frequency
+15 .. +20  flame/crit embers, more visible but controlled
+20 extra   occasional end-rod/totem-like golden flourish
```

Target intensity:

```text
+1  .. +4   every 10 ticks, 1 particle burst
+5  .. +9   every 8 ticks, 1-2 particles
+10 .. +14  every 6 ticks, 2 particles
+15 .. +20  every 5 ticks, 2-3 particles
+20 extra   every 20-30 ticks, 1 special flourish particle
```

These values are starting points. Performance and visibility should be tested in-game before locking them.

## Particle Positioning

The aura should originate near the held item.

Priority order:

1. Main-hand weapon/forearm area if a reliable hand position can be computed.
2. Upper body side offset based on the player's facing direction.
3. Eye/body-relative fallback if hand-specific positioning is too unstable.

For MVP 4, an acceptable approximation is:

- Start from player position.
- Add a side offset based on main arm and view/body rotation.
- Add a forward offset toward the held weapon.
- Add a vertical offset around chest/hand height.
- Add small random jitter so particles do not stack in one point.

The particle should not spawn at the target entity. That is already covered by hit particles.

## First-Person And Third-Person Expectations

First-person:

- The local player should see a subtle aura near the held weapon area.
- Particles must not constantly cover the crosshair.
- Low and mid tiers should be restrained.

Third-person:

- The aura should appear near the hand/weapon side.
- Other players should be able to see the aura if particles are spawned in the world.

Multiplayer:

- If implemented client-side only, each client can render aura for visible players using their synced item stacks.
- If client-side visibility for other players is insufficient, fallback to server-side particle spawning with throttling.

## Technical Direction

Preferred MVP 4 implementation:

- Use a client tick event for local/world visual aura.
- Scan visible players in the client level.
- Check each player's main-hand item.
- Use `WeaponEnhancementProfiles.find(stack)` and `EnhancementData.getLevel(stack)`.
- Skip if unsupported or level `<= 0`.
- Determine the aura bracket from the same level boundaries used by the enhancement chance brackets.
- Spawn particles client-side near the player's main-hand side.

Why client-side first:

- Aura is visual-only.
- It avoids server particle spam.
- It avoids changing gameplay state.
- It should work in single-player and multiplayer clients for visible synced players.

Possible files:

```text
src/main/java/org/brokkr/enhancement/client/BrokkrClientEvents.java
src/main/java/org/brokkr/enhancement/client/HeldWeaponAuraEffects.java
src/main/java/org/brokkr/enhancement/EnhancementAuraParticles.java
```

Recommended aura bracket helper:

```text
HeldWeaponAuraBracket.fromLevel(level)
  +1..+4   -> FIRST
  +5..+9   -> SECOND
  +10..+14 -> THIRD
  +15..+20 -> FOURTH

HeldWeaponAuraBracket.isMaxLevel(level)
  +20 -> true
```

Do not reuse `EnhancementTier.fromLevel(level)` for held aura unless its ranges are changed to match chance brackets without breaking existing hit effects.

Keep existing hit effect files intact:

```text
src/main/java/org/brokkr/enhancement/EnhancementParticles.java
src/main/java/org/brokkr/enhancement/event/EnhancementCombatEvents.java
```

Do not mix hit particle logic and held aura logic too tightly. They have different triggers and performance constraints.

## Performance Requirements

Held aura runs continuously, so it must be throttled.

Requirements:

- Do not spawn particles every tick for every player.
- Limit by tier interval.
- Skip invisible, removed, dead, or spectator players where practical.
- Avoid expensive allocations in the hot path.
- Keep particle count low for low/mid tiers.
- Stop immediately when no enhanced weapon is held.

Recommended limits:

- Local world scan once per client tick is acceptable for MVP 4.
- Per-player particle spawn should happen only on bracket interval.
- A player should spawn at most 3 base aura particles per interval in the `+15..+20` bracket.
- `+20` max flourish should be separately throttled.

## Interaction With Existing Hit Particles

Existing hit particles remain:

- Trigger: successful incoming damage event caused by a player.
- Position: damaged target.
- Purpose: impact feedback.

New held aura:

- Trigger: enhanced weapon held in main hand.
- Position: weapon/hand/forearm area.
- Purpose: persistent identity and power feedback.

Both effects can appear during combat. The held aura should not replace hit particles.

## I18N Requirements

MVP 4 does not require new player-facing text.

If debug commands or config messages are added later, they must use the existing I18N pattern:

```text
src/main/java/org/brokkr/enhancement/text/EnhancementTextKeys.java
src/main/resources/assets/brokkr/lang/en_us.json
src/main/resources/assets/brokkr/lang/ko_kr.json
```

## Acceptance Criteria

- Existing hit particles still appear when enhanced weapons hit an entity.
- Holding a `+0` weapon shows no aura.
- Holding a `+1` or higher supported weapon shows aura particles.
- Aura appears near the held weapon/hand/forearm area, not at the feet or target.
- Aura changes by enhancement chance bracket.
- `+20` keeps the `+15..+20` aura and adds an extra max-level flourish.
- Switching away from an enhanced weapon stops the aura.
- Unsupported items do not show aura.
- The effect is visible in third-person.
- First-person visibility is noticeable but not obstructive.
- Clean build succeeds.
- Jar still copies to the configured CurseForge test instance.

## Manual Test Checklist

Build:

```text
./gradlew.bat clean build --no-daemon --no-configuration-cache
```

In-game:

- Give a sword and set it to `+0`.
- Confirm no aura appears.
- Set the sword to `+1`.
- Hold it in main hand and confirm subtle aura near the weapon/hand.
- Test `+1`, `+5`, `+10`, `+15`, and `+20`.
- Confirm aura changes exactly at the chance bracket boundaries.
- Confirm `+20` has the `+15..+20` base aura plus an extra flourish.
- Switch to an unenhanced item and confirm aura stops.
- Switch to an unsupported item and confirm aura does not appear.
- Hit an entity and confirm existing hit particles still appear.
- Test first-person and third-person.
- If possible, test another visible player holding an enhanced weapon.

## Risks And Open Questions

Hand position accuracy:

- Minecraft particle positioning around a held item can be approximate without custom renderer hooks.
- MVP 4 accepts a good player-relative approximation first.

First-person clutter:

- Particles near the camera can obstruct combat.
- Low/mid tiers should be subtle, and the spawn point should avoid the crosshair.

Multiplayer visibility:

- Client-only aura should work for visible players if item stack enhancement data is synced.
- If data sync is incomplete, implementation may need a lightweight server-driven fallback.

Particle spam:

- Continuous aura can become noisy.
- Bracket intervals and particle counts must be conservative.

## Future Extensions

- Config option to reduce or disable aura.
- Separate first-person and third-person intensity.
- Swing trail effect during attacks.
- Weapon-type-specific aura placement.
- Custom Brokkr particle textures.
- Aura color customization by enhancement tier.
