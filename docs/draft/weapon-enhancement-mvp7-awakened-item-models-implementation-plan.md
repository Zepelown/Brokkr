# Weapon Enhancement MVP 7 Awakened Item Models Implementation Plan

## Purpose

This plan implements `docs/spec/weapon-enhancement-mvp7-awakened-weapons.md`.

MVP 7 keeps vanilla weapon item IDs and changes only the rendered item model through `DataComponents.ITEM_MODEL`.

Target behavior:

```text
+0  .. +9   vanilla sword model
+10         Brokkr aqua aura composite model
+11 .. +15  Brokkr purple aura composite model
+16 .. +19  Brokkr gold aura composite model
+20         Brokkr red aura composite model
```

## API Verification

Local Minecraft/NeoForge 1.21.6 source confirms:

```text
DataComponents.ITEM_MODEL
  type: DataComponentType<ResourceLocation>
  persistent and network synchronized

ItemStack#set(DataComponents.ITEM_MODEL, ResourceLocation)
  writes a model override onto the stack

ItemStack#remove(DataComponents.ITEM_MODEL)
  removes the model override so the item returns to default model behavior

Client item definitions
  are loaded from assets/<namespace>/items/<path>.json

minecraft:composite item model
  can render multiple item models in order
```

This is enough for MVP 7 without a custom Java `ItemModel`.

## Current Code Entry Points

Level writes currently flow through:

```text
EnhancementAttemptService.attempt(...)
  -> EnhancementData.increment(...)
  -> EnhancementData.setLevel(...)

WeaponEnhancementService.setLevel(...)
  -> EnhancementData.setLevel(...)

EnhancementScreen.resultPreviewStack(...)
  -> copy weapon
  -> EnhancementData.setLevel(preview, level + 1)
```

Therefore:

```text
EnhancementData.setLevel(...)
```

is the correct single point to refresh both name and item model.

Existing enhanced items from earlier builds are a separate entry point:

```text
Player inventory contains a Brokkr-enhanced sword
  -> server-side inventory refresh
  -> AwakenedWeaponModelService.refreshModel(...)
```

This keeps old worlds compatible after installing MVP 7.

## Remove MVP 6 Experimental Render Path

MVP 6 introduced first-person fallback/mixin render code. MVP 7 replaces that approach with item model components.

Remove or disable:

```text
src/main/java/org/brokkr/enhancement/client/render/HeldWeaponAuraRenderer.java
src/main/java/org/brokkr/enhancement/client/render/WeaponAuraRenderContext.java
src/main/java/org/brokkr/enhancement/client/render/WeaponAuraRenderEvents.java
src/main/java/org/brokkr/enhancement/client/render/WeaponAuraRenderBracket.java
src/main/java/org/brokkr/enhancement/client/render/WeaponAuraRenderProfile.java
src/main/java/org/brokkr/mixin/client/ItemStackRenderStateLayerRenderStateMixin.java
src/main/java/org/brokkr/mixin/client/ItemInHandRendererMixin.java
src/main/resources/brokkr.mixins.json
```

Update:

```text
src/main/java/org/brokkr/enhancement/client/BrokkrClientEvents.java
  remove WeaponAuraRenderEvents listener

src/main/templates/META-INF/neoforge.mods.toml
  remove [[mixins]] entry if no other mixins remain
```

Reason:

- The previous render path has visible alignment problems.
- Keeping it would duplicate or conflict with model-based aura.
- MVP 7 should prove the item-model path cleanly.

## Add Awakened Weapon Model Service

Create:

```text
src/main/java/org/brokkr/enhancement/AwakenedWeaponModelService.java
```

Responsibilities:

- Determine whether a stack is a supported vanilla sword.
- Map sword item to material key.
- Map enhancement level to aura model stage.
- Write or remove `DataComponents.ITEM_MODEL`.

Target API:

```java
public final class AwakenedWeaponModelService {
    public static boolean refreshModel(ItemStack stack, int level) {
        if (stack.isEmpty()) {
            return false;
        }

        Optional<String> material = materialKey(stack);
        if (material.isEmpty()) {
            return false;
        }

        int clamped = EnhancementData.clampLevel(level);
        if (clamped < 10) {
            if (hasAwakenedModel(stack)) {
                stack.remove(DataComponents.ITEM_MODEL);
                return true;
            }
            return false;
        }

        String stage = stageKey(clamped);
        stack.set(DataComponents.ITEM_MODEL, ResourceLocation.fromNamespaceAndPath(
                Brokkr.MODID,
                "awakened_" + material.get() + "_sword_" + stage
        ));
        return true;
    }
}
```

Material map:

```text
Items.WOODEN_SWORD    -> wooden
Items.STONE_SWORD     -> stone
Items.IRON_SWORD      -> iron
Items.GOLDEN_SWORD    -> golden
Items.DIAMOND_SWORD   -> diamond
Items.NETHERITE_SWORD -> netherite
```

