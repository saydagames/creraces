package mc.sayda.creraces.fabric;

import mc.sayda.creraces.village.VillageStructureInjector;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * Fabric equivalent of CreRacesNeoForgeVillageStructures - SERVER_STARTING fires with registries
 * loaded, before the server begins ticking, matching NeoForge's ServerAboutToStartEvent timing.
 */
public final class CreRacesFabricVillageStructures {
    private CreRacesFabricVillageStructures() {
    }

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTING.register(VillageStructureInjector::inject);
    }
}
