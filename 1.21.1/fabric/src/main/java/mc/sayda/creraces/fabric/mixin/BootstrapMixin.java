package mc.sayda.creraces.fabric.mixin;

import mc.sayda.creraces.registry.ModAttributes;
import net.minecraft.server.Bootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Bootstrap.class)
public class BootstrapMixin {
    @Inject(method = "bootStrap", at = @At("RETURN"))
    private static void creraces$bootStrap(CallbackInfo ci) {
        // Register attributes as soon as vanilla bootstrap finishes, before any Player is created.
        // CreRaces.init() calls this too; init() ignores the repeat.
        ModAttributes.init();
    }
}
