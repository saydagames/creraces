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
                    ability.onActivate().forEach(action -> action.execute(player, null, slot));
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
