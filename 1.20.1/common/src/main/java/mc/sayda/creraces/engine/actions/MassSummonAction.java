package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MassSummonAction implements ActionRegistry.RaceAction {
    private final ScalingValue minCount;
    private final ScalingValue maxCount;
    private final List<WeightedEntity> pool;
    private final ScalingValue range;
    private final boolean markAsServant;

    public MassSummonAction(ScalingValue minCount, ScalingValue maxCount, List<WeightedEntity> pool, ScalingValue range, boolean markAsServant) {
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.pool = pool;
        this.range = range;
        this.markAsServant = markAsServant;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot, @Nullable BlockPos interact_pos) {
        if (player.level().isClientSide) return true;

        ServerLevel level = (ServerLevel) player.level();
        int min = (int) minCount.evaluate(player, target, slot);
        int max = (int) maxCount.evaluate(player, target, slot);
        int numToSummon = min + (max > min ? player.getRandom().nextInt(max - min + 1) : 0);
        int maxCap = mc.sayda.creraces.config.CreRacesConfig.MASS_SUMMON_MAX_COUNT.get();
        if (maxCap > 0) numToSummon = Math.min(numToSummon, maxCap);
        
        BlockPos spawnBase = interact_pos != null ? interact_pos : player.blockPosition();
        double r = range.evaluate(player, target, slot);

        for (int i = 0; i < numToSummon; i++) {
            WeightedEntity weighted = getRandomFromPool(player);
            if (weighted == null) continue;

            EntityType<?> type = EntityType.byString(weighted.id.toString()).orElse(null);
            if (type == null) continue;

            double dx = (player.getRandom().nextDouble() - 0.5) * r * 2.0;
            double dz = (player.getRandom().nextDouble() - 0.5) * r * 2.0;
            BlockPos spawnPos = spawnBase.offset((int)dx, 0, (int)dz);
            
            // Ground check
            int upAttempts = 0;
            while (!level.getBlockState(spawnPos).isAir() && upAttempts < 5 && spawnPos.getY() < level.getMaxBuildHeight()) {
                spawnPos = spawnPos.above();
                upAttempts++;
            }
            int downAttempts = 0;
            while (level.getBlockState(spawnPos.below()).isAir() && downAttempts < 10 && spawnPos.getY() > level.getMinBuildHeight()) {
                spawnPos = spawnPos.below();
                downAttempts++;
            }

            Entity summoned = type.spawn(level, spawnPos, MobSpawnType.MOB_SUMMONED);
            if (summoned == null) continue;
            
            if (summoned instanceof Mob mob) {
                if (markAsServant) {
                    CompoundTag nbt = ((IPersistentDataAccessor) mob).creraces$getPersistentData();
                    nbt.putUUID("creraces:servant_of", player.getUUID());
                    mob.setPersistenceRequired();
                }
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, mob.getX(), mob.getY() + 1, mob.getZ(), 10, 0.5, 0.5, 0.5, 0.05);
            }
        }

        return true;
    }

    private @Nullable WeightedEntity getRandomFromPool(Player player) {
        if (pool.isEmpty()) return null;
        int totalWeight = pool.stream().mapToInt(e -> e.weight).sum();
        if (totalWeight <= 0) return pool.get(player.getRandom().nextInt(pool.size()));
        
        int r = player.getRandom().nextInt(totalWeight);
        int current = 0;
        for (WeightedEntity e : pool) {
            current += e.weight;
            if (r < current) return e;
        }
        return pool.get(0);
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "mass_summon"), (json) -> {
            ScalingValue min = json.has("min_count") ? ScalingValue.fromJson(json, "min_count", 1) : new ScalingValue(1, null, 0, new ArrayList<>());
            ScalingValue max = json.has("max_count") ? ScalingValue.fromJson(json, "max_count", 3) : new ScalingValue(3, null, 0, new ArrayList<>());
            ScalingValue range = ScalingValue.fromJson(json, "range", 6.0);
            boolean servant = json.has("mark_as_servant") && json.get("mark_as_servant").getAsBoolean();
            
            List<WeightedEntity> pool = new ArrayList<>();
            if (json.has("pool")) {
                JsonArray arr = json.getAsJsonArray("pool");
                for (JsonElement e : arr) {
                    JsonObject obj = e.getAsJsonObject();
                    pool.add(new WeightedEntity(
                        new ResourceLocation(obj.get("entity").getAsString()),
                        obj.has("weight") ? obj.get("weight").getAsInt() : 10
                    ));
                }
            }
            return new MassSummonAction(min, max, pool, range, servant);
        });
    }

    private static record WeightedEntity(ResourceLocation id, int weight) {}
}
