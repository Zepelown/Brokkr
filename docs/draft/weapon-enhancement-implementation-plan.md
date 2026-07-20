# Weapon Enhancement MVP Implementation Plan

## Purpose

This document turns `docs/spec/weapon-enhancement-mvp.md` into an implementation plan.

The first implementation should keep the system small but not sword-hardcoded. The MVP starts with vanilla swords, while the design must allow axe, trident, bow, and crossbow profiles to be added later.

## Current Project Baseline

Current mod entry point:

- `src/main/java/org/brokkr/Brokkr.java`

Current config class:

- `(removed sample Config.java)`

Current language file:

- `src/main/resources/assets/brokkr/lang/en_us.json`

The current `Test.java` is still close to the NeoForge sample mod. The implementation should move enhancement logic into focused classes instead of growing the sample entry point.

## Review Findings

The first draft was directionally correct, but it still left several implementation-critical details open. These decisions are now fixed for the MVP:

- Store enhancement level in `DataComponents.CUSTOM_DATA` through `CustomData`.
- Apply attack damage through `ItemAttributeModifierEvent`, not by rewriting every incoming damage amount.
- Use `LivingIncomingDamageEvent` for hit particles because it has the final target, damage source, and amount.
- Register commands through `RegisterCommandsEvent`.
- Register tooltip handling through `ItemTooltipEvent`.
- Use `ServerLevel.sendParticles(...)` for server-side particle spawning.
- Use `DataComponents.ITEM_NAME` for the enhanced name prefix when possible.
- Avoid mixins for MVP.

One caveat remains: preserving an anvil custom name while always showing `(+N)` before it is not cleanly supported by a simple event in the current local API. The MVP should implement the prefix for normal vanilla item names through `DataComponents.ITEM_NAME`. If strict custom-name prefixing is required later, handle it as a separate client/mixin or deeper item-name rendering task.

## MVP Implementation Decisions

- Enhancement storage: `DataComponents.CUSTOM_DATA`.
- Enhancement max level: `20`.
- Damage bonus: `level * 0.2`.
- Success chance: `100%`, `70%`, `40%`, `10%` by current-level tier.
- Enhancement attempt UX: `/enhanceweapon`.
- Debug UX: `/enhanceweapon set <level>`.
- Result feedback: chat via `sendSystemMessage`.
- Name prefix: `DataComponents.ITEM_NAME` for vanilla item names.
- Custom anvil name prefixing: deferred.
- Damage implementation: dynamic attack-damage attribute modifier.
- Hit effect implementation: server particles from incoming damage event.

## Target Package Layout

Use the existing base package:

```text
org.brokkr
```

Suggested new packages:

```text
org.brokkr.enhancement
org.brokkr.enhancement.command
org.brokkr.enhancement.event
org.brokkr.enhancement.item
org.brokkr.enhancement.profile
org.brokkr.enhancement.text
```

Suggested classes:

```text
enhancement/EnhancementData.java
enhancement/EnhancementChance.java
enhancement/WeaponEnhancementService.java
enhancement/EnhancementResult.java
enhancement/EnhancementTier.java
enhancement/EnhancementNameService.java
enhancement/EnhancementParticles.java

enhancement/profile/EnhancedWeaponProfile.java
enhancement/profile/SwordEnhancementProfile.java
enhancement/profile/WeaponEnhancementProfiles.java

enhancement/command/EnhanceWeaponCommand.java

enhancement/event/EnhancementCombatEvents.java
enhancement/event/EnhancementTooltipEvents.java
enhancement/event/EnhancementNameEvents.java
enhancement/event/EnhancementCommandEvents.java

enhancement/item/ModItems.java

enhancement/text/EnhancementTextKeys.java
```

## Event Bus Wiring

Use `Test.java` only as the composition root.

Mod event bus:

- Register item `DeferredRegister`s.
- Register creative tab contents listener.

NeoForge event bus:

- `RegisterCommandsEvent` -> command registration.
- `ItemTooltipEvent` -> tooltip line.
- `ItemAttributeModifierEvent` -> dynamic attack damage bonus.
- `LivingIncomingDamageEvent` -> hit particles.

Suggested constructor wiring:

```text
ModItems.ITEMS.register(modEventBus)
modEventBus.addListener(Test::addCreative)

NeoForge.EVENT_BUS.addListener(EnhancementCommandEvents::registerCommands)
NeoForge.EVENT_BUS.addListener(EnhancementTooltipEvents::addTooltip)
NeoForge.EVENT_BUS.addListener(EnhancementCombatEvents::addAttackDamage)
NeoForge.EVENT_BUS.addListener(EnhancementCombatEvents::spawnHitParticles)
```

