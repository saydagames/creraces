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

    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath("creraces", "textures/entity/bell/torii_bell.png");
    private static final ResourceLocation W_TEX = ResourceLocation.fromNamespaceAndPath("creraces", "textures/entity/bell/weathered_torii_bell.png");
    // Shadow the parent's private bellBody so we own a reference.
    private final ModelPart bellBody;

    public ToriiBellRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        // Re-bake the same vanilla layer: correct geometry and UV guaranteed.
        this.bellBody = context.bakeLayer(ModelLayers.BELL).getChild("bell_body");
    }

    @Override
    public void render(BellBlockEntity entity, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int light, int overlay) {
        float f = entity.ticks + partialTick;
        float xRot = 0f, zRot = 0f;
        if (entity.shaking) {
            float swing = (float) Math.sin(f / Math.PI) / (4.0F + f / 3.0F);
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
        VertexConsumer vc = buffer.getBuffer(RenderType.entitySolid(weathered ? W_TEX : TEX));
        poseStack.pushPose();
        this.bellBody.render(poseStack, vc, light, overlay);
        poseStack.popPose();
    }
}
