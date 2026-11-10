package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.AimOffset;
import mc.sayda.creraces.engine.CastSource;
import mc.sayda.creraces.engine.Position;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.SpiritMobilityHandler;
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

/**
 * Spawns "count" particles at "position" (a block above the target, or the caster when there is no
 * target). "spread" scatters them on every axis, "spread_x/y/z" per axis, and "speed" is their
 * motion. A "shape" (circle, disc, sphere, helix) lays them out around the position instead, with
 * "radius", "points", "axis", "height", "rotations" and "spin"; "line" runs them "length" blocks
 * along the caster's view, turned by "yaw" and "pitch". Block and falling-dust particles take
 * a "block", item particles an "item", and entity_effect a "color" ("#RRGGBB").
 */
@SuppressWarnings("null")
public class SpawnParticlesAction implements ActionRegistry.RaceAction {

    private interface ParticlePattern {
        void spawn(ServerLevel level, Player caster, Vec3 center, ParticleOptions particle, int totalCount,
                double speed, double spin, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
                @Nullable BlockPos interactPos);
    }

    private final ParticleOptions particle;
    private final ScalingValue count;
    private final ScalingValue speed;
    private final ScalingValue spreadX;
    private final ScalingValue spreadY;
    private final ScalingValue spreadZ;
    private final ScalingValue spin;
    private final Position position;
    @Nullable
    private final ParticlePattern pattern;

    private SpawnParticlesAction(ParticleOptions particle, ScalingValue count, ScalingValue speed, ScalingValue spreadX,
            ScalingValue spreadY, ScalingValue spreadZ, ScalingValue spin, Position position,
            @Nullable ParticlePattern pattern) {
        this.particle = particle;
        this.count = count;
        this.speed = speed;
        this.spreadX = spreadX;
        this.spreadY = spreadY;
        this.spreadZ = spreadZ;
        this.spin = spin;
        this.position = position;
        this.pattern = pattern;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (!(player.level() instanceof ServerLevel level)) {
            return true;
        }
        int particleCount = (int) count.evaluate(player, target, slot, interactPos);
        Vec3 center = position.resolve(player, target, slot, interactPos);
        if (particleCount <= 0 || center == null) {
            return true;
        }
        double motion = speed.evaluate(player, target, slot, interactPos);
        if (pattern != null) {
            pattern.spawn(level, player, center, particle, particleCount, motion,
                    spin.evaluate(player, target, slot, interactPos), target, slot, interactPos);
        } else {
            send(level, player, particle, center, particleCount, spreadX.evaluate(player, target, slot, interactPos),
                    spreadY.evaluate(player, target, slot, interactPos),
                    spreadZ.evaluate(player, target, slot, interactPos), motion);
        }
        return true;
    }

    private static void send(ServerLevel level, Player caster, ParticleOptions particle, Vec3 at, int count,
            double spreadX, double spreadY, double spreadZ, double speed) {
        sendToViewers(level, caster, particle, at.x, at.y, at.z, count, spreadX, spreadY, spreadZ, speed);
    }

    private static void sendToViewers(ServerLevel level, Player caster, ParticleOptions particle,
            double x, double y, double z, int count, double spreadX, double spreadY, double spreadZ, double speed) {
        if (SpiritMobilityHandler.isOnSpiritPlane(caster)) {
            SpiritMobilityHandler.sendParticlesIfSpirit(level, particle, x, y, z, count, spreadX, spreadY, spreadZ,
                    speed);
        } else {
            level.sendParticles(particle, x, y, z, count, spreadX, spreadY, spreadZ, speed);
        }
    }

    /** Splits the total particle count across the points as evenly as possible. */
    private static int countAtPoint(int point, int totalCount, int points) {
        return totalCount / points + (point < totalCount % points ? 1 : 0);
    }

    /** A point at {@code angle} on a ring of the given radius around the axis. */
    private static Vec3 ringOffset(String axis, double angle, double radius) {
        double a = Math.cos(angle) * radius;
        double b = Math.sin(angle) * radius;
        return switch (axis) {
            case "x" -> new Vec3(0, a, b);
            case "z" -> new Vec3(a, b, 0);
            default -> new Vec3(a, 0, b);
        };
    }

