package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.EventContext;
import mc.sayda.creraces.engine.TraitRegistry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

/** Runs when the player hurts an entity ("targets" filters it, enemies by default), with event:damage. */
public class OnHitTrait extends EventTrait {
    OnHitTrait(JsonObject json) {
        super(json, Set.of("enemies"));
    }

    @Override
    public void onHit(Player player, LivingEntity target, DamageSource source, float amount) {
        fire(player, target, null, EventContext.ofDamage(source, amount));
    }

    public static void register() {
        TraitRegistry.register(CreRaces.id("on_hit"), OnHitTrait::new);
    }
}
