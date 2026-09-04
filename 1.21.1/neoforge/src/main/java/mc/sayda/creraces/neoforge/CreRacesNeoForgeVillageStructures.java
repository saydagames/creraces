package mc.sayda.creraces.neoforge;

import mc.sayda.creraces.village.VillageStructureInjector;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/**
 * ServerAboutToStartEvent fires on the game event bus rather than the mod bus, so this
 * registers directly, mirroring CreRacesNeoForgeVillagerTrades.
 */
public final class CreRacesNeoForgeVillageStructures {
    private CreRacesNeoForgeVillageStructures() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(
                (ServerAboutToStartEvent event) -> VillageStructureInjector.inject(event.getServer()));
    }
}
