package mc.sayda.creraces.block;

import mc.sayda.creraces.ability.EssenceRegistry;
import mc.sayda.creraces.ability.EssenceType;
import mc.sayda.creraces.block.entity.EssenceVortexBlockEntity;
import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class EssenceVortexBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Block.box(4, 4, 4, 12, 12, 12);

    private final EssenceType essenceType;

    public EssenceVortexBlock(EssenceType essenceType) {
        super(BlockBehaviour.Properties.of()
            .mapColor(MapColor.QUARTZ)
            .strength(-1.0f, 3600000.0f)
            .sound(SoundType.AMETHYST)
            .lightLevel(state -> 12)
            .noOcclusion()
            .noCollission()
            .randomTicks());
        this.essenceType = essenceType;
    }

    public EssenceType getEssenceType() {
        return essenceType;
    }

    @Override
    public MutableComponent getName() {
        return Component.translatable("block.creraces.essence_vortex",
                Component.translatable("essence.creraces." + essenceType.getSerializedName()));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EssenceVortexBlockEntity(pos, state);
    }

    /** Feeding the vortex a shard of another essence converts it to that essence's vortex. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!CreRacesConfig.ESSENCE_VORTEX_CONVERSION_ENABLED.get()) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        EssenceType heldType = EssenceRegistry.typeFromShard(held.getItem());
        if (heldType == null || heldType == essenceType) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            Block targetVortex = EssenceRegistry.VORTEXES.get(heldType).get();
            level.setBlock(pos, targetVortex.defaultBlockState(), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundType.AMETHYST.getPlaceSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Occasionally grows a matching essence cluster on solid ground nearby. */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0) return;
        Block cluster = EssenceRegistry.CLUSTERS.get(essenceType).get();
        for (int attempt = 0; attempt < 8; attempt++) {
            int dx = random.nextIntBetweenInclusive(-5, 5);
            int dy = random.nextIntBetweenInclusive(-3, 3);
            int dz = random.nextIntBetweenInclusive(-5, 5);
            BlockPos target = pos.offset(dx, dy, dz);
            if (!level.getBlockState(target).isAir()) continue;
            BlockPos below = target.below();
            if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                level.setBlock(target, cluster.defaultBlockState()
                        .setValue(EssenceClusterBlock.FACING, Direction.UP), Block.UPDATE_ALL);
                return;
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            double px = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
            double py = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
            double pz = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
            EssenceClusterBlock.addEssenceParticle(level, essenceType, px, py, pz);
        }
    }
}
