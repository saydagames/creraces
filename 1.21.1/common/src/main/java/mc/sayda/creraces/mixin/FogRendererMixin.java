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
 * Removes the underwater and lava fog when the camera is submerged, for races
 * with {@code waterVision}/{@code lavaVision} or {@code unaffectedByWater}/
 * {@code unaffectedByLava} passives.
 *
 * By injecting into {@code setupFog} and bailing early when the passive is
 * active, the player effectively sees through the liquid as if they were in
 * air.
 *
 * The screen tint overlay (the translucent water/fire layer drawn over the
 * HUD) is a separate vanilla mechanism, {@code ScreenEffectRenderer#renderScreenEffect}
 * - see {@code LiquidOverlayMixin.java} for the equivalent suppression of that
 * overlay.
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
            if ((fogType == FogType.WATER && (passives.waterVision() || passives.unaffectedByWater()))) {
                ci.cancel();
            } else if (fogType == FogType.LAVA && (passives.lavaVision() || passives.unaffectedByLava())) {
                ci.cancel();
            }
        });
    }
}
