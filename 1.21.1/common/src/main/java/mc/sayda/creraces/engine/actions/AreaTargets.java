package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.function.Predicate;

/** Radius capping and entity gathering shared by the actions that can hit an area. */
final class AreaTargets {
    private AreaTargets() {
    }

    /** Caps a radius at the configured AoE limit; a limit of 0 or less (the default, -1) disables the cap. */
    static double clampRadius(double radius) {
        int maxRadius = CreRacesConfig.AOE_MAX_RADIUS.get();
        return maxRadius > 0 ? Math.min(radius, maxRadius) : radius;
    }

    /** Living entities (the caster included) within {@code radius} of the caster's bounding box. */
    @SuppressWarnings("null")
    static List<LivingEntity> around(Player player, double radius, Predicate<LivingEntity> filter) {
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius), filter);
    }
}
