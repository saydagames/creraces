package mc.sayda.creraces.engine;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Which entity an action or condition works on, read from its "entity" field ("self" or "target").
 * Leaving the field out gives the type's default; AUTO means the target when there is one and the
 * caster otherwise, which is what makes actions inside aoe, on_hit and friends hit the right entity.
 */
public enum EntitySelector {
    AUTO, SELF, TARGET;

    /**
     * The selected entity, or null when "target" was asked for and there is none. "Self" is the
     * caster, or the clone repeating the cast (see CastSource).
     */
    @Nullable
    public LivingEntity resolve(Player caster, @Nullable LivingEntity target) {
        LivingEntity self = CastSource.self(caster);
        return switch (this) {
            case AUTO -> target != null ? target : self;
            case SELF -> self;
            case TARGET -> target;
        };
    }

    public static EntitySelector fromJson(JsonObject json, EntitySelector fallback) {
        if (!json.has("entity") || !json.get("entity").isJsonPrimitive()) {
            return fallback;
        }
        String name = json.get("entity").getAsString();
        return switch (name) {
            case "self" -> SELF;
            case "target" -> TARGET;
            default -> {
                CreRaces.LOGGER.error("Unknown entity '{}' (expected \"self\" or \"target\") in {}", name, json);
                yield fallback;
            }
        };
    }
}
