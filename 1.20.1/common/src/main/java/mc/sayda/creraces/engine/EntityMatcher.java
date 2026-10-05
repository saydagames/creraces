package mc.sayda.creraces.engine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Matches entities against a list of entity ids and/or #tags, e.g. a global handler's
 * whitelist/blacklist. An empty matcher matches nothing, so a blacklist can be checked
 * unconditionally and an empty/missing whitelist can be treated as "allow all" by the caller.
 */
public final class EntityMatcher {
    private static final EntityMatcher EMPTY = new EntityMatcher(Set.of(), List.of());

    private final Set<ResourceLocation> ids;
    private final List<TagKey<EntityType<?>>> tags;

    private EntityMatcher(Set<ResourceLocation> ids, List<TagKey<EntityType<?>>> tags) {
        this.ids = ids;
        this.tags = tags;
    }

    public boolean isEmpty() {
        return ids.isEmpty() && tags.isEmpty();
    }

    public boolean matches(Entity entity) {
        if (isEmpty()) {
            return false;
        }
        if (ids.contains(EntityType.getKey(entity.getType()))) {
            return true;
        }
        for (TagKey<EntityType<?>> tag : tags) {
            if (entity.getType().is(tag)) {
                return true;
            }
        }
        return false;
    }

    public static EntityMatcher fromJson(JsonObject json, String member) {
        if (!json.has(member) || !json.get(member).isJsonArray()) {
            return EMPTY;
        }
        JsonArray array = json.getAsJsonArray(member);
        Set<ResourceLocation> ids = new HashSet<>();
        List<TagKey<EntityType<?>>> tags = new ArrayList<>();

        for (JsonElement e : array) {
            if (!e.isJsonPrimitive()) {
                continue;
            }
            String raw = e.getAsString();
            if (raw.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(raw.substring(1));
                if (tagId != null) {
                    tags.add(TagKey.create(Registries.ENTITY_TYPE, tagId));
                } else {
                    CreRaces.LOGGER.warn("EntityMatcher: malformed tag '{}' in '{}'", raw, member);
                }
            } else {
                ResourceLocation id = ResourceLocation.tryParse(raw);
                if (id != null) {
                    ids.add(id);
                } else {
                    CreRaces.LOGGER.warn("EntityMatcher: malformed entity id '{}' in '{}'", raw, member);
                }
            }
        }

        return new EntityMatcher(ids, tags);
    }
}
