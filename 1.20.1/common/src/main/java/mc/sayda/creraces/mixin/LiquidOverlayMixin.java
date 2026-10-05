package mc.sayda.creraces.mixin;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Hides the underwater/lava screen overlay for races with waterVision/unaffectedByWater or
 * lavaVision/unaffectedByLava. {@link ScreenEffectRenderer#renderScreenEffect} also draws the fire
 * overlay, so cancelling it hides fire on the same frames (e.g. a lavaVision race standing in lava).
 * FogRendererMixin handles the matching fog.
 */
@Mixin(ScreenEffectRenderer.class)
public class LiquidOverlayMixin {

    @Inject(method = "renderScreenEffect", at = @At("HEAD"), cancellable = true)
    private static void creraces$suppressLiquidOverlay(Minecraft minecraft, PoseStack poseStack, CallbackInfo ci) {
        Player player = minecraft.player;
        if (player == null)
            return;

        DataUtils.getVariables(player).ifPresent(vars -> {
            Race race = RaceRegistry.get(vars.getRace());
            if (race == null)
                return;
            Race.Passives passives = race.passives() != null ? race.passives() : Race.Passives.DEFAULT;

            boolean inWater = player.isEyeInFluid(FluidTags.WATER);
            boolean inLava = player.isEyeInFluid(FluidTags.LAVA);

            if ((inWater && (passives.waterVision() || passives.unaffectedByWater())) ||
                    (inLava && (passives.lavaVision() || passives.unaffectedByLava()))) {
                ci.cancel();
            }
        });
    }
}
