package mc.sayda.creraces.fluid;

import mc.sayda.creraces.registry.ModBlocks;
import mc.sayda.creraces.registry.ModFluids;
import mc.sayda.creraces.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

public abstract class FairySourceFluid extends WaterlikeFluid {

    @Override
    public FlowingFluid getSource() { return ModFluids.FAIRY_SOURCE.get(); }

    @Override
    public FlowingFluid getFlowing() { return ModFluids.FAIRY_SOURCE_FLOWING.get(); }

    @Override
    public Item getBucket() { return ModItems.FAIRY_BUCKET.get(); }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return ModBlocks.FAIRY_SOURCE_BLOCK.get().defaultBlockState()
                .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
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
