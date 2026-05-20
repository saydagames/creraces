package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonArray;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AOEAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "aoe");

    private final mc.sayda.creraces.engine.ScalingValue radius;
    private final mc.sayda.creraces.engine.TargetFilter targets;
    private final net.minecraft.world.effect.MobEffect cachedRequired;
    private final net.minecraft.world.effect.MobEffect cachedNot;
    private final boolean failIfEmpty;
    private final List<ActionRegistry.RaceAction> actions;

    public AOEAction(mc.sayda.creraces.engine.ScalingValue radius, mc.sayda.creraces.engine.TargetFilter targets,
            String requiredEffect,
            String notEffect, boolean failIfEmpty, List<ActionRegistry.RaceAction> actions) {
        this.radius = radius;
        this.targets = targets;
        this.failIfEmpty = failIfEmpty;
        this.actions = actions;

        this.cachedRequired = (requiredEffect != null && !requiredEffect.isEmpty())
                ? net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                        .get(new net.minecraft.resources.ResourceLocation(requiredEffect))
                : null;
        this.cachedNot = (notEffect != null && !notEffect.isEmpty())
                ? net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                        .get(new net.minecraft.resources.ResourceLocation(notEffect))
                : null;
    }

    @Override
    public boolean execute(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        if (player.level() == null)
            return true;

        double r = radius.evaluate(player, target, slot);
        int maxRadius = mc.sayda.creraces.config.CreRacesConfig.AOE_MAX_RADIUS.get();
        if (maxRadius > 0)
            r = Math.min(r, maxRadius);

        final AABB area = Objects.requireNonNull(player.getBoundingBox().inflate(r));

        List<LivingEntity> hitTargets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                area,
                e -> {
                    if (!this.targets.isValid(e, player))
                        return false;

                    if (cachedRequired != null && !e.hasEffect(cachedRequired))
                        return false;

                    if (cachedNot != null && e.hasEffect(cachedNot))
                        return false;

                    return true;
                });

        if (hitTargets.isEmpty()) {
            if (failIfEmpty) {
                mc.sayda.creraces.CreRaces.LOGGER.debug("AOEAction: No targets found, fail_if_empty is true - returning false.");
            }
            return !failIfEmpty;
        }

        mc.sayda.creraces.CreRaces.LOGGER.debug("AOEAction: Found {} valid targets.", hitTargets.size());

        for (LivingEntity e : hitTargets) {
            for (ActionRegistry.RaceAction action : actions) {
                if (!action.execute(player, e, slot, interact_pos)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            if (json == null) return (player, target, slot, interact_pos) -> true;
            mc.sayda.creraces.engine.ScalingValue radius = mc.sayda.creraces.engine.ScalingValue.fromJson(json,
                    "radius", 5.0);
            mc.sayda.creraces.engine.TargetFilter targets = mc.sayda.creraces.engine.TargetFilter.fromJson(json,
                    "targets", java.util.Set.of("enemies"));
            String requiredEffect = GsonHelper.getAsString(json, "required_effect", "");
            String notEffect = GsonHelper.getAsString(json, "not_effect", "");
            boolean failIfEmpty = GsonHelper.getAsBoolean(json, "fail_if_empty", false);
            List<ActionRegistry.RaceAction> actions = new ArrayList<>();
            if (json.has("actions")) {
                JsonArray array = json.getAsJsonArray("actions");
                for (int i = 0; i < array.size(); i++) {
                    actions.add(ActionRegistry.fromJson(array.get(i).getAsJsonObject()));
                }
            }
            return new AOEAction(radius, targets, requiredEffect, notEffect, failIfEmpty, actions);
        });
    }
}
