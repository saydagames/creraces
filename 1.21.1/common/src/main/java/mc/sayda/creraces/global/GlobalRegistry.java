package mc.sayda.creraces.global;

import mc.sayda.creraces.engine.TraitRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class GlobalRegistry {
    private static final Map<GlobalTrigger, List<GlobalHandler>> BY_TRIGGER = new EnumMap<>(GlobalTrigger.class);
    private static final List<TraitRegistry.RaceTrait> PLAYER_SCOPED_TRAITS = new ArrayList<>();
    private static int handlerCount = 0;

    private GlobalRegistry() {
    }

    public static void clear() {
        BY_TRIGGER.clear();
        PLAYER_SCOPED_TRAITS.clear();
        handlerCount = 0;
    }

    public static void register(GlobalHandler handler) {
        for (GlobalTrigger trigger : handler.triggers()) {
            BY_TRIGGER.computeIfAbsent(trigger, t -> new ArrayList<>()).add(handler);
        }
        PLAYER_SCOPED_TRAITS.addAll(handler.traits());
        handlerCount++;
    }

    public static List<GlobalHandler> byTrigger(GlobalTrigger trigger) {
        return BY_TRIGGER.getOrDefault(trigger, List.of());
    }

    /** Trait blocks declared directly in global handler files, applied to every player. */
    public static List<TraitRegistry.RaceTrait> playerScopedTraits() {
        return Collections.unmodifiableList(PLAYER_SCOPED_TRAITS);
    }

    public static boolean isEmpty() {
        return handlerCount == 0;
    }
}
