package mc.sayda.creraces.client.render;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import mc.sayda.creraces.entity.RemainsEntity;
import mc.sayda.creraces.entity.UndeadRemainsEntity;
import mc.sayda.creraces.client.model.RemainsModel;

public class RemainsRenderer extends MobRenderer<RemainsEntity, RemainsModel<RemainsEntity>> {
	private static final ResourceLocation NORMAL_TEXTURE = new ResourceLocation("creraces", "textures/entities/remains.png");
	private static final ResourceLocation UNDEAD_TEXTURE = new ResourceLocation("creraces", "textures/entities/remains_undead.png");

	public RemainsRenderer(EntityRendererProvider.Context context) {
		super(context, new RemainsModel<>(context.bakeLayer(RemainsModel.LAYER_LOCATION)), 0.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(RemainsEntity entity) {
		if (entity instanceof UndeadRemainsEntity) {
			return UNDEAD_TEXTURE;
		}
		return NORMAL_TEXTURE;
	}
}
