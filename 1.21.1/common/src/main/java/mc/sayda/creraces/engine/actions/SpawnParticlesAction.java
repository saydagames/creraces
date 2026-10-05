package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.SpiritMobilityHandler;
import mc.sayda.creraces.engine.TargetFilter;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Spawns particles on the target (or the caster when there is none): a plain burst around the body,
 * or a shape ("circle", "disc", "sphere", "helix") whose points share the particle count. Casters in
 * the spirit realm only show their particles to other spirits.
 */
@SuppressWarnings("null")
public class SpawnParticlesAction implements ActionRegistry.RaceAction {

    private interface ParticlePattern {
        void spawn(ServerLevel level, @Nullable Player caster, LivingEntity center, ParticleOptions particle,
                int totalCount, double speed, double spin, @Nullable AbilitySlot slot);
    }

    private final ParticleOptions particle;
    private final ScalingValue count;
    private final ScalingValue speed;
    private final ScalingValue dx;
    private final ScalingValue dy;
    private final ScalingValue dz;
    private final ScalingValue spin;
    private final TargetFilter targets;
    @Nullable
    private final ParticlePattern pattern;

    private SpawnParticlesAction(ParticleOptions particle, ScalingValue count, ScalingValue speed, ScalingValue dx,
            ScalingValue dy, ScalingValue dz, ScalingValue spin, TargetFilter targets,
            @Nullable ParticlePattern pattern) {
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
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (!(player.level() instanceof ServerLevel level)) {
            return true;
        }
        int particleCount = (int) count.evaluate(player, target, slot);
        if (particleCount <= 0) {
            return true;
        }
        LivingEntity center = target != null ? target : player;
        if (targets.isValid(center, player)) {
            spawnOn(level, center, particleCount, slot, player);
        }
        return true;
    }

    private void spawnOn(ServerLevel level, LivingEntity center, int particleCount, @Nullable AbilitySlot slot,
            Player caster) {
        if (pattern != null) {
            pattern.spawn(level, caster, center, particle, particleCount, speed.evaluate(null, center, slot),
                    spin.evaluate(null, center, slot), slot);
        } else {
            send(level, caster, particle, center.getX(), center.getY() + 1.0, center.getZ(), particleCount,
                    dx.evaluate(null, center, slot), dy.evaluate(null, center, slot),
                    dz.evaluate(null, center, slot), speed.evaluate(null, center, slot));
        }
    }

    private static void send(ServerLevel level, @Nullable Player caster, ParticleOptions particle, double x,
            double y, double z, int count, double spreadX, double spreadY, double spreadZ, double speed) {
        if (caster != null && SpiritMobilityHandler.isOnSpiritPlane(caster)) {
            SpiritMobilityHandler.sendParticlesIfSpirit(level, particle, x, y, z, count, spreadX, spreadY, spreadZ,
                    speed);
        } else {
            level.sendParticles(particle, x, y, z, count, spreadX, spreadY, spreadZ, speed);
        }
    }

    /** Sends {@code count} particles at a point offset from the entity's feet. */
    private static void sendAt(ServerLevel level, @Nullable Player caster, ParticleOptions particle,
            LivingEntity center, Vec3 offset, int count, double speed) {
        send(level, caster, particle, center.getX() + offset.x, center.getY() + offset.y, center.getZ() + offset.z,
                count, 0, 0, 0, speed);
    }

    /** Splits the total particle count across the points as evenly as possible. */
    private static int countAtPoint(int point, int totalCount, int points) {
        return totalCount / points + (point < totalCount % points ? 1 : 0);
    }

    /** A point at {@code angle} on a ring of the given radius around the axis, lifted one block off the feet. */
    private static Vec3 ringOffset(String axis, double angle, double radius) {
        double a = Math.cos(angle) * radius;
        double b = Math.sin(angle) * radius;
        if ("Y".equalsIgnoreCase(axis)) {
            return new Vec3(a, 1.0, b);
        }
        if ("X".equalsIgnoreCase(axis)) {
            return new Vec3(0, a + 1.0, b);
        }
        return new Vec3(a, b + 1.0, 0);
    }

    /**
     * Shape sizes are scaled against the centre entity when it is a player, and with no player
     * otherwise, rather than against the caster.
     */
    @Nullable
    private static Player scalingPlayer(LivingEntity center) {
        return center instanceof Player p ? p : null;
    }

