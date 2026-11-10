package mc.sayda.creraces.engine.traits;

import com.google.common.collect.MapMaker;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.Resources;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.engine.condition.ConditionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lets the player fly while "condition" holds, no "blocked_by_effects" effect is active and the
 * "resource" (if any) can pay "drain_rate" per tick of flight, or "hover_drain_rate" once they
 * have held still for a quarter second (turning doesn't count as moving). "force_fly" keeps them
 * airborne. When flight fails, "on_fail" runs once and the optional "exhaustion_cooldown" (a
 * cooldown id) is set to "exhaustion_duration" ticks, which the condition can then wait on.
 */
@SuppressWarnings("null")
public class FlightTrait implements TraitRegistry.RaceTrait {
    /**
     * Ticks a player must hold still before flight counts as hovering. The server only learns of
     * movement from the client's packets, and the odd tick passes without one.
     */
    private static final int HOVER_DELAY = 5;

    private record Motion(Vec3 position, int stillTicks) {
    }

    // Set once flight fails, so on_fail and the exhaustion cooldown fire once per failure, not every tick.
    private final ResourceLocation failStateId;
    @Nullable
    private final Resources.Resource resource;
    private final ScalingValue drainRate;
    @Nullable
    private final ScalingValue hoverDrainRate;
    /**
     * Where each player was on this trait's last tick and how long they have stayed there. Weak keys
     * compare by identity: entities otherwise compare by id, which a singleplayer client shares with
     * the server's copy of the same player, and both sides tick this same trait.
     */
    private final Map<Player, Motion> motions = new MapMaker().weakKeys().makeMap();
    private final boolean forceFly;
    private final List<Holder<MobEffect>> blockingEffects;
    @Nullable
    private final Condition condition;
    @Nullable
    private final ResourceLocation exhaustionCooldownId;
    private final ScalingValue exhaustionDuration;
    private final List<ActionRegistry.RaceAction> onFail;

    FlightTrait(JsonObject json) {
        this.failStateId = TraitIds.of(json).withSuffix("_failed");
        String resourceName = GsonHelper.getNullableString(json, "resource", null);
        this.resource = resourceName != null ? Resources.get(resourceName) : null;
        if (resourceName != null && resource == null) {
            throw new IllegalArgumentException("unknown resource '" + resourceName + "'; known: " + Resources.knownNames());
        }
        this.drainRate = ScalingValue.fromJson(json, "drain_rate", 0.0);
        this.hoverDrainRate = json.has("hover_drain_rate") ? ScalingValue.fromJson(json, "hover_drain_rate", 0.0) : null;
        this.forceFly = GsonHelper.getAsBoolean(json, "force_fly", false);
        this.blockingEffects = parseBlockingEffects(json);
        this.condition = ConditionRegistry.optional(json, "condition");
        String cooldown = GsonHelper.getNullableString(json, "exhaustion_cooldown", null);
        this.exhaustionCooldownId = cooldown != null
                ? ResourceLocation.tryParse(cooldown.contains(":") ? cooldown : CreRaces.MODID + ":" + cooldown) : null;
        this.exhaustionDuration = ScalingValue.fromJson(json, "exhaustion_duration", 0.0);
        this.onFail = ActionRegistry.listFromJson(json, "on_fail");
    }

    @Override
    public void tick(Player player) {
        boolean conditionMet = condition == null || condition.evaluate(player, null, null, null);

        Vec3 position = player.position();
        Motion last = motions.get(player);
        boolean stayedPut = last != null && last.position().distanceToSqr(position) < 1.0E-6;
        int stillTicks = stayedPut ? last.stillTicks() + 1 : 0;
        motions.put(player, new Motion(position, stillTicks));
        boolean hovering = stillTicks >= HOVER_DELAY;

        DataUtils.getVariables(player).ifPresent(vars -> {
            double drain = hovering && hoverDrainRate != null
                    ? hoverDrainRate.evaluate(player)
                    : drainRate.evaluate(player);
            boolean resourceOk = resource == null || resource.read(player) >= drain;
            boolean canFly = conditionMet && resourceOk && !hasBlockingEffect(player);
            boolean wasMayfly = player.getAbilities().mayfly;
            boolean wasFlying = player.getAbilities().flying;

            if (canFly) {
                player.getAbilities().mayfly = true;
                if (forceFly) {
                    player.getAbilities().flying = true;
                }
                if (player.getAbilities().flying && resource != null) {
                    resource.set(player, Math.max(0, resource.read(player) - drain));
                }
                vars.setPersistentState(failStateId, 0.0);
            } else if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;

                // With the condition met, flight can only have failed on the resource or a blocking effect.
                boolean alreadyFailed = vars.getPersistentState(failStateId) > 0;
                if (conditionMet && !alreadyFailed) {
                    vars.setPersistentState(failStateId, 1.0);
                    if (exhaustionCooldownId != null) {
                        vars.setCooldown(exhaustionCooldownId, (int) exhaustionDuration.evaluate(player));
                    }
                    ActionRegistry.runChain(onFail, player, null, null, null);
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

    private static List<Holder<MobEffect>> parseBlockingEffects(JsonObject json) {
        List<Holder<MobEffect>> effects = new ArrayList<>();
        if (!json.has("blocked_by_effects")) {
            return effects;
        }
        for (JsonElement element : json.getAsJsonArray("blocked_by_effects")) {
            ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
            Holder<MobEffect> effect = id != null ? BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null) : null;
            if (effect == null) {
                throw new IllegalArgumentException("unknown effect '" + element.getAsString() + "' in blocked_by_effects");
            }
            effects.add(effect);
        }
        return effects;
    }

    public static void register() {
        TraitRegistry.register(CreRaces.id("flight"), FlightTrait::new);
    }
}
