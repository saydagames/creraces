package mc.sayda.creraces.mixin;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.AquaticMovementHandler;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.registry.ModMobEffects;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(Entity.class)
public abstract class EntityMixin implements IPersistentDataAccessor {
    @Unique
    private static final String CRERACES_PERSISTENT_DATA_KEY = "creraces:persistent_data";

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
        if (tag.contains(CRERACES_PERSISTENT_DATA_KEY, Tag.TAG_COMPOUND)) {
            this.creraces$persistentData = tag.getCompound(CRERACES_PERSISTENT_DATA_KEY);
        }
    }

    @Inject(method = "saveWithoutId", at = @At("TAIL"))
    private void creraces$writePersistentData(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (this.creraces$persistentData != null && !this.creraces$persistentData.isEmpty()) {
            tag.put(CRERACES_PERSISTENT_DATA_KEY, this.creraces$persistentData);
        }
    }

    @Inject(method = "updateFluidHeightAndDoFluidPushing", at = @At("HEAD"), cancellable = true)
    private void creraces$cancelFluidDetection(TagKey<Fluid> tag, double motionScale,
            CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player && AquaticMovementHandler.isUnaffected(player, tag)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "setSwimming", at = @At("HEAD"), cancellable = true)
    private void creraces$blockSwimming(boolean swimming, CallbackInfo ci) {
        if (swimming && (Object) this instanceof Player player
                && creraces$hasPassive(player, Race.Passives::unaffectedByWater)) {
            ci.cancel();
        }
    }

    /**
     * Races that can't breathe on land get no air back out of water (unless under Water Breathing).
     * Resetting air to 0 is still allowed.
     */
    @Inject(method = "setAirSupply", at = @At("HEAD"), cancellable = true)
    private void creraces$preventRefill(int air, CallbackInfo ci) {
        if ((Object) this instanceof Player player && !player.level().isClientSide()
                && air > player.getAirSupply() && air != 0
                && !player.isInWaterRainOrBubble() && !player.hasEffect(MobEffects.WATER_BREATHING)
                && creraces$hasPassive(player, passives -> !passives.canBreatheOnLand())) {
            ci.cancel();
        }
    }

    /** Lava-immune races can't be set on fire by lava or fire blocks. */
    @Inject(method = "setRemainingFireTicks", at = @At("HEAD"), cancellable = true)
    private void creraces$suppressFireFromLava(int fireTicks, CallbackInfo ci) {
        if ((Object) this instanceof Player player && fireTicks > player.getRemainingFireTicks()
                && creraces$hasPassive(player, Race.Passives::unaffectedByLava)) {
            ci.cancel();
        }
    }

    @Inject(method = "isInvisible", at = @At("HEAD"), cancellable = true)
    private void creraces$trueInvisibilityFlag(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof LivingEntity living && ModMobEffects.isInvisible(living)) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private static boolean creraces$hasPassive(Player player, Predicate<Race.Passives> test) {
        return DataUtils.getVariables(player)
                .map(vars -> RaceRegistry.get(vars.getRace()))
                .map(Race::passives)
                .filter(test)
                .isPresent();
    }
}
