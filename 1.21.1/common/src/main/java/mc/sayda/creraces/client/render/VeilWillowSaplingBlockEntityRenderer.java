package mc.sayda.creraces.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import mc.sayda.creraces.block.entity.VeilWillowSaplingBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class VeilWillowSaplingBlockEntityRenderer implements BlockEntityRenderer<VeilWillowSaplingBlockEntity> {

    public VeilWillowSaplingBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(VeilWillowSaplingBlockEntity entity, float partialTick, PoseStack stack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        VeilMushroomBlockEntityRenderer.renderStaticModel(entity, stack, bufferSource);
    }
}
