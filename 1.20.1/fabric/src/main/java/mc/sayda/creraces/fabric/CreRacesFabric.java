package mc.sayda.creraces.fabric;

import mc.sayda.creraces.CreRaces;
import net.fabricmc.api.ModInitializer;

public class CreRacesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CreRaces.init();
    }
}
