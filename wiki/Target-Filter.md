# Target Filter

Target Filters allow you to precicely define who is affected by an [[Actions|Action]] or [[Mechanic-Catalog|Mechanic]]. They are used in AoE actions, auras, and entity conditions.

## 📥 Format
Filters are defined as a list of strings within a `targets` key.

```json
"targets": ["enemies", "!self"]
```

## 🔍 Logic
Categories are evaluated in two steps:
1. **Deny Rules**: Any category prefixed with `!` is immediately blocked.
2. **Allow Rules**: If not denied, the entity must belong to at least one allowed category.

### Default Behavior
If the `targets` key is missing, the engine uses **Context-Aware Defaults** to balance safety and convenience:
- **Single-Target Buffs & Visuals**: `["enemies", "self"]` (e.g., `apply_effect` with radius 0, `modify_resource`, `spawn_particles`).
- **AOE & Damage Actions**: `["enemies"]` (e.g., `aoe`, `damage`, `beam`, `apply_effect` with radius > 0).

---

## 🏷️ Categories

| Category | Description |
| :--- | :--- |
| `all` | Every living entity. |
| `self` | The player who triggered the effect. |
| `allies` | Entities on the same team (or the player themselves). |
| `enemies` | Entities that can be hurt by the player (includes neutral mobs). |
| `players` | Only players. |
| `mobs` | Only non-player living entities. |

### Examples

**Damage Everyone Except Allies**:
`["all", "!allies"]`

**Friendly Healing (including self)**:
`["allies"]`

**Enemies Only (no self-damage)**:
`["enemies"]`

**Enemy Players Only**:
`["players", "!allies"]`

---
> [!NOTE]
> Allied status is determined by the `RaceTeamManager`. Generally, vanilla teams and specific race-based alliances are respected.
