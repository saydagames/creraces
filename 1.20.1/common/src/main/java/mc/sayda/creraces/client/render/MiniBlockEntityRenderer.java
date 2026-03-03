package mc.sayda.creraces.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mc.sayda.creraces.block.entity.MicroBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders the contents of a MicroBlockEntity using a vertex caching system.
 * Sub-blocks are baked into transformed quads with slot-specific lighting
 * and only re-baked when the geometry or host light changes.
 */
public class MiniBlockEntityRenderer implements BlockEntityRenderer<MicroBlockEntity> {

    private final Map<BlockPos, CachedMiniModel> modelCache = new HashMap<>();
    private final Map<BlockState, BlockEntity> dummyCache = new HashMap<>();

    public MiniBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(@Nonnull MicroBlockEntity entity, float partialTick,
            @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource bufferSource,
            int packedLight, int packedOverlay) {

        if (entity.isEmpty())
            return;

        CachedMiniModel cached = modelCache.computeIfAbsent(entity.getBlockPos().immutable(),
                k -> new CachedMiniModel());

        // Re-bake if version or host light changed (to update sub-block shading)
        if (cached.version != entity.getRenderVersion() || cached.lastPackedLight != packedLight) {
            bakeModel(entity, cached, packedLight, packedOverlay);
        }

        // Render the baked quads
        PoseStack.Pose lastPose = poseStack.last();
        for (var entry : cached.quadsByRenderType.entrySet()) {
            RenderType rt = entry.getKey();
            VertexConsumer consumer = bufferSource.getBuffer(rt);
            for (BakedQuad quad : entry.getValue()) {
                // readExistingColor=true: use the pre-baked vertex tint (biome color).
                // The 3-float convenience overload uses readExistingColor=false which
                // throws away the baked tint entirely.
                consumer.putBulkData(lastPose, quad,
                        new float[] { 1.0f, 1.0f, 1.0f, 1.0f }, // per-vertex AO brightness
                        1.0f, 1.0f, 1.0f, // RGB multiplier (white = no extra tint)
                        new int[] { packedLight, packedLight, packedLight, packedLight },
                        packedOverlay, true);
            }
        }

        // Render advanced (ENTITYBLOCK_ANIMATED) blocks
        entity.forEachOccupied((x, y, z, state) -> {
            if (state.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED) {
                renderAdvanced(entity, x, y, z, state, poseStack, bufferSource, packedLight, packedOverlay,
                        partialTick);
            }
        });
    }

