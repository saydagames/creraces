package mc.sayda.creraces.worldgen;

import mc.sayda.creraces.CreRaces;
import net.minecraft.resources.ResourceLocation;
import terrablender.api.Regions;

public class TerraBlenderIntegration {

    public static void register() {
        Regions.register(new VeilwoodRegion(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "veilwood"), 5));
    }
}
