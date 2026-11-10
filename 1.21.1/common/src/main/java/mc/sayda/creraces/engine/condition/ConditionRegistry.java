package mc.sayda.creraces.engine.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.Comparison;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for condition types. The built-in ones live under creraces:, grouped by what they look
 * at; addons register their own under their own namespace with {@link #register}.
 */
public class ConditionRegistry {

    public interface ConditionFactory {
        Condition create(JsonObject data);
    }

    private static final Map<ResourceLocation, ConditionFactory> REGISTRY = new HashMap<>();

    // An absent condition passes; a broken one fails, so a typo never unlocks an effect.
    static final Condition ALWAYS_TRUE = (player, target, slot, interactPos) -> true;
    static final Condition ALWAYS_FALSE = (player, target, slot, interactPos) -> false;

    public static void register(ResourceLocation id, ConditionFactory factory) {
        REGISTRY.put(id, factory);
    }

    public static Condition fromJson(JsonObject json) {
        if (json == null || json.size() == 0 || !json.has("type")) {
            return ALWAYS_TRUE;
        }
        String typeStr = json.get("type").getAsString();
        ResourceLocation typeId = ResourceLocation.tryParse(typeStr);
        if (typeId == null) {
            CreRaces.LOGGER.error("Malformed condition type '{}' - it will always be false.", typeStr);
            return ALWAYS_FALSE;
        }
        ConditionFactory factory = REGISTRY.get(typeId);
        if (factory == null) {
            CreRaces.LOGGER.error("Unknown condition type '{}' - it will always be false.", typeStr);
            return ALWAYS_FALSE;
        }
        try {
            Condition condition = factory.create(json);
            return condition != null ? condition : ALWAYS_FALSE;
        } catch (Exception e) {
            CreRaces.LOGGER.error("Failed to parse condition '{}': {} - it will always be false. JSON: {}",
                    typeStr, e.getMessage(), json);
            return ALWAYS_FALSE;
        }
    }

    /** The optional "condition" member, or null when there is none. */
    public static Condition optional(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonObject() ? fromJson(json.getAsJsonObject(key)) : null;
    }

    static Condition[] parseList(JsonObject json) {
        JsonArray array = json.getAsJsonArray("conditions");
        Condition[] conditions = new Condition[array.size()];
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            conditions[i] = element.isJsonObject() ? fromJson(element.getAsJsonObject()) : ALWAYS_FALSE;
        }
        return conditions;
    }

    /** Reads "operator"; an unknown one has already been reported, and the condition never passes. */
    static Comparison operator(JsonObject json) {
        Comparison comparison = Comparison.fromJson(json);
        if (comparison == null) {
            throw new IllegalArgumentException("unknown operator");
        }
        return comparison;
    }

    static ResourceLocation id(String path) {
        return CreRaces.id(path);
    }

    public static void init() {
        register(id("and"), json -> {
            Condition[] conditions = parseList(json);
            return (player, target, slot, pos) -> {
                for (Condition condition : conditions) {
                    if (!condition.evaluate(player, target, slot, pos)) {
                        return false;
                    }
                }
                return true;
            };
        });
        register(id("or"), json -> {
            Condition[] conditions = parseList(json);
            return (player, target, slot, pos) -> {
                for (Condition condition : conditions) {
                    if (condition.evaluate(player, target, slot, pos)) {
                        return true;
                    }
                }
                return false;
            };
        });
        register(id("not"), json -> {
            Condition condition = fromJson(json.getAsJsonObject("condition"));
            return (player, target, slot, pos) -> !condition.evaluate(player, target, slot, pos);
        });

        EntityConditions.register();
        PlayerConditions.register();
        WorldConditions.register();
        BlockConditions.register();
        EventConditions.register();
    }
}
