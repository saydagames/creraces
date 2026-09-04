package mc.sayda.creraces.client.render;

import mc.sayda.creraces.client.model.PoisonEmitterModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

@SuppressWarnings("null")
public class PoisonEmitterRenderer<T extends Mob> extends MobRenderer<T, PoisonEmitterModel<T>> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/entities/poison_emitter.png");

    public PoisonEmitterRenderer(EntityRendererProvider.Context context) {
        super(context, new PoisonEmitterModel<>(context.bakeLayer(PoisonEmitterModel.LAYER_LOCATION)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }
}
