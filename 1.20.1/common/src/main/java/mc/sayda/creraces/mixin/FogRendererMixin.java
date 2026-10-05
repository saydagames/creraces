package mc.sayda.creraces.mixin;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips liquid fog while the camera is in water or lava, for races with waterVision/unaffectedByWater
 * or lavaVision/unaffectedByLava, so they see as if in air. LiquidOverlayMixin removes the matching
 * screen tint.
 */
@Mixin(FogRenderer.class)
public class FogRendererMixin {

    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private static void creraces$clearLiquidFog(
            Camera camera, FogRenderer.FogMode fogMode, float fogDistance,
            boolean thickFog, float partialTick, CallbackInfo ci) {

        if (!(camera.getEntity() instanceof Player player))
            return;

        DataUtils.getVariables(player).ifPresent(vars -> {
            Race race = RaceRegistry.get(vars.getRace());
            if (race == null)
                return;
            Race.Passives passives = race.passives() != null ? race.passives() : Race.Passives.DEFAULT;

            FogType fogType = camera.getFluidInCamera();
            if ((fogType == FogType.WATER && (passives.waterVision() || passives.unaffectedByWater()))
                    || (fogType == FogType.LAVA && (passives.lavaVision() || passives.unaffectedByLava()))) {
                ci.cancel();
            }
        });
    }
}
