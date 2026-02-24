package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Multiplies damage taken if a condition is met.
 * Useful for immunities (multiplier = 0) or weaknesses.
 */
public class DamageMultiplierTrait implements TraitRegistry.RaceTrait {
    private final float multiplier;
    @Nullable
    private final Condition condition;

    public DamageMultiplierTrait(float multiplier, @Nullable Condition condition) {
        this.multiplier = multiplier;
        this.condition = condition;
    }

    @Override
    public float modifyDamageTaken(Player player, DamageSource source, float amount) {
        if (condition == null || condition.evaluate(player,
                source.getEntity() instanceof net.minecraft.world.entity.LivingEntity le ? le : null, null, null)) {
            return amount * multiplier;
        }
        return amount;
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "damage_multiplier"), json -> {
            float multiplier = GsonHelper.getAsFloat(json, "multiplier", 1.0f);
            Condition condition = null;
            if (json.has("condition")) {
                condition = Condition.fromJson(json.getAsJsonObject("condition"));
            }
            return new DamageMultiplierTrait(multiplier, condition);
        });
    }
}