    /** Evenly spaced points on a ring, rotating over time with "spin". */
    private record CirclePattern(ScalingValue radius, ScalingValue points, String axis) implements ParticlePattern {
        @Override
        public void spawn(ServerLevel level, @Nullable Player caster, LivingEntity center, ParticleOptions particle,
                int totalCount, double speed, double spin, @Nullable AbilitySlot slot) {
            Player player = scalingPlayer(center);
            double r = radius.evaluate(player, center, slot);
            int pointCount = (int) points.evaluate(player, center, slot);
            if (pointCount <= 0 || totalCount <= 0) {
                return;
            }
            double spinOffset = level.getGameTime() * spin;
            for (int i = 0; i < pointCount; i++) {
                int pointParticles = countAtPoint(i, totalCount, pointCount);
                if (pointParticles > 0) {
                    double angle = (2 * Math.PI * i / pointCount) + spinOffset;
                    sendAt(level, caster, particle, center, ringOffset(axis, angle, r), pointParticles, speed);
                }
            }
        }
    }

    /** Random points spread evenly over a filled disc. */
    private record DiscPattern(ScalingValue radius, ScalingValue points, String axis) implements ParticlePattern {
        @Override
        public void spawn(ServerLevel level, @Nullable Player caster, LivingEntity center, ParticleOptions particle,
                int totalCount, double speed, double spin, @Nullable AbilitySlot slot) {
            Player player = scalingPlayer(center);
            double maxRadius = radius.evaluate(player, center, slot);
            int pointCount = (int) points.evaluate(player, center, slot);
            if (pointCount <= 0 || totalCount <= 0) {
                return;
            }
            RandomSource random = level.getRandom();
            for (int i = 0; i < pointCount; i++) {
                int pointParticles = countAtPoint(i, totalCount, pointCount);
                if (pointParticles > 0) {
                    // The square root keeps the density even instead of bunching points at the centre.
                    double r = maxRadius * Math.sqrt(random.nextDouble());
                    double angle = 2 * Math.PI * random.nextDouble();
                    sendAt(level, caster, particle, center, ringOffset(axis, angle, r), pointParticles, speed);
                }
            }
        }
    }

    private record SpherePattern(ScalingValue radius, ScalingValue points) implements ParticlePattern {
        @Override
        public void spawn(ServerLevel level, @Nullable Player caster, LivingEntity center, ParticleOptions particle,
                int totalCount, double speed, double spin, @Nullable AbilitySlot slot) {
            Player player = scalingPlayer(center);
            double r = radius.evaluate(player, center, slot);
            int pointCount = (int) points.evaluate(player, center, slot);
            if (pointCount <= 0 || totalCount <= 0) {
                return;
            }
            double spinOffset = level.getGameTime() * spin;
            for (int i = 0; i < pointCount; i++) {
                int pointParticles = countAtPoint(i, totalCount, pointCount);
                if (pointParticles > 0) {
                    // Fibonacci sphere: spreads the points evenly using the golden angle.
                    double phi = Math.acos(1 - 2 * (i + 0.5) / pointCount);
                    double theta = (Math.PI * (1 + Math.sqrt(5)) * (i + 0.5)) + spinOffset;
                    Vec3 offset = new Vec3(r * Math.sin(phi) * Math.cos(theta),
                            r * Math.sin(phi) * Math.sin(theta) + 1.0, r * Math.cos(phi));
                    sendAt(level, caster, particle, center, offset, pointParticles, speed);
                }
            }
        }
    }

    /** A spiral rising from the feet to "height" over "rotations" turns. */
    private record HelixPattern(ScalingValue radius, ScalingValue height, ScalingValue points,
            ScalingValue rotations) implements ParticlePattern {
        @Override
        public void spawn(ServerLevel level, @Nullable Player caster, LivingEntity center, ParticleOptions particle,
                int totalCount, double speed, double spin, @Nullable AbilitySlot slot) {
            Player player = scalingPlayer(center);
            double r = radius.evaluate(player, center, slot);
            double h = height.evaluate(player, center, slot);
            int pointCount = (int) points.evaluate(player, center, slot);
            double turns = rotations.evaluate(player, center, slot);
            if (pointCount <= 0 || totalCount <= 0) {
                return;
            }
            double spinOffset = level.getGameTime() * spin;
            for (int i = 0; i < pointCount; i++) {
                int pointParticles = countAtPoint(i, totalCount, pointCount);
                if (pointParticles > 0) {
                    double t = (double) i / pointCount;
                    double angle = (2 * Math.PI * turns * t) + spinOffset;
                    Vec3 offset = new Vec3(Math.cos(angle) * r, t * h, Math.sin(angle) * r);
                    sendAt(level, caster, particle, center, offset, pointParticles, speed);
                }
            }
        }
    }

