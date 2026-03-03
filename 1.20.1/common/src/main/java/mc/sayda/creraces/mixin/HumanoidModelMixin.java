package mc.sayda.creraces.mixin;

import mc.sayda.creraces.client.render.AnimationHandler;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin<T extends LivingEntity> {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void creraces$setupBeamPose(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof Player player) {
            if (AnimationHandler.isCastingBeam(player.getUUID())) {
                HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;

                // Raise both arms forward (like a kamehameha or beam cast)
                // 0 is down, -1.57 (PI/2) is straight forward
                float pitch = (headPitch * ((float) Math.PI / 180F));

                model.rightArm.xRot = -1.57F + pitch;
                model.rightArm.yRot = -0.1F;

                model.leftArm.xRot = -1.57F + pitch;
                model.leftArm.yRot = 0.1F;
            }
        }
    }
}
