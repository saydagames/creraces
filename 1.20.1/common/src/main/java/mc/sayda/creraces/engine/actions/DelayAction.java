package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonArray;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DelayAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "delay");

    private final ScalingValue ticks;
    private final List<ActionRegistry.RaceAction> actions;

    public DelayAction(ScalingValue ticks, List<ActionRegistry.RaceAction> actions) {
        this.ticks = ticks;
        this.actions = actions;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        if (player.level().isClientSide())
            return true;

        int t = Math.max(1, (int) ticks.evaluate(player, target, slot));
        // Safety cap: prevents scheduler bloat (0 = disabled)
        int max = 1200;
        if (max > 0) {
            t = Math.min(t, max);
        }

        final net.minecraft.server.MinecraftServer server = player.getServer();
        if (server == null)
            return true;

        final UUID playerUUID = player.getUUID();
        final UUID targetUUID = target != null ? target.getUUID() : null;
        final net.minecraft.core.BlockPos immutablePos = interactionPos != null ? interactionPos.immutable() : null;

        mc.sayda.creraces.util.Scheduler.delay(t, () -> {
            ServerPlayer delayedPlayer = server.getPlayerList().getPlayer(Objects.requireNonNull(playerUUID));
            if (delayedPlayer == null)
                return; // Player left - skip silently

            LivingEntity delayedTarget = null;
            if (targetUUID != null
                    && delayedPlayer.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                // Look up the target entity by UUID in the player's current level
                net.minecraft.world.entity.Entity found = serverLevel.getEntity(targetUUID);
                if (found instanceof LivingEntity le)
                    delayedTarget = le;
            }

            final LivingEntity resolvedTarget = delayedTarget;
            for (ActionRegistry.RaceAction action : actions) {
                action.execute(delayedPlayer, resolvedTarget, slot, immutablePos);
            }
        });
        return true;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            ScalingValue ticks = ScalingValue.fromJson(json, "ticks", 20.0);
            List<ActionRegistry.RaceAction> actions = new ArrayList<>();
            if (json.has("actions")) {
                JsonArray array = json.getAsJsonArray("actions");
                for (int i = 0; i < array.size(); i++) {
                    actions.add(ActionRegistry.fromJson(array.get(i).getAsJsonObject()));
                }
            }
            return new DelayAction(ticks, actions);
        });
    }
}
