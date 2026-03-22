# Customization & Integration

The Customization system connects static player choices (from the Mirror GUI) and persistent script data to the engine's logic.

## Placeholders

Placeholders allow you to inject player-defined values into text or resource paths. They are primarily used in **Race Metadata**, **Addons**, and **Morphs**.

### Usage
Wrap the customization key in curly braces: `{key}`.

**Example (Morph based on color choice):**
```json
{
  "type": "creraces:morph",
  "entity_type": "twilight_lib:{color}_fox"
}
```
If the player's `color` customization is set to `white`, the engine resolves this to `twilight_lib:white_fox`.

---

## Integration with Logic

The `custom:` prefix allows scaling and conditions to interact with persistent player data.

### 1. Scaling Logic
Reference a custom numeric value in a [[Scaling-Values]] block:
```json
"power": {
  "base": 0.0,
  "scales_with": "custom:stored_energy",
  "factor": 1.0
}
```

### 2. Condition Logic
Check the current value of a customization key:
```json
{
  "type": "creraces:customization_equals",
  "id": "color",
  "values": ["red", "blue"]
}
```

### 3. Modifying Data
Use the `creraces:set_customization` action to save data mid-game. This is often used for:
- Saving "Home" coordinates (`mode: "POS_X"`, etc.).
- Tracking quest progress or unique resource counters.
- Updating visual states dynamically.

---

## Special Variables

While most customization keys are defined in your race's `customization` list, the engine also uses:
- `gstate`: Forced Gstate (Gender State) (`0` for Male, `1` for Female).
- `race`: The ID of the currently active race.

---

## Global Defaults (`race_defaults`)

You can define default values for customizations that vary by race using the `race_defaults` object at the **top level** of a race JSON.

**Example (fairy.json):**
```json
{
  "race_defaults": {
    "creraces:spring_fairy": {
      "fairy_color": "#FF99FF"
    },
    "creraces:summer_fairy": {
      "fairy_color": "#FFFF99"
    }
  }
}
```
When a player selects `creraces:spring_fairy`, the engine automatically sets their `fairy_color` customization to `#FF99FF` unless they have already manually customized it.

