package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;

import java.util.Optional;

public class SmeltItemAction implements ActionRegistry.RaceAction {

    @Override
    public void execute(Player player, net.minecraft.world.entity.LivingEntity target,
            mc.sayda.creraces.ability.AbilitySlot slot) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return;

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty())
            return;

        Optional<SmeltingRecipe> recipe = player.level().getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SimpleContainer(stack), player.level());

        if (recipe.isPresent()) {
            ItemStack result = recipe.get().getResultItem(player.level().registryAccess()).copy();
            result.setCount(stack.getCount());
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, result);
        }
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "smelt_item"), json -> new SmeltItemAction());
    }
}
