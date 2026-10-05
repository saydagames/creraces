package mc.sayda.creraces.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BellRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BellBlockEntity;

public class ToriiBellRenderer extends BellRenderer {

    private static final ResourceLocation TEXTURE = new ResourceLocation("creraces", "textures/entity/bell/torii_bell.png");
    private static final ResourceLocation WEATHERED_TEXTURE = new ResourceLocation("creraces", "textures/entity/bell/weathered_torii_bell.png");
    // BellRenderer keeps its bellBody private, so bake our own copy of the same vanilla part
    private final ModelPart bellBody;

    public ToriiBellRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        this.bellBody = context.bakeLayer(ModelLayers.BELL).getChild("bell_body");
    }

    @Override
    public void render(BellBlockEntity entity, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int light, int overlay) {
        float ticks = entity.ticks + partialTick;
        float xRot = 0f, zRot = 0f;
        if (entity.shaking) {
            float swing = (float) Math.sin(ticks / Math.PI) / (4.0F + ticks / 3.0F);
            if (entity.clickDirection == Direction.NORTH)
                xRot = -swing;
            else if (entity.clickDirection == Direction.SOUTH)
                xRot = swing;
            else if (entity.clickDirection == Direction.EAST)
                zRot = -swing;
            else if (entity.clickDirection == Direction.WEST)
                zRot = swing;
        }
        this.bellBody.xRot = xRot;
        this.bellBody.zRot = zRot;

        boolean weathered = entity.getBlockState().is(ModBlocks.WEATHERED_TORII_BELL.get());
        VertexConsumer consumer = buffer.getBuffer(RenderType.entitySolid(weathered ? WEATHERED_TEXTURE : TEXTURE));
        poseStack.pushPose();
        this.bellBody.render(poseStack, consumer, light, overlay);
        poseStack.popPose();
    }
}
