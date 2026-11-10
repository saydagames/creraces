package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.AreaSearch;
import mc.sayda.creraces.engine.EntityMatcher;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.entity.RecallableProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Calls the caster's own projectiles within "radius" back to them. "projectiles" (entity ids and
 * #tags) limits which; without it every owned projectile comes. Projectiles with their own recall
 * (harpy feathers) use it; any other is sent flying back towards the caster.
 */
@SuppressWarnings("null")
public class RecallProjectilesAction implements ActionRegistry.RaceAction {
    private static final double RETURN_SPEED = 1.5;

    private final ScalingValue radius;
    private final EntityMatcher projectiles;

    public RecallProjectilesAction(ScalingValue radius, EntityMatcher projectiles) {
        this.radius = radius;
        this.projectiles = projectiles;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (player.level().isClientSide()) {
            return true;
        }
        AABB area = player.getBoundingBox().inflate(AreaSearch.clampRadius(radius.evaluate(player, target, slot, interactPos)));
        for (Projectile projectile : player.level().getEntitiesOfClass(Projectile.class, area,
                p -> p.getOwner() == player && (projectiles.isEmpty() || projectiles.matches(p)))) {
            if (projectile instanceof RecallableProjectile recallable) {
                recallable.recall();
            } else {
                Vec3 home = player.getEyePosition().subtract(projectile.position());
                if (home.lengthSqr() > 1.0E-4) {
                    projectile.setDeltaMovement(home.normalize().scale(RETURN_SPEED));
                    projectile.hurtMarked = true;
                }
            }
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("recall_projectiles"), json -> new RecallProjectilesAction(
                ScalingValue.fromJson(json, "radius", 64.0),
                EntityMatcher.fromJson(json, "projectiles")));
    }
}
