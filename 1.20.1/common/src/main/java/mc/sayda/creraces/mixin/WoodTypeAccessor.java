package mc.sayda.creraces.mixin;

import net.minecraft.world.level.block.state.properties.WoodType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes WoodType's private registration method so modded wood types can be registered. */
@Mixin(WoodType.class)
public interface WoodTypeAccessor {
    @Invoker("register")
    static WoodType creraces$callRegister(WoodType type) {
        throw new UnsupportedOperationException("Mixin not applied");
    }
}
