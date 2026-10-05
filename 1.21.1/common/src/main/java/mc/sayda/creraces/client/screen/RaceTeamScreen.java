package mc.sayda.creraces.client.screen;

import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.TeamRequestPacket;
import mc.sayda.creraces.network.TeamUpdatePacket.MemberInfo;
import mc.sayda.creraces.team.RaceTeamManager.Role;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Race team management: create, invite, join and leave, toggle friendly fire, and promote, demote
 * or kick the selected member depending on the local player's role.
 */
@SuppressWarnings("null")
public class RaceTeamScreen extends Screen {
    private static final int MEMBER_ROW_HEIGHT = 10;

    // Pushed by TeamUpdatePacket on the client thread; kept across screen instances.
    private static List<MemberInfo> members = new ArrayList<>();
    private static boolean friendlyFire;
    private static String pendingInviteTeamName = "";
    private static MemberInfo selectedMember = null;

    private EditBox teamNameBox;
    private EditBox invitePlayerBox;
    private Button promoteButton;
    private Button demoteButton;
    private Button kickButton;
    private Role localRole = Role.MEMBER;

    public RaceTeamScreen() {
        this(Component.translatable("gui.creraces.team.title"));
    }

    public RaceTeamScreen(Component title) {
        super(title);
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new RaceTeamScreen());
    }

    public static void update(List<MemberInfo> membersIn, boolean friendlyFireIn, String invitedTeamNameIn) {
        members = membersIn;
        friendlyFire = friendlyFireIn;
        pendingInviteTeamName = invitedTeamNameIn;

        if (selectedMember != null) {
            UUID selectedId = selectedMember.uuid();
            selectedMember = members.stream().filter(m -> m.uuid().equals(selectedId)).findFirst().orElse(null);
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof RaceTeamScreen screen) {
            screen.init(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        }
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        teamNameBox = new EditBox(this.font, centerX - 120, centerY - 80, 100, 20,
                Component.translatable("gui.creraces.team.name_field"));
        this.addRenderableWidget(teamNameBox);
        this.addRenderableWidget(Button.builder(Component.translatable("gui.creraces.team.create"),
                b -> sendIfNotEmpty(TeamRequestPacket.Action.CREATE, teamNameBox.getValue()))
                .bounds(centerX - 15, centerY - 80, 50, 20).build());

        invitePlayerBox = new EditBox(this.font, centerX - 120, centerY - 50, 100, 20,
                Component.translatable("gui.creraces.team.invite_field"));
        this.addRenderableWidget(invitePlayerBox);
        this.addRenderableWidget(Button.builder(Component.translatable("gui.creraces.team.invite"),
                b -> sendIfNotEmpty(TeamRequestPacket.Action.INVITE, invitePlayerBox.getValue()))
                .bounds(centerX - 15, centerY - 50, 50, 20).build());

        this.localRole = findLocalRole();

        Button friendlyFireButton = Button.builder(Component.translatable("gui.creraces.team.friendly_fire",
                        Component.translatable(friendlyFire ? "gui.creraces.on" : "gui.creraces.off")),
                        b -> send(TeamRequestPacket.Action.TOGGLE_FRIENDLY_FIRE, ""))
                .bounds(centerX + 40, centerY - 80, 100, 20).build();
        friendlyFireButton.active = localRole == Role.LEADER || localRole == Role.OFFICER;
        this.addRenderableWidget(friendlyFireButton);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.creraces.team.leave"),
                b -> send(TeamRequestPacket.Action.LEAVE, ""))
                .bounds(centerX + 40, centerY - 50, 100, 20).build());

        if (!pendingInviteTeamName.isEmpty() && members.isEmpty()) {
            Component joinLabel = Component.translatable("gui.creraces.team.join", pendingInviteTeamName);
            this.addRenderableWidget(Button.builder(joinLabel, b -> send(TeamRequestPacket.Action.JOIN, ""))
                    .bounds(centerX - 50, centerY + 20, 100, 20).build());
        }

        if (localRole == Role.LEADER) {
            promoteButton = this.addRenderableWidget(memberActionButton("gui.creraces.team.promote",
                    TeamRequestPacket.Action.PROMOTE, centerX + 60, centerY + 20));
            demoteButton = this.addRenderableWidget(memberActionButton("gui.creraces.team.demote",
                    TeamRequestPacket.Action.DEMOTE, centerX + 60, centerY + 45));
        }
        if (localRole == Role.LEADER || localRole == Role.OFFICER) {
            kickButton = this.addRenderableWidget(memberActionButton("gui.creraces.team.kick",
                    TeamRequestPacket.Action.KICK, centerX + 60, centerY + 70));
        }
        updateRoleButtons();

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.minecraft.setScreen(null))
                .bounds(centerX - 50, centerY + 80, 100, 20).build());
    }

    /** A button that sends an action targeting the selected member, if any. */
    private Button memberActionButton(String labelKey, TeamRequestPacket.Action action, int x, int y) {
        return Button.builder(Component.translatable(labelKey), b -> {
            if (selectedMember != null) {
                BoundaryHandler.sendTeamRequest(new TeamRequestPacket(action, selectedMember.uuid()));
            }
        }).bounds(x, y, 70, 20).build();
    }

    private static void send(TeamRequestPacket.Action action, String data) {
        BoundaryHandler.sendTeamRequest(new TeamRequestPacket(action, data));
    }

    private static void sendIfNotEmpty(TeamRequestPacket.Action action, String data) {
        if (!data.isEmpty()) {
            send(action, data);
        }
    }

    private static Role findLocalRole() {
        UUID localId = localPlayerId();
        for (MemberInfo member : members) {
            if (member.uuid().equals(localId)) {
                return member.role();
            }
        }
        return Role.MEMBER;
    }

    @Nullable
    private static UUID localPlayerId() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null ? mc.player.getUUID() : null;
    }

    private void updateRoleButtons() {
        boolean hasSelection = selectedMember != null;
        boolean isSelf = hasSelection && selectedMember.uuid().equals(localPlayerId());
        boolean canTarget = hasSelection && !isSelf;

        if (promoteButton != null && demoteButton != null) {
            promoteButton.active = canTarget;
            demoteButton.active = canTarget && selectedMember.role() != Role.MEMBER;
        }
        if (kickButton != null) {
            // Officers may only kick plain members; the leader may kick anyone.
            kickButton.active = canTarget && (localRole == Role.LEADER
                    || (localRole == Role.OFFICER && selectedMember.role() == Role.MEMBER));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        graphics.fill(centerX - 130, centerY - 90, centerX + 150, centerY + 110, 0x88000000);
        graphics.renderOutline(centerX - 130, centerY - 90, 280, 200, 0xFFAAAAAA);

        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, centerX, centerY - 100, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable("gui.creraces.team.members"), centerX - 120,
                centerY - 20, 0xAAAAAA, false);

        int y = memberListTop();
        for (MemberInfo member : members) {
            MutableComponent text = Component.literal(member.name() + " ");
            String roleKey = "gui.creraces.team.role." + member.role().name().toLowerCase(Locale.ROOT);
            text.append(Component.translatable(roleKey).withStyle(ChatFormatting.GRAY));

            int color = 0xFFFFFF;
            if (selectedMember != null && selectedMember.uuid().equals(member.uuid())) {
                color = 0xFFFF55;
                graphics.fill(centerX - 112, y - 1, centerX + 50, y + 9, 0x44FFFFFF);
            }
            graphics.drawString(this.font, text, centerX - 110, y, color, false);
            y += MEMBER_ROW_HEIGHT;
        }
    }

    private int memberListTop() {
        return this.height / 2 - 8;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;
        if (mouseX >= centerX - 110 && mouseX <= centerX + 50) {
            int y = memberListTop();
            for (MemberInfo member : members) {
                if (mouseY >= y && mouseY < y + MEMBER_ROW_HEIGHT) {
                    selectedMember = member;
                    updateRoleButtons();
                    return true;
                }
                y += MEMBER_ROW_HEIGHT;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Drawn at the top of render() instead, so Screen.render() does not blur over the panel.
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }
}
