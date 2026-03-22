package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

public class RemoveBlockAction implements ActionRegistry.RaceAction {
    private final ScalingValue x;
    private final ScalingValue y;
    private final ScalingValue z;
    private final boolean useTarget;
    private final boolean useTargetBlock;

    public RemoveBlockAction(ScalingValue x, ScalingValue y, ScalingValue z, boolean useTarget, boolean useTargetBlock) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.useTarget = useTarget;
        this.useTargetBlock = useTargetBlock;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        
        BlockPos basePos = player.blockPosition();
        if (useTarget && target != null) {
            basePos = target.blockPosition();
        } else if (useTargetBlock && interactionPos != null) {
            basePos = interactionPos;
        }

        int ox = (int) x.evaluate(player, target);
        int oy = (int) y.evaluate(player, target);
        int oz = (int) z.evaluate(player, target);
        
        BlockPos finalPos = basePos.offset(ox, oy, oz);

        // Protection: only remove if it's not bedrock or other unbreakable stuff?
        // Actually, Rat Tunnels use this to remove their own blocks.
        if (player.level().getBlockState(finalPos).getDestroySpeed(player.level(), finalPos) >= 0) {
            player.level().setBlockAndUpdate(finalPos, Blocks.AIR.defaultBlockState());
            return true;
        }

        return false;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "remove_block"), json -> {
            ScalingValue x = ScalingValue.fromJson(json, "x", 0.0);
            ScalingValue y = ScalingValue.fromJson(json, "y", 0.0);
            ScalingValue z = ScalingValue.fromJson(json, "z", 0.0);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            boolean useTargetBlock = GsonHelper.getAsBoolean(json, "use_target_block", false);
            return new RemoveBlockAction(x, y, z, useTarget, useTargetBlock);
        });
    }
}
