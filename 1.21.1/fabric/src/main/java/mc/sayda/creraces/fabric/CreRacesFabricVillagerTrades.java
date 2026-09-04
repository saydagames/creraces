package mc.sayda.creraces.fabric;

import mc.sayda.creraces.registry.ModVillagerProfessions;
import mc.sayda.creraces.villager.GuildReceptionistTrades;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;

/**
 * All 5 tier trades are registered at level 1, mirroring CreRacesNeoForgeVillagerTrades, so a
 * fresh Guild Receptionist accepts every tier immediately without needing to level up first.
 */
public final class CreRacesFabricVillagerTrades {
    private CreRacesFabricVillagerTrades() {
    }

    public static void init() {
        TradeOfferHelper.registerVillagerOffers(ModVillagerProfessions.GUILD_RECEPTIONIST.get(), 1,
                factories -> {
                    for (int tier = 1; tier <= 5; tier++) {
                        for (var listing : GuildReceptionistTrades.buildOffers(tier)) {
                            factories.add(listing::getOffer);
                        }
                    }
                });
    }
}
