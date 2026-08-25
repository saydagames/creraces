package mc.sayda.creraces.network;

import com.mojang.logging.LogUtils;
import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import mc.sayda.creraces.world.inventory.MirrorMenu;

/**
 * Manages the boundaries between server and client.
 */
public class BoundaryHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static FriendlyByteBuf newBuf() {
        return new FriendlyByteBuf(java.util.Objects.requireNonNull(Unpooled.buffer()));
    }

    public static void init() {
        registerC2S();
        LOGGER.info("Server network boundaries registered.");
    }

    public static void registerC2S() {
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

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, OpenEssenceBeltPacket.ID, (buf, context) -> {
            var pkt = new OpenEssenceBeltPacket(buf);
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

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, DebugActionPacket.ID, (buf, context) -> {
            var pkt = new DebugActionPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, MiniPlacePacket.ID,
                (buf, context) -> {
                    var pkt = new MiniPlacePacket(buf);
                    pkt.handle(() -> context);
                });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, MiniRemovePacket.ID,
                (buf, context) -> {
                    var pkt = new MiniRemovePacket(buf);
                    pkt.handle(() -> context);
                });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, MiniUsePacket.ID,
                (buf, context) -> {
                    var pkt = new MiniUsePacket(buf);
                    pkt.handle(() -> context);
                });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PocketInvitePacket.ID, (buf, context) -> {
            var pkt = new PocketInvitePacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, UpdateGStatePacket.ID, (buf, context) -> {
            var pkt = new UpdateGStatePacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, DoubleJumpPacket.ID, (buf, context) -> {
            var pkt = new DoubleJumpPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RequestMirrorPacket.ID, (buf, context) -> {
            var pkt = new RequestMirrorPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ClaimChunkPacket.ID, (buf, context) -> {
            var pkt = new ClaimChunkPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ClanActionPacket.ID, (buf, context) -> {
            var pkt = new ClanActionPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RequestTerritoryDataPacket.ID, (buf, context) -> {
            var pkt = new RequestTerritoryDataPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PlaceEssencePacket.ID, (buf, context) -> {
            var pkt = new PlaceEssencePacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RemoveEssencePacket.ID, (buf, context) -> {
            var pkt = new RemoveEssencePacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CraftScrollPacket.ID, (buf, context) -> {
            var pkt = new CraftScrollPacket(buf);
            pkt.handle(() -> context);
        });
    }

    public static void registerS2C() {
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

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OpenDebugScreenPacket.ID, (buf, context) -> {
            var pkt = new OpenDebugScreenPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OpenSkillWheelPacket.ID, (buf, context) -> {
            var pkt = new OpenSkillWheelPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OpenHUDEditorPacket.ID, (buf, context) -> {
            var pkt = new OpenHUDEditorPacket(buf);
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

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncTetherPacket.ID, (buf, context) -> {
            var pkt = new SyncTetherPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, StopSoundPacket.ID, (buf, context) -> {
            var pkt = new StopSoundPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OpenClanManagePacket.ID, (buf, context) -> {
            var pkt = new OpenClanManagePacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OpenTerritoryMapPacket.ID, (buf, context) -> {
            var pkt = new OpenTerritoryMapPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ClaimResponsePacket.ID, (buf, context) -> {
            var pkt = new ClaimResponsePacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, TerritoryDataPacket.ID, (buf, context) -> {
            var pkt = new TerritoryDataPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, TerrainSamplePacket.ID, (buf, context) -> {
            var pkt = new TerrainSamplePacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ClanUpdatePacket.ID, (buf, context) -> {
            var pkt = new ClanUpdatePacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncGamerulePacket.ID, (buf, context) -> {
            var pkt = new SyncGamerulePacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncHexGridPacket.ID, (buf, context) -> {
            var pkt = new SyncHexGridPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ResearchResultPacket.ID, (buf, context) -> {
            var pkt = new ResearchResultPacket(buf);
            pkt.handle(() -> context);
        });

        LOGGER.info("Client network boundaries registered.");
    }

    public static void sendSetRace(SetRacePacket pkt) {
        FriendlyByteBuf buf = newBuf();
        pkt.encode(buf);
        NetworkManager.sendToServer(SetRacePacket.ID, buf);
    }

    public static void sendOpenSelection(ServerPlayer player) {
        send(player, OpenSelectionScreenPacket.ID, buf -> {
        });
    }

    public static void sendOpenMirror(ServerPlayer player) {
        MenuRegistry.openExtendedMenu(player, new ExtendedMenuProvider() {
            @Override
            public void saveExtraData(FriendlyByteBuf buf) {
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.creraces.mirror");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
                return new MirrorMenu(id, inventory, null);
            }
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

    public static void sendOpenHUDEditor(ServerPlayer player) {
        send(player, OpenHUDEditorPacket.ID, buf -> {
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
        FriendlyByteBuf buf = newBuf();
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
        FriendlyByteBuf buf = newBuf();
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
        send(player, ShowItemAnimationPacket.ID, buf -> buf.writeResourceLocation(java.util.Objects.requireNonNull(itemId)));
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
        FriendlyByteBuf buf = newBuf();
        pkt.encode(buf);
        NetworkManager.sendToServer(EquipAbilityPacket.ID, buf);
    }

    public static void sendCastAbility(CastAbilityPacket pkt) {
        FriendlyByteBuf buf = newBuf();
        pkt.encode(buf);
        NetworkManager.sendToServer(CastAbilityPacket.ID, buf);
    }

    public static void sendSyncBeam(ServerPlayer player, SyncBeamPacket pkt) {
        send(player, SyncBeamPacket.ID, pkt::encode);
    }

    public static void sendSyncAnimation(ServerPlayer player, SyncAnimationPacket pkt) {
        send(player, SyncAnimationPacket.ID, pkt::encode);
    }

    public static void sendStopSound(ServerPlayer player, ResourceLocation soundId,
            net.minecraft.sounds.SoundSource source) {
        var pkt = new StopSoundPacket(soundId, source);
        send(player, StopSoundPacket.ID, pkt::encode);
    }

    public static void broadcastStopSound(Player player, ResourceLocation soundId,
            net.minecraft.sounds.SoundSource source) {
        var pkt = new StopSoundPacket(soundId, source);
        sendToTrackers(player, StopSoundPacket.ID, pkt::encode);
        // Also send to the player themselves
        if (player instanceof ServerPlayer sp) {
            sendStopSound(sp, soundId, source);
        }
    }

    public static void sendSetCustomization(SetCustomizationPacket pkt) {
        FriendlyByteBuf buf = newBuf();
        pkt.encode(buf);
        NetworkManager.sendToServer(SetCustomizationPacket.ID, buf);
    }

    public static void sendOpenMenu() {
        FriendlyByteBuf buf = newBuf();
        new OpenMenuPacket().encode(buf);
        NetworkManager.sendToServer(OpenMenuPacket.ID, buf);
    }

    public static void sendOpenEssenceBelt() {
        FriendlyByteBuf buf = newBuf();
        new OpenEssenceBeltPacket().encode(buf);
        NetworkManager.sendToServer(OpenEssenceBeltPacket.ID, buf);
    }

    public static void sendGStateUpdate(int gState) {
        FriendlyByteBuf buf = newBuf();
        new UpdateGStatePacket(gState).encode(buf);
        NetworkManager.sendToServer(UpdateGStatePacket.ID, buf);
    }

    public static void sendDoubleJump() {
        FriendlyByteBuf buf = newBuf();
        new DoubleJumpPacket().encode(buf);
        NetworkManager.sendToServer(DoubleJumpPacket.ID, buf);
    }

    public static void sendDebugAction(String action, String key, String value) {
        FriendlyByteBuf buf = newBuf();
        new DebugActionPacket(action, key, value).encode(buf);
        NetworkManager.sendToServer(DebugActionPacket.ID, buf);
    }

    public static void sendMiniPlace(MiniPlacePacket pkt) {
        FriendlyByteBuf buf = newBuf();
        pkt.encode(buf);
        NetworkManager.sendToServer(MiniPlacePacket.ID, buf);
    }

    public static void sendMiniRemove(MiniRemovePacket pkt) {
        FriendlyByteBuf buf = newBuf();
        pkt.encode(buf);
        NetworkManager.sendToServer(MiniRemovePacket.ID, buf);
    }

    public static void sendMiniUse(MiniUsePacket pkt) {
        FriendlyByteBuf buf = newBuf();
        pkt.encode(buf);
        NetworkManager.sendToServer(MiniUsePacket.ID, buf);
    }

    /**
     * Resyncs all variables for a target player to a specific recipient.
     * fullSync = true includes resources (use for joins, respawn, casts).
     * fullSync = false excludes resources (use for periodic ticks - client
     * predicts).
     */
    public static void resyncVariables(Player target, Player recipient, boolean fullSync) {
        DataUtils.getVariables(target).ifPresent(vars -> {
            CompoundTag tag = vars.serialize(fullSync);

            // Also sync persistent data from IPersistentDataAccessor
            if (target instanceof IPersistentDataAccessor accessor) {
                CompoundTag persistentData = accessor.creraces$getPersistentData();
                if (!persistentData.isEmpty()) {
                    tag.put("creraces:persistent_data", java.util.Objects.requireNonNull(persistentData.copy()));
                }
            }

            var pkt = new SyncIncidentPacket(target.getUUID(), tag);
            sendIncidentToPlayer(recipient, pkt);
        });
    }

    /**
     * Convenience overload - defaults to full sync (safe for all explicit events).
     */
    public static void resyncVariables(Player target, Player recipient) {
        resyncVariables(target, recipient, true);
    }

    /**
     * Resyncs a player's variables to everyone tracking them.
     */
    public static void resyncForAllTrackers(Player player) {
        resyncForAllTrackers(player, true);
    }

    public static void resyncForAllTrackers(Player player, boolean fullSync) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            var pkt = new SyncIncidentPacket(player.getUUID(), vars.serialize(fullSync));
            sendToTrackers(player, SyncIncidentPacket.ID, pkt::encode);
        });
    }

    public static void sendToTrackers(Player entity, ResourceLocation id,
            java.util.function.Consumer<net.minecraft.network.FriendlyByteBuf> encoder) {
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel))
            return;

        var players = serverLevel.getServer().getPlayerList().getPlayers();
        int syncDist = mc.sayda.creraces.config.CreRacesConfig.VISUAL_SYNC_DISTANCE.get();
        double syncDistSqr = syncDist * syncDist;

        for (net.minecraft.server.level.ServerPlayer p : players) {
            if (p.level().dimension() == entity.level().dimension() && p.distanceToSqr(entity) < syncDistSqr) {
                send(p, id, encoder);
            }
        }
    }

    // ── Territory: S2C senders ───────────────────────────────────────────────
    public static void sendOpenClanManage(ServerPlayer player) {
        send(player, OpenClanManagePacket.ID, buf -> {});
    }

    public static void sendOpenTerritoryMap(ServerPlayer player) {
        send(player, OpenTerritoryMapPacket.ID, buf -> {});
    }

    public static void sendClaimResponse(ServerPlayer player, ClaimResponsePacket pkt) {
        send(player, ClaimResponsePacket.ID, pkt::encode);
    }

    public static void sendTerritoryData(ServerPlayer player, TerritoryDataPacket pkt) {
        send(player, TerritoryDataPacket.ID, pkt::encode);
    }

    public static void sendTerrainSample(ServerPlayer player, TerrainSamplePacket pkt) {
        send(player, TerrainSamplePacket.ID, pkt::encode);
    }

    // ── Territory: C2S senders ───────────────────────────────────────────────
    public static void sendClaimChunk(ClaimChunkPacket pkt) {
        FriendlyByteBuf buf = newBuf();
        pkt.encode(buf);
        NetworkManager.sendToServer(ClaimChunkPacket.ID, buf);
    }

    public static void sendClanAction(ClanActionPacket pkt) {
        FriendlyByteBuf buf = newBuf();
        pkt.encode(buf);
        NetworkManager.sendToServer(ClanActionPacket.ID, buf);
    }

    public static void sendRequestTerritoryData() {
        FriendlyByteBuf buf = newBuf();
        new RequestTerritoryDataPacket().encode(buf);
        NetworkManager.sendToServer(RequestTerritoryDataPacket.ID, buf);
    }

    public static void sendClanUpdate(net.minecraft.server.level.ServerPlayer player, ClanUpdatePacket pkt) {
        send(player, ClanUpdatePacket.ID, pkt::encode);
    }

    public static void syncHexGrid(ServerPlayer player, mc.sayda.creraces.block.entity.ResearchTableBlockEntity be) {
        send(player, SyncHexGridPacket.ID, new SyncHexGridPacket(be.getHexGrid())::encode);
    }

    public static void sendResearchResult(ServerPlayer player, ResearchResultPacket pkt) {
        send(player, ResearchResultPacket.ID, pkt::encode);
    }

    public static void sendPlaceEssence(PlaceEssencePacket pkt) {
        FriendlyByteBuf buf = newBuf();
        pkt.encode(buf);
        NetworkManager.sendToServer(PlaceEssencePacket.ID, buf);
    }

    public static void sendRemoveEssence(RemoveEssencePacket pkt) {
        FriendlyByteBuf buf = newBuf();
        pkt.encode(buf);
        NetworkManager.sendToServer(RemoveEssencePacket.ID, buf);
    }

    public static void sendCraftScroll(CraftScrollPacket pkt) {
        FriendlyByteBuf buf = newBuf();
        pkt.encode(buf);
        NetworkManager.sendToServer(CraftScrollPacket.ID, buf);
    }

    public static void broadcastSpiritFlameGamerule(boolean value) {
        var server = dev.architectury.utils.GameInstance.getServer();
        if (server != null) {
            var pkt = new SyncGamerulePacket(value);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                send(player, SyncGamerulePacket.ID, pkt::encode);
            }
        }
    }

    private static void send(ServerPlayer player, net.minecraft.resources.ResourceLocation id,
            java.util.function.Consumer<net.minecraft.network.FriendlyByteBuf> encoder) {
        FriendlyByteBuf buf = newBuf();
        encoder.accept(buf);
        NetworkManager.sendToPlayer(player, id, buf);
    }
}
