package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Every interval, runs its actions once on the player (no target) and once per other living entity
 * within the radius (as the target).
 */
public class DomainTrait extends PeriodicTrait {

    private final ScalingValue radius;
    private final List<ActionRegistry.RaceAction> actions;
    @Nullable
    private final Condition condition;

    public DomainTrait(ResourceLocation traitId, ScalingValue radius, List<ActionRegistry.RaceAction> actions,
            @Nullable Condition condition, ScalingValue interval) {
        super(traitId, interval);
        this.radius = radius;
        this.actions = actions;
        this.condition = condition;
    }

    @Override
    protected boolean shouldExecute(Player player, IPlayerVariables vars) {
        return condition == null || condition.evaluate(player, null, null, null);
    }

    @Override
    protected void execute(Player player, IPlayerVariables vars) {
        for (ActionRegistry.RaceAction action : actions) {
            action.execute(player, null, null, null);
        }

        double r = Math.max(0, radius.evaluate(player, null));
        int maxAoeRadius = CreRacesConfig.AOE_MAX_RADIUS.get();
        if (maxAoeRadius > 0)
            r = Math.min(r, maxAoeRadius);

        List<LivingEntity> others = player.level().getEntitiesOfClass(LivingEntity.class,
                Objects.requireNonNull(player.getBoundingBox().inflate(r)), e -> e != player);

        for (LivingEntity other : others) {
            for (ActionRegistry.RaceAction action : actions) {
                action.execute(player, other, null, null);
            }
        }
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "domain"), json -> {
            ScalingValue radius = ScalingValue.fromJson(json, "radius", 10.0);
            ScalingValue interval = ScalingValue.fromJson(json, "interval", 20.0);
            Condition condition = json.has("condition") ? Condition.fromJson(json.getAsJsonObject("condition")) : null;
            return new DomainTrait(TraitIds.fromJson(json, "domain_"), radius,
                    ActionRegistry.listFromJson(json, "actions"), condition, interval);
        });
    }
}
