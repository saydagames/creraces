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

import java.util.Map;
import java.util.Set;

@Mixin(Player.class)
public class PlayerMixin implements IPlayerVariables {
    @Unique
    private final PlayerVariables creraces$variables = new PlayerVariables();

    @Inject(method = "createAttributes", at = @At("RETURN"))
    private static void creraces$createAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        // This mixin allows attributes to be registered on both Fabric and Forge
        // without platform-specific code.
        // On Fabric, we must ensure ModAttributes are registered BEFORE the Player
        // class loads to avoid a race condition.
        try {
            cir.getReturnValue().add(ModAttributes.MAX_MANA.get())
                    .add(ModAttributes.MAX_RAGE.get())
                    .add(ModAttributes.MAX_ENERGY.get())
                    .add(ModAttributes.MAX_GRIT.get())
                    .add(ModAttributes.ABILITY_POWER.get())
                    .add(ModAttributes.ATTACK_DAMAGE.get())
                    .add(ModAttributes.CRIT_RATE.get())
                    .add(ModAttributes.ARMOR_PENETRATION.get())
                    .add(ModAttributes.MANA_REGEN.get())
                    .add(ModAttributes.ENERGY_REGEN.get())
                    .add(ModAttributes.GRIT_DECAY.get())
                    .add(ModAttributes.RAGE_DECAY.get());
        } catch (Exception e) {
            // If attributes aren't registered yet, this will fail.
            // We are fixing the initialization order on Fabric to prevent this.
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
        return creraces$variables.getAp();
    }

    @Override
    public void setAp(double ap) {
        creraces$variables.setAp(ap);
    }

    @Override
    public double getAd() {
        return creraces$variables.getAd();
    }

    @Override
    public double getAh() {
        return creraces$variables.getAh();
    }

    @Override
    public double getCr() {
        return creraces$variables.getCr();
    }

    @Override
    public void setAd(double ad) {
        creraces$variables.setAd(ad);
    }

    @Override
    public void setAh(double ah) {
        creraces$variables.setAh(ah);
    }

    @Override
    public void setCr(double cr) {
        creraces$variables.setCr(cr);
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
    public double getSouls() {
        return creraces$variables.getSouls();
    }

    @Override
    public void setSouls(double souls) {
        creraces$variables.setSouls(souls);
    }

    @Override
    public double getStacks() {
        return creraces$variables.getStacks();
    }

    @Override
    public void setStacks(double stacks) {
        creraces$variables.setStacks(stacks);
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
    public double getResourceTimer() {
        return creraces$variables.getResourceTimer();
    }

    @Override
    public void setResourceTimer(double ticks) {
        creraces$variables.setResourceTimer(ticks);
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
    public double getAbilityState(ResourceLocation abilityId) {
        return creraces$variables.getAbilityState(abilityId);
    }

    @Override
    public void setAbilityState(ResourceLocation abilityId, double value) {
        creraces$variables.setAbilityState(abilityId, value);
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
    public CompoundTag serialize() {
        return creraces$variables.serialize();
    }

    @Override
    public void deserialize(CompoundTag tag) {
        creraces$variables.deserialize(tag);
    }
}