Stage map:

```text
10..14 -> white
15..19 -> orange
20     -> final
```

Important:

- For unsupported items, do not remove `ITEM_MODEL`; another system may own it.
- For supported swords below `+10`, remove Brokkr's item model override only if the current model is Brokkr-owned.
- Do not remove non-Brokkr model overrides from ordinary vanilla swords.
- Return whether the stack was actually mutated so inventory migration can mark the inventory changed only when needed.

## Update EnhancementData

Update:

```text
src/main/java/org/brokkr/enhancement/EnhancementData.java
```

Current behavior:

```java
EnhancementNameService.refreshName(stack, clamped);
```

Target behavior:

```java
EnhancementNameService.refreshName(stack, clamped);
AwakenedWeaponModelService.refreshModel(stack, clamped);
```

Reason:

- Every level change should update the visible model.
- UI preview uses `EnhancementData.setLevel(preview, nextLevel)`, so preview will also get the correct model.

## Resource Generation Strategy

MVP 7 needs six materials times three stages.

Generated client item files:

```text
src/main/resources/assets/brokkr/items/awakened_<material>_sword_aqua.json
src/main/resources/assets/brokkr/items/awakened_<material>_sword_purple.json
src/main/resources/assets/brokkr/items/awakened_<material>_sword_red.json
src/main/resources/assets/brokkr/items/awakened_<material>_sword_gold.json
```

Each client item should be a composite model:

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

Generated Brokkr aura model files:

```text
src/main/resources/assets/brokkr/models/item/aura/<material>_sword_aqua.json
src/main/resources/assets/brokkr/models/item/aura/<material>_sword_purple.json
src/main/resources/assets/brokkr/models/item/aura/<material>_sword_red.json
src/main/resources/assets/brokkr/models/item/aura/<material>_sword_gold.json
```

Each aura model:

```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "layer0": "brokkr:item/aura/<material>_sword_aqua"
  }
}
```

Generated aura textures:

```text
src/main/resources/assets/brokkr/textures/item/aura/<material>_sword_aqua.png
src/main/resources/assets/brokkr/textures/item/aura/<material>_sword_purple.png
src/main/resources/assets/brokkr/textures/item/aura/<material>_sword_red.png
src/main/resources/assets/brokkr/textures/item/aura/<material>_sword_gold.png
```

Texture requirements:

- 16x16 or 32x32 PNG.
- Transparent background.
- Aura aligned to vanilla sword sprite direction.
- No filled square background.
- Aqua/purple/gold/red visual stages match item name enhancement colors.

Implementation note:

- Use generated simple pixel art first.
- Material-specific aura can share a shape but use stage-specific color/intensity.
- Keep texture dimensions compatible with vanilla item sprite style.

## Resource Cleanup

MVP 6 effect textures are no longer used by the model component path.

Remove if no code references them:

```text
src/main/resources/assets/brokkr/textures/effect/weapon_aura_white_blade.png
src/main/resources/assets/brokkr/textures/effect/weapon_aura_orange_blade.png
src/main/resources/assets/brokkr/textures/effect/weapon_aura_wrap_orbit.png
src/main/resources/assets/brokkr/textures/effect/weapon_aura_wrap_core.png
```

Do not remove MVP 5 particle textures:

```text
src/main/resources/assets/brokkr/textures/particle/*
```

MVP 5 particles remain part of the mod.

## UI Preview

Current preview:

```text
EnhancementScreen.resultPreviewStack()
  copy weapon
  EnhancementData.setLevel(preview, level + 1)
  renderFakeItem(preview)
```

Because MVP 7 refreshes model in `EnhancementData.setLevel`, no separate UI-specific model code should be needed.

Validation:

```text
+9 preview -> aqua aura model
+10 preview -> purple aura model
+15 preview -> gold aura model
+19 preview -> red aura model
```

If preview does not update:

- Confirm the stack copy preserves item identity.
- Confirm `DataComponents.ITEM_MODEL` is written to preview.
- Confirm client item JSON path exists.

## Command Behavior

Existing command path should already call:

```text
WeaponEnhancementService.setLevel(...)
  -> EnhancementData.setLevel(...)
```

Validation:

```text
set level 9  -> model override removed
set level 10 -> white model
set level 15 -> orange model
set level 20 -> final model
```

No command-specific implementation should be needed unless a command bypasses `EnhancementData.setLevel`.

## Implementation Phases

### Phase 1: Remove Experimental Runtime Aura Renderer

Tasks:

