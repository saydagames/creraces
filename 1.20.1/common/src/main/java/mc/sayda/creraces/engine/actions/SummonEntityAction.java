package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.DataValue;
import mc.sayda.creraces.engine.Position;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Summons "count" entities at "position" (the caster by default), each scattered up to "spread"
 * blocks and settled onto the ground. "entity_type" is an entity id, a data value that reads one (e.g.
 * the id stored on a target), or a weighted pool [{ "entity_type": id, "weight": 10 }] picked from per
 * summon. "tag_data" writes persistent data before the entity enters the world; "actions" then run
 * with each summoned living entity as the target. "natural_spawn" rolls equipment and variants the way
 * a natural spawn does; "persistent" stops summoned mobs from despawning.
 */
@SuppressWarnings("null")
public class SummonEntityAction implements ActionRegistry.RaceAction {
    /** How far a spawn position may be pushed up out of solid blocks, and dropped down onto the ground. */
    private static final int MAX_CLIMB = 5;
    private static final int MAX_DROP = 10;

    private record WeightedEntity(ResourceLocation id, int weight) {
    }

    /** A "tag_data" entry: a persistent data key written onto each summoned entity. */
    record TagDataEntry(String key, DataValue value) {
    }

    // Exactly one source of entity ids is set.
    @Nullable
    private final ResourceLocation fixedEntityId;
    @Nullable
    private final DataValue entityValue;
    private final List<WeightedEntity> pool;

    private final ScalingValue count;
    private final Position position;
    private final ScalingValue spread;
    private final boolean tame;
    private final boolean naturalSpawn;
    private final boolean persistent;
    private final List<TagDataEntry> tagData;
    private final List<ActionRegistry.RaceAction> actions;

