
package mc.sayda.creraces.fabric;

import mc.sayda.creraces.client.CreRacesClient;
import net.fabricmc.api.ClientModInitializer;

public class CreRacesFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CreRacesClient.init();
        // MenuGUIScreen is registered via MenuRegistry.registerScreenFactory inside
        // CreRacesClient.init() -> CLIENT_SETUP. Architectury delegates this call to
        // MenuScreens.register on Fabric, so a separate direct call here caused a
        // duplicate registration crash (IllegalStateException: Duplicate registration
        // for creraces:menu_gui).
    }
}
