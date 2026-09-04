package mc.sayda.creraces.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceIncidents;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.territory.ClaimData;
import mc.sayda.creraces.territory.TerritoryManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * Main command for race management.
 * Consolidates all race-related tools under /creraces.
 */
@SuppressWarnings("null")
public class CreracesCommand {
        private static final Random RANDOM = new Random();

        public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
                dispatcher.register(Commands.literal("creraces")
                                // Base command accessible to everyone (for 'abilities' etc.)
                                .requires(src -> true)

                                // help (Available to everyone)
                                .then(Commands.literal("help")
                                                .executes(ctx -> executeHelp(ctx.getSource())))

                                // hud (Available to everyone; opens HUD editor on client)
                                .then(Commands.literal("hud")
                                                .executes(ctx -> {
                                                        ServerPlayer player = ctx.getSource().getPlayer();
                                                        if (player == null)
                                                                return 0;
                                                        mc.sayda.creraces.network.BoundaryHandler
                                                                        .sendOpenHUDEditor(player);
                                                        return 1;
                                                }))

                                // abilities (Available to everyone)
                                .then(Commands.literal("abilities")
                                                .executes(ctx -> {
                                                        ServerPlayer player = ctx.getSource().getPlayer();
                                                        if (player == null)
                                                                return 0;
                                                        mc.sayda.creraces.network.BoundaryHandler
                                                                        .sendOpenSkillWheel(player);
                                                        return 1;
                                                }))

                                // team (Available to everyone)
                                .then(Commands.literal("team")
                                                .executes(ctx -> {
                                                        ServerPlayer player = ctx.getSource().getPlayer();
                                                        if (player == null)
                                                                return 0;
                                                        mc.sayda.creraces.network.BoundaryHandler
                                                                        .sendOpenTeamGUI(player);
                                                        return 1;
                                                }))

                                // territory (Available to everyone; opens TerritoryMapScreen - admin sub-branches below)
                                .then(Commands.literal("territory")
                                                .executes(ctx -> executeTerritory(ctx.getSource()))

                                                // territory race unclaim <race_id> (Admin Only)
                                                .then(Commands.literal("race")
                                                                .requires(src -> src.hasPermission(2))
                                                                .then(Commands.literal("unclaim")
                                                                                .then(Commands.argument("race_id",
                                                                                                java.util.Objects.requireNonNull(
                                                                                                                ResourceLocationArgument.id()))
                                                                                                .suggests(CreracesCommand::suggestRaces)
                                                                                                .executes(ctx -> executeAdminTerritoryRaceUnclaim(
                                                                                                                ctx.getSource(),
                                                                                                                ResourceLocationArgument.getId(ctx, "race_id"))))))

                                                // territory chunk unclaim <x> <z> / chunk info <x> <z> (Admin Only)
                                                .then(Commands.literal("chunk")
                                                                .requires(src -> src.hasPermission(2))
                                                                .then(Commands.literal("unclaim")
                                                                                .then(Commands.argument("x",
                                                                                                java.util.Objects.requireNonNull(
                                                                                                                IntegerArgumentType.integer()))
                                                                                                .then(Commands.argument("z",
                                                                                                                java.util.Objects.requireNonNull(
                                                                                                                                IntegerArgumentType.integer()))
                                                                                                                .executes(ctx -> executeAdminTerritoryChunkUnclaim(
                                                                                                                                ctx.getSource(),
                                                                                                                                IntegerArgumentType.getInteger(ctx, "x"),
                                                                                                                                IntegerArgumentType.getInteger(ctx, "z"))))))
                                                                .then(Commands.literal("info")
                                                                                .then(Commands.argument("x",
                                                                                                java.util.Objects.requireNonNull(
                                                                                                                IntegerArgumentType.integer()))
                                                                                                .then(Commands.argument("z",
                                                                                                                java.util.Objects.requireNonNull(
                                                                                                                                IntegerArgumentType.integer()))
                                                                                                                .executes(ctx -> executeAdminTerritoryChunkInfo(
                                                                                                                                ctx.getSource(),
                                                                                                                                IntegerArgumentType.getInteger(ctx, "x"),
                                                                                                                                IntegerArgumentType.getInteger(ctx, "z"))))))))

                                // clan (Available to everyone; opens ClanManagementScreen)
                                .then(Commands.literal("clan")
                                                .executes(ctx -> executeClan(ctx.getSource())))

                                // select subcommand
                                .then(Commands.literal("select")
                                                .then(Commands.argument("target",
                                                                java.util.Objects.requireNonNull(
                                                                                EntityArgument.player()))
                                                                .requires(src -> src.hasPermission(2))
                                                                .executes(ctx -> executeOpenSelection(ctx.getSource(),
                                                                                EntityArgument.getPlayer(ctx,
                                                                                                "target"))))
                                                .executes(ctx -> {
                                                        ServerPlayer player = ctx.getSource().getPlayer();
                                                        if (player == null)
                                                                return 0;
                                                        return executeOpenSelection(ctx.getSource(), player);
                                                }))

                                // mirror subcommand
                                .then(Commands.literal("mirror")
                                                .then(Commands.argument("target",
                                                                java.util.Objects.requireNonNull(
                                                                                EntityArgument.player()))
                                                                .requires(src -> src.hasPermission(2))
                                                                .executes(ctx -> executeOpenMirror(ctx.getSource(),
                                                                                EntityArgument.getPlayer(ctx,
                                                                                                "target"))))
                                                .executes(ctx -> {
                                                        ServerPlayer player = ctx.getSource().getPlayer();
                                                        if (player == null)
                                                                return 0;
                                                        return executeOpenMirror(ctx.getSource(), player);
                                                }))

                                // debug subcommand
                                .then(Commands.literal("debug")
                                                .then(Commands.argument("target",
                                                                java.util.Objects.requireNonNull(
                                                                                EntityArgument.player()))
                                                                .requires(src -> src.hasPermission(2))
                                                                .executes(ctx -> executeOpenDebug(ctx.getSource(),
                                                                                EntityArgument.getPlayer(ctx,
                                                                                                "target"))))
                                                .executes(ctx -> {
                                                        ServerPlayer player = ctx.getSource().getPlayer();
                                                        if (player == null)
                                                                return 0;
                                                        return executeOpenDebug(ctx.getSource(), player);
                                                }))

                                // reset <target> (Admin Only)
                                .then(Commands.literal("reset")
                                                .requires(src -> src.hasPermission(2))
                                                .then(Commands.argument("target",
                                                                java.util.Objects.requireNonNull(
                                                                                EntityArgument.player()))
                                                                .executes(ctx -> executeReset(ctx.getSource(),
                                                                                EntityArgument.getPlayer(ctx,
                                                                                                "target"))))
                                                .executes(ctx -> {
                                                        ServerPlayer player = ctx.getSource().getPlayer();
                                                        if (player == null)
                                                                return 0;
                                                        return executeReset(ctx.getSource(), player);
                                                }))

                                // setrace <target> <race_id> (Admin Only)
                                .then(Commands.literal("setrace")
                                                .requires(src -> src.hasPermission(2))
                                                .then(Commands.argument("target",
                                                                java.util.Objects.requireNonNull(
                                                                                EntityArgument.player()))
                                                                .then(Commands.argument("race",
                                                                                java.util.Objects.requireNonNull(
                                                                                                ResourceLocationArgument
                                                                                                                .id()))
                                                                                .suggests(CreracesCommand::suggestRaces)
                                                                                .executes(ctx -> executeSet(
                                                                                                ctx.getSource(),
                                                                                                EntityArgument.getPlayer(
                                                                                                                ctx,
                                                                                                                "target"),
                                                                                                ResourceLocationArgument
                                                                                                                .getId(ctx, "race"))))))

                                // grant <target> <ability_id> (Admin Only)
                                .then(Commands.literal("grant")
                                                .requires(src -> src.hasPermission(2))
                                                .then(Commands.argument("target",
                                                                java.util.Objects.requireNonNull(
                                                                                EntityArgument.player()))
                                                                .then(Commands.argument("ability",
                                                                                java.util.Objects.requireNonNull(
                                                                                                ResourceLocationArgument
                                                                                                                .id()))
                                                                                .suggests(CreracesCommand::suggestAbilities)
                                                                                .executes(ctx -> executeGrant(
                                                                                                ctx.getSource(),
                                                                                                EntityArgument.getPlayer(
                                                                                                                ctx,
                                                                                                                "target"),
                                                                                                ResourceLocationArgument
                                                                                                                .getId(ctx, "ability"))))))

                                // revoke <target> <ability_id> (Admin Only)
                                .then(Commands.literal("revoke")
                                                .requires(src -> src.hasPermission(2))
                                                .then(Commands.argument("target",
                                                                java.util.Objects.requireNonNull(
                                                                                EntityArgument.player()))
                                                                .then(Commands.argument("ability",
                                                                                java.util.Objects.requireNonNull(
                                                                                                ResourceLocationArgument
                                                                                                                .id()))
                                                                                .suggests(CreracesCommand::suggestUnlockedAbilities)
                                                                                .executes(ctx -> executeRevoke(
                                                                                                ctx.getSource(),
                                                                                                EntityArgument.getPlayer(
                                                                                                                ctx,
                                                                                                                "target"),
                                                                                                ResourceLocationArgument
                                                                                                                .getId(ctx, "ability"))))))

                                // setrandom <target> (Admin Only)
                                .then(Commands.literal("setrandom")
                                                .requires(src -> src.hasPermission(2))
                                                .then(Commands.argument("target",
                                                                java.util.Objects.requireNonNull(
                                                                                EntityArgument.player()))
                                                                .executes(ctx -> executeSetRandom(ctx.getSource(),
                                                                                EntityArgument.getPlayer(ctx,
                                                                                                "target"))))
                                                .executes(ctx -> {
                                                        ServerPlayer player = ctx.getSource().getPlayer();
                                                        if (player == null)
                                                                return 0;
                                                        return executeSetRandom(ctx.getSource(), player);
                                                }))

                                // reload (Admin Only)
                                .then(Commands.literal("reload")
                                                .requires(src -> src.hasPermission(2))
                                                .executes(ctx -> executeReload(ctx.getSource())))

                                // refresh (Available to everyone)
                                .then(Commands.literal("refresh")
                                                .executes(ctx -> executeRefresh(ctx.getSource())))

                                // modify <target> <variable> <value> (Admin Only)
                                .then(Commands.literal("modify")
                                                .requires(src -> src.hasPermission(2))
                                                .then(Commands.argument("target",
                                                                java.util.Objects.requireNonNull(
                                                                                EntityArgument.player()))
                                                                .then(Commands.argument("variable", java.util.Objects
                                                                                .requireNonNull(StringArgumentType
                                                                                                .word()))
                                                                                .suggests(CreracesCommand::suggestVariables)
                                                                                .then(Commands.argument("value",
                                                                                                java.util.Objects
                                                                                                                .requireNonNull(StringArgumentType
                                                                                                                                .word()))
                                                                                                .suggests(CreracesCommand::suggestValues)
                                                                                                .executes(ctx -> executeModify(
                                                                                                                ctx.getSource(),
                                                                                                                EntityArgument.getPlayer(
                                                                                                                                ctx,
                                                                                                                                "target"),
                                                                                                                StringArgumentType
                                                                                                                                .getString(ctx, "variable"),
                                                                                                                StringArgumentType
                                                                                                                                .getString(ctx, "value")))))))

                                // pocket subcommand
                                .then(Commands.literal("pocket")
                                                .then(Commands.literal("goto")
                                                                .requires(src -> src.hasPermission(2))
                                                                .then(Commands.argument("index", java.util.Objects
                                                                                .requireNonNull(IntegerArgumentType
                                                                                                .integer(1)))
                                                                                .executes(ctx -> executePocketTeleport(
                                                                                                ctx.getSource(),
                                                                                                IntegerArgumentType
                                                                                                                .getInteger(ctx, "index")))))
                                                .then(Commands.literal("invite")
                                                                .then(Commands.argument("target", java.util.Objects
                                                                                .requireNonNull(EntityArgument
                                                                                                .player()))
                                                                                .executes(ctx -> executePocketInvite(
                                                                                                ctx.getSource(),
                                                                                                EntityArgument.getPlayer(
                                                                                                                ctx,
                                                                                                                "target")))))
                                                .then(Commands.literal("revoke")
                                                                .then(Commands.argument("target", java.util.Objects
                                                                                .requireNonNull(EntityArgument
                                                                                                .player()))
                                                                                .executes(ctx -> executePocketRevoke(
                                                                                                ctx.getSource(),
                                                                                                EntityArgument.getPlayer(
                                                                                                                ctx,
                                                                                                                "target")))))
                                                .then(Commands.literal("kick")
                                                                .then(Commands.argument("target", java.util.Objects
                                                                                .requireNonNull(EntityArgument
                                                                                                .player()))
                                                                                .executes(ctx -> executePocketKick(
                                                                                                ctx.getSource(),
                                                                                                EntityArgument.getPlayer(
                                                                                                                ctx,
                                                                                                                "target")))))
                                                .then(Commands.literal("list")
                                                                .executes(ctx -> executePocketList(ctx.getSource())))
                                                .then(Commands.literal("join")
                                                                .then(Commands.argument("host", java.util.Objects
                                                                                .requireNonNull(EntityArgument
                                                                                                .player()))
                                                                                .executes(ctx -> executePocketJoin(
                                                                                                ctx.getSource(),
                                                                                                EntityArgument.getPlayer(
                                                                                                                ctx,
                                                                                                                "host")))))
                                                .then(Commands.literal("leave")
                                                                .executes(ctx -> executePocketLeave(ctx.getSource())))));

