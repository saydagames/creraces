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

    int getCooldown(ResourceLocation abilityId);

    void sakuyaTimeLeap();

    Set<ResourceLocation> getUnlockedAbilities();

    void unlockAbility(ResourceLocation abilityId);

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
}
