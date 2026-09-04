package mc.sayda.creraces.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.UUID;

/**
 * GUI for managing Race Teams.
 * Features: Member list, Invite list, Invite by name, Friendly Fire toggle.
 */
@SuppressWarnings("null")
public class RaceTeamScreen extends Screen {

    private static volatile java.util.List<mc.sayda.creraces.network.TeamUpdatePacket.MemberInfo> members = new java.util.ArrayList<>();
    private static boolean friendlyFire;
    private static String pendingInviteTeamName = "";
    private static mc.sayda.creraces.network.TeamUpdatePacket.MemberInfo selectedMember = null;

    private EditBox teamNameBox;
    private EditBox invitePlayerBox;
    private Button promoteButton;
    private Button demoteButton;
    private Button kickButton;
    private mc.sayda.creraces.team.RaceTeamManager.Role localRole = mc.sayda.creraces.team.RaceTeamManager.Role.MEMBER;

    public RaceTeamScreen() {
        this(Component.translatable("gui.creraces.team.title"));
    }

    public RaceTeamScreen(Component title) {
        super(title);
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new RaceTeamScreen());
    }

    public static void update(java.util.List<mc.sayda.creraces.network.TeamUpdatePacket.MemberInfo> membersIn,
            boolean friendlyFireIn, String invitedTeamNameIn) {
        members = membersIn;
        friendlyFire = friendlyFireIn;
        pendingInviteTeamName = invitedTeamNameIn;

        if (selectedMember != null) {
            selectedMember = members.stream().filter(m -> m.uuid().equals(selectedMember.uuid())).findFirst()
                    .orElse(null);
        }

        if (Minecraft.getInstance().screen instanceof RaceTeamScreen screen) {
            screen.init(Minecraft.getInstance(), Minecraft.getInstance().getWindow().getGuiScaledWidth(),
                    Minecraft.getInstance().getWindow().getGuiScaledHeight());
        }
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Team Name / Create Team
        teamNameBox = new EditBox(this.font, centerX - 120, centerY - 80, 100, 20,
                Component.translatable("gui.creraces.team.name_field"));
        this.addRenderableWidget(teamNameBox);
        this.addRenderableWidget(Button.builder(Component.translatable("gui.creraces.team.create"), b -> {
            String name = teamNameBox.getValue();
            if (!name.isEmpty()) {
                mc.sayda.creraces.network.BoundaryHandler
                        .sendTeamRequest(new mc.sayda.creraces.network.TeamRequestPacket(
                                mc.sayda.creraces.network.TeamRequestPacket.Action.CREATE, name));
            }
        }).bounds(centerX - 15, centerY - 80, 50, 20).build());

        // Invite Player
        invitePlayerBox = new EditBox(this.font, centerX - 120, centerY - 50, 100, 20,
                Component.translatable("gui.creraces.team.invite_field"));
        this.addRenderableWidget(invitePlayerBox);
        this.addRenderableWidget(Button.builder(Component.translatable("gui.creraces.team.invite"), b -> {
            String name = invitePlayerBox.getValue();
            if (!name.isEmpty()) {
                mc.sayda.creraces.network.BoundaryHandler
                        .sendTeamRequest(new mc.sayda.creraces.network.TeamRequestPacket(
                                mc.sayda.creraces.network.TeamRequestPacket.Action.INVITE, name));
            }
        }).bounds(centerX - 15, centerY - 50, 50, 20).build());

        // Role Management (Leader only)
        UUID localId = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;
        this.localRole = mc.sayda.creraces.team.RaceTeamManager.Role.MEMBER;
        var localMembers = members;
        synchronized (localMembers) {
            for (var m : localMembers) {
                if (m.uuid().equals(localId)) {
                    this.localRole = m.role();
                    break;
                }
            }
        }

        // Friendly Fire Toggle
        boolean canToggleFF = this.localRole == mc.sayda.creraces.team.RaceTeamManager.Role.LEADER
                || this.localRole == mc.sayda.creraces.team.RaceTeamManager.Role.OFFICER;

        Button btn = Button.builder(Component.translatable("gui.creraces.team.friendly_fire",
                Component.translatable(friendlyFire ? "gui.creraces.on" : "gui.creraces.off")), b -> {
                    mc.sayda.creraces.network.BoundaryHandler
                            .sendTeamRequest(new mc.sayda.creraces.network.TeamRequestPacket(
                                    mc.sayda.creraces.network.TeamRequestPacket.Action.TOGGLE_FRIENDLY_FIRE,
                                    ""));
                }).bounds(centerX + 40, centerY - 80, 100, 20)
                .build();
        btn.active = canToggleFF;
        this.addRenderableWidget(btn);

        // Leave Team
        this.addRenderableWidget(Button.builder(Component.translatable("gui.creraces.team.leave"), b -> {
            mc.sayda.creraces.network.BoundaryHandler.sendTeamRequest(new mc.sayda.creraces.network.TeamRequestPacket(
                    mc.sayda.creraces.network.TeamRequestPacket.Action.LEAVE, ""));
        }).bounds(centerX + 40, centerY - 50, 100, 20).build());

        // Join / Accept Invite
        if (!pendingInviteTeamName.isEmpty() && members.isEmpty()) {
            this.addRenderableWidget(
                    Button.builder(Component.translatable("gui.creraces.team.join", pendingInviteTeamName), b -> {
                        mc.sayda.creraces.network.BoundaryHandler
                                .sendTeamRequest(new mc.sayda.creraces.network.TeamRequestPacket(
                                        mc.sayda.creraces.network.TeamRequestPacket.Action.JOIN, ""));
                    }).bounds(centerX - 50, centerY + 20, 100, 20).build());
        }

        if (this.localRole == mc.sayda.creraces.team.RaceTeamManager.Role.LEADER) {
            promoteButton = Button.builder(Component.translatable("gui.creraces.team.promote"), b -> {
                if (selectedMember != null) {
                    mc.sayda.creraces.network.BoundaryHandler
                            .sendTeamRequest(new mc.sayda.creraces.network.TeamRequestPacket(
                                    mc.sayda.creraces.network.TeamRequestPacket.Action.PROMOTE, selectedMember.uuid()));
                }
            }).bounds(centerX + 60, centerY + 20, 70, 20).build();

            demoteButton = Button.builder(Component.translatable("gui.creraces.team.demote"), b -> {
                if (selectedMember != null) {
                    mc.sayda.creraces.network.BoundaryHandler
                            .sendTeamRequest(new mc.sayda.creraces.network.TeamRequestPacket(
                                    mc.sayda.creraces.network.TeamRequestPacket.Action.DEMOTE, selectedMember.uuid()));
                }
            }).bounds(centerX + 60, centerY + 45, 70, 20).build();

            this.addRenderableWidget(promoteButton);
            this.addRenderableWidget(demoteButton);
        }

        if (this.localRole == mc.sayda.creraces.team.RaceTeamManager.Role.LEADER || this.localRole == mc.sayda.creraces.team.RaceTeamManager.Role.OFFICER) {
            kickButton = Button.builder(Component.translatable("gui.creraces.team.kick"), b -> {
                if (selectedMember != null) {
                    mc.sayda.creraces.network.BoundaryHandler
                            .sendTeamRequest(new mc.sayda.creraces.network.TeamRequestPacket(
                                    mc.sayda.creraces.network.TeamRequestPacket.Action.KICK, selectedMember.uuid()));
                }
            }).bounds(centerX + 60, centerY + 70, 70, 20).build();

            this.addRenderableWidget(kickButton);
        }
        updateRoleButtons();

        // Close button
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.minecraft.setScreen(null))
                .bounds(centerX - 50, centerY + 80, 100, 20).build());
    }

    private void updateRoleButtons() {
        boolean hasSelection = selectedMember != null;
        UUID localId = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;
        boolean isSelf = hasSelection && selectedMember.uuid().equals(localId);

        if (promoteButton != null && demoteButton != null) {
            promoteButton.active = hasSelection && !isSelf;
            demoteButton.active = hasSelection && !isSelf
                    && selectedMember.role() != mc.sayda.creraces.team.RaceTeamManager.Role.MEMBER;
        }

        if (kickButton != null) {
            if (hasSelection && !isSelf) {
                if (this.localRole == mc.sayda.creraces.team.RaceTeamManager.Role.LEADER) {
                    kickButton.active = true;
                } else if (this.localRole == mc.sayda.creraces.team.RaceTeamManager.Role.OFFICER) {
                    kickButton.active = selectedMember.role() == mc.sayda.creraces.team.RaceTeamManager.Role.MEMBER;
                } else {
                    kickButton.active = false;
                }
            } else {
                kickButton.active = false;
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Render glassmorphic background plate
        graphics.fill(centerX - 130, centerY - 90, centerX + 150, centerY + 110, 0x88000000);
        graphics.renderOutline(centerX - 130, centerY - 90, 280, 200, 0xFFAAAAAA);

        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, centerX, centerY - 100, 0xFFFFFF);

        // Render member list
        int y = centerY - 20;
        graphics.drawString(this.font, Component.translatable("gui.creraces.team.members"), centerX - 120, y, 0xAAAAAA,
                false);
        y += 12;

        UUID localId = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;

        synchronized (members) {
            for (mc.sayda.creraces.network.TeamUpdatePacket.MemberInfo member : members) {
                if (member.uuid().equals(localId)) {
                    this.localRole = member.role();
                    break;
                }
            }

            for (mc.sayda.creraces.network.TeamUpdatePacket.MemberInfo member : members) {
                MutableComponent text = Component.literal(member.name() + " ");
                String roleKey = "gui.creraces.team.role." + member.role().name().toLowerCase();
                text.append(Component.translatable(roleKey).withStyle(net.minecraft.ChatFormatting.GRAY));

                int color = 0xFFFFFF;
                if (selectedMember != null && selectedMember.uuid().equals(member.uuid())) {
                    color = 0xFFFF55;
                    graphics.fill(centerX - 112, y - 1, centerX + 50, y + 9, 0x44FFFFFF);
                }

                graphics.drawString(this.font, text, centerX - 110, y, color, false);
                y += 10;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (mouseX >= centerX - 110 && mouseX <= centerX + 50) {
            int y = centerY - 8;
            synchronized (members) {
                for (var member : members) {
                    if (mouseY >= y && mouseY < y + 10) {
                        selectedMember = member;
                        updateRoleButtons();
                        return true;
                    }
                    y += 10;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 1.21 Screen.render() draws the background before widgets, which would blur anything this
     * screen has already drawn. The real background is invoked at the top of render() instead, so
     * this stays empty to keep it from being drawn twice.
     */
    @Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }
}
