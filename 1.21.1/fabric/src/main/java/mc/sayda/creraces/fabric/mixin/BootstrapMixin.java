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
        // Ensure attributes are registered immediately after vanilla bootstrap.
        // Registries are ready by this point, and it beats Player class loading.
        // CreRaces.init() calls this too; DeferredRegister makes the repeat harmless.
        ModAttributes.init();
    }
}
