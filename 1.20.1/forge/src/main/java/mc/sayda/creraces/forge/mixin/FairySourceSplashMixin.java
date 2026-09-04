package mc.sayda.creraces.forge.mixin;

import mc.sayda.creraces.forge.CreRacesForge;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.extensions.IForgeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forge's fluid rewrite hardcodes Entity.updateFluidHeightAndDoFluidPushing(TagKey, double) to
 * resolve FluidTags.WATER via literal identity with ForgeMod.WATER_TYPE (see the compiled shim in
 * Entity.class), so a modded fluid with its own FluidType never trips wasTouchingWater / the vanilla
 * splash sound+particles / fire-clear, no matter what fluid tags it carries. This replicates just that
 * entry effect for Fairy Source specifically, deliberately NOT the full isInWater() semantics
 * WaterEquivalentFluidMixin grants Eterveil, since fairy wings are meant to go soggy in real water and
 * Fairy Source shouldn't trigger that.
 */
@Mixin(Entity.class)
public abstract class FairySourceSplashMixin {

    @Unique
    private boolean creraces$wasTouchingFairySource;

    @Shadow
    protected boolean firstTick;

    @Shadow
    protected abstract void doWaterSplashEffect();

    @Shadow
    public abstract void resetFallDistance();

    @Shadow
    public abstract void clearFire();

    @Inject(method = "updateInWaterStateAndDoWaterCurrentPushing", at = @At("TAIL"))
    private void creraces$fairySourceSplash(CallbackInfo ci) {
        IForgeEntity self = (IForgeEntity) this;
        boolean touching = CreRacesForge.FAIRY_FLUID_TYPE != null
                && self.isInFluidType(CreRacesForge.FAIRY_FLUID_TYPE.get());

        if (touching) {
            if (!this.creraces$wasTouchingFairySource && !this.firstTick) {
                this.doWaterSplashEffect();
            }
            this.resetFallDistance();
            this.creraces$wasTouchingFairySource = true;
            this.clearFire();
        } else {
            this.creraces$wasTouchingFairySource = false;
        }
    }
}
