package mc.sayda.creraces.villager;

import mc.sayda.creraces.item.QuestScrollItem;
import mc.sayda.creraces.registry.ModDataComponents;
import mc.sayda.creraces.registry.ModItems;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Shared, loader-agnostic reward table for the Guild Receptionist: trade a COMPLETED quest
 * scroll of tier N for TIER_DIME_REWARDS[N] Dimes (5/10/20/35/50 for tiers 1-5).
 *
 * 1.20.1 matched costs with a one-directional NBT subset check (MerchantOffer.isRequiredItem /
 * NbtUtils.compareNbt), so a plain cost stack carrying Tier/State matched a live scroll and its
 * extra tags (QuestId, OwnerUUID, Progress, ...) were ignored. 1.21 matches through ItemCost's
 * DataComponentPredicate instead, which compares a component for exact equality, so the tier
 * moved into its own QUEST_GRADE component rather than staying inside the varying CUSTOM_DATA.
 * It has to live in the cost and not in an overridden satisfiedBy: offers are serialised to the
 * client, which rebuilds plain MerchantOffers and would drop any subclass.
 */
public final class GuildReceptionistTrades {
    private GuildReceptionistTrades() {
    }

    private static final int[] TIER_DIME_REWARDS = {0, 5, 10, 20, 35, 50};

    public static VillagerTrades.ItemListing[] buildOffers(int tier) {
        ItemStack template = QuestScrollItem.createTemplate(tier, QuestScrollItem.State.COMPLETED);
        ItemStack reward = new ItemStack(ModItems.DIME.get(), TIER_DIME_REWARDS[tier]);

        DataComponentPredicate predicate = DataComponentPredicate.builder()
                .expect(ModDataComponents.QUEST_GRADE.get(), tier)
                .build();
        // ItemCost only serialises item/count/components and rebuilds its display stack from the
        // predicate, so the template is what the server shows and QUEST_GRADE is all that survives
        // to the client. QuestScrollItem.getTier/getState read that component back, which is what
        // keeps the tier number and the completed glint on the trade slot.
        ItemCost cost = new ItemCost(template.getItemHolder(), template.getCount(), predicate, template);

        return new VillagerTrades.ItemListing[]{
                (trader, random) -> new MerchantOffer(cost, reward, 999999, 0, 0.0f)
        };
    }
}
