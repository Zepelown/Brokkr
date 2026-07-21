# Weapon Enhancement MVP 7 Awakened Item Models Spec

## Goal

MVP 7 introduces **Brokkr awakened item models**.

The weapon item ID stays vanilla, but Brokkr changes the rendered item model when the enhancement level reaches aura milestones.

Player-facing goal:

```text
Iron Sword remains minecraft:iron_sword.
At +10, it visually awakens with Brokkr aqua blade light.
At +11, the awakened visual changes to purple blade light.
At +16, the awakened visual changes to gold blade light.
At +20, the visual changes to a red max-level aura model.
```

This replaces the previous idea of converting vanilla weapons into Brokkr item IDs.

## Design Decision

MVP 7 should not convert vanilla swords into new Brokkr item IDs.

Instead, MVP 7 uses Minecraft 1.21.6 item model components:

```text
DataComponents.ITEM_MODEL
  -> points the ItemStack to a Brokkr client item model
  -> item ID remains the original vanilla sword
```

Reason:

- Keeps vanilla item identity.
- Preserves compatibility better than replacing the item.
- Uses the normal item rendering pipeline.
- Lets first-person, third-person, GUI, ground, and fixed display contexts share vanilla transforms.
- Avoids manually matching `RenderHandEvent` hand transforms.

## Current Problem Being Solved

MVP 6 tried to draw aura geometry around the first-person weapon.

Problems found:

- Direct first-person overlay alignment is unstable.
- The aura can float away from the weapon.
- The aura can rotate around the wrong screen-space point.
- Big wrap textures look detached from the blade.

MVP 7 changes the approach:

```text
Do not place aura by hand in screen/hand render space.
Make the item model itself include the aura layer.
```

## Scope

Included:

- Keep vanilla sword item IDs.
- Change `DataComponents.ITEM_MODEL` based on enhancement level.
- Refresh legacy enhanced swords that were created before MVP 7 and therefore do not yet have the model component.
- Add Brokkr client item JSON files under `assets/brokkr/items`.
- Add Brokkr item model JSON files under `assets/brokkr/models/item`.
- Use composite item models for static aura layers.
- Support vanilla sword family:
  - wooden sword
  - stone sword
  - iron sword
  - golden sword
  - diamond sword
  - netherite sword
- Define visual model stages:
  - `+0..+9`: vanilla model
  - `+10`: aqua awakened model
  - `+11..+15`: purple awakened model
  - `+16..+19`: gold awakened model
  - `+20`: red awakened model
- Keep existing enhancement mechanics.
- Keep attack damage bonus at `+0.2` per level.
- Keep enhancement UI and command behavior.

Excluded:

- Replacing vanilla item IDs with Brokkr item IDs.
- Time-based animated aura.
- Custom `ItemModel` Java implementation.
- `SpecialModelRenderer`.
- True bloom/post-processing.
- Dynamic 3D ribbon or coil.
- Third-person custom player render layer.
- Support for modded weapons.

## Model Stage Rules

Rendered model should be derived from enhancement level.

```text
+0  .. +9   use vanilla item model
+10         use Brokkr aqua aura item model
+11 .. +15  use Brokkr purple aura item model
+16 .. +19  use Brokkr gold aura item model
+20         use Brokkr red aura item model
```

When level changes:

```text
EnhancementData.setLevel(...)
  -> refresh item name
  -> refresh DataComponents.ITEM_MODEL
```

If level is reset to `0`:

```text
remove Brokkr ITEM_MODEL override
return to vanilla default model
```

## Item Identity

The item ID must remain vanilla.

Examples:

```text
Before:
  item id: minecraft:iron_sword
  level: +9
  item model: default minecraft iron sword

After successful +9 -> +10:
  item id: minecraft:iron_sword
  level: +10
  item model: brokkr:awakened_iron_sword_aqua

After +15:
  item id: minecraft:iron_sword
  level: +15
  item model: brokkr:awakened_iron_sword_purple

After +20:
  item id: minecraft:iron_sword
  level: +20
  item model: brokkr:awakened_iron_sword_red
```

This means existing vanilla sword behavior remains tied to the vanilla item.

## Supported Source Items

Supported item model switching applies to:

```text
minecraft:wooden_sword
minecraft:stone_sword
minecraft:iron_sword
minecraft:golden_sword
minecraft:diamond_sword
minecraft:netherite_sword
```

Awakened model IDs:

