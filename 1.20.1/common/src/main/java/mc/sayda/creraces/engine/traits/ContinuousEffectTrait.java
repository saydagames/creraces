package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.race.ResourceType;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies a mob effect while a condition is met, draining a resource.
 */
public class ContinuousEffectTrait implements TraitRegistry.RaceTrait {
    private final ResourceLocation effectId;
    private final int amplifier;
    private final boolean visible;
    private final ResourceType resource;
    private final ScalingValue drainRate;
    private final int duration;
    @Nullable
    private final Condition condition;
    private final List<ActionRegistry.RaceAction> onFail;
    private boolean failed = false;

    public ContinuousEffectTrait(ResourceLocation effectId, int amplifier, boolean visible, ResourceType resource,
            ScalingValue drainRate, int duration, @Nullable Condition condition,
            List<ActionRegistry.RaceAction> onFail) {
        this.effectId = effectId;
        this.amplifier = amplifier;
        this.visible = visible;
        this.resource = resource;
        this.drainRate = drainRate;
        this.duration = duration;
        this.condition = condition;
        this.onFail = onFail;
    }

    @Override
    public void tick(Player player) {
        boolean conditionMet = condition == null || condition.evaluate(player, null, null, null);

        DataUtils.getVariables(player).ifPresent(vars -> {
            double currentResource = switch (resource) {
                case MANA -> vars.getMana();
                case ENERGY -> vars.getEnergy();
                case GRIT -> vars.getGrit();
                case RAGE -> vars.getRage();
                case SOULS -> vars.getSouls();
                case NONE -> 0.0;
                default -> 100.0;
            };

            double evaluatedDrain = drainRate.evaluate(player);
            boolean canApply = conditionMet && (resource == ResourceType.NONE || currentResource >= evaluatedDrain);

            if (canApply) {
                MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectId);
                if (effect != null) {
                    player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, visible, true));

                    // Drain resource
                    if (resource != ResourceType.NONE) {
                        switch (resource) {
                            case MANA -> vars.setMana(Math.max(0, vars.getMana() - evaluatedDrain));
                            case ENERGY -> vars.setEnergy(Math.max(0, vars.getEnergy() - evaluatedDrain));
                            case GRIT -> vars.setGrit(Math.max(0, vars.getGrit() - evaluatedDrain));
                            case RAGE -> vars.setRage(Math.max(0, vars.getRage() - evaluatedDrain));
                            case SOULS -> vars.setSouls(Math.max(0, vars.getSouls() - evaluatedDrain));
                            case NONE -> {
                            }
                        }
                    }
                }
            } else {
                // Handle failure
                if (conditionMet && resource != ResourceType.NONE && currentResource < evaluatedDrain && !failed) {
                    failed = true;
                    for (ActionRegistry.RaceAction action : onFail) {
                        action.execute(player, null, null, null);
                    }
                }
            }

            if (conditionMet && (resource == ResourceType.NONE || currentResource >= evaluatedDrain)) {
                failed = false;
            }
        });
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "continuous_effect"), json -> {
            ResourceLocation effectId = new ResourceLocation(GsonHelper.getAsString(json, "effect"));
            int amplifier = GsonHelper.getAsInt(json, "amplifier", 0);
            boolean visible = GsonHelper.getAsBoolean(json, "visible", true);
            String resStr = GsonHelper.getAsString(json, "resource", "NONE").toUpperCase();
            ResourceType resource = ResourceType.valueOf(resStr);
            ScalingValue drainRate = ScalingValue.fromJson(json, "drain_rate", 0.0);
            int duration = GsonHelper.getAsInt(json, "duration", 40); // 2 second default

            Condition condition = null;
            if (json.has("condition")) {
                condition = Condition.fromJson(json.getAsJsonObject("condition"));
            }

            List<ActionRegistry.RaceAction> onFail = new ArrayList<>();
            if (json.has("on_fail") && json.get("on_fail").isJsonArray()) {
                for (var actionElem : json.getAsJsonArray("on_fail")) {
                    if (actionElem.isJsonObject()) {
                        ActionRegistry.RaceAction action = ActionRegistry.fromJson(actionElem.getAsJsonObject());
                        if (action != null) {
                            onFail.add(action);
                        }
                    }
                }
            }

            return new ContinuousEffectTrait(effectId, amplifier, visible, resource, drainRate, duration, condition,
                    onFail);
        });
    }
}
