package mc.sayda.creraces.territory;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores cosmetic diplomacy relationships for one race.
 * Each race has at most one ClanData; it is created on first use.
 */
public final class ClanData {
    private final ResourceLocation raceId;
    private final Map<ResourceLocation, DiplomacyStatus> relations = new HashMap<>();

    public ClanData(ResourceLocation raceId) {
        this.raceId = raceId;
    }

    public ResourceLocation getRaceId() { return raceId; }

    public DiplomacyStatus getRelation(ResourceLocation other) {
        return relations.getOrDefault(other, DiplomacyStatus.NEUTRAL);
    }

    public void setRelation(ResourceLocation other, DiplomacyStatus status) {
        if (status == DiplomacyStatus.NEUTRAL) {
            relations.remove(other);
        } else {
            relations.put(other, status);
        }
    }

    public Map<ResourceLocation, DiplomacyStatus> getRelations() {
        return Collections.unmodifiableMap(relations);
    }
}
