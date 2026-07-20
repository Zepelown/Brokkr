# Weapon Enhancement MVP 3 UI Polish Implementation Plan

## Purpose

This document turns `docs/spec/weapon-enhancement-mvp3-ui-polish.md` into an implementation-ready plan.

MVP 3 improves the enhancement UI without changing enhancement mechanics.

Primary implementation goals:

- Make the weapon slot visually read as a weapon slot.
- Make the Enhancement Stone slot visually read as a stone/material slot.
- Add a proper Enhancement Stone item texture and item model.
- Add a current weapon -> success result preview.
- Make the current success chance visually prominent.
- Preserve MVP 2 PROCESSING/result effects.

## Current Code Baseline

Relevant files:

```text
src/main/java/org/brokkr/enhancement/client/EnhancementScreen.java
src/main/java/org/brokkr/enhancement/menu/EnhancementMenu.java
src/main/java/org/brokkr/enhancement/EnhancementData.java
src/main/java/org/brokkr/enhancement/EnhancementNameService.java
src/main/java/org/brokkr/enhancement/EnhancementChance.java
src/main/java/org/brokkr/enhancement/profile/WeaponEnhancementProfiles.java
src/main/resources/assets/brokkr/lang/en_us.json
src/main/resources/assets/brokkr/lang/ko_kr.json
```

Current UI behavior:

- `EnhancementScreen` manually draws a simple panel and two slot outlines.
- The real weapon slot is `EnhancementMenu.WEAPON_SLOT`.
- The real stone slot is `EnhancementMenu.STONE_SLOT`.
- The PROCESSING overlay renders after the normal UI.
- `forge_overlay.png` is packaged but not rendered.
- GUI effect images are rendered with `RenderPipelines.GUI_TEXTURED`.

Current missing resources:

```text
src/main/resources/assets/brokkr/textures/item/enhancement_stone.png
src/main/resources/assets/brokkr/models/item/enhancement_stone.json
src/main/resources/assets/brokkr/textures/gui/enhancement/slot_weapon_hint.png
src/main/resources/assets/brokkr/textures/gui/enhancement/slot_stone_hint.png
src/main/resources/assets/brokkr/textures/gui/enhancement/slot_weapon_frame.png
src/main/resources/assets/brokkr/textures/gui/enhancement/slot_stone_frame.png
src/main/resources/assets/brokkr/textures/gui/enhancement/enhancement_panel.png
src/main/resources/assets/brokkr/textures/gui/enhancement/preview_arrow.png
src/main/resources/assets/brokkr/textures/gui/enhancement/preview_current_frame.png
src/main/resources/assets/brokkr/textures/gui/enhancement/preview_result_frame.png
```

## Implementation Strategy

Use a conservative client-only UI refactor:

- Keep `EnhancementMenu` slot indices and server behavior unchanged.
- Update `EnhancementMenu` slot coordinates to match the polished screen layout.
- Keep `EnhancementAttemptService` unchanged.
- Keep PROCESSING overlay logic unchanged except for ensuring it still draws above the polished UI.
- Add normal-state rendering helpers inside `EnhancementScreen`.
- Generate or create missing PNG assets before wiring references.
- Add the item model JSON.

Do not add custom networking for MVP 3.

## Readiness Corrections

The first draft still left implementation ambiguity. This revision fixes it by making these decisions:

- The UI will expand from `176x166` to `220x214`.
- `EnhancementMenu` slot coordinates will be updated, not only screen-drawn frames.
- Player inventory slots will move down to make room for the preview row.
- The preview row will use non-clickable fake item rendering.
- Slot frames and hints will align to the actual container slot coordinates.
- Generated assets are optional for frames/panel if small-scale readability is poor; deterministic PNGs are acceptable for those.

This avoids the main implementation trap: drawing pretty frames at positions that do not match the actual clickable slots.

## Asset Plan

### Required Item Assets

Create:

```text
src/main/resources/assets/brokkr/textures/item/enhancement_stone.png
src/main/resources/assets/brokkr/models/item/enhancement_stone.json
```

`enhancement_stone.png` target:

- Transparent PNG.
- Readable at Minecraft inventory scale.
- Magical forge gemstone.
- Gold/orange magical core with warm forge glow and dark rim.
- Avoid dominant cyan/blue as the item core color.
- No text.