## Implementation Phases

### Phase 1: Registry Cleanup and Enhancement Stone

Goal:

- Add the MVP enhancement item and make the current registry structure ready for feature work.

Tasks:

- Keep `Brokkr.MODID` as the single mod id source.
- Add `ModItems` for item registration.
- Register `brokkr:enhancement_stone`.
- Add the enhancement stone to the existing mod creative tab.
- Add I18N entries for the item name.

Expected files:

```text
src/main/java/org/brokkr/enhancement/item/ModItems.java
src/main/resources/assets/brokkr/lang/en_us.json
src/main/resources/assets/brokkr/lang/ko_kr.json
```

Validation:

```text
./gradlew.bat build
```

### Phase 2: Enhancement Data

Goal:

- Store and read enhancement level from item stacks.

Design:

`EnhancementData` owns only low-level data behavior.

Responsibilities:

- Read enhancement level.
- Write enhancement level.
- Clamp to `0..20`.
- Treat missing data as `0`.
- Avoid weapon-type checks.

Public API shape:

```text
int getLevel(ItemStack stack)
void setLevel(ItemStack stack, int level)
int clampLevel(int level)
boolean hasEnhancement(ItemStack stack)
int increment(ItemStack stack)
```

Storage approach:

- Use `DataComponents.CUSTOM_DATA`.
- Use `CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> ...)` to write.
- Use `stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()` to read.
- Store the integer under key `brokkr:enhancement_level`.
- Remove or set the key to `0` when level is `0`, so unenhanced items stay clean.
- Keep callers unaware of the storage mechanism.

Concrete implementation shape:

```text
static final String LEVEL_KEY = Brokkr.MODID + ":enhancement_level"

int getLevel(ItemStack stack):
  CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
  CompoundTag tag = data.copyTag()
  return clampLevel(tag.getIntOr(LEVEL_KEY, 0))

void setLevel(ItemStack stack, int level):
  int clamped = clampLevel(level)
  CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
    if (clamped <= 0) tag.remove(LEVEL_KEY)
    else tag.putInt(LEVEL_KEY, clamped)
  })
```

After any level change, call the name refresh logic so the visible name stays in sync.

Validation:

- `/enhanceweapon set <level>` can later use this without knowing storage details.
- Build succeeds after adding the helper.

### Phase 3: Weapon Profiles

Goal:

- Keep enhancement rules extensible for future weapon families.

Core interface:

```text
EnhancedWeaponProfile
- String id()
- boolean supports(ItemStack stack)
- int maxLevel()
- float bonusDamage(int level)
- EnhancementTier tier(int level)
```

Initial implementation:

```text
SwordEnhancementProfile
```

Supported MVP items:

- `Items.WOODEN_SWORD`
- `Items.STONE_SWORD`
- `Items.IRON_SWORD`
- `Items.GOLDEN_SWORD`
- `Items.DIAMOND_SWORD`
- `Items.NETHERITE_SWORD`

Damage formula:

```text
bonusDamage = level * 0.2
```

Profile registry:

`WeaponEnhancementProfiles` stores registered profiles and exposes lookup:

```text
Optional<EnhancedWeaponProfile> find(ItemStack stack)
boolean isSupported(ItemStack stack)
```

Validation:

- Sword checks exist inside the sword profile only.
- Commands and events ask the profile registry instead of checking sword items directly.

### Phase 4: Enhancement Chance and Result Model

Goal:

- Centralize success-rate calculation and command result reporting.

`EnhancementChance`:

```text
int successChanceForCurrentLevel(int currentLevel)
boolean rollSuccess(int currentLevel, RandomSource random)
```

Chance table:

```text
+0 to +4:   100
+5 to +9:   70
+10 to +14: 40
+15 to +19: 10
```

`EnhancementResult` describes command outcomes without formatting messages directly.

Suggested result types:

```text
SUCCESS
FAILED_ROLL
NOT_SUPPORTED_WEAPON
NO_STONE
MAX_LEVEL
INVALID_LEVEL
```

Suggested result payload:

```text
EnhancementResult
- type
- previousLevel
- newLevel
- successChance
```

The result model must not contain `Component` instances. Convert results to messages in the command layer through `EnhancementTextKeys`.

