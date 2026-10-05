package mc.sayda.creraces.mixin;

import mc.sayda.creraces.CreRaces;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Works around Mojang bug MC-273228/MC-272370: LakeFeature.place() calls getBiome() for water-tagged
 * fluids (to decide whether the surface freezes), and on a freshly forced chunk that lookup can reach an
 * ungenerated neighbour and throw "Requested chunk unavailable during world generation". Vanilla only
 * places lava lakes, but creraces:ethereal_veil_spring uses the water-tagged creraces:eterveil so it
 * freezes in cold biomes. Falls back to the uncached noise biome, which needs no chunk; same technique
 * as the MIT-licensed fix at github.com/SimonShiki/worldgen-feature-fix.
 */
@Mixin(LakeFeature.class)
public abstract class LakeFeatureMixin {
    @Redirect(method = "place", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/WorldGenLevel;getBiome(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;"))
    private Holder<Biome> creraces$getBiomeOrUncached(WorldGenLevel level, BlockPos pos) {
        try {
            return level.getBiome(pos);
        } catch (Exception e) {
            CreRaces.LOGGER.warn(
                    "[CreRaces] LakeFeature.getBiome() failed at {} (chunk unavailable during world generation), falling back to the uncached noise biome: {}",
                    pos, e.getMessage());
            return level.getUncachedNoiseBiome(pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
