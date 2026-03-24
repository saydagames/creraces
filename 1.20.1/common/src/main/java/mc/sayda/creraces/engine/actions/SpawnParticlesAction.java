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
                ParticleOptions particle, int count, double speed, double spin, @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot);

        static ParticlePattern fromJson(JsonObject json) {
            String type = GsonHelper.getAsString(json, "type", "circle");
            return switch (type.toLowerCase()) {
                case "circle" -> {
                    ScalingValue radius = ScalingValue.fromJson(json, "radius", 1.0);
                    ScalingValue count = ScalingValue.fromJson(json, "count", 10.0);
                    String axis = GsonHelper.getAsString(json, "axis", "Y");
                    yield new CirclePattern(radius, count, axis);
                }
                case "sphere" -> {
                    ScalingValue radius = ScalingValue.fromJson(json, "radius", 1.0);
                    ScalingValue count = ScalingValue.fromJson(json, "count", 20.0);
                    yield new SpherePattern(radius, count);
                }
                case "helix" -> {
                    ScalingValue radius = ScalingValue.fromJson(json, "radius", 1.0);
                    ScalingValue height = ScalingValue.fromJson(json, "height", 2.0);
                    ScalingValue count = ScalingValue.fromJson(json, "count", 30.0);
                    ScalingValue rotations = ScalingValue.fromJson(json, "rotations", 2.0);
                    yield new HelixPattern(radius, height, count, rotations);
                }
                default -> null;
            };
        }
    }

    private static class CirclePattern implements ParticlePattern {
        private final ScalingValue radius;
        private final ScalingValue points;
        private final String axis;

        public CirclePattern(ScalingValue radius, ScalingValue points, String axis) {
            this.radius = radius;
            this.points = points;
            this.axis = axis;
        }

        @Override
        public void spawn(ServerLevel level, Player player, net.minecraft.world.entity.LivingEntity target,
                ParticleOptions particle, int count, double speed, double spin, @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            double r = radius.evaluate(player, target, slot);
            int pCount = (int) points.evaluate(player, target, slot);
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

    private static class SpherePattern implements ParticlePattern {
        private final ScalingValue radius;
        private final ScalingValue points;

        public SpherePattern(ScalingValue radius, ScalingValue points) {
            this.radius = radius;
            this.points = points;
        }

        @Override
        public void spawn(ServerLevel level, Player player, net.minecraft.world.entity.LivingEntity target,
                ParticleOptions particle, int count, double speed, double spin, @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            double r = radius.evaluate(player, target, slot);
            int pCount = (int) points.evaluate(player, target, slot);
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

    private static class HelixPattern implements ParticlePattern {
        private final ScalingValue radius;
        private final ScalingValue height;
        private final ScalingValue points;
        private final ScalingValue rotations;

        public HelixPattern(ScalingValue radius, ScalingValue height, ScalingValue points, ScalingValue rotations) {
            this.radius = radius;
            this.height = height;
            this.points = points;
            this.rotations = rotations;
        }

        @Override
        public void spawn(ServerLevel level, Player player, net.minecraft.world.entity.LivingEntity target,
                ParticleOptions particle, int count, double speed, double spin, @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            double r = radius.evaluate(player, target, slot);
            double h = height.evaluate(player, target, slot);
            int pCount = (int) points.evaluate(player, target, slot);
            double rot = rotations.evaluate(player, target, slot);
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
    private final mc.sayda.creraces.engine.TargetFilter targets;
    @javax.annotation.Nullable
    private final ParticlePattern pattern;

    public SpawnParticlesAction(ParticleOptions particle, ScalingValue count, ScalingValue speed, ScalingValue dx,
            ScalingValue dy,
            ScalingValue dz,
            ScalingValue spin, mc.sayda.creraces.engine.TargetFilter targets, @javax.annotation.Nullable ParticlePattern pattern) {
        this.particle = particle;
        this.count = count;
        this.speed = speed;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.spin = spin;
        this.targets = targets;
        this.pattern = pattern;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        if (player.level() instanceof ServerLevel sl) {
            int pCount = (int) count.evaluate(player, target, slot);
            if (pCount <= 0)
                return true;

            CreRaces.LOGGER.info("SpawnParticlesAction: Spawning {} particles for {}", pCount, player.getName().getString());

            if (target != null) {
                // AoE Context: apply to valid target
                if (targets.isValid(target, player)) {
                    spawnOnTarget(sl, player, target, pCount, slot);
                }
            } else {
                // Non-AoE Context: apply to player if valid (e.g. self)
                if (targets.isValid(player, player)) {
                    spawnOnTarget(sl, player, player, pCount, slot);
                }
            }
        }
        return true;
    }

    private void spawnOnTarget(ServerLevel sl, Player player, net.minecraft.world.entity.LivingEntity actualTarget, int pCount, @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
        if (pattern != null) {
            pattern.spawn(sl, player, actualTarget, particle, pCount, speed.evaluate(player, actualTarget, slot),
                    spin.evaluate(player, actualTarget, slot), slot);
        } else {
            sl.sendParticles(particle, actualTarget.getX(), actualTarget.getY() + 1.0, actualTarget.getZ(), pCount,
                    dx.evaluate(player, actualTarget, slot), dy.evaluate(player, actualTarget, slot), dz.evaluate(player, actualTarget, slot),
                    speed.evaluate(player, actualTarget, slot));
        }
    }

    public static void register() {
        ActionRegistry.ActionFactory factory = json -> {
            String particleId = GsonHelper.getAsString(json, "particle");
            if (particleId.isEmpty())
                return null;

            ResourceLocation loc = new ResourceLocation(particleId);
            if (!BuiltInRegistries.PARTICLE_TYPE.containsKey(loc)) {
                CreRaces.LOGGER.error("SpawnParticlesAction: Unknown particle ID '{}'.", particleId);
                return null;
            }
            ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(loc);

            ParticleOptions options;
            if (type instanceof ParticleOptions po) {
                options = po;
            } else if (type == net.minecraft.core.particles.ParticleTypes.BLOCK
                    || type == net.minecraft.core.particles.ParticleTypes.FALLING_DUST) {
                String blockId = GsonHelper.getAsString(json, "block", "minecraft:air");
                ResourceLocation bLoc = new ResourceLocation(blockId);
                if (!BuiltInRegistries.BLOCK.containsKey(bLoc)) {
                    CreRaces.LOGGER.error("SpawnParticlesAction: Unknown block ID '{}' for block particle.", blockId);
                }
                net.minecraft.world.level.block.Block block = BuiltInRegistries.BLOCK.get(bLoc);
                @SuppressWarnings("unchecked")
                net.minecraft.core.particles.ParticleType<net.minecraft.core.particles.BlockParticleOption> blockType = (net.minecraft.core.particles.ParticleType<net.minecraft.core.particles.BlockParticleOption>) type;
                options = new net.minecraft.core.particles.BlockParticleOption(blockType, block.defaultBlockState());
            } else if (type == net.minecraft.core.particles.ParticleTypes.ITEM) {
                String itemId = GsonHelper.getAsString(json, "item", "minecraft:air");
                ResourceLocation iLoc = new ResourceLocation(itemId);
                if (!BuiltInRegistries.ITEM.containsKey(iLoc)) {
                    CreRaces.LOGGER.error("SpawnParticlesAction: Unknown item ID '{}' for item particle.", itemId);
                }
                net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(iLoc);
                @SuppressWarnings("unchecked")
                net.minecraft.core.particles.ParticleType<net.minecraft.core.particles.ItemParticleOption> itemType = (net.minecraft.core.particles.ParticleType<net.minecraft.core.particles.ItemParticleOption>) type;
                options = new net.minecraft.core.particles.ItemParticleOption(itemType,
                        new net.minecraft.world.item.ItemStack(item));
            } else {
                CreRaces.LOGGER.error("Invalid or unsupported complex particle type for id {}: {}", particleId, type);
                return null;
            }

            ScalingValue count = ScalingValue.fromJson(json, "count", 10.0);
            ScalingValue speed = ScalingValue.fromJson(json, "speed", 0.0);
            ScalingValue spread = ScalingValue.fromJson(json, "spread", 0.0);
            ScalingValue dx = json.has("dx") ? ScalingValue.fromJson(json, "dx", 0.0) : spread;
            ScalingValue dy = json.has("dy") ? ScalingValue.fromJson(json, "dy", 0.0) : spread;
            ScalingValue dz = json.has("dz") ? ScalingValue.fromJson(json, "dz", 0.0) : spread;
            ScalingValue spin = ScalingValue.fromJson(json, "spin", 0.0);
            mc.sayda.creraces.engine.TargetFilter targets = mc.sayda.creraces.engine.TargetFilter.fromJson(json,
                    "targets", java.util.Set.of("enemies", "self"));

            ParticlePattern pattern = null;
            if (json.has("pattern") && json.get("pattern").isJsonObject()) {
                pattern = ParticlePattern.fromJson(json.getAsJsonObject("pattern"));
            }

            return new SpawnParticlesAction(options, count, speed, dx, dy, dz, spin, targets, pattern);
        };

        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "spawn_particles"), factory);
    }
}
