# Weapon Enhancement MVP 2 Spec

## Goal

MVP 2 turns the command-based enhancement loop into a dedicated in-game enhancement experience.

The existing Brokkr enhancement core remains the source of truth:

- Enhancement level: `+0` to `+20`
- Success chance: `100%`, `70%`, `40%`, `10%`
- Failure result: stone consumed, weapon level unchanged
- Damage bonus: `+0.2` attack damage per level
- Supported weapon profile design

MVP 2 adds two user-facing layers:

- A suspenseful enhancement process with anvil sounds and repeated feedback.
- A dedicated enhancement interface UI.
- A same-screen PROCESSING overlay with image-based effects.

## Implementation Status

This spec reflects the current implemented MVP 2 baseline.

Implemented:

- `/brokkr` opens the Brokkr enhancement UI.
- The UI uses one weapon slot and one dedicated Enhancement Stone slot.
- UI attempts consume only the stone slot, not the player's general inventory.
- Enhancement logic is shared through `EnhancementAttemptService`.
- Server result authority is preserved.
- Result metadata is synchronized to the client with menu data.
- Client PROCESSING animation hides the result until reveal time.
- Screen-space rune, spark, flash, smoke, success, and failure images render in `EnhancementScreen`.
- `forge_overlay.png` exists as an asset but is not rendered in the current effect stack because the large yellow outer ring was visually disruptive.
- Build copies the jar to the CurseForge test instance `mods` folder after `build`.

Not yet fully verified in-game:

- All click/shift-click/close duplication paths during PROCESSING.
- Final visual tuning across multiple GUI scale settings.

## Core Direction

Use one menu/screen and switch states inside it.

Do not open a second screen for the process animation.

State flow:

```text
READY -> PROCESSING -> RESULT -> READY
```

The PROCESSING state should visually replace the normal enhancement UI, but it remains the same `EnhancementScreen` and menu. This keeps slots, item return, server sync, and duplication safety manageable.

The current implementation renders the PROCESSING overlay in `EnhancementScreen` after the normal menu render. It uses the full screen center as the effect center so the rune, flash, and particles align with each other.

## Player Experience

The player opens a Brokkr enhancement interface, places a weapon and an Enhancement Stone, checks the current level and success chance, then presses an enhance button.

After pressing the button, the UI enters an enhancement process state:

- The result is not shown immediately.
- Anvil sounds play several times.
- Visual status text changes during the process.
- The player waits briefly while the attempt resolves.
- The final result is revealed as success or failure.

The purpose is to make enhancement feel deliberate and tense, not like an instant command.

## MVP 2 Scope

Included:

- Dedicated enhancement UI.
- One supported weapon slot.
- One dedicated Enhancement Stone slot.
- Current level display.
- Next level display.
- Success chance display.
- Attack damage bonus preview.
- Enhance button.
- Enhancement process state.
- Multiple anvil sounds during the process.
- Same-screen PROCESSING overlay.
- Image-based GUI effects for processing and result reveal.
- Strong success effect.
- Strong failure effect.
- Final success/failure feedback in the UI and chat.
- Reuse of existing enhancement chance and result logic.

Excluded:

- Full custom UI skin/theme texture.
- New enhancement block model.
- JEI/REI integration.
- Recipe or loot acquisition changes.
- Protection charms.
- Downgrade/destruction failure penalties.
- Advanced animations beyond vanilla UI drawing and sound timing.
- Multiplayer party broadcast effects.

Included image assets are limited to effect overlays such as sparks, smoke, flashes, and runes. A full custom UI frame/background skin is deferred.

The previously generated forge aura asset remains packaged but is not part of the active rendering stack.

## Entry Point

MVP 2 can use a command to open the UI first:

```text
/brokkr
```

Rules:

- Opens the Brokkr enhancement screen for the player.
- Does not attempt enhancement by itself.
- Does not require operator permission.

Reason:

- A command-opened UI avoids adding a block, block entity, menu provider block item, recipe, and placement flow before the enhancement interface itself is validated.

Future entry point:

- Add a Brokkr Anvil or Enhancement Table block.

## UI Layout

Required UI elements:

```text
[ Weapon Slot ]    [ Enhancement Stone Slot ]

Current Level: +N
Next Level: +N+1
Success Chance: X%
Attack Bonus: +A -> +B

[ Enhance ]

Status / Result Text
```

