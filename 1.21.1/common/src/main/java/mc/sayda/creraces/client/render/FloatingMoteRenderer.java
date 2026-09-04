package mc.sayda.creraces.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mc.sayda.creraces.entity.FloatingMoteEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class FloatingMoteRenderer extends EntityRenderer<FloatingMoteEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("creraces", "textures/entities/floating_mote.png");

    public FloatingMoteRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(FloatingMoteEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(FloatingMoteEntity entity, float yaw, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(0.35f, 0.35f, 0.35f);

        // entityTranslucent (no cull) rather than entityTranslucentCull: this is a single
        // billboarded quad, so culling its back face leaves it invisible from one side.
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        Matrix4f pose = poseStack.last().pose();
        int fullBright = 0xF000F0;

        consumer.addVertex(pose, -0.5f, 0.5f, 0f).setColor(255, 255, 255, 255)
                .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullBright).setNormal(0, 0, 1);
        consumer.addVertex(pose, 0.5f, 0.5f, 0f).setColor(255, 255, 255, 255)
                .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullBright).setNormal(0, 0, 1);
        consumer.addVertex(pose, 0.5f, -0.5f, 0f).setColor(255, 255, 255, 255)
                .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullBright).setNormal(0, 0, 1);
        consumer.addVertex(pose, -0.5f, -0.5f, 0f).setColor(255, 255, 255, 255)
                .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullBright).setNormal(0, 0, 1);

        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }
}
