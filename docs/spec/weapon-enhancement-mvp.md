# Weapon Enhancement MVP Spec

## Goal

Build the first playable version of a weapon enhancement mod.

The MVP focuses on existing swords first, but the implementation must be weapon-type agnostic. A player can apply an enhancement level to a supported weapon, see that level in the tooltip, gain a small combat bonus, and see a hit effect that becomes stronger as the level rises.

This version does not introduce custom weapon textures, new weapon models, advanced upgrade stations, or full balancing systems.

## Player Experience

The player uses an enhancement item on a supported weapon to increase its enhancement level.

Enhanced weapons keep their vanilla appearance, but they feel stronger through:

- Tooltip text showing the enhancement level.
- Item name prefix showing the enhancement level.
- Increased attack damage.
- Visual particles when hitting an entity.
- Stronger visual feedback at higher enhancement levels.

Example:

- `(+1) Iron Sword`
- `(+5) Diamond Sword`
- `(+20) Netherite Sword`

## MVP Scope

Included:

- Weapon enhancement level from `+0` to `+20`.
- One enhancement material item.
- Enhancement level stored directly on the weapon item stack.
- Tooltip display for enhanced weapons.
- Attack damage bonus based on enhancement level.
- Hit particles based on enhancement tier.
- Command-based enhancement attempt for MVP.
- Object design that allows additional weapon families without rewriting the enhancement system.

Excluded:

- Custom weapon textures.
- New weapon classes.
- Armor enhancement.
- Downgrade or destruction penalties.
- Protection charms.
- Dedicated enhancement block UI.
- Multiplayer economy or rarity systems.
- Direct right-click enhancement flow.

## Supported Items

MVP target items:

- `minecraft:wooden_sword`
- `minecraft:stone_sword`
- `minecraft:iron_sword`
- `minecraft:golden_sword`
- `minecraft:diamond_sword`
- `minecraft:netherite_sword`

Other weapons can be added after the sword system is stable. The code should still be structured around supported weapon profiles, not hardcoded sword-only logic.

## Enhancement Data

Each enhanced weapon stores an integer enhancement level.

Data key:

- `brokkr:enhancement_level`

Valid range:

- Minimum: `0`
- Maximum: `20`

Rules:

- Missing data means `0`.
- Values below `0` are treated as `0`.
- Values above `20` are treated as `20`.
- Only supported weapon item stacks should receive this data in normal gameplay.

## Weapon Type Design

The enhancement system should not directly check only for individual sword items throughout the codebase.

Use a small abstraction that describes what kind of weapon is being enhanced.

Suggested model:

```text
EnhancedWeaponProfile
- id
- supported item predicate or item tag
- max level
- damage bonus formula
- effect tier resolver
- allowed enhancement material
```

Initial profile:

```text
id: sword
supported items: vanilla sword items
max level: 20
damage bonus formula: melee flat damage
effect resolver: melee hit particles
```

Future profiles:

```text
id: axe
supported items: vanilla axe items
max level: 20
damage bonus formula: slower but heavier melee flat damage
effect resolver: melee hit particles
```

```text
id: trident
supported items: trident
max level: 20
damage bonus formula: melee and thrown damage
effect resolver: water/lightning themed particles
```

```text
id: bow
supported items: bow and crossbow
max level: 20
damage bonus formula: projectile damage
effect resolver: projectile trail or impact particles
```

Rules:

- `EnhancementData` should only know how to read, write, and clamp the level.
- `WeaponEnhancementService` should decide whether an item is supported by asking registered profiles.
- Combat event handlers should ask the matched profile how to calculate bonus damage.
- Particle handlers should ask the matched profile which effect tier to use.
- Sword-specific decisions should live inside the sword profile.
- Adding a new weapon family should usually mean adding one profile and one small effect implementation.

## Enhancement Material

Add one item:

- Registry id: `brokkr:enhancement_stone`
- Display name: `Enhancement Stone`

MVP behavior:

- Consumed when enhancement is attempted.
- Increases the weapon level by `1` on success.
- Does not increase the weapon level on failure.
- Does nothing if the weapon is already `+20`.

Initial acquisition:

- Creative tab only.

Recipe and loot integration are deferred until after MVP combat behavior is validated.

## Enhancement Flow

MVP enhancement attempts are command-based.

Command:

```text
/enhanceweapon
```

Attempt rules:

- The player must hold a supported weapon in the right hand/main hand.
- The player must have at least one Enhancement Stone anywhere in their inventory.
- The command consumes one Enhancement Stone when the attempt starts.
- If the roll succeeds, the held weapon gains `+1`.
- If the roll fails, the held weapon level does not change.
- If the weapon is already `+20`, no stone is consumed.
- If the held item is not a supported weapon, no stone is consumed.
- If the player has no Enhancement Stone, no attempt is made.
- The result is sent as a chat message.

Debug command:

```text
/enhanceweapon set <level>
```

Debug command rules:

- Sets the enhancement level of the held supported weapon.
- Does not consume Enhancement Stones.
- Should be limited to players with permission level suitable for commands.
- Exists only to speed up development, testing, and balancing.

## Enhancement Chance

Enhancement uses a tiered success chance based on the current enhancement level.

The current level means the weapon's level before the enhancement attempt.

```text
+0 to +4:   100% success
+5 to +9:   70% success
+10 to +14: 40% success
+15 to +19: 10% success
```

Rules:

- `+20` is the maximum level and cannot be enhanced further.
- On success, the weapon gains `+1`.
- On failure, the weapon level does not change.
- The enhancement stone is consumed on both success and failure.
- MVP failure has no downgrade, destruction, durability loss, or curse penalty.
- The minimum success chance is `10%`.

## Tooltip

Enhanced weapons show an extra tooltip line.

For `+0`:

- No extra line.

For `+1` and above:

- `Enhancement: +<level>`

Tooltip color by tier:

- `+1` to `+5`: gray
- `+6` to `+10`: aqua
- `+11` to `+15`: light purple
- `+16` to `+19`: red
- `+20`: gold

## Display Name

Enhanced weapons should show their enhancement level before the weapon name.

Format:

```text
(+<level>) <weapon name>
```

Examples:

- `(+1) Iron Sword`
- `(+10) Diamond Sword`
- `(+20) Netherite Sword`

Rules:

- `+0` weapons use the normal vanilla name.
- `+1` and above use the enhancement prefix.
- If the item has a custom anvil name, the enhancement prefix should still be shown before that custom name.
- The prefix format must be localizable through language keys.

## Message and I18N

All player-facing text must be managed through Minecraft language files.

Do not hardcode user-visible messages directly in command handlers, tooltip handlers, item use logic, or enhancement result logic.

Primary language file:

- `src/main/resources/assets/brokkr/lang/en_us.json`

Optional Korean language file:

- `src/main/resources/assets/brokkr/lang/ko_kr.json`

Required message key groups:

```text
item.brokkr.enhancement_stone
tooltip.brokkr.enhancement_level
command.brokkr.enhance.success
command.brokkr.enhance.failure.not_weapon
command.brokkr.enhance.failure.no_stone
command.brokkr.enhance.failure.invalid_level
command.brokkr.enhance.failure.max_level
command.brokkr.enhance.failure
command.brokkr.enhance.debug.set
item_name.brokkr.enhancement_prefix
```

Code should use translatable components:

```text
Component.translatable("tooltip.brokkr.enhancement_level", level)
```

Message rules:

- Tooltip text uses `tooltip.brokkr.*` keys.
- Command response text uses `command.brokkr.*` keys.
- Enhanced item name prefix text uses `item_name.brokkr.*` keys.
- Item display names use `item.brokkr.*` keys.
- Message keys should include the mod id namespace in the key path.
- Messages with dynamic values must use format arguments instead of string concatenation.

## Combat Bonus

Enhanced melee weapons gain flat bonus attack damage.

Formula:

```text
bonusDamage = enhancementLevel * 0.2
```

Each enhancement level increases attack damage by `+0.2`.

Examples:

- `+1`: `+0.2` damage
- `+10`: `+2.0` damage
- `+20`: `+4.0` damage

## Hit Effects

When an enhanced melee weapon hits a living entity, spawn particles around the target.

Effect tiers:

- `+1` to `+5`
  - Particle type: `CRIT`
  - Small number of crit-like particles.
  - Low intensity.

- `+6` to `+10`
  - Particle types: `CRIT`, `ENCHANT`
  - More particles.
  - Add subtle enchantment particles.

- `+11` to `+15`
  - Particle types: `ENCHANT`, `WITCH`
  - Larger burst.
  - Stronger colored particles.