                // /raceteam alias for /creraces team
                dispatcher.register(Commands.literal("raceteam")
                                .requires(src -> true)
                                .executes(ctx -> {
                                        ServerPlayer player = ctx.getSource().getPlayer();
                                        if (player == null)
                                                return 0;
                                        mc.sayda.creraces.network.BoundaryHandler.sendOpenTeamGUI(player);
                                        return 1;
                                }));
        }

        private static int executeTerritory(CommandSourceStack source) {
                ServerPlayer player = source.getPlayer();
                if (player == null) return 0;
                mc.sayda.creraces.network.BoundaryHandler.sendTerritoryData(player,
                        mc.sayda.creraces.network.RequestTerritoryDataPacket.buildFor(player));
                mc.sayda.creraces.network.BoundaryHandler.sendTerrainSample(player,
                        mc.sayda.creraces.network.TerrainSamplePacket.buildFor(player));
                mc.sayda.creraces.network.BoundaryHandler.sendOpenTerritoryMap(player);
                return 1;
        }

        private static int executeClan(CommandSourceStack source) {
                ServerPlayer player = source.getPlayer();
                if (player == null) return 0;
                net.minecraft.resources.ResourceLocation myRace =
                        mc.sayda.creraces.capability.DataUtils.getVariables(player)
                                .map(mc.sayda.creraces.capability.IPlayerVariables::getRace)
                                .orElse(null);
                if (myRace == null || myRace.equals(mc.sayda.creraces.race.RaceRegistry.NONE)) {
                        source.sendFailure(Component.literal("You have not chosen a race."));
                        return 0;
                }
                mc.sayda.creraces.territory.TerritoryManager tm = mc.sayda.creraces.territory.TerritoryManager.get();
                mc.sayda.creraces.territory.ClanData clan = tm.getOrCreateClan(myRace);
                mc.sayda.creraces.network.BoundaryHandler.sendClanUpdate(player,
                        mc.sayda.creraces.network.ClanUpdatePacket.from(clan));
                mc.sayda.creraces.network.BoundaryHandler.sendOpenClanManage(player);
                return 1;
        }

        private static int executeHelp(CommandSourceStack source) {
                boolean isOp = source.hasPermission(2);

                source.sendSuccess(
                                () -> java.util.Objects.requireNonNull(Component.translatable("help.creraces.header")
                                                .withStyle(ChatFormatting.GOLD)),
                                false);

                // Public Commands
                sendHelp(source, "/creraces hud",                           "help.creraces.hud");
                sendHelp(source, "/creraces abilities",                      "help.creraces.abilities");
                sendHelp(source, "/creraces select" + (isOp ? " [player]" : ""), "help.creraces.selection");
                sendHelp(source, "/creraces mirror"  + (isOp ? " [player]" : ""), "help.creraces.mirror");
                sendHelp(source, "/creraces debug"   + (isOp ? " [player]" : ""), "help.creraces.debug");
                sendHelp(source, "/creraces team",                           "help.creraces.team");
                sendHelp(source, "/creraces territory",                      "help.creraces.territory");
                sendHelp(source, "/creraces clan",                           "help.creraces.clan");
                sendHelp(source, "/creraces pocket <invite|join|leave|list|kick|revoke>", "help.creraces.pocket");
                sendHelp(source, "/creraces refresh",                        "help.creraces.refresh");

                // OP-Only Commands
                if (isOp) {
                        sendHelp(source, "/creraces reset <player>",              "help.creraces.reset");
                        sendHelp(source, "/creraces setrace <player> <id>",       "help.creraces.setrace");
                        sendHelp(source, "/creraces setrandom <player>",          "help.creraces.setrandom");
                        sendHelp(source, "/creraces grant <player> <ability>",    "help.creraces.grant");
                        sendHelp(source, "/creraces revoke <player> <ability>",   "help.creraces.revoke");
                        sendHelp(source, "/creraces modify <player> <var> <val>", "help.creraces.modify");
                        sendHelp(source, "/creraces reload",                                          "help.creraces.reload");
                        sendHelp(source, "/creraces pocket goto <index>",                             "help.creraces.pocket_goto");
                        sendHelp(source, "/creraces territory race unclaim <race_id>",                "help.creraces.territory_race_unclaim");
                        sendHelp(source, "/creraces territory chunk unclaim <x> <z>",                "help.creraces.territory_chunk_unclaim");
                        sendHelp(source, "/creraces territory chunk info <x> <z>",                   "help.creraces.territory_chunk_info");
                }

                return 1;
        }

        private static void sendHelp(CommandSourceStack source, String command, String descKey) {
                source.sendSuccess(
                                () -> Component.literal(command)
                                                .withStyle(ChatFormatting.AQUA)
                                                .append(Component.translatable(descKey)
                                                                .withStyle(ChatFormatting.WHITE)),
                                false);
        }

        private static int executeOpenSelection(CommandSourceStack source, ServerPlayer target) {
                mc.sayda.creraces.network.BoundaryHandler.sendOpenSelection(target);
                return 1;
        }

        private static int executeOpenMirror(CommandSourceStack source, ServerPlayer target) {
                mc.sayda.creraces.network.BoundaryHandler.sendOpenMirror(target);
                return 1;
        }

        private static int executeOpenDebug(CommandSourceStack source, ServerPlayer target) {
                mc.sayda.creraces.network.BoundaryHandler.sendOpenDebug(target);
                return 1;
        }

        private static int executeReset(CommandSourceStack source, ServerPlayer target) {
                RaceIncidents.transformPlayer(target, RaceRegistry.NONE);
                source.sendSuccess(
                                () -> java.util.Objects.requireNonNull(Component
                                                .literal("Reset race for " + target.getGameProfile().getName())),
                                true);
                return 1;
        }

        private static int executeSet(CommandSourceStack source, ServerPlayer target, ResourceLocation raceId) {
                // Default to creraces namespace if the race is not found in minecraft (default)
                ResourceLocation actualId = raceId;
                if (raceId.getNamespace().equals("minecraft") && mc.sayda.creraces.race.RaceRegistry.get(raceId) == null) {
                        actualId = ResourceLocation.fromNamespaceAndPath("creraces", raceId.getPath());
                }

                Race race = RaceRegistry.get(actualId);

                if (race == null) {
                        source.sendFailure(Component.literal("Unknown race: " + actualId.toString()).withStyle(ChatFormatting.RED));
                        return 0;
                }

                RaceIncidents.transformPlayer(target, race.id());
                source.sendSuccess(() -> java.util.Objects
                                .requireNonNull(Component.translatable("cmd.creraces.set_success",
                                                race.name(), target.getGameProfile().getName())),
                                true);
                return 1;
        }

        private static int executeGrant(CommandSourceStack source, ServerPlayer target, ResourceLocation abilityId) {
                return mc.sayda.creraces.capability.DataUtils.getVariables(target).map(vars -> {
                        mc.sayda.creraces.ability.Ability ability = mc.sayda.creraces.ability.AbilityRegistry
                                        .get(abilityId);
                        if (ability == null) {
                                source.sendFailure(Component.literal("Unknown ability: " + abilityId)
                                                .withStyle(ChatFormatting.RED));
                                return 0;
                        }

                        vars.unlockAbility(abilityId);
                        mc.sayda.creraces.network.BoundaryHandler.resyncVariables(target, target);

                        source.sendSuccess(() -> java.util.Objects.requireNonNull(Component
                                        .literal("Granted " + abilityId + " to " + target.getGameProfile().getName())
                                        .withStyle(ChatFormatting.GREEN)), true);
                        return 1;
                }).orElse(0);
        }

        private static int executeRevoke(CommandSourceStack source, ServerPlayer target, ResourceLocation abilityId) {
                return mc.sayda.creraces.capability.DataUtils.getVariables(target).map(vars -> {
                        vars.revokeAbility(abilityId);

                        // Force refresh to update menus and UI
                        RaceIncidents.refreshPlayer(target);

                        source.sendSuccess(() -> java.util.Objects.requireNonNull(Component
                                        .literal("Revoked " + abilityId + " from " + target.getGameProfile().getName())
                                        .withStyle(ChatFormatting.YELLOW)), true);
                        return 1;
                }).orElse(0);
        }

        private static int executeSetRandom(CommandSourceStack source, ServerPlayer target) {
                List<Race> races = RaceRegistry.getAll().stream()
                        .filter(Race::selectable)
                        .collect(java.util.stream.Collectors.toList());
                if (races.isEmpty())
                        return 0;

                Race randomRace = races.get(RANDOM.nextInt(races.size()));
                return executeSet(source, target, randomRace.id());
        }

        private static int executeReload(CommandSourceStack source) {
                source.sendSuccess(() -> java.util.Objects
                                .requireNonNull(Component.translatable("cmd.creraces.reloading")
                                                .withStyle(ChatFormatting.YELLOW)),
                                true);

                List<String> ids = new ArrayList<>(source.getServer().getPackRepository().getSelectedIds());

                // reloadResources() is async - chain on the server thread so we push fresh data
                // to clients only AFTER the reload has fully completed.
                source.getServer().reloadResources(ids).thenRunAsync(() -> {
                        mc.sayda.creraces.CreRaces.LOGGER.info("CreracesCommand: Reload complete, broadcasting sync packets...");
                        
                        mc.sayda.creraces.network.SyncRacesPacket racePacket = mc.sayda.creraces.race.RaceManager
                                        .createSyncPacket();
                        mc.sayda.creraces.network.SyncAbilitiesPacket abilityPacket = mc.sayda.creraces.ability.AbilityManager
                                        .createSyncPacket();

                        int playerCount = source.getServer().getPlayerList().getPlayers().size();
                        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                                mc.sayda.creraces.network.BoundaryHandler.syncRacesToPlayer(player, racePacket);
                                mc.sayda.creraces.network.BoundaryHandler.syncAbilitiesToPlayer(player, abilityPacket);
                        }

                        // Clear remote wiki-text cache after definitions are pushed
                        mc.sayda.creraces.network.BoundaryHandler.broadcastClearCache();

                        source.sendSuccess(() -> java.util.Objects
                                        .requireNonNull(Component.translatable("cmd.creraces.reloaded")
                                                        .withStyle(ChatFormatting.GREEN)),
                                        true);
                        
                        mc.sayda.creraces.CreRaces.LOGGER.info("CreracesCommand: Synced data to {} players.", playerCount);
                }, source.getServer()); // run on the main server thread

                return 1;
        }

        private static int executeModify(CommandSourceStack source, ServerPlayer target, String variable,
                        String value) {
                return mc.sayda.creraces.capability.DataUtils.getVariables(target).map(vars -> {
                        String varLower = variable.toLowerCase();
                        String normalizedValue = value.toLowerCase();

                        // modify's setters are numeric, so map true/false to 1.0/0.0
                        if (normalizedValue.equals("true"))
                                normalizedValue = "1.0";
                        else if (normalizedValue.equals("false"))
                                normalizedValue = "0.0";

                        final String finalValue = normalizedValue;

                        boolean core = switch (varLower) {
                                case "mana", "energy", "grit", "rage", "karma", "ap", "ad", "ah", "cr", "coins",
                                                "soul", "spirit", "minibuild",
                                                "a1", "a2", "a3", "a4", "a5",
                                                "c1", "c2", "c3", "c4", "c5" ->
                                        true;
                                default -> false;
                        };

                        try {
                                switch (varLower) {
                                        case "mana" -> vars.setMana(Double.parseDouble(finalValue));
                                        case "energy" -> vars.setEnergy(Double.parseDouble(finalValue));
                                        case "grit" -> vars.setGrit(Double.parseDouble(finalValue));
                                        case "rage" -> vars.setRage(Double.parseDouble(finalValue));
                                        case "karma" -> vars.setKarma(Double.parseDouble(finalValue));
                                        case "ap" -> vars.setAp(Double.parseDouble(finalValue));
                                        case "ad" -> vars.setAd(Double.parseDouble(finalValue));
                                        case "ah" -> vars.setAh(Double.parseDouble(finalValue));
                                        case "cr" -> vars.setCr(Double.parseDouble(finalValue));
                                        case "coins" -> vars.setCoins(Double.parseDouble(finalValue));
                                        case "soul" -> vars.setSoul(Double.parseDouble(finalValue));
                                        case "morphed" -> vars.setMorphed(finalValue.equals("1.0"));
                                        case "spirit" -> {
                                                vars.setInSpiritRealm(finalValue.equals("1.0"));
                                                mc.sayda.creraces.network.BoundaryHandler.resyncForAllTrackers(target);
                                                mc.sayda.creraces.network.BoundaryHandler.resyncVariables(target,
                                                                target);
                                        }
                                        case "minibuild" -> vars.setSmallBuild(finalValue.equals("1.0"));
                                        case "gstate" -> vars.setGState(Integer.parseInt(finalValue));
                                        case "a1" -> {
                                                ResourceLocation id = vars.getAbilityInSlot(
                                                                mc.sayda.creraces.ability.AbilitySlot.A1);
                                                if (id != null)
                                                        vars.setPersistentState(id, Double.parseDouble(finalValue));
                                        }
                                        case "a2" -> {
                                                ResourceLocation id = vars.getAbilityInSlot(
                                                                mc.sayda.creraces.ability.AbilitySlot.A2);
                                                if (id != null)
                                                        vars.setPersistentState(id, Double.parseDouble(finalValue));
                                        }
                                        case "a3" -> {
                                                ResourceLocation id = vars.getAbilityInSlot(
                                                                mc.sayda.creraces.ability.AbilitySlot.A3);
                                                if (id != null)
                                                        vars.setPersistentState(id, Double.parseDouble(finalValue));
                                        }
                                        case "a4" -> {
                                                ResourceLocation id = vars.getAbilityInSlot(
                                                                mc.sayda.creraces.ability.AbilitySlot.A4);
                                                if (id != null)
                                                        vars.setPersistentState(id, Double.parseDouble(finalValue));
                                        }
                                        case "a5" -> {
                                                ResourceLocation id = vars.getAbilityInSlot(
                                                                mc.sayda.creraces.ability.AbilitySlot.A5);
                                                if (id != null)
                                                        vars.setPersistentState(id, Double.parseDouble(finalValue));
                                        }
                                        case "c1" -> {
                                                ResourceLocation id = vars.getAbilityInSlot(
                                                                mc.sayda.creraces.ability.AbilitySlot.A1);
                                                if (id != null)
                                                        vars.setCooldown(id, (int) Double.parseDouble(finalValue));
                                        }
                                        case "c2" -> {
                                                ResourceLocation id = vars.getAbilityInSlot(
                                                                mc.sayda.creraces.ability.AbilitySlot.A2);
                                                if (id != null)
                                                        vars.setCooldown(id, (int) Double.parseDouble(finalValue));
                                        }
                                        case "c3" -> {
                                                ResourceLocation id = vars.getAbilityInSlot(
                                                                mc.sayda.creraces.ability.AbilitySlot.A3);
                                                if (id != null)
                                                        vars.setCooldown(id, (int) Double.parseDouble(finalValue));
                                        }
                                        case "c4" -> {
                                                ResourceLocation id = vars.getAbilityInSlot(
                                                                mc.sayda.creraces.ability.AbilitySlot.A4);
                                                if (id != null)
                                                        vars.setCooldown(id, (int) Double.parseDouble(finalValue));
                                        }
                                        case "c5" -> {
                                                ResourceLocation id = vars.getAbilityInSlot(
                                                                mc.sayda.creraces.ability.AbilitySlot.A5);
                                                if (id != null)
                                                        vars.setCooldown(id, (int) Double.parseDouble(finalValue));
                                        }
                                        default -> {
                                                if (varLower.startsWith("state:")) {
                                                        String subKey = varLower.substring(6);
                                                        if (!subKey.contains(":")) {
                                                                subKey = "creraces:" + subKey;
                                                        }
                                                        ResourceLocation id = ResourceLocation.tryParse(subKey);
                                                        if (id != null) {
                                                                vars.setPersistentState(id, Double.parseDouble(finalValue));
                                                        } else {
                                                                vars.setCustomization(varLower, value);
                                                        }
                                                } else {
                                                        vars.setCustomization(varLower, value);
                                                }
                                        }
                                }
                        } catch (NumberFormatException e) {
                                source.sendFailure(Component.literal("Invalid number: " + value));
                                return 0;
                        }

                        // Sync To Client
                        RaceIncidents.refreshPlayer(target);

                        ChatFormatting color = core ? ChatFormatting.GREEN : ChatFormatting.AQUA;
                        source.sendSuccess(() -> Component.literal("Modified " + variable + " to " + value
                                        + " for " + target.getGameProfile().getName())
                                        .withStyle(color), true);

                        return 1;
                }).orElse(0);
        }

        private static int executeRefresh(CommandSourceStack source) {
                ServerPlayer player = source.getPlayer();
                if (player == null)
                        return 0;

                RaceIncidents.refreshPlayer(player);
                source.sendSuccess(() -> Component.literal("Refreshed racial state.")
                                .withStyle(ChatFormatting.GREEN), false);
                return 1;
        }

        private static int executePocketInvite(CommandSourceStack source, ServerPlayer target) {
                ServerPlayer player = source.getPlayer();
                if (player == null)
                        return 0;
                if (player == target) {
                        source.sendFailure(Component.translatable("msg.creraces.pocket.cannot_invite_self"));
                        return 0;
                }

                return mc.sayda.creraces.capability.DataUtils.getVariables(player).map(vars -> {
                        if (!vars.hasPocket()) {
                                source.sendFailure(Component.translatable("msg.creraces.pocket.no_pocket_to_manage"));
                                return 0;
                        }

                        int maxInvites = mc.sayda.creraces.config.CreRacesConfig.POCKET_INVITE_MAX.get();
                        if (maxInvites >= 0 && vars.getPocketInvitations().size() >= maxInvites) {
                                source.sendFailure(Component.translatable("msg.creraces.pocket.max_invites_reached", maxInvites));
                                return 0;
                        }

                        vars.inviteToPocket(target.getUUID());
                        source.sendSuccess(() -> Component.translatable("msg.creraces.pocket.invite_success", target.getDisplayName())
                                        .withStyle(ChatFormatting.GREEN), true);
                        
                        target.sendSystemMessage(Component.translatable("msg.creraces.pocket.invite_received", player.getDisplayName())
                                        .append("\n")
                                        .append(Component.translatable("msg.creraces.pocket.join_command_hint", player.getGameProfile().getName()))
                                        .withStyle(ChatFormatting.GOLD));
                        return 1;
                }).orElse(0);
        }

        private static int executePocketRevoke(CommandSourceStack source, ServerPlayer target) {
                ServerPlayer player = source.getPlayer();
                if (player == null)
                        return 0;

                return mc.sayda.creraces.capability.DataUtils.getVariables(player).map(vars -> {
                        if (!vars.hasPocket()) {
                                source.sendFailure(Component.translatable("msg.creraces.pocket.no_pocket_to_manage"));
                                return 0;
                        }
                        vars.revokePocketInvitation(target.getUUID());
                        source.sendSuccess(() -> Component.translatable("msg.creraces.pocket.revoke_success", target.getDisplayName())
                                        .withStyle(ChatFormatting.YELLOW), true);
                        return 1;
                }).orElse(0);
        }

        private static int executePocketKick(CommandSourceStack source, ServerPlayer target) {
                ServerPlayer player = source.getPlayer();
                if (player == null)
                        return 0;

                // Check if target is in the player's pocket
                String pocketDim = mc.sayda.creraces.config.CreRacesConfig.ACTION_DEFAULT_POCKET_DIM.get();
                if (!target.level().dimension().location().toString().equals(pocketDim)) {
                        source.sendFailure(Component.translatable("msg.creraces.pocket.not_in_pocket_dim"));
                        return 0;
                }

                return mc.sayda.creraces.capability.DataUtils.getVariables(player).map(hostVars -> {
                        if (!hostVars.hasPocket()) {
                                source.sendFailure(Component.translatable("msg.creraces.pocket.no_pocket_to_manage"));
                                return 0;
                        }

                        return mc.sayda.creraces.capability.DataUtils.getVariables(target).map(targetVars -> {
                                double tx = hostVars.getPocketX();
                                double tz = hostVars.getPocketZ();
                                double range = mc.sayda.creraces.config.CreRacesConfig.POCKET_BOUNDARY.get();

                                if (Math.abs(target.getX() - tx) < range && Math.abs(target.getZ() - tz) < range) {
                                        // Kick them out using their OWN return coordinates
                                        String returnDimName = targetVars.getReturnDim();
                                        if (returnDimName == null || returnDimName.isEmpty() || returnDimName.contains("pocket")) {
                                            returnDimName = "minecraft:overworld";
                                        }

                                        net.minecraft.resources.ResourceLocation returnDimLoc = ResourceLocation.parse(
                                                net.minecraft.resources.ResourceLocation.tryParse(returnDimName) != null ? returnDimName : "minecraft:overworld");
                                        net.minecraft.server.level.ServerLevel world = player.server.getLevel(
                                                        net.minecraft.resources.ResourceKey.create(
                                                                        net.minecraft.core.registries.Registries.DIMENSION,
                                                                        returnDimLoc));

                                        if (world == null)
                                                world = player.server.overworld();

                                        target.teleportTo(world, targetVars.getReturnX(), targetVars.getReturnY(), targetVars.getReturnZ(), target.getYRot(), target.getXRot());

                                        source.sendSuccess(() -> Component.translatable("msg.creraces.pocket.kick_success_server", target.getDisplayName())
                                                        .withStyle(ChatFormatting.RED), true);
                                        target.sendSystemMessage(Component.translatable("msg.creraces.pocket.kick_success_client", player.getDisplayName())
                                                        .withStyle(ChatFormatting.RED));
                                        return 1;
                                } else {
                                        source.sendFailure(Component.translatable("msg.creraces.pocket.kick_not_in_area"));
                                        return 0;
                                }
                        }).orElse(0);
                }).orElse(0);
        }

        private static int executePocketList(CommandSourceStack source) {
                ServerPlayer player = source.getPlayer();
                if (player == null)
                        return 0;

                return mc.sayda.creraces.capability.DataUtils.getVariables(player).map(vars -> {
                        if (!vars.hasPocket()) {
                                source.sendFailure(Component.translatable("msg.creraces.pocket.no_pocket_to_manage"));
                                return 0;
                        }
                        java.util.Set<java.util.UUID> invites = vars.getPocketInvitations();
                        if (invites.isEmpty()) {
                                source.sendSuccess(() -> Component.translatable("msg.creraces.pocket.list_empty"), false);
                        } else {
                                source.sendSuccess(() -> Component.translatable("msg.creraces.pocket.list_header")
                                                .withStyle(ChatFormatting.GOLD), false);
                                for (java.util.UUID uuid : invites) {
                                        String name = uuid.toString();
                                        ServerPlayer target = player.server.getPlayerList().getPlayer(uuid);
                                        if (target != null) {
                                            name = target.getGameProfile().getName();
                                        }
                                        final String finalName = name;
                                        source.sendSuccess(() -> Component.literal("- " + finalName)
                                                        .withStyle(ChatFormatting.GRAY), false);
                                }
                        }
                        return 1;
                }).orElse(0);
        }

        @SuppressWarnings("null")
        private static int executePocketTeleport(CommandSourceStack source, int index) {
                ServerPlayer player = source.getPlayer();
                if (player == null)
                        return 0;

                String pocketDimName = mc.sayda.creraces.config.CreRacesConfig.ACTION_DEFAULT_POCKET_DIM.get();
                net.minecraft.server.level.ServerLevel pocketWorld = player.server.getLevel(
                                net.minecraft.resources.ResourceKey.create(
                                                net.minecraft.core.registries.Registries.DIMENSION,
                                                ResourceLocation.parse(net.minecraft.resources.ResourceLocation.tryParse(pocketDimName) != null ? pocketDimName : "minecraft:overworld")));

                if (pocketWorld == null) {
                        source.sendFailure(Component.translatable("msg.creraces.pocket.not_found"));
                        return 0;
                }

                double tx = 1000 * (index % 1000);
                double ty = mc.sayda.creraces.capability.DataUtils.getVariables(player)
                        .map(v -> v.getPocketSpawnY()).orElse(128.0);
                double tz = 1000 * (index / 1000);

                player.teleportTo(pocketWorld, tx + 0.5, ty + 1.0, tz + 0.5, 0, 0);

                source.sendSuccess(() -> Component.translatable("msg.creraces.pocket.teleport_success", index, (int) tx, (int) ty, (int) tz)
                                .withStyle(ChatFormatting.GREEN), false);
                return 1;
        }

        private static int executePocketLeave(CommandSourceStack source) {
                ServerPlayer player = source.getPlayer();
                if (player == null) return 0;

                String pocketDim = mc.sayda.creraces.config.CreRacesConfig.ACTION_DEFAULT_POCKET_DIM.get();
                if (!player.level().dimension().location().toString().equals(pocketDim)) {
                        source.sendFailure(Component.translatable("msg.creraces.pocket.not_in_pocket_dim"));
                        return 0;
                }

                return mc.sayda.creraces.capability.DataUtils.getVariables(player).map(vars -> {
                        String returnDimName = vars.getReturnDim();
                        if (returnDimName == null || returnDimName.isEmpty() || returnDimName.contains("pocket")) {
                                returnDimName = "minecraft:overworld";
                        }

                        ResourceLocation returnDimLoc = ResourceLocation.parse(
                                ResourceLocation.tryParse(returnDimName) != null ? returnDimName : "minecraft:overworld");
                        net.minecraft.server.level.ServerLevel world = player.server.getLevel(
                                net.minecraft.resources.ResourceKey.create(
                                        net.minecraft.core.registries.Registries.DIMENSION, returnDimLoc));

                        if (world == null) world = player.server.overworld();

                        player.teleportTo(world, vars.getReturnX(), vars.getReturnY(), vars.getReturnZ(),
                                player.getYRot(), player.getXRot());
                        source.sendSuccess(() -> Component.translatable("msg.creraces.pocket.leave_success")
                                .withStyle(ChatFormatting.GREEN), false);
                        return 1;
                }).orElse(0);
        }

        @SuppressWarnings("null")
        private static int executePocketJoin(CommandSourceStack source, ServerPlayer host) {
                ServerPlayer player = source.getPlayer();
                if (player == null)
                        return 0;

                java.util.Optional<mc.sayda.creraces.capability.IPlayerVariables> optVars = mc.sayda.creraces.capability.DataUtils
                                .getVariables(host);
                if (!optVars.isPresent())
                        return 0;

                mc.sayda.creraces.capability.IPlayerVariables hostVars = optVars.get();

                if (!hostVars.hasPocket()) {
                        source.sendFailure(Component.translatable("msg.creraces.pocket.no_pocket_initialized", host.getDisplayName()));
                        return 0;
                }

                // Check permissions: always allow if self, if invited, or if OP
                if (!player.getUUID().equals(host.getUUID()) && !hostVars.getPocketInvitations().contains(player.getUUID())
                                && !source.hasPermission(2)) {
                        source.sendFailure(Component.translatable("msg.creraces.pocket.no_invite_to_join", host.getDisplayName()));
                        return 0;
                }

                // Block entry to your own pocket if you haven't claimed a node yet (any race,
                // not just Dryad - node_x/y/z is the same race-agnostic anchor used everywhere else)
                if (player == host) {
                    double tx = hostVars.getPersistentState(ResourceLocation.fromNamespaceAndPath("creraces", "node_x"));
                    double ty = hostVars.getPersistentState(ResourceLocation.fromNamespaceAndPath("creraces", "node_y"));
                    double tz = hostVars.getPersistentState(ResourceLocation.fromNamespaceAndPath("creraces", "node_z"));
                    if (tx == 0 && ty == 0 && tz == 0) {
                        source.sendFailure(Component.translatable("msg.creraces.dryad.no_tree"));
                        return 0;
                    }
                }

                // Teleport to host's pocket
                String pocketDimName = mc.sayda.creraces.config.CreRacesConfig.ACTION_DEFAULT_POCKET_DIM.get();
                net.minecraft.server.level.ServerLevel pocketWorld = player.server.getLevel(
                                net.minecraft.resources.ResourceKey.create(
                                                net.minecraft.core.registries.Registries.DIMENSION,
                                                ResourceLocation.parse(net.minecraft.resources.ResourceLocation.tryParse(pocketDimName) != null ? pocketDimName : "minecraft:overworld")));

                if (pocketWorld == null) {
                        source.sendFailure(Component.translatable("msg.creraces.pocket.not_found"));
                        return 0;
                }

                // Store return point for the player joining
                mc.sayda.creraces.capability.DataUtils.getVariables(player).ifPresent(pVars -> {
                        pVars.setReturnX(player.getX());
                        pVars.setReturnY(player.getY());
                        pVars.setReturnZ(player.getZ());
                        pVars.setReturnDim(java.util.Objects.requireNonNull(player.level().dimension().location()).toString());
                });

                double tx = hostVars.getPocketSpawnX();
                double ty = hostVars.getPocketSpawnY();
                double tz = hostVars.getPocketSpawnZ();

                player.teleportTo(pocketWorld, tx, ty, tz, player.getYRot(), player.getXRot());

                source.sendSuccess(() -> Component.translatable("msg.creraces.pocket.joined", host.getDisplayName())
                                .withStyle(ChatFormatting.GREEN), false);
                return 1;
        }

        private static int executeAdminTerritoryRaceUnclaim(CommandSourceStack source, ResourceLocation raceId) {
                TerritoryManager tm = TerritoryManager.get();
                long before = tm.getClaims().values().stream()
                                .filter(c -> c.getRaceId().equals(raceId)).count();
                if (before == 0) {
                        source.sendFailure(Component.literal("No claims found for race: " + raceId));
                        return 0;
                }
                tm.unclaimAllForRace(raceId);
                source.sendSuccess(() -> Component.literal(
                                "Removed " + before + " claim(s) for race '" + raceId + "'.")
                                .withStyle(ChatFormatting.YELLOW), true);
                return 1;
        }

        private static int executeAdminTerritoryChunkUnclaim(CommandSourceStack source, int chunkX, int chunkZ) {
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                TerritoryManager tm = TerritoryManager.get();
                ClaimData claim = tm.getClaimAt(chunkPos);
                if (claim == null) {
                        source.sendFailure(Component.literal(
                                        "Chunk [" + chunkX + ", " + chunkZ + "] is not claimed."));
                        return 0;
                }
                boolean ok = tm.unclaimChunk(claim.getRaceId(), chunkPos);
                if (!ok) {
                        tm.forceUnclaimChunk(chunkPos);
                        source.sendSuccess(() -> Component.literal(
                                        "Force-unclaimed chunk [" + chunkX + ", " + chunkZ + "] (was anchor).")
                                        .withStyle(ChatFormatting.YELLOW), true);
                } else {
                        source.sendSuccess(() -> Component.literal(
                                        "Unclaimed chunk [" + chunkX + ", " + chunkZ + "].")
                                        .withStyle(ChatFormatting.GREEN), true);
                }
                return 1;
        }

        private static int executeAdminTerritoryChunkInfo(CommandSourceStack source, int chunkX, int chunkZ) {
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                ClaimData claim = TerritoryManager.get().getClaimAt(chunkPos);
                if (claim == null) {
                        source.sendSuccess(() -> Component.literal(
                                        "Chunk [" + chunkX + ", " + chunkZ + "] is unclaimed.")
                                        .withStyle(ChatFormatting.GRAY), false);
                        return 1;
                }
                String raceStr = claim.getRaceId().toString();
                boolean persistent = claim.isPersistent();
                source.sendSuccess(() -> Component.literal(
                                "Chunk [" + chunkX + ", " + chunkZ + "]: race=" + raceStr
                                                + (persistent ? " §e[ANCHOR]§r" : ""))
                                .withStyle(ChatFormatting.AQUA), false);
                return 1;
        }

        private static CompletableFuture<Suggestions> suggestRaces(CommandContext<CommandSourceStack> context,
                        SuggestionsBuilder builder) {
                RaceRegistry.getAll().stream().filter(Race::selectable).forEach(race -> {
                        builder.suggest(race.id().toString());
                });
                return builder.buildFuture();
        }

        private static CompletableFuture<Suggestions> suggestAbilities(CommandContext<CommandSourceStack> context,
                        SuggestionsBuilder builder) {
                mc.sayda.creraces.ability.AbilityRegistry.getAll().forEach(ability -> {
                        builder.suggest(ability.id().toString());
                });
                return builder.buildFuture();
        }

        private static CompletableFuture<Suggestions> suggestUnlockedAbilities(
                        CommandContext<CommandSourceStack> context,
                        SuggestionsBuilder builder) {
                try {
                        ServerPlayer target = EntityArgument.getPlayer(context, "target");
                        mc.sayda.creraces.capability.DataUtils.getVariables(target).ifPresent(vars -> {
                                vars.getUnlockedAbilities().forEach(id -> builder.suggest(id.toString()));
                        });
                } catch (Exception ignored) {
                }
                return builder.buildFuture();
        }

        private static CompletableFuture<Suggestions> suggestVariables(CommandContext<CommandSourceStack> context,
                        SuggestionsBuilder builder) {
                // Core Stats
                List.of("mana", "energy", "grit", "rage", "karma", "ap", "ad", "ah", "cr", "coins", "soul",
                                "gstate", "morphed", "spirit", "minibuild")
                                .forEach(builder::suggest);

                // Ability Slots (State and Cooldown)
                List.of("a1", "a2", "a3", "a4", "a5", "c1", "c2", "c3", "c4", "c5").forEach(builder::suggest);

                // Dynamic Customizations from target
                try {
                        ServerPlayer target = EntityArgument.getPlayer(context, "target");
                        mc.sayda.creraces.capability.DataUtils.getVariables(target).ifPresent(vars -> {
                                vars.getCustomizations().keySet().forEach(builder::suggest);
                        });
                } catch (Exception ignored) {
                }

                return builder.buildFuture();
        }

        private static CompletableFuture<Suggestions> suggestValues(CommandContext<CommandSourceStack> context,
                        SuggestionsBuilder builder) {
                String variable = StringArgumentType.getString(context, "variable").toLowerCase();

                if (variable.equals("gstate")) {
                        builder.suggest("0");
                        builder.suggest("1");
                        return builder.buildFuture();
                }

                if (variable.equals("morphed") || variable.equals("spirit") || variable.equals("minibuild")) {
                        builder.suggest("true");
                        builder.suggest("false");
                        return builder.buildFuture();
                }

                return builder.buildFuture();
        }
}
