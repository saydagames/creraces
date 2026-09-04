package mc.sayda.creraces.neoforge.mixin;

import mc.sayda.creraces.neoforge.CreRacesNeoForge;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.extensions.IEntityExtension;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Entity.updateFluidHeightAndDoFluidPushing(TagKey, double)'s return value IS wasTouchingWater, which
 * backs isInWater()/isInWaterOrBubble()/etc, but the fluid rewrite resolves FluidTags.WATER via
 * literal identity with NeoForgeMod.WATER_TYPE, so a modded FluidType never satisfies it regardless of
 * fluid tags (see FairySourceSplashMixin for the fuller writeup). Folding
 * CreRacesNeoForge.WATER_EQUIVALENT_FLUID_TYPES into this check, rather than replicating vanilla's
 * downstream side effects separately, gives those fluids real isInWater()-family semantics for free:
 * mob AI, drowning, other mods' water checks, all of it, not just splash/particles.
 */
@Mixin(Entity.class)
public abstract class WaterEquivalentFluidMixin {

    @Inject(method = "updateFluidHeightAndDoFluidPushing", at = @At("RETURN"), cancellable = true)
    private void creraces$includeWaterEquivalentFluids(TagKey<Fluid> tag, double motionScale,
            CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue().booleanValue() || !tag.equals(FluidTags.WATER)) {
            return;
        }

        IEntityExtension self = (IEntityExtension) this;
        for (DeferredHolder<FluidType, FluidType> fluidType : CreRacesNeoForge.WATER_EQUIVALENT_FLUID_TYPES) {
            if (fluidType.isBound() && self.isInFluidType(fluidType.get())) {
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
