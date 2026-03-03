package mc.sayda.creraces.network;

import com.mojang.logging.LogUtils;
import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Manages the boundaries between server and client.
 * Yukari would be proud.
 */
public class BoundaryHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void registerC2S() {
        // Register server-bound packets
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, EquipAbilityPacket.ID, (buf, context) -> {
            var pkt = new EquipAbilityPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SetRacePacket.ID, (buf, context) -> {
            var pkt = new SetRacePacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CastAbilityPacket.ID, (buf, context) -> {
            var pkt = new CastAbilityPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SetCustomizationPacket.ID, (buf, context) -> {
            var pkt = new SetCustomizationPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, OpenMenuPacket.ID, (buf, context) -> {
            var pkt = new OpenMenuPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, TeamRequestPacket.ID, (buf, context) -> {
            var pkt = new TeamRequestPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RequestSyncPacket.ID, (buf, context) -> {
            var pkt = new RequestSyncPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, mc.sayda.creraces.network.MiniPlacePacket.ID,
                (buf, context) -> {
                    var pkt = new mc.sayda.creraces.network.MiniPlacePacket(buf);
                    pkt.handle(() -> context);
                });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, mc.sayda.creraces.network.MiniRemovePacket.ID,
                (buf, context) -> {
                    var pkt = new mc.sayda.creraces.network.MiniRemovePacket(buf);
                    pkt.handle(() -> context);
                });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, mc.sayda.creraces.network.MiniUsePacket.ID,
                (buf, context) -> {
                    var pkt = new mc.sayda.creraces.network.MiniUsePacket(buf);
                    pkt.handle(() -> context);
                });

        LOGGER.info("Yukari has established the server network boundaries.");
    }

    public static void registerS2C() {
        // Register client-bound packets
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncRacesPacket.ID, (buf, context) -> {
            var pkt = new SyncRacesPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncAbilitiesPacket.ID, (buf, context) -> {
            var pkt = new SyncAbilitiesPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncIncidentPacket.ID, (buf, context) -> {
            var pkt = new SyncIncidentPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OpenSelectionScreenPacket.ID, (buf, context) -> {
            var pkt = new OpenSelectionScreenPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OpenMirrorScreenPacket.ID, (buf, context) -> {
            var pkt = new OpenMirrorScreenPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OpenDebugScreenPacket.ID, (buf, context) -> {
            var pkt = new OpenDebugScreenPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OpenSkillWheelPacket.ID, (buf, context) -> {
            var pkt = new OpenSkillWheelPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OpenTeamScreenPacket.ID, (buf, context) -> {
            var pkt = new OpenTeamScreenPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, TeamUpdatePacket.ID, (buf, context) -> {
            var pkt = new TeamUpdatePacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ShowItemAnimationPacket.ID, (buf, context) -> {
            var pkt = new ShowItemAnimationPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ClearRemoteCachePacket.ID, (buf, context) -> {
            var pkt = new ClearRemoteCachePacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncBeamPacket.ID, (buf, context) -> {
            var pkt = new SyncBeamPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncAnimationPacket.ID, (buf, context) -> {
            var pkt = new SyncAnimationPacket(buf);
            pkt.handle(() -> context);
        });

        LOGGER.info("Yukari has established the client network boundaries.");
    }

    public static void sendSetRace(SetRacePacket pkt) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        pkt.toBytes(buf);
        NetworkManager.sendToServer(SetRacePacket.ID, buf);
    }

    public static void sendOpenSelection(ServerPlayer player) {
        send(player, OpenSelectionScreenPacket.ID, buf -> {
        });
    }

    public static void sendOpenMirror(ServerPlayer player) {
        send(player, OpenMirrorScreenPacket.ID, buf -> {
        });
    }

    public static void sendOpenDebug(ServerPlayer player) {
        send(player, OpenDebugScreenPacket.ID, buf -> {
        });
    }

    public static void sendOpenSkillWheel(ServerPlayer player) {
        send(player, OpenSkillWheelPacket.ID, buf -> {
        });
    }

    public static void sendOpenTeamGUI(ServerPlayer player) {
        send(player, OpenTeamScreenPacket.ID, buf -> {
        });
    }

    public static void sendTeamUpdate(ServerPlayer player, TeamUpdatePacket pkt) {
        send(player, TeamUpdatePacket.ID, pkt::encode);
    }

    public static void sendTeamRequest(TeamRequestPacket pkt) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        pkt.encode(buf);
        NetworkManager.sendToServer(TeamRequestPacket.ID, buf);
    }

    public static void sendIncidentToAll(SyncIncidentPacket pkt) {
        var server = dev.architectury.utils.GameInstance.getServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                send(player, SyncIncidentPacket.ID, pkt::encode);
            }
        }
    }

    public static void sendSyncRequest() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        new RequestSyncPacket().encode(buf);
        NetworkManager.sendToServer(RequestSyncPacket.ID, buf);
    }

    public static void syncRacesToPlayer(ServerPlayer player, SyncRacesPacket pkt) {
        send(player, SyncRacesPacket.ID, pkt::encode);
    }

    public static void syncAbilitiesToPlayer(ServerPlayer player, SyncAbilitiesPacket pkt) {
        send(player, SyncAbilitiesPacket.ID, pkt::encode);
    }

    public static void sendIncidentToPlayer(Player player, SyncIncidentPacket pkt) {
        if (player instanceof ServerPlayer sp) {
            send(sp, SyncIncidentPacket.ID, pkt::encode);
        }
    }

    public static void sendItemAnimation(ServerPlayer player, ResourceLocation itemId) {
        send(player, ShowItemAnimationPacket.ID, buf -> buf.writeResourceLocation(itemId));
    }

    public static void broadcastClearCache() {
        var server = dev.architectury.utils.GameInstance.getServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                send(player, ClearRemoteCachePacket.ID, buf -> {
                });
            }
        }
    }

    public static void sendEquipAbility(EquipAbilityPacket pkt) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        pkt.encode(buf);
        NetworkManager.sendToServer(EquipAbilityPacket.ID, buf);
    }

    public static void sendCastAbility(CastAbilityPacket pkt) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        pkt.encode(buf);
        NetworkManager.sendToServer(CastAbilityPacket.ID, buf);
    }

    public static void sendSyncBeam(ServerPlayer player, SyncBeamPacket pkt) {
        send(player, SyncBeamPacket.ID, pkt::encode);
    }

    public static void sendSyncAnimation(ServerPlayer player, SyncAnimationPacket pkt) {
        send(player, SyncAnimationPacket.ID, pkt::encode);
    }

    public static void sendSetCustomization(SetCustomizationPacket pkt) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        pkt.encode(buf);
        NetworkManager.sendToServer(SetCustomizationPacket.ID, buf);
    }

    public static void sendOpenMenu() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        new OpenMenuPacket().encode(buf);
        NetworkManager.sendToServer(OpenMenuPacket.ID, buf);
    }

    public static void sendMiniPlace(mc.sayda.creraces.network.MiniPlacePacket pkt) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        pkt.encode(buf);
        NetworkManager.sendToServer(MiniPlacePacket.ID, buf);
    }

    public static void sendMiniRemove(mc.sayda.creraces.network.MiniRemovePacket pkt) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        pkt.encode(buf);
        NetworkManager.sendToServer(MiniRemovePacket.ID, buf);
    }

    public static void sendMiniUse(mc.sayda.creraces.network.MiniUsePacket pkt) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        pkt.encode(buf);
        NetworkManager.sendToServer(mc.sayda.creraces.network.MiniUsePacket.ID, buf);
    }

    /**
     * Resyncs all variables for a target player to a specific recipient.
     */
    public static void resyncVariables(Player target, Player recipient) {
        DataUtils.getVariables(target).ifPresent(vars -> {
            var pkt = new SyncIncidentPacket(target.getUUID(), vars.serialize());
            sendIncidentToPlayer(recipient, pkt);
        });
    }

    /**
     * Resyncs a player's variables to everyone tracking them.
     */
    public static void resyncForAllTrackers(Player player) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            var pkt = new SyncIncidentPacket(player.getUUID(), vars.serialize());
            // In Forge this would be called by tracking events, in Architectury we can send
            // to all as a fallback
            sendIncidentToAll(pkt);
        });
    }

    private static void send(ServerPlayer player, net.minecraft.resources.ResourceLocation id,
            java.util.function.Consumer<net.minecraft.network.FriendlyByteBuf> encoder) {
        net.minecraft.network.FriendlyByteBuf buf = new net.minecraft.network.FriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer());
        encoder.accept(buf);
        NetworkManager.sendToPlayer(player, id, buf);
    }
}
