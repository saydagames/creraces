package mc.sayda.creraces.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.config.CreRacesConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class SpiritRealmRenderer {
    @SuppressWarnings("null")
    private static final ResourceLocation BLUE_MOON = new ResourceLocation("creraces",
            "textures/environment/blue_moon.png");

    public static void renderScreenTint(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;

        DataUtils.getVariables(mc.player).ifPresent(vars -> {
            if (vars.isInSpiritRealm()) {
                int width = mc.getWindow().getGuiScaledWidth();
                int height = mc.getWindow().getGuiScaledHeight();

                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                graphics.fill(0, 0, width, height, CreRacesConfig.SPIRIT_REALM_TINT_COLOR.get()); 
                RenderSystem.disableBlend();
                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
            }
        });
    }

    /**
     * Called from LevelRendererMixin to render the second moon.
     */
    public static void renderSecondMoon(PoseStack poseStack, Matrix4f matrix4f, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
            return;

        DataUtils.getVariables(mc.player).ifPresent(vars -> {
            if (vars.isInSpiritRealm()) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderTexture(0, BLUE_MOON);
                float moonAlpha = CreRacesConfig.SPIRIT_REALM_MOON_ALPHA.get();
                RenderSystem.setShaderColor(0.6f, 0.8f, 1.0f, moonAlpha);

                poseStack.pushPose();
                @SuppressWarnings("null")
                var axisY = com.mojang.math.Axis.YP;
                poseStack.mulPose(axisY.rotationDegrees(-45.0F));
                @SuppressWarnings("null")
                var axisX = com.mojang.math.Axis.XP;
                poseStack.mulPose(axisX.rotationDegrees(mc.level.getTimeOfDay(partialTicks) * 360.0F + 180.0F));

                @SuppressWarnings("null")
                Matrix4f matrix = poseStack.last().pose();
                Tesselator tesselator = Tesselator.getInstance();
                @SuppressWarnings("null")
                BufferBuilder bufferBuilder = tesselator.getBuilder();

                float moonSize = CreRacesConfig.SPIRIT_REALM_MOON_SIZE.get();
                @SuppressWarnings("null")
                var mode = VertexFormat.Mode.QUADS;
                bufferBuilder.begin(mode, DefaultVertexFormat.POSITION_TEX);
                bufferBuilder.vertex(matrix, -moonSize, 100.0F, -moonSize).uv(0.0F, 0.0F).endVertex();
                bufferBuilder.vertex(matrix, moonSize, 100.0F, -moonSize).uv(1.0F, 0.0F).endVertex();
                bufferBuilder.vertex(matrix, moonSize, 100.0F, moonSize).uv(1.0F, 1.0F).endVertex();
                bufferBuilder.vertex(matrix, -moonSize, 100.0F, moonSize).uv(0.0F, 1.0F).endVertex();
                tesselator.end();

                poseStack.popPose();
                RenderSystem.disableBlend();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        });
    }
}
