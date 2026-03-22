# Actions

Actions are the "verbs" of the engine—they perform changes to the world, the player, or entities. They are primarily used inside [[Mechanic-Catalog]] components like `on_tick` or `on_hurt`.

## ⚙️ Generic Parameters
Every action supports these optional parameters:
- `chance`: ([[Scaling-Values]]) Probability of execution (0.0 to 1.0). Default: `1.0`.

---

## 📂 Logic & Timing

### `creraces:delay`
Delays execution of child actions.
- `ticks`: ([[Scaling-Values]]) Amount of time to wait.
- `actions`: (List) [[Actions]] to execute after the delay.

### `creraces:conditional`
Executes different actions based on a condition.
- `condition`: ([[Conditions]]) The test to perform.
- `if_true`: (List) [[Actions]] to execute if successful.
- `if_false`: (List) [[Actions]] to execute if failed.

### `creraces:cancel`
Aborts the current action pipeline. Useful inside a `creraces:conditional` to stop further effects.

### `creraces:toggle_state` / `creraces:set_state`
Modifies persistent variable states (often used for abilities).
- `state`: (String) State ID or `slot`.
- `value`: ([[Scaling-Values]]) Target value.
- `operation`: (String) `set`, `add`. Default: `set`.
- `on_value` / `off_value`: (Scaling) Used by `toggle_state` to determine which value to flip to.

### `creraces:bind`
Forcefully binds an ability to a slot, even if the player hasn't learned it.
- `slot`: (String) `a1`, `a2`, `a3`, `a4`, `a5`.
- `ability`: (String) The ability ID to bind.
- `save_to`: (String) Optional customization key to save the *previous* ability ID in.

### `creraces:unbind`
Clears an ability from a slot or restores a previously saved one.
- `slot`: (String) `a1`, `a2`, `a3`, `a4`, `a5`.
- `restore_from`: (String) Optional customization key to restore the ability ID from. The key is automatically removed after restoration.

---

## 🎨 Visual & Audio

### `creraces:spawn_particles`
Spawns visual effects.
- `particle`: (String) Particle ID.
- `count`: ([[Scaling-Values]])
- `dx/dy/dz`: ([[Scaling-Values]]) Velocity or offset depending on particle.
- `spread`: ([[Scaling-Values]]) Fallback for dx/dy/dz if not specified.
- `speed`: ([[Scaling-Values]])
- `targets`: (List) Defaults to `["enemies", "self"]`. See [[Target-Filter]].

### `creraces:play_sound` / `creraces:stop_sound`
- `sound`: (String) Sound ID.
- `source`: (String) `master`, `music`, `record`, `weather`, `block`, `hostile`, `neutral`, `player`, `ambient`, `voice`.
- `volume`: ([[Scaling-Values]]) Default: `1.0`.
- `pitch`: ([[Scaling-Values]]) Default: `1.0`.

### `creraces:beam`
Visual line effect between player and targets.
- `duration`: ([[Scaling-Values]]) Ticks.
- `radius`: ([[Scaling-Values]]) Width of the beam.
- `drain`: ([[Scaling-Values]]) Amount to subtract from `resource` per tick.
- `resource`: (String) `mana`, `energy`, etc.
- `color`: (String) Hex color (e.g., `#FF0000`).
- `actions`: (List) Actions to perform on entities hit by the beam.

### `creraces:tether`
Creates a persistent visual link.
- `duration`: ([[Scaling-Values]]) Ticks.
- `distance`: ([[Scaling-Values]]) Max length before breaking.
- `interval`: (Int) Ticks between `interval_actions`.
- `interval_actions`: (List) Actions performed while tethered.
- `break_actions` / `complete_actions`: (List) Triggered when tether ends.

### `creraces:item_animation`
Triggers a visual item use animation (mainhand/offhand).
- `hand`: (String) `mainhand`, `offhand`.

---

## ⚔️ Combat & Movement

### `creraces:apply_effect` / `creraces:remove_effect`
- `effect`: (String) Mob effect ID. (Single effect mode)
- `effects`: (List) List of objects containing `effect`, `duration`, and `amplifier`. (Plural effect mode)
- `duration`: ([[Scaling-Values]]) Ticks.
- `amplifier`: ([[Scaling-Values]])
- `ambient` / `visible`: (Boolean)
- `radius`: ([[Scaling-Values]]) AoE radius.
- `targets`: See [[Target-Filter]].
- `increment_amplifier`: (Boolean) If true, adds the new amplifier to the existing one.

### `creraces:dash`
- `power`: ([[Scaling-Values]]) Strength of the dash.
- `direction`: (String) `forward`, `backward`, `look`, `up`, `down`.
- `y_multiplier`: ([[Scaling-Values]]) Multiplier for Y component in horizontal dashes.
- `y_boost`: ([[Scaling-Values]]) Additive Y boost.
- `reset_fall`: (Boolean) Resets fall distance if true.

