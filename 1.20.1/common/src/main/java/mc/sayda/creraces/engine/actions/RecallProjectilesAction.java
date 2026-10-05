package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.entity.FeatherProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/** Calls back the caster's feather projectiles within a radius. */
public class RecallProjectilesAction implements ActionRegistry.RaceAction {
    private final ScalingValue radius;

    public RecallProjectilesAction(ScalingValue radius) {
        this.radius = radius;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (player.level().isClientSide()) {
            return true;
        }
        AABB area = player.getBoundingBox().inflate(AreaTargets.clampRadius(radius.evaluate(player, target, slot)));
        for (FeatherProjectile feather : player.level().getEntitiesOfClass(FeatherProjectile.class, area,
                f -> f.getOwner() == player)) {
            feather.setRecalling(true);
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "recall_projectiles"),
                json -> new RecallProjectilesAction(ScalingValue.fromJson(json, "radius", 64.0)));
    }
}
