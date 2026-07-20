# Weapon Enhancement MVP 3 UI Polish Spec

## Goal

MVP 3 focuses on making the Brokkr enhancement UI understandable before the player reads any text.

The current MVP 2 UI is functional, but the weapon slot and Enhancement Stone slot are visually generic. MVP 3 should make each slot's purpose obvious through dedicated slot graphics, placeholder images, clearer UI layout, and a proper Enhancement Stone item resource.

MVP 3 also adds a clear result preview flow: when a weapon is placed in the UI, the screen should show the current weapon on the left, the expected successful enhancement result on the right, and the current success chance between or near them.

The enhancement rules do not change in MVP 3.

Unchanged core rules:

- Enhancement level: `+0` to `+20`
- Success chance: `100%`, `70%`, `40%`, `10%`
- Failure result: stone consumed, weapon level unchanged
- Damage bonus: `+0.2` attack damage per level
- Server owns the enhancement result
- UI attempts consume only the dedicated Enhancement Stone slot

## Current Baseline

MVP 2 already has:

- `/brokkr` command-opened enhancement UI
- One weapon slot
- One Enhancement Stone slot
- Current/next level display
- Success chance display
- Attack bonus preview
- Enhance button
- PROCESSING overlay
- Rune, spark, flash, smoke, success, and failure GUI effects
- Build task that copies the jar to the CurseForge test instance

Known UI gaps:

- The weapon slot does not visually communicate "put weapon here".
- The Enhancement Stone slot does not visually communicate "put Enhancement Stone here".
- The Enhancement Stone item currently lacks a proper dedicated item texture/model resource.
- The player cannot visually compare the current weapon with the success result item.
- Success chance is present, but it is not yet visually central to the enhancement decision.
- The screen background is still utilitarian and does not yet feel like a focused enhancement station.
- Text exists, but the visual affordance is weak.

## MVP 3 Scope

Included:

- Dedicated slot background/placeholder image for the weapon slot.
- Dedicated slot background/placeholder image for the Enhancement Stone slot.
- Dedicated Enhancement Stone item texture.
- Item model JSON for `brokkr:enhancement_stone`.
- Current weapon and success-result preview layout.
- Success chance display positioned as a primary UI element.
- Clearer slot labels or icon-based slot identity.
- Improved enhancement UI layout spacing.
- UI visual pass so the screen reads as a forge/enhancement interface.
- Generated image assets when no existing image is available.
- Update specs/docs to reflect final resource names.

Excluded:

- New enhancement block.
- New recipes or loot tables.
- JEI/REI integration.
- Full texture pack replacement.
- Weapon-specific item art.
- Animated sprite sheets.
- New enhancement mechanics.
- Protection charms or extra material slots.
- Server/network protocol changes unless UI rendering requires a small sync fix.

## UX Direction

The player should immediately understand:

- Left slot is for a weapon.
- Right slot is for an Enhancement Stone.
- The left preview item is the current weapon.
- The right preview item is the successful enhancement result.
- The displayed success chance applies to the next enhancement attempt.
- The stone slot expects a special magical material, not any inventory item.
- The enhancement UI is a deliberate forge process, not a generic chest-like menu.

Text should support the UI, but not carry the entire meaning.

## UI Layout Direction

MVP 3 keeps the current two-slot interaction model:

```text
[ Current Weapon ]  ---- success chance ---->  [ Success Result Preview ]

[ Weapon Slot ]                          [ Enhancement Stone Slot ]

Current Level
Next Level
Success Chance
Attack Bonus

[ Enhance ]

Status / Result
```

Visual improvements:

- Add a weapon silhouette or sword placeholder inside/behind the weapon slot when empty.
- Add a gem/stone placeholder inside/behind the stone slot when empty.
- Use the left current weapon preview area as the actual weapon input slot.
- Add a success result preview area on the right.
- Add an arrow, divider, or forge-path visual between current weapon and success result.
- Make the success chance prominent near the arrow/path between the two preview items.
- Make slot frames visually distinct from player inventory slots.
- Keep inserted item stacks readable above placeholder art.
- Keep count text readable for Enhancement Stones.
- Keep UI usable at vanilla GUI scale 2 and 3.

Slot placeholder behavior:

- Placeholder image is visible only when the corresponding slot is empty.
- Placeholder image does not render over actual inserted items.
- Placeholder image should be muted enough to read as a hint, not an item.
- Slot validation remains server-side and unchanged.

