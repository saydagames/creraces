package mc.sayda.creraces.worldgen;

import com.mojang.datafixers.util.Pair;
import mc.sayda.creraces.CreRaces;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

public class VeilwoodRegion extends Region {

    public VeilwoodRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        ResourceKey<Biome> veilwoodForest = ResourceKey.create(
                Registries.BIOME, new ResourceLocation(CreRaces.MODID, "veilwood_forest"));

        // TerraBlender's climate parameter space is fully covered by vanilla. There are no
        // gaps for new points to win nearest-neighbour. Replacing a vanilla climate slot is
        // unavoidable. Mangrove swamp sits in the same warm-humid coastal zone as regular
        // swamp and is rare enough that replacing it has minimal impact on vanilla variety.
        addModifiedVanillaOverworldBiomes(mapper, builder ->
                builder.replaceBiome(Biomes.MANGROVE_SWAMP, veilwoodForest));
    }
}
