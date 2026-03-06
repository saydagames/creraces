package mc.sayda.creraces.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
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

    public static void handleSync(UUID casterId, UUID targetId, boolean active, String texture, float width) {
        if (active) {
            ACTIVE_TETHERS.computeIfAbsent(casterId, k -> new ConcurrentHashMap<>())
                    .put(targetId, new TetherRenderData(new ResourceLocation(texture), width));
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

        PoseStack mvs = RenderSystem.getModelViewStack();
        mvs.pushPose();
        mvs.last().pose().set(cameraRotation);
        RenderSystem.applyModelViewMatrix();

        try {
            for (Map.Entry<UUID, Map<UUID, TetherRenderData>> casterEntry : ACTIVE_TETHERS.entrySet()) {
                Entity caster = mc.level.getPlayerByUUID(casterEntry.getKey());
                if (caster == null || !caster.isAlive())
                    continue;

                for (Map.Entry<UUID, TetherRenderData> targetEntry : casterEntry.getValue().entrySet()) {
                    // Mobs aren't cleanly fetched by UUID via mc.level on client, need to scan
                    // entities
                    // or use getEntity API if available on client level. ClientLevel has
                    // getEntity().
                    Entity target = getClientEntityByUUID(mc.level, targetEntry.getKey());

                    if (target == null || !target.isAlive())
                        continue;

                    renderTether(caster, target, targetEntry.getValue(), cam, partialTick, gameTime);
                }
            }
        } finally {
            mvs.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(savedProj, null);
        }
    }

    // Fallback for retrieving client entities by UUID since older Forge versions or
    // mappings
    // might not expose it natively via simple iterators quickly.
    private static Entity getClientEntityByUUID(net.minecraft.client.multiplayer.ClientLevel level, UUID uuid) {
        // level.entitiesForRendering() is available, but iterating is slow.
        // Client levels track entities by ID, but also maintain a UUID map internally
        // in 1.20+.
        for (Entity e : level.entitiesForRendering()) {
            if (e.getUUID().equals(uuid))
                return e;
        }
        return null; // fallback
    }

    // -------------------------------------------------------------------------
    // Core rendering
    // -------------------------------------------------------------------------

    private static void renderTether(Entity caster, Entity target, TetherRenderData data, Vec3 cam,
            float partialTick, long gameTime) {

        Vec3 start = getTetherPos(caster, partialTick).add(0, caster.getBbHeight() * 0.5, 0);
        Vec3 end = getTetherPos(target, partialTick).add(0, target.getBbHeight() * 0.5, 0);

        Vec3 look = end.subtract(start).normalize();

        Vector3f lookV = new Vector3f((float) look.x, (float) look.y, (float) look.z);
        Vector3f refV = Math.abs(look.y) < 0.9
                ? new Vector3f(0, 1, 0)
                : new Vector3f(1, 0, 0);
        Vector3f right = new Vector3f(lookV).cross(refV).normalize();
        Vector3f up = new Vector3f(right).cross(lookV).normalize();

        float innerR = data.width * 0.1f;
        float outerR = data.width * 0.2f;

        // Animated time
        float animOffset = -(gameTime + partialTick) * 0.1f;

        // Pulsing Guardian Effect Calculation
        float d1 = Mth.sin(animOffset * 0.5f) * 0.1f;
        innerR += d1;
        outerR += d1 * 1.5f;

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        RenderSystem.setShaderTexture(0, data.texture);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();

        float dist = (float) start.distanceTo(end);

        // Calculate texture tiling based on distance
        float vStart = animOffset;
        float vEnd = animOffset + dist * 0.5f;

        // V ranges should be [length, 0] instead of [0, length] based on BeamRenderer
        // logic (anim to anim + 64)
        vStart = animOffset + dist * 1.0f;
        vEnd = animOffset;

        // Inner beam
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        emitSquareTube(buf, start, end, right, up, innerR,
                1f, 1f, 1f, 1f, vStart, vEnd, cam);
        tess.end();

        // Outer glow
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        emitSquareTube(buf, start, end, right, up, outerR,
                1f, 1f, 1f, 0.45f, vStart, vEnd, cam);
        tess.end();

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

    private static void emitSquareTube(
            BufferBuilder buf,
            Vec3 start, Vec3 end,
            Vector3f right, Vector3f up,
            float radius,
            float r, float g, float b, float a,
            float vStart, float vEnd,
            Vec3 cam) {

        float[] rx = { 1, 1, -1, -1 };
        float[] ry = { 1, -1, -1, 1 };

        for (int face = 0; face < 4; face++) {
            int next = (face + 1) % 4;

            float sAx = (float) (start.x - cam.x) + radius * (rx[face] * right.x + ry[face] * up.x);
            float sAy = (float) (start.y - cam.y) + radius * (rx[face] * right.y + ry[face] * up.y);
            float sAz = (float) (start.z - cam.z) + radius * (rx[face] * right.z + ry[face] * up.z);

            float sBx = (float) (start.x - cam.x) + radius * (rx[next] * right.x + ry[next] * up.x);
            float sBy = (float) (start.y - cam.y) + radius * (rx[next] * right.y + ry[next] * up.y);
            float sBz = (float) (start.z - cam.z) + radius * (rx[next] * right.z + ry[next] * up.z);

            float eAx = (float) (end.x - cam.x) + radius * (rx[face] * right.x + ry[face] * up.x);
            float eAy = (float) (end.y - cam.y) + radius * (rx[face] * right.y + ry[face] * up.y);
            float eAz = (float) (end.z - cam.z) + radius * (rx[face] * right.z + ry[face] * up.z);

            float eBx = (float) (end.x - cam.x) + radius * (rx[next] * right.x + ry[next] * up.x);
            float eBy = (float) (end.y - cam.y) + radius * (rx[next] * right.y + ry[next] * up.y);
            float eBz = (float) (end.z - cam.z) + radius * (rx[next] * right.z + ry[next] * up.z);

            // Standard UVs are typically 0/1 for U (horizontal around the beam), and
            // vStart/vEnd for V (length along beam)
            buf.vertex(sAx, sAy, sAz).color(r, g, b, a).uv(1f, vStart).endVertex();
            buf.vertex(sBx, sBy, sBz).color(r, g, b, a).uv(0f, vStart).endVertex();
            buf.vertex(eBx, eBy, eBz).color(r, g, b, a).uv(0f, vEnd).endVertex();
            buf.vertex(eAx, eAy, eAz).color(r, g, b, a).uv(1f, vEnd).endVertex();
        }
    }

    private record TetherRenderData(ResourceLocation texture, float width) {
    }
}
