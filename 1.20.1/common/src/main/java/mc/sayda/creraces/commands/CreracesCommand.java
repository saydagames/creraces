package mc.sayda.creraces.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceIncidents;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * Main command for race management.
 * Consolidates all race-related tools under /creraces.
 */
public class CreracesCommand {
        private static final Random RANDOM = new Random();

        public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
                dispatcher.register(Commands.literal("creraces")
                                // Base command accesible to everyone (for 'abilities' etc)
                                .requires(src -> true)

                                // help (Available to everyone)
                                .then(Commands.literal("help")
                                                .executes(ctx -> executeHelp(ctx.getSource())))

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

                                // select <target> (Available to everyone - Fallback for Race Selection)
                                .then(Commands.literal("select")
                                                .then(Commands.argument("target", EntityArgument.player())
                                                                .requires(src -> src.hasPermission(2)) // Only admins
                                                                                                       // can select for
                                                                                                       // others
                                                                .executes(ctx -> executeOpenSelection(ctx.getSource(),
                                                                                EntityArgument.getPlayer(ctx,
                                                                                                "target"))))
                                                .executes(ctx -> {
                                                        ServerPlayer player = ctx.getSource().getPlayer();
                                                        if (player == null)
                                                                return 0;
                                                        return executeOpenSelection(ctx.getSource(), player);
                                                }))

                                // mirror <target> (Available to everyone - Fallback for Mirror)
                                .then(Commands.literal("mirror")
                                                .then(Commands.argument("target", EntityArgument.player())
                                                                .requires(src -> src.hasPermission(2)) // Only admins
                                                                                                       // can mirror
                                                                                                       // others
                                                                .executes(ctx -> executeOpenMirror(ctx.getSource(),
                                                                                EntityArgument.getPlayer(ctx,
                                                                                                "target"))))
                                                .executes(ctx -> {
                                                        ServerPlayer player = ctx.getSource().getPlayer();
                                                        if (player == null)
                                                                return 0;
                                                        return executeOpenMirror(ctx.getSource(), player);
                                                }))

                                // debug <target> (Available to everyone - Fallback for Debug)
                                .then(Commands.literal("debug")
                                                .then(Commands.argument("target", EntityArgument.player())
                                                                .requires(src -> src.hasPermission(2)) // Only admins
                                                                                                       // can debug
                                                                                                       // others
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
                                                .then(Commands.argument("target", EntityArgument.player())
                                                                .executes(
                                                                                ctx -> executeReset(ctx.getSource(),
                                                                                                EntityArgument.getPlayer(
                                                                                                                ctx,
                                                                                                                "target"))))
                                                .executes(ctx -> {
                                                        ServerPlayer player = ctx.getSource().getPlayer();
                                                        if (player == null)
                                                                return 0;
                                                        return executeReset(ctx.getSource(), player);
                                                }))

                                // setrace <target> <race_id/legacy_id> (Admin Only)
                                .then(Commands.literal("setrace")
                                                .requires(src -> src.hasPermission(2))
                                                .then(Commands.argument("target", EntityArgument.player())
                                                                .then(Commands.argument("race",
                                                                                StringArgumentType.word())
                                                                                .suggests(CreracesCommand::suggestRaces)
                                                                                .executes(ctx -> executeSet(
                                                                                                ctx.getSource(),
                                                                                                EntityArgument.getPlayer(
                                                                                                                ctx,
                                                                                                                "target"),
                                                                                                StringArgumentType
                                                                                                                .getString(ctx, "race"))))))

                                // setrandom <target> (Admin Only)
                                .then(Commands.literal("setrandom")
                                                .requires(src -> src.hasPermission(2))
                                                .then(Commands.argument("target", EntityArgument.player())
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
                                                .then(Commands.argument("target", EntityArgument.player())
                                                                .then(Commands.argument("variable",
                                                                                StringArgumentType.word())
                                                                                .suggests(CreracesCommand::suggestVariables)
                                                                                .then(Commands.argument("value",
                                                                                                StringArgumentType
                                                                                                                .word())
                                                                                                .suggests(CreracesCommand::suggestValues)
                                                                                                .executes(ctx -> executeModify(
                                                                                                                ctx.getSource(),
                                                                                                                EntityArgument.getPlayer(
                                                                                                                                ctx,
                                                                                                                                "target"),
                                                                                                                StringArgumentType
                                                                                                                                .getString(ctx, "variable"),
                                                                                                                StringArgumentType
                                                                                                                                .getString(ctx, "value"))))))));
        }

        private static int executeHelp(CommandSourceStack source) {
                boolean isOp = source.hasPermission(2);

                source.sendSuccess(
                                () -> Component.translatable("creraces.help.header")
                                                .withStyle(ChatFormatting.GOLD),
                                false);

                // Public Commands
                source.sendSuccess(
                                () -> Component.literal("/creraces abilities").withStyle(ChatFormatting.GRAY)
                                                .append(Component.translatable("creraces.help.abilities")
                                                                .withStyle(ChatFormatting.DARK_GRAY)),
                                false);

                source.sendSuccess(
                                () -> Component.literal("/creraces select" + (isOp ? " [player]" : ""))
                                                .withStyle(ChatFormatting.GRAY)
                                                .append(Component.translatable("creraces.help.selection")
                                                                .withStyle(ChatFormatting.DARK_GRAY)),
                                false);

                source.sendSuccess(
                                () -> Component.literal("/creraces mirror" + (isOp ? " [player]" : ""))
                                                .withStyle(ChatFormatting.GRAY)
                                                .append(Component.translatable("creraces.help.mirror")
                                                                .withStyle(ChatFormatting.DARK_GRAY)),
                                false);

                source.sendSuccess(
                                () -> Component.literal("/creraces debug" + (isOp ? " [player]" : ""))
                                                .withStyle(ChatFormatting.GRAY)
                                                .append(Component.translatable("creraces.help.debug")
                                                                .withStyle(ChatFormatting.DARK_GRAY)),
                                false);

                // OP-Only Commands
                if (isOp) {
                        source.sendSuccess(
                                        () -> Component.literal("/creraces reset <player>")
                                                        .withStyle(ChatFormatting.GRAY)
                                                        .append(Component.translatable("creraces.help.reset")
                                                                        .withStyle(ChatFormatting.DARK_GRAY)),
                                        false);
                        source.sendSuccess(
                                        () -> (Component) Component.literal("/creraces setrace <player> <id>")
                                                        .withStyle(ChatFormatting.GRAY)
                                                        .append(Component.translatable("creraces.help.setrace")
                                                                        .withStyle(ChatFormatting.DARK_GRAY)),
                                        false);
                        source.sendSuccess(
                                        () -> (Component) Component.literal("/creraces setrandom <player>")
                                                        .withStyle(ChatFormatting.GRAY)
                                                        .append(Component.translatable("creraces.help.setrandom")
                                                                        .withStyle(ChatFormatting.DARK_GRAY)),
                                        false);
                        source.sendSuccess(
                                        () -> (Component) Component.literal("/creraces reload")
                                                        .withStyle(ChatFormatting.GRAY)
                                                        .append(Component
                                                                        .literal(" - Reloads all race and ability data")
                                                                        .withStyle(ChatFormatting.DARK_GRAY)),
                                        false);
                }

                source.sendSuccess(
                                () -> (Component) Component.literal("/creraces refresh")
                                                .withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal(" - Refreshes your attributes and cosmetics")
                                                                .withStyle(ChatFormatting.DARK_GRAY)),
                                false);

                return 1;
        }

