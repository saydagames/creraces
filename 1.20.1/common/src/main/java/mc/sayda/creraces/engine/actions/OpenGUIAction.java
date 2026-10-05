package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SmokerBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Opens a screen for the caster. The crafting grid and ender chest open anywhere; container types
 * ("chest", "furnace", "anvil", ...) open the interacted block if it matches, or else the nearest
 * matching block within "radius" (default 4). The mod's own screens ("race_selection", "skill_wheel",
 * "team_menu", "mirror", "debug") are opened client-side.
 */
@SuppressWarnings("null")
public class OpenGUIAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "open_gui");

    private final String guiId;
    private final ScalingValue radius;

    public OpenGUIAction(String guiId, ScalingValue radius) {
        this.guiId = guiId;
        this.radius = radius;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        return switch (guiId) {
            case "crafting_table", "crafting" -> openCraftingGrid(serverPlayer);
            case "ender_chest", "enderchest" -> openEnderChest(serverPlayer);
            case "inventory", "chest", "barrel", "furnace", "smoker", "blast_furnace", "loom", "cartography",
                    "grindstone", "stonecutter", "anvil" -> openNearestContainer(serverPlayer, interactPos, slot);
            case "race_selection", "race_menu" -> {
                BoundaryHandler.sendOpenSelection(serverPlayer);
                yield true;
            }
            case "skill_wheel" -> {
                BoundaryHandler.sendOpenSkillWheel(serverPlayer);
                yield true;
            }
            case "team_menu" -> {
                BoundaryHandler.sendOpenTeamGUI(serverPlayer);
                yield true;
            }
            case "mirror" -> {
                BoundaryHandler.sendOpenMirror(serverPlayer);
                yield true;
            }
            case "debug" -> {
                BoundaryHandler.sendOpenDebug(serverPlayer);
                yield true;
            }
            default -> {
                CreRaces.LOGGER.warn("[OpenGUIAction] Unknown gui id '{}' - no screen opened.", guiId);
                yield false;
            }
        };
    }

    /** The caster's own ender chest inventory, as if they had opened an ender chest block. */
    private static boolean openEnderChest(ServerPlayer player) {
        PlayerEnderChestContainer enderChest = player.getEnderChestInventory();
        player.openMenu(new SimpleMenuProvider((syncId, inventory, p) -> ChestMenu.threeRows(syncId, inventory,
                enderChest), Component.translatable("container.enderchest")));
        return true;
    }

    /** A 3x3 crafting grid that works anywhere, with no crafting table required. */
    private static boolean openCraftingGrid(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider((syncId, inventory, p) -> new CraftingMenu(syncId, inventory),
                Component.translatable("container.crafting")));
        return true;
    }

    private boolean openNearestContainer(ServerPlayer player, @Nullable BlockPos interactPos,
            @Nullable AbilitySlot slot) {
        Level level = player.level();
        BlockPos origin = player.blockPosition();

        BlockPos best = null;
        if (interactPos != null) {
            // An interacted block that isn't the right container means nothing opens; there is no fallback scan.
            if (isMatchingContainer(level.getBlockState(interactPos))) {
                best = interactPos;
            }
        } else {
            int scanRadius = Math.max(1, (int) radius.evaluate(player, null, slot));
            double bestDist = Double.MAX_VALUE;
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-scanRadius, -scanRadius, -scanRadius),
                    origin.offset(scanRadius, scanRadius, scanRadius))) {
                if (isMatchingContainer(level.getBlockState(pos))) {
                    double dist = pos.distSqr(origin);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = pos.immutable();
                    }
                }
            }
        }

        if (best == null) {
            CreRaces.LOGGER.debug("[OpenGUIAction] No container found within {} blocks of {}.",
                    (int) radius.evaluate(player, null, slot), origin);
            return false;
        }
        MenuProvider menu = level.getBlockState(best).getMenuProvider(level, best);
        if (menu == null) {
            return false;
        }
        player.openMenu(menu);
        return true;
    }

    private boolean isMatchingContainer(BlockState state) {
        Block block = state.getBlock();
        return switch (guiId) {
            case "inventory", "chest" -> block instanceof ChestBlock || block instanceof ShulkerBoxBlock;
            case "barrel" -> block instanceof BarrelBlock;
            case "furnace" -> block instanceof FurnaceBlock;
            case "smoker" -> block instanceof SmokerBlock;
            case "blast_furnace" -> block instanceof BlastFurnaceBlock;
            case "loom" -> block instanceof LoomBlock;
            case "cartography" -> block instanceof CartographyTableBlock;
            case "grindstone" -> block instanceof GrindstoneBlock;
            case "stonecutter" -> block instanceof StonecutterBlock;
            case "anvil" -> block instanceof AnvilBlock;
            default -> false;
        };
    }

    public static void register() {
        ActionRegistry.register(ID, json -> new OpenGUIAction(
                GsonHelper.getAsString(json, "gui", "inventory").toLowerCase(),
                ScalingValue.fromJson(json, "radius", 4.0)));
    }
}