Validation:

- Chance logic is unit-testable or at least isolated enough for simple manual checks.
- Message formatting is handled outside the result model.

### Phase 5: `/enhanceweapon` Command

Goal:

- Implement the MVP enhancement interaction.

Command behavior:

```text
/enhanceweapon
```

Rules:

- Check main hand item.
- Find matching weapon profile.
- Reject unsupported item without consuming a stone.
- Reject max-level weapon without consuming a stone.
- Find one `brokkr:enhancement_stone` anywhere in player inventory.
- Reject if no stone exists.
- Consume one stone for a valid attempt.
- Roll success chance from current level.
- On success, increase level by `1`.
- On failure, keep level unchanged.
- Send chat message using I18N keys.

Debug command:

```text
/enhanceweapon set <level>
```

Rules:

- Requires command permission.
- Checks held item profile.
- Accepts only integer levels from `0` to `20` through the command argument.
- Does not consume stones.
- Sends chat message using I18N keys.

Suggested command class:

```text
EnhanceWeaponCommand
```

Registration:

- Register during `RegisterCommandsEvent`.
- Keep command registration separate from `Test.java` except for event hook wiring.

Concrete API notes:

- Use `event.getDispatcher()`.
- Use `Commands.literal("enhanceweapon")`.
- Use `Commands.argument("level", IntegerArgumentType.integer(0, 20))`.
- Use `CommandSourceStack#getPlayerOrException()` to require a player source.
- Use `player.getMainHandItem()` for the right hand/main hand item.
- Iterate `Inventory#getContainerSize()`, check `Inventory#getItem(i)`, and `ItemStack#shrink(1)` the first matching stone stack.
- Send chat feedback with `player.sendSystemMessage(Component.translatable(...))`.

Inventory consumption helper:

```text
boolean consumeEnhancementStone(Player player):
  Inventory inventory = player.getInventory()
  for slot in 0..inventory.getContainerSize()-1:
    ItemStack stack = inventory.getItem(slot)
    if stack.is(ModItems.ENHANCEMENT_STONE.get()):
      stack.shrink(1)
      return true
  return false
```

Validation:

- No weapon in hand: localized not-weapon message.
- No stone: localized no-stone message.
- Max level: localized max-level message.
- Valid attempt at `+0`: always succeeds and consumes one stone.

### Phase 6: Tooltip and Item Name Display

Goal:

- Show enhancement level clearly through I18N.

Tooltip:

```text
Enhancement: +<level>
```

Rules:

- Show only when level is above `0`.
- Use tier color:
  - `+1..+5`: gray
  - `+6..+10`: aqua
  - `+11..+15`: light purple
  - `+16..+19`: red
  - `+20`: gold

Name display:

```text
(+<level>) <weapon name>
```

Rules:

- Show only when level is above `0`.
- Preserve vanilla item names after the prefix.
- Do not overwrite anvil custom names in MVP.
- Prefix format uses language key:

```text
item_name.brokkr.enhancement_prefix
```

Implementation note:

- There is no obvious simple NeoForge item-name formatting event in the local API.
- For MVP, update `DataComponents.ITEM_NAME` when enhancement level changes.
- Use `Component.translatable(stack.getItem().getDescriptionId())` as the base vanilla name before writing the enhanced display name.
- Do not repeatedly prefix an already-prefixed name. Always rebuild from the item base name and the current enhancement level.
- Remove `DataComponents.ITEM_NAME` when level becomes `0` if the item does not need a custom generated name.
- Do not overwrite `DataComponents.CUSTOM_NAME` in MVP. This avoids destroying anvil custom names.
- Known limitation: if a player has an anvil custom name, vanilla hover-name priority may hide the generated `ITEM_NAME` prefix. Strict `(+N)` before custom names is deferred.

Suggested service:

```text
EnhancementNameService.refreshName(ItemStack stack, int level):
  if level <= 0:
    stack.remove(DataComponents.ITEM_NAME)
    return

  Component baseName = Component.translatable(stack.getItem().getDescriptionId())
  stack.set(
    DataComponents.ITEM_NAME,
    Component.translatable("item_name.brokkr.enhancement_prefix", level, baseName)
  )
```

Validation:

- `+0` sword name is unchanged.
- `+1` sword name shows `(+1) Iron Sword`.
- Tooltip and prefix are localized through language files.
- Anvil custom-name behavior is documented if the prefix is not visible for custom-named items.

