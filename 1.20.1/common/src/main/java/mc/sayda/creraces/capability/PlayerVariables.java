package mc.sayda.creraces.capability;

import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Map;
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
    private double resourceTimer = 0.0;
    private double stacks = 0.0;
    private final Map<ResourceLocation, Integer> cooldowns = new ConcurrentHashMap<>();
    private final Set<ResourceLocation> unlockedAbilities = ConcurrentHashMap.newKeySet();
    private final Map<AbilitySlot, ResourceLocation> equippedAbilities = new ConcurrentHashMap<>();
    private final Map<String, String> customizations = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Double> abilityStates = new ConcurrentHashMap<>();
    private boolean morphed = false;
    private UUID teamId = null;
    private String teamName = "";
    private int gState = 0;

    @Override
    public ResourceLocation getRace() {
        return race;
    }

    @Override
    public void setRace(ResourceLocation race) {
        this.race = race;
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
    public double getPassiveCooldown() {
        return passiveCooldown;
    }

    @Override
    public void setPassiveCooldown(double ticks) {
        this.passiveCooldown = ticks;
    }

    @Override
    public double getResourceTimer() {
        return resourceTimer;
    }

    @Override
    public void setResourceTimer(double ticks) {
        this.resourceTimer = ticks;
    }

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
        if (resourceTimer > 0)
            resourceTimer--;

        // slotStates (Ability States) are now exclusively managed by
        // ResourceTicker/Executors
        // to avoid conflicts with toggle values (1, 2) vs timers.

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
    public boolean isAbilityUnlocked(ResourceLocation abilityId) {
        return unlockedAbilities.contains(abilityId);
    }

    @Override
    public Map<AbilitySlot, ResourceLocation> getEquippedAbilities() {
        return equippedAbilities;
    }

    @Override
    public void equipAbility(AbilitySlot slot, ResourceLocation abilityId) {
        if (abilityId == null)
            equippedAbilities.remove(slot);
        else
            equippedAbilities.put(slot, abilityId);
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
        this.coins = 0;
        this.mana = 0;
        this.rage = 0;
        this.energy = 0;
        this.grit = 0;
        this.souls = 0;
        this.passiveCooldown = 0;
        this.resourceTimer = 0;
        this.stacks = 0;
        this.cooldowns.clear();
        this.unlockedAbilities.clear();
        this.equippedAbilities.clear();
        this.customizations.clear();
        this.abilityStates.clear();
        this.morphed = false;
        this.teamId = null;
        this.teamName = "";
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
    public double getAbilityState(ResourceLocation abilityId) {
        if (abilityId == null)
            return 0.0;
        return abilityStates.getOrDefault(abilityId, 0.0);
    }

    @Override
    public void setAbilityState(ResourceLocation abilityId, double value) {
        if (abilityId == null)
            return;
        if (value == 0)
            abilityStates.remove(abilityId);
        else
            abilityStates.put(abilityId, value);
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
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("race", race.toString());
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
        tag.putDouble("passiveCooldown", passiveCooldown);
        tag.putDouble("resourceTimer", resourceTimer);
        tag.putDouble("stacks", stacks);

        ListTag cooldownList = new ListTag();
        cooldowns.forEach((id, ticks) -> {
            CompoundTag c = new CompoundTag();
            c.putString("id", id.toString());
            c.putInt("ticks", ticks);
            cooldownList.add(c);
        });
        tag.put("cooldowns", cooldownList);

        ListTag unlockedList = new ListTag();
        unlockedAbilities.forEach(id -> unlockedList.add(net.minecraft.nbt.StringTag.valueOf(id.toString())));
        tag.put("unlockedAbilities", unlockedList);

        CompoundTag equippedTag = new CompoundTag();
        equippedAbilities.forEach((slot, id) -> equippedTag.putString(slot.name(), id.toString()));
        tag.put("equippedAbilities", equippedTag);

        CompoundTag custTag = new CompoundTag();
        customizations.forEach(custTag::putString);
        tag.put("customizations", custTag);
        CompoundTag statesTag = new CompoundTag();
        abilityStates.forEach((id, val) -> statesTag.putDouble(id.toString(), val));
        tag.put("abilityStates", statesTag);

        tag.putBoolean("morphed", morphed);

        if (teamId != null) {
            tag.putUUID("teamId", teamId);
        }
        tag.putString("teamName", teamName);
        tag.putInt("gState", gState);

        return tag;
    }

    @Override
    public void deserialize(CompoundTag tag) {
        if (tag.contains("race"))
            this.race = new ResourceLocation(tag.getString("race"));
        if (tag.contains("hasChosenRace"))
            this.hasChosenRace = tag.getBoolean("hasChosenRace");
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
        if (tag.contains("passiveCooldown"))
            this.passiveCooldown = tag.getDouble("passiveCooldown");
        if (tag.contains("resourceTimer"))
            this.resourceTimer = tag.getDouble("resourceTimer");
        if (tag.contains("stacks"))
            this.stacks = tag.getDouble("stacks");
        else if (tag.contains("powerStacks")) // Backwards compatibility
            this.stacks = tag.getDouble("powerStacks");

        this.cooldowns.clear();
        if (tag.contains("cooldowns", Tag.TAG_LIST)) {
            ListTag list = tag.getList("cooldowns", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag c = list.getCompound(i);
                this.cooldowns.put(new ResourceLocation(c.getString("id")), c.getInt("ticks"));
            }
        }

        this.unlockedAbilities.clear();
        if (tag.contains("unlockedAbilities", Tag.TAG_LIST)) {
            ListTag list = tag.getList("unlockedAbilities", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                this.unlockedAbilities.add(new ResourceLocation(list.getString(i)));
            }
        }

        this.equippedAbilities.clear();
        if (tag.contains("equippedAbilities", Tag.TAG_COMPOUND)) {
            CompoundTag equippedTag = tag.getCompound("equippedAbilities");
            for (AbilitySlot slot : AbilitySlot.values()) {
                if (equippedTag.contains(slot.name())) {
                    this.equippedAbilities.put(slot, new ResourceLocation(equippedTag.getString(slot.name())));
                }
            }
        }

        this.customizations.clear();
        if (tag.contains("customizations", Tag.TAG_COMPOUND)) {
            CompoundTag custTag = tag.getCompound("customizations");
            for (String key : custTag.getAllKeys()) {
                this.customizations.put(key, custTag.getString(key));
            }
        }
        this.abilityStates.clear();
        if (tag.contains("abilityStates", Tag.TAG_COMPOUND)) {
            CompoundTag statesTag = tag.getCompound("abilityStates");
            for (String key : statesTag.getAllKeys()) {
                this.abilityStates.put(new ResourceLocation(key), statesTag.getDouble(key));
            }
        }
        if (tag.contains("morphed"))
            this.morphed = tag.getBoolean("morphed");

        if (tag.contains("teamId"))
            this.teamId = tag.getUUID("teamId");
        if (tag.contains("teamName"))
            this.teamName = tag.getString("teamName");
        if (tag.contains("gState"))
            this.gState = tag.getInt("gState");
    }
}
