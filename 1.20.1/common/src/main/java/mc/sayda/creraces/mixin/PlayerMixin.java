package mc.sayda.creraces.mixin;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.capability.PlayerVariables;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ManagedModifier;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.race.ResourceTicker;
import mc.sayda.creraces.registry.ModAttributes;
import mc.sayda.creraces.registry.ModMobEffects;
import mc.sayda.creraces.util.RaceUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("null")
@Mixin(Player.class)
public class PlayerMixin implements IPlayerVariables {
    @Unique
    private final PlayerVariables creraces$variables = new PlayerVariables();

    @Inject(method = "tick", at = @At("TAIL"))
    private void creraces$tick(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        // On the server IncidentResolver.onIncidentTick drives ResourceTicker; ticking it here as well
        // would double-tick cooldowns and resources, so this only covers the client.
        if (!player.level().isClientSide()) {
            creraces$handleStaffPreselection(player);
            return;
        }
        ResourceTicker.tick(player);
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void creraces$onDeath(DamageSource source, CallbackInfo ci) {
        Player player = (Player)(Object)this;
        if (player.level().isClientSide()) return;
        this.resetOnDeath();
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void creraces$cancelAttack(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        var stunned = ModMobEffects.STUNNED.get();
        var disarmed = ModMobEffects.DISARMED.get();
        var frozen = ModMobEffects.FROZEN.get();
        if ((stunned != null && player.hasEffect(stunned)) ||
                (disarmed != null && player.hasEffect(disarmed)) ||
                (frozen != null && player.hasEffect(frozen))) {
            ci.cancel();
        }
    }

    @Inject(method = "canEat", at = @At("HEAD"), cancellable = true)
    private void creraces$canEatWhenFull(boolean ignoreHunger, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        DataUtils.getVariables(player).ifPresent(vars -> {
            Race race = RaceRegistry.get(vars.getRace());
            if (race != null && race.passives() != null) {
                // Food restriction check
                ItemStack stack = player.getUseItem();
                if (stack.isEmpty()) {
                    stack = player.getMainHandItem();
                    if (!stack.isEdible())
                        stack = player.getOffhandItem();
                }

                if (RaceUtils.isFoodBlocked(player, stack)) {
                    cir.setReturnValue(false);
                    return;
                }

                Race.Passives passives = race.passives();
                if (passives != null && passives.canEatWhenFull()) {
                    cir.setReturnValue(true);
                }
            }
        });
    }

    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void creraces$cancelInteraction(Entity target,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        Player player = (Player) (Object) this;
        var stunned = ModMobEffects.STUNNED.get();
        var disarmed = ModMobEffects.DISARMED.get();
        var frozen = ModMobEffects.FROZEN.get();
        if ((stunned != null && player.hasEffect(stunned)) ||
                (disarmed != null && player.hasEffect(disarmed)) ||
                (frozen != null && player.hasEffect(frozen))) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "createAttributes", at = @At("RETURN"))
    private static void creraces$createAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        // Added here instead of through a loader attribute event so one code path covers every loader.
        try {
            AttributeSupplier.Builder builder = cir.getReturnValue();
            Attribute[] attributes = {
                    ModAttributes.MAX_MANA.get(),
                    ModAttributes.MAX_RAGE.get(),
                    ModAttributes.MAX_ENERGY.get(),
                    ModAttributes.MAX_GRIT.get(),
                    ModAttributes.ABILITY_POWER.get(),
                    ModAttributes.ATTACK_DAMAGE.get(),
                    ModAttributes.CRIT_RATE.get(),
                    ModAttributes.ABILITY_HASTE.get(),
                    ModAttributes.MANA_REGEN.get(),
                    ModAttributes.ENERGY_REGEN.get(),
                    ModAttributes.GRIT_DECAY.get(),
                    ModAttributes.RAGE_DECAY.get(),
                    ModAttributes.DOUBLE_JUMP.get(),
                    ModAttributes.HEALING_RECEIVED.get(),
                    ModAttributes.ARMOR_PIERCE.get(),
                    ModAttributes.ARMOR_SHRED.get(),
                    ModAttributes.MAGIC_RESIST.get(),
                    ModAttributes.MAGIC_PIERCE.get(),
                    ModAttributes.MAGIC_SHRED.get()
            };
            for (Attribute attribute : attributes) {
                if (attribute != null)
                    builder.add(attribute);
            }
        } catch (Exception e) {
            // Usually means ModAttributes wasn't registered before Player loaded. BootstrapMixin
            // orders that on Fabric, so this should never fire, but it must be visible if it does.
            CreRaces.LOGGER.error(
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
    public void setStatePersistent(ResourceLocation id, boolean persistent) {
        creraces$variables.setStatePersistent(id, persistent);
    }

    @Override
    public boolean isStatePersistent(ResourceLocation id) {
        return creraces$variables.isStatePersistent(id);
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
    public UUID getTeamId() {
        return creraces$variables.getTeamId();
    }

    @Override
    public void setTeamId(UUID teamId) {
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
    public Set<UUID> getPocketInvitations() {
        return creraces$variables.getPocketInvitations();
    }

    @Override
    public void inviteToPocket(UUID uuid) {
        creraces$variables.inviteToPocket(uuid);
    }

    @Override
    public void revokePocketInvitation(UUID uuid) {
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
        ActionRegistry.cleanup((Player) (Object) this);
    }

    @Override
    public void sync(Player player) {
        BoundaryHandler.resyncVariables((Player) (Object) this, player);
    }

    @Override
    public Collection<ManagedModifier> getManagedModifiers() {
        return creraces$variables.getManagedModifiers();
    }

    @Override
    public int getAbilityLevel(ResourceLocation abilityId) {
        return creraces$variables.getAbilityLevel(abilityId);
    }

    @Override
    public void setAbilityLevel(ResourceLocation abilityId, int level) {
        creraces$variables.setAbilityLevel(abilityId, level);
    }

    @Override
    public Optional<ManagedModifier> getManagedModifier(UUID uuid) {
        return creraces$variables.getManagedModifier(uuid);
    }

    @Override
    public void addManagedModifier(ManagedModifier mod) {
        creraces$variables.addManagedModifier(mod);
    }

    @Override
    public void removeManagedModifier(UUID uuid) {
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
                case "follow" -> Component.translatable("msg.creraces.mode_follow").withStyle(ChatFormatting.GREEN);
                case "move" -> Component.translatable("msg.creraces.mode_move").withStyle(ChatFormatting.AQUA);
                case "attack" -> Component.translatable("msg.creraces.mode_attack").withStyle(ChatFormatting.RED);
                case "free" -> Component.translatable("msg.creraces.mode_free").withStyle(ChatFormatting.YELLOW);
                default -> Component.translatable("msg.creraces.mode_unknown");
            };

            player.displayClientMessage(Component.translatable("msg.creraces.staff_activated", modeComp), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.5f, 0.5f);

            if (player.level() instanceof ServerLevel serverLevel) {
                SimpleParticleType pt = switch (activatedMode) {
                    case "follow" -> ParticleTypes.HAPPY_VILLAGER;
                    case "move" -> ParticleTypes.SOUL;
                    case "attack" -> ParticleTypes.SOUL_FIRE_FLAME;
                    case "free" -> ParticleTypes.GLOW;
                    default -> ParticleTypes.SOUL;
                };
                serverLevel.sendParticles(pt, player.getX(), player.getY() + 2.5, player.getZ(), 10, 0.4, 0.4, 0.4,
                        0.05);
            }
        }
    }
}
