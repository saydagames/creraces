package mc.sayda.creraces.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import mc.sayda.creraces.client.model.TrollPillarModel;
import mc.sayda.creraces.entity.TrollPillarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for {@link TrollPillarEntity}.
 * Uses the Blockbench-exported {@link TrollPillarModel} with the black stone
 * brick texture.
 */
public class TrollPillarRenderer extends MobRenderer<TrollPillarEntity, TrollPillarModel<TrollPillarEntity>> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("creraces",
            "textures/entities/black_stone_bricks.png");

    public TrollPillarRenderer(EntityRendererProvider.Context context) {
        super(context,
                new TrollPillarModel<>(context.bakeLayer(TrollPillarModel.LAYER_LOCATION)),
                0.5f);
    }

    @Override
    protected void scale(TrollPillarEntity entity, PoseStack poseStack, float partialTick) {
        float s = entity.getScale();
        poseStack.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(TrollPillarEntity entity) {
        return TEXTURE;
    }
}
