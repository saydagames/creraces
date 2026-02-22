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
                    double rotations = GsonHelper.getAsDouble(json, "rotations", 1.0);
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

    private record HelixPattern(ScalingValue radius, ScalingValue height, ScalingValue points, double rotations)
            implements ParticlePattern {
        @Override
        public void spawn(ServerLevel level, Player player, net.minecraft.world.entity.LivingEntity target,
                ParticleOptions particle, int count, double speed, double spin) {
            double r = radius.evaluate(player, target);
            double h = height.evaluate(player, target);
            int pCount = (int) points.evaluate(player, target);
            if (pCount <= 0)
                return;

            double spinOffset = level.getGameTime() * spin;
            for (int i = 0; i < pCount; i++) {
                double t = (double) i / pCount;
                double angle = (2 * Math.PI * rotations * t) + spinOffset;
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
    private final double speed;
    private final double dx, dy, dz;
    private final double spin;
    @javax.annotation.Nullable
    private final ParticlePattern pattern;

    public SpawnParticlesAction(ParticleOptions particle, ScalingValue count, double speed, double dx, double dy,
            double dz,
            double spin, @javax.annotation.Nullable ParticlePattern pattern) {
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
    public void execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
        if (player.level() instanceof ServerLevel sl) {
            int pCount = (int) count.evaluate(player, target);
            if (pCount <= 0)
                return;

            if (pattern != null) {
                pattern.spawn(sl, player, target, particle, pCount, speed, spin);
            } else {
                sl.sendParticles(particle, player.getX(), player.getY() + 1.0, player.getZ(), pCount, dx, dy, dz,
                        speed);
            }
        }
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "spawn_particles"), json -> {
            String particleId = GsonHelper.getAsString(json, "particle");
            if (particleId == null)
                return null;

            ResourceLocation loc = new ResourceLocation(particleId);
            ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(loc);
            if (!(type instanceof ParticleOptions options)) {
                CreRaces.LOGGER.error("Invalid particle type for id {}: {}", particleId, type);
                return null;
            }

            ScalingValue count = ScalingValue.fromJson(json, "count", 1.0);
            double speed = GsonHelper.getAsDouble(json, "speed", 0.0);
            double dx = GsonHelper.getAsDouble(json, "dx", 0.0);
            double dy = GsonHelper.getAsDouble(json, "dy", 0.0);
            double dz = GsonHelper.getAsDouble(json, "dz", 0.0);
            double spin = GsonHelper.getAsDouble(json, "spin", 0.0);

            ParticlePattern pattern = null;
            if (json.has("pattern") && json.get("pattern").isJsonObject()) {
                pattern = ParticlePattern.fromJson(json.getAsJsonObject("pattern"));
            }

            return new SpawnParticlesAction(options, count, speed, dx, dy, dz, spin, pattern);
        });
    }
}