`enhancement_stone.json`:

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "brokkr:item/enhancement_stone"
  }
}
```

### Required GUI Assets

Create:

```text
slot_weapon_hint.png
slot_stone_hint.png
slot_weapon_frame.png
slot_stone_frame.png
enhancement_panel.png
preview_arrow.png
preview_current_frame.png
preview_result_frame.png
```

Recommended final dimensions:

```text
slot_weapon_hint.png: 32x32
slot_stone_hint.png: 32x32
slot_weapon_frame.png: 24x24
slot_stone_frame.png: 24x24
enhancement_panel.png: 220x214
preview_arrow.png: 64x16
preview_current_frame.png: 40x40
preview_result_frame.png: 40x40
```

Asset generation approach:

1. Generate a compact asset sheet with chroma-key background.
2. Split into individual PNGs.
3. Remove chroma key to alpha.
4. Downscale to target dimensions.
5. Validate transparent corners and non-empty alpha coverage.
6. Commit only final assets under `src/main/resources`.
7. Keep generation scratch files ignored under `tmp/`.

If generated assets are too noisy at small scale:

- Replace slot frames and preview frames with deterministic code-created simple PNGs.
- Keep only the Enhancement Stone and slot hints as generated art.

## UI Layout Plan

MVP 3 should change the screen size and menu slot coordinates together.

Screen size:

```text
imageWidth = 220
imageHeight = 214
```

All coordinates below are relative to `leftPos/topPos`.

Preview row:

```text
preview current frame: 34,  20, 40x40
preview arrow:         78,  32, 64x16
preview result frame:  146, 20, 40x40
weapon slot/current item: 46, 32, vanilla 16x16 slot render
result item render:    158, 32, 16x16
chance text center:    110, 51
```

Real enhancement slots:

```text
weapon slot:           46,  32
stone slot:            102, 76
stone slot frame:      98,  72, 24x24
weapon hint:           38,  24, 32x32
stone hint:            94,  68, 32x32
```

Stats and action:

```text
current level text:    12,  64
next level text:       12,  74
attack bonus text:     118, 64
enhance button:        79,  106, 62x20
status text:           12,  128
```

Player inventory:

```text
inventory label:       29,  120
inventory grid start:  29,  132
hotbar start:          29,  190
```

Why expand to `220x214`:

- The preview row needs two item frames plus arrow/chance text.
- Existing `176x166` forces preview, stats, button, and inventory to overlap.
- Menu click safety is preserved because only slot coordinates change; slot indices stay unchanged.

## Menu Coordinate Plan

Update `EnhancementMenu` constructor positions.

Current:

```text
weapon slot: 44,35
stone slot: 116,35
player inventory: x=8, y=84
hotbar: x=8, y=142
```

MVP 3 target:

```text
weapon slot: 46,32
stone slot: 102,76
player inventory: x=29, y=132
hotbar: x=29, y=190
```

Code-level replacement:

```text
addSlot(new EnhancementSlot(inputSlots, WEAPON_SLOT, 46, 32));
addSlot(new EnhancementStoneSlot(inputSlots, STONE_SLOT, 102, 76));

for inventory rows:
  addSlot(new Slot(playerInventory, column + row * 9 + 9, 29 + column * 18, 132 + row * 18));

for hotbar:
  addSlot(new Slot(playerInventory, column, 29 + column * 18, 190));
```

Do not change:

```text
WEAPON_SLOT = 0
STONE_SLOT = 1
PLAYER_INVENTORY_START = 2
PLAYER_INVENTORY_END = 29
HOTBAR_START = 29
HOTBAR_END = 38
```

## Rendering Plan

Refactor `EnhancementScreen` normal rendering into helper methods:

```text
renderBg(...)
  renderPanel(graphics)
  renderPreviewArea(graphics)
  renderEnhancementSlotFrames(graphics)
  renderSlotHints(graphics)

renderLabels(...)
  renderPreviewLabels(graphics)
  renderStats(graphics)
  renderStatus(graphics)
