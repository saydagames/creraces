package mc.sayda.creraces.neoforge.migration;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps CreRaces Classic's {@code IsRace} value onto this rewrite's future race ResourceLocations.
 * Mirrors {@code mc.sayda.creraces.forge.migration.LegacyRaceMap} from the 1.20.1 module, kept
 * ready for when 1.21.1 gets its own race system; unused until then. See that file for the full
 * rationale (the "no rewrite equivalent" Classic values are deliberately absent from this table).
 */
public final class LegacyRaceMap {
    private static final String MODID = "creraces";
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
                return ResourceLocation.fromNamespaceAndPath(MODID, entry.getValue());
            }
        }
        return null;
    }
}