Slot behavior:

- Weapon slot accepts only registered supported weapons.
- Enhancement Stone slot accepts only `brokkr:enhancement_stone`.
- Enhancement Stones must be placed in this dedicated slot.
- Enhancement Stones in the player's inventory do not count for UI enhancement attempts unless they are moved into the Enhancement Stone slot.
- The Enhance button is enabled only when:
  - weapon slot has a supported weapon
  - Enhancement Stone slot has at least one Enhancement Stone
  - weapon level is below `+20`
  - no enhancement process is currently running

Max-level behavior:

- If weapon is `+20`, show max-level status.
- Disable the Enhance button.
- Do not consume stones.

## Enhancement Process

When the player presses Enhance:

1. Client sends the enhance request.
2. Server validates and resolves the enhancement attempt immediately.
3. Server updates menu data with the authoritative result.
4. Client detects the changed attempt sequence and enters `PROCESSING`.
5. Inputs are locked.
6. Enhance button is disabled.
7. Anvil sound sequence starts.
8. Status text updates through several process steps.
9. Success or failure effect plays at reveal time.
10. Result is shown.
11. Client sends/reset-triggers unlock through the menu button path after the process finishes.
12. UI returns to `READY` state.

Recommended timing:

```text
0 ticks:  start processing, dark overlay fades in
5 ticks:  anvil sound 1, small shake, spark burst
15 ticks: anvil sound 2, stronger shake, spark burst
25 ticks: anvil sound 3, flash pulse, rune glow
34 ticks: result reveal
50 ticks: allow returning to READY
```

At 20 TPS this is roughly 2.5 seconds total.

Recommended status text:

```text
Preparing...
Heating the edge...
Striking the weapon...
Reading the result...
```

The exact text must be I18N-managed.

## Result Effects

Success and failure must feel impactful.

The result reveal is the emotional peak of the enhancement process. It should not be a quiet text-only outcome.

Success effect requirements:

- Play a strong positive sound.
- Render bright screen-space particles/effect images.
- Show success status text in a high-emphasis color.
- Display the updated weapon level at reveal time.
- The effect should feel rewarding and sharp.

Recommended success effects:

```text
SoundEvents.ANVIL_LAND
SoundEvents.PLAYER_LEVELUP or another available level-up/positive sound
ParticleTypes.HAPPY_VILLAGER
ParticleTypes.END_ROD
ParticleTypes.TOTEM_OF_UNDYING
```

Failure effect requirements:

- Play a distinct negative sound.
- Render dull or smoke-like screen-space particles/effect images.
- Show failure status text in a warning color.
- Keep the weapon level unchanged.
- The effect should feel tense and disappointing, but not like item destruction.

Recommended failure effects:

```text
SoundEvents.ANVIL_LAND
SoundEvents.ITEM_BREAK
ParticleTypes.SMOKE
ParticleTypes.LARGE_SMOKE or the nearest available smoke particle
ParticleTypes.ANGRY_VILLAGER
```

Effect rules:

- Success and failure effects are mandatory for MVP 2.
- Effects should be visible/audible to the player using the UI.
- Effects are screen-space GUI effects in MVP 2.
- World-position result effects for nearby players are deferred.
- Do not hide the result behind only chat text.
- The effect should trigger after the anvil sound sequence, not before the result is known.
- GUI effects should render inside the PROCESSING/RESULT overlay, not as a separate screen.

## GUI Image Resources

Add GUI effect assets under:

```text
src/main/resources/assets/brokkr/textures/gui/enhancement/
```

Required or placeholder-ready PNG assets:

```text
forge_overlay.png
rune_circle.png
rune_success.png
rune_failure.png
spark.png
smoke.png
hammer_flash.png
result_flash.png
```

Active rendered assets:

```text
rune_circle.png
rune_success.png
rune_failure.png
spark.png
smoke.png
hammer_flash.png
result_flash.png
```

Packaged but currently not rendered:

```text
forge_overlay.png
```

Asset rules:

- Use transparent PNGs.
- Keep assets small enough for GUI rendering.
- Do not require a full texture pack.
- Effects should work with vanilla UI scale.
- Generated PNGs are currently used. Placeholder PNGs are acceptable only as a fallback if generated art is replaced or missing.

## GUI Effect Rendering

The PROCESSING and RESULT states should render image-based effects inside `EnhancementScreen`.

