package mc.sayda.creraces.block.entity;

import mc.sayda.creraces.ability.EssenceType;
import mc.sayda.creraces.block.EssenceVortexBlock;
import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class EssenceVortexBlockEntity extends BlockEntity {

    public EssenceVortexBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ESSENCE_VORTEX_ENTITY.get(), pos, state);
    }

    public EssenceType getEssenceType() {
        if (getBlockState().getBlock() instanceof EssenceVortexBlock vortex) {
            return vortex.getEssenceType();
        }
        return EssenceType.ARCANE;
    }
}
