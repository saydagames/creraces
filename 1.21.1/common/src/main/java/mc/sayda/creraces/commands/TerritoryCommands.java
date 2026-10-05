package mc.sayda.creraces.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.ClanUpdatePacket;
import mc.sayda.creraces.network.RequestTerritoryDataPacket;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.territory.ClaimData;
import mc.sayda.creraces.territory.ClanData;
import mc.sayda.creraces.territory.TerritoryManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

/** /creraces territory (the map, plus op-only claim tools) and /creraces clan. */
@SuppressWarnings("null")
final class TerritoryCommands {
    private TerritoryCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> territory() {
        return Commands.literal("territory")
                .executes(ctx -> openMap(ctx.getSource()))
                .then(Commands.literal("race")
                        .requires(CreracesCommand::isOp)
                        .then(Commands.literal("unclaim")
                                .then(Commands.argument("race_id", ResourceLocationArgument.id())
                                        .suggests(CreracesCommand::suggestRaces)
                                        .executes(ctx -> unclaimRace(ctx.getSource(),
                                                ResourceLocationArgument.getId(ctx, "race_id"))))))
                .then(Commands.literal("chunk")
                        .requires(CreracesCommand::isOp)
                        .then(Commands.literal("unclaim")
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(ctx -> unclaimChunk(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "x"),
                                                        IntegerArgumentType.getInteger(ctx, "z"))))))
                        .then(Commands.literal("info")
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(ctx -> chunkInfo(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "x"),
                                                        IntegerArgumentType.getInteger(ctx, "z")))))));
    }

    static LiteralArgumentBuilder<CommandSourceStack> clan() {
        return Commands.literal("clan")
                .executes(ctx -> openClanScreen(ctx.getSource()));
    }

    private static int openMap(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        RequestTerritoryDataPacket.sendSnapshot(player);
        BoundaryHandler.sendOpenTerritoryMap(player);
        return 1;
    }

    private static int openClanScreen(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        ResourceLocation myRace = DataUtils.getVariables(player)
                .map(IPlayerVariables::getRace)
                .orElse(null);
        if (myRace == null || myRace.equals(RaceRegistry.NONE)) {
            source.sendFailure(Component.literal("You have not chosen a race."));
            return 0;
        }
        ClanData clan = TerritoryManager.get().getClanOrEmpty(myRace);
        BoundaryHandler.sendClanUpdate(player, ClanUpdatePacket.from(clan));
        BoundaryHandler.sendOpenClanManage(player);
        return 1;
    }

    private static int unclaimRace(CommandSourceStack source, ResourceLocation raceId) {
        TerritoryManager tm = TerritoryManager.get();
        long before = tm.getClaims().values().stream()
                .filter(c -> c.getRaceId().equals(raceId)).count();
        if (before == 0) {
            source.sendFailure(Component.literal("No claims found for race: " + raceId));
            return 0;
        }
        tm.unclaimAllForRace(raceId);
        source.sendSuccess(() -> Component.literal("Removed " + before + " claim(s) for race '" + raceId + "'.")
                .withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int unclaimChunk(CommandSourceStack source, int chunkX, int chunkZ) {
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        TerritoryManager tm = TerritoryManager.get();
        ClaimData claim = tm.getClaimAt(chunkPos);
        if (claim == null) {
            source.sendFailure(Component.literal("Chunk [" + chunkX + ", " + chunkZ + "] is not claimed."));
            return 0;
        }
        // unclaimChunk refuses anchor chunks, which an admin still needs to be able to clear
        if (tm.unclaimChunk(claim.getRaceId(), chunkPos)) {
            source.sendSuccess(() -> Component.literal("Unclaimed chunk [" + chunkX + ", " + chunkZ + "].")
                    .withStyle(ChatFormatting.GREEN), true);
        } else {
            tm.forceUnclaimChunk(chunkPos);
            source.sendSuccess(() -> Component.literal("Force-unclaimed chunk [" + chunkX + ", " + chunkZ + "] (was anchor).")
                    .withStyle(ChatFormatting.YELLOW), true);
        }
        return 1;
    }

    private static int chunkInfo(CommandSourceStack source, int chunkX, int chunkZ) {
        ClaimData claim = TerritoryManager.get().getClaimAt(new ChunkPos(chunkX, chunkZ));
        if (claim == null) {
            source.sendSuccess(() -> Component.literal("Chunk [" + chunkX + ", " + chunkZ + "] is unclaimed.")
                    .withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        String raceStr = claim.getRaceId().toString();
        boolean persistent = claim.isPersistent();
        source.sendSuccess(() -> Component.literal("Chunk [" + chunkX + ", " + chunkZ + "]: race=" + raceStr
                + (persistent ? " §e[ANCHOR]§r" : "")).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }
}
