package mc.sayda.creraces.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import mc.sayda.creraces.block.ElysianVeilBloomBlock;
import mc.sayda.creraces.block.entity.ElysianVeilBloomBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;

public class ElysianVeilBloomBlockEntityRenderer implements BlockEntityRenderer<ElysianVeilBloomBlockEntity> {

    public ElysianVeilBloomBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(ElysianVeilBloomBlockEntity entity, float partialTick, PoseStack stack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = entity.getBlockState();
        boolean blooming = state.getValue(ElysianVeilBloomBlock.BLOOMING);
        int light = blooming ? packedLight : LightTexture.FULL_BRIGHT;
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getBlockRenderer().getBlockModel(state);
        mc.getBlockRenderer().getModelRenderer().renderModel(
            stack.last(),
            bufferSource.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS)),
            state,
            model,
            1.0f, 1.0f, 1.0f,
            light,
            OverlayTexture.NO_OVERLAY);
    }
}
