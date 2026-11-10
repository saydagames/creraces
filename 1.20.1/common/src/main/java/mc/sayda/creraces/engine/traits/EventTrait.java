package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.EventContext;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TargetFilter;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.engine.condition.ConditionRegistry;
import mc.sayda.creraces.network.BoundaryHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * What every event trait (on_hit, on_hurt, on_kill, ...) shares: "actions" run when the event
 * happens and "condition" holds, at most once per "cooldown" ticks. Traits whose event has another
 * entity also take "targets" to filter it.
 */
abstract class EventTrait implements TraitRegistry.RaceTrait {
    private final ResourceLocation id;
    private final List<ActionRegistry.RaceAction> actions;
    @Nullable
    private final Condition condition;
    @Nullable
    private final ScalingValue cooldown;
    @Nullable
    private final TargetFilter targets;
    private String traitId = "";

    /** {@code defaultTargets} is null for events without another entity. */
    EventTrait(JsonObject json, @Nullable Set<String> defaultTargets) {
        this.id = TraitIds.of(json);
        this.actions = ActionRegistry.listFromJson(json, "actions");
        this.condition = ConditionRegistry.optional(json, "condition");
        this.cooldown = json.has("cooldown") ? ScalingValue.fromJson(json, "cooldown", 0.0) : null;
        this.targets = defaultTargets != null ? TargetFilter.fromJson(json, "targets", defaultTargets) : null;
    }

    @Override
    public void setTraitId(String id) {
        this.traitId = id;
    }

    @Override
    public String getTraitId() {
        return traitId;
    }

    /** Runs the actions with {@code target} and {@code pos} as the context, if everything allows it. */
    protected boolean fire(Player player, @Nullable LivingEntity target, @Nullable BlockPos pos, EventContext event) {
        if (targets != null && target != null && !targets.isValid(target, player)) {
            return false;
        }
        return event.call(() -> {
            if (cooldown != null && DataUtils.getVariables(player).map(vars -> vars.getCooldown(id) > 0).orElse(false)) {
                return false;
            }
            if (condition != null && !condition.evaluate(player, target, null, pos)) {
                return false;
            }
            boolean completed = ActionRegistry.runChain(actions, player, target, null, pos);
            if (cooldown != null) {
                int ticks = (int) cooldown.evaluate(player, target, null, pos);
                DataUtils.getVariables(player).ifPresent(vars -> {
                    vars.setCooldown(id, ticks);
                    BoundaryHandler.resyncVariables(player, player);
                });
            }
            return completed;
        });
    }

    protected boolean hasActions() {
        return !actions.isEmpty();
    }
}
