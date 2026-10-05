package mc.sayda.creraces.global;

import javax.annotation.Nullable;

/** Event types a {@code data/creraces/globals/*.json} handler can fire on. */
public enum GlobalTrigger {
    ON_ENTITY_DEATH("on_entity_death"),
    ON_PLAYER_DEATH("on_player_death"),
    ON_ENTITY_HURT("on_entity_hurt"),
    ON_ENTITY_SPAWN("on_entity_spawn"),
    ON_WORLD_TICK("on_world_tick");

    private final String id;

    GlobalTrigger(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Nullable
    public static GlobalTrigger fromString(String s) {
        for (GlobalTrigger t : values()) {
            if (t.id.equalsIgnoreCase(s)) {
                return t;
            }
        }
        return null;
    }
}
