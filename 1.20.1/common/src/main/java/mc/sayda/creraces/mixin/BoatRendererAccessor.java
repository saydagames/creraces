package mc.sayda.creraces.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** Exposes BoatRenderer.boatResources so a single-texture boat skin can swap the OAK-slot entry. */
@Mixin(BoatRenderer.class)
public interface BoatRendererAccessor {
    @Accessor("boatResources")
    Map<Boat.Type, Pair<ResourceLocation, ListModel<Boat>>> creraces$getBoatResources();

    @Accessor("boatResources")
    @Mutable
    void creraces$setBoatResources(Map<Boat.Type, Pair<ResourceLocation, ListModel<Boat>>> boatResources);
}
