package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TargetFilter;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/** Runs a list of actions once per valid entity around the caster, with each entity as the target. */
public class AOEAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "aoe");

    private final ScalingValue radius;
    private final TargetFilter targets;
    @Nullable
    private final Holder<MobEffect> requiredEffect;
    @Nullable
    private final Holder<MobEffect> excludedEffect;
    private final boolean failIfEmpty;
    private final List<ActionRegistry.RaceAction> actions;

    public AOEAction(ScalingValue radius, TargetFilter targets, String requiredEffect, String excludedEffect,
            boolean failIfEmpty, List<ActionRegistry.RaceAction> actions) {
        this.radius = radius;
        this.targets = targets;
        this.requiredEffect = resolveEffect(requiredEffect);
        this.excludedEffect = resolveEffect(excludedEffect);
        this.failIfEmpty = failIfEmpty;
        this.actions = actions;
    }

    @Nullable
    private static Holder<MobEffect> resolveEffect(String id) {
        return id.isEmpty() ? null : BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id)).orElse(null);
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        double r = AreaTargets.clampRadius(radius.evaluate(player, target, slot));
        List<LivingEntity> hitTargets = AreaTargets.around(player, r, e -> isValidTarget(e, player));

        if (hitTargets.isEmpty()) {
            if (failIfEmpty) {
                CreRaces.LOGGER.debug("AOEAction: No targets found, fail_if_empty is true - returning false.");
            }
            return !failIfEmpty;
        }

        CreRaces.LOGGER.debug("AOEAction: Found {} valid targets.", hitTargets.size());
        for (LivingEntity entity : hitTargets) {
            // A failing action only ends this entity's chain; the remaining entities still run theirs.
            ActionRegistry.runChain(actions, player, entity, slot, interactPos);
        }
        return true;
    }

    private boolean isValidTarget(LivingEntity entity, Player player) {
        return targets.isValid(entity, player)
                && (requiredEffect == null || entity.hasEffect(requiredEffect))
                && (excludedEffect == null || !entity.hasEffect(excludedEffect));
    }

    public static void register() {
        ActionRegistry.register(ID, json -> new AOEAction(
                ScalingValue.fromJson(json, "radius", 5.0),
                TargetFilter.fromJson(json, "targets", Set.of("enemies")),
                GsonHelper.getAsString(json, "required_effect", ""),
                GsonHelper.getAsString(json, "not_effect", ""),
                GsonHelper.getAsBoolean(json, "fail_if_empty", false),
                ActionRegistry.listFromJson(json, "actions")));
    }
}
