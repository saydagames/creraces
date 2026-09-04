package mc.sayda.creraces.fabric;

import mc.sayda.creraces.village.VillageStructureInjector;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * Fabric equivalent of CreRacesForgeVillageStructures - SERVER_STARTING fires with registries
 * loaded, before the server begins ticking, matching Forge's ServerAboutToStartEvent timing.
 */
public final class CreRacesFabricVillageStructures {
    private CreRacesFabricVillageStructures() {
    }

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTING.register(VillageStructureInjector::inject);
    }
}
