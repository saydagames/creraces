package mc.sayda.creraces.client.waypoint;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * A gate a kitsune has personally travelled through. Mutable so the editor screen can update it
 * in place; identity is {@link #id()}. Client-only - never sent to or read from the server.
 */
public class Waypoint {
    // Old Bell, not the current Torii Bell. Its texture is closer to plain grey, so the marker
    // colour tints it cleanly instead of fighting the current bell's own gold/orange paint.
    public static final ResourceLocation DEFAULT_ICON = new ResourceLocation("creraces", "old_bell");
    public static final int DEFAULT_COLOR = 0xFFE0A0FF;

    private final UUID id;
    private String name;
    private final String dimension;
    private final BlockPos pos;
    private final ResourceLocation icon;
    private int color;
    private boolean stale;

    public Waypoint(UUID id, String name, String dimension, BlockPos pos, ResourceLocation icon, int color) {
        this.id = id;
        this.name = name;
        this.dimension = dimension;
        this.pos = pos;
        this.icon = icon;
        this.color = color;
    }

    public static Waypoint discovered(String dimension, BlockPos pos) {
        return new Waypoint(UUID.randomUUID(), defaultName(pos), dimension, pos, DEFAULT_ICON, DEFAULT_COLOR);
    }

    // Resolved once into the stored name, which is a plain editable label from then on.
    private static String defaultName(BlockPos pos) {
        return I18n.get("gui.creraces.waypoint.default_name", pos.getX(), pos.getY(), pos.getZ());
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** The name as the waypoint UIs show it, marked once the gate's bell is gone. */
    public String displayName() {
        return stale ? I18n.get("gui.creraces.waypoint.gone", name) : name;
    }

    public String dimension() {
        return dimension;
    }

    public BlockPos pos() {
        return pos;
    }

    public ResourceLocation icon() {
        return icon;
    }

    public int color() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    /**
     * True once the block at {@link #pos()} has been seen loaded and is no longer a torii bell.
     * Kept across sessions until a bell is seen there again.
     */
    public boolean isStale() {
        return stale;
    }

    public void setStale(boolean stale) {
        this.stale = stale;
    }

    /** Whether this entry refers to the same gate a newly discovered one would (same dimension + block). */
    public boolean sameGate(String dimension, BlockPos pos) {
        return this.dimension.equals(dimension) && this.pos.equals(pos);
    }
}
