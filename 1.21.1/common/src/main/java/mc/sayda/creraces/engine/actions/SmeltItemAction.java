package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;

import java.util.Optional;

public class SmeltItemAction implements ActionRegistry.RaceAction {

    private final int amount;

    private SmeltItemAction(int amount) {
        this.amount = amount;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        if (!(player instanceof ServerPlayer))
            return true;

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty())
            return true;

        Optional<net.minecraft.world.item.crafting.RecipeHolder<SmeltingRecipe>> recipe = player.level().getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new net.minecraft.world.item.crafting.SingleRecipeInput(stack), player.level());

        if (recipe.isPresent()) {
            ItemStack singleResult = recipe.get().value().getResultItem(player.level().registryAccess()).copy();
            int toSmelt = Math.min(amount, stack.getCount());
            stack.shrink(toSmelt);
            ItemStack result = singleResult.copyWithCount(
                    Math.min(singleResult.getCount() * toSmelt, 64));
            if (stack.isEmpty()) {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, result);
            } else {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
                player.getInventory().placeItemBackInInventory(result);
            }
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "smelt_item"), json -> {
            int amount = GsonHelper.getAsInt(json, "amount", 1);
            return new SmeltItemAction(amount);
        });
    }
}
