package mc.sayda.creraces.fluid;

import mc.sayda.creraces.registry.ModBlocks;
import mc.sayda.creraces.registry.ModFluids;
import mc.sayda.creraces.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;

public abstract class FairySourceFluid extends FlowingFluid {

    @Override
    public FlowingFluid getSource() { return ModFluids.FAIRY_SOURCE.get(); }

    @Override
    public FlowingFluid getFlowing() { return ModFluids.FAIRY_SOURCE_FLOWING.get(); }

    @Override
    public Item getBucket() { return ModItems.FAIRY_BUCKET.get(); }

    @Override
    public boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
        return !isSame(fluid);
    }

    @Override
    protected boolean canConvertToSource(net.minecraft.world.level.Level level) { return false; }

    @Override
    protected void beforeDestroyingBlock(net.minecraft.world.level.LevelAccessor level,
            net.minecraft.core.BlockPos pos, BlockState state) {
        // Drop loot only in a real Level (not WorldGenRegion)
        if (level instanceof net.minecraft.world.level.Level realLevel) {
            net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                    realLevel, pos.getX(), pos.getY(), pos.getZ(),
                    new net.minecraft.world.item.ItemStack(state.getBlock().asItem()));
            realLevel.addFreshEntity(drop);
        }
    }

    @Override
    protected int getSlopeFindDistance(LevelReader level) { return 4; }

    @Override
    protected int getDropOff(LevelReader level) { return 1; }

    @Override
    public int getTickDelay(LevelReader level) { return 5; }

    @Override
    protected float getExplosionResistance() { return 100.0F; }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == ModFluids.FAIRY_SOURCE.get() || fluid == ModFluids.FAIRY_SOURCE_FLOWING.get();
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return ModBlocks.FAIRY_SOURCE_BLOCK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    public static class Source extends FairySourceFluid {
        @Override public boolean isSource(FluidState state) { return true; }
        @Override public int getAmount(FluidState state) { return 8; }
    }

    public static class Flowing extends FairySourceFluid {
        @Override public boolean isSource(FluidState state) { return false; }
        @Override public int getAmount(FluidState state) { return state.getValue(LEVEL); }
        @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> b) {
            super.createFluidStateDefinition(b);
            b.add(LEVEL);
        }
    }
}
