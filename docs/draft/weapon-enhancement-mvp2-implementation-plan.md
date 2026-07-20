# Weapon Enhancement MVP 2 Implementation Plan

## Purpose

This document turns `docs/spec/weapon-enhancement-mvp2.md` into an implementation-ready plan.

MVP 2 adds a dedicated Brokkr enhancement menu, screen, and same-screen PROCESSING overlay while reusing the existing enhancement rules.

## Readiness Review

Current implementation readiness: mostly ready after this revision.

Before this revision, the plan had the right product direction but was still too broad for direct implementation. The main missing details were:

- Concrete menu state ownership.
- Slot index contract.
- How the Enhance button reaches the server.
- How the authoritative result reaches the client.
- Where temporary menu items are stored.
- How closing and shift-clicking avoid item duplication.
- Which client-only registration hooks are needed.
- How image assets are represented if final art is not available.

This plan resolves those gaps with an MVP-safe implementation route:

- Use an `AbstractContainerMenu` with two temporary server-owned slots.
- Use `ContainerData` for result and process metadata sync where possible.
- Use vanilla menu button click handling for the Enhance action first.
- Avoid custom networking unless result sync cannot be expressed cleanly through menu data.
- Keep PROCESSING animation entirely client-side after the server has already resolved the attempt.

## Existing Code Baseline

Existing reusable core:

- `org.brokkr.enhancement.EnhancementData`
- `org.brokkr.enhancement.EnhancementChance`
- `org.brokkr.enhancement.EnhancementResult`
- `org.brokkr.enhancement.WeaponEnhancementService`
- `org.brokkr.enhancement.profile.WeaponEnhancementProfiles`
- `org.brokkr.enhancement.profile.EnhancedWeaponProfile`
- `org.brokkr.enhancement.item.ModItems`
- `org.brokkr.enhancement.text.EnhancementTextKeys`

Current command flow:

- `/enhanceweapon` reads the weapon from the player's main hand.
- It searches the player's inventory for `brokkr:enhancement_stone`.
- It consumes one stone for a valid attempt.
- It updates the weapon level on success.
- It sends a chat message.

MVP 2 must keep this command working while adding a slot-based UI adapter.

## Target User Flow

1. Player runs `/brokkr`.
2. Server opens the Brokkr enhancement menu.
3. Player places a supported weapon into the weapon slot.
4. Player places `brokkr:enhancement_stone` into the stone slot.
5. Screen shows current level, next level, success chance, and attack bonus preview.
6. Player presses Enhance.
7. Server validates slots, consumes one stone, rolls the result, and updates the weapon if successful.
8. Client receives the authoritative result through menu data.
9. Client enters PROCESSING overlay and hides the result until reveal tick.
10. Client plays repeated anvil sounds and GUI image effects.
11. Result is revealed in the same screen and a chat message is sent.
12. Screen returns to READY.

## Package Layout

Add these packages:

```text
org.brokkr.enhancement.menu
org.brokkr.enhancement.client
org.brokkr.enhancement.command
```

Network package is optional for MVP 2:

```text
org.brokkr.enhancement.network
```

Use custom payloads only if menu data cannot reliably sync the result.

## New Classes

Required server/common classes:

```text
enhancement/EnhancementAttemptService.java
enhancement/menu/EnhancementMenu.java
enhancement/menu/EnhancementMenuData.java
enhancement/menu/EnhancementSlot.java
enhancement/menu/EnhancementStoneSlot.java
enhancement/menu/ModMenus.java
enhancement/command/BrokkrCommand.java
```

Required client-only classes:

```text
enhancement/client/EnhancementScreen.java
enhancement/client/EnhancementScreenState.java
enhancement/client/EnhancementProcessStep.java
enhancement/client/GuiEffectParticle.java
enhancement/client/EnhancementScreenSounds.java
enhancement/client/BrokkrClientEvents.java
```

Optional network classes:

```text
enhancement/network/EnhancementResultPayload.java
enhancement/network/BrokkrPayloads.java
```

## Menu Registration

Add a deferred menu register in `ModMenus`.

Expected shape:

```text
public static final DeferredRegister<MenuType<?>> MENUS
public static final DeferredHolder<MenuType<?>, MenuType<EnhancementMenu>> ENHANCEMENT_MENU
```

Wire it from `Brokkr` constructor:

```text
ModMenus.MENUS.register(modEventBus)
```

