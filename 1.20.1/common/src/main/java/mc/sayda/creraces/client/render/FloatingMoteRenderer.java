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
            new ResourceLocation("creraces", "textures/entities/floating_mote.png");

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

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentCull(TEXTURE));
        Matrix4f pose = poseStack.last().pose();
        int fullBright = 0xF000F0;

        consumer.vertex(pose, -0.5f, 0.5f, 0f).color(255, 255, 255, 255)
                .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullBright).normal(0, 0, 1).endVertex();
        consumer.vertex(pose, 0.5f, 0.5f, 0f).color(255, 255, 255, 255)
                .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullBright).normal(0, 0, 1).endVertex();
        consumer.vertex(pose, 0.5f, -0.5f, 0f).color(255, 255, 255, 255)
                .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullBright).normal(0, 0, 1).endVertex();
        consumer.vertex(pose, -0.5f, -0.5f, 0f).color(255, 255, 255, 255)
                .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullBright).normal(0, 0, 1).endVertex();

        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }
}
