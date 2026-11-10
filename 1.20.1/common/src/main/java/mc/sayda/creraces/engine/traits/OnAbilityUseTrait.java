package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.ability.Ability;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.EventContext;
import mc.sayda.creraces.engine.TraitRegistry;
import net.minecraft.world.entity.player.Player;

/** Runs when the player uses an ability; event_ability reads which one. */
public class OnAbilityUseTrait extends EventTrait {
    OnAbilityUseTrait(JsonObject json) {
        super(json, null);
    }

    @Override
    public void onAbilityUse(Player player, Ability ability) {
        fire(player, null, null, EventContext.ofAbility(ability.id()));
    }

    public static void register() {
        TraitRegistry.register(CreRaces.id("on_ability_use"), OnAbilityUseTrait::new);
    }
}
