package mc.sayda.creraces.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class SpiritRealmRenderer {
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

                // Semi-transparent blue tint
                // Color: 0x400080FF (40 alpha, 00 red, 80 green, FF blue)
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                graphics.fill(0, 0, width, height, 0x400080FF);
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
                // Secondary blue moon logic
                // We'll place it at an offset from the normal moon
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderTexture(0, BLUE_MOON);
                RenderSystem.setShaderColor(0.6f, 0.8f, 1.0f, 0.7f);

                poseStack.pushPose();
                // TODO: Rotate to position the moon differently than the vanilla one
                // Vanilla moon is at -90 rotation around X?
                // Let's try an offset.
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-45.0F));
                poseStack.mulPose(
                        com.mojang.math.Axis.XP.rotationDegrees(mc.level.getTimeOfDay(partialTicks) * 360.0F + 180.0F));

                Matrix4f matrix = poseStack.last().pose();
                Tesselator tesselator = Tesselator.getInstance();
                BufferBuilder bufferBuilder = tesselator.getBuilder();

                float moonSize = 25.0F; // Larger than vanilla moon?
                bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
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
