package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Multiplies damage taken while the condition holds (the attacker, if any, is the condition's
 * target). A multiplier of 0 makes an immunity; above 1, a weakness.
 */
public class DamageMultiplierTrait implements TraitRegistry.RaceTrait {
    private final ScalingValue multiplier;
    @Nullable
    private final Condition condition;

    public DamageMultiplierTrait(ScalingValue multiplier, @Nullable Condition condition) {
        this.multiplier = multiplier;
        this.condition = condition;
    }

    @Override
    public float modifyDamageTaken(Player player, DamageSource source, float amount) {
        LivingEntity attacker = source.getEntity() instanceof LivingEntity le ? le : null;
        if (condition == null || condition.evaluate(player, attacker, null, null)) {
            return amount * (float) multiplier.evaluate(player);
        }
        return amount;
    }

    public static void register() {
        TraitRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "damage_multiplier"), json -> {
            ScalingValue multiplier = ScalingValue.fromJson(json, "multiplier", 1.0);
            Condition condition = json.has("condition") ? Condition.fromJson(json.getAsJsonObject("condition")) : null;
            return new DamageMultiplierTrait(multiplier, condition);
        });
    }
}
