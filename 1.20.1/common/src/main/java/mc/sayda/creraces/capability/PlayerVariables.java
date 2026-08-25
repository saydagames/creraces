package mc.sayda.creraces.capability;

import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of player-specific variables.
 */
public class PlayerVariables implements IPlayerVariables {
    private ResourceLocation race = RaceRegistry.NONE;
    private boolean hasChosenRace = false;
    private double karma = 0.0;
    private double ap = 0.0;
    private double ad = 0.0;
    private double ah = 0.0;
    private double cr = 0.0;
    private double coins = 0.0;
    private double mana = 0.0;
    private double rage = 0.0;
    private double energy = 0.0;
    private double grit = 0.0;
    private double soul = 0.0;
    private double passiveCooldown = 0.0;
    private final Map<ResourceLocation, Integer> cooldowns = new ConcurrentHashMap<>();
    private final Set<ResourceLocation> unlockedAbilities = ConcurrentHashMap.newKeySet();
    private final Map<AbilitySlot, ResourceLocation> equippedAbilities = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Integer> traitTimers = new ConcurrentHashMap<>();
    private final Map<String, String> customizations = new ConcurrentHashMap<>();

    private final Map<ResourceLocation, Double> abilityStates = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Integer> abilityLevels = new ConcurrentHashMap<>();
    private boolean morphed = false;
    private UUID teamId = null;
    private String teamName = "";
    private boolean abilityActive = false;
    private ResourceLocation activeAbility = null;
    private int activeAbilityDuration = 0;
    private double activeAbilityDrain = 0.0;
    private int gState = 0;
    private boolean hasPocket = false;
    private double pocketSize = 0.0;
    private int pocketIndex = 0;
    private final Set<UUID> pocketInvitations = ConcurrentHashMap.newKeySet();
    private double pocketX = 0.0;
    private double pocketY = 0.0;
    private double pocketZ = 0.0;
    private double pocketSpawnX = 0.0;
    private double pocketSpawnY = 0.0;
    private double pocketSpawnZ = 0.0;
    private double returnX = 0.0;
    private double returnY = 0.0;
    private double returnZ = 0.0;
    private String returnDim = "minecraft:overworld";
    private boolean isInSpiritRealm = false;
    private boolean smallBuild = false;
    private boolean isSpirit = false;
    private boolean isTiny = false;
    private boolean isAquatic = false;
    private boolean isUndead = false;
    private long resourceTimer = 0;
    private final Map<UUID, mc.sayda.creraces.engine.ManagedModifier> managedModifiers = new ConcurrentHashMap<>();
    private final Set<ResourceLocation> persistentStateIds = ConcurrentHashMap.newKeySet();
    @Override
    public ResourceLocation getRace() {
        return race;
    }

    @Override
    public void setRace(ResourceLocation race) {
        if (race == null) {
            mc.sayda.creraces.CreRaces.LOGGER.warn("setRace() called with null; ignoring");
            return;
        }
        if (!Objects.equals(this.race, race)) {
            this.race = race;
        }
    }

    @Override
    public boolean hasChosenRace() {
        return hasChosenRace;
    }

    @Override
    public void setHasChosenRace(boolean hasChosen) {
        this.hasChosenRace = hasChosen;
    }

    @Override
    public double getKarma() {
        return karma;
    }

    @Override
    public void setKarma(double karma) {
        this.karma = karma;
    }

    @Override
    public double getAp() {
        return ap;
    }

    @Override
    public void setAp(double ap) {
        this.ap = ap;
    }

    @Override
    public double getAd() {
        return ad;
    }

    @Override
    public double getAh() {
        return ah;
    }

    @Override
    public double getCr() {
        return cr;
    }

    @Override
    public void setAd(double ad) {
        this.ad = ad;
    }

    @Override
    public void setAh(double ah) {
        this.ah = ah;
    }

    @Override
    public void setCr(double cr) {
        this.cr = cr;
    }