        private static int executeOpenSelection(CommandSourceStack source, ServerPlayer target) {
                mc.sayda.creraces.network.BoundaryHandler.openMenu(target);
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
                source.sendSuccess(() -> Component.literal("Reset race for " + target.getGameProfile().getName()),
                                true);
                return 1;
        }

        private static int executeSet(CommandSourceStack source, ServerPlayer target, String input) {
                Optional<Race> targetRace = Optional.empty();

                // 1. Try numeric legacy ID
                try {
                        double legacyId = Double.parseDouble(input);
                        targetRace = RaceRegistry.getRaceByLegacyId(legacyId);
                } catch (NumberFormatException ignored) {
                }

                // 2. Try ResourceLocation
                if (targetRace.isEmpty()) {
                        if (!input.contains(":")) {
                                input = "creraces:" + input;
                        }
                        targetRace = Optional.ofNullable(
                                        RaceRegistry.get(new net.minecraft.resources.ResourceLocation(input)));
                }

                if (targetRace.isEmpty()) {
                        source.sendFailure(Component.literal("Unknown race: " + input).withStyle(ChatFormatting.RED));
                        return 0;
                }

                Race race = targetRace.get();
                RaceIncidents.transformPlayer(target, race.id());
                source.sendSuccess(() -> Component.translatable("creraces.command.set_success",
                                race.name(), target.getGameProfile().getName()), true);
                return 1;
        }

        private static int executeSetRandom(CommandSourceStack source, ServerPlayer target) {
                List<Race> races = new ArrayList<>(RaceRegistry.getAll());
                if (races.isEmpty())
                        return 0;

                Race randomRace = races.get(RANDOM.nextInt(races.size()));
                return executeSet(source, target, randomRace.id().toString());
        }

