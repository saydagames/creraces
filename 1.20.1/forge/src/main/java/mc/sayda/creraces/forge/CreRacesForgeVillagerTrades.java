package mc.sayda.creraces.forge;

import mc.sayda.creraces.registry.ModVillagerProfessions;
import mc.sayda.creraces.villager.GuildReceptionistTrades;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.village.VillagerTradesEvent;

/**
 * All 5 tier trades are registered at level 1 so a fresh Guild Receptionist accepts every
 * tier immediately, without needing to level up first. VillagerTradesEvent fires on the
 * Forge event bus (not the mod bus), so this registers directly rather than via
 * @Mod.EventBusSubscriber.
 */
public final class CreRacesForgeVillagerTrades {
    private CreRacesForgeVillagerTrades() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(CreRacesForgeVillagerTrades::onVillagerTrades);
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