    @Override
    public double getCoins() {
        return coins;
    }

    @Override
    public void setCoins(double coins) {
        this.coins = coins;
    }

    @Override
    public double getMana() {
        return mana;
    }

    @Override
    public void setMana(double mana) {
        this.mana = mana;
    }

    @Override
    public double getRage() {
        return rage;
    }

    @Override
    public void setRage(double rage) {
        this.rage = rage;
    }

    @Override
    public double getEnergy() {
        return energy;
    }

    @Override
    public void setEnergy(double energy) {
        this.energy = energy;
    }

    @Override
    public double getGrit() {
        return grit;
    }

    @Override
    public void setGrit(double grit) {
        this.grit = grit;
    }

    @Override
    public double getSoul() {
        return soul;
    }

    @Override
    public void setSoul(double soul) {
        this.soul = soul;
    }

    @Override
    public long getResourceTimer() {
        return resourceTimer;
    }

    @Override
    public void setResourceTimer(long ticks) {
        this.resourceTimer = ticks;
    }

    // resourceTimer is retained for save-file compatibility; only omitted from delta syncs (see serialize(boolean) below), not full ones.

    @Override
    public double getPassiveCooldown() {
        return passiveCooldown;
    }

    @Override
    public void setPassiveCooldown(double ticks) {
        this.passiveCooldown = ticks;
    }

    @Override
    public Map<ResourceLocation, Integer> getCooldowns() {
        return cooldowns;
    }

    @Override
    public void setCooldown(ResourceLocation abilityId, int ticks) {
        if (ticks <= 0)
            cooldowns.remove(abilityId);
        else
            cooldowns.put(abilityId, ticks);
    }

    @Override
    public int getCooldown(ResourceLocation abilityId) {
        return cooldowns.getOrDefault(abilityId, 0);
    }

    @Override
    public void sakuyaTimeLeap() {
        if (passiveCooldown > 0)
            passiveCooldown--;

        for (ResourceLocation id : new HashSet<>(cooldowns.keySet())) {
            cooldowns.computeIfPresent(id, (k, v) -> {
                int next = v - 1;
                return next <= 0 ? null : next;
            });
        }
    }

    @Override
    public Set<ResourceLocation> getUnlockedAbilities() {
        return unlockedAbilities;
    }

    @Override
    public void unlockAbility(ResourceLocation abilityId) {
        unlockedAbilities.add(abilityId);
        if (!abilityLevels.containsKey(abilityId)) {
            abilityLevels.put(abilityId, 1);
        }
    }

    @Override
    public void revokeAbility(ResourceLocation abilityId) {
        unlockedAbilities.remove(abilityId);
        // Also remove from equipped slots
        equippedAbilities.entrySet().removeIf(entry -> entry.getValue().equals(abilityId));
    }

    @Override
    public boolean isAbilityUnlocked(ResourceLocation abilityId) {
        return unlockedAbilities.contains(abilityId);
    }

    @Override
    public int getAbilityLevel(ResourceLocation abilityId) {
        return abilityLevels.getOrDefault(abilityId, 1);
    }

    @Override
    public void setAbilityLevel(ResourceLocation abilityId, int level) {
        if (abilityId == null)
            return;
        abilityLevels.put(abilityId, Math.max(1, level));
    }

    @Override
    public Map<AbilitySlot, ResourceLocation> getEquippedAbilities() {
        return equippedAbilities;
    }

    @Override
    public void equipAbility(AbilitySlot slot, ResourceLocation abilityId) {
        if (abilityId == null) {
            equippedAbilities.remove(slot);
        } else {
            equippedAbilities.put(slot, abilityId);
        }
    }

    @Override
    public ResourceLocation getAbilityInSlot(AbilitySlot slot) {
        return equippedAbilities.get(slot);
    }