        private static int executeReload(CommandSourceStack source) {
                source.sendSuccess(() -> Component.translatable("creraces.command.reloading")
                                .withStyle(ChatFormatting.YELLOW), true);

                List<String> ids = new ArrayList<>(source.getServer().getPackRepository().getSelectedIds());

                // reloadResources() is async — chain on the server thread so we push fresh data
                // to clients only AFTER the reload has fully completed.
                source.getServer().reloadResources(ids).thenRunAsync(() -> {
                        mc.sayda.creraces.network.SyncRacesPacket racePacket = mc.sayda.creraces.race.RaceManager
                                        .createSyncPacket();
                        mc.sayda.creraces.network.SyncAbilitiesPacket abilityPacket = mc.sayda.creraces.ability.AbilityManager
                                        .createSyncPacket();

                        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                                mc.sayda.creraces.network.BoundaryHandler.syncRacesToPlayer(player, racePacket);
                                mc.sayda.creraces.network.BoundaryHandler.syncAbilitiesToPlayer(player, abilityPacket);
                        }

                        // Clear remote wiki-text cache after definitions are pushed
                        mc.sayda.creraces.network.BoundaryHandler.broadcastClearCache();

                        source.sendSuccess(() -> Component.translatable("creraces.command.reloaded")
                                        .withStyle(ChatFormatting.GREEN), true);
                }, source.getServer()); // run on the main server thread

                return 1;
        }

        private static int executeModify(CommandSourceStack source, ServerPlayer target, String variable,
                        String value) {
                return mc.sayda.creraces.capability.DataUtils.getVariables(target).map(vars -> {
                        String varLower = variable.toLowerCase();
                        String normalizedValue = value.toLowerCase();

                        // Smart numeric mapping for booleans
                        if (normalizedValue.equals("true"))
                                normalizedValue = "1.0";
                        else if (normalizedValue.equals("false"))
                                normalizedValue = "0.0";

                        final String finalValue = normalizedValue;

                        boolean core = switch (varLower) {
                                case "mana", "energy", "grit", "rage", "karma", "ap", "ad", "ah", "cr", "coins",
                                                "souls", "stacks",
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
                                        case "souls" -> vars.setSouls(Double.parseDouble(finalValue));
                                        case "stacks" -> vars.setStacks(Double.parseDouble(finalValue));
                                        case "morphed" -> vars.setMorphed(finalValue.equals("1.0"));
                                        case "gstate" -> vars.setGState(Integer.parseInt(finalValue));
                                        case "a1" -> {
                                                ResourceLocation id = vars.getAbilityInSlot(
                                                                mc.sayda.creraces.ability.AbilitySlot.A1);
                                                if (id != null)
                                                        vars.setAbilityState(id, Double.parseDouble(finalValue));
                                        }
                                        case "a2" -> {
                                                ResourceLocation id = vars.getAbilityInSlot(
                                                                mc.sayda.creraces.ability.AbilitySlot.A2);
                                                if (id != null)
                                                        vars.setAbilityState(id, Double.parseDouble(finalValue));
                                        }
                                        case "a3" -> {
                                                ResourceLocation id = vars.getAbilityInSlot(
                                                                mc.sayda.creraces.ability.AbilitySlot.A3);
                                                if (id != null)
                                                        vars.setAbilityState(id, Double.parseDouble(finalValue));
                                        }
                                        case "a4" -> {
                                                ResourceLocation id = vars.getAbilityInSlot(
                                                                mc.sayda.creraces.ability.AbilitySlot.A4);
                                                if (id != null)
                                                        vars.setAbilityState(id, Double.parseDouble(finalValue));
                                        }
                                        case "a5" -> {
                                                ResourceLocation id = vars.getAbilityInSlot(
                                                                mc.sayda.creraces.ability.AbilitySlot.A5);
                                                if (id != null)
                                                        vars.setAbilityState(id, Double.parseDouble(finalValue));
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
                                        default -> vars.setCustomization(variable.toLowerCase(), value);
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

        private static CompletableFuture<Suggestions> suggestRaces(CommandContext<CommandSourceStack> context,
                        SuggestionsBuilder builder) {
                RaceRegistry.getAll().forEach(race -> {
                        builder.suggest(race.id().toString());
                        // Hide legacyId from suggestions
                });
                return builder.buildFuture();
        }

        private static CompletableFuture<Suggestions> suggestVariables(CommandContext<CommandSourceStack> context,
                        SuggestionsBuilder builder) {
                // Core Stats
                List.of("mana", "energy", "grit", "rage", "karma", "ap", "ad", "ah", "cr", "coins", "souls", "stacks",
                                "gstate", "morphed")
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

                if (variable.equals("morphed")) {
                        builder.suggest("true");
                        builder.suggest("false");
                        return builder.buildFuture();
                }

                return builder.buildFuture();
        }
}