## Result Preview Behavior

When no weapon is inserted:

- Current weapon preview is empty or shows a muted weapon hint.
- Success result preview is empty or shows a muted result hint.
- Success chance shows `-` or an empty-state message.

When a supported weapon below `+20` is inserted:

- Current weapon preview is the actual inserted weapon slot.
- The current weapon slot uses vanilla item rendering. The empty-slot weapon hint should be smaller and dimmer than a real item so it does not look like an inserted weapon.
- Success result preview renders a copy of the weapon as it would look after a successful enhancement.
- If the current weapon is `+N`, the result preview should show `+(N+1)`.
- The result preview should include the updated item name prefix, e.g. `(+7) Iron Sword`.
- The displayed success chance is the chance for the current level's next attempt.
- Attack bonus preview remains available and should match the result preview.

When the weapon is `+20`:

- Current weapon preview renders the inserted weapon.
- Result preview is empty.
- Result preview must not show a max-level item copy.
- Result preview must not show `+21`.
- Success chance should show max-level status or `-`.
- Enhance button remains disabled.

When an unsupported item is inserted:

- Current preview may show the item.
- Result preview should show invalid state, not a fake result.
- Success chance should show `-` or invalid state.

Important rule:

- The result preview is only a visual client-side copy.
- It must not mutate the real item stack.
- The server still decides the actual success/failure result when Enhance is pressed.

Suggested visual:

```text
[ current weapon item ]  --  Success Chance: 70%  -->  [ (+N+1) result item ]
```

The result item should feel like a preview card/slot, not a real inventory slot that can be clicked or extracted.

## Result Preview Assets

Add or reuse GUI assets under:

```text
src/main/resources/assets/brokkr/textures/gui/enhancement/
```

Recommended new assets:

```text
preview_arrow.png
preview_result_frame.png
preview_current_frame.png
```

Asset roles:

- `preview_arrow.png`: visual connection between current weapon and result preview.
- `preview_current_frame.png`: frame behind the current weapon preview.
- `preview_result_frame.png`: frame behind the success result preview.

These assets are optional if the first implementation uses code-drawn lines/frames, but the layout must still clearly communicate current weapon -> success result.

## Required GUI Assets

Add or update assets under:

```text
src/main/resources/assets/brokkr/textures/gui/enhancement/
```

Required new GUI assets:

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

Asset roles:

- `slot_weapon_hint.png`: empty-slot weapon/sword silhouette.
- `slot_stone_hint.png`: empty-slot Enhancement Stone/gem silhouette.
- `slot_weapon_frame.png`: weapon slot frame, visually distinct but compact.
- `slot_stone_frame.png`: stone slot frame, visually distinct but compact.
- `enhancement_panel.png`: optional main panel/background texture for the enhancement UI.
- `preview_arrow.png`: current-to-result direction marker.
- `preview_current_frame.png`: current weapon preview frame.
- `preview_result_frame.png`: success result preview frame.

Recommended dimensions:

```text
slot_weapon_hint.png: 18x18 or 32x32
slot_stone_hint.png: 18x18 or 32x32
slot_weapon_frame.png: 24x24
slot_stone_frame.png: 24x24
enhancement_panel.png: 220x214
preview_arrow.png: 48x16 or 64x16
preview_current_frame.png: 32x32 or 40x40
preview_result_frame.png: 32x32 or 40x40
```

Rules:

- PNG with transparency where appropriate.
- No baked text.
- Readable at small GUI sizes.
- Do not use full-screen decorative art for the panel.
- Avoid large bright effects in the normal READY state.
- Keep PROCESSING/result effect assets separate from normal UI assets.

## Enhancement Stone Item Resource

Add a proper Enhancement Stone item texture and model.

Required paths:

```text
src/main/resources/assets/brokkr/textures/item/enhancement_stone.png
src/main/resources/assets/brokkr/models/item/enhancement_stone.json
```

Item texture direction:

- Small magical forge gemstone.
- Should read as an enhancement material, not raw ore.
- Palette: gold/orange magical core, warm forge glow, dark stone or metal edge.
- Avoid a dominant cyan/blue core for the Enhancement Stone item.
- No text.
- No large background.
- Must work in inventory at 16x16 item scale.

