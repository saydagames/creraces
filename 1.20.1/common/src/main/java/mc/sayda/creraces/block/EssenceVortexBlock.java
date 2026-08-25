package mc.sayda.creraces.block;

import mc.sayda.creraces.ability.EssenceRegistry;
import mc.sayda.creraces.ability.EssenceType;
import mc.sayda.creraces.block.entity.EssenceVortexBlockEntity;
import mc.sayda.creraces.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

public class EssenceVortexBlock extends BaseEntityBlock {

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
    public net.minecraft.network.chat.MutableComponent getName() {
        return net.minecraft.network.chat.Component.translatable("block.creraces.essence_vortex",
                net.minecraft.network.chat.Component.translatable("essence.creraces." + essenceType.getSerializedName()));
    }

    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE =
            net.minecraft.world.level.block.Block.box(4, 4, 4, 12, 12, 12);

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext ctx) {
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

    @Override
    public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (!mc.sayda.creraces.config.CreRacesConfig.ESSENCE_VORTEX_CONVERSION_ENABLED.get()) {
            return net.minecraft.world.InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        EssenceType heldType = EssenceRegistry.typeFromShard(held.getItem());
        if (heldType == null || heldType == essenceType) {
            return net.minecraft.world.InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            Block targetVortex = EssenceRegistry.VORTEXES.get(heldType).get();
            level.setBlock(pos, targetVortex.defaultBlockState(), 3);
            level.playSound(null, pos, SoundType.AMETHYST.getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0) return;
        EssenceClusterBlock cluster = (EssenceClusterBlock) EssenceRegistry.CLUSTERS.get(essenceType).get();
        for (int attempt = 0; attempt < 8; attempt++) {
            int dx = random.nextIntBetweenInclusive(-5, 5);
            int dy = random.nextIntBetweenInclusive(-3, 3);
            int dz = random.nextIntBetweenInclusive(-5, 5);
            BlockPos target = pos.offset(dx, dy, dz);
            if (!level.getBlockState(target).isAir()) continue;
            BlockPos below = target.below();
            if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                level.setBlock(target, cluster.defaultBlockState()
                        .setValue(EssenceClusterBlock.FACING, Direction.UP), 3);
                return;
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            int color = essenceType.getColor();
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            double px = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
            double py = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
            double pz = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
            level.addParticle(ModParticles.ESSENCE_PARTICLE.get(), px, py, pz, r, g, b);
        }
    }
}
