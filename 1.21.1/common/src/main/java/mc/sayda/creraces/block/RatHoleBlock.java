package mc.sayda.creraces.block;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.block.entity.RatHoleBlockEntity;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

import javax.annotation.Nonnull;

/**
 * Flat, invisible, indestructible tunnel marker placed by Ratkin's Rat Tunnels ability.
 */
public class RatHoleBlock extends Block implements EntityBlock {

    // Persistent state the Rat Tunnels ability uses to count its placed holes
    private static final ResourceLocation RAT_TUNNELS = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "rat_tunnels");

    // 15x1x15 pixel floor-level hitbox
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
    public InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
            @Nonnull Player player, @Nonnull BlockHitResult hit) {
        if (level.isClientSide())
            return InteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof RatHoleBlockEntity hole)) {
            return InteractionResult.SUCCESS;
        }

        // The owner sneak-clicks to fill the hole in, along with its linked partner
        if (player.isSecondaryUseActive() && player.getUUID().equals(hole.getOwnerUUID())) {
            BlockPos destPos = hole.getDestination();
            int holesRemoved = 1;
            if (destPos != null && !destPos.equals(BlockPos.ZERO) && !destPos.equals(pos)) {
                level.setBlockAndUpdate(destPos, Blocks.AIR.defaultBlockState());
                holesRemoved = 2;
            }
            IPlayerVariables vars = DataUtils.getVariables(player).orElse(null);
            if (vars != null) {
                vars.setPersistentState(RAT_TUNNELS, Math.max(0, vars.getPersistentState(RAT_TUNNELS) - holesRemoved));
            }

            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.playSound(null, pos, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 1.0f, 1.0f);
            return InteractionResult.SUCCESS;
        }

        BlockPos dest = hole.getDestination();
        if (dest == null || dest.equals(BlockPos.ZERO)) {
            player.displayClientMessage(
                    Component.translatable("msg.creraces.invalid_rat_hole").withStyle(ChatFormatting.WHITE), true);
            return InteractionResult.SUCCESS;
        }

        player.teleportTo(dest.getX() + 0.5, dest.getY() + 0.1, dest.getZ() + 0.5);
        level.playSound(null, pos, SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 1.0f, 1.5f);
        level.playSound(null, dest, SoundEvents.GRAVEL_PLACE, SoundSource.PLAYERS, 1.0f, 1.2f);
        return InteractionResult.CONSUME;
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
        if (random.nextFloat() < 0.1f) {
            level.addParticle(ParticleTypes.ASH,
                pos.getX() + 0.5 + random.nextGaussian() * 0.2,
                pos.getY() + 0.1,
                pos.getZ() + 0.5 + random.nextGaussian() * 0.2,
                0, 0, 0);
        }
    }
}
