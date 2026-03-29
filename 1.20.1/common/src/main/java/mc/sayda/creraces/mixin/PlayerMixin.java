package mc.sayda.creraces.mixin;

import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.capability.PlayerVariables;
import mc.sayda.creraces.registry.ModAttributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import mc.sayda.creraces.item.CommandingStaffItem;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.Set;

@SuppressWarnings("null")
@Mixin(Player.class)
public class PlayerMixin implements IPlayerVariables {
    @Unique
    private final PlayerVariables creraces$variables = new PlayerVariables();

    @Inject(method = "tick", at = @At("TAIL"))
    private void creraces$tick(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        // ResourceTicker is called server-side exclusively by
        // IncidentResolver.onGensokyoTick.
        // Calling it here caused double-ticking of cooldowns/resources every server
        // tick.
        // On the client, ticking is handled by CreRacesClient via ClientTickEvent.
        if (!player.level().isClientSide()) {
            creraces$handleStaffPreselection(player);
            return;
        }
        mc.sayda.creraces.race.ResourceTicker.tick(player);
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void creraces$onDeath(net.minecraft.world.damagesource.DamageSource source, CallbackInfo ci) {
        this.resetOnDeath();
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void creraces$cancelAttack(net.minecraft.world.entity.Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        var stunned = mc.sayda.creraces.registry.ModMobEffects.STUNNED.get();
        var disarmed = mc.sayda.creraces.registry.ModMobEffects.DISARMED.get();
        var frozen = mc.sayda.creraces.registry.ModMobEffects.FROZEN.get();
        if ((stunned != null && player.hasEffect(stunned)) ||
                (disarmed != null && player.hasEffect(disarmed)) ||
                (frozen != null && player.hasEffect(frozen))) {
            ci.cancel();
        }
    }

    @Inject(method = "canEat", at = @At("HEAD"), cancellable = true)
    private void creraces$canEatWhenFull(boolean ignoreHunger, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        mc.sayda.creraces.capability.DataUtils.getVariables(player).ifPresent(vars -> {
            mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
            if (race != null && race.passives() != null) {
                // Food restriction check
                net.minecraft.world.item.ItemStack stack = player.getUseItem();
                if (stack.isEmpty()) {
                    stack = player.getMainHandItem();
                    if (!stack.isEdible())
                        stack = player.getOffhandItem();
                }

                if (mc.sayda.creraces.util.RaceUtils.isFoodBlocked(player, stack)) {
                    cir.setReturnValue(false);
                    return;
                }

                mc.sayda.creraces.race.Race.Passives passives = race.passives();
                if (passives != null && passives.canEatWhenFull()) {
                    cir.setReturnValue(true);
                }
            }
        });
    }

    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void creraces$cancelInteraction(net.minecraft.world.entity.Entity target,
            net.minecraft.world.InteractionHand hand,
            CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir) {
        Player player = (Player) (Object) this;
        var stunned = mc.sayda.creraces.registry.ModMobEffects.STUNNED.get();
        var disarmed = mc.sayda.creraces.registry.ModMobEffects.DISARMED.get();
        var frozen = mc.sayda.creraces.registry.ModMobEffects.FROZEN.get();
        if ((stunned != null && player.hasEffect(stunned)) ||
                (disarmed != null && player.hasEffect(disarmed)) ||
                (frozen != null && player.hasEffect(frozen))) {
            cir.setReturnValue(net.minecraft.world.InteractionResult.FAIL);
        }
    }

    @Inject(method = "createAttributes", at = @At("RETURN"))
    private static void creraces$createAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        // This mixin allows attributes to be registered on both Fabric and Forge
        // without platform-specific code.
        // On Fabric, we must ensure ModAttributes are registered BEFORE the Player
        // class loads to avoid a race condition.
        try {
            var maxMana = ModAttributes.MAX_MANA.get();
            if (maxMana != null)
                cir.getReturnValue().add(maxMana);
            var maxRage = ModAttributes.MAX_RAGE.get();
            if (maxRage != null)
                cir.getReturnValue().add(maxRage);
            var maxEnergy = ModAttributes.MAX_ENERGY.get();
            if (maxEnergy != null)
                cir.getReturnValue().add(maxEnergy);
            var maxGrit = ModAttributes.MAX_GRIT.get();
            if (maxGrit != null)
                cir.getReturnValue().add(maxGrit);
            var abilityPower = ModAttributes.ABILITY_POWER.get();
            if (abilityPower != null)
                cir.getReturnValue().add(abilityPower);
            var attackDamage = ModAttributes.ATTACK_DAMAGE.get();
            if (attackDamage != null)
                cir.getReturnValue().add(attackDamage);
            var critRate = ModAttributes.CRIT_RATE.get();
            if (critRate != null)
                cir.getReturnValue().add(critRate);
            var abilityHaste = ModAttributes.ABILITY_HASTE.get();
            if (abilityHaste != null)
                cir.getReturnValue().add(abilityHaste);
            var manaRegen = ModAttributes.MANA_REGEN.get();
            if (manaRegen != null)
                cir.getReturnValue().add(manaRegen);
            var energyRegen = ModAttributes.ENERGY_REGEN.get();
            if (energyRegen != null)
                cir.getReturnValue().add(energyRegen);
            var gritDecay = ModAttributes.GRIT_DECAY.get();
            if (gritDecay != null)
                cir.getReturnValue().add(gritDecay);
            var rageDecay = ModAttributes.RAGE_DECAY.get();
            if (rageDecay != null)
                cir.getReturnValue().add(rageDecay);
            var doubleJump = ModAttributes.DOUBLE_JUMP.get();
            if (doubleJump != null)
                cir.getReturnValue().add(doubleJump);

            // Advanced Combat Attributes
            var healRec = ModAttributes.HEALING_RECEIVED.get();
            if (healRec != null)
                cir.getReturnValue().add(healRec);
            var armP = ModAttributes.ARMOR_PIERCE.get();
            if (armP != null)
                cir.getReturnValue().add(armP);
            var armS = ModAttributes.ARMOR_SHRED.get();
            if (armS != null)
                cir.getReturnValue().add(armS);
            var magR = ModAttributes.MAGIC_RESIST.get();
            if (magR != null)
                cir.getReturnValue().add(magR);
            var magP = ModAttributes.MAGIC_PIERCE.get();
            if (magP != null)
                cir.getReturnValue().add(magP);
            var magS = ModAttributes.MAGIC_SHRED.get();
            if (magS != null)
                cir.getReturnValue().add(magS);
        } catch (Exception e) {
            // Attribute registration failed - this usually means ModAttributes.init()
            // hasn't run yet (a Fabric bootstrap ordering issue). BootstrapMixin should
            // prevent this, but log it so it's visible if it ever happens.
            com.mojang.logging.LogUtils.getLogger().error(
                    "Failed to add custom attributes to Player.createAttributes: {}", e.getMessage());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void creraces$readAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("creraces:data", 10)) {
            this.deserialize(tag.getCompound("creraces:data"));
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void creraces$addAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        tag.put("creraces:data", this.serialize());
    }

    // IPlayerVariables implementation
    @Override
    public ResourceLocation getRace() {
        return creraces$variables.getRace();
    }

    @Override
    public void setRace(ResourceLocation race) {
        creraces$variables.setRace(race);
    }

    @Override
    public boolean hasChosenRace() {
        return creraces$variables.hasChosenRace();
    }

    @Override
    public void setHasChosenRace(boolean hasChosen) {
        creraces$variables.setHasChosenRace(hasChosen);
    }

    @Override
    public double getKarma() {
        return creraces$variables.getKarma();
    }

    @Override
    public void setKarma(double karma) {
        creraces$variables.setKarma(karma);
    }

    @Override
    public double getAp() {
        var attr = ModAttributes.ABILITY_POWER.get();
        return attr != null ? ((Player) (Object) this).getAttributeValue(attr) : 0.0;
    }

    @Override
    public void setAp(double ap) {
        var attr = ModAttributes.ABILITY_POWER.get();
        if (attr != null) {
            var inst = ((Player) (Object) this).getAttribute(attr);
            if (inst != null)
                inst.setBaseValue(ap);
        }
    }

    @Override
    public double getAd() {
        var attr = ModAttributes.ATTACK_DAMAGE.get();
        return attr != null ? ((Player) (Object) this).getAttributeValue(attr) : 0.0;
    }

    @Override
    public void setAd(double ad) {
        var attr = ModAttributes.ATTACK_DAMAGE.get();
        if (attr != null) {
            var inst = ((Player) (Object) this).getAttribute(attr);
            if (inst != null)
                inst.setBaseValue(ad);
        }
    }

    @Override
    public double getAh() {
        var attr = ModAttributes.resolve(ModAttributes.ABILITY_HASTE.get());
        if (attr != null) {
            double val = ((Player) (Object) this).getAttributeValue(attr);
            if (ModAttributes.isPercentAttribute(attr))
                return val * 100.0;
            return val;
        }
        return 0.0;
    }

    @Override
    public void setAh(double ah) {
        var attr = ModAttributes.resolve(ModAttributes.ABILITY_HASTE.get());
        if (attr != null) {
            var inst = ((Player) (Object) this).getAttribute(attr);
            if (inst != null) {
                if (ModAttributes.isPercentAttribute(attr))
                    inst.setBaseValue(ah / 100.0);
                else
                    inst.setBaseValue(ah);
            }
        }
    }

    @Override
    public double getCr() {
        var attr = ModAttributes.resolve(ModAttributes.CRIT_RATE.get());
        if (attr != null) {
            double val = ((Player) (Object) this).getAttributeValue(attr);
            if (ModAttributes.isPercentAttribute(attr))
                return val * 100.0;
            return val;
        }
        return 0.0;
    }

    @Override
    public void setCr(double cr) {
        var attr = ModAttributes.resolve(ModAttributes.CRIT_RATE.get());
        if (attr != null) {
            var inst = ((Player) (Object) this).getAttribute(attr);
            if (inst != null) {
                if (ModAttributes.isPercentAttribute(attr))
                    inst.setBaseValue(cr / 100.0);
                else
                    inst.setBaseValue(cr);
            }
        }
    }

    @Override
    public double getCoins() {
        return creraces$variables.getCoins();
    }

    @Override
    public void setCoins(double coins) {
        creraces$variables.setCoins(coins);
    }

    @Override
    public double getMana() {
        return creraces$variables.getMana();
    }

    @Override
    public void setMana(double mana) {
        creraces$variables.setMana(mana);
    }

    @Override
    public double getRage() {
        return creraces$variables.getRage();
    }

    @Override
    public void setRage(double rage) {
        creraces$variables.setRage(rage);
    }

    @Override
    public double getEnergy() {
        return creraces$variables.getEnergy();
    }

    @Override
    public void setEnergy(double energy) {
        creraces$variables.setEnergy(energy);
    }

    @Override
    public double getGrit() {
        return creraces$variables.getGrit();
    }

    @Override
    public void setGrit(double grit) {
        creraces$variables.setGrit(grit);
    }

    @Override
    public double getSoul() {
        return creraces$variables.getSoul();
    }

    @Override
    public void setSoul(double soul) {
        creraces$variables.setSoul(soul);
    }

    @Override
    public double getPassiveCooldown() {
        return creraces$variables.getPassiveCooldown();
    }

    @Override
    public void setPassiveCooldown(double ticks) {
        creraces$variables.setPassiveCooldown(ticks);
    }

    @Override
    public Map<ResourceLocation, Integer> getCooldowns() {
        return creraces$variables.getCooldowns();
    }

    @Override
    public void setCooldown(ResourceLocation abilityId, int ticks) {
        creraces$variables.setCooldown(abilityId, ticks);
    }

    @Override
    public int getCooldown(ResourceLocation abilityId) {
        return creraces$variables.getCooldown(abilityId);
    }

    @Override
    public void sakuyaTimeLeap() {
        creraces$variables.sakuyaTimeLeap();
    }

    @Override
    public Set<ResourceLocation> getUnlockedAbilities() {
        return creraces$variables.getUnlockedAbilities();
    }

    @Override
    public void unlockAbility(ResourceLocation abilityId) {
        creraces$variables.unlockAbility(abilityId);
    }

    @Override
    public void revokeAbility(ResourceLocation abilityId) {
        creraces$variables.revokeAbility(abilityId);
    }

    @Override
    public boolean isAbilityUnlocked(ResourceLocation abilityId) {
        return creraces$variables.isAbilityUnlocked(abilityId);
    }

    @Override
    public Map<AbilitySlot, ResourceLocation> getEquippedAbilities() {
        return creraces$variables.getEquippedAbilities();
    }

    @Override
    public void equipAbility(AbilitySlot slot, ResourceLocation abilityId) {
        creraces$variables.equipAbility(slot, abilityId);
    }

    @Override
    public ResourceLocation getAbilityInSlot(AbilitySlot slot) {
        return creraces$variables.getAbilityInSlot(slot);
    }

    @Override
    public void fantasySealReset() {
        creraces$variables.fantasySealReset();
    }

    @Override
    public Map<String, String> getCustomizations() {
        return creraces$variables.getCustomizations();
    }

    @Override
    public void setCustomization(String key, String value) {
        creraces$variables.setCustomization(key, value);
    }

    @Override
    public String getCustomization(String key) {
        return creraces$variables.getCustomization(key);
    }

    @Override
    public double getPersistentState(ResourceLocation id) {
        return creraces$variables.getPersistentState(id);
    }

    @Override
    public void setPersistentState(ResourceLocation id, double value) {
        creraces$variables.setPersistentState(id, value);
    }

    @Override
    public AbilitySlot getSlotForAbility(ResourceLocation abilityId) {
        return creraces$variables.getSlotForAbility(abilityId);
    }

    @Override
    public boolean isMorphed() {
        return creraces$variables.isMorphed();
    }

    @Override
    public void setMorphed(boolean morphed) {
        creraces$variables.setMorphed(morphed);
    }

    @Override
    public java.util.UUID getTeamId() {
        return creraces$variables.getTeamId();
    }

    @Override
    public void setTeamId(java.util.UUID teamId) {
        creraces$variables.setTeamId(teamId);
    }

    @Override
    public String getTeamName() {
        return creraces$variables.getTeamName();
    }

    @Override
    public void setTeamName(String teamName) {
        creraces$variables.setTeamName(teamName);
    }

    @Override
    public int getGState() {
        return creraces$variables.getGState();
    }

    @Override
    public void setGState(int state) {
        creraces$variables.setGState(state);
    }

    @Override
    public boolean hasPocket() {
        return creraces$variables.hasPocket();
    }

    @Override
    public void setHasPocket(boolean hasPocket) {
        creraces$variables.setHasPocket(hasPocket);
    }

    @Override
    public double getPocketSize() {
        return creraces$variables.getPocketSize();
    }

    @Override
    public void setPocketSize(double size) {
        creraces$variables.setPocketSize(size);
    }

    @Override
    public int getPocketIndex() {
        return creraces$variables.getPocketIndex();
    }

    @Override
    public void setPocketIndex(int index) {
        creraces$variables.setPocketIndex(index);
    }

    @Override
    public java.util.Set<java.util.UUID> getPocketInvitations() {
        return creraces$variables.getPocketInvitations();
    }

    @Override
    public void inviteToPocket(java.util.UUID uuid) {
        creraces$variables.inviteToPocket(uuid);
    }

    @Override
    public void revokePocketInvitation(java.util.UUID uuid) {
        creraces$variables.revokePocketInvitation(uuid);
    }

    @Override
    public double getPocketX() {
        return creraces$variables.getPocketX();
    }

    @Override
    public void setPocketX(double x) {
        creraces$variables.setPocketX(x);
    }

    @Override
    public double getPocketY() {
        return creraces$variables.getPocketY();
    }

    @Override
    public void setPocketY(double y) {
        creraces$variables.setPocketY(y);
    }

    @Override
    public double getPocketZ() {
        return creraces$variables.getPocketZ();
    }

    @Override
    public void setPocketZ(double z) {
        creraces$variables.setPocketZ(z);
    }

    @Override
    public double getPocketSpawnX() {
        return creraces$variables.getPocketSpawnX();
    }

    @Override
    public void setPocketSpawnX(double x) {
        creraces$variables.setPocketSpawnX(x);
    }

    @Override
    public double getPocketSpawnY() {
        return creraces$variables.getPocketSpawnY();
    }

    @Override
    public void setPocketSpawnY(double y) {
        creraces$variables.setPocketSpawnY(y);
    }

    @Override
    public double getPocketSpawnZ() {
        return creraces$variables.getPocketSpawnZ();
    }

    @Override
    public void setPocketSpawnZ(double z) {
        creraces$variables.setPocketSpawnZ(z);
    }

    @Override
    public double getReturnX() {
        return creraces$variables.getReturnX();
    }

    @Override
    public void setReturnX(double x) {
        creraces$variables.setReturnX(x);
    }

    @Override
    public double getReturnY() {
        return creraces$variables.getReturnY();
    }

    @Override
    public void setReturnY(double y) {
        creraces$variables.setReturnY(y);
    }

    @Override
    public double getReturnZ() {
        return creraces$variables.getReturnZ();
    }

    @Override
    public void setReturnZ(double z) {
        creraces$variables.setReturnZ(z);
    }

    @Override
    public String getReturnDim() {
        return creraces$variables.getReturnDim();
    }

    @Override
    public void setReturnDim(String dim) {
        creraces$variables.setReturnDim(dim);
    }

    @Override
    public boolean isInSpiritRealm() {
        return creraces$variables.isInSpiritRealm();
    }

    @Override
    public void setInSpiritRealm(boolean inSpiritRealm) {
        creraces$variables.setInSpiritRealm(inSpiritRealm);
    }

    @Override
    public boolean isSmallBuild() {
        return creraces$variables.isSmallBuild();
    }

    @Override
    public void setSmallBuild(boolean smallBuild) {
        creraces$variables.setSmallBuild(smallBuild);
    }

    @Override
    public boolean isUndead() {
        return creraces$variables.isUndead();
    }

    @Override
    public void setUndead(boolean undead) {
        creraces$variables.setUndead(undead);
    }

    @Override
    public boolean isAquatic() {
        return creraces$variables.isAquatic();
    }

    @Override
    public void setAquatic(boolean aquatic) {
        creraces$variables.setAquatic(aquatic);
    }

    @Override
    public boolean isSpirit() {
        return creraces$variables.isSpirit();
    }

    @Override
    public void setSpirit(boolean spirit) {
        creraces$variables.setSpirit(spirit);
    }

    @Override
    public boolean isTiny() {
        return creraces$variables.isTiny();
    }

    @Override
    public void setTiny(boolean tiny) {
        creraces$variables.setTiny(tiny);
    }

    @Override
    public boolean isAbilityActive() {
        return creraces$variables.isAbilityActive();
    }

    @Override
    public void setAbilityActive(boolean active) {
        creraces$variables.setAbilityActive(active);
    }

    @Override
    public ResourceLocation getActiveAbility() {
        return creraces$variables.getActiveAbility();
    }

    @Override
    public void setActiveAbility(ResourceLocation abilityId) {
        creraces$variables.setActiveAbility(abilityId);
    }

    @Override
    public int getActiveAbilityDuration() {
        return creraces$variables.getActiveAbilityDuration();
    }

    @Override
    public void setActiveAbilityDuration(int ticks) {
        creraces$variables.setActiveAbilityDuration(ticks);
    }

    @Override
    public double getActiveAbilityDrain() {
        return creraces$variables.getActiveAbilityDrain();
    }

    @Override
    public void setActiveAbilityDrain(double drain) {
        creraces$variables.setActiveAbilityDrain(drain);
    }

    @Override
    public CompoundTag serialize() {
        return creraces$variables.serialize();
    }

    @Override
    public long getResourceTimer() {
        return creraces$variables.getResourceTimer();
    }

    @Override
    public void setResourceTimer(long ticks) {
        creraces$variables.setResourceTimer(ticks);
    }

    @Override
    public void deserialize(CompoundTag tag) {
        creraces$variables.deserialize(tag);
    }

    @Override
    public Map<ResourceLocation, Integer> getTraitTimers() {
        return this.creraces$variables.getTraitTimers();
    }

    @Override
    public void setTraitTimer(ResourceLocation id, int ticks) {
        this.creraces$variables.setTraitTimer(id, ticks);
    }

    @Override
    public void resetOnDeath() {
        creraces$variables.resetOnDeath();
        mc.sayda.creraces.engine.ActionRegistry.cleanup((Player) (Object) this);
    }

    @Override
    public void sync(Player player) {
        mc.sayda.creraces.network.BoundaryHandler.resyncVariables((Player) (Object) this, player);
    }

    @Override
    public java.util.Collection<mc.sayda.creraces.engine.ManagedModifier> getManagedModifiers() {
        return creraces$variables.getManagedModifiers();
    }

    @Override
    public void addManagedModifier(mc.sayda.creraces.engine.ManagedModifier mod) {
        creraces$variables.addManagedModifier(mod);
    }

    @Override
    public void removeManagedModifier(java.util.UUID uuid) {
        creraces$variables.removeManagedModifier(uuid);
    }

    @Override
    public void clearManagedModifiers() {
        creraces$variables.clearManagedModifiers();
    }

    @Unique
    private void creraces$handleStaffPreselection(Player player) {
        if (player.isShiftKeyDown())
            return;

        // Check both hands
        creraces$checkAndActivateStaff(player, player.getMainHandItem());
        creraces$checkAndActivateStaff(player, player.getOffhandItem());
    }

    @Unique
    private void creraces$checkAndActivateStaff(Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof CommandingStaffItem))
            return;

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("PendingMode")) {
            String activatedMode = tag.getString("PendingMode");
            tag.putString("CommandMode", activatedMode);
            tag.remove("PendingMode");

            MutableComponent modeComp = switch (activatedMode) {
                case "follow" -> Component.translatable("message.creraces.mode_follow").withStyle(ChatFormatting.GREEN);
                case "move" -> Component.translatable("message.creraces.mode_move").withStyle(ChatFormatting.AQUA);
                case "attack" -> Component.translatable("message.creraces.mode_attack").withStyle(ChatFormatting.RED);
                case "free" -> Component.translatable("message.creraces.mode_free").withStyle(ChatFormatting.YELLOW);
                default -> Component.translatable("message.creraces.mode_unknown");
            };

            player.displayClientMessage(Component.translatable("message.creraces.staff_activated", modeComp), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.5f, 0.5f);

            if (player.level() instanceof ServerLevel serverLevel) {
                net.minecraft.core.particles.SimpleParticleType pt = switch (activatedMode) {
                    case "follow" -> net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER;
                    case "move" -> net.minecraft.core.particles.ParticleTypes.SOUL;
                    case "attack" -> net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME;
                    case "free" -> net.minecraft.core.particles.ParticleTypes.GLOW;
                    default -> net.minecraft.core.particles.ParticleTypes.SOUL;
                };
                serverLevel.sendParticles(pt, player.getX(), player.getY() + 2.5, player.getZ(), 10, 0.4, 0.4, 0.4,
                        0.05);
            }
        }
    }
}
