package mc.sayda.creraces.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes FireBlock's private setFlammable so modded logs/leaves can register their burn odds.
 * Mojang made this method public in 1.21, so 1.21.1 calls it directly instead of needing this.
 */
@Mixin(FireBlock.class)
public interface FireBlockAccessor {
    @Invoker("setFlammable")
    void creraces$callSetFlammable(Block block, int encouragement, int flammability);
}
