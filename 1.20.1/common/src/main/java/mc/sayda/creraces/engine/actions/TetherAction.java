package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonArray;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TetherAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "tether");

    // Static tracking map for active tethers
    // Structure: map.get(casterUUID).get(targetUUID) -> TetherData
    private static final Map<UUID, Map<UUID, TetherData>> ACTIVE_TETHERS = new ConcurrentHashMap<>();

    private final ScalingValue duration;
    private final ScalingValue maxDistance;
    private final ScalingValue interval;
    private final List<ActionRegistry.RaceAction> actions;
    private final List<ActionRegistry.RaceAction> onCompleteActions;
    private final List<ActionRegistry.RaceAction> onBreakActions;
    private final ResourceLocation texture;
    private final ScalingValue width;
    private final mc.sayda.creraces.engine.TargetFilter targets;

    public TetherAction(ScalingValue duration, ScalingValue maxDistance, ScalingValue interval,
            List<ActionRegistry.RaceAction> actions, List<ActionRegistry.RaceAction> onCompleteActions,
            List<ActionRegistry.RaceAction> onBreakActions, ResourceLocation texture, ScalingValue width,
            mc.sayda.creraces.engine.TargetFilter targets) {
        this.duration = duration;
        this.maxDistance = maxDistance;
        this.interval = interval;
        this.actions = actions;
        this.onCompleteActions = onCompleteActions;
        this.onBreakActions = onBreakActions;
        this.texture = texture;
        this.width = width;
        this.targets = targets;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable BlockPos interactionPos) {
        if (player.level().isClientSide() || target == null)
            return false;

        if (target == player || !targets.isValid(target, player))
            return false;

        UUID casterId = player.getUUID();
        UUID targetId = target.getUUID();

        int durData = (int) duration.evaluate(player);
        int intData = (int) interval.evaluate(player);
        float distData = (float) maxDistance.evaluate(player);
        float widthData = (float) width.evaluate(player);

        TetherData data = new TetherData(durData, distData, intData, actions, onCompleteActions, onBreakActions,
                target);

        ACTIVE_TETHERS.computeIfAbsent(casterId, k -> new ConcurrentHashMap<>()).put(targetId, data);

        // Sync to clients
        syncTetherToClients(player, casterId, targetId, true, texture, widthData);

        return true;
    }

    private static void syncTetherToClients(Player caster, UUID casterId, UUID targetId, boolean add,
            ResourceLocation tex, float width) {
        mc.sayda.creraces.network.BoundaryHandler.sendToTrackers(caster, mc.sayda.creraces.network.SyncTetherPacket.ID,
                buf -> {
                    new mc.sayda.creraces.network.SyncTetherPacket(casterId, targetId, add, tex.toString(), width)
                            .encode(buf);
                });
    }

    // Called periodically by ResourceTicker to evaluate active tethers
    public static void tickTethers(Player caster) {
        if (caster.level().isClientSide())
            return;

        UUID casterId = caster.getUUID();
        Map<UUID, TetherData> tethers = ACTIVE_TETHERS.get(casterId);
        if (tethers == null || tethers.isEmpty())
            return;

        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, TetherData> entry : tethers.entrySet()) {
            UUID targetId = entry.getKey();
            TetherData data = entry.getValue();

            // Validate target existence and health
            if (data.targetEntity == null || !data.targetEntity.isAlive() || data.targetEntity.isRemoved()
                    || data.targetEntity.level() != caster.level()) {
                toRemove.add(targetId);
                continue;
            }

            // Check distance logic
            if (data.maxDistance > 0) {
                double dist = caster.distanceToSqr(data.targetEntity);
                if (dist > (data.maxDistance * data.maxDistance)) {
                    // Tether broke natively from distance
                    for (ActionRegistry.RaceAction action : data.onBreakActions) {
                        action.execute(caster, data.targetEntity, null, null);
                    }
                    toRemove.add(targetId);
                    continue;
                }
            }

            // Tick Duration
            data.ticksAlive++;

            // Apply interval actions
            if (data.interval > 0 && data.ticksAlive % data.interval == 0) {
                for (ActionRegistry.RaceAction action : data.actions) {
                    action.execute(caster, data.targetEntity, null, null);
                }
            }

            // Complete successfully if it reached max duration
            if (data.ticksAlive >= data.durationTicks) {
                for (ActionRegistry.RaceAction action : data.onCompleteActions) {
                    action.execute(caster, data.targetEntity, null, null);
                }
                toRemove.add(targetId);
            }
        }

        // Cleanup
        for (UUID tId : toRemove) {
            tethers.remove(tId);
            syncTetherToClients(caster, casterId, tId, false, new ResourceLocation("minecraft", "air"), 0f);
        }
    }

    public static void clearTethersFor(Player caster) {
        if (caster == null)
            return;
        UUID casterId = caster.getUUID();
        Map<UUID, TetherData> tethers = ACTIVE_TETHERS.remove(casterId);
        if (tethers != null) {
            for (UUID tId : tethers.keySet()) {
                syncTetherToClients(caster, casterId, tId, false, new ResourceLocation("minecraft", "air"), 0f);
            }
        }
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            ScalingValue dur = ScalingValue.fromJson(json, "duration", 100.0);
            ScalingValue dist = ScalingValue.fromJson(json, "max_distance", 10.0);
            ScalingValue inter = ScalingValue.fromJson(json, "interval", 20.0);
            String texStr = GsonHelper.getAsString(json, "texture", "creraces:textures/misc/tether.png");
            ResourceLocation tex = new ResourceLocation(Objects.requireNonNull(texStr));
            ScalingValue w = ScalingValue.fromJson(json, "width", 0.1);

            List<ActionRegistry.RaceAction> actions = new ArrayList<>();
            if (json.has("actions")) {
                JsonArray arr = json.getAsJsonArray("actions");
                for (int i = 0; i < arr.size(); i++) {
                    actions.add(ActionRegistry.fromJson(arr.get(i).getAsJsonObject()));
                }
            }
            List<ActionRegistry.RaceAction> onComplete = new ArrayList<>();
            if (json.has("on_fill") || json.has("on_complete")) {
                JsonArray arr = json.has("on_complete") ? json.getAsJsonArray("on_complete")
                        : json.getAsJsonArray("on_fill");
                for (int i = 0; i < arr.size(); i++) {
                    onComplete.add(ActionRegistry.fromJson(arr.get(i).getAsJsonObject()));
                }
            }
            List<ActionRegistry.RaceAction> onBreak = new ArrayList<>();
            if (json.has("on_break")) {
                JsonArray arr = json.getAsJsonArray("on_break");
                for (int i = 0; i < arr.size(); i++) {
                    onBreak.add(ActionRegistry.fromJson(arr.get(i).getAsJsonObject()));
                }
            }

            mc.sayda.creraces.engine.TargetFilter targets = mc.sayda.creraces.engine.TargetFilter.fromJson(json,
                    "targets", java.util.Set.of("enemies"));

            return new TetherAction(dur, dist, inter, actions, onComplete, onBreak, tex, w, targets);
        });
    }

    private static class TetherData {
        public final int durationTicks;
        public final float maxDistance;
        public final int interval;
        public final List<ActionRegistry.RaceAction> actions;
        public final List<ActionRegistry.RaceAction> onCompleteActions;
        public final List<ActionRegistry.RaceAction> onBreakActions;
        @javax.annotation.Nonnull
        public final LivingEntity targetEntity;

        public int ticksAlive = 0;

        public TetherData(int durationTicks, float maxDistance, int interval,
                List<ActionRegistry.RaceAction> actions, List<ActionRegistry.RaceAction> onCompleteActions,
                List<ActionRegistry.RaceAction> onBreakActions, @javax.annotation.Nonnull LivingEntity targetEntity) {
            this.durationTicks = durationTicks;
            this.maxDistance = maxDistance;
            this.interval = interval;
            this.actions = actions;
            this.onCompleteActions = onCompleteActions;
            this.onBreakActions = onBreakActions;
            this.targetEntity = targetEntity;
        }
    }
}
