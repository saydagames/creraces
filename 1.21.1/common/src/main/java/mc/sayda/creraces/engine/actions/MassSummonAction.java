package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Summons a random number of mobs picked from a weighted pool, scattered around the interacted
 * block or the caster. Summons that get tag_data are also made persistent so they don't despawn.
 */
public class MassSummonAction implements ActionRegistry.RaceAction {
    private final ScalingValue minCount;
    private final ScalingValue maxCount;
    private final List<WeightedEntity> pool;
    private final ScalingValue range;
    private final List<SummonEntityAction.TagDataEntry> tagData;

    private record WeightedEntity(ResourceLocation id, int weight) {
    }

    private MassSummonAction(ScalingValue minCount, ScalingValue maxCount, List<WeightedEntity> pool,
            ScalingValue range, List<SummonEntityAction.TagDataEntry> tagData) {
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.pool = pool;
        this.range = range;
        this.tagData = tagData;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (!(player.level() instanceof ServerLevel level)) {
            return true;
        }

        int min = (int) minCount.evaluate(player, target, slot);
        int max = (int) maxCount.evaluate(player, target, slot);
        int count = min + (max > min ? player.getRandom().nextInt(max - min + 1) : 0);
        int maxCap = CreRacesConfig.MASS_SUMMON_MAX_COUNT.get();
        if (maxCap > 0) {
            count = Math.min(count, maxCap);
        }

        BlockPos base = interactPos != null ? interactPos : player.blockPosition();
        double r = range.evaluate(player, target, slot);
        for (int i = 0; i < count; i++) {
            WeightedEntity picked = pickFromPool(player);
            EntityType<?> type = picked != null ? EntityType.byString(picked.id().toString()).orElse(null) : null;
            if (type == null) {
                continue;
            }

            BlockPos spawnPos = SummonEntityAction.settleOnGround(level,
                    SummonEntityAction.scatter(base, r, player.getRandom()));
            Entity summoned = type.spawn(level, spawnPos, MobSpawnType.MOB_SUMMONED);
            if (summoned instanceof Mob mob) {
                if (!tagData.isEmpty()) {
                    SummonEntityAction.writeTagData(tagData, ((IPersistentDataAccessor) mob).creraces$getPersistentData(),
                            player, target, slot, interactPos);
                    mob.setPersistenceRequired();
                }
                level.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + 1, mob.getZ(), 10, 0.5, 0.5, 0.5, 0.05);
            }
        }
        return true;
    }

    /** Weighted random pick; a pool whose weights sum to zero or less is picked from uniformly. */
    @Nullable
    private WeightedEntity pickFromPool(Player player) {
        if (pool.isEmpty()) {
            return null;
        }
        int totalWeight = pool.stream().mapToInt(WeightedEntity::weight).sum();
        if (totalWeight <= 0) {
            return pool.get(player.getRandom().nextInt(pool.size()));
        }
        int roll = player.getRandom().nextInt(totalWeight);
        int cumulative = 0;
        for (WeightedEntity entry : pool) {
            cumulative += entry.weight();
            if (roll < cumulative) {
                return entry;
            }
        }
        return pool.get(0);
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "mass_summon"), json -> {
            List<WeightedEntity> pool = new ArrayList<>();
            if (json.has("pool")) {
                for (JsonElement element : json.getAsJsonArray("pool")) {
                    JsonObject entry = element.getAsJsonObject();
                    pool.add(new WeightedEntity(ResourceLocation.parse(entry.get("entity").getAsString()),
                            GsonHelper.getAsInt(entry, "weight", 10)));
                }
            }
            return new MassSummonAction(
                    ScalingValue.fromJson(json, "min_count", 1.0),
                    ScalingValue.fromJson(json, "max_count", 3.0),
                    pool,
                    ScalingValue.fromJson(json, "range", 6.0),
                    SummonEntityAction.parseTagData(json, "MassSummonAction"));
        });
    }
}
