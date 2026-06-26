package mc.sayda.creraces.client.screen;

import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.ClanActionPacket;
import mc.sayda.creraces.network.FactionActionPacket;
import mc.sayda.creraces.network.FactionUpdatePacket;
import mc.sayda.creraces.territory.FactionRank;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.UUID;

@SuppressWarnings("null")
public class FactionManagementScreen extends Screen {

    // ── Static state shared with packet handler ────────────────────────────────
    private static FactionUpdatePacket lastUpdate;

    // ── Instance state ─────────────────────────────────────────────────────────
    private FactionUpdatePacket.MemberInfo selectedMember;
    private FactionRank localRank = FactionRank.MEMBER;

    private boolean confirmingLeave = false;

    private EditBox inviteBox;
    private Button inviteBtn;
    private Button promoteBtn;
    private Button demoteBtn;
    private Button kickBtn;
    private Button disbandBtn;
    private Button leaveBtn;
    private Button mapBtn;

    // Panel dimensions
    private static final int PW = 290;
    private static final int PH = 210;

    public FactionManagementScreen() {
        super(Component.translatable("creraces.screen.faction_management"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new FactionManagementScreen());
    }

    public static void update(FactionUpdatePacket pkt) {
        lastUpdate = pkt;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof FactionManagementScreen s) {
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

        // Resolve local rank
        UUID localId = minecraft != null && minecraft.player != null ? minecraft.player.getUUID() : null;
        localRank = FactionRank.MEMBER;
        if (lastUpdate != null && localId != null) {
            for (FactionUpdatePacket.MemberInfo m : lastUpdate.members) {
                if (m.uuid.equals(localId)) { localRank = m.rank; break; }
            }
        }

        // Keep selected member in sync after refresh
        if (selectedMember != null && lastUpdate != null) {
            selectedMember = lastUpdate.members.stream()
                    .filter(m -> m.uuid.equals(selectedMember.uuid))
                    .findFirst().orElse(null);
        }

        // ── Invite row (OFFICER+) ──
        if (localRank.isAtLeast(FactionRank.OFFICER)) {
            inviteBox = new EditBox(font, pl + 5, pt + 18, 130, 16,
                    Component.translatable("creraces.screen.invite_player_hint"));
            inviteBox.setMaxLength(32);
            addRenderableWidget(inviteBox);

            inviteBtn = Button.builder(Component.translatable("creraces.screen.invite"), b -> {
                if (inviteBox != null) {
                    String name = inviteBox.getValue().trim();
                    if (!name.isEmpty()) {
                        BoundaryHandler.sendFactionAction(new FactionActionPacket(
                                FactionActionPacket.Action.INVITE, name));
                        inviteBox.setValue("");
                    }
                }
            }).bounds(pl + 138, pt + 18, 50, 16).build();
            addRenderableWidget(inviteBtn);
        }

        // ── Right-side action buttons ──
        int bx = pl + PW - 100;

        if (localRank == FactionRank.LEADER) {
            promoteBtn = Button.builder(Component.translatable("creraces.screen.promote"), b -> {
                if (selectedMember != null)
                    BoundaryHandler.sendFactionAction(new FactionActionPacket(
                            FactionActionPacket.Action.PROMOTE, selectedMember.uuid));
            }).bounds(bx, pt + 40, 95, 16).build();
            addRenderableWidget(promoteBtn);

            demoteBtn = Button.builder(Component.translatable("creraces.screen.demote"), b -> {
                if (selectedMember != null)
                    BoundaryHandler.sendFactionAction(new FactionActionPacket(
                            FactionActionPacket.Action.DEMOTE, selectedMember.uuid));
            }).bounds(bx, pt + 60, 95, 16).build();
            addRenderableWidget(demoteBtn);
        }

        if (localRank.isAtLeast(FactionRank.OFFICER)) {
            kickBtn = Button.builder(Component.translatable("creraces.screen.kick"), b -> {
                if (selectedMember != null)
                    BoundaryHandler.sendFactionAction(new FactionActionPacket(
                            FactionActionPacket.Action.KICK, selectedMember.uuid));
            }).bounds(bx, pt + 80, 95, 16).build();
            addRenderableWidget(kickBtn);
        }

        // Territory Map
        mapBtn = Button.builder(Component.translatable("creraces.screen.territory_map"), b ->
                BoundaryHandler.sendRequestTerritoryData()
        ).bounds(bx, pt + 110, 95, 16).build();
        addRenderableWidget(mapBtn);

        // Clan button (leader only) — Create if no clan, View if already in one
        if (localRank == FactionRank.LEADER) {
            boolean inClan = lastUpdate != null && lastUpdate.clanId != null;
            Component clanLabel = inClan
                    ? Component.translatable("creraces.screen.view_clan")
                    : Component.translatable("creraces.screen.create_clan");
            addRenderableWidget(Button.builder(clanLabel, b -> {
                if (inClan) {
                    BoundaryHandler.sendClanAction(new ClanActionPacket(ClanActionPacket.Action.VIEW));
                } else {
                    BoundaryHandler.sendClanAction(new ClanActionPacket(ClanActionPacket.Action.CREATE));
                }
            }).bounds(bx, pt + 130, 95, 16).build());
        }

        // ── Bottom row ────────────────────────────────────────────────────────
        if (!confirmingLeave) {
            leaveBtn = Button.builder(Component.translatable("creraces.screen.leave"), b -> {
                confirmingLeave = true;
                clearWidgets();
                init();
            }).bounds(pl + 5, pt + PH - 22, 85, 16).build();
            addRenderableWidget(leaveBtn);

            if (localRank == FactionRank.LEADER) {
                disbandBtn = Button.builder(Component.translatable("creraces.screen.disband")
                        .withStyle(ChatFormatting.RED), b -> {
                    BoundaryHandler.sendFactionAction(new FactionActionPacket(FactionActionPacket.Action.DISBAND));
                    onClose();
                }).bounds(pl + 100, pt + PH - 22, 85, 16).build();
                addRenderableWidget(disbandBtn);
            }

            addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                    .bounds(pl + PW - 90, pt + PH - 22, 85, 16).build());
        } else {
            // Leave confirmation: Disband Claims | New Faction | Cancel
            addRenderableWidget(Button.builder(
                    Component.literal("Disband Claims").withStyle(ChatFormatting.RED), b -> {
                BoundaryHandler.sendFactionAction(new FactionActionPacket(FactionActionPacket.Action.LEAVE_DISBAND));
                onClose();
            }).bounds(pl + 5, pt + PH - 22, 85, 16).build());

            addRenderableWidget(Button.builder(
                    Component.literal("New Faction").withStyle(ChatFormatting.GREEN), b -> {
                BoundaryHandler.sendFactionAction(new FactionActionPacket(FactionActionPacket.Action.LEAVE_SPLIT));
                onClose();
            }).bounds(pl + 95, pt + PH - 22, 85, 16).build());

            addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> {
                confirmingLeave = false;
                clearWidgets();
                init();
            }).bounds(pl + 185, pt + PH - 22, 55, 16).build());
        }

        updateActionButtons();
    }

    private void updateActionButtons() {
        UUID localId = minecraft != null && minecraft.player != null ? minecraft.player.getUUID() : null;
        boolean has = selectedMember != null;
        boolean notSelf = has && !selectedMember.uuid.equals(localId);

        if (promoteBtn != null) promoteBtn.active = notSelf
                && selectedMember.rank != FactionRank.LEADER;
        if (demoteBtn  != null) demoteBtn.active  = notSelf
                && selectedMember.rank != FactionRank.MEMBER;
        if (kickBtn    != null) {
            if (notSelf && localRank == FactionRank.LEADER) kickBtn.active = true;
            else if (notSelf && localRank == FactionRank.OFFICER)
                kickBtn.active = selectedMember.rank == FactionRank.MEMBER;
            else kickBtn.active = false;
        }
    }

    // ── Render ─────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        renderBackground(g);

        int cx = this.width / 2;
        int cy = this.height / 2;
        int pl = cx - PW / 2;
        int pt = cy - PH / 2;

        // Panel background
        g.fill(pl, pt, pl + PW, pt + PH, 0xBB000000);
        g.renderOutline(pl, pt, PW, PH, 0xFFAAAAAA);

        // Title + faction name
        String factionTitle = lastUpdate != null ? lastUpdate.factionName : "—";
        g.drawCenteredString(font, Component.literal(factionTitle).withStyle(ChatFormatting.GOLD),
                cx, pt + 5, 0xFFFFFF);

        // Column divider
        int divX = pl + PW - 102;
        g.fill(divX, pt + 14, divX + 1, pt + PH - 25, 0x55FFFFFF);

        // "Members" label
        g.drawString(font, Component.translatable("creraces.screen.members"),
                pl + 5, pt + 38, 0xAAAAAA, false);

        // Member list
        if (lastUpdate != null) {
            int y = pt + 48;
            for (FactionUpdatePacket.MemberInfo m : lastUpdate.members) {
                boolean selected = selectedMember != null && selectedMember.uuid.equals(m.uuid);
                if (selected) g.fill(pl + 3, y - 1, divX - 3, y + 9, 0x44FFFFFF);

                int nameColor = rankColor(m.rank);
                MutableComponent row = Component.literal(m.name + " ")
                        .append(Component.literal("[" + m.rank.name().charAt(0) + "]")
                                .withStyle(rankFormatting(m.rank)));
                g.drawString(font, row, pl + 5, y, nameColor, false);
                y += 11;
                if (y > pt + PH - 30) break;
            }
        }

        super.render(g, mx, my, dt);
    }

    // ── Mouse ──────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (lastUpdate != null) {
            int cx = this.width / 2;
            int cy = this.height / 2;
            int pl = cx - PW / 2;
            int pt = cy - PH / 2;
            int divX = pl + PW - 102;

            if (mx >= pl + 3 && mx < divX - 3) {
                int y = pt + 48;
                for (FactionUpdatePacket.MemberInfo m : lastUpdate.members) {
                    if (my >= y - 1 && my < y + 10) {
                        selectedMember = (selectedMember != null && selectedMember.uuid.equals(m.uuid))
                                ? null : m;
                        updateActionButtons();
                        return true;
                    }
                    y += 11;
                    if (y > pt + PH - 30) break;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static int rankColor(FactionRank rank) {
        return switch (rank) {
            case LEADER -> 0xFFD700;
            case OFFICER -> 0x55FFFF;
            default -> 0xFFFFFF;
        };
    }

    private static ChatFormatting rankFormatting(FactionRank rank) {
        return switch (rank) {
            case LEADER -> ChatFormatting.GOLD;
            case OFFICER -> ChatFormatting.AQUA;
            default -> ChatFormatting.GRAY;
        };
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