    private void renderAdvanced(MicroBlockEntity host, int x, int y, int z, BlockState state,
            PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay,
            float partialTick) {
        poseStack.pushPose();
        float scale = 1f / MicroBlockEntity.SIZE;
        poseStack.translate(x * scale, y * scale, z * scale);
        poseStack.scale(scale, scale, scale);

        BlockEntity dummy = getDummyBE(host.getLevel(), state);
        if (dummy != null) {
            ((mc.sayda.creraces.mixin.BlockEntityAccessor) dummy).setWorldPosition(host.getBlockPos());
            var renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(dummy);
            if (renderer != null) {
                renderer.render(dummy, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            }
        }

        poseStack.popPose();
    }

    private BlockEntity getDummyBE(Level level, BlockState state) {
        return dummyCache.computeIfAbsent(state, s -> {
            if (s.getBlock() instanceof EntityBlock eb) {
                BlockEntity be = eb.newBlockEntity(BlockPos.ZERO, s);
                if (be != null) {
                    be.setLevel(level);
                }
                return be;
            }
            return null;
        });
    }

    private void bakeModel(MicroBlockEntity entity, CachedMiniModel cached, int packedLight, int packedOverlay) {
        cached.quadsByRenderType.clear();
        cached.version = entity.getRenderVersion();
        cached.lastPackedLight = packedLight;

        final float scale = 1f / MicroBlockEntity.SIZE;
        var blockRenderer = Minecraft.getInstance().getBlockRenderer();
        var level = entity.getLevel();
        if (level == null)
            return;

        entity.forEachOccupied((x, y, z, state) -> {
            if (state.getRenderShape() != RenderShape.MODEL)
                return;

            // Sample light for this specific slot
            BlockPos pos = entity.getBlockPos();
            int slotLight = LevelRenderer.getLightColor(level, pos);

            BakedModel model = blockRenderer.getBlockModel(state);
            net.minecraft.util.RandomSource random = level.getRandom();

            // Use entity-renderer-compatible render types (NOT chunk pipeline types).
            // ItemBlockRenderTypes.getChunkRenderType returns types tied to the chunk
            // tessellator which are NOT compatible with block entity renderer buffers
            // on Fabric/Indigo. Map to the nearest entity-renderer equivalent instead.
            RenderType rt = getEntityCompatibleRenderType(state);
            List<BakedQuad> quads = cached.quadsByRenderType.computeIfAbsent(rt, k -> new ArrayList<>());

            // Bake quads from all directions
            for (Direction dir : Direction.values()) {
                addTransformedQuads(quads, model.getQuads(state, dir, random), x * scale, y * scale,
                        z * scale, scale, slotLight, state, level, pos, -1);
            }
            addTransformedQuads(quads, model.getQuads(state, null, random), x * scale, y * scale,
                    z * scale,
                    scale, slotLight, state, level, pos, -1);
        });
    }

    private void addTransformedQuads(List<BakedQuad> out, List<BakedQuad> in,
            float tx, float ty, float tz, float scale, int light,
            BlockState state, Level level, BlockPos pos, int forcedColor) {
        var blockColors = Minecraft.getInstance().getBlockColors();

        for (BakedQuad quad : in) {
            int[] vertices = quad.getVertices().clone();
            int color = forcedColor;
            if (color == -1 && quad.isTinted()) {
                color = blockColors.getColor(state, level, pos, quad.getTintIndex());
            }

            for (int i = 0; i < 4; i++) {
                int offset = i * 8;
                float x = Float.intBitsToFloat(vertices[offset]);
                float y = Float.intBitsToFloat(vertices[offset + 1]);
                float z = Float.intBitsToFloat(vertices[offset + 2]);

                vertices[offset] = Float.floatToRawIntBits(x * scale + tx);
                vertices[offset + 1] = Float.floatToRawIntBits(y * scale + ty);
                vertices[offset + 2] = Float.floatToRawIntBits(z * scale + tz);

                if (color != -1) {
                    // blockColors.getColor() returns 0xRRGGBB.
                    // Vertex buffer stores ABGR (R=byte0, G=byte1, B=byte2, A=byte3),
                    // which as an int is 0xAABBGGRR. Must convert from ARGB→ABGR.
                    int r = (color >> 16) & 0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = color & 0xFF;
                    vertices[offset + 3] = 0xFF000000 | (b << 16) | (g << 8) | r;
                }

                vertices[offset + 6] = light;
            }
            // Use -1 for tint index because we've already applied the color to the vertices
            out.add(new BakedQuad(
                    vertices, -1, quad.getDirection(), quad.getSprite(), quad.isShade()));
        }
    }

    /**
     * Maps a block's chunk-pipeline render type to a render type that is safe to
     * use
     * with a block entity renderer's {@link MultiBufferSource}.
     *
     * <p>
     * On Fabric/Indigo, chunk render types ({@code RenderType.solid()},
     * {@code cutout()}, etc.) are tied to the chunk tessellator and cannot be used
     * directly from a BER buffer source — they produce invisible geometry. Using
     * the
     * standard BER render types ({@code RenderType.solid()}, {@code cutout()},
     * {@code translucent()}) works correctly on both Forge and Fabric.
     *
     * <p>
     * We use {@link ItemBlockRenderTypes#getChunkRenderType} to detect the
     * intent (opaque / cutout / translucent) and then return the matching type
     * that the BER buffer source supports.
     */
    private static RenderType getEntityCompatibleRenderType(BlockState state) {
        RenderType chunk = ItemBlockRenderTypes.getChunkRenderType(state);
        if (chunk == RenderType.translucent()) {
            return RenderType.translucent();
        } else if (chunk == RenderType.cutout() || chunk == RenderType.cutoutMipped()) {
            return RenderType.cutout();
        } else {
            // solid / tripwire / everything else → solid
            return RenderType.solid();
        }
    }

    private static class CachedMiniModel {
        long version = -1;
        int lastPackedLight = -1;
        final Map<RenderType, List<BakedQuad>> quadsByRenderType = new HashMap<>();
    }

    /** Renderer is visible from any distance — matches host block visibility. */
    @Override
    public boolean shouldRenderOffScreen(@Nonnull MicroBlockEntity blockEntity) {
        return false;
    }
}