### Phase 7: Combat Damage Bonus

Goal:

- Apply the enhancement damage formula during combat.

Rules:

- Attacker must be a player.
- Main hand item must match a registered profile.
- Enhancement level must be above `0`.
- Bonus damage comes from the matched profile.

Formula for sword MVP:

```text
bonusDamage = level * 0.2
```

Implementation:

- Use `ItemAttributeModifierEvent`.
- Add an `Attributes.ATTACK_DAMAGE` modifier for `EquipmentSlotGroup.MAINHAND`.
- Use `AttributeModifier.Operation.ADD_VALUE`.
- Use a stable modifier id such as `brokkr:enhancement_attack_damage`.
- Keep event handler thin:
  - get event item stack
  - find profile
  - read level
  - add modifier

Concrete implementation shape:

```text
void addAttackDamage(ItemAttributeModifierEvent event):
  ItemStack stack = event.getItemStack()
  Optional<EnhancedWeaponProfile> profile = WeaponEnhancementProfiles.find(stack)
  int level = EnhancementData.getLevel(stack)
  if profile empty or level <= 0: return

  double bonus = profile.bonusDamage(level)
  event.addModifier(
    Attributes.ATTACK_DAMAGE,
    new AttributeModifier(
      Identifier.fromNamespaceAndPath(Brokkr.MODID, "enhancement_attack_damage"),
      bonus,
      AttributeModifier.Operation.ADD_VALUE
    ),
    EquipmentSlotGroup.MAINHAND
  )
```

Validation:

- Enhanced sword deals more damage than the same unenhanced sword.
- Item tooltip/stat display reflects the added attack damage.
- Non-supported items do not receive bonus damage.

### Phase 8: Hit Particles

Goal:

- Spawn tier-based particles when enhanced melee weapons hit a living entity.

Tier mapping:

```text
+1 to +5:   CRIT
+6 to +10:  CRIT, ENCHANT
+11 to +15: ENCHANT, WITCH
+16 to +19: CRIT, FLAME
+20:        TOTEM_OF_UNDYING, END_ROD
```

Rules:

- Spawn from `LivingIncomingDamageEvent`.
- Only spawn when `event.getSource().getEntity()` is a player.
- Only spawn when `event.getAmount() > 0`.
- Spawn from server-side event handling so nearby clients can see particles.
- Spawn around the target.
- Do not spawn idle held-item particles in MVP.
- Keep counts modest to avoid visual spam.

Suggested tier enum:

```text
EnhancementTier
- LOW
- MID
- HIGH
- VERY_HIGH
- MAX
```

Concrete implementation shape:

```text
void spawnHitParticles(LivingIncomingDamageEvent event):
  if event.getAmount() <= 0: return
  if event.getSource().getEntity() is not Player player: return

  ItemStack stack = player.getMainHandItem()
  Optional<EnhancedWeaponProfile> profile = WeaponEnhancementProfiles.find(stack)
  int level = EnhancementData.getLevel(stack)
  if profile empty or level <= 0: return

  LivingEntity target = event.getEntity()
  if target.level() is not ServerLevel serverLevel: return

  EnhancementTier tier = profile.tier(level)
  EnhancementParticles.spawn(serverLevel, target, tier)
```

Particle spawning helper:

```text
EnhancementParticles.spawn(ServerLevel level, LivingEntity target, EnhancementTier tier):
  level.sendParticles(type, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), count, spreadX, spreadY, spreadZ, speed)
```

Validation:

- `+1` produces a small visible effect.
- `+20` is visibly stronger than `+1`.
- Unenhanced swords do not spawn enhancement particles.

### Phase 9: I18N Resources

Goal:

- Make all player-facing text localizable.

Required `en_us.json` keys:

```json
{
  "item.brokkr.enhancement_stone": "Enhancement Stone",
  "tooltip.brokkr.enhancement_level": "Enhancement: +%s",
  "command.brokkr.enhance.success": "Enhancement succeeded: +%s -> +%s",
  "command.brokkr.enhance.failure": "Enhancement failed. The weapon remains at +%s.",
  "command.brokkr.enhance.failure.not_weapon": "Hold a supported weapon in your main hand.",
  "command.brokkr.enhance.failure.no_stone": "You need an Enhancement Stone.",
  "command.brokkr.enhance.failure.invalid_level": "Enhancement level must be between +0 and +20.",
  "command.brokkr.enhance.failure.max_level": "This weapon is already at maximum enhancement.",
  "command.brokkr.enhance.debug.set": "Set enhancement level to +%s.",
  "item_name.brokkr.enhancement_prefix": "(+%s) %s"
}
```

