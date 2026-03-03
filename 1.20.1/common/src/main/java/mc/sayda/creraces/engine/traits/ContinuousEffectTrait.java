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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Applies a mob effect while a condition is met, draining a resource.
 */
public class ContinuousEffectTrait implements TraitRegistry.RaceTrait {
    private final ResourceLocation effectId;
    private final mc.sayda.creraces.engine.ScalingValue amplifier;
    private final boolean visible;
    private final ResourceType resource;
    private final ScalingValue drainRate;
    private final mc.sayda.creraces.engine.ScalingValue duration;
    @Nullable
    private final Condition condition;
    private final List<ActionRegistry.RaceAction> onFail;
    // Per-player failure state: traits are race-level singletons so instance fields
    // would be shared across all players of the same race.
    private final Map<UUID, Boolean> failedMap = new HashMap<>();

    public ContinuousEffectTrait(ResourceLocation effectId, mc.sayda.creraces.engine.ScalingValue amplifier,
            boolean visible, ResourceType resource,
            ScalingValue drainRate, mc.sayda.creraces.engine.ScalingValue duration, @Nullable Condition condition,
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
                    player.addEffect(new MobEffectInstance(effect, (int) duration.evaluate(player),
                            (int) amplifier.evaluate(player), false, visible, true));

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
                boolean alreadyFailed = failedMap.getOrDefault(player.getUUID(), false);
                if (conditionMet && resource != ResourceType.NONE && currentResource < evaluatedDrain
                        && !alreadyFailed) {
                    failedMap.put(player.getUUID(), true);
                    for (ActionRegistry.RaceAction action : onFail) {
                        action.execute(player, null, null, null);
                    }
                }
            }

            if (conditionMet && (resource == ResourceType.NONE || currentResource >= evaluatedDrain)) {
                failedMap.put(player.getUUID(), false);
            }
        });
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "continuous_effect"), json -> {
            ResourceLocation effectId = new ResourceLocation(GsonHelper.getAsString(json, "effect"));
            mc.sayda.creraces.engine.ScalingValue amplifier = mc.sayda.creraces.engine.ScalingValue.fromJson(json,
                    "amplifier", 0.0);
            boolean visible = GsonHelper.getAsBoolean(json, "visible", true);
            String resStr = GsonHelper.getAsString(json, "resource", "NONE").toUpperCase();
            ResourceType resource = ResourceType.valueOf(resStr);
            ScalingValue drainRate = ScalingValue.fromJson(json, "drain_rate", 0.0);
            mc.sayda.creraces.engine.ScalingValue duration = mc.sayda.creraces.engine.ScalingValue.fromJson(json,
                    "duration", 40.0); // 2 second default

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
