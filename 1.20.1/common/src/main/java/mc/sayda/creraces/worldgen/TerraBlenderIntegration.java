package mc.sayda.creraces.worldgen;

import mc.sayda.creraces.CreRaces;
import net.minecraft.resources.ResourceLocation;
import terrablender.api.Regions;

public class TerraBlenderIntegration {

    // Matches TerraBlender's default vanilla region weight, making veilwood as common as mangrove.
    private static final int VEILWOOD_REGION_WEIGHT = 10;

    public static void register() {
        Regions.register(new VeilwoodRegion(
                new ResourceLocation(CreRaces.MODID, "veilwood"), VEILWOOD_REGION_WEIGHT));
    }
}
