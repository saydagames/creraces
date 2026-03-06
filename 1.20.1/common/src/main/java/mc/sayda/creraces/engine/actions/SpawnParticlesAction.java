package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

@SuppressWarnings("null")
public class SpawnParticlesAction implements ActionRegistry.RaceAction {

    public interface ParticlePattern {
        void spawn(ServerLevel level, Player player, net.minecraft.world.entity.LivingEntity target,
                ParticleOptions particle, int count, double speed, double spin);

        static ParticlePattern fromJson(JsonObject json) {
            String type = GsonHelper.getAsString(json, "type", "random");
            return switch (type.toLowerCase()) {
                case "circle" -> {
                    ScalingValue radius = ScalingValue.fromJson(json, "radius", 1.0);
                    ScalingValue count = ScalingValue.fromJson(json, "count", 8.0);
                    String axis = GsonHelper.getAsString(json, "axis", "Y");
                    yield new CirclePattern(radius, count, axis);
                }
                case "sphere" -> {
                    ScalingValue radius = ScalingValue.fromJson(json, "radius", 1.0);
                    ScalingValue count = ScalingValue.fromJson(json, "count", 16.0);
                    yield new SpherePattern(radius, count);
                }
                case "helix" -> {
                    ScalingValue radius = ScalingValue.fromJson(json, "radius", 1.0);
                    ScalingValue height = ScalingValue.fromJson(json, "height", 2.0);
                    ScalingValue count = ScalingValue.fromJson(json, "count", 20.0);
                    ScalingValue rotations = ScalingValue.fromJson(json, "rotations", 1.0);
                    yield new HelixPattern(radius, height, count, rotations);
                }
                default -> null;
            };
        }
    }

    private record CirclePattern(ScalingValue radius, ScalingValue points, String axis) implements ParticlePattern {
        @Override
        public void spawn(ServerLevel level, Player player, net.minecraft.world.entity.LivingEntity target,
                ParticleOptions particle, int count, double speed, double spin) {
            double r = radius.evaluate(player, target);
            int pCount = (int) points.evaluate(player, target);
            if (pCount <= 0)
                return;

            double spinOffset = level.getGameTime() * spin;
            for (int i = 0; i < pCount; i++) {
                double angle = (2 * Math.PI * i / pCount) + spinOffset;
                double dx = 0, dy = 0, dz = 0;
                if ("Y".equalsIgnoreCase(axis)) {
                    dx = Math.cos(angle) * r;
                    dz = Math.sin(angle) * r;
                    dy = 1.0;
                } else if ("X".equalsIgnoreCase(axis)) {
                    dy = Math.cos(angle) * r + 1.0;
                    dz = Math.sin(angle) * r;
                } else {
                    dx = Math.cos(angle) * r;
                    dy = Math.sin(angle) * r + 1.0;
                }
                level.sendParticles(particle, player.getX() + dx, player.getY() + dy, player.getZ() + dz, count, 0, 0,
                        0,
                        speed);
            }
        }
    }

    private record SpherePattern(ScalingValue radius, ScalingValue points) implements ParticlePattern {
        @Override
        public void spawn(ServerLevel level, Player player, net.minecraft.world.entity.LivingEntity target,
                ParticleOptions particle, int count, double speed, double spin) {
            double r = radius.evaluate(player, target);
            int pCount = (int) points.evaluate(player, target);
            if (pCount <= 0)
                return;

            double spinOffset = level.getGameTime() * spin;
            for (int i = 0; i < pCount; i++) {
                double phi = Math.acos(1 - 2 * (i + 0.5) / pCount);
                double theta = (Math.PI * (1 + Math.sqrt(5)) * (i + 0.5)) + spinOffset;
                double dx = r * Math.sin(phi) * Math.cos(theta);
                double dy = r * Math.sin(phi) * Math.sin(theta) + 1.0;
                double dz = r * Math.cos(phi);
                level.sendParticles(particle, player.getX() + dx, player.getY() + dy, player.getZ() + dz, count, 0, 0,
                        0,
                        speed);
            }
        }
    }

