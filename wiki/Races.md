# Races

Races are defined via JSON files in `data/creraces/races/`. The engine uses a **flat root-level architecture**.

## 🧩 Discovery Rule: Prefixed vs. Unprefixed

To maintain a clean architecture and avoid logic conflicts, the engine uses a simple discovery rule:

| Key Format | Purpose | Usage |
| :--- | :--- | :--- |
| **Prefixed** (`creraces:`) | **Metadata & Flags** | Identity, Stats, Behavioral Flags ([[Passives]]) |
| **Unprefixed** (no colon) | **Mechanic Categories** | Containers for modular components ([[Mechanic-Catalog]]) |

> [!IMPORTANT]
> Any root-level key **without** a colon (e.g., `passive`) is interpreted as a **Category** for modular mechanics. Using unprefixed keys for flags like `can_fly` is considered **incorrect** and can cause discovery conflicts.

---

## 1. Prefixed Metadata & Identity

### Identity & Branding
- `creraces:name`: (Translatable) Display name.
- `creraces:description`: (Translatable) Flavor text.
- `creraces:icon`: Texture path for the race icon (e.g., `minecraft:textures/item/diamond.png`).
- `creraces:portrait`: Background portrait for the selection screen.
- `creraces:splash`: Featured splash art.
- `creraces:bg_texture`: Background texture for the GUI.
- `creraces:name_texture`: Optional texture for the race's title text.

### Logic & Inheritance
- `creraces:parent_race`: Inherits all fields/mechanics from another race ID.
- `creraces:index`: (Double) Mapping index for sorting and internal ID stability.
- `creraces:difficulty`: Star rating (1-5).
- `creraces:gstate`: `"MALE"`, `"FEMALE"`, or `"BOTH"`.

### Special Identity Flags
- `creraces:is_spirit`: (Boolean) Uses the spirit realm mechanics.
- `creraces:is_tiny`: (Boolean) Logic for small-scale races.
- `creraces:stacks_affect_resource`: (Boolean) Stacks system influence.

---

## 2. Core Stats & Scaling
These define the baseline power and resource systems.

- `creraces:scale`: (Object/Double) See [[Scaling-Values]].
- `creraces:base_ap` / `base_ad` / `base_ah` / `base_cr`: Core Stats.
- `creraces:resource_type`: `MANA`, `ENERGY`, `GRIT`, `RAGE`, `SOULS`, `KARMA`, `STACKS`, `COINS`, `NONE`.
- `creraces:max_resource`: Capacity.

---

## 3. UI Customization (Dimensions)
Fine-tune where splash art and name textures appear in the selection screen.

- `creraces:splash_x/y/w/h`: Position and size of the splash art.
- `creraces:name_tex_x/y/w/h`: Position and size of the name texture.

---

## 4. Remote Documentation
Connects the in-game GUI to external resources or the Wiki.

- `creraces:wiki_page`: URL or Wiki slug.
- `creraces:remote_description`: Selector for external descriptions.
- `creraces:remote_passive`: Configuration for external passive docs.

---

## 5. Mechanic Categories
Any root-level key that **does not** contain a colon (`:`) is treated as a **Category**.

> [!IMPORTANT]
> Any root-level key **without** a colon (e.g., `passive`) is interpreted as a **Category** for modular mechanics. Using unprefixed keys for any component on this list is considered **incorrect** and will result in the engine failing to discover the attribute.

**Example**:
```json
{
  "creraces:name": "example.race",
  "creraces:can_fly": true,
  "passive_mechanics": [
    { "type": "creraces:food_multiplier", "multiplier": 2.0 }
  ],
  "starting_abilities": ["creraces:dash"]
}
```

View the available components in the [[Mechanic-Catalog]].

---

## Starting Configuration
- `creraces:starting_abilities`: List of Ability IDs.
- `creraces:starting_items`: List of Item IDs.
- `creraces:customization`: Configuration for UI (Tints/Models).
- `creraces:wiki_page`: Links a remote wiki page for browser integration.