- Remove render listener registration from `BrokkrClientEvents`.
- Delete MVP 6 client render package if only used for runtime aura.
- Delete client mixin package.
- Delete `brokkr.mixins.json`.
- Remove `[[mixins]]` entry from 
eoforge.mods.toml`.
- Build.

Exit criteria:

- Clean build succeeds.
- No references to `WeaponAuraRenderEvents`, `HeldWeaponAuraRenderer`, or `brokkr.mixins.json`.
- MVP 5 ambient particles still compile.

### Phase 2: Add Model Refresh Service

Tasks:

- Add `AwakenedWeaponModelService`.
- Wire it into `EnhancementData.setLevel`.
- Keep unsupported items untouched.
- Remove Brokkr-owned `ITEM_MODEL` below `+10` for supported swords.
- Set correct model IDs at `+10`, `+15`, and `+20`.
- Return a mutation flag from model refresh.

Exit criteria:

- Compile succeeds.
- Code paths are deterministic and side-effect limited to `DataComponents.ITEM_MODEL`.

### Phase 2.5: Add Existing Item Model Migration

Tasks:

- Add a server-side player inventory tick handler.
- Scan player inventory at a low frequency, not every render frame.
- Only process stacks with Brokkr enhancement data or Brokkr awakened model IDs.
- Refresh model components based on the stored enhancement level.
- Call `Inventory#setChanged` only when a stack was actually mutated.

Exit criteria:

- Existing `+10` or higher swords from old saves gain the expected awakened model.
- Ordinary vanilla swords without Brokkr enhancement data are not modified.
- Non-Brokkr custom item model components are not removed.

### Phase 3: Generate Client Item JSON And Aura Models

Tasks:

- Generate 18 client item JSON files.
- Generate 18 aura model JSON files.
- Generate 18 aura textures.
- Validate paths match `AwakenedWeaponModelService`.

Exit criteria:

- Every model ID written by service has a matching `assets/brokkr/items/<id>.json`.
- Every client item JSON references existing model JSON.
- Every model JSON references existing texture PNG.

### Phase 4: Build And Jar Verification

Commands:

```text
./gradlew.bat clean build --no-daemon --no-configuration-cache
jar tf build/libs/brokkr-1.0-SNAPSHOT.jar | findstr /i "awakened_ aura brokkr.mixins"
```

Expected:

- `awakened_*` resources exist in jar.
- `brokkr.mixins.json` does not exist unless another feature needs it.
- Jar copies to CurseForge test instance.

### Phase 5: In-Game Verification

Manual checks:

- Hold `+0` iron sword: vanilla model.
- Set/enhance to `+9`: vanilla model.
- Set/enhance to `+10`: aqua aura model.
- Set/enhance to `+15`: purple aura model.
- Set/enhance to `+16`: gold aura model.
- Set/enhance to `+20`: red aura model.
- Open inventory: icon readable.
- Press F5: third-person held item not broken.
- Drop item: ground item not broken.
- Confirm item ID remains `minecraft:iron_sword`.

## Review Checklist

Before calling the implementation complete:

- No item ID conversion occurs.
- No first-person fallback renderer remains active.
- `DataComponents.ITEM_MODEL` is the only model switching mechanism.
- Model stage boundaries match spec exactly.
- Existing enhanced items are migrated by inventory refresh.
- UI preview uses the same model refresh path as real enhancement.
- Existing enhancement chance and stone consumption are unchanged.
- Existing attack damage `+0.2` per level remains unchanged.
- Build succeeds.
- Test jar is copied.

## Risks And Mitigations

Resource count:

- 18 client items, 18 models, 18 textures is a lot but manageable.
- If iteration speed is an issue, implement iron sword first, then expand.

Visual quality:

- Static aura textures may look like a flat icon overlay.
- Keep the base vanilla sword model in the composite so readability remains.

Resource pack compatibility:

- The composite references vanilla base models, so vanilla resource pack changes to base sword models should still be visible if they override `minecraft:item/<sword>`.
- Brokkr aura texture remains Brokkr-owned.

Stale model components:

- Removing `ITEM_MODEL` below `+10` is required.
- Test level downgrade through command to catch stale visuals.
- Remove only Brokkr-owned awakened model IDs to avoid clobbering unrelated custom model components.

Existing world compatibility:

- Older enhanced swords will not call `setLevel` automatically.
- Server-side inventory refresh handles this without changing item IDs or enhancement data.

MVP 6 leftovers:

- Leaving old runtime aura active would cause duplicate/conflicting visuals.
- Remove it in Phase 1.

## Definition Of Done

- MVP 7 spec and implementation plan align.
- Supported vanilla swords keep vanilla item IDs.
- `+10`, `+15`, and `+20` apply correct Brokkr item model components.
- `+0..+9` use default vanilla item models.
- Enhancement UI preview shows the next aura model stage.
- Existing enhanced swords from older saves are refreshed to the correct awakened model.
- No MVP 6 fallback/mixin aura renders remain active.
- Clean build succeeds.
- Jar copies to the configured CurseForge test instance.


