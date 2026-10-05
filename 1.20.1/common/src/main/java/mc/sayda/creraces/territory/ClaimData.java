package mc.sayda.creraces.territory;

import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class ClaimData {
    private final ResourceLocation raceId;
    private final boolean persistent;
    private UUID ownerUUID;
    private final int pricePaid;

    public ClaimData(ResourceLocation raceId, boolean persistent, UUID ownerUUID) {
        this(raceId, persistent, ownerUUID, 0);
    }

    public ClaimData(ResourceLocation raceId, boolean persistent, UUID ownerUUID, int pricePaid) {
        this.raceId     = raceId;
        this.persistent = persistent;
        this.ownerUUID  = ownerUUID;
        this.pricePaid  = pricePaid;
    }

    public ResourceLocation getRaceId()             { return raceId; }
    public boolean          isPersistent()          { return persistent; }
    public UUID             getOwnerUUID()          { return ownerUUID; }
    public int              getPricePaid()          { return pricePaid; }
    public void             setOwnerUUID(UUID uuid) { ownerUUID = uuid; }
}
