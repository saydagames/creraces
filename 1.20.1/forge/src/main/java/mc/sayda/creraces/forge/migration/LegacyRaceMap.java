package mc.sayda.creraces.forge.migration;

import mc.sayda.creraces.CreRaces;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps CreRaces Classic's {@code IsRace} value (a double, e.g. 9.3 = Dryad) onto this rewrite's
 * race ResourceLocations. The rewrite's {@code creraces:index} field was deliberately kept 1:1
 * with Classic's IsRace scheme, so this table is a direct lookup, not a heuristic.
 *
 * Classic values with no rewrite equivalent (Dragonborn 3.1-3.4, Day/Night Fairy 5.1/5.2,
 * Axolotl 6.6, Elementalist 7, Golem 8, Pixie/Nixie 14.1/14.2, Slime 19, Goblin 20) are simply
 * absent from this map, callers should treat a missing lookup as "no migration for this player,
 * let them pick a race normally" rather than an error.
 */
public final class LegacyRaceMap {
    private static final double EPSILON = 0.001;

    private static final Map<Double, String> TABLE = new LinkedHashMap<>();
    static {
        TABLE.put(0.0, "human");
        TABLE.put(1.0, "undead");
        TABLE.put(2.0, "dwarf");
        TABLE.put(4.0, "harpy");
        TABLE.put(5.0, "fairy");
        TABLE.put(5.3, "spring_fairy");
        TABLE.put(5.4, "summer_fairy");
        TABLE.put(5.5, "autumn_fairy");
        TABLE.put(5.6, "winter_fairy");
        TABLE.put(6.0, "mermaid");
        TABLE.put(9.1, "oread");
        TABLE.put(9.2, "naiad");
        TABLE.put(9.3, "dryad");
        TABLE.put(9.4, "aurai");
        TABLE.put(10.0, "lycan");
        TABLE.put(11.0, "giant");
        TABLE.put(12.0, "elves");
        TABLE.put(12.1, "elf");
        TABLE.put(12.2, "velox");
        TABLE.put(13.0, "ratkin");
        TABLE.put(15.0, "troll");
        TABLE.put(16.0, "orc");
        TABLE.put(17.0, "kitsune");
    }

    private LegacyRaceMap() {}

    @Nullable
    public static ResourceLocation resolve(double isRace) {
        for (Map.Entry<Double, String> entry : TABLE.entrySet()) {
            if (Math.abs(entry.getKey() - isRace) < EPSILON) {
                return new ResourceLocation(CreRaces.MODID, entry.getValue());
            }
        }
        return null;
    }
}
