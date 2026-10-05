package mc.sayda.creraces.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.client.AbilityIconRenderer;
import mc.sayda.creraces.client.waypoint.Waypoint;
import mc.sayda.creraces.client.waypoint.WaypointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Draws on-screen markers for the local kitsune's known gates while they are in the spirit
 * realm. Deliberately screen-space, not world-space: screen-edge clamping (an off-screen gate
 * still shows an arrow toward it) has no world-space equivalent, and drawing here means every
 * primitive goes through GuiGraphics rather than raw RenderSystem/Tesselator calls, which is
 * what SpiritRealmRenderer's own HUD hook already warns not to mix (see its screen-tint comment).
 */
public final class WaypointRenderer {
    private static final ResourceLocation ARROW = new ResourceLocation("creraces",
            "textures/gui/waypoint_arrow.png");
    private static final int ICON_SIZE = 16;
    private static final int ARROW_SIZE = 12;
    /** Grey used for a gate that no longer exists (its marker, arrow and label). */
    private static final int STALE_COLOR = 0x888888;
    private static final float EDGE_MARGIN = 20f;
    private static final float MIN_EDGE_MARGIN = 12f;
    /** Icon size holds at its smallest within this range of the player. */
    private static final double NEAR_DISTANCE = 16.0;
    /** Base size of a marker's icon and label, relative to the raw 16px icon and 8px text. */
    private static final float BASE_SCALE = 0.75f;
    /** Extra multiplier on top of BASE_SCALE, icon only; the label doesn't use this. */
    private static final float ICON_BOOST = 0.72f;
    /** Icon size close up, as a fraction of its full size; grows to full size by ICON_GROW_DISTANCE. */
    private static final float NEAR_ICON_SCALE = 0.6f;
    /** Distance at which the icon reaches full size; unchanged beyond it. Purely distance-based -
     *  unlike the label, icon size never reacts to how much of the screen's centre it's near. */
    private static final double ICON_GROW_DISTANCE = 64.0;
    /** Labels finish shrinking well before icons do, so a far gate's name never dwarfs its icon. */
    private static final double TEXT_FAR_DISTANCE = 96.0;
    /** How small a label gets once fully shrunk by distance, as a fraction of its near-distance size. */
    private static final float MIN_TEXT_SCALE = 0.48f;
    /** A label is fully opaque while its marker is within this fraction of the screen's shorter side of the centre... */
    private static final float FOCUS_INNER = 0.07f;
    /** ...and has faded to LABEL_MIN_OPACITY by this fraction, so labels read while looked at and fade when not. */
    private static final float FOCUS_OUTER = 0.30f;
    private static final float LABEL_MIN_OPACITY = 0.0f;
    /** Font draws any alpha under 4 as fully opaque, so a label this faint is skipped instead of drawn. */
    private static final int MIN_DRAWN_ALPHA = 8;
    /** Safety bound on per-frame draw cost only; every known gate in the dimension is normally drawn. */
    private static final int MAX_MARKERS = 64;

    private static Matrix4f viewMatrix;
    private static Matrix4f projMatrix;
    private static boolean matricesValid = false;

    private WaypointRenderer() {
    }

