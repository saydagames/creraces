package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;

import javax.annotation.Nullable;
import java.util.Optional;

/** Instantly smelts up to "amount" items from the caster's main hand using the furnace recipes. */
public class SmeltItemAction implements ActionRegistry.RaceAction {

    private final int amount;

    private SmeltItemAction(int amount) {
        this.amount = amount;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (!(player instanceof ServerPlayer)) {
            return true;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return true;
        }

        Optional<RecipeHolder<SmeltingRecipe>> recipe = player.level().getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), player.level());
        if (recipe.isEmpty()) {
            return true;
        }
        ItemStack singleResult = recipe.get().value().getResultItem(player.level().registryAccess()).copy();
        int toSmelt = Math.min(amount, stack.getCount());
        stack.shrink(toSmelt);

        int remaining = singleResult.getCount() * toSmelt;
        int maxStack = singleResult.getMaxStackSize();
        if (stack.isEmpty()) {
            int inHand = Math.min(remaining, maxStack);
            player.setItemInHand(InteractionHand.MAIN_HAND, singleResult.copyWithCount(inHand));
            remaining -= inHand;
        } else {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        }
        // Handed over one stack at a time so whatever does not fit is dropped as normal-sized stacks.
        while (remaining > 0) {
            int count = Math.min(remaining, maxStack);
            player.getInventory().placeItemBackInInventory(singleResult.copyWithCount(count));
            remaining -= count;
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "smelt_item"),
                json -> new SmeltItemAction(GsonHelper.getAsInt(json, "amount", 1)));
    }
}
