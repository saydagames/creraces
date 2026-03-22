package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.entity.FeatherProjectile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Action that recalls projectiles owned by the player.
 */
public class RecallProjectilesAction implements ActionRegistry.RaceAction {
    private final mc.sayda.creraces.engine.ScalingValue radius;

    public RecallProjectilesAction(mc.sayda.creraces.engine.ScalingValue radius) {
        this.radius = radius;
    }

    @Override
    public boolean execute(Player player, @Nullable net.minecraft.world.entity.LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @Nullable net.minecraft.core.BlockPos interactionPos) {
        double r = radius.evaluate(player, target);
        AABB area = player.getBoundingBox().inflate(r);
        List<FeatherProjectile> feathers = player.level().getEntitiesOfClass(FeatherProjectile.class, area,
                f -> f.getOwner() == player);

        for (FeatherProjectile feather : feathers) {
            feather.setRecalling(true);
        }
        
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "recall_projectiles"), json -> {
            mc.sayda.creraces.engine.ScalingValue radius = mc.sayda.creraces.engine.ScalingValue.fromJson(json, "radius", 64.0);
            return new RecallProjectilesAction(radius);
        });
    }
}