Required processing effects:

- Darkened background overlay.
- Rune circle at the full screen center.
- Hammer flash on each anvil hit.
- Sparks from the center on each strike.
- Small screen shake on each strike.
- Status text below the effect.

Required success effects:

- Bright flash.
- Success rune or gold glow.
- Spark burst.
- Optional end-rod/totem-like visual particles.
- Success result text.

Required failure effects:

- Dull red/gray flash.
- Failure rune or cracked mark.
- Smoke burst.
- Slight shake.
- Failure result text.

GUI effect particles are screen-space effects, not world particles.

Current active render order:

```text
1. Dark translucent full-screen overlay
2. Centered rune_circle.png
3. Timed hammer_flash.png
4. Spark or smoke GUI particles
5. result_flash.png at reveal
6. rune_success.png or rune_failure.png at reveal
7. Status/result text
```

`forge_overlay.png` is intentionally excluded from the current render order.

## Sound Design

Use vanilla sounds first.

Required process sounds:

- Anvil use sound during process ticks.

Recommended sounds:

```text
SoundEvents.ANVIL_USE
SoundEvents.ANVIL_LAND
SoundEvents.EXPERIENCE_ORB_PICKUP
SoundEvents.ITEM_BREAK
```

Sound rules:

- Play repeated anvil sounds during processing.
- Play a brighter/success sound on success.
- Play a dull/failure sound on failure.
- Sounds should play for the player using the UI.
- Avoid world-global loud sounds in MVP 2.

## Server Authority

The server must own the actual enhancement result.

Client responsibilities:

- Render UI.
- Send enhance button click request.
- Display process animation.
- Display server-provided result.

Server responsibilities:

- Validate weapon slot.
- Validate stone slot.
- Validate max level.
- Consume one Enhancement Stone from the dedicated Enhancement Stone slot for a valid attempt.
- Roll enhancement chance.
- Update weapon level on success.
- Return result to client.

The client must not decide success/failure locally.

Current sync contract:

```text
DATA_STATE
DATA_RESULT_TYPE
DATA_PREVIOUS_LEVEL
DATA_NEW_LEVEL
DATA_SUCCESS_CHANCE
DATA_ATTEMPT_SEQUENCE
```

`DATA_ATTEMPT_SEQUENCE` increments only for accepted success/failure roll attempts. The client starts PROCESSING when that value changes.

Current menu button ids:

```text
BUTTON_ENHANCE = 0
BUTTON_RESET = 1
```

`BUTTON_RESET` unlocks the server menu after the client-side PROCESSING timeline completes.

## State Model

Suggested UI states:

```text
EMPTY
READY
PROCESSING
SUCCESS
FAILURE
MAX_LEVEL
INVALID
```

Suggested process model:

```text
EnhancementProcessState
- state
- startedAtTick
- currentStep
- pendingResult
- lastResultLevelBefore
- lastResultLevelAfter
- screenShakeTicks
- flashAlpha
- sparkParticles
- smokeParticles
```

Suggested process steps:

```text
PREPARE
HAMMER_1
HAMMER_2
HAMMER_3
REVEAL
```

Current implementation uses the suggested UI states and keeps the process timer inside `EnhancementScreen`.

## Reuse Existing Core

MVP 2 should not duplicate enhancement rules.

Reuse:

- `EnhancementData`
- `EnhancementChance`
- `WeaponEnhancementService`
- `EnhancementAttemptService`
- `EnhancementResult`
- `WeaponEnhancementProfiles`
- `EnhancedWeaponProfile`

The existing `WeaponEnhancementService` has been split so command and UI adapters share the same slot-stack attempt logic:

```text
EnhancementAttemptService
- attempt(ItemStack weapon, ItemStack material, RandomSource random)
```

Then keep command and UI as separate adapters.

Command adapter:

- Finds weapon in main hand.
- Finds material in player inventory.
- Calls shared attempt service.

UI adapter:

- Finds weapon in menu slot.
- Finds material in the dedicated Enhancement Stone slot.
- Calls shared attempt service.

## Client/Server Result Timing

Server resolves immediately when the Enhance button request is accepted.

Flow:

