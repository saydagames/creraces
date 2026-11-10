package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.CastSource;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TargetFilter;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.engine.condition.ConditionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * Runs "actions" on the first entity under the caster's crosshair within "range", the way an
 * ability aims at what you look at. Blocks stop the ray. "targets" and "condition" decide which
 * entities count; with "fail_if_empty", hitting nothing fails the action.
 */
@SuppressWarnings("null")
public class RaycastAction implements ActionRegistry.RaceAction {
    private final ScalingValue range;
    private final TargetFilter targets;
    @Nullable
    private final Condition condition;
    private final boolean failIfEmpty;
    private final List<ActionRegistry.RaceAction> actions;

    public RaycastAction(ScalingValue range, TargetFilter targets, @Nullable Condition condition, boolean failIfEmpty,
            List<ActionRegistry.RaceAction> actions) {
        this.range = range;
        this.targets = targets;
        this.condition = condition;
        this.failIfEmpty = failIfEmpty;
        this.actions = actions;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        double reach = range.evaluate(player, target, slot, interactPos);
        LivingEntity source = CastSource.self(player);
        Vec3 eye = source.getEyePosition();
        Vec3 look = source.getViewVector(1f);
        Vec3 end = eye.add(look.scale(reach));
        BlockHitResult wall = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, source));
        if (wall.getType() != HitResult.Type.MISS) {
            end = wall.getLocation();
        }

        AABB swept = source.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.level(), source, eye, end, swept,
                entity -> entity instanceof LivingEntity living && living != player && living != source
                        && living.isAlive()
                        && targets.isValid(living, player)
                        && (condition == null || condition.evaluate(player, living, slot, interactPos)));
        if (hit == null) {
            return !failIfEmpty;
        }
        return ActionRegistry.runChain(actions, player, (LivingEntity) hit.getEntity(), slot, interactPos);
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("raycast"), json -> new RaycastAction(
                ScalingValue.fromJson(json, "range", 10.0),
                TargetFilter.fromJson(json, "targets", Set.of("enemies")),
                ConditionRegistry.optional(json, "condition"),
                GsonHelper.getAsBoolean(json, "fail_if_empty", false),
                ActionRegistry.listFromJson(json, "actions")));
    }
}
