package mc.sayda.creraces.territory;

import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class ClaimData {
    private final long chunkKey;
    private final ResourceLocation raceId;
    private final boolean persistent;
    private UUID ownerUUID;

    public ClaimData(long chunkKey, ResourceLocation raceId, boolean persistent, UUID ownerUUID) {
        this.chunkKey   = chunkKey;
        this.raceId     = raceId;
        this.persistent = persistent;
        this.ownerUUID  = ownerUUID;
    }

    public long             getChunkKey()           { return chunkKey; }
    public ResourceLocation getRaceId()             { return raceId; }
    public boolean          isPersistent()          { return persistent; }
    public UUID             getOwnerUUID()          { return ownerUUID; }
    public void             setOwnerUUID(UUID uuid) { ownerUUID = uuid; }
}
