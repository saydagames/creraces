package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonArray;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TargetFilter;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.SyncAnimationPacket;
import mc.sayda.creraces.network.SyncBeamPacket;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fires a beam along the caster's view and runs its actions on every valid entity inside it.
 * With a duration or drain rate it becomes a sustained ability instead: the beam is cached per
 * player and re-fired every tick through {@link #tickExecution} until the ability ends.
 */
@SuppressWarnings("null")
public class BeamAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "beam");

    private static final Map<UUID, Map<ResourceLocation, BeamAction>> CACHED_INSTANCES = new ConcurrentHashMap<>();

    private final ScalingValue length;
    private final ScalingValue radius;
    private final TargetFilter targets;
    private final ScalingValue duration;
    private final ScalingValue drainRate;
    private final List<ActionRegistry.RaceAction> actions;
    private final float[] color;
    private final int syncInterval;

    public BeamAction(ScalingValue length, ScalingValue radius, TargetFilter targets, ScalingValue duration,
            ScalingValue drainRate, List<ActionRegistry.RaceAction> actions, float[] color, int syncInterval) {
        this.length = length;
        this.radius = radius;
        this.targets = targets;
        this.duration = duration;
        this.drainRate = drainRate;
        this.actions = actions;
        this.color = color;
        this.syncInterval = syncInterval;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (player.level().isClientSide()) {
            return false;
        }

        double drain = drainRate.evaluate(player, target, slot);
        double ticks = duration.evaluate(player, target, slot);
        if (ticks <= 0 && drain <= 0) {
            fire(player, slot);
            return true;
        }

        DataUtils.getVariables(player).ifPresent(vars -> {
            vars.setAbilityActive(true);
            ResourceLocation activeAbilityId = slot != null ? vars.getAbilityInSlot(slot) : null;
            vars.setActiveAbility(activeAbilityId);
            vars.setActiveAbilityDuration((int) ticks);
            vars.setActiveAbilityDrain(drain);
            if (activeAbilityId != null) {
                CACHED_INSTANCES.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>())
                        .put(activeAbilityId, this);
            }

            // Trackers need the active-ability state as well as the beam visual.
            BoundaryHandler.resyncForAllTrackers(player);
            sendBeamVisual(player, slot);
        });
        return true;
    }

    private void fire(Player player, @Nullable AbilitySlot slot) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(clampedLength(player, slot)));
        AABB searchArea = new AABB(start, end).inflate(AreaTargets.clampRadius(radius.evaluate(player, null, slot)));
        List<Entity> candidates = player.level().getEntities(player, searchArea,
                e -> e instanceof LivingEntity living && targets.isValid(living, player));

        for (Entity candidate : candidates) {
            LivingEntity living = (LivingEntity) candidate;
            double hitRadius = AreaTargets.clampRadius(radius.evaluate(player, living, slot));
            if (isInsideBeam(start, end, living.getEyePosition(), hitRadius)) {
                ActionRegistry.runAll(actions, player, living, slot, null);
            }
        }
    }

    private double clampedLength(Player player, @Nullable AbilitySlot slot) {
        double l = length.evaluate(player, null, slot);
        int maxLength = CreRacesConfig.BEAM_MAX_LENGTH.get();
        return maxLength > 0 ? Math.min(l, maxLength) : l;
    }

    private void sendBeamVisual(Player player, @Nullable AbilitySlot slot) {
        float visualRadius = (float) AreaTargets.clampRadius(radius.evaluate(player, null, slot));
        float visualLength = (float) clampedLength(player, slot);
        broadcast(player, new SyncBeamPacket(player.getUUID(), true, color[0], color[1], color[2], color[3],
                visualRadius, visualLength));
    }

    private static void broadcast(Player player, SyncBeamPacket packet) {
        BoundaryHandler.sendToTrackers(player, SyncBeamPacket.ID, packet::encode);
    }

    /** Re-fires a sustained beam each tick while its ability is active, and stops it once the ability ends. */
    public static void tickExecution(Player player, ResourceLocation abilityId) {
        if (player.level().isClientSide()) {
            return;
        }
        Map<ResourceLocation, BeamAction> playerBeams = CACHED_INSTANCES.get(player.getUUID());
        if (playerBeams == null) {
            return;
        }
        BeamAction beam = playerBeams.get(abilityId);
        if (beam == null) {
            return;
        }

        DataUtils.getVariables(player).ifPresent(vars -> {
            if (vars.isAbilityActive() && vars.getActiveAbilityDuration() > 0) {
                AbilitySlot slot = vars.getSlotForAbility(abilityId);
                beam.fire(player, slot);
                // Resend now and then so players who start tracking mid-beam still see it.
                if (player.tickCount % Math.max(1, beam.syncInterval) == 0) {
                    beam.sendBeamVisual(player, slot);
                }
            } else {
                broadcast(player, new SyncBeamPacket(player.getUUID(), false, 0, 0, 0, 0, 0, 0));
                BoundaryHandler.sendToTrackers(player, SyncAnimationPacket.ID,
                        new SyncAnimationPacket(player.getUUID(), "beam_casting", false)::encode);

                // Drop the cached beam so the stop packets go out only once.
                playerBeams.remove(abilityId);
                if (playerBeams.isEmpty()) {
                    CACHED_INSTANCES.remove(player.getUUID());
                }
            }
        });
    }

    /** Drops any cached beams for a player, so the static cache doesn't hold on to players who left. */
    public static void clearForPlayer(Player player) {
        if (player != null) {
            CACHED_INSTANCES.remove(player.getUUID());
        }
    }

    /** Whether {@code point} lies within {@code radius} of the segment from start to end. */
    private static boolean isInsideBeam(Vec3 start, Vec3 end, Vec3 point, double radius) {
        Vec3 line = end.subtract(start);
        double lenSq = line.lengthSqr();
        if (lenSq == 0) {
            return point.distanceToSqr(start) <= radius * radius;
        }
        double t = Math.max(0, Math.min(1, point.subtract(start).dot(line) / lenSq));
        Vec3 projection = start.add(line.scale(t));
        return point.distanceToSqr(projection) <= radius * radius;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            float[] color = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
            if (json.has("color")) {
                JsonArray rgba = json.getAsJsonArray("color");
                for (int i = 0; i < Math.min(rgba.size(), 4); i++) {
                    color[i] = rgba.get(i).getAsFloat();
                }
            }
            return new BeamAction(
                    ScalingValue.fromJson(json, "length", 16.0),
                    ScalingValue.fromJson(json, "radius", 0.25),
                    TargetFilter.fromJson(json, "targets", Set.of("enemies")),
                    ScalingValue.fromJson(json, "duration", 0.0),
                    ScalingValue.fromJson(json, "drain_rate", 0.0),
                    ActionRegistry.listFromJson(json, "actions"),
                    color,
                    GsonHelper.getAsInt(json, "sync_interval", 2));
        });
    }
}
