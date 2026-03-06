package mc.sayda.creraces.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import mc.sayda.creraces.client.render.SpiritRealmRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @org.spongepowered.asm.mixin.Shadow
    private net.minecraft.client.Minecraft minecraft;

    @Inject(method = "renderSky(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getTimeOfDay(F)F"))
    private void creraces$renderSecondMoon(PoseStack poseStack, Matrix4f matrix4f, float tickDelta, Camera camera,
            boolean bl, Runnable runnable, CallbackInfo ci) {
        SpiritRealmRenderer.renderSecondMoon(poseStack, matrix4f, tickDelta);
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void creraces$renderBeams(com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTick, long gameTime,
            boolean renderBlockOutline, net.minecraft.client.Camera camera,
            net.minecraft.client.renderer.GameRenderer gameRenderer,
            net.minecraft.client.renderer.LightTexture lightTexture, org.joml.Matrix4f projectionMatrix,
            CallbackInfo ci) {
        mc.sayda.creraces.client.render.BeamRenderer.render(poseStack, projectionMatrix, partialTick, gameTime,
                this.minecraft);
        mc.sayda.creraces.client.render.TetherRenderer.render(poseStack, projectionMatrix, partialTick, gameTime,
                this.minecraft);
    }
}
