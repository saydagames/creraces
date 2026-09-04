package mc.sayda.creraces.client.render;

import mc.sayda.creraces.client.model.TornadoModel;
import mc.sayda.creraces.entity.TornadoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("null")
public class TornadoRenderer extends MobRenderer<TornadoEntity, TornadoModel<TornadoEntity>> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/entities/wind_wall.png");

    public TornadoRenderer(EntityRendererProvider.Context context) {
        super(context, new TornadoModel<>(context.bakeLayer(TornadoModel.LAYER_LOCATION)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(TornadoEntity entity) {
        return TEXTURE;
    }
}
