package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.race.ResourceType;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Lets the player fly while a resource lasts, optionally only while a condition holds. Any effect in
 * blocked_by_effects grounds the player and counts as a failure.
 */
public class FlightTrait implements TraitRegistry.RaceTrait {
    // Set once flight fails, so on_fail and the exhaustion cooldown fire once per failure, not every tick.
    private final ResourceLocation failStateId;
    private final ResourceType resource;
    private final ScalingValue drainRate;
    private final boolean forceFly;
    private final List<Holder<MobEffect>> blockingEffects;
    @Nullable
    private final Condition condition;
    @Nullable
    private final ResourceLocation exhaustionCooldownId;
    @Nullable
    private final ScalingValue exhaustionCooldownDuration;
    private final List<ActionRegistry.RaceAction> onFail;

    public FlightTrait(ResourceLocation traitId, ResourceType resource, ScalingValue drainRate, boolean forceFly,
            List<Holder<MobEffect>> blockingEffects, @Nullable Condition condition,
            @Nullable ResourceLocation exhaustionCooldownId, @Nullable ScalingValue exhaustionCooldownDuration,
            List<ActionRegistry.RaceAction> onFail) {
        this.failStateId = traitId.withSuffix("_failed");
        this.resource = resource;
        this.drainRate = drainRate;
        this.forceFly = forceFly;
        this.blockingEffects = blockingEffects;
        this.condition = condition;
        this.exhaustionCooldownId = exhaustionCooldownId;
        this.exhaustionCooldownDuration = exhaustionCooldownDuration;
        this.onFail = onFail;
    }

    @Override
    public void tick(Player player) {
        boolean conditionMet = condition == null || condition.evaluate(player, null, null, null);

        DataUtils.getVariables(player).ifPresent(vars -> {
            double evaluatedDrain = drainRate.evaluate(player);
            boolean resourceOk = resource == ResourceType.NONE || currentResource(vars) >= evaluatedDrain;
            boolean canFly = conditionMet && resourceOk && !hasBlockingEffect(player);
            boolean wasMayfly = player.getAbilities().mayfly;
            boolean wasFlying = player.getAbilities().flying;

            if (canFly) {
                player.getAbilities().mayfly = true;
                if (forceFly) {
                    player.getAbilities().flying = true;
                }
                if (player.getAbilities().flying) {
                    drain(vars, evaluatedDrain);
                }
                vars.setPersistentState(failStateId, 0.0);
            } else if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;

                // With the condition met, flight can only have failed on the resource or a blocking effect.
                boolean alreadyFailed = vars.getPersistentState(failStateId) > 0;
                if (conditionMet && !alreadyFailed) {
                    vars.setPersistentState(failStateId, 1.0);

                    if (exhaustionCooldownId != null && exhaustionCooldownDuration != null) {
                        vars.setCooldown(exhaustionCooldownId, (int) exhaustionCooldownDuration.evaluate(player));
                    }

                    for (ActionRegistry.RaceAction action : onFail) {
                        action.execute(player, null, null, null);
                    }
                }

                // Reset the failure flag once the condition clears, so a later re-enable can fail again.
                if (!conditionMet) {
                    vars.setPersistentState(failStateId, 0.0);
                }
            }

            if (player.getAbilities().mayfly != wasMayfly || player.getAbilities().flying != wasFlying) {
                player.onUpdateAbilities();
            }
        });
    }

    private boolean hasBlockingEffect(Player player) {
        for (Holder<MobEffect> effect : blockingEffects) {
            if (player.hasEffect(effect)) {
                return true;
            }
        }
        return false;
    }

    private double currentResource(IPlayerVariables vars) {
        return switch (resource) {
            case MANA -> vars.getMana();
            case ENERGY -> vars.getEnergy();
            case GRIT -> vars.getGrit();
            case RAGE -> vars.getRage();
            case SOUL -> vars.getSoul();
            case NONE -> 0.0;
        };
    }

    private void drain(IPlayerVariables vars, double amount) {
        switch (resource) {
            case MANA -> vars.setMana(Math.max(0, vars.getMana() - amount));
            case ENERGY -> vars.setEnergy(Math.max(0, vars.getEnergy() - amount));
            case GRIT -> vars.setGrit(Math.max(0, vars.getGrit() - amount));
            case RAGE -> vars.setRage(Math.max(0, vars.getRage() - amount));
            case SOUL -> vars.setSoul(Math.max(0, vars.getSoul() - amount));
            case NONE -> {
            }
        }
    }

    private static List<Holder<MobEffect>> parseBlockingEffects(JsonObject json) {
        List<Holder<MobEffect>> effects = new ArrayList<>();
        if (!json.has("blocked_by_effects")) {
            return effects;
        }
        for (JsonElement element : json.getAsJsonArray("blocked_by_effects")) {
            ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
            Holder<MobEffect> effect = id != null ? BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null) : null;
            if (effect == null) {
                CreRaces.LOGGER.error("FlightTrait: unknown effect '{}' in blocked_by_effects, ignoring it.",
                        element.getAsString());
            } else {
                effects.add(effect);
            }
        }
        return effects;
    }

    @SuppressWarnings("null")
    public static void register() {
        TraitRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "flight"), json -> {
            String resStr = GsonHelper.getAsString(json, "resource", "NONE");
            if (resStr.contains(":"))
                resStr = resStr.substring(resStr.indexOf(':') + 1);

            ResourceType resource = ResourceType.NONE;
            try {
                resource = ResourceType.valueOf(resStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                CreRaces.LOGGER.warn("FlightTrait has unknown resource type: {}", resStr);
            }
            ScalingValue drainRate = ScalingValue.fromJson(json, "drain_rate", 0.0);
            boolean forceFly = GsonHelper.getAsBoolean(json, "force_fly", false);
            Condition condition = json.has("condition") ? Condition.fromJson(json.getAsJsonObject("condition")) : null;

            ResourceLocation cooldownId = json.has("exhaustion_cooldown")
                    ? ResourceLocation.tryParse(json.get("exhaustion_cooldown").getAsString()) : null;
            ScalingValue cooldownDuration = json.has("exhaustion_duration")
                    ? ScalingValue.fromJson(json, "exhaustion_duration", 0.0)
                    : null;

            return new FlightTrait(TraitIds.fromJson(json, "flight_"), resource, drainRate, forceFly,
                    parseBlockingEffects(json), condition, cooldownId, cooldownDuration,
                    ActionRegistry.listFromJson(json, "on_fail"));
        });
    }
}
