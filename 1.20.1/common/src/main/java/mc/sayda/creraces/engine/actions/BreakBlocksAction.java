package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class BreakBlocksAction implements ActionRegistry.RaceAction {

    private final ScalingValue radius;
    private final boolean dropItems;

    public BreakBlocksAction(ScalingValue radius, boolean dropItems) {
        this.radius = radius;
        this.dropItems = dropItems;
    }

    @SuppressWarnings("null")
    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        BlockPos center = player.blockPosition();
        int r = (int) radius.evaluate(player, target);
        int maxRadius = 10;
        if (maxRadius > 0)
            r = Math.min(r, maxRadius);
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (pos.equals(center))
                        continue;

                    float speed = player.level().getBlockState(pos).getDestroySpeed(player.level(), pos);
                    // destroySpeed == -1 means the block is unbreakable (bedrock, command blocks,
                    // etc.)
                    if (speed >= 0) {
                        player.level().destroyBlock(pos, dropItems, player);
                    }
                }
            }
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "break_blocks"), json -> {
            ScalingValue radius = ScalingValue.fromJson(json, "radius", 1.0);
            boolean dropItems = GsonHelper.getAsBoolean(json, "drop_items", true);
            return new BreakBlocksAction(radius, dropItems);
        });
    }
}
