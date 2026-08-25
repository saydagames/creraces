package mc.sayda.creraces.engine.actions;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** Shared "resilient" (exception-swallowing) particle+sound feedback for block place/remove actions. */
final class BlockActionEffects {
    private BlockActionEffects() {
    }

    static void spawnResilientEffects(Player player, BlockPos finalPos, String particle, String sound,
            int particleCount) {
        if (particle != null && !particle.isEmpty()) {
            try {
                ResourceLocation res = new ResourceLocation(particle);
                net.minecraft.core.particles.ParticleOptions options = null;

                var optParticle = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.getOptional(res);
                if (optParticle.isPresent() && optParticle.get() instanceof net.minecraft.core.particles.ParticleOptions opt) {
                    options = opt;
                } else {
                    var optBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(res);
                    if (optBlock.isPresent() && optBlock.get() != net.minecraft.world.level.block.Blocks.AIR) {
                        options = new net.minecraft.core.particles.BlockParticleOption(
                                net.minecraft.core.particles.ParticleTypes.BLOCK, optBlock.get().defaultBlockState());
                    }
                }

                if (options != null) {
                    if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        for (int i = 0; i < particleCount; i++) {
                            serverLevel.sendParticles(options,
                                    finalPos.getX() + 0.5 + player.level().random.nextGaussian() * 0.2,
                                    finalPos.getY() + 0.5 + player.level().random.nextGaussian() * 0.2,
                                    finalPos.getZ() + 0.5 + player.level().random.nextGaussian() * 0.2,
                                    1, 0, 0.05, 0, 0.0);
                        }
                    } else if (player.level().isClientSide()) {
                        for (int i = 0; i < particleCount; i++) {
                            player.level().addParticle(options,
                                    finalPos.getX() + 0.5 + player.level().random.nextGaussian() * 0.2,
                                    finalPos.getY() + 0.5 + player.level().random.nextGaussian() * 0.2,
                                    finalPos.getZ() + 0.5 + player.level().random.nextGaussian() * 0.2,
                                    0, 0.05, 0);
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore particle errors
            }
        }

        if (sound != null && !sound.isEmpty()) {
            try {
                ResourceLocation res = new ResourceLocation(sound);
                net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getOptional(res).ifPresent(s -> {
                    player.level().playSound(null, finalPos, s, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
                });
            } catch (Exception e) {
                // Ignore sound errors
            }
        }
    }
}
