package mc.sayda.creraces.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.SpiritMobilityHandler;
import mc.sayda.creraces.registry.ModGameRules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

public class SpiritRealmRenderer {

    public static volatile boolean CLIENT_SPIRIT_FLAME_VISIBLE = true;

    public static final ResourceLocation SPIRIT_MOON_ATLAS = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/environment/moon_phases.png");
    private static final ResourceLocation MOON_LOCATION = ResourceLocation.parse("textures/environment/moon_phases.png");
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

                // GuiGraphics.fill() uses RenderType.gui() which already handles blending;
                // manual RenderSystem blend calls here corrupt the GuiGraphics batch pipeline
                graphics.fill(0, 0, width, height, 0x2240BFFF);
            }
        });
    }

    public static void spawnSpiritFlameParticles(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) return;
        MinecraftServer localServer = minecraft.getSingleplayerServer();
        boolean flameVisible = localServer != null
                ? localServer.overworld().getGameRules().getRule(ModGameRules.SPIRIT_FLAME_VISIBLE).get()
                : CLIENT_SPIRIT_FLAME_VISIBLE;
        if (!flameVisible) return;
        if (SpiritMobilityHandler.isOnSpiritPlane(minecraft.player)) return;

        for (Player p : minecraft.level.players()) {
            if (p == minecraft.player) continue;
            if (!SpiritMobilityHandler.isOnSpiritPlane(p)) continue;
            minecraft.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    p.getX(), p.getY() + p.getBbHeight() * 0.5, p.getZ(),
                    0.0, 0.005, 0.0);
        }
    }

    /** Called from LevelRendererMixin in place of vanilla's sun and moon draws to render the spirit moons. */
    public static void renderSecondMoon(PoseStack poseStack, float partialTicks, boolean isMirror) {
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

        double time = (double) mc.level.getDayTime() + partialTicks;
        // 216,000 ticks = 9 Minecraft days
        double progress = (time % 216000.0) / 216000.0;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, isSecond ? SPIRIT_MOON_ATLAS : MOON_LOCATION);

        poseStack.pushPose();

        // Same celestial-arc rotation vanilla applies before drawing the sun/moon (see
        // LevelRenderer.renderSky), so these moons travel across the sky with time of day instead
        // of sitting fixed at the pole. Applied here rather than inherited from the caller:
        // LevelRendererMixin hands over a fresh camera-only pose stack, not the one vanilla rotates.
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(mc.level.getTimeOfDay(partialTicks) * 360.0f));

        // Universal 180-degree texture rotation for Spirit Realm moons
        poseStack.mulPose(Axis.YP.rotationDegrees(180));

        if (isSecond) {
            // Align at 210,000 ticks: this is the Night 9 peak (Spirit Moon aligns here)
            double alignmentPoint = 210000.0 / 216000.0;
            double orbitAngle = (progress - alignmentPoint) * 360.0;
            double rad = Math.toRadians(orbitAngle);

            float radius = 25.0f;
            // Shift the pivot so the orbit passes through (0,0) at angle 0
            float xOffset = (float) (Math.cos(rad) * radius - radius);
            float zOffset = (float) (Math.sin(rad) * radius);

            // Translate on the celestial plane (XZ)
            poseStack.translate(xOffset, 0, zOffset);

            RenderSystem.setShaderColor(0.4f, 1.0f, 0.9f, MOON_ALPHA);
        } else {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, MOON_ALPHA);
        }

        Matrix4f matrix = poseStack.last().pose();
        // Same UV mapping as vanilla's 4x2 moon phase atlas
        int phase = mc.level.getMoonPhase();
        int column = phase % 4;
        int row = phase / 4 % 2;
        float u1 = (float) column / 4.0F;
        float v1 = (float) row / 2.0F;
        float u2 = (float) (column + 1) / 4.0F;
        float v2 = (float) (row + 1) / 2.0F;

        // Follow vanilla celestial arch (Y = 100 for Sun-mirror, Y = -100 for Moon)
        float yPos = isMirror ? 100.0F : -100.0F;

        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        if (isMirror) {
            // Sun-pole winding (Y = 100): this order keeps the quad from being culled when seen from below
            bufferBuilder.addVertex(matrix, -MOON_SIZE, yPos, -MOON_SIZE).setUv(u1, v1);
            bufferBuilder.addVertex(matrix, MOON_SIZE, yPos, -MOON_SIZE).setUv(u2, v1);
            bufferBuilder.addVertex(matrix, MOON_SIZE, yPos, MOON_SIZE).setUv(u2, v2);
            bufferBuilder.addVertex(matrix, -MOON_SIZE, yPos, MOON_SIZE).setUv(u1, v2);
        } else {
            // Moon-pole winding (Y = -100): standard celestial order
            bufferBuilder.addVertex(matrix, -MOON_SIZE, yPos, MOON_SIZE).setUv(u1, v2);
            bufferBuilder.addVertex(matrix, MOON_SIZE, yPos, MOON_SIZE).setUv(u2, v2);
            bufferBuilder.addVertex(matrix, MOON_SIZE, yPos, -MOON_SIZE).setUv(u2, v1);
            bufferBuilder.addVertex(matrix, -MOON_SIZE, yPos, -MOON_SIZE).setUv(u1, v1);
        }

        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        poseStack.popPose();
    }
}