- `+16` to `+19`
  - Particle types: `CRIT`, `FLAME`
  - Large burst.
  - Red or intense particles.
  - Optional stronger hit sound.

- `+20`
  - Particle types: `TOTEM_OF_UNDYING`, `END_ROD`
  - Distinct burst.
  - Gold or bright particles.
  - Optional stronger hit sound.

Rules:

- Effects are visual only in MVP.
- Effects should spawn server-side so nearby clients see them.
- Do not spawn idle particles while holding the weapon in MVP.

## Configuration

MVP can hardcode values.

Config should be introduced after the basic loop works:

- Maximum enhancement level.
- Damage bonus per level.
- Particle intensity.
- Whether debug command is enabled.

## Implementation Plan

1. Add enhancement level helper.
   - Read level from item stack.
   - Write level to item stack.
   - Clamp values to `0..20`.

2. Add weapon profile model.
   - Define supported weapon matching.
   - Define max level per profile.
   - Define bonus damage formula per profile.
   - Define hit effect tier per profile.
   - Register the initial sword profile.

3. Add tooltip display.
   - Listen for item tooltip event.
   - Add enhancement line for supported weapons above `+0`.
   - Use `Component.translatable` with tooltip language keys.

4. Add debug command.
   - Register `/enhanceweapon set <level>`.
   - Apply to main hand item if it matches a registered weapon profile.
   - Send success or failure message to the player through language keys.

5. Add enhancement attempt command.
   - Register `/enhanceweapon`.
   - Require a supported weapon in the player's main hand.
   - Require at least one Enhancement Stone in the player's inventory.
   - Block attempts at max level without consuming a stone.
   - Consume one stone when a valid attempt starts.
   - Roll success chance from the current enhancement level.
   - Increase held weapon level by `1` on success.
   - Keep held weapon level unchanged on failure.
   - Send success, failure, no-stone, not-weapon, and max-level feedback through language keys.

6. Add enhanced item name display.
   - Show `(+<level>) <weapon name>` for supported weapons above `+0`.
   - Keep `+0` names unchanged.
   - Use language keys for the prefix format.

7. Add attack damage bonus.
   - Hook into the living damage flow.
   - If attacker is a player and main hand item is an enhanced supported weapon, ask its profile for bonus damage.

8. Add hit particles.
   - Spawn profile-based tier particles around the target after a successful hit.
   - Use the MVP particle type mapping for each enhancement tier.

9. Add enhancement stone item.
   - Register item.
   - Add item language entries.
   - Add to mod creative tab.

10. Add I18N message resources.
   - Add all MVP message keys to `en_us.json`.
   - Add `ko_kr.json` if Korean support is included in MVP.
   - Keep command, tooltip, item, and enhancement-result messages grouped by key prefix.
   - Avoid hardcoded user-visible text in Java code.

## Acceptance Criteria

- `./gradlew.bat build` succeeds.
- A sword can receive enhancement level `+1` to `+20`.
- `/enhanceweapon` attempts to enhance the supported weapon in the player's main hand.
- `/enhanceweapon` requires at least one Enhancement Stone in the player's inventory.
- A valid enhancement attempt consumes one Enhancement Stone.
- Enhancement chance uses `100%`, `70%`, `40%`, and `10%` tier thresholds.
- Failed enhancement consumes the enhancement stone but does not lower or destroy the weapon.
- Tooltip shows the correct enhancement level.
- Enhanced weapon names display as `(+<level>) <weapon name>`.
- Tooltip, command responses, item names, and enhancement result messages use language keys.
- Java code does not hardcode player-facing text except translation keys.
- Non-sword items cannot be enhanced through the normal flow.
- Enhanced swords deal more damage than the same unenhanced sword.
- Hitting an entity with an enhanced sword spawns visible particles.
- Particle types follow the MVP tier mapping: `CRIT`, `ENCHANT`, `WITCH`, `FLAME`, `TOTEM_OF_UNDYING`, and `END_ROD`.
- `+20` has a clearly stronger hit effect than `+1`.
- No custom texture pack is required.
- Enhancement logic is routed through weapon profiles instead of scattered sword-only checks.

## Future Extensions

- Advanced success chance scaling.
- Failure penalties beyond stone consumption.
- Upgrade cost scaling by level.
- Dedicated enhancement table block.
- Protection charm item.
- Bow, crossbow, trident, axe support.
- Elemental enhancement paths.
- Configurable server-side balance.
- Data-generated recipes and loot tables.
