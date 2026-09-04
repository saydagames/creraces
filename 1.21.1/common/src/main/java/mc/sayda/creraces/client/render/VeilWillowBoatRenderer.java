package mc.sayda.creraces.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class VeilWillowBoatRenderer extends SingleTextureBoatRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("creraces", "textures/entity/boat/veil_willow.png");
    private static final ResourceLocation CHEST_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("creraces", "textures/entity/chest_boat/veil_wood.png");

    public VeilWillowBoatRenderer(EntityRendererProvider.Context context, boolean hasChest) {
        super(context, hasChest, TEXTURE, CHEST_TEXTURE);
    }
}
