package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * C2S: Client sends an invite to another player to join their pocket.
 */
public class PocketInvitePacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "pocket_invite");

    private final UUID targetUuid;

    public PocketInvitePacket(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public PocketInvitePacket(FriendlyByteBuf buf) {
        this.targetUuid = buf.readUUID();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(targetUuid);
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> ctxSupplier) {
        var ctx = ctxSupplier.get();
        ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer sender))
                return;

            ServerPlayer target = sender.server.getPlayerList().getPlayer(targetUuid);
            if (target == null) {
                sender.displayClientMessage(Component.translatable("msg.creraces.player_not_found"), true);
                return;
            }

            DataUtils.getVariables(sender).ifPresent(vars -> {
                if (vars.hasPocket()) {
                    int maxInvites = mc.sayda.creraces.config.CreRacesConfig.POCKET_INVITE_MAX.get();
                    if (maxInvites >= 0 && vars.getPocketInvitations().size() >= maxInvites) {
                        sender.displayClientMessage(Component.translatable("msg.creraces.pocket.max_invites_reached", maxInvites)
                                .withStyle(net.minecraft.ChatFormatting.RED), true);
                        return;
                    }

                    // Send a clickable invitation message to the target
                    var inviteMsg = Component.translatable("msg.creraces.pocket.invite_received", sender.getDisplayName())
                            .withStyle(net.minecraft.ChatFormatting.GOLD)
                            .append("\n")
                            .append(Component.translatable("msg.creraces.pocket.invite_click_here", sender.getGameProfile().getName())
                                    .withStyle(s -> s.withColor(net.minecraft.ChatFormatting.YELLOW)
                                            .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                                    net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND,
                                                    "/creraces pocket join " + sender.getGameProfile().getName()))
                                            .withUnderlined(true)));

                    target.sendSystemMessage(inviteMsg);
                    sender.displayClientMessage(
                            Component.translatable("msg.creraces.pocket.invite_success", target.getDisplayName()), true);

                    // Add to invitation list
                    vars.inviteToPocket(target.getUUID());
                } else {
                    sender.displayClientMessage(Component.translatable("msg.creraces.pocket.no_pocket_to_manage")
                            .withStyle(net.minecraft.ChatFormatting.RED), true);
                }
            });
        });
    }
}
