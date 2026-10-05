package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TargetFilter;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.SyncTetherPacket;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Links the caster to a target for a duration. While linked, "actions" run every interval; the
 * tether completes (on_complete) when the duration runs out and breaks (on_break) if the target
 * gets further away than max_distance. Tethers are ticked through {@link #tickTethers}.
 */
public class TetherAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "tether");

    // Sent in the sync packet to tell the client to clear a tether visual.
    private static final ResourceLocation NO_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "air");

    /** Caster UUID -> (target UUID -> tether). */
    private static final Map<UUID, Map<UUID, TetherData>> ACTIVE_TETHERS = new ConcurrentHashMap<>();

    private final ScalingValue duration;
    private final ScalingValue maxDistance;
    private final ScalingValue interval;
    private final List<ActionRegistry.RaceAction> actions;
    private final List<ActionRegistry.RaceAction> onCompleteActions;
    private final List<ActionRegistry.RaceAction> onBreakActions;
    private final ResourceLocation texture;
    private final ScalingValue width;
    private final boolean effects;
    private final TargetFilter targets;

    public TetherAction(ScalingValue duration, ScalingValue maxDistance, ScalingValue interval,
            List<ActionRegistry.RaceAction> actions, List<ActionRegistry.RaceAction> onCompleteActions,
            List<ActionRegistry.RaceAction> onBreakActions, ResourceLocation texture, ScalingValue width,
            boolean effects, TargetFilter targets) {
        this.duration = duration;
        this.maxDistance = maxDistance;
        this.interval = interval;
        this.actions = actions;
        this.onCompleteActions = onCompleteActions;
        this.onBreakActions = onBreakActions;
        this.texture = texture;
        this.width = width;
        this.effects = effects;
        this.targets = targets;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (player.level().isClientSide() || target == null || target == player || !targets.isValid(target, player)) {
            return false;
        }

        TetherData data = new TetherData(
                (int) duration.evaluate(player, target, slot),
                (float) maxDistance.evaluate(player, target, slot),
                (int) interval.evaluate(player, target, slot),
                actions, onCompleteActions, onBreakActions, target, slot);
        ACTIVE_TETHERS.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>()).put(target.getUUID(), data);
        sync(player, target.getUUID(), true, texture, (float) width.evaluate(player, target, slot), effects);
        return true;
    }

    private static void sync(Player caster, UUID targetId, boolean add, ResourceLocation texture, float width,
            boolean effects) {
        BoundaryHandler.sendToTrackers(caster, SyncTetherPacket.ID, buf -> new SyncTetherPacket(caster.getUUID(),
                targetId, add, texture.toString(), width, effects).encode(buf));
    }

    /** Advances every tether of this caster by one tick; called once per player tick. */
    public static void tickTethers(Player caster) {
        if (caster.level().isClientSide()) {
            return;
        }
        Map<UUID, TetherData> tethers = ACTIVE_TETHERS.get(caster.getUUID());
        if (tethers == null || tethers.isEmpty()) {
            return;
        }

        List<UUID> finished = new ArrayList<>();
        for (Map.Entry<UUID, TetherData> entry : tethers.entrySet()) {
            TetherData data = entry.getValue();
            LivingEntity linked = data.targetEntity;
            if (!linked.isAlive() || linked.isRemoved() || linked.level() != caster.level()) {
                finished.add(entry.getKey());
                continue;
            }

            if (data.maxDistance > 0 && caster.distanceToSqr(linked) > data.maxDistance * data.maxDistance) {
                ActionRegistry.runAll(data.onBreakActions, caster, linked, data.slot, null);
                finished.add(entry.getKey());
                continue;
            }

            data.ticksAlive++;
            if (data.interval > 0 && data.ticksAlive % data.interval == 0) {
                ActionRegistry.runAll(data.actions, caster, linked, data.slot, null);
            }
            if (data.ticksAlive >= data.durationTicks) {
                ActionRegistry.runAll(data.onCompleteActions, caster, linked, data.slot, null);
                finished.add(entry.getKey());
            }
        }

        for (UUID targetId : finished) {
            tethers.remove(targetId);
            sync(caster, targetId, false, NO_TEXTURE, 0f, false);
        }
    }

    /** Drops all of a caster's tethers and clears their visuals. */
    public static void clearTethersFor(Player caster) {
        if (caster == null) {
            return;
        }
        Map<UUID, TetherData> tethers = ACTIVE_TETHERS.remove(caster.getUUID());
        if (tethers != null) {
            for (UUID targetId : tethers.keySet()) {
                sync(caster, targetId, false, NO_TEXTURE, 0f, false);
            }
        }
    }

    public static void register() {
        ActionRegistry.register(ID, json -> new TetherAction(
                ScalingValue.fromJson(json, "duration", 100.0),
                ScalingValue.fromJson(json, "max_distance", 10.0),
                ScalingValue.fromJson(json, "interval", 20.0),
                ActionRegistry.listFromJson(json, "actions"),
                // "on_fill" is the older name for on_complete.
                ActionRegistry.listFromJson(json, json.has("on_complete") ? "on_complete" : "on_fill"),
                ActionRegistry.listFromJson(json, "on_break"),
                ResourceLocation.parse(GsonHelper.getAsString(json, "texture",
                        "minecraft:textures/entity/guardian_beam.png")),
                ScalingValue.fromJson(json, "width", 0.1),
                GsonHelper.getAsBoolean(json, "effects", true),
                TargetFilter.fromJson(json, "targets", Set.of("enemies"))));
    }

    private static class TetherData {
        final int durationTicks;
        final float maxDistance;
        final int interval;
        final List<ActionRegistry.RaceAction> actions;
        final List<ActionRegistry.RaceAction> onCompleteActions;
        final List<ActionRegistry.RaceAction> onBreakActions;
        final LivingEntity targetEntity;
        @Nullable
        final AbilitySlot slot;
        int ticksAlive = 0;

        TetherData(int durationTicks, float maxDistance, int interval, List<ActionRegistry.RaceAction> actions,
                List<ActionRegistry.RaceAction> onCompleteActions, List<ActionRegistry.RaceAction> onBreakActions,
                LivingEntity targetEntity, @Nullable AbilitySlot slot) {
            this.durationTicks = durationTicks;
            this.maxDistance = maxDistance;
            this.interval = interval;
            this.actions = actions;
            this.onCompleteActions = onCompleteActions;
            this.onBreakActions = onBreakActions;
            this.targetEntity = targetEntity;
            this.slot = slot;
        }
    }
}
