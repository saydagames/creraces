package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;

/** Optional particle and sound feedback for the place/remove block actions; a bad id only skips its own part. */
final class BlockActionEffects {
    /** Gaussian spread of the particles around the block centre. */
    private static final double PARTICLE_SPREAD = 0.2;

    private BlockActionEffects() {
    }

    static void play(Player player, BlockPos pos, @Nullable String particle, @Nullable String sound,
            int particleCount) {
        Level level = player.level();
        ParticleOptions options = resolveParticle(particle);
        if (options != null) {
            RandomSource random = level.random;
            if (level instanceof ServerLevel serverLevel) {
                for (int i = 0; i < particleCount; i++) {
                    serverLevel.sendParticles(options, jitter(pos.getX(), random), jitter(pos.getY(), random),
                            jitter(pos.getZ(), random), 1, 0, 0.05, 0, 0.0);
                }
            } else if (level.isClientSide()) {
                for (int i = 0; i < particleCount; i++) {
                    level.addParticle(options, jitter(pos.getX(), random), jitter(pos.getY(), random),
                            jitter(pos.getZ(), random), 0, 0.05, 0);
                }
            }
        }

        if (sound != null && !sound.isEmpty()) {
            ResourceLocation soundId = ResourceLocation.tryParse(sound);
            if (soundId == null) {
                CreRaces.LOGGER.warn("Block action: malformed sound id '{}'", sound);
            } else {
                BuiltInRegistries.SOUND_EVENT.getOptional(soundId)
                        .ifPresent(s -> level.playSound(null, pos, s, SoundSource.BLOCKS, 1.0f, 1.0f));
            }
        }
    }

    /** A simple particle id is used as-is; a block id becomes that block's breaking particles. */
    @Nullable
    private static ParticleOptions resolveParticle(@Nullable String particle) {
        if (particle == null || particle.isEmpty()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(particle);
        if (id == null) {
            CreRaces.LOGGER.warn("Block action: malformed particle id '{}'", particle);
            return null;
        }
        var particleType = BuiltInRegistries.PARTICLE_TYPE.getOptional(id);
        if (particleType.isPresent() && particleType.get() instanceof ParticleOptions simple) {
            return simple;
        }
        var block = BuiltInRegistries.BLOCK.getOptional(id);
        if (block.isPresent() && block.get() != Blocks.AIR) {
            return new BlockParticleOption(ParticleTypes.BLOCK, block.get().defaultBlockState());
        }
        return null;
    }

    private static double jitter(int blockCoordinate, RandomSource random) {
        return blockCoordinate + 0.5 + random.nextGaussian() * PARTICLE_SPREAD;
    }
}
