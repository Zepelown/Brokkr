# Weapon Enhancement MVP 6 Weapon Aura Rendering Spec

## Goal

MVP 6 adds a rendered weapon aura layer.

In Brokkr, **aura** means a visual layer of light wrapped around the held weapon itself. This is different from MVP 5 particles, which float around the hand and weapon area.

The player-facing goal:

- Weapons below `+10` keep MVP 5 ambient particles only.
- A `+10` or higher weapon gains a visible rendered aura attached to the weapon.
- `+10` starts as clean white light coming from the blade.
- `+15` changes the blade light to orange.
- `+20` adds rotating light that wraps around the weapon.
- Existing MVP 5 custom particles should remain as secondary ambient effects.

## Current Baseline

Current visual layers:

```text
Hit feedback:
  server-side particles around damaged target

MVP 5 ambient aura:
  client-side custom particles around hand/weapon area

Enhancement UI:
  custom GUI effects during enhancement process
```

MVP 6 adds a new layer:

```text
Rendered weapon aura:
  client-side overlay attached visually to the held weapon
```

Relevant files:

```text
src/main/java/org/brokkr/enhancement/client/HeldWeaponAuraEffects.java
src/main/java/org/brokkr/enhancement/client/HeldWeaponAuraBracket.java
src/main/java/org/brokkr/enhancement/client/particle/BrokkrAuraParticle.java
src/main/java/org/brokkr/enhancement/EnhancementData.java
src/main/java/org/brokkr/enhancement/profile/WeaponEnhancementProfiles.java
```

## MVP 6 Scope

Included:

- Add a client-side rendered aura layer for held enhanced weapons.
- Start rendered weapon aura at `+10`.
- Render aura attached to the held item in first-person.
- Keep third-person on the existing MVP 5 ambient particle aura for this MVP.
- Keep MVP 5 custom particles.
- Keep enhancement mechanics unchanged.
- Add white blade light from `+10`.
- Change the blade light to orange from `+15`.
- Add rotating weapon-wrap light at `+20`.

Excluded:

- Rendered weapon aura for `+1..+9`.
- Real dynamic lighting.
- Shader pipeline replacement.
- Custom weapon models.
- Per-weapon 3D model authoring.
- Full swing trail system.
- Rendered third-person weapon aura.
- Full-screen or HUD-positioned aura overlays.
- Server networking.
- Gameplay stat changes.

## Aura Definition

Use these terms consistently:

```text
Ambient aura:
  MVP 5 custom particles around the hand/weapon area.

Rendered weapon aura:
  MVP 6 translucent render layer visually attached to the weapon.
```

When this document says **aura**, it means **rendered weapon aura** unless it explicitly says ambient aura.

The rendered weapon aura should:

- Appear to wrap the weapon.
- Follow the held item render position.
- Preserve the original weapon appearance.
- Add blade-attached light without replacing the item texture.
- Change color and behavior by enhancement milestone.
- Never appear when the matching enhanced weapon is not being rendered.

## Bracket Design

Rendered weapon aura starts at `+10`.

Enhancement chance bracket boundaries still matter, but only the upper two brackets receive rendered weapon aura:

```text
+0  .. +9   no rendered weapon aura
+10 .. +14  white blade light
+15 .. +19  orange blade light
+20         orange blade light + rotating weapon-wrap light
```

Relationship to MVP 5 ambient particles:

```text
+1  .. +4   ambient particles only
+5  .. +9   ambient particles only
+10 .. +14  ambient particles + white blade light
+15 .. +19  ambient particles + orange blade light
+20         ambient particles + orange blade light + rotating weapon-wrap light
```

Visual progression:

```text
+0..+9
- No rendered weapon aura.
- MVP 5 particles may still appear from +1 upward.

+10..+14
- First visible rendered weapon aura.
- Thin clean white light on the blade.
- Low-to-moderate alpha.
- No rotation effect yet.

+15..+19
- White light is replaced by orange light.
- Orange light is stronger than the +10 white light.
- Thin blade/edge line is always present while held.
- No rotating weapon-wrap light yet.

+20
- Keeps the orange blade light.
- Adds a rotating light wrap around the weapon.
- Rotation should read as light orbiting or coiling around the blade, not as a full-screen effect.
```

## Visual Direction

The desired effect is not a particle cloud.

The weapon should look like light is wrapped around it:

- A thin translucent aura following the weapon sprite/model silhouette.
- A clean white blade light at `+10..+14`.
- A stronger orange blade light at `+15..+19`.
- A rotating orange/white wrap around the weapon at `+20`.
- The `+20` rotation should feel attached to the weapon, like light coiling around the blade.

