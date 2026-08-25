package mc.sayda.creraces.block;

import mc.sayda.creraces.ability.EssenceRegistry;
import mc.sayda.creraces.ability.EssenceType;
import mc.sayda.creraces.block.entity.EssenceCauldronBlockEntity;
import mc.sayda.creraces.item.EssenceBottleItem;
import mc.sayda.creraces.item.EssenceBucketItem;
import mc.sayda.creraces.item.EssenceShardItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class EssenceCauldronBlock extends BaseEntityBlock {

    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 4);
    public static final BooleanProperty HAS_ESSENCE = BooleanProperty.create("has_essence");

    protected static final VoxelShape INSIDE = Block.box(2.0, 4.0, 2.0, 14.0, 16.0, 14.0);
    private static final VoxelShape SHAPE = Shapes.join(
        Shapes.block(),
        Shapes.or(
            Block.box(0.0, 0.0, 4.0, 16.0, 3.0, 12.0),
            Block.box(4.0, 0.0, 0.0, 12.0, 3.0, 16.0),
            Block.box(2.0, 0.0, 2.0, 14.0, 3.0, 14.0),
            INSIDE
        ),
        BooleanOp.ONLY_FIRST
    );

    // World-y offset of the liquid surface for each level (in block units, 1 block = 16 px)
    private static final double[] SURFACE_Y = { 0, 7.0/16, 9.0/16, 12.0/16, 15.0/16 };

    public EssenceCauldronBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0).setValue(HAS_ESSENCE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, HAS_ESSENCE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return INSIDE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EssenceCauldronBlockEntity(pos, state);
    }

    // Player interaction (right-click)

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        int lvl = state.getValue(LEVEL);
        boolean hasEssence = state.getValue(HAS_ESSENCE);

        // Water bucket: fill completely
        if (held.is(Items.WATER_BUCKET)) {
            if (lvl >= 4 || hasEssence) return InteractionResult.PASS;
            if (!level.isClientSide) {
                if (!(level.getBlockEntity(pos) instanceof EssenceCauldronBlockEntity be))
                    return InteractionResult.PASS;
                be.setEssenceType(null);
                level.setBlock(pos, state.setValue(LEVEL, 4).setValue(HAS_ESSENCE, false), 3);
                player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.BUCKET)));
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1f, 1f);
                level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // Water bottle: add 1 level
        if (held.is(Items.POTION) && PotionUtils.getPotion(held) == Potions.WATER) {
            if (lvl >= 4 || hasEssence) return InteractionResult.PASS;
            if (!level.isClientSide) {
                if (!(level.getBlockEntity(pos) instanceof EssenceCauldronBlockEntity be))
                    return InteractionResult.PASS;
                level.setBlock(pos, state.setValue(LEVEL, lvl + 1).setValue(HAS_ESSENCE, false), 3);
                player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.GLASS_BOTTLE)));
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1f, 1f);
                level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // Empty bucket: collect full essence cauldron or drain water
        if (held.is(Items.BUCKET) && lvl > 0) {
            if (hasEssence && lvl != 4) return InteractionResult.PASS;
            if (!level.isClientSide) {
                if (!(level.getBlockEntity(pos) instanceof EssenceCauldronBlockEntity be))
                    return InteractionResult.PASS;
                EssenceType essType = be.getEssenceType();
                ItemStack bucket = essType != null
                        ? EssenceBucketItem.of(essType, held)
                        : new ItemStack(Items.WATER_BUCKET);
                be.setEssenceType(null);
                level.setBlock(pos, state.setValue(LEVEL, 0).setValue(HAS_ESSENCE, false), 3);
                player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, bucket));
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1f, 1f);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // Essence bucket: fill empty cauldron with 4 levels
        if (held.getItem() instanceof EssenceBucketItem) {
            if (lvl > 0) return InteractionResult.PASS;
            if (!level.isClientSide) {
                EssenceType bucketType = EssenceBucketItem.getEssenceType(held);
                if (bucketType == null) return InteractionResult.PASS;
                if (!(level.getBlockEntity(pos) instanceof EssenceCauldronBlockEntity be))
                    return InteractionResult.PASS;
                ItemStack emptyBucket = new ItemStack(Items.BUCKET);
                EssenceBucketItem.transferDisplay(held, emptyBucket);
                be.setEssenceType(bucketType);
                level.setBlock(pos, state.setValue(LEVEL, 4).setValue(HAS_ESSENCE, true), 3);
                player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, emptyBucket));
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1f, 1f);
                level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // Glass bottle: collect 1 level of essence or water
        if (held.is(Items.GLASS_BOTTLE)) {
            if (lvl <= 0) return InteractionResult.PASS;
            if (!level.isClientSide) {
                if (!(level.getBlockEntity(pos) instanceof EssenceCauldronBlockEntity be))
                    return InteractionResult.PASS;
                EssenceType type = be.getEssenceType();
                ItemStack bottle = type == null
                        ? PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER)
                        : new ItemStack(EssenceRegistry.BOTTLES.get(type).get());
                player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, bottle));
                int newLvl = lvl - 1;
                if (newLvl <= 0) be.setEssenceType(null);
                level.setBlock(pos, state.setValue(LEVEL, newLvl).setValue(HAS_ESSENCE, type != null && newLvl > 0), 3);
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1f, 1f);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // Essence bottle: pour back into cauldron
        if (held.getItem() instanceof EssenceBottleItem bottleItem) {
            if (lvl >= 4) return InteractionResult.PASS;
            if (!level.isClientSide) {
                if (!(level.getBlockEntity(pos) instanceof EssenceCauldronBlockEntity be))
                    return InteractionResult.PASS;
                EssenceType cauldronType = be.getEssenceType();
                EssenceType bottleType = bottleItem.getEssenceType();
                if (cauldronType != null && cauldronType != bottleType) return InteractionResult.PASS;
                if (cauldronType == null && lvl > 0) return InteractionResult.PASS; // water mode
                be.setEssenceType(bottleType);
                level.setBlock(pos, state.setValue(LEVEL, lvl + 1).setValue(HAS_ESSENCE, true), 3);
                player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.GLASS_BOTTLE)));
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1f, 1f);
                level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    // Thrown shard lands inside

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) return;
        if (!(entity instanceof ItemEntity itemEntity)) return;

        ItemStack stack = itemEntity.getItem();
        if (!(stack.getItem() instanceof EssenceShardItem shardItem)) return;

        int lvl = state.getValue(LEVEL);
        if (lvl <= 0) return;
        if (!isHeated(level, pos)) return;

        if (!(level.getBlockEntity(pos) instanceof EssenceCauldronBlockEntity be)) return;

        EssenceType existing = be.getEssenceType();
        if (existing != null) return; // already converted to essence

        // One shard converts all water levels to this essence type; update block state immediately
        // so the tint color handler reads HAS_ESSENCE from the state instead of stale block entity data
        be.setEssenceType(shardItem.getEssenceType());
        level.setBlock(pos, state.setValue(HAS_ESSENCE, true), 3);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.8f);

        {
            if (stack.getCount() <= 1) {
                itemEntity.discard();
            } else {
                stack.shrink(1);
                itemEntity.setItem(stack);
            }
        }
    }

    // Bubble particles

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int lvl = state.getValue(LEVEL);
        if (lvl <= 0) return;
        if (!isHeated(level, pos)) return;

        if (random.nextInt(4) != 0) return;

        double surfaceY = pos.getY() + SURFACE_Y[lvl] - 0.05;
        double x = pos.getX() + 0.125 + random.nextDouble() * 0.75;
        double z = pos.getZ() + 0.125 + random.nextDouble() * 0.75;
        level.addParticle(ParticleTypes.BUBBLE_POP, x, surfaceY, z, 0, 0.05, 0);
    }

    // Helpers

    private static boolean isHeated(Level level, BlockPos pos) {
        var blockBelow = level.getBlockState(pos.below()).getBlock();
        return blockBelow == Blocks.FIRE
            || blockBelow == Blocks.SOUL_FIRE
            || blockBelow == Blocks.LAVA
            || blockBelow == Blocks.MAGMA_BLOCK;
    }
}
