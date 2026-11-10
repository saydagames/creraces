package mc.sayda.creraces.engine;

import com.google.gson.JsonObject;
import mc.sayda.creraces.ability.AbilitySlot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Turns an action away from the way its source looks: "yaw" degrees to the right (negative turns
 * left) and "pitch" degrees up. A sweep or a fan is a row of actions with stepped offsets.
 */
public record AimOffset(ScalingValue yaw, ScalingValue pitch) {

    public static AimOffset fromJson(JsonObject json) {
        return new AimOffset(ScalingValue.fromJson(json, "yaw", 0.0), ScalingValue.fromJson(json, "pitch", 0.0));
    }

    /** The direction {@code source} looks in, turned by the offsets. */
    public Vec3 direction(LivingEntity source, Player caster, @Nullable LivingEntity target,
            @Nullable AbilitySlot slot, @Nullable BlockPos interactPos) {
        float right = (float) yaw.evaluate(caster, target, slot, interactPos);
        float up = (float) pitch.evaluate(caster, target, slot, interactPos);
        return Vec3.directionFromRotation(source.getViewXRot(1f) - up, source.getViewYRot(1f) + right);
    }
}
