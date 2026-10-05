package mc.sayda.creraces.client.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.network.SharedGate;
import mc.sayda.creraces.registry.ModBlocks;
import mc.sayda.creraces.util.PlatformServices;
import mc.sayda.creraces.util.RaceUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import javax.annotation.Nullable;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Client-only, per-world list of gates a kitsune has travelled through. Never touches the
 * server. Whenever the list or the visibility toggle changes, the current state is pushed to
 * {@link PlatformServices#waypointSink} so an optional JourneyMap plugin can mirror it.
 */
public final class WaypointStore {
    private static final WaypointStore INSTANCE = new WaypointStore();
    private static final double MIN_MARKER_SCALE = 0.25;
    private static final double MAX_MARKER_SCALE = 3.0;
    private static final int STALE_CHECK_INTERVAL = 20;
    /** How often the sink is re-run while JourneyMap is present, so it settles after JourneyMap loads its own data. */
    private static final int SINK_RESYNC_INTERVAL = 100;

    /** Gates another kitsune shared as one offer, awaiting accept/decline. Not persisted to disk. */
    public record PendingOffer(String senderName, List<SharedGate> gates) {
    }

    private final List<Waypoint> waypoints = new ArrayList<>();
    private final List<PendingOffer> pendingOffers = new ArrayList<>();
    private boolean markersEnabled = true;
    private double markerScale = 1.0;
    private double arrowScale = 1.0;
    private String worldId;
    private boolean lastPushWasKitsune;
    private int staleCheckCountdown;
    private int sinkResyncCountdown;

    private WaypointStore() {
    }

    public static WaypointStore get() {
        return INSTANCE;
    }

    /** Reloads the list for whichever world/server the client just joined. */
    public void onWorldJoin() {
        this.worldId = currentWorldId();
        this.waypoints.clear();
        // A pending offer's dimension/position are only meaningful in the world they arrived in.
        this.pendingOffers.clear();
        load();
        pushToSink(null);
    }

    public void addPendingOffer(String senderName, List<SharedGate> gates) {
        pendingOffers.add(new PendingOffer(senderName, gates));
    }

    public List<PendingOffer> pendingOffers() {
        return Collections.unmodifiableList(pendingOffers);
    }

    /** Adds every gate in the offer that is not already known, saving and syncing once. */
    public void acceptOffer(PendingOffer offer) {
        if (!pendingOffers.remove(offer)) {
            return;
        }
        boolean added = false;
        for (SharedGate gate : offer.gates()) {
            if (!hasGate(gate.dimension(), gate.pos())) {
                waypoints.add(new Waypoint(UUID.randomUUID(), gate.name(), gate.dimension(), gate.pos(),
                        Waypoint.DEFAULT_ICON, Waypoint.DEFAULT_COLOR));
                added = true;
            }
        }
        if (added) {
            save();
            pushToSink(Minecraft.getInstance().player);
        }
    }

    public void declineOffer(PendingOffer offer) {
        pendingOffers.remove(offer);
    }

    /**
     * Checked once per client tick: a mid-session race change re-evaluates who sees the sink, and
     * about once a second every gate is compared against the part of the world that is loaded. With
     * JourneyMap present the sink is also re-run every few seconds.
     */
    public void tick(@Nullable Player player) {
        boolean isKitsune = player != null && RaceUtils.isKitsune(player);
        if (isKitsune != lastPushWasKitsune) {
            pushToSink(player);
        }
        if (player != null && --staleCheckCountdown <= 0) {
            staleCheckCountdown = STALE_CHECK_INTERVAL;
            refreshStale(player.level());
        }
        if (player != null && PlatformServices.journeyMapPresent.getAsBoolean() && --sinkResyncCountdown <= 0) {
            sinkResyncCountdown = SINK_RESYNC_INTERVAL;
            pushToSink(player);
        }
    }

    /**
     * Marks a gate stale when its chunk is loaded on the client and the block there is no longer a
     * torii bell, and clears the mark if the bell is back. A gate whose chunk is not loaded keeps
     * whatever it was last seen as, so a far gate is never guessed at.
     */
    private void refreshStale(Level level) {
        String dimension = level.dimension().location().toString();
        boolean changed = false;
        for (Waypoint wp : waypoints) {
            if (!wp.dimension().equals(dimension) || !level.isLoaded(wp.pos())) {
                continue;
            }
            boolean gone = !level.getBlockState(wp.pos()).is(ModBlocks.TORII_BELL.get());
            if (wp.isStale() != gone) {
                wp.setStale(gone);
                changed = true;
            }
        }
        if (changed) {
            save();
            pushToSink(Minecraft.getInstance().player);
        }
    }

    public List<Waypoint> all() {
        return Collections.unmodifiableList(waypoints);
    }

    public boolean hasGate(String dimension, BlockPos pos) {
        for (Waypoint wp : waypoints) {
            if (wp.sameGate(dimension, pos)) {
                return true;
            }
        }
        return false;
    }

    public void add(Waypoint waypoint) {
        waypoints.add(waypoint);
        save();
        pushToSink(Minecraft.getInstance().player);
    }

    /** Returns the gate at this position, discovering it first (with a default name) if unknown. */
    public Waypoint discoverIfNew(String dimension, BlockPos pos) {
        for (Waypoint wp : waypoints) {
            if (wp.sameGate(dimension, pos)) {
                return wp;
            }
        }
        Waypoint discovered = Waypoint.discovered(dimension, pos);
        add(discovered);
        return discovered;
    }

    public void remove(UUID id) {
        if (waypoints.removeIf(wp -> wp.id().equals(id))) {
            save();
            pushToSink(Minecraft.getInstance().player);
        }
    }

    /** Call after mutating a Waypoint in place (rename, recolour) to persist and re-sync. */
    public void notifyEdited() {
        save();
        pushToSink(Minecraft.getInstance().player);
    }

    public boolean isMarkersEnabled() {
        return markersEnabled;
    }

    public void setMarkersEnabled(boolean enabled) {
        if (this.markersEnabled == enabled) {
            return;
        }
        this.markersEnabled = enabled;
        save();
        pushToSink(Minecraft.getInstance().player);
    }

    /** Multiplier on the on-screen marker size, applied on top of the shrink-with-distance. */
    public double markerScale() {
        return markerScale;
    }

    public void setMarkerScale(double scale) {
        if (Double.isNaN(scale)) {
            return;
        }
        double clamped = clampScale(scale);
        if (clamped == markerScale) {
            return;
        }
        this.markerScale = clamped;
        save();
    }

    /** Multiplier on the off-screen edge arrow, independent of {@link #markerScale()}. */
    public double arrowScale() {
        return arrowScale;
    }

    public void setArrowScale(double scale) {
        if (Double.isNaN(scale)) {
            return;
        }
        double clamped = clampScale(scale);
        if (clamped == arrowScale) {
            return;
        }
        this.arrowScale = clamped;
        save();
    }

    private static double clampScale(double scale) {
        return Math.max(MIN_MARKER_SCALE, Math.min(MAX_MARKER_SCALE, scale));
    }

    /** Whether the HUD renderer should draw markers itself, or defer to JourneyMap. */
    public boolean shouldRenderHud(@Nullable Player player) {
        return markersEnabled && player != null && RaceUtils.isKitsune(player)
                && !PlatformServices.journeyMapPresent.getAsBoolean();
    }

    private void pushToSink(@Nullable Player player) {
        if (player == null) {
            player = Minecraft.getInstance().player;
        }
        boolean isKitsune = player != null && RaceUtils.isKitsune(player);
        lastPushWasKitsune = isKitsune;
        boolean shouldPush = isKitsune && markersEnabled;
        PlatformServices.waypointSink.accept(shouldPush ? List.copyOf(waypoints) : List.of());
    }

    // Waypoints are kept per world: the save folder in singleplayer, the server address otherwise.
    private static String currentWorldId() {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer local = mc.getSingleplayerServer();
        if (local != null) {
            return "sp:" + local.getWorldPath(Objects.requireNonNull(LevelResource.ROOT)).getFileName().toString();
        }
        ServerData server = mc.getCurrentServer();
        if (server != null) {
            return "mp:" + server.ip;
        }
        return "mp:unknown";
    }

    // One file for every world this client has visited, keyed by world id.
    private static Path storePath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config/creraces/waypoints.json");
    }

    private void load() {
        Path path = storePath();
        if (!Files.exists(path)) {
            return;
        }
        try (FileReader r = new FileReader(path.toFile())) {
            JsonObject root = new Gson().fromJson(r, JsonObject.class);
            if (root == null) {
                return;
            }
            if (root.has("markers_enabled")) {
                markersEnabled = root.get("markers_enabled").getAsBoolean();
            }
            if (root.has("marker_scale")) {
                markerScale = clampScale(root.get("marker_scale").getAsDouble());
            }
            if (root.has("arrow_scale")) {
                arrowScale = clampScale(root.get("arrow_scale").getAsDouble());
            }
            JsonObject worlds = root.has("worlds") ? root.getAsJsonObject("worlds") : null;
            if (worlds == null || !worlds.has(worldId)) {
                return;
            }
            for (var element : worlds.getAsJsonArray(worldId)) {
                Waypoint wp = fromJson(element.getAsJsonObject());
                if (wp != null) {
                    waypoints.add(wp);
                }
            }
        } catch (Exception e) {
            CreRaces.LOGGER.warn("Failed to load gate waypoints", e);
        }
    }

    private void save() {
        Path path = storePath();
        JsonObject root = new JsonObject();
        JsonObject worlds = new JsonObject();
        try {
            // Preserve every other world's entries; only this session's world is in memory.
            if (Files.exists(path)) {
                try (FileReader r = new FileReader(path.toFile())) {
                    JsonObject existing = new Gson().fromJson(r, JsonObject.class);
                    if (existing != null && existing.has("worlds")) {
                        worlds = existing.getAsJsonObject("worlds");
                    }
                }
            }
        } catch (Exception e) {
            CreRaces.LOGGER.warn("Failed to read existing gate waypoints before saving", e);
        }

        JsonArray thisWorld = new JsonArray();
        for (Waypoint wp : waypoints) {
            thisWorld.add(toJson(wp));
        }
        if (worldId != null) {
            worlds.add(worldId, thisWorld);
        }

        root.addProperty("markers_enabled", markersEnabled);
        root.addProperty("marker_scale", markerScale);
        root.addProperty("arrow_scale", arrowScale);
        root.add("worlds", worlds);
        try {
            Files.createDirectories(path.getParent());
            try (FileWriter w = new FileWriter(path.toFile())) {
                new GsonBuilder().setPrettyPrinting().create().toJson(root, w);
            }
        } catch (Exception e) {
            CreRaces.LOGGER.warn("Failed to save gate waypoints", e);
        }
    }

    private static JsonObject toJson(Waypoint wp) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", wp.id().toString());
        obj.addProperty("name", wp.name());
        obj.addProperty("dimension", wp.dimension());
        obj.addProperty("x", wp.pos().getX());
        obj.addProperty("y", wp.pos().getY());
        obj.addProperty("z", wp.pos().getZ());
        obj.addProperty("icon", wp.icon().toString());
        obj.addProperty("color", wp.color());
        if (wp.isStale()) {
            obj.addProperty("stale", true);
        }
        return obj;
    }

    @Nullable
    private static Waypoint fromJson(JsonObject obj) {
        try {
            UUID id = UUID.fromString(obj.get("id").getAsString());
            String name = obj.get("name").getAsString();
            String dimension = obj.get("dimension").getAsString();
            BlockPos pos = new BlockPos(obj.get("x").getAsInt(), obj.get("y").getAsInt(), obj.get("z").getAsInt());
            ResourceLocation icon = obj.has("icon")
                    ? ResourceLocation.parse(obj.get("icon").getAsString())
                    : Waypoint.DEFAULT_ICON;
            int color = obj.has("color") ? obj.get("color").getAsInt() : Waypoint.DEFAULT_COLOR;
            Waypoint wp = new Waypoint(id, name, dimension, pos, icon, color);
            wp.setStale(obj.has("stale") && obj.get("stale").getAsBoolean());
            return wp;
        } catch (Exception e) {
            CreRaces.LOGGER.warn("Skipping malformed gate waypoint entry: {}", obj, e);
            return null;
        }
    }
}
