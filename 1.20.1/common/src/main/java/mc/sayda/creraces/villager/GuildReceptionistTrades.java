package mc.sayda.creraces.villager;

import mc.sayda.creraces.item.QuestScrollItem;
import mc.sayda.creraces.registry.ModItems;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Shared, loader-agnostic reward table for the Guild Receptionist: trade a COMPLETED quest
 * scroll of tier N for TIER_DIME_REWARDS[N] Dimes (5/10/20/35/50 for tiers 1-5). Vanilla's
 * MerchantOffer cost-matching (ItemStack.isSameItem + a one-directional NBT subset check, see
 * MerchantOffer.isRequiredItem/NbtUtils.compareNbt) only requires the cost stack's tags
 * (Tier/State) to be present and equal on the player's stack - extra tags on a real scroll
 * (QuestId, owner, progress) are ignored, so a plain MerchantOffer works with no custom
 * matching code.
 */
public final class GuildReceptionistTrades {
    private GuildReceptionistTrades() {
    }

    private static final int[] TIER_DIME_REWARDS = {0, 5, 10, 20, 35, 50};

    public static VillagerTrades.ItemListing[] buildOffers(int tier) {
        ItemStack cost = QuestScrollItem.createTemplate(tier, QuestScrollItem.State.COMPLETED);
        ItemStack reward = new ItemStack(ModItems.DIME.get(), TIER_DIME_REWARDS[tier]);
        return new VillagerTrades.ItemListing[]{
                (trader, random) -> new MerchantOffer(cost, reward, 999999, 0, 0.0f)
        };
    }
}
