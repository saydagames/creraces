package mc.sayda.creraces.fabric;

import mc.sayda.creraces.client.CreRacesClient;
import net.fabricmc.api.ClientModInitializer;

public class CreRacesFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CreRacesClient.init();
    }
}
