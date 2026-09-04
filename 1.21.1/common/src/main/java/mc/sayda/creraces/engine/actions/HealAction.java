package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TargetFilter;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

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
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        float h = (float) amount.evaluate(player, target, slot);
        if (h <= 0) return true;

        double r = radius.evaluate(player, target, slot);
        int maxAoeRadius = mc.sayda.creraces.config.CreRacesConfig.AOE_MAX_RADIUS.get();
        if (maxAoeRadius > 0) r = Math.min(r, maxAoeRadius);

        if (r > 0) {
            final float amount = h;
            net.minecraft.world.phys.AABB area = player.getBoundingBox().inflate(r);
            player.level().getEntitiesOfClass(LivingEntity.class, area, e -> targets.isValid(e, player))
                    .forEach(e -> e.heal(amount));
        } else if (useTarget && target != null && targets.isValid(target, player)) {
            target.heal(h);
        } else if (!useTarget) {
            player.heal(h);
        }

        return true;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "heal"), json -> {
            ScalingValue amount = ScalingValue.fromJson(json, "amount", 1.0);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            ScalingValue radius = ScalingValue.fromJson(json, "radius", 0.0);
            TargetFilter targets = TargetFilter.fromJson(json, "targets", java.util.Set.of("allies", "self"));
            return new HealAction(amount, useTarget, radius, targets);
        });
    }
}
