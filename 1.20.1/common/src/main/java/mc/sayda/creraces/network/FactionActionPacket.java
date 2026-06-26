package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.territory.FactionData;
import mc.sayda.creraces.territory.FactionRank;
import mc.sayda.creraces.territory.TerritoryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * C2S: client requests a faction operation.
 */
public class FactionActionPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "faction_action");

    private static final ResourceLocation TX = new ResourceLocation("creraces", "tx");
    private static final ResourceLocation TY = new ResourceLocation("creraces", "ty");
    private static final ResourceLocation TZ = new ResourceLocation("creraces", "tz");
    private static final ResourceLocation SPIRIT_REALM = new ResourceLocation("creraces", "spirit_realm");
    private static final int NAME_MAX = 32;

    public enum Action { CREATE, INVITE, KICK, PROMOTE, DEMOTE, LEAVE, DISBAND, VIEW, LEAVE_DISBAND, LEAVE_SPLIT }

    private final Action action;
    private String name;
    private UUID targetUuid;

    public FactionActionPacket(Action action, String name) {
        this.action = action;
        this.name = name;
    }

    public FactionActionPacket(Action action, UUID targetUuid) {
        this.action = action;
        this.targetUuid = targetUuid;
    }

    public FactionActionPacket(Action action) {
        this.action = action;
    }

    public FactionActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        if (buf.readBoolean()) this.name = buf.readUtf(NAME_MAX);
        if (buf.readBoolean()) this.targetUuid = buf.readUUID();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeBoolean(name != null);
        if (name != null) buf.writeUtf(name);
        buf.writeBoolean(targetUuid != null);
        if (targetUuid != null) buf.writeUUID(targetUuid);
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;
            TerritoryManager tm = TerritoryManager.get();
            UUID playerId = player.getUUID();

            switch (action) {
                case CREATE        -> handleCreate(player, tm);
                case INVITE        -> handleInvite(player, tm);
                case KICK          -> handleKick(player, tm, playerId);
                case PROMOTE       -> handleRankChange(player, tm, FactionRank.OFFICER, false);
                case DEMOTE        -> handleRankChange(player, tm, FactionRank.MEMBER, true);
                case LEAVE         -> handleLeave(player, tm, playerId);
                case DISBAND       -> handleDisband(player, tm, playerId);
                case VIEW          -> handleView(player, tm, playerId);
                case LEAVE_DISBAND -> handleLeaveDisband(player, tm, playerId);
                case LEAVE_SPLIT   -> handleLeaveSplit(player, tm, playerId);
            }
        });
    }

    private void handleCreate(ServerPlayer player, TerritoryManager tm) {
        if (name == null || name.isBlank()) return;
        if (name.length() > NAME_MAX) name = name.substring(0, NAME_MAX);
        if (tm.hasFaction(player.getUUID())) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "creraces.territory.already_in_faction"));
            return;
        }

        // Read anchor position from persistent player state
        var vars = DataUtils.getVariables(player).orElse(null);
        if (vars == null) return;
        double tx = vars.getPersistentState(TX);
        double ty = vars.getPersistentState(TY);
        double tz = vars.getPersistentState(TZ);
        if (tx == 0 && ty == 0 && tz == 0) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "creraces.territory.no_anchor_placed"));
            return;
        }

        ResourceLocation raceId = vars.getRace();
        BlockPos anchor = new BlockPos((int) tx, (int) ty, (int) tz);
        UUID factionId = tm.createFaction(player.getServer(), player, name, raceId, anchor);

        // Exit spirit realm if applicable
        if (player.level().dimension().location().equals(SPIRIT_REALM)) {
            ServerLevel overworld = player.getServer().getLevel(
                    net.minecraft.world.level.Level.OVERWORLD);
            if (overworld != null) {
                player.teleportTo(overworld, tx, ty + 1, tz,
                        player.getYRot(), player.getXRot());
            }
            DataUtils.getVariables(player).ifPresent(v -> v.setInSpiritRealm(false));
            BoundaryHandler.resyncForAllTrackers(player);
            BoundaryHandler.resyncVariables(player, player);
        }

        // Execute the deferred on_success actions from ClaimTerritoryAction (places dryad_root, etc.)
        mc.sayda.creraces.engine.actions.ClaimTerritoryAction.resumePending(player);

        broadcastFactionUpdate(player.getServer(), factionId);
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "creraces.territory.faction_created", name));
    }

    private void handleInvite(ServerPlayer player, TerritoryManager tm) {
        if (name == null || name.isBlank()) return;
        UUID playerId = player.getUUID();
        if (!tm.hasFaction(playerId)) return;
        UUID factionId = tm.getFactionId(playerId);
        FactionData faction = tm.getFaction(factionId);
        if (faction == null) return;
        FactionRank rank = faction.getRank(playerId);
        if (rank == null || !rank.isAtLeast(FactionRank.OFFICER)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "creraces.territory.invalid_rank"));
            return;
        }
        if (name.length() > NAME_MAX) {
            CreRaces.LOGGER.warn("Player {} sent oversized invite name", player.getName().getString());
            name = name.substring(0, NAME_MAX);
        }
        ServerPlayer target = player.getServer().getPlayerList().getPlayerByName(name);
        if (target == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "msg.creraces.team.player_not_found", name));
            return;
        }
        if (tm.hasFaction(target.getUUID())) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "creraces.territory.player_has_faction", name));
            return;
        }
        // Require the invited player to share the faction's race.
        ResourceLocation factionRace = faction.getRaceId();
        ResourceLocation targetRace = mc.sayda.creraces.capability.DataUtils.getVariables(target)
                .map(mc.sayda.creraces.capability.IPlayerVariables::getRace).orElse(null);
        if (targetRace == null || !factionRace.equals(targetRace)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "msg.creraces.team.different_race"));
            return;
        }
        // Add as member immediately (a join-request flow can be layered later)
        tm.addMember(factionId, target.getUUID(), FactionRank.MEMBER);
        broadcastFactionUpdate(player.getServer(), factionId);
        target.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "creraces.territory.joined_faction", faction.getName()));
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "creraces.territory.invited_player", name));
    }

    private void handleKick(ServerPlayer player, TerritoryManager tm, UUID playerId) {
        if (targetUuid == null) return;
        if (targetUuid.equals(playerId)) return; // use LEAVE
        if (!tm.hasFaction(playerId)) return;
        UUID factionId = tm.getFactionId(playerId);
        FactionData faction = tm.getFaction(factionId);
        if (faction == null) return;
        FactionRank requesterRank = faction.getRank(playerId);
        FactionRank targetRank = faction.getRank(targetUuid);
        if (requesterRank == null || targetRank == null) return;
        // Officer can kick Members; Leader can kick Officers and Members
        if (!requesterRank.isAtLeast(FactionRank.OFFICER)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "creraces.territory.invalid_rank"));
            return;
        }
        if (targetRank.isAtLeast(requesterRank)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "creraces.territory.cannot_kick_equal_rank"));
            return;
        }
        tm.removeMember(player.getServer(), factionId, targetUuid, true);
        broadcastFactionUpdate(player.getServer(), factionId);
    }

    private void handleRankChange(ServerPlayer player, TerritoryManager tm,
            FactionRank newRank, boolean isDemote) {
        if (targetUuid == null) return;
        UUID playerId = player.getUUID();
        if (targetUuid.equals(playerId)) return;
        if (!tm.hasFaction(playerId)) return;
        UUID factionId = tm.getFactionId(playerId);
        FactionData faction = tm.getFaction(factionId);
        if (faction == null) return;
        FactionRank requesterRank = faction.getRank(playerId);
        if (requesterRank == null || requesterRank != FactionRank.LEADER) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "creraces.territory.leader_only"));
            return;
        }
        if (tm.setRank(factionId, targetUuid, newRank, requesterRank)) {
            broadcastFactionUpdate(player.getServer(), factionId);
        }
    }

    private void handleLeave(ServerPlayer player, TerritoryManager tm, UUID playerId) {
        if (!tm.hasFaction(playerId)) return;
        UUID factionId = tm.getFactionId(playerId);
        FactionData faction = tm.getFaction(factionId);
        if (faction == null) return;
        boolean wasLast = faction.getMembers().size() == 1;
        tm.removeMember(player.getServer(), factionId, playerId, false);
        if (wasLast) {
            tm.disbandFaction(player.getServer(), factionId);
        } else {
            broadcastFactionUpdate(player.getServer(), factionId);
        }
    }

    private void handleLeaveDisband(ServerPlayer player, TerritoryManager tm, UUID playerId) {
        if (!tm.hasFaction(playerId)) return;
        UUID factionId = tm.getFactionId(playerId);
        FactionData faction = tm.getFaction(factionId);
        if (faction == null) return;
        boolean wasLast = faction.getMembers().size() == 1;
        tm.leaveDisband(player.getServer(), factionId, playerId);
        if (!wasLast && tm.getFaction(factionId) != null) {
            broadcastFactionUpdate(player.getServer(), factionId);
        }
    }

    private void handleLeaveSplit(ServerPlayer player, TerritoryManager tm, UUID playerId) {
        if (!tm.hasFaction(playerId)) return;
        UUID oldFactionId = tm.getFactionId(playerId);
        FactionData oldFaction = tm.getFaction(oldFactionId);
        if (oldFaction == null) return;
        boolean wasLast = oldFaction.getMembers().size() == 1;
        UUID newFactionId = tm.leaveSplit(player.getServer(), oldFactionId, playerId);
        if (newFactionId != null) {
            // Send updated faction data to the player (now in their new faction)
            FactionData newFaction = tm.getFaction(newFactionId);
            if (newFaction != null) {
                BoundaryHandler.sendFactionUpdate(player, FactionUpdatePacket.from(newFaction, player.getServer()));
            }
            // Broadcast to remaining old-faction members if it still exists
            if (!wasLast && tm.getFaction(oldFactionId) != null) {
                broadcastFactionUpdate(player.getServer(), oldFactionId);
            }
        }
    }

    private void handleDisband(ServerPlayer player, TerritoryManager tm, UUID playerId) {
        if (!tm.hasFaction(playerId)) return;
        UUID factionId = tm.getFactionId(playerId);
        FactionData faction = tm.getFaction(factionId);
        if (faction == null) return;
        FactionRank rank = faction.getRank(playerId);
        if (rank != FactionRank.LEADER) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "creraces.territory.leader_only"));
            return;
        }
        tm.disbandFaction(player.getServer(), factionId);
    }

    private void handleView(ServerPlayer player, TerritoryManager tm, UUID playerId) {
        if (!tm.hasFaction(playerId)) return;
        UUID factionId = tm.getFactionId(playerId);
        FactionData faction = tm.getFaction(factionId);
        if (faction == null) return;
        FactionUpdatePacket pkt = FactionUpdatePacket.from(faction, player.getServer());
        BoundaryHandler.sendFactionUpdate(player, pkt);
        BoundaryHandler.sendOpenFactionManage(player);
    }

    private static void broadcastFactionUpdate(net.minecraft.server.MinecraftServer server,
            UUID factionId) {
        FactionData faction = TerritoryManager.get().getFaction(factionId);
        if (faction == null) return;
        FactionUpdatePacket pkt = FactionUpdatePacket.from(faction, server);
        for (UUID memberId : faction.getMembers().keySet()) {
            ServerPlayer online = server.getPlayerList().getPlayer(memberId);
            if (online != null) BoundaryHandler.sendFactionUpdate(online, pkt);
        }
    }
}
