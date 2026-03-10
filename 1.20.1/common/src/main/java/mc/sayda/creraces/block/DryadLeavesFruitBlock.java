package mc.sayda.creraces.block;

import mc.sayda.creraces.registry.ModBlocks;
import mc.sayda.creraces.registry.ModItems;
import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class DryadLeavesFruitBlock extends LeavesBlock {
    private static final ResourceLocation DRYAD_RACE = new ResourceLocation("creraces", "dryad");

    public DryadLeavesFruitBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        boolean isDryad = DataUtils.getVariables(player)
                .map(vars -> DRYAD_RACE.equals(vars.getRace()))
                .orElse(false);

        if (isDryad) {
            // Drop apple
            ItemStack apple = new ItemStack(ModItems.DRYAD_APPLE.get());
            ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, apple);
            entity.setPickUpDelay(10);
            level.addFreshEntity(entity);

            // Revert to flowering
            level.setBlockAndUpdate(pos, ModBlocks.DRYAD_LEAVES_FLOWERING.get().withPropertiesOf(state));

            return InteractionResult.CONSUME;
        } else {
            player.displayClientMessage(Component.translatable("msg.creraces.cant_harvest_fruit"), true);
            return InteractionResult.PASS;
        }
    }
}
