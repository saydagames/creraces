package mc.sayda.creraces.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import mc.sayda.creraces.block.entity.ElysianVeilBloomBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class ElysianVeilBloomBlockEntityRenderer implements BlockEntityRenderer<ElysianVeilBloomBlockEntity> {

    public ElysianVeilBloomBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(ElysianVeilBloomBlockEntity entity, float partialTick, PoseStack stack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // The flower glows in both states; only the open bloom emits light (see its block's lightLevel).
        VeilMushroomBlockEntityRenderer.renderStaticModel(entity, stack, bufferSource, LightTexture.FULL_BRIGHT);
    }
}
