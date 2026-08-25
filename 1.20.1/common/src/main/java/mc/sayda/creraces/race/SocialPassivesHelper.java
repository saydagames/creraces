package mc.sayda.creraces.race;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.IPlayerVariables;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper utilities for race social interaction passives
 * Supports:
 * - Direct entity IDs: "minecraft:zombie"
 * - Entity tags: "#minecraft:undead" (starts with #)
 * - Exclusions: "!minecraft:drowned" or "!#minecraft:skeletons" (starts with !)
 */
public class SocialPassivesHelper {
    private static final Map<String, TagKey<EntityType<?>>> TAG_CACHE = new ConcurrentHashMap<>();

    @SuppressWarnings("null")
    private static TagKey<EntityType<?>> getOrCreateTag(@javax.annotation.Nonnull String path) {
        return TAG_CACHE.computeIfAbsent(path, p -> TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(p)));
    }

    public static boolean isHatedBy(Player player, LivingEntity entity) {
        Race race = getPlayerRace(player);
        if (race == null || race.passives() == null)
            return false;

        return matchesAnyEntry(entity, race.passives().hatedByEntities());
    }

    public static boolean isRespectedBy(Player player, LivingEntity entity) {
        Race race = getPlayerRace(player);
        if (race == null || race.passives() == null)
            return false;

        return matchesAnyEntry(entity, race.passives().respectedByEntities());
    }

    public static boolean defendsRace(Player player, LivingEntity entity) {
        Race race = getPlayerRace(player);
        if (race == null || race.passives() == null)
            return false;

        return matchesAnyEntry(entity, race.passives().defendedByEntities());
    }

    public static List<String> getDefenders(@Nullable Player player) {
        if (player == null)
            return List.of();
        Race race = getPlayerRace(player);
        if (race == null || race.passives() == null)
            return List.of();
        return race.passives().defendedByEntities();
    }

    @Nullable
    private static Race getPlayerRace(Player player) {
        if (!(player instanceof IPlayerVariables ipv))
            return null;
        ResourceLocation raceId = ipv.getRace();
        if (raceId == null)
            return null;
        return RaceRegistry.get(raceId);
    }

    /**
     * Check if an entity matches any entry in the list (supports IDs, tags, and
     * exclusions)
     */
    private static boolean matchesAnyEntry(LivingEntity entity, List<String> entries) {
        EntityType<?> entityType = entity.getType();
        ResourceLocation entityId = EntityType.getKey(entityType);

        boolean matched = false;

        // First pass: check inclusions
        for (String entry : entries) {
            if (entry.startsWith("!")) {
                continue; // Skip exclusions in first pass
            }

            if (matchesEntry(entity, entityType, entityId, entry)) {
                matched = true;
                break;
            }
        }

        if (!matched) {
            return false;
        }

        // Second pass: check exclusions
        for (String entry : entries) {
            if (!entry.startsWith("!")) {
                continue; // Skip non-exclusions
            }

            String exclusion = entry.substring(1);

            if (matchesEntry(entity, entityType, entityId, exclusion)) {
                return false; // Excluded
            }
        }

        return true; // Matched and not excluded
    }

    /**
     * Checks if an entity matches a single spec: either a direct entity id, or a
     * #-prefixed entity type tag reference (with a MobType fallback for undead).
     */
    private static boolean matchesEntry(LivingEntity entity, EntityType<?> entityType, ResourceLocation entityId,
            String spec) {
        if (spec.startsWith("#")) {
            // Tag reference
            String tagPath = spec.substring(1);
            TagKey<EntityType<?>> tag = getOrCreateTag(java.util.Objects.requireNonNull(tagPath));

            if (entityType.is(tag)) {
                return true;
            }

            // Fallback for UNDEAD tag using MobType if tag check is unreliable
            if (tagPath.equals("minecraft:undead") && entity.getMobType() == MobType.UNDEAD) {
                return true;
            }
            return false;
        } else {
            // Direct entity ID
            return entityId.toString().equals(spec);
        }
    }
}
