package mc.sayda.creraces.mixin;

import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin implements IPersistentDataAccessor {
    @Unique
    private CompoundTag creraces$persistentData;

    @Override
    public CompoundTag creraces$getPersistentData() {
        if (this.creraces$persistentData == null) {
            this.creraces$persistentData = new CompoundTag();
        }
        return this.creraces$persistentData;
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void creraces$readPersistentData(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("creraces:persistent_data", 10)) {
            this.creraces$persistentData = tag.getCompound("creraces:persistent_data");
        }
    }

    @Inject(method = "saveWithoutId", at = @At("TAIL"))
    private void creraces$writePersistentData(CompoundTag tag, CallbackInfoReturnable cir) {
        if (this.creraces$persistentData != null && !this.creraces$persistentData.isEmpty()) {
            tag.put("creraces:persistent_data", this.creraces$persistentData);
        }
    }

    @Inject(method = "setAirSupply", at = @At("HEAD"), cancellable = true)
    private void creraces$preventRefill(int air, CallbackInfo ci) {
        if ((Object) this instanceof net.minecraft.world.entity.player.Player player
                && !player.level().isClientSide()) {
            // If the new air value is higher than current, it's a refill attempt
            if (air > player.getAirSupply()) {
                mc.sayda.creraces.capability.DataUtils.getVariables(player).ifPresent(vars -> {
                    mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
                    if (race != null && race.passives() != null && !race.passives().canBreatheOnLand()) {
                        // Prevent refill if on land and not protected
                        if (!player.isInWaterRainOrBubble()
                                && !player.hasEffect(net.minecraft.world.effect.MobEffects.WATER_BREATHING)) {
                            ci.cancel();
                        }
                    }
                });
            }
        }
    }
}
