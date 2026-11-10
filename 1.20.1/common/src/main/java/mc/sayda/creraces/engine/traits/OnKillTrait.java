package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.EventContext;
import mc.sayda.creraces.engine.TraitRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

/** Runs when the player kills an entity ("targets" filters it, enemies by default), with the victim as the target. */
public class OnKillTrait extends EventTrait {
    OnKillTrait(JsonObject json) {
        super(json, Set.of("enemies"));
    }

    @Override
    public void onKill(Player player, LivingEntity target) {
        fire(player, target, null, EventContext.NONE);
    }

    public static void register() {
        TraitRegistry.register(CreRaces.id("on_kill"), OnKillTrait::new);
    }
}
