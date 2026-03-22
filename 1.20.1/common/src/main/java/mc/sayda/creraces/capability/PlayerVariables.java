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
    private double souls = 0.0;
    private double passiveCooldown = 0.0;
    private double stacks = 0.0;
    private final Map<ResourceLocation, Integer> cooldowns = new ConcurrentHashMap<>();
    private final Set<ResourceLocation> unlockedAbilities = ConcurrentHashMap.newKeySet();
    private final Map<AbilitySlot, ResourceLocation> equippedAbilities = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Integer> traitTimers = new ConcurrentHashMap<>();
    private final Map<String, String> customizations = new ConcurrentHashMap<>();

    private final Map<ResourceLocation, Double> abilityStates = new ConcurrentHashMap<>();
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
    private final Set<UUID> pocketInvitations = new HashSet<>();
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
    private long resourceTimer = 0;

    @Override
    public ResourceLocation getRace() {
        return race;
    }

    @Override
    public void setRace(ResourceLocation race) {
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
    public double getSouls() {
        return souls;
    }

    @Override
    public void setSouls(double souls) {
        this.souls = souls;
    }

    @Override
    public long getResourceTimer() {
        return resourceTimer;
    }

    @Override
    public void setResourceTimer(long ticks) {
        this.resourceTimer = ticks;
    }

    // getResourceTimer / setResourceTimer removed from interface.
    // Field retained only for save-file backward-compat (loaded from disk, never
    // sent over network).

    @Override
    public double getPassiveCooldown() {
        return passiveCooldown;
    }

    @Override
    public void setPassiveCooldown(double ticks) {
        this.passiveCooldown = ticks;
    }

    // getResourceTimer / setResourceTimer removed from interface.
    // Field retained only for save-file backward-compat (loaded from disk, never
    // sent over network).

    @Override
    public double getStacks() {
        return stacks;
    }

    @Override
    public void setStacks(double stacks) {
        this.stacks = stacks;
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
        this.souls = 0;
        this.passiveCooldown = 0;
        this.stacks = 0;
        this.cooldowns.clear();
        this.unlockedAbilities.clear();
        this.equippedAbilities.clear();
        this.customizations.clear();
        this.abilityStates.clear();
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
        this.returnX = 0;
        this.returnY = 0;
        this.returnZ = 0;
        this.returnDim = "minecraft:overworld";
        this.isInSpiritRealm = false;
        this.smallBuild = false;
        this.abilityActive = false;
        this.activeAbility = null;
        this.activeAbilityDuration = 0;
        this.activeAbilityDrain = 0;
    }

    @Override
    public void sync(net.minecraft.world.entity.player.Player player) {
        if (player instanceof net.minecraft.server.level.ServerPlayer) {
            mc.sayda.creraces.network.BoundaryHandler.resyncVariables(player, player);
            mc.sayda.creraces.network.BoundaryHandler.resyncForAllTrackers(player);
        }
    }

    @Override
    public void resetOnDeath() {
        this.mana = 0;
        this.rage = 0;
        this.energy = 0;
        this.grit = 0;
        this.souls = 0;
        this.stacks = 0;
        this.passiveCooldown = 0;

        // Clear non-persistent cooldowns
        this.cooldowns.entrySet().removeIf(entry -> {
            mc.sayda.creraces.ability.Ability ability = mc.sayda.creraces.ability.AbilityRegistry.get(entry.getKey());
            return ability == null || !ability.persistent();
        });

        // Clear non-persistent ability states
        abilityStates.entrySet().removeIf(entry -> {
            mc.sayda.creraces.ability.Ability ability = mc.sayda.creraces.ability.AbilityRegistry.get(entry.getKey());
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
    @SuppressWarnings("null")
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("race", Objects.requireNonNull(race.toString()));
        tag.putBoolean("hasChosenRace", hasChosenRace);
        tag.putDouble("karma", karma);
        tag.putDouble("ap", ap);
        tag.putDouble("ad", ad);
        tag.putDouble("ah", ah);
        tag.putDouble("cr", cr);
        tag.putDouble("coins", coins);
        tag.putDouble("mana", mana);
        tag.putDouble("rage", rage);
        tag.putDouble("energy", energy);
        tag.putDouble("grit", grit);
        tag.putDouble("souls", souls);
        tag.putLong("resourceTimer", resourceTimer);

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
        tag.putBoolean("abilityActive", abilityActive);
        if (activeAbility != null) {
            tag.putString("activeAbility", activeAbility.toString());
        }
        tag.putInt("activeAbilityDuration", activeAbilityDuration);
        tag.putDouble("activeAbilityDrain", activeAbilityDrain);

        return tag;
    }

    @Override
    public CompoundTag serialize(boolean fullSync) {
        if (fullSync) {
            return serialize();
        }

        // TODO: This feels cheep? What if others are added in the future, it would be
        // better if it can obtain them from a list or similar?

        // Delta sync: omit resources (mana, rage, energy, grit, souls, stacks,
        // passiveCooldown).
        // The client predicts these every tick; a full sync fires on all discrete
        // events.
        CompoundTag tag = serialize();
        tag.remove("mana");
        tag.remove("rage");
        tag.remove("energy");
        tag.remove("grit");
        tag.remove("souls");
        tag.remove("stacks");
        tag.remove("passiveCooldown");
        tag.remove("resourceTimer");
        return tag;
    }

    @Override
    @SuppressWarnings("null")
    public void deserialize(CompoundTag tag) {
        if (tag.contains("race"))
            this.race = new ResourceLocation(Objects.requireNonNull(tag.getString("race")));
        if (tag.contains("hasChosenRace"))
            this.hasChosenRace = tag.getBoolean("hasChosenRace");
        if (tag.contains("resourceTimer"))
            this.resourceTimer = tag.getLong("resourceTimer");
        if (tag.contains("karma"))
            this.karma = tag.getDouble("karma");
        if (tag.contains("ap"))
            this.ap = tag.getDouble("ap");
        if (tag.contains("ad"))
            this.ad = tag.getDouble("ad");
        if (tag.contains("ah"))
            this.ah = tag.getDouble("ah");
        if (tag.contains("cr"))
            this.cr = tag.getDouble("cr");
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
        if (tag.contains("souls"))
            this.souls = tag.getDouble("souls");

        if (tag.contains("cooldowns", Tag.TAG_COMPOUND)) {
            this.cooldowns.clear();
            CompoundTag cooldownsTag = tag.getCompound("cooldowns");
            for (String key : cooldownsTag.getAllKeys()) {
                this.cooldowns.put(new ResourceLocation(key), cooldownsTag.getInt(key));
            }
        }

        this.unlockedAbilities.clear();
        if (tag.contains("unlockedAbilities", Tag.TAG_LIST)) {
            ListTag list = tag.getList("unlockedAbilities", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                this.unlockedAbilities.add(new ResourceLocation(Objects.requireNonNull(list.getString(i))));
            }
        }

        this.equippedAbilities.clear();
        if (tag.contains("equippedAbilities", Tag.TAG_COMPOUND)) {
            CompoundTag equippedTag = tag.getCompound("equippedAbilities");
            for (AbilitySlot slot : AbilitySlot.values()) {
                if (equippedTag.contains(slot.name())) {
                    this.equippedAbilities.put(slot,
                            new ResourceLocation(Objects.requireNonNull(equippedTag.getString(slot.name()))));
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
                this.abilityStates.put(new ResourceLocation(key), statesTag.getDouble(key));
            }
        }

        this.traitTimers.clear();
        if (tag.contains("traitTimers", Tag.TAG_COMPOUND)) {
            CompoundTag traitTimersTag = tag.getCompound("traitTimers");
            for (String key : traitTimersTag.getAllKeys()) {
                this.traitTimers.put(new ResourceLocation(key), traitTimersTag.getInt(key));
            }
        }

        if (tag.contains("morphed"))

            this.morphed = tag.getBoolean("morphed");

        if (tag.contains("teamId"))
            this.teamId = tag.getUUID("teamId");
        if (tag.contains("teamName"))
            this.teamName = Objects.requireNonNull(tag.getString("teamName"));
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
        if (tag.contains("abilityActive"))
            this.abilityActive = tag.getBoolean("abilityActive");
        if (tag.contains("activeAbility")) {
            String activeId = tag.getString("activeAbility");
            if (!activeId.isEmpty())
                this.activeAbility = new ResourceLocation(activeId);
        }
        if (tag.contains("activeAbilityDuration"))
            this.activeAbilityDuration = tag.getInt("activeAbilityDuration");
        if (tag.contains("activeAbilityDrain"))
            this.activeAbilityDrain = tag.getDouble("activeAbilityDrain");
    }
}
