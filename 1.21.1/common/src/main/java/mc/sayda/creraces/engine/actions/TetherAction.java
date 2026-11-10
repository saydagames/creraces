package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.EventContext;
import mc.sayda.creraces.engine.ScalingValue;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Links the caster to the target for a duration. While linked, "actions" run every interval; the
 * tether completes (on_complete) when the duration runs out and breaks (on_break) if the target
 * gets further away than "range". An aoe or raycast around it picks whom to tether. Tethers are
 * ticked through {@link #tickTethers}.
 */
public class TetherAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = CreRaces.id("tether");

    // Sent in the sync packet to tell the client to clear a tether visual.
    private static final ResourceLocation NO_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "air");

    /** Caster UUID -> (target UUID -> tether). */
    private static final Map<UUID, Map<UUID, TetherData>> ACTIVE_TETHERS = new ConcurrentHashMap<>();

    private final ScalingValue duration;
    private final ScalingValue range;
    private final ScalingValue interval;
    private final List<ActionRegistry.RaceAction> actions;
    private final List<ActionRegistry.RaceAction> onCompleteActions;
    private final List<ActionRegistry.RaceAction> onBreakActions;
    private final ResourceLocation texture;
    private final ScalingValue width;
    private final boolean effects;

    public TetherAction(ScalingValue duration, ScalingValue range, ScalingValue interval,
            List<ActionRegistry.RaceAction> actions, List<ActionRegistry.RaceAction> onCompleteActions,
            List<ActionRegistry.RaceAction> onBreakActions, ResourceLocation texture, ScalingValue width,
            boolean effects) {
        this.duration = duration;
        this.range = range;
        this.interval = interval;
        this.actions = actions;
        this.onCompleteActions = onCompleteActions;
        this.onBreakActions = onBreakActions;
        this.texture = texture;
        this.width = width;
        this.effects = effects;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (player.level().isClientSide() || target == null || target == player) {
            return false;
        }

        TetherData data = new TetherData(
                (int) duration.evaluate(player, target, slot, interactPos),
                (float) range.evaluate(player, target, slot, interactPos),
                (int) interval.evaluate(player, target, slot, interactPos),
                actions, onCompleteActions, onBreakActions, target, slot, EventContext.current());
        ACTIVE_TETHERS.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>()).put(target.getUUID(), data);
        sync(player, target.getUUID(), true, texture, (float) width.evaluate(player, target, slot, interactPos), effects);
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
                data.event.run(() -> ActionRegistry.runChain(data.onBreakActions, caster, linked, data.slot, null));
                finished.add(entry.getKey());
                continue;
            }

            data.ticksAlive++;
            if (data.interval > 0 && data.ticksAlive % data.interval == 0) {
                data.event.run(() -> ActionRegistry.runChain(data.actions, caster, linked, data.slot, null));
            }
            if (data.ticksAlive >= data.durationTicks) {
                data.event.run(() -> ActionRegistry.runChain(data.onCompleteActions, caster, linked, data.slot, null));
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
                ScalingValue.fromJson(json, "range", 10.0),
                ScalingValue.fromJson(json, "interval", 20.0),
                ActionRegistry.listFromJson(json, "actions"),
                ActionRegistry.listFromJson(json, "on_complete"),
                ActionRegistry.listFromJson(json, "on_break"),
                ResourceLocation.parse(GsonHelper.getAsString(json, "texture",
                        "minecraft:textures/entity/guardian_beam.png")),
                ScalingValue.fromJson(json, "width", 0.1),
                GsonHelper.getAsBoolean(json, "effects", true)));
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
        /** The event the tether started from, kept for the actions it runs later. */
        final EventContext event;
        int ticksAlive = 0;

        TetherData(int durationTicks, float maxDistance, int interval, List<ActionRegistry.RaceAction> actions,
                List<ActionRegistry.RaceAction> onCompleteActions, List<ActionRegistry.RaceAction> onBreakActions,
                LivingEntity targetEntity, @Nullable AbilitySlot slot, EventContext event) {
            this.durationTicks = durationTicks;
            this.maxDistance = maxDistance;
            this.interval = interval;
            this.actions = actions;
            this.onCompleteActions = onCompleteActions;
            this.onBreakActions = onBreakActions;
            this.targetEntity = targetEntity;
            this.slot = slot;
            this.event = event;
        }
    }
}
