package mc.sayda.creraces.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class DryadBoatRenderer extends SingleTextureBoatRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("creraces", "textures/entity/boat/dryad.png");
    private static final ResourceLocation CHEST_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("creraces", "textures/entity/chest_boat/dryad.png");

    public DryadBoatRenderer(EntityRendererProvider.Context context, boolean hasChest) {
        super(context, hasChest, TEXTURE, CHEST_TEXTURE);
    }
}
