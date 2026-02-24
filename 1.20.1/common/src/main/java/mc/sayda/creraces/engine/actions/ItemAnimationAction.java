package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Action that triggers the visual item activation animation on the client.
 */
public class ItemAnimationAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = new ResourceLocation("creraces", "item_animation");

    private final ResourceLocation itemId;

    public ItemAnimationAction(ResourceLocation itemId) {
        this.itemId = itemId;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            String itemStr = GsonHelper.getAsString(json, "item");
            ResourceLocation item = new ResourceLocation(itemStr);
            return new ItemAnimationAction(item);
        });
    }

    @Override
    public boolean execute(@Nonnull Player p, @Nullable net.minecraft.world.entity.LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @Nullable net.minecraft.core.BlockPos interactionPos) {
        if (p instanceof ServerPlayer serverPlayer) {
            BoundaryHandler.sendItemAnimation(serverPlayer, itemId);
        }
        return true;
    }
}
