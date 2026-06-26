package mc.sayda.creraces.territory;

import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class ClaimData {
    private final long chunkKey;
    private final UUID factionId;
    private final ResourceLocation raceId;
    private boolean dormant;
    private boolean persistent;
    private UUID ownerUUID; // individual player who claimed this chunk; null for legacy data

    public ClaimData(long chunkKey, UUID factionId, ResourceLocation raceId, boolean dormant) {
        this(chunkKey, factionId, raceId, dormant, false, null);
    }

    public ClaimData(long chunkKey, UUID factionId, ResourceLocation raceId, boolean dormant, boolean persistent) {
        this(chunkKey, factionId, raceId, dormant, persistent, null);
    }

    public ClaimData(long chunkKey, UUID factionId, ResourceLocation raceId, boolean dormant, boolean persistent, UUID ownerUUID) {
        this.chunkKey = chunkKey;
        this.factionId = factionId;
        this.raceId = raceId;
        this.dormant = dormant;
        this.persistent = persistent;
        this.ownerUUID = ownerUUID;
    }

    public long getChunkKey()          { return chunkKey; }
    public UUID getFactionId()         { return factionId; }
    public ResourceLocation getRaceId(){ return raceId; }
    public boolean isDormant()         { return dormant; }
    public void setDormant(boolean v)  { dormant = v; }
    public boolean isPersistent()      { return persistent; }
    public UUID getOwnerUUID()         { return ownerUUID; }
    public void setOwnerUUID(UUID uuid){ ownerUUID = uuid; }
}
