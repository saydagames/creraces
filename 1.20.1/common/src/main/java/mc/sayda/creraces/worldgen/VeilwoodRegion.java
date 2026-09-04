package mc.sayda.creraces.worldgen;

import com.mojang.datafixers.util.Pair;
import mc.sayda.creraces.CreRaces;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.ParameterUtils.Continentalness;
import terrablender.api.ParameterUtils.Depth;
import terrablender.api.ParameterUtils.Erosion;
import terrablender.api.ParameterUtils.Humidity;
import terrablender.api.ParameterUtils.ParameterPointListBuilder;
import terrablender.api.ParameterUtils.Temperature;
import terrablender.api.Region;
import terrablender.api.RegionType;
import terrablender.api.VanillaParameterOverlayBuilder;

import java.util.function.Consumer;

public class VeilwoodRegion extends Region {

    public VeilwoodRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        ResourceKey<Biome> veilwoodForest = ResourceKey.create(
                Registries.BIOME, new ResourceLocation(CreRaces.MODID, "veilwood_forest"));

        // Overlays Veilwood Forest onto mangrove swamp's own climate range (warm, humid,
        // near-to-far inland, low erosion) without touching mangrove's points - unclaimed space
        // defers to vanilla, so the two never tie over the same point.
        VanillaParameterOverlayBuilder overlay = new VanillaParameterOverlayBuilder();
        new ParameterPointListBuilder()
                .temperature(Temperature.span(Temperature.WARM, Temperature.HOT))
                .humidity(Humidity.FULL_RANGE)
                .continentalness(Continentalness.span(Continentalness.NEAR_INLAND, Continentalness.FAR_INLAND))
                .erosion(Erosion.EROSION_6)
                .depth(Depth.SURFACE)
                .build().forEach(point -> overlay.add(point, veilwoodForest));
        overlay.build().forEach(mapper);
    }
}