Use the current NeoForge 1.21.6 menu registration API available in the project environment. Confirm exact helper names while coding because NeoForge menu factory APIs can differ by minor version.

## Client Screen Registration

Register `EnhancementScreen` only on the client mod event bus.

Expected event:

```text
RegisterMenuScreensEvent
```

Expected registration:

```text
event.register(ModMenus.ENHANCEMENT_MENU.get(), EnhancementScreen::new)
```

Do not reference `EnhancementScreen` from common setup code that loads on a dedicated server.

## Command Entry Point

Add `/brokkr`.

Rules:

- No operator permission required.
- Server-player only.
- Opens `EnhancementMenu`.
- Sends `command.brokkr.open_ui.success` only if useful; do not spam chat if opening the screen is enough.

Implementation note:

- Use a menu provider opened from the server side.
- The UI command must not run an enhancement attempt.

## Slot Storage Model

`EnhancementMenu` owns a two-slot temporary container:

```text
slot 0: weapon slot
slot 1: enhancement stone slot
```

Player inventory slots follow after those two slots.

The menu must define constants:

```text
WEAPON_SLOT = 0
STONE_SLOT = 1
PLAYER_INVENTORY_START = 2
PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27
HOTBAR_START = PLAYER_INVENTORY_END
HOTBAR_END = HOTBAR_START + 9
```

Use these constants everywhere in `quickMoveStack`, validation, and click locking.

## Slot Rules

Weapon slot:

- Accepts only `WeaponEnhancementProfiles.isSupported(stack)`.
- Maximum stack size is `1`.

Stone slot:

- Accepts only `ModItems.ENHANCEMENT_STONE`.
- Allows normal item stack size.

Player inventory:

- Inventory-only stones do not count for UI attempts.
- Only the stone slot is consumed by UI enhancement.

## Shift-Click Rules

`quickMoveStack` must implement:

- From weapon slot or stone slot to player inventory/hotbar.
- From player inventory/hotbar:
  - supported weapon goes to weapon slot if empty,
  - enhancement stone goes to stone slot,
  - other items move between inventory and hotbar.

During PROCESSING lock:

- `quickMoveStack` returns `ItemStack.EMPTY`.
- Slot clicks are rejected.

## Closing Rules

Preferred MVP 2 behavior:

- Closing the UI is allowed.
- Server has already resolved the result when PROCESSING starts.
- Remaining slot contents are returned through normal menu `removed(Player)` handling.

`removed(Player)` must:

- Call superclass behavior.
- Return weapon slot contents to the player.
- Return remaining stone slot contents to the player.
- Drop items only if vanilla inventory insertion fails.

Duplication safety requirement:

- Every item return must clear the temporary slot before or as part of returning/dropping.

Fallback if testing finds an issue:

- Prevent screen close during PROCESSING until reveal finishes.

## Server Attempt Service

Extract slot-based attempt logic so command and UI do not drift.

Add:

```text
EnhancementAttemptService.attempt(ItemStack weapon, ItemStack stone, RandomSource random)
```

Rules:

- Reject unsupported weapon.
- Reject missing stone.
- Reject max level.
- For a valid attempt, consume exactly one stone from the provided stone stack.
- Roll `EnhancementChance`.
- On success, increase level by `+1`.
- On failure, keep level unchanged.
- Return `EnhancementResult`.

Refactor `WeaponEnhancementService.attempt(Player)` into an adapter:

```text
weapon = player.getMainHandItem()
stone = first matching inventory stack
result = EnhancementAttemptService.attempt(weapon, stone, player.getRandom())
```

This preserves MVP 1 command behavior.

## Menu Data Contract

Use `ContainerData` or the equivalent current API to sync small integers from server menu to client screen.

Required data slots:

```text
DATA_STATE
DATA_RESULT_TYPE
DATA_PREVIOUS_LEVEL
DATA_NEW_LEVEL
DATA_SUCCESS_CHANCE
DATA_ATTEMPT_SEQUENCE
```

Meaning:

- `DATA_STATE`: server menu state for lock decisions.
- `DATA_RESULT_TYPE`: integer id of `EnhancementResult.Type`.
- `DATA_PREVIOUS_LEVEL`: level before latest attempt.
- `DATA_NEW_LEVEL`: level after latest attempt.
- `DATA_SUCCESS_CHANCE`: chance used by latest attempt.
- `DATA_ATTEMPT_SEQUENCE`: increments after each accepted attempt so the client can detect a new result even if the result type is the same as the previous attempt.

