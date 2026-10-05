package mc.sayda.creraces.client.screen;

import mc.sayda.creraces.client.waypoint.Waypoint;
import mc.sayda.creraces.client.waypoint.WaypointStore;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.ShareWaypointPacket;
import mc.sayda.creraces.network.SharedGate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * Lists every gate the local kitsune has discovered, with per-gate rename, recolour and delete,
 * sharing (the selected gate, the ticked ones, or all of them), a global marker toggle and the icon
 * and arrow sizes. Opened by sneak-clicking a torii bell (see ToriiGateInteractions). The panel is
 * sized from the screen so it stays usable at large GUI scales.
 */
@SuppressWarnings("null")
public class WaypointEditorScreen extends Screen {
    private static final int PALETTE_COLS = 8;
    private static final int[] PALETTE = {
            0xFFE0A0FF, 0xFFFF6B6B, 0xFFFFA94D, 0xFFFFE066, 0xFF69DB7C, 0xFF66D9E8, 0xFF74C0FC, 0xFFB197FC,
            0xFFFFFFFF, 0xFFCED4DA, 0xFF868E96, 0xFF343A40, 0xFFF783AC, 0xFF38D9A9, 0xFF5C7CFA, 0xFFA9784A,
    };
    private static final int MAX_PANEL_WIDTH = 340;
    private static final int MAX_PANEL_HEIGHT = 220;
    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int FOOTER_H = 26;
    private static final int BANNER_H = 20;
    private static final int ROW_H = 22;
    private static final int BTN_H = 16;
    private static final int CHECK_W = 9;
    private static final int OFFER_BTN_W = 54;
    private static final int SHARE_BTN_W = 52;
    /**
     * The share block sits at a fixed offset below the header, selection or not, so it does not
     * jump around. It clears two full rows of the colour palette (up to 16px swatches, 4px gaps).
     */
    private static final int SHARE_BLOCK_Y = 94;

    /** Which gates the Share button sends. */
    private enum Scope {
        SELECTED, MARKED, ALL
    }

    @Nullable
    private final Waypoint initialFocus;
    @Nullable
    private Waypoint selected;
    private final Set<UUID> marked = new HashSet<>();
    private Scope scope = Scope.SELECTED;
    private int scrollOffset = 0;
    private int recipientIndex = 0;
    private boolean didInitialScroll = false;

    // Layout, recomputed by init()
    private int panelLeft, panelTop, panelWidth, panelHeight;
    private int listX, listW, listTop, listBottom, visibleRows;
    private int rightX, rightW;
    private int colourLabelY, shareRowY;
    private boolean noPlayersHint;
    private int footerY, bannerY, scaleLabelX, arrowLabelX;

    @Nullable
    private EditBox nameBox;
    @Nullable
    private EditBox scaleBox;
    @Nullable
    private EditBox arrowBox;
    private Button markersButton;

    private WaypointEditorScreen(@Nullable Waypoint focus) {
        super(Component.translatable("gui.creraces.waypoint_editor.title"));
        this.initialFocus = focus;
        this.selected = focus;
    }

    /**
     * Opens the editor on the gate at this position, adding it first if it has not been seen yet:
     * sneak-opening the editor on an undiscovered gate counts as discovering it.
     */
    public static void openFor(String dimension, BlockPos pos) {
        Waypoint target = WaypointStore.get().discoverIfNew(dimension, pos);
        Minecraft.getInstance().setScreen(new WaypointEditorScreen(target));
    }

