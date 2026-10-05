package mc.sayda.creraces.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Set;
import java.util.UUID;

/** /creraces pocket: inviting players into a pocket dimension, visiting one and leaving it. */
@SuppressWarnings("null")
final class PocketCommands {
    private static final ResourceLocation NODE_X = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "node_x");
    private static final ResourceLocation NODE_Y = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "node_y");
    private static final ResourceLocation NODE_Z = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "node_z");

    private PocketCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> pocket() {
        return Commands.literal("pocket")
                .then(Commands.literal("goto")
                        .requires(CreracesCommand::isOp)
                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                .executes(ctx -> teleportToIndex(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "index")))))
                .then(Commands.literal("invite")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> invite(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> revoke(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
                .then(Commands.literal("kick")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> kick(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
                .then(Commands.literal("list")
                        .executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("join")
                        .then(Commands.argument("host", EntityArgument.player())
                                .executes(ctx -> join(ctx.getSource(), EntityArgument.getPlayer(ctx, "host")))))
                .then(Commands.literal("leave")
                        .executes(ctx -> leave(ctx.getSource())));
    }

    private static int invite(CommandSourceStack source, ServerPlayer target) {
        ServerPlayer player = source.getPlayer();
        if (player == null)
            return 0;
        if (player == target) {
            source.sendFailure(Component.translatable("msg.creraces.pocket.cannot_invite_self"));
            return 0;
        }

        return DataUtils.getVariables(player).map(vars -> {
            if (!vars.hasPocket()) {
                source.sendFailure(Component.translatable("msg.creraces.pocket.no_pocket_to_manage"));
                return 0;
            }

            int maxInvites = CreRacesConfig.POCKET_INVITE_MAX.get();
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

    private static int revoke(CommandSourceStack source, ServerPlayer target) {
        ServerPlayer player = source.getPlayer();
        if (player == null)
            return 0;

        return DataUtils.getVariables(player).map(vars -> {
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

    private static int kick(CommandSourceStack source, ServerPlayer target) {
        ServerPlayer player = source.getPlayer();
        if (player == null)
            return 0;

        if (!isInPocketDimension(target)) {
            source.sendFailure(Component.translatable("msg.creraces.pocket.not_in_pocket_dim"));
            return 0;
        }

        return DataUtils.getVariables(player).map(hostVars -> {
            if (!hostVars.hasPocket()) {
                source.sendFailure(Component.translatable("msg.creraces.pocket.no_pocket_to_manage"));
                return 0;
            }

            return DataUtils.getVariables(target).map(targetVars -> {
                double range = CreRacesConfig.POCKET_BOUNDARY.get();
                if (Math.abs(target.getX() - hostVars.getPocketX()) >= range
                        || Math.abs(target.getZ() - hostVars.getPocketZ()) >= range) {
                    source.sendFailure(Component.translatable("msg.creraces.pocket.kick_not_in_area"));
                    return 0;
                }

                // Kicked players go back to their own return point, not the host's
                ServerLevel world = returnLevel(player.server, targetVars.getReturnDim());
                target.teleportTo(world, targetVars.getReturnX(), targetVars.getReturnY(), targetVars.getReturnZ(),
                        target.getYRot(), target.getXRot());

                source.sendSuccess(() -> Component.translatable("msg.creraces.pocket.kick_success_server", target.getDisplayName())
                        .withStyle(ChatFormatting.RED), true);
                target.sendSystemMessage(Component.translatable("msg.creraces.pocket.kick_success_client", player.getDisplayName())
                        .withStyle(ChatFormatting.RED));
                return 1;
            }).orElse(0);
        }).orElse(0);
    }

    private static int list(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null)
            return 0;

        return DataUtils.getVariables(player).map(vars -> {
            if (!vars.hasPocket()) {
                source.sendFailure(Component.translatable("msg.creraces.pocket.no_pocket_to_manage"));
                return 0;
            }
            Set<UUID> invites = vars.getPocketInvitations();
            if (invites.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("msg.creraces.pocket.list_empty"), false);
                return 1;
            }
            source.sendSuccess(() -> Component.translatable("msg.creraces.pocket.list_header")
                    .withStyle(ChatFormatting.GOLD), false);
            for (UUID uuid : invites) {
                ServerPlayer invited = player.server.getPlayerList().getPlayer(uuid);
                String name = invited != null ? invited.getGameProfile().getName() : uuid.toString();
                source.sendSuccess(() -> Component.literal("- " + name).withStyle(ChatFormatting.GRAY), false);
            }
            return 1;
        }).orElse(0);
    }

    private static int teleportToIndex(CommandSourceStack source, int index) {
        ServerPlayer player = source.getPlayer();
        if (player == null)
            return 0;

        ServerLevel pocketWorld = pocketLevel(player.server);
        if (pocketWorld == null) {
            source.sendFailure(Component.translatable("msg.creraces.pocket.not_found"));
            return 0;
        }

        double tx = 1000 * (index % 1000);
        double ty = DataUtils.getVariables(player).map(IPlayerVariables::getPocketSpawnY).orElse(128.0);
        double tz = 1000 * (index / 1000);

        player.teleportTo(pocketWorld, tx + 0.5, ty + 1.0, tz + 0.5, 0, 0);

        source.sendSuccess(() -> Component.translatable("msg.creraces.pocket.teleport_success", index, (int) tx, (int) ty, (int) tz)
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int leave(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        if (!isInPocketDimension(player)) {
            source.sendFailure(Component.translatable("msg.creraces.pocket.not_in_pocket_dim"));
            return 0;
        }

        return DataUtils.getVariables(player).map(vars -> {
            ServerLevel world = returnLevel(player.server, vars.getReturnDim());
            player.teleportTo(world, vars.getReturnX(), vars.getReturnY(), vars.getReturnZ(),
                    player.getYRot(), player.getXRot());
            source.sendSuccess(() -> Component.translatable("msg.creraces.pocket.leave_success")
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        }).orElse(0);
    }

    private static int join(CommandSourceStack source, ServerPlayer host) {
        ServerPlayer player = source.getPlayer();
        if (player == null)
            return 0;

        IPlayerVariables hostVars = DataUtils.getVariables(host).orElse(null);
        if (hostVars == null)
            return 0;

        if (!hostVars.hasPocket()) {
            source.sendFailure(Component.translatable("msg.creraces.pocket.no_pocket_initialized", host.getDisplayName()));
            return 0;
        }

        // The host, invited players and ops may enter
        if (!player.getUUID().equals(host.getUUID()) && !hostVars.getPocketInvitations().contains(player.getUUID())
                && !CreracesCommand.isOp(source)) {
            source.sendFailure(Component.translatable("msg.creraces.pocket.no_invite_to_join", host.getDisplayName()));
            return 0;
        }

        // Your own pocket stays shut until you have claimed a node. node_x/y/z is the race-agnostic
        // anchor every race uses, not just the Dryad's.
        if (player == host) {
            double tx = hostVars.getPersistentState(NODE_X);
            double ty = hostVars.getPersistentState(NODE_Y);
            double tz = hostVars.getPersistentState(NODE_Z);
            if (tx == 0 && ty == 0 && tz == 0) {
                source.sendFailure(Component.translatable("msg.creraces.dryad.no_tree"));
                return 0;
            }
        }

        ServerLevel pocketWorld = pocketLevel(player.server);
        if (pocketWorld == null) {
            source.sendFailure(Component.translatable("msg.creraces.pocket.not_found"));
            return 0;
        }

        DataUtils.getVariables(player).ifPresent(pVars -> {
            pVars.setReturnX(player.getX());
            pVars.setReturnY(player.getY());
            pVars.setReturnZ(player.getZ());
            pVars.setReturnDim(player.level().dimension().location().toString());
        });

        player.teleportTo(pocketWorld, hostVars.getPocketSpawnX(), hostVars.getPocketSpawnY(), hostVars.getPocketSpawnZ(),
                player.getYRot(), player.getXRot());

        source.sendSuccess(() -> Component.translatable("msg.creraces.pocket.joined", host.getDisplayName())
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static boolean isInPocketDimension(ServerPlayer player) {
        return player.level().dimension().location().toString().equals(CreRacesConfig.ACTION_DEFAULT_POCKET_DIM.get());
    }

    /** The configured pocket dimension, falling back to the overworld if the id is malformed. Null if not loaded. */
    private static ServerLevel pocketLevel(MinecraftServer server) {
        ResourceLocation id = ResourceLocation.tryParse(CreRacesConfig.ACTION_DEFAULT_POCKET_DIM.get());
        return server.getLevel(id != null ? ResourceKey.create(Registries.DIMENSION, id) : Level.OVERWORLD);
    }

    /** A saved return dimension, or the overworld when it is unset, unusable or itself a pocket. */
    private static ServerLevel returnLevel(MinecraftServer server, String returnDim) {
        ResourceLocation id = returnDim == null || returnDim.contains("pocket") ? null : ResourceLocation.tryParse(returnDim);
        ServerLevel level = id != null ? server.getLevel(ResourceKey.create(Registries.DIMENSION, id)) : null;
        return level != null ? level : server.overworld();
    }
}
