# Weapon Enhancement MVP 6 Weapon Aura Rendering Implementation Plan

## Purpose

This plan implements the MVP 6 weapon aura defined in
`docs/spec/weapon-enhancement-mvp6-weapon-aura-rendering.md`.

Target visual:

```text
+0  .. +9   no rendered weapon aura
+10 .. +14  white light from the blade
+15 .. +19  orange light from the blade
+20         orange blade light + rotating light wrapping around the weapon
```

MVP 5 custom aura particles remain as ambient secondary effects.

## Review Result

The previous implementation plan was not concrete enough because it still treated the render hook as an open research problem.

Local NeoForge/Minecraft 1.21.6 source review found:

```text
RegisterRenderStateModifiersEvent
  supports entity and map render states, not item stack render layers.
  Not suitable as the primary weapon aura hook.

RegisterSpecialModelRendererEvent
  registers codecs for special item model renderers.
  Useful for custom item model JSON, but too invasive for vanilla weapon overlays.

RenderHandEvent
  fires before vanilla first-person hand/item rendering.
  provides the active ItemStack.
  good for capturing "which held item should get aura".
  bad as the direct draw location because item layer transforms have not run yet.

ItemStackRenderState.LayerRenderState.render(...)
  applies the item layer transform before rendering quads/special renderer.
  this is the first practical point where aura can inherit the item transform.
```

Conclusion:

```text
Use RenderHandEvent to open a short-lived first-person aura render context.
Use a mixin injection in ItemStackRenderState$LayerRenderState.render(...)
after the item layer transform is applied to render the aura.
Do not render aura directly from RenderHandEvent.
Do not use HUD or GUI render events.
```

This is the implementation path for MVP 6.

## Current Baseline

Working systems:

```text
Held aura particles:
  BrokkrClientEvents.clientTick(...)
    -> HeldWeaponAuraEffects.tick(Minecraft)

Hit feedback:
  server-side particles around damaged target

Enhancement UI:
  Brokkr enhancement menu and process screen
```

Current safe state:

```text
BrokkrClientEvents.clientSetup(...)
  registers only client tick aura particles
```

The render aura listener is intentionally not registered until the corrected implementation is ready.

## Rejected Approaches

Do not use:

```text
RenderGuiEvent
HUD blits
screen-space images
tiled full-screen overlays
large camera/world billboards
direct RenderHandEvent aura drawing
```

Reasons:

- HUD rendering caused repeated aura images.
- Direct `RenderHandEvent` quads appeared detached from the sword.
- The requested aura must belong to the weapon, not the screen.

## Chosen Render Architecture

Use a two-part client-only architecture.

```text
RenderHandEvent
  -> validates held main-hand item
  -> reads enhancement level
  -> creates a short-lived WeaponAuraRenderContext

ItemStackRenderState$LayerRenderState.render(...) mixin
  -> runs while vanilla item rendering is active
  -> injects after this.transform.apply(...)
  -> renders Brokkr aura using the already transformed pose stack
  -> marks the context consumed

ItemInHandRenderer.renderHandsWithItems(...) mixin
  -> clears stale context at method return
```

Why this is implementable:

- `RenderHandEvent` gives the correct `ItemStack`.
- `ItemStackRenderState.LayerRenderState.render(...)` already has the item display transform applied.
- The aura renderer can draw in item-local render space instead of guessing screen-space offsets.
- A cleanup injection prevents stale context from leaking to later renders.

## Mixin Setup

Add mixin configuration:

```text
src/main/resources/brokkr.mixins.json
```

