# Scaling Values

The Scaling system allows numeric parameters (damage, duration, radius, etc.) to dynamically change based on player or target attributes.

## Basic Formula
The engine calculates the final value using the following logic:
`Final Value = base + (Stat_1 * Factor_1) + (Stat_2 * Factor_2) + ...`

---

## JSON Formats

Many properties in the engine (damage, duration, radius) use the `ScalingValue` system. This allows values to change dynamically based on player attributes, resource levels, or target stats.

## 📥 Format

### 1. Simple Number (Double)
A fixed value.
```json
"radius": 5.0
```

### 2. Stat Key (String)
Inherits the value of the specified stat directly (factor = 1.0).
```json
"power": "ap"
```

### 3. Complex Object
Allows for base values, multipliers, and multiple scaling components.
```json
"damage": {
  "base": 5.0,
  "scales_with": "ap",
  "factor": 1.5,
  "scaling": {
    "max_hp": 0.1,
    "target_armor": -0.5
  }
}
```

### 4. Advanced Math & Bounding
You can apply mathematical operations and bounds to the final calculated value.
```json
"damage": {
  "base": 10.0,
  "scales_with": "ap",
  "math": "sqrt",
  "min": 1.0,
  "max": 50.0
}
```

- **`math`**: (Optional) Operation applied to the final result of the calculation.
  - `round`: Rounds to the nearest integer.
  - `floor`: Rounds down.
  - `ceil`: Rounds up.
  - `sqrt`: Square root.
  - `abs`: Absolute value.
- **`min`**: (Optional) Minimum allowed value. If the result is lower, it becomes this value.
- **`max`**: (Optional) Maximum allowed value. If the result is higher, it becomes this value.

---

## 📊 Available Stat Keys

### Core Attributes
- `ap`, `creraces:ability_power`: Magic offensive power.
- `ad`, `creraces:attack_damage`: Physical offensive power. Also provides a bonus to total final attack damage (default: 0.2% per point).
- `ah`, `haste`, `creraces:ability_haste`: Cooldown reduction (Capped at 40.0).
- `crit`, `cr`, `creraces:crit_rate`: Chance for critical effects.
- `pen`, `creraces:armor_penetration`: Ignores enemy defenses.

### Vanilla & Vitality
- `hp`, `health`: Current health.
- `max_hp`, `max_health`, `minecraft:generic.max_health`: Maximum health.
- `armor`, `minecraft:generic.armor`: Physical defense.
- `speed`, `movement`, `minecraft:generic.movement_speed`: Walk speed.

### Dynamic Resources (`var:`)
Check current resource levels:
- `var:mana`, `var:energy`, `var:grit`, `var:rage`, `var:karma`, `var:souls`, `var:stacks`, `var:coins`.

### Persistent States (`state:`)
Check the numeric value of a persistent state variable:
- `state:some_state`: Returns the current value of `creraces:some_state`.
- `state:slot`: Returns the state of the ability in the current slot (if applicable).

### Target Context (`target_`)
Prefix any key with `target_` to evaluate it for the entity being hit/targeted. This works for **ANY** valid stat key or attribute:
- `target_hp`: Current health of the enemy.
- `target_armor`: Enemy's defense value.
- `target_custom:some_var`: Enemy's persistent customization data.

### Persistent Customization (`custom:`)
Pull values from persistent player choices:
- `custom:strength_level`: Evaluates a numeric customization choice.

---

## 🛠️ Additional Scaling Components
Inside the complex object, you can add multiple scales using either the `scaling` map or the `scales` array.

- **`scaling` (Map)**: `{"stat_key": factor}`
- **`scales` (Array)**: `[{"stat": "key", "factor": 1.0}]`
- **Loose Keys**: Any additional key in the root of the scaling object (except `base`, `factor`, etc.) that contains a number is treated as a scaling component.

> [!TIP]
> **Attribute Fallback**: Any namespaced ID (e.g., `minecraft:generic.luck`) can be used as a stat key. If the engine doesn't recognize a shortcut, it will attempt to find a registered Attribute with that ID.