Required `ko_kr.json` keys:

```json
{
  "item.brokkr.enhancement_stone": "강화석",
  "tooltip.brokkr.enhancement_level": "강화: +%s",
  "command.brokkr.enhance.success": "강화 성공: +%s -> +%s",
  "command.brokkr.enhance.failure": "강화 실패. 무기는 +%s 상태를 유지합니다.",
  "command.brokkr.enhance.failure.not_weapon": "오른손에 지원되는 무기를 들어야 합니다.",
  "command.brokkr.enhance.failure.no_stone": "강화석이 필요합니다.",
  "command.brokkr.enhance.failure.invalid_level": "강화 수치는 +0부터 +20까지여야 합니다.",
  "command.brokkr.enhance.failure.max_level": "이 무기는 이미 최대 강화 상태입니다.",
  "command.brokkr.enhance.debug.set": "강화 수치를 +%s로 설정했습니다.",
  "item_name.brokkr.enhancement_prefix": "(+%s) %s"
}
```

`item_name.brokkr.enhancement_prefix` receives two arguments:

1. Enhancement level.
2. Base weapon name component.

Rules:

- Java code may contain translation keys.
- Java code should not contain user-facing English or Korean message bodies.
- Dynamic values use translation arguments.
- Remove unused `enhancement.test.*` keys unless a non-command enhancement UI is added later.

Validation:

- Language files are valid JSON.
- Build succeeds after resource changes.

## Suggested Delivery Order

1. Enhancement stone item and language files.
2. Enhancement data helper.
3. Weapon profile abstraction and sword profile.
4. Chance table and result model.
5. `/enhanceweapon set <level>` debug command.
6. `/enhanceweapon` real attempt command.
7. Tooltip display.
8. Item name prefix display.
9. Damage bonus event.
10. Hit particle event.
11. Final cleanup of sample code and build verification.

This order keeps each step buildable and testable. It also avoids implementing particles or display features before the data model exists.

Run this after every phase:

```text
./gradlew.bat build --no-daemon --no-configuration-cache
```

Do not start the next phase while the current phase fails to compile.

## Manual Test Checklist

- `./gradlew.bat build` succeeds.
- `brokkr:enhancement_stone` appears in the mod creative tab.
- `/enhanceweapon` with empty hand shows not-weapon message.
- `/enhanceweapon` with sword but no stone shows no-stone message.
- `/enhanceweapon` with sword and stone at `+0` consumes one stone and succeeds.
- `/enhanceweapon` at `+20` does not consume a stone.
- `/enhanceweapon set 10` sets held sword to `+10`.
- `+10` sword displays `(+10)` prefix.
- `+10` sword tooltip shows enhancement level.
- `+10` sword deals `+2.0` extra damage.
- Enhanced sword hit spawns particles.
- `+20` hit particles are visibly stronger than `+1`.
- Non-sword item cannot be enhanced.
- Player-facing messages come from language keys.

## Risks and Decisions to Verify

- Item name prefix for anvil-custom-named weapons is the only known MVP caveat.
- `DataComponents.ITEM_NAME` is acceptable for normal vanilla weapon names.
- `DataComponents.CUSTOM_DATA`, `CustomData`, `RegisterCommandsEvent`, `ItemTooltipEvent`, `ItemAttributeModifierEvent`, `LivingIncomingDamageEvent`, and the selected particle constants exist in the local API.
- If `DataComponents.ITEM_NAME` does not appear in the inventory as expected, keep tooltip support and move strict name prefixing to a follow-up task.
- If `ItemAttributeModifierEvent` fires too often, keep logic pure and allocation-light. Use a stable modifier id and avoid reading anything outside the stack/profile/data helpers.

If an API differs, keep the public helper/service shape intact and adapt only the event/storage implementation.

## Ready-to-Implement Criteria

This draft is ready for implementation when all of these are true:

- Each phase has a build command.
- Each new responsibility has an owning class.
- Data storage API is fixed.
- Command API is fixed.
- Inventory stone consumption algorithm is fixed.
- Damage bonus event is fixed.
- Particle event and particle constants are fixed.
- I18N keys and message bodies are listed.
- Known item-name caveat is documented.

Current status: ready for MVP implementation with the documented item-name caveat.