Expected content shape:

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "org.brokkr.mixin.client",
  "compatibilityLevel": "JAVA_21",
  "client": [
    "ItemStackRenderStateLayerRenderStateMixin",
    "ItemInHandRendererMixin"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```

Update mod metadata template:

```text
src/main/templates/META-INF/neoforge.mods.toml
```

Add:

```toml
[[mixins]]
config = "brokkr.mixins.json"
```

Implementation rule:

- Mixins must be client-only.
- Mixin package must not be referenced by common gameplay code.

## Aura Context Design

Create:

```text
src/main/java/org/brokkr/enhancement/client/render/WeaponAuraRenderContext.java
```

Responsibilities:

- Store the currently expected first-person item stack reference.
- Store the current aura profile.
- Store the current first-person hand display context.
- Prevent more than one aura render per held item render.
- Clear itself after hand rendering completes.

Fields:

```text
ItemStack stack
WeaponAuraRenderProfile profile
ItemDisplayContext displayContext
boolean consumed
long gameTime
float partialTick
```

API:

```java
public static void begin(
        ItemStack stack,
        WeaponAuraRenderProfile profile,
        ItemDisplayContext displayContext,
        long gameTime,
        float partialTick
)

public static Optional<ActiveAura> consumeFor(ItemDisplayContext displayContext)

public static void clear()
```

Consumption rules:

- Return empty if no context exists.
- Return empty if already consumed.
- Return empty if display context is not first-person.
- Return empty if display context does not match the stored context.
- Mark consumed before returning the aura data.

Reason:

- The mixin does not need to rediscover enhancement state.
- It only draws if a matching first-person hand render opened the context.
- Empty hand and unsupported item cannot create a context.

## Event Filtering

Revise:

```text
src/main/java/org/brokkr/enhancement/client/render/WeaponAuraRenderEvents.java
```

Register this listener from `BrokkrClientEvents.clientSetup(...)` only after mixin path is implemented.

Filtering:

```text
client player exists
hand is MAIN_HAND
stack is not empty
WeaponEnhancementProfiles.find(stack) is present
EnhancementData.getLevel(stack) is at least 10
player is not using an item with non-NONE use animation
```

Event action:

```text
Do not draw.
Do not cancel the event.
Only call WeaponAuraRenderContext.begin(...).
```

Display context:

```text
player main arm RIGHT -> ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
player main arm LEFT  -> ItemDisplayContext.FIRST_PERSON_LEFT_HAND
```

This keeps the vanilla item render path intact.

## Mixin Injection Details

### `ItemStackRenderStateLayerRenderStateMixin`

Target:

```text
net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState
```

Method:

```text
render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay)
```

Injection point:

```text
after ItemTransform.apply(...)
before specialRenderer/renderItem branch
```

Reason:

- The pose stack has the item layer transform applied.
- Drawing here lets the aura follow first-person item orientation.
- Drawing before vanilla item quads keeps the weapon readable over the glow.

Target behavior:

```java
@Inject(
    method = "render",
    at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/block/model/ItemTransform;apply(ZLcom/mojang/blaze3d/vertex/PoseStack$Pose;)V",
        shift = At.Shift.AFTER
    )
)
private void brokkr$renderWeaponAura(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay,
        CallbackInfo ci
) {
    WeaponAuraRenderContext.consumeForCurrentFirstPersonContext()
            .ifPresent(aura -> HeldWeaponAuraRenderer.renderItemAttachedAura(
                    poseStack,
                    bufferSource,
                    aura.profile(),
                    aura.gameTime(),
                    aura.partialTick()
            ));
}
```

Exact method/descriptor must be verified by compilation.

### `ItemInHandRendererMixin`

Target:

```text
net.minecraft.client.renderer.ItemInHandRenderer
```

Method:

```text
renderHandsWithItems(...)
```

Injection point:

```text
RETURN
```

Action:

```java
WeaponAuraRenderContext.clear();
```

Reason:

- If the item render path changes or no layer consumes the context, stale aura data must not leak to the next render.

## Render Bracket Design

Revise:

```text
src/main/java/org/brokkr/enhancement/client/render/WeaponAuraRenderBracket.java
```

Target enum:

```java
public enum WeaponAuraRenderBracket {
    WHITE_BLADE,
    ORANGE_BLADE,
    ORANGE_WRAP;

    public static Optional<WeaponAuraRenderBracket> fromLevel(int level) {
        int clampedLevel = EnhancementData.clampLevel(level);
        if (clampedLevel < 10) {
            return Optional.empty();
        }
        if (clampedLevel <= 14) {
            return Optional.of(WHITE_BLADE);
        }
        if (clampedLevel <= 19) {
            return Optional.of(ORANGE_BLADE);
        }
        return Optional.of(ORANGE_WRAP);
    }
}
```

Acceptance:

```text
fromLevel(9)  -> empty
fromLevel(10) -> WHITE_BLADE
fromLevel(14) -> WHITE_BLADE
fromLevel(15) -> ORANGE_BLADE
fromLevel(19) -> ORANGE_BLADE
fromLevel(20) -> ORANGE_WRAP
```

## Render Profile Design

Revise:

```text
src/main/java/org/brokkr/enhancement/client/render/WeaponAuraRenderProfile.java
```

Profile data:

```text
blade texture
optional wrap orbit texture
optional wrap core texture
red/green/blue
blade alpha
wrap alpha
blade scale
wrap scale
wrap rotation speed
```

Initial values:

```text
WHITE_BLADE
  blade: weapon_aura_white_blade.png
  color: 245, 250, 255
  blade alpha: 95
  blade scale: 0.78
  wrap disabled

ORANGE_BLADE
  blade: weapon_aura_orange_blade.png
  color: 255, 145, 40
  blade alpha: 120
  blade scale: 0.82
  wrap disabled

