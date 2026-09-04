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
import java.util.List;

@SuppressWarnings("null")
public class ClanManagementScreen extends Screen {

    private static final int PW = 320;
    private static final int PH = 240;
    private static final int ROW_H = 20;
    private static final int LIST_TOP_OFFSET = 44; // from panel top to first row
    private static final int LIST_BOTTOM_MARGIN = 28; // space reserved below list

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

    // ── Init ───────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int pl = cx - PW / 2;
        int pt = cy - PH / 2;

        List<Race> others = otherRaces();
        int listTop = pt + LIST_TOP_OFFSET;
        int listBottom = pt + PH - LIST_BOTTOM_MARGIN;
        int maxVisible = Math.max(1, (listBottom - listTop) / ROW_H);
        int maxOffset = Math.max(0, others.size() - maxVisible);
        scrollOffset = Math.min(scrollOffset, maxOffset);

        int btnW = 48;
        int btnX = pl + PW - 3 * btnW - 8;

        int end = Math.min(others.size(), scrollOffset + maxVisible);
        for (int i = scrollOffset; i < end; i++) {
            Race race = others.get(i);
            ResourceLocation rid = race.id();
            int rowY = listTop + (i - scrollOffset) * ROW_H;
            DiplomacyStatus current = relationFor(rid);

            for (DiplomacyStatus s : DiplomacyStatus.values()) {
                final DiplomacyStatus chosen = s;
                boolean active = current == s;
                Component label = Component.literal(s.name())
                        .withStyle(active ? statusFormat(s) : ChatFormatting.DARK_GRAY);
                addRenderableWidget(Button.builder(label, b ->
                        BoundaryHandler.sendClanAction(
                                new ClanActionPacket(ClanActionPacket.Action.SET_RELATION, rid, chosen)))
                        .bounds(btnX + s.ordinal() * btnW, rowY + 2, btnW - 2, 14).build());
            }
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(cx - 50, pt + PH - 22, 100, 16).build());
    }

    // ── Input ──────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double delta) {
        List<Race> others = otherRaces();
        int cy = this.height / 2;
        int pt = cy - PH / 2;
        int listTop = pt + LIST_TOP_OFFSET;
        int listBottom = pt + PH - LIST_BOTTOM_MARGIN;
        int maxVisible = Math.max(1, (listBottom - listTop) / ROW_H);
        int maxOffset = Math.max(0, others.size() - maxVisible);
        int newOffset = (int) Math.max(0, Math.min(maxOffset, scrollOffset - delta));
        if (newOffset != scrollOffset) {
            scrollOffset = newOffset;
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, delta);
    }

    // ── Render ─────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        renderBackground(g, mx, my, dt);

        int cx = this.width / 2;
        int cy = this.height / 2;
        int pl = cx - PW / 2;
        int pt = cy - PH / 2;

        g.fill(pl, pt, pl + PW, pt + PH, 0xBB000000);
        g.renderOutline(pl, pt, PW, PH, 0xFFAAAAAA);

        // Title
        g.drawCenteredString(font, Component.translatable("screen.creraces.clan_management")
                .withStyle(ChatFormatting.GOLD), cx, pt + 6, 0xFFFFFF);

        // Own race subtitle
        if (lastUpdate != null) {
            Race own = RaceRegistry.get(lastUpdate.raceId);
            String ownName = own != null ? own.name().getString() : lastUpdate.raceId.getPath();
            g.drawCenteredString(font, Component.literal(ownName).withStyle(ChatFormatting.WHITE),
                    cx, pt + 18, 0xAAAAAA);
        }

        // Column headers
        int btnW = 48;
        int btnX = pl + PW - 3 * btnW - 8;
        g.drawString(font, Component.literal("Race").withStyle(ChatFormatting.GRAY), pl + 6, pt + 32, -1, false);
        for (DiplomacyStatus s : DiplomacyStatus.values()) {
            g.drawCenteredString(font, Component.literal(s.name().charAt(0) + "")
                            .withStyle(statusFormat(s)),
                    btnX + s.ordinal() * btnW + btnW / 2, pt + 32, -1);
        }

        // Race rows (scissored to list area)
        List<Race> others = otherRaces();
        int listTop = pt + LIST_TOP_OFFSET;
        int listBottom = pt + PH - LIST_BOTTOM_MARGIN;
        int maxVisible = Math.max(1, (listBottom - listTop) / ROW_H);

        g.enableScissor(pl, listTop, pl + PW, listBottom);
        int end = Math.min(others.size(), scrollOffset + maxVisible);
        for (int i = scrollOffset; i < end; i++) {
            Race race = others.get(i);
            int rowY = listTop + (i - scrollOffset) * ROW_H;
            DiplomacyStatus current = relationFor(race.id());
            int color = switch (current) {
                case ALLY    -> 0x5555FF;
                case ENEMY   -> 0xFF5555;
                case NEUTRAL -> 0xAAAAAA;
            };
            g.drawString(font, race.name().getString(), pl + 6, rowY + 6, color, false);
        }
        g.disableScissor();

        // Scrollbar
        if (others.size() > maxVisible) {
            int sbX = pl + PW - 5;
            int sbH = listBottom - listTop;
            int thumbH = Math.max(10, sbH * maxVisible / others.size());
            int maxOffset = Math.max(1, others.size() - maxVisible);
            int thumbY = listTop + (sbH - thumbH) * scrollOffset / maxOffset;
            g.fill(sbX, listTop, sbX + 4, listBottom, 0x44FFFFFF);
            g.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, 0xCCFFFFFF);
        }

        super.render(g, mx, my, dt);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private List<Race> otherRaces() {
        if (lastUpdate == null) return java.util.Collections.emptyList();
        ResourceLocation myId = lastUpdate.raceId;
        List<Race> result = new ArrayList<>();
        for (Race r : RaceRegistry.getAll()) {
            if (!r.id().equals(myId) && r.selectable()) result.add(r);
        }
        return result;
    }

    private DiplomacyStatus relationFor(ResourceLocation raceId) {
        if (lastUpdate == null) return DiplomacyStatus.NEUTRAL;
        return lastUpdate.relations.getOrDefault(raceId, DiplomacyStatus.NEUTRAL);
    }

    private static ChatFormatting statusFormat(DiplomacyStatus s) {
        return switch (s) {
            case ALLY    -> ChatFormatting.BLUE;
            case ENEMY   -> ChatFormatting.RED;
            case NEUTRAL -> ChatFormatting.GRAY;
        };
    }

    /**
     * This screen deliberately draws no backdrop. 1.21 Screen.render() calls renderBackground()
     * on its own where 1.20.1 did not, so it is suppressed here to keep the view unobstructed.
     */
    @Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }
}
