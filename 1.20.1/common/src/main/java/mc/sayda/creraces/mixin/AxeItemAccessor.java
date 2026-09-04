package mc.sayda.creraces.mixin;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** Exposes AxeItem.STRIPPABLES so modded logs can register their stripped-block conversion. */
@Mixin(AxeItem.class)
public interface AxeItemAccessor {
    @Accessor("STRIPPABLES")
    static Map<Block, Block> creraces$getStrippables() {
        throw new UnsupportedOperationException("Mixin not applied");
    }

    @Accessor("STRIPPABLES")
    @Mutable
    static void creraces$setStrippables(Map<Block, Block> strippables) {
        throw new UnsupportedOperationException("Mixin not applied");
    }
}
