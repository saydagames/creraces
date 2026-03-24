package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.race.ResourceType;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import mc.sayda.creraces.engine.ScalingValue;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import mc.sayda.creraces.engine.ActionRegistry;

/**
 * Trait that allows flight while draining a resource.
 * Can be conditional (e.g., only while Foxfire is active).
 */
public class FlightTrait implements TraitRegistry.RaceTrait {
    private final ResourceLocation traitId;
    private final ResourceType resource;
    private final ScalingValue drainRate;
    private final boolean forceFly;
    private final boolean soggyWings;
    @Nullable
    private final Condition condition;
    @Nullable
    private final ResourceLocation exhaustionCooldownId;
    @Nullable
    private final ScalingValue exhaustionCooldownDuration;
    private final List<ActionRegistry.RaceAction> onFail;

    public FlightTrait(ResourceLocation traitId, ResourceType resource, ScalingValue drainRate, boolean forceFly,
            boolean soggyWings, @Nullable Condition condition,
            @Nullable ResourceLocation exhaustionCooldownId, @Nullable ScalingValue exhaustionCooldownDuration,
            List<ActionRegistry.RaceAction> onFail) {
        this.traitId = traitId;
        this.resource = resource;
        this.drainRate = drainRate;
        this.forceFly = forceFly;
        this.soggyWings = soggyWings;
        this.condition = condition;
        this.exhaustionCooldownId = exhaustionCooldownId;
        this.exhaustionCooldownDuration = exhaustionCooldownDuration;
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

            // Apply Soggy effect if wings can sag and environment matches
            if (soggyWings) {
                boolean inRain = mc.sayda.creraces.util.WorldUtils.isExposedToRain(player);
                boolean inWater = player.isInWater();
                boolean shouldBeSoggy = inWater || (inRain && mc.sayda.creraces.config.CreRacesConfig.SAG_WINGS.get());

                if (shouldBeSoggy) {


                    var soggyEffect = mc.sayda.creraces.registry.ModMobEffects.SOGGY.get();
                    if (soggyEffect != null) {
                        player.addEffect(
                                new net.minecraft.world.effect.MobEffectInstance(soggyEffect, 9600, 0, false, false));
                    }
                }
            }


            boolean isSoggy = soggyWings && player.hasEffect(java.util.Objects.requireNonNull(mc.sayda.creraces.registry.ModMobEffects.SOGGY.get()));
            boolean canFly = conditionMet && currentResource >= evaluatedDrain && !isSoggy;
            boolean wasMayfly = player.getAbilities().mayfly;
            boolean wasFlying = player.getAbilities().flying;

            if (canFly) {
                player.getAbilities().mayfly = true;

                // Force flying state if enabled
                if (forceFly) {
                    player.getAbilities().flying = true;
                }

                if (player.getAbilities().flying) {
                    // Drain resource
                    switch (resource) {
                        case MANA -> vars.setMana(Math.max(0, vars.getMana() - evaluatedDrain));
                        case ENERGY -> vars.setEnergy(Math.max(0, vars.getEnergy() - evaluatedDrain));
                        case GRIT -> vars.setGrit(Math.max(0, vars.getGrit() - evaluatedDrain));
                        case RAGE -> vars.setRage(Math.max(0, vars.getRage() - evaluatedDrain));
                        case SOUL -> vars.setSoul(Math.max(0, vars.getSoul() - evaluatedDrain));
                        case NONE -> {
                        } // No resource to drain
                    }
                }
            } else {
                // Fallback case: condition not met or resource exhausted
                if (!player.isCreative() && !player.isSpectator()) {
                    player.getAbilities().mayfly = false;
                    if (player.getAbilities().flying) {
                        player.getAbilities().flying = false;
                    }

                    // On Fail Logic
                    ResourceLocation failId = new ResourceLocation(java.util.Objects.requireNonNull(traitId.getNamespace()),
                            java.util.Objects.requireNonNull(traitId.getPath()) + "_failed");
                    boolean alreadyFailed = vars.getPersistentState(failId) > 0;
                    if (conditionMet && currentResource < evaluatedDrain && !alreadyFailed) {
                        vars.setPersistentState(failId, 1.0);

                        // Native exhaustion cooldown
                        if (exhaustionCooldownId != null && exhaustionCooldownDuration != null) {
                            vars.setCooldown(exhaustionCooldownId, (int) exhaustionCooldownDuration.evaluate(player));
                        }

                        for (ActionRegistry.RaceAction action : onFail) {
                            action.execute(player, null, null, null);
                        }
                    }

                    // Reset failure flag if the condition is no longer met (e.g. ability toggled
                    // off)
                    // This allows the failure to trigger again if the ability is re-enabled with
                    // low mana
                    if (!conditionMet) {
                        vars.setPersistentState(failId, 0.0);
                    }
                }
            }

            if (conditionMet && currentResource >= evaluatedDrain) {
                ResourceLocation failId = new ResourceLocation(java.util.Objects.requireNonNull(traitId.getNamespace()), java.util.Objects.requireNonNull(traitId.getPath()) + "_failed");
                vars.setPersistentState(failId, 0.0);
            }

            // Sync if changed
            if (player.getAbilities().mayfly != wasMayfly || player.getAbilities().flying != wasFlying) {
                player.onUpdateAbilities();
            }
        });
    }

    @SuppressWarnings("null")
    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "flight"), json -> {
            String resStr = GsonHelper.getAsString(json, "resource", "NONE");
            if (resStr.contains(":"))
                resStr = resStr.substring(resStr.indexOf(':') + 1);

            ResourceType resource = ResourceType.NONE;
            try {
                resource = ResourceType.valueOf(resStr.toUpperCase());
            } catch (Exception e) {
                CreRaces.LOGGER.warn("FlightTrait has unknown resource type: {}", resStr);
            }
            ScalingValue drainRate = ScalingValue.fromJson(json, "drain_rate", 0.0);
            boolean forceFly = GsonHelper.getAsBoolean(json, "force_fly", false);
            boolean soggyWings = GsonHelper.getAsBoolean(json, "creraces:soggy_wings",
                    GsonHelper.getAsBoolean(json, "soggy_wings", false));

            Condition condition = null;
            if (json.has("condition")) {
                condition = Condition.fromJson(json.getAsJsonObject("condition"));
            }

            ResourceLocation cooldownId = null;
            if (json.has("exhaustion_cooldown")) {
                cooldownId = ResourceLocation.tryParse(json.get("exhaustion_cooldown").getAsString());
            }
            ScalingValue cooldownDuration = json.has("exhaustion_duration")
                    ? ScalingValue.fromJson(json, "exhaustion_duration", 0.0)
                    : null;

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
                    : "flight_" + Math.abs(json.toString().hashCode());
            ResourceLocation traitId = new ResourceLocation(CreRaces.MODID, traitName);

            return new FlightTrait(traitId, resource, drainRate, forceFly, soggyWings, condition, cooldownId,
                    cooldownDuration, onFail);
        });
    }
}
