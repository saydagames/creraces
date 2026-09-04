package mc.sayda.creraces.worldgen;

import dev.architectury.platform.Platform;
import mc.sayda.creraces.config.CreRacesConfig;

public class VeilwoodBiomeInjector {

    public static boolean isEnabled() {
        return CreRacesConfig.VEILWOOD_FOREST_ENABLED.get();
    }

    public static void init() {
        if (!isEnabled()) return;
        if (Platform.isModLoaded("terrablender")) {
            TerraBlenderIntegration.register();
        }
    }
}