    /** Evenly spaced points on a ring, rotating over time with "spin". */
    private record CirclePattern(ScalingValue radius, ScalingValue points, String axis) implements ParticlePattern {
        @Override
        public void spawn(ServerLevel level, Player caster, Vec3 center, ParticleOptions particle, int totalCount,
                double speed, double spin, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
                @Nullable BlockPos interactPos) {
            double r = radius.evaluate(caster, target, slot, interactPos);
            int pointCount = (int) points.evaluate(caster, target, slot, interactPos);
            if (pointCount <= 0) {
                return;
            }
            double spinOffset = level.getGameTime() * spin;
            for (int i = 0; i < pointCount; i++) {
                int pointParticles = countAtPoint(i, totalCount, pointCount);
                if (pointParticles > 0) {
                    double angle = (2 * Math.PI * i / pointCount) + spinOffset;
                    send(level, caster, particle, center.add(ringOffset(axis, angle, r)), pointParticles, 0, 0, 0, speed);
                }
            }
        }
    }

    /** Random points spread evenly over a filled disc. */
    private record DiscPattern(ScalingValue radius, ScalingValue points, String axis) implements ParticlePattern {
        @Override
        public void spawn(ServerLevel level, Player caster, Vec3 center, ParticleOptions particle, int totalCount,
                double speed, double spin, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
                @Nullable BlockPos interactPos) {
            double maxRadius = radius.evaluate(caster, target, slot, interactPos);
            int pointCount = (int) points.evaluate(caster, target, slot, interactPos);
            if (pointCount <= 0) {
                return;
            }
            RandomSource random = level.getRandom();
            for (int i = 0; i < pointCount; i++) {
                int pointParticles = countAtPoint(i, totalCount, pointCount);
                if (pointParticles > 0) {
                    // The square root keeps the density even instead of bunching points at the centre.
                    double r = maxRadius * Math.sqrt(random.nextDouble());
                    double angle = 2 * Math.PI * random.nextDouble();
                    send(level, caster, particle, center.add(ringOffset(axis, angle, r)), pointParticles, 0, 0, 0, speed);
                }
            }
        }
    }

    private record SpherePattern(ScalingValue radius, ScalingValue points) implements ParticlePattern {
        @Override
        public void spawn(ServerLevel level, Player caster, Vec3 center, ParticleOptions particle, int totalCount,
                double speed, double spin, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
                @Nullable BlockPos interactPos) {
            double r = radius.evaluate(caster, target, slot, interactPos);
            int pointCount = (int) points.evaluate(caster, target, slot, interactPos);
            if (pointCount <= 0) {
                return;
            }
            double spinOffset = level.getGameTime() * spin;
            for (int i = 0; i < pointCount; i++) {
                int pointParticles = countAtPoint(i, totalCount, pointCount);
                if (pointParticles > 0) {
                    // Fibonacci sphere: spreads the points evenly using the golden angle.
                    double phi = Math.acos(1 - 2 * (i + 0.5) / pointCount);
                    double theta = (Math.PI * (1 + Math.sqrt(5)) * (i + 0.5)) + spinOffset;
                    Vec3 offset = new Vec3(r * Math.sin(phi) * Math.cos(theta), r * Math.sin(phi) * Math.sin(theta),
                            r * Math.cos(phi));
                    send(level, caster, particle, center.add(offset), pointParticles, 0, 0, 0, speed);
                }
            }
        }
    }

    /** A spiral rising from the position to "height" over "rotations" turns. */
    private record HelixPattern(ScalingValue radius, ScalingValue height, ScalingValue points,
            ScalingValue rotations) implements ParticlePattern {
        @Override
        public void spawn(ServerLevel level, Player caster, Vec3 center, ParticleOptions particle, int totalCount,
                double speed, double spin, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
                @Nullable BlockPos interactPos) {
            double r = radius.evaluate(caster, target, slot, interactPos);
            double h = height.evaluate(caster, target, slot, interactPos);
            int pointCount = (int) points.evaluate(caster, target, slot, interactPos);
            double turns = rotations.evaluate(caster, target, slot, interactPos);
            if (pointCount <= 0) {
                return;
            }
            double spinOffset = level.getGameTime() * spin;
            for (int i = 0; i < pointCount; i++) {
                int pointParticles = countAtPoint(i, totalCount, pointCount);
                if (pointParticles > 0) {
                    double t = (double) i / pointCount;
                    double angle = (2 * Math.PI * turns * t) + spinOffset;
                    send(level, caster, particle, center.add(Math.cos(angle) * r, t * h, Math.sin(angle) * r),
                            pointParticles, 0, 0, 0, speed);
                }
            }
        }
    }

