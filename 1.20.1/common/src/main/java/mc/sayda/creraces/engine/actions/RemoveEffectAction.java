package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;

public class RemoveEffectAction implements ActionRegistry.RaceAction {

    private final MobEffect effect;
    private final ScalingValue radius;
    private final mc.sayda.creraces.engine.TargetFilter targets;
    private final boolean useTarget;

    public RemoveEffectAction(MobEffect effect, ScalingValue radius, mc.sayda.creraces.engine.TargetFilter targets,
            boolean useTarget) {
        this.effect = effect;
        this.radius = radius;
        this.targets = targets;
        this.useTarget = useTarget;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        if (effect == null)
            return true;

        java.util.function.Consumer<net.minecraft.world.entity.LivingEntity> remove = e -> {
            e.removeEffect(effect);
        };

        double r = radius.evaluate(player, target, slot);
        int maxAoeRadius = mc.sayda.creraces.config.CreRacesConfig.AOE_MAX_RADIUS.get();
        if (maxAoeRadius > 0)
            r = Math.min(r, maxAoeRadius);

        if (r > 0) {
            // AoE Mode
            net.minecraft.world.phys.AABB area = player.getBoundingBox().inflate(r);
            player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, area, e -> {
                return targets.isValid(e, player);
            }).forEach(remove);
        } else {
            // Single Target Mode
            net.minecraft.world.entity.LivingEntity entity = (target != null) ? target : (useTarget ? null : player);
            if (entity != null && targets.isValid(entity, player)) {
                remove.accept(entity);
            }
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "remove_effect"), json -> {
            String effectId = GsonHelper.getAsString(json, "effect");
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(new ResourceLocation(effectId));
            ScalingValue radius = ScalingValue.fromJson(json, "radius", 0.0);
            mc.sayda.creraces.engine.TargetFilter targets = mc.sayda.creraces.engine.TargetFilter.fromJson(json,
                    "targets", java.util.Set.of("enemies", "self"));
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);

            return new RemoveEffectAction(effect, radius, targets, useTarget);
        });
    }
}
