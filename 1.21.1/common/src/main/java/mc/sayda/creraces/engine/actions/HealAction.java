package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TargetFilter;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Heals everything valid in a radius, or else the target (use_target) or the caster. Healing the
 * caster directly skips the target filter.
 */
public class HealAction implements ActionRegistry.RaceAction {

    private final ScalingValue amount;
    private final boolean useTarget;
    private final ScalingValue radius;
    private final TargetFilter targets;

    public HealAction(ScalingValue amount, boolean useTarget, ScalingValue radius, TargetFilter targets) {
        this.amount = amount;
        this.useTarget = useTarget;
        this.radius = radius;
        this.targets = targets;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        float heal = (float) amount.evaluate(player, target, slot);
        if (heal <= 0) {
            return true;
        }

        double r = AreaTargets.clampRadius(radius.evaluate(player, target, slot));
        if (r > 0) {
            AreaTargets.around(player, r, e -> targets.isValid(e, player)).forEach(e -> e.heal(heal));
        } else if (useTarget && target != null && targets.isValid(target, player)) {
            target.heal(heal);
        } else if (!useTarget) {
            player.heal(heal);
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "heal"), json -> new HealAction(
                ScalingValue.fromJson(json, "amount", 1.0),
                GsonHelper.getAsBoolean(json, "use_target", false),
                ScalingValue.fromJson(json, "radius", 0.0),
                TargetFilter.fromJson(json, "targets", Set.of("allies", "self"))));
    }
}
