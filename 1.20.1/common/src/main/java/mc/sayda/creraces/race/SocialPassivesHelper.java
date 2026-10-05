package mc.sayda.creraces.race;

import mc.sayda.creraces.capability.IPlayerVariables;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Matches entities against a race's social passive lists. Entries can be:
 * - an entity id: "minecraft:zombie"
 * - an entity type tag: "#minecraft:undead"
 * - an exclusion of either: "!minecraft:drowned", "!#minecraft:skeletons"
 */
public class SocialPassivesHelper {
    private static final Map<String, TagKey<EntityType<?>>> TAG_CACHE = new ConcurrentHashMap<>();

    @SuppressWarnings("null")
    private static TagKey<EntityType<?>> getOrCreateTag(@Nonnull String path) {
        return TAG_CACHE.computeIfAbsent(path, p -> TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(p)));
    }

    public static boolean isHatedBy(Player player, LivingEntity entity) {
        Race.Passives passives = getPassives(player);
        return passives != null && matchesAnyEntry(entity, passives.hatedByEntities());
    }

    public static boolean isRespectedBy(Player player, LivingEntity entity) {
        Race.Passives passives = getPassives(player);
        return passives != null && matchesAnyEntry(entity, passives.respectedByEntities());
    }

    public static boolean defendsRace(Player player, LivingEntity entity) {
        Race.Passives passives = getPassives(player);
        return passives != null && matchesAnyEntry(entity, passives.defendedByEntities());
    }

    public static List<String> getDefenders(@Nullable Player player) {
        if (player == null)
            return List.of();
        Race.Passives passives = getPassives(player);
        return passives != null ? passives.defendedByEntities() : List.of();
    }

    @Nullable
    private static Race.Passives getPassives(Player player) {
        if (!(player instanceof IPlayerVariables vars))
            return null;
        ResourceLocation raceId = vars.getRace();
        if (raceId == null)
            return null;
        Race race = RaceRegistry.get(raceId);
        return race != null ? race.passives() : null;
    }

    /** True if any inclusion entry matches and no exclusion entry does. */
    private static boolean matchesAnyEntry(LivingEntity entity, List<String> entries) {
        EntityType<?> entityType = entity.getType();
        ResourceLocation entityId = EntityType.getKey(entityType);

        boolean included = false;
        for (String entry : entries) {
            if (!entry.startsWith("!") && matchesEntry(entity, entityType, entityId, entry)) {
                included = true;
                break;
            }
        }
        if (!included) {
            return false;
        }

        for (String entry : entries) {
            if (entry.startsWith("!") && matchesEntry(entity, entityType, entityId, entry.substring(1))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if an entity matches a single spec: either a direct entity id, or a
     * #-prefixed entity type tag reference (with a fallback for undead-race players).
     */
    private static boolean matchesEntry(LivingEntity entity, EntityType<?> entityType, ResourceLocation entityId,
            String spec) {
        if (!spec.startsWith("#")) {
            return entityId.toString().equals(spec);
        }

        String tagPath = spec.substring(1);
        if (entityType.is(getOrCreateTag(Objects.requireNonNull(tagPath)))) {
            return true;
        }

        // Undead-race players are never in the vanilla entity type tag, but LivingEntityMixin
        // makes them report MobType.UNDEAD.
        return tagPath.equals("minecraft:undead") && entity.getMobType() == MobType.UNDEAD;
    }
}