Avoid:

- Large flat rectangles around the weapon.
- Opaque overlays that hide the original weapon.
- Full-screen aura.
- HUD overlays that stay visible independently of the held weapon.
- Repeated tiled aura images on the screen.
- Excessive bloom-like visual noise.
- Effects that block the crosshair.

## Rendering Approach

MVP 6 uses an overlay rendering approach instead of replacing item rendering.

Implemented path:

```text
1. RenderHandEvent validates the first-person main-hand ItemStack.
2. Unsupported items, empty hand, and enhancement levels below +10 are skipped.
3. Valid enhanced weapons open a short-lived WeaponAuraRenderContext.
4. ItemStackRenderState$LayerRenderState.render(...) is injected with a client mixin.
5. Aura rendering runs after the vanilla item layer transform is applied.
6. ItemInHandRenderer clears stale aura context when hand rendering returns.
7. +10..+14 renders white blade light.
8. +15..+19 renders orange blade light.
9. +20 renders orange blade light plus rotating weapon-wrap light.
```

The implementation should not modify the weapon texture or item model asset.

Reason:

- The user does not want a texture pack or model replacement.
- The base weapon should remain recognizable.
- Aura should be an additive visual layer.

## First-Person Requirements

First-person is the priority because the player constantly sees their held weapon.

Requirements:

- Main-hand `+10` or higher supported weapon shows a rendered aura layer.
- `+1..+9` does not show the rendered weapon aura.
- Aura appears near the weapon sprite/model, not centered on the screen.
- Crosshair remains readable.
- Aura follows item movement enough to feel attached.
- Switching away from the weapon removes the aura immediately.

Target visual:

```text
+0..+9    no rendered weapon aura
+10..+14  white light on the blade
+15..+19  orange light on the blade
+20       orange light plus rotating wrap around the weapon
```

## Third-Person Scope

Third-person rendered weapon aura is deferred from MVP 6.

Current MVP 6 behavior:

- Other visible players keep MVP 5 ambient aura particles.
- No third-person rendered weapon aura is required for MVP 6 release.
- First-person rendered aura is the acceptance-critical visual.

Reason:

```text
NeoForge 1.21.6 RenderPlayerEvent uses PlayerRenderState.
The checked event API does not directly expose the original player ItemStack in the render event.
Implementing third-person rendered aura safely needs a separate render-layer or client cache design.
```

## Asset Plan

MVP 6 needs overlay textures, separate from particle textures.

Resource directory:

```text
src/main/resources/assets/brokkr/textures/effect/
```

Required assets:

```text
weapon_aura_white_blade.png
weapon_aura_orange_blade.png
weapon_aura_wrap_orbit.png
weapon_aura_wrap_core.png
```

Asset roles:

```text
weapon_aura_white_blade.png   clean white blade light for +10..+14
weapon_aura_orange_blade.png  stronger orange blade light for +15..+20
weapon_aura_wrap_orbit.png    rotating outer wrap line for +20
weapon_aura_wrap_core.png     rotating inner wrap highlight for +20
```

Recommended size:

```text
64x64 PNG RGBA
transparent background
white/orange visual core depending on asset
soft alpha edges
```

Asset constraints:

- Blade light assets should be thin and linear.
- Wrap assets should be curved or spiral-like enough to imply rotation.
- Assets must not look like rectangular panels.
- Assets must not be designed for HUD tiling.

## Technical Direction

The implementation should be client-only.

Implemented render path:

```text
RenderHandEvent
  validates the held main-hand item and opens WeaponAuraRenderContext

ItemStackRenderState$LayerRenderState.render(...) mixin
  renders the aura after the item layer transform is applied

ItemInHandRenderer.renderHandsWithItems(...) mixin
  clears stale aura context at method return
```

Reason:

```text
Direct RenderHandEvent quads were not stable enough because the held item transform had not run yet.
The mixin-backed item-layer hook lets the aura use item-local render space instead of HUD/screen space.
```

Implemented files:

```text
src/main/java/org/brokkr/enhancement/client/render/HeldWeaponAuraRenderer.java
src/main/java/org/brokkr/enhancement/client/render/WeaponAuraRenderContext.java
src/main/java/org/brokkr/enhancement/client/render/WeaponAuraRenderProfile.java
src/main/java/org/brokkr/enhancement/client/render/WeaponAuraRenderEvents.java
src/main/java/org/brokkr/enhancement/client/render/WeaponAuraRenderBracket.java
src/main/java/org/brokkr/mixin/client/ItemStackRenderStateLayerRenderStateMixin.java
src/main/java/org/brokkr/mixin/client/ItemInHandRendererMixin.java
src/main/resources/brokkr.mixins.json
```