1. Client sends enhance request.
2. Server validates weapon and stone slots.
3. Server consumes one Enhancement Stone for a valid attempt.
4. Server rolls and applies the result.
5. Client syncs the result through menu data.
6. Client plays the PROCESSING animation.
7. Client reveals the already-known authoritative result at reveal tick.
8. Client resets the server lock after the process finish tick.

The client may animate the suspense, but it must not decide success or failure.

Deferred server resolution at reveal time is not part of MVP 2.

## Interaction Locking

During PROCESSING:

- Enhance button is disabled.
- Slot clicks are rejected.
- Shift-click is rejected.
- Item pickup/movement in the menu is blocked.
- Closing the screen is allowed only if server-side item return remains safe.

Preferred MVP 2 behavior:

- Allow closing.
- Keep server state already resolved.
- Return slot contents through normal menu close handling.

Current implementation:

- `EnhancementMenu.clicked(...)` rejects slot clicks while locked.
- `EnhancementMenu.quickMoveStack(...)` returns empty while locked.
- `EnhancementMenu.removed(...)` returns temporary slot contents through vanilla clear-container handling.
- Closing during PROCESSING is allowed, but still needs manual in-game duplication testing.

Fallback:

- If closing during PROCESSING causes item safety issues, block Escape/close until result reveal.

## I18N Keys

All UI and process text must use language keys.

Required keys:

```text
screen.brokkr.enhancement.title
screen.brokkr.enhancement.current_level
screen.brokkr.enhancement.next_level
screen.brokkr.enhancement.success_chance
screen.brokkr.enhancement.attack_bonus
screen.brokkr.enhancement.button.enhance
screen.brokkr.enhancement.status.empty
screen.brokkr.enhancement.status.ready
screen.brokkr.enhancement.status.processing.prepare
screen.brokkr.enhancement.status.processing.hammer_1
screen.brokkr.enhancement.status.processing.hammer_2
screen.brokkr.enhancement.status.processing.hammer_3
screen.brokkr.enhancement.status.success
screen.brokkr.enhancement.status.failure
screen.brokkr.enhancement.status.max_level
screen.brokkr.enhancement.status.invalid_weapon
screen.brokkr.enhancement.status.no_stone
command.brokkr.open_ui.success
```

Korean keys must be added to `ko_kr.json` with the same key set.

## Acceptance Criteria

- `./gradlew.bat clean build --no-daemon --no-configuration-cache` succeeds.
- Build copies `brokkr-1.0-SNAPSHOT.jar` to `C:\Users\Yoon\curseforge\minecraft\Instances\test\mods`.
- `/brokkr` opens the enhancement UI.
- Weapon slot accepts supported weapons only.
- Stone slot accepts Enhancement Stones only.
- Enhancement cannot start unless an Enhancement Stone is placed in the dedicated Enhancement Stone slot.
- Enhancement Stones sitting only in the player inventory are not consumed by the UI attempt.
- UI shows current level, next level, success chance, and attack bonus preview.
- Enhance button is disabled when required inputs are missing.
- Enhance button is disabled for `+20` weapons.
- Pressing Enhance starts a visible processing state.
- PROCESSING state visually replaces the normal enhancement UI without opening a second screen.
- Anvil sounds play multiple times before the result appears.
- Sparks, flash, rune, and smoke image effects render in the screen overlay.
- The large forge aura asset is not rendered in the active MVP 2 overlay.
- Success reveal plays a strong positive sound and visible bright particles.
- Failure reveal plays a distinct negative sound and visible failure particles.
- During processing, slots and button cannot be used to duplicate or bypass items.
- Server resolves success/failure.
- Success consumes one stone and increases weapon level by `+1`.
- Failure consumes one stone and keeps weapon level unchanged.
- Result appears in the UI and chat.
- Closing the UI returns any slotted items safely.

## Build And Test Deployment

The Gradle build includes:

```text
copyJarToCurseForgeTestMods
```

Running:

```text
./gradlew.bat clean build --no-daemon --no-configuration-cache
```

also copies the built jar to:

```text
C:\Users\Yoon\curseforge\minecraft\Instances\test\mods
```

The destination can be overridden with:

```text
curseforge_test_mods_dir
BROKKR_TEST_MODS_DIR
```

## Future Extensions

- Dedicated Brokkr enhancement block.
- Custom UI texture.
- Better sound palette.
- Particle burst around the enhancement block.
- Protection charm slot.
- Required material scaling by enhancement tier.
- Recipe for enhancement station.