```text
brokkr:awakened_wooden_sword_aqua
brokkr:awakened_wooden_sword_purple
brokkr:awakened_wooden_sword_red
brokkr:awakened_wooden_sword_gold

brokkr:awakened_stone_sword_aqua
brokkr:awakened_stone_sword_purple
brokkr:awakened_stone_sword_red
brokkr:awakened_stone_sword_gold

brokkr:awakened_iron_sword_aqua
brokkr:awakened_iron_sword_purple
brokkr:awakened_iron_sword_red
brokkr:awakened_iron_sword_gold

brokkr:awakened_golden_sword_aqua
brokkr:awakened_golden_sword_purple
brokkr:awakened_golden_sword_red
brokkr:awakened_golden_sword_gold

brokkr:awakened_diamond_sword_aqua
brokkr:awakened_diamond_sword_purple
brokkr:awakened_diamond_sword_red
brokkr:awakened_diamond_sword_gold

brokkr:awakened_netherite_sword_aqua
brokkr:awakened_netherite_sword_purple
brokkr:awakened_netherite_sword_red
brokkr:awakened_netherite_sword_gold
```

## Resource Structure

Client item definitions:

```text
src/main/resources/assets/brokkr/items/awakened_iron_sword_aqua.json
```

Example structure:

```json
{
  "model": {
    "type": "minecraft:composite",
    "models": [
      {
        "type": "minecraft:model",
        "model": "minecraft:item/iron_sword"
      },
      {
        "type": "minecraft:model",
        "model": "brokkr:item/aura/iron_sword_aqua"
      }
    ]
  }
}
```

Item model JSON files:

```text
src/main/resources/assets/brokkr/models/item/aura/iron_sword_aqua.json
src/main/resources/assets/brokkr/models/item/aura/iron_sword_purple.json
src/main/resources/assets/brokkr/models/item/aura/iron_sword_gold.json
```

Aura texture files:

```text
src/main/resources/assets/brokkr/textures/item/aura/iron_sword_aqua.png
src/main/resources/assets/brokkr/textures/item/aura/iron_sword_purple.png
src/main/resources/assets/brokkr/textures/item/aura/iron_sword_gold.png
```

The exact resource count can be reduced by reusing shared aura textures if material-specific texture generation is too large for MVP 7.

## Visual Requirements

MVP 7 uses static model layers.

Visual stages:

```text
+10
  Thin aqua blade highlight aligned to the vanilla sword sprite.
  Matches the +10 item name color tier.

+11..+15
  Thin purple blade highlight plus small arcane accents.
  Matches the high enhancement item name color tier.

+16..+19
  Thin gold outer highlight plus small ember accents.
  Matches the very high enhancement item name color tier.

+20
  Red max-level aura model.
  Uses a red line, a warmer outer edge, and small final highlight marks.
  Matches the max enhancement item name color tier.
  Must remain static for MVP 7.
```

Important:

- The base vanilla sword must remain readable.
- Aura layer should be transparent.
- No large square panel around the weapon.
- No detached screen-space effect.
- GUI icons should remain understandable.
- Avoid broad filled light shapes in GUI; prefer 1-pixel highlights and sparse accents.

## Animation Policy

MVP 7 does not implement time-based animation.

Reason:

- Static composite model layers solve the main alignment problem first.
- Animated ribbon/coil needs custom `ItemModel`, `SpecialModelRenderer`, or another renderer path.
- Animation should be handled after the static model approach is proven in-game.

Future animation path:

```text
MVP 8:
  keep vanilla item ID
  keep Brokkr ITEM_MODEL component
  replace +20 static model with custom ItemModel or SpecialModelRenderer
  add time-based wrap/ribbon rendering
```

Important conclusion:

```text
Custom item ID is not required for animation.
Custom rendering is required for animation.
```

## Data Component Updates

Add a model refresh service:

```text
AwakenedWeaponModelService
```

Responsibilities:

- Determine whether an item is a supported vanilla sword.
- Determine the correct Brokkr item model for enhancement level.
- Write `DataComponents.ITEM_MODEL` for `+10` or higher.
- Remove the custom item model override below `+10`.

Pseudo behavior:

```text
refreshModel(stack, level):
  if unsupported item:
    return

  if level < 10:
    remove DataComponents.ITEM_MODEL override
    return

  material = sword material from item id
  stage = aqua/purple/gold/red from level
  stack.set(DataComponents.ITEM_MODEL, ResourceLocation("brokkr", modelId))
```

This service should be called from:

```text
EnhancementData.setLevel(...)
EnhancementData.increment(...)
admin set-level flow if it bypasses setLevel
server-side inventory refresh for existing enhanced items
```

If `setLevel` is the single write path, calling from `setLevel` is enough.

## Existing Item Migration

Existing worlds can contain Brokkr-enhanced vanilla swords created before MVP 7.

Those stacks already have Brokkr enhancement data, but may not have the new `DataComponents.ITEM_MODEL` value.

MVP 7 should correct those items without requiring the player to re-enhance them:

