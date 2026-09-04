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

import java.util.concurrent.ConcurrentHashMap;
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

    private static final ResourceLocation BEAM_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "textures/entity/beacon_beam.png");
    private static final Map<UUID, BeamData> ACTIVE_BEAMS = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public static void handleSync(UUID playerId, boolean active,
            float r, float g, float b, float a, float radius, float length) {
        if (active) {
            ACTIVE_BEAMS.put(playerId, new BeamData(r, g, b, a, radius, length));
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
     * @param poseStack        PoseStack at renderLevel TAIL - top matrix = camera
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

        org.joml.Matrix4fStack mvs = RenderSystem.getModelViewStack();
        mvs.pushMatrix();
        mvs.set(cameraRotation); // World-to-view camera rotation
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
            mvs.popMatrix();
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
        Vec3 end = origin.add(look.scale(data.length));

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
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, BEAM_TEXTURE);

        Tesselator tess = Tesselator.getInstance();

        // Inner beam
        BufferBuilder innerBuf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        emitSquareTube(innerBuf, origin, end, right, up, innerR,
                data.r, data.g, data.b, data.a, anim, anim + data.length, cam);
        BufferUploader.drawWithShader(innerBuf.buildOrThrow());

        // Outer beam
        BufferBuilder outerBuf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        emitSquareTube(outerBuf, origin, end, right, up, outerR,
                data.r, data.g, data.b, data.a * 0.35f, anim, anim + data.length, cam);
        BufferUploader.drawWithShader(outerBuf.buildOrThrow());

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

            buf.addVertex(sAx, sAy, sAz).setColor(r, g, b, a).setUv(0f, vStart);
            buf.addVertex(sBx, sBy, sBz).setColor(r, g, b, a).setUv(1f, vStart);
            buf.addVertex(eBx, eBy, eBz).setColor(r, g, b, a).setUv(1f, vEnd);
            buf.addVertex(eAx, eAy, eAz).setColor(r, g, b, a).setUv(0f, vEnd);
        }
    }

    private static class BeamData {
        public final float r;
        public final float g;
        public final float b;
        public final float a;
        public final float radius;
        public final float length;

        public BeamData(float r, float g, float b, float a, float radius, float length) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.radius = radius;
            this.length = length;
        }
    }
}
