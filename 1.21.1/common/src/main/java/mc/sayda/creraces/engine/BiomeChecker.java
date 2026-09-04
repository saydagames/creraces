package mc.sayda.creraces.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

public final class BiomeChecker {

    private BiomeChecker() {}

    /**
     * Returns true if the biome holder matches any entry in the list.
     * Supported entry formats:
     *   "#namespace:tag"      - biome tag membership
     *   "temp>=X" / "temp<=X" / "temp>X" / "temp<X" / "temp==X" - base temperature comparison
     *   "namespace:biome_id"  - exact biome resource location match
     */
    public static boolean matches(Holder<Biome> holder, List<String> entries) {
        for (String entry : entries) {
            if (matchesEntry(holder, entry)) return true;
        }
        return false;
    }

    /**
     * Samples a 4×4 grid across the chunk (one sample per 4-block cell, matching
     * Minecraft's biome storage resolution) and returns true if the fraction of
     * matching samples meets or exceeds {@code threshold}.
     */
    @SuppressWarnings("null")
    public static boolean matchesChunk(LevelReader level, ChunkPos cp, int sampleY,
            List<String> entries, float threshold) {
        if (entries.isEmpty()) return true;
        int matched = 0;
        for (int sx = 2; sx < 16; sx += 4) {
            for (int sz = 2; sz < 16; sz += 4) {
                Holder<Biome> h = level.getBiome(new BlockPos(cp.x * 16 + sx, sampleY, cp.z * 16 + sz));
                if (matches(h, entries)) matched++;
            }
        }
        return matched / 16.0f >= threshold;
    }

    @SuppressWarnings("null")
    public static boolean matchesEntry(Holder<Biome> holder, String entry) {
        if (entry.startsWith("#")) {
            try {
                TagKey<Biome> tag = TagKey.create(Registries.BIOME, ResourceLocation.parse(entry.substring(1)));
                return holder.is(tag);
            } catch (Exception ignored) {
                return false;
            }
        }
        if (entry.startsWith("temp")) {
            float temp = holder.value().getBaseTemperature();
            try {
                if (entry.startsWith("temp>=")) return temp >= Float.parseFloat(entry.substring(6));
                if (entry.startsWith("temp<=")) return temp <= Float.parseFloat(entry.substring(6));
                if (entry.startsWith("temp>"))  return temp >  Float.parseFloat(entry.substring(5));
                if (entry.startsWith("temp<"))  return temp <  Float.parseFloat(entry.substring(5));
                if (entry.startsWith("temp==")) return temp == Float.parseFloat(entry.substring(6));
            } catch (NumberFormatException ignored) {}
            return false;
        }
        return holder.unwrapKey().map(k -> k.location().toString().equals(entry)).orElse(false);
    }
}
