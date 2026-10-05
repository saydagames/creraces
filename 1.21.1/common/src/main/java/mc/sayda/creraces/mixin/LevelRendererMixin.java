package mc.sayda.creraces.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.client.render.BeamRenderer;
import mc.sayda.creraces.client.render.SpiritRealmRenderer;
import mc.sayda.creraces.client.render.TetherRenderer;
import mc.sayda.creraces.client.render.WaypointRenderer;
import mc.sayda.creraces.engine.WorldState;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Spirit realm sky (black sky, permanent stars, SpiritRealmRenderer's moons in place of the vanilla
 * sun and moon), the overworld spirit moon texture, and the world-space overlay renderers.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private PoseStack creraces$currentPoseStack;
    @Unique
    private float creraces$currentDelta;
    // Set when the texture redirect swallows the sun/moon texture, so the next draw is replaced too.
    @Unique
    private boolean creraces$moonPass;
    @Unique
    private boolean creraces$sunPass;

    @Inject(method = "renderSky", at = @At("HEAD"))
    private void creraces$storeLocals(Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick,
            Camera camera, boolean isFoggy, Runnable skyFogSetup, CallbackInfo ci) {
        // 1.21 stopped handing renderSky a PoseStack and builds one from the frustum matrix itself.
        // Mirror that so the celestial redirects below draw against the same transform as vanilla.
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(frustumMatrix);
        this.creraces$currentPoseStack = poseStack;
        this.creraces$currentDelta = partialTick;
        this.creraces$moonPass = false;
        this.creraces$sunPass = false;
    }

    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V"))
    private void creraces$overrideCelestialTexture(int unit, ResourceLocation location) {
        if (creraces$viewerInSpiritRealm()) {
            // Skip vanilla's sun and moon; creraces$hijackCelestialDraw draws ours instead.
            if (location.getPath().contains("moon")) {
                this.creraces$moonPass = true;
                return;
            }
            if (location.getPath().contains("sun")) {
                this.creraces$sunPass = true;
                return;
            }
        } else if (location.getPath().contains("moon") && WorldState.isSpiritMoon(this.minecraft.level)) {
            RenderSystem.setShaderTexture(unit, SpiritRealmRenderer.SPIRIT_MOON_ATLAS);
            return;
        }
        RenderSystem.setShaderTexture(unit, location);
    }

    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V"))
    private void creraces$hijackCelestialDraw(MeshData buffer) {
        if (this.creraces$moonPass) {
            this.creraces$moonPass = false;
            buffer.close();
            SpiritRealmRenderer.renderSecondMoon(this.creraces$currentPoseStack, this.creraces$currentDelta, false);
            return;
        }
        if (this.creraces$sunPass) {
            this.creraces$sunPass = false;
            buffer.close();
            SpiritRealmRenderer.renderSecondMoon(this.creraces$currentPoseStack, this.creraces$currentDelta, true);
            return;
        }
        BufferUploader.drawWithShader(buffer);
    }

    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getSkyColor(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 creraces$spiritSkyColor(ClientLevel level, Vec3 pos, float partialTick) {
        return creraces$viewerInSpiritRealm() ? Vec3.ZERO : level.getSkyColor(pos, partialTick);
    }

    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getStarBrightness(F)F"))
    private float creraces$spiritStarBrightness(ClientLevel level, float partialTick) {
        return creraces$viewerInSpiritRealm() ? 1.0f : level.getStarBrightness(partialTick);
    }

    @Unique
    private boolean creraces$viewerInSpiritRealm() {
        return this.minecraft.player != null
                && DataUtils.getVariables(this.minecraft.player).map(vars -> vars.isInSpiritRealm()).orElse(false);
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void creraces$renderBeams(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera,
            GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix,
            Matrix4f projectionMatrix, CallbackInfo ci) {
        if (this.minecraft.level == null) {
            return;
        }
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        long gameTime = this.minecraft.level.getGameTime();
        // renderLevel no longer receives a PoseStack or a game time in 1.21, so rebuild the same
        // world-space transform from the frustum matrix the way the sky path does.
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(frustumMatrix);
        BeamRenderer.render(poseStack, projectionMatrix, partialTick, gameTime, this.minecraft);
        TetherRenderer.render(poseStack, projectionMatrix, partialTick, gameTime, this.minecraft);
        // frustumMatrix is the world-to-view transform here; stash it for this frame's waypoint HUD pass.
        WaypointRenderer.captureMatrices(frustumMatrix, projectionMatrix);
    }
}
