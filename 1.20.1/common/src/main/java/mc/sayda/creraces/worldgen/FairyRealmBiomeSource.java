package mc.sayda.creraces.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

/**
 * Assigns vanilla biomes to four seasonal quadrants using block-coordinate quadrant split.
 * River/moat corridors are detected via {@link FairyRealmChunkGenerator#isWater} so the
 * biome assignment always agrees with where the chunk generator actually places water.
 *
 *   Island center         → center (plains)
 *   NE (x>=0, z<0)        → spring  (cherry_grove)
 *   SE (x>=0, z>=0)       → summer  (forest)
 *   NW (x<0,  z<0)        → winter  (snowy_taiga)
 *   SW (x<0,  z>=0)       → autumn  (taiga)
 *
 * Water zones in the winter quadrant (NW) return frozen_river; elsewhere river.
 */
public class FairyRealmBiomeSource extends BiomeSource {

    private final Holder<Biome> center;
    private final Holder<Biome> spring;
    private final Holder<Biome> summer;
    private final Holder<Biome> winter;
    private final Holder<Biome> autumn;
    private final Holder<Biome> river;
    private final Holder<Biome> frozenRiver;

    public static final Codec<FairyRealmBiomeSource> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Biome.CODEC.fieldOf("center").forGetter(b -> b.center),
                    Biome.CODEC.fieldOf("spring").forGetter(b -> b.spring),
                    Biome.CODEC.fieldOf("summer").forGetter(b -> b.summer),
                    Biome.CODEC.fieldOf("winter").forGetter(b -> b.winter),
                    Biome.CODEC.fieldOf("autumn").forGetter(b -> b.autumn),
                    Biome.CODEC.fieldOf("river").forGetter(b -> b.river),
                    Biome.CODEC.fieldOf("frozen_river").forGetter(b -> b.frozenRiver)
            ).apply(instance, FairyRealmBiomeSource::new)
    );

    public FairyRealmBiomeSource(Holder<Biome> center, Holder<Biome> spring, Holder<Biome> summer,
                                  Holder<Biome> winter, Holder<Biome> autumn,
                                  Holder<Biome> river, Holder<Biome> frozenRiver) {
        this.center = center;
        this.spring = spring;
        this.summer = summer;
        this.winter = winter;
        this.autumn = autumn;
        this.river = river;
        this.frozenRiver = frozenRiver;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(center, spring, summer, winter, autumn, river, frozenRiver);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        // Biome coords are 1/4 of block coords
        int bx = x << 2;
        int bz = z << 2;

        // Island center gets plains biome
        if (FairyRealmChunkGenerator.isIsland(bx, bz)) {
            return center;
        }

        // Use the same geometry as the chunk generator so biomes match actual water placement
        if (FairyRealmChunkGenerator.isWater(bx, bz)) {
            return (bx < 0 && bz < 0) ? frozenRiver : river;
        }

        // Seasonal quadrant assignment
        if (bx >= 0 && bz < 0) return spring;   // NE
        if (bx >= 0)            return summer;   // SE
        if (bz < 0)             return winter;   // NW
        return autumn;                            // SW
    }
}
