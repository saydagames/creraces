package mc.sayda.creraces.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import mc.sayda.creraces.territory.ClaimData;
import mc.sayda.creraces.territory.FactionData;
import mc.sayda.creraces.territory.FactionRank;
import mc.sayda.creraces.territory.TerritoryManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("null")
public class TerritoryCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("creterritory")
                .requires(src -> src.hasPermission(3))

                // faction branch
                .then(Commands.literal("faction")

                        // forcejoin <player> <faction_name> <rank>
                        .then(Commands.literal("forcejoin")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("faction_name", StringArgumentType.word())
                                                .suggests(TerritoryCommand::suggestFactionNames)
                                                .then(Commands.argument("rank", StringArgumentType.word())
                                                        .suggests(TerritoryCommand::suggestRanks)
                                                        .executes(ctx -> executeForcejoin(ctx.getSource(),
                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                StringArgumentType.getString(ctx, "faction_name"),
                                                                StringArgumentType.getString(ctx, "rank")))))))

                        // forcerank <player> <rank>
                        .then(Commands.literal("forcerank")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("rank", StringArgumentType.word())
                                                .suggests(TerritoryCommand::suggestRanks)
                                                .executes(ctx -> executeForcerank(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "rank"))))))

                        // disband <faction_name>
                        .then(Commands.literal("disband")
                                .then(Commands.argument("faction_name", StringArgumentType.word())
                                        .suggests(TerritoryCommand::suggestFactionNames)
                                        .executes(ctx -> executeFactionDisband(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "faction_name"))))))

                // clan branch
                .then(Commands.literal("clan")

                        // disband <faction_name> — identifies clan via a member faction
                        .then(Commands.literal("disband")
                                .then(Commands.argument("faction_name", StringArgumentType.word())
                                        .suggests(TerritoryCommand::suggestFactionNames)
                                        .executes(ctx -> executeClanDisband(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "faction_name"))))))

                // chunk branch
                .then(Commands.literal("chunk")

                        // unclaim <x> <z>
                        .then(Commands.literal("unclaim")
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(ctx -> executeChunkUnclaim(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "x"),
                                                        IntegerArgumentType.getInteger(ctx, "z"))))))

                        // info <x> <z>
                        .then(Commands.literal("info")
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(ctx -> executeChunkInfo(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "x"),
                                                        IntegerArgumentType.getInteger(ctx, "z"))))))));
    }

    // ── faction forcejoin ──────────────────────────────────────────────────────

    private static int executeForcejoin(CommandSourceStack source, ServerPlayer target,
            String factionName, String rankStr) {
        FactionData faction = findFactionByName(factionName);
        if (faction == null) {
            source.sendFailure(Component.literal("No faction named: " + factionName));
            return 0;
        }

        FactionRank rank = parseRank(rankStr);
        if (rank == null) {
            source.sendFailure(Component.literal("Invalid rank: " + rankStr + " (use LEADER, OFFICER, or MEMBER)"));
            return 0;
        }

        TerritoryManager tm = TerritoryManager.get();

        // Remove from existing faction first if necessary
        if (tm.hasFaction(target.getUUID())) {
            UUID oldFactionId = tm.getFactionId(target.getUUID());
            tm.removeMember(source.getServer(), oldFactionId, target.getUUID(), false);
        }

        boolean ok = tm.addMember(faction.getId(), target.getUUID(), rank);
        if (!ok) {
            source.sendFailure(Component.literal("Failed to add player to faction."));
            return 0;
        }

        String playerName = target.getGameProfile().getName();
        source.sendSuccess(() -> Component.literal(
                "Added " + playerName + " to faction '" + factionName + "' as " + rank.name())
                .withStyle(ChatFormatting.GREEN), true);
        target.sendSystemMessage(Component.literal(
                "An admin added you to faction '" + factionName + "' as " + rank.name())
                .withStyle(ChatFormatting.GOLD));
        return 1;
    }

    // ── faction forcerank ──────────────────────────────────────────────────────

    private static int executeForcerank(CommandSourceStack source, ServerPlayer target, String rankStr) {
        FactionRank rank = parseRank(rankStr);
        if (rank == null) {
            source.sendFailure(Component.literal("Invalid rank: " + rankStr + " (use LEADER, OFFICER, or MEMBER)"));
            return 0;
        }

        TerritoryManager tm = TerritoryManager.get();
        UUID playerId = target.getUUID();
        if (!tm.hasFaction(playerId)) {
            source.sendFailure(Component.literal(target.getGameProfile().getName() + " is not in any faction."));
            return 0;
        }

        UUID factionId = tm.getFactionId(playerId);
        FactionData faction = tm.getFaction(factionId);
        if (faction == null) {
            source.sendFailure(Component.literal("Faction data not found."));
            return 0;
        }

        // If promoting to LEADER, demote current leader
        if (rank == FactionRank.LEADER) {
            UUID currentLeader = faction.getLeader();
            if (currentLeader != null && !currentLeader.equals(playerId)) {
                faction.getMembers().put(currentLeader, FactionRank.OFFICER);
            }
        }
        faction.getMembers().put(playerId, rank);

        String playerName = target.getGameProfile().getName();
        source.sendSuccess(() -> Component.literal(
                "Set " + playerName + "'s rank to " + rank.name() + " in faction '" + faction.getName() + "'")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    // ── faction disband ────────────────────────────────────────────────────────

    private static int executeFactionDisband(CommandSourceStack source, String factionName) {
        FactionData faction = findFactionByName(factionName);
        if (faction == null) {
            source.sendFailure(Component.literal("No faction named: " + factionName));
            return 0;
        }

        TerritoryManager.get().disbandFaction(source.getServer(), faction.getId());
        source.sendSuccess(() -> Component.literal("Disbanded faction '" + factionName + "'.")
                .withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    // ── clan disband ───────────────────────────────────────────────────────────

    private static int executeClanDisband(CommandSourceStack source, String factionName) {
        FactionData faction = findFactionByName(factionName);
        if (faction == null) {
            source.sendFailure(Component.literal("No faction named: " + factionName));
            return 0;
        }

        UUID clanId = faction.getClanId();
        if (clanId == null) {
            source.sendFailure(Component.literal("Faction '" + factionName + "' is not in a clan."));
            return 0;
        }

        TerritoryManager.get().disbandClan(source.getServer(), clanId);
        source.sendSuccess(() -> Component.literal("Disbanded the clan containing faction '" + factionName + "'.")
                .withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    // ── chunk unclaim ──────────────────────────────────────────────────────────

    private static int executeChunkUnclaim(CommandSourceStack source, int chunkX, int chunkZ) {
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        TerritoryManager tm = TerritoryManager.get();
        ClaimData claim = tm.getClaimAt(chunkPos);

        if (claim == null) {
            source.sendFailure(Component.literal("Chunk [" + chunkX + ", " + chunkZ + "] is not claimed."));
            return 0;
        }

        // Use LEADER rank to bypass the officer+ check inside unclaimChunk
        boolean ok = tm.unclaimChunk(claim.getFactionId(), chunkPos, FactionRank.LEADER);
        if (!ok) {
            source.sendFailure(Component.literal("Failed to unclaim chunk."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "Force-unclaimed chunk [" + chunkX + ", " + chunkZ + "].")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    // ── chunk info ─────────────────────────────────────────────────────────────

    private static int executeChunkInfo(CommandSourceStack source, int chunkX, int chunkZ) {
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        TerritoryManager tm = TerritoryManager.get();
        ClaimData claim = tm.getClaimAt(chunkPos);

        if (claim == null) {
            source.sendSuccess(() -> Component.literal(
                    "Chunk [" + chunkX + ", " + chunkZ + "] is unclaimed.").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        FactionData faction = tm.getFaction(claim.getFactionId());
        String factionName = faction != null ? faction.getName() : claim.getFactionId().toString();
        String raceStr = claim.getRaceId() != null ? claim.getRaceId().toString() : "unknown";
        boolean dormant = claim.isDormant();

        source.sendSuccess(() -> Component.literal(
                "Chunk [" + chunkX + ", " + chunkZ + "]: faction='" + factionName
                        + "', race=" + raceStr
                        + (dormant ? " §e[DORMANT]§r" : ""))
                .withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static FactionData findFactionByName(String name) {
        for (FactionData f : TerritoryManager.get().getFactions().values()) {
            if (f.getName().equalsIgnoreCase(name)) return f;
        }
        return null;
    }

    private static FactionRank parseRank(String rankStr) {
        try {
            return FactionRank.valueOf(rankStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static CompletableFuture<Suggestions> suggestFactionNames(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        TerritoryManager.get().getFactions().values()
                .forEach(f -> builder.suggest(f.getName()));
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestRanks(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        for (FactionRank r : FactionRank.values()) {
            builder.suggest(r.name());
        }
        return builder.buildFuture();
    }
}
