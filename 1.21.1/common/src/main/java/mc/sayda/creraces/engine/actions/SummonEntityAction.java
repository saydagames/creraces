package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.DataValue;
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
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Spawns a registered entity at a resolved position, optionally tamed to the caster and tagged with
 * persistent data. The entity type is either a fixed id or a DataValue resolved at cast time
 * (e.g. releasing an entity type previously stored on the target).
 */
public class SummonEntityAction implements ActionRegistry.RaceAction {
    /** How far a spawn position may be pushed up out of solid blocks, and dropped down onto the ground. */
    private static final int MAX_CLIMB = 5;
    private static final int MAX_DROP = 10;

    // Exactly one of these is set, depending on whether "entity" was a literal id or a DataValue.
    @Nullable
    private final ResourceLocation fixedEntityId;
    @Nullable
    private final DataValue entityValue;

    private final boolean useRaycast;
    private final boolean useTarget;
    private final ScalingValue rayRange;
    private final ScalingValue range;
    private final boolean tame;
    private final ScalingValue offsetY;
    private final List<TagDataEntry> tagData;

    /** A "tag_data" entry: a persistent data key written onto each summoned entity. */
    record TagDataEntry(String key, DataValue value) {
    }

    public SummonEntityAction(@Nullable ResourceLocation fixedEntityId, @Nullable DataValue entityValue,
            boolean useRaycast, boolean useTarget, ScalingValue rayRange, ScalingValue range, boolean tame,
            ScalingValue offsetY, List<TagDataEntry> tagData) {
        this.fixedEntityId = fixedEntityId;
        this.entityValue = entityValue;
        this.useRaycast = useRaycast;
        this.useTarget = useTarget;
        this.rayRange = rayRange;
        this.range = range;
        this.tame = tame;
        this.offsetY = offsetY;
        this.tagData = tagData;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (player.level().isClientSide()) {
            return true;
        }

        // Positioning off the target spawns into the target's level, so a target in another
        // dimension never gets its summon in the wrong world.
        ServerLevel level;
        if (useTarget && target != null && target.level() instanceof ServerLevel targetLevel) {
            level = targetLevel;
        } else if (player.level() instanceof ServerLevel playerLevel) {
            level = playerLevel;
        } else {
            return false;
        }

        BlockPos spawnPos;
        if (useRaycast) {
            BlockHitResult hit = BlockTargeting.raycast(level, player, rayRange.evaluate(player, target, slot));
            if (hit.getType() == HitResult.Type.MISS) {
                return false;
            }
            spawnPos = hit.getBlockPos().above();
        } else {
            BlockPos base = (useTarget && target != null) ? target.blockPosition() : player.blockPosition();
            double r = range.evaluate(player, target, slot);
            spawnPos = r > 0 ? settleOnGround(level, scatter(base, r, player.getRandom())) : base;
        }
        int yOffset = (int) Math.round(offsetY.evaluate(player, target, slot));
        if (yOffset != 0) {
            spawnPos = spawnPos.above(yOffset);
        }

        ResourceLocation entityId = fixedEntityId;
        if (entityValue != null) {
            Object resolved = entityValue.resolve(player, target, slot, interactPos);
            entityId = resolved instanceof String s ? ResourceLocation.tryParse(s) : null;
            if (entityId == null) {
                return false;
            }
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
        if (type == null) {
            CreRaces.LOGGER.error("SummonEntityAction: unknown entity type '{}'", entityId);
            return false;
        }
        Entity entity = type.create(level);
        if (entity == null) {
            return false;
        }

        entity.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, player.getYRot(), 0f);
        if (entity instanceof Projectile projectile) {
            projectile.setOwner(player);
        }
        if (tame && entity instanceof TamableAnimal tamable) {
            tamable.tame(player);
        }
        if (entity instanceof IPersistentDataAccessor accessor) {
            writeTagData(tagData, accessor.creraces$getPersistentData(), player, target, slot, interactPos);
        }
        level.addFreshEntity(entity);
        return true;
    }

    /** A random position up to {@code range} blocks away from {@code base} on each horizontal axis. */
    static BlockPos scatter(BlockPos base, double range, RandomSource random) {
        double dx = (random.nextDouble() - 0.5) * range * 2.0;
        double dz = (random.nextDouble() - 0.5) * range * 2.0;
        return base.offset((int) dx, 0, (int) dz);
    }

    /** Moves a spawn position up out of solid blocks, then down until it stands on something. */
    static BlockPos settleOnGround(ServerLevel level, BlockPos pos) {
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

    static void writeTagData(List<TagDataEntry> tagData, CompoundTag tag, Player player,
            @Nullable LivingEntity target, @Nullable AbilitySlot slot, @Nullable BlockPos interactPos) {
        for (TagDataEntry entry : tagData) {
            entry.value().writeInto(tag, entry.key(), player, target, slot, interactPos);
        }
    }

    /** Parses "tag_data"; {@code context} names the action in log messages. */
    static List<TagDataEntry> parseTagData(JsonObject json, String context) {
        List<TagDataEntry> tagData = new ArrayList<>();
        if (!json.has("tag_data")) {
            return tagData;
        }
        for (JsonElement element : json.getAsJsonArray("tag_data")) {
            JsonObject entry = element.getAsJsonObject();
            String key = DataValue.sanitizeKey(GsonHelper.getAsString(entry, "key"), context);
            DataValue value = DataValue.fromJson(entry, "value", DataValue.DataType.STRING);
            if (value == null) {
                CreRaces.LOGGER.error("{}: tag_data entry '{}' has no valid 'value' - skipping", context, key);
                continue;
            }
            tagData.add(new TagDataEntry(key, value));
        }
        return tagData;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "summon_entity"), json -> {
            ResourceLocation fixedEntityId = null;
            DataValue entityValue = null;
            if (json.has("entity") && json.get("entity").isJsonObject()) {
                entityValue = DataValue.fromJson(json, "entity", DataValue.DataType.STRING);
            } else {
                fixedEntityId = ResourceLocation.parse(GsonHelper.getAsString(json, "entity", "minecraft:pig"));
            }
            return new SummonEntityAction(fixedEntityId, entityValue,
                    GsonHelper.getAsBoolean(json, "use_raycast", false),
                    GsonHelper.getAsBoolean(json, "use_target", false),
                    ScalingValue.fromJson(json, "ray_range", 10.0),
                    ScalingValue.fromJson(json, "range", 0.0),
                    GsonHelper.getAsBoolean(json, "tame", false),
                    ScalingValue.fromJson(json, "offset_y", 0.0),
                    parseTagData(json, "SummonEntityAction"));
        });
    }
}
