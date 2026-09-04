package mc.sayda.creraces.neoforge;

import mc.sayda.creraces.registry.ModVillagerProfessions;
import mc.sayda.creraces.villager.GuildReceptionistTrades;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

/**
 * All 5 tier trades are registered at level 1 so a fresh Guild Receptionist accepts every
 * tier immediately, without needing to level up first. VillagerTradesEvent fires on the game
 * event bus rather than the mod bus, so this registers directly.
 */
public final class CreRacesNeoForgeVillagerTrades {
    private CreRacesNeoForgeVillagerTrades() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(CreRacesNeoForgeVillagerTrades::onVillagerTrades);
    }

    private static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != ModVillagerProfessions.GUILD_RECEPTIONIST.get()) return;

        var offers = event.getTrades().get(1);
        if (offers == null) return;
        for (int tier = 1; tier <= 5; tier++) {
            for (var listing : GuildReceptionistTrades.buildOffers(tier)) {
                offers.add(listing);
            }
        }
    }
}
