package mc.sayda.creraces.territory;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

public final class ClanData {
    private final UUID id;
    private ResourceLocation raceId;
    private UUID leaderId;
    private final Set<UUID>          memberFactionIds = new LinkedHashSet<>();
    // Settings here override / lock the same key in all child factions
    private final Map<String, String> lockedSettings  = new HashMap<>();

    public ClanData(UUID id, ResourceLocation raceId, UUID leaderId) {
        this.id       = id;
        this.raceId   = raceId;
        this.leaderId = leaderId;
    }

    public UUID             getId()               { return id; }
    public ResourceLocation  getRaceId()           { return raceId; }
    public UUID             getLeaderId()          { return leaderId; }
    public void             setLeaderId(UUID id)   { leaderId = id; }
    public Set<UUID>         getMemberFactionIds() { return memberFactionIds; }
    public Map<String,String> getLockedSettings()  { return lockedSettings; }
}
