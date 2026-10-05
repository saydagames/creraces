package mc.sayda.creraces.registry;

import mc.sayda.creraces.network.BoundaryHandler;
import net.minecraft.world.level.GameRules;

public class ModGameRules {
    @SuppressWarnings("null")
    public static final GameRules.Key<GameRules.BooleanValue> RULE_RACEGRIEFING = GameRules.register(
            "raceGriefing",
            GameRules.Category.PLAYER,
            GameRules.BooleanValue.create(true)
    );

    @SuppressWarnings("null")
    public static final GameRules.Key<GameRules.BooleanValue> SPIRIT_FLAME_VISIBLE = GameRules.register(
            "spiritFlameVisible",
            GameRules.Category.MISC,
            GameRules.BooleanValue.create(true, (server, rule) ->
                BoundaryHandler.broadcastSpiritFlameGamerule(rule.get()))
    );
    public static void init() {
        // Forces class loading
    }
}
