package mc.sayda.creraces.client.render;

import mc.sayda.creraces.client.model.PoisonEmitterMobileModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

@SuppressWarnings("null")
public class PoisonEmitterMobileRenderer<T extends Mob> extends MobRenderer<T, PoisonEmitterMobileModel<T>> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/entities/poison_emitter.png");

    public PoisonEmitterMobileRenderer(EntityRendererProvider.Context context) {
        super(context, new PoisonEmitterMobileModel<>(context.bakeLayer(PoisonEmitterMobileModel.LAYER_LOCATION)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }
}
