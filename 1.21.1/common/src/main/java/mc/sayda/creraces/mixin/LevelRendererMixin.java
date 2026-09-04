package mc.sayda.creraces.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import mc.sayda.creraces.client.render.SpiritRealmRenderer;
import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    private PoseStack creraces$currentPoseStack;
    private Matrix4f creraces$currentMatrix;
    private float creraces$currentDelta;
    private boolean creraces$moonPass = false;
    private boolean creraces$sunPass = false;

    private static final ResourceLocation SUN_LOCATION = ResourceLocation.parse("textures/environment/sun.png");
    private static final ResourceLocation MOON_LOCATION = ResourceLocation.parse("textures/environment/moon_phases.png");

    @Inject(method = "renderSky", at = @At("HEAD"))
    private void creraces$storeLocals(Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick,
            Camera camera, boolean isFoggy, Runnable skyFogSetup, CallbackInfo ci) {
        // 1.21 stopped handing renderSky a PoseStack and builds one from the frustum matrix itself.
        // Mirror that here so the celestial redirects below draw against the same transform as vanilla.
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(frustumMatrix);
        this.creraces$currentPoseStack = poseStack;
        this.creraces$currentMatrix = projectionMatrix;
        this.creraces$currentDelta = partialTick;
        this.creraces$moonPass = false;
        this.creraces$sunPass = false;
    }

    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V"))
    private void creraces$overrideCelestialTexture(int unit, ResourceLocation location) {
        boolean inSpirit = this.minecraft.player != null
                && DataUtils.getVariables(this.minecraft.player).map(v -> v.isInSpiritRealm()).orElse(false);

        if (inSpirit) {
            // Suppress vanilla celestial rendering in spirit realm; custom draw handled by
            // hijackCelestialDraw
            if (location.getPath().contains("moon")) {
                this.creraces$moonPass = true;
                return;
            }
            if (location.getPath().contains("sun")) {
                this.creraces$sunPass = true;
                return;
            }
        } else {
            // In the overworld: replace moon texture when a spirit moon is active
            if (location.getPath().contains("moon")
                    && mc.sayda.creraces.engine.WorldState.isSpiritMoon(this.minecraft.level)) {
                com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(unit, SpiritRealmRenderer.SPIRIT_MOON_ATLAS);
                return;
            }
        }
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(unit, location);
    }

    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V"))
    private void creraces$hijackCelestialDraw(com.mojang.blaze3d.vertex.MeshData buffer) {
        if (this.creraces$moonPass) {
            this.creraces$moonPass = false;
            buffer.close();
            SpiritRealmRenderer.renderSecondMoon(this.creraces$currentPoseStack, this.creraces$currentMatrix,
                    this.creraces$currentDelta, false);
            return;
        }
        if (this.creraces$sunPass) {
            this.creraces$sunPass = false;
            buffer.close();
            SpiritRealmRenderer.renderSecondMoon(this.creraces$currentPoseStack, this.creraces$currentMatrix,
                    this.creraces$currentDelta, true);
            return;
        }
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buffer);
    }

    /**
     * Force Absolute Night color in Spirit Realm.
     */
    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getSkyColor(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;"))
    private net.minecraft.world.phys.Vec3 creraces$spiritSkyColor(net.minecraft.client.multiplayer.ClientLevel level,
            net.minecraft.world.phys.Vec3 pos, float f) {
        if (this.minecraft.player != null
                && DataUtils.getVariables(this.minecraft.player).map(v -> v.isInSpiritRealm()).orElse(false)) {
            return new net.minecraft.world.phys.Vec3(0, 0, 0);
        }
        return level.getSkyColor(pos, f);
    }

    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getStarBrightness(F)F"))
    private float creraces$spiritStarBrightness(net.minecraft.client.multiplayer.ClientLevel level, float f) {
        if (this.minecraft.player != null
                && DataUtils.getVariables(this.minecraft.player).map(v -> v.isInSpiritRealm()).orElse(false)) {
            return 1.0f; // Stars visible at all times
        }
        return level.getStarBrightness(f);
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void creraces$renderBeams(net.minecraft.client.DeltaTracker deltaTracker, boolean renderBlockOutline,
            net.minecraft.client.Camera camera,
            net.minecraft.client.renderer.GameRenderer gameRenderer,
            net.minecraft.client.renderer.LightTexture lightTexture, org.joml.Matrix4f frustumMatrix,
            org.joml.Matrix4f projectionMatrix, CallbackInfo ci) {
        if (this.minecraft.level == null) {
            return;
        }
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        long gameTime = this.minecraft.level.getGameTime();
        // renderLevel no longer receives a PoseStack or a game time in 1.21, so rebuild the same
        // world-space transform from the frustum matrix the way the sky path does.
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(frustumMatrix);
        mc.sayda.creraces.client.render.BeamRenderer.render(poseStack, projectionMatrix, partialTick, gameTime,
                this.minecraft);
        mc.sayda.creraces.client.render.TetherRenderer.render(poseStack, projectionMatrix, partialTick, gameTime,
                this.minecraft);
    }
}
