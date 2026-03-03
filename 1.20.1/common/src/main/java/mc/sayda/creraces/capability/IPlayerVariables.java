package mc.sayda.creraces.capability;

import mc.sayda.creraces.ability.AbilitySlot;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;

/**
 * Interface for player-specific variables in CreRaces.
 */
public interface IPlayerVariables extends ISerializableData {
    ResourceLocation getRace();

    void setRace(ResourceLocation race);

    boolean hasChosenRace();

    void setHasChosenRace(boolean hasChosen);

    double getKarma();

    void setKarma(double karma);

    // Legacy RPG Attributes - simplified
    double getAp();

    void setAp(double ap);

    double getAd();

    double getAh();

    double getCr();

    void setAd(double ad);

    void setAh(double ah);

    void setCr(double cr);

    double getCoins();

    void setCoins(double coins);

    double getMana();

    void setMana(double mana);

    double getRage();

    void setRage(double rage);

    double getEnergy();

    void setEnergy(double energy);

    double getGrit();

    void setGrit(double grit);

    double getSouls();

    void setSouls(double souls);

    double getStacks();

    void setStacks(double stacks);

    double getPassiveCooldown();

    void setPassiveCooldown(double ticks);

    double getResourceTimer();

    void setResourceTimer(double ticks);

    Map<ResourceLocation, Integer> getCooldowns();

    void setCooldown(ResourceLocation abilityId, int ticks);

    // Channeled Ability State
    boolean isAbilityActive();

    void setAbilityActive(boolean active);

    ResourceLocation getActiveAbility();

    void setActiveAbility(ResourceLocation abilityId);

    int getActiveAbilityDuration();

    void setActiveAbilityDuration(int ticks);

    double getActiveAbilityDrain();

    void setActiveAbilityDrain(double drain);

    int getCooldown(ResourceLocation abilityId);

    void sakuyaTimeLeap();

    Set<ResourceLocation> getUnlockedAbilities();

    void unlockAbility(ResourceLocation abilityId);

    void revokeAbility(ResourceLocation abilityId);

    boolean isAbilityUnlocked(ResourceLocation abilityId);

    Map<AbilitySlot, ResourceLocation> getEquippedAbilities();

    void equipAbility(AbilitySlot slot, ResourceLocation abilityId);

    ResourceLocation getAbilityInSlot(AbilitySlot slot);

    void fantasySealReset();

    Map<String, String> getCustomizations();

    void setCustomization(String key, String value);

    String getCustomization(String key);

    double getAbilityState(ResourceLocation abilityId);

    void setAbilityState(ResourceLocation abilityId, double value);

    AbilitySlot getSlotForAbility(ResourceLocation abilityId);

    boolean isMorphed();

    void setMorphed(boolean morphed);

    java.util.UUID getTeamId();

    void setTeamId(java.util.UUID teamId);

    String getTeamName();

    void setTeamName(String teamName);

    int getGState();

    void setGState(int state);

    boolean hasPocket();

    void setHasPocket(boolean hasPocket);

    double getPocketX();

    void setPocketX(double x);

    double getPocketY();

    void setPocketY(double y);

    double getPocketZ();

    void setPocketZ(double z);

    double getReturnX();

    void setReturnX(double x);

    double getReturnY();

    void setReturnY(double y);

    double getReturnZ();

    void setReturnZ(double z);

    String getReturnDim();

    void setReturnDim(String dim);

    boolean isInSpiritRealm();

    void setInSpiritRealm(boolean inSpiritRealm);

    boolean isSmallBuild();

    void setSmallBuild(boolean smallBuild);

    void resetOnDeath();
}
