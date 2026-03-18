package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Opens a nearby inventory or crafting surface on behalf of the player.
 * Action that forces a container GUI to open for the player.
 * It searches the area around the interaction position or player position for a
 * container block of the appropriate type.
 * Supported UI types: "crafting", "enderchest", "chest", "barrel", "anvil",
 * "furnace", "smoker", "blast_furnace", "loom", "cartography", "grindstone",
 * "stonecutter"
 * The scan radius is controlled by the {@code "radius"} JSON field (default 4).
 */
public class OpenGUIAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = new ResourceLocation("creraces", "open_gui");

    private final String guiId;
    private final ScalingValue radius;

    public OpenGUIAction(String guiId, ScalingValue radius) {
        this.guiId = guiId;
        this.radius = radius;
    }

    @Override
    public boolean execute(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable BlockPos interactionPos) {

        if (!(player instanceof ServerPlayer sp))
            return false;

        return switch (guiId) {
            case "crafting_table", "crafting" -> openNearestCraftingTable(sp);
            case "ender_chest", "enderchest" -> openEnderChest(sp);
            case "inventory", "chest", "barrel", "furnace", "smoker", "blast_furnace", "loom", "cartography",
                    "grindstone", "stonecutter", "anvil" ->
                openNearestContainer(sp, interactionPos);
            case "race_selection", "race_menu" -> {
                mc.sayda.creraces.network.BoundaryHandler.sendOpenSelection(sp);
                yield true;
            }
            case "skill_wheel" -> {
                mc.sayda.creraces.network.BoundaryHandler.sendOpenSkillWheel(sp);
                yield true;
            }
            case "team_menu" -> {
                mc.sayda.creraces.network.BoundaryHandler.sendOpenTeamGUI(sp);
                yield true;
            }
            case "mirror" -> {
                mc.sayda.creraces.network.BoundaryHandler.sendOpenMirror(sp);
                yield true;
            }
            case "debug" -> {
                mc.sayda.creraces.network.BoundaryHandler.sendOpenDebug(sp);
                yield true;
            }
            default -> {
                CreRaces.LOGGER.warn("[OpenGUIAction] Unknown gui id '{}' - no screen opened.", guiId);
                yield false;
            }
        };
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Opens the player's personal ender chest inventory without requiring a nearby
     * ender chest block. Each player has their own private 27-slot inventory stored
     * server-side - equivalent to right-clicking an ender chest.
     */
    private boolean openEnderChest(ServerPlayer player) {
        net.minecraft.world.inventory.PlayerEnderChestContainer enderInv = player.getEnderChestInventory();
        player.openMenu(new net.minecraft.world.MenuProvider() {
            @Override
            public net.minecraft.network.chat.Component getDisplayName() {
                return net.minecraft.network.chat.Component.translatable("container.enderchest");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                    int syncId,
                    @javax.annotation.Nonnull net.minecraft.world.entity.player.Inventory inv,
                    @javax.annotation.Nonnull net.minecraft.world.entity.player.Player p) {
                return net.minecraft.world.inventory.ChestMenu.threeRows(syncId, inv, enderInv);
            }
        });
        return true;
    }

    /**
     * Opens a full 3×3 crafting grid for the player without requiring a nearby
     * CraftingTable block - equivalent to right-clicking a crafting table but
     * available anywhere.
     */
    private boolean openNearestCraftingTable(ServerPlayer player) {
        player.openMenu(new net.minecraft.world.MenuProvider() {
            @Override
            public net.minecraft.network.chat.Component getDisplayName() {
                return net.minecraft.network.chat.Component.translatable("container.crafting");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                    int syncId,
                    @javax.annotation.Nonnull net.minecraft.world.entity.player.Inventory inv,
                    @javax.annotation.Nonnull net.minecraft.world.entity.player.Player p) {
                return new net.minecraft.world.inventory.CraftingMenu(syncId, inv);
            }
        });
        return true;
    }

    /**
     * Scans nearby blocks for a valid container and opens it.
     */
    private boolean openNearestContainer(ServerPlayer player, @javax.annotation.Nullable BlockPos interactionPos) {
        Level level = player.level();
        BlockPos origin = player.blockPosition();

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        boolean foundContainer = false;

        if (interactionPos != null) {
            if (isMatchingContainer(level.getBlockState(interactionPos))) {
                best = interactionPos;
                foundContainer = true;
            }
        }

        if (!foundContainer && interactionPos == null) {
            int rad = Math.max(1, (int) radius.evaluate(player));
            for (BlockPos pos : BlockPos.betweenClosed(
                    origin.offset(-rad, -rad, -rad),
                    origin.offset(rad, rad, rad))) {
                if (isMatchingContainer(level.getBlockState(pos))) {
                    double dist = pos.distSqr(origin);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = pos.immutable();
                        foundContainer = true;
                    }
                }
            }
        }

        if (best == null) {
            CreRaces.LOGGER.debug("[OpenGUIAction] No container found within {} blocks of {}.",
                    (int) radius.evaluate(player),
                    origin);
            return false;
        }

        MenuProvider mp = level.getBlockState(best).getMenuProvider(level, best);
        if (mp != null) {
            player.openMenu(mp);
            return true;
        }
        return false;
    }

    private boolean isMatchingContainer(net.minecraft.world.level.block.state.BlockState state) {
        net.minecraft.world.level.block.Block block = state.getBlock();
        return switch (guiId) {
            case "inventory", "chest" -> block instanceof net.minecraft.world.level.block.ChestBlock
                    || block instanceof net.minecraft.world.level.block.ShulkerBoxBlock;
            case "barrel" -> block instanceof net.minecraft.world.level.block.BarrelBlock;
            case "furnace" -> block instanceof net.minecraft.world.level.block.FurnaceBlock;
            case "smoker" -> block instanceof net.minecraft.world.level.block.SmokerBlock;
            case "blast_furnace" -> block instanceof net.minecraft.world.level.block.BlastFurnaceBlock;
            case "loom" -> block instanceof net.minecraft.world.level.block.LoomBlock;
            case "cartography" -> block instanceof net.minecraft.world.level.block.CartographyTableBlock;
            case "grindstone" -> block instanceof net.minecraft.world.level.block.GrindstoneBlock;
            case "stonecutter" -> block instanceof net.minecraft.world.level.block.StonecutterBlock;
            case "anvil" -> block instanceof net.minecraft.world.level.block.AnvilBlock;
            default -> false;
        };
    }

    // ─────────────────────────────────────────────────────────────────────────

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "open_gui"), json -> {
            String gui = GsonHelper.getAsString(json, "gui", "inventory").toLowerCase();
            ScalingValue radius = ScalingValue.fromJson(json, "radius", 4.0);
            return new OpenGUIAction(gui, radius);
        });
    }
}
