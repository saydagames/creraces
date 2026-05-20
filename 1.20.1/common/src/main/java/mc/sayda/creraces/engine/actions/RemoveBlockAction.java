package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import mc.sayda.creraces.registry.ModGameRules;

public class RemoveBlockAction implements ActionRegistry.RaceAction {
    private final ScalingValue x;
    private final ScalingValue y;
    private final ScalingValue z;
    private final boolean useTarget;
    private final boolean useTargetBlock;
    private final boolean absolute;
    private final ScalingValue.MathOp coordinateMath;
    private final String particle;
    private final String sound;
    private final int particleCount;
    private final boolean bypass;

    public RemoveBlockAction(ScalingValue x, ScalingValue y, ScalingValue z, boolean useTarget, boolean useTargetBlock,
            boolean absolute, ScalingValue.MathOp coordinateMath, String particle, String sound, int particleCount,
            boolean bypass) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.useTarget = useTarget;
        this.useTargetBlock = useTargetBlock;
        this.absolute = absolute;
        this.coordinateMath = coordinateMath != null ? coordinateMath : ScalingValue.MathOp.FLOOR;
        this.particle = particle;
        this.sound = sound;
        this.particleCount = particleCount;
        this.bypass = bypass;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {

        if (!player.level().getGameRules().getBoolean(ModGameRules.RULE_RACEGRIEFING)) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("msg.creraces.race_griefing_disabled").withStyle(net.minecraft.ChatFormatting.RED), true);
            return false;
        }

        BlockPos basePos;
        if (absolute) {
            basePos = BlockPos.ZERO;
        } else if (useTarget && target != null) {
            basePos = target.blockPosition();
        } else if (useTargetBlock && interact_pos != null) {
            basePos = interact_pos;
        } else {
            double tx = player.getX();
            double ty = player.getY();
            double tz = player.getZ();

            int bx = (int) Math.floor(tx);
            int by = (int) Math.floor(ty);
            int bz = (int) Math.floor(tz);

            if (coordinateMath == ScalingValue.MathOp.ROUND) {
                bx = (int) Math.round(tx);
                by = (int) Math.round(ty);
                bz = (int) Math.round(tz);
            } else if (coordinateMath == ScalingValue.MathOp.CEIL) {
                bx = (int) Math.ceil(tx);
                by = (int) Math.ceil(ty);
                bz = (int) Math.ceil(tz);
            }
            basePos = new BlockPos(bx, by, bz);
        }

        int ox = (int) x.evaluate(player, target, slot);
        int oy = (int) y.evaluate(player, target, slot);
        int oz = (int) z.evaluate(player, target, slot);

        BlockPos finalPos = basePos.offset(ox, oy, oz);

        BlockState state = player.level().getBlockState(finalPos);
        float hardness = state.getDestroySpeed(player.level(), finalPos);

        float limit = mc.sayda.creraces.config.CreRacesConfig.REMOVE_BLOCK_HARDNESS_LIMIT.get().floatValue();

        boolean canRemove;
        if (bypass || limit < 0) {
            // Unrestricted
            canRemove = true;
        } else {
            // Limit active: protect unbreakable (hardness < 0) and check threshold (0 <= hardness <= limit)
            canRemove = (hardness >= 0 && hardness <= limit);
        }

        if (canRemove) {
            player.level().setBlockAndUpdate(finalPos, Blocks.AIR.defaultBlockState());

            if (particle != null && !particle.isEmpty()) {
                try {
                    ResourceLocation res = new ResourceLocation(particle);
                    net.minecraft.core.particles.ParticleOptions options = null;

                    var optParticle = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.getOptional(res);
                    if (optParticle.isPresent()
                            && optParticle.get() instanceof net.minecraft.core.particles.ParticleOptions opt) {
                        options = opt;
                    } else {
                        var optBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(res);
                        if (optBlock.isPresent() && optBlock.get() != net.minecraft.world.level.block.Blocks.AIR) {
                            options = new net.minecraft.core.particles.BlockParticleOption(
                                    net.minecraft.core.particles.ParticleTypes.BLOCK,
                                    optBlock.get().defaultBlockState());
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

            // Sound
            if (sound != null && !sound.isEmpty()) {
                try {
                    ResourceLocation res = new ResourceLocation(sound);
                    net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getOptional(res).ifPresent(s -> {
                        player.level().playSound(null, finalPos, s, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f,
                                1.0f);
                    });
                } catch (Exception e) {
                    // Ignore sound errors
                }
            }
            return true;
        }

        return false;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "remove_block"), json -> {
            ScalingValue x = ScalingValue.fromJson(json, "x", 0.0);
            ScalingValue y = ScalingValue.fromJson(json, "y", 0.0);
            ScalingValue z = ScalingValue.fromJson(json, "z", 0.0);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            boolean useTargetBlock = GsonHelper.getAsBoolean(json, "use_target_block", false);
            boolean absolute = GsonHelper.getAsBoolean(json, "absolute", false);
            String particle = GsonHelper.getAsString(json, "particle", "");
            int particleCount = GsonHelper.getAsInt(json, "particle_count", 10);
            String sound = GsonHelper.getAsString(json, "sound", "");
            boolean bypass = GsonHelper.getAsBoolean(json, "bypass", false);

            ScalingValue.MathOp coordinateMath = ScalingValue.MathOp.FLOOR;
            if (json.has("math")) {
                try {
                    String mode = json.get("math").getAsString().toUpperCase();
                    for (ScalingValue.MathOp op : ScalingValue.MathOp.values()) {
                        if (op.name().equals(mode)) {
                            coordinateMath = op;
                            break;
                        }
                    }
                } catch (Exception e) {
                    mc.sayda.creraces.CreRaces.LOGGER.warn("Invalid math mode in RemoveBlockAction: {}",
                            json.get("math").getAsString());
                }
            }
            return new RemoveBlockAction(x, y, z, useTarget, useTargetBlock, absolute, coordinateMath, particle,
                    sound, particleCount, bypass);
        });
    }
}
