package mc.sayda.creraces.client.screen;

import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.ClanActionPacket;
import mc.sayda.creraces.network.ClanUpdatePacket;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.territory.DiplomacyStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Sets the local race's diplomatic stance (ally, enemy, neutral) towards every other race. */
@SuppressWarnings("null")
public class ClanManagementScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 240;
    private static final int ROW_HEIGHT = 20;
    private static final int LIST_TOP_OFFSET = 44; // from panel top to first row
    private static final int LIST_BOTTOM_MARGIN = 28; // space reserved below list
    private static final int STATUS_BUTTON_WIDTH = 48;

    private static volatile ClanUpdatePacket lastUpdate;

    private int scrollOffset = 0;

    public ClanManagementScreen() {
        super(Component.translatable("screen.creraces.clan_management"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new ClanManagementScreen());
    }

    public static void update(ClanUpdatePacket pkt) {
        lastUpdate = pkt;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ClanManagementScreen s) {
            s.init(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        }
    }

    @Override
    protected void init() {
        List<Race> others = otherRaces();
        int maxOffset = Math.max(0, others.size() - visibleRows());
        scrollOffset = Math.min(scrollOffset, maxOffset);

        int end = Math.min(others.size(), scrollOffset + visibleRows());
        for (int i = scrollOffset; i < end; i++) {
            ResourceLocation raceId = others.get(i).id();
            int rowY = listTop() + (i - scrollOffset) * ROW_HEIGHT;
            DiplomacyStatus current = relationFor(raceId);

            for (DiplomacyStatus status : DiplomacyStatus.values()) {
                Component label = Component.translatable(statusKey(status))
                        .withStyle(current == status ? statusFormat(status) : ChatFormatting.DARK_GRAY);
                addRenderableWidget(Button.builder(label, b -> BoundaryHandler.sendClanAction(
                                new ClanActionPacket(ClanActionPacket.Action.SET_RELATION, raceId, status)))
                        .bounds(statusColumnX(status), rowY + 2, STATUS_BUTTON_WIDTH - 2, 14).build());
            }
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(this.width / 2 - 50, panelTop() + PANEL_HEIGHT - 22, 100, 16).build());
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int maxOffset = Math.max(0, otherRaces().size() - visibleRows());
        int newOffset = (int) Math.max(0, Math.min(maxOffset, scrollOffset - delta));
        if (newOffset != scrollOffset) {
            scrollOffset = newOffset;
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        renderBackground(g);

        int cx = this.width / 2;
        int pl = panelLeft();
        int pt = panelTop();

        g.fill(pl, pt, pl + PANEL_WIDTH, pt + PANEL_HEIGHT, 0xBB000000);
        g.renderOutline(pl, pt, PANEL_WIDTH, PANEL_HEIGHT, 0xFFAAAAAA);

        g.drawCenteredString(font, Component.translatable("screen.creraces.clan_management")
                .withStyle(ChatFormatting.GOLD), cx, pt + 6, 0xFFFFFF);

        if (lastUpdate != null) {
            Race own = RaceRegistry.get(lastUpdate.raceId);
            String ownName = own != null ? own.name().getString() : lastUpdate.raceId.getPath();
            g.drawCenteredString(font, Component.literal(ownName).withStyle(ChatFormatting.WHITE),
                    cx, pt + 18, 0xAAAAAA);
        }

        // Column headers: the race name, then each status button's initial.
        g.drawString(font, Component.translatable("screen.creraces.clan_management.race")
                .withStyle(ChatFormatting.GRAY), pl + 6, pt + 32, -1, false);
        for (DiplomacyStatus status : DiplomacyStatus.values()) {
            g.drawCenteredString(font, Component.translatable(statusKey(status) + ".initial")
                    .withStyle(statusFormat(status)), statusColumnX(status) + STATUS_BUTTON_WIDTH / 2, pt + 32, -1);
        }

        List<Race> others = otherRaces();
        int listTop = listTop();
        int listBottom = listBottom();
        int visibleRows = visibleRows();

        g.enableScissor(pl, listTop, pl + PANEL_WIDTH, listBottom);
        int end = Math.min(others.size(), scrollOffset + visibleRows);
        for (int i = scrollOffset; i < end; i++) {
            Race race = others.get(i);
            int rowY = listTop + (i - scrollOffset) * ROW_HEIGHT;
            int color = statusFormat(relationFor(race.id())).getColor();
            g.drawString(font, race.name().getString(), pl + 6, rowY + 6, color, false);
        }
        g.disableScissor();

        if (others.size() > visibleRows) {
            int sbX = pl + PANEL_WIDTH - 5;
            int sbH = listBottom - listTop;
            int thumbH = Math.max(10, sbH * visibleRows / others.size());
            int maxOffset = Math.max(1, others.size() - visibleRows);
            int thumbY = listTop + (sbH - thumbH) * scrollOffset / maxOffset;
            g.fill(sbX, listTop, sbX + 4, listBottom, 0x44FFFFFF);
            g.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, 0xCCFFFFFF);
        }

        super.render(g, mx, my, dt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int panelLeft() {
        return this.width / 2 - PANEL_WIDTH / 2;
    }

    private int panelTop() {
        return this.height / 2 - PANEL_HEIGHT / 2;
    }

    private int listTop() {
        return panelTop() + LIST_TOP_OFFSET;
    }

    private int listBottom() {
        return panelTop() + PANEL_HEIGHT - LIST_BOTTOM_MARGIN;
    }

    private int visibleRows() {
        return Math.max(1, (listBottom() - listTop()) / ROW_HEIGHT);
    }

    /** Left edge of a status column; the three columns sit flush against the panel's right side. */
    private int statusColumnX(DiplomacyStatus status) {
        return panelLeft() + PANEL_WIDTH - 3 * STATUS_BUTTON_WIDTH - 8 + status.ordinal() * STATUS_BUTTON_WIDTH;
    }

    private List<Race> otherRaces() {
        if (lastUpdate == null) {
            return Collections.emptyList();
        }
        ResourceLocation myId = lastUpdate.raceId;
        List<Race> result = new ArrayList<>();
        for (Race r : RaceRegistry.getAll()) {
            if (!r.id().equals(myId) && r.selectable()) {
                result.add(r);
            }
        }
        return result;
    }

    private DiplomacyStatus relationFor(ResourceLocation raceId) {
        if (lastUpdate == null) {
            return DiplomacyStatus.NEUTRAL;
        }
        return lastUpdate.relations.getOrDefault(raceId, DiplomacyStatus.NEUTRAL);
    }

    private static String statusKey(DiplomacyStatus status) {
        return "screen.creraces.clan_management.status." + status.name().toLowerCase(Locale.ROOT);
    }

    private static ChatFormatting statusFormat(DiplomacyStatus status) {
        return switch (status) {
            case ALLY -> ChatFormatting.BLUE;
            case ENEMY -> ChatFormatting.RED;
            case NEUTRAL -> ChatFormatting.GRAY;
        };
    }
}
