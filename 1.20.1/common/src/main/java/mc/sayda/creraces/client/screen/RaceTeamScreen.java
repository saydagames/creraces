package mc.sayda.creraces.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * Modern GUI for managing Race Teams.
 * Features: Member list, Invite list, Invite by name, Friendly Fire toggle.
 */
/**
 * Modern GUI for managing Race Teams.
 * Features: Member list, Invite list, Invite by name, Friendly Fire toggle.
 */
@SuppressWarnings("null")
public class RaceTeamScreen extends Screen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation("creraces", "textures/screens/team_bg.png");

    private static java.util.List<mc.sayda.creraces.network.TeamUpdatePacket.MemberInfo> members = new java.util.ArrayList<>();
    private static boolean friendlyFire;
    private static String pendingInviteTeamName = "";

    private EditBox teamNameBox;
    private EditBox invitePlayerBox;

    public RaceTeamScreen() {
        super(Component.translatable("gui.creraces.team.title"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new RaceTeamScreen());
    }

    public static void update(java.util.List<mc.sayda.creraces.network.TeamUpdatePacket.MemberInfo> membersIn,
            boolean friendlyFireIn, String invitedTeamNameIn) {
        members = membersIn;
        friendlyFire = friendlyFireIn;
        pendingInviteTeamName = invitedTeamNameIn;
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

        // Friendly Fire Toggle
        this.addRenderableWidget(
                Button.builder(Component.translatable("gui.creraces.team.friendly_fire",
                        Component.translatable(friendlyFire ? "gui.creraces.on" : "gui.creraces.off")), b -> {
                            mc.sayda.creraces.network.BoundaryHandler
                                    .sendTeamRequest(new mc.sayda.creraces.network.TeamRequestPacket(
                                            mc.sayda.creraces.network.TeamRequestPacket.Action.TOGGLE_FRIENDLY_FIRE,
                                            ""));
                        }).bounds(centerX + 40, centerY - 80, 100, 20).build());

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

        // Close button
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.minecraft.setScreen(null))
                .bounds(centerX - 50, centerY + 80, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
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
        synchronized (members) {
            for (mc.sayda.creraces.network.TeamUpdatePacket.MemberInfo member : members) {
                MutableComponent text = Component.literal(member.name());
                if (member.isLeader()) {
                    text.append(Component.translatable("gui.creraces.team.leader"));
                }
                graphics.drawString(this.font, text, centerX - 110, y, 0xFFFFFF, false);
                y += 10;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
