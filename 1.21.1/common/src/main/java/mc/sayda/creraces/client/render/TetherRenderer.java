package mc.sayda.creraces.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Axis;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.UUID;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side renderer for the tether capability.
 * Draws an undulating Guardian-style beam between connected entities.
 */
public class TetherRenderer {

    // caster -> (target -> data)
    private static final Map<UUID, Map<UUID, TetherRenderData>> ACTIVE_TETHERS = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public static void clear() {
        ACTIVE_TETHERS.clear();
    }

    public static void handleSync(UUID casterId, UUID targetId, boolean active, String texture, float width, boolean effects) {
        if (active) {
            ACTIVE_TETHERS.computeIfAbsent(casterId, k -> new ConcurrentHashMap<>())
                    .put(targetId, new TetherRenderData(ResourceLocation.parse(texture), width, effects));
        } else {
            Map<UUID, TetherRenderData> tethers = ACTIVE_TETHERS.get(casterId);
            if (tethers != null) {
                tethers.remove(targetId);
                if (tethers.isEmpty()) {
                    ACTIVE_TETHERS.remove(casterId);
                }
            }
        }
    }

    /**
     * Render all active tethers. Called from LevelRendererMixin at TAIL of
     * renderLevel.
     */
    public static void render(PoseStack poseStack, Matrix4f projectionMatrix,
            float partialTick, long gameTime, Minecraft mc) {
        if (ACTIVE_TETHERS.isEmpty() || mc.level == null)
            return;

        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();

        Matrix4f cameraRotation = new Matrix4f(poseStack.last().pose());
        Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        RenderSystem.setProjectionMatrix(projectionMatrix, null);

        org.joml.Matrix4fStack mvs = RenderSystem.getModelViewStack();
        mvs.pushMatrix();
        mvs.set(cameraRotation);
        RenderSystem.applyModelViewMatrix();

        try {
            // Pruning pass: Remove tethers with dead or missing casters/targets
            ACTIVE_TETHERS.entrySet().removeIf(casterEntry -> {
                Entity caster = mc.level.getPlayerByUUID(Objects.requireNonNull(casterEntry.getKey()));
                if (caster == null || !caster.isAlive()) {
                    // Only prune if they've been missing for a while (handled by target checks
                    // or just immediate for the caster as they are players)
                    return true;
                }

                casterEntry.getValue().entrySet().removeIf(targetEntry -> {
                    Entity target = getClientEntityByUUID(mc.level, targetEntry.getKey());
                    return target == null || !target.isAlive();
                });

                return casterEntry.getValue().isEmpty();
            });

            for (Map.Entry<UUID, Map<UUID, TetherRenderData>> casterEntry : ACTIVE_TETHERS.entrySet()) {
                Entity caster = mc.level.getPlayerByUUID(casterEntry.getKey());
                if (caster == null)
                    continue;

                for (Map.Entry<UUID, TetherRenderData> targetEntry : casterEntry.getValue().entrySet()) {
                    Entity target = getClientEntityByUUID(mc.level, targetEntry.getKey());
                    if (target == null)
                        continue;

                    renderTether(caster, target, targetEntry.getValue(), cam, partialTick, gameTime, mc);
                }
            }
        } finally {

            mvs.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(savedProj, null);
        }
    }