```

Keep PROCESSING overlay in:

```text
renderProcessOverlay(graphics)
```

Normal-state draw order:

```text
1. enhancement_panel.png or code-drawn panel fallback
2. preview_current_frame.png
3. preview_result_frame.png
4. preview_arrow.png or code-drawn arrow fallback
5. current weapon preview item
6. success result preview item
7. prominent success chance near arrow
8. slot_stone_frame.png
9. empty-slot hints
10. vanilla slot items via super.render
11. labels, stats, button, status
```

Actual render placement:

- Draw panel, frames, arrow, hints, and preview items in `renderBg(...)`.
- Draw labels, success chance, stats, and status in `renderLabels(...)`.
- Keep `super.render(...)` call order unchanged.

Reason:

- `AbstractContainerScreen` calls `renderBg(...)`, then renders real slots/items, then calls `renderLabels(...)`.
- Empty hints must be behind real slot items, so they belong in `renderBg(...)`.
- Preview fake items are not real slots and do not overlap real slots in the target layout.

Important rendering detail:

- Slot hints must draw before `super.render(...)` renders actual item stacks.
- Preview items are not real slots and must not receive clicks.
- Preview frames should not look like extractable inventory slots.

## Rendering API Contract

Use the current Minecraft 1.21.6 APIs confirmed in the local environment:

Texture rendering:

```text
graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, width, height, textureWidth, textureHeight)
```

Item preview rendering:

```text
graphics.renderFakeItem(stack, x, y)
```

Do not use older `GuiGraphics.blit(ResourceLocation, ...)` overloads for new assets.

Add helper:

```text
private void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, int textureWidth, int textureHeight)
private void renderFakePreviewItem(GuiGraphics graphics, ItemStack stack, int x, int y)
```

## Result Preview Implementation

Add helper methods to `EnhancementScreen`:

```text
private ItemStack currentPreviewStack()
private ItemStack resultPreviewStack()
private Component previewChanceText()
private boolean hasSupportedWeapon()
private boolean isMaxLevelWeapon()
```

Current preview:

```text
return menu.getWeapon()
```

Implementation detail:

- Return `ItemStack.EMPTY` if the slot is empty.
- Returning `menu.getWeapon()` is acceptable for read-only rendering.
- Never mutate the returned current preview stack.

Result preview:

```text
weapon = menu.getWeapon()
if unsupported or empty: return ItemStack.EMPTY
level = EnhancementData.getLevel(weapon)
if level >= EnhancementData.MAX_LEVEL: return ItemStack.EMPTY
copy = weapon.copy()
EnhancementData.setLevel(copy, level + 1)
return copy
```

Important:

- Always use `weapon.copy()`.
- Never call `EnhancementData.setLevel(...)` on `menu.getWeapon()` for preview.
- Do not consume stones or touch menu data for preview.

Max-level behavior:

- Current preview shows the weapon.
- Result preview does not show `+21`.
- Result preview is empty.
- Do not render a max-level weapon copy in the result preview.
- Chance text shows max-level status or `-`.

Unsupported item behavior:

- Current preview can show the item if present.
- Result preview remains empty.
- Chance text shows invalid/`-`.

Success chance:

```text
if supported and level < max:
  EnhancementChance.successChanceForCurrentLevel(level)
else:
  "-"
