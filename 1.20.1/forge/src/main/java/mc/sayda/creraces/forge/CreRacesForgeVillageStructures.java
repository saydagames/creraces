package mc.sayda.creraces.forge;

import mc.sayda.creraces.village.VillageStructureInjector;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;

/**
 * ServerAboutToStartEvent fires on the Forge event bus (not the mod bus), so this registers
 * directly rather than via @Mod.EventBusSubscriber, mirroring CreRacesForgeVillagerTrades.
 */
public final class CreRacesForgeVillageStructures {
    private CreRacesForgeVillageStructures() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(
                (ServerAboutToStartEvent event) -> VillageStructureInjector.inject(event.getServer()));
    }
}
