package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

public class PermanentEffectTrait implements TraitRegistry.RaceTrait {

    private final MobEffect effect;
    private final mc.sayda.creraces.engine.ScalingValue amplifier;
    private final boolean ambient;
    private final boolean showParticles;
    private final int duration;
    @javax.annotation.Nullable
    private final Condition condition;

    public PermanentEffectTrait(MobEffect effect, mc.sayda.creraces.engine.ScalingValue amplifier, boolean ambient,
            boolean showParticles, int duration,
            @javax.annotation.Nullable Condition condition) {
        this.effect = effect;
        this.amplifier = amplifier;
        this.ambient = ambient;
        this.showParticles = showParticles;
        this.duration = duration;
        this.condition = condition;
    }

    @Override
    public void tick(Player player) {
        if (effect != null && (condition == null || condition.evaluate(player, null, null, null))) {
            if (!player.level().isClientSide()) {
                // Apply for moderate duration to avoid flickering, but short enough to clear if
                // race changes
                player.addEffect(
                        new MobEffectInstance(effect, duration,
                                (int) amplifier.evaluate(player), ambient, showParticles));
            }
        }
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "permanent_effect"), json -> {
            String effectId = GsonHelper.getAsString(json, "effect");
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(new ResourceLocation(effectId));

            mc.sayda.creraces.engine.ScalingValue amplifier = mc.sayda.creraces.engine.ScalingValue.fromJson(json,
                    "amplifier", 0.0);
            boolean ambient = GsonHelper.getAsBoolean(json, "ambient", true);
            boolean showParticles = GsonHelper.getAsBoolean(json, "visible", true);

            int duration = GsonHelper.getAsInt(json, "duration", 60);

            mc.sayda.creraces.engine.condition.Condition condition = null;
            if (json.has("condition")) {
                condition = mc.sayda.creraces.engine.condition.Condition.fromJson(json.getAsJsonObject("condition"));
            }

            return new PermanentEffectTrait(effect, amplifier, ambient, showParticles, duration, condition);
        });
    }
}
