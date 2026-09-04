package mc.sayda.creraces.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mc.sayda.creraces.ability.EssenceType;
import mc.sayda.creraces.block.entity.EssenceVortexBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

public class EssenceVortexRenderer implements BlockEntityRenderer<EssenceVortexBlockEntity> {

    private static final ResourceLocation SPRITE_ID =
            ResourceLocation.fromNamespaceAndPath("creraces", "block/essence_vortex");
    // Identity normal matrix so the world-up (0,1,0) normal is stored unmodified in the vertex buffer.
    // minecraft_mix_light uses the raw stored normal; (0,1,0) saturates both directional lights
    // regardless of camera angle, giving full brightness on all sides.

    public EssenceVortexRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(EssenceVortexBlockEntity entity, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        EssenceType type = entity.getEssenceType();

        int color = type.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager()
                .getAtlas(InventoryMenu.BLOCK_ATLAS)
                .getSprite(SPRITE_ID);

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(camera.rotation());

        Matrix4f pose = poseStack.last().pose();
        // entityTranslucentEmissive: no lightmap (always fullbright), NO_CULL (both sides rendered)
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(InventoryMenu.BLOCK_ATLAS));

        addVertex(consumer, pose, -0.5f, -0.5f, 0f, r, g, b, u0, v1);
        addVertex(consumer, pose,  0.5f, -0.5f, 0f, r, g, b, u1, v1);
        addVertex(consumer, pose,  0.5f,  0.5f, 0f, r, g, b, u1, v0);
        addVertex(consumer, pose, -0.5f,  0.5f, 0f, r, g, b, u0, v0);

        poseStack.popPose();
    }

    private static void addVertex(VertexConsumer consumer, Matrix4f pose,
            float x, float y, float z, float r, float g, float b, float u, float v) {
        consumer.addVertex(pose, x, y, z)
                .setColor(r, g, b, 1.0f)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0f, 1f, 0f)
                ;
    }

    @Override
    public boolean shouldRenderOffScreen(EssenceVortexBlockEntity entity) {
        return true;
    }
}
