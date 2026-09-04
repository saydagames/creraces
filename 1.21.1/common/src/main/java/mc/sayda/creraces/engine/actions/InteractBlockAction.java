package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
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

public class InteractBlockAction implements ActionRegistry.RaceAction {
    private final ScalingValue ox, oy, oz;
    private final boolean useInteractPos;

    public InteractBlockAction(ScalingValue ox, ScalingValue oy, ScalingValue oz, boolean useInteractPos) {
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
        this.useInteractPos = useInteractPos;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath("creraces", "interact_block"), json -> {
            ScalingValue ox = ScalingValue.fromJson(json, "offset_x", 0.0);
            ScalingValue oy = ScalingValue.fromJson(json, "offset_y", 0.0);
            ScalingValue oz = ScalingValue.fromJson(json, "offset_z", 0.0);
            boolean useInteractPos = GsonHelper.getAsBoolean(json, "use_interact_pos", true);
            return new InteractBlockAction(ox, oy, oz, useInteractPos);
        });
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @Nullable BlockPos interact_pos) {
        if (player.level().isClientSide()) return true;
        BlockPos pos = (useInteractPos && interact_pos != null) ? interact_pos : player.blockPosition();
        pos = pos.offset(
                (int) ox.evaluate(player, target, slot, interact_pos),
                (int) oy.evaluate(player, target, slot, interact_pos),
                (int) oz.evaluate(player, target, slot, interact_pos));

        BlockState state = player.level().getBlockState(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        InteractionResult result = state.useWithoutItem(player.level(), player, hit);
        return result.consumesAction();
    }
}
