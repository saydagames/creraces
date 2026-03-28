package mc.sayda.creraces.client.render;
 
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import mc.sayda.creraces.capability.DataUtils;
 
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
 
public class SpiritRealmRenderer {
    private static final BufferBuilder SPIRIT_MOON_BUILDER = new BufferBuilder(256);

    @SuppressWarnings("null")
    public static final ResourceLocation SPIRIT_MOON_ATLAS = new ResourceLocation("creraces",
            "textures/environment/moon_phases.png");
    private static final ResourceLocation MOON_LOCATION = new ResourceLocation("textures/environment/moon_phases.png");
    private static final ResourceLocation VANILLA_MOON = new ResourceLocation("textures/environment/moon_phases.png");
    private static final float MOON_ALPHA = 0.5f;
    private static final float MOON_SIZE = 20.0f;
 
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
                graphics.fill(0, 0, width, height, 0x2240BFFF); // Light aqua tint
                RenderSystem.disableBlend();
                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
            }
        });
    }
 
    /**
     * Called from LevelRendererMixin to render the spirit moons.
     */
    public static void renderSecondMoon(PoseStack poseStack, Matrix4f matrix4f, float partialTicks, boolean isMirror) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
            return;
 
        DataUtils.getVariables(mc.player).ifPresent(vars -> {
            if (vars.isInSpiritRealm()) {
                RenderSystem.enableBlend();
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
 
                // The Anchor Moon follows the natural vanilla orbital path
                renderMoonUnit(poseStack, partialTicks, false, isMirror);

                // The Spirit Moon follows the Main Moon's path, orbiting it and aligning on Day 9
                renderMoonUnit(poseStack, partialTicks, true, isMirror);
 
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(true);
            }
        });
    }
 
    private static void renderMoonUnit(PoseStack poseStack, float partialTicks, boolean isSecond, boolean isMirror) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
 
        double time = (double)mc.level.getDayTime() + partialTicks;
        // 216,000 ticks = 9 Minecraft days
        double progress = (time % 216000.0) / 216000.0;
        
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, isSecond ? SPIRIT_MOON_ATLAS : MOON_LOCATION); 

        poseStack.pushPose();

        // Universal 180-degree texture rotation for Spirit Realm moons
        poseStack.mulPose(Axis.YP.rotationDegrees(180));

        if (isSecond) {
            // Align perfectly at time = 210,000 ticks (the user's perfect Night 9 point)
            double alignmentPoint = 210000.0 / 216000.0;
            double orbitAngle = (progress - alignmentPoint) * 360.0;
            double rad = Math.toRadians(orbitAngle);
            
            float radius = 25.0f;
            // Mathematical pivot shift to make the orbit touch (0,0) at angle 0
            float xOffset = (float)(Math.cos(rad) * radius - radius);
            float zOffset = (float)(Math.sin(rad) * radius);
            
            // Translate on the celestial plane (XZ)
            poseStack.translate(xOffset, 0, zOffset);
            
            RenderSystem.setShaderColor(0.4f, 1.0f, 0.9f, MOON_ALPHA);
        } else {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, MOON_ALPHA);
        }

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder bufferBuilder = SPIRIT_MOON_BUILDER;

        // Standard Moon Phase UV Mapping
        int phase = mc.level.getMoonPhase();
        int l = phase % 4;
        int m = phase / 4 % 2;
        float u1 = (float)l / 4.0F;
        float v1 = (float)m / 2.0F;
        float u2 = (float)(l + 1) / 4.0F;
        float v2 = (float)(m + 1) / 2.0F;

        float moonSize = MOON_SIZE;
        // Follow vanilla celestial arch (Y = 100 for Sun-mirror, Y = -100 for Moon)
        float yPos = isMirror ? 100.0F : -100.0F;
        
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        if (isMirror) {
            // Sun-pole Winding (Y = 100): Needs specific order to avoid backface culling from below
            bufferBuilder.vertex(matrix, -moonSize, yPos, -moonSize).uv(u1, v1).endVertex();
            bufferBuilder.vertex(matrix, moonSize, yPos, -moonSize).uv(u2, v1).endVertex();
            bufferBuilder.vertex(matrix, moonSize, yPos, moonSize).uv(u2, v2).endVertex();
            bufferBuilder.vertex(matrix, -moonSize, yPos, moonSize).uv(u1, v2).endVertex();
        } else {
            // Moon-pole Winding (Y = -100): Standard celestial order
            bufferBuilder.vertex(matrix, -moonSize, yPos, moonSize).uv(u1, v2).endVertex();
            bufferBuilder.vertex(matrix, moonSize, yPos, moonSize).uv(u2, v2).endVertex();
            bufferBuilder.vertex(matrix, moonSize, yPos, -moonSize).uv(u2, v1).endVertex();
            bufferBuilder.vertex(matrix, -moonSize, yPos, -moonSize).uv(u1, v1).endVertex();
        }
        
        BufferUploader.drawWithShader(bufferBuilder.end());
        poseStack.popPose();
    }
}
