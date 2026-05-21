package mc.sayda.creraces.engine.traits;

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
    private final ResourceLocation traitId;
    @Nullable
    private final ResourceLocation effectId;
    private final ScalingValue amplifier;
    private final boolean visible;
    private final ResourceType resource;
    private final ScalingValue drainRate;
    private final ScalingValue duration;
    @Nullable
    private final ResourceLocation exhaustionCooldownId;
    @Nullable
    private final ScalingValue exhaustionCooldownDuration;
    @Nullable
    private final Condition condition;
    private final List<ActionRegistry.RaceAction> onFail;

    public ContinuousEffectTrait(ResourceLocation traitId, @Nullable ResourceLocation effectId,
            ScalingValue amplifier, boolean visible, ResourceType resource,
            ScalingValue drainRate, ScalingValue duration,
            @Nullable ResourceLocation exhaustionCooldownId, @Nullable ScalingValue exhaustionCooldownDuration,
            @Nullable Condition condition, List<ActionRegistry.RaceAction> onFail) {
        this.traitId = traitId;
        this.effectId = effectId;
        this.amplifier = amplifier;
        this.visible = visible;
        this.resource = resource;
        this.drainRate = drainRate;
        this.duration = duration;
        this.exhaustionCooldownId = exhaustionCooldownId;
        this.exhaustionCooldownDuration = exhaustionCooldownDuration;
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
                case SOUL -> vars.getSoul();
                case NONE -> 0.0;
                default -> 100.0;
            };

            double evaluatedDrain = drainRate.evaluate(player);
            boolean canApply = conditionMet && (resource == ResourceType.NONE || currentResource >= evaluatedDrain);

            if (canApply) {
                ResourceLocation effectLoc = effectId;
                if (effectLoc != null) {
                    MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectLoc);
                    if (effect != null) {
                        if (!player.level().isClientSide()) {
                            int amp = Math.max(0, Math.min(255, (int) amplifier.evaluate(player)));
                            player.addEffect(new MobEffectInstance(effect, (int) duration.evaluate(player),
                                    amp, false, visible, true));
                        }
                    }
                }

                // Drain resource
                if (resource != ResourceType.NONE) {
                    switch (resource) {
                        case MANA -> vars.setMana(Math.max(0, vars.getMana() - evaluatedDrain));
                        case ENERGY -> vars.setEnergy(Math.max(0, vars.getEnergy() - evaluatedDrain));
                        case GRIT -> vars.setGrit(Math.max(0, vars.getGrit() - evaluatedDrain));
                        case RAGE -> vars.setRage(Math.max(0, vars.getRage() - evaluatedDrain));
                        case SOUL -> vars.setSoul(Math.max(0, vars.getSoul() - evaluatedDrain));
                        case NONE -> {
                        }
                    }
                }
            } else {
                // Handle failure (Server only)
                if (!player.level().isClientSide()) {
                    String namespace = java.util.Objects.requireNonNull(traitId.getNamespace());
                    String path = java.util.Objects.requireNonNull(traitId.getPath()) + "_failed";
                    ResourceLocation failId = new ResourceLocation(namespace, path);
                    boolean alreadyFailed = vars.getPersistentState(failId) > 0;
                    if (conditionMet && resource != ResourceType.NONE && currentResource < evaluatedDrain
                            && !alreadyFailed) {
                        vars.setPersistentState(failId, 1.0);

                        // Native exhaustion cooldown
                        ResourceLocation cooldownId = exhaustionCooldownId;
                        ScalingValue cooldownDur = exhaustionCooldownDuration;
                        if (cooldownId != null && cooldownDur != null) {
                            vars.setCooldown(cooldownId, (int) cooldownDur.evaluate(player));
                        }

                        for (ActionRegistry.RaceAction action : onFail) {
                            action.execute(player, null, null, null);
                        }
                    }
                }
            }

            if (conditionMet && (resource == ResourceType.NONE || currentResource >= evaluatedDrain)) {
                String namespace = java.util.Objects.requireNonNull(traitId.getNamespace());
                String path = java.util.Objects.requireNonNull(traitId.getPath()) + "_failed";
                ResourceLocation failId = new ResourceLocation(namespace, path);
                vars.setPersistentState(failId, 0.0);
            }
        });
    }

    @SuppressWarnings("null")
    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "continuous_effect"), json -> {
            ResourceLocation effectId = null;
            if (json.has("effect")) {
                String effectIdStr = json.get("effect").getAsString();
                effectId = ResourceLocation.tryParse(effectIdStr);
            }

            ScalingValue amplifier = ScalingValue.fromJson(json, "amplifier", 0);
            boolean visible = GsonHelper.getAsBoolean(json, "visible", true);
            String resStr = GsonHelper.getAsString(json, "resource", "NONE");
            if (resStr.contains(":"))
                resStr = resStr.substring(resStr.indexOf(':') + 1);

            ResourceType resource = ResourceType.NONE;
            try {
                resource = ResourceType.valueOf(resStr.toUpperCase());
            } catch (Exception e) {
                CreRaces.LOGGER.warn("ContinuousEffectTrait has unknown resource type: {}", resStr);
            }
            ScalingValue drainRate = ScalingValue.fromJson(json, "drain_rate", 0.0);
            ScalingValue duration = ScalingValue.fromJson(json, "duration", 20);

            ResourceLocation cooldownId = null;
            if (json.has("exhaustion_cooldown")) {
                cooldownId = ResourceLocation.tryParse(json.get("exhaustion_cooldown").getAsString());
            }
            ScalingValue cooldownDuration = json.has("exhaustion_duration")
                    ? ScalingValue.fromJson(json, "exhaustion_duration", 0.0)
                    : null;

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

            String traitName = json.has("name") ? json.get("name").getAsString()
                    : "continuous_" + Math.abs(json.toString().hashCode());
            ResourceLocation traitId = new ResourceLocation(CreRaces.MODID, traitName);

            return new ContinuousEffectTrait(traitId, effectId, amplifier, visible, resource, drainRate, duration,
                    cooldownId, cooldownDuration, condition, onFail);
        });
    }
}
