package mc.sayda.creraces.mixin;

import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
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
            tag.put("creraces:persistent_data", java.util.Objects.requireNonNull(this.creraces$persistentData));
        }
    }

    @Inject(method = "updateFluidHeightAndDoFluidPushing", at = @At("HEAD"), cancellable = true)
    private void creraces$cancelFluidDetection(TagKey<Fluid> tag, double d, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player) {
            if (mc.sayda.creraces.engine.AquaticMovementHandler.isUnaffected(player, tag)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "setSwimming", at = @At("HEAD"), cancellable = true)
    private void creraces$blockSwimming(boolean swimming, CallbackInfo ci) {
        if (swimming && (Object) this instanceof Player player) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null) {
                    var passives = race.passives();
                    if (passives != null && passives.unaffectedByWater()) {
                        ci.cancel();
                    }
                }
            });
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
                    if (race != null) {
                        var passives = race.passives();
                        if (passives != null && !passives.canBreatheOnLand()) {
                            // Prevent refill if on land and not protected, EXCEPT if it's a reset to 0
                            var waterBreathing = net.minecraft.world.effect.MobEffects.WATER_BREATHING;
                            if (air != 0 && !player.isInWaterRainOrBubble()
                                    && (waterBreathing == null || !player.hasEffect(waterBreathing))) {
                                ci.cancel();
                            }
                        }
                    } else {
                        player.sendSystemMessage(
                                java.util.Objects
                                        .requireNonNull(Component.translatable("creraces.message.ability_invalid")
                                                .withStyle(ChatFormatting.RED)));
                    }
                });
            }
        }
    }

    /**
     * Suppress fire-ticks for unaffectedByLava races - they cannot be set on fire
     * by lava or fire blocks.
     */
    @Inject(method = "setRemainingFireTicks", at = @At("HEAD"), cancellable = true)
    private void creraces$suppressFireFromLava(int fireTicks, CallbackInfo ci) {
        if ((Object) this instanceof Player player && fireTicks > player.getRemainingFireTicks()) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null) {
                    var passives = race.passives();
                    if (passives != null && passives.unaffectedByLava()) {
                        ci.cancel();
                    }
                }
            });
        }
    }

    @Inject(method = "isInvisible", at = @At("HEAD"), cancellable = true)
    private void creraces$trueInvisibilityFlag(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof net.minecraft.world.entity.LivingEntity living) {
            if (mc.sayda.creraces.registry.ModMobEffects.isInvisible(living)) {
                cir.setReturnValue(true);
            }
        }
    }
}
