package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.Ability;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;

/** Runs actions whenever the player uses any ability. */
public class OnAbilityUseTrait implements TraitRegistry.RaceTrait {
    private final List<ActionRegistry.RaceAction> actions;
    @Nullable
    private final Condition condition;

    public OnAbilityUseTrait(List<ActionRegistry.RaceAction> actions, @Nullable Condition condition) {
        this.actions = actions;
        this.condition = condition;
    }

    @Override
    public void onAbilityUse(Player player, Ability ability) {
        if (condition == null || condition.evaluate(player, null, null, null)) {
            ActionRegistry.runChain(actions, player, null, null, null);
        }
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "on_ability_use"), json -> {
            Condition condition = json.has("condition") ? Condition.fromJson(json.getAsJsonObject("condition")) : null;
            return new OnAbilityUseTrait(ActionRegistry.listFromJson(json, "actions"), condition);
        });
    }
}
