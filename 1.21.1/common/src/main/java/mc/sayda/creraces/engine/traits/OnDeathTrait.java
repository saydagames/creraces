package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.EventContext;
import mc.sayda.creraces.engine.TraitRegistry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

/** Runs when the player dies, with the killer (if any) as the target and the killing blow's damage_type. */
public class OnDeathTrait extends EventTrait {
    OnDeathTrait(JsonObject json) {
        super(json, Set.of("all"));
    }

    @Override
    public void onDeath(Player player, DamageSource source) {
        LivingEntity killer = source.getEntity() instanceof LivingEntity living ? living : null;
        fire(player, killer, null, EventContext.ofDamage(source, 0));
    }

    public static void register() {
        TraitRegistry.register(CreRaces.id("on_death"), OnDeathTrait::new);
    }
}
