package mc.sayda.creraces.ability;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * @param positioned when true the pattern cells must be placed at their exact q,r coordinates;
 *                   when false (default) the pattern is matched anywhere in the grid by shape alone.
 */
public record HexRecipe(ResourceLocation ability, int level, Map<HexPos, EssenceType> pattern, boolean positioned) {

    /** Shifts a grid map so the minimum (q,r) corner sits at (0,0). */
    public static Map<HexPos, EssenceType> normalize(Map<HexPos, EssenceType> raw) {
        if (raw.isEmpty()) return raw;
        int minQ = raw.keySet().stream().mapToInt(HexPos::q).min().orElse(0);
        int minR = raw.keySet().stream().mapToInt(HexPos::r).min().orElse(0);
        Map<HexPos, EssenceType> out = new HashMap<>();
        for (Map.Entry<HexPos, EssenceType> e : raw.entrySet()) {
            out.put(new HexPos(e.getKey().q() - minQ, e.getKey().r() - minR), e.getValue());
        }
        return out;
    }

    /** Matches against the raw grid (caller must not pre-normalize). */
    public boolean matches(Map<HexPos, EssenceType> grid) {
        return positioned ? pattern.equals(grid) : pattern.equals(normalize(grid));
    }
}
