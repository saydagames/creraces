package mc.sayda.creraces.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mc.sayda.creraces.client.CharmClient;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    /** Runs every frame, so a charmed player's view follows the source smoothly and ignores the mouse. */
    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void creraces$charmedView(LocalPlayer player, double yaw, double pitch, Operation<Void> original) {
        if (!CharmClient.aimView(player)) {
            original.call(player, yaw, pitch);
        }
    }
}