    @Override
    public void fantasySealReset() {
        this.race = RaceRegistry.NONE;
        this.hasChosenRace = false;
        this.karma = 0;
        this.ap = 0;
        this.ad = 0;
        this.ah = 0;
        this.cr = 0;
        // Preservation: coins are NOT reset
        this.mana = 0;
        this.rage = 0;
        this.energy = 0;
        this.grit = 0;
        this.soul = 0;
        this.passiveCooldown = 0;
        this.cooldowns.clear();
        this.unlockedAbilities.clear();
        this.equippedAbilities.clear();
        this.customizations.clear();
        this.abilityStates.clear();
        this.persistentStateIds.clear();
        this.abilityLevels.clear();
        this.traitTimers.clear();
        this.morphed = false;
        this.teamId = null;
        this.teamName = "";
        this.gState = 0;
        this.hasPocket = false;
        this.pocketX = 0;
        this.pocketY = 0;
        this.pocketZ = 0;
        this.pocketSpawnX = 0;
        this.pocketSpawnY = 0;
        this.pocketSpawnZ = 0;
        this.pocketIndex = 0;
        this.pocketSize = 0;
        this.pocketInvitations.clear();
        this.managedModifiers.clear();
        this.returnX = 0;
        this.returnY = 0;
        this.returnZ = 0;
        this.returnDim = "minecraft:overworld";
        this.isInSpiritRealm = false;
        this.isSpirit = false;
        this.isTiny = false;
        this.isAquatic = false;
        this.isUndead = false;
        this.smallBuild = false;
        this.abilityActive = false;
        this.activeAbility = null;
        this.activeAbilityDuration = 0;
        this.activeAbilityDrain = 0;
    }

    @Override
    public void sync(net.minecraft.world.entity.player.Player player) {
        // Overridden by PlayerMixin; this body is never reached via DataUtils.getVariables().
    }

    @Override
    public void resetOnDeath() {
        this.mana = 0;
        this.rage = 0;
        this.energy = 0;
        this.grit = 0;
        this.passiveCooldown = 0;

        // Clear non-persistent cooldowns
        this.cooldowns.entrySet().removeIf(entry -> {
            mc.sayda.creraces.ability.Ability ability = mc.sayda.creraces.ability.AbilityRegistry.get(entry.getKey());
            return ability == null || !ability.persistent();
        });

        // Clear non-persistent ability states
        abilityStates.entrySet().removeIf(entry -> {
            ResourceLocation id = entry.getKey();
            if (persistentStateIds.contains(id))
                return false;
            mc.sayda.creraces.ability.Ability ability = mc.sayda.creraces.ability.AbilityRegistry.get(id);
            return ability == null || !ability.persistent();
        });

        // Clear trait timers on death (transient state)
        this.traitTimers.clear();

        this.abilityActive = false;
        this.activeAbility = null;
        this.activeAbilityDuration = 0;
        this.activeAbilityDrain = 0;

        this.isInSpiritRealm = false;
    }

    @Override
    public Map<String, String> getCustomizations() {
        return customizations;
    }

    @Override
    public void setCustomization(String key, String value) {
        if (value == null)
            customizations.remove(key);
        else
            customizations.put(key, value);
    }

    @Override
    public String getCustomization(String key) {
        return customizations.get(key);
    }

    @Override
    public double getPersistentState(ResourceLocation id) {
        return abilityStates.getOrDefault(id, 0.0);
    }

    @Override
    public void setPersistentState(ResourceLocation id, double value) {
        if (id == null)
            return;
        if (value == 0)
            abilityStates.remove(id);
        else
            abilityStates.put(id, value);
    }

    @Override
    public void setStatePersistent(ResourceLocation id, boolean persistent) {
        if (id == null)
            return;
        if (persistent)
            persistentStateIds.add(id);
        else
            persistentStateIds.remove(id);
    }

    @Override
    public boolean isStatePersistent(ResourceLocation id) {
        return persistentStateIds.contains(id);
    }

