# Conditions

Conditions are logical tests that return `true` or `false`. They are used to control when [[Actions]] execute.

---

## 📂 Logical Operators

### `creraces:and` / `creraces:or`
Combines multiple conditions.
- `conditions`: (List) A list of nested [[Conditions]].

### `creraces:not`
Inverts a condition.
- `condition`: (Object) The [[Conditions]] to invert.

---

## 👤 Player State

### `creraces:morphed` / `creraces:spirit` / `creraces:flying`
Checks binary states.
- `value`: (Boolean) Expected state. Default: `true`.

### `creraces:sneaking` / `creraces:on_ground` / `creraces:is_burning`
- `value`: (Boolean)

### `creraces:in_water` / `creraces:exposed_to_rain` / `creraces:in_sunlight`
Environmental state checks.
- `value`: (Boolean) 
> [!NOTE]
> `exposed_to_rain` is smart and checks for micro-block shelter.

### `creraces:is_moving`
- `value`: (Boolean)
- `threshold`: ([[Scaling-Values]]) Movement speed required. Default: `0.1`.

### `creraces:attack_charged`
Checks if the player's attack cooldown is ready.
- `threshold`: ([[Scaling-Values]]) Value from 0.0 to 1.0. Default: `0.9`.

---

## 📊 Resources & Variables

### `creraces:resource_level`
Checks numerical player stats.
- `resource`: (String) `mana`, `energy`, `grit`, `rage`, `souls`, `karma`, `stacks`, `coins`, `health`, `food`, `air`, `custom:<key>`.
- `operator`: (String) `>=`, `<=`, `>`, `<`, `==`, `!=`.
- `value`: ([[Scaling-Values]])

### `creraces:state`
Checks a persistent state variable.
- `state`: (String) State ID or `slot`.
- `operator`: (String) `>=`, `<=`, `>`, `<`, `==`, `!=`. Default: `==`.
- `value`: ([[Scaling-Values]])

### `creraces:cooldown`
Checks current ability cooldowns.
- `id` / `ability`: (String) Ability ID.
- `operator`: (String)
- `value`: ([[Scaling-Values]])

### `creraces:entity_data`
Checks NBT/persistent data on the entity.
- `key`: (String) NBT key.
- `operator`: (String)
- `value`: ([[Scaling-Values]])
- `use_target`: (Boolean) Check the target entity instead of the player.

---

## 🌍 World & Environment

### `creraces:biome` / `creraces:dimension`
- `biome` / `tag`: (String) Biome ID or `#tag`.
- `value`: (String) (For dimension, e.g., `minecraft:the_nether`).

### `creraces:weather`
- `weather`: (String) `rain`, `thunder`, `clear`.

### `creraces:time` / `creraces:altitude`
- `min` / `max`: ([[Scaling-Values]])

### `creraces:is_block`
Checks for a specific block at an offset.
- `block`: (String) ID or `#tag`.
- `offset_x/y/z`: ([[Scaling-Values]])
- `use_interaction_pos`: (Boolean) Base offset on right-clicked block. Default: `true`.

---

## 🔍 Presence & Targets

### `creraces:has_effect`
- `effect`: (String) Mob effect ID.
- `amplifier`: ([[Scaling-Values]]) Minimum required level.
- `use_target`: (Boolean)

### `creraces:distance`
- `range`: ([[Scaling-Values]]) Max distance allowed.
- `x/y/z`: ([[Scaling-Values]]) Target coordinates.

### `creraces:has_entities`
- `radius`: ([[Scaling-Values]])
- `targets`: See [[Target-Filter]].

---

## ⚔️ Items & Equipment

### `creraces:wearing_armor` / `creraces:holding_item`
- `item` / `tag`: (String) Item ID or `#tag`.
- `slot`: (String) `head`, `chest`, `legs`, `feet`, `any`. (Armor only).
- `use_target`: (Boolean) (Holding item only).

### `creraces:item_interaction`
Returns `true` if the player is holding the specified item in either hand.
- `item`: (String) ID.

### `creraces:is_smeltable`
Returns `true` if the item in the player's main hand can be smelted in a furnace.
