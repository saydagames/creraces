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
                sender.displayClientMessage(Component.translatable("message.creraces.player_not_found"), true);
                return;
            }

            DataUtils.getVariables(sender).ifPresent(vars -> {
                if (vars.hasPocket()) {
                    // Send a clickable invitation message to the target
                    var inviteMsg = Component.translatable("message.creraces.pocket_invite_received", sender.getName())
                            .withStyle(net.minecraft.ChatFormatting.GOLD)
                            .append("\n")
                            .append(Component.translatable("message.creraces.pocket_invite_click_here")
                                    .withStyle(s -> s.withColor(net.minecraft.ChatFormatting.YELLOW)
                                            .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                                    net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND,
                                                    "/creraces pocket join " + sender.getGameProfile().getName()))
                                            .withUnderlined(true)));

                    target.sendSystemMessage(inviteMsg);
                    sender.displayClientMessage(
                            Component.translatable("message.creraces.pocket_invite_sent", target.getName()), true);

                    // Also ensure the target is added to the sender's invitation list
                    vars.inviteToPocket(target.getUUID());
                } else {
                    sender.displayClientMessage(Component.translatable("message.creraces.no_pocket_error")
                            .withStyle(net.minecraft.ChatFormatting.RED), true);
                }
            });
        });
    }
}
