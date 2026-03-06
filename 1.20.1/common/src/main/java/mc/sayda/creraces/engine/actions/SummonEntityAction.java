package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;

/**
 * Action: {@code creraces:summon_entity}
 * <p>
 * Spawns a registered entity at a resolved position and optionally tames it to
 * the casting player.
 * <ul>
 * <li>{@code entity} — Entity type ID (e.g.
 * {@code "creraces:troll_pillar"}).</li>
 * <li>{@code use_raycast} — (Optional, default {@code false}) If true, places
 * the entity
 * at the block surface the player is looking at, within {@code ray_range}
 * blocks.</li>
 * <li>{@code ray_range} — (Optional ScalingValue, default {@code 10.0}) Max
 * raycast distance.</li>
 * <li>{@code tame} — (Optional, default {@code false}) If true and the entity
 * is a
 * {@link TamableAnimal}, the entity is tamed to the casting player.</li>
 * <li>{@code offset_y} — (Optional ScalingValue, default {@code 0}) Additional
 * Y offset
 * applied to the spawn position.</li>
 * </ul>
 */
public class SummonEntityAction implements ActionRegistry.RaceAction {

    private final ResourceLocation entityId;
    private final boolean useRaycast;
    private final ScalingValue rayRange;
    private final boolean tame;
    private final ScalingValue offsetY;

    public SummonEntityAction(ResourceLocation entityId, boolean useRaycast, ScalingValue rayRange,
            boolean tame, ScalingValue offsetY) {
        this.entityId = entityId;
        this.useRaycast = useRaycast;
        this.rayRange = rayRange;
        this.tame = tame;
        this.offsetY = offsetY;
    }

    @Override
    public boolean execute(Player player, @Nullable net.minecraft.world.entity.LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @Nullable BlockPos interactionPos) {

        if (player.level().isClientSide())
            return true;
        if (!(player.level() instanceof ServerLevel serverLevel))
            return false;

        // Resolve spawn position
        BlockPos spawnPos;
        if (useRaycast) {
            double range = rayRange.evaluate(player, target);
            BlockHitResult hit = serverLevel.clip(new ClipContext(
                    player.getEyePosition(1f),
                    player.getEyePosition(1f).add(player.getViewVector(1f).scale(range)),
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    player));
            if (hit.getType() == HitResult.Type.MISS) {
                return false;
            }
            spawnPos = hit.getBlockPos().above();
        } else {
            spawnPos = player.blockPosition();
        }

        // Apply Y offset
        int yOff = (int) Math.round(offsetY.evaluate(player, target));
        if (yOff != 0)
            spawnPos = spawnPos.above(yOff);

        // Resolve entity type
        EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getOptional(entityId)
                .orElse(null);
        if (type == null) {
            CreRaces.LOGGER.error("[CreRaces] SummonEntityAction: unknown entity type '{}'", entityId);
            return false;
        }

        Entity entity = type.create(serverLevel);
        if (entity == null)
            return false;

        entity.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                player.getYRot(), 0f);

        // Tame to player if requested
        if (tame && entity instanceof TamableAnimal tamable) {
            tamable.tame(player);
        }

        serverLevel.addFreshEntity(entity);
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "summon_entity"), json -> {
            String entityIdStr = GsonHelper.getAsString(json, "entity", "minecraft:zombie");
            ResourceLocation entityId = new ResourceLocation(entityIdStr);
            boolean useRaycast = GsonHelper.getAsBoolean(json, "use_raycast", false);
            ScalingValue rayRange = ScalingValue.fromJson(json, "ray_range", 10.0);
            boolean tame = GsonHelper.getAsBoolean(json, "tame", false);
            ScalingValue offsetY = ScalingValue.fromJson(json, "offset_y", 0.0);
            return new SummonEntityAction(entityId, useRaycast, rayRange, tame, offsetY);
        });
    }
}
