package mc.sayda.creraces.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import mc.sayda.creraces.block.entity.RatHoleBlockEntity;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Flat, invisible, indestructible tunnel marker placed by Ratkin's Rat Tunnels ability.
 */
public class RatHoleBlock extends Block implements EntityBlock {

    // 15×1×15 pixel-thin floor-level hitbox, same as legacy
    private static final VoxelShape SHAPE = Block.box(0.5, 0, 0.5, 15.5, 1, 15.5);

    public RatHoleBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.GRAVEL)
                .strength(-1.0f, 3600000.0f) // indestructible
                .noCollission()
                .noOcclusion()
                .pushReaction(PushReaction.BLOCK)
                .isRedstoneConductor((bs, bl, bp) -> false));
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new RatHoleBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand,
            @Nonnull BlockHitResult hit) {
        if (level.isClientSide())
            return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof mc.sayda.creraces.block.entity.RatHoleBlockEntity hole) {
            // Sneak-click deletion
            if (player.isSecondaryUseActive()) {
                if (player.getUUID().equals(hole.getOwnerUUID())) {
                    BlockPos destPos = hole.getDestination();
                    mc.sayda.creraces.capability.IPlayerVariables vars = mc.sayda.creraces.capability.DataUtils
                            .getVariables(player).orElse(null);

                    if (destPos != null && !destPos.equals(BlockPos.ZERO) && !destPos.equals(pos)) {
                        // Remove linked partner
                        level.setBlockAndUpdate(destPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                        if (vars != null) {
                            ResourceLocation stateId = new ResourceLocation("creraces", "rat_tunnels");
                            vars.setPersistentState(stateId, Math.max(0, vars.getPersistentState(stateId) - 2));
                        }
                    } else {
                        // Remove unlinked single hole
                        if (vars != null) {
                            ResourceLocation stateId = new ResourceLocation("creraces", "rat_tunnels");
                            vars.setPersistentState(stateId, Math.max(0, vars.getPersistentState(stateId) - 1));
                        }
                    }

                    // Remove current hole
                    level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    level.playSound(null, pos, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                            SoundSource.BLOCKS, 1.0f, 1.0f);
                    
                    return InteractionResult.SUCCESS;
                }
            }

            // Teleportation
            BlockPos dest = hole.getDestination();
            if (dest != null && !dest.equals(BlockPos.ZERO)) {
                // Perform tunneling
                player.teleportTo(dest.getX() + 0.5, dest.getY() + 0.1, dest.getZ() + 0.5);
                
                // Play sounds at both ends
                level.playSound(null, pos, SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 1.0f, 1.5f);
                level.playSound(null, dest, SoundEvents.GRAVEL_PLACE, SoundSource.PLAYERS, 1.0f, 1.2f);

                return InteractionResult.CONSUME;
            } else {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.creraces.invalid_rat_hole").withStyle(net.minecraft.ChatFormatting.WHITE), true);
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nonnull VoxelShape getVisualShape(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(@Nonnull BlockState state, @Nonnull BlockGetter reader, @Nonnull BlockPos pos) {
        return true;
    }

    @Override
    public int getLightBlock(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos) {
        return 0;
    }

    @Override
    public void animateTick(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        // Subtle dust particles
        if (random.nextFloat() < 0.1f) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.ASH, 
                pos.getX() + 0.5 + random.nextGaussian() * 0.2, 
                pos.getY() + 0.1, 
                pos.getZ() + 0.5 + random.nextGaussian() * 0.2, 
                0, 0, 0);
        }
    }
}