Implemented resources:

```text
src/main/resources/assets/brokkr/textures/effect/weapon_aura_white_blade.png
src/main/resources/assets/brokkr/textures/effect/weapon_aura_orange_blade.png
src/main/resources/assets/brokkr/textures/effect/weapon_aura_wrap_orbit.png
src/main/resources/assets/brokkr/textures/effect/weapon_aura_wrap_core.png
```

Do not let rendering code change enhancement data.

## Render Behavior

When holding no item:

- No rendered weapon aura.

When holding unsupported item:

- No rendered weapon aura.

When holding supported weapon below `+10`:

- No rendered weapon aura.
- MVP 5 ambient particles may still appear for enhanced levels.

When holding supported `+10..+14` weapon:

- Render white blade light.

When holding supported `+15..+19` weapon:

- Render orange blade light.

When holding supported `+20` weapon:

- Render orange blade light.
- Render rotating wrap light around the weapon.

## Animation Requirements

Animation should be lightweight and time-based.

Allowed animation:

- Alpha pulse using game time or partial tick.
- Rotation around the weapon for `+20`.
- Phase-shifted wrap layers for `+20` to imply coiling light.

Avoid:

- Complex per-frame mesh allocation.
- Long history trail buffers.
- Full swing trail persistence.
- Screen-space fake aura unless it is proven to attach to the held item.

## Interaction With MVP 5 Particles

MVP 6 should not remove MVP 5 particles.

Layer relationship:

```text
MVP 6 rendered weapon aura:
  attached visual identity on the weapon, starts at +10

MVP 5 ambient particles:
  ambient magical energy around hand/weapon, starts at +1

Hit particles:
  combat impact feedback around target
```

If visuals become too noisy:

- Reduce MVP 5 particle frequency for `+15..+20` only after in-game review.
- Do not remove MVP 5 particles in the first MVP 6 implementation.

## Acceptance Criteria

- A supported `+10` or higher main-hand weapon gets a visible rendered weapon aura.
- Supported `+1..+9` weapons do not get rendered weapon aura.
- `+0` and unsupported items do not get rendered weapon aura.
- `+10..+14` shows white blade light.
- `+15..+19` shows orange blade light.
- `+20` shows orange blade light plus rotating weapon-wrap light.
- Rendered aura is not visible when no supported enhanced weapon is held.
- Rendered aura does not repeat as multiple HUD images.
- Rendered weapon aura is first-person only in MVP 6.
- Base weapon appearance remains visible.
- Existing MVP 5 particles still appear.
- Existing hit particles still appear.
- Existing enhancement UI still works.
- Clean build succeeds.
- Jar copies to the configured CurseForge test instance.

## Manual Test Checklist

Build:

```text
./gradlew.bat clean build --no-daemon --no-configuration-cache
```

In-game:

- Hold `+0` sword and confirm no rendered weapon aura.
- Hold `+5` sword and confirm no rendered weapon aura.
- Hold `+10` sword and confirm white blade light appears.
- Hold `+15` sword and confirm orange blade light appears.
- Hold `+20` sword and confirm rotating wrap light appears around the weapon.
- Switch to unsupported item and confirm rendered weapon aura disappears.
- Switch to empty hand and confirm rendered weapon aura disappears.
- Check first-person crosshair readability.
- Check that third-person MVP 5 ambient particles still appear.
- Confirm MVP 5 particles still exist.
- Hit an entity and confirm hit particles still exist.

## Risks And Open Questions

Mixin stability:

- MVP 6 is tied to the checked Minecraft `1.21.6` / NeoForge `21.6.20-beta` render method shape.
- Future Minecraft/NeoForge upgrades may require rechecking mixin targets.

Position accuracy:

- Weapon-attached aura is harder than particle aura.
- MVP 6 should prioritize first-person correctness.
- HUD overlays and direct screen-space aura rendering must not be reused.

Visual occlusion:

- Overlay alpha must stay low enough to preserve weapon readability.

Compatibility:

- Custom rendering must stay client-only.
- Server must not load client render classes.

Performance:

- Render code runs every frame.
- Avoid expensive allocation in render paths.

## Future Extensions

- True swing trail.
- Per-weapon aura profiles.
- Separate axe/sword geometry hints.
- Configurable aura intensity.
- Optional shader-based bloom later.
