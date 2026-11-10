package mc.sayda.creraces.race;

import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.registry.ModEntities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * Undead players also count as creraces:undead_player and aquatic players as creraces:aquatic_player
 * when an entity type is checked against a tag. The data tags put those types in #minecraft:undead and
 * #minecraft:aquatic, so they pick up every vanilla tag built on top of those.
 */
public final class StandInEntityTypes {
    private StandInEntityTypes() {}

    /** True if the entity's own type or one of its stand-ins is in the tag. */
    public static boolean is(Entity entity, TagKey<EntityType<?>> tag) {
        return entity.getType().is(tag) || standInIs(entity, tag);
    }

    /** True if one of the entity's stand-ins is in the tag. */
    public static boolean standInIs(@Nullable Entity entity, TagKey<EntityType<?>> tag) {
        return anyMatch(entity, type -> type.is(tag));
    }

    /** True if one of the entity's stand-ins passes the test. Only players have stand-ins. */
    public static boolean anyMatch(@Nullable Entity entity, Predicate<EntityType<?>> test) {
        if (!(entity instanceof IPlayerVariables vars))
            return false;

        ResourceLocation raceId = vars.getRace();
        Race race = raceId != null ? RaceRegistry.get(raceId) : null;
        boolean undead = vars.isUndead() || (race != null && race.isUndead());
        boolean aquatic = vars.isAquatic() || (race != null && race.isAquatic());

        return (undead && test.test(ModEntities.UNDEAD_PLAYER.get()))
                || (aquatic && test.test(ModEntities.AQUATIC_PLAYER.get()));
    }

    /** Wraps a mob's target selector so it also skips entities whose stand-in is in the tag. */
    public static Predicate<LivingEntity> excluding(Predicate<LivingEntity> selector, TagKey<EntityType<?>> tag) {
        return selector.and(entity -> !standInIs(entity, tag));
    }
}
