package mc.sayda.creraces.forge.mixin;

import mc.sayda.creraces.fluid.FairySourceFluid;
import mc.sayda.creraces.forge.CreRacesForge;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Fluid.class)
public class FairyFluidTypeMixin {

    @Inject(method = "getFluidType", at = @At("HEAD"), cancellable = true, remap = false)
    private void creraces$getFluidType(CallbackInfoReturnable<net.minecraftforge.fluids.FluidType> cir) {
        if ((Object) this instanceof FairySourceFluid && CreRacesForge.FAIRY_FLUID_TYPE != null) {
            cir.setReturnValue(CreRacesForge.FAIRY_FLUID_TYPE.get());
        }
    }
}
