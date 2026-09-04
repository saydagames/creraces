package mc.sayda.creraces.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Works around Mojang bug MC-273228/MC-272370: LakeFeature.place() calls getBiome() (only for
 * water-tagged fluids, to decide whether to freeze the lake's surface), but on a freshly forced
 * chunk (e.g. a player teleporting straight into unexplored terrain) that call can reach a
 * neighbor chunk that isn't generated yet, throwing IllegalStateException ("Requested chunk
 * unavailable during world generation") and crashing chunk generation. Vanilla never hits this
 * because it only ever uses LakeFeature for lava lakes; creraces:ethereal_veil_spring uses a
 * water-tagged custom fluid (creraces:eterveil) specifically so it freezes over in cold biomes,
 * so it does. Not yet observed on this module (1.20.1's older chunk-generation pipeline doesn't
 * seem to hit it), but the same latent bug exists here too, applied for consistency with 1.21.1.
 * Falls back to the uncached noise-based biome, which needs no real chunk, whenever the direct
 * lookup fails. Same technique as the (MIT-licensed) reference fix at
 * github.com/SimonShiki/worldgen-feature-fix.
 */
@Mixin(LakeFeature.class)
public abstract class LakeFeatureMixin {
    @Redirect(method = "place", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/WorldGenLevel;getBiome(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;"))
    private Holder<Biome> creraces$getBiomeOrUncached(WorldGenLevel level, BlockPos pos) {
        try {
            return level.getBiome(pos);
        } catch (Exception e) {
            mc.sayda.creraces.CreRaces.LOGGER.warn(
                    "[CreRaces] LakeFeature.getBiome() failed at {} (chunk unavailable during world generation), falling back to the uncached noise biome: {}",
                    pos, e.getMessage());
            return level.getUncachedNoiseBiome(pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
