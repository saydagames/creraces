package mc.sayda.creraces.mixin;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Suppresses the underwater and lava screen overlay (the blue/orange
 * translucent
 * layer drawn over the viewport) for races with waterVision/unaffectedByWater
 * or lavaVision/unaffectedByLava passives.
 *
 * {@link ScreenEffectRenderer#renderScreenEffect} draws both the water overlay
 * (when submerged) and the fire overlay (when on fire) in one method, so
 * cancelling it here also suppresses fire whenever that same tick would have
 * shown it (e.g. a lavaVision race standing in lava).
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

            boolean inWater = player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
            boolean inLava = player.isEyeInFluid(net.minecraft.tags.FluidTags.LAVA);

            if ((inWater && (passives.waterVision() || passives.unaffectedByWater())) ||
                    (inLava && (passives.lavaVision() || passives.unaffectedByLava()))) {
                // Cancels the whole overlay pass at HEAD. Note this also suppresses the fire
                // overlay if this same tick would have shown one (e.g. a lavaVision race
                // standing in lava), since fire and liquid overlays share this one vanilla
                // method.
                ci.cancel();
            }
        });
    }
}