    /**
     * Captures the same view/projection transform BeamRenderer and TetherRenderer already
     * receive at the TAIL of renderLevel, for use later in this frame's HUD pass. Copied rather
     * than referenced - these Matrix4f instances are reused by the caller for other draws later
     * in the same frame.
     */
    public static void captureMatrices(Matrix4f view, Matrix4f projection) {
        viewMatrix = new Matrix4f(view);
        projMatrix = new Matrix4f(projection);
        matricesValid = true;
    }

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui || !matricesValid) {
            return;
        }
        boolean inSpiritRealm = DataUtils.getVariables(player)
                .map(IPlayerVariables::isInSpiritRealm)
                .orElse(false);
        if (!inSpiritRealm || !WaypointStore.get().shouldRenderHud(player)) {
            return;
        }

        String dimension = mc.level.dimension().location().toString();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        List<Candidate> candidates = new ArrayList<>();
        for (Waypoint wp : WaypointStore.get().all()) {
            if (!wp.dimension().equals(dimension)) {
                continue;
            }
            BlockPos pos = wp.pos();
            Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            candidates.add(new Candidate(wp, center, cameraPos.distanceTo(center)));
        }
        candidates.sort((a, b) -> Double.compare(a.distance, b.distance));

        Matrix4f viewProj = new Matrix4f(projMatrix).mul(viewMatrix);
        int guiWidth = mc.getWindow().getGuiScaledWidth();
        int guiHeight = mc.getWindow().getGuiScaledHeight();
        float userScale = (float) WaypointStore.get().markerScale();
        float arrowScale = (float) WaypointStore.get().arrowScale();

        int drawn = 0;
        for (Candidate c : candidates) {
            if (drawn >= MAX_MARKERS) {
                break;
            }
            drawn++;
            drawMarker(graphics, c, viewProj, cameraPos, guiWidth, guiHeight, userScale, arrowScale);
        }
    }

    private static void drawMarker(GuiGraphics graphics, Candidate c, Matrix4f viewProj, Vec3 cameraPos,
            int guiWidth, int guiHeight, float userScale, float arrowScale) {
        float markerMargin = Math.max(MIN_EDGE_MARGIN, EDGE_MARGIN * userScale * BASE_SCALE * ICON_BOOST);
        float arrowMargin = Math.max(MIN_EDGE_MARGIN, EDGE_MARGIN * arrowScale);
        ScreenPoint sp = project(c.worldPos, cameraPos, viewProj, guiWidth, guiHeight, markerMargin, arrowMargin);
        boolean stale = c.waypoint.isStale();

        if (sp.offscreen) {
            drawArrow(graphics, sp.x, sp.y, sp.angle, stale ? STALE_COLOR : c.waypoint.color(), arrowScale);
            return;
        }

        float iconT = (float) Mth.clamp((c.distance - NEAR_DISTANCE) / (ICON_GROW_DISTANCE - NEAR_DISTANCE), 0.0, 1.0);
        float textT = (float) Mth.clamp((c.distance - NEAR_DISTANCE) / (TEXT_FAR_DISTANCE - NEAR_DISTANCE), 0.0, 1.0);
        float iconScale = userScale * BASE_SCALE * ICON_BOOST * (NEAR_ICON_SCALE + iconT * (1.0f - NEAR_ICON_SCALE));
        float textScale = userScale * BASE_SCALE * (1.0f - textT * (1.0f - MIN_TEXT_SCALE));
        int alpha = (int) (lookOpacity(sp, guiWidth, guiHeight) * 255);

        var pose = graphics.pose();
        ResourceLocation icon = stale ? Waypoint.DEFAULT_ICON : c.waypoint.icon();
        int iconColor = stale ? STALE_COLOR : c.waypoint.color();
        pose.pushPose();
        pose.translate(sp.x, sp.y, 0);
        pose.scale(iconScale, iconScale, 1.0f);
        AbilityIconRenderer.render(graphics, icon, -ICON_SIZE / 2, -ICON_SIZE / 2, ICON_SIZE, iconColor);
        pose.popPose();

        if (alpha < MIN_DRAWN_ALPHA) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        String label = c.waypoint.displayName();
        int textColor = (alpha << 24) | (stale ? STALE_COLOR : 0xFFFFFF);
        pose.pushPose();
        pose.translate(sp.x, sp.y + ICON_SIZE * iconScale / 2f + 2f * textScale, 0);
        pose.scale(textScale, textScale, 1.0f);
        graphics.drawCenteredString(font, label, 0, 0, textColor);
        graphics.drawCenteredString(font, formatDistance(c.distance), 0, font.lineHeight, (alpha << 24) | 0xAAAAAA);
        pose.popPose();
    }

    /**
     * 1.0 when the marker sits on the crosshair, easing down to LABEL_MIN_OPACITY as it moves away
     * from the centre of the screen.
     */
    private static float lookOpacity(ScreenPoint sp, int guiWidth, int guiHeight) {
        float reference = Math.min(guiWidth, guiHeight);
        float offCenter = (float) Math.hypot(sp.x - guiWidth / 2f, sp.y - guiHeight / 2f) / reference;
        float t = Mth.clamp((offCenter - FOCUS_INNER) / (FOCUS_OUTER - FOCUS_INNER), 0.0f, 1.0f);
        float eased = t * t * (3.0f - 2.0f * t);
        return 1.0f - eased * (1.0f - LABEL_MIN_OPACITY);
    }

    private static String formatDistance(double distance) {
        return distance >= 1000.0
                ? String.format(Locale.ROOT, "%.1fkm", distance / 1000.0)
                : (int) distance + "m";
    }

    private static void drawArrow(GuiGraphics graphics, float x, float y, float angle, int color, float userScale) {
        int size = Math.max(6, Math.round(ARROW_SIZE * userScale));
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.mulPose(Axis.ZP.rotation(angle));
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        RenderSystem.setShaderColor(r, g, b, 1f);
        graphics.blit(ARROW, -size / 2, -size / 2, 0, 0, size, size, size, size);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        pose.popPose();
    }

    /**
     * World -> clip -> NDC -> GUI-scaled screen space, with off-screen points clamped to an
     * inset border and given the angle to point an arrow along. A marker turns into an arrow once it
     * is within markerMargin of the edge; the arrow itself then sits arrowMargin in, so the two
     * sizes can be set independently.
     */
    private static ScreenPoint project(Vec3 worldPos, Vec3 cameraPos, Matrix4f viewProj, int guiWidth,
            int guiHeight, float markerMargin, float arrowMargin) {
        Vector4f clip = new Vector4f(
                (float) (worldPos.x - cameraPos.x),
                (float) (worldPos.y - cameraPos.y),
                (float) (worldPos.z - cameraPos.z),
                1.0f);
        viewProj.transform(clip);

        // A point behind the camera divides by a negative w, which flips both axes; undo that
        // so a clamped marker still points toward the gate instead of away from it.
        boolean behind = clip.w <= 0.0001f;
        float ndcX = behind ? -(clip.x / clip.w) : clip.x / clip.w;
        float ndcY = behind ? -(clip.y / clip.w) : clip.y / clip.w;

        float sx = (ndcX * 0.5f + 0.5f) * guiWidth;
        float sy = (1.0f - (ndcY * 0.5f + 0.5f)) * guiHeight;

        boolean offscreen = behind || sx < markerMargin || sx > guiWidth - markerMargin
                || sy < markerMargin || sy > guiHeight - markerMargin;
        if (!offscreen) {
            return new ScreenPoint(sx, sy, false, 0f);
        }

        float cx = guiWidth / 2f;
        float cy = guiHeight / 2f;
        float dx = sx - cx;
        float dy = sy - cy;
        if (dx == 0 && dy == 0) {
            dx = 1;
        }
        float angle = (float) Math.atan2(dy, dx);
        float halfW = guiWidth / 2f - arrowMargin;
        float halfH = guiHeight / 2f - arrowMargin;
        float scale = Math.min(
                dx != 0 ? Math.abs(halfW / dx) : Float.MAX_VALUE,
                dy != 0 ? Math.abs(halfH / dy) : Float.MAX_VALUE);
        return new ScreenPoint(cx + dx * scale, cy + dy * scale, true, angle);
    }

    private record Candidate(Waypoint waypoint, Vec3 worldPos, double distance) {
    }

    private record ScreenPoint(float x, float y, boolean offscreen, float angle) {
    }
}
