package mc.sayda.creraces.mixin;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes TreeDecoratorType's private constructor so a modded tree decorator can register a type. */
@Mixin(TreeDecoratorType.class)
public interface TreeDecoratorTypeAccessor {
    @Invoker("<init>")
    static <P extends TreeDecorator> TreeDecoratorType<P> creraces$callNew(MapCodec<P> codec) {
        throw new UnsupportedOperationException("Mixin not applied");
    }
}
