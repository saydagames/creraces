package mc.sayda.creraces.network;

import com.mojang.logging.LogUtils;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.GameInstance;
import io.netty.buffer.Unpooled;
import mc.sayda.creraces.block.entity.ResearchTableBlockEntity;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Registers every mod packet and wraps each send, so callers never deal with buffers or channel ids. */
@SuppressWarnings("null")
public class BoundaryHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Consumer<FriendlyByteBuf> NO_PAYLOAD = buf -> {
    };

    private static FriendlyByteBuf newBuf() {
        return new FriendlyByteBuf(Objects.requireNonNull(Unpooled.buffer()));
    }

    public static void init() {
        registerC2S();
        LOGGER.info("Server network boundaries registered.");
    }

    public static void registerC2S() {
        receive(NetworkManager.Side.C2S, EquipAbilityPacket.ID, EquipAbilityPacket::new, EquipAbilityPacket::handle);
        receive(NetworkManager.Side.C2S, SetRacePacket.ID, SetRacePacket::new, SetRacePacket::handle);
        receive(NetworkManager.Side.C2S, CastAbilityPacket.ID, CastAbilityPacket::new, CastAbilityPacket::handle);
        receive(NetworkManager.Side.C2S, SetCustomizationPacket.ID, SetCustomizationPacket::new, SetCustomizationPacket::handle);
        receive(NetworkManager.Side.C2S, OpenEssenceBeltPacket.ID, OpenEssenceBeltPacket::new, OpenEssenceBeltPacket::handle);
        receive(NetworkManager.Side.C2S, TeamRequestPacket.ID, TeamRequestPacket::new, TeamRequestPacket::handle);
        receive(NetworkManager.Side.C2S, RequestSyncPacket.ID, RequestSyncPacket::new, RequestSyncPacket::handle);
        receive(NetworkManager.Side.C2S, DebugActionPacket.ID, DebugActionPacket::new, DebugActionPacket::handle);
        receive(NetworkManager.Side.C2S, MiniPlacePacket.ID, MiniPlacePacket::new, MiniPlacePacket::handle);
        receive(NetworkManager.Side.C2S, MiniRemovePacket.ID, MiniRemovePacket::new, MiniRemovePacket::handle);
        receive(NetworkManager.Side.C2S, MiniUsePacket.ID, MiniUsePacket::new, MiniUsePacket::handle);
        receive(NetworkManager.Side.C2S, UpdateGStatePacket.ID, UpdateGStatePacket::new, UpdateGStatePacket::handle);
        receive(NetworkManager.Side.C2S, DoubleJumpPacket.ID, DoubleJumpPacket::new, DoubleJumpPacket::handle);
        receive(NetworkManager.Side.C2S, ClaimChunkPacket.ID, ClaimChunkPacket::new, ClaimChunkPacket::handle);
        receive(NetworkManager.Side.C2S, ClanActionPacket.ID, ClanActionPacket::new, ClanActionPacket::handle);
        receive(NetworkManager.Side.C2S, RequestTerritoryDataPacket.ID, RequestTerritoryDataPacket::new, RequestTerritoryDataPacket::handle);
        receive(NetworkManager.Side.C2S, PlaceEssencePacket.ID, PlaceEssencePacket::new, PlaceEssencePacket::handle);
        receive(NetworkManager.Side.C2S, RemoveEssencePacket.ID, RemoveEssencePacket::new, RemoveEssencePacket::handle);
        receive(NetworkManager.Side.C2S, CraftScrollPacket.ID, CraftScrollPacket::new, CraftScrollPacket::handle);
        receive(NetworkManager.Side.C2S, TakeQuestPacket.ID, TakeQuestPacket::new, TakeQuestPacket::handle);
        receive(NetworkManager.Side.C2S, AbandonQuestPacket.ID, AbandonQuestPacket::new, AbandonQuestPacket::handle);
        receive(NetworkManager.Side.C2S, ShareWaypointPacket.ID, ShareWaypointPacket::new, ShareWaypointPacket::handle);
    }

    public static void registerS2C() {
        receive(NetworkManager.Side.S2C, SyncRacesPacket.ID, SyncRacesPacket::new, SyncRacesPacket::handle);
        receive(NetworkManager.Side.S2C, SyncAbilitiesPacket.ID, SyncAbilitiesPacket::new, SyncAbilitiesPacket::handle);
        receive(NetworkManager.Side.S2C, SyncIncidentPacket.ID, SyncIncidentPacket::new, SyncIncidentPacket::handle);
        receive(NetworkManager.Side.S2C, OpenSelectionScreenPacket.ID, OpenSelectionScreenPacket::new, OpenSelectionScreenPacket::handle);
        receive(NetworkManager.Side.S2C, OpenDebugScreenPacket.ID, OpenDebugScreenPacket::new, OpenDebugScreenPacket::handle);
        receive(NetworkManager.Side.S2C, OpenMirrorScreenPacket.ID, OpenMirrorScreenPacket::new, OpenMirrorScreenPacket::handle);
        receive(NetworkManager.Side.S2C, OpenSkillWheelPacket.ID, OpenSkillWheelPacket::new, OpenSkillWheelPacket::handle);
        receive(NetworkManager.Side.S2C, OpenHUDEditorPacket.ID, OpenHUDEditorPacket::new, OpenHUDEditorPacket::handle);
        receive(NetworkManager.Side.S2C, OpenTeamScreenPacket.ID, OpenTeamScreenPacket::new, OpenTeamScreenPacket::handle);
        receive(NetworkManager.Side.S2C, TeamUpdatePacket.ID, TeamUpdatePacket::new, TeamUpdatePacket::handle);
        receive(NetworkManager.Side.S2C, ShowItemAnimationPacket.ID, ShowItemAnimationPacket::new, ShowItemAnimationPacket::handle);
        receive(NetworkManager.Side.S2C, ClearRemoteCachePacket.ID, ClearRemoteCachePacket::new, ClearRemoteCachePacket::handle);
        receive(NetworkManager.Side.S2C, SyncBeamPacket.ID, SyncBeamPacket::new, SyncBeamPacket::handle);
        receive(NetworkManager.Side.S2C, SyncAnimationPacket.ID, SyncAnimationPacket::new, SyncAnimationPacket::handle);
        receive(NetworkManager.Side.S2C, SyncTetherPacket.ID, SyncTetherPacket::new, SyncTetherPacket::handle);
        receive(NetworkManager.Side.S2C, StopSoundPacket.ID, StopSoundPacket::new, StopSoundPacket::handle);
        receive(NetworkManager.Side.S2C, OpenClanManagePacket.ID, OpenClanManagePacket::new, OpenClanManagePacket::handle);
        receive(NetworkManager.Side.S2C, OpenTerritoryMapPacket.ID, OpenTerritoryMapPacket::new, OpenTerritoryMapPacket::handle);
        receive(NetworkManager.Side.S2C, ClaimResponsePacket.ID, ClaimResponsePacket::new, ClaimResponsePacket::handle);
        receive(NetworkManager.Side.S2C, TerritoryDataPacket.ID, TerritoryDataPacket::new, TerritoryDataPacket::handle);
        receive(NetworkManager.Side.S2C, TerrainSamplePacket.ID, TerrainSamplePacket::new, TerrainSamplePacket::handle);
        receive(NetworkManager.Side.S2C, ClanUpdatePacket.ID, ClanUpdatePacket::new, ClanUpdatePacket::handle);
        receive(NetworkManager.Side.S2C, SyncGamerulePacket.ID, SyncGamerulePacket::new, SyncGamerulePacket::handle);
        receive(NetworkManager.Side.S2C, SyncHexGridPacket.ID, SyncHexGridPacket::new, SyncHexGridPacket::handle);
        receive(NetworkManager.Side.S2C, ResearchResultPacket.ID, ResearchResultPacket::new, ResearchResultPacket::handle);
        receive(NetworkManager.Side.S2C, SyncQuestsPacket.ID, SyncQuestsPacket::new, SyncQuestsPacket::handle);
        receive(NetworkManager.Side.S2C, QuestBoardStateSyncPacket.ID, QuestBoardStateSyncPacket::new, QuestBoardStateSyncPacket::handle);
        receive(NetworkManager.Side.S2C, WaypointOfferPacket.ID, WaypointOfferPacket::new, WaypointOfferPacket::handle);
        receive(NetworkManager.Side.S2C, GateBuiltPacket.ID, GateBuiltPacket::new, GateBuiltPacket::handle);

        LOGGER.info("Client network boundaries registered.");
    }

    /** Every packet class pairs a buffer-reading constructor with handle(Supplier&lt;PacketContext&gt;). */
    private static <P> void receive(NetworkManager.Side side, ResourceLocation id, Function<FriendlyByteBuf, P> decoder,
            BiConsumer<P, Supplier<NetworkManager.PacketContext>> handler) {
        NetworkManager.registerReceiver(side, id, (buf, context) -> handler.accept(decoder.apply(buf), () -> context));
    }

    // Client to server

    public static void sendSetRace(SetRacePacket pkt) {
        sendToServer(SetRacePacket.ID, pkt::encode);
    }

    public static void sendShareWaypoint(ShareWaypointPacket pkt) {
        sendToServer(ShareWaypointPacket.ID, pkt::encode);
    }

    public static void sendTeamRequest(TeamRequestPacket pkt) {
        sendToServer(TeamRequestPacket.ID, pkt::encode);
    }

    public static void sendSyncRequest() {
        sendToServer(RequestSyncPacket.ID, NO_PAYLOAD);
    }

    public static void sendEquipAbility(EquipAbilityPacket pkt) {
        sendToServer(EquipAbilityPacket.ID, pkt::encode);
    }

    public static void sendCastAbility(CastAbilityPacket pkt) {
        sendToServer(CastAbilityPacket.ID, pkt::encode);
    }

    public static void sendSetCustomization(SetCustomizationPacket pkt) {
        sendToServer(SetCustomizationPacket.ID, pkt::encode);
    }

    public static void sendOpenEssenceBelt() {
        sendToServer(OpenEssenceBeltPacket.ID, NO_PAYLOAD);
    }

    public static void sendGStateUpdate(int gState) {
        sendToServer(UpdateGStatePacket.ID, new UpdateGStatePacket(gState)::encode);
    }

    public static void sendDoubleJump() {
        sendToServer(DoubleJumpPacket.ID, NO_PAYLOAD);
    }

    public static void sendDebugAction(String action, String key, String value) {
        sendToServer(DebugActionPacket.ID, new DebugActionPacket(action, key, value)::encode);
    }

    public static void sendMiniPlace(MiniPlacePacket pkt) {
        sendToServer(MiniPlacePacket.ID, pkt::encode);
    }

    public static void sendMiniRemove(MiniRemovePacket pkt) {
        sendToServer(MiniRemovePacket.ID, pkt::encode);
    }

    public static void sendMiniUse(MiniUsePacket pkt) {
        sendToServer(MiniUsePacket.ID, pkt::encode);
    }

    public static void sendClaimChunk(ClaimChunkPacket pkt) {
        sendToServer(ClaimChunkPacket.ID, pkt::encode);
    }

    public static void sendClanAction(ClanActionPacket pkt) {
        sendToServer(ClanActionPacket.ID, pkt::encode);
    }

    public static void sendRequestTerritoryData() {
        sendToServer(RequestTerritoryDataPacket.ID, NO_PAYLOAD);
    }

    public static void sendPlaceEssence(PlaceEssencePacket pkt) {
        sendToServer(PlaceEssencePacket.ID, pkt::encode);
    }

    public static void sendRemoveEssence(RemoveEssencePacket pkt) {
        sendToServer(RemoveEssencePacket.ID, pkt::encode);
    }

    public static void sendCraftScroll(CraftScrollPacket pkt) {
        sendToServer(CraftScrollPacket.ID, pkt::encode);
    }

    public static void sendTakeQuest(TakeQuestPacket pkt) {
        sendToServer(TakeQuestPacket.ID, pkt::encode);
    }

    public static void sendAbandonQuest(AbandonQuestPacket pkt) {
        sendToServer(AbandonQuestPacket.ID, pkt::encode);
    }

    // Server to client

    public static void sendWaypointOffer(ServerPlayer player, WaypointOfferPacket pkt) {
        send(player, WaypointOfferPacket.ID, pkt::encode);
    }

    public static void sendGateBuilt(ServerPlayer player, GateBuiltPacket pkt) {
        send(player, GateBuiltPacket.ID, pkt::encode);
    }

    public static void sendOpenSelection(ServerPlayer player) {
        send(player, OpenSelectionScreenPacket.ID, NO_PAYLOAD);
    }

    public static void sendOpenDebug(ServerPlayer player) {
        send(player, OpenDebugScreenPacket.ID, NO_PAYLOAD);
    }

    public static void sendOpenMirror(ServerPlayer player) {
        send(player, OpenMirrorScreenPacket.ID, NO_PAYLOAD);
    }

    public static void sendOpenSkillWheel(ServerPlayer player) {
        send(player, OpenSkillWheelPacket.ID, NO_PAYLOAD);
    }

    public static void sendOpenHUDEditor(ServerPlayer player) {
        send(player, OpenHUDEditorPacket.ID, NO_PAYLOAD);
    }

    public static void sendOpenTeamGUI(ServerPlayer player) {
        send(player, OpenTeamScreenPacket.ID, NO_PAYLOAD);
    }

    public static void sendOpenClanManage(ServerPlayer player) {
        send(player, OpenClanManagePacket.ID, NO_PAYLOAD);
    }

    public static void sendOpenTerritoryMap(ServerPlayer player) {
        send(player, OpenTerritoryMapPacket.ID, NO_PAYLOAD);
    }

    public static void sendTeamUpdate(ServerPlayer player, TeamUpdatePacket pkt) {
        send(player, TeamUpdatePacket.ID, pkt::encode);
    }

    public static void syncRacesToPlayer(ServerPlayer player, SyncRacesPacket pkt) {
        send(player, SyncRacesPacket.ID, pkt::encode);
    }

    public static void syncAbilitiesToPlayer(ServerPlayer player, SyncAbilitiesPacket pkt) {
        send(player, SyncAbilitiesPacket.ID, pkt::encode);
    }

    public static void syncQuestsToPlayer(ServerPlayer player, SyncQuestsPacket pkt) {
        send(player, SyncQuestsPacket.ID, pkt::encode);
    }

    public static void sendIncidentToPlayer(Player player, SyncIncidentPacket pkt) {
        if (player instanceof ServerPlayer sp) {
            send(sp, SyncIncidentPacket.ID, pkt::encode);
        }
    }

    public static void sendItemAnimation(ServerPlayer player, ResourceLocation itemId) {
        send(player, ShowItemAnimationPacket.ID, new ShowItemAnimationPacket(itemId)::encode);
    }

    public static void sendStopSound(ServerPlayer player, ResourceLocation soundId, SoundSource source) {
        send(player, StopSoundPacket.ID, new StopSoundPacket(soundId, source)::encode);
    }

    public static void broadcastStopSound(Player player, ResourceLocation soundId, SoundSource source) {
        sendToTrackers(player, StopSoundPacket.ID, new StopSoundPacket(soundId, source)::encode);
        // sendToTrackers normally reaches the player too, but not with a sync distance of 0
        if (player instanceof ServerPlayer sp) {
            sendStopSound(sp, soundId, source);
        }
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

    public static void sendClanUpdate(ServerPlayer player, ClanUpdatePacket pkt) {
        send(player, ClanUpdatePacket.ID, pkt::encode);
    }

    public static void syncHexGrid(ServerPlayer player, ResearchTableBlockEntity be) {
        send(player, SyncHexGridPacket.ID, new SyncHexGridPacket(be.getHexGrid())::encode);
    }

    public static void sendResearchResult(ServerPlayer player, ResearchResultPacket pkt) {
        send(player, ResearchResultPacket.ID, pkt::encode);
    }

    public static void sendQuestBoardSync(ServerPlayer player, QuestBoardStateSyncPacket pkt) {
        send(player, QuestBoardStateSyncPacket.ID, pkt::encode);
    }

    public static void broadcastClearCache() {
        sendToAll(ClearRemoteCachePacket.ID, NO_PAYLOAD);
    }

    public static void broadcastSpiritFlameGamerule(boolean value) {
        sendToAll(SyncGamerulePacket.ID, new SyncGamerulePacket(value)::encode);
    }

    // Player variable sync

    /**
     * Resyncs a player's variables to one recipient. A full sync includes resources (joins,
     * respawns, casts); periodic ticks skip them because the client predicts resources itself.
     */
    public static void resyncVariables(Player target, Player recipient, boolean fullSync) {
        DataUtils.getVariables(target).ifPresent(vars -> {
            CompoundTag tag = vars.serialize(fullSync);

            if (target instanceof IPersistentDataAccessor accessor) {
                CompoundTag persistentData = accessor.creraces$getPersistentData();
                if (!persistentData.isEmpty()) {
                    tag.put("creraces:persistent_data", Objects.requireNonNull(persistentData.copy()));
                }
            }

            sendIncidentToPlayer(recipient, new SyncIncidentPacket(target.getUUID(), tag));
        });
    }

    /** Full sync, the safe default for any explicit event. */
    public static void resyncVariables(Player target, Player recipient) {
        resyncVariables(target, recipient, true);
    }

    /** Resyncs a player's variables to everyone tracking them. */
    public static void resyncForAllTrackers(Player player) {
        resyncForAllTrackers(player, true);
    }

    public static void resyncForAllTrackers(Player player, boolean fullSync) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            var pkt = new SyncIncidentPacket(player.getUUID(), vars.serialize(fullSync));
            sendToTrackers(player, SyncIncidentPacket.ID, pkt::encode);
        });
    }

    /** Sends to every player in the entity's dimension within the configured visual sync distance. */
    public static void sendToTrackers(Player entity, ResourceLocation id, Consumer<FriendlyByteBuf> encoder) {
        if (!(entity.level() instanceof ServerLevel serverLevel))
            return;

        int syncDist = CreRacesConfig.VISUAL_SYNC_DISTANCE.get();
        double syncDistSqr = syncDist * syncDist;

        for (ServerPlayer p : serverLevel.getServer().getPlayerList().getPlayers()) {
            if (p.level().dimension() == entity.level().dimension() && p.distanceToSqr(entity) < syncDistSqr) {
                send(p, id, encoder);
            }
        }
    }

    private static void sendToServer(ResourceLocation id, Consumer<FriendlyByteBuf> encoder) {
        FriendlyByteBuf buf = newBuf();
        encoder.accept(buf);
        NetworkManager.sendToServer(id, buf);
    }

    private static void send(ServerPlayer player, ResourceLocation id, Consumer<FriendlyByteBuf> encoder) {
        FriendlyByteBuf buf = newBuf();
        encoder.accept(buf);
        NetworkManager.sendToPlayer(player, id, buf);
    }

    private static void sendToAll(ResourceLocation id, Consumer<FriendlyByteBuf> encoder) {
        MinecraftServer server = GameInstance.getServer();
        if (server == null)
            return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, id, encoder);
        }
    }
}
