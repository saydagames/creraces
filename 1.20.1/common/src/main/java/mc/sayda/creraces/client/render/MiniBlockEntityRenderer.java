package mc.sayda.creraces.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mc.sayda.creraces.block.entity.MicroBlockEntity;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.mixin.BlockEntityAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Renders the contents of a MicroBlockEntity from a per-position cache of transformed quads,
 * lit with the host block's light and re-baked only when the geometry or that light changes.
 */
public class MiniBlockEntityRenderer implements BlockEntityRenderer<MicroBlockEntity> {

    // Vertex positions for each face of a unit cube, indexed by Direction.ordinal(). NORTH to EAST
    // follow vanilla FaceInfo's outward (counter-clockwise) winding; DOWN and UP are listed in the
    // reverse order, so the culling render types used for liquids hide those two faces from outside.
    private static final float[][][] FACE_POSITIONS = {
        {{0,0,0}, {0,0,1}, {1,0,1}, {1,0,0}}, // DOWN
        {{0,1,1}, {0,1,0}, {1,1,0}, {1,1,1}}, // UP
        {{1,0,0}, {0,0,0}, {0,1,0}, {1,1,0}}, // NORTH
        {{0,0,1}, {1,0,1}, {1,1,1}, {0,1,1}}, // SOUTH
        {{0,0,0}, {0,0,1}, {0,1,1}, {0,1,0}}, // WEST
        {{1,0,1}, {1,0,0}, {1,1,0}, {1,1,1}}, // EAST
    };

