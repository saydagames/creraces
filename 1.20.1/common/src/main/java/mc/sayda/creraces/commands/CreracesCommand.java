package mc.sayda.creraces.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilityManager;
import mc.sayda.creraces.ability.AbilityRegistry;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.SyncAbilitiesPacket;
import mc.sayda.creraces.network.SyncRacesPacket;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceIncidents;
import mc.sayda.creraces.race.RaceManager;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/**
 * /creraces, the umbrella command for race management. The pocket and territory branches live in
 * {@link PocketCommands} and {@link TerritoryCommands}.
 */
@SuppressWarnings("null")
public class CreracesCommand {
    private static final Random RANDOM = new Random();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("creraces")
                .then(Commands.literal("help")
                        .executes(ctx -> executeHelp(ctx.getSource())))
                .then(openScreenCommand("hud", BoundaryHandler::sendOpenHUDEditor))
                .then(openScreenCommand("abilities", BoundaryHandler::sendOpenSkillWheel))
                .then(openScreenCommand("team", BoundaryHandler::sendOpenTeamGUI))
                .then(TerritoryCommands.territory())
                .then(TerritoryCommands.clan())
                .then(openScreenCommandWithTarget("select", BoundaryHandler::sendOpenSelection))
                .then(openScreenCommandWithTarget("mirror", BoundaryHandler::sendOpenMirror))
                .then(openScreenCommandWithTarget("debug", BoundaryHandler::sendOpenDebug))
                .then(Commands.literal("reset")
                        .requires(CreracesCommand::isOp)
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> executeReset(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"))))
                        .executes(ctx -> runForSelf(ctx, player -> executeReset(ctx.getSource(), player))))
                .then(Commands.literal("setrace")
                        .requires(CreracesCommand::isOp)
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("race", ResourceLocationArgument.id())
                                        .suggests(CreracesCommand::suggestRaces)
                                        .executes(ctx -> executeSet(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target"),
                                                ResourceLocationArgument.getId(ctx, "race"))))))
                .then(Commands.literal("grant")
                        .requires(CreracesCommand::isOp)
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("ability", ResourceLocationArgument.id())
                                        .suggests(CreracesCommand::suggestAbilities)
                                        .executes(ctx -> executeGrant(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target"),
                                                ResourceLocationArgument.getId(ctx, "ability"))))))
                .then(Commands.literal("revoke")
                        .requires(CreracesCommand::isOp)
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("ability", ResourceLocationArgument.id())
                                        .suggests(CreracesCommand::suggestUnlockedAbilities)
                                        .executes(ctx -> executeRevoke(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target"),
                                                ResourceLocationArgument.getId(ctx, "ability"))))))
                .then(Commands.literal("setrandom")
                        .requires(CreracesCommand::isOp)
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> executeSetRandom(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"))))
                        .executes(ctx -> runForSelf(ctx, player -> executeSetRandom(ctx.getSource(), player))))
                .then(Commands.literal("reload")
                        .requires(CreracesCommand::isOp)
                        .executes(ctx -> executeReload(ctx.getSource())))
                .then(Commands.literal("refresh")
                        .executes(ctx -> executeRefresh(ctx.getSource())))
                .then(Commands.literal("modify")
                        .requires(CreracesCommand::isOp)
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("variable", StringArgumentType.word())
                                        .suggests(CreracesCommand::suggestVariables)
                                        .then(Commands.argument("value", StringArgumentType.word())
                                                .suggests(CreracesCommand::suggestValues)
                                                .executes(ctx -> executeModify(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "target"),
                                                        StringArgumentType.getString(ctx, "variable"),
                                                        StringArgumentType.getString(ctx, "value")))))))
                .then(PocketCommands.pocket()));

        dispatcher.register(Commands.literal("raceteam")
                .executes(ctx -> runForSelf(ctx, player -> openScreen(player, BoundaryHandler::sendOpenTeamGUI))));
    }

    static boolean isOp(CommandSourceStack source) {
        return source.hasPermission(2);
    }

    /** Runs a command against the source's own player; from the console it does nothing and returns 0. */
    static int runForSelf(CommandContext<CommandSourceStack> ctx, ToIntFunction<ServerPlayer> command) {
        ServerPlayer player = ctx.getSource().getPlayer();
        return player == null ? 0 : command.applyAsInt(player);
    }

    /** A literal that opens a client screen for whoever runs it. */
    private static LiteralArgumentBuilder<CommandSourceStack> openScreenCommand(String name,
            Consumer<ServerPlayer> screenSender) {
        return Commands.literal(name)
                .executes(ctx -> runForSelf(ctx, player -> openScreen(player, screenSender)));
    }

    /** Like {@link #openScreenCommand}, but ops may name another player to open it for. */
    private static LiteralArgumentBuilder<CommandSourceStack> openScreenCommandWithTarget(String name,
            Consumer<ServerPlayer> screenSender) {
        return openScreenCommand(name, screenSender)
                .then(Commands.argument("target", EntityArgument.player())
                        .requires(CreracesCommand::isOp)
                        .executes(ctx -> openScreen(EntityArgument.getPlayer(ctx, "target"), screenSender)));
    }

    private static int openScreen(ServerPlayer player, Consumer<ServerPlayer> screenSender) {
        screenSender.accept(player);
        return 1;
    }

    private static int executeHelp(CommandSourceStack source) {
        boolean isOp = isOp(source);

        source.sendSuccess(() -> Component.translatable("help.creraces.header").withStyle(ChatFormatting.GOLD), false);

        sendHelp(source, "/creraces hud", "help.creraces.hud");
        sendHelp(source, "/creraces abilities", "help.creraces.abilities");
        sendHelp(source, "/creraces select" + (isOp ? " [player]" : ""), "help.creraces.selection");
        sendHelp(source, "/creraces mirror" + (isOp ? " [player]" : ""), "help.creraces.mirror");
        sendHelp(source, "/creraces debug" + (isOp ? " [player]" : ""), "help.creraces.debug");
        sendHelp(source, "/creraces team", "help.creraces.team");
        sendHelp(source, "/creraces territory", "help.creraces.territory");
        sendHelp(source, "/creraces clan", "help.creraces.clan");
        sendHelp(source, "/creraces pocket <invite|join|leave|list|kick|revoke>", "help.creraces.pocket");
        sendHelp(source, "/creraces refresh", "help.creraces.refresh");

        if (isOp) {
            sendHelp(source, "/creraces reset <player>", "help.creraces.reset");
            sendHelp(source, "/creraces setrace <player> <id>", "help.creraces.setrace");
            sendHelp(source, "/creraces setrandom <player>", "help.creraces.setrandom");
            sendHelp(source, "/creraces grant <player> <ability>", "help.creraces.grant");
            sendHelp(source, "/creraces revoke <player> <ability>", "help.creraces.revoke");
            sendHelp(source, "/creraces modify <player> <var> <val>", "help.creraces.modify");
            sendHelp(source, "/creraces reload", "help.creraces.reload");
            sendHelp(source, "/creraces pocket goto <index>", "help.creraces.pocket_goto");
            sendHelp(source, "/creraces territory race unclaim <race_id>", "help.creraces.territory_race_unclaim");
            sendHelp(source, "/creraces territory chunk unclaim <x> <z>", "help.creraces.territory_chunk_unclaim");
            sendHelp(source, "/creraces territory chunk info <x> <z>", "help.creraces.territory_chunk_info");
        }

        return 1;
    }

    private static void sendHelp(CommandSourceStack source, String command, String descKey) {
        source.sendSuccess(() -> Component.literal(command)
                .withStyle(ChatFormatting.AQUA)
                .append(Component.translatable(descKey).withStyle(ChatFormatting.WHITE)), false);
    }

    private static int executeReset(CommandSourceStack source, ServerPlayer target) {
        RaceIncidents.transformPlayer(target, RaceRegistry.NONE);
        source.sendSuccess(() -> Component.literal("Reset race for " + target.getGameProfile().getName()), true);
        return 1;
    }

    private static int executeSet(CommandSourceStack source, ServerPlayer target, ResourceLocation raceId) {
        // An id typed without a namespace parses as minecraft:, so fall back to ours when no such race exists
        ResourceLocation actualId = raceId;
        if (raceId.getNamespace().equals("minecraft") && RaceRegistry.get(raceId) == null) {
            actualId = new ResourceLocation(CreRaces.MODID, raceId.getPath());
        }

        Race race = RaceRegistry.get(actualId);
        if (race == null) {
            source.sendFailure(Component.literal("Unknown race: " + actualId).withStyle(ChatFormatting.RED));
            return 0;
        }

        RaceIncidents.transformPlayer(target, race.id());
        source.sendSuccess(() -> Component.translatable("cmd.creraces.set_success",
                race.name(), target.getGameProfile().getName()), true);
        return 1;
    }

    private static int executeGrant(CommandSourceStack source, ServerPlayer target, ResourceLocation abilityId) {
        return DataUtils.getVariables(target).map(vars -> {
            if (AbilityRegistry.get(abilityId) == null) {
                source.sendFailure(Component.literal("Unknown ability: " + abilityId).withStyle(ChatFormatting.RED));
                return 0;
            }

            vars.unlockAbility(abilityId);
            BoundaryHandler.resyncVariables(target, target);

            source.sendSuccess(() -> Component.literal("Granted " + abilityId + " to " + target.getGameProfile().getName())
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;
        }).orElse(0);
    }

    private static int executeRevoke(CommandSourceStack source, ServerPlayer target, ResourceLocation abilityId) {
        return DataUtils.getVariables(target).map(vars -> {
            vars.revokeAbility(abilityId);
            // Refresh so equipped slots and open menus drop the ability too
            RaceIncidents.refreshPlayer(target);

            source.sendSuccess(() -> Component.literal("Revoked " + abilityId + " from " + target.getGameProfile().getName())
                    .withStyle(ChatFormatting.YELLOW), true);
            return 1;
        }).orElse(0);
    }

    private static int executeSetRandom(CommandSourceStack source, ServerPlayer target) {
        List<Race> races = RaceRegistry.getAll().stream()
                .filter(Race::selectable)
                .toList();
        if (races.isEmpty())
            return 0;

        Race randomRace = races.get(RANDOM.nextInt(races.size()));
        return executeSet(source, target, randomRace.id());
    }

    private static int executeReload(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("cmd.creraces.reloading").withStyle(ChatFormatting.YELLOW), true);

        List<String> ids = new ArrayList<>(source.getServer().getPackRepository().getSelectedIds());

        // reloadResources() is async; chain on the server thread so clients only get fresh data
        // once the reload has fully completed.
        source.getServer().reloadResources(ids).thenRunAsync(() -> {
            CreRaces.LOGGER.info("CreracesCommand: Reload complete, broadcasting sync packets...");

            SyncRacesPacket racePacket = RaceManager.createSyncPacket();
            SyncAbilitiesPacket abilityPacket = AbilityManager.createSyncPacket();

            List<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
            for (ServerPlayer player : players) {
                BoundaryHandler.syncRacesToPlayer(player, racePacket);
                BoundaryHandler.syncAbilitiesToPlayer(player, abilityPacket);
            }

            // Cached wiki text may describe the old definitions
            BoundaryHandler.broadcastClearCache();

            source.sendSuccess(() -> Component.translatable("cmd.creraces.reloaded").withStyle(ChatFormatting.GREEN), true);
            CreRaces.LOGGER.info("CreracesCommand: Synced data to {} players.", players.size());
        }, source.getServer());

        return 1;
    }

    private static int executeModify(CommandSourceStack source, ServerPlayer target, String variable, String value) {
        return DataUtils.getVariables(target).map(vars -> {
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
                        "c1", "c2", "c3", "c4", "c5" -> true;
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
                        BoundaryHandler.resyncForAllTrackers(target);
                        BoundaryHandler.resyncVariables(target, target);
                    }
                    case "minibuild" -> vars.setSmallBuild(finalValue.equals("1.0"));
                    case "gstate" -> vars.setGState(Integer.parseInt(finalValue));
                    case "a1", "a2", "a3", "a4", "a5" -> {
                        ResourceLocation id = abilityInSlot(vars, varLower);
                        if (id != null)
                            vars.setPersistentState(id, Double.parseDouble(finalValue));
                    }
                    case "c1", "c2", "c3", "c4", "c5" -> {
                        ResourceLocation id = abilityInSlot(vars, varLower);
                        if (id != null)
                            vars.setCooldown(id, (int) Double.parseDouble(finalValue));
                    }
                    default -> {
                        ResourceLocation stateId = varLower.startsWith("state:") ? parseStateId(varLower) : null;
                        if (stateId != null) {
                            vars.setPersistentState(stateId, Double.parseDouble(finalValue));
                        } else {
                            vars.setCustomization(varLower, value);
                        }
                    }
                }
            } catch (NumberFormatException e) {
                source.sendFailure(Component.literal("Invalid number: " + value));
                return 0;
            }

            RaceIncidents.refreshPlayer(target);

            ChatFormatting color = core ? ChatFormatting.GREEN : ChatFormatting.AQUA;
            source.sendSuccess(() -> Component.literal("Modified " + variable + " to " + value
                    + " for " + target.getGameProfile().getName()).withStyle(color), true);
            return 1;
        }).orElse(0);
    }

    /** "a3" and "c3" both address the ability equipped in slot A3. */
    private static ResourceLocation abilityInSlot(IPlayerVariables vars, String slotKey) {
        return vars.getAbilityInSlot(AbilitySlot.valueOf("A" + slotKey.charAt(1)));
    }

    /** "state:foo" targets creraces:foo; a namespaced "state:mod:foo" is taken as-is. */
    private static ResourceLocation parseStateId(String variable) {
        String subKey = variable.substring("state:".length());
        if (!subKey.contains(":")) {
            subKey = CreRaces.MODID + ":" + subKey;
        }
        return ResourceLocation.tryParse(subKey);
    }

    private static int executeRefresh(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null)
            return 0;

        RaceIncidents.refreshPlayer(player);
        source.sendSuccess(() -> Component.literal("Refreshed racial state.").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    static CompletableFuture<Suggestions> suggestRaces(CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        RaceRegistry.getAll().stream().filter(Race::selectable).forEach(race -> builder.suggest(race.id().toString()));
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestAbilities(CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        AbilityRegistry.getAll().forEach(ability -> builder.suggest(ability.id().toString()));
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestUnlockedAbilities(CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "target");
            DataUtils.getVariables(target).ifPresent(
                    vars -> vars.getUnlockedAbilities().forEach(id -> builder.suggest(id.toString())));
        } catch (Exception ignored) {
            // The target selector doesn't resolve yet, so there is nothing to suggest
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestVariables(CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        List.of("mana", "energy", "grit", "rage", "karma", "ap", "ad", "ah", "cr", "coins", "soul",
                "gstate", "morphed", "spirit", "minibuild").forEach(builder::suggest);

        // Ability slot state (a1-a5) and cooldown (c1-c5)
        List.of("a1", "a2", "a3", "a4", "a5", "c1", "c2", "c3", "c4", "c5").forEach(builder::suggest);

        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "target");
            DataUtils.getVariables(target).ifPresent(vars -> vars.getCustomizations().keySet().forEach(builder::suggest));
        } catch (Exception ignored) {
            // The target selector doesn't resolve yet; the fixed suggestions above still apply
        }

        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestValues(CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        String variable = StringArgumentType.getString(context, "variable").toLowerCase();

        if (variable.equals("gstate")) {
            builder.suggest("0");
            builder.suggest("1");
        } else if (variable.equals("morphed") || variable.equals("spirit") || variable.equals("minibuild")) {
            builder.suggest("true");
            builder.suggest("false");
        }
        return builder.buildFuture();
    }
}