    private SummonEntityAction(@Nullable ResourceLocation fixedEntityId, @Nullable DataValue entityValue,
            List<WeightedEntity> pool, ScalingValue count, Position position, ScalingValue spread, boolean tame,
            boolean naturalSpawn, boolean persistent, List<TagDataEntry> tagData, List<ActionRegistry.RaceAction> actions) {
        this.fixedEntityId = fixedEntityId;
        this.entityValue = entityValue;
        this.pool = pool;
        this.count = count;
        this.position = position;
        this.spread = spread;
        this.tame = tame;
        this.naturalSpawn = naturalSpawn;
        this.persistent = persistent;
        this.tagData = tagData;
        this.actions = actions;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (!(player.level() instanceof ServerLevel level)) {
            return true;
        }
        BlockPos base = position.resolveBlock(player, target, slot, interactPos);
        if (base == null) {
            return false;
        }
        int total = (int) count.evaluate(player, target, slot, interactPos);
        int cap = CreRacesConfig.MASS_SUMMON_MAX_COUNT.get();
        if (cap > 0) {
            total = Math.min(total, cap);
        }
        double scatter = spread.evaluate(player, target, slot, interactPos);

        boolean summonedAny = false;
        for (int i = 0; i < total; i++) {
            ResourceLocation entityId = pickEntity(player, target, slot, interactPos);
            EntityType<?> type = entityId != null ? BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null) : null;
            if (type == null) {
                CreRaces.LOGGER.error("SummonEntityAction: unknown entity type '{}'", entityId);
                continue;
            }
            BlockPos spawnPos = scatter > 0 ? settleOnGround(level, scatter(base, scatter, player.getRandom())) : base;
            Entity entity = summon(level, type, spawnPos, player, target, slot, interactPos);
            if (entity != null) {
                summonedAny = true;
                if (entity instanceof LivingEntity living) {
                    ActionRegistry.runChain(actions, player, living, slot, interactPos);
                }
            }
        }
        return summonedAny || total <= 0;
    }

    @Nullable
    private Entity summon(ServerLevel level, EntityType<?> type, BlockPos spawnPos, Player player,
            @Nullable LivingEntity target, @Nullable AbilitySlot slot, @Nullable BlockPos interactPos) {
        Entity entity = type.create(level);
        if (entity == null) {
            return null;
        }
        entity.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, player.getYRot(), 0f);
        if (entity instanceof Mob mob) {
            if (naturalSpawn) {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.MOB_SUMMONED, null, null);
            }
            if (persistent) {
                mob.setPersistenceRequired();
            }
        }
        if (entity instanceof Projectile projectile) {
            projectile.setOwner(player);
        }
        if (tame && entity instanceof TamableAnimal tamable) {
            tamable.tame(player);
        }
        if (entity instanceof IPersistentDataAccessor accessor) {
            CompoundTag data = accessor.creraces$getPersistentData();
            for (TagDataEntry entry : tagData) {
                entry.value().writeInto(data, entry.key(), player, target, slot, interactPos);
            }
        }
        level.addFreshEntity(entity);
        return entity;
    }

    @Nullable
    private ResourceLocation pickEntity(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (fixedEntityId != null) {
            return fixedEntityId;
        }
        if (entityValue != null) {
            return entityValue.resolve(player, target, slot, interactPos) instanceof String id
                    ? ResourceLocation.tryParse(id) : null;
        }
        int totalWeight = pool.stream().mapToInt(WeightedEntity::weight).sum();
        if (totalWeight <= 0) {
            return pool.get(player.getRandom().nextInt(pool.size())).id();
        }
        int roll = player.getRandom().nextInt(totalWeight);
        for (WeightedEntity entry : pool) {
            roll -= entry.weight();
            if (roll < 0) {
                return entry.id();
            }
        }
        return pool.get(0).id();
    }

    /** A random position up to {@code range} blocks away from {@code base} on each horizontal axis. */
    private static BlockPos scatter(BlockPos base, double range, RandomSource random) {
        double dx = (random.nextDouble() - 0.5) * range * 2.0;
        double dz = (random.nextDouble() - 0.5) * range * 2.0;
        return base.offset((int) dx, 0, (int) dz);
    }

    /** Moves a spawn position up out of solid blocks, then down until it stands on something. */
    private static BlockPos settleOnGround(ServerLevel level, BlockPos pos) {
        for (int climbed = 0; climbed < MAX_CLIMB && !level.getBlockState(pos).isAir()
                && pos.getY() < level.getMaxBuildHeight(); climbed++) {
            pos = pos.above();
        }
        for (int dropped = 0; dropped < MAX_DROP && level.getBlockState(pos.below()).isAir()
                && pos.getY() > level.getMinBuildHeight(); dropped++) {
            pos = pos.below();
        }
        return pos;
    }

    private static List<TagDataEntry> parseTagData(JsonObject json) {
        List<TagDataEntry> tagData = new ArrayList<>();
        if (!json.has("tag_data")) {
            return tagData;
        }
        for (JsonElement element : json.getAsJsonArray("tag_data")) {
            JsonObject entry = element.getAsJsonObject();
            String key = DataValue.sanitizeKey(GsonHelper.getAsString(entry, "key"), "summon_entity");
            DataValue value = DataValue.fromJson(entry, "value", DataValue.DataType.STRING);
            if (value == null) {
                throw new IllegalArgumentException("tag_data entry '" + key + "' has no valid \"value\"");
            }
            tagData.add(new TagDataEntry(key, value));
        }
        return tagData;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("summon_entity"), json -> {
            ResourceLocation fixedEntityId = null;
            DataValue entityValue = null;
            List<WeightedEntity> pool = new ArrayList<>();
            JsonElement entity = json.get("entity_type");
            if (entity == null) {
                throw new IllegalArgumentException("needs an \"entity_type\"");
            } else if (entity.isJsonArray()) {
                for (JsonElement element : entity.getAsJsonArray()) {
                    JsonObject entry = element.getAsJsonObject();
                    ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(entry, "entity_type"));
                    if (id == null) {
                        throw new IllegalArgumentException("malformed entity id in pool");
                    }
                    pool.add(new WeightedEntity(id, GsonHelper.getAsInt(entry, "weight", 1)));
                }
                if (pool.isEmpty()) {
                    throw new IllegalArgumentException("the entity pool is empty");
                }
            } else if (entity.isJsonObject()) {
                entityValue = DataValue.fromJson(json, "entity_type", DataValue.DataType.STRING);
            } else {
                fixedEntityId = ResourceLocation.tryParse(entity.getAsString());
                if (fixedEntityId == null) {
                    throw new IllegalArgumentException("malformed entity id '" + entity.getAsString() + "'");
                }
            }
            return new SummonEntityAction(fixedEntityId, entityValue, pool,
                    ScalingValue.fromJson(json, "count", 1.0),
                    Position.fromJson(json, "position", Position.of(Position.Source.SELF)),
                    ScalingValue.fromJson(json, "spread", 0.0),
                    GsonHelper.getAsBoolean(json, "tame", false),
                    GsonHelper.getAsBoolean(json, "natural_spawn", false),
                    GsonHelper.getAsBoolean(json, "persistent", false),
                    parseTagData(json),
                    ActionRegistry.listFromJson(json, "actions"));
        });
    }
}
