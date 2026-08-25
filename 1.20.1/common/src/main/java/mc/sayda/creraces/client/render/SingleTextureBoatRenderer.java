package mc.sayda.creraces.client.render;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;

/**
 * Base for boat renderers that swap only the OAK-slot texture for a single custom skin.
 */
public abstract class SingleTextureBoatRenderer extends BoatRenderer {
    private final ResourceLocation activeTexture;

    protected SingleTextureBoatRenderer(EntityRendererProvider.Context context, boolean hasChest,
            ResourceLocation texture, ResourceLocation chestTexture) {
        super(context, hasChest);
        this.activeTexture = hasChest ? chestTexture : texture;
        ImmutableMap.Builder<Boat.Type, Pair<ResourceLocation, ListModel<Boat>>> builder = ImmutableMap.builder();
        boatResources.forEach((type, pair) -> {
            if (type == Boat.Type.OAK) {
                builder.put(type, Pair.of(activeTexture, pair.getSecond()));
            } else {
                builder.put(type, pair);
            }
        });
        boatResources = builder.build();
    }

    @Override
    public ResourceLocation getTextureLocation(Boat entity) {
        return activeTexture;
    }
}
