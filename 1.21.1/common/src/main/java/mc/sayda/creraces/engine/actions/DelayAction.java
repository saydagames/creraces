package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Runs its actions after a number of ticks. The caster and target are looked up again by UUID when
 * the delay ends, so a caster who logged off is skipped and a target that is gone becomes null.
 */
public class DelayAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "delay");

    private final ScalingValue ticks;
    private final List<ActionRegistry.RaceAction> actions;

    public DelayAction(ScalingValue ticks, List<ActionRegistry.RaceAction> actions) {
        this.ticks = ticks;
        this.actions = actions;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (player.level().isClientSide()) {
            return true;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return true;
        }

        int delay = Math.max(1, (int) ticks.evaluate(player, target, slot));
        // Caps how far ahead actions can be queued, so the scheduler can't pile up (0 disables the cap).
        int maxDelay = CreRacesConfig.DELAY_ACTION_MAX_TICKS.get();
        if (maxDelay > 0) {
            delay = Math.min(delay, maxDelay);
        }

        UUID playerId = player.getUUID();
        UUID targetId = target != null ? target.getUUID() : null;
        BlockPos pos = interactPos != null ? interactPos.immutable() : null;

        Scheduler.delay(delay, () -> {
            ServerPlayer delayedPlayer = server.getPlayerList().getPlayer(playerId);
            if (delayedPlayer == null) {
                return;
            }
            LivingEntity delayedTarget = null;
            if (targetId != null && delayedPlayer.level() instanceof ServerLevel level
                    && level.getEntity(targetId) instanceof LivingEntity living) {
                delayedTarget = living;
            }
            ActionRegistry.runChain(actions, delayedPlayer, delayedTarget, slot, pos);
        });
        return true;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> new DelayAction(
                ScalingValue.fromJson(json, "ticks", 20.0),
                ActionRegistry.listFromJson(json, "actions")));
    }
}