ORANGE_WRAP
  blade: weapon_aura_orange_blade.png
  wrap orbit: weapon_aura_wrap_orbit.png
  wrap core: weapon_aura_wrap_core.png
  color: 255, 160, 52
  blade alpha: 135
  wrap alpha: 135
  blade scale: 0.84
  wrap scale: 0.95
  wrap rotation speed: moderate
```

## Renderer Design

Revise:

```text
src/main/java/org/brokkr/enhancement/client/render/HeldWeaponAuraRenderer.java
```

Public render method:

```java
public static void renderItemAttachedAura(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        WeaponAuraRenderProfile profile,
        long gameTime,
        float partialTick
)
```

Rendering rules:

- Draw in item-local space after item transform has been applied.
- Draw before vanilla item quads.
- Use small quads with transparent textures.
- Use full-bright light only for aura vertices.
- Do not mutate global render state.
- Do not allocate persistent objects in the frame path.

Texture paths:

```text
textures/effect/weapon_aura_white_blade.png
textures/effect/weapon_aura_orange_blade.png
textures/effect/weapon_aura_wrap_orbit.png
textures/effect/weapon_aura_wrap_core.png
```

Initial item-local placement:

```text
blade quad:
  translate: 0.00, 0.00, -0.015
  rotate Z: -45 degrees
  width: 0.22
  height: 0.72

wrap quad:
  translate: 0.00, 0.00, -0.012
  rotate Z: time based
  width: 0.44
  height: 0.82
```

Tuning rule:

- Tune item-local quad size and offset only after confirming the mixin hook follows the sword.
- Do not reintroduce screen-space offsets.

## Asset Plan

Required resources:

```text
src/main/resources/assets/brokkr/textures/effect/weapon_aura_white_blade.png
src/main/resources/assets/brokkr/textures/effect/weapon_aura_orange_blade.png
src/main/resources/assets/brokkr/textures/effect/weapon_aura_wrap_orbit.png
src/main/resources/assets/brokkr/textures/effect/weapon_aura_wrap_core.png
```

Replace old prototype textures if unused:

```text
weapon_aura_soft.png
weapon_aura_edge.png
weapon_aura_streak.png
weapon_aura_pulse.png
```

Asset constraints:

- PNG RGBA.
- Transparent background.
- Fully transparent corners.
- Thin blade light shapes.
- Wrap textures must be centered and non-rectangular.

Validation:

```text
dimensions are 64x64 or 128x128
corner alpha is 0
visible pixels do not fill the whole square
```

## File-Level Changes

Add or revise:

```text
src/main/java/org/brokkr/enhancement/client/render/WeaponAuraRenderBracket.java
src/main/java/org/brokkr/enhancement/client/render/WeaponAuraRenderProfile.java
src/main/java/org/brokkr/enhancement/client/render/WeaponAuraRenderContext.java
src/main/java/org/brokkr/enhancement/client/render/WeaponAuraRenderEvents.java
src/main/java/org/brokkr/enhancement/client/render/HeldWeaponAuraRenderer.java
src/main/java/org/brokkr/mixin/client/ItemStackRenderStateLayerRenderStateMixin.java
src/main/java/org/brokkr/mixin/client/ItemInHandRendererMixin.java
src/main/resources/brokkr.mixins.json
src/main/templates/META-INF/neoforge.mods.toml
```

Resource changes:

```text
src/main/resources/assets/brokkr/textures/effect/weapon_aura_white_blade.png
src/main/resources/assets/brokkr/textures/effect/weapon_aura_orange_blade.png
src/main/resources/assets/brokkr/textures/effect/weapon_aura_wrap_orbit.png
src/main/resources/assets/brokkr/textures/effect/weapon_aura_wrap_core.png
```

Client registration:

```java
NeoForge.EVENT_BUS.addListener(WeaponAuraRenderEvents::renderFirstPersonHand);
```

Register only from `BrokkrClientEvents`, which is already client-only.

## Implementation Phases

### Phase 1: Mixin And Context Skeleton

Tasks:

- Add `brokkr.mixins.json`.
- Add `[[mixins]]` entry to `neoforge.mods.toml`.
- Add `WeaponAuraRenderContext`.
- Add both client mixin classes.
- Make the mixin compile without rendering any aura yet.

Exit criteria:

- Clean build succeeds.
- Client starts.
- Server starts or at least compiles without client class loading errors.

### Phase 2: Minimal Attachment Proof

Tasks:

- Register `WeaponAuraRenderEvents`.
- Open aura context only for supported main-hand weapons at `+10` or higher.
- Render a single thin white test blade line from the `LayerRenderState` mixin.
- Do not implement orange or wrap yet.

Exit criteria:

- `+10` test marker follows the sword while idle.
- `+10` test marker follows normal swing.
- Empty hand shows no marker.
- Unsupported item shows no marker.
- `+0..+9` shows no marker.
- No repeated HUD images appear.

If this phase fails:

- Remove listener registration.
- Keep mixin disabled or no-op.
- Do not continue to full aura visuals.

### Phase 3: Brackets, Profiles, And Assets

Tasks:

- Replace prototype bracket names with final names.
- Generate final four aura textures.
- Update renderer texture constants.
- Validate texture alpha.

Exit criteria:

- Required textures exist.
- Old prototype textures are removed or no longer referenced.
- Bracket boundaries match the spec.
- Build succeeds.

### Phase 4: +10 And +15 Blade Aura

Tasks:

- Render white blade texture for `WHITE_BLADE`.
- Render orange blade texture for `ORANGE_BLADE` and `ORANGE_WRAP`.
- Tune alpha and size.

Exit criteria:

- `+10..+14` shows white blade light.
- `+15..+19` shows orange blade light.
- `+20` shows orange blade light but no wrap yet in this phase.
- Weapon remains readable.

### Phase 5: +20 Rotating Wrap

Tasks:

- Render wrap orbit/core only for `ORANGE_WRAP`.
- Use `gameTime + partialTick` for rotation.
- Keep wrap centered around item-local blade space.

Exit criteria:

- `+20` shows rotating wrap.
- `+15..+19` does not show wrap.
- Wrap does not cover the crosshair.
- Wrap disappears immediately when switching items.

### Phase 6: Build And Runtime Verification

Commands:

```text
./gradlew.bat clean build --no-daemon --no-configuration-cache
jar tf build/libs/brokkr-1.0-SNAPSHOT.jar | findstr /i "WeaponAura weapon_aura brokkr.mixins"
```

Expected deployment copy:

```text
C:\Users\Yoon\curseforge\minecraft\Instances\test\mods\brokkr-1.0-SNAPSHOT.jar
```

Exit criteria:

- Build succeeds.
- Jar contains mixin config, mixin classes, render classes, and textures.
- Jar is copied to the configured CurseForge test instance.
- Client loads without mixin errors.
- No server-side client class loading crash.

## Manual Verification Checklist

In-game:

```text
+0 sword:
  no rendered aura