    private record HelixPattern(ScalingValue radius, ScalingValue height, ScalingValue points, ScalingValue rotations)
            implements ParticlePattern {
        @Override
        public void spawn(ServerLevel level, Player player, net.minecraft.world.entity.LivingEntity target,
                ParticleOptions particle, int count, double speed, double spin) {
            double r = radius.evaluate(player, target);
            double h = height.evaluate(player, target);
            int pCount = (int) points.evaluate(player, target);
            double rot = rotations.evaluate(player, target);
            if (pCount <= 0)
                return;

            double spinOffset = level.getGameTime() * spin;
            for (int i = 0; i < pCount; i++) {
                double t = (double) i / pCount;
                double angle = (2 * Math.PI * rot * t) + spinOffset;
                double dx = Math.cos(angle) * r;
                double dz = Math.sin(angle) * r;
                double dy = t * h;
                level.sendParticles(particle, player.getX() + dx, player.getY() + dy, player.getZ() + dz, count, 0, 0,
                        0,
                        speed);
            }
        }
    }

    private final ParticleOptions particle;
    private final ScalingValue count;
    private final ScalingValue speed;
    private final ScalingValue dx, dy, dz;
    private final ScalingValue spin;
    @javax.annotation.Nullable
    private final ParticlePattern pattern;

    public SpawnParticlesAction(ParticleOptions particle, ScalingValue count, ScalingValue speed, ScalingValue dx,
            ScalingValue dy,
            ScalingValue dz,
            ScalingValue spin, @javax.annotation.Nullable ParticlePattern pattern) {
        this.particle = particle;
        this.count = count;
        this.speed = speed;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.spin = spin;
        this.pattern = pattern;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        if (player.level() instanceof ServerLevel sl) {
            int pCount = (int) count.evaluate(player, target);
            if (pCount <= 0)
                return true;

            if (pattern != null) {
                pattern.spawn(sl, player, target, particle, pCount, speed.evaluate(player, target),
                        spin.evaluate(player, target));
            } else {
                sl.sendParticles(particle, player.getX(), player.getY() + 1.0, player.getZ(), pCount,
                        dx.evaluate(player, target), dy.evaluate(player, target), dz.evaluate(player, target),
                        speed.evaluate(player, target));
            }
        }
        return true;
    }

    public static void register() {
        ActionRegistry.ActionFactory factory = json -> {
            String particleId = GsonHelper.getAsString(json, "particle");
            if (particleId == null)
                return null;

            ResourceLocation loc = new ResourceLocation(particleId);
            ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(loc);

            ParticleOptions options;
            if (type instanceof ParticleOptions po) {
                options = po;
            } else if (type == net.minecraft.core.particles.ParticleTypes.BLOCK
                    || type == net.minecraft.core.particles.ParticleTypes.FALLING_DUST) {
                String blockId = GsonHelper.getAsString(json, "block", "minecraft:stone");
                net.minecraft.world.level.block.Block block = BuiltInRegistries.BLOCK
                        .get(new ResourceLocation(blockId));
                @SuppressWarnings("unchecked")
                net.minecraft.core.particles.ParticleType<net.minecraft.core.particles.BlockParticleOption> blockType = (net.minecraft.core.particles.ParticleType<net.minecraft.core.particles.BlockParticleOption>) type;
                options = new net.minecraft.core.particles.BlockParticleOption(blockType, block.defaultBlockState());
            } else if (type == net.minecraft.core.particles.ParticleTypes.ITEM) {
                String itemId = GsonHelper.getAsString(json, "item", "minecraft:stone");
                net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(itemId));
                @SuppressWarnings("unchecked")
                net.minecraft.core.particles.ParticleType<net.minecraft.core.particles.ItemParticleOption> itemType = (net.minecraft.core.particles.ParticleType<net.minecraft.core.particles.ItemParticleOption>) type;
                options = new net.minecraft.core.particles.ItemParticleOption(itemType,
                        new net.minecraft.world.item.ItemStack(item));
            } else {
                CreRaces.LOGGER.error("Invalid or unsupported complex particle type for id {}: {}", particleId, type);
                return null;
            }

            ScalingValue count = ScalingValue.fromJson(json, "count", 1.0);
            ScalingValue speed = ScalingValue.fromJson(json, "speed", 0.0);
            ScalingValue dx = ScalingValue.fromJson(json, "dx", 0.0);
            ScalingValue dy = ScalingValue.fromJson(json, "dy", 0.0);
            ScalingValue dz = ScalingValue.fromJson(json, "dz", 0.0);
            ScalingValue spin = ScalingValue.fromJson(json, "spin", 0.0);

            ParticlePattern pattern = null;
            if (json.has("pattern") && json.get("pattern").isJsonObject()) {
                pattern = ParticlePattern.fromJson(json.getAsJsonObject("pattern"));
            }

            return new SpawnParticlesAction(options, count, speed, dx, dy, dz, spin, pattern);
        };

        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "spawn_particles"), factory);
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "apply_particles"), factory);
    }
}
