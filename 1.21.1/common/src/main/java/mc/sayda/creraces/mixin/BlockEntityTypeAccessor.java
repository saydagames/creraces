package mc.sayda.creraces.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

/** Exposes BlockEntityType.validBlocks so SIGN/HANGING_SIGN can be extended with modded sign blocks. */
@Mixin(BlockEntityType.class)
public interface BlockEntityTypeAccessor {
    @Accessor("validBlocks")
    Set<Block> creraces$getValidBlocks();

    @Accessor("validBlocks")
    @Mutable
    void creraces$setValidBlocks(Set<Block> validBlocks);
}