Server menu states:

```text
IDLE = 0
PROCESSING_LOCKED = 1
RESULT_READY = 2
```

The server may move from `PROCESSING_LOCKED` to `RESULT_READY` immediately after resolving because the client owns the reveal timing. The menu should still reject additional enhance actions until the client sends or triggers a reset after reveal.

Simpler fallback:

- Keep server state locked for a fixed tick counter in the menu.
- Unlock after about 50 ticks.

Use the simpler fallback first if adding a client reset packet would add too much networking.

## Enhance Button Handling

Preferred MVP 2 path:

- The client button calls the vanilla menu button-click route.
- Server receives the button id in `EnhancementMenu`.
- Server calls `tryEnhance()`.

Button id:

```text
BUTTON_ENHANCE = 0
```

`tryEnhance()` must:

- Reject if processing/locked.
- Read weapon slot and stone slot.
- Call `EnhancementAttemptService`.
- Update menu data fields.
- Broadcast menu changes.
- Send chat result message to the player.

If vanilla button-click routing is unavailable or awkward in NeoForge 1.21.6, use a custom payload:

```text
StartEnhancementPayload(menuContainerId)
```

The server handler must validate that the player's current container is an `EnhancementMenu` with the same container id before attempting enhancement.

## Client State Contract

Client screen states:

```text
EMPTY
READY
PROCESSING
SUCCESS
FAILURE
MAX_LEVEL
INVALID
```

Client detects a new accepted attempt when `DATA_ATTEMPT_SEQUENCE` changes.

On new attempt:

- Cache result type and levels from menu data.
- Set screen state to `PROCESSING`.
- Set `processTick = 0`.
- Clear previous GUI particles.
- Disable the Enhance button.

At reveal tick:

- If result type is success, state becomes `SUCCESS`.
- If result type is failed roll, state becomes `FAILURE`.
- For validation failures, show an INVALID/MAX_LEVEL/no-stone status without PROCESSING.

At finish tick:

- State returns to `READY`, `MAX_LEVEL`, or `INVALID` based on current slot contents.

## PROCESSING Timeline

Use this fixed timeline for MVP 2:

```text
0 ticks:  PROCESSING starts, dark overlay appears
5 ticks:  hammer strike 1, small shake, spark burst
15 ticks: hammer strike 2, stronger shake, spark burst
25 ticks: hammer strike 3, hammer flash, rune pulse
34 ticks: result reveal
50 ticks: return to READY-like input state
```

Status keys by tick:

```text
0-9:   screen.brokkr.enhancement.status.processing.prepare
10-19: screen.brokkr.enhancement.status.processing.hammer_1
20-29: screen.brokkr.enhancement.status.processing.hammer_2
30-33: screen.brokkr.enhancement.status.processing.hammer_3
34+:   success/failure status
```

## Interaction Locking

The server menu is authoritative for item movement.

During lock:

- Enhance action is rejected.
- `quickMoveStack` returns empty.
- Slot click behavior rejects taking, placing, swapping, or throwing items from menu slots.

Client button locking is visual convenience only. It is not security.

Implementation candidates:

- Override menu click handling if available in current mappings.
- Otherwise reject movement in slot methods and `quickMoveStack`, then manually test common click paths.

Manual test paths are required because container click APIs differ across Minecraft versions.

## GUI Image Assets

Create assets under:

```text
src/main/resources/assets/brokkr/textures/gui/enhancement/
```

Required files:

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

MVP asset dimensions:

```text
forge_overlay.png: 256x256
rune_circle.png: 128x128
rune_success.png: 128x128
rune_failure.png: 128x128
spark.png: 32x32
smoke.png: 48x48
hammer_flash.png: 96x96
result_flash.png: 256x256
```

Asset requirements:

- PNG.
- Transparent background.
- No text baked into the image.
- High contrast enough to read over the dark overlay.
- Designed for screen-space GUI rendering, not world particles.

If generated art is not ready:

- Use placeholder transparent PNGs with the exact filenames.
- Keep placeholders visually distinct so rendering and animation can be tested.

## GUI Rendering Plan

`EnhancementScreen.render()`:

- Render normal background and slots first.
- Render labels and button for non-processing states.
- If `PROCESSING`, `SUCCESS`, or `FAILURE`, render overlay on top.

Overlay draw order:

