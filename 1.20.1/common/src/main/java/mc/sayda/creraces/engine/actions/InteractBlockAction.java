package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.Position;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Right-clicks the block at "position" (the interacted block by default) with the main hand. */
@SuppressWarnings("null")
public class InteractBlockAction implements ActionRegistry.RaceAction {
    private final Position position;

    public InteractBlockAction(Position position) {
        this.position = position;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (player.level().isClientSide()) {
            return true;
        }
        BlockPos pos = position.resolveBlock(player, target, slot, interactPos);
        if (pos == null) {
            return false;
        }
        BlockState state = player.level().getBlockState(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        InteractionResult result = state.use(player.level(), player, InteractionHand.MAIN_HAND, hit);
        return result.consumesAction();
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("interact_block"), json -> new InteractBlockAction(
                Position.fromJson(json, "position", Position.of(Position.Source.BLOCK))));
    }
}