    @Override
    protected void init() {
        WaypointStore store = WaypointStore.get();
        List<Waypoint> all = store.all();
        if (selected != null && !all.contains(selected)) {
            selected = null;
        }
        Set<UUID> known = new HashSet<>();
        for (Waypoint wp : all) {
            known.add(wp.id());
        }
        marked.retainAll(known);
        nameBox = null;

        List<WaypointStore.PendingOffer> offers = store.pendingOffers();
        layOut(!offers.isEmpty());

        int maxOffset = Math.max(0, all.size() - visibleRows);
        if (!didInitialScroll && initialFocus != null) {
            didInitialScroll = true;
            int idx = all.indexOf(initialFocus);
            if (idx >= 0) {
                scrollOffset = idx;
            }
        }
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxOffset));

        if (selected != null) {
            initEditColumn(store, selected);
        }
        initShareBlock();
        // Pending offers are answered one at a time; the rest queue behind the first.
        if (!offers.isEmpty()) {
            initOfferButtons(store, offers.get(0));
        }
        initFooter(store);
    }

    private void layOut(boolean hasOffer) {
        panelWidth = Math.min(MAX_PANEL_WIDTH, this.width - 8);
        panelHeight = Math.min(MAX_PANEL_HEIGHT, this.height - 8);
        panelLeft = (this.width - panelWidth) / 2;
        panelTop = (this.height - panelHeight) / 2;

        footerY = panelTop + panelHeight - FOOTER_H + 6;
        bannerY = panelTop + panelHeight - FOOTER_H - BANNER_H + 2;

        listX = panelLeft + PAD;
        listW = Math.max(96, Math.min(150, panelWidth * 2 / 5));
        listTop = contentTop();
        listBottom = panelTop + panelHeight - FOOTER_H - (hasOffer ? BANNER_H : 0);
        visibleRows = Math.max(1, (listBottom - listTop) / ROW_H);
        // Leaves room between the list and the edit column for the scrollbar.
        rightX = listX + listW + PAD + 6;
        rightW = Math.max(60, panelLeft + panelWidth - PAD - rightX);
        shareRowY = contentTop() + SHARE_BLOCK_Y;
    }

    private int contentTop() {
        return panelTop + HEADER_H;
    }

    /** Name box, rename and delete, and the colour swatches for the selected gate. */
    private void initEditColumn(WaypointStore store, Waypoint sel) {
        int y = contentTop();
        nameBox = new EditBox(this.font, rightX, y, rightW, BTN_H,
                Component.translatable("gui.creraces.waypoint_editor.name_box"));
        nameBox.setMaxLength(48);
        nameBox.setValue(sel.name());
        addRenderableWidget(nameBox);
        y += BTN_H + 2;

        int half = (rightW - 2) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.creraces.waypoint_editor.btn.rename"),
                b -> applyRename(sel)).bounds(rightX, y, half, BTN_H).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.creraces.waypoint_editor.btn.delete"), b -> {
            store.remove(sel.id());
            selected = null;
            refresh();
        }).bounds(rightX + half + 2, y, rightW - half - 2, BTN_H).build());
        y += BTN_H + 4;

        colourLabelY = y;
        y += 10;
        int size = Math.max(8, Math.min(16, (rightW - (PALETTE_COLS - 1)) / PALETTE_COLS));
        int gap = Math.min(4, Math.max(1, (rightW - PALETTE_COLS * size) / (PALETTE_COLS - 1)));
        for (int i = 0; i < PALETTE.length; i++) {
            int col = i % PALETTE_COLS;
            int row = i / PALETTE_COLS;
            addRenderableWidget(new ColorSwatch(rightX + col * (size + gap), y + row * (size + gap), size,
                    PALETTE[i], sel));
        }
    }

    /** Recipient, which gates, and the Share button. Shown with or without a selection. */
    private void initShareBlock() {
        List<PlayerInfo> candidates = shareCandidates();
        noPlayersHint = candidates.isEmpty();
        if (noPlayersHint) {
            return;
        }

        recipientIndex = Math.floorMod(recipientIndex, candidates.size());
        PlayerInfo target = candidates.get(recipientIndex);
        addRenderableWidget(Button.builder(Component.literal("< " + target.getProfile().getName() + " >"), b -> {
            recipientIndex++;
            refresh();
        }).bounds(rightX, shareRowY, rightW, BTN_H).build());

        List<Waypoint> toShare = gatesForScope();
        int scopeY = shareRowY + BTN_H + 2;
        addRenderableWidget(Button.builder(scopeLabel(toShare.size()), b -> {
            scope = Scope.values()[(scope.ordinal() + 1) % Scope.values().length];
            refresh();
        }).bounds(rightX, scopeY, rightW - SHARE_BTN_W - 2, BTN_H).build());
        Button share = addRenderableWidget(Button.builder(
                Component.translatable("gui.creraces.waypoint_editor.btn.share"),
                b -> shareGates(target, toShare)).bounds(rightX + rightW - SHARE_BTN_W, scopeY, SHARE_BTN_W, BTN_H)
                .build());
        share.active = !toShare.isEmpty();
    }

    private void initOfferButtons(WaypointStore store, WaypointStore.PendingOffer offer) {
        int right = panelLeft + panelWidth - PAD;
        addRenderableWidget(Button.builder(Component.translatable("gui.creraces.waypoint_editor.btn.accept"), b -> {
            store.acceptOffer(offer);
            refresh();
        }).bounds(right - OFFER_BTN_W * 2 - 2, bannerY, OFFER_BTN_W, BTN_H).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.creraces.waypoint_editor.btn.decline"), b -> {
            store.declineOffer(offer);
            refresh();
        }).bounds(right - OFFER_BTN_W, bannerY, OFFER_BTN_W, BTN_H).build());
    }

    /** Marker toggle, icon size, arrow size and Done. */
    private void initFooter(WaypointStore store) {
        markersButton = addRenderableWidget(Button.builder(markersLabel(), b -> {
            store.setMarkersEnabled(!store.isMarkersEnabled());
            markersButton.setMessage(markersLabel());
        }).bounds(panelLeft + PAD, footerY, 88, BTN_H).build());

        scaleLabelX = panelLeft + PAD + 88 + 6;
        scaleBox = sizeBox(scaleLabelX, "gui.creraces.waypoint_editor.scale", store.markerScale());
        arrowLabelX = scaleBox.getX() + scaleBox.getWidth() + 6;
        arrowBox = sizeBox(arrowLabelX, "gui.creraces.waypoint_editor.arrow_scale", store.arrowScale());

        addRenderableWidget(Button.builder(Component.translatable("gui.creraces.waypoint_editor.btn.done"),
                b -> onClose()).bounds(panelLeft + panelWidth - PAD - 48, footerY, 48, BTN_H).build());
    }

    /** A labelled size field: render() draws the label, the box sits right after it. */
    private EditBox sizeBox(int labelX, String labelKey, double value) {
        int boxX = labelX + this.font.width(Component.translatable(labelKey)) + 4;
        EditBox box = new EditBox(this.font, boxX, footerY, 34, BTN_H, Component.translatable(labelKey));
        box.setMaxLength(5);
        box.setValue(formatScale(value));
        box.setHint(Component.literal("1.00"));
        return addRenderableWidget(box);
    }

    private Component markersLabel() {
        boolean on = WaypointStore.get().isMarkersEnabled();
        return Component.translatable("gui.creraces.waypoint_editor.markers",
                Component.translatable(on ? "gui.creraces.waypoint_editor.on" : "gui.creraces.waypoint_editor.off"));
    }

    private Component scopeLabel(int count) {
        return Component.translatable("gui.creraces.waypoint_editor.scope." + scope.name().toLowerCase(Locale.ROOT),
                count);
    }

    private List<PlayerInfo> shareCandidates() {
        List<PlayerInfo> result = new ArrayList<>();
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null || Minecraft.getInstance().player == null) {
            return result;
        }
        UUID selfId = Minecraft.getInstance().player.getUUID();
        for (PlayerInfo info : connection.getOnlinePlayers()) {
            if (!info.getProfile().getId().equals(selfId)) {
                result.add(info);
            }
        }
        return result;
    }

    /** The gates the Share button would send right now. */
    private List<Waypoint> gatesForScope() {
        List<Waypoint> all = WaypointStore.get().all();
        Waypoint sel = selected;
        return switch (scope) {
            case SELECTED -> sel != null ? List.of(sel) : List.<Waypoint>of();
            case MARKED -> all.stream().filter(wp -> marked.contains(wp.id())).toList();
            case ALL -> List.copyOf(all);
        };
    }

    private void shareGates(PlayerInfo target, List<Waypoint> gates) {
        List<SharedGate> shared = new ArrayList<>();
        for (Waypoint wp : gates) {
            shared.add(new SharedGate(wp.dimension(), wp.pos(), wp.name()));
        }
        // One packet per MAX_PER_PACKET gates; anything larger arrives as several offers.
        for (int i = 0; i < shared.size(); i += SharedGate.MAX_PER_PACKET) {
            List<SharedGate> chunk = shared.subList(i, Math.min(shared.size(), i + SharedGate.MAX_PER_PACKET));
            BoundaryHandler.sendShareWaypoint(new ShareWaypointPacket(target.getProfile().getId(), chunk));
        }
    }

    private void applyRename(Waypoint sel) {
        if (nameBox == null) {
            return;
        }
        String value = nameBox.getValue().trim();
        if (!value.isEmpty()) {
            sel.setName(value);
            WaypointStore.get().notifyEdited();
            refresh();
        }
    }

    /** Parses a size box into the store, then shows the stored (clamped) value back in it. */
    private static void commitScale(@Nullable EditBox box, DoubleConsumer setter, DoubleSupplier current) {
        if (box == null) {
            return;
        }
        try {
            setter.accept(Double.parseDouble(box.getValue().trim()));
        } catch (NumberFormatException ignored) {
            // An unparseable entry just reverts to the stored value below.
        }
        box.setValue(formatScale(current.getAsDouble()));
    }

    private void applyScales() {
        WaypointStore store = WaypointStore.get();
        commitScale(scaleBox, store::setMarkerScale, store::markerScale);
        commitScale(arrowBox, store::setArrowScale, store::arrowScale);
    }

    /** Full re-init rather than patching widgets in place; commits pending size edits first. */
    private void refresh() {
        applyScales();
        rebuildWidgets();
    }

    private static String formatScale(double scale) {
        return String.format(Locale.ROOT, "%.2f", scale);
    }

    private static String shortDimension(String dimension) {
        int colon = dimension.indexOf(':');
        return colon >= 0 ? dimension.substring(colon + 1) : dimension;
    }

    /** "x, y, z", plus the dimension when the gate is not in the one the player is standing in. */
    private static String coordinateLine(Waypoint wp, String currentDimension) {
        BlockPos p = wp.pos();
        String line = p.getX() + ", " + p.getY() + ", " + p.getZ();
        return wp.dimension().equals(currentDimension) ? line : line + " (" + shortDimension(wp.dimension()) + ")";
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && mx >= listX && mx < listX + listW && my >= listTop && my < listBottom) {
            int idx = scrollOffset + (int) ((my - listTop) / ROW_H);
            List<Waypoint> all = WaypointStore.get().all();
            if (idx < all.size() && idx < scrollOffset + visibleRows) {
                Waypoint wp = all.get(idx);
                // The tick box's hit area reaches a little further left than the box itself.
                if (mx >= listX + listW - CHECK_W - 6) {
                    if (!marked.remove(wp.id())) {
                        marked.add(wp.id());
                    }
                } else {
                    selected = wp;
                }
                refresh();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollY) {
        int max = Math.max(0, WaypointStore.get().all().size() - visibleRows);
        int next = (int) Math.max(0, Math.min(max, scrollOffset - Math.signum(scrollY)));
        if (next != scrollOffset) {
            scrollOffset = next;
            refresh();
            return true;
        }
        return super.mouseScrolled(mx, my, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            for (EditBox box : new EditBox[] { scaleBox, arrowBox }) {
                if (box != null && box.isFocused()) {
                    applyScales();
                    box.setFocused(false);
                    return true;
                }
            }
            if (nameBox != null && nameBox.isFocused() && selected != null) {
                applyRename(selected);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        applyScales();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        g.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xBB000000);
        g.renderOutline(panelLeft, panelTop, panelWidth, panelHeight, 0xFFAAAAAA);
        g.drawCenteredString(font, Component.translatable("gui.creraces.waypoint_editor.title")
                .withStyle(ChatFormatting.GOLD), panelLeft + panelWidth / 2, panelTop + 6, 0xFFFFFF);

        renderGateList(g, mx, my);

        if (selected == null) {
            drawWrapped(g, Component.translatable("gui.creraces.waypoint_editor.select_hint"), rightX,
                    contentTop() + 2, rightW);
        } else {
            g.drawString(font, Component.translatable("gui.creraces.waypoint_editor.colour"), rightX, colourLabelY,
                    0xAAAAAA, false);
        }
        if (noPlayersHint) {
            g.drawString(font, font.plainSubstrByWidth(
                            Component.translatable("gui.creraces.waypoint_editor.no_players").getString(), rightW),
                    rightX, shareRowY + 4, 0x777777, false);
        }

        renderOfferBanner(g);

        g.drawString(font, Component.translatable("gui.creraces.waypoint_editor.scale"), scaleLabelX, footerY + 4,
                0xAAAAAA, false);
        g.drawString(font, Component.translatable("gui.creraces.waypoint_editor.arrow_scale"), arrowLabelX,
                footerY + 4, 0xAAAAAA, false);

        super.render(g, mx, my, dt);
    }

    /** Each row: a colour bar, the name over the coordinates, and a tick box for sharing. */
    private void renderGateList(GuiGraphics g, int mx, int my) {
        List<Waypoint> all = WaypointStore.get().all();
        g.fill(listX - 1, listTop - 1, listX + listW + 1, listBottom + 1, 0x55000000);
        g.renderOutline(listX - 1, listTop - 1, listW + 2, listBottom - listTop + 2, 0xFF555555);
        if (all.isEmpty()) {
            drawWrapped(g, Component.translatable("gui.creraces.waypoint_editor.empty"), listX + 4, listTop + 4,
                    listW - 8);
        }

        Minecraft mc = Minecraft.getInstance();
        String currentDimension = mc.level != null ? mc.level.dimension().location().toString() : "";
        int textW = listW - CHECK_W - 15;
        int end = Math.min(all.size(), scrollOffset + visibleRows);
        for (int i = scrollOffset; i < end; i++) {
            Waypoint wp = all.get(i);
            int rowY = listTop + (i - scrollOffset) * ROW_H;
            boolean hover = mx >= listX && mx < listX + listW && my >= rowY && my < rowY + ROW_H;
            if (wp == selected) {
                g.fill(listX, rowY, listX + listW, rowY + ROW_H - 1, 0x55FFFFFF);
            } else if (hover) {
                g.fill(listX, rowY, listX + listW, rowY + ROW_H - 1, 0x22FFFFFF);
            }
            g.fill(listX + 2, rowY + 2, listX + 5, rowY + ROW_H - 3, 0xFF000000 | (wp.color() & 0xFFFFFF));
            String name = wp.displayName();
            g.drawString(font, font.plainSubstrByWidth(name, textW), listX + 9, rowY + 3,
                    wp.isStale() ? 0x888888 : 0xFFFFFF, false);
            g.drawString(font, font.plainSubstrByWidth(coordinateLine(wp, currentDimension), textW), listX + 9,
                    rowY + 12, 0x888888, false);

            boolean isMarked = marked.contains(wp.id());
            int cbX = listX + listW - CHECK_W - 3;
            int cbY = rowY + (ROW_H - CHECK_W) / 2;
            g.renderOutline(cbX, cbY, CHECK_W, CHECK_W, isMarked ? 0xFFFFFFFF : 0xFF777777);
            if (isMarked) {
                g.fill(cbX + 2, cbY + 2, cbX + CHECK_W - 2, cbY + CHECK_W - 2, 0xFFFFFFFF);
            }
        }

        if (all.size() > visibleRows) {
            int sbX = listX + listW + 3;
            int sbH = listBottom - listTop;
            int thumbH = Math.max(10, sbH * visibleRows / all.size());
            int maxOffset = Math.max(1, all.size() - visibleRows);
            int thumbY = listTop + (sbH - thumbH) * scrollOffset / maxOffset;
            g.fill(sbX, listTop, sbX + 3, listBottom, 0x44FFFFFF);
            g.fill(sbX, thumbY, sbX + 3, thumbY + thumbH, 0xCCFFFFFF);
        }
    }

    private void renderOfferBanner(GuiGraphics g) {
        List<WaypointStore.PendingOffer> offers = WaypointStore.get().pendingOffers();
        if (offers.isEmpty()) {
            return;
        }
        WaypointStore.PendingOffer offer = offers.get(0);
        Component offerText = offer.gates().size() == 1
                ? Component.translatable("gui.creraces.waypoint_editor.offer", offer.senderName(),
                        offer.gates().get(0).name())
                : Component.translatable("gui.creraces.waypoint_editor.offer_many", offer.senderName(),
                        offer.gates().size());
        String text = offerText.getString();
        if (offers.size() > 1) {
            text += " " + Component.translatable("gui.creraces.waypoint_editor.more_offers", offers.size() - 1)
                    .getString();
        }
        g.drawString(font, font.plainSubstrByWidth(text, panelWidth - PAD * 2 - OFFER_BTN_W * 2 - 6),
                panelLeft + PAD, bannerY + 4, 0xFFD966, false);
    }

    private void drawWrapped(GuiGraphics g, Component text, int x, int y, int width) {
        for (FormattedCharSequence line : font.split(text, width)) {
            g.drawString(font, line, x, y, 0xAAAAAA, false);
            y += font.lineHeight + 1;
        }
    }

    /** A single clickable colour square; applies immediately and persists through the store. */
    private static class ColorSwatch extends Button {
        private final int color;
        private final Waypoint target;

        ColorSwatch(int x, int y, int size, int color, Waypoint target) {
            super(x, y, size, size, Component.empty(), b -> {
                target.setColor(color);
                WaypointStore.get().notifyEdited();
            }, DEFAULT_NARRATION);
            this.color = color;
            this.target = target;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float dt) {
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000 | (color & 0xFFFFFF));
            if (target.color() == color) {
                g.renderOutline(getX() - 1, getY() - 1, width + 2, height + 2, 0xFFFFFFFF);
            } else if (isHoveredOrFocused()) {
                g.renderOutline(getX(), getY(), width, height, 0xFFAAAAAA);
            }
        }
    }
}
