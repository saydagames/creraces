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

// Vanilla's overworld with every mangrove swamp swapped for Veilwood Forest. Don't switch this to
// VanillaParameterOverlayBuilder: it spreads the biome over most warm inland climates.
public class VeilwoodRegion extends Region {
    private static final ResourceKey<Biome> VEILWOOD_FOREST = ResourceKey.create(
            Registries.BIOME, new ResourceLocation(CreRaces.MODID, "veilwood_forest"));

    public VeilwoodRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        addModifiedVanillaOverworldBiomes(mapper,
                builder -> builder.replaceBiome(Biomes.MANGROVE_SWAMP, VEILWOOD_FOREST));
    }
}
