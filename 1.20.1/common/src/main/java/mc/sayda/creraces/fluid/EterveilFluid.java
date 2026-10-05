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

/** Eterveil, a mystical spring liquid. */
public abstract class EterveilFluid extends WaterlikeFluid {

    @Override
    public FlowingFluid getSource() { return ModFluids.ETERVEIL.get(); }

    @Override
    public FlowingFluid getFlowing() { return ModFluids.ETERVEIL_FLOWING.get(); }

    @Override
    public Item getBucket() { return ModItems.ETERVEIL_BUCKET.get(); }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return ModBlocks.ETERVEIL_BLOCK.get().defaultBlockState()
                .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    public static class Source extends EterveilFluid {
        @Override public boolean isSource(FluidState state) { return true; }
        @Override public int getAmount(FluidState state) { return 8; }
    }

    public static class Flowing extends EterveilFluid {
        @Override public boolean isSource(FluidState state) { return false; }
        @Override public int getAmount(FluidState state) { return state.getValue(LEVEL); }
        @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> b) {
            super.createFluidStateDefinition(b);
            b.add(LEVEL);
        }
    }
}
