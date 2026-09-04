package mc.sayda.creraces.fabric;

import net.fabricmc.api.DedicatedServerModInitializer;

public class CreRacesFabricServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        // Safe to call here: Fabric runs every "main" entrypoint (including TerraBlender's, which
        // loads TerraBlender.CONFIG) to completion before any "server" entrypoint starts.
        mc.sayda.creraces.worldgen.VeilwoodBiomeInjector.init();
    }
}
