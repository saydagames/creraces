package mc.sayda.creraces.registry;

import net.minecraft.world.level.GameRules;

public class ModGameRules {
    @SuppressWarnings("null")
    public static final GameRules.Key<GameRules.BooleanValue> RULE_RACEGRIEFING = GameRules.register(
            "raceGriefing",
            GameRules.Category.PLAYER,
            GameRules.BooleanValue.create(true)
    );
    public static void init() {
        // Forces class loading
    }
}