```

Concrete helper contract:

```text
private ItemStack resultPreviewStack() {
    ItemStack weapon = menu.getWeapon();
    if (!WeaponEnhancementProfiles.isSupported(weapon)) {
        return ItemStack.EMPTY;
    }
    int level = EnhancementData.getLevel(weapon);
    if (level >= EnhancementData.MAX_LEVEL) {
        return ItemStack.EMPTY;
    }
    ItemStack preview = weapon.copy();
    EnhancementData.setLevel(preview, level + 1);
    return preview;
}
```

This mutates only `preview`, not the real menu stack.

Do not cache `resultPreviewStack()` as a field unless cache invalidation is tied to the weapon stack contents. Recompute each render tick first; it is cheap and safer.

## I18N Plan

Add constants to `EnhancementTextKeys`:

```text
SCREEN_SLOT_WEAPON
SCREEN_SLOT_STONE
SCREEN_SLOT_WEAPON_TOOLTIP
SCREEN_SLOT_STONE_TOOLTIP
SCREEN_PREVIEW_CURRENT
SCREEN_PREVIEW_RESULT
SCREEN_PREVIEW_NO_RESULT
```

Add language keys:

```text
screen.brokkr.enhancement.slot.weapon
screen.brokkr.enhancement.slot.stone
screen.brokkr.enhancement.slot.weapon.tooltip
screen.brokkr.enhancement.slot.stone.tooltip
screen.brokkr.enhancement.preview.current
screen.brokkr.enhancement.preview.result
screen.brokkr.enhancement.preview.no_result
```

Korean copy suggestion:

```text
무기
강화석
강화할 무기를 넣으세요.
강화석을 넣으세요.
현재 무기
성공 결과
결과 없음
```

English copy suggestion:

```text
Weapon
Stone
Place a weapon to enhance.
Place an Enhancement Stone.
Current
Success Result
No Result
```

## Code Changes By File

### `EnhancementScreen.java`

Add resource locations:

```text
ENHANCEMENT_PANEL
SLOT_WEAPON_HINT
SLOT_STONE_HINT
SLOT_WEAPON_FRAME
SLOT_STONE_FRAME
PREVIEW_ARROW
PREVIEW_CURRENT_FRAME
PREVIEW_RESULT_FRAME
```

Add layout constants:

```text
PREVIEW_CURRENT_X/Y
PREVIEW_RESULT_X/Y
PREVIEW_ARROW_X/Y
WEAPON_SLOT_FRAME_X/Y
STONE_SLOT_FRAME_X/Y
PLAYER_INVENTORY_LABEL_X/Y
```

Update `init()`:

- Set `imageWidth = 220` and `imageHeight = 214` in the constructor.
- Set `inventoryLabelX = 29`.
- Set `inventoryLabelY = 120`.
- Set Enhance button bounds to `leftPos + 79, topPos + 106, 62, 20`.

Update `renderBg()`:

- Draw `enhancement_panel.png`.
- Draw preview frames and arrow.
- Draw the stone slot frame.
- Draw empty hints when real slots are empty.
- Render the result fake item preview. The current weapon is the real weapon slot.
- Keep real weapon item rendering at vanilla 16x16 scale. Make only the empty-slot weapon hint smaller and dimmer so it reads as a placeholder instead of a second oversized weapon.
- Fall back to code-drawn panel/frames if an asset is intentionally omitted.

Update `renderLabels()`:

- Draw preview labels.
- Draw prominent success chance near preview arrow.
- Move current/next/attack bonus text so it does not overlap preview row.

Add preview item rendering:

- Use `GuiGraphics.renderFakeItem` for preview stacks.
- Use `renderItemDecorations` only if it does not make preview look like a real slot.
- Do not render stack count for the current/result weapon preview.

Potential method:

```text
renderPreviewItem(graphics, stack, x, y)
```

### `EnhancementMenu.java`

Update slot coordinates as specified in "Menu Coordinate Plan".

Do not modify:

- slot constants
- quick-move index ranges
- server attempt logic
- button ids

This means existing quick-move and processing lock logic should continue to work.

### `EnhancementTextKeys.java`

Add MVP 3 keys listed above.

### `en_us.json` and `ko_kr.json`

Add MVP 3 translations.

### Resources

Add generated or deterministic PNGs:

```text
src/main/resources/assets/brokkr/textures/gui/enhancement/*.png
src/main/resources/assets/brokkr/textures/item/enhancement_stone.png
src/main/resources/assets/brokkr/models/item/enhancement_stone.json
```

Minimum viable fallback if image generation is slow:

- Generate only `enhancement_stone.png`, `slot_weapon_hint.png`, and `slot_stone_hint.png`.
- Create frames, arrow, and panel deterministically with simple transparent PNGs or draw them in code.
- The build must not reference missing PNGs.

## Implementation Phases

### Phase 1: Resource Foundation

- Create `textures/item/enhancement_stone.png`.
- Create `models/item/enhancement_stone.json`.
- Create GUI hint/frame/panel/preview assets.
- Validate transparent PNGs.
- Verify resource names exactly match `ResourceLocation` constants.

Exit criteria:

- All required resources exist.
- Build packages the item model and textures.
- `jar tf build/libs/brokkr-1.0-SNAPSHOT.jar` shows item model, item texture, and GUI textures.

### Phase 2: I18N

- Add text key constants.
- Add English translations.
- Add Korean translations.

Exit criteria:

- JSON parses.
- No hardcoded slot/preview labels are required in screen code.

### Phase 3: Preview Stack Helpers

- Add current preview helper.
- Add result preview helper using `weapon.copy()`.
- Add chance text helper.
- Add max-level/invalid handling.
- Add a small debug-safe helper comment near `EnhancementData.setLevel(preview, ...)` explaining that it is applied only to a copy.

Exit criteria:

- Preview helper never mutates the real menu weapon.
- `+20` never produces `+21`.
- `+20` returns `ItemStack.EMPTY` from `resultPreviewStack()`.
- Removing a weapon returns `ItemStack.EMPTY` for result preview.

### Phase 4: Menu Coordinate Update

- Update `EnhancementMenu` slot coordinates.
- Move player inventory and hotbar coordinates down.
- Keep quick-move ranges unchanged.
- Build after the coordinate update.

Exit criteria:

- The UI opens.
- Clickable slots align with their frames once Phase 5 rendering is added.
- No quick-move compilation or index regressions.

### Phase 5: Normal UI Render Polish

- Add panel rendering.
- Add preview frames and arrow.
- Render current weapon preview.
- Render result preview.
- Render success chance prominently near arrow.
- Render slot frames and empty-slot hints.
- Reposition existing stats/button/status.

Exit criteria:

- Empty UI clearly communicates weapon slot and stone slot.
- Inserted sword and stone remain readable.
- Result preview appears for supported weapons.

### Phase 6: PROCESSING Regression Pass

- Confirm PROCESSING overlay still draws above polished normal UI.
- Confirm rune/flash/spark/smoke still render.
- Confirm `forge_overlay.png` remains inactive.

Exit criteria:

- MVP 2 process effect behavior still works.

### Phase 7: Build And Manual Test

- Run clean build.
- Confirm jar copies to CurseForge test instance.
- Run game and test UI at GUI scale 2 and 3.

Exit criteria:

- Acceptance checklist passes or any gaps are documented.

## Manual Test Checklist

Build:

```text
./gradlew.bat clean build --no-daemon --no-configuration-cache
```

Resource checks:

- `enhancement_stone.png` exists.
- `enhancement_stone.json` exists and references `brokkr:item/enhancement_stone`.
- GUI slot/preview assets exist.
- Jar contains all new assets.
- There are no missing texture purple/black placeholders in the UI.

In-game checks:

- `/brokkr` opens the UI.
- Clickable weapon and stone slots align with the visible frames.
- Empty weapon slot shows weapon hint.
- Empty stone slot shows stone hint.
- Sword item appears above/over the hint, not under it.
- Enhancement Stone count remains readable.
- Current weapon preview appears when a supported weapon is inserted.
- Success result preview shows `+N+1`.
- Success chance is visible between or near current/result preview.
- Removing the weapon clears result preview.
- `+20` weapon leaves the result preview empty and does not show `+21`.
- Unsupported item does not show a fake success result.
- Enhance button behavior is unchanged.
- PROCESSING overlay still appears.
- Success/failure reveal still appears.
- GUI scale 2 and 3 remain readable.
- Player inventory rows fit inside the panel and do not overlap status/button text.

Regression checks:

- `/enhanceweapon` still works.
- `/enhanceweapon set <level>` still works for permission level 2.
- UI enhancement consumes only the dedicated stone slot.
- Build still copies jar to `C:\Users\Yoon\curseforge\minecraft\Instances\test\mods`.

## Risks And Mitigations

Small UI space:

- `176x166` is too tight for preview + stats + slots.
- Use `220x214` as the MVP 3 baseline.
- If text still overlaps in Korean at GUI scale 3, shorten labels or move detailed stats below the preview row.

Generated asset readability:

- High-resolution generated art may blur at 16x16.
- Validate by downscaling early.
- Replace noisy frames with deterministic simple assets if needed.

Preview mutation risk:

- Calling `EnhancementData.setLevel` on the real menu stack would mutate the real item.
- Always call it only on `weapon.copy()`.

Fake preview slot confusion:

- Preview should not look like a clickable/extractable slot.
- Use distinct preview frames and labels.
- Do not use the same exact frame art as real slots for preview frames.

PROCESSING overlay regression:

- Normal UI polish should not alter PROCESSING state.
- Keep overlay render path separate and test after UI changes.

## Definition Of Done

- MVP 3 resources are present and packaged.
- Enhancement Stone has a visible inventory icon.
- UI visually communicates weapon slot and stone slot.
- Current weapon -> success result preview is present.
- Success chance is prominent in the preview flow.
- Existing enhancement mechanics are unchanged.
- Clean build succeeds.
- Jar is copied to the configured CurseForge test instance.