    /** Builds the shape named in "shape"; the point count defaults to the particle count. */
    @Nullable
    private static ParticlePattern parsePattern(String shape, JsonObject json, ScalingValue defaultPoints) {
        ScalingValue radius = ScalingValue.fromJson(json, "radius", 1.0);
        ScalingValue points = json.has("points") ? ScalingValue.fromJson(json, "points", 1.0) : defaultPoints;
        return switch (shape.toLowerCase()) {
            case "circle" -> new CirclePattern(radius, points, GsonHelper.getAsString(json, "axis", "Y"));
            case "disc" -> new DiscPattern(radius, points, GsonHelper.getAsString(json, "axis", "Y"));
            case "sphere" -> new SpherePattern(radius, points);
            case "helix" -> new HelixPattern(radius, ScalingValue.fromJson(json, "height", 2.0), points,
                    ScalingValue.fromJson(json, "rotations", 2.0));
            default -> {
                CreRaces.LOGGER.warn("SpawnParticlesAction: unknown shape '{}', spawning a plain burst.", shape);
                yield null;
            }
        };
    }

    /**
     * Simple particles are used as-is; block, falling-dust and item particles take their "block" or
     * "item", and entity_effect its "color".
     */
    @Nullable
    @SuppressWarnings("unchecked")
    private static ParticleOptions parseParticle(JsonObject json, String particleId, ParticleType<?> type) {
        if (type instanceof ParticleOptions simple) {
            return simple;
        }
        if (type == ParticleTypes.BLOCK || type == ParticleTypes.FALLING_DUST) {
            String blockId = GsonHelper.getAsString(json, "block", "minecraft:air");
            ResourceLocation blockLoc = ResourceLocation.parse(blockId);
            if (!BuiltInRegistries.BLOCK.containsKey(blockLoc)) {
                CreRaces.LOGGER.error("SpawnParticlesAction: Unknown block ID '{}' for block particle.", blockId);
                return null;
            }
            return new BlockParticleOption((ParticleType<BlockParticleOption>) type,
                    BuiltInRegistries.BLOCK.get(blockLoc).defaultBlockState());
        }
        if (type == ParticleTypes.ITEM) {
            String itemId = GsonHelper.getAsString(json, "item", "minecraft:air");
            ResourceLocation itemLoc = ResourceLocation.parse(itemId);
            if (!BuiltInRegistries.ITEM.containsKey(itemLoc)) {
                CreRaces.LOGGER.error("SpawnParticlesAction: Unknown item ID '{}' for item particle.", itemId);
                return null;
            }
            return new ItemParticleOption((ParticleType<ItemParticleOption>) type,
                    new ItemStack(BuiltInRegistries.ITEM.get(itemLoc)));
        }
        if (type == ParticleTypes.ENTITY_EFFECT) {
            // Since 1.21 entity_effect takes its tint as a real argument, read from the optional "color" object.
            JsonObject color = json.has("color") && json.get("color").isJsonObject()
                    ? json.getAsJsonObject("color")
                    : new JsonObject();
            return ColorParticleOption.create((ParticleType<ColorParticleOption>) type,
                    GsonHelper.getAsFloat(color, "r", 1.0f),
                    GsonHelper.getAsFloat(color, "g", 1.0f),
                    GsonHelper.getAsFloat(color, "b", 1.0f));
        }
        CreRaces.LOGGER.error("Invalid or unsupported complex particle type for id {}: {}", particleId, type);
        return null;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "spawn_particles"), json -> {
            String particleId = GsonHelper.getAsString(json, "particle");
            if (particleId.isEmpty()) {
                return null;
            }
            ResourceLocation particleLoc = ResourceLocation.parse(particleId);
            if (!BuiltInRegistries.PARTICLE_TYPE.containsKey(particleLoc)) {
                CreRaces.LOGGER.error("SpawnParticlesAction: Unknown particle ID '{}'.", particleId);
                return null;
            }
            ParticleOptions options = parseParticle(json, particleId, BuiltInRegistries.PARTICLE_TYPE.get(particleLoc));
            if (options == null) {
                return null;
            }

            ScalingValue count = ScalingValue.fromJson(json, "count", 10.0);
            String shape = GsonHelper.getAsString(json, "shape", "");
            ParticlePattern pattern = shape.isEmpty() ? null : parsePattern(shape, json, count);
            // "spread" sets all three axes at once; dx/dy/dz override it individually.
            ScalingValue spread = ScalingValue.fromJson(json, "spread", 0.0);
            return new SpawnParticlesAction(options, count,
                    ScalingValue.fromJson(json, "speed", 0.0),
                    json.has("dx") ? ScalingValue.fromJson(json, "dx", 0.0) : spread,
                    json.has("dy") ? ScalingValue.fromJson(json, "dy", 0.0) : spread,
                    json.has("dz") ? ScalingValue.fromJson(json, "dz", 0.0) : spread,
                    ScalingValue.fromJson(json, "spin", 0.0),
                    TargetFilter.fromJson(json, "targets", Set.of("enemies", "self")),
                    pattern);
        });
    }
}
