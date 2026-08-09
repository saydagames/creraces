package mc.sayda.creraces.ability;

import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for ability execution logic.
 */
public class AbilityExecutionRegistry {
    private static final Map<ResourceLocation, AbilityExecutor> EXECUTORS = new ConcurrentHashMap<>();

    public static void register(ResourceLocation id, AbilityExecutor executor) {
        EXECUTORS.put(id, executor);
    }

    public static AbilityExecutor get(ResourceLocation id) {
        AbilityExecutor exec = EXECUTORS.get(id);
        if (exec == null) {
            // Fallback to JSON actions
            return (player, ability, slot) -> {
                if (ability.onActivate() != null && !ability.onActivate().isEmpty()) {
                    // Raytrace the block the player is looking at so actions using
                    // use_target_block (e.g. rat_tunnels place_block) get a meaningful pos.
                    net.minecraft.core.BlockPos lookTarget = null;
                    net.minecraft.world.phys.HitResult hit = player.pick(5.0, 0f, false);
                    if (hit instanceof net.minecraft.world.phys.BlockHitResult bhr &&
                            bhr.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                        // Use the face-adjacent block position so the rat hole goes
                        // ON TOP of the surface the player is looking at.
                        lookTarget = bhr.getBlockPos().relative(bhr.getDirection());
                    }
                    mc.sayda.creraces.CreRaces.LOGGER.debug(
                            "AbilityExecutionRegistry: Executing {} actions for ability {}",
                            ability.onActivate().size(), ability.id());
                    for (mc.sayda.creraces.engine.ActionRegistry.RaceAction action : ability.onActivate()) {
                        if (!action.execute(player, null, slot, lookTarget)) {
                            mc.sayda.creraces.CreRaces.LOGGER.debug("AbilityExecutionRegistry: Action failed, stopping execution for ability {}", ability.id());
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            };
        }
        return exec;

    }

    public static void clear() {
        EXECUTORS.clear();
    }
}
