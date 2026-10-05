package mc.sayda.creraces.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.client.render.BeamRenderer;
import mc.sayda.creraces.client.render.SpiritRealmRenderer;
import mc.sayda.creraces.client.render.TetherRenderer;
import mc.sayda.creraces.client.render.WaypointRenderer;
import mc.sayda.creraces.engine.WorldState;
import net.minecraft.client.Camera;
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
    private void creraces$storeLocals(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick,
            Camera camera, boolean isFoggy, Runnable skyFogSetup, CallbackInfo ci) {
        // Snapshot the camera pose instead of keeping vanilla's mutable PoseStack: vanilla pushes its
        // sun/moon rotation onto it further down, and SpiritRealmRenderer applies that rotation itself.
        PoseStack snapshot = new PoseStack();
        snapshot.mulPoseMatrix(poseStack.last().pose());
        this.creraces$currentPoseStack = snapshot;
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

    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer;)V"))
    private void creraces$hijackCelestialDraw(BufferBuilder.RenderedBuffer buffer) {
        if (this.creraces$moonPass) {
            this.creraces$moonPass = false;
            buffer.release();
            SpiritRealmRenderer.renderSecondMoon(this.creraces$currentPoseStack, this.creraces$currentDelta, false);
            return;
        }
        if (this.creraces$sunPass) {
            this.creraces$sunPass = false;
            buffer.release();
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
    private void creraces$renderBeams(PoseStack poseStack, float partialTick, long frameNanos,
            boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
            LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        if (this.minecraft.level == null) {
            return;
        }
        // renderLevel's long is the frame's nano time, not a tick count, so the beams animate off
        // the level's game time instead.
        long gameTime = this.minecraft.level.getGameTime();
        BeamRenderer.render(poseStack, projectionMatrix, partialTick, gameTime, this.minecraft);
        TetherRenderer.render(poseStack, projectionMatrix, partialTick, gameTime, this.minecraft);
        // Every push onto poseStack within renderLevel is matched by a pop, so by TAIL its top
        // matrix is back to the world-to-view transform renderLevel started with.
        WaypointRenderer.captureMatrices(poseStack.last().pose(), projectionMatrix);
    }
}