1. Dark translucent full-screen layer.
2. Optional screen shake transform.
3. Centered `forge_overlay.png`.
4. Rotating/pulsing `rune_circle.png`.
5. Timed `hammer_flash.png`.
6. GUI particles.
7. `result_flash.png` at reveal.
8. `rune_success.png` or `rune_failure.png` at result.
9. Status/result text.

`containerTick()`:

- Advance `processTick`.
- Trigger sound cues only once per scheduled tick.
- Spawn `GuiEffectParticle` instances.
- Decay particle alpha and position.
- Update flash alpha and shake ticks.

## GUI Particle Model

Use a lightweight client-only model:

```text
GuiEffectParticle
- ResourceLocation texture
- float x
- float y
- float velocityX
- float velocityY
- float scale
- float alpha
- float rotation
- int age
- int lifetime
```

Particle update:

- `x += velocityX`
- `y += velocityY`
- `alpha` fades by age/lifetime
- remove when `age >= lifetime`

Use deterministic local random seeded from process start time or screen random. The visual particles do not affect gameplay.

## Sound Plan

Add `EnhancementScreenSounds`.

Methods:

```text
playStrike(Minecraft client, int strikeIndex)
playSuccess(Minecraft client)
playFailure(Minecraft client)
```

Initial vanilla sound choices:

```text
strike 1: anvil use
strike 2: anvil use
strike 3: anvil land
success: player level-up or experience pickup
failure: item break
```

Confirm exact `SoundEvents` field names against the current 1.21.6 mappings during implementation.

Sound rules:

- Play client-local sounds for the player using the UI.
- Do not broadcast world-global sounds in MVP 2.
- Prevent repeated playback by tracking which cue ticks already fired.

## I18N Work

Add constants to `EnhancementTextKeys`.

Add keys to both:

```text
src/main/resources/assets/brokkr/lang/en_us.json
src/main/resources/assets/brokkr/lang/ko_kr.json
```

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

Suggested Korean copy can be adjusted during implementation:

```text
강화
현재 강화: +%s
다음 강화: +%s
성공 확률: %s%%
공격력 보너스: +%s -> +%s
강화
무기와 강화석을 넣어주세요.
강화를 시작할 수 있습니다.
준비 중...
날을 달구는 중...
무기를 두드리는 중...
결과를 읽는 중...
강화 성공!
강화 실패
최대 강화입니다.
지원하지 않는 무기입니다.
강화석이 필요합니다.
```

## Implementation Phases

### Phase 1: Core Refactor

- Add `EnhancementAttemptService`.
- Move slot-stack attempt logic into it.
- Refactor `WeaponEnhancementService.attempt(Player)` to call the new service.
- Keep `/enhanceweapon` behavior unchanged.
- Build after this phase.

Exit criteria:

- Existing command still consumes inventory stones.
- Existing command still updates levels and names.
- Build passes.

### Phase 2: Menu Foundation

- Add `ModMenus`.
- Register menu type from `Brokkr`.
- Add `EnhancementMenu`.
- Add temporary two-slot container.
- Add weapon and stone slot classes.
- Add player inventory and hotbar slots.
- Add `quickMoveStack`.
- Add `removed(Player)` item return.

Exit criteria:

- Menu can be constructed without a client screen.
- Shift-click rules compile and are reviewable.
- Slot return path is explicit.

### Phase 3: UI Command

- Add `/brokkr`.
- Open `EnhancementMenu` for the server player.
- Register command from existing command event path.

Exit criteria:

- `/brokkr` opens a menu once the client screen exists.
- `/enhanceweapon` remains registered.

### Phase 4: Basic Client Screen

- Add client screen registration.
- Add `EnhancementScreen`.
- Draw title, slots, level/chance/bonus labels.
- Add Enhance button.
- Disable button for empty, invalid, no-stone, max-level, or processing states.

Exit criteria:

- UI opens.
- Slots display.
- Labels update when slot contents change.

### Phase 5: Server Attempt From UI

- Wire Enhance button to server action.
- Server validates menu and slots.
- Server calls `EnhancementAttemptService`.
- Server updates menu data.
- Server sends chat result message.
- Client detects `DATA_ATTEMPT_SEQUENCE` change.

Exit criteria:

- UI enhancement consumes only the dedicated stone slot.
- Success increases weapon level.
- Failure keeps weapon level.
- Inventory-only stones are ignored.

