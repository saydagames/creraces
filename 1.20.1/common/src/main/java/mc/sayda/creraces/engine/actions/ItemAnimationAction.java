package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/** Plays the item activation animation (as with a totem) for an item on the caster's screen. */
public class ItemAnimationAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "item_animation");

    private final ResourceLocation itemId;

    public ItemAnimationAction(ResourceLocation itemId) {
        this.itemId = itemId;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> new ItemAnimationAction(
                new ResourceLocation(GsonHelper.getAsString(json, "item", "minecraft:air"))));
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (player instanceof ServerPlayer serverPlayer) {
            BoundaryHandler.sendItemAnimation(serverPlayer, itemId);
        }
        return true;
    }
}