    @Override
    public AbilitySlot getSlotForAbility(ResourceLocation abilityId) {
        for (Map.Entry<AbilitySlot, ResourceLocation> entry : equippedAbilities.entrySet()) {
            if (entry.getValue().equals(abilityId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public boolean isMorphed() {
        return morphed;
    }

    @Override
    public void setMorphed(boolean morphed) {
        this.morphed = morphed;
    }

    @Override
    public UUID getTeamId() {
        return teamId;
    }

    @Override
    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    @Override
    public String getTeamName() {
        return teamName;
    }

    @Override
    public void setTeamName(String teamName) {
        this.teamName = teamName != null ? teamName : "";
    }

    @Override
    public int getGState() {
        return gState;
    }

    @Override
    public void setGState(int state) {
        this.gState = state;
    }

    @Override
    public boolean hasPocket() {
        return hasPocket;
    }

    @Override
    public void setHasPocket(boolean hasPocket) {
        this.hasPocket = hasPocket;
    }

    @Override
    public double getPocketSize() {
        return pocketSize;
    }

    @Override
    public void setPocketSize(double size) {
        this.pocketSize = size;
    }

    @Override
    public int getPocketIndex() {
        return pocketIndex;
    }

    @Override
    public void setPocketIndex(int index) {
        this.pocketIndex = index;
    }

    @Override
    public Set<UUID> getPocketInvitations() {
        return pocketInvitations;
    }

    @Override
    public void inviteToPocket(UUID uuid) {
        pocketInvitations.add(uuid);
    }

    @Override
    public void revokePocketInvitation(UUID uuid) {
        pocketInvitations.remove(uuid);
    }

    @Override
    public double getPocketX() {
        return pocketX;
    }

    @Override
    public void setPocketX(double x) {
        this.pocketX = x;
    }

    @Override
    public double getPocketY() {
        return pocketY;
    }

    @Override
    public void setPocketY(double y) {
        this.pocketY = y;
    }

    @Override
    public double getPocketZ() {
        return pocketZ;
    }

    @Override
    public void setPocketZ(double z) {
        this.pocketZ = z;
    }

    @Override
    public double getPocketSpawnX() {
        return pocketSpawnX;
    }

    @Override
    public void setPocketSpawnX(double x) {
        this.pocketSpawnX = x;
    }

    @Override
    public double getPocketSpawnY() {
        return pocketSpawnY;
    }

    @Override
    public void setPocketSpawnY(double y) {
        this.pocketSpawnY = y;
    }

    @Override
    public double getPocketSpawnZ() {
        return pocketSpawnZ;
    }

    @Override
    public void setPocketSpawnZ(double z) {
        this.pocketSpawnZ = z;
    }

    @Override
    public double getReturnX() {
        return returnX;
    }

    @Override
    public void setReturnX(double x) {
        this.returnX = x;
    }

    @Override
    public double getReturnY() {
        return returnY;
    }

    @Override
    public void setReturnY(double y) {
        this.returnY = y;
    }

    @Override
    public double getReturnZ() {
        return returnZ;
    }

    @Override
    public void setReturnZ(double z) {
        this.returnZ = z;
    }

    @Override
    public String getReturnDim() {
        return returnDim;
    }

    @Override
    public void setReturnDim(String dim) {
        this.returnDim = dim != null ? dim : "minecraft:overworld";
    }

    @Override
    public boolean isInSpiritRealm() {
        return isInSpiritRealm;
    }

    @Override
    public void setInSpiritRealm(boolean inSpiritRealm) {
        this.isInSpiritRealm = inSpiritRealm;
    }

    @Override
    public boolean isAbilityActive() {
        return this.abilityActive;
    }

    @Override
    public void setAbilityActive(boolean active) {
        this.abilityActive = active;
    }

    @Override
    public ResourceLocation getActiveAbility() {
        return this.activeAbility;
    }

    @Override
    public void setActiveAbility(ResourceLocation abilityId) {
        this.activeAbility = abilityId;
    }

    @Override
    public int getActiveAbilityDuration() {
        return this.activeAbilityDuration;
    }

    @Override
    public void setActiveAbilityDuration(int ticks) {
        this.activeAbilityDuration = ticks;
    }

    @Override
    public double getActiveAbilityDrain() {
        return this.activeAbilityDrain;
    }

    @Override
    public void setActiveAbilityDrain(double drain) {
        this.activeAbilityDrain = drain;
    }

    @Override
    public Map<ResourceLocation, Integer> getTraitTimers() {
        return traitTimers;
    }

    @Override
    public void setTraitTimer(ResourceLocation id, int ticks) {
        if (ticks <= 0)
            traitTimers.remove(id);
        else
            traitTimers.put(id, ticks);
    }

    @Override
    public boolean isSmallBuild() {

        return smallBuild;
    }

    @Override
    public void setSmallBuild(boolean smallBuild) {
        if (this.smallBuild != smallBuild) {
            this.smallBuild = smallBuild;
        }
    }

    @Override
    public boolean isUndead() {
        return isUndead;
    }

    @Override
    public void setUndead(boolean undead) {
        this.isUndead = undead;
    }

    @Override
    public boolean isAquatic() {
        return isAquatic;
    }

    @Override
    public void setAquatic(boolean aquatic) {
        this.isAquatic = aquatic;
    }

    @Override
    public boolean isSpirit() {
        return isSpirit;
    }

    @Override
    public void setSpirit(boolean spirit) {
        this.isSpirit = spirit;
    }

    @Override
    public boolean isTiny() {
        return isTiny;
    }

    @Override
    public void setTiny(boolean tiny) {
        this.isTiny = tiny;
    }

    @Override
    @SuppressWarnings("null")
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("race", Objects.requireNonNull(race.toString()));
        tag.putBoolean("hasChosenRace", hasChosenRace);
        tag.putDouble("karma", karma);
        tag.putDouble("coins", coins);
        tag.putDouble("mana", mana);
        tag.putDouble("rage", rage);
        tag.putDouble("energy", energy);
        tag.putDouble("grit", grit);
        tag.putDouble("soul", soul);
        tag.putLong("resourceTimer", resourceTimer);
        tag.putDouble("passiveCooldown", passiveCooldown);

        // Cooldowns ARE included in every sync so the HUD overlay counts down
        // correctly.
        CompoundTag cooldownsTag = new CompoundTag();
        cooldowns.forEach((id, val) -> cooldownsTag.putInt(Objects.requireNonNull(id.toString()), val));
        tag.put("cooldowns", cooldownsTag);

        ListTag unlockedList = new ListTag();
        unlockedAbilities.forEach(
                id -> unlockedList.add(net.minecraft.nbt.StringTag.valueOf(Objects.requireNonNull(id.toString()))));
        tag.put("unlockedAbilities", unlockedList);

        CompoundTag equippedTag = new CompoundTag();
        equippedAbilities.forEach((slot, id) -> equippedTag.putString(Objects.requireNonNull(slot.name()),
                Objects.requireNonNull(id.toString())));
        tag.put("equippedAbilities", equippedTag);

        CompoundTag custTag = new CompoundTag();
        customizations.forEach(custTag::putString);
        tag.put("customizations", custTag);
        CompoundTag statesTag = new CompoundTag();
        abilityStates.forEach((id, val) -> statesTag.putDouble(Objects.requireNonNull(id.toString()), val));
        tag.put("abilityStates", statesTag);

        ListTag persistentList = new ListTag();
        persistentStateIds.forEach(
                id -> persistentList.add(net.minecraft.nbt.StringTag.valueOf(Objects.requireNonNull(id.toString()))));
        tag.put("persistentStateIds", persistentList);

        CompoundTag levelsTag = new CompoundTag();
        abilityLevels.forEach((id, val) -> levelsTag.putInt(Objects.requireNonNull(id.toString()), val));
        tag.put("abilityLevels", levelsTag);

        CompoundTag traitTimersTag = new CompoundTag();
        traitTimers.forEach((id, val) -> traitTimersTag.putInt(Objects.requireNonNull(id.toString()), val));
        tag.put("traitTimers", traitTimersTag);

        tag.putBoolean("morphed", morphed);

        if (teamId != null) {
            tag.putUUID("teamId", teamId);
        }
        tag.putString("teamName", Objects.requireNonNull(teamName));
        tag.putInt("gState", gState);
        tag.putBoolean("hasPocket", hasPocket);
        tag.putDouble("pocketSize", pocketSize);
        tag.putDouble("pocketX", pocketX);
        tag.putDouble("pocketY", pocketY);
        tag.putDouble("pocketZ", pocketZ);
        tag.putDouble("pocketSpawnX", pocketSpawnX);
        tag.putDouble("pocketSpawnY", pocketSpawnY);
        tag.putDouble("pocketSpawnZ", pocketSpawnZ);
        tag.putDouble("returnX", returnX);
        tag.putDouble("returnY", returnY);
        tag.putDouble("returnZ", returnZ);
        tag.putString("returnDim", Objects.requireNonNull(returnDim));
        tag.putInt("pocketIndex", pocketIndex);
        ListTag invitationsList = new ListTag();
        pocketInvitations.forEach(uuid -> invitationsList.add(net.minecraft.nbt.StringTag.valueOf(uuid.toString())));
        tag.put("pocketInvitations", invitationsList);
        tag.putBoolean("isInSpiritRealm", isInSpiritRealm);
        tag.putBoolean("smallBuild", smallBuild);
        tag.putBoolean("isSpirit", isSpirit);
        tag.putBoolean("isTiny", isTiny);
        tag.putBoolean("isAquatic", isAquatic);
        tag.putBoolean("isUndead", isUndead);
        tag.putBoolean("abilityActive", abilityActive);
        if (activeAbility != null) {
            tag.putString("activeAbility", activeAbility.toString());
        }
        tag.putInt("activeAbilityDuration", activeAbilityDuration);
        tag.putDouble("activeAbilityDrain", activeAbilityDrain);

        if (!managedModifiers.isEmpty()) {
            ListTag managedList = new ListTag();
            for (mc.sayda.creraces.engine.ManagedModifier mod : managedModifiers.values()) {
                managedList.add(mod.toNBT());
            }
            tag.put("managedModifiers", managedList);
        }


        return tag;
    }

    @Override
    public CompoundTag serialize(boolean fullSync) {
        if (fullSync) {
            return serialize();
        }

        // TODO: This feels cheep? What if others are added in the future, it would be
        // better if it can obtain them from a list or similar?

        // Delta sync: omit resources (mana, rage, energy, grit, soul, passiveCooldown, resourceTimer).
        // The client predicts these every tick; a full sync fires on all discrete
        // events.
        CompoundTag tag = serialize();
        tag.remove("mana");
        tag.remove("rage");
        tag.remove("energy");
        tag.remove("grit");
        tag.remove("soul");
        tag.remove("passiveCooldown");
        tag.remove("resourceTimer");
        return tag;
    }

    @Override
    @SuppressWarnings("null")
    public void deserialize(CompoundTag tag) {
        if (tag.contains("race")) {
            ResourceLocation parsedRace = ResourceLocation.tryParse(tag.getString("race"));
            if (parsedRace != null)
                this.race = parsedRace;
        }
        if (tag.contains("hasChosenRace"))
            this.hasChosenRace = tag.getBoolean("hasChosenRace");
        if (tag.contains("resourceTimer"))
            this.resourceTimer = tag.getLong("resourceTimer");
        if (tag.contains("karma"))
            this.karma = tag.getDouble("karma");
        if (tag.contains("coins"))
            this.coins = tag.getDouble("coins");
        // Resources - directly assign server-authoritative values.
        // They are only sent on full syncs (join, respawn, cast).
        if (tag.contains("mana"))
            this.mana = tag.getDouble("mana");
        if (tag.contains("rage"))
            this.rage = tag.getDouble("rage");
        if (tag.contains("energy"))
            this.energy = tag.getDouble("energy");
        if (tag.contains("grit"))
            this.grit = tag.getDouble("grit");
        if (tag.contains("soul"))
            this.soul = tag.getDouble("soul");
        if (tag.contains("passiveCooldown"))
            this.passiveCooldown = tag.getDouble("passiveCooldown");

        if (tag.contains("cooldowns", Tag.TAG_COMPOUND)) {
            this.cooldowns.clear();
            CompoundTag cooldownsTag = tag.getCompound("cooldowns");
            for (String key : cooldownsTag.getAllKeys()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id != null)
                    this.cooldowns.put(id, cooldownsTag.getInt(key));
            }
        }

        if (tag.contains("unlockedAbilities", Tag.TAG_LIST)) {
            this.unlockedAbilities.clear();
            ListTag list = tag.getList("unlockedAbilities", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
                if (id != null)
                    this.unlockedAbilities.add(id);
            }
        }

        if (tag.contains("equippedAbilities", Tag.TAG_COMPOUND)) {
            this.equippedAbilities.clear();
            CompoundTag equippedTag = tag.getCompound("equippedAbilities");
            for (AbilitySlot slot : AbilitySlot.values()) {
                if (equippedTag.contains(slot.name())) {
                    ResourceLocation id = ResourceLocation.tryParse(equippedTag.getString(slot.name()));
                    if (id != null)
                        this.equippedAbilities.put(slot, id);
                }
            }
        }

        this.customizations.clear();
        if (tag.contains("customizations", Tag.TAG_COMPOUND)) {
            CompoundTag custTag = tag.getCompound("customizations");
            for (String key : custTag.getAllKeys()) {
                this.customizations.put(key, Objects.requireNonNull(custTag.getString(key)));
            }
        }
        this.abilityStates.clear();
        if (tag.contains("abilityStates", Tag.TAG_COMPOUND)) {
            CompoundTag statesTag = tag.getCompound("abilityStates");
            for (String key : statesTag.getAllKeys()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id != null)
                    this.abilityStates.put(id, statesTag.getDouble(key));
            }
        }

        if (tag.contains("persistentStateIds", Tag.TAG_LIST)) {
            this.persistentStateIds.clear();
            ListTag list = tag.getList("persistentStateIds", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
                if (id != null)
                    this.persistentStateIds.add(id);
            }
        }

        this.traitTimers.clear();
        if (tag.contains("traitTimers", Tag.TAG_COMPOUND)) {
            CompoundTag traitTimersTag = tag.getCompound("traitTimers");
            for (String key : traitTimersTag.getAllKeys()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id != null)
                    this.traitTimers.put(id, traitTimersTag.getInt(key));
            }
        }

        if (tag.contains("morphed"))
            this.morphed = tag.getBoolean("morphed");

        if (tag.contains("teamId")) {
            this.teamId = tag.getUUID("teamId");
        } else {
            this.teamId = null;
        }

        if (tag.contains("teamName")) {
            this.teamName = Objects.requireNonNull(tag.getString("teamName"));
        } else {
            this.teamName = "";
        }
        if (tag.contains("gState"))
            this.gState = tag.getInt("gState");
        if (tag.contains("hasPocket"))
            this.hasPocket = tag.getBoolean("hasPocket");
        if (tag.contains("pocketSize"))
            this.pocketSize = tag.getDouble("pocketSize");
        if (tag.contains("pocketX"))
            this.pocketX = tag.getDouble("pocketX");
        if (tag.contains("pocketY"))
            this.pocketY = tag.getDouble("pocketY");
        if (tag.contains("pocketZ"))
            this.pocketZ = tag.getDouble("pocketZ");
        if (tag.contains("pocketSpawnX"))
            this.pocketSpawnX = tag.getDouble("pocketSpawnX");
        if (tag.contains("pocketSpawnY"))
            this.pocketSpawnY = tag.getDouble("pocketSpawnY");
        if (tag.contains("pocketSpawnZ"))
            this.pocketSpawnZ = tag.getDouble("pocketSpawnZ");
        if (tag.contains("pocketIndex"))
            this.pocketIndex = tag.getInt("pocketIndex");
        if (tag.contains("pocketInvitations", Tag.TAG_LIST)) {
            this.pocketInvitations.clear();
            ListTag list = tag.getList("pocketInvitations", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                try {
                    this.pocketInvitations.add(UUID.fromString(list.getString(i)));
                } catch (Exception ignored) {
                }
            }
        }
        if (tag.contains("returnX"))
            this.returnX = tag.getDouble("returnX");
        if (tag.contains("returnY"))
            this.returnY = tag.getDouble("returnY");
        if (tag.contains("returnZ"))
            this.returnZ = tag.getDouble("returnZ");
        if (tag.contains("returnDim"))
            this.returnDim = Objects.requireNonNull(tag.getString("returnDim"));
        if (tag.contains("isInSpiritRealm"))
            this.isInSpiritRealm = tag.getBoolean("isInSpiritRealm");
        if (tag.contains("smallBuild"))
            this.smallBuild = tag.getBoolean("smallBuild");
        if (tag.contains("isSpirit"))
            this.isSpirit = tag.getBoolean("isSpirit");
        if (tag.contains("isTiny"))
            this.isTiny = tag.getBoolean("isTiny");
        if (tag.contains("isAquatic"))
            this.isAquatic = tag.getBoolean("isAquatic");
        if (tag.contains("isUndead"))
            this.isUndead = tag.getBoolean("isUndead");
        if (tag.contains("abilityActive"))
            this.abilityActive = tag.getBoolean("abilityActive");
        if (tag.contains("activeAbility")) {
            String activeId = tag.getString("activeAbility");
            if (!activeId.isEmpty()) {
                ResourceLocation parsed = ResourceLocation.tryParse(activeId);
                if (parsed != null)
                    this.activeAbility = parsed;
            }
        }
        if (tag.contains("activeAbilityDuration"))
            this.activeAbilityDuration = tag.getInt("activeAbilityDuration");
        if (tag.contains("activeAbilityDrain"))
            this.activeAbilityDrain = tag.getDouble("activeAbilityDrain");

        if (tag.contains("managedModifiers", Tag.TAG_LIST)) {
            this.managedModifiers.clear();
            ListTag list = tag.getList("managedModifiers", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                mc.sayda.creraces.engine.ManagedModifier mod = mc.sayda.creraces.engine.ManagedModifier
                        .fromNBT(list.getCompound(i));
                if (mod != null) this.managedModifiers.put(mod.uuid(), mod);
            }
        }


        if (tag.contains("abilityLevels", Tag.TAG_COMPOUND)) {
            this.abilityLevels.clear();
            CompoundTag levelsTag = tag.getCompound("abilityLevels");
            for (String key : levelsTag.getAllKeys()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id != null)
                    this.abilityLevels.put(id, levelsTag.getInt(key));
            }
        }
    }

    @Override
    public java.util.Collection<mc.sayda.creraces.engine.ManagedModifier> getManagedModifiers() {
        return managedModifiers.values();
    }

    @Override
    public java.util.Optional<mc.sayda.creraces.engine.ManagedModifier> getManagedModifier(UUID uuid) {
        return java.util.Optional.ofNullable(managedModifiers.get(uuid));
    }

    @Override
    public void addManagedModifier(mc.sayda.creraces.engine.ManagedModifier mod) {
        managedModifiers.put(mod.uuid(), mod);
    }

    @Override
    public void removeManagedModifier(UUID uuid) {
        managedModifiers.remove(uuid);
    }

    @Override
    public void clearManagedModifiers() {
        managedModifiers.clear();
    }

}
