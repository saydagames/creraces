package mc.sayda.creraces.mixin;

import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.registry.ModMobEffects;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void creraces$rendering(LivingEntity entity, float entityYaw, float partialTicks, PoseStack matrixStack,
            MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (entity.hasEffect(ModMobEffects.TRUE_INVISIBILITY.get())) {
            // Don't cancel the render for the local player themselves (they'd vanish in F5
            // mode etc.)
            net.minecraft.client.player.LocalPlayer localPlayer = net.minecraft.client.Minecraft.getInstance().player;
            if (entity != localPlayer) {
                ci.cancel();
                return;
            }
        }

        // Spirit Realm Visibility
        net.minecraft.client.player.LocalPlayer localPlayer = net.minecraft.client.Minecraft.getInstance().player;
        if (localPlayer == null)
            return;

        boolean viewerInSpirit = ((IPlayerVariables) localPlayer).isInSpiritRealm();
        if (viewerInSpirit)
            return; // Spirits see everyone (Overworld + Spirits)

        boolean targetInSpirit = false;
        if (entity instanceof IPlayerVariables vars) {
            targetInSpirit = vars.isInSpiritRealm();
        } else {
            // Check for spirit tag on mobs
            targetInSpirit = entity.getTags().contains("creraces:spirit");
        }

        if (targetInSpirit) {
            ci.cancel();
        }
    }

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void creraces$hideNameTag(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity.hasEffect(ModMobEffects.TRUE_INVISIBILITY.get())) {
            cir.setReturnValue(false);
        }
    }
}