### Phase 6: Processing Overlay

- Add client process state and timer.
- Lock client controls visually.
- Lock server item movement during processing.
- Add dark overlay.
- Add status text timeline.
- Add sound cue tracking.

Exit criteria:

- Pressing Enhance does not reveal result immediately.
- Anvil sounds play multiple times.
- Result reveals around tick 34.
- Input unlocks around tick 50.

### Phase 7: Image Effects

- Add placeholder or generated PNG assets.
- Add `GuiEffectParticle`.
- Render rune, sparks, smoke, hammer flash, and result flash.
- Add distinct success and failure effect paths.

Exit criteria:

- PROCESSING overlay has visible image effects.
- Success reveal is visually different from failure reveal.
- Missing texture errors do not occur.

### Phase 8: Safety and Polish

- Test click, shift-click, hotbar swap, and close behavior during READY and PROCESSING.
- Test max-level weapon.
- Test unsupported item.
- Test missing stone.
- Test GUI scale changes.
- Build with clean Gradle command.

Exit criteria:

- No obvious duplication path.
- No lost items on close.
- `./gradlew.bat clean build --no-daemon --no-configuration-cache` passes.

## Manual Test Checklist

Build:

```text
./gradlew.bat clean build --no-daemon --no-configuration-cache
```

Run client:

```text
./gradlew.bat runClient --no-daemon --no-configuration-cache
```

Functional tests:

- `/brokkr` opens the UI.
- Weapon slot accepts iron sword.
- Weapon slot rejects non-weapons.
- Stone slot accepts Enhancement Stone.
- Stone slot rejects non-stones.
- Enhance button disabled when weapon missing.
- Enhance button disabled when stone missing.
- Enhance button disabled at `+20`.
- Enhancement consumes exactly one stone from the stone slot.
- Inventory-only stones are not consumed.
- Success increments level by one.
- Failure keeps level unchanged.
- Name prefix updates to `(+N)`.
- Chat result appears.

Interaction safety tests:

- Shift-click weapon into weapon slot.
- Shift-click stone into stone slot.
- Shift-click other items between inventory and hotbar.
- Try moving slots during PROCESSING.
- Try closing during PROCESSING.
- Confirm slotted items return or remain safe.
- Confirm no duplication after reopen.

Visual/audio tests:

- PROCESSING overlay appears in the same screen.
- Normal UI is dimmed or visually replaced.
- Three strike sounds play before reveal.
- Hammer flashes appear on strike ticks.
- Sparks appear during processing.
- Success shows bright flash/rune/sparks.
- Failure shows dull flash/rune/smoke.
- GUI scale 2 and 3 remain readable.

Regression tests:

- Existing `/enhanceweapon` command still works.
- Existing `/enhanceweapon set <level>` still works for permission level 2.
- Hit particles still appear for enhanced weapons.
- Attack damage bonus remains `+0.2` per level.

## Known Implementation Risks

NeoForge API drift:

- Menu factory, button click, and screen registration APIs may differ slightly in 1.21.6.
- Resolve by checking the generated mappings/source in the local Gradle environment during coding.

Container click locking:

- Minecraft container click paths are easy to miss.
- Manual duplication testing is mandatory before considering MVP 2 done.

Client/server timing:

- Server resolves immediately, client reveals later.
- The result may already be visible in the item tooltip if the player can inspect the slot during PROCESSING.
- MVP mitigation: draw the overlay above slots and block interaction during PROCESSING.

Generated image assets:

- Transparent GUI art may need cleanup.
- Use placeholder PNGs first if final generated assets are not ready.

## Acceptance Checklist

- `./gradlew.bat clean build --no-daemon --no-configuration-cache` succeeds.
- `/brokkr` opens the enhancement UI.
- Weapon slot rejects unsupported items.
- Stone slot rejects non-stone items.
- Enhancement cannot start without stone in the dedicated slot.
- Inventory-only stones are not consumed.
- UI shows current level, next level, success chance, and attack bonus preview.
- Enhance button starts PROCESSING state.
- PROCESSING state visually replaces the normal enhancement UI.
- Anvil sounds play multiple times.
- Sparks, smoke, flash, and rune images appear during processing/result reveal.
- Success reveal feels strong and rewarding.
- Failure reveal feels distinct and impactful.
- Server decides the result.
- Slot contents remain safe when closing the UI.
- Existing `/enhanceweapon` command still works.
