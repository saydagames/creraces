package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import java.util.Objects;
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
 * <li>{@code entity} - Entity type ID (e.g.
 * {@code "creraces:troll_pillar"}).</li>
 * <li>{@code use_raycast} - (Optional, default {@code false}) If true, places
 * the entity
 * at the block surface the player is looking at, within {@code ray_range}
 * blocks.</li>
 * <li>{@code ray_range} - (Optional ScalingValue, default {@code 10.0}) Max
 * raycast distance.</li>
 * <li>{@code range} - (Optional ScalingValue, default {@code 0.0}) Random
 * horizontal offset radius applied when not raycasting.</li>
 * <li>{@code tame} - (Optional, default {@code false}) If true and the entity
 * is a
 * {@link TamableAnimal}, the entity is tamed to the casting player.</li>
 * <li>{@code offset_y} - (Optional ScalingValue, default {@code 0}) Additional
 * Y offset
 * applied to the spawn position.</li>
 * </ul>
 */
public class SummonEntityAction implements ActionRegistry.RaceAction {

    private final ResourceLocation entityId;
    private final boolean useRaycast;
    private final boolean useTarget;
    private final ScalingValue rayRange;
    private final ScalingValue range;
    private final boolean tame;
    private final ScalingValue offsetY;

    public SummonEntityAction(ResourceLocation entityId, boolean useRaycast, boolean useTarget,
            ScalingValue rayRange, ScalingValue range, boolean tame, ScalingValue offsetY) {
        this.entityId = entityId;
        this.useRaycast = useRaycast;
        this.useTarget = useTarget;
        this.rayRange = rayRange;
        this.range = range;
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
            double rRange = rayRange.evaluate(player, target, slot);
            BlockHitResult hit = serverLevel.clip(new ClipContext(
                    player.getEyePosition(1f),
                    player.getEyePosition(1f).add(player.getViewVector(1f).scale(rRange)),
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    player));
            if (hit.getType() == HitResult.Type.MISS) {
                return false;
            }
            spawnPos = hit.getBlockPos().above();
        } else {
            BlockPos base = (useTarget && target != null) ? target.blockPosition() : player.blockPosition();
            double r = range.evaluate(player, target, slot);
            if (r > 0) {
                double dx = (player.getRandom().nextDouble() - 0.5) * r * 2.0;
                double dz = (player.getRandom().nextDouble() - 0.5) * r * 2.0;
                spawnPos = base.offset((int) dx, 0, (int) dz);

                // Ground check
                int attempts = 0;
                while (!serverLevel.getBlockState(spawnPos).isAir() && attempts < 5
                        && spawnPos.getY() < serverLevel.getMaxBuildHeight()) {
                    spawnPos = spawnPos.above();
                    attempts++;
                }
                while (serverLevel.getBlockState(spawnPos.below()).isAir() && attempts < 10
                        && spawnPos.getY() > serverLevel.getMinBuildHeight()) {
                    spawnPos = spawnPos.below();
                    attempts++;
                }
            } else {
                spawnPos = base;
            }
        }

        // Apply Y offset
        int yOff = (int) Math.round(offsetY.evaluate(player, target, slot));
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

        // Attribution and Taming
        if (entity instanceof net.minecraft.world.entity.projectile.Projectile proj) {
            proj.setOwner(player);
        }

        if (entity instanceof TamableAnimal tamable) {
            if (tame) {
                tamable.tame(player);
            } else {
                tamable.setOwnerUUID(Objects.requireNonNull(player.getUUID()));
            }
        }

        serverLevel.addFreshEntity(entity);
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "summon_entity"), json -> {
            String entityIdStr = GsonHelper.getAsString(json, "entity", "minecraft:pig");
            ResourceLocation entityId = new ResourceLocation(entityIdStr);
            boolean useRaycast = GsonHelper.getAsBoolean(json, "use_raycast", false);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            ScalingValue rayRange = ScalingValue.fromJson(json, "ray_range", 10.0);
            ScalingValue range = ScalingValue.fromJson(json, "range", 0.0);
            boolean tame = GsonHelper.getAsBoolean(json, "tame", false);
            ScalingValue offsetY = ScalingValue.fromJson(json, "offset_y", 0.0);
            return new SummonEntityAction(entityId, useRaycast, useTarget, rayRange, range, tame, offsetY);
        });
    }
}