+5 sword:
  no rendered aura
  MVP 5 ambient particles may still appear

+10 sword:
  white blade light

+14 sword:
  white blade light

+15 sword:
  orange blade light

+19 sword:
  orange blade light

+20 sword:
  orange blade light plus rotating wrap

empty hand:
  no rendered aura

unsupported item:
  no rendered aura

normal swing:
  aura follows weapon

item switch:
  aura disappears immediately
```

Regression checks:

- No repeated screen-space aura images.
- No aura when weapon is not held.
- No HUD overlay.
- Weapon remains readable.
- Crosshair remains readable.
- MVP 5 particles still work.
- Enhancement UI still works.
- Hit particles still work.

## Risks And Mitigations

### Mixin Fragility

Risk:

- The injected method descriptor or invocation target can change with Minecraft/NeoForge versions.

Mitigation:

- Keep mixin targets narrow.
- Require clean build before runtime testing.
- Document this as MC `1.21.6` / NeoForge `21.6.20-beta` specific.

### Wrong Render Context Consumption

Risk:

- The mixin could consume aura context in another item render path.

Mitigation:

- Context is opened only by `RenderHandEvent`.
- Context checks first-person display context.
- Context consumes only once.
- `ItemInHandRendererMixin` clears at return.

### Visual Overdraw

Risk:

- Aura texture can hide the sword.

Mitigation:

- Render before vanilla item quads.
- Keep alpha low.
- Use thin transparent textures.

### Client/Server Boundary

Risk:

- Server crashes if client classes are loaded from common code.

Mitigation:

- Event registration stays in `BrokkrClientEvents`.
- Mixins are listed under `client` in `brokkr.mixins.json`.
- No common gameplay class imports mixin/render classes.

## Definition Of Done

- The implementation uses the mixin-backed item-attached render path.
- `+10..+14` shows white blade light.
- `+15..+19` shows orange blade light.
- `+20` shows orange blade light plus rotating wrap.
- No rendered aura appears below `+10`.
- No rendered aura appears for empty hand or unsupported items.
- Aura follows the held weapon instead of the screen.
- No repeated HUD images appear.
- MVP 5 ambient particles still work.
- Enhancement UI still works.
- Hit particles still work.
- Clean build succeeds.
- Jar is copied to the CurseForge test instance.
- Client loads without mixin or render errors.
