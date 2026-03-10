package mc.sayda.creraces.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side renderer for the casting beam.
 *
 * <h3>Why this approach works</h3>
 * <p>
 * When {@code Tesselator.end()} is called, it internally triggers
 * {@code ShaderInstance.apply()}, which reads
 * {@code RenderSystem.getModelViewMatrix()}
 * (NOT any values we manually set on the Uniform object) and uploads them to
 * the GPU.
 * Therefore we must push our desired camera-rotation matrix onto
 * {@code RenderSystem.getModelViewStack()} BEFORE ending the batch so that
 * {@code apply()} reads the correct world-to-view matrix.
 *
 * <p>
 * Vertices are provided in camera-relative world space (world_pos - cam_pos).
 * The camera rotation (world→view) is provided via the ModelViewStack. The
 * projectionMatrix from renderLevel is set via
 * {@code RenderSystem.setProjectionMatrix()}.
 */
public class BeamRenderer {

    private static final ResourceLocation BEAM_TEXTURE = new ResourceLocation(
            "minecraft", "textures/entity/beacon_beam.png");
    private static final Map<UUID, BeamData> ACTIVE_BEAMS = new HashMap<>();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public static void handleSync(UUID playerId, boolean active,
            float r, float g, float b, float a, float radius) {
        if (active) {
            ACTIVE_BEAMS.put(playerId, new BeamData(r, g, b, a, radius));
        } else {
            ACTIVE_BEAMS.remove(playerId);
        }
    }

    public static void clear() {
        ACTIVE_BEAMS.clear();
    }

    /**
     * Render all active beams. Called from LevelRendererMixin at TAIL of
     * renderLevel.
     *
     * @param poseStack        PoseStack at renderLevel TAIL — top matrix = camera
     *                         rotation (world→view)
     * @param projectionMatrix the projection matrix from renderLevel
     * @param partialTick      interpolation factor [0,1]
     * @param gameTime         game tick (for UV scroll animation)
     * @param mc               Minecraft instance
     */
    public static void render(PoseStack poseStack, Matrix4f projectionMatrix,
            float partialTick, long gameTime, Minecraft mc) {
        if (ACTIVE_BEAMS.isEmpty() || mc.level == null)
            return;

        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();

        // poseStack.last().pose() = world-to-view rotation (confirmed by debug
        // analysis).
        // We push this onto the RenderSystem ModelViewStack so that
        // ShaderInstance.apply()
        // reads the correct matrix when tess.end() is called.
        Matrix4f cameraRotation = new Matrix4f(poseStack.last().pose());

        // Back up and configure the RenderSystem matrices
        Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        RenderSystem.setProjectionMatrix(projectionMatrix, null);

        PoseStack mvs = RenderSystem.getModelViewStack();
        mvs.pushPose();
        mvs.last().pose().set(cameraRotation); // World-to-view camera rotation
        RenderSystem.applyModelViewMatrix();

        try {
            for (Map.Entry<UUID, BeamData> entry : ACTIVE_BEAMS.entrySet()) {
                Player player = mc.level.getPlayerByUUID(entry.getKey());
                if (player == null || player.isRemoved())
                    continue;
                renderBeam(player, entry.getValue(), cam, partialTick, gameTime);
            }
        } finally {
            // Always restore matrices
            mvs.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(savedProj, null);
        }
    }

    // -------------------------------------------------------------------------
    // Core rendering
    // -------------------------------------------------------------------------

    private static void renderBeam(Player player, BeamData data, Vec3 cam,
            float partialTick, long gameTime) {

        // Beam axis: look direction, starting just in front of the eye
        Vec3 look = player.getViewVector(partialTick);
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 origin = eye.add(look.scale(0.8));
        Vec3 end = origin.add(look.scale(64.0));

        // Orthonormal basis perpendicular to look
        Vector3f lookV = new Vector3f((float) look.x, (float) look.y, (float) look.z);
        Vector3f refV = Math.abs(look.y) < 0.9
                ? new Vector3f(0, 1, 0)
                : new Vector3f(1, 0, 0);
        Vector3f right = new Vector3f(lookV).cross(refV).normalize();
        Vector3f up = new Vector3f(right).cross(lookV).normalize();

        float innerR = data.radius * 0.1f;
        float outerR = data.radius * 0.2f;
        float anim = Mth.frac(-((float) Math.floorMod(gameTime, 40L) + partialTick) * 0.2f);

        // GL state
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        RenderSystem.setShaderTexture(0, BEAM_TEXTURE);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();

        // Inner beam
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        emitSquareTube(buf, origin, end, right, up, innerR,
                data.r, data.g, data.b, data.a, anim, anim + 64f, cam);
        tess.end();

        // Outer beam
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        emitSquareTube(buf, origin, end, right, up, outerR,
                data.r, data.g, data.b, data.a * 0.35f, anim, anim + 64f, cam);
        tess.end();

        // Restore
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /**
     * Emits a 4-sided square tube between {@code start} and {@code end}.
     *
     * <p>
     * Corners use all four diagonal combinations of {@code ±right ± up},
     * forming a proper square (not diamond) cross-section.
     * Vertices are in camera-relative world space (pos − cam).
     */
    private static void emitSquareTube(
            BufferBuilder buf,
            Vec3 start, Vec3 end,
            Vector3f right, Vector3f up,
            float radius,
            float r, float g, float b, float a,
            float vStart, float vEnd,
            Vec3 cam) {

        // Four square corners: (±right ± up) × radius
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

            buf.vertex(sAx, sAy, sAz).color(r, g, b, a).uv(0f, vStart).endVertex();
            buf.vertex(sBx, sBy, sBz).color(r, g, b, a).uv(1f, vStart).endVertex();
            buf.vertex(eBx, eBy, eBz).color(r, g, b, a).uv(1f, vEnd).endVertex();
            buf.vertex(eAx, eAy, eAz).color(r, g, b, a).uv(0f, vEnd).endVertex();
        }
    }

    private static class BeamData {
        public final float r;
        public final float g;
        public final float b;
        public final float a;
        public final float radius;

        public BeamData(float r, float g, float b, float a, float radius) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.radius = radius;
        }
    }
}
