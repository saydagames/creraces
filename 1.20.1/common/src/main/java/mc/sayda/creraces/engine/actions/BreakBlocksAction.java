package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class BreakBlocksAction implements ActionRegistry.RaceAction {

    private final int radius;
    private final boolean dropItems;

    public BreakBlocksAction(int radius, boolean dropItems) {
        this.radius = radius;
        this.dropItems = dropItems;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        BlockPos center = player.blockPosition();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (pos.equals(center))
                        continue; // Don't break floor under player? Or maybe yes.

                    if (player.level().getBlockState(pos).getDestroySpeed(player.level(), pos) >= 0) {
                        player.level().destroyBlock(pos, dropItems, player);
                    }
                }
            }
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "break_blocks"), json -> {
            int radius = json.has("radius") ? json.get("radius").getAsInt() : 1;
            boolean dropItems = GsonHelper.getAsBoolean(json, "drop_items", true);
            return new BreakBlocksAction(radius, dropItems);
        });
    }
}
