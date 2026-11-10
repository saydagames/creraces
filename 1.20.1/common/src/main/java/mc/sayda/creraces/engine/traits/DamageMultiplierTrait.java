package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.EventContext;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.engine.condition.ConditionRegistry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Multiplies damage the player takes by "multiplier" while "condition" holds. The attacker is the
 * target, and damage_type / event:damage read the incoming hit.
 */
public class DamageMultiplierTrait implements TraitRegistry.RaceTrait {
    private final ScalingValue multiplier;
    @Nullable
    private final Condition condition;

    DamageMultiplierTrait(JsonObject json) {
        this.multiplier = ScalingValue.fromJson(json, "multiplier", 1.0);
        this.condition = ConditionRegistry.optional(json, "condition");
    }

    @Override
    public float modifyDamageTaken(Player player, DamageSource source, float amount) {
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
        return EventContext.ofDamage(source, amount).call(() -> {
            if (condition != null && !condition.evaluate(player, attacker, null, null)) {
                return amount;
            }
            return amount * (float) multiplier.evaluate(player, attacker, null, null);
        });
    }

    public static void register() {
        TraitRegistry.register(CreRaces.id("damage_multiplier"), DamageMultiplierTrait::new);
    }
}
