package mc.sayda.creraces.villager;

import mc.sayda.creraces.item.QuestScrollItem;
import mc.sayda.creraces.registry.ModVillagerProfessions;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Vanilla only ever activates Villager.TRADES_PER_LEVEL (2) trades per villager level, even
 * though all 5 of our tier trades are registered at level 1 (see
 * CreRacesForgeVillagerTrades/CreRacesFabricVillagerTrades) - so a fresh Guild Receptionist
 * would still only ever show 2 random tiers at a time. Rather than mixin-patching
 * Villager.updateTrades() to change that constant, this appends any of our 5 tier offers a
 * Guild Receptionist is still missing directly via the public Merchant.getOffers() list.
 * Called from IncidentResolver's INTERACT_ENTITY hook, so the full set is guaranteed present
 * the instant a player opens trades, regardless of the villager's current level.
 *
 * Vanilla's own level-1 registration (see CreRacesForgeVillagerTrades/CreRacesFabricVillagerTrades)
 * lists all 5 tiers, but Villager.updateTrades() only ever activates TRADES_PER_LEVEL (2) of them,
 * chosen at random - so whichever 2 vanilla picks can land in any order at the front of the offer
 * list before this method appends the rest. Sorting by tier afterward is what actually guarantees
 * the 1-5 display order, not the registration order alone.
 */
public final class GuildReceptionistOfferSync {
    private GuildReceptionistOfferSync() {
    }

    public static void sync(Villager villager) {
        if (villager.getVillagerData().getProfession() != ModVillagerProfessions.GUILD_RECEPTIONIST.get()) return;

        MerchantOffers offers = villager.getOffers();
        for (int tier = 1; tier <= 5; tier++) {
            if (hasTierOffer(offers, tier)) continue;
            for (var listing : GuildReceptionistTrades.buildOffers(tier)) {
                MerchantOffer offer = listing.getOffer(villager, villager.getRandom());
                if (offer != null) offers.add(offer);
            }
        }
        offers.sort(java.util.Comparator.comparingInt(GuildReceptionistOfferSync::tierOf));
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