### `creraces:apply_velocity`
- `x` / `y` / `z`: ([[Scaling-Values]]) Velocity components to apply. Used if `mode` is not `push`/`pull`.
- `mode`: (String) `push` (away from player), `pull` (towards player), `default`. Default: `default`.
- `strength`: ([[Scaling-Values]]) Intensity factor for `push`/`pull`. Default: `1.0`.
- `relative`: (Boolean) If true, `x` is applied as forward velocity relative to looking direction. Default: `false`.
- `absolute`: (Boolean) If true, sets velocity instead of adding it. Default: `false`.
- `use_target`: (Boolean) If true, applies to the target entity instead of the player. Default: `true`.

### `creraces:damage` / `creraces:heal`
- `amount`: ([[Scaling-Values]])
- `damage_type`: (String) `minecraft:magic`, `minecraft:fireworks`, etc.
- `targets`: See [[Target-Filter]].

### `creraces:launch_projectile`
- `projectile`: (String) Entity ID (e.g., `minecraft:arrow`).
- `damage`: ([[Scaling-Values]])
- `speed`: ([[Scaling-Values]])
- `inaccuracy`: ([[Scaling-Values]])

### `creraces:disable_shield`
Disables a player's shield for a short duration.
- `duration`: ([[Scaling-Values]]) Instant break if not set.

### `creraces:set_on_fire`
- `duration`: ([[Scaling-Values]]) Seconds.

---

## 📦 Items & Blocks

### `creraces:modify_resource`
- `resource`: (String) `mana`, `energy`, `grit`, `rage`, `souls`, `karma`, `stacks`, `coins`, `health`, `food`, `air`, `custom:<key>`.
- `value`: ([[Scaling-Values]])
- `operation`: (String) `add`, `set`.

### `creraces:give_item` / `creraces:drop_item` / `creraces:consume_item`
- `item`: (String) Item ID.
- `amount`: ([[Scaling-Values]])

### `creraces:steal_item`
- `chance`: ([[Scaling-Values]]) Success rate (0-1).
- `slot`: (String) `mainhand`, `offhand`, `random`.

### `creraces:smelt_item`
Instantly smelts the item in the player's main hand if a recipe exists.

### `creraces:place_block` / `creraces:break_blocks`
- `block`: (String) ID.
- `radius`: ([[Scaling-Values]]) (For break)
- `offset_x/y/z`: (Scaling)
- `overwrite`: (Boolean) (For place)

### `creraces:remove_block`
Instantly removes a block at a relative offset without dropping items.
- `x/y/z`: ([[Scaling-Values]]) Relative offset.
- `use_target`: (Boolean) Offset from target instead of player.
- `use_target_block`: (Boolean) Offset from interact_pos (e.g. clicked block).

---

## 🗺️ World & Dimension

### `creraces:summon_entity` / `creraces:mass_summon`
- `entity`: (String) ID.
- `count` / `min_count` / `max_count`: ([[Scaling-Values]])
- `range`: ([[Scaling-Values]]) Max horizontal randomization radius. (Default: `0.0` for `summon_entity`, `6.0` for `mass_summon`).
- `use_raycast`: (Boolean) Places entity at block surface player is looking at. (Default: `false`).
- `ray_range`: ([[Scaling-Values]]) Max distance for raycast. (Default: `10.0`).
- `use_target`: (Boolean) Spawns at target entity position if not raycasting. (Default: `false`).
- `offset_y`: ([[Scaling-Values]]) Additional vertical offset. (Default: `0.0`).
- `pool`: (List) Weighted list of entities for `mass_summon`.
- `tame`: (Boolean) Tames the entity to the player.


### `creraces:enter_pocket` / `creraces:expand_pocket`
Manages racial pocket dimensions (e.g., for the Dryad).
- `dimension` / `structure`: (ResourceLocation)
- `cost`: ([[Scaling-Values]]) (For expand)
- `limit`: ([[Scaling-Values]]) (Max expansions)
- `faces`: (Mapping) Defines expansion structures per direction.

### `creraces:enter_spirit_realm`
Toggles a player's ethereal/spirit state.
- `radius`: ([[Scaling-Values]]) If set, pulls nearby entities into the realm too.

### `creraces:sleep`
Forcibly puts the player to sleep at their current position.
- `set_spawn`: (Boolean) Update respawn point.

---

## 🛠️ Utility & Meta

### `creraces:command`
- `command`: (String) Supports `@s` (self UUID) and `@t` (target UUID).
- `as_op`: (Boolean) Runs with op bypass.
- `at_entity`: (Boolean) Runs at player's location.

### `creraces:message`
- `text`: (String) Supports `&` color codes and translation keys.
- `actionbar`: (Boolean) Show above hotbar instead of chat.

### `creraces:open_gui`
- `gui`: (String) `crafting`, `enderchest`, `inventory`, `race_selection`, `skill_wheel`, `team_menu`, `mirror`.
- `radius`: (Scaling) For block-based UIs like `chest` or `anvil`.

### `creraces:modify_entity_data` / `creraces:set_customization`
- `key`: (String) NBT or Customization key.
- `value`: (Scaling or String) Value to set.
- `operation`: `SET`, `ADD`, `REMOVE`, `MULTIPLY`.

---
*For logic control, see [[Conditions]].*

