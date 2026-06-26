package mc.sayda.creraces.territory;

import java.util.UUID;

public final class PlayerActivityData {
    private final UUID playerUUID;
    private long lastSeenMs;

    public PlayerActivityData(UUID playerUUID, long lastSeenMs) {
        this.playerUUID = playerUUID;
        this.lastSeenMs = lastSeenMs;
    }

    public UUID getPlayerUUID()        { return playerUUID; }
    public long getLastSeenMs()        { return lastSeenMs; }
    public void setLastSeenMs(long ms) { lastSeenMs = ms; }
}
