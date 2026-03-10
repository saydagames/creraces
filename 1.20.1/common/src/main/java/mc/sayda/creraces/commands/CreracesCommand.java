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

                                .then(Commands.literal("select")
                                                .then(Commands.argument("target", EntityArgument.player())
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

                                .then(Commands.literal("mirror")
                                                .then(Commands.argument("target", EntityArgument.player())
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

                                // setrace <target> <race_id> (Admin Only)
                                .then(Commands.literal("setrace")
                                                .requires(src -> src.hasPermission(2))
                                                .then(Commands.argument("target", EntityArgument.player())
                                                                .then(Commands.argument("race",
                                                                                ResourceLocationArgument.id())
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
                                                .then(Commands.argument("target", EntityArgument.player())
                                                                .then(Commands.argument("ability",
                                                                                ResourceLocationArgument.id())
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
                                                .then(Commands.argument("target", EntityArgument.player())
                                                                .then(Commands.argument("ability",
                                                                                ResourceLocationArgument.id())
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
                                                                                                                                .getString(ctx, "value")))))))
                                .then(Commands.literal("pocket")
                                                .then(Commands.literal("goto")
                                                                .requires(src -> src.hasPermission(2))
                                                                .then(Commands.argument("index",
                                                                                IntegerArgumentType.integer(1))
                                                                                .executes(ctx -> executePocketTeleport(
                                                                                                ctx.getSource(),
                                                                                                IntegerArgumentType
                                                                                                                .getInteger(ctx,
                                                                                                                                "index")))))
                                                .then(Commands.literal("invite")
                                                                .then(Commands.argument("target",
                                                                                EntityArgument.player())
                                                                                .executes(ctx -> executePocketInvite(
                                                                                                ctx.getSource(),
                                                                                                EntityArgument.getPlayer(
                                                                                                                ctx,
                                                                                                                "target")))))
                                                .then(Commands.literal("revoke")
                                                                .then(Commands.argument("target",
                                                                                EntityArgument.player())
                                                                                .executes(ctx -> executePocketRevoke(
                                                                                                ctx.getSource(),
                                                                                                EntityArgument.getPlayer(
                                                                                                                ctx,
                                                                                                                "target")))))
                                                .then(Commands.literal("kick")
                                                                .then(Commands.argument("target",
                                                                                EntityArgument.player())
                                                                                .executes(ctx -> executePocketKick(
                                                                                                ctx.getSource(),
                                                                                                EntityArgument.getPlayer(
                                                                                                                ctx,
                                                                                                                "target")))))
                                                .then(Commands.literal("list")
                                                                .executes(ctx -> executePocketList(ctx.getSource())))
                                                .then(Commands.literal("join")
                                                                .then(Commands.argument("host", EntityArgument.player())
                                                                                .executes(ctx -> executePocketJoin(
                                                                                                ctx.getSource(),
                                                                                                EntityArgument.getPlayer(
                                                                                                                ctx,
                                                                                                                "host")))))));
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
                                () -> Component.literal("/creraces pocket <invite|join|list|kick|revoke>")
                                                .withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal(" - Pocket management commands")
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
                                        () -> Component.literal("/creraces setrace <player> <id>")
                                                        .withStyle(ChatFormatting.GRAY)
                                                        .append(Component.translatable("creraces.help.setrace")
                                                                        .withStyle(ChatFormatting.DARK_GRAY)),
                                        false);
                        source.sendSuccess(
                                        () -> Component.literal("/creraces grant <player> <ability>")
                                                        .withStyle(ChatFormatting.GRAY)
                                                        .append(Component.literal(" - Grants an ability to a player")
                                                                        .withStyle(ChatFormatting.DARK_GRAY)),
                                        false);
                        source.sendSuccess(
                                        () -> Component.literal("/creraces revoke <player> <ability>")
                                                        .withStyle(ChatFormatting.GRAY)
                                                        .append(Component.literal(" - Revokes an ability from a player")
                                                                        .withStyle(ChatFormatting.DARK_GRAY)),
                                        false);
                        source.sendSuccess(
                                        () -> Component.literal("/creraces setrandom <player>")
                                                        .withStyle(ChatFormatting.GRAY)
                                                        .append(Component.translatable("creraces.help.setrandom")
                                                                        .withStyle(ChatFormatting.DARK_GRAY)),
                                        false);
                        source.sendSuccess(
                                        () -> Component.literal("/creraces reload")
                                                        .withStyle(ChatFormatting.GRAY)
                                                        .append(Component
                                                                        .literal(" - Reloads all race and ability data")
                                                                        .withStyle(ChatFormatting.DARK_GRAY)),
                                        false);
                        source.sendSuccess(
                                        () -> Component.literal("/creraces pocket goto <index>")
                                                        .withStyle(ChatFormatting.GRAY)
                                                        .append(Component.literal(" - Teleport to any pocket index")
                                                                        .withStyle(ChatFormatting.DARK_GRAY)),
                                        false);
                }

                source.sendSuccess(
                                () -> Component.literal("/creraces refresh")
                                                .withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal(" - Refreshes your attributes and cosmetics")
                                                                .withStyle(ChatFormatting.DARK_GRAY)),
                                false);

                return 1;
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
                source.sendSuccess(() -> Component.literal("Reset race for " + target.getGameProfile().getName()),
                                true);
                return 1;
        }

        private static int executeSet(CommandSourceStack source, ServerPlayer target, ResourceLocation raceId) {
                Race race = RaceRegistry.get(raceId);

                if (race == null) {
                        source.sendFailure(Component.literal("Unknown race: " + raceId).withStyle(ChatFormatting.RED));
                        return 0;
                }

                RaceIncidents.transformPlayer(target, race.id());
                source.sendSuccess(() -> Component.translatable("creraces.command.set_success",
                                race.name(), target.getGameProfile().getName()), true);
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

                        source.sendSuccess(() -> Component
                                        .literal("Granted " + abilityId + " to " + target.getGameProfile().getName())
                                        .withStyle(ChatFormatting.GREEN), true);
                        return 1;
                }).orElse(0);
        }

        private static int executeRevoke(CommandSourceStack source, ServerPlayer target, ResourceLocation abilityId) {
                return mc.sayda.creraces.capability.DataUtils.getVariables(target).map(vars -> {
                        vars.revokeAbility(abilityId);

                        // Force refresh to update menus and UI
                        RaceIncidents.refreshPlayer(target);

                        source.sendSuccess(() -> Component
                                        .literal("Revoked " + abilityId + " from " + target.getGameProfile().getName())
                                        .withStyle(ChatFormatting.YELLOW), true);
                        return 1;
                }).orElse(0);
        }

        private static int executeSetRandom(CommandSourceStack source, ServerPlayer target) {
                List<Race> races = new ArrayList<>(RaceRegistry.getAll());
                if (races.isEmpty())
                        return 0;

                Race randomRace = races.get(RANDOM.nextInt(races.size()));
                return executeSet(source, target, randomRace.id());
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
                                                "souls", "stacks", "spirit", "minibuild",
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

        private static int executePocketInvite(CommandSourceStack source, ServerPlayer target) {
                ServerPlayer player = source.getPlayer();
                if (player == null)
                        return 0;
                if (player == target) {
                        source.sendFailure(Component.literal("You cannot invite yourself."));
                        return 0;
                }

                return mc.sayda.creraces.capability.DataUtils.getVariables(player).map(vars -> {
                        vars.inviteToPocket(target.getUUID());
                        source.sendSuccess(() -> Component
                                        .literal("Invited " + target.getGameProfile().getName() + " to your pocket.")
                                        .withStyle(ChatFormatting.GREEN), true);
                        target.sendSystemMessage(Component.literal(player.getGameProfile().getName()
                                        + " has invited you to their pocket! Type /creraces pocket join "
                                        + player.getGameProfile().getName() + " to enter.")
                                        .withStyle(ChatFormatting.GOLD));
                        return 1;
                }).orElse(0);
        }

        private static int executePocketRevoke(CommandSourceStack source, ServerPlayer target) {
                ServerPlayer player = source.getPlayer();
                if (player == null)
                        return 0;

                return mc.sayda.creraces.capability.DataUtils.getVariables(player).map(vars -> {
                        vars.revokePocketInvitation(target.getUUID());
                        source.sendSuccess(() -> Component
                                        .literal("Revoked " + target.getGameProfile().getName() + "'s invitation.")
                                        .withStyle(ChatFormatting.YELLOW), true);
                        return 1;
                }).orElse(0);
        }

        private static int executePocketKick(CommandSourceStack source, ServerPlayer target) {
                ServerPlayer player = source.getPlayer();
                if (player == null)
                        return 0;

                // Check if target is in the player's pocket
                String pocketDim = "creraces:pocket";
                if (!target.level().dimension().location().toString().equals(pocketDim)) {
                        source.sendFailure(Component.literal("Target is not in a pocket dimension."));
                        return 0;
                }

                return mc.sayda.creraces.capability.DataUtils.getVariables(player).map(vars -> {
                        double tx = vars.getPocketX();
                        double tz = vars.getPocketZ();
                        double range = 500; // Pocket boundary

                        if (Math.abs(target.getX() - tx) < range && Math.abs(target.getZ() - tz) < range) {
                                // Kick them out
                                ResourceLocation returnDimName = new ResourceLocation(vars.getReturnDim());
                                net.minecraft.server.level.ServerLevel world = player.server.getLevel(
                                                net.minecraft.resources.ResourceKey.create(
                                                                net.minecraft.core.registries.Registries.DIMENSION,
                                                                returnDimName));
                                if (world == null)
                                        world = player.server.overworld();

                                target.teleportTo(world, world.getSharedSpawnPos().getX(),
                                                world.getSharedSpawnPos().getY(), world.getSharedSpawnPos().getZ(), 0,
                                                0);
                                source.sendSuccess(() -> Component
                                                .literal("Kicked " + target.getGameProfile().getName()
                                                                + " from your pocket.")
                                                .withStyle(ChatFormatting.RED), true);
                                target.sendSystemMessage(Component
                                                .literal("You have been kicked from "
                                                                + player.getGameProfile().getName() + "'s pocket.")
                                                .withStyle(ChatFormatting.RED));
                                return 1;
                        } else {
                                source.sendFailure(Component.literal("Target is not inside your pocket area."));
                                return 0;
                        }
                }).orElse(0);
        }

        private static int executePocketList(CommandSourceStack source) {
                ServerPlayer player = source.getPlayer();
                if (player == null)
                        return 0;

                return mc.sayda.creraces.capability.DataUtils.getVariables(player).map(vars -> {
                        java.util.Set<java.util.UUID> invites = vars.getPocketInvitations();
                        if (invites.isEmpty()) {
                                source.sendSuccess(() -> Component.literal("No active invitations."), false);
                        } else {
                                source.sendSuccess(() -> Component.literal("Invited players: ")
                                                .withStyle(ChatFormatting.GOLD), false);
                                for (java.util.UUID uuid : invites) {
                                        source.sendSuccess(() -> Component.literal("- " + uuid.toString())
                                                        .withStyle(ChatFormatting.GRAY), false);
                                }
                        }
                        return 1;
                }).orElse(0);
        }

        private static int executePocketTeleport(CommandSourceStack source, int index) {
                ServerPlayer player = source.getPlayer();
                if (player == null)
                        return 0;

                String pocketDimName = "creraces:pocket";
                net.minecraft.server.level.ServerLevel pocketWorld = player.server.getLevel(
                                net.minecraft.resources.ResourceKey.create(
                                                net.minecraft.core.registries.Registries.DIMENSION,
                                                new ResourceLocation(pocketDimName)));

                if (pocketWorld == null) {
                        source.sendFailure(Component.literal("Pocket dimension not found."));
                        return 0;
                }

                double tx = 1000 * (index % 1000);
                double ty = 128;
                double tz = 1000 * (index / 1000);

                player.teleportTo(pocketWorld, tx + 0.5, ty + 1.0, tz + 0.5, 0, 0);

                source.sendSuccess(() -> Component
                                .literal("Teleported to pocket index " + index + " at " + (int) tx + ", " + (int) ty
                                                + ", " + (int) tz)
                                .withStyle(ChatFormatting.GREEN), false);
                return 1;
        }

        private static int executePocketJoin(CommandSourceStack source, ServerPlayer host) {
                ServerPlayer player = source.getPlayer();
                if (player == null)
                        return 0;

                return mc.sayda.creraces.capability.DataUtils.getVariables(host).map(hostVars -> {
                        if (!hostVars.getPocketInvitations().contains(player.getUUID()) && !source.hasPermission(2)) {
                                source.sendFailure(Component.literal("You have not been invited to "
                                                + host.getGameProfile().getName() + "'s pocket."));
                                return 0;
                        }

                        // Teleport to host's pocket
                        String pocketDimName = "creraces:pocket";
                        net.minecraft.server.level.ServerLevel pocketWorld = player.server.getLevel(
                                        net.minecraft.resources.ResourceKey.create(
                                                        net.minecraft.core.registries.Registries.DIMENSION,
                                                        new ResourceLocation(pocketDimName)));

                        if (pocketWorld == null) {
                                source.sendFailure(Component.literal("Pocket dimension not found."));
                                return 0;
                        }

                        double tx = hostVars.getPocketSpawnX();
                        double ty = hostVars.getPocketSpawnY();
                        double tz = hostVars.getPocketSpawnZ();

                        player.teleportTo(pocketWorld, tx, ty, tz, 0, 0);

                        source.sendSuccess(() -> Component
                                        .literal("Joined " + host.getGameProfile().getName() + "'s pocket.")
                                        .withStyle(ChatFormatting.GREEN), false);
                        return 1;
                }).orElse(0);
        }

        private static CompletableFuture<Suggestions> suggestRaces(CommandContext<CommandSourceStack> context,
                        SuggestionsBuilder builder) {
                RaceRegistry.getAll().forEach(race -> {
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
                List.of("mana", "energy", "grit", "rage", "karma", "ap", "ad", "ah", "cr", "coins", "souls", "stacks",
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
