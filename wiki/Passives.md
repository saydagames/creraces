# Passives

Passives (or "Behavioral Flags") are hardcoded engine properties intrinsic to a race. They are defined as **root-level** properties in the race JSON.

### 🛡️ Prefix Rule
To distinguish these flags from [[Mechanic]] categories, you **MUST** use the `creraces:` prefix for every flag listed on this page.

> [!IMPORTANT]
> Any root-level key **without** a colon (e.g., `passive`) is interpreted as a **Category** for modular mechanics. Using unprefixed keys for flags like `can_fly` is considered "Legacy" and can cause discovery conflicts.

---

## Behavioral Flags

### Breathing & Environmental
- `creraces:can_breathe_underwater`: (Boolean) Player cannot drown in water. Default: `false`.
- `creraces:can_breathe_on_land`: (Boolean) If false, the player drowns when out of water. Default: `true`.
- `creraces:burns_in_sunlight`: (Boolean) Player catches fire in direct sunlight. Default: `false`.
- `creraces:immune_to_damage`: (List) Damage source IDs (e.g. `["minecraft:fire", "minecraft:lava"]`). 
- `creraces:negate_effects`: (List) Mob effect IDs to ignore.

### Vision & Perception
- `creraces:night_vision`: (Boolean) Permanent night vision.
- `creraces:water_vision`: (Boolean) vision while submerged in water.
- `creraces:lava_vision`: (Boolean) vision while submerged in lava.

### Movement & Physics
- `creraces:can_fly`: (Boolean) Hardcoded creative flight toggle. **Note**: For complex flight (resource drain, environmental effects like soggy wings), use the modular `creraces:flight` mechanic in the [[Mechanic]].
- `creraces:liquid_speed_multiplier`: ([[Scaling-Values]]) Speed multiplier for swimming. Default: `1.0`.
- `creraces:unaffected_by_water`: (Boolean) No movement/mining penalty underwater.
- `creraces:unaffected_by_lava`: (Boolean) No movement/mining penalty in lava.
- `creraces:cannot_sprint`: (Boolean) Disables sprinting.

### Health & Regeneration
- `creraces:no_natural_regeneration`: (Boolean) Disables vanilla regeneration.
- `creraces:regeneration_multiplier`: ([[Scaling-Values]]) Scales natural healing amount. Default: `1.0`.

### Combat & Vitality
- `creraces:immune_to_knockback`: (Boolean) 100% resistance.
- `creraces:invulnerability_ticks_multiplier`: ([[Scaling-Values]]) Scales i-frames. Default: `1.0`.

> [!NOTE]
> Some damage types (like `creraces:ratvenom`) are tagged with `minecraft:no_knockback` and will skip knockback regardless of this passive. This behavior is natively supported in 1.21.1+ and handled via Mixin in 1.20.1.


### Food & Hunger
- `creraces:no_hunger`: (Boolean) Disables the hunger bar.
- `creraces:no_hunger_drain`: (Boolean) Hunger bar never decreases.
- `creraces:fixed_hunger`: ([[Scaling-Values]]) Locks hunger to a specific value. Default: `0.0`.
- `creraces:allowed_food_types`: (List) Explicitly allowed food groups or IDs. Format: `["meat", "minecraft:apple", "#minecraft:fishes"]`.
- `creraces:blocked_food_types`: (List) Explicitly forbidden food groups or IDs.
- `creraces:can_eat_when_full`: (Boolean) Allows eating even when saturation is maxed.

### Social & Interaction
- `creraces:hated_by_entities`: (List) Entities that attack the player on sight. Format: `["minecraft:zombie"]`.
- `creraces:respected_by_entities`: (List) Entities that ignore the player.
- `creraces:defended_by_entities`: (List) Entities that protect the player.
- `creraces:can_command_socials`: (Boolean) Allows Commanding Staff on respected mobs.

### Special Mechanics
- `creraces:spawn_on_death`: (Object)
  - `entity_type`: Entity ID.
  - `nbt`: NBT string. Default: `"{}"`.
  - `count`: Integer. Default: `1`.
