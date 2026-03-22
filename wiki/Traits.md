# Traits
Traits are modular components that provide specific behaviors to a race. Unlike [[Passives]], which are simple binary flags, Traits are objects with configurable parameters.

---

## 📂 Action Triggers

These traits execute [[Actions]] when specific events occur.

### `creraces:on_tick`
Executes actions repeatedly.
- `interval`: ([[Scaling-Values]]) Ticks between activations. Default: `20`.
- `actions`: (List) [[Actions]] to execute.
- `condition`: ([[Conditions]]) Optional requirement.

### `creraces:on_hit` / `creraces:on_kill`
Triggered when attacking or killing a living entity.
- `actions`: (List)
- `condition`: ([[Conditions]])

### `creraces:on_hurt` / `creraces:on_death` / `creraces:on_respawn`
Triggered by health-related events.
- `actions`: (List)
- `condition`: ([[Conditions]])

### `creraces:on_land`
Triggered when the player lands on the ground (after falling).
- `actions`: (List)
- `condition`: ([[Conditions]])

### `creraces:on_item_pickup`
Triggered when an item is added to the inventory.
- `actions`: (List)
- `condition`: ([[Conditions]])

### `creraces:on_ability_use`
Triggered when the player activates any ability.
- `actions`: (List)
- `condition`: ([[Conditions]])

---

## 🔄 Persistent Logic

### `creraces:permanent_effect`
Applies a mob effect while conditions are met.
- `effect`: (String) Mob effect ID.
- `amplifier`: ([[Scaling-Values]])
- `ambient` / `visible`: (Boolean)
- `condition`: ([[Conditions]])

### `creraces:continuous_effect`
Applies an effect while a condition is met, draining resources.
- `effect`: (String) Mob effect ID.
- `amplifier`: ([[Scaling-Values]])
- `resource`: (String) `MANA`, `ENERGY`, `GRIT`, `RAGE`, `SOULS`, `STACKS`, or `NONE`.
- `drain_rate`: ([[Scaling-Values]]) Per tick.
- `duration`: ([[Scaling-Values]]) Refresh interval.
- `on_fail`: (List) Actions if out of resources.

### `creraces:attribute_modifier`
Provides attribute bonuses.
- `attribute`: (String) Registry ID (e.g., `minecraft:generic.max_health`).
- `amount`: ([[Scaling-Values]])
- `operation`: `ADDITION`, `MULTIPLY_BASE`, `MULTIPLY_TOTAL`.
- `condition`: ([[Conditions]])

### `creraces:flight`
Allows player flight with resource consumption.
- `resource`: (String) Default: `NONE`. Resource type to drain (e.g., `MANA`). If `NONE`, flight is free.
- `drain_rate`: ([[Scaling-Values]]) Default: `0`. Amount to drain per tick. Use `0` for free flight.
- `force_fly`: (Boolean) Default: `false`.
- `soggy_wings`: (Boolean) Default: `false`. Disabled in rain/water if true.
- `condition`: ([[Conditions]])

---

## 🛠️ Interaction Rules

### `creraces:block_interaction` / `creraces:block_place`
Triggers actions when interacting with or placing a specific block.
- `block`: (String) ID or `#tag`.
- `actions`: (List)
- `condition`: ([[Conditions]])

### `creraces:item_interaction`
Triggers actions when right-clicking with an item.
- `item`: (String) ID.
- `actions`: (List)
- `consume`: (Boolean) If true, removes one item from stack.
- `condition`: ([[Conditions]])

### `creraces:aquatic_movement`
- `speed`: ([[Scaling-Values]])
- `neutral_buoyancy`: (Boolean) Prevents sinking in water.

### `creraces:food_multiplier` / `creraces:damage_multiplier`
- `multiplier`: ([[Scaling-Values]])

---

## 🎨 Cosmetics

### `creraces:addon`
Attaches a visual model from Twilight Lib.
- `addon_id`: (String)
- `tint`: (String) Optional Hex color.
- `permanent`: (Boolean) If true, cannot be removed in the mirror GUI.
- `config`: (String) Config group (default: `race_addons`).
- `condition`: ([[Conditions]])
