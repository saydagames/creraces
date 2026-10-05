package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** "Right-clicks" a block as the caster, succeeding only if the block consumed the interaction. */
public class InteractBlockAction implements ActionRegistry.RaceAction {
    private final ScalingValue offsetX;
    private final ScalingValue offsetY;
    private final ScalingValue offsetZ;
    private final boolean useInteractPos;

    public InteractBlockAction(ScalingValue offsetX, ScalingValue offsetY, ScalingValue offsetZ,
            boolean useInteractPos) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.useInteractPos = useInteractPos;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "interact_block"), json -> new InteractBlockAction(
                ScalingValue.fromJson(json, "offset_x", 0.0),
                ScalingValue.fromJson(json, "offset_y", 0.0),
                ScalingValue.fromJson(json, "offset_z", 0.0),
                GsonHelper.getAsBoolean(json, "use_interact_pos", true)));
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (player.level().isClientSide()) {
            return true;
        }
        BlockPos base = (useInteractPos && interactPos != null) ? interactPos : player.blockPosition();
        BlockPos pos = base.offset(
                (int) offsetX.evaluate(player, target, slot, interactPos),
                (int) offsetY.evaluate(player, target, slot, interactPos),
                (int) offsetZ.evaluate(player, target, slot, interactPos));

        BlockState state = player.level().getBlockState(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        InteractionResult result = state.use(player.level(), player, InteractionHand.MAIN_HAND, hit);
        return result.consumesAction();
    }
}
