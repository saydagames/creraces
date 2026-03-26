package mc.sayda.creraces.capability;

import mc.sayda.creraces.ability.AbilitySlot;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

    double getSoul();

    void setSoul(double soul);

    double getPassiveCooldown();

    void setPassiveCooldown(double ticks);

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

    double getPersistentState(ResourceLocation id);

    void setPersistentState(ResourceLocation id, double value);

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

    double getPocketSize();

    void setPocketSize(double size);

    int getPocketIndex();

    void setPocketIndex(int index);

    Set<UUID> getPocketInvitations();

    void inviteToPocket(UUID uuid);

    void revokePocketInvitation(UUID uuid);

    double getPocketX();

    void setPocketX(double x);

    double getPocketY();

    void setPocketY(double y);

    double getPocketZ();

    void setPocketZ(double z);

    double getPocketSpawnX();

    void setPocketSpawnX(double x);

    double getPocketSpawnY();

    void setPocketSpawnY(double y);

    double getPocketSpawnZ();

    void setPocketSpawnZ(double z);

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
 
    boolean isUndead();
 
    void setUndead(boolean undead);
 
    boolean isAquatic();
 
    void setAquatic(boolean aquatic);
 
    boolean isSpirit();
 
    void setSpirit(boolean spirit);
 
    boolean isTiny();
 
    void setTiny(boolean tiny);


    Map<ResourceLocation, Integer> getTraitTimers();

    void setTraitTimer(ResourceLocation id, int ticks);

    long getResourceTimer();

    void setResourceTimer(long ticks);

    void resetOnDeath();

    /** Triggers a network sync for this data. */
    void sync(net.minecraft.world.entity.player.Player player);
}
