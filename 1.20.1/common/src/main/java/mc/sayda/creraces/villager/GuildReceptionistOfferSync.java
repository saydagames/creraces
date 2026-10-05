package mc.sayda.creraces.villager;

import mc.sayda.creraces.item.QuestScrollItem;
import mc.sayda.creraces.registry.ModVillagerProfessions;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.Comparator;

/**
 * Vanilla activates only Villager.TRADES_PER_LEVEL (2) random trades per villager level, so although all five
 * tier trades are registered at level 1 (see CreRacesForgeVillagerTrades / CreRacesFabricVillagerTrades), a
 * Guild Receptionist would only ever show two of them. Instead of patching Villager.updateTrades(), sync()
 * appends whichever tier offers are missing and sorts the list by tier. IncidentResolver calls it from its
 * INTERACT_ENTITY hook, so the full, ordered set is there the moment a player opens trades.
 */
public final class GuildReceptionistOfferSync {
    private GuildReceptionistOfferSync() {
    }

    public static void sync(Villager villager) {
        if (villager.getVillagerData().getProfession() != ModVillagerProfessions.GUILD_RECEPTIONIST.get()) return;

        MerchantOffers offers = villager.getOffers();
        for (int tier = 1; tier <= GuildReceptionistTrades.MAX_TIER; tier++) {
            if (hasTierOffer(offers, tier)) continue;
            for (var listing : GuildReceptionistTrades.buildOffers(tier)) {
                MerchantOffer offer = listing.getOffer(villager, villager.getRandom());
                if (offer != null) offers.add(offer);
            }
        }
        offers.sort(Comparator.comparingInt(GuildReceptionistOfferSync::tierOf));
    }

    private static int tierOf(MerchantOffer offer) {
        ItemStack cost = offer.getBaseCostA();
        return cost.getItem() instanceof QuestScrollItem ? QuestScrollItem.getTier(cost) : Integer.MAX_VALUE;
    }

    private static boolean hasTierOffer(MerchantOffers offers, int tier) {
        for (MerchantOffer offer : offers) {
            ItemStack cost = offer.getBaseCostA();
            if (cost.getItem() instanceof QuestScrollItem
                    && QuestScrollItem.getTier(cost) == tier
                    && QuestScrollItem.getState(cost) == QuestScrollItem.State.COMPLETED) {
                return true;
            }
        }
        return false;
    }
}
