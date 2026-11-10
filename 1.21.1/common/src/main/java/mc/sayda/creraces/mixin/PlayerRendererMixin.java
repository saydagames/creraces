package mc.sayda.creraces.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mc.sayda.creraces.client.render.AnimationHandler;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Draws a player in a dash's glide pose as if they were gliding, while they move as usual. */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @WrapOperation(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;isFallFlying()Z"))
    private boolean creraces$glidePose(AbstractClientPlayer player, Operation<Boolean> original) {
        return original.call(player) || AnimationHandler.isGliding(player.getUUID());
    }

    @WrapOperation(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;getFallFlyingTicks()I"))
    private int creraces$glidePoseTicks(AbstractClientPlayer player, Operation<Integer> original) {
        return AnimationHandler.isGliding(player.getUUID())
                ? AnimationHandler.glideTicks(player.getUUID())
                : original.call(player);
    }
}
