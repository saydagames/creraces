package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.EventContext;
import mc.sayda.creraces.engine.TraitRegistry;
import net.minecraft.world.entity.player.Player;

/** Runs when the player picks this race. */
public class OnSelectTrait extends EventTrait {
    OnSelectTrait(JsonObject json) {
        super(json, null);
    }

    @Override
    public void onSelect(Player player) {
        fire(player, null, null, EventContext.NONE);
    }

    public static void register() {
        TraitRegistry.register(CreRaces.id("on_select"), OnSelectTrait::new);
    }
}
