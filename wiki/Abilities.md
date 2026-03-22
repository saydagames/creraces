# Abilities

Abilities are active or passive player-triggered skills. They are defined via JSON files in `data/creraces/abilities/`.

## Metadata Fields

- `name`: (Translatable) The display name of the ability.
- `description`: (Translatable) Flavor text and technical description.
- `type`: `"ACTIVE"` or `"PASSIVE"`.
  - `ACTIVE`: Triggered by a keybind (Ability 1, 2, etc.).
  - `PASSIVE`: Triggers automatically on activation (toggleable).
- `icon`: Texture ID (e.g., `minecraft:textures/item/diamond.png`).
- `cooldown`: (Integer) Time in ticks before the ability can be used again.
- `cost`: (Integer) Resource amount consumed on activation.
- `persistent`: (Boolean) If true, the ability state (e.g., toggled on) is saved across player deaths and server restarts.
- `race`: (ResourceLocation or Array) Restricts the ability to specific races.

## Logic Hooks

- `actions`: List of [[Actions]] to execute when the ability is **activated**. Actions are executed **sequentially**. If any action returns `false` (e.g., a targeted AOE hits nothing with `fail_if_empty: true`, or a raycast fails), subsequent actions are skipped, and the ability will not go on cooldown or consume resources.
- `on_deactivate`: List of [[Actions]] to execute when the ability is **deactivated** (for persistent/toggleable abilities).

## Example Ability
```json
{
  "name": "ability.creraces.foxfire.name",
  "description": "ability.creraces.foxfire.description",
  "type": "ACTIVE",
  "icon": "twilight:textures/items/fox_fire.png",
  "cooldown": 100,
  "cost": 10,
  "race": "creraces:kitsune",
  "actions": [
    {
      "type": "creraces:spawn_projectile",
      "projectile": "twilight:foxfire"
    }
  ]
}
```

## Remote Documentation
Abilities support remote documentation fetchers for in-game browsing:
- `wiki_page`: URL to a GitHub Wiki page.
- `remote_description`: Object defining the selector and URL for the short description.
- `remote_full_description`: Object defining the selector and URL for the full documentation page.
