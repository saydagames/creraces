package mc.sayda.creraces.compat;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.waypoint.WaypointFactory;
import journeymap.api.v2.common.waypoint.WaypointGroup;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.waypoint.Waypoint;
import mc.sayda.creraces.util.PlatformServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Mirrors kitsune gate waypoints into JourneyMap, replacing this mod's HUD markers while JourneyMap
 * is present (see WaypointStore.shouldRenderHud). Only JourneyMap's own annotation scan loads this
 * class; nothing in the mod references it, so it never classloads without JourneyMap installed.
 *
 * Sync is one-way from the mod's client-side store. Waypoints are pushed as non-persistent and
 * diffed against JourneyMap's current set every time, and the group is looked up (or recreated)
 * on each sync rather than cached, because JourneyMap reloads its world data on its own schedule
 * and may drop either. WaypointStore re-runs the sync periodically so this settles on its own.
 */
@JourneyMapPlugin(apiVersion = "2.0.0")
public class CreRacesJourneyMapPlugin implements IClientPlugin {
    private static final String MOD_ID = CreRaces.MODID;
    private static final String GROUP_NAME = "Gate Waypoints";
    private static final String ID_KEY = "creraces_id";
    private static final int STALE_COLOR = 0x888888;
    /** The torii bell item texture, the same art the HUD marker draws. */
    private static final ResourceLocation GATE_ICON = Objects
            .requireNonNull(ResourceLocation.tryParse(MOD_ID + ":textures/item/tori_bell.png"));
    private static final int GATE_ICON_SIZE = 16;

    private IClientAPI api;

    @Override
    public String getModId() {
        return MOD_ID;
    }

    @Override
    public void initialize(IClientAPI jmClientApi) {
        this.api = jmClientApi;
        PlatformServices.journeyMapPresent = () -> true;
        PlatformServices.waypointSink = this::sync;
        CreRaces.LOGGER.info("JourneyMap detected: gate waypoints will sync there instead of drawing their own HUD markers.");
    }

    private void sync(List<Waypoint> waypoints) {
        try {
            syncUnsafe(waypoints);
        } catch (Exception e) {
            // Never let a JourneyMap-side failure reach the caller - WaypointStore invokes this
            // synchronously from the client tick.
            CreRaces.LOGGER.error("Failed to sync gate waypoints to JourneyMap", e);
        }
    }

    private void syncUnsafe(List<Waypoint> waypoints) {
        if (api == null) {
            return;
        }
        WaypointGroup group = resolveGroup();

        // Match by our own id first, then by position, so a waypoint whose custom data was lost
        // is adopted instead of duplicated.
        Map<String, journeymap.api.v2.common.waypoint.Waypoint> byId = new HashMap<>();
        Map<String, journeymap.api.v2.common.waypoint.Waypoint> byPlace = new HashMap<>();
        for (journeymap.api.v2.common.waypoint.Waypoint existing : api.getWaypoints(MOD_ID)) {
            String id = existing.getCustomData(ID_KEY);
            if (id != null) {
                byId.putIfAbsent(id, existing);
            }
            byPlace.putIfAbsent(placeKey(existing.getPrimaryDimension(), existing.getBlockPos()), existing);
        }

        Set<String> keep = new HashSet<>();
        for (Waypoint wp : waypoints) {
            ResourceLocation dimId = ResourceLocation.tryParse(wp.dimension());
            if (dimId == null) {
                continue;
            }
            journeymap.api.v2.common.waypoint.Waypoint jmWp = byId.get(wp.id().toString());
            if (jmWp == null) {
                jmWp = byPlace.get(placeKey(wp.dimension(), wp.pos()));
            }

            boolean created = jmWp == null;
            if (created) {
                jmWp = WaypointFactory.createWaypoint(MOD_ID, wp.pos(), wp.name(),
                        ResourceKey.create(Registries.DIMENSION, dimId), false);
                // Misspelled in the JourneyMap API itself; 1.20.1's API calls it setIconIdentifier
                jmWp.setIconResourceLoctaion(GATE_ICON);
                jmWp.setIconTextureSize(GATE_ICON_SIZE, GATE_ICON_SIZE);
            }
            boolean changed = apply(jmWp, wp);
            if (created || changed) {
                api.addWaypoint(MOD_ID, jmWp);
            }
            if (!group.getGuid().equals(jmWp.getGroupId()) && !group.addWaypoint(jmWp)) {
                CreRaces.LOGGER.debug("JourneyMap refused to move gate '{}' into '{}'", wp.name(), GROUP_NAME);
            }
            keep.add(jmWp.getGuid());
        }

        for (journeymap.api.v2.common.waypoint.Waypoint existing : api.getWaypoints(MOD_ID)) {
            if (!keep.contains(existing.getGuid())) {
                api.removeWaypoint(MOD_ID, existing);
            }
        }
    }

    /**
     * Brings one JourneyMap waypoint in line with the gate it mirrors. Returns whether anything
     * changed, so an unchanged waypoint is not re-saved on every periodic sync.
     */
    private static boolean apply(journeymap.api.v2.common.waypoint.Waypoint jmWp, Waypoint wp) {
        boolean stale = wp.isStale();
        String name = stale ? wp.name() + " (gone)" : wp.name();
        int rgb = stale ? STALE_COLOR : wp.color() & 0xFFFFFF;
        boolean changed = false;

        if (!name.equals(jmWp.getName())) {
            jmWp.setName(name);
            changed = true;
        }
        if ((jmWp.getColor() & 0xFFFFFF) != rgb) {
            jmWp.setColor(rgb);
            changed = true;
        }
        Integer iconColor = jmWp.getIconColor();
        if (iconColor == null || (iconColor & 0xFFFFFF) != rgb) {
            jmWp.setIconColor(rgb);
            changed = true;
        }
        String id = wp.id().toString();
        if (!id.equals(jmWp.getCustomData(ID_KEY))) {
            jmWp.setCustomData(ID_KEY, id);
            changed = true;
        }
        return changed;
    }

    /** Looks the group up fresh every time; recreates it if JourneyMap no longer knows it. */
    private WaypointGroup resolveGroup() {
        WaypointGroup found = api.getWaypointGroupByName(MOD_ID, GROUP_NAME);
        if (found != null) {
            return found;
        }
        WaypointGroup created = WaypointFactory.createWaypointGroup(MOD_ID, GROUP_NAME);
        created.setPersistent(true);
        api.addWaypointGroup(created);
        return created;
    }

    private static String placeKey(String dimension, BlockPos pos) {
        return dimension + "@" + pos.asLong();
    }
}