    private final Map<BlockPos, CachedMiniModel> modelCache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<BlockPos, CachedMiniModel> eldest) {
            return size() > CreRacesConfig.MINI_MODEL_CACHE_SIZE.get();
        }
    };

    private final Map<BlockState, BlockEntity> dummyCache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<BlockState, BlockEntity> eldest) {
            return size() > CreRacesConfig.MINI_DUMMY_CACHE_SIZE.get();
        }
    };

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

        if (cached.version != entity.getRenderVersion() || cached.lastPackedLight != packedLight) {
            bakeModel(entity, cached, packedLight);
        }

        PoseStack.Pose lastPose = poseStack.last();
        for (var entry : cached.quadsByRenderType.entrySet()) {
            VertexConsumer consumer = bufferSource.getBuffer(entry.getKey());
            for (BakedQuad quad : entry.getValue()) {
                // readExistingColor=true keeps the tint baked into the vertices; the shorter
                // overload passes false and throws it away.
                consumer.putBulkData(lastPose, quad,
                        new float[] { 1.0f, 1.0f, 1.0f, 1.0f }, // per-vertex AO brightness
                        1.0f, 1.0f, 1.0f, // RGB multiplier (white = no extra tint)
                        new int[] { packedLight, packedLight, packedLight, packedLight },
                        packedOverlay, true);
            }
        }

        // Animated blocks (chests, bells and the like) need their own block entity renderer
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
            ((BlockEntityAccessor) dummy).setWorldPosition(host.getBlockPos());
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

    private void bakeModel(MicroBlockEntity entity, CachedMiniModel cached, int packedLight) {
        cached.quadsByRenderType.clear();
        cached.version = entity.getRenderVersion();
        cached.lastPackedLight = packedLight;

        final float scale = 1f / MicroBlockEntity.SIZE;
        var blockRenderer = Minecraft.getInstance().getBlockRenderer();
        var level = entity.getLevel();
        if (level == null)
            return;

        entity.forEachOccupied((x, y, z, state) -> {
            BlockPos pos = Objects.requireNonNull(entity.getBlockPos().immutable());
            int hostLight = LevelRenderer.getLightColor(level, pos);

            if (state.getBlock() instanceof LiquidBlock) {
                addLiquidFaces(cached.quadsByRenderType, level, pos, x, y, z, state, scale, hostLight);
                return;
            }

            if (state.getRenderShape() != RenderShape.MODEL)
                return;

            BakedModel model = blockRenderer.getBlockModel(state);
            long seed = state.getSeed(pos) + MicroBlockEntity.toIndex(x, y, z);
            RandomSource random = RandomSource.create(seed);

            RenderType rt = getEntityCompatibleRenderType(state);
            List<BakedQuad> quads = cached.quadsByRenderType.computeIfAbsent(rt, k -> new ArrayList<>());

            for (Direction dir : Direction.values()) {
                addTransformedQuads(quads, model.getQuads(state, dir, random), x * scale, y * scale,
                        z * scale, scale, hostLight, state, level, pos, -1);
            }
            addTransformedQuads(quads, model.getQuads(state, null, random), x * scale, y * scale,
                    z * scale, scale, hostLight, state, level, pos, -1);
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
                    // blockColors gives 0xRRGGBB, but the vertex data stores bytes in R, G, B, A
                    // order, which reads back as 0xAABBGGRR, so swap red and blue.
                    int r = (color >> 16) & 0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = color & 0xFF;
                    vertices[offset + 3] = 0xFF000000 | (b << 16) | (g << 8) | r;
                }

                vertices[offset + 6] = light;
            }
            // Tint index -1: the colour is already baked into the vertices
            out.add(new BakedQuad(
                    vertices, -1, quad.getDirection(), quad.getSprite(), quad.isShade()));
        }
    }

    /**
     * Maps a block's chunk render layer to the entity render type a block entity renderer can use.
     * The chunk types (solid, cutout, translucent) belong to the chunk pipeline and draw nothing
     * from a BER buffer source on Fabric/Indigo, so {@link ItemBlockRenderTypes#getChunkRenderType}
     * is only used to tell which of them the block wants.
     */
    private static RenderType getEntityCompatibleRenderType(BlockState state) {
        RenderType chunk = ItemBlockRenderTypes.getChunkRenderType(state);
        if (chunk == RenderType.translucent()) {
            return RenderType.entityTranslucentCull(Objects.requireNonNull(InventoryMenu.BLOCK_ATLAS));
        }
        // Cutout and solid both map to entityCutout; solid faces are never seen from behind, so
        // culling is fine there too.
        return RenderType.entityCutout(Objects.requireNonNull(InventoryMenu.BLOCK_ATLAS));
    }

    private void addLiquidFaces(Map<RenderType, List<BakedQuad>> quadsByRenderType,
            Level level, BlockPos pos,
            int x, int y, int z, BlockState state, float scale, int light) {
        boolean isWater = state.getBlock() == Blocks.WATER;
        ResourceLocation texLoc = new ResourceLocation(
                "minecraft", isWater ? "block/water_still" : "block/lava_still");
        TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager()
                .getAtlas(InventoryMenu.BLOCK_ATLAS)
                .getSprite(texLoc);

        RenderType rt = isWater
                ? RenderType.entityTranslucentCull(Objects.requireNonNull(InventoryMenu.BLOCK_ATLAS))
                : RenderType.entityCutout(Objects.requireNonNull(InventoryMenu.BLOCK_ATLAS));
        List<BakedQuad> quads = quadsByRenderType.computeIfAbsent(rt, k -> new ArrayList<>());

        for (Direction dir : Direction.values()) {
            addTransformedQuads(quads, List.of(buildLiquidFaceQuad(dir, sprite)),
                    x * scale, y * scale, z * scale, scale,
                    light, state, level, pos, -1);
        }
    }

    private static BakedQuad buildLiquidFaceQuad(Direction dir, TextureAtlasSprite sprite) {
        int[] vertices = new int[32];
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();
        float[][] uvs = {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
        float[][] fp = FACE_POSITIONS[dir.ordinal()];
        for (int i = 0; i < 4; i++) {
            int off = i * 8;
            vertices[off]     = Float.floatToRawIntBits(fp[i][0]);
            vertices[off + 1] = Float.floatToRawIntBits(fp[i][1]);
            vertices[off + 2] = Float.floatToRawIntBits(fp[i][2]);
            vertices[off + 3] = 0xFFFFFFFF;
            vertices[off + 4] = Float.floatToRawIntBits(uvs[i][0]);
            vertices[off + 5] = Float.floatToRawIntBits(uvs[i][1]);
            vertices[off + 6] = 0;
            vertices[off + 7] = 0;
        }
        return new BakedQuad(vertices, -1, dir, sprite, false);
    }

    /**
     * Use the global BER list so rendering is driven by getRenderBoundingBox()
     * rather than chunk-section frustum culling. Without this, a tiny fairy
     * (1/4 scale) standing inside the block's 1×1×1 space can trigger a
     * chunk-section edge case where the per-section render list is skipped.
     */
    @Override
    public boolean shouldRenderOffScreen(@Nonnull MicroBlockEntity blockEntity) {
        return true;
    }

    private static class CachedMiniModel {
        long version = -1;
        int lastPackedLight = -1;
        final Map<RenderType, List<BakedQuad>> quadsByRenderType = new HashMap<>();
    }
}