    /** Evenly spaced points along the caster's view, turned by the aim offset, from the position onwards. */
    private record LinePattern(ScalingValue length, ScalingValue points, AimOffset aim) implements ParticlePattern {
        @Override
        public void spawn(ServerLevel level, Player caster, Vec3 center, ParticleOptions particle, int totalCount,
                double speed, double spin, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
                @Nullable BlockPos interactPos) {
            double l = length.evaluate(caster, target, slot, interactPos);
            int pointCount = (int) points.evaluate(caster, target, slot, interactPos);
            if (pointCount <= 0) {
                return;
            }
            Vec3 direction = aim.direction(CastSource.self(caster), caster, target, slot, interactPos);
            for (int i = 0; i < pointCount; i++) {
                int pointParticles = countAtPoint(i, totalCount, pointCount);
                if (pointParticles > 0) {
                    double t = pointCount == 1 ? 0 : (double) i / (pointCount - 1);
                    send(level, caster, particle, center.add(direction.scale(t * l)), pointParticles, 0, 0, 0, speed);
                }
            }
        }
    }

    /** Builds the shape named in "shape"; the point count defaults to the particle count. */
    private static ParticlePattern parsePattern(String shape, JsonObject json, ScalingValue defaultPoints) {
        ScalingValue radius = ScalingValue.fromJson(json, "radius", 1.0);
        ScalingValue points = json.has("points") ? ScalingValue.fromJson(json, "points", 1.0) : defaultPoints;
        String axis = GsonHelper.getAsString(json, "axis", "y");
        return switch (shape) {
            case "circle" -> new CirclePattern(radius, points, axis);
            case "disc" -> new DiscPattern(radius, points, axis);
            case "sphere" -> new SpherePattern(radius, points);
            case "helix" -> new HelixPattern(radius, ScalingValue.fromJson(json, "height", 2.0), points,
                    ScalingValue.fromJson(json, "rotations", 2.0));
            case "line" -> new LinePattern(ScalingValue.fromJson(json, "length", 5.0), points,
                    AimOffset.fromJson(json));
            default -> throw new IllegalArgumentException(
                    "unknown shape '" + shape + "' (circle, disc, sphere, helix, line)");
        };
    }

    /** Simple particles are used as-is; block, falling-dust and item particles take their "block" or "item". */
    @SuppressWarnings("unchecked")
    private static ParticleOptions parseParticle(JsonObject json, ParticleType<?> type) {
        if (type instanceof ParticleOptions simple) {
            return simple;
        }
        if (type == ParticleTypes.BLOCK || type == ParticleTypes.FALLING_DUST) {
            return new BlockParticleOption((ParticleType<BlockParticleOption>) type,
                    PlaceBlockAction.parseBlock(GsonHelper.getAsString(json, "block")).defaultBlockState());
        }
        if (type == ParticleTypes.ITEM) {
            return new ItemParticleOption((ParticleType<ItemParticleOption>) type,
                    new ItemStack(GiveItemAction.parseItem(GsonHelper.getAsString(json, "item"))));
        }
        if (type == ParticleTypes.ENTITY_EFFECT) {
            // Since 1.21 entity_effect carries its colour itself rather than in its motion.
            int rgb = GsonHelper.getAsColor(json, "color", 0xFFFFFFFF);
            return ColorParticleOption.create((ParticleType<ColorParticleOption>) type,
                    (rgb >> 16 & 0xFF) / 255f, (rgb >> 8 & 0xFF) / 255f, (rgb & 0xFF) / 255f);
        }
        throw new IllegalArgumentException("particle type " + BuiltInRegistries.PARTICLE_TYPE.getKey(type)
                + " is not supported");
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("spawn_particles"), json -> {
            String particleId = GsonHelper.getAsString(json, "particle");
            ResourceLocation particleLoc = ResourceLocation.tryParse(particleId);
            if (particleLoc == null || !BuiltInRegistries.PARTICLE_TYPE.containsKey(particleLoc)) {
                throw new IllegalArgumentException("unknown particle '" + particleId + "'");
            }
            ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(particleLoc);
            ScalingValue count = ScalingValue.fromJson(json, "count", 10.0);
            String shape = GsonHelper.getAsString(json, "shape", "");
            ScalingValue spread = ScalingValue.fromJson(json, "spread", 0.0);
            return new SpawnParticlesAction(parseParticle(json, type), count,
                    ScalingValue.fromJson(json, "speed", 0.0),
                    json.has("spread_x") ? ScalingValue.fromJson(json, "spread_x", 0.0) : spread,
                    json.has("spread_y") ? ScalingValue.fromJson(json, "spread_y", 0.0) : spread,
                    json.has("spread_z") ? ScalingValue.fromJson(json, "spread_z", 0.0) : spread,
                    ScalingValue.fromJson(json, "spin", 0.0),
                    Position.fromJson(json, "position", Position.of(Position.Source.AUTO, 1.0)),
                    shape.isEmpty() ? null : parsePattern(shape, json, count));
        });
    }
}
