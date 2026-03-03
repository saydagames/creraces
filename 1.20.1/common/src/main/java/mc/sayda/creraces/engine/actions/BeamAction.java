package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonArray;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class BeamAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "beam");
    private static final Map<ResourceLocation, BeamAction> CACHED_INSTANCES = new HashMap<>();

    private final ScalingValue length;
    private final ScalingValue radius;
    private final boolean targetPlayers;
    private final boolean targetEnemies;
    private final boolean excludeCaster;
    private final ScalingValue duration;
    private final ScalingValue drainRate;
    private final List<ActionRegistry.RaceAction> actions;
    private final float[] color;

    public BeamAction(ScalingValue length, ScalingValue radius, boolean targetPlayers, boolean targetEnemies,
            boolean excludeCaster,
            ScalingValue duration, ScalingValue drainRate, List<ActionRegistry.RaceAction> actions, float[] color) {
        this.length = length;
        this.radius = radius;
        this.targetPlayers = targetPlayers;
        this.targetEnemies = targetEnemies;
        this.excludeCaster = excludeCaster;
        this.duration = duration;
        this.drainRate = drainRate;
        this.actions = actions;
        this.color = color;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        double dr = drainRate.evaluate(player, target);
        double dur = duration.evaluate(player, target);
        if (dur > 0 || dr > 0) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                vars.setAbilityActive(true);
                vars.setActiveAbility(vars.getAbilityInSlot(slot)); // Link to the ability being cast
                vars.setActiveAbilityDuration((int) dur);
                vars.setActiveAbilityDrain(dr);
                CACHED_INSTANCES.put(vars.getActiveAbility(), this);

                // Sync start of beam
                float rVal = (float) radius.evaluate(player, null);
                mc.sayda.creraces.network.SyncBeamPacket pkt = new mc.sayda.creraces.network.SyncBeamPacket(
                        player.getUUID(), true, color[0], color[1], color[2], color[3], rVal);
                mc.sayda.creraces.network.BoundaryHandler.resyncForAllTrackers(player); // Fallback for general state
                // We should probably broadcast this specific packet
                broadcastBeamSync(player, pkt);
            });
            return true;
        }

        performBeamLogic(player);
        return true;
    }

    private void broadcastBeamSync(Player player, mc.sayda.creraces.network.SyncBeamPacket pkt) {
        var server = player.getServer();
        if (server != null) {
            for (net.minecraft.server.level.ServerPlayer p : server.getPlayerList().getPlayers()) {
                mc.sayda.creraces.network.BoundaryHandler.sendSyncBeam(p, pkt);
            }
        }
    }

    @SuppressWarnings("null")
    private void performBeamLogic(Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        double l = length.evaluate(player, null);
        int maxLen = mc.sayda.creraces.config.CreRacesConfig.BEAM_MAX_LENGTH.get();
        if (maxLen > 0)
            l = Math.min(l, maxLen);
        double rVal = radius.evaluate(player, null);
        Vec3 end = start.add(direction.scale(l));

        AABB searchArea = new AABB(start, end).inflate(rVal);
        List<Entity> possibleTargets = player.level().getEntities(player, searchArea, e -> {
            if (!(e instanceof LivingEntity))
                return false;
            if (excludeCaster && e == player)
                return false;
            if (!targetPlayers && e instanceof Player)
                return false;
            if (!targetEnemies && !(e instanceof Player))
                return false;
            return mc.sayda.creraces.team.RaceTeamManager.canHurt((LivingEntity) e, player);
        });

        for (Entity e : possibleTargets) {
            LivingEntity living = (LivingEntity) e;
            double r = radius.evaluate(player, living);
            if (isInsideBeam(start, end, living.getEyePosition(), r)) {
                for (ActionRegistry.RaceAction action : actions) {
                    action.execute(player, living, null, null);
                }
            }
        }
    }

    private void broadcastAnimationSync(Player player, mc.sayda.creraces.network.SyncAnimationPacket pkt) {
        var server = player.getServer();
        if (server != null) {
            for (net.minecraft.server.level.ServerPlayer p : server.getPlayerList().getPlayers()) {
                mc.sayda.creraces.network.BoundaryHandler.sendSyncAnimation(p, pkt);
            }
        }
    }

    public static void tickExecution(Player player, ResourceLocation abilityId) {
        BeamAction action = CACHED_INSTANCES.get(abilityId);
        if (action != null) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                if (vars.isAbilityActive() && vars.getActiveAbilityDuration() > 0) {
                    action.performBeamLogic(player);

                    // Periodically re-sync to ensure trackers see it
                    if (player.tickCount % 3 == 0) {
                        float rVal = (float) action.radius.evaluate(player, null);
                        var pkt = new mc.sayda.creraces.network.SyncBeamPacket(
                                player.getUUID(), true, action.color[0], action.color[1], action.color[2],
                                action.color[3],
                                rVal);
                        action.broadcastBeamSync(player, pkt);
                    }
                } else {
                    // Stop visuals and animations — then clear the cache so this fires only once
                    var stopPkt = new mc.sayda.creraces.network.SyncBeamPacket(
                            player.getUUID(), false, 0, 0, 0, 0, 0);
                    action.broadcastBeamSync(player, stopPkt);

                    var animPkt = new mc.sayda.creraces.network.SyncAnimationPacket(player.getUUID(), "beam_casting",
                            false);
                    action.broadcastAnimationSync(player, animPkt);

                    // Remove from cache so we stop sending STOP packets every tick
                    CACHED_INSTANCES.remove(abilityId);
                }
            });
        }
    }

    /**
     * Called when a player disconnects to clear any cached beam instance.
     * Prevents the static map from accumulating stale entries.
     */
    public static void clearForPlayer(Player player) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            ResourceLocation active = vars.getActiveAbility();
            if (active != null) {
                CACHED_INSTANCES.remove(active);
            }
        });
    }

    @SuppressWarnings("null")
    private boolean isInsideBeam(Vec3 start, Vec3 end, Vec3 point, double radius) {
        Vec3 line = end.subtract(start);
        double lenSq = line.lengthSqr();
        if (lenSq == 0)
            return point.distanceToSqr(start) <= radius * radius;
        double t = Math.max(0, Math.min(1, point.subtract(start).dot(line) / lenSq));
        Vec3 projection = start.add(line.scale(t));
        return point.distanceToSqr(projection) <= radius * radius;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            ScalingValue length = ScalingValue.fromJson(json, "length", 20.0);
            ScalingValue radius = ScalingValue.fromJson(json, "radius", 2.0);
            boolean targetPlayers = GsonHelper.getAsBoolean(json, "target_players", true);
            boolean targetEnemies = GsonHelper.getAsBoolean(json, "target_enemies", true);
            boolean excludeCaster = GsonHelper.getAsBoolean(json, "exclude_caster", true);
            ScalingValue duration = ScalingValue.fromJson(json, "duration", 0.0);
            ScalingValue drainRate = ScalingValue.fromJson(json, "drain_rate", 0.0);

            float[] color = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
            if (json.has("color")) {
                JsonArray arr = json.getAsJsonArray("color");
                for (int i = 0; i < Math.min(arr.size(), 4); i++) {
                    color[i] = arr.get(i).getAsFloat();
                }
            }

            List<ActionRegistry.RaceAction> actions = new ArrayList<>();
            if (json.has("actions")) {
                JsonArray array = json.getAsJsonArray("actions");
                for (int i = 0; i < array.size(); i++) {
                    actions.add(ActionRegistry.fromJson(array.get(i).getAsJsonObject()));
                }
            }
            return new BeamAction(length, radius, targetPlayers, targetEnemies, excludeCaster, duration,
                    drainRate, actions, color);
        });
    }
}
