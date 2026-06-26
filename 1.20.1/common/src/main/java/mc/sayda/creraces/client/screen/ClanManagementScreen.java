package mc.sayda.creraces.client.screen;

import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.ClanActionPacket;
import mc.sayda.creraces.network.ClanUpdatePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.UUID;

@SuppressWarnings("null")
public class ClanManagementScreen extends Screen {

    // ── Static state ───────────────────────────────────────────────────────────
    private static ClanUpdatePacket lastUpdate;

    // ── Instance state ─────────────────────────────────────────────────────────
    private ClanUpdatePacket.FactionInfo selectedFaction;
    private boolean isLeader = false;

    private EditBox inviteBox;

    private static final int PW = 310;
    private static final int PH = 240;

    public ClanManagementScreen() {
        super(Component.translatable("creraces.screen.clan_management"));
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

        // Resolve if local player is the clan leader
        UUID localId = minecraft != null && minecraft.player != null ? minecraft.player.getUUID() : null;
        isLeader = lastUpdate != null && localId != null && localId.equals(lastUpdate.leaderId);

        // Keep selected in sync
        if (selectedFaction != null && lastUpdate != null) {
            selectedFaction = lastUpdate.factions.stream()
                    .filter(f -> f.factionId.equals(selectedFaction.factionId))
                    .findFirst().orElse(null);
        }

        // ── Invite faction row (leader only) ──
        if (isLeader) {
            // Row 1 (from bottom -66): invite text field + button
            inviteBox = new EditBox(font, pl + 5, pt + PH - 66, 140, 16,
                    Component.translatable("creraces.screen.invite_faction_hint"));
            inviteBox.setMaxLength(32);
            addRenderableWidget(inviteBox);

            addRenderableWidget(Button.builder(
                    Component.translatable("creraces.screen.invite_faction"), b -> {
                        if (inviteBox != null) {
                            String name = inviteBox.getValue().trim();
                            if (!name.isEmpty()) {
                                BoundaryHandler.sendClanAction(new ClanActionPacket(
                                        ClanActionPacket.Action.INVITE_FACTION, name));
                                inviteBox.setValue("");
                            }
                        }
                    }).bounds(pl + 148, pt + PH - 66, 85, 16).build());

            // Row 2 (from bottom -46): kick selected faction
            addRenderableWidget(Button.builder(
                    Component.translatable("creraces.screen.kick_faction").withStyle(ChatFormatting.RED),
                    b -> {
                        if (selectedFaction != null)
                            BoundaryHandler.sendClanAction(new ClanActionPacket(
                                    ClanActionPacket.Action.KICK_FACTION, selectedFaction.factionId));
                    }).bounds(pl + 5, pt + PH - 46, 130, 16).build());

            // Row 3 (from bottom -22): disband on the left, done on the right — no overlap
            addRenderableWidget(Button.builder(
                    Component.translatable("creraces.screen.disband_clan").withStyle(ChatFormatting.DARK_RED),
                    b -> {
                        BoundaryHandler.sendClanAction(new ClanActionPacket(ClanActionPacket.Action.DISBAND));
                        onClose();
                    }).bounds(pl + 5, pt + PH - 22, 100, 16).build());
        }

        // Done: right-aligned when leader buttons are visible, centred otherwise
        int doneX = isLeader ? pl + PW - 105 : cx - 50;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(doneX, pt + PH - 22, 100, 16).build());
    }

    // ── Render ─────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        renderBackground(g);

        int cx = this.width / 2;
        int cy = this.height / 2;
        int pl = cx - PW / 2;
        int pt = cy - PH / 2;

        // Panel
        g.fill(pl, pt, pl + PW, pt + PH, 0xBB000000);
        g.renderOutline(pl, pt, PW, PH, 0xFFAAAAAA);

        // Title
        g.drawCenteredString(font, Component.translatable("creraces.screen.clan_management")
                .withStyle(ChatFormatting.GOLD), cx, pt + 5, 0xFFFFFF);

        if (lastUpdate != null) {
            // Leader line
            g.drawString(font, Component.literal("Leader: " + lastUpdate.leaderName)
                    .withStyle(ChatFormatting.YELLOW), pl + 5, pt + 18, -1, false);

            // Column divider — stops above the invite/kick/disband rows
            int divX = pl + PW / 2;
            g.fill(divX, pt + 30, divX + 1, pt + PH - 78, 0x55FFFFFF);

            // Left: faction list
            g.drawString(font, Component.translatable("creraces.screen.factions"),
                    pl + 5, pt + 32, 0xAAAAAA, false);

            int y = pt + 44;
            for (ClanUpdatePacket.FactionInfo f : lastUpdate.factions) {
                boolean sel = selectedFaction != null && selectedFaction.factionId.equals(f.factionId);
                if (sel) g.fill(pl + 3, y - 1, divX - 3, y + 20, 0x44FFFFFF);

                g.drawString(font, Component.literal(f.factionName).withStyle(ChatFormatting.WHITE),
                        pl + 5, y, -1, false);
                g.drawString(font, Component.literal("  " + f.leaderName + " (" + f.memberCount + ")")
                        .withStyle(ChatFormatting.GRAY), pl + 5, y + 10, -1, false);
                y += 22;
                if (y > pt + PH - 80) break;
            }

            // Right: info / settings placeholder
            g.drawString(font, Component.literal("Clans: " + lastUpdate.factions.size()),
                    divX + 5, pt + 32, 0xAAAAAA, false);
        }

        super.render(g, mx, my, dt);
    }

    // ── Mouse ──────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && lastUpdate != null) {
            int cx = this.width / 2;
            int cy = this.height / 2;
            int pl = cx - PW / 2;
            int pt = cy - PH / 2;
            int divX = pl + PW / 2;

            if (mx >= pl + 3 && mx < divX - 3) {
                int y = pt + 44;
                for (ClanUpdatePacket.FactionInfo f : lastUpdate.factions) {
                    if (my >= y - 1 && my < y + 21) {
                        selectedFaction = (selectedFaction != null
                                && selectedFaction.factionId.equals(f.factionId)) ? null : f;
                        return true;
                    }
                    y += 22;
                    if (y > pt + PH - 80) break;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
