package mc.sayda.creraces.territory;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

public final class FactionData {
    private final UUID id;
    private ResourceLocation raceId;
    private String name;
    private UUID clanId; // nullable

    private final Map<UUID, FactionRank> members    = new LinkedHashMap<>();
    private final Set<ResourceLocation>  habitableBiomes = new HashSet<>();
    private final Set<Long>              claimedChunks   = new HashSet<>();
    // Settings map — values may be locked by parent clan
    private final Map<String, String>    settings    = new HashMap<>();

    public FactionData(UUID id, ResourceLocation raceId, String name) {
        this.id     = id;
        this.raceId = raceId;
        this.name   = name;
    }

    public UUID            getId()                          { return id; }
    public ResourceLocation getRaceId()                    { return raceId; }
    public String          getName()                        { return name; }
    public void            setName(String name)             { this.name = name; }
    public UUID            getClanId()                      { return clanId; }
    public void            setClanId(UUID clanId)           { this.clanId = clanId; }

    public Map<UUID, FactionRank> getMembers()              { return members; }
    public Set<ResourceLocation>  getHabitableBiomes()      { return habitableBiomes; }
    public Set<Long>               getClaimedChunks()       { return claimedChunks; }
    public Map<String, String>     getSettings()            { return settings; }

    public boolean hasMember(UUID player)                   { return members.containsKey(player); }
    public FactionRank getRank(UUID player)                 { return members.getOrDefault(player, null); }

    /** Returns the leader UUID, or null if no leader is set (should never happen in normal operation). */
    public UUID getLeader() {
        for (Map.Entry<UUID, FactionRank> e : members.entrySet()) {
            if (e.getValue() == FactionRank.LEADER) return e.getKey();
        }
        return null;
    }
}
