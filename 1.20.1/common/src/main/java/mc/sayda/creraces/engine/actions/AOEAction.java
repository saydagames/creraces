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

public class AOEAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "aoe");

    private final mc.sayda.creraces.engine.ScalingValue radius;
    private final mc.sayda.creraces.engine.TargetFilter targets;
    private final String requiredEffect;
    private final String notEffect;
    private final List<ActionRegistry.RaceAction> actions;

    public AOEAction(mc.sayda.creraces.engine.ScalingValue radius, mc.sayda.creraces.engine.TargetFilter targets,
            String requiredEffect,
            String notEffect, List<ActionRegistry.RaceAction> actions) {
        this.radius = radius;
        this.targets = targets;
        this.requiredEffect = requiredEffect;
        this.notEffect = notEffect;
        this.actions = actions;
    }

    @Override
    public boolean execute(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        if (player.level() == null)
            return true;

        double r = radius.evaluate(player, target);
        int maxRadius = mc.sayda.creraces.config.CreRacesConfig.AOE_MAX_RADIUS.get();
        if (maxRadius > 0)
            r = Math.min(r, maxRadius);
        AABB area = player.getBoundingBox().inflate(r);
        List<LivingEntity> hitTargets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                area,
                e -> {
                    if (!this.targets.isValid(e, player))
                        return false;

                    if (requiredEffect != null && !requiredEffect.isEmpty()) {
                        net.minecraft.resources.ResourceLocation effectId = new net.minecraft.resources.ResourceLocation(
                                requiredEffect);
                        net.minecraft.world.effect.MobEffect effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                                .get(effectId);
                        if (effect != null && !e.hasEffect(effect))
                            return false;
                    }

                    if (notEffect != null && !notEffect.isEmpty()) {
                        net.minecraft.resources.ResourceLocation effectId = new net.minecraft.resources.ResourceLocation(
                                notEffect);
                        net.minecraft.world.effect.MobEffect effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                                .get(effectId);
                        if (effect != null && e.hasEffect(effect))
                            return false;
                    }

                    return true;
                });

        if (hitTargets.isEmpty()) {
            return true;
        }

        for (LivingEntity e : hitTargets) {
            for (ActionRegistry.RaceAction action : actions) {
                if (!action.execute(player, e, slot, interactionPos)) {
                    return false;
                }
            }
        }
        return true;
    }

    @SuppressWarnings("null")
    public static void register() {
        ActionRegistry.register(ID, json -> {
            mc.sayda.creraces.engine.ScalingValue radius = mc.sayda.creraces.engine.ScalingValue.fromJson(json,
                    "radius", 5.0);
            mc.sayda.creraces.engine.TargetFilter targets = mc.sayda.creraces.engine.TargetFilter.fromJson(json,
                    "targets");
            String requiredEffect = GsonHelper.getAsString(json, "required_effect", "");
            String notEffect = GsonHelper.getAsString(json, "not_effect", "");
            List<ActionRegistry.RaceAction> actions = new ArrayList<>();
            if (json.has("actions")) {
                JsonArray array = json.getAsJsonArray("actions");
                for (int i = 0; i < array.size(); i++) {
                    actions.add(ActionRegistry.fromJson(array.get(i).getAsJsonObject()));
                }
            }
            return new AOEAction(radius, targets, requiredEffect, notEffect, actions);
        });
    }
}