Model JSON direction:

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "brokkr:item/enhancement_stone"
  }
}
```

If generated art is used:

- Generate at high resolution first.
- Crop/clean to transparent PNG.
- Downscale to Minecraft item-friendly dimensions.
- Validate it is readable at inventory scale.

## Generated Image Policy

When existing images are missing, generate assets that match the Brokkr direction:

- Fantasy blacksmith forge.
- Magical rune/gem language.
- Enhancement Stone item art should be gold/orange dominant.
- High contrast, readable at Minecraft GUI scale.
- Transparent background.
- No text or watermark.
- No real-world logos.

Generated asset candidates:

- Enhancement Stone item icon.
- Weapon slot hint silhouette.
- Stone slot hint silhouette.
- Slot frames.
- Compact enhancement panel texture.

Do not generate:

- UI text as part of images.
- Full-screen cinematic backgrounds for the normal UI.
- New weapon appearance textures.

## Rendering Requirements

`EnhancementScreen` should render in this order during normal READY/EMPTY/MAX/INVALID states:

```text
1. Enhancement panel/background
2. Current/result preview frames
3. Success result preview item
4. Vanilla slot items, including the current weapon slot
5. Success chance near the preview arrow/path
6. Slot frames
7. Empty-slot hints if slots are empty
8. Vanilla slot items
9. UI labels and stats
10. Enhance button
11. Status text
```

PROCESSING/SUCCESS/FAILURE states keep the MVP 2 overlay behavior:

```text
1. Normal UI render
2. Dark overlay
3. Rune/effect assets
4. Status/result text
```

The MVP 2 large `forge_overlay.png` remains inactive unless explicitly reintroduced in a smaller, less disruptive form.

## I18N Requirements

Existing labels can remain, but MVP 3 may add slot-specific labels/tooltips if needed.

Optional keys:

```text
screen.brokkr.enhancement.slot.weapon
screen.brokkr.enhancement.slot.stone
screen.brokkr.enhancement.slot.weapon.tooltip
screen.brokkr.enhancement.slot.stone.tooltip
screen.brokkr.enhancement.preview.current
screen.brokkr.enhancement.preview.result
screen.brokkr.enhancement.preview.no_result
```

Rules:

- Add every key to both `en_us.json` and `ko_kr.json`.
- Do not bake these labels into PNG assets.

## Acceptance Criteria

- `./gradlew.bat clean build --no-daemon --no-configuration-cache` succeeds.
- Build still copies the jar to the CurseForge test instance `mods` folder.
- `/brokkr` opens the enhancement UI.
- Weapon slot has a visible weapon-specific hint when empty.
- Enhancement Stone slot has a visible stone/gem-specific hint when empty.
- Current weapon preview appears when a supported weapon is inserted.
- Success result preview appears beside it and shows the `+N+1` result.
- Success chance is visually prominent between or near current weapon and result preview.
- Result preview does not mutate the real weapon item.
- Slot hints disappear or stay behind items when actual items are inserted.
- Enhancement Stone has a proper inventory icon.
- `brokkr:enhancement_stone` has a valid item model JSON.
- Slot frames are visually distinct from the player inventory.
- Text and item counts remain readable.
- Normal READY UI does not look like a mostly black screen.
- PROCESSING/result effects still render after the UI polish.
- Existing enhancement mechanics remain unchanged.

## Manual Test Checklist

Build:

```text
./gradlew.bat clean build --no-daemon --no-configuration-cache
```

In-game:

- Open `/brokkr`.
- Confirm empty weapon slot clearly looks like a weapon slot.
- Confirm empty stone slot clearly looks like an Enhancement Stone slot.
- Insert a sword and confirm the placeholder does not cover the sword.
- Insert Enhancement Stones and confirm count text remains readable.
- Confirm the current weapon preview appears.
- Confirm the success result preview shows the next enhancement level.
- Confirm success chance is visible near the preview path.
- Confirm removing the weapon clears the result preview.
- Confirm a `+20` weapon leaves the result preview empty and does not show `+21`.
- Press Enhance and confirm PROCESSING overlay still appears.
- Confirm success/failure effects still render.
- Check inventory item icon for Enhancement Stone.
- Check GUI scale 2 and 3.

## Future Extensions

- Dedicated Brokkr enhancement block UI skin.
- Animated slot highlights.
- Tier-based stone variants.
- Material requirement preview.
- Drag-and-drop hover glow for valid items.
- Invalid-item red slot feedback.
