package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.EventContext;
import mc.sayda.creraces.engine.TraitRegistry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

/** Runs when the player is hurt, with the attacker (if any) as the target, event:damage and damage_type. */
public class OnHurtTrait extends EventTrait {
    OnHurtTrait(JsonObject json) {
        super(json, Set.of("all"));
    }

    @Override
    public void onHurt(Player player, DamageSource source, float amount) {
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
        fire(player, attacker, null, EventContext.ofDamage(source, amount));
    }

    public static void register() {
        TraitRegistry.register(CreRaces.id("on_hurt"), OnHurtTrait::new);
    }
}