    private static Entity getClientEntityByUUID(net.minecraft.client.multiplayer.ClientLevel level, UUID uuid) {
        for (Entity e : level.entitiesForRendering()) {
            if (e.getUUID().equals(uuid))
                return e;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Core rendering
    // -------------------------------------------------------------------------

    private static void renderTether(Entity caster, Entity target, TetherRenderData data, Vec3 cam,
            float partialTick, long gameTime, Minecraft mc) {

        Vec3 start = getTetherPos(caster, partialTick).add(0, caster.getBbHeight() * 0.5, 0);
        Vec3 end   = getTetherPos(target, partialTick).add(0, target.getBbHeight() * 0.5, 0);

        Vec3 dir = end.subtract(start);
        float beamLen = (float) dir.length() + 1.0f;
        Vec3 dirNorm = dir.normalize();

        // Euler angles to align local Y axis with the beam direction (vanilla approach)
        float n = (float) Math.acos(Mth.clamp((float) dirNorm.y, -1.0f, 1.0f));
        float o = (float) Math.atan2(dirNorm.z, dirNorm.x);

        // Animation driven by game ticks - gameTime param is finishNanoTime (ns), unusable here
        long tick = mc.level.getGameTime();
        float j = (float)(tick % 100) + partialTick;
        float k = j * 0.5f % 1.0f;   // texture scroll offset
        float q = j * 0.05f * -1.5f;  // cross-section spin angle

        float innerR = data.width * 0.2f;
        float outerR = data.width * 0.282f;

        // Inner beam cross-section corners (4 points evenly around the circle)
        float af = Mth.cos(q + (float) Math.PI)       * innerR;
        float ag = Mth.sin(q + (float) Math.PI)       * innerR;
        float ah = Mth.cos(q)                          * innerR;
        float ai = Mth.sin(q)                          * innerR;
        float aj = Mth.cos(q + (float) Math.PI / 2f)  * innerR;
        float ak = Mth.sin(q + (float) Math.PI / 2f)  * innerR;
        float al = Mth.cos(q + (float) Math.PI * 1.5f)* innerR;
        float am = Mth.sin(q + (float) Math.PI * 1.5f)* innerR;

        // V scroll: vanilla uses -1+k as base and scales by 2.5 per world-unit
        float vq = -1.0f + k;
        float vr = beamLen * 2.5f + vq;

        // GL state
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, data.texture);
        // setShaderTexture only records the ID; bind explicitly so _texParameter
        // actually modifies the tether texture object, not whatever GL had bound last.
        int glId = mc.getTextureManager().getTexture(data.texture).getId();
        com.mojang.blaze3d.platform.GlStateManager._bindTexture(glId);
        com.mojang.blaze3d.platform.GlStateManager._texParameter(
                org.lwjgl.opengl.GL11.GL_TEXTURE_2D,
                org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T,
                org.lwjgl.opengl.GL11.GL_REPEAT);

        // Push per-beam transform so local Y aligns with the beam axis.
        // Vertices are emitted in local beam space (Y=0 → caster, Y=beamLen → target).
        org.joml.Matrix4fStack mvs = RenderSystem.getModelViewStack();
        mvs.pushMatrix();
        mvs.translate((float) (start.x - cam.x), (float) (start.y - cam.y), (float) (start.z - cam.z));
        mvs.rotate(Axis.YP.rotationDegrees((((float) Math.PI / 2f) - o) * (180f / (float) Math.PI)));
        mvs.rotate(Axis.XP.rotationDegrees(n * (180f / (float) Math.PI)));
        RenderSystem.applyModelViewMatrix();

        Tesselator tess = Tesselator.getInstance();

        // Two crossing quads - same X-shape geometry as vanilla guardian beam, U 0.0–0.5
        BufferBuilder buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buf.addVertex(af, beamLen, ag).setColor(1f, 1f, 1f, 1f).setUv(0.4999f, vr);
        buf.addVertex(af, 0f,      ag).setColor(1f, 1f, 1f, 1f).setUv(0.4999f, vq);
        buf.addVertex(ah, 0f,      ai).setColor(1f, 1f, 1f, 1f).setUv(0.0f,    vq);
        buf.addVertex(ah, beamLen, ai).setColor(1f, 1f, 1f, 1f).setUv(0.0f,    vr);
        buf.addVertex(aj, beamLen, ak).setColor(1f, 1f, 1f, 1f).setUv(0.4999f, vr);
        buf.addVertex(aj, 0f,      ak).setColor(1f, 1f, 1f, 1f).setUv(0.4999f, vq);
        buf.addVertex(al, 0f,      am).setColor(1f, 1f, 1f, 1f).setUv(0.0f,    vq);
        buf.addVertex(al, beamLen, am).setColor(1f, 1f, 1f, 1f).setUv(0.0f,    vr);
        BufferUploader.drawWithShader(buf.buildOrThrow());

        // Effects cap - rotating diamond at the beam tip using right half of texture, U 0.5–1.0
        if (data.effects) {
            float ex = Mth.cos(q + 2.3561945f)            * outerR;
            float ey = Mth.sin(q + 2.3561945f)            * outerR;
            float ez = Mth.cos(q + (float) Math.PI / 4f)  * outerR;
            float ew = Mth.sin(q + (float) Math.PI / 4f)  * outerR;
            float ea = Mth.cos(q + 3.926991f)              * outerR;
            float eb = Mth.sin(q + 3.926991f)              * outerR;
            float ec = Mth.cos(q + 5.4977875f)             * outerR;
            float ed = Mth.sin(q + 5.4977875f)             * outerR;
            float as = (tick % 2 == 0) ? 0.0f : 0.5f;

            BufferBuilder capBuf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            capBuf.addVertex(ex, beamLen, ey).setColor(1f, 1f, 1f, 1f).setUv(0.5f, as + 0.5f);
            capBuf.addVertex(ez, beamLen, ew).setColor(1f, 1f, 1f, 1f).setUv(1.0f, as + 0.5f);
            capBuf.addVertex(ec, beamLen, ed).setColor(1f, 1f, 1f, 1f).setUv(1.0f, as);
            capBuf.addVertex(ea, beamLen, eb).setColor(1f, 1f, 1f, 1f).setUv(0.5f, as);
            BufferUploader.drawWithShader(capBuf.buildOrThrow());
        }

        mvs.popMatrix();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static Vec3 getTetherPos(Entity entity, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()),
                Mth.lerp(partialTick, entity.zo, entity.getZ()));
    }

    private static class TetherRenderData {
        public final ResourceLocation texture;
        public final float width;
        public final boolean effects;

        public TetherRenderData(ResourceLocation texture, float width, boolean effects) {
            this.texture = texture;
            this.width = width;
            this.effects = effects;
        }
    }
}