```text
10 existing sword without ITEM_MODEL
  -> server inventory refresh applies Brokkr aqua aura model

+15 existing sword without ITEM_MODEL
  -> server inventory refresh applies Brokkr purple aura model

+20 existing sword without ITEM_MODEL
  -> server inventory refresh applies Brokkr red aura model
```

The migration must stay narrow:

- Only stacks with Brokkr enhancement data or Brokkr awakened model IDs are considered.
- Unsupported items are not modified.
- Non-Brokkr custom `ITEM_MODEL` values are not removed.
- Supported swords below `+10` only remove Brokkr-owned awakened model overrides.

## Enhancement Flow

No item conversion happens.

Existing enhancement flow remains:

```text
EnhancementAttemptService.attempt(...)
  -> success increments enhancement level
  -> EnhancementData.setLevel(...)
  -> name refresh
  -> item model refresh
```

Failure behavior:

- No level change.
- No item model stage change.

Command behavior:

```text
Set level to 9:
  remove Brokkr item model override

Set level to 10:
  apply aqua aura model

Set level to 15:
  apply purple aura model

Set level to 20:
  apply red aura model
```

## UI Behavior

Enhancement UI should preview the result item using the model that would be applied on success.

Examples:

```text
(+9) Iron Sword success preview:
  (+10) Iron Sword with aqua aura model

(+14) Iron Sword success preview:
  (+15) Iron Sword with purple aura model

(+19) Iron Sword success preview:
  (+20) Iron Sword with red aura model
```

No item name needs to change to "Awakened" in MVP 7 unless the user explicitly wants that later.

Recommended display name:

```text
(+10) Iron Sword
(+15) Diamond Sword
(+20) Netherite Sword
```

The awakening is visual, not an item identity/name conversion.

## I18N

No new item IDs are introduced, so no new item translation keys are required.

Optional message keys:

```text
message.brokkr.enhancement.awakened.aqua
message.brokkr.enhancement.awakened.purple
message.brokkr.enhancement.awakened.red
```

These can be used if Brokkr wants to announce visual awakening milestones.

Example:

```text
+10 success:
  The weapon awakens with pale light.

+15 success:
  The weapon shines with purple aura.

+20 success:
  The weapon reaches its red aura.
```

## Acceptance Criteria

- Item ID remains vanilla after `+10`.
- `+0..+9` uses default vanilla item model.
- `+10` applies Brokkr aqua aura item model.
- `+11..+15` applies Brokkr purple aura item model.
- `+16..+19` applies Brokkr gold aura item model.
- `+20` applies Brokkr red aura item model.
- 1인칭에서 오러 레이어가 무기와 함께 움직인다.
- 3인칭, GUI, ground/fixed 렌더에서도 모델이 크게 깨지지 않는다.
- Existing enhancement level data remains unchanged.
- Existing attack damage bonus remains `+0.2` per level.
- Existing enhancement chance and stone consumption remain unchanged.
- Enhancement UI preview reflects the next model stage.
- Admin/command set-level updates item model stage.
- Existing enhanced swords receive the correct model component after being in a player inventory.
- Clean build succeeds.
- Jar copies to the configured CurseForge test instance.

## Manual Test Checklist

Build:

```text
./gradlew.bat clean build --no-daemon --no-configuration-cache
```

In-game:

- Hold `+0` iron sword and confirm vanilla model.
- Set/enhance to `+9` and confirm vanilla model.
- Set/enhance to `+10` and confirm aqua aura model in first person.
- Set/enhance to `+15` and confirm purple aura model in first person.
- Set/enhance to `+20` and confirm red aura model in first person.
- Press F5 and confirm third-person item model is not broken.
- Drop the item and confirm ground item model is not broken.
- Open inventory and confirm GUI icon remains readable.
- Confirm item ID is still `minecraft:iron_sword`.
- Confirm attack damage tooltip still includes enhancement bonus.
- Load an existing `+10` or higher sword from an older build and confirm the awakened model appears without another enhancement attempt.

## Risks

Resource count:

- Six sword materials times three stages can create many model/texture files.
- MVP 7 can reduce scope to one material first if needed, then expand.

Resource pack compatibility:

- Overriding `ITEM_MODEL` means the stack points to Brokkr model resources.
- User resource packs changing vanilla sword textures may not affect the aura model unless Brokkr model references vanilla base models correctly.

Static visual limitation:

- Composite JSON models do not provide true time-based animation.
- MVP 8 should handle animated aura if MVP 7 static alignment is accepted.

Data component correctness:

- `DataComponents.ITEM_MODEL` must be removed below `+10`.
- Incorrect removal may leave stale aura models on lower-level weapons.

## Future Extensions

- MVP 8 animated final aura via custom `ItemModel`.
- `+20` time-based wrap/ribbon renderer.
- Material-specific aura art.
- Optional awakened display names.
- Support for axes.
- Support for modded weapons through a mapping/config system.


